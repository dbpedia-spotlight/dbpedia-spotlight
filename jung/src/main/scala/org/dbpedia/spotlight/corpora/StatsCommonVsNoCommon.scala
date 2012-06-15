/*
 * Copyright 2011 DBpedia Spotlight Development Team
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

package org.dbpedia.spotlight.corpora


import java.io.{PrintWriter, File}
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.disambiguate.DefaultDisambiguator
import scalaj.collection.Imports._
import org.dbpedia.spotlight.graph.AdjacencyList
import org.dbpedia.spotlight.io.{FileOccurrenceSource, AnnotatedTextSource}
import org.dbpedia.spotlight.annotate.DefaultAnnotator
import com.officedepot.cdap2.collection.CompactHashMap
import java.util.concurrent.atomic.AtomicInteger
import collection.immutable.HashSet
import org.dbpedia.spotlight.model._
import io.Source
import org.semanticweb.yars.nx.Node


/**
 * Collects information on how DBpedia interconnects common words (NIL) and other candidates in a corpus.
 * @author pablomendes
 */
object StatsCommonVsNoCommon {

    val LOG = LogFactory.getLog(this.getClass)

    def printStats(paragraphId: String,uriSet1: Set[String], uriSet2: Set[String], hops:String, out: PrintWriter) {
        for {x <- uriSet1; y <- uriSet2; if x!=y} yield { // cartesian product
            //val d = intersect(x,y,hop1)
            val d = AdjacencyList.intersect(x,y,hops)
            out.println("%s\t%s\t%s\t%d\t%s".format(paragraphId, x, y, d.length, d.mkString(",")))
        }
    }

    def getKBSlice(hops: String, occs: Traversable[DBpediaResourceOccurrence]) = {
        hops match {
            case "0hop" => getKBSlice0Hop(occs)
            case "1hop" => getKBSlice1Hop(occs)
        }
    }

    def getKBSlice0Hop(occs: Traversable[DBpediaResourceOccurrence]) : CompactHashMap[String,AtomicInteger] = {
        val immediates = new CompactHashMap[String,AtomicInteger]()
        occs.foreach( occ => immediates
                .getOrElseUpdate(occ.resource.uri, new AtomicInteger(0))
                .incrementAndGet())
        immediates
    }

    /**
     * Gets a slice of the KB representing the neighborhood of a set of occurrences
     */
    def getKBSlice1Hop(occs: Traversable[DBpediaResourceOccurrence]) : CompactHashMap[String,AtomicInteger] = {
        // First pass, generates counts.
        val kbSupport = new CompactHashMap[String,AtomicInteger]()
        occs.foreach(occ => {
            val neighbors = AdjacencyList.mNeighbors.getOrElse(occ.resource.uri, new HashSet[String]()) // +  occ.resource.uri
            neighbors.foreach(n => kbSupport
                .getOrElseUpdate(n, new AtomicInteger(0))
                .incrementAndGet() )


            //                new DBpediaResourceOccurrence(laterOcc.id,
            //                    coreferentOcc.get.resource,
            //                    laterOcc.surfaceForm,
            //                    laterOcc.context,
            //                    laterOcc.textOffset,
            //                    laterOcc.provenance,
            //                    coreferentOcc.get.similarityScore,           // what to put here?
            //                    coreferentOcc.get.percentageOfSecondRank)    // what to put here?

        })
        kbSupport
    }

    /**
     * Computes the percentage of other candidates that occur in the KB neighborhood of a candidate
     */
    def getSupport(candidate: String, paragraphNeighborhood: CompactHashMap[String,AtomicInteger]) = {
        val candidateKbNeighbors = AdjacencyList.mNeighbors.getOrElse(candidate, new HashSet[String]())
        //val kbSupport = candidateKbNeighbors.foldLeft(0)( (acc, b) => { acc + paragraphNeighborhood.getOrElse(b, new AtomicInteger(0)).intValue()  } )
        val kbSupport = candidateKbNeighbors.foldLeft(0)( (acc, b) => { val i = if (paragraphNeighborhood.contains(b)) 1 else 0; acc + i } )
        println("%s\t%s\t%s\t%s".format(candidate, kbSupport, candidateKbNeighbors.size, candidateKbNeighbors))
        if (candidateKbNeighbors.size==0) 0 else kbSupport.toDouble / candidateKbNeighbors.size
    }


