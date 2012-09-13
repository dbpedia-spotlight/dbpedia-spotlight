package org.dbpedia.spotlight.tagging.lingpipe;

import com.aliasi.sentences.IndoEuropeanSentenceModel;
import junit.framework.TestCase;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.tagging.TaggedToken;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * LingPipeTaggedTokenProvider Tester.
 *
 * @author jodaiber
 */
public class LingPipeTaggedTokenProviderTest extends TestCase {

	LingPipeTaggedTokenProvider lingPipeTaggedTokenProvider1;
	String text1 = "Aguri Suzuki, a 44-year-old real estate agent, says she sometimes " +
			"thinks the ground is shaking even when it is not. When she sees a tree branch swaying in the wind, " +
			"she worries there has been an earthquake. Doctors here say they are seeing more people who are " +
			"experiencing such phantom quakes, as well as other symptoms of \"earthquake sickness\" " +
			"like dizziness and anxiety.";

	LingPipeTaggedTokenProvider lingPipeTaggedTokenProvider2;
	String text2 = "Aguri Suzuki, a 44-year-old real estate agent, says she sometimes " +
			"thinks the ground is shaking even when it is not. When she sees a tree branch swaying in the wind, " +
			"she worries there has been an earthquake. Doctors here say they are seeing more people who are " +
			"experiencing such phantom quakes, as well as other symptoms of \"earthquake sickness\" " +
			"like dizziness and anxiety";


	public LingPipeTaggedTokenProviderTest(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();

		SpotlightConfiguration configuration = new SpotlightConfiguration("conf/dev.properties");
        LingPipeFactory lingPipeFactory = new LingPipeFactory(new File(configuration.getTaggerFile()), new IndoEuropeanSentenceModel());

		lingPipeTaggedTokenProvider1 = new LingPipeTaggedTokenProvider(lingPipeFactory);
		lingPipeTaggedTokenProvider1.initialize(text1);

		lingPipeTaggedTokenProvider2 = new LingPipeTaggedTokenProvider(lingPipeFactory);
		lingPipeTaggedTokenProvider2.initialize(text2);

	}


	/**
	 * Test that we get a non-empty list of tagged tokens for a non-empty range.
	 */
	public void testGetTaggedTokensNotNull() {
		assertNotNull(lingPipeTaggedTokenProvider1.getTaggedTokens(0, 6));
	}


	/**
	 * Test the number of tagged tokens returned for a range in the text.
	 */
	public void testGetTaggedTokensLength() {
		assertEquals(3, lingPipeTaggedTokenProvider1.getTaggedTokens(28, 45).size());
	}


	/**
	 * Test the number of tagged tokens returned for the whole text.
	 */
	public void testGetTaggedTokensLengthForEntireText() {
		assertEquals(71, lingPipeTaggedTokenProvider1.getTaggedTokens(0, text1.length()).size());
	}

	/**
	 * Test the number of tagged tokens returned for the whole text, with missing final punctuation.
	 */
	public void testGetTaggedTokensLengthWithoutFinalPunctuation() {
		assertEquals(70, lingPipeTaggedTokenProvider2.getTaggedTokens(0, text2.length()).size());
	}


	/**
	 * Test the tokens returned for a range in the text.
	 */
	public void testGetTaggedTokensTokens() {
		List<TaggedToken> taggedTokens = lingPipeTaggedTokenProvider1.getTaggedTokens(28, 45);

		List<String> tokens = new LinkedList<String>();
		for (TaggedToken taggedToken : taggedTokens) {
			tokens.add(taggedToken.getToken());
		}

		List<String> tokensExpected = new LinkedList<String>();
		tokensExpected.add("real");
		tokensExpected.add("estate");
		tokensExpected.add("agent");

		assertEquals(tokensExpected, tokens);
	}


	/**
	 * Test the retrieval of the left neighbour of a term candidate.
	 */
	public void testGetLeftNeigbour() throws ItemNotFoundException {
		TaggedToken leftNeighbour = lingPipeTaggedTokenProvider1.getLeftNeighbourToken(28, 45);
		assertEquals("44-year-old", leftNeighbour.getToken());
	}


	public void testGetLeftContext1() throws ItemNotFoundException {
		List<TaggedToken> leftNeighbours=lingPipeTaggedTokenProvider1.getLeftContext(28, 45, 1);
		assertEquals("44-year-old", leftNeighbours.get(0).getToken());
	}

	public void testGetLeftContext2() throws ItemNotFoundException {
		List<TaggedToken> leftNeighbours = lingPipeTaggedTokenProvider1.getLeftContext(28, 45, 2);

		assertEquals("44-year-old", leftNeighbours.get(0).getToken());
		assertEquals("a", leftNeighbours.get(1).getToken());
	}


