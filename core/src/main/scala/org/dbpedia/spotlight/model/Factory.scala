package org.dbpedia.spotlight.model

import org.dbpedia.spotlight.string.ModifiedWikiUtil
import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.{StopAnalyzer, Analyzer}
import java.io.File
import org.apache.lucene.store.Directory
import org.dbpedia.spotlight.annotate.DefaultAnnotator
import org.dbpedia.spotlight.spot.lingpipe.LingPipeSpotter
import org.dbpedia.spotlight.filter.annotations.CombineAllAnnotationFilters
import org.apache.lucene.document.Document
import org.dbpedia.spotlight.lucene.LuceneManager.DBpediaResourceField
import collection.JavaConversions._
import org.dbpedia.spotlight.lucene.search.{BaseSearcher, MergedOccurrencesContextSearcher}
import org.dbpedia.spotlight.candidate.{CoOccurrenceBasedSelector}
import com.aliasi.sentences.IndoEuropeanSentenceModel
import org.dbpedia.spotlight.tagging.lingpipe.{LingPipeTextUtil, LingPipeTaggedTokenProvider, LingPipeFactory}
import org.dbpedia.spotlight.disambiguate.{ DefaultDisambiguator}
import org.dbpedia.spotlight.exceptions.{ItemNotFoundException, ConfigurationException}
import org.dbpedia.spotlight.spot.{WikiMarkupSpotter, Spotter, AtLeastOneNounSelector, SpotterWithSelector}
import java.util.HashMap
import org.dbpedia.spotlight.disambiguate.{Disambiguator, DefaultDisambiguator}
import org.dbpedia.spotlight.lucene.disambiguate.MergedOccurrencesDisambiguator
import org.apache.commons.logging.LogFactory
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.snowball.SnowballAnalyzer
import org.dbpedia.spotlight.lucene.similarity.{InvCandFreqSimilarity, CachedInvCandFreqSimilarity, JCSTermCache}
import org.apache.lucene.misc.SweetSpotSimilarity
import org.apache.lucene.search.{DefaultSimilarity, ScoreDoc, Similarity}

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
                new FreebaseType(uri)
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
                            case "f" | "freebase" => new FreebaseType(suffix)
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

    object Paragraph {
        def from(a: AnnotatedParagraph) = {
            new Paragraph(a.id,a.text,a.occurrences.map( dro => Factory.SurfaceFormOccurrence.from(dro)))
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
                resource.setTypes(document.getValues(field.name).map( t => new DBpediaType(t) ).toList)
            case _ =>
        }
    }


}

/**
 * This class contains many of the "defaults" for DBpedia Spotlight. Maybe consider renaming to DefaultFactory.
 *
 *
 */
class SpotlightFactory(val configuration: SpotlightConfiguration,
                    val analyzer: Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET)
                    ) {

    private val LOG = LogFactory.getLog(this.getClass)

    def this(configuration: SpotlightConfiguration) {
        this(configuration, new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", configuration.getStopWords))
        if (!new File(configuration.getTaggerFile).exists()) throw new ConfigurationException("POS tagger file does not exist! "+configuration.getTaggerFile);
    }

    val directory : Directory = LuceneManager.pickDirectory(new File(configuration.getContextIndexDirectory))
    val luceneManager : LuceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
    val similarity : Similarity = new CachedInvCandFreqSimilarity(JCSTermCache.getInstance(luceneManager, configuration.getMaxCacheSize))

    val lingPipeFactory : LingPipeFactory = new LingPipeFactory(new File(configuration.getTaggerFile), new IndoEuropeanSentenceModel())

    luceneManager.setDefaultAnalyzer(analyzer);
    luceneManager.setContextSimilarity(similarity);

    // The dbpedia resource factory is used every time a document is retrieved from the index.
    // We can use the index itself as provider, or we can use a database. whichever is faster.
    val dbpediaResourceFactory : DBpediaResourceFactory = configuration.getDBpediaResourceFactory
    luceneManager.setDBpediaResourceFactory(dbpediaResourceFactory)
    LOG.debug("DBpedia Resource Factory is null?? %s".format(luceneManager.getDBpediaResourceFactory == null))

    val searcher = new MergedOccurrencesContextSearcher(luceneManager);

    val spotters = new HashMap[SpotterConfiguration.SpotterPolicy,Spotter]()

    def disambiguator() = {
        //val mixture = new LinearRegressionMixture
        //new MixedWeightsDisambiguator(searcher,mixture);
        new MergedOccurrencesDisambiguator(searcher)
        //new GraphCentralityDisambiguator(configuration)
    }

    def spotter(policy: SpotterConfiguration.SpotterPolicy) : Spotter = {
        if (policy == SpotterConfiguration.SpotterPolicy.LingPipeSpotter)
            spotters.getOrElse(policy, new LingPipeSpotter(new File(configuration.getSpotterConfiguration.getSpotterFile)))
        else if (policy == SpotterConfiguration.SpotterPolicy.AtLeastOneNounSelector) {
            spotters.getOrElse(policy, SpotterWithSelector.getInstance(spotter(SpotterConfiguration.SpotterPolicy.LingPipeSpotter),new AtLeastOneNounSelector(),taggedTokenProvider()))
        } else if (policy == SpotterConfiguration.SpotterPolicy.CoOccurrenceBasedSelector) {
            spotters.getOrElse(policy, SpotterWithSelector.getInstance(spotter(SpotterConfiguration.SpotterPolicy.LingPipeSpotter),new CoOccurrenceBasedSelector(configuration.getSpotterConfiguration),taggedTokenProvider()))
        } else {
            new WikiMarkupSpotter
        }
    }

    def spotter() : Spotter = {
        configuration.getSpotterConfiguration.getSpotterPolicies.foreach( policy => {
            spotters.put(policy, spotter(policy))
        })
        spotters.head._2
    }

    def annotator() ={
        new DefaultAnnotator(spotter(), disambiguator())
    }

    def filter() ={
        new CombineAllAnnotationFilters(configuration)
    }

    def taggedTokenProvider() = {
       new LingPipeTaggedTokenProvider(lingPipeFactory);
    }

    def textUtil() = {
       new LingPipeTextUtil(lingPipeFactory);
    }

    object DBpediaResource {
        def from(dbpediaID : String) : DBpediaResource = dbpediaResourceFactory.from(dbpediaID)
        def from(dbpediaResource : DBpediaResource) = dbpediaResourceFactory.from(dbpediaResource.uri)
    }

}
