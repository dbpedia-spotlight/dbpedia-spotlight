package org.dbpedia.spotlight.candidate.cooccurrence.features;

import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.TaggedText;
import org.dbpedia.spotlight.tagging.TaggedToken;

import java.util.List;

/**
 * Collection of utility methods for describing the features of a surface form occurrence.
 *
 * @author Joachim Daiber
 */
public class CandidateFeatures {


	private static final String QUOTE_PATTERN = "[”“\"']";
	private static final String PRE_QUOTE_PUNC = "[\\.,]";


	/**
	 * Returns a value expressing whether the term candidate begins with an
	 * uppercase character and is in a non-sentence-initial position.
	 *
	 * E.g.
	 * Peter drove the Ferrari. <code>true</code> for Ferrari
	 * Driving is one of Peter's favourite activities. <code>false</code> for Driving
	 *
	 * @param surfaceFormOccurrence surface form occurrence of the term candidate
	 * @return 1 if non-sentence-initial uppercase word, 0 else
	 */
	public static int nonSentenceInitialUppercase(SurfaceFormOccurrence surfaceFormOccurrence) {

		boolean isUppercase = Character.isUpperCase(surfaceFormOccurrence.surfaceForm().name().charAt(0));
		boolean isAllUppercase = surfaceFormOccurrence.surfaceForm().name().toUpperCase().equals(surfaceFormOccurrence.surfaceForm().name());
		boolean isSentenceInitial = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
				.isSentenceInitial(surfaceFormOccurrence);

		if (isAllUppercase)
			return 2;
		else if (!isSentenceInitial && isUppercase )
			return 1;
		else
			return 0;

	}


	/**
	 * Returns a value expressing whether the term candidate appears in quotation marks.
	 *
	 * @param surfaceFormOccurrence surface form occurrence of the term candidate
	 * @return 1 if term candidate is in quotation marks, 0 else
	 */

	public static int quoted(SurfaceFormOccurrence surfaceFormOccurrence) {

		String leftNeighbourToken = null;
		try {
			leftNeighbourToken = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
					.getLeftNeighbourToken(surfaceFormOccurrence).getToken();
		} catch (ItemNotFoundException e) {
			return 0;
		}

		TaggedToken rightNeighbour = null;
		try {
			rightNeighbour = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
					.getRightNeighbourToken(surfaceFormOccurrence);
		} catch (ItemNotFoundException e) {
			return 0;
		}

		return leftNeighbourToken.matches(QUOTE_PATTERN) && (rightNeighbour.getToken().matches(QUOTE_PATTERN) || rightNeighbour.getPOSTag().matches(PRE_QUOTE_PUNC)) ? 1 : 0;

	}


	/**
	 *
	 *
	 * @param textOffsetStart
	 * @param textOffsetEnd
	 * @return
	 */
	public static int isAcronym(int textOffsetStart, int textOffsetEnd) {
		//TODO implement
		return 0;
	}


	public static int followedByOf(SurfaceFormOccurrence surfaceFormOccurrence) {

		String rightNeighbourToken = null;
		try {
			rightNeighbourToken = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
					.getRightNeighbourToken(surfaceFormOccurrence).getToken();
		} catch (ItemNotFoundException e) {
			return 0;
		}

		if (rightNeighbourToken.equals("of"))
			return 1;
		else
			return 0;


	}

	public static int followedByPrep(SurfaceFormOccurrence surfaceFormOccurrence) {

		TaggedToken rightNeighbourToken = null;
		try {
			rightNeighbourToken = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
					.getRightNeighbourToken(surfaceFormOccurrence);
		} catch (ItemNotFoundException e) {
			return 0;
		}

		if (rightNeighbourToken.getPOSTag().equals("in"))
			return 1;
		else
			return 0;


	}

	public static int nextToUppercase(SurfaceFormOccurrence surfaceFormOccurrence) {

		if(nonSentenceInitialUppercase(surfaceFormOccurrence) == 0)
			return 0;

		String leftNeighbourToken = null;

		try {
			leftNeighbourToken = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
					.getLeftNeighbourToken(surfaceFormOccurrence).getToken();
		} catch (ItemNotFoundException e) {
		}

		String rightNeighbourToken = null;

		try {
			rightNeighbourToken = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
					.getRightNeighbourToken(surfaceFormOccurrence).getToken();
		} catch (ItemNotFoundException e) {
		}


		if (rightNeighbourToken != null && Character.isUpperCase(rightNeighbourToken.charAt(0))) {
			return 1;
		}else if(leftNeighbourToken != null && Character.isUpperCase(leftNeighbourToken.charAt(0))) {
			return 1;
		} else {
			return 0;
		}


	}

