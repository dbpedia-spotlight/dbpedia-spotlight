package org.dbpedia.spotlight.candidate.cooccurrence.training;

import com.mongodb.*;
import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClass;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.tagging.lingpipe.AnnotatedString;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeTaggedTokenProvider;
import org.dbpedia.spotlight.util.KeywordExtractor;
import org.dbpedia.spotlight.util.WebSearchConfiguration;
import org.dbpedia.spotlight.util.YahooBossSearcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A training data generator for term candidates.
 *
 * @author Joachim Daiber
 */

public abstract class DatasetGenerator {

	private DB dbpediaInfo;
	private DB wikipediaData;
	private DB genericSentences;
	private SpotlightConfiguration configuration;
	protected MergedOccurrencesContextSearcher searcher;

	protected static final int EXAMPLES_WIKIPEDIA = 10;
	protected static final int EXAMPLES_GENERIC = 15;
	protected static final int NUMBER_OF_EXAMINED_SURFACE_FORMS = 100;
	private String indexingConfigFileName = "/Users/jodaiber/Documents/workspace/ba/workspace/DBPedia Spotlight/conf/indexing.properties";

	
	/**
	 * Maximum number of sentences that are analysed for a single surface form
	 *
	 * If all examples of a surface form are Adjectives and valid examples are
	 * defined as Nouns, the program will look at maximum of RANDOM_SENTENCE_LIMIT
	 * sentences before returning.
	 * 
	 */

	private static final int RANDOM_SENTENCE_LIMIT = 50;
	private SpotlightFactory luceneFactory;

	public SpotlightFactory getLuceneFactory() {
		return luceneFactory;
	}


	/**
	 * A training data generator for term candidates.
	 *
	 * @throws UnknownHostException
	 * @throws ConfigurationException
	 */

	public DatasetGenerator() throws UnknownHostException, ConfigurationException {
		Mongo mongodb = new Mongo();

		dbpediaInfo = mongodb.getDB("dbpediainfo");
		wikipediaData = mongodb.getDB("wikipedia");
		genericSentences = mongodb.getDB("generic");

		configuration = new SpotlightConfiguration("/Users/jodaiber/Documents/workspace/ba/workspace/DBPedia Spotlight/conf/server.properties");

		luceneFactory = new SpotlightFactory(configuration);
		searcher = luceneFactory.searcher();

	}


	/**
	 * Returns a single random instance of a surface form occurrence from a number of generic sources.
	 *
	 * @param surfaceForm the surface form of the occurrence
	 * @return
	 */

	protected OccurrenceInstance findExampleSentenceGeneric(String surfaceForm) {

		DBCursor cursor = getMongoDBCursorGeneric(surfaceForm);

		//Get a random article:
		DBObject random = cursor.skip((int) (Math.random() * cursor.count())).next();

		String sentence = ((BasicDBObject) random).getString("sentence");
		int offset = sentence.indexOf(surfaceForm);

		LingPipeTaggedTokenProvider lingPipeTaggedTokenProvider = luceneFactory.taggedTokenProvider();
		lingPipeTaggedTokenProvider.initialize(sentence);

		OccurrenceInstance candidateOccurrence = new OccurrenceInstance(
				surfaceForm, offset, new TaggedText(sentence, luceneFactory.taggedTokenProvider()), null, null, null, CandidateClass.common);

		return candidateOccurrence;
	}


	private DBCursor getMongoDBCursorGeneric(String surfaceForm) {
		DBCollection sentences = genericSentences.getCollection("sentences");

		BasicDBObject query = new BasicDBObject();
		query.put("unigrams", surfaceForm);

		DBCursor cursor = sentences.find(query);

		return cursor;
	}

	/**
	 * Get generic example sentences for a surface form
	 *
	 * @param surfaceForm
	 * @return
	 */

	private int countExampleSentencesGeneric(String surfaceForm){
		return getMongoDBCursorGeneric(surfaceForm).count();
	}


	/**
	 * Get a cursor for all Wikipedia articles containing the combination of surface form
	 * and DBPediaResource
	 *
	 * @param surfaceForm
	 * @param dbpediaResource
	 * @return
	 */

