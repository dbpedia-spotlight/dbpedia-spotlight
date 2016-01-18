package org.dbpedia.spotlight.db.model

class Stemmer(var isThreadSafe: Boolean = true) {

  def stem(token: String): String = token

}