package org.dbpedia.spotlight.model

import org.dbpedia.spotlight.model.{Token, Paragraph}
import org.dbpedia.spotlight.train.TrainEntityTopicLocal

/**
 * @author dirk
 *          Date: 5/15/14
 *          Time: 9:43 AM
 */
@SerialVersionUID(3891518562128537200L)
case class EntityTopicDocument(tokens: Array[Int],
                               tokenEntities: Array[Int],
                               mentions: Array[Int],
                               mentionEntities: Array[Int],
                               entityTopics: Array[Int]) extends Serializable with Cloneable {
  def this() = this(Array[Int](), Array[Int](), Array[Int](), Array[Int](), Array[Int]())

  override def toString = "EntityTopicDocument(" +
    "tokens=(" + tokens.mkString(",") + ")," +
    "tokenEntities=(" + tokenEntities.mkString(",") + ")," +
    "mentions=(" + mentions.mkString(",") + ")," +
    "mentionEntities=(" + mentionEntities.mkString(",") + ")," +
    "entityTopics=(" + entityTopics.mkString(",") + "))"

  override def clone() = {
    EntityTopicDocument(tokens, tokenEntities.clone(), mentions, mentionEntities.clone(), entityTopics.clone())
  }
}

//entityAssignments is provide information of already fixed entities, this is used for training, s.t. those entities are not sampled but kept as they are
class EntityTopicTrainingDocument(tokens: Array[Int],
                                       tokenEntities: Array[Int],
                                       mentions: Array[Int],
                                       mentionEntities: Array[Int],
                                       entityTopics: Array[Int], 
                                       val entityFixed:Array[Boolean])
  extends EntityTopicDocument(tokens,
                              tokenEntities,
                              mentions,
                              mentionEntities,
                              entityTopics) {
  def this(tokens: Array[Int],
           tokenEntities: Array[Int],
           mentions: Array[Int],
           mentionEntities: Array[Int],
           entityTopics: Array[Int]) =
    this(tokens,
         tokenEntities,
         mentions,
         mentionEntities,
         entityTopics,
         mentionEntities.map(e => e >= 0))

  def this() = this(Array[Int](), Array[Int](), Array[Int](), Array[Int](), Array[Int](), Array[Boolean]())

}

object EntityTopicDocument {
  def fromParagraph(p:Paragraph) = {
    val tokens = p.text.feature("tokens").get.value.asInstanceOf[List[Token]]

    val tokenIds = tokens.withFilter(_.tokenType.id > 0).map(_.tokenType.id).toArray
    val mentionIds = p.occurrences.withFilter(_.surfaceForm.id > 0).map(_.surfaceForm.id).toArray

    EntityTopicDocument(
      tokenIds,
      tokenIds.map(_ => Int.MinValue),
      mentionIds,
      mentionIds.map(_ => Int.MinValue),
      mentionIds.map(_ => Int.MinValue)
    )
  }

}
