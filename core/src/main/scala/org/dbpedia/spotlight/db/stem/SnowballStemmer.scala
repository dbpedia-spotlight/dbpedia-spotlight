package org.dbpedia.spotlight.db.stem

import org.dbpedia.spotlight.db.model.Stemmer
import org.tartarus.snowball.SnowballProgram

class SnowballStemmer(stemmer: SnowballProgram) extends Stemmer {

  def this(s: String) {
    this(Class.forName("org.tartarus.snowball.ext.%s".format(s)).newInstance().asInstanceOf[SnowballProgram])
  }

  override def stem(token: String): String = this.synchronized {
    stemmer.setCurrent(token.toLowerCase)
    stemmer.stem()
    stemmer.getCurrent
  }

}
