package org.dbpedia.spotlight.tagging;

/**
 * Part-of-speech data for a token.
 * <p/>
 * This is a wrapper class for part-of-speech data produced by a POS tagger.
 * It includes the token, text offset and the part-of-speech tag. If the tagger
 * supports this feature, it may also include a confidence value.
 *
 * @author Joachim Daiber
 */

public class TaggedToken implements Comparable<Integer> {

	public TaggedToken(String token, String white, String POSTag, int offset, Float confidence) {
		this.token = token;
		this.white = white;
		this.offset = offset;
		this.POSTag = POSTag;
		this.confidence = confidence;
	}

	private String token;
	private String white;
	private String POSTag;
	private int offset;
	private Float confidence;

	@Override
	public int compareTo(Integer o) {
		if (offset < o)
			return -1;
		else if (offset > o)
			return 1;
		else
			return 0;
	}

	@Override
	public String toString() {
		return token + '/' + POSTag + ' ';
	}


	/**
	 * Returns the token.
	 *
	 * @return the token
	 */
	public String getToken() {
		return token;
	}


	/**
	 * Returns the part-of-speech tag corresponding to the token. The part-of-speech tag
	 * may be null (the tagger was not confident enough to assign a tag).
	 *
	 * @return part-of-speech tag or <code>null</code>
	 */
	public String getPOSTag() {
		return POSTag;
	}


	/**
	 * Returns the text offset of the TaggedToken
	 *
	 * @return text offset (characters from start of the text)
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * Returns the white-space elements after the token.
	 *
	 * @return
	 */
	public String getWhite() {
		return white;
	}

	/**
	 * Returns the tagger's confidence for the part-of-speech tag that was assigned.
	 * <p/>
	 * If the tagger does not provide a confidence, this value is <code>null</code>
	 *
	 * @return tagger's confidence for the assignment or <code>null</code>
	 */
	public Float getConfidence() {
		return confidence;
	}

}
