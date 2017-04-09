package org.dbpedia.spotlight.tagging;

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
	List<String> getTokens(String text);

}
