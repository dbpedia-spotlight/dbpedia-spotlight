/*
 * Copyright 2011 Pablo Mendes, Max Jakob
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
 */

package org.dbpedia.spotlight.graph

import org.semanticweb.yars.nx.Node
import org.apache.commons.collections15.Transformer
import scala.collection.JavaConversions._
import java.io._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model.DBpediaResource
import org.dbpedia.spotlight.util.Profiling._
import edu.uci.ics.jung.algorithms.scoring.KStepMarkov
import edu.uci.ics.jung.graph.Graph
import sjson.json.{JsonSerialization, DefaultProtocol, Serializer}
import JsonSerialization._
import DefaultProtocol._
import io.Source

//class Cohesion {
//    def walk(graph: Nothing): Unit = {
//        var alpha: Float = 0.15f
//        var clusterer: WeakComponentClusterer[DBpediaResource, DBpediaRelation] = new WeakComponentClusterer[DBpediaResource, DBpediaRelation]
//        var components: Set[Set[DBpediaResource]] = clusterer.transform(graph)
//        var numComponents: Int = components.size
//        System.out.println("Number of components: " + numComponents)
//        System.out.println("Number of edges: " + graph.getEdgeCount)
//        System.out.println("Number of nodes: " + graph.getVertexCount)
//        System.out.println("Random jump factor: " + alpha)
//        var ranker = new PageRank(graph, alpha)
//
//        /**
//         * PageRankWithPriors ranker = new PageRankWithPriors(someGraph,0.3,1,rootSet,null);
//            ranker.evaluate();
//            ranker.printRankings();
//         */
//
//        ranker.evaluate
//        ranker.printRankings(true, true);
//    }
//}


object CohesionInt {

    val LOG = LogFactory.getLog(this.getClass)

    def getStartingPoints(g: Graph[CompressedDBpediaResource,CompressedDBpediaRelation], rootEntity: CompressedDBpediaResource) = {
        // This function will eventually contain contextual scores of entities
        object StartingPoints extends Transformer[CompressedDBpediaResource,java.lang.Double]  {
            var rootEntity : CompressedDBpediaResource = null;
            def apply(r: CompressedDBpediaResource) = {
                rootEntity = r;
                this
            }
            @Override
            def transform(resource: CompressedDBpediaResource) : java.lang.Double = {
                if (rootEntity == null) {
                    println(" null root set ")
                    0.0
                } else if (rootEntity.id equals resource.id) {
                    1//0.85
                } else
                    0// 0.15 / (g.getVertexCount-1)
            }
        }
        StartingPoints(rootEntity)
    }


    def getBerlinRootSet(g: Graph[CompressedDBpediaResource,CompressedDBpediaRelation], factory: DBpediaRelationFactory) = {
        LOG.info("Building root set.")
        val r1 = factory.DBpediaResource.fromName("Berlin").get
        var rootSet = List[CompressedDBpediaResource]()
        if (g.containsVertex(r1))
            rootSet = r1 :: rootSet
        rootSet
    }

    def main(args: Array[String]) {

        val base = "page_links_en";
        val ntFile = new File("/home/pablo/data/dbpedia/"+base+".nt")
        val mapFile = new File("/data/dbpedia/en/page_links_en.nt.redirectsResolved.cut.sorted.uniq.compressed")
        val gFile = new File(ntFile.getPath+".Graph");

        val rootSetFile = new File("/home/pablo/eval/csaw/gold/candidates.set.compressed")
        val outputFile = new File(gFile+".PageRank")

        val alpha = 0.85

        LOG.info("Writing results to %s".format(outputFile.getPath))
        val out = new PrintWriter(outputFile)

        val jungSerializer = new JungSerializerUtil

        // Make sure we have parsed and serialized the file already
        if (!gFile.exists)
            jungSerializer.serialize(jungSerializer.parseMap(mapFile), gFile)

        // If the file has been serialized, new we just need to load it.
        val g = jungSerializer.deserialize(gFile)

        //val rootSet = getBerlinRootSet(g, factory);
        val rootSet = Source.fromFile(rootSetFile).getLines.map( id => new CompressedDBpediaResource(id.toInt)).toList

        val serializer = Serializer.SJSON

        val nEntities = 0
        rootSet.foreach( e => {

            LOG.info("Generating starting points (with priors).")
            val nodePriors = getStartingPoints(g, e)
            LOG.info("Creating ranker.")
            val ranker = new KStepMarkov(g,nodePriors,4)
            //val ranker = new PageRank(g, alpha)
            //val ranker = new MarkovCentrality[DBpediaResource,DBpediaRelation](g, rootSet.toSet.asJava)
            //val ranker = new PageRankWithPriors[DBpediaResource,DBpediaRelation](g, nodePriors, alpha);

            ranker.acceptDisconnectedGraph(true);
            //ranker.setMaxIterations(3) //30

            LOG.info("Starting walk from "+e.id)
            timed(printTime("Walk took ")){
                ranker.evaluate();
            }

            //graph.getVertices.foldLeft( PriorityQueue() ) {
            //    (queue, v) => v :: queue
            //}.each(_.dequeue)

            LOG.info("Getting ranked list of nodes.")

            val cutoff = 0.0
            //val cutoff = (1 / g.getVertexCount)

            // get scores for all non-zero nodes
            val pageRankedNodes = timed(printTime("Sorting took ")){
                // get scores just for the ones we started with
                //val scoredNodeList = rootSet.map(v => (v, ranker.getVertexScore(v).doubleValue) )

                g.getVertices.map(v => (v.id,ranker.getVertexScore(v).doubleValue)).toList
                    .filter(_._2 > cutoff).sortBy(_._2)
                    .take(1000)
                    .foldLeft(Map[Int,Double]())( (acc,t) => acc + (t._1 -> scala.math.log(t._2)) )
            }

            LOG.info("Printing non-zero nodes.");
            out.println(tojson(Map(e.id -> pageRankedNodes)))

            LOG.info("Done. (entity %d)".format(e.id))
        })

        // use priority queue to sort vertices by PageRank values
        //		PriorityQueue<Ranking<UMLSNode>> q = new PriorityQueue<Ranking<UMLSNode>>();
        //		int i = 0;
        //		for (UMLSNode pmid : graph.getVertices()) {
        //			q.add(new Ranking<UMLSNode>(i++, ranker.getVertexScore(pmid), pmid));
        //		}
        //        println("\nPageRank of nodes, in descending order:")

        //		while ((r = q.poll()) != null) {
        //			println(r.rankScore + "\t" + r.getRanked());
        //		}



    }

}

