package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.db.stem.LuceneAnalysisSlovakStemmer
import org.dbpedia.spotlight.db.stem.SnowballStemmer

class Stemmer {

  def stem(token: String): String = token

}

object Stemmer {

  def getStemmer(token: String) : Stemmer = {
      if (token.startsWith ("sk."))
        new LuceneAnalysisSlovakStemmer ()
      else
        new SnowballStemmer (token.toString)
  }

}