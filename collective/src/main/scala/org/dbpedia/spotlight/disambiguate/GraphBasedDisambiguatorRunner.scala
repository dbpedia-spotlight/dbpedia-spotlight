package org.dbpedia.spotlight.disambiguate

import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.exceptions.SearchException

/*
 * Copyright 2012 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

/**
 * Just write a runner to see how things work and test whether the new method works
 * User: hector
 * Date: 5/29/12
 * Time: 4:25 PM
 */

object GraphBasedDisambiguatorRunner {

  def main(args: Array[String]) {
    testGraphBasedDisambiguator("../conf/server.properties", "Default", "Default")
  }

  def testGraphBasedDisambiguator(configFileName: String, spotterName: String, disambiguatorName: String) {
    val config = new SpotlightConfiguration(configFileName)
    val factory = new SpotlightFactory(config)

    val spotterPolicy = SpotterPolicy.valueOf(spotterName)
    val spotter = factory.spotter(spotterPolicy)

    val disambiguationPolicy = DisambiguationPolicy.valueOf(disambiguatorName)
    val disambiguator = factory.disambiguator(disambiguationPolicy)

    val occList = process(passage, spotter, disambiguator)

    for (occ <- occList) {
      println("--------------------------")
      println(occ.id, occ.surfaceForm, occ.similarityScore, occ.contextualScore, occ.percentageOfSecondRank)

    }
  }

  def process(text: String, spotter: Spotter, disambiguator: ParagraphDisambiguatorJ): List[DBpediaResourceOccurrence] = {
    val spots: List[SurfaceFormOccurrence] = spotter.extract(new Text(text)).toList


    var resources: List[DBpediaResourceOccurrence] = List()


    if (spots.size == 0) return resources


    try {
      resources = disambiguator.disambiguate(Factory.paragraph.fromJ(spots)).toList
    }
    catch {
      case e: UnsupportedOperationException => {
        throw new SearchException(e)
      }
    }
    resources
  }

  val passage = "Soccer Aid: England glory\n\nEngland regained the Soccer Aid crown" +
    "with a deserved 3-1 win over the Rest of the World at Old Trafford on Sunday evening.\n " +
    "Goals from former United striker Teddy Sheringham, actor Jonathan Wilkes and ex-Sunderland" +
    "star Kevin Phillips secured a third Soccer Aid win for the Three Lions who had earlier gone behind" +
    "to a sublime strike from Kasabian guitarist Sergio Pizzorno.\n\nJust over 67,000 fans - together" +
    "with some famous faces including Wayne Rooney - were packed inside the Reds' stadium for Soccer Aid " +
    "2012, which once again raised millions of pounds for global children’s charity UNICEF. The crowd " +
    "and peak-time television audience alike were treated to a highly entertaining evening involving a " +
    "host of football legends and some of the world’s biggest celebrities from the world of TV, film and " +
    "music.\n\nThe Rest of the World, who went into the game as defending champions, may have had the " +
    "advantage when it came to the number of former United players on their side – Edwin van der Sar " +
    "lined up in goal with Roy Keane and Jaap Stam starting together at the heart of the defence - but " +
    "it was England who made the early running.\n\nThe Three Lions had good pace on both wings with the " +
    "young legs of X Factor star Olly Murs, on the right, and JLS’s Aston Merrygold down the left. And " +
    "both had a couple of early sighters. A neat passing move ended with Merrygold striking an effort " +
    "straight at Van der Sar, before the Dutch stopper saved well with his feet after an excellent run " +
    "and shot from United fan Murs, coming off the right wing.\n\nPhillips blasted both a shot and a " +
    "free-kick over the bar soon after, before the Rest of the World";
}