	public static int precededByOf(SurfaceFormOccurrence surfaceFormOccurrence) {

		String leftNeighbourToken = null;
		try {
			leftNeighbourToken  = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
					.getLeftNeighbourToken(surfaceFormOccurrence).getToken();
		} catch (ItemNotFoundException e) {
			return 0;
		}

		if (leftNeighbourToken .equals("of"))
			return 1;
		else
			return 0;

	}

	public static int precedesActiveVerb(SurfaceFormOccurrence surfaceFormOccurrence) {

		String rightNeighbourPOS = null;
		try {
			rightNeighbourPOS = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
					.getRightNeighbourToken(surfaceFormOccurrence).getPOSTag();
		} catch (ItemNotFoundException e) {
			return 0;
		}

		if (rightNeighbourPOS.startsWith("v"))
			return 1;
		else
			return 0;

	}

	public static int precededByReflexivePronoun(SurfaceFormOccurrence surfaceFormOccurrence) {
		String leftNeighbourPOS = null;
		try {
			leftNeighbourPOS  = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
					.getLeftNeighbourToken(surfaceFormOccurrence).getPOSTag();
		} catch (ItemNotFoundException e) {
			return 0;
		}

		if (leftNeighbourPOS.equals("pp$"))
			return 1;
		else
			return 0;
	}

	public static Integer prePOS(SurfaceFormOccurrence surfaceFormOccurrence) {
		TaggedToken leftNeighbour = null;

		try {
			leftNeighbour = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
					.getLeftNeighbourToken(surfaceFormOccurrence);
		} catch (ItemNotFoundException e) {
			return null;
		}

		TaggedToken rightNeighbour = null;
		try {
			rightNeighbour = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
					.getRightNeighbourToken(surfaceFormOccurrence);
		} catch (ItemNotFoundException e) {

		}


		if (leftNeighbour.getPOSTag().equals("pp$") && (rightNeighbour != null && rightNeighbour.getPOSTag().equals("in")))
			return 0;
		else if (leftNeighbour.getToken().equals("of"))
			return 2;
		else if (leftNeighbour.getPOSTag().equals("in"))
			return 1;
		else if(leftNeighbour.getToken().toLowerCase().equals("a") || leftNeighbour.getToken().toLowerCase().equals("an"))
			return 3;
		else if(leftNeighbour.getToken().toLowerCase().equals("the"))
			return 4;
		else if(leftNeighbour.getPOSTag().startsWith("j"))
			return 5;
		else
			return null;

	}

	public static Integer nextPOS(SurfaceFormOccurrence surfaceFormOccurrence) {
		TaggedToken rightNeighbour = null;

		try {
			rightNeighbour = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
					.getRightNeighbourToken(surfaceFormOccurrence);
		} catch (ItemNotFoundException e) {
			return null;
		}

		if (rightNeighbour.getPOSTag().startsWith("vb"))
			return 0;
		else if(rightNeighbour.getToken().equals("of"))
			return 1;
		else
			return null;

	}

	public static boolean isInEnumeration(SurfaceFormOccurrence surfaceFormOccurrence) {

		TaggedToken leftNeighbour = null;
		TaggedToken rightNeighbour = null;

		try {
			leftNeighbour = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
					.getLeftNeighbourToken(surfaceFormOccurrence);
			rightNeighbour = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
					.getRightNeighbourToken(surfaceFormOccurrence);
		} catch (ItemNotFoundException e) {
			return false;
		}

		if(leftNeighbour.getPOSTag().equals(",") &&  (rightNeighbour.getPOSTag().equals("cc") || rightNeighbour.getPOSTag().equals("m")))
			return true;
		else
			return false;


	}


	public static boolean isPlural(SurfaceFormOccurrence surfaceFormOccurrence) {
		List<TaggedToken> taggedTokens =
				((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider().getTaggedTokens(surfaceFormOccurrence);

		return taggedTokens.get(taggedTokens.size()-1).getPOSTag().contains("s");
	}

	public static boolean isPossessive(SurfaceFormOccurrence surfaceFormOccurrence) {
		List<TaggedToken> taggedTokens = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider().getTaggedTokens(surfaceFormOccurrence);
		return taggedTokens.size() > 0 && taggedTokens.get(0).getPOSTag().contains("$");
	}
}
