package org.dbpedia.spotlight.model

import org.dbpedia.spotlight.string.ModifiedWikiUtil
import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.{StopAnalyzer, Analyzer}
import java.io.File
import org.apache.lucene.search.Similarity
import org.dbpedia.spotlight.lucene.similarity.{CachedInvCandFreqSimilarity, JCSTermCache}
import org.apache.lucene.store.Directory
import org.dbpedia.spotlight.annotate.DefaultAnnotator
import org.dbpedia.spotlight.spot.lingpipe.LingPipeSpotter
import org.dbpedia.spotlight.filter.annotations.CombineAllAnnotationFilters
import org.apache.lucene.document.Document
import org.dbpedia.spotlight.lucene.LuceneManager.DBpediaResourceField
import collection.JavaConversions._
import org.dbpedia.spotlight.disambiguate.DefaultDisambiguator
import org.dbpedia.spotlight.lucene.search.{BaseSearcher, MergedOccurrencesContextSearcher}
import org.dbpedia.spotlight.exceptions.ConfigurationException
import org.dbpedia.spotlight.candidate.{CoOccurrenceBasedSelector}
import com.aliasi.sentences.IndoEuropeanSentenceModel
import org.dbpedia.spotlight.tagging.lingpipe.{LingPipeTaggedTokenProvider, LingPipeFactory}
import org.dbpedia.spotlight.spot.{AtLeastOneNounSelector, SpotterWithSelector}

/**
 * Class containing methods to create model objects in many different ways
 *
 * @author pablomendes
 */
object Factory {

    def createSurfaceFormFromDBpediaResourceURI(resource: DBpediaResource, lowercased: Boolean) = {
        val name = ModifiedWikiUtil.cleanPageTitle(resource.uri)
        val surfaceForm = if (lowercased) new SurfaceForm(name.toLowerCase) else new SurfaceForm(name)
        surfaceForm;
    }

    def createMergedDBpediaResourceOccurrenceFromDocument(doc : Document, id: Int, searcher: BaseSearcher) = {
        // getField: If multiple fields exists with this name, this method returns the first value added.
        var resource = searcher.getDBpediaResource(id);
        var context = new Text(doc.getFields(LuceneManager.DBpediaResourceField.CONTEXT.toString).map(f => f.stringValue).mkString("\n"))

        Array(new DBpediaResourceOccurrence( //TODO add document id as occurrence id
            resource,
            createSurfaceFormFromDBpediaResourceURI(resource, false), // this is sort of the "official" surface form, since it's the cleaned up title
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
                createSurfaceFormFromDBpediaResourceURI(resource, false), // this is sort of the "official" surface form, since it's the cleaned up title
                context,
                -1, //TODO find offset
                Provenance.Wikipedia // Ideally grab this from index, if we have sources other than Wikipedia
            ))
    }


    def setField(resource: DBpediaResource, field: DBpediaResourceField, document: Document) {
        field match {
            case DBpediaResourceField.URI_COUNT =>
                resource.setSupport(document.getField(field.name).stringValue.toInt)
            case DBpediaResourceField.URI_PRIOR =>
                resource.setPrior(document.getField(field.name).stringValue.toDouble)
            case DBpediaResourceField.TYPE =>
                resource.setTypes(document.getValues(field.name).map( t => new DBpediaType(t) ).toList)
            case _ =>
        }
    }


  


}


class LuceneFactory(val configuration: SpotlightConfiguration,
                    val analyzer: Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET)
                    ) {

    def this(configuration: SpotlightConfiguration) {
        this(configuration, new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET))
        if (!new File(configuration.getTaggerFile).exists()) throw new ConfigurationException("POS tagger file does not exist! "+configuration.getTaggerFile);

        //Initialize the part-of-speech tagger with the HMM model from the configuration:
        LingPipeFactory.setTaggerModelFile(new File(configuration.getTaggerFile()));

        //Initialize the sentence model:
        LingPipeFactory.setSentenceModel(new IndoEuropeanSentenceModel());


    }

    val directory : Directory = LuceneManager.pickDirectory(new File(configuration.getIndexDirectory))
    val luceneManager : LuceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
    val similarity : Similarity = new CachedInvCandFreqSimilarity(JCSTermCache.getInstance(luceneManager, configuration.getMaxCacheSize))

    luceneManager.setContextAnalyzer(analyzer);
    luceneManager.setContextSimilarity(similarity);

    val searcher = new MergedOccurrencesContextSearcher(luceneManager);

    def disambiguator() = {
        //val mixture = new LinearRegressionMixture
        //new MixedWeightsDisambiguator(searcher,mixture);
        new DefaultDisambiguator(configuration)
    }

    def spotter() ={
        //For the simple spotter (no spot selection)
        //new LingPipeSpotter(new File(configuration.getSpotterConfiguration().getSpotterFile()));

        SpotterWithSelector.getInstance(
          new LingPipeSpotter(new File(configuration.getSpotterConfiguration().getSpotterFile())),
          new AtLeastOneNounSelector(),
          new LingPipeTaggedTokenProvider()
        );

    }

    def annotator() ={
        new DefaultAnnotator(spotter(), disambiguator())
    }

    def filter() ={
        new CombineAllAnnotationFilters(configuration)
    }

}