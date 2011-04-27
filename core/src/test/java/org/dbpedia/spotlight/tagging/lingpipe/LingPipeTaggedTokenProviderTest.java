package org.dbpedia.spotlight.tagging.lingpipe;

import com.aliasi.sentences.IndoEuropeanSentenceModel;
import junit.framework.TestCase;
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
			"experiencing such phantom quakes, as well as other symptoms of “earthquake sickness” " +
			"like dizziness and anxiety.";

	LingPipeTaggedTokenProvider lingPipeTaggedTokenProvider2;
	String text2 = "Aguri Suzuki, a 44-year-old real estate agent, says she sometimes " +
			"thinks the ground is shaking even when it is not. When she sees a tree branch swaying in the wind, " +
			"she worries there has been an earthquake. Doctors here say they are seeing more people who are " +
			"experiencing such phantom quakes, as well as other symptoms of “earthquake sickness” " +
			"like dizziness and anxiety";


	public LingPipeTaggedTokenProviderTest(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
		LingPipeFactory.setSentenceModel(new IndoEuropeanSentenceModel());
		LingPipeFactory.setTaggerModelFile(new File("/home/pablo/data/brown/pos-en-general-brown.HiddenMarkovModel"));

		lingPipeTaggedTokenProvider1 = new LingPipeTaggedTokenProvider();
		lingPipeTaggedTokenProvider1.initialize(text1);

		lingPipeTaggedTokenProvider2 = new LingPipeTaggedTokenProvider();
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

}
