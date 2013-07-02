package org.dbpedia.spotlight.db.entityTopic

import scala.collection.mutable.HashMap

/**
 *
 * @param mentions
 * @param words
 * @param entityOfMention
 * @param topicOfMention
 * @param entityOfWord
 * @param topicCount
 * @param entityForMentionCount count of entity e being assigned to mention in this document
 * @param entityForWordCount count of entity e being assigned to word in this document
 */
class Document (val mentions:Array[Int],
                val words:Array[Int],
                val entityOfMention:Array[Int],
                val topicOfMention:Array[Int],
                val entityOfWord:Array[Int],
                val topicCount:HashMap[Int,Int],
                val entityForMentionCount:HashMap[Int,Int],
                val entityForWordCount:HashMap[Int,Int]) extends  Serializable{


}
