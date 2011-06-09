package org.dbpedia.spotlight.tagging;

import org.dbpedia.spotlight.tagging.lingpipe.AnnotatedString;

import java.util.List;


/**
 * Various utility methods for working with natural language.
 *
 * @author Joachim Daiber
 */
public interface TextUtil {


	/**
	 * Tokenize String and return List of tokens using the default String tokenizer.
	 *
	 * @param text String to tokenize
	 * @return List of tokens
	 */
	
	public List<String> getTokens(String text);

	
	/**
	 * Get the sentence containing the annotation defined by start and end offset in a text.
	 * This method is similar to {@link org.dbpedia.spotlight.tagging.TaggedTokenProvider#getSentence(int, int)},
	 * however, it does not require the entire text to be tagged and is therefore much faster.
	 *
	 * @param offsetStart	start offset of the annotation in the text
	 * @param offsetEnd		end offset of the annotation in the text
	 * @param text			the text
	 * @return 				annotated String object with the position of the sentence
	 */

	public AnnotatedString getSentence(int offsetStart, int offsetEnd, String text);


}
