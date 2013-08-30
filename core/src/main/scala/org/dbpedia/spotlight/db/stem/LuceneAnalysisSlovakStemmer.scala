package org.dbpedia.spotlight.db.stem

import org.dbpedia.spotlight.db.model.Stemmer
import org.apache.lucene.analysis.sk.SlovakStemmer

class LuceneAnalysisSlovakStemmer(stemmer: SlovakStemmer) extends Stemmer {

  def this() {
    this(Class.forName("org.apache.lucene.analysis.sk.SlovakStemmer").newInstance().asInstanceOf[SlovakStemmer])
  }

  override def stem(token: String): String = this.synchronized {
    
    new String (stemmer.stem(token.toLowerCase().toCharArray(), token.toLowerCase().length()));

  }

}
