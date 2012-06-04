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
import scalaj.collection.Imports._
import org.dbpedia.spotlight.io.FileOccurrenceSource
import java.util.concurrent.atomic.AtomicInteger
import collection.immutable.HashSet
import org.dbpedia.spotlight.model._
import io.Source
import org.dbpedia.spotlight.graph.{DBpediaRelationFactory, DBpediaResourceDictionary, CompressedAdjacencyList}
import org.dbpedia.spotlight.spot.Spotter
import com.officedepot.cdap2.collection.CompactHashSet._
import org.dbpedia.spotlight.exceptions.ItemNotFoundException
import collection.immutable.Map._
import com.officedepot.cdap2.collection.{CompactHashSet, CompactHashMap}
import org.dbpedia.spotlight.disambiguate.{TwoStepDisambiguator, ParagraphDisambiguator, Disambiguator, CuttingEdgeDisambiguator}

/**
 * Collects information on how DBpedia interconnects common words (NIL) and other candidates in a corpus.
 * Uses a compressed AdjacencyList in order to be able to load the entire page links set.
 *
 * @author pablomendes
 */
class StatsCommonVsNoCommonCompressed(val relFactory: DBpediaRelationFactory, val kb: Set[DBpediaResource]) {

    val LOG = LogFactory.getLog(this.getClass)



    def getKBSlice(hops: String, occs: Traversable[DBpediaResourceOccurrence]) = {
        hops match {
            case "0hop" => getKBSlice0Hop(occs)
            case "1hop" => getKBSlice1Hop(occs)
        }
    }

    def getKBSlice0Hop(otherCandidates: Traversable[DBpediaResourceOccurrence]) : CompactHashMap[String,AtomicInteger] = {
        print("*0hop* ")
        val immediates = new CompactHashMap[String,AtomicInteger]()
        otherCandidates.foreach( c =>
            immediates.getOrElseUpdate(c.resource.uri, new AtomicInteger(0)).incrementAndGet()
            //relFactory.DBpediaResource.fromName(c.resource.uri) match {
            //                case Some(resource) => immediates.getOrElseUpdate(resource.id, new AtomicInteger(0)).incrementAndGet()
            //                case None =>  LOG.debug("Did not find occ.resource=%s in Dict".format(c.resource.uri))
            //            }
        )
        immediates
    }

    /**
     * Gets a slice of the KB representing the neighborhood of a set of occurrences
     */
    def getKBSlice1Hop(occs: Traversable[DBpediaResourceOccurrence]) : CompactHashMap[String,AtomicInteger] = {
        print("*1hop* ")
        val kbSupport = new CompactHashMap[String,AtomicInteger]()
        occs.foreach(occ => {
            val neighbors = relFactory.DBpediaResource.fromName(occ.resource.uri) match {
                case Some(cResource) => CompressedAdjacencyList.mNeighbors.getOrElse(cResource.id, new HashSet[Int]())
                case None => new HashSet[Int]()
            }

            //neighbors.foreach(n => kbSupport.getOrElseUpdate(n, new AtomicInteger(0)).incrementAndGet() )
            neighbors.foreach(n => kbSupport.getOrElseUpdate(relFactory.DBpediaResource.fromId(n).uri, new AtomicInteger(0)).incrementAndGet() )

        })
        kbSupport
    }

    /**
     * Computes the percentage of other candidates that occur in the KB neighborhood of a candidate
     */
    def getSupport(candidate: String, paragraphNeighborhood: CompactHashMap[String,AtomicInteger]) : (Int,Int) = {
        val candidateKbNeighbors = relFactory.DBpediaResource.fromName(candidate) match {
                case Some(cResource) => CompressedAdjacencyList.mNeighbors.getOrElse(cResource.id, new HashSet[Int]())
                case None => new HashSet[Int]()
            }

        val kbSupport = candidateKbNeighbors.foldLeft(0)( (acc, b) => {
            val i = if (paragraphNeighborhood.contains(relFactory.DBpediaResource.fromId(b).uri)) 1 else 0;
            acc + i
        })
        println("%s\t%s\t%s".format(candidate, kbSupport, candidateKbNeighbors.size))
        if (candidateKbNeighbors.size==0) (0,0) else (kbSupport, candidateKbNeighbors.size)
    }