	private DBCursor getMongoDBCursorWikipedia(SurfaceForm surfaceForm, DBpediaResource dbpediaResource) {

		DBCollection articles = wikipediaData.getCollection("articles");
		BasicDBObject query = new BasicDBObject();

		/**
		 * Because of the way, the MongoDB is indexed, it is necessary to replace (, )
		 */
		String searchURI = dbpediaResource.uri().replace("%28", "(").replace("%29", ")");
		query.put("sfAndURI", surfaceForm.name() + "<-->" + searchURI);
		DBCursor cursor = articles.find(query);

		return cursor;
	}

	protected int countExampleSentencesWikipedia(SurfaceForm surfaceForm, DBpediaResource dbpediaResource){
		return getMongoDBCursorWikipedia(surfaceForm, dbpediaResource).count();
	}


	/**
	 * Returns a single random DBpediaResourceOccurrence for the combination of surface form and DBPedia resource.
	 *
	 * @param surfaceForm the surface form of the occurrence
	 * @param dbpediaResource the annotated DBPedia resource of the occurence
	 * @return DBpediaResourceOccurrence including the sentence
	 */

	protected OccurrenceInstance findExampleSentenceWikipedia(SurfaceForm surfaceForm, DBpediaResource dbpediaResource){

		DBCursor cursor = getMongoDBCursorWikipedia(surfaceForm, dbpediaResource);

		//Get a random article:
		DBObject random = cursor.skip((int) (Math.random() * cursor.count())).next();

		int offset = 0;
		for (Object annotation : ((BasicDBList) random.get("annotations"))) {
			String annotationSurfaceForm = (String) ((DBObject) annotation).get("surface_form");
			String annotationURI = ((String) ((DBObject) annotation).get("uri"))
					.replace("(", "%28").replace(")", "%29");


			if(surfaceForm.name().equals(annotationSurfaceForm) && dbpediaResource.uri().equals(annotationURI)) {
				offset = (Integer) ((DBObject) annotation).get("offset");
				break;
			}

		}

		String text = ((BasicDBObject) random).getString("text");
		AnnotatedString sentence = luceneFactory.textUtil().getSentence(offset,
				offset + surfaceForm.name().length(), text);

		OccurrenceInstance occurrenceInstance = new OccurrenceInstance(surfaceForm.name(),
				//The offset must be relative to the offset of the sentence:
				offset - sentence.getOffsetFrom(),
				new TaggedText(sentence.getAnnotation(), luceneFactory.taggedTokenProvider()),
				dbpediaResource.uri(),
				getAnnotationTitle(dbpediaResource),
				getAnnotationAbstract(dbpediaResource),
				CandidateClass.valid
		);

		return occurrenceInstance;
	}


	/**
	 * Find generic example sentences for a surface form.
	 *
	 * @param surfaceForm
	 * @return
	 */

	protected List<OccurrenceInstance> findExampleSentencesGeneric(SurfaceForm surfaceForm, int numberOfExamples) {

		int totalCount = countExampleSentencesGeneric(surfaceForm.name());
		List<OccurrenceInstance> genericExamples = new LinkedList<OccurrenceInstance>();

		Set<OccurrenceInstance> exampleSet = new HashSet<OccurrenceInstance>();


		/**
		 * Get n example sentence from the corpora, where
		 *
		 * totalCount >= n <= numberOfExamples
		 */

		int i = 0;
		while(exampleSet.size() < totalCount && genericExamples.size() < numberOfExamples) {

			if(i == RANDOM_SENTENCE_LIMIT)
				break;
			i++;


			OccurrenceInstance exampleSentenceGeneric = findExampleSentenceGeneric(surfaceForm.name());

			if(!genericExamples.contains(exampleSentenceGeneric) && isValidExampleSentence(exampleSentenceGeneric))
				genericExamples.add(exampleSentenceGeneric);

			exampleSet.add(exampleSentenceGeneric);
		}

		return genericExamples;
	}


