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
import org.dbpedia.spotlight.tagging.TaggedTokenProvider;

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
     * @throws InitializationException Either the OccurrenceDataProvider or the ClassifierFactory
	 * 			could not be initialized.
	 */
	public CoOccurrenceBasedSelector(SpotterConfiguration spotterConfiguration) throws InitializationException {
		
		LOG.info("Initializing spot occurrence data provider.");
		OccurrenceDataProviderSQL.initialize(spotterConfiguration);
		LOG.info("Done.");

		LOG.info("Initializing spot candidate classifiers.");
		new ClassifierFactory(spotterConfiguration.getCoOcSelectorClassifierUnigram(),
				spotterConfiguration.getCoOcSelectorClassifierNGram(),
				spotterConfiguration.getCoOcSelectorDatasource(),
				OccurrenceDataProviderSQL.getInstance()
			);
        LOG.info("Done.");
    }
    

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
     * @param taggedTokenProvider TaggedTokenProvider used to create a tagged text to test the
     *          classifiers.
     * @throws InitializationException Either the OccurrenceDataProvider or the ClassifierFactory
	 * 			could not be initialized.
	 */

    public CoOccurrenceBasedSelector(SpotterConfiguration spotterConfiguration, TaggedTokenProvider taggedTokenProvider) throws InitializationException {

        this(spotterConfiguration);

        //TODO Instead of doing a test classification here, we should properly check if the serialized model suits the WEKA instances that are produced from SurfaceFormOccurrences.
        LOG.info("Testing classifiers for co-occurrence based spot selector.");
		SpotClassifier unigramClassifier = ClassifierFactory.getClassifierInstanceUnigram();
		SpotClassifier ngramClassifier = ClassifierFactory.getClassifierInstanceNGram();

        Text taggedText = new TaggedText("Bill Gates is a software developer from Berlin.", taggedTokenProvider);

        SurfaceFormOccurrence ngramOccurrence = new SurfaceFormOccurrence(new SurfaceForm("Bill Gates"),
                        taggedText, 0, Provenance.Undefined(), -1);

        SurfaceFormOccurrence unigramOccurrence = new SurfaceFormOccurrence(new SurfaceForm("Berlin"),
                        taggedText, 41, Provenance.Undefined(), -1);

        try {
            unigramClassifier.classify(unigramOccurrence);
            ngramClassifier.classify(ngramOccurrence);
        } catch (Exception e) {
            throw new InitializationException("An error occurred while classifying a test spot using the co-occurrence " +
                    "based spot selector. This is most probably caused by an outdated spot selector model. Please " +
                    "check the spot selector models defined 'org.dbpedia.spotlight.spot.cooccurrence.classifier.*'.", e);
        }
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

		//ngramClassifier.setVerboseMode(true);                                         f
		//unigramClassifier.setVerboseMode(true);
		List<String> decisions = new LinkedList<String>();

		for(SurfaceFormOccurrence surfaceFormOccurrence : surfaceFormOccurrences) {

            if (surfaceFormOccurrence.surfaceForm().name().trim().length()==0) {
                LOG.warn("I have an occurrence with empty surface form. :-O Ignoring.");
                LOG.error(surfaceFormOccurrence);
                continue;
            }

            if (! (surfaceFormOccurrence.context() instanceof TaggedText)) { //FIXME added this to avoid breaking, but code below will never run if we don't pass the taggedtext
                LOG.error(String.format("SurfaceFormOccurrence did not contain TaggedText. Cannot apply %s",this.getClass()));
				
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

        if (LOG.isDebugEnabled())
            for (String decision : decisions) {
                LOG.debug(decision);
            }
		
		return selectedOccurrences;
	}

}
