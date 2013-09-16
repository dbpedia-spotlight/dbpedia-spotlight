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

package org.dbpedia.spotlight.model

import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Document
import org.dbpedia.spotlight.lucene.LuceneManager.DBpediaResourceField
import collection.JavaConversions._
import org.dbpedia.spotlight.lucene.search.{LuceneCandidateSearcher, BaseSearcher}
import org.dbpedia.spotlight.exceptions.{ItemNotFoundException, ConfigurationException}
import org.dbpedia.spotlight.log.SpotlightLog
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.dbpedia.spotlight.lucene.similarity.{CachedInvCandFreqSimilarity, JCSTermCache, InvCandFreqSimilarity}
import org.apache.lucene.misc.SweetSpotSimilarity
import org.apache.lucene.search.{DefaultSimilarity, ScoreDoc, Similarity}
import scala.collection.JavaConverters._
import org.dbpedia.spotlight.spot.{SpotSelector, AtLeastOneNounSelector, ShortSurfaceFormSelector}
import java.io.File
import org.dbpedia.extraction.util.WikiUtil

/**
 * Class containing methods to create model objects in many different ways
 *
 * @author pablomendes
 * @author Joachim Daiber (Tagger and Spotter methods)
 */
object Factory {
    /*
    * I like this style for the factory. group by return type and offer many .from* methods
    */
    object SurfaceFormOccurrence {
        def from(occ: DBpediaResourceOccurrence) : SurfaceFormOccurrence = {
            new SurfaceFormOccurrence(occ.surfaceForm, occ.context, occ.textOffset)
        }
    }

    object SurfaceForm {
        def fromString(name: String) = {
            new SurfaceForm(name.toString)
        }
        def fromDBpediaResourceURI(uri: String, lowerCased: Boolean): SurfaceForm = {
            // decode URI and truncate trailing parentheses
            val name = WikiUtil.wikiDecode(uri).replaceAll(""" \(.+?\)$""", "")
            fromString(if (lowerCased) name.toLowerCase else name)
        }
        def fromDBpediaResourceURI(resource: DBpediaResource, lowerCased: Boolean): SurfaceForm = {
            fromDBpediaResourceURI(resource.uri, lowerCased)
        }
        def fromWikiPageTitle(pageTitle: String, lowerCased: Boolean): SurfaceForm = {
            fromDBpediaResourceURI(pageTitle, lowerCased)
        }
    }

    object DBpediaResourceOccurrence {
        def from(sfOcc: SurfaceFormOccurrence, resource: DBpediaResource, score: Double) = {
            SpotlightLog.debug(this.getClass, "Factory test: support=%s, ctxScore=%s, sfOcc=%s", resource.support, score, sfOcc)
            new DBpediaResourceOccurrence("",  // there is no way to know this here
                resource,
                sfOcc.surfaceForm,
                sfOcc.context,
                sfOcc.textOffset,
                Provenance.Annotation,
                score,
                -1,         // there is no way to know percentage of second here
                score)      // to be set later
        }
        def from(sfOcc: SurfaceFormOccurrence, fullResource: DBpediaResource, score: Tuple2[Int,Double]) = {
            //TODO can take a mixture as param and use resource.score to mix with the other two scores in Tuple2
            new DBpediaResourceOccurrence("",  // there is no way to know this here
                fullResource, // support is also available from score._1, but types are only in fullResource
                sfOcc.surfaceForm,
                sfOcc.context,
                sfOcc.textOffset,
                Provenance.Annotation,
                score._2,
                -1,         // there is no way to know percentage of second here
                score._2)      // to be set later
        }
        //TODO take a mixture as param?
        def from(sfOcc: SurfaceFormOccurrence, resource: DBpediaResource, score: Tuple3[Int,Double,Double]) = {
            new DBpediaResourceOccurrence("",  // there is no way to know this here
                new DBpediaResource(resource.uri, score._1),
                sfOcc.surfaceForm,
                sfOcc.context,
                sfOcc.textOffset,
                Provenance.Annotation,
                score._2 * score._3,
                -1,         // there is no way to know percentage of second here
                score._2)   // to be set later
        }
        def from(sfOcc: SurfaceFormOccurrence, hit: ScoreDoc, contextSearcher: BaseSearcher) = {
            var resource: DBpediaResource = contextSearcher.getDBpediaResource(hit.doc)
            if (resource == null) throw new ItemNotFoundException("Could not choose a URI for " + sfOcc.surfaceForm)
            val percentageOfSecond = -1;
            new DBpediaResourceOccurrence("", resource, sfOcc.surfaceForm, sfOcc.context, sfOcc.textOffset, Provenance.Annotation, hit.score, percentageOfSecond, hit.score)
        }
    }

