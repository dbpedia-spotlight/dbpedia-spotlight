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

import au.com.bytecode.opencsv.CSVReader;
import org.apache.jcs.access.exception.CacheException;
import org.dbpedia.spotlight.annotate.DefaultParagraphAnnotator;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.spot.cooccurrence.training.AnnotatedDataset;
import org.json.JSONException;

import java.io.*;
import java.util.*;

/**
 * @author Joachim Daiber
 */
public class ExtractionEvaluator {

	public static void main(String[] args) throws IOException, ConfigurationException, JSONException, InputException, CacheException {

		SpotlightConfiguration configuration = new SpotlightConfiguration("conf/server.properties");
		SpotlightFactory spotlightFactory = new SpotlightFactory(configuration);

		AnnotatedDataset evaluationCorpus =
				new AnnotatedDataset(new File(args[0]),
						AnnotatedDataset.Format.CSAW, spotlightFactory);

		int tp = 0, fp = 0, fn = 0;

		Set<DBpediaResourceOccurrence> goldAnnotations = evaluationCorpus.toDBpediaResourceOccurrences();
		writeAnnotations(spotlightFactory, evaluationCorpus,
				new File(args[1]));

		/**
		 * Read all annotations made by a configuration of DBpedia Spotlight.
		 */
		List<List<DBpediaResourceOccurrence>> allAnnotatedTexts = readAnnotations(new File(args[1]),
			evaluationCorpus);

		/**
		 * Remove all
		 */
		Set<DBpediaResourceOccurrence> allAnnotatedOccurrences = new HashSet<DBpediaResourceOccurrence>();
		Set<Text> texts = new HashSet<Text>();
		for(List<DBpediaResourceOccurrence> annotatedOccurrenceText : allAnnotatedTexts) {
			if(annotatedOccurrenceText.size() == 0)
				continue;

			texts.add(annotatedOccurrenceText.get(0).context());
			allAnnotatedOccurrences.addAll(annotatedOccurrenceText);

			for(DBpediaResourceOccurrence dbpediaResourceOccurrence : annotatedOccurrenceText) {
				if(goldAnnotations.contains(dbpediaResourceOccurrence))
					tp++;
				else
					fp++;
			}
		}

		/**
		 * Remove all annotations whose texts have not been
		 * annotated.
		 */
		Set<DBpediaResourceOccurrence> goldAnnotationsForTexts =  new HashSet<DBpediaResourceOccurrence>();
		for(DBpediaResourceOccurrence goldAnnotation : goldAnnotations) {
			if(texts.contains(goldAnnotation.context())) {
				goldAnnotationsForTexts.add(goldAnnotation);
			}
		}


		System.out.println("Gold annotations: " + goldAnnotations.size());
		System.out.println("Gold annotations with annotated equivalent: " + goldAnnotationsForTexts.size());


		for(DBpediaResourceOccurrence goldAnnotation : goldAnnotationsForTexts) {
			if(!allAnnotatedOccurrences.contains(goldAnnotation))
				fn++;
		}


		System.out.println("TP: " + tp);
		System.out.println("FP: " + fp);
		System.out.println("FN: " + fn);
		
		float precision = (tp / (float) (tp + fn));
		System.out.println("P: " + precision);

		float recall = (tp / (float) (tp + fp));
		System.out.println("R: " + recall);
		System.out.println("F1: " + ((2 * precision * recall)  / (recall+precision)));


	}

	private static List<List<DBpediaResourceOccurrence>> readAnnotations(File folder, AnnotatedDataset evaluationCorpus) {

		File[] files = folder.listFiles();

		List<List<DBpediaResourceOccurrence>> allAnnotatedOccurrences
				= new LinkedList<List<DBpediaResourceOccurrence>>();

		for(File file : files) {

			if(!file.getName().endsWith(".tsv")) {
				continue;
			}

			int textID = Integer.parseInt(file.getName().replace(".tsv", ""));

			List<DBpediaResourceOccurrence> occurrencesInText = new ArrayList<DBpediaResourceOccurrence>();
			try {
				System.out.println("Read " + file);
				CSVReader csvReader = new CSVReader(new FileReader(file), '\t');

				System.out.println(evaluationCorpus.getTexts().size());
				Text text = evaluationCorpus.getTexts().get(textID - 1);
				System.out.println(text);

				String[] line;
				while((line = csvReader.readNext()) != null){

					DBpediaResource dbpediaResource = new DBpediaResource(line[1]);
					SurfaceForm surfaceForm = new SurfaceForm(line[2]);
					int offset = Integer.parseInt(line[4]);

					DBpediaResourceOccurrence dbpediaResourceOccurrence
							= new DBpediaResourceOccurrence(dbpediaResource, surfaceForm, text, offset);
					occurrencesInText.add(dbpediaResourceOccurrence);
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (IndexOutOfBoundsException e) {
				/*
				  A line in the CSV file could not be read, this is likely caused by the file not 
				  being wrote properly, so we will ignore it.
				 */
				System.out.println("Error reading file " + file + e);
				continue;
			} catch (NumberFormatException e) {
				/*
				  A line in the CSV file could not be read, this is likely caused by the file not
				  being wrote properly, so we will ignore it.
				 */
				System.out.println("Error reading file " + file + e);
				continue;
			}

			System.out.println("Read file " + file);
			allAnnotatedOccurrences.add(occurrencesInText);
		}

		return allAnnotatedOccurrences;
	}

	private static void writeAnnotations(SpotlightFactory spotlightFactory, AnnotatedDataset evaluationCorpus,
										 File annotationsFolder) throws IOException {


		DefaultParagraphAnnotator annotator = null;
		//spotlightFactory.annotator(); --> HACK: DefaultAnnotator is not DefaultParagraphAnnotator

		int i = 0;
		for(Text text : evaluationCorpus.getTexts()){
			i++;
			if(new File(annotationsFolder, i + ".tsv").exists()) {
				continue;
			}

			try{
				List<DBpediaResourceOccurrence> annotatedOccurrences = annotator.annotate(text.text());

				FileWriter fileWriter = new FileWriter(new File(annotationsFolder, i + ".tsv"));
				for(DBpediaResourceOccurrence annotatedOccurrence : annotatedOccurrences){
					fileWriter.write(annotatedOccurrence.toTsvString() + "\n");
				}
				fileWriter.close();
			}catch (Exception ignored) {}
		}

	}

}
