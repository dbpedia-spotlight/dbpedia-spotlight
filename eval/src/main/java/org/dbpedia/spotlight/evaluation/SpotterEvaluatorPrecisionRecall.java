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
import org.dbpedia.spotlight.spot.opennlp.OpenNLPChunkerSpotter;
import org.dbpedia.spotlight.spot.opennlp.ProbabilisticSurfaceFormDictionary;
import org.dbpedia.spotlight.spot.opennlp.SurfaceFormDictionary;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeFactory;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeTaggedTokenProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
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


        evaluate(documents, goldSurfaceFormOccurrences, baseResult, lingPipeFactory, configuration);

        LOG.info("Done.");

	}

    private static void evaluate(List<Text> documents,
                                 Map<SurfaceFormOccurrence, AnnotatedSurfaceFormOccurrence> goldSurfaceFormOccurrences,
                                 SelectorResult baseResult,
                                 LingPipeFactory lingPipeFactory,
                                 SpotlightConfiguration configuration) throws InitializationException, ConfigurationException {

        StringBuffer latexTable = new StringBuffer();


		/**
		 * No selection:
         */
        File lexSpotterGoldFile = new File("/home/pablo/eval/csaw/gold/surfaceForms.set.spotterDictionary");
        Spotter lexSpotterGold = new LingPipeSpotter(lexSpotterGoldFile , configuration.getAnalyzer());
        lexSpotterGold.setName("\\lexspot{gold}        ");
        latexTable.append(getLatexTableRow(lexSpotterGold, documents, goldSurfaceFormOccurrences,baseResult));

        File lexSpotterT3File = new File("/home/pablo/web/dbpedia36data/2.9.3/surface_forms-Wikipedia-TitRedDis.thresh3.spotterDictionary");
        Spotter lexSpotterT3 = new LingPipeSpotter(lexSpotterT3File, configuration.getAnalyzer());
        lexSpotterT3.setName("\\lexspot{>3}        ");
        latexTable.append(getLatexTableRow(lexSpotterT3, documents, goldSurfaceFormOccurrences,baseResult));

		File lexSpotterT10File = new File("/home/pablo/web/dbpedia36data/2.9.3/surface_forms-Wikipedia-TitRedDis.uriThresh10.tsv.spotterDictionary");
        Spotter lexSpotterT10 = new LingPipeSpotter(lexSpotterT10File, configuration.getAnalyzer());
        lexSpotterT10.setName("\\lexspot{>10}        ");
        latexTable.append(getLatexTableRow(lexSpotterT10, documents, goldSurfaceFormOccurrences,baseResult));

        File lexSpotterT75File = new File("/home/pablo/web/dbpedia36data/2.9.3/surface_forms-Wikipedia-TitRedDis.uriThresh75.tsv.spotterDictionary");
        Spotter lexSpotterT75 = new LingPipeSpotter(lexSpotterT75File, configuration.getAnalyzer());
        lexSpotterT75.setName("\\lexspot{>75}        ");
        latexTable.append(getLatexTableRow(lexSpotterT75, documents, goldSurfaceFormOccurrences,baseResult));

		/**
		 * At least one noun:
		 */
		Spotter npSpotter = SpotterWithSelector.getInstance(
				lexSpotterT3,
				new AtLeastOneNounSelector(),
				new LingPipeTaggedTokenProvider(lingPipeFactory)
		);
        npSpotter.setName("\\atLeastOneNoun");
		latexTable.append(getLatexTableRow(npSpotter, documents, goldSurfaceFormOccurrences,baseResult));

        /**
         * OpenNLP Chunker
         */
        String openNLPDir = "/data/spotlight/3.7/opennlp/english/";
        String i18nLanguageCode = "en";
        File stopwords = new File("data/stopwords/stopwords_en.txt");



        SurfaceFormDictionary sfDictProbThreshGold = ProbabilisticSurfaceFormDictionary.fromLingPipeDictionaryFile(lexSpotterGoldFile,false);
        Spotter onlpChunksSpotterGold = OpenNLPChunkerSpotter.fromDir(openNLPDir,i18nLanguageCode,sfDictProbThreshGold,stopwords);
        onlpChunksSpotterGold.setName("\\joNPL{gold}                  ");
        latexTable.append(getLatexTableRow(onlpChunksSpotterGold, documents, goldSurfaceFormOccurrences,baseResult));

        //File sfDictThresh3 = new File("/home/pablo/workspace/spotlight/index/output/surfaceForms-fromOccs-thresh3-TRD.set");
        //SurfaceFormDictionary sfDictProbThresh3 = ProbabilisticSurfaceFormDictionary.fromFile(sfDictThresh3, false);
        SurfaceFormDictionary sfDictProbThresh3 = ProbabilisticSurfaceFormDictionary.fromLingPipeDictionaryFile(lexSpotterT3File,false);
        Spotter onlpChunksSpotter3 = OpenNLPChunkerSpotter.fromDir(openNLPDir,i18nLanguageCode,sfDictProbThresh3,stopwords);
        onlpChunksSpotter3.setName("\\joNPL{>3}                  ");
        latexTable.append(getLatexTableRow(onlpChunksSpotter3, documents, goldSurfaceFormOccurrences,baseResult));

        //File sfDictThresh10 = new File("/home/pablo/workspace/spotlight/index/output/surfaceForms-fromOccs-thresh10-TRD.set");
        //SurfaceFormDictionary sfDictProbThresh10 = ProbabilisticSurfaceFormDictionary.fromFile(sfDictThresh10, false);
        SurfaceFormDictionary sfDictProbThresh10 = ProbabilisticSurfaceFormDictionary.fromLingPipeDictionaryFile(lexSpotterT10File,false);
        Spotter onlpChunksSpotter10 = OpenNLPChunkerSpotter.fromDir(openNLPDir,i18nLanguageCode,sfDictProbThresh10,stopwords);
        onlpChunksSpotter10.setName("\\joNPL{>10}                 ");
        latexTable.append(getLatexTableRow(onlpChunksSpotter10, documents, goldSurfaceFormOccurrences,baseResult));

        //File sfDictThresh75 = new File("/home/pablo/workspace/spotlight/index/output/surfaceForms-fromOccs-thresh75.tsv");
        //SurfaceFormDictionary sfDictProbThresh75 = ProbabilisticSurfaceFormDictionary.fromFile(sfDictThresh75, false);
        SurfaceFormDictionary sfDictProbThresh75 = ProbabilisticSurfaceFormDictionary.fromLingPipeDictionaryFile(lexSpotterT75File,false);
        Spotter onlpChunksSpotter75 = OpenNLPChunkerSpotter.fromDir(openNLPDir,i18nLanguageCode,sfDictProbThresh75,stopwords);
        onlpChunksSpotter75.setName("\\joNPL{>75}                 ");
        latexTable.append(getLatexTableRow(onlpChunksSpotter75, documents, goldSurfaceFormOccurrences,baseResult));

        /**
         * No common words.
         */
        Spotter noCommonSpotter = SpotterWithSelector.getInstance(
                lexSpotterT3,
                new CoOccurrenceBasedSelector(configuration.getSpotterConfiguration()),
                new LingPipeTaggedTokenProvider(lingPipeFactory)
        );
        noCommonSpotter.setName("\\cw       ");
        latexTable.append(getLatexTableRow(noCommonSpotter, documents, goldSurfaceFormOccurrences,baseResult));

        /**
         * Kea
         */
        Spotter keaSpotter1 = new KeaSpotter("/data/spotlight/3.7/kea/keaModel-1-3-1", 1000, -1);
        keaSpotter1.setName("$Kea_{>0}$                      ");
        latexTable.append(getLatexTableRow(keaSpotter1, documents, goldSurfaceFormOccurrences,baseResult));

//        Spotter keaSpotter2 = new KeaSpotter("/data/spotlight/3.7/kea/keaModel-1-3-1", 1000, 0.015);
//        keaSpotter2.setName("$Kea_{>0.015}$                  ");
//        latexTable.append(getLatexTableRow(keaSpotter2, documents, goldSurfaceFormOccurrences,baseResult));
//
//        Spotter keaSpotter3 = new KeaSpotter("/data/spotlight/3.7/kea/keaModel-1-3-1", 1000, 0.075);
//        keaSpotter3.setName("$Kea_{>0.075}$                  ");
//        latexTable.append(getLatexTableRow(keaSpotter3, documents, goldSurfaceFormOccurrences,baseResult));
//
//        Spotter keaSpotter4 = new KeaSpotter("/data/spotlight/3.7/kea/keaModel-1-3-1", 1000, 0.15);
//        keaSpotter4.setName("$Kea_{>0.15}$                  ");
//        latexTable.append(getLatexTableRow(keaSpotter4, documents, goldSurfaceFormOccurrences,baseResult));
//
//        Spotter keaSpotter5 = new KeaSpotter("/data/spotlight/3.7/kea/keaModel-1-3-1", 1000, 0.3);
//        keaSpotter5.setName("$Kea_{>0.3}$                  ");
//        latexTable.append(getLatexTableRow(keaSpotter5,documents,goldSurfaceFormOccurrences,baseResult));

        /**
         * NER
         */
        Spotter neSpotter = new NESpotter(configuration.getSpotterConfiguration().getOpenNLPModelDir(), configuration.getI18nLanguageCode(), configuration.getSpotterConfiguration().getOpenNLPModelsURI());
        neSpotter.setName("\\ner                           ");
        latexTable.append(getLatexTableRow(neSpotter, documents, goldSurfaceFormOccurrences,baseResult));

        /**
         * NER+NP
         */
        Spotter onlpSpotter = new OpenNLPNGramSpotter(configuration.getSpotterConfiguration().getOpenNLPModelDir()+ "/"  + configuration.getLanguage().toLowerCase(), configuration.getI18nLanguageCode());
        onlpSpotter.setName("\\nerNP                     ");
        latexTable.append(getLatexTableRow(onlpSpotter, documents, goldSurfaceFormOccurrences,baseResult));

        /**
         * NER+NP+NG-CW
         */
        Spotter onlpNoCommonSpotter = SpotterWithSelector.getInstance(
                onlpSpotter,
                new CoOccurrenceBasedSelector(configuration.getSpotterConfiguration()),
                new LingPipeTaggedTokenProvider(lingPipeFactory)
        );
        onlpNoCommonSpotter.setName("NER+NP+NG-CW                  ");
        latexTable.append(getLatexTableRow(onlpNoCommonSpotter, documents, goldSurfaceFormOccurrences,baseResult));

        System.out.println(latexTable);
    }

    private static String getLatexTableRow(Spotter spotter,
                                           List<Text> documents,
                                           Map<SurfaceFormOccurrence, AnnotatedSurfaceFormOccurrence> goldSurfaceFormOccurrences,
                                           SelectorResult baseResult) {
		SelectorResult spotterBaseResult = evaluatePrecisionRecall(spotter, documents, goldSurfaceFormOccurrences);
		//SelectorResult spotterBaseResult = evaluatePrecisionRecallWithNameVariation(spotter, documents, goldSurfaceFormOccurrences);
		spotterBaseResult.printResult(baseResult);
        return spotterBaseResult.getLatexResult(baseResult);
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

        PrintStream incorrectSpotsWriter = System.err;
        try {
            incorrectSpotsWriter = new PrintStream(new File("data",spotter.getName().trim().replaceAll("[^A-Za-z]","").concat(".incorrectSpots")));
        } catch (FileNotFoundException e) {
            LOG.error("Cannot write to incorrectSpots.");
        }

        Set<SurfaceFormOccurrence> found = new HashSet<SurfaceFormOccurrence>();
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
                    found.add(sfo);
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
                    incorrectSpotsWriter.println(sfo.surfaceForm().name());
                }
                selectorResult.addTotal();
            }
        }
        long end = System.currentTimeMillis();
        selectorResult.setTime(end - start);

        incorrectSpotsWriter.close();

        PrintStream notFoundWriter = System.err;
        try {
            notFoundWriter = new PrintStream(new File("data",spotter.getName().trim().replaceAll("[^A-Za-z]","").concat(".correctSpotNotFound")));
        } catch (FileNotFoundException e) {
            LOG.error("Cannot write to notFoundWriter.");
        }

        for (SurfaceFormOccurrence correctSfo: goldSurfaceFormOccurrences.keySet()) {
            if (!found.contains(correctSfo)) {
                notFoundWriter.println(String.format("%s \t %s", correctSfo.surfaceForm().name(), correctSfo.textOffset()));
            }
        }

        notFoundWriter.close();

		return selectorResult;
	}

    /**
     * Since CSAW disagrees with Wikipedia on the boundaries of entities, we created this method to alleviate the impact of this small detail.
     * This method should only be used with dictionary-based spotters
     * It checks if the spot starts with an article, and also tries it without the article
     */
    private static SelectorResult evaluatePrecisionRecallWithNameVariation(Spotter spotter,
                                                                           List<Text> documents,
                                                                           Map<SurfaceFormOccurrence, AnnotatedSurfaceFormOccurrence> goldSurfaceFormOccurrences) {
        SelectorResult selectorResult = new SelectorResult(spotter.getName());

        PrintStream spotsWriter = System.err;
        try {
            spotsWriter = new PrintStream(new File("data",spotter.getName().trim().replaceAll("[^A-Za-z]","").concat(".spots.tsv")));
        } catch (FileNotFoundException e) {
            LOG.error("Cannot write to spotsWriter.");
        }

        for(Text text : documents) {
            List<SurfaceFormOccurrence> extractedSurfaceFormOccurrences = null;
            try {
                extractedSurfaceFormOccurrences = spotter.extract(text);
            } catch (SpottingException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            printSpots(spotsWriter,text,extractedSurfaceFormOccurrences);
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
        spotsWriter.close();
        return selectorResult;
    }

    private static void printSpots(PrintStream writer, Text text, List<SurfaceFormOccurrence> extractedSurfaceFormOccurrences) {
        StringBuffer line = new StringBuffer();
        line.append(text.text().hashCode());
        line.append("\t");
        for (SurfaceFormOccurrence sfo: extractedSurfaceFormOccurrences) {
            line.append(sfo.surfaceForm());
            line.append(":");
            line.append(sfo.textOffset());
            line.append(",");
        }
        line.append("\n");
        writer.print(line.toString());
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
