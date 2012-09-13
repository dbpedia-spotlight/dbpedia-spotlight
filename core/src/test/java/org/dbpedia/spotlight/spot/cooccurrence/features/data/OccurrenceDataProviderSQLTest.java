package org.dbpedia.spotlight.spot.cooccurrence.features.data;

import junit.framework.TestCase;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.SpotlightFactory;
import org.dbpedia.spotlight.model.SpotlightConfiguration;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for the SQL-based occurrence data provider.
 *
 * @author jodaiber
 */

public class OccurrenceDataProviderSQLTest extends TestCase {

	List<String> words;
	OccurrenceDataProviderSQL occurrenceDataProviderUnigram;

	public OccurrenceDataProviderSQLTest(String name) throws InitializationException, ConfigurationException {
        super(name);
        SpotlightConfiguration config = new SpotlightConfiguration("conf/server.properties");
		SpotlightFactory luceneFactory = new SpotlightFactory(config);


		OccurrenceDataProviderSQL.initialize(config.getSpotterConfiguration());
		occurrenceDataProviderUnigram = OccurrenceDataProviderSQL.getInstance();
		String text = "PLEASANT GROVE, Ala. — The death toll in five Southern states rose sharply Thursday morning " +
				"to nearly 200 after devastating storms ripped through the region, spawning a deadly tornado in " +
				"downtown Tuscaloosa, Ala., and leaving a trail of flattened homes and buildings in an area already " +
				"battered by storms. States of emergency have been declared from Alabama to Virginia, and " +
				"President Obama said in a statement that the federal government had pledged its assistance." +
				"Raj Rajaratnam, the billionaire investor who once ran one of the world’s largest hedge funds, was found guilty on Wednesday of fraud and conspiracy by a federal jury in Manhattan. He is the most prominent figure convicted in the government’s crackdown on insider trading on Wall Street.\n" +
				"Mr. Rajaratnam, who was convicted on all 14 counts, could face as much as 19 and a half years in prison under federal sentencing guidelines, prosecutors said on Wednesday. (The law allows up to 25 years.) He is to be sentenced on July 29.\n" +
				"Mr. Rajaratnam, dressed in a black suit and a khaki green tie, had no expression as the verdict was read in the overflowing courtroom.\n" +
				"His lawyer, John Dowd, said he would appeal.\n" +
				"Prosecutors had asked that Mr. Rajaratnam be placed in custody, arguing that he was a flight risk. They said that he had the means to leave the country, noting that he owned property in Sri Lanka and Singapore.";

		words = Arrays.asList(
				luceneFactory.lingPipeFactory().getTokenizerFactoryInstance().tokenizer(text.toCharArray(), 0, text.length()).tokenize());
	}

    public void setUp() throws Exception {
        super.setUp();
    }

	public void testLeftNeighbour() throws ItemNotFoundException {

		CandidateData blue = occurrenceDataProviderUnigram.getCandidateData("blue");
		CandidateData sky = occurrenceDataProviderUnigram.getCandidateData("sky");

		CoOccurrenceData blueSky = occurrenceDataProviderUnigram.getBigramData(blue, sky);

		assertNotNull(blueSky);
	}

	public void testRightNeighbour() throws ItemNotFoundException {

		CandidateData left = occurrenceDataProviderUnigram.getCandidateData("left");
		CandidateData side = occurrenceDataProviderUnigram.getCandidateData("side");

		CoOccurrenceData skyAbove = occurrenceDataProviderUnigram.getBigramData(left, side);

		assertNotNull(skyAbove);
	}

	public void testMultipleQueries() throws ItemNotFoundException {

		CandidateData blue = occurrenceDataProviderUnigram.getCandidateData("sky");
		CandidateData above = occurrenceDataProviderUnigram.getCandidateData("above");


		for (String word : words) {
			try {
				CandidateData candidateData = occurrenceDataProviderUnigram.getCandidateData(word);
				occurrenceDataProviderUnigram.getBigramData(blue, candidateData);
				System.out.println(occurrenceDataProviderUnigram.getBigramData(candidateData, above));
			} catch (ItemNotFoundException e) {
				
			}
		}

		assertNotNull(words);
	}

}