    //Workaround for Java:
    def ontologyType() = this.OntologyType
    object OntologyType {

        def fromURI(uri: String) : OntologyType = {
            if (uri.startsWith(DBpediaType.DBPEDIA_ONTOLOGY_PREFIX))
                return new DBpediaType(uri)

            if (uri.startsWith(FreebaseType.FREEBASE_RDF_PREFIX))
                return FreebaseType.fromTypeString(uri)

            if (uri.startsWith(SchemaOrgType.SCHEMAORG_PREFIX))
                return new SchemaOrgType(uri)

            if (uri.startsWith(OpenCycConcept.OPENCYCCONCEPT_PREFIX))
                return new OpenCycConcept(uri)

            new DBpediaType(uri)
        }

        def fromQName(ontologyType : String) : OntologyType = {
            val r = """^([A-Za-z]*):(.*)""".r
            try {
                val r(prefix, suffix) = ontologyType

                suffix.toLowerCase match {
                    case "unknown" => DBpediaType.UNKNOWN
                    case _ => {     
                        prefix.toLowerCase match {
                            case "d" | "dbpedia"  => new DBpediaType(suffix)
                            case "f" | "freebase" => FreebaseType.fromTypeString(suffix)
                            case "s" | "schema" => new SchemaOrgType(suffix)
                            case "o" | "opencyc" => new OpenCycConcept(suffix)
                            case _ => new DBpediaType(ontologyType)
                        }
                    }
                }
                
            }catch{
                //The default type for non-prefixed type strings:
                case e: scala.MatchError => new DBpediaType(ontologyType)
            }
        }

        def fromCSVString(ontologyTypesString: String) : List[OntologyType] = {
//            val ontologyTypes: java.util.List[OntologyType] = new java.util.ArrayList[OntologyType]
//            val types: Array[String] = ontologyTypesString.trim.split(",")
//            for (t <- types) {
//                if (!(t.trim == "")) ontologyTypes.add(Factory.ontologyType.fromQName(t.trim))
//            }
//            ontologyTypes
            val types = ontologyTypesString.split(",").filterNot(t => t.trim.isEmpty)
            if (types.size>0) types.map(Factory.ontologyType.fromQName).toList else List[OntologyType]()
        }
    }

    def paragraph() = Paragraph
    object Paragraph {
        def from(a: AnnotatedParagraph) = {
            new Paragraph(a.id,a.text,a.occurrences.map( dro => Factory.SurfaceFormOccurrence.from(dro)))
        }
        def fromJ(occs: java.util.List[SurfaceFormOccurrence]) = {
            from(occs.asScala.toList)
        }
        def from(occs: List[SurfaceFormOccurrence]) = {
            if (occs.size==0) throw new IllegalArgumentException("Cannot create paragraph out of empty occurrence list.")
            val first = occs.head
            new Paragraph("",first.context,occs.sortBy(o => o.textOffset))
        }
    }

    def analyzer() = this.Analyzer //TODO for compatibility with java
    object Analyzer {

