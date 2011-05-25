package org.dbpedia.spotlight.candidate.cooccurrence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.candidate.cooccurrence.features.AttributesNGram;
import org.dbpedia.spotlight.candidate.cooccurrence.features.AttributesUnigram;
import org.dbpedia.spotlight.candidate.cooccurrence.features.CandidateFeatures;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.CandidateData;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.CoOccurrenceData;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.TaggedText;
import org.dbpedia.spotlight.tagging.TaggedToken;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeUtil;
import weka.core.Attribute;
import weka.core.Instance;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Joachim Daiber
 */

public class CandidateUtil {

	static Log LOG = LogFactory.getLog(CandidateUtil.class);

	//private static final long TRIGRAM_WORD_TRESH = 250;
	//public static final long BIGRAM_RIGHT_TRESH_WEB = 250000;
	public static final long BIGRAM_LEFT_TRESH_WEB = 25000;

	public static final long BIGRAM_LEFT_SIGNIFICANCE_TRESH_WEB = 100000;
	public static final long BIGRAM_RIGHT_SIGNIFICANCE_TRESH_WEB = 100000;


	public static final long UNIGRAM_WEB_MIN = 25000000;
	public static final long UNIGRAM_CORPUS_MAX = 40000;
	public static final long TRIGRAM_RIGHT_TRESH_COUNT_WEB = 500000;
	public static final long TRIGRAM_LEFT_TRESH_COUNT_WEB = 600000;

	public static final String FUNCTION_WORD_PATTERN = "(at|,|\\.|dt|to|wp.|c.*)";


