package org.dbpedia.spotlight.tagging.lingpipe;

/**
 * String with a single annotated substring, e.g. a paragraph containing an
 * annotated sentence.
 *
 * @author Joachim Daiber
 */
public class AnnotatedString {

	String string;

	int offsetFrom;
	int offsetTo;

	public AnnotatedString(String string, int offsetFrom, int offsetTo) {
		this.string = string;
		this.offsetFrom = offsetFrom;
		this.offsetTo = offsetTo;
	}

	/**
	 * Returns the entire String.
	 *
	 * @return the String
	 */
	public String getString() {
		return string;
	}


	/**
	 * Returns the annotated part of the String.
	 *
	 * @return annotated part of the String.
	 */
	public String getAnnotation() {
		return string.substring(offsetFrom, offsetTo);
	}

	/**
	 * Returs the start offset of the annotation.
	 *
	 * @return start offset of the annotation.
	 */
	public int getOffsetFrom() {
		return offsetFrom;
	}

	/**
	 * Returns the end offset of the annotation.
	 *
	 * @return end offset of the annotation.
	 */
	public int getOffsetTo() {
		return offsetTo;
	}
	
}
