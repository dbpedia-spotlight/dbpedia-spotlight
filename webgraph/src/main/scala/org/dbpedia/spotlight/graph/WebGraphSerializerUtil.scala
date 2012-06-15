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

package org.dbpedia.spotlight.graph

import org.apache.commons.logging.LogFactory
import java.io.File
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph
import it.unimi.dsi.webgraph.{BVGraph, ArrayListMutableGraph}
import io.Source

/**
 *
 * @author pablomendes
 */

object WebGraphSerializerUtil {

    val LOG = LogFactory.getLog(this.getClass)

    def parseMap(inputFile: File, nNodes: Int) :ArrayListMutableGraph  = {

        val g: ArrayListMutableGraph = new ArrayListMutableGraph();
        g.addNodes(nNodes)

        var i = 0;
        Source.fromFile(inputFile).getLines().filterNot(line => line.trim() == "").foreach( line => {
            i = i + 1;

            val fields = line.split("\\t");
            if (fields.length==2) {
                val tSubject = fields(0).toInt;
                fields(1).split(" ").foreach( id => {
                    val tObject = id.toInt
                    g.addArc(tSubject, tObject)
                });

            } else {
                LOG.error("Skipped line %d.".format(i))
            }

            if (i % 10000 == 0) println(String.format("processed %s triples",i.toString))
            if (i % 4000000 == 0) {
             //   serialize(g, new File(inputFile.getPath+"."+i));
            }
        })
        g
    }

//    def parse(inputFile: File, f : Array[Node] => Boolean, dict: DBpediaResourceDictionary) : ArrayListMutableGraph  = {
//
//        val parser: NxParser = new NxParser(new FileInputStream(inputFile))
//
//        val g: ArrayListMutableGraph = new ArrayListMutableGraph();
//        g.addNodes(dict.getNumberOfEntries())
//
//        var i = 0;
//        while (parser.hasNext) {
//            i = i + 1;
//            val triple: Array[Node] = parser.next
//
//            if (f(triple)) {
//                val tSubject = new DBpediaResource(triple(0).toString);
//                val tObject = new DBpediaResource(triple(2).toString);
//                //val tRelation = new DBpediaRelation(triple(1).toString, tSubject, tObject);
//                g.addArc(dict.getId(tSubject), dict.getId(tObject))
//            }
//
//            //println(resource,property,category);
//            if (i % 10000 == 0) println(String.format("processed %s triples",i.toString))
//            //            if (i % 4000000 == 0) {
//            //                serialize(g, new File(inputFile.getPath+"."+i));
//            //            }
//        }
//        g
//    }

    /**
     *
     * @param graph
     * @param outputFileName
     * @author PabloMendes
     */
    def serialize(graph: ArrayListMutableGraph, outputFile: File) = {
        LOG.info("Storing Graph to "+outputFile.getName);
        val g = graph.immutableView()
        BVGraph.store(g, outputFile.getName + ArcLabelledImmutableGraph.UNDERLYINGGRAPH_SUFFIX)
        //BinIO.storeObject(graph, outputFile);
        LOG.info("Done.");
        g
    }

    def deserialize(inputFile: File) = {
        BVGraph.load(inputFile.getAbsolutePath)
    }

    /**
     * Load a Graph from a serialized object (e.g. umls_entities.Graph)
     * @param file
     * @return
     * @author PabloMendes
     */
//    def deserialize(file: File)	: DirectedGraph[DBpediaResource, DBpediaRelation] = {
//        LOG.info("Reading Graph object from file "+file);
//
//        try {
//            var input : InputStream = if (file.getName().endsWith(".gz")) new GZIPInputStream(new FileInputStream(file)) else new FileInputStream(file);
//
//            val obj = timed(printTime("Loading took ")){
//                BinIO.loadObject(input)
//            }
//            obj match {
//                case graph : DirectedGraph[DBpediaResource, DBpediaRelation] => {
//                    LOG.info("Loaded graph with "+
//                        graph.getVertices().size()+" vertices and "+
//                        graph.getEdgeCount()+" edges.")
//                    return graph
//                };
//                case o : Object => throw new ClassCastException("Error loading serialized object. Expected Graph, found: " + o.getClass)
//            }
//        } catch {
//            case e: Exception => throw new IndexException("Error loading Graph from serialized object. ", e)
//        }
//
//    }

}