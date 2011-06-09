package org.dbpedia.spotlight.tagging.lingpipe;

import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.Tokenizer;
import org.dbpedia.spotlight.tagging.TextUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of TextUtil using LingPipe.
 *
 * @author Joachim Daiber
 * 
 */
public class LingPipeTextUtil implements TextUtil {

	private LingPipeFactory lingPipeFactory;

	/**
	 * Creates a new TextUtil using LingPipe.
	 *
	 * @see LingPipeFactory
	 *
	 * @param lingPipeFactory factory for creating LingPipe tools
	 */
	public LingPipeTextUtil(LingPipeFactory lingPipeFactory) {
		this.lingPipeFactory = lingPipeFactory;
	}
	

	/** {@inheritDoc} */
	public List<String> getTokens(String text) {

		List<String> tokenList = new ArrayList<String>();
		List<String> whiteList = new ArrayList<String>();
		Tokenizer tokenizer = lingPipeFactory.getTokenizerFactoryInstance().tokenizer(text.toCharArray(),
				0, text.length());
		tokenizer.tokenize(tokenList, whiteList);

		return tokenList;

	}

	
	/**
	 * <p>
	 * {@inheritDoc}
	 * </p>
	 * 
	 * This implementation iterates over all tokens in the text in O(|token|) but does not require tagging etc.
	 * */

 	public AnnotatedString getSentence(int offsetStart, int offsetEnd, String text) {

		//1.) Tokenization
		List<String> tokenList = new ArrayList<String>();
		List<String> whiteList = new ArrayList<String>();
		Tokenizer tokenizer = lingPipeFactory.getTokenizerFactoryInstance().tokenizer(text.toCharArray(),
				0, text.length());
		tokenizer.tokenize(tokenList, whiteList);


		//2.) Sentence detection
		String[] tokens = new String[tokenList.size()];
		String[] whites = new String[whiteList.size()];
		tokenList.toArray(tokens);
		whiteList.toArray(whites);

		SentenceModel sentenceModel = lingPipeFactory.getSentenceModelInstance();
		int[] sentenceBoundaries = sentenceModel.boundaryIndices(tokens, whites);

		int sentenceEndToken;
		int sentenceEndOffset = -1;
		int sentenceStartOffset = whites[0].length();
		int iToken = 0;
		int iSentence = 0;

		while (sentenceEndOffset < offsetEnd && iSentence < sentenceBoundaries.length) {
			sentenceStartOffset = sentenceEndOffset + 1;
			sentenceEndToken = sentenceBoundaries[iSentence];

			while(iToken <= sentenceEndToken) {
				sentenceEndOffset += tokens[iToken].length() + whites[iToken+1].length();
				iToken++;
			}

			iSentence++;
		}

		return new AnnotatedString(text, sentenceStartOffset, sentenceEndOffset);
	}

	
}
