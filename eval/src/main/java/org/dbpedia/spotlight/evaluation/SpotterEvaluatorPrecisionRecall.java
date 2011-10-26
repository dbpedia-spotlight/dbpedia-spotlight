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

package org.dbpedia.spotlight.evaluation;

import com.aliasi.sentences.IndoEuropeanSentenceModel;
import net.sf.json.JSONException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.exceptions.SpottingException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.spot.*;
import org.dbpedia.spotlight.spot.cooccurrence.classification.SpotClass;
import org.dbpedia.spotlight.spot.cooccurrence.training.AnnotatedDataset;
import org.dbpedia.spotlight.spot.cooccurrence.training.AnnotatedSurfaceFormOccurrence;
import org.dbpedia.spotlight.spot.lingpipe.LingPipeSpotter;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeFactory;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeTaggedTokenProvider;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * Evaluator for {@link org.dbpedia.spotlight.spot.Spotter}s (and spot selectors).
 *
 * @author pablomendes (based on Spotter Evaluator from Joachim Daiber, modified for precision/recall calculations)
 * @author Joachim Daiber
 */
public class SpotterEvaluatorPrecisionRecall {

    private final static Log LOG = LogFactory.getLog(SpotterEvaluatorPrecisionRecall.class);

	public static void main(String[] args) throws IOException, JSONException, ConfigurationException, InitializationException, org.json.JSONException {

        StringBuffer latexTable = new StringBuffer();

		SpotlightConfiguration configuration = new SpotlightConfiguration("conf/dev.properties");

        LingPipeFactory lingPipeFactory = new LingPipeFactory(new File(configuration.getTaggerFile()), new IndoEuropeanSentenceModel());

        LOG.info("Reading gold standard.");
        AnnotatedDataset evaluationCorpus =
				new AnnotatedDataset(new File("/home/pablo/eval/csaw/original"),
						AnnotatedDataset.Format.CSAW, lingPipeFactory);

        LOG.info(String.format("Read %s annotations.",evaluationCorpus.getInstances().size()));


		/**
		 * Base:
		 */
		SelectorResult baseResult = getDatasetBaseResult(evaluationCorpus);
		LOG.info(baseResult);

        LOG.info("Reformatting.");
        Map<SurfaceFormOccurrence, AnnotatedSurfaceFormOccurrence> goldSurfaceFormOccurrences = new HashMap<SurfaceFormOccurrence, AnnotatedSurfaceFormOccurrence>();
		for(AnnotatedSurfaceFormOccurrence annotatedSurfaceFormOccurrence : evaluationCorpus.getInstances()) {
            SurfaceFormOccurrence sfo = annotatedSurfaceFormOccurrence.toSurfaceFormOccurrence();
            goldSurfaceFormOccurrences.put(sfo, annotatedSurfaceFormOccurrence);
            //goldSurfaceFormOccurrences.put(getNameVariation(sfo), annotatedSurfaceFormOccurrence);
        }
        List<Text> documents = evaluationCorpus.getTexts();

		/**
		 * No selection:
         */
		Spotter spotter = new LingPipeSpotter(new File(configuration.getSpotterConfiguration().getSpotterFile()));
        spotter.setName("Lexicon-based $L$*            ");
        //SelectorResult spotterBaseResult = getSelectorResult(spotter, evaluationCorpus);
        SelectorResult spotterBaseResult = evaluatePrecisionRecall(spotter, documents, goldSurfaceFormOccurrences);
		spotterBaseResult.printResult(baseResult);
        latexTable.append(spotterBaseResult.getLatexResult(baseResult));


		/**
		 * Advanced Spotter:
		 */
        Spotter noCommonSpotter = SpotterWithSelector.getInstance(
                spotter,
                new CoOccurrenceBasedSelector(configuration.getSpotterConfiguration()),
                new LingPipeTaggedTokenProvider(lingPipeFactory)
        );
        noCommonSpotter.setName("No common words $L$-CW*       ");
		SelectorResult selectorResultCoOc = evaluatePrecisionRecall(noCommonSpotter, documents, goldSurfaceFormOccurrences);
        //SelectorResult selectorResultCoOc = evaluatePrecisionRecallWithNameVariation(noCommonSpotter, documents, goldSurfaceFormOccurrences);
		selectorResultCoOc.printResult(baseResult);
        latexTable.append(selectorResultCoOc.getLatexResult(baseResult));


		/**
		 * At least one noun:
		 */
		Spotter npSpotter = SpotterWithSelector.getInstance(
				spotter,
				new AtLeastOneNounSelector(),
				new LingPipeTaggedTokenProvider(lingPipeFactory)
		);
        npSpotter.setName("At least one noun $L \\cap$ NP*");
		SelectorResult selectorResultOneNoun = evaluatePrecisionRecall(npSpotter, documents, goldSurfaceFormOccurrences);
		//SelectorResult selectorResultOneNoun = evaluatePrecisionRecallWithNameVariation(npSpotter, documents, goldSurfaceFormOccurrences);
		selectorResultOneNoun.printResult(baseResult);
        latexTable.append(selectorResultOneNoun.getLatexResult(baseResult));

        /**
         * NER
         */
        Spotter neSpotter = new NESpotter(configuration.getSpotterConfiguration().getOpenNLPModelDir());
        neSpotter.setName("NER                           ");
        SelectorResult selectorResultNE = evaluatePrecisionRecall(neSpotter, documents, goldSurfaceFormOccurrences);
        selectorResultNE.printResult(baseResult);
        latexTable.append(selectorResultNE.getLatexResult(baseResult));


        /**
         * OpenNLP
         */
        Spotter onlpSpotter = new OpenNLPNGramSpotter(configuration.getSpotterConfiguration().getOpenNLPModelDir());
        onlpSpotter.setName("NER+NP+NG                     ");
        SelectorResult selectorResultNGram = evaluatePrecisionRecall(onlpSpotter, documents, goldSurfaceFormOccurrences);
        selectorResultNGram.printResult(baseResult);
        latexTable.append(selectorResultNGram.getLatexResult(baseResult));

        /**
         * OpenNLP
         */
        Spotter onlpNoCommonSpotter = SpotterWithSelector.getInstance(
                onlpSpotter,
                new CoOccurrenceBasedSelector(configuration.getSpotterConfiguration()),
                new LingPipeTaggedTokenProvider(lingPipeFactory)
        );
        onlpNoCommonSpotter.setName("NER+NP+NG-CW                  ");
        SelectorResult selectorResultOnlpNoCommon = evaluatePrecisionRecall(onlpNoCommonSpotter, documents, goldSurfaceFormOccurrences);
        selectorResultOnlpNoCommon.printResult(baseResult);
        latexTable.append(selectorResultOnlpNoCommon.getLatexResult(baseResult));

        /**
         * OpenNLP Chunker
         */
//        File surfaceFormDictionaryFile = new File();
//        Spotter onlpChunksSpotter = new OpenNLPChunkerSpotter(OpenNLPUtil.OpenNlpModels.SentenceModel.file(),
//                OpenNLPUtil.OpenNlpModels.TokenizerModel.file(),
//                OpenNLPUtil.OpenNlpModels.POSModel.file(),
//                OpenNLPUtil.OpenNlpModels.ChunkModel.file(),
//                surfaceFormDictionaryFile);
//        onlpChunksSpotter.setName("NP+NG                           ");
//        SelectorResult selectorResultOnlpChunker = evaluatePrecisionRecall(onlpChunksSpotter, documents, goldSurfaceFormOccurrences);
//        selectorResultOnlpChunker.printResult(baseResult);
//        latexTable.append(selectorResultOnlpChunker.getLatexResult(baseResult));


        /**
          * Kea
          */
         Spotter keaSpotter1 = new KeaSpotter("/data/spotlight/3.7/kea/keaModel-1-3-1", 1000, -1);
         keaSpotter1.setName("Kea_{>0}                      ");
         SelectorResult keaResults1 = evaluatePrecisionRecall(keaSpotter1, documents, goldSurfaceFormOccurrences);
         keaResults1.printResult(baseResult);
         latexTable.append(keaResults1.getLatexResult(baseResult));

         Spotter keaSpotter2 = new KeaSpotter("/data/spotlight/3.7/kea/keaModel-1-3-1", 1000, 0.015);
         keaSpotter2.setName("Kea_{>0.015}                  ");
         SelectorResult keaResults2 = evaluatePrecisionRecall(keaSpotter2, documents, goldSurfaceFormOccurrences);
         keaResults2.printResult(baseResult);
         latexTable.append(keaResults2.getLatexResult(baseResult));

        Spotter keaSpotter3 = new KeaSpotter("/data/spotlight/3.7/kea/keaModel-1-3-1", 1000, 0.075);
        keaSpotter3.setName("Kea_{>0.075}                  ");
        SelectorResult keaResults3 = evaluatePrecisionRecall(keaSpotter3, documents, goldSurfaceFormOccurrences);
        keaResults3.printResult(baseResult);
        latexTable.append(keaResults3.getLatexResult(baseResult));

        Spotter keaSpotter4 = new KeaSpotter("/data/spotlight/3.7/kea/keaModel-1-3-1", 1000, 0.15);
        keaSpotter4.setName("Kea_{>0.15}                  ");
        SelectorResult keaResults4 = evaluatePrecisionRecall(keaSpotter4, documents, goldSurfaceFormOccurrences);
        keaResults4.printResult(baseResult);
        latexTable.append(keaResults4.getLatexResult(baseResult));

        Spotter keaSpotter5 = new KeaSpotter("/data/spotlight/3.7/kea/keaModel-1-3-1", 1000, 0.3);
        keaSpotter5.setName("Kea_{>0.3}                  ");
        SelectorResult keaResults5 = evaluatePrecisionRecall(keaSpotter5, documents, goldSurfaceFormOccurrences);
        keaResults5.printResult(baseResult);
        latexTable.append(keaResults5.getLatexResult(baseResult));

        System.out.println(latexTable);

        LOG.info("Done.");

	}