	/**
	 * Determines whether an OccurenceInstance represents a valid occurence of the term.
	 *
	 * @param exampleSentenceGeneric
	 * @return
	 */
	
	protected abstract boolean isValidExampleSentence(OccurrenceInstance exampleSentenceGeneric);
	

	/**
	 * Find example sentences for the surface form and each of the DBpedia resources in the Map
	 * wikipediaResourceCountEstimate. This Map contains the proposed number of examples for each
	 * surface form - DBpedia resource combination that was estimated by querying a search engine.
	 *
	 * @param surfaceForm 
	 * @param wikipediaResourceCountEstimate
	 * @return
	 * @throws SearchException
	 */

	protected List<OccurrenceInstance> findExampleSentencesWikipedia(SurfaceForm surfaceForm, Map<DBpediaResource, Long> wikipediaResourceCountEstimate) throws SearchException {

		List<OccurrenceInstance> wikipediaExamplesForAllResources = new LinkedList<OccurrenceInstance>();

		for(DBpediaResource dbpediaResourceCandidate : wikipediaResourceCountEstimate.keySet()) {

			/**
			 * Contains all examples to be returned
			 */
			List<OccurrenceInstance> wikipediaExamplesForResource = new LinkedList<OccurrenceInstance>();

			
			/**
			 * Contains all examples that were already observed, weather they were valid or not.
			 */
			Set<OccurrenceInstance> wikipediaExamplesObserved = new HashSet<OccurrenceInstance>();


			/**
			 * Gather example sentences for each surface form until there are no more examples sentences
			 * or until enough examples have been collected.
			 */
			int countSurfaceFormResource = countExampleSentencesWikipedia(surfaceForm, dbpediaResourceCandidate);

			int i = 0;
			while (wikipediaExamplesObserved.size() < countSurfaceFormResource
					&& wikipediaExamplesForResource.size() < wikipediaResourceCountEstimate.get(dbpediaResourceCandidate)) {

				if(i == RANDOM_SENTENCE_LIMIT)
					break;
				i++;

				OccurrenceInstance exampleSentenceWikipedia = findExampleSentenceWikipedia(surfaceForm,
						dbpediaResourceCandidate);

				if(!wikipediaExamplesObserved.contains(exampleSentenceWikipedia)
						&& isValidExampleSentence(exampleSentenceWikipedia)){

					wikipediaExamplesForResource.add(exampleSentenceWikipedia);
				}

				wikipediaExamplesObserved.add(exampleSentenceWikipedia);
			}

			wikipediaExamplesForAllResources.addAll(wikipediaExamplesForResource);

		}

		return wikipediaExamplesForAllResources;
	}


	/**
	 * Get the title of the DBPedia resource
	 *
	 * @param dbpediaResource
	 * @return
	 */

	protected String getAnnotationTitle(DBpediaResource dbpediaResource) {

		BasicDBObject entity = getDBPediaInfo(dbpediaResource.getFullUri());
		return entity.getString("title");

	}


	/**
	 * Get the abstract for the DBPedia Resource.
	 *
	 * @param dbpediaResource
	 * @return
	 */
	protected String getAnnotationAbstract(DBpediaResource dbpediaResource) {

		BasicDBObject entity = getDBPediaInfo(dbpediaResource.getFullUri());
		return entity.getString("abstract");

	}


	/**
	 * Get DBPedia info to enrich the annotation sheets.
	 *
	 * @param uri	URI of the resource
	 * @return
	 */

	public BasicDBObject getDBPediaInfo(String uri) {

		DBCollection dbpediainfo = dbpediaInfo.getCollection("dbpediainfo");

		BasicDBObject query = new BasicDBObject();
		query.put("_id", uri);
		BasicDBObject entity = (BasicDBObject) dbpediainfo.findOne(query);

		return entity;
	}


	/**
	 * Load surface forms that are confusable with common words.
	 *
	 * @param file NT with wiktionary links
	 * @return
	 * @throws IOException
	 */

