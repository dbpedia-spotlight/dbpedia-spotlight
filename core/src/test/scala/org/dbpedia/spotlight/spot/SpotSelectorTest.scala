package org.dbpedia.spotlight.spot

import org.dbpedia.spotlight.model.Factory
import org.scalatest._
import matchers.ShouldMatchers
import org.dbpedia.spotlight.exceptions.{ConfigurationException}

class SpotSelectorTest extends FlatSpec with ShouldMatchers {

    "SpotSelector Factory" should "create a simple selector" in {
        Factory.SpotSelector.fromName("ShortSurfaceFormSelector")
    }

    it should "create one selector from a list" in {
        Factory.SpotSelector.fromNameList("AtLeastOneNounSelector").size==1
    }

    it should "create multiple selectors" in {
        Factory.SpotSelector.fromNameList("ShortSurfaceFormSelector,AtLeastOneNounSelector").size==2
    }

    it should "throw an exception when a name does not exist" in {
        evaluating { Factory.SpotSelector.fromName("fff") } should produce [ConfigurationException]
    }

    it should "throw an exception when the name is empty" in {
        evaluating { Factory.SpotSelector.fromName("") } should produce [IllegalArgumentException]
    }

}

