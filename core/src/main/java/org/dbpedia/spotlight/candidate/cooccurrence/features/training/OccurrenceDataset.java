package org.dbpedia.spotlight.candidate.cooccurrence.features.training;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClass;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.Filter;
import org.dbpedia.spotlight.model.TaggedText;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeTaggedTokenProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A dataset of surface form occurrences and their annotation by a human annotator used for training
 * a classifier.
 *
 * @author Joachim Daiber
 */

public class OccurrenceDataset {


	/**
	 * Instances, i.e. annotated occurences of a term candidate
	 */
	private List<OccurrenceInstance> instances = new ArrayList<OccurrenceInstance>();


	/**
	 * Input format.
	 */
	public enum Format {JSON, TSV}


	public OccurrenceDataset() {

	}

	/**
	 * Create a new training dataset from an annotated TSV file.
	 *w
	 * @param file
	 * @throws IOException
	 */

	public OccurrenceDataset(File file, Format format) throws IOException, JSONException {


		switch (format) {

			case JSON:

				BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
				String jsonString = bufferedReader.readLine();


				JSONArray jsonObjects = new JSONArray(jsonString);

				for (int i = 0; i < jsonObjects.length(); i++) {

					JSONObject jsonObject = jsonObjects.getJSONObject(i).getJSONObject("annotations");
					String text = jsonObject.getString("@text");
					TaggedText taggedText = new TaggedText(text, new LingPipeTaggedTokenProvider());
					JSONArray annotations = jsonObject.getJSONArray("Resources");

					for (int j = 0; j < annotations.length(); j++) {
						JSONObject annotation = annotations.getJSONObject(j);


						/**
						 * Important: ignore empty annotations!
						 */
						
						if (annotation.has("annotation") && !annotation.getString("annotation").equals("")) {

							CandidateClass userAnnotation;

							if (annotation.getString("annotation").contains("c"))
								userAnnotation = CandidateClass.common;
							else if(annotation.getString("annotation").contains("p"))
								userAnnotation = CandidateClass.part;
							else
								userAnnotation = CandidateClass.term;

							addInstance(annotation.getString("@surfaceForm"),
								annotation.getInt("@offset"),
								taggedText,
								annotation.getString("@URI"),
								null, null,
								userAnnotation
							);

						}
					}

				}

			case TSV:

				CSVReader reader = new CSVReader(new FileReader(file), '\t');

				String[] row;
				while ((row = reader.readNext()) != null) {
					try{
						CandidateClass annotation = row[5].equals("t") ? CandidateClass.term : CandidateClass.common;
						addInstance(row[0], Integer.parseInt(row[1]), new TaggedText(row[2], new LingPipeTaggedTokenProvider()), row[3], row[4], row[5], annotation);
					}catch (ArrayIndexOutOfBoundsException e){

					}
				}

				reader.close();

		}

	}

	/**
	 * Filter the instances with all Filters.
	 *
	 * @param filters
	 */

	public void filter(List<Filter> filters) {

		for (Filter filter : filters) {
			filter.apply(instances);
		}

	}

	/**
	 * Add an instance to the training data set.
	 *
	 * @param candidate
	 * @param offset
	 * @param sentence
	 * @param annotationURI
	 * @param annotationTitle
	 * @param annotationAbstract
	 * @param annotation
	 */
	public void addInstance(String candidate, int offset, TaggedText sentence, String annotationURI,
							String annotationTitle, String annotationAbstract, CandidateClass annotation) {

		instances.add(new OccurrenceInstance(candidate, offset, sentence,
				annotationURI,	annotationTitle, annotationAbstract, annotation));
	}

	/**
	 * Add all instances in the Collection.
	 *
	 * @param instances
	 */
	public void addInstances(List<OccurrenceInstance> instances) {
		this.instances.addAll(instances);
	}


	/**
	 * Write training dataset to a TSV file.
	 *
	 * @param tsvFile
	 * @throws IOException
	 */
	public void write(File tsvFile) throws IOException {

		System.out.println("Writting instances: " + this.instances.size());

		CSVWriter writer = new CSVWriter(new FileWriter(tsvFile), '\t');

		for (OccurrenceInstance instance : instances) {
			String annotationString = instance.getCandidateClass() == CandidateClass.term ? "t" : "c";
			writer.writeNext(new String[] {instance.getSurfaceForm(), "" + instance.getOffset(), instance.getTextString(),
					instance.getAnnotationTitle(), instance.getAnnotationAbstract(), annotationString});
		}

		writer.close();

	}

	/**
	 * Get all instances in the dataset.
	 *
	 * @return List of instances
	 */
	public List<OccurrenceInstance> getInstances() {
		return instances;
	}


	/**
	 * @return number of instances
	 */
	public int size() {
		return instances.size();
	}


}
