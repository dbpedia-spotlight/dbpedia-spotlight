package org.dbpedia.spotlight.lucene.index

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers


@RunWith(classOf[JUnitRunner])
class InformedIndexTest extends FlatSpec with ShouldMatchers {

  "The informed index" should " be valid" in {

    IndexTest.isIndexValid() should be === true
  }

}

object IndexTest {

  def isIndexValid(): Boolean = {
    //TODO

    false
  }

}