	public static Instance buildInstanceUnigram(SurfaceFormOccurrence surfaceFormOccurrence, OccurrenceDataProvider dataProvider, Instance instance, boolean verbose) {


		/**
		 * Occurrence data of the candidate
		 */

		CandidateData candidateData = null;
		try {
			candidateData = dataProvider.getCandidateData(surfaceFormOccurrence.surfaceForm().name());
		} catch (ItemNotFoundException e) {

			/**
			 * No information about the candidate available.
			 *
			 * This means that no co-occurrence data can be gathered for the term.
			 *
			 */

			System.out.println("Skipped candidate " + surfaceFormOccurrence.surfaceForm());
		}

		if (candidateData != null) {

			List<TaggedToken> leftContext = null;
			try {
				leftContext = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider().getLeftContext(surfaceFormOccurrence, 2);
			} catch (ItemNotFoundException e) {	}

			CandidateData left1 = null;
			if(leftContext.size() > 0) {
				try {
					left1 = dataProvider.getCandidateData(leftContext.get(0).getToken());
				} catch (ItemNotFoundException e) {

				}
			}

			CandidateData left2 = null;
			if(leftContext.size() > 1) {
				try {
					left2 = dataProvider.getCandidateData(leftContext.get(1).getToken());
				} catch (ItemNotFoundException e) {

				}
			}

			List<TaggedToken> rightContext = null;
			try {
				rightContext = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider().getRightContext(surfaceFormOccurrence, 2);
			} catch (ItemNotFoundException e) {	}

			CandidateData right1 = null;
			if(rightContext.size() > 0) {
				try {
					right1 = dataProvider.getCandidateData(rightContext.get(0).getToken());
				} catch (ItemNotFoundException e) {

				}
			}

			CandidateData right2 = null;
			if(rightContext.size() > 1) {
				try {
					right2 = dataProvider.getCandidateData(rightContext.get(1).getToken());
				} catch (ItemNotFoundException e) {

				}
			}


			try{
				if(candidateData.getCountCorpus() != null && candidateData.getCountCorpus() < UNIGRAM_CORPUS_MAX)
					instance.setValue(AttributesUnigram.unigram_count_corpus, candidateData.getCountCorpus());
				else
					instance.setValue(AttributesUnigram.i(AttributesUnigram.unigram_count_corpus), UNIGRAM_CORPUS_MAX);
			}catch (ArrayIndexOutOfBoundsException e) {}

			try {
				if(candidateData.getCountWeb() != null && candidateData.getCountWeb() > UNIGRAM_WEB_MIN)
					instance.setValue(AttributesUnigram.i(AttributesUnigram.unigram_count_web), candidateData.getCountWeb());
			}catch (ArrayIndexOutOfBoundsException e) {}


			/**
			 * Co-Occurrence data of the left neighbour token:
			 */


			if(left1 != null && !leftContext.get(0).getPOSTag().matches(FUNCTION_WORD_PATTERN) && !leftContext.get(0).getPOSTag().contains("$") && !leftContext.get(0).getPOSTag().equals("in")) {

				try {
					CoOccurrenceData leftBigram = dataProvider.getBigramData(left1, candidateData);

					if(leftBigram.getUnitSignificanceWeb() > BIGRAM_LEFT_SIGNIFICANCE_TRESH_WEB) {

						try {
							instance.setValue(AttributesUnigram.i(AttributesUnigram.bigram_left_count_corpus), leftBigram.getUnitCountCorpus());
						}catch (ArrayIndexOutOfBoundsException e) {}

						try{
							instance.setValue(AttributesUnigram.i(AttributesUnigram.bigram_left_significance_web), leftBigram.getUnitSignificanceWeb());
						}catch (ArrayIndexOutOfBoundsException e) {}


						try {
							instance.setValue(AttributesUnigram.i(AttributesUnigram.bigram_left_count_web), AttributesUnigram.getNominalBigramCount(leftBigram.getUnitCountWeb()));
						}catch (ArrayIndexOutOfBoundsException e) {}

						try {
							instance.setValue(AttributesUnigram.i(AttributesUnigram.bigram_left_significance_corpus), leftBigram.getUnitSignificanceCorpus());
						}catch (ArrayIndexOutOfBoundsException e) {}
					}

				} catch (ItemNotFoundException e) {

				}


			}


			/**
			 * Co-Occurrence data of the left two tokens
			 */



			if(left1 != null && left2 != null) {

				try {
					CoOccurrenceData leftTrigram = dataProvider.getTrigramData(left2, left1, candidateData);
					if(!(leftContext.get(0).getPOSTag().equals(",") || leftContext.get(1).getPOSTag().equals(",")) &&
							!(leftContext.get(0).getPOSTag().equals("in") && leftContext.get(1).getPOSTag().equals("at")))
						instance.setValue(AttributesUnigram.i(AttributesUnigram.trigram_left_count_web), leftTrigram.getUnitCountWeb());
				}
				catch (ArrayIndexOutOfBoundsException e) {}
				catch (ItemNotFoundException e) {}

			}


			/**
			 * Co-Occurrence data of the right two tokens
			 */



			if(right1 != null && right2 != null) {

				try{
					CoOccurrenceData rightTrigram = dataProvider.getTrigramData(candidateData, right1, right2);

					if(!(rightContext.get(0).getPOSTag().equals(",") || rightContext.get(1).getPOSTag().equals(",")))
						instance.setValue(AttributesUnigram.i(AttributesUnigram.trigram_right_count_web), rightTrigram.getUnitCountWeb());
				}
				catch (ArrayIndexOutOfBoundsException e) { }
				catch (ItemNotFoundException e) { }
			}




			/**
			 * Co-Occurrence data with term in the middle
			 */


			if(left1 != null && right1 != null) {
				try{
					CoOccurrenceData middleTrigram = dataProvider.getTrigramData(left1, candidateData, right1);
					if(!(leftContext.get(0).getPOSTag().equals(",") || rightContext.get(0).getPOSTag().equals(","))
							&& !(leftContext.get(0).getPOSTag().equals("in") || rightContext.get(0).getPOSTag().equals("cc"))
						//&& !(leftToken.getPOSTag().equals("at") || rightToken.getPOSTag().equals("in"))

							)
						instance.setValue(AttributesUnigram.i(AttributesUnigram.trigram_middle_count_web), middleTrigram.getUnitCountWeb());
				}
				catch (ArrayIndexOutOfBoundsException e) { }
				catch (ItemNotFoundException e) { }
			}





			/**
			 * Co-Occurrence data of the right neighbour token:
			 */

			if(right1 != null && !rightContext.get(0).getPOSTag().matches(FUNCTION_WORD_PATTERN)) {

				CoOccurrenceData rightNeighbourData = null;
				try {
					rightNeighbourData = dataProvider.getBigramData(candidateData, right1);
				} catch (ItemNotFoundException e) {

					//System.err.println("No information for " + candidateData + ", " + rightNeighbour);

				}

				if (rightNeighbourData != null && rightNeighbourData.getUnitSignificanceWeb() > BIGRAM_RIGHT_SIGNIFICANCE_TRESH_WEB) {
					try {
						instance.setValue(AttributesUnigram.i(AttributesUnigram.bigram_right_significance_corpus), rightNeighbourData.getUnitSignificanceCorpus());
					}catch (ArrayIndexOutOfBoundsException e) {}

					try{
						instance.setValue(AttributesUnigram.i(AttributesUnigram.bigram_right_count_corpus), rightNeighbourData.getUnitCountCorpus());
					}catch (ArrayIndexOutOfBoundsException e) {}

					try {
						instance.setValue(AttributesUnigram.i(AttributesUnigram.bigram_right_significance_web), rightNeighbourData.getUnitSignificanceWeb());
					}catch (ArrayIndexOutOfBoundsException e) {}

					try {
						instance.setValue(AttributesUnigram.i(AttributesUnigram.bigram_right_count_web), AttributesUnigram.getNominalBigramCount(rightNeighbourData.getUnitCountWeb()));
					}catch (ArrayIndexOutOfBoundsException e) {}

				}
			}
		}

		try {
			int uppercaseValue = CandidateFeatures.nonSentenceInitialUppercase(surfaceFormOccurrence);
			instance.setValue(AttributesUnigram.i(AttributesUnigram.non_sentence_initial_uppercase), uppercaseValue);
		}catch (ArrayIndexOutOfBoundsException e) {
			//value does not exist in header: ignore
		}

		try{
			int quotedValue = CandidateFeatures.quoted(surfaceFormOccurrence);
			instance.setValue(AttributesUnigram.i(AttributesUnigram.quoted), quotedValue);
		}catch (ArrayIndexOutOfBoundsException e) {
			//value does not exist in header: ignore
		}

		try{
			int nextToUppercase = CandidateFeatures.nextToUppercase(surfaceFormOccurrence);
			instance.setValue(AttributesUnigram.i(AttributesUnigram.next_to_uppercase), nextToUppercase);
		}catch (ArrayIndexOutOfBoundsException e) {
			//value does not exist in header: ignore
		}

		//try{
		//	List<TaggedToken> taggedTokens = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider().getTaggedTokens(surfaceFormOccurrence);
		//	int pos = AttributesUnigram.nPOSTags.indexOf(taggedTokens.get(taggedTokens.size() - 1).getPOSTag());
		//	instance.setValue(AttributesUnigram.i(AttributesUnigram.POS), pos);
		//}catch (ArrayIndexOutOfBoundsException e) {
		//	//value does not exist in header: ignore
		//}

		try {
			Integer prePOS = CandidateFeatures.prePOS(surfaceFormOccurrence);
			if (prePOS != null)
				instance.setValue(AttributesUnigram.i(AttributesUnigram.pre_pos), prePOS);
		}catch (ArrayIndexOutOfBoundsException e) {
			//value does not exist in header: ignore
		}

		try{
			Integer nextPOS = CandidateFeatures.nextPOS(surfaceFormOccurrence);
			if (nextPOS != null)
				instance.setValue(AttributesUnigram.i(AttributesUnigram.next_pos), nextPOS);
		}catch (ArrayIndexOutOfBoundsException e) {
			//value does not exist in header: ignore
		}



		try{
			Integer inEnumeration = CandidateFeatures.inEnumeration(surfaceFormOccurrence);
			if (inEnumeration != null)
				instance.setValue(AttributesUnigram.i(AttributesUnigram.in_enumeration), inEnumeration);
		}catch (ArrayIndexOutOfBoundsException e) {
			//value does not exist in header: ignore
		}

		try{
			List<TaggedToken> taggedTokens = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider().getTaggedTokens(surfaceFormOccurrence);

			if (taggedTokens.size() > 0 && taggedTokens.get(0).getPOSTag().contains("$"))
				instance.setValue(AttributesUnigram.i(AttributesUnigram.possesive), 0);
		}catch (ArrayIndexOutOfBoundsException e) {
			//value does not exist in header: ignore
		}

		if (verbose) {
			LOG.info("\n\n Classifing unigram: " + surfaceFormOccurrence);
			for(Attribute attribute : AttributesUnigram.attributeList) {
				LOG.info(attribute + ": " + instance.toString(AttributesUnigram.i(attribute)));
			}
		}


		return instance;

	}