    private static SelectorResult getSelectorResult(Spotter spotter, AnnotatedDataset evaluationCorpus) {
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
            SurfaceFormOccurrence sfo = annotatedSurfaceFormOccurrence.toSurfaceFormOccurrence();
			if(extractedSurfaceFormOccurrences.contains(sfo)) {
                SpotClass c = annotatedSurfaceFormOccurrence.getSpotClass();
                if (c.equals(SpotClass.common))
                    selectorResult.addCommon();
                else if (c.equals(SpotClass.valid))
                    selectorResult.addValid();
                else if (c.equals(SpotClass.part))
                    selectorResult.addPart();
                else {
                    System.out.println("WTF?");
                }
			} else {
			    selectorResult.addBlank();
                //LOG.info(sfo);
			}
            selectorResult.addTotal();
		}

		return selectorResult;
	}

	/**
	 * Evaluates precision and recall for A and NA. Iterates over spots, checks if they are in gold.
	 */
	private static SelectorResult evaluatePrecisionRecall(Spotter spotter, List<Text> documents, Map<SurfaceFormOccurrence, AnnotatedSurfaceFormOccurrence> goldSurfaceFormOccurrences) {
		SelectorResult selectorResult = new SelectorResult(spotter.getName());

        long start = System.currentTimeMillis();
        for(Text text : documents) {
            List<SurfaceFormOccurrence> extractedSurfaceFormOccurrences = null;
            try {
                extractedSurfaceFormOccurrences = spotter.extract(text);
            } catch (SpottingException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            for (SurfaceFormOccurrence sfo: extractedSurfaceFormOccurrences) {
                if(goldSurfaceFormOccurrences.containsKey(sfo)) {
                    SpotClass c = goldSurfaceFormOccurrences.get(sfo).getSpotClass();
                    if (c.equals(SpotClass.common))
                        selectorResult.addCommon();
                    else if (c.equals(SpotClass.valid))
                        selectorResult.addValid();
                    else if (c.equals(SpotClass.part))
                        selectorResult.addPart();
                    else {
                        System.out.println("WTF?");
                    }
                } else {
                    //Annotation not found
                    selectorResult.addBlank();
                    LOG.info(sfo);
                }
                selectorResult.addTotal();
            }
        }
        long end = System.currentTimeMillis();
        selectorResult.setTime(end - start);

		return selectorResult;
	}

    /**
     * Since CSAW disagrees with Wikipedia on the boundaries of entities, we created this method to alleviate the impact of this small detail.
     * This method should only be used with dictionary-based spotters
     * It checks if the spot starts with an article, and also tries it without the article
     */
    private static SelectorResult evaluatePrecisionRecallWithNameVariation(Spotter spotter, List<Text> documents, Map<SurfaceFormOccurrence, AnnotatedSurfaceFormOccurrence> goldSurfaceFormOccurrences) {
        SelectorResult selectorResult = new SelectorResult(spotter.getName());

        for(Text text : documents) {
            List<SurfaceFormOccurrence> extractedSurfaceFormOccurrences = null;
            try {
                extractedSurfaceFormOccurrences = spotter.extract(text);
            } catch (SpottingException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            for (SurfaceFormOccurrence sfo: extractedSurfaceFormOccurrences) {
                if(goldSurfaceFormOccurrences.containsKey(sfo)) {
                    updateCount(goldSurfaceFormOccurrences.get(sfo), selectorResult);
                } else { //Annotation not found. try variation
                    if (goldSurfaceFormOccurrences.containsKey(getNameVariation(sfo))) {
                        updateCount(goldSurfaceFormOccurrences.get(getNameVariation(sfo)), selectorResult);
                    } else { //Annotation still not found
                        selectorResult.addBlank();
                    }
                }
                selectorResult.addTotal();
            }
        }
        return selectorResult;
    }

    private static void updateCount(AnnotatedSurfaceFormOccurrence asfo, SelectorResult selectorResult) {
        SpotClass c = asfo.getSpotClass();
        if (c.equals(SpotClass.common))
            selectorResult.addCommon();
        else if (c.equals(SpotClass.valid))
            selectorResult.addValid();
        else if (c.equals(SpotClass.part))
            selectorResult.addPart();
        else {
            System.out.println("WTF?");
        }
    }


    private static SurfaceFormOccurrence getNameVariation(SurfaceFormOccurrence sfo) {
        String sf = sfo.surfaceForm().name();
        int offsetFromStart = 0;
        int offsetFromEnd = 0;
        if (sf.toLowerCase().startsWith("the ")) {
            offsetFromStart = 4;
        } else if (sf.toLowerCase().startsWith("a ")) {
            offsetFromStart = 2;
        } else if (sf.toLowerCase().startsWith("an ")) {
            offsetFromStart = 3;
        }
        if (sf.toLowerCase().endsWith("[\\.\\,]")) {
            offsetFromEnd = 1;
        }
        int end = sfo.surfaceForm().name().length()-1;
        SurfaceForm variation = new SurfaceForm(sf.substring(offsetFromStart, end-offsetFromEnd).trim());
        return new SurfaceFormOccurrence(variation, sfo.context(), sfo.textOffset()+offsetFromStart, sfo.provenance(), sfo.spotProb());
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
                default:
                    baseResult.addBlank();
            }
            baseResult.addTotal();
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
        int blank = 0; // number of spots that were neither valid, common or part
        int total = 0; // should be the sum of valid, common, part and blank?

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

        /**
         * When it' s neither valid, commmon, nor part
         */
        public void addBlank() {
			blank++;
		}

        public void addTotal() {
			total++;
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

        public float getTotal() {
			return total;
		}

		@Override
		public String toString() {
			return "SelectorResult[" +
					"valid=" + valid +
					", common=" + common +
					", part=" + part +
                    ", blank=" + blank +
					"] with Spotter " + this.name;
		}
		

		public void printResult(SelectorResult baseResult) {
            SelectorResult myMethod = this;
            SelectorResult goldStandard = baseResult;
			System.err.println("\n\n\n\nResult for Spotter '" + name() + "' compared with '" + baseResult.name() + "'");
			System.err.println("\nSpotting took " + time + "ms, " +
					String.format("%1.2f", time / (float) (baseResult.part + baseResult.valid + baseResult.common))
					+ "ms per spot");

			System.err.println(
				" part:" + part + " (" + String.format("%1.2f", (((part/baseResult.getPart())) * 100)) + "%)" +
				", common:" + common + " (" + String.format("%1.2f", (((common/baseResult.getCommon())) * 100)) + "%)" +
				", valid:" + valid + " (" + String.format("%1.2f", (((valid/baseResult.getValid())) * 100)) + "%)" +
                ", blank:" + blank);

            int spotted = part + common + valid + blank;
            System.err.println(String.format("Precision: %s/%s = %1.2f",valid,myMethod.getTotal(),    new Double(valid)/myMethod.getTotal()));
            System.err.println(String.format("Recall: %s/%s = %1.2f",   valid,goldStandard.getTotal(),new Double(valid)/goldStandard.getValid()));

            System.err.println(String.format("Total: %s = Sum: %s ?", total, valid+common+part+blank));
		}

        public String getLatexResult(SelectorResult goldStandard) {
            SelectorResult myMethod = this;

            float timePerSpot = time / (float) (myMethod.getTotal());
            double precision = (new Double(valid)/myMethod.getTotal()) * 100;
            double recall = (new Double(valid)/goldStandard.getValid()) * 100;
            double precisionNA = (common/goldStandard.getCommon()) * 100;
            long spotted = (long) myMethod.getTotal();
            long missed = (long) (goldStandard.getTotal() - myMethod.getValid());

            StringBuffer b = new StringBuffer();
            b.append("% spotter & P & R & $P_{NA}$ & N_{spotted} & N_{missed} & time per spot \\\\ \n");
            b.append(String.format("  %s & %1.2f & %1.2f & %1.2f & %d & %d & %1.4f \\\\ \n", name(), precision, recall, precisionNA, spotted, missed,  timePerSpot));
            return b.toString();
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
