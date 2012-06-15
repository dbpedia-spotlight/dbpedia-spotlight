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
import org.semanticweb.yars.nx.Node
import org.semanticweb.yars.nx.parser.NxParser
import org.dbpedia.spotlight.model.{DBpediaRelation, DBpediaResource}
import edu.uci.ics.jung.graph.{Graph, DirectedSparseMultigraph, DirectedGraph}
import it.unimi.dsi.fastutil.io.BinIO
import java.util.zip.GZIPInputStream
import org.dbpedia.spotlight.util.Profiling._
import java.lang.Exception
import io.Source
import java.io.{PrintWriter, FileInputStream, InputStream, File}
import java.util.ArrayList
import javax.sound.sampled.Line

/**
 * 
 * @author pablomendes
 */

class JungSerializerUtil {

    val LOG = LogFactory.getLog(this.getClass)

    def parseNt(inputFile: File, f : Array[Node] => Boolean, factory: DBpediaRelationFactory ) : DirectedGraph[CompressedDBpediaResource, CompressedDBpediaRelation]  = {
        if (factory==null) throw new Exception("You need to initialize JungSerializerUtil with a DBpediaRelationFactory object.")

        val parser: NxParser = new NxParser(new FileInputStream(inputFile))
        val g: DirectedGraph[CompressedDBpediaResource, CompressedDBpediaRelation] = new DirectedSparseMultigraph[CompressedDBpediaResource, CompressedDBpediaRelation]

        var i = 0;
        while (parser.hasNext) {
            i = i + 1;
            val triple: Array[Node] = parser.next

            if (f(triple)) {
                val tSubject = factory.DBpediaResource.from(new DBpediaResource(triple(0).toString));
                val tObject = factory.DBpediaResource.from(new DBpediaResource(triple(2).toString));
                val tRelation = factory.DBpediaRelation.fromResources(tSubject, tObject);
                g.addEdge(tRelation, tSubject, tObject)
            }


            //println(resource,property,category);
            if (i % 10000 == 0) println(String.format("processed %s triples",i.toString))
            if (i % 4000000 == 0) {
                serialize(g, new File(inputFile.getPath+"."+i));
            }
        }
        g
    }

    def parseMap(inputFile: File) : DirectedGraph[CompressedDBpediaResource, CompressedDBpediaRelation]  = {

        val g: DirectedGraph[CompressedDBpediaResource, CompressedDBpediaRelation] = new DirectedSparseMultigraph[CompressedDBpediaResource, CompressedDBpediaRelation]

        var i = 0;
        Source.fromFile(inputFile).getLines().filterNot(line => line.trim() == "").foreach( line => {
            i = i + 1;

            val fields = line.split("\\t");
            if (fields.length==2) {
                val tSubject = new CompressedDBpediaResource(fields(0).toInt);
                fields(1).split(" ").foreach( id => {
                    val tObject = new CompressedDBpediaResource(id.toInt)
                    val tRelation = new CompressedDBpediaRelation(tSubject.id,tObject.id)
                    g.addEdge(tRelation, tSubject, tObject)
                });

            } else {
                LOG.error("Skipped line %d.".format(i))
            }

            if (i % 10000 == 0) println(String.format("processed %s triples",i.toString))
            if (i % 4000000 == 0) {
                serialize(g, new File(inputFile.getPath+"."+i));
            }
        })
        g
    }


    /**
     *
     * @param graph
     * @param outputFileName
     * @author PabloMendes
     */
    def serialize(graph: Graph[CompressedDBpediaResource, CompressedDBpediaRelation], outputFile: File) {
        LOG.info("Storing Graph to "+outputFile.getName);
        BinIO.storeObject(graph, outputFile);
        LOG.info("Done.");
    }

    /**
     * Load a Graph from a serialized object (e.g. umls_entities.Graph)
     * @param file
     * @return
     * @author PabloMendes
     */
    def deserialize(file: File)	: DirectedGraph[CompressedDBpediaResource, CompressedDBpediaRelation] = {
        LOG.info("Reading Graph object from file "+file);

        try {
            var input : InputStream = if (file.getName().endsWith(".gz")) new GZIPInputStream(new FileInputStream(file)) else new FileInputStream(file);

            val obj = timed(printTime("Loading took ")){
                BinIO.loadObject(input)
            }
            obj match {
                case graph : DirectedGraph[CompressedDBpediaResource, CompressedDBpediaRelation] => {
                    LOG.info("Loaded graph with "+
                        graph.getVertices().size()+" vertices and "+
                        graph.getEdgeCount()+" edges.")
                    return graph
                };
                case o : Object => throw new ClassCastException("Error loading serialized object. Expected Graph, found: " + o.getClass)
            }
        } catch {
            case e: Exception => throw new Exception("Error loading Graph from serialized object. ", e)
        }

    }

}