	public static Instance buildInstanceNGram(SurfaceFormOccurrence surfaceFormOccurrence, OccurrenceDataProvider dataProvider, Instance instance, boolean verbose) {

		TaggedText text = (TaggedText) surfaceFormOccurrence.context();
		List<TaggedToken> candidateTokens = text.taggedTokenProvider().getTaggedTokens(surfaceFormOccurrence);
		int termSize = LingPipeUtil.getTokens(surfaceFormOccurrence.surfaceForm().name()).size();

		TaggedToken firstTaggedToken = candidateTokens.get(0);
		CandidateData firstTaggedTokenData = null;
		try {
			firstTaggedTokenData = dataProvider.getCandidateData(firstTaggedToken.getToken());
		} catch (ItemNotFoundException e) {
			//No information about the token!
		}


		CandidateData secondTaggedTokenData = null;
		if(candidateTokens.size() > 1){
			TaggedToken secondTaggedToken = candidateTokens.get(1);
			try {
				secondTaggedTokenData = dataProvider.getCandidateData(secondTaggedToken.getToken());
			} catch (ItemNotFoundException e) {
				//No information about the token!
			}
		}

		TaggedToken lastTaggedToken = candidateTokens.get(candidateTokens.size()-1);
		CandidateData lastTaggedTokenData = null;
		try {
			lastTaggedTokenData = dataProvider.getCandidateData(lastTaggedToken.getToken());
		} catch (ItemNotFoundException e) {
			//No information about the token!
		}


		CandidateData lastBut1TaggedTokenData = null;

		if(candidateTokens.size() > 1) {
			TaggedToken lastBut1TaggedToken = candidateTokens.get(candidateTokens.size()-2);
			try {
				lastBut1TaggedTokenData = dataProvider.getCandidateData(lastBut1TaggedToken.getToken());
			} catch (ItemNotFoundException e) {
				//No information about the token!
			}
		}


		/**
		 * Left context
		 */

		List<TaggedToken> leftContext = null;
		try {
			leftContext = text.taggedTokenProvider().getLeftContext(surfaceFormOccurrence, 2);
		} catch (ItemNotFoundException e) {

		}

		CandidateData left1 = null;
		if(leftContext != null && leftContext.size() > 0) {
			try {
				String token;
				if(leftContext.size() == 1) {
					/**
					 * There are no more tokens to the left, the token is sentence initial.
					 */
					token = leftContext.get(0).getToken().toLowerCase();

				}else{
					token = leftContext.get(0).getToken();

				}
				left1 = dataProvider.getCandidateData(token);
			} catch (ItemNotFoundException e) {
				//No information about the token
			}
		}

		CandidateData left2 = null;
		if(leftContext != null && leftContext.size() > 1) {
			try {
				left2 = dataProvider.getCandidateData(leftContext.get(1).getToken());
			} catch (ItemNotFoundException e) {
				//No information about the token
			}
		}


		/**
		 * Right context
		 */

		List<TaggedToken> rightContext = null;
		try {
			rightContext = text.taggedTokenProvider().getRightContext(surfaceFormOccurrence, 2);
		} catch (ItemNotFoundException e) {

		}

		CandidateData right1 = null;
		if(rightContext != null && rightContext.size() > 0) {
			try {
				right1 = dataProvider.getCandidateData(rightContext.get(0).getToken());
			} catch (ItemNotFoundException e) {
				//No information about the token
			}
		}

		CandidateData right2 = null;
		if(rightContext != null && rightContext.size() > 1) {
			try {
				right2 = dataProvider.getCandidateData(rightContext.get(1).getToken());
			} catch (ItemNotFoundException e){
				//No information about the token
			}
		}



		/**
		 * Features:
		 */

		if(termSize == 2) {

			try {
				if(firstTaggedTokenData != null && secondTaggedTokenData != null) {
					CoOccurrenceData bigramData = dataProvider.getBigramData(firstTaggedTokenData, secondTaggedTokenData);

					if (bigramData.getUnitCountCorpus() < 7000)
						instance.setValue(AttributesNGram.i(AttributesNGram.count_corpus), bigramData.getUnitCountCorpus());

					if (bigramData.getUnitCountWeb() > 0)
						instance.setValue(AttributesNGram.i(AttributesNGram.count_web), bigramData.getUnitCountWeb());
				}
			} catch (ItemNotFoundException e) {

			} catch (ArrayIndexOutOfBoundsException e) {}



		}



		List<String> verbs = new LinkedList<String>();

		boolean allLowercase = surfaceFormOccurrence.surfaceForm().name().toLowerCase().equals(surfaceFormOccurrence.surfaceForm().name());
		boolean allUppercase = surfaceFormOccurrence.surfaceForm().name().toUpperCase().equals(surfaceFormOccurrence.surfaceForm().name());

		int capitalizedWords = 0;

		for(TaggedToken candidateToken : candidateTokens) {
			if(candidateToken.getPOSTag().startsWith("v") || candidateToken.getPOSTag().equals("be")) {
				verbs.add(candidateToken.getPOSTag());
			}

			if(Character.isUpperCase(candidateToken.getToken().charAt(0)))
				capitalizedWords++;
		}

		try{
			if(verbs.size() > 1)
				instance.setValue(AttributesNGram.i(AttributesNGram.contains_verb), 5);
			else if(verbs.size()==0)
				instance.setValue(AttributesNGram.i(AttributesNGram.contains_verb), 0);
			else if(verbs.get(0).equals("vb"))
				instance.setValue(AttributesNGram.i(AttributesNGram.contains_verb), 1);
			else if(verbs.get(0).equals("vbd"))
				instance.setValue(AttributesNGram.i(AttributesNGram.contains_verb), 2);
			else if(verbs.get(0).equals("vbg"))
				instance.setValue(AttributesNGram.i(AttributesNGram.contains_verb), 3);
			else if(verbs.get(0).equals("vbn"))
				instance.setValue(AttributesNGram.i(AttributesNGram.contains_verb), 4);
			else if(verbs.get(0).equals("be"))
				instance.setValue(AttributesNGram.i(AttributesNGram.contains_verb), 5);
		}catch (ArrayIndexOutOfBoundsException e) {}

		try{
			if(allLowercase)
				instance.setValue(AttributesNGram.i(AttributesNGram.term_case), 0);
			else if(allUppercase)
				instance.setValue(AttributesNGram.i(AttributesNGram.term_case), 3);
			else if(capitalizedWords == candidateTokens.size())
				instance.setValue(AttributesNGram.i(AttributesNGram.term_case), 2);
			else if(capitalizedWords == 1 && Character.isUpperCase(candidateTokens.get(0).getToken().charAt(0)))
				instance.setValue(AttributesNGram.i(AttributesNGram.term_case), 4);
			else
				instance.setValue(AttributesNGram.i(AttributesNGram.term_case), 1);


		}catch (ArrayIndexOutOfBoundsException e) {}


		try{
			instance.setValue(AttributesNGram.i(AttributesNGram.term_size), termSize);
		}catch (ArrayIndexOutOfBoundsException e) {}

		try {
			TaggedToken leftNeighbourToken = text.taggedTokenProvider().getLeftNeighbourToken(surfaceFormOccurrence);

			if(leftNeighbourToken.getPOSTag().equals("to")) {
				instance.setValue(AttributesNGram.i(AttributesNGram.pre_pos), 0);
			}
			else if(leftNeighbourToken.getPOSTag().matches("[mnf].*")) {
				instance.setValue(AttributesNGram.i(AttributesNGram.pre_pos), 1);
			}else if(leftNeighbourToken.getToken().matches("[aA][nN]?")) {
				instance.setValue(AttributesNGram.i(AttributesNGram.pre_pos), 2);
			}

		} catch (ItemNotFoundException e) {

		}catch (ArrayIndexOutOfBoundsException e) {}


		try {

			if(leftContext.size() > 0) {

				if(leftContext.get(0).getPOSTag().equals("to")) {
					instance.setValue(AttributesNGram.i(AttributesNGram.pre_pos), 0);
				}
				else if(leftContext.get(0).getPOSTag().matches("[mnf].*")) {
					instance.setValue(AttributesNGram.i(AttributesNGram.pre_pos), 1);
				}else if(leftContext.get(0).getToken().matches("[aA][nN]?")) {
					instance.setValue(AttributesNGram.i(AttributesNGram.pre_pos), 2);
				}
			}

		}catch (ArrayIndexOutOfBoundsException e) {}

		try{
			if (CandidateFeatures.quoted(surfaceFormOccurrence) == 1)
				instance.setValue(AttributesNGram.i(AttributesNGram.quoted), 0);

		} catch (ArrayIndexOutOfBoundsException e) {}




		try {
			if(rightContext.size() > 0) {

				if(rightContext.get(0).getToken().equals("of")) {
					instance.setValue(AttributesNGram.i(AttributesNGram.next_pos), 0);
				}else if(rightContext.get(0).getToken().equals("to")) {
					instance.setValue(AttributesNGram.i(AttributesNGram.next_pos), 1);
				}else if(rightContext.get(0).getPOSTag().startsWith("be")) {
					instance.setValue(AttributesNGram.i(AttributesNGram.next_pos), 2);
				}else if(rightContext.get(0).getPOSTag().startsWith("v")) {
					instance.setValue(AttributesNGram.i(AttributesNGram.next_pos), 3);
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {}



		try {
			TaggedToken lastToken = candidateTokens.get(candidateTokens.size() - 1);


			if(lastToken.getPOSTag().equals("in")) {
				instance.setValue(AttributesNGram.i(AttributesNGram.ends_with), 0);
			}
		}catch (ArrayIndexOutOfBoundsException e) {}



		/**
		 * Co-Occurrence data of the left neighbour token:
		 */


		if(left1 != null && firstTaggedTokenData != null && leftContext.size() > 0 && !leftContext.get(0).getPOSTag().matches(CandidateUtil.FUNCTION_WORD_PATTERN) && !leftContext.get(0).getPOSTag().equals("in")) {

			CoOccurrenceData bigramLeft = null;
			try {
				bigramLeft = dataProvider.getBigramData(left1, firstTaggedTokenData);
			} catch (ItemNotFoundException e) {

			}

			if(bigramLeft != null && bigramLeft.getUnitSignificanceWeb() > BIGRAM_LEFT_SIGNIFICANCE_TRESH_WEB) {

				long count = bigramLeft.getUnitCountWeb();
				try{
					instance.setValue(AttributesNGram.i(AttributesNGram.bigram_left), AttributesUnigram.getNominalBigramCount(count));
				}catch (ArrayIndexOutOfBoundsException e) {}

			}
		}

		/**
		 * Co-Occurrence data for the left trigram
		 */

		if(firstTaggedTokenData != null && secondTaggedTokenData != null && left1 != null) {

			CoOccurrenceData trigramLeft = null;
			try {
				trigramLeft = dataProvider.getTrigramData(left1, firstTaggedTokenData, secondTaggedTokenData);
			} catch (ItemNotFoundException e) {

			}

			if(trigramLeft != null && trigramLeft.getUnitCountWeb() > TRIGRAM_LEFT_TRESH_COUNT_WEB) {

				try{
					instance.setValue(AttributesNGram.i(AttributesNGram.trigram_left), trigramLeft.getUnitCountWeb());
				}catch (ArrayIndexOutOfBoundsException e) {}

			}
		}


		if(lastTaggedTokenData != null && lastBut1TaggedTokenData != null && right1 != null) {

			CoOccurrenceData trigramRight = null;
			try {
				trigramRight = dataProvider.getTrigramData(lastBut1TaggedTokenData, lastTaggedTokenData, right1);
			} catch (ItemNotFoundException e) {

			} catch(NullPointerException e) {

			}

			if(trigramRight != null && trigramRight.getUnitCountWeb() > TRIGRAM_RIGHT_TRESH_COUNT_WEB) {

				try{
					instance.setValue(AttributesNGram.i(AttributesNGram.trigram_right), trigramRight.getUnitCountWeb());
				}catch (ArrayIndexOutOfBoundsException e) {}

			}
		}



		/**
		 * Co-Occurrence data of the right neighbour token:
		 */

		if(lastTaggedTokenData != null && right1 != null && !rightContext.get(0).getPOSTag().matches(CandidateUtil.FUNCTION_WORD_PATTERN) && !rightContext.get(0).getPOSTag().equals("in")) {
			CoOccurrenceData bigramRightData = null;
			try {
				bigramRightData = dataProvider.getBigramData(lastTaggedTokenData, right1);
			} catch (ItemNotFoundException e) {
				//System.err.println("No information for " + leftNeighbourToken  + ", " + candidateData);
			}

			if(bigramRightData != null && bigramRightData.getUnitSignificanceWeb() > BIGRAM_RIGHT_SIGNIFICANCE_TRESH_WEB) {

				long count = bigramRightData.getUnitCountWeb();

				try {
					instance.setValue(AttributesNGram.i(AttributesNGram.bigram_right), AttributesUnigram.getNominalBigramCount(count));
				}catch (ArrayIndexOutOfBoundsException e) {}

			}


		}


		if (verbose) {
			LOG.info("\n\n Classifing ngram: " + surfaceFormOccurrence);
			for(Attribute attribute : AttributesNGram.attributeList) {
				LOG.info(attribute + ": " + instance.toString(AttributesNGram.i(attribute)));
			}
		}

		return instance;
	}
}
