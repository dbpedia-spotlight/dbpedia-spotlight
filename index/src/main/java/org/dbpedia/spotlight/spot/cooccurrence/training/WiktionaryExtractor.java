package org.dbpedia.spotlight.spot.cooccurrence.training;

import au.com.bytecode.opencsv.CSVWriter;
import org.apache.commons.lang.StringUtils;
import org.dbpedia.helper.CoreUtil;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.rio.ntriples.NTriplesParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Proof-of-concept implementation of an extractor of linguistic knowledge from Wiktionary.
 *
 * @author Joachim Daiber
 */
public class WiktionaryExtractor extends DefaultHandler {

	private String currentTitle;
	private CSVWriter csvWriterIdioms;
	private CSVWriter csvWriterGeneralReference;

	private int iRead = 0;
	private StringBuilder contentBuilder = new StringBuilder();
	private HashMap<String, Statement> confusables;


	public void setConfusables(List<Statement> confusablesRDF) {

		this.confusables = new HashMap<String, Statement>();

		for(Statement statement : confusablesRDF) {
			this.confusables.put(statement.getObject().stringValue().replaceFirst("http://en.wiktionary.org/wiki/", ""), statement);
		}

	}

	private enum Tag {title, text};
	private Tag currentTag = null;

	Pattern idiomPattern = Pattern.compile("[\\s]*# (\\{\\{(idiomatic|[^\\}]*)+\\}\\}).*[\\s]*", Pattern.MULTILINE);

	public WiktionaryExtractor(CSVWriter csvWriterIdiom, CSVWriter csvWriterGeneralReference) {
		super();
		this.csvWriterIdioms = csvWriterIdiom;
		this.csvWriterGeneralReference = csvWriterGeneralReference;
	}

	public void startDocument (){
		iRead++;
		if(iRead % 10 == 0)
			System.err.println("Read " + iRead + " documents.");
	}

	public void startElement (String uri, String name,
							  String qName, Attributes atts)
	{

		if(name.equals("title"))
			currentTag = Tag.title;
		else if(name.equals("text"))
			currentTag = Tag.text;
		else
			currentTag = null;



	}

	public void endElement (String uri, String name,
							String qName)
	{

		if(name.equals("text")){
			boolean properNoun = false;
			boolean commonNoun = false;

			List<String> wikipediaParts = new LinkedList<String>();
			for(String line : contentBuilder.toString().split("\\r?\\n")) {


				if(line.contains("=Noun=")) {
					commonNoun = true;
				}else if(line.contains("=Proper noun=")) {
					properNoun = true;
				} if(line.contains("{{wikipedia")) {
					wikipediaParts = extractTemplate(line);

					for(String wikipediaPart : wikipediaParts){
						if(wikipediaPart.startsWith("lang=") && !wikipediaPart.equals("lang=" + SpotlightConfiguration.DEFAULT_LANGUAGE_I18N_CODE)){
							/**
							 * We only want links to the Wikipedia of the current language.
							 */

							wikipediaParts.clear();
							break;
						}
					}

				} if(line.contains("{{idiomatic") || line.contains("|idiomatic")){

					Matcher m = idiomPattern.matcher(line);

					if(m.matches())
						writeIdiom(m);

				}


			}

			if(commonNoun && !properNoun && (wikipediaParts.size() > 0 || confusables.containsKey(currentTitle))) {

				String dbpediaResource;
				if(confusables.containsKey(currentTitle)) {
					dbpediaResource = confusables.get(currentTitle).getSubject().stringValue();
				}else{
					String wikiLink = null;
					for(String wikipediaPart : wikipediaParts) {
						if(!wikipediaPart.equals("wikipedia") && !wikipediaPart.contains("=")) {
							wikiLink = wikipediaPart;
							break;
						}
					}

					if(wikiLink == null)
						dbpediaResource = SpotlightConfiguration.DEFAULT_NAMESPACE + wikipediaEncode(currentTitle);
					else
						dbpediaResource = SpotlightConfiguration.DEFAULT_NAMESPACE + wikipediaEncode(wikiLink);

				}


				writeGeneralReference(dbpediaResource);
			}

			contentBuilder = new StringBuilder();

		}


	}

	private String wikipediaEncode(String wikiLink) {
		String link = CoreUtil.wikipediaEncode(wikiLink);
		return link.substring(0,1).toUpperCase() + link.substring(1);
	}

	private String l1 = null;
	public void characters(char[] ch, int start, int end) throws SAXException {

		if(currentTag == null)
			return;

		if(currentTag == Tag.title) {
			String s = new String(ch, start, end);
			currentTitle = s;
			currentTag = null; //Only read first line, next line will be whitespace

		}else if(currentTag == Tag.text){
			String s = new String(ch, start, end);
			contentBuilder.append(s);
		}
	}


	private void writeIdiom(Matcher m) {

		try{
			List<String> types = extractTemplate(m.group(1));
			types.remove("idiomatic");
			csvWriterIdioms.writeNext(new String[]{currentTitle, "idiomatic", StringUtils.join(types, ",")});

		}catch (IllegalStateException e) {
			System.out.println(m.group(0));
		}

	}

	private void writeGeneralReference(String uri) {
		csvWriterGeneralReference.writeNext(new String[]{uri});
	}



	private List<String> extractTemplate(String template) {
		return new LinkedList<String>(Arrays.asList(template.replaceAll(".*\\{\\{", "").replaceAll("\\}\\}.*", "").split("\\|")));
	}

	public void close() throws IOException {
		csvWriterGeneralReference.close();
		csvWriterIdioms.close();
	}


	public static void main(String[] args) throws SAXException, IOException, RDFHandlerException, RDFParseException {


//		NTriplesParser nTriplesParser = new NTriplesParser();
//		nTriplesParser.parse(new FileReader(new File("/Users/jodaiber/Documents/workspace/ba/BachelorThesis/02 Implementation/DBPedia Spotlight data/confusables_with_wiktionary.nt")), "http://dbpedia.org/property/");



		XMLReader xmlReader = XMLReaderFactory.createXMLReader();
		WiktionaryExtractor wiktionaryExtractor = new WiktionaryExtractor(
				new CSVWriter(new FileWriter(new File("/Users/jodaiber/Desktop/idioms.csv"))),
				new CSVWriter(new FileWriter(new File("/Users/jodaiber/Desktop/generalReference.csv")))
		);

		List<Statement> confusables = new LinkedList<Statement>();
		StatementCollector inputCollector = new StatementCollector(confusables);
		NTriplesParserFactory nTriplesParserFactory = new NTriplesParserFactory();
		RDFParser parser = nTriplesParserFactory.getParser();
		parser.setRDFHandler(inputCollector);
		parser.parse(new FileReader(new File("/Users/jodaiber/Documents/workspace/ba/BachelorThesis/01 Evaluation/03 Training/Training Set/data/confusables_with_wiktionary.nt")), "");

		wiktionaryExtractor.setConfusables(confusables);

		xmlReader.setContentHandler(wiktionaryExtractor);

		InputSource inputSource = new InputSource(new FileReader(new File("/Users/jodaiber/Desktop/wiktionary-parser/enwiktionary-20110512-pages-articles.xml")));
		xmlReader.parse(inputSource);
		wiktionaryExtractor.close();



	}



}
