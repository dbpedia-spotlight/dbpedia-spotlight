package org.dbpedia.spotlight.spot.cooccurrence.training;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.aliasi.sentences.IndoEuropeanSentenceModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.spot.cooccurrence.classification.SpotClass;
import org.dbpedia.spotlight.spot.cooccurrence.filter.Filter;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeFactory;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeTaggedTokenProvider;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

/**
 * A dataset of surface form occurrences and their annotation by a human annotator used for training
 * a classifier.
 *
 * @author Joachim Daiber
 */

public class AnnotatedDataset {

    private final static Log LOG = LogFactory.getLog(AnnotatedDataset.class);

	/**
	 * Instances, i.e. annotated occurrences of a candidate
	 */
	private List<AnnotatedSurfaceFormOccurrence> instances = new ArrayList<AnnotatedSurfaceFormOccurrence>();

	private List<Text> texts = new LinkedList<Text>();

    private LingPipeFactory lingPipeFactory;
	/**
	 * Input formats:
	 */
	public enum Format {JSON, TSV, CSAW}


	public AnnotatedDataset() {

	}

    private LingPipeTaggedTokenProvider getTaggedTokenProvider() {
        return new LingPipeTaggedTokenProvider(lingPipeFactory);
    }

    /**
     *
     * @param file
     * @param format
     * @param spotlightFactory SpotlightFactory for creating
     * @throws IOException
     * @throws JSONException
     * @throws ConfigurationException
     */
      public AnnotatedDataset(File file, Format format, SpotlightFactory spotlightFactory) throws IOException, JSONException, ConfigurationException {
          this(file, format, spotlightFactory.lingPipeFactory());
      }

