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
import org.semanticweb.yars.nx.parser.NxParser
import org.semanticweb.yars.nx.Node
import org.dbpedia.spotlight.model.{DBpediaResource}
import collection.mutable.{HashSet, HashMap}
import it.unimi.dsi.fastutil.io.BinIO
import java.io.{InputStream, PrintWriter, File, FileInputStream}
import java.util.zip.GZIPInputStream
import org.dbpedia.spotlight.exceptions.IndexException
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.graph.AdjacencyList

/**
 * Collects information on how DBpedia interconnects annotated entities in a corpus.
 * @author pablomendes
 */
object Stats {

    val LOG = LogFactory.getLog(this.getClass)

    def main(args : Array[String]) {

        val paragraphsFile  = new java.io.File("/home/pablo/eval/csaw/gold/CSAWoccs.sortedpars.tsv")

        val hops = "1hop";

        //val dataset = "mappingbased_properties"
        //val dataset = "article_categories"
        //val dataset = "page_links"
        val dataset = "mappingbased_with_categories"

        val triplesFile = new File("/data/dbpedia/en/"+dataset+"_en.nt")     // read from one disk
        val out = new PrintWriter(new File("/home/pablo/eval/csaw/cohesion."+dataset+"."+hops+".tsv")) // write to the other

        AdjacencyList.load(triplesFile)

        val paragraphs = AnnotatedTextSource.fromOccurrencesFile(paragraphsFile)

        var j = 0;
        paragraphs.foreach( p => {
            //println("Paragraph with %d occurrences.".format(p.occurrences.size))
            val uris = p.occurrences.map(o => o.resource.uri).toSet
            for {x <- uris; y <- uris; if x!=y} yield { // cartesian product
                //val d = intersect(x,y,hop1)
                val f = hops match {
                    case "1hop" => AdjacencyList.hop1 _;
                    case "2hop" => AdjacencyList.hop2 _;
                    case "3hop" => AdjacencyList.hop3 _;
                }
                val d = AdjacencyList.intersect(x,y,f)
                out.println("%s\t%s\t%d\t%s".format(x, y, d.length, d.mkString(",")))
            }
            j = j + 1;
            if (j % 20 == 0) LOG.info(String.format("processed %s paragraphs",j.toString))

        })
        LOG.info(String.format("finished %s paragraphs",j.toString))

        out.close()
    }

}