    /**
     * Given an occurrence that we're trying to disambiguate, find all other surrounding occurrences that may provide context.
     */
    def getOtherCandidatesD(annotatedOccurrence: DBpediaResourceOccurrence, spotter: Spotter, disambiguator: Disambiguator) = {
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
        otherCandidates
    }
    def getOtherCandidatesP(annotatedOccurrence: DBpediaResourceOccurrence, spotter: Spotter, disambiguator: ParagraphDisambiguator) = {
        // If this was an annotation evaluation, we'd have already all candidates in a paragraph
        // But since we're looking at one spot at a time, we have to go ahead and find all the candidates in the paragraph
        val spots = spotter.extract(annotatedOccurrence.context)
        println("Spots (%s): %s".format(spotter.getName, spots.asScala.map( s => s.surfaceForm.name )))
        val otherCandidates = disambiguator.disambiguate(Factory.Paragraph.fromJ(spots)).filterNot(o => {
            o.surfaceForm.name == annotatedOccurrence.surfaceForm.name ||
                o.textOffset == annotatedOccurrence.textOffset
        });
        println("Other Candidates: "+otherCandidates.map( c => c.resource.uri ))
        //allCandidatesOut.println(otherCandidates.map( c => c.resource.uri ).mkString("\n"))
        otherCandidates
    }

    def getCandidatesD(annotatedOccurrence: DBpediaResourceOccurrence, disambiguator: Disambiguator) = {
        val candidates = disambiguator.bestK(Factory.SurfaceFormOccurrence.from(annotatedOccurrence),5).asScala
        println("Candidates: "+candidates.map( c => c.resource.uri ))
        candidates
    }

    def getCandidatesP(annotatedOccurrence: DBpediaResourceOccurrence, disambiguator: ParagraphDisambiguator)  = {
        val occ = Factory.SurfaceFormOccurrence.from(annotatedOccurrence)
        val candidateMap = disambiguator.bestK(Factory.Paragraph.fromJ(List(occ).asJava),5)
        var candidates = new scala.collection.mutable.HashSet[DBpediaResourceOccurrence]();
        candidateMap.foreach( entry => entry._2.foreach( o => candidates.add(o) ) );
        println("Candidates: "+candidates.map( c => c.resource.uri ))
        candidates
    }

    def filter(r: DBpediaResource) = {
        if (kb.contains(r)) {
            r
        } else {
            //LOG.debug("NIL detected by 'uri not found in KB'.")
            new DBpediaResource("NIL")
        }
    }

