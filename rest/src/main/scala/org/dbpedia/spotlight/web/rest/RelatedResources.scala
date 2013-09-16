package org.dbpedia.spotlight.web.rest

/*
 * Copyright 2012 DBpedia Spotlight Development Team
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

import org.dbpedia.spotlight.lucene.LuceneManager.DBpediaResourceField
import org.apache.lucene.search.similar.MoreLikeThis

import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.lucene.LuceneManager
import org.dbpedia.spotlight.model.DBpediaResource
import org.dbpedia.spotlight.exceptions.{SearchException, TimeoutException}
import org.apache.lucene.search._
import org.apache.lucene.index.{Term, IndexReader}
import java.io.{IOException, File}
import org.apache.lucene.document.{MapFieldSelector, FieldSelector, Document}
import org.apache.lucene.analysis.snowball.SnowballAnalyzer
import org.apache.lucene.util.Version
import java.net.URI
import com.sun.grizzly.http.SelectorThread
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory
import related.Related

import org.dbpedia.spotlight.sparql.SparqlQueryExecuter
import scala.collection.JavaConverters._
import collection.mutable.{Map, HashMap}
import collection.Set

import net.liftweb.json._
import net.liftweb.json.JsonDSL._


/**
 * 
 * @author pablomendes
 */
class RelatedResources {

    SpotlightLog.info(this.getClass, "Initializing objects ...")
    val directory = LuceneManager.pickDirectory(new File("/data/spotlight/release-0.6/index-0.6-withTypes-withSF/"))
    val reader = IndexReader.open(directory, true)
    val searcher = new IndexSearcher(reader)

    val sparqler = new SparqlQueryExecuter("http://dbpedia.org/links","http://spotlight.dbpedia.org/sparql/")
    /**
     * Generic search method to access the lucene index
     * @param query
     * @param n
     * @return
     * @throws SearchException
     */
    def getHits(query: Query, n: Int, timeout: Int, filter: Filter): Array[ScoreDoc] = {
        var hits: Array[ScoreDoc] = null
        try {
            var start: Long = System.nanoTime
            var collector: TopScoreDocCollector = TopScoreDocCollector.create(n, false)
            searcher.search(query, filter, collector)
            hits = collector.topDocs.scoreDocs
            var end: Long = System.nanoTime
            SpotlightLog.debug(this.getClass, "Done search in %f ms. hits.length=%d", (end - start) / 1000000.0, hits.length)
        }
        catch {
            case timedOutException: TimeLimitingCollector.TimeExceededException => {
                throw new TimeoutException("Timeout (>" + timeout + "ms searching for surface form " + query.toString, timedOutException)
            }
            case e: Exception => {
                throw new SearchException("Error searching for surface form " + query.toString, e)
            }
        }
        SpotlightLog.debug(this.getClass, "Doc ids: %s", hits.map(d => d.doc.intValue() ))
        return hits
    }

    def getQuery(resource: DBpediaResource): Query = {
        return new TermQuery(new Term(DBpediaResourceField.URI.toString, resource.uri))
    }

    def getDocument(docNo: Int, selector: FieldSelector): Document = {
        var document: Document = null
        try {
            document = reader.document(docNo, selector)
        }
        catch {
            case e: IOException => {
                throw new SearchException("Error reading document " + docNo, e)
            }
        }
       // SpotlightLog.debug(this.getClass, "Document: %s", document)
        return document
    }

    def getScoredUris(query: Query, nHits: Int) = {
        val field = DBpediaResourceField.URI
        val hits = getHits(query,nHits,50000,null).toList
        val scoredUris = new HashMap[String,Float]()
        val fields : java.util.List[String] = List(field.toString).asJava
        hits.foreach(d => {
            val doc = getDocument(d.doc,new MapFieldSelector(fields))
            val score = d.score
            val uri = doc.getValues(field.toString).head //there should be only one URI
            scoredUris.put(uri,score)
        })
        SpotlightLog.debug(this.getClass, "Values: %s", scoredUris)
        scoredUris
    }