      def defaultAnalyzer(stopWords: java.util.Set[String]) :Analyzer = {
        SpotlightLog.warn(this.getClass, "Using Lucene StandardAnalyzer/ Version.LUCENE_36 by default...")
        new StandardAnalyzer(Version.LUCENE_36,stopWords)
      }

      def from(analyzerName : String, luceneVersion: String, stopWords: java.util.Set[String]) : Analyzer = {

        try{
          val analyzerClass = Class.forName(analyzerName)
          val version = Version.valueOf(luceneVersion)
          val instanceAnalyzerClass = if(stopWords != null) analyzerClass.getConstructor(classOf[Version],classOf[java.util.Set[String]]).newInstance(version, stopWords) else
                                                            analyzerClass.getConstructor(classOf[Version]).newInstance(version)
          instanceAnalyzerClass match {
            case instanceAnalyzerClass: Analyzer => instanceAnalyzerClass
            case _ => throw new ClassCastException
          }
        }
        catch {

          case cnfe: ClassNotFoundException => {
            SpotlightLog.error(this.getClass, "I can't find a class with the name %s or lucene version %s", analyzerName, luceneVersion)
            SpotlightLog.error(this.getClass, "Try to use in org.dbpedia.spotlight.lucene.analyzer property a complete name, such :")
            SpotlightLog.error(this.getClass, " - org.apache.lucene.analysis.de.GermanAnalyzer for German;")
            SpotlightLog.error(this.getClass, " - org.apache.lucene.analysis.fr.FrenchAnalyzer for French;")
            SpotlightLog.error(this.getClass, " - org.apache.lucene.analysis.br.BrazilianAnalyzer for Brazilian Portuguese;")
            SpotlightLog.error(this.getClass, "etc...")
            SpotlightLog.error(this.getClass, "For org.dbpedia.spotlight.lucene.version property, try to use such:")
            SpotlightLog.error(this.getClass, " - LUCENE_36 - Lucene Version 3.6")

            defaultAnalyzer(stopWords)

          }

          case cce: ClassCastException => {
            SpotlightLog.error(this.getClass, "I found a class with the name %s, but this class isn't an Analyzer class.", analyzerName)
            SpotlightLog.error(this.getClass, "Please check org.dbpedia.spotlight.lucene.analyzer and org.dbpedia.spotlight.lucene.version " +
              "properties in your server.properties file.")
            defaultAnalyzer(stopWords)

          }
          case e: Exception => {
            SpotlightLog.error(this.getClass, "Something went wrong. Please check your server.properties file.")
            SpotlightLog.error(this.getClass, "If the problem persists, please send the stacktrace below to the dev team.")
            SpotlightLog.error(this.getClass, "[BOF]*************************Stacktrace error:*************************")
            e.printStackTrace()
            SpotlightLog.error(this.getClass, "[EOF]*************************Stacktrace error:*************************")
            defaultAnalyzer(stopWords)

          }

        }
      }
    }

    object Similarity {
        def fromName(similarityName : String) : Similarity = {
            (new InvCandFreqSimilarity :: new SweetSpotSimilarity :: new DefaultSimilarity :: Nil)
                .map(sim => (sim.getClass.getSimpleName, sim))
                .toMap
                .get(similarityName)
                .getOrElse(throw new ConfigurationException("Unknown Similarity: "+similarityName))
        }
        def fromConfig(configuration: SpotlightConfiguration, contextLuceneManager: LuceneManager) = {
            if (configuration.getDisambiguatorConfiguration.isContextIndexInMemory)
                new InvCandFreqSimilarity
            else
                new CachedInvCandFreqSimilarity(JCSTermCache.getInstance(contextLuceneManager, configuration.getMaxCacheSize))
        }
    }

    /*** left here for compatibility with Java. used by IndexEnricher ****/
    def createSurfaceFormFromDBpediaResourceURI(resource: DBpediaResource, lowercased: Boolean) = {
        SurfaceForm.fromDBpediaResourceURI(resource, lowercased)
    }

