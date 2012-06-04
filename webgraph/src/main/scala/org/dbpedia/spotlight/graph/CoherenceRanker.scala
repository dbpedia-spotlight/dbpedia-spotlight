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

package org.dbpedia.spotlight.graph

import java.io._
import org.apache.commons.logging.LogFactory
import io.Source
import it.unimi.dsi.law.rank.{PageRank, PageRankPowerMethod}
import it.unimi.dsi.fastutil.io.BinIO
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence
import com.officedepot.cdap2.collection.CompactHashMap
import it.unimi.dsi.fastutil.doubles.{DoubleList, DoubleArrayList}

class CoherenceRanker(val ntFile: File = new File("/home/pablo/data/dbpedia/page_links_en.nt"),
                      val mapFile: File = new File("/data/dbpedia/en/page_links_en.nt.redirectsResolved.cut.sorted.uniq.compressed"),
                      val dictFile: File = new File("/home/pablo/data/dbpedia/page_links_en_uris.set")) {

    val LOG = LogFactory.getLog(this.getClass)
    val gFile = new File(ntFile.getAbsolutePath+".WebGraph-underlying");

    /*
    Read a list of nodes (URIs) and their integer ids. Build bidirectional map
     */
    val nameToId = new CompactHashMap[String, Int]()
    val idToName = new CompactHashMap[Int, String]()

    LOG.debug("Counting nodes (and hopefully buffering file access)")
    var nNodes = Source.fromFile(dictFile).getLines().size

    val alpha = PageRank.DEFAULT_ALPHA
    val pageRankThreshold = PageRank.DEFAULT_THRESHOLD
    val pageRankMaxIterations = PageRank.DEFAULT_MAX_ITER

    var graph = loadGraph

    def getOutputFile(gFile: File, candidate: Int) = {
        new File(gFile+".PageRank%d".format(candidate))
    }

//    def save() {
//        BinIO.storeDoubles(pr.rank, outputFile.getAbsolutePath + ".ranks")
//    }

    def loadGraph = {
        val fullGraphFile = new File(gFile.getAbsolutePath+".graph")
        // Make sure we have parsed and serialized the file already
        val g = if (! fullGraphFile.exists) {
            LOG.info("Parsing graph. File %s does not exist. ".format(fullGraphFile.getAbsolutePath));
            WebGraphSerializerUtil.serialize(WebGraphSerializerUtil.parseMap(mapFile, nNodes), gFile)
        } else {
            LOG.info("Loading graph. File %s exists.".format(fullGraphFile.getAbsolutePath));
            WebGraphSerializerUtil.deserialize(gFile);
        }
        g
    }

    private def loadDict(dictFile: File, targetNames: Set[String]) {
        LOG.info("Loading node dictionary.")
        var id = 0;
        Source.fromFile(dictFile).getLines().foreach( name => {
            if (targetNames.contains(name)) {
                nameToId.put(name, id)
                idToName.put(id, name)
            }
            id = id + 1;
        })
        LOG.info("Done.")
        nNodes = id
    }

    /**
     * Run preferential page rank
     */
    private def pageRank(preferences: DoubleList) = {
        LOG.info("Running PageRank...")
        val pr = new PageRankPowerMethod(graph);
        pr.stronglyPreferential = true
        pr.preference = preferences
        // cycle until we reach maxIter interations or the norm is less than the given threshold (whichever comes first)
        pr.stepUntil(PageRank.or(new PageRank.NormDeltaStoppingCriterion(pageRankThreshold),
            new PageRank.IterationNumberStoppingCriterion(pageRankMaxIterations))) // PageRank.DEFAULT_MAX_ITER
        LOG.info("Done.")
        pr.rank
    }

    /**
     * @param candidates URI -> (support, contextualScore)
     */
    def getCoherence(candidates: Map[String,Tuple2[Int,Double]]) : Map[String,Tuple3[Int,Double,Double]] = {
        LOG.info("Initializing weights for PageRank...")

        // initialize node dictionary (could be in a database)
        loadDict(dictFile, candidates.keySet)

        // initialize prior scores for page rank
        var max = Double.MinValue
        var maxElement = 0
        var min = Double.MaxValue
        var minElement = 0
        var sum = 0.0
        var preferences = DoubleArrayList.wrap(Array.fill(nNodes){ 0.0 })
        candidates.foreach( c =>  {
            val candidateName = c._1
            val similarityScore = c._2._2

            sum = sum + similarityScore

            nameToId.get(candidateName) match {
                case Some(candidateId) => {
                    if (similarityScore > 0 ) {
                        if (similarityScore > max) {
                            max = similarityScore
                            maxElement = candidateId
                        } else if (similarityScore < min) {
                            min = similarityScore
                            minElement = candidateId
                        }
                        preferences.set(candidateId, similarityScore)
                        LOG.debug("Nonzero: %s, %s, %s".format(candidateId, candidateName, similarityScore));
                    }
                } //final score //TODO test with negative simScore too?
                case None => LOG.debug("Found a candidate that has no page links: %s".format(candidateName));
            }
        })
        LOG.info("Done.")
        LOG.debug("Total: %s".format(CoherenceRanker.getTotal(preferences)))

        if (max <= 0.0) // return all zeroes
            return candidates.foldLeft(Map[String,Tuple3[Int,Double,Double]]())( (acc,c) => acc + (c._1 -> (c._2._1, c._2._2, 0.0)) )

        // make it a stochastic vector
        for (val i <- 0 until preferences.size()) {
            preferences.set(i, (preferences.getDouble(i) / sum) )
        }
        val remainder = 1 - CoherenceRanker.getNormL1(preferences)
        if (remainder > 0)
            preferences.set(maxElement,preferences.get(maxElement)+remainder)
        else
            preferences.set(minElement,preferences.get(minElement)-remainder)
        LOG.debug("NormL1: %s ".format(CoherenceRanker.getNormL1(preferences)))

        // compute scores after the coherence walk
        val rank = pageRank(preferences)

        // add new scores alongside old and return
        val result = candidates.foldLeft(Map[String,Tuple3[Int,Double,Double]]())( (acc,c) => {
            val candidateName = c._1
            val oldScores = c._2
            val weight = nameToId.get(candidateName) match {
                case Some(id) => {
                    rank(id)
                }
                case None => 0.0
            }
            acc + (candidateName -> (oldScores._1, oldScores._2, weight))
        })
        result
    }


}

