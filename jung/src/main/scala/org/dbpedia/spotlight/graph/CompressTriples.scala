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

import java.io.{PrintWriter, File}
import io.Source
import org.dbpedia.spotlight.model.DBpediaResource
import java.util.ArrayList
import scalaj.collection.Imports._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.lucene.LuceneManager.CaseSensitiveSurfaceForms

/**
 * Saves NT in
 * @author pablomendes
 */

object CompressTriples {

    val LOG = LogFactory.getLog(this.getClass)

    def compressCandidates(candidatesFile:File, factory: DBpediaRelationFactory) {
        val out = new PrintWriter(candidatesFile.getPath+".compressed");
        Source.fromFile(candidatesFile).getLines.map(uri => try {
            factory.DBpediaResource.from(new DBpediaResource(uri)).id
        } catch {
            case _ => LOG.error("Id not found for uri %s.".format(uri))
        }).foreach( id => out.println(id))
        out.close
    }

    def compressTriples(mapFile: File,factory: DBpediaRelationFactory) {
        val out = new PrintWriter(mapFile.getPath+".compressed");
        var i = 0;
        var lastSubject : CompressedDBpediaResource = null;
        var neighbors = new ArrayList[Int]()
        Source.fromFile(mapFile).getLines().foreach( line => {
            i = i + 1;

            val fields = line.split(" ");
            if (fields.length==2) {

                val tSubject = factory.DBpediaResource.from(new DBpediaResource(fields(0).toString));
                val tObject = factory.DBpediaResource.from(new DBpediaResource(fields(1).toString));

                if (lastSubject != null && tSubject != lastSubject) {
                    out.println(lastSubject.id+"\t"+neighbors.asScala.mkString(" ")+"\n")
                    neighbors = new ArrayList[Int]();
                }
                neighbors.add(tObject.id)
                lastSubject = tSubject
            } else {
                LOG.error("Skipped line %d.".format(i))
            }

            if (i % 10000 == 0) {
                println(String.format("processed %s triples",i.toString))
                out.flush()
            }
        })
        out.println(lastSubject.id+"\t"+neighbors.asScala.mkString(" ")+"\n")
        out.close()
    }

    def main(args: Array[String]) {
        //val ntFile = new File("/home/pablo/data/dbpedia/page_links_en.nt")
        val mapFile = new File("/data/dbpedia/en/page_links_en.nt.redirectsResolved.cut.sorted.uniq")
        val dictFile = new File("/home/pablo/data/dbpedia/page_links_en_uris.set.drd")
        val candidatesFile = new File("/home/pablo/eval/csaw/gold/candidates.set");
        val drd = new DBpediaResourceDictionary()
        //drd.load(dictFile)
        //drd.serialize(dictFile+".drd)
        drd.deserialize(dictFile)
        val factory = new DBpediaRelationFactory(drd)

        compressCandidates(candidatesFile,factory);

        compressTriples(mapFile,factory);
    }

}