    /*** TODO old style factory methods that need to be changed ****/

    def createMergedDBpediaResourceOccurrenceFromDocument(doc : Document, id: Int, searcher: BaseSearcher) = {
        // getField: If multiple fields exists with this name, this method returns the first value added.
        var resource = searcher.getDBpediaResource(id);
        var context = new Text(doc.getFields(LuceneManager.DBpediaResourceField.CONTEXT.toString).map(f => f.stringValue).mkString("\n"))

        Array(new DBpediaResourceOccurrence( //TODO add document id as occurrence id
            resource,
            SurfaceForm.fromDBpediaResourceURI(resource, false), // this is sort of the "official" surface form, since it's the cleaned up title
            context,
            -1,
            Provenance.Wikipedia // Ideally grab this from index, if we have sources other than Wikipedia
            ))
    }

    def createDBpediaResourceOccurrencesFromDocument(doc : Document, id: Int, searcher: BaseSearcher) = {
        // getField: If multiple fields exists with this name, this method returns the first value added.
        var resource = searcher.getDBpediaResource(id);
        var occContexts = doc.getFields(LuceneManager.DBpediaResourceField.CONTEXT.toString).map(f => new Text(f.stringValue))

        occContexts.map(context =>
            new DBpediaResourceOccurrence( //TODO add document id as occurrence id
                resource,
                SurfaceForm.fromDBpediaResourceURI(resource, false), // this is sort of the "official" surface form, since it's the cleaned up title
                context,
                -1, //TODO find offset
                Provenance.Wikipedia // Ideally grab this from index, if we have sources other than Wikipedia
            ))
    }


    def setField(resource: DBpediaResource, field: DBpediaResourceField, document: Document) {
        field match {
            case DBpediaResourceField.URI_COUNT =>
                resource.setSupport(document.getField(field.name).stringValue.toInt)
            case DBpediaResourceField.TYPE =>
                resource.setTypes(document.getValues(field.name)
                    .filterNot(t => t.equalsIgnoreCase("http://www.w3.org/2002/07/owl#Thing"))
                    .map( t => OntologyType.fromQName(t) ).toList)
            case _ =>
        }
    }

    object SpotSelector {
        def fromNameList(commaSeparatedNames: String) : List[SpotSelector] = {
            commaSeparatedNames.split(",").flatMap(name => if (name.isEmpty) None else Some(fromName(name.trim))).toList
        }
        def fromName(name: String) : SpotSelector = { //TODO use reflection
            if (name.isEmpty) throw new IllegalArgumentException("You need to pass a SpotSelector name.")
            name match {
                case "ShortSurfaceFormSelector" => new ShortSurfaceFormSelector
                case "AtLeastOneNounSelector" => new AtLeastOneNounSelector
                case _ => throw new ConfigurationException("SpotSelector of name %s has not been configured in the properties file.".format(name))
            }
        }
    }

    object CandidateSearcher {
        def fromLuceneIndex(configuration: SpotlightConfiguration) = {
            val inMemory = configuration.isCandidateMapInMemory
            val candidateIndexDir = LuceneManager.pickDirectory(new File(configuration.getCandidateIndexDirectory))
            //candLuceneManager = new LuceneManager.CaseSensitiveSurfaceForms(candidateIndexDir) // or we can provide different functionality for surface forms (e.g. n-gram search)
            var candLuceneManager : LuceneManager = new LuceneManager(candidateIndexDir)
            candLuceneManager.setDBpediaResourceFactory(configuration.getDBpediaResourceFactory)
            val candidateSearcher = new LuceneCandidateSearcher(candLuceneManager,inMemory)
            SpotlightLog.info(this.getClass, "CandidateSearcher initiated (inMemory=%s) from %s", candidateIndexDir,inMemory)
            candidateSearcher
        }
    }
}