	protected static List<String> loadConfusables(File file) throws IOException {

		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

		Pattern pattern = Pattern.compile(
				"<http://dbpedia.org/resource/(.*)> <http://dbpedia.org/property/(wikiPageWiktionaryLink|wikiDisambigPageWiktionaryLink)> <http://en.wiktionary.org/wiki/(.*)> .*");


		List<String> confusables = new ArrayList<String>();

		String line;
		while((line = bufferedReader.readLine()) != null) {
			Matcher matcher = pattern.matcher(line);
			matcher.find();
			confusables.add(matcher.group(3));
		}

		return confusables;
	}


	/**
	 * The web prior for a list of DBPedia resources is a very rough estimate of the distribution
	 * of each of the DBPedia resources and the most common form of surface form.
	 *
	 * This method returns a map from DBpedia resource to search engine hits. The count for the common
	 * form of the term (common in the sense that it may not be one of the DBpedia resources) can be
	 * retrieved using map.get(null).
	 *
	 * @param surfaceForm the surface form to search for
	 * @param dbpediaResources List of DBpedia resource candidates for the surface form
	 * @return
	 * @throws ConfigurationException
	 */
	protected Map<DBpediaResource, Long> getWebPrior(SurfaceForm surfaceForm, Collection<DBpediaResource> dbpediaResources) throws ConfigurationException {

		Map<DBpediaResource, Long> resourcePriors = new HashMap<DBpediaResource, Long>();


		KeywordExtractor keywordExtractor = new KeywordExtractor(configuration);

		WebSearchConfiguration wConfig = new WebSearchConfiguration(indexingConfigFileName);
		YahooBossSearcher yahooBossSearcher = new YahooBossSearcher(wConfig);

		long totalHitsResources = 0;
		for (DBpediaResource dbpediaResource : dbpediaResources) {
			String allKeywords = keywordExtractor.getAllKeywords(dbpediaResource);
			System.err.println(allKeywords);

			Long hitsResource = (Long) yahooBossSearcher.getYahooAnswer(allKeywords)._1();
			totalHitsResources += hitsResource;

			resourcePriors.put(dbpediaResource, hitsResource);
		}

		//Get the overall count for the surface form
		Long totalHits = (Long) yahooBossSearcher.getYahooAnswer(surfaceForm.name())._1();
		resourcePriors.put(null, totalHits - totalHitsResources);

		return resourcePriors;
	}


	public List<OccurrenceInstance> findExampleSentences(SurfaceForm exampleSF) throws SearchException, ConfigurationException {

		Set<DBpediaResource> candidates = searcher.getCandidates(exampleSF);

		Map<DBpediaResource, Long> webPriors = getWebPrior(exampleSF, candidates);
		Map<DBpediaResource, Long> wikipediaResourceCountEstimate = getWikipediaResourceCountEstimate(EXAMPLES_WIKIPEDIA, webPriors);

		LinkedList<OccurrenceInstance> exampleSentences = new LinkedList<OccurrenceInstance>();

		//Add Wikipedia examples:
		exampleSentences.addAll(findExampleSentencesWikipedia(exampleSF, wikipediaResourceCountEstimate));

		//Add generic examples:
		exampleSentences.addAll(findExampleSentencesGeneric(exampleSF, EXAMPLES_GENERIC));

		
		return exampleSentences;
		
	}
	

	private Map<DBpediaResource, Long> getWikipediaResourceCountEstimate(long numberOfExamples,
																		 Map<DBpediaResource, Long> webPriors) {
		double resourcesCountTotal = 0;

		for(Map.Entry<DBpediaResource, Long> webPrior : webPriors.entrySet()) {
			if(webPrior.getKey() != null)
				resourcesCountTotal += webPrior.getValue();
		}

		Map<DBpediaResource, Long> resourcesCounts = new HashMap<DBpediaResource, Long>();

		for(Map.Entry<DBpediaResource, Long> webPrior : webPriors.entrySet()) {
			long numberOfExamplesForResource = Math.round((webPrior.getValue() / resourcesCountTotal) * numberOfExamples);

			if(numberOfExamplesForResource > 0 && webPrior.getKey() != null)
				resourcesCounts.put(webPrior.getKey(), numberOfExamplesForResource);
		}

		return resourcesCounts;

	}
}
