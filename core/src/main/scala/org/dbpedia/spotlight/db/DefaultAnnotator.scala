/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.db

import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.exceptions.InputException
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.disambiguate.{ParagraphDisambiguatorJ, Disambiguator}
import org.dbpedia.spotlight.db.model.Tokenizer
import org.dbpedia.spotlight.annotate.{ParagraphAnnotator, Annotator}

/**
 * Annotates a text with DBpedia Resources.
 * This is just an example of how to wire the steps in our pipeline with default configurations.
 *
 * @author maxjakob, pablomendes
 */
class DefaultAnnotator(val tokenizer: Tokenizer, val spotter : Spotter, val disambiguator: Disambiguator) extends Annotator {

  private val LOG = LogFactory.getLog(this.getClass)

  def this(spotter: Spotter, disambiguator: Disambiguator) {
    this(null, spotter, disambiguator)
  }

  @throws(classOf[InputException])
  def annotate(text : String) : java.util.List[DBpediaResourceOccurrence] = {

    val textObject = new Text(text)

    if (tokenizer != null) {
      LOG.info("Tokenizing input text...")
      val tokens = tokenizer.tokenize(textObject)
      textObject.setFeature(new Feature("tokens", tokens))
    }

    LOG.info("Spotting... ("+spotter.getName()+")")
    val spottedSurfaceForms : java.util.List[SurfaceFormOccurrence] = spotter.extract(textObject)

    LOG.info("Disambiguating... ("+disambiguator.name+")")
    val disambiguatedOccurrences : java.util.List[DBpediaResourceOccurrence] = disambiguator.disambiguate(spottedSurfaceForms)

    LOG.info("Done.")
    disambiguatedOccurrences
  }

}

class DefaultParagraphAnnotator(val tokenizer: Tokenizer, val spotter: Spotter, val disambiguator: ParagraphDisambiguatorJ) extends ParagraphAnnotator {

  def this(spotter: Spotter, disambiguator: ParagraphDisambiguatorJ) {
    this(null, spotter, disambiguator)
  }

  private val LOG = LogFactory.getLog(this.getClass)

  @throws(classOf[InputException])
  def annotate(text : String) : java.util.List[DBpediaResourceOccurrence] = {

    val textObject = new Text(text)

    if (tokenizer != null) {
      LOG.info("Tokenizing input text...")
      val tokens = tokenizer.tokenize(textObject)
      textObject.setFeature(new Feature("tokens", tokens))
    }

    LOG.info("Spotting... ("+spotter.getName()+")")
    val spottedSurfaceForms : List[SurfaceFormOccurrence] = asBuffer(spotter.extract(textObject)).toList

    LOG.info("Disambiguating... ("+disambiguator.name+")")
    val disambiguatedOccurrences : java.util.List[DBpediaResourceOccurrence] = disambiguator.disambiguate(Factory.Paragraph.from(spottedSurfaceForms))

    LOG.info("Done.")
    disambiguatedOccurrences
  }

}