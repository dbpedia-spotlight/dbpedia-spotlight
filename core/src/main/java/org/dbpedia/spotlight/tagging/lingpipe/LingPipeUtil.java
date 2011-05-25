package org.dbpedia.spotlight.tagging.lingpipe;

import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.Tokenizer;
import org.dbpedia.spotlight.model.SurfaceForm;

import java.util.ArrayList;
import java.util.List;

/**
 * Various LingPipe utils.
 *
 * Methods of this class may require LingPipeFactory to be initialized, see {@link LingPipeUtilTest}.
 *
 * <p>
 * 	<code>
 * 	LingPipeFactory.setSentenceModel(new IndoEuropeanSentenceModel());
 * 	</code>
 * </p>
 *
 * @author Joachim Daiber
 * 
 */
public class LingPipeUtil {

	/**
	 * Get the tokens of the String.
	 * @param text
	 * @return
	 */

	public static List<String> getTokens(String text) {

		List<String> tokenList = new ArrayList<String>();
		List<String> whiteList = new ArrayList<String>();
		Tokenizer tokenizer = LingPipeFactory.getTokenizerFactory().tokenizer(text.toCharArray(),
				0, text.length());
		tokenizer.tokenize(tokenList, whiteList);

		return tokenList;
		
	}

	public static List<String> getTokens(SurfaceForm surfaceForm) {
		return getTokens(surfaceForm.name());
	}
	
	
	/**
	 * Get the sentence containing the annotation defined by start and end offset in a text.
	 *
	 * This implementation iterates over all tokens, i.e. it is O(|tokens|). For production,
	 * this should be improved.
	 * 
	 *
	 * @param offsetStart	start offset of the annotation in the text
	 * @param offsetEnd		end offset of the annotation in the text
	 * @param text			the text
	 * @return 				sentence containing the annotation
	 */
	
	public static AnnotatedString getSentence(int offsetStart, int offsetEnd, String text) {

		//1.) Tokenization
		List<String> tokenList = new ArrayList<String>();
		List<String> whiteList = new ArrayList<String>();
		Tokenizer tokenizer = LingPipeFactory.getTokenizerFactory().tokenizer(text.toCharArray(),
				0, text.length());
		tokenizer.tokenize(tokenList, whiteList);


		//2.) Sentence detection
		String[] tokens = new String[tokenList.size()];
		String[] whites = new String[whiteList.size()];
		tokenList.toArray(tokens);
		whiteList.toArray(whites);

		SentenceModel sentenceModel = LingPipeFactory.createSentenceModel();
		int[] sentenceBoundaries;
		try {
			sentenceBoundaries = sentenceModel.boundaryIndices(tokens, whites);
		}catch (NullPointerException e) {
			throw new NullPointerException("The sentence model is not initialized, see JavaDoc.");
		}

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
