package org.dbpedia.spotlight.tagging.lingpipe;

import com.aliasi.sentences.SentenceModel;
import com.aliasi.tag.Tagger;
import com.aliasi.tag.Tagging;
import com.aliasi.tokenizer.Tokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.tagging.TaggedToken;
import org.dbpedia.spotlight.tagging.TaggedTokenProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * TaggedToken provider based on LingPipe.
 * <p/>
 * This implementation uses LingPipe to do tokenization, sentence detection and Part-of-Speech
 * tagging.
 * <p/>
 * Once the text is tagged (initialize()), the part-of-speech tags for a range in the text can be
 * retrieved by using getTaggedTokens(int textOffsetStart, int textOffsetEnd) in O(log n) time.
 *
 * @author jodaiber
 */

//TODO see if you can use hashing for O(1) retrieval

public class LingPipeTaggedTokenProvider implements TaggedTokenProvider {

    private Log LOG = LogFactory.getLog(this.getClass());

	List<TaggedToken> taggedTokens = new ArrayList<TaggedToken>();
	private static final int MAX_TAG_RESULTS = 10;


	/**
	 * Returns a list of tagged tokens for the range specified by two offsets.
	 *
	 * @param textOffsetStart text offset of the start (number of characters from the start of the text)
	 * @param textOffsetEnd   text offset of the end
	 * @return
	 */
	@Override
	public List<TaggedToken> getTaggedTokens(int textOffsetStart, int textOffsetEnd) {

		int posPositionOfOffset = Collections.binarySearch(taggedTokens, textOffsetStart);

		int firstTaggedToken;
		if (posPositionOfOffset >= 0) {

			//An exact match was found
			firstTaggedToken = posPositionOfOffset;
		} else {

			//No exact match was found, take the next token
			firstTaggedToken = (posPositionOfOffset * -1) - 1;
		}


		//Gather all tokens to be returned
		int i = firstTaggedToken;
		while (i < taggedTokens.size() - 1
				&& (taggedTokens.get(i).getOffset() + taggedTokens.get(i).getToken().length()) <= textOffsetEnd) {

			i++;
		}

		List<TaggedToken> taggedTokensInRange = taggedTokens.subList(firstTaggedToken, i);


		return taggedTokensInRange;
	}


	/**
	 * Initialize the TaggedTokenProvider. On initialization, the whole text is tokenized
	 * and tagged.
	 *
	 * @param text String that should be tagged.
	 */

	@Override
	public void initialize(String text) {

		//Load the POS model:
		Tagger posTagger = LingPipeFactory.createPOSTagger();

		long start = System.currentTimeMillis();
		//1.) Tokenization
		ArrayList tokenList = new ArrayList();
		ArrayList whiteList = new ArrayList();
		Tokenizer tokenizer = LingPipeFactory.getTokenizerFactory().tokenizer(text.toCharArray(),
				0, text.length());
		tokenizer.tokenize(tokenList, whiteList);
		LOG.trace("Tokenization took " + (System.currentTimeMillis() - start) + "ms.");

		//2.) Sentence detection
		String[] tokens = new String[tokenList.size()];
		String[] whites = new String[whiteList.size()];
		tokenList.toArray(tokens);
		whiteList.toArray(whites);


		start = System.currentTimeMillis();
		SentenceModel sentenceModel = LingPipeFactory.createSentenceModel();
		int[] sentenceBoundaries = sentenceModel.boundaryIndices(tokens, whites);
		LOG.trace("Sentence segmentation took " + (System.currentTimeMillis() - start) + "ms.");

		start = System.currentTimeMillis();
		//3.) Part-of-Speech tagging
		int sentStartToken = 0;
		int sentEndToken;
		int textOffset = 0;


		/**
		 * Tag every sentence with final punctuation (i < sentenceBoundaries.length), if there is
		 * text without final punctuation, treat the rest of the text as a single sentence.
		 */

		for (int i = 0; (i < sentenceBoundaries.length || sentStartToken < tokens.length); ++i) {

			if (i < sentenceBoundaries.length) {
				//We are between two sentence-final punctuation tokens.

				sentEndToken = sentenceBoundaries[i];
			} else {
				//We are beyond the last sentence-final punctuation: Tag the rest of the text.

				sentEndToken = tokens.length - 1;
			}


			Tagging<String> tags = posTagger.tag(tokenList.subList(sentStartToken, sentEndToken + 1));
			for (int j = 0; j < tags.size(); j++) {
				TaggedToken taggedToken = new TaggedToken(tags.token(j), tags.tag(j), textOffset, null);
				taggedTokens.add(taggedToken);
				textOffset += tokens[sentStartToken + j].length() + whites[sentStartToken + j + 1].length();
			}

			sentStartToken = sentEndToken + 1;
		}

		LOG.trace("POS tagging took " + (System.currentTimeMillis() - start) + "ms.");


	}


}