    def search(resourceUri: String, nHits: Int) : scala.collection.mutable.Map[String,Float] = {
        SpotlightLog.debug(this.getClass, "Setting up lucene search query.")

        val mlt = new MoreLikeThis(reader)
        mlt.setFieldNames(Array(DBpediaResourceField.CONTEXT.toString))
        mlt.setAnalyzer(new SnowballAnalyzer(Version.LUCENE_36,"English"))

        val hits = getHits(getQuery(new DBpediaResource(resourceUri)),5,50000,null) //find the document for this URI

        val scoredUris = hits.headOption match {
            case Some(scoredDoc) => getScoredUris(mlt.like(scoredDoc.doc),nHits)
            case _ => new HashMap[String,Float]()
        }

        scoredUris
    }

    def search(resources: Set[String], nHits: Int) : Map[String,Float] = {
        val relatedSets : List[Map[String,Float]] = resources.toList.map( resourceUri => search(resourceUri,nHits) )

        relatedSets.foldLeft(new HashMap[String,Float]())((acc, m) =>
            acc ++ m.map{ case (k,v) => k -> (v + acc.getOrElse(k,0.0F)) }
        )
        // sum up the results in different maps into one
        //relatedSets.map( t => t)
    }

    def sparql(resources: Set[String]) : scala.collection.immutable.Map[String,Float] = {
        SpotlightLog.debug(this.getClass, "Setting up sparql query.")

        val header = "select * where { "
        val unionClause = "{ <http://dbpedia.org/resource/%s> <http://dbpedia.org/ontology/wikiPageWikiLink> ?entity . } ";
        val footer = "}"
        val body = resources.map(r => unionClause.format(r)).mkString(" union ")
        val sparqlQuery = header + body + footer
        val relatedResources = sparqler.query(sparqlQuery).asScala.toList.map( r => (r.uri -> 1.0F))
        // sort by uri and sum up number of occurrences
        val relatedResourcesCount = relatedResources.groupBy(_._1) map { case (k,v) => k -> (v map (_._2) sum) }
        relatedResourcesCount
    }

    def query(resources: Set[String], nHits: Int) : List[(String,Float)] = {
        SpotlightLog.info(this.getClass, "Searched: %s", resources)
        /* get resources whose context in DBpedia is similar to these */
        val similar = search(resources, nHits) //println(similar)
        /* get resources linked to these via DBpedia wikilinks */
        //val related = sparql(resources) // println(related)
        /* merge lucene search and sparql query results */
        //val merged = similar ++ related.map{ case (k,v) => k -> (v + similar.getOrElse(k,0.0F)) }
        val merged=similar
        // sort by value
        val sorted = merged.toList.sortWith{_._2 < _._2}.reverse.take(nHits)
        SpotlightLog.info(this.getClass, "Retrieved: %s", sorted)
        sorted
    }

    def queryAsJson(resources: java.util.Set[String], nHits: Int=100) : String = {
        val values = query(resources.asScala,nHits)
        compact(render(values))
    }

    def json(values: Array[String]) : String = {
        import net.liftweb.json._
        import net.liftweb.json.JsonDSL._
        compact(render(values.toSeq))
    }
}

object RelatedResources {

    def main(args: Array[String]) {
        val serverURI: URI = new URI(Related.uri)
        val initParams: java.util.Map[String, String] = new java.util.HashMap[String, String]
        initParams.put("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig")
        initParams.put("com.sun.jersey.config.property.packages", "org.dbpedia.spotlight.web.rest.related")

        var threadSelector: SelectorThread = GrizzlyWebContainerFactory.create(serverURI, initParams)
        threadSelector.start
        System.err.println("Server started in " + System.getProperty("user.dir") + " listening on " + serverURI)


        val resources = Set("Cloud_computing","Computing_platform","Scala_%28programming_language%29")
        val q = new RelatedResources
        q.query(resources,20).foreach(println(_))
    }
}
