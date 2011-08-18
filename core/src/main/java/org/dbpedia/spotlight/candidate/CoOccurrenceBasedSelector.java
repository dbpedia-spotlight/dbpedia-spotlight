package org.dbpedia.spotlight.candidate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.candidate.cooccurrence.ClassifierFactory;
import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClass;
import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClassification;
import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClassifier;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProviderSQL;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterPOS;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterPattern;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterTermsize;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.model.SpotterConfiguration;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.TaggedText;
import org.dbpedia.spotlight.spot.TaggedSpotSelector;
import org.dbpedia.spotlight.tagging.TaggedToken;

import java.util.LinkedList;
import java.util.List;

/**
 * Candidate selector based on co-occurrence data using two classifiers for unigram
 * and ngram candidates.
 *
 * @author Joachim Daiber
 */
public class CoOccurrenceBasedSelector implements TaggedSpotSelector {

	private final Log LOG = LogFactory.getLog(this.getClass());


	/**
	 * Creates a co-occurrence based selector. A SpotterConfiguration object must be
	 * passed as a parameter since the selector must use and initialize an occurrence
	 * data provider and a factory for classifiers.
	 *
	 * @see org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider
	 * @see ClassifierFactory
	 *
	 * @param spotterConfiguration SpotterConfiguration object with classifier paths and JDBC
	 * 			description of occurrence data provider.
	 * @throws InitializationException Either the OccurrenceDataProvider or the ClassifierFactory
	 * 			could not be initialized.
	 */
	public CoOccurrenceBasedSelector(SpotterConfiguration spotterConfiguration) throws InitializationException {
		
		LOG.info("Initializing occurrence data provider.");
		OccurrenceDataProviderSQL.initialize(spotterConfiguration);
		LOG.info("Done.");

		LOG.info("Initializing candidate classifiers.");
		new ClassifierFactory(spotterConfiguration.getCandidateClassifierUnigram(),
				spotterConfiguration.getCandidateClassifierNGram(),
				spotterConfiguration.getCandidateOccurrenceDataSource(),
				OccurrenceDataProviderSQL.getInstance()
			);
		
		LOG.info("Done.");
		
	}
	

	/**
	 * Filter the list of surface form occurrences, removing all occurrences that are considered
	 * common.
	 *
	 * @param surfaceFormOccurrences spotted surface form occurrences
	 * @return List of non-common surface form occurrences
	 */
	public List<SurfaceFormOccurrence> select(List<SurfaceFormOccurrence> surfaceFormOccurrences) {

		List<SurfaceFormOccurrence> selectedOccurrences = new LinkedList<SurfaceFormOccurrence>();

		FilterPOS filterPOS = new FilterPOS();
		FilterTermsize unigramFilter = new FilterTermsize(FilterTermsize.Termsize.unigram);
		FilterPattern filterPattern = new FilterPattern();

		CandidateClassifier unigramClassifier = ClassifierFactory.getClassifierInstanceUnigram();
		CandidateClassifier ngramClassifier = ClassifierFactory.getClassifierInstanceNGram();

		assert unigramClassifier != null;
		assert ngramClassifier != null;

		//ngramClassifier.setVerboseMode(true);
		//unigramClassifier.setVerboseMode(true);
		List<String> decisions = new LinkedList<String>();

		for(SurfaceFormOccurrence surfaceFormOccurrence : surfaceFormOccurrences) {

            if (! (surfaceFormOccurrence.context() instanceof TaggedText)) { //FIXME added this to avoid breaking, but code below will never run if we don't pass the taggedtext
                LOG.warn("SurfaceFormOccurrence did not contain TaggedText. Cannot apply "+this.getClass());
				
                selectedOccurrences.add(surfaceFormOccurrence);
                continue;
            }


			if(unigramFilter.applies(surfaceFormOccurrence)) {

				/**
				 * Unigram (n = 1)
				 */

				if(!filterPOS.applies(surfaceFormOccurrence)) {

					/**
					 * The Surface Form is on the POS blacklist, i.e. a single adjective,
					 * verb, etc.
					 */


					if(Character.isUpperCase(surfaceFormOccurrence.surfaceForm().name().charAt(0))){
						TaggedToken taggedToken = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider().getTaggedTokens(surfaceFormOccurrence).get(0);

						/**
						 * Add uppercase adjectives (e.g. Canadian tv star)
						 */
						if(taggedToken.getPOSTag() != null && taggedToken.getPOSTag().startsWith("j"))
							selectedOccurrences.add(surfaceFormOccurrence);

					}else{
						decisions.add("Dropped by POS filter: " + surfaceFormOccurrence);

					}

				}else if(!filterPattern.applies(surfaceFormOccurrence)){
					decisions.add("Dropped by Pattern filter: " + surfaceFormOccurrence);
				}else{

					CandidateClassification candidateClassification = null;
					try {
						candidateClassification = unigramClassifier.classify(surfaceFormOccurrence);
					} catch (Exception e) {
						LOG.error("Exception when classifying unigram candidate: " + e);
						continue;
					}

					if(candidateClassification.getCandidateClass() == CandidateClass.valid) {
						selectedOccurrences.add(surfaceFormOccurrence);
						//LOG.info(("Kept by UnigramClassifier (Confidence: " + candidateClassification.getConfidence() + "): " + surfaceFormOccurrence);
					}else{
						decisions.add("Dropped by UnigramClassifier (Confidence: " + candidateClassification.getConfidence() + "): " + surfaceFormOccurrence);
					}

				}


			}else{

				/**
				 * n > 1
				 */

				CandidateClassification candidateClassification = null;
				try{
					candidateClassification = ngramClassifier.classify(surfaceFormOccurrence);
				}catch (Exception e) {
					LOG.error("Exception when classifying ngram candidate: " + e);
					continue;
				}

				if(candidateClassification.getCandidateClass() == CandidateClass.valid) {
					selectedOccurrences.add(surfaceFormOccurrence);
					//LOG.info("Kept by nGramClassifier (Confidence: " + candidateClassification.getConfidence() + "): " + surfaceFormOccurrence);
				}else{
					decisions.add("Dropped by NGramClassifier: " + surfaceFormOccurrence);
				}

			}

		}

		for (String decision : decisions) {
			LOG.info(decision);
		}
		
		return selectedOccurrences;
	}

}
