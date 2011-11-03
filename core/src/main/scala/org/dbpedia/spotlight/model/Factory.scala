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

package org.dbpedia.spotlight.model

import org.dbpedia.spotlight.string.ModifiedWikiUtil
import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.{StopAnalyzer, Analyzer}
import java.io.File
import org.apache.lucene.store.Directory
import org.dbpedia.spotlight.spot.lingpipe.LingPipeSpotter
import org.dbpedia.spotlight.filter.annotations.CombineAllAnnotationFilters
import org.apache.lucene.document.Document
import org.dbpedia.spotlight.lucene.LuceneManager.DBpediaResourceField
import collection.JavaConversions._
import org.dbpedia.spotlight.lucene.search.{BaseSearcher, MergedOccurrencesContextSearcher}
import org.dbpedia.spotlight.spot._
import com.aliasi.sentences.IndoEuropeanSentenceModel
import org.dbpedia.spotlight.tagging.lingpipe.{LingPipeTextUtil, LingPipeTaggedTokenProvider, LingPipeFactory}
import org.dbpedia.spotlight.exceptions.{ItemNotFoundException, ConfigurationException}
import java.util.HashMap
import org.dbpedia.spotlight.lucene.disambiguate.MergedOccurrencesDisambiguator
import org.apache.commons.logging.LogFactory
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.snowball.SnowballAnalyzer
import org.dbpedia.spotlight.lucene.similarity.{InvCandFreqSimilarity, CachedInvCandFreqSimilarity, JCSTermCache}
import org.apache.lucene.misc.SweetSpotSimilarity
import org.apache.lucene.search.{DefaultSimilarity, ScoreDoc, Similarity}
import org.dbpedia.spotlight.disambiguate._
import org.dbpedia.spotlight.annotate.{DefaultParagraphAnnotator, DefaultAnnotator}
import scalaj.collection.Imports._

/**
 * Class containing methods to create model objects in many different ways
 *
 * @author pablomendes
 * @author Joachim Daiber (Tagger and Spotter methods)
 */
object Factory {
    private val LOG = LogFactory.getLog(this.getClass)
    /*
    * I like this style for the factory. group by return type and offer many .from* methods
    */
    object SurfaceFormOccurrence {
        def from(occ: DBpediaResourceOccurrence) : SurfaceFormOccurrence = {
            new SurfaceFormOccurrence(occ.surfaceForm, occ.context, occ.textOffset)
        }
    }

    object SurfaceForm {
        def fromDBpediaResourceURI(resource: DBpediaResource, lowercased: Boolean) = {
            val name = ModifiedWikiUtil.cleanPageTitle(resource.uri)
            val surfaceForm = if (lowercased) new SurfaceForm(name.toLowerCase) else new SurfaceForm(name)
            surfaceForm;
        }
        def fromString(name: String) = {
            new SurfaceForm(name.toString)
        }
    }

    object DBpediaResourceOccurrence {
        def from(sfOcc: SurfaceFormOccurrence, resource: DBpediaResource, score: Double) = {
            LOG.debug("Factory test: support=%s, ctxScore=%s, sfOcc=%s".format(resource.support, score, sfOcc));
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
        def from(sfOcc: SurfaceFormOccurrence, resource: DBpediaResource, score: Tuple2[Int,Double]) = {
            //TODO can take a mixture as param and use resource.score to mix with the other two scores in Tuple2
            new DBpediaResourceOccurrence("",  // there is no way to know this here
                new DBpediaResource(resource.uri, score._1),
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
            if (uri.startsWith(DBpediaType.DBPEDIA_ONTOLOGY_PREFIX)) {
                new DBpediaType(uri)
            } else if (uri.startsWith(FreebaseType.FREEBASE_RDF_PREFIX)) {
                FreebaseType.fromTypeString(uri)
            } else if (uri.startsWith(SchemaOrgType.SCHEMAORG_PREFIX)) {
                new SchemaOrgType(uri)
            } else {
                new DBpediaType(uri)
            }
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
                            case _ => new DBpediaType(ontologyType)
                        }
                    }
                }
                
            }catch{
                //The default type for non-prefixed type strings:
                case e: scala.MatchError => new DBpediaType(ontologyType)
            }
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
        def from(analyzerName : String, language: String, stopWords: java.util.Set[String]) : Analyzer = {
            (new StandardAnalyzer(Version.LUCENE_29, stopWords) ::
                new SnowballAnalyzer(Version.LUCENE_29, language, stopWords) ::
                Nil)
                .map(a => (a.getClass.getSimpleName, a))
                .toMap
                .get(analyzerName)
                .getOrElse(throw new ConfigurationException("Unknown Analyzer: "+analyzerName))
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
                resource.setTypes(document.getValues(field.name).map( t => OntologyType.fromQName(t) ).toList)
            case _ =>
        }
    }


}


