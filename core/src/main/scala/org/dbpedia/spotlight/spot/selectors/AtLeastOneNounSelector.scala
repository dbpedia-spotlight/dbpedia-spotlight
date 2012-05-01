package org.dbpedia.spotlight.spot.selectors

/*
 * *
 *  * Copyright 2011 Pablo Mendes, Max Jakob
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

import scalaj.collection.Imports._

import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.spot.SpotSelector


/**
 *
 * @author pablomendes
 * @author Joachim Daiber (removed tagging, changed to TaggedSpotSelector)
 */
class AtLeastOneNounSelector extends SpotSelector with RequiresAnalyzedText {

  private val LOG = LogFactory.getLog(this.getClass)

  var lastText = ""


  def select(occurrences: java.util.List[SurfaceFormOccurrence]): java.util.List[SurfaceFormOccurrence] = {
    val occs = occurrences.asScala
    occs.filter(o => {
      val tokens = o.context.analysis.get.getTaggedTokens(o.textOffset, o.textOffset + o.surfaceForm.name.length)
      val atLeastOneNoun = (None != tokens.asScala.find(t => t.getPOSTag.startsWith("n") || t.getPOSTag.startsWith("fw"))) // at least one token is a noun.
      atLeastOneNoun
    }).asJava
  }

}