	/**
	 * Create a new training dataset from an annotated file.
	 *
	 * <p>
	 * Currently supported formats:
	 * </p>
	 * <ul>
	 *     <li>JSON. DBpedia Spotlight JSON annotation output with additional manual annotation.</li>
	 *     <li>TSV. Tab-separated file produced by a {@link DatasetGenerator} class.</li>
	 *     <li>CSAW. Folder containing annotations from the CSAW project.</li>
	 * </ul>
	 *
	 * @param file the input file containing annotations.
	 * @param format the format of the input file, see currently supported formats
	 * @param {@link org.dbpedia.spotlight.tagging.TaggedTokenProvider}s for POS tagging
	 * @throws IOException Error while reading file.
	 * @throws org.dbpedia.spotlight.exceptions.ConfigurationException Error in the configuration.
	 * @throws org.json.JSONException Error while deserializing JSON file.
	 */
	public AnnotatedDataset(File file, Format format, LingPipeFactory lingPipeFactory) throws IOException, ConfigurationException, JSONException {
        this.lingPipeFactory = lingPipeFactory;
        if (!file.exists()) {
            throw new ConfigurationException(String.format("Could not find dataset on path provided %s.",file.getAbsolutePath()));
        }

        switch (format) {

			case JSON:

				BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
				String jsonString = bufferedReader.readLine();


				JSONArray jsonObjects = new JSONArray(jsonString);

				for (int i = 0; i < jsonObjects.length(); i++) {

					JSONObject jsonObject = jsonObjects.getJSONObject(i).getJSONObject("annotations");
					String text = jsonObject.getString("@text");
					TaggedText taggedText = new TaggedText(text, getTaggedTokenProvider());
					texts.add(taggedText);

					JSONArray annotations = jsonObject.getJSONArray("Resources");

					for (int j = 0; j < annotations.length(); j++) {
						JSONObject annotation = annotations.getJSONObject(j);



						//Important: ignore empty annotations!


						if (annotation.has("annotation") && !annotation.getString("annotation").equals("")) {

							SpotClass userAnnotation;

							if (annotation.getString("annotation").contains("c"))
								userAnnotation = SpotClass.common;
							else if(annotation.getString("annotation").contains("p"))
								userAnnotation = SpotClass.part;
							else
								userAnnotation = SpotClass.valid;

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
				break;

			case TSV:

				CSVReader reader = new CSVReader(new FileReader(file), '\t');

				String[] row;
				while ((row = reader.readNext()) != null) {
					try{
						SpotClass annotation = row[5].equals("t") ? SpotClass.valid : SpotClass.common;
						addInstance(row[0], Integer.parseInt(row[1]), new TaggedText(row[2], getTaggedTokenProvider()), row[3], row[4], row[5], annotation);
					}catch (ArrayIndexOutOfBoundsException ignored){}
				}

				reader.close();
				break;

			case CSAW:

				/**
				 * Read and tag the crawled documents:
				 */
				File crawledDocs = new File(file, "crawledDocs");
				Map<String, TaggedText> textMap = new HashMap<String, TaggedText>();
				for(String crawledDoc : crawledDocs.list()) {
					if(crawledDoc.equals("CZdata1") || crawledDoc.equals("docPaths.txt")
							|| crawledDoc.equals("13Oct08_allUrls.txt.txt"))
						continue;

					/**
					 * Read the text file :
					 */
					File crawledDocFile = new File(crawledDocs, crawledDoc);
					byte[] buffer = new byte[(int) crawledDocFile.length()];
					BufferedInputStream f = null;
					try {
						f = new BufferedInputStream(new FileInputStream(crawledDocFile));
						f.read(buffer);
					} finally {
						if (f != null) try { f.close(); } catch (IOException ignored) { }
					}

					TaggedText text = new TaggedText(new String(buffer), getTaggedTokenProvider());
					textMap.put(crawledDoc, text);
					texts.add(text);


				}

				/**
				 * Read the annotations:
				 */
				File annotationFile = new File(file, "CSAW_Annotations.xml");

				DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = null;
				try {
					docBuilder = docBuilderFactory.newDocumentBuilder();
					Document doc = docBuilder.parse (annotationFile);

					doc.getDocumentElement().normalize();

					NodeList annotations = doc.getElementsByTagName("annotation");

					for(int i = 0; i < annotations.getLength(); i++) {
						Node annotation = annotations.item(i);
						NodeList childNodes = annotation.getChildNodes();

						String docName = null;
						int length = 0;
						String wikiName = null;
						int offset = 0;

						for(int j = 0; j < childNodes.getLength(); j++) {

							Node item = childNodes.item(j);

							if(item.getNodeName().equals("docName")) {
								docName = item.getTextContent();
							}else if(item.getNodeName().equals("wikiName")) {
								wikiName = item.getTextContent();
							}else if(item.getNodeName().equals("offset")) {
								offset = Integer.parseInt(item.getTextContent());
							}else if(item.getNodeName().equals("length")) {
								length = Integer.parseInt(item.getTextContent());
							}
						}

						try{
							if(docName != null && wikiName != null){
								TaggedText text = textMap.get(docName);
								addInstance(((Text) text).text().substring(offset, offset+length),
										offset,	text, wikiName, "", "",
										wikiName.equals("") ? SpotClass.common : SpotClass.valid);
							}
						} catch (StringIndexOutOfBoundsException ignored) {

						}

					}

				} catch (ParserConfigurationException e) {
					throw new IOException(e);
				} catch (SAXException e) {
					throw new IOException(e);
				}
				break;
		}

	}


	/**
	 * Filter the instances with all Filters.
	 *
	 * @param filters list of filters to apply to the dataset.
	 */
	public void filter(List<Filter> filters) {
		for (Filter filter : filters) {
			filter(filter);
		}
	}

	
	/**
	 * Filter the instances with a single filter
	 *
	 * @param filter the Filter to apply
	 */

	public void filter(Filter filter) {
		filter.apply(instances);
	}

	
	/**
	 * Add an instance to the training data set.
	 *
	 * @param candidate the surface form of the candidate
	 * @param offset offset of the candidate
	 * @param sentence the sentence the candidate occurs in
	 * @param annotationURI the DBpedia resource URI the candidate was annotated with
	 * @param annotationTitle the title of the DBpedia resource
	 * @param annotationAbstract the abstract (if available) of the DBpedia resource
	 * @param annotation assigned candidate class
	 */
	public void addInstance(String candidate, int offset, TaggedText sentence, String annotationURI,
							String annotationTitle, String annotationAbstract, SpotClass annotation) {

		instances.add(new AnnotatedSurfaceFormOccurrence(candidate, offset, sentence,
				annotationURI,	annotationTitle, annotationAbstract, annotation));
	}


	/**
	 * Add all instances in the Collection.
	 *
	 * @param instances List of instances
	 */
	public void addInstances(List<AnnotatedSurfaceFormOccurrence> instances) {
		this.instances.addAll(instances);
	}


	/**
	 * Write training dataset to a TSV file.
	 *
     *
	 * @param tsvFile TSV file to write to
	 * @throws IOException Error while writing to TSV file.
	 */
	public void write(File tsvFile) throws IOException {

		LOG.info("Writing instances: " + this.instances.size());

		CSVWriter writer = new CSVWriter(new FileWriter(tsvFile), '\t');

		for (AnnotatedSurfaceFormOccurrence instance : instances) {
			String annotationString = instance.getSpotClass() == SpotClass.valid ? "t" : "c";
			writer.writeNext(new String[] {instance.getSurfaceForm(), "" + instance.getOffset(), instance.getTextString(),
					instance.getAnnotationTitle(), instance.getAnnotationAbstract(), annotationString});
                                                         // getAnnotationAbstract() seems to be always null?
		}

		writer.close();
        LOG.info("Done.");

	}

    /**
	 * Write training dataset to a TSV file.
	 * TODO allow other formats as well
     *
	 * @param tsvFile TSV file to write to
	 * @throws IOException Error while writing to TSV file.
	 */
	public void reformat(File tsvFile) throws IOException {

		LOG.info("Writing instances: " + this.instances.size());

		FileWriter writer = new FileWriter(tsvFile);
        int i = 0;
		for (AnnotatedSurfaceFormOccurrence instance : instances) {
            i++;
            String text = (instance.getTextString()!=null) ? instance.getTextString().replaceAll("\\s", " ") : "";
            String uri = new org.dbpedia.spotlight.model.DBpediaResource(instance.getAnnotationURI()).uri();
			//occId	URI surfaceForm text offset
            writer.write(String.format("%s\t%s\t%s\t%s\t%s\n", instance.getSpotClass().toString() +"-"+ i, uri, instance.getSurfaceForm(), text, "" + instance.getOffset()));
		}

		writer.close();
        LOG.info("Done.");

	}

    /**
     * Get all documents in the dataset.
     *
     * @return List of text objects representing documents
     */
    public List<Text> getTexts() {
        return texts;
    }

	/**
	 * Get all instances in the dataset.
	 *
	 * @return List of instances
	 */
	public List<AnnotatedSurfaceFormOccurrence> getInstances() {
		return instances;
	}

	/**
	 * Get a Set of valid {@link DBpediaResourceOccurrence}s.
	 *
	 * @return Set of all valid {@link DBpediaResourceOccurrence}s.
	 */
	public Set<DBpediaResourceOccurrence> toDBpediaResourceOccurrences() {
		Set<DBpediaResourceOccurrence> dbpediaResourceOccurrences = new HashSet<DBpediaResourceOccurrence>();

		for(AnnotatedSurfaceFormOccurrence instance : getInstances()) {
			if(instance.getSpotClass() == SpotClass.valid)
				dbpediaResourceOccurrences.add(instance.toDBpediaResourceOccurrence());
		}

		return dbpediaResourceOccurrences;
	}


	/**
	 * Get the size of the annotated dataset.
	 *
	 * @return number of instances in the dataset
	 */
	public int size() {
		return instances.size();
	}

    public static void main(String[] args) throws ConfigurationException, IOException, JSONException, InputException {

        String usage = "\nUsage: AnnotatedDataset  [config]  [dataset]  [type] \n\n";
        String configFile = "conf/dev.properties";
        String datasetLocation = "/home/pablo/eval/jo/jo.json";
        Format datasetType = Format.JSON;
        try {
            configFile = args[0]; //
            datasetLocation = args[1]; //"/home/pablo/eval/jo/jo.json"
            datasetType = Format.valueOf(args[2]);
        } catch (IndexOutOfBoundsException e) {
            System.err.println(usage);
            throw new InputException("Missing parameters in command line.",e);
        } catch (Exception e) {
            System.err.println(usage);
            throw new InputException("Error in parameters provided in command line.",e);
        }

        SpotlightConfiguration configuration = new SpotlightConfiguration(configFile);
        LingPipeFactory lingPipeFactory = new LingPipeFactory(new File(configuration.getTaggerFile()), new IndoEuropeanSentenceModel());
        LOG.info("Reading gold standard.");

        AnnotatedDataset evaluationCorpus =
				new AnnotatedDataset(new File(datasetLocation),
						AnnotatedDataset.Format.JSON, lingPipeFactory);

        evaluationCorpus.reformat(new File(datasetLocation + ".tsv"));

    }


}