object CoherenceRanker {
    def getTotal(preferences: DoubleList) = {
        var total = 0.0
        for (val i <- 0 until preferences.size()) {
            total = total + preferences.get(i)
        }
        println("Total:"+total)
        total
    }
    def getNormL1(v: DoubleList) = {
        var normL1 = 0.0
        var c = 0.0
        var t = 0.0
        var y = 0.0
        for (i <- 0 until v.size()) {
            if (v.getDouble(i) < 0) {
                println("ERROR...")
            }
            y = v.getDouble( i ) - c;
            t = ( normL1 + y );
            c = ( t - normL1 ) - y;
            normL1 = t;
        }
        println("NormL1:"+normL1)
        normL1
    }
    def main(args: Array[String]) {

//        val ntFile = new File("/home/pablo/data/dbpedia/page_links_en.nt")
//        val mapFile = new File("/data/dbpedia/en/page_links_en.nt.redirectsResolved.cut.sorted.uniq.compressed")
//        val dictFile = new File("/home/pablo/data/dbpedia/page_links_en_uris.set")
//
//        val ranker = new CoherenceRanker(ntFile, mapFile, dictFile);
        val candidates = Map(0 -> (100, 0.9),
                             1 -> (100, 0.8),
                             2 -> (100, 0.8),
                             3 -> (100, 0.8))
//        println(ranker.getCoherence(candidates))


        var preferences = DoubleArrayList.wrap(Array.fill(candidates.size){ 0.0 })

        // initialize prior scores for page rank
        var max = Double.MinValue
        var maxElement = -1
        var min = Double.MaxValue
        var minElement = -1
        var sum = 0.0
        candidates.foreach( c =>  {
            val similarityScore = c._2._2

            if (similarityScore > max) {
                max = similarityScore
                maxElement = c._1
            } else if (similarityScore < min) {
                min = similarityScore
                minElement = c._1
            }
            sum = sum + similarityScore
            preferences.set(c._1, similarityScore)
        })

        println("Original preferences: "+preferences)

        // make it a stochastic vector
        for (val i <- 0 until preferences.size()) {
            preferences.set(i, (preferences.get(i) / (sum)) )
        }

        println("Normalized preferences: "+preferences)



        getTotal(preferences)



        val remainder = 1 - getNormL1(preferences)
        //val remainder = 1 - getTotal(preferences)

        if (remainder > 0) preferences.set(maxElement,preferences.get(maxElement)-remainder)
        else preferences.set(minElement,preferences.get(minElement)+remainder)

        getTotal(preferences)

        println(Math.abs(getNormL1(preferences) - 1.0) > 1E-6.toDouble)
    }
}