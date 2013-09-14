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

package org.dbpedia.spotlight.evaluation

import org.dbpedia.spotlight.log.SpotlightLog
import scala.io.Source
import org.dbpedia.spotlight.util.AnnotationFilter
import org.dbpedia.spotlight.string.WikiLinkParser
import scala.collection.JavaConversions._
import java.io.{PrintStream, File}
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.annotate.DefaultParagraphAnnotator
import org.dbpedia.spotlight.disambiguate.{ParagraphDisambiguatorJ, TwoStepDisambiguator}
import org.dbpedia.spotlight.extract.{LuceneTagExtractor, TagExtractorFromAnnotator}
import javax.swing.filechooser.FileNameExtensionFilter
import org.dbpedia.spotlight.model.Factory.OntologyType
import scala._

/**
 * Reads in text from the BBC transcripts dataset and generates output in the format required by their evaluation tool.
 *
 * See:
 * https://github.com/bbcrd/automated-audio-tagging-evaluation
 *
 * @author pablomendes
 */
object EvaluateBBCTranscripts {

    val configuration = new SpotlightConfiguration("conf/bbc.properties");
    //    val confidence = 0.0;
    //    val support = 0;
    val factory = new SpotlightFactory(configuration)
    val disambiguator = new ParagraphDisambiguatorJ(new TwoStepDisambiguator(factory.candidateSearcher, factory.contextSearcher))
    //  val disambiguator = new ParagraphDisambiguatorJ(new TwoStepDisambiguator(factory))

    //val spotter = factory.spotter(SpotterConfiguration.SpotterPolicy.LingPipeSpotter)
//    val annotator = new DefaultParagraphAnnotator(spotter, disambiguator);

    val targetTypesList = List(OntologyType.fromQName("TopicalConcept"),OntologyType.fromQName("Person"),OntologyType.fromQName("Organisation"),OntologyType.fromQName("Place"))

    val coreferenceResolution = true;

    val filter = new AnnotationFilter(configuration)


    val tagExtractor = new LuceneTagExtractor(factory.contextLuceneManager,factory.contextSearcher)

    def main(args: Array[String]) {

        val baseDir = if (args.length>0) args(0) else "/home/pablo/eval/bbc/"

        val inputDir = new File(baseDir+"/gold/automated-transcripts/")
        val outputDir = new File(baseDir+"/spotlight/")

        if (!new File(baseDir).exists) {
            System.err.println("Base directory does not exist. " + baseDir);
            exit();
        }

        // list with URIs in target KB (after removing namespace http://dbpedia.org/resource/)
        val validUris = Source.fromFile(baseDir+"uris.set").getLines().toSet

        var i = 0
        inputDir.listFiles().filter(_.getName.endsWith(".txt")).foreach(f =>{
            i = i + 1
            val cleanText = Source.fromFile(f).mkString // Either get from file

            try {
                SpotlightLog.info(this.getClass, "Doc %d", i)
                SpotlightLog.info(this.getClass, "Doc length: %s tokens", cleanText.split(" ").size)
                //val allEntities = tagExtractor.extract(new Text(cleanText), 5000, List(OntologyType.fromQName("TopicalConcept"))).toList
                val allEntities = tagExtractor.extract(new Text(cleanText), 5000, List()).toList

                // constrain to entities within BBCs KB
                val filteredEntities = allEntities.filter( e => validUris.contains(e._1.uri) )
                //constrain to only entities of allowed types: val finalEntities = spottedEntities.filter( e => (e._1.getTypes.intersect(targetTypesList).size > 0) )

                //constrain to only the entities that occurr explicitly (verbatim) in paragraph
//                var occurringEntities = TagExtractorFromAnnotator.bySimilarity(annotator)
//                                            .rank(annotator.annotate(cleanText).toList)
//                                            .filter( e=> validUris.contains(e._1.uri))

//              // don't constrain, but bump verbatim entities
//                val bumpedOccurringEntities = (filteredEntities ::: occurringEntities).groupBy(_._1).map { case (k,list) => {
//                                                val sum = list.foldLeft(0.0) { (acc,b) => acc + b._2 }
//                                                (k,sum)
//                                            }}.toList
//                                                .sortBy(_._2)
//                                                .reverse


                val rerankedEntities = rerank(filteredEntities).toList

                val finalEntities = rerankedEntities

                //System.out.println(outputDir + f.getName.replaceAll(".txt","") + ".json");
                val setOutputFile: File = new File(outputDir + "/" + f.getName.replaceAll(".txt","") + ".json");
                val allOut = new PrintStream(setOutputFile)
                append(allOut, finalEntities)
                SpotlightLog.info(this.getClass, "NTags: %d", finalEntities.size)
                allOut.close()

            } catch {
                case e: Exception =>
                    SpotlightLog.error(this.getClass, "Exception: %s")
            }

        })

    }

    def score(t: (DBpediaResource,Double)) = {
        val contextual = t._2
        val resource = t._1
        val max = 60000000.0//tags.maxBy( t => t._1.support )._1.support.toDouble // get highest support
        val prior = resource.support.toDouble / max
        val topical = if (resource.getTypes.contains(OntologyType.fromQName("TopicalConcept"))) 1.1 else 1
        val place = if (resource.getTypes.contains(OntologyType.fromQName("PopulatedPlace"))) 1.2 else 1
        val person = if (resource.getTypes.contains(OntologyType.fromQName("Person"))) 1.2 else 1
        //val organisation = if (resource.getTypes.contains(OntologyType.fromQName("Organisation"))) 1.2 else 1
        contextual*topical*place*person * (1 + 10*(prior))
    }

    def rerank(tags: Seq[(DBpediaResource,Double)]) = {
        tags.map( t => (t._1, score(t)) ).sortBy(-1 * _._2) //reweight and rerank
    }

    def append(stream: PrintStream, tags: List[(DBpediaResource, Double)]) {
        val template = "{\"score\": %f, \"link\": \"%s\"}"
        stream.append("[" + tags.map(sl => template.format(sl._2,sl._1.getFullUri)).mkString(", \n") + "]\n")
    }

}