    def main(args : Array[String]) {

        val configFile = new File("../conf/eval.properties")

        val config = new SpotlightConfiguration(configFile.getAbsolutePath)
        val factory = new SpotlightFactory(config)
        //val baseDir = "/home/pablo/eval/csaw"
        //val paragraphsFile  = new File(baseDir+"/gold/CSAWoccs.sortedpars.tsv")
        val baseDir = "/home/pablo/eval/tac/gold"
        val paragraphsFile  = new File(baseDir+"/TAC-KBP-evaluation-1.1-2010.tsv")

        // URIs that belong to the KB (for TAC KBP which has a smaller KB than ours)
        val kbUriSetFile = new File("/home/pablo/eval/tac/kb.set")

        // Dictionary with mapping from URI to integer, and triples of integers to load the adjacency list
        val dictFile = new File("/home/pablo/data/dbpedia/page_links_en_uris.set.drd")
        val compressedTriplesFile = new File("/data/dbpedia/en/page_links_en.nt.redirectsResolved.cut.sorted.uniq.compressed")

        val validated = (configFile :: paragraphsFile :: kbUriSetFile :: dictFile :: compressedTriplesFile :: Nil).forall( f => {
            if (!f.exists()) {
                println("Could not find file %s".format(f.getAbsolutePath))
            }
            f.exists()
        })
        if (!validated) {
            println("Usage: ...")
            exit()
        }


        //val hops = "0hop";
        val hops = "1hop";

        //val disambiguator : Any = new CuttingEdgeDisambiguator(factory);
        val disambiguator : Any = new TwoStepDisambiguator(factory.candidateSearcher,factory.contextSearcher);

        val kb = Source.fromFile(kbUriSetFile).getLines.map(uri => new DBpediaResource(uri)).toSet

        val drd = new DBpediaResourceDictionary()
        drd.deserialize(dictFile)
        val relFactory = new DBpediaRelationFactory(drd)
        CompressedAdjacencyList.load(compressedTriplesFile)


        val stats = new StatsCommonVsNoCommonCompressed(relFactory, kb)

        //val allCandidatesOut = new PrintWriter(new File(baseDir+"/candidates.tsv")) // write to the other
        val out = new PrintWriter(new File(paragraphsFile.getAbsolutePath + ".kb.log")) // write to the other


//        val uriSubSet = Source.fromFile(baseDir+"/candidates.set").getLines.toSet
//        LOG.info("Accepting a subset of %s URIs in the KB.")
//        def constrain(triple: Array[Node]) : Boolean = {
//            //println(new DBpediaResource(triple(0).toString).uri,new DBpediaResource(triple(2).toString).uri)
//            //println(uriSubSet.contains(new DBpediaResource(triple(0).toString).uri),uriSubSet.contains(new DBpediaResource(triple(2).toString).uri))
//
//            CompressedAdjacencyList.isObjectProperty(triple) &&
//            (uriSubSet.contains(new DBpediaResource(triple(0).toString).uri) || uriSubSet.contains(new DBpediaResource(triple(2).toString).uri))
//        }

        val occurrences = FileOccurrenceSource.fromFile(paragraphsFile)

        var j = 0;
        occurrences
            //.filter(o => o.resource.uri == "NIL")
            .foreach( annotatedOccurrence => {
            val correct = annotatedOccurrence.resource.uri
            // This is the occurrence we are inspecting
            try {
                val candidates = disambiguator match {
                    case p: ParagraphDisambiguator => stats.getCandidatesP(annotatedOccurrence, p);
                    case d: Disambiguator => stats.getCandidatesD(annotatedOccurrence, d);
                }
                //allCandidatesOut.println(candidates.map( c => c.resource.uri ).mkString("\n"))

                // A detector of common words could add a NIL as early as spotting\
                // we could add a NIL here.

                // If this was an annotation evaluation, we'd have already all candidates in a paragraph
                // But since we're looking at one spot at a time, we have to go ahead and find all the candidates in the paragraph
                val otherCandidates = disambiguator match {
                    case p: ParagraphDisambiguator => stats.getOtherCandidatesP(annotatedOccurrence, factory.spotter(), p)
                    case d: Disambiguator => stats.getOtherCandidatesD(annotatedOccurrence, factory.spotter(), d)
                }

                // Get a slice of the KB that contains the neighborhood of the other candidates
                val kbSlice0hop = stats.getKBSlice0Hop(otherCandidates)
                val kbSlice1hop = stats.getKBSlice1Hop(otherCandidates)

                // keep only candidates that have KB support
                val restricted = candidates.filter( candidate => {
                    val (support0hop, neighborhoodSize0hop) = stats.getSupport(candidate.resource.uri, kbSlice0hop)
                    //val (support1hop, neighborhoodSize1hop) = getSupport(candidate.resource.uri, kbSlice1hop)
                    support0hop > 0
                });

                val chosenSupport0hop = if (candidates.size>0) stats.getSupport(candidates.head.resource.uri, kbSlice0hop) else 0
                val chosenSupport1hop = if (candidates.size>0) stats.getSupport(candidates.head.resource.uri, kbSlice1hop) else 0

                val chosenWithoutKB = if (candidates.size>0) candidates.head.resource.uri else "NIL"
                val chosenWithKB = if (restricted.size>0) restricted.head.resource.uri else "NIL"
                val chosenWithTACKBP = if (restricted.size>0) stats.filter(restricted.head.resource).uri else "NIL"

                println("Chosen without KB: ", chosenWithoutKB)
                println("Chosen with KB:    ", chosenWithKB, chosenSupport0hop, chosenSupport1hop)
                println("Chosen with TACKBP:    ", chosenWithTACKBP)
                println("Correct:           ", correct)

                out.println("%s\t%s\t%s\t%s\t%s\t%s\t%s".format(correct, chosenWithoutKB, chosenWithKB, chosenWithTACKBP, chosenSupport0hop, chosenSupport1hop, otherCandidates.size))

                //allCandidatesOut.flush()
                out.flush()
            } catch {
                case e:Exception => {
                    out.println("%s\t%s\t%s\t%s\t%s\t%s\t%s".format(correct, "NIL", "NIL", "NIL", 0, 0, 0))
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