	public void testGetRightContext1() throws ItemNotFoundException {
		List<TaggedToken> rightNeighbours= lingPipeTaggedTokenProvider1.getRightContext(28, 45, 1);
		assertEquals(",", rightNeighbours.get(0).getToken());
	}

	public void testGetRightContext2() throws ItemNotFoundException {
		List<TaggedToken> rightNeighbours = lingPipeTaggedTokenProvider1.getRightContext(28, 45, 2);

		assertEquals(",", rightNeighbours.get(0).getToken());
		assertEquals("says", rightNeighbours.get(1).getToken());
	}


	/**
	 * Test the retrieval of the right neighbour of a term candidate.
	 */
	public void testGetRightNeigbour() throws ItemNotFoundException {
		TaggedToken rightNeighbour = lingPipeTaggedTokenProvider1.getRightNeighbourToken(28, 45);
		assertEquals(",", rightNeighbour.getToken());
	}

	/**
	 * Test the retrieval of the right neighbour of a term candidate.
	 */
	public void testGetRightNeigbour2() throws ItemNotFoundException {
		TaggedToken rightNeighbour = lingPipeTaggedTokenProvider1.getRightNeighbourToken(326, 343);
		assertEquals("\"", rightNeighbour.getToken());
	}




	/**
	 * Test the retrieval of sentence containing a term candidate.
	 */
	public void testGetSentence() throws ItemNotFoundException {
		List<TaggedToken> sentence = lingPipeTaggedTokenProvider1.getSentenceTokens(28, 45);
		assertEquals(23, sentence.size());
	}

	/**
	 * Test the retrieval of sentence containing a term candidate.
	 */
	public void testGetSentence2() throws ItemNotFoundException {
		List<TaggedToken> sentence = lingPipeTaggedTokenProvider1.getSentenceTokens(0, 45);
		assertEquals(23, sentence.size());
	}



	/**
	 * Test the tags returned for a range in the text.
	 */
	public void testGetTaggedTokensTags() {
		List<TaggedToken> taggedTokens = lingPipeTaggedTokenProvider1.getTaggedTokens(28, 45);

		List<String> tokens = new LinkedList<String>();
		for (TaggedToken taggedToken : taggedTokens) {
			tokens.add(taggedToken.getPOSTag());
		}

		List<String> tokensExpected = new LinkedList<String>();
		tokensExpected.add("jj");
		tokensExpected.add("nn");
		tokensExpected.add("nn");

		assertEquals(tokensExpected, tokens);
	}

	public void testGetPartlyTaggedTokensTags() {
		List<TaggedToken> tokens1 = lingPipeTaggedTokenProvider1.getTaggedTokens(17, 19);
		List<TaggedToken> tokens2 = lingPipeTaggedTokenProvider1.getTaggedTokens(20, 24);
		List<TaggedToken> tokens3 = lingPipeTaggedTokenProvider1.getTaggedTokens(25, 28);

		assertEquals("jj", tokens1.get(0).getPOSTag());
		assertEquals("jj", tokens2.get(0).getPOSTag());
		assertEquals("jj", tokens3.get(0).getPOSTag());
	}

	public void testGetLeftNeighbour() throws ItemNotFoundException {
		TaggedToken leftNeighbourToken = lingPipeTaggedTokenProvider1.getLeftNeighbourToken(17, 19);
		
		assertEquals("a", leftNeighbourToken.getToken());
	}



	public void testGetLeftNeighbours() throws ItemNotFoundException {
		List<TaggedToken> leftNeighbourTokens = lingPipeTaggedTokenProvider1.getLeftContext(17, 19, 2);

		assertEquals("a", leftNeighbourTokens.get(0).getToken());
	}


	public void testGetRightNeighbour() throws ItemNotFoundException {
		TaggedToken leftNeighbourToken = lingPipeTaggedTokenProvider1.getRightNeighbourToken(17, 19);

		assertEquals("real", leftNeighbourToken.getToken());
	}



	public void testGetRightNeighbours() throws ItemNotFoundException {
		List<TaggedToken> leftNeighbourTokens = lingPipeTaggedTokenProvider1.getRightContext(17, 19, 2);

		assertEquals("real", leftNeighbourTokens.get(0).getToken());
	}

	public void testPartialTokenMatch() throws ItemNotFoundException {
		List<TaggedToken> taggedTokens = lingPipeTaggedTokenProvider1.getTaggedTokens(20, 24);

		assertEquals(1, taggedTokens.size());
		assertEquals("jj", taggedTokens.get(0).getPOSTag());
	}




}
