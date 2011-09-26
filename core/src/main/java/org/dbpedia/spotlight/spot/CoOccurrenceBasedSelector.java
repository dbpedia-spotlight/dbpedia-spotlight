package org.dbpedia.spotlight.spot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.spot.cooccurrence.ClassifierFactory;
import org.dbpedia.spotlight.spot.cooccurrence.classification.SpotClass;
import org.dbpedia.spotlight.spot.cooccurrence.classification.SpotClassification;
import org.dbpedia.spotlight.spot.cooccurrence.classification.SpotClassifier;
import org.dbpedia.spotlight.spot.cooccurrence.features.data.OccurrenceDataProviderSQL;
import org.dbpedia.spotlight.spot.cooccurrence.filter.FilterPOS;
import org.dbpedia.spotlight.spot.cooccurrence.filter.FilterPattern;
import org.dbpedia.spotlight.spot.cooccurrence.filter.FilterTermsize;
import org.dbpedia.spotlight.tagging.TaggedToken;

import java.util.LinkedList;
import java.util.List;

/**
 * Spot selector based on co-occurrence data using two classifiers for unigram
 * and ngram candidates.
 *
 * @author Joachim Daiber
 */
public class CoOccurrenceBasedSelector implements TaggedSpotSelector {

	private final Log LOG = LogFactory.getLog(this.getClass());


	/**
	 * Creates a spot selector based on n-gram co-occurrence. A SpotterConfiguration object must be
	 * passed as a parameter since the selector must use and initialize an occurrence
	 * data provider and a factory for classifiers.
	 *
	 * @see org.dbpedia.spotlight.spot.cooccurrence.features.data.OccurrenceDataProvider
	 * @see ClassifierFactory
	 *
	 * @param spotterConfiguration SpotterConfiguration object with classifier paths and JDBC
	 * 			description of occurrence data provider.
	 * @param spotlightFactory
     * @throws InitializationException Either the OccurrenceDataProvider or the ClassifierFactory
	 * 			could not be initialized.
	 */
	public CoOccurrenceBasedSelector(SpotterConfiguration spotterConfiguration, SpotlightFactory spotlightFactory) throws InitializationException {
		
		LOG.info("Initializing spot occurrence data provider.");
		OccurrenceDataProviderSQL.initialize(spotterConfiguration);
		LOG.info("Done.");

		LOG.info("Initializing spot candidate classifiers.");
		new ClassifierFactory(spotterConfiguration.getCoOcSelectorClassifierUnigram(),
				spotterConfiguration.getCoOcSelectorClassifierNGram(),
				spotterConfiguration.getCoOcSelectorDatasource(),
				OccurrenceDataProviderSQL.getInstance()
			);
		

        //Classify a single n-gram and unigram occurrence to make sure the classifiers are working.
		SpotClassifier unigramClassifier = ClassifierFactory.getClassifierInstanceUnigram();
		SpotClassifier ngramClassifier = ClassifierFactory.getClassifierInstanceNGram();

        SurfaceFormOccurrence unigramOccurrence = new SurfaceFormOccurrence(new SurfaceForm("Berlin"),
                new TaggedText("Berlin is a city.", spotlightFactory.taggedTokenProvider()),
                0, Provenance.Undefined(), -1);
        try {
            unigramClassifier.classify(unigramOccurrence);
        } catch (Exception e) {
            throw new InitializationException("An error occurred while classifying a test spot using the co-occurrence " +
                    "based spot selector. This is most probably caused by an outdated spot selector model. Please " +
                    "check the spot selector models defined 'org.dbpedia.spotlight.spot.cooccurrence.classifier.*'.", e);
        }

        SurfaceFormOccurrence ngramOccurrence = new SurfaceFormOccurrence(new SurfaceForm("Bill Gates"),
                new TaggedText("Bill Gates is an American.", spotlightFactory.taggedTokenProvider()),
                0, Provenance.Undefined(), -1);
        try {
            ngramClassifier.classify(ngramOccurrence);
        } catch (Exception e) {
            throw new InitializationException("An error occurred while classifying a test spot using the co-occurrence " +
                    "based spot selector. This is most probably caused by an outdated spot selector model. Please " +
                    "check the spot selector models defined 'org.dbpedia.spotlight.spot.cooccurrence.classifier.*'.", e);        }

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

		SpotClassifier unigramClassifier = ClassifierFactory.getClassifierInstanceUnigram();
		SpotClassifier ngramClassifier = ClassifierFactory.getClassifierInstanceNGram();

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

                    SpotClassification spotClassification;
                    try {
                        spotClassification = unigramClassifier.classify(surfaceFormOccurrence);

                        if(spotClassification.getCandidateClass() == SpotClass.valid) {
                            selectedOccurrences.add(surfaceFormOccurrence);
                            //LOG.info(("Kept by UnigramClassifier (Confidence: " + spotClassification.getConfidence() + "): " + surfaceFormOccurrence);
                        }else{
                            decisions.add("Dropped by UnigramClassifier (Confidence: " + spotClassification.getConfidence() + "): " + surfaceFormOccurrence);
                        }

                    } catch (Exception e) {
                        LOG.error("Exception when classifying unigram candidate: " + e);
                    }

				}


			}else{

				/**
				 * n > 1
				 */

				SpotClassification spotClassification;
				try{
					spotClassification = ngramClassifier.classify(surfaceFormOccurrence);
				}catch (Exception e) {
                    LOG.error("Exception when classifying ngram candidate: " + e);
                    continue;
				}

				if(spotClassification.getCandidateClass() == SpotClass.valid) {
					selectedOccurrences.add(surfaceFormOccurrence);
					//LOG.info("Kept by nGramClassifier (Confidence: " + spotClassification.getConfidence() + "): " + surfaceFormOccurrence);
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
