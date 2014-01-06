package org.dbpedia.spotlight.db.stem

import org.junit.Test
import org.junit.Assert._
/**
 * Tests SnowballStemmer
 * @author dav009
 */
class SnowballStemmerTest  {

  @Test
  def englishStemmer(){
    val snowballStemmer = new SnowballStemmer("EnglishStemmer")
    assertTrue( "buy".equals(snowballStemmer.stem("buying")))
    assertTrue( "poni".equals(snowballStemmer.stem("ponies")))
  }
}
