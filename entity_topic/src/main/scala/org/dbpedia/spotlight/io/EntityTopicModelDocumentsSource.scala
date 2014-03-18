package org.dbpedia.spotlight.io

import org.dbpedia.spotlight.model._
import scala.Some
import org.dbpedia.spotlight.spot.Spotter
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.db.model.{TokenTypeStore, ResourceStore, SurfaceFormStore, TextTokenizer}

/**
 * Created by dirkw on 3/11/14.
 */
object EntityTopicModelDocumentsSource {

    def fromOccurrenceSource(occSource:OccurrenceSource,
                             spotter:Spotter,
                             tokenizer:TextTokenizer,
                             resStore:ResourceStore)= new Traversable[EntityTopicDocument] {
        def foreach[U](f: (EntityTopicDocument) => U): Unit = {
            var currentContext:Text = new Text("")
            var currentAnnotations = List[DBpediaResourceOccurrence]()

            def doSth() = {
                val tokens = tokenizer.tokenize(currentContext)
                currentContext.setFeature(new Feature("tokens", tokens))

                val spots = spotter.extract(currentContext)
                val entityToMentions =
                    currentAnnotations.
                        map(occ => (Some(occ.resource), occ.surfaceForm)) ++
                        spots.withFilter(spot => !currentAnnotations.exists(_.textOffset == spot.textOffset)).map(spot => {
                            //sample candidate from its support
                            (None, spot.surfaceForm)
                        })

                f(EntityTopicDocument(
                    tokens.map(_.tokenType.id).toArray,
                    tokens.map(_ => Int.MinValue).toArray,
                    entityToMentions.map(_._2.id).toArray,
                    entityToMentions.map(em => em._1.map(e => resStore.getResourceByName(e.uri).id).getOrElse(Int.MinValue)).toArray,
                    entityToMentions.map(_ => Int.MinValue).toArray))
            }

            occSource.foreach(resOcc => {
                if(currentContext == resOcc.context) {
                    currentAnnotations ::= resOcc
                }
                else {
                    if(currentContext.text != "") {
                        doSth
                    }

                    currentContext = resOcc.context
                    currentAnnotations = List(resOcc)
                }
            })
            doSth
        }
    }
    
}

@SerialVersionUID(3891518562128537200L)
case class EntityTopicDocument(tokens:Array[Int], tokenEntities:Array[Int], mentions:Array[Int], mentionEntities:Array[Int], entityTopics:Array[Int]) extends Serializable

