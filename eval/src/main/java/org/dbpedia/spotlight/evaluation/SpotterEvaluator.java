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
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.evaluation;

import com.aliasi.sentences.IndoEuropeanSentenceModel;
import net.sf.json.JSONException;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.exceptions.SpottingException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.spot.*;
import org.dbpedia.spotlight.spot.cooccurrence.training.AnnotatedDataset;
import org.dbpedia.spotlight.spot.cooccurrence.training.AnnotatedSurfaceFormOccurrence;
import org.dbpedia.spotlight.spot.lingpipe.LingPipeSpotter;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeFactory;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeTaggedTokenProvider;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


/**
 * Evaluator for {@link Spotter}s (and spot selectors).
 *
 * @author Joachim Daiber
 */
public class SpotterEvaluator {

	public static void main(String[] args) throws IOException, ConfigurationException, JSONException, InitializationException, SpottingException, org.json.JSONException {

		SpotlightConfiguration configuration = new SpotlightConfiguration("conf/server.properties");

        LingPipeFactory lingPipeFactory = new LingPipeFactory(new File(configuration.getTaggerFile()), new IndoEuropeanSentenceModel());

		//AnnotatedDataset evaluationCorpus = new AnnotatedDataset(new File("/Users/jodaiber/Documents/workspace/ba/" +
		//		"BachelorThesis/01 Evaluation/02 Annotation/Software/custom/src/annotation/final.test.json"),
		//		AnnotatedDataset.Format.JSON, spotlightFactory);
//
		AnnotatedDataset evaluationCorpus =
				new AnnotatedDataset(new File("/home/pablo/eval/csaw/original"),
						AnnotatedDataset.Format.CSAW, lingPipeFactory);
		
		/**
		 * Base:
		 */
		SelectorResult baseResult = getDatasetBaseResult(evaluationCorpus);
		System.out.println(baseResult);


		/**
		 * No selection:
		 */
		Spotter spotter = new LingPipeSpotter(new File(configuration.getSpotterConfiguration().getSpotterFile()), configuration.getAnalyzer());
		SelectorResult spotterBaseResult = getSelectorResult(spotter, evaluationCorpus);
		spotterBaseResult.printResult(baseResult);
		
		
		/**
		 * Advanced Spotter:
		 */
		Spotter spotterWithSelector = SpotterWithSelector.getInstance(
				spotter,
				new CoOccurrenceBasedSelector(configuration.getSpotterConfiguration()),
				new LingPipeTaggedTokenProvider(lingPipeFactory)
		);

		SelectorResult selectorResultCoOc = getSelectorResult(spotterWithSelector, evaluationCorpus);
		selectorResultCoOc.printResult(baseResult);

		
		/**
		 * At least one noun:
		 */
		spotterWithSelector = SpotterWithSelector.getInstance(
				spotter,
				new AtLeastOneNounSelector(),
				new LingPipeTaggedTokenProvider(lingPipeFactory)
		);

		SelectorResult selectorResultOneNoun = getSelectorResult(spotterWithSelector, evaluationCorpus);
		selectorResultOneNoun.printResult(baseResult);


		spotterWithSelector = SpotterWithSelector.getInstance(
				spotter,
				new RandomSelector(spotterBaseResult.valid, spotterBaseResult.common)
		);

		SelectorResult selectorResultRandom = getSelectorResult(spotterWithSelector, evaluationCorpus);
		selectorResultRandom.printResult(baseResult);
	}


	/**
	 * Measure the overlap between the annotated dataset and the
	 * results produced by the Spotter on the texts in the texts
	 * in the annotated dataset.
	 *
	 * @param spotter Spotter that is to be tested
	 * @param evaluationCorpus annotated dataset for testing
	 * @return Overlap between annotated dataset and Spotter result per
	 * candidate class
	 */
	private static SelectorResult getSelectorResult(Spotter spotter, AnnotatedDataset evaluationCorpus)  {
		SelectorResult selectorResult = new SelectorResult(spotter.getName());

		Set<SurfaceFormOccurrence> extractedSurfaceFormOccurrences = new HashSet<SurfaceFormOccurrence>();

		long start = System.currentTimeMillis();
		for(Text text : evaluationCorpus.getTexts())
            try {
                extractedSurfaceFormOccurrences.addAll(spotter.extract(text));
            } catch (SpottingException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        long end = System.currentTimeMillis();
		selectorResult.setTime(end - start);

		for(AnnotatedSurfaceFormOccurrence annotatedSurfaceFormOccurrence : evaluationCorpus.getInstances()) {
			if(extractedSurfaceFormOccurrences.contains(annotatedSurfaceFormOccurrence.toSurfaceFormOccurrence())){
				switch (annotatedSurfaceFormOccurrence.getSpotClass()){
					case common:
						selectorResult.addCommon();
						break;
					case valid:
						selectorResult.addValid();
						break;
					case part:
						selectorResult.addPart();
						break;
				}

			}else{
				//Annotation not found

			}
		}

		return selectorResult;
	}

	
	/**
	 * Retrieve the base distribution for valid and common results from the annotated
	 * dataset.
	 *
	 * @param evaluationCorpus corpus for evaluation
	 * @return base result
	 */
	private static SelectorResult getDatasetBaseResult(AnnotatedDataset evaluationCorpus) {
		SelectorResult baseResult = new SelectorResult("Evaluation corpus base");

		for(AnnotatedSurfaceFormOccurrence annotatedSurfaceFormOccurrence : evaluationCorpus.getInstances()) {

				switch (annotatedSurfaceFormOccurrence.getSpotClass()){
					case common:
						baseResult.addCommon();
						break;
					case valid:
						baseResult.addValid();
						break;
					case part:
						baseResult.addPart();
						break;
				}
		}

		return baseResult;
	}


	private static class SelectorResult {

		private long time;

		public SelectorResult(String name) {
			this.name = name;
		}

		private String name;
		int valid = 0;
		int common = 0;
		int part = 0;

		public String name() {
			return name;
		}

		public void addValid() {
			valid++;
		}

		public void addCommon() {
			common++;
		}

		public void addPart() {
			part++;
		}

		public float getValid() {
			return valid;
		}

		public float getCommon() {
			return common;
		}

		public float getPart() {
			return part;
		}


		@Override
		public String toString() {
			return "SelectorResult[" +
					"valid=" + valid +
					", common=" + common +
					", part=" + part +
					"] with Spotter " + this.name;
		}
		

		public void printResult(SelectorResult baseResult) {

			System.out.println("\n\n\n\nResult for Spotter '" + name() + "' compared with '" + baseResult.name() + "'");
			System.out.println("\nSpotting took " + time + "ms, " +
					String.format("%1.2f", time / (float) (baseResult.part + baseResult.valid + baseResult.common))
					+ "ms per spot");

			System.out.println(
				" part:" + part + " (" + String.format("%1.2f", (((part/baseResult.getPart())) * 100)) + "%)" +
				", common:" + common + " (" + String.format("%1.2f", (((common/baseResult.getCommon())) * 100)) + "%)" +
				", valid:" + valid + " (" + String.format("%1.2f", (((valid/baseResult.getValid())) * 100)) + "%)");
		}

		
		/**
		 * Set the spotting time in ms.
		 *
		 * @param time time for the entire spotting process in ms
		 */
		public void setTime(long time) {
			this.time = time;
		}

		
	}
}