    def main(args : Array[String]) {

        val config = new SpotlightConfiguration("../conf/eval.properties")
        val factory = new SpotlightFactory(config)
        //val baseDir = "/home/pablo/eval/csaw"
        //val paragraphsFile  = new File(baseDir+"/gold/CSAWoccs.sortedpars.tsv")
        val baseDir = "/home/pablo/eval/huge/gold"
        val paragraphsFile  = new File(baseDir+"/huge.tsv")

        //val hops = "0hop";
        val hops = "1hop";

        //val dataset = "mappingbased_properties"
        //val dataset = "article_categories"
        val dataset = "page_links"
        //val dataset = "mappingbased_with_categories"

        val triplesFile = new File("/data/dbpedia/en/"+dataset+"_en.nt")     // read from one disk
        //val triplesFile = new File("/data/dbpedia/en/mappingbased_properties_en.berlin.nt" )

        //val allCandidatesOut = new PrintWriter(new File(baseDir+"/candidates.tsv")) // write to the other
        val out = new PrintWriter(new File(baseDir+"/results.tsv."+hops)) // write to the other


        val uriSubSet = Source.fromFile(baseDir+"/candidates.set").getLines.toSet
        LOG.info("Accepting a subset of %s URIs in the KB.")
        def constrain(triple: Array[Node]) : Boolean = {
            //println(new DBpediaResource(triple(0).toString).uri,new DBpediaResource(triple(2).toString).uri)
            //println(uriSubSet.contains(new DBpediaResource(triple(0).toString).uri),uriSubSet.contains(new DBpediaResource(triple(2).toString).uri))

            AdjacencyList.isObjectProperty(triple) &&
            (uriSubSet.contains(new DBpediaResource(triple(0).toString).uri) || uriSubSet.contains(new DBpediaResource(triple(2).toString).uri))
        }

        AdjacencyList.load(triplesFile, constrain)

        val disambiguator = new DefaultDisambiguator(factory);
        val spotter = factory.spotter();

        val occurrences = FileOccurrenceSource.fromFile(paragraphsFile)

        var j = 0;
        occurrences
            //.filter(o => o.resource.uri == "NIL")
            .foreach( annotatedOccurrence => {

            // This is the occurrence we are inspecting
            try {
                val candidates = disambiguator.bestK(Factory.SurfaceFormOccurrence.from(annotatedOccurrence),5).asScala
                println("Candidates: "+candidates.map( c => c.resource.uri ))
                //allCandidatesOut.println(candidates.map( c => c.resource.uri ).mkString("\n"))

                // A detector of common words could add a NIL as early as spotting\
                // we could add a NIL here.

                // If this was an annotation evaluation, we'd have already all candidates in a paragraph
                // But since we're looking at one spot at a time, we have to go ahead and find all the candidates in the paragraph
                val spots = spotter.extract(annotatedOccurrence.context)
                println("Spots (%s): %s".format(spotter.getName, spots.asScala.map( s => s.surfaceForm.name )))
                val otherCandidates = disambiguator.disambiguate(spots).asScala.filterNot(o => {
                    o.surfaceForm.name == annotatedOccurrence.surfaceForm.name ||
                        o.textOffset == annotatedOccurrence.textOffset
                });
                println("Other Candidates: "+otherCandidates.map( c => c.resource.uri ))
                //allCandidatesOut.println(otherCandidates.map( c => c.resource.uri ).mkString("\n"))

                // Get a slice of the KB that contains the neighborhood of the other candidates
                val kbSlice0hop = getKBSlice0Hop(otherCandidates)
                val kbSlice1hop = getKBSlice1Hop(otherCandidates)

                var total = 0.0;
                // keep only candidates that have KB support
                val restricted = candidates.filter( candidate => {
                    val support0hop = getSupport(candidate.resource.uri, kbSlice0hop)
                    val support1hop = getSupport(candidate.resource.uri, kbSlice1hop)
                    val support = (support0hop + (support1hop / 10))
                    total = total + support
                    support > 0.01
                });

                val chosenSupport0hop = if (candidates.size>0) getSupport(candidates.head.resource.uri, kbSlice0hop) else 0
                val chosenSupport1hop = if (candidates.size>0) getSupport(candidates.head.resource.uri, kbSlice1hop) else 0

                val chosenWithoutKB = if (candidates.size>0) candidates.head.resource.uri else "NIL"
                val chosenWithKB = if (restricted.size>0) restricted.head.resource.uri else "NIL"
                val correct = annotatedOccurrence.resource.uri

                println("Chosen without KB: ", chosenWithoutKB)
                println("Chosen with KB:    ", chosenWithKB, chosenSupport0hop, chosenSupport1hop)
                println("Correct:           ", correct)

                out.println("%s\t%s\t%s\t%s\t%s\t%s".format(correct, chosenWithoutKB, chosenWithKB, chosenSupport0hop, chosenSupport1hop, otherCandidates.size))

                //allCandidatesOut.flush()
                out.flush()
            } catch {
                case e:Exception => {
                    LOG.error(e)
                    e.printStackTrace();
                }
            }
            //println(candidates)

            // all-against-all correct URIs
            //printStats(annotatedOccurrence.id, correctURIs,correctURIs,hops,corrects)
            // all corrects vs all incorrects
            //printStats(annotatedOccurrence.id, correctURIs,otherCandidates,hops,incorrects)

            j = j + 1;
            if (j % 20 == 0) LOG.info(String.format("processed %s paragraphs",j.toString))

        })
        LOG.info(String.format("finished %s paragraphs",j.toString))

        out.close()
        //allCandidatesOut.close()
    }

}
