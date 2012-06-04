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

package org.dbpedia.spotlight.corpora

import org.dbpedia.spotlight.io.AnnotatedTextSource
import java.io.{PrintWriter, File}
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.disambiguate.DefaultDisambiguator
import scalaj.collection.Imports._
import org.dbpedia.spotlight.graph.AdjacencyList
import org.dbpedia.spotlight.model.{SpotlightFactory, Factory, SpotlightConfiguration}

/**
 * Collects information on how DBpedia interconnects correct and incorrect entities in a corpus.
 * @author pablomendes
 */
object StatsCandidates {

    val LOG = LogFactory.getLog(this.getClass)

    def printStats(paragraphId: String,uriSet1: Set[String], uriSet2: Set[String], hops:String, out: PrintWriter) {
        for {x <- uriSet1; y <- uriSet2; if x!=y} yield { // cartesian product
            //val d = intersect(x,y,hop1)
            val d = AdjacencyList.intersect(x,y,hops)
            out.println("%s\t%s\t%s\t%d\t%s".format(paragraphId, x, y, d.length, d.mkString(",")))
        }
    }

    def main(args : Array[String]) {

        val config = new SpotlightConfiguration("conf/eval.properties")
        val factory = new SpotlightFactory(config)
        //val baseDir = "/home/pablo/eval/csaw"
        //val paragraphsFile  = new File(baseDir+"/gold/CSAWoccs.sortedpars.tsv")
        val baseDir = "/home/pablo/eval/huge/gold"
        val paragraphsFile  = new File(baseDir+"/huge.both.tsv")

        val hops = "1hop";

        //val dataset = "mappingbased_properties"
        //val dataset = "article_categories"
        //val dataset = "page_links"
        val dataset = "mappingbased_with_categories"

        val triplesFile = new File("/data/dbpedia/en/"+dataset+"_en.nt")     // read from one disk
        //val triplesFile = new File("/data/dbpedia/en/mappingbased_properties_en.berlin.nt" )

        val incorrects = new PrintWriter(new File(baseDir+"/cohesion."+dataset+"."+hops+".incorrects.tsv")) // write to the other
        val corrects = new PrintWriter(new File(baseDir+"/cohesion."+dataset+"."+hops+".corrects.tsv")) // write to the other

        AdjacencyList.load(triplesFile)

        val disambiguator = new DefaultDisambiguator(factory);

        val paragraphs = AnnotatedTextSource.fromOccurrencesFile(paragraphsFile)

        var j = 0;
        paragraphs.foreach( p => {
            //println("Paragraph with %d occurrences.".format(p.occurrences.size))
            val correctURIs = p.occurrences.map(o => o.resource.uri).toSet
            val incorrectURIs = p.occurrences.foldLeft(List[String]())( (acc, b) => {
                try {
                    val candidates = disambiguator.bestK(Factory.SurfaceFormOccurrence.from(b),5).asScala.map(o => o.resource.uri).toList;
                    acc ++ candidates.filter(c => (c!=b.resource.uri))
                } catch {
                    case e:Exception => {
                        LOG.error(e)
                        e.printStackTrace();
                        acc;
                    }
                }
            }).toSet

            // all-against-all correct URIs
            printStats(p.id, correctURIs,correctURIs,hops,corrects)
            // all corrects vs all incorrects
            printStats(p.id, correctURIs,incorrectURIs,hops,incorrects)

            j = j + 1;
            if (j % 20 == 0) LOG.info(String.format("processed %s paragraphs",j.toString))

        })
        LOG.info(String.format("finished %s paragraphs",j.toString))

        corrects.close()
        incorrects.close()
    }

}
