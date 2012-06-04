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

import org.junit.Test
import org.junit.Assert._
import org.dbpedia.spotlight.io.TripleSource
import it.unimi.dsi.fastutil.io.BinIO
import java.util.zip.GZIPInputStream
import org.dbpedia.spotlight.exceptions.IndexException
import collection.mutable.{HashSet, HashMap}
import org.semanticweb.yars.nx.Node
import java.io._
import scalaj.collection.Imports._
import scalaj.collection.s2j.SeqWrapper

/**
 * Partial tests for AdjacencyList
 * @author pablomendes
 */

class AdjacencyListTests {

    val triplesString = "<http://dbpedia.org/resource/Berlin> <http://dbpedia.org/ontology/leader> <http://dbpedia.org/resource/Klaus_Wowereit> .\n" +
        "<http://dbpedia.org/resource/Berlin> <http://dbpedia.org/ontology/leaderTitle> \"Governing Mayor\"@en .\n" +
        "<http://dbpedia.org/resource/Berlin> <http://dbpedia.org/ontology/populationMetro> \"5000000\"^^<http://www.w3.org/2001/XMLSchema#nonNegativeInteger> .\n" +
        "<http://dbpedia.org/resource/Berlin> <http://dbpedia.org/ontology/areaCode> \"030\"@en .\n" +
        "<http://dbpedia.org/resource/Berlin> <http://dbpedia.org/ontology/leaderParty> <http://dbpedia.org/resource/Die_Linke> .\n" +
        "<http://dbpedia.org/resource/Berlin> <http://dbpedia.org/ontology/populationTotal> \"3439100\"^^<http://www.w3.org/2001/XMLSchema#nonNegativeInteger> .\n" +
        "<http://dbpedia.org/resource/Berlin> <http://dbpedia.org/ontology/areaTotal> \"8.9182E8\"^^<http://www.w3.org/2001/XMLSchema#double> .\n" +
        "<http://dbpedia.org/resource/Berlin> <http://dbpedia.org/ontology/elevation> \"34.0\"^^<http://www.w3.org/2001/XMLSchema#double> .\n" +
        "<http://dbpedia.org/resource/Berlin> <http://dbpedia.org/ontology/populationAsOf> \"2009-09-30\"^^<http://www.w3.org/2001/XMLSchema#date> .\n" +
        "<http://dbpedia.org/resource/Berlin> <http://dbpedia.org/ontology/postalCode> \"10001\\u201314199\"@en .\n" +
        "<http://dbpedia.org/resource/Berlin> <http://www.georss.org/georss/point> \"52.50055555555556 13.398888888888889\"@en .\n" +
        "<http://dbpedia.org/resource/Berlin> <http://www.w3.org/2003/01/geo/wgs84_pos#long> \"13.398888888888889\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
        "<http://dbpedia.org/resource/Berlin> <http://www.w3.org/2003/01/geo/wgs84_pos#lat> \"52.50055555555556\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
        "<http://dbpedia.org/resource/Berlin> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.opengis.net/gml/_Feature> ."

    @Test
    def parseTest() {
        val s = TripleSource.fromString(triplesString)
        assertNotNull(s)
        val size = triplesString.split("\n").size
        assertEquals(size, s.toList.size);
    }

    @Test
    def filterTest() {
        val s = TripleSource.fromString(triplesString).filter(TripleSource.isObjectProperty)
        assertEquals(3, s.toList.size);
    }

    @Test
    def serializeScalaTest() {
        val s = TripleSource.fromString(triplesString).filter(TripleSource.isObjectProperty).toList
        val file = new File("/tmp/test1.serialization")
        val o = new ObjectOutputStream(new FileOutputStream(file))
        o.writeObject(s)
        o.close()
        var input : InputStream = if (file.getName().endsWith(".gz")) new GZIPInputStream(new FileInputStream(file)) else new FileInputStream(file);
        val i = new ObjectInputStream(input)
        val obj = i.readObject()
        obj match {
                case map : List[Node] => {
                    println("Loaded adjacency matrix" );// with "+ map.neighbors.keys.size+" keys.")
                    return map
                };
                case o : Object => throw new ClassCastException("Error loading serialized object. Expected Array[Node], found: " + o.getClass)
            }
    }

    @Test
    def serializeBinIOTest() {
        val s = TripleSource.fromString(triplesString).filter(TripleSource.isObjectProperty).toList.asJava
        val file = new File("/tmp/test2.serialization")
        BinIO.storeObject(s, file);
        var input : InputStream = if (file.getName().endsWith(".gz")) new GZIPInputStream(new FileInputStream(file)) else new FileInputStream(file);
        val obj = BinIO.loadObject(input)
        val a = obj match {
                case map : SeqWrapper[Node] => {
                    println("Loaded adjacency matrix" );// with "+ map.neighbors.keys.size+" keys.")
                    map.toArray
                };
                case o : Object => throw new ClassCastException("Error loading serialized object. Expected Array[Node], found: " + o.getClass)
            }

        assertEquals(3, a.size);
    }

    @Test
    def serializeMapOfSets() {
        val neighbors = AdjacencyList.parse(triplesString)
        val file = new File("/tmp/test3.serialization")
        BinIO.storeObject(neighbors, file)
        var input : InputStream = if (file.getName().endsWith(".gz")) new GZIPInputStream(new FileInputStream(file)) else new FileInputStream(file);
        val obj = BinIO.loadObject(input)
        val a = obj match {
                case map : HashMap[String,HashSet[String]] => {
                    println("Loaded adjacency matrix" );// with "+ map.neighbors.keys.size+" keys.")
                    map
                };
                case o : Object => throw new ClassCastException("Error loading serialized object. Expected HashMap[String,HashSet[String]], found: " + o.getClass)
            }
        assertEquals(1, a.size);
        println(a)
        assertEquals(4, a.getOrElse("Berlin", new HashSet[String]()).size);
    }

    @Test
    def readCompressed() {
        val drd = new DBpediaResourceDictionary()
        val uris = Set("http://www.opengis.net/gml/_Feature", "Berlin", "Klaus_Wowereit", "Die_Linke")
        drd.load(uris.iterator)
        val factory = new DBpediaRelationFactory(drd)
        println(CompressedAdjacencyList.parseFromUncompressed(TripleSource.fromString(triplesString).filter(AdjacencyList.isObjectProperty), factory))
    }
}