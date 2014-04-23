package org.dbpedia.spotlight.io

import org.apache.commons.logging.LogFactory
import scala.concurrent.{Await, Future, ExecutionContext}
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.db.model.{SurfaceFormStore, ResourceStore, TextTokenizer}
import org.dbpedia.spotlight.db.WikipediaToDBpediaClosure
import org.dbpedia.spotlight.model.{DBpediaResource, Feature, DBpediaResourceOccurrence, Text}
import org.dbpedia.spotlight.exceptions.{NotADBpediaResourceException, DBpediaResourceNotFoundException, SurfaceFormNotFoundException}
import scala.collection.JavaConversions._
import scala.Array
import scala.concurrent.duration.Duration


/**
 * Created by dirkw on 3/11/14.
 */
object EntityTopicModelDocumentsSource {
  private final val log = LogFactory.getLog(getClass)

  private implicit val execontext = ExecutionContext.global

  def fromOccurrenceSource(occSource: OccurrenceSource,
                           spotter: Spotter,
                           tokenizer: TextTokenizer,
                           resStore: ResourceStore,
                           sfStore: SurfaceFormStore,
                           candidates: Array[Array[Int]],
                           wikiToDBpediaClosure: WikipediaToDBpediaClosure) = new Traversable[EntityTopicDocument] {

    def foreach[U](f: (EntityTopicDocument) => U): Unit = {
      def getResourceId(e: DBpediaResource) = {
        var id = Int.MinValue
        try {
          val uri = wikiToDBpediaClosure.wikipediaToDBpediaURI(e.uri)
          id = resStore.getResourceByName(uri).id
        }
        catch {
          case ex: DBpediaResourceNotFoundException => log.debug(ex.getMessage)
          case ex: NotADBpediaResourceException => log.debug(e.uri + " -> " + ex.getMessage)
        }
        id
      }

      def getDocument(currentContext: Text, currentAnnotations: List[DBpediaResourceOccurrence]) = {
        val tokens = tokenizer.tokenize(currentContext)
        currentContext.setFeature(new Feature("tokens", tokens))

        val spots = spotter.extract(currentContext)

        val anchors =
          currentAnnotations.foldLeft((List[Int](), List[Int]())) {
            case ((resourceIds, sfIds), occ) =>
              var id = 0
              try {
                id = sfStore.getSurfaceForm(occ.surfaceForm.name).id
              }
              catch {
                case ex: SurfaceFormNotFoundException => log.debug(ex.getMessage)
              }
              if (id > 0 && candidates(id) != null && candidates(id).length > 0)
                (getResourceId(occ.resource) :: resourceIds, id :: sfIds)
              else (resourceIds, sfIds)
          }

        val (entities, mentions) = spots.foldLeft(anchors) {
          case ((resourceIds, sfIds), spot) =>
            val id = spot.surfaceForm.id
            if (id > 0 && candidates(id) != null && candidates(id).length > 0 && !currentAnnotations.exists(_.textOffset == spot.textOffset))
              (Int.MinValue :: resourceIds, id :: sfIds)
            else (resourceIds, sfIds)
        }

        val mentionEntities = entities.toArray
        val tokenArray = tokens.filter(_.tokenType.id > 0).toArray

        val document = EntityTopicDocument(
          tokenArray.map(_.tokenType.id),
          tokenArray.map(_ => Int.MinValue),
          mentions.toArray,
          mentionEntities,
          mentionEntities.map(_ => Int.MinValue))

        document
      }

      var currentContext: Text = new Text("")
      var currentAnnotations = List[DBpediaResourceOccurrence]()
      var future: Future[Unit] = null

      occSource.foreach(resOcc => {
        if (currentContext == resOcc.context) {
          currentAnnotations ::= resOcc
        }
        else {
          if (currentContext.text != "") {
            //HACK: parallelize parsing and execution very simply
            if (future != null)
              Await.result(future, Duration.Inf)
            future = Future {
              val doc = getDocument(currentContext, currentAnnotations)
              if (!doc.mentions.isEmpty)
                f(doc)
            }
          }

          currentContext = resOcc.context
          currentAnnotations = List(resOcc)
        }
      })
      f(getDocument(currentContext, currentAnnotations))
    }
  }

}

@SerialVersionUID(3891518562128537200L)
case class EntityTopicDocument(tokens: Array[Int],
                               tokenEntities: Array[Int],
                               mentions: Array[Int],
                               mentionEntities: Array[Int],
                               entityTopics: Array[Int]) extends Serializable {
  def this() = this(Array[Int](), Array[Int](), Array[Int](), Array[Int](), Array[Int]())

  override def toString = "EntityTopicDocument(" +
    "tokens=(" + tokens.mkString(",") + ")," +
    "tokenEntities=(" + tokenEntities.mkString(",") + ")," +
    "mentions=(" + mentions.mkString(",") + ")," +
    "mentionEntities=(" + mentionEntities.mkString(",") + ")," +
    "entityTopics=(" + entityTopics.mkString(",") + "))"

}

