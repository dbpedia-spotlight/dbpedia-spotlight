package org.dbpedia.spotlight.tagging.lingpipe;

/**
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

	public String getString() {
		return string;
	}

	public String getAnnotation() {
		return string.substring(offsetFrom, offsetTo);
	}

	public int getOffsetFrom() {
		return offsetFrom;
	}

	public int getOffsetTo() {
		return offsetTo;
	}
}
