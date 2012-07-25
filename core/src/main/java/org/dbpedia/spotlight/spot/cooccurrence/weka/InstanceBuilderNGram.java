package org.dbpedia.spotlight.spot.cooccurrence.weka;

import org.dbpedia.spotlight.spot.cooccurrence.features.CandidateFeatures;
import org.dbpedia.spotlight.spot.cooccurrence.features.data.CandidateData;
import org.dbpedia.spotlight.spot.cooccurrence.features.data.CoOccurrenceData;
import org.dbpedia.spotlight.spot.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.TaggedText;
import org.dbpedia.spotlight.tagging.TaggedToken;
import weka.core.Attribute;
import weka.core.Instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * Abstract Builder for WEKA instances for n-grams.
 *
 * @author Joachim Daiber
 */
public abstract class InstanceBuilderNGram extends InstanceBuilder {

	public static Attribute count_corpus = new Attribute("count_corpus");
	public static Attribute count_web = new Attribute("count_web");
	public static Attribute contains_verb = new Attribute("contains_verb", Arrays.asList("no", "vb", "vbd", "vbg", "vbn", "be", "multiple"));
	public static Attribute term_case = new Attribute("case", Arrays.asList("all_lowercase", "mixed", "titlecase", "all_uppercase", "first_uppercase"));
	public static Attribute quoted = new Attribute("quoted", Arrays.asList("yes"));
	public static Attribute candidate_size = new Attribute("candidate_size");
	public static Attribute pre_pos = new Attribute("token_left", Arrays.asList("to", "verb", "a"));
	public static Attribute next_pos = new Attribute("token_right", Arrays.asList("of", "to", "be", "verb"));
	public static Attribute ends_with = new Attribute("ends_with", Arrays.asList("prep"));
	public static Attribute bigram_left_significance_web = new Attribute("bigram_left_signifance_web"); //, AttributesUnigram.BIGRAM_COUNTS_WEB);
	public static Attribute bigram_right_significance_web = new Attribute("bigram_right_significance_web"); //, AttributesUnigram.BIGRAM_COUNTS_WEB);
	public static Attribute trigram_left = new Attribute("trigram_left");
	public static Attribute trigram_right = new Attribute("trigram_right");


	/**
	 * Default Thresholds:
	 */
	protected long bigramLeftWebMin = 0;
	protected long bigramRightWebMin = 0;
	protected long trigramLeftWebMin = 0;
	protected long trigramMiddleWebMin = 0;
	protected long trigramRightWebMin = 0;


	/**
	 * Create an instance builder for n-grams.
	 * 
	 * @param dataProvider occurrence data provider
	 */
	protected InstanceBuilderNGram(OccurrenceDataProvider dataProvider) {
		super(dataProvider);

	}

	@Override
	/** {@inheritDoc} */
	public ArrayList<Attribute> buildAttributeList() {

		ArrayList<Attribute> attributeList = new ArrayList<Attribute>();
		attributeList.addAll(Arrays.asList(
			contains_verb,
			term_case,
			candidate_size,
			pre_pos,
			next_pos,
			ends_with,
			quoted,
			bigram_left_significance_web,
			bigram_right_significance_web,
			trigram_left,
			trigram_right,
			candidate_class
		));
		
		return attributeList;
	}


	@Override
	/** {@inheritDoc} */
	public Instance buildInstance(SurfaceFormOccurrence surfaceFormOccurrence, Instance instance) {

		TaggedText text = (TaggedText) surfaceFormOccurrence.context();
		List<TaggedToken> candidateTokens = text.taggedTokenProvider().getTaggedTokens(surfaceFormOccurrence);
		int termSize = candidateTokens.size();

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
		} catch (ItemNotFoundException ignored) {}

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


		/**
		 * Right context
		 */

		List<TaggedToken> rightContext = null;
		try {
			rightContext = text.taggedTokenProvider().getRightContext(surfaceFormOccurrence, 2);
		} catch (ItemNotFoundException ignored) {}

		CandidateData right1 = null;
		if(rightContext != null && rightContext.size() > 0) {
			try {
				right1 = dataProvider.getCandidateData(rightContext.get(0).getToken());
			} catch (ItemNotFoundException e) {
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

					//if (bigramData.getUnitCountWeb() > bigramLeftWebMin)
						instance.setValue(i(count_web, buildAttributeList()), bigramData.getUnitCountWeb());
				}
			} catch (ItemNotFoundException ignored) {}
			catch (ArrayIndexOutOfBoundsException ignored) {}



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
				instance.setValue(i(contains_verb, buildAttributeList()), 5);
			else if(verbs.size()==0)
				instance.setValue(i(contains_verb, buildAttributeList()), 0);
			else if(verbs.get(0).equals("vb"))
				instance.setValue(i(contains_verb, buildAttributeList()), 1);
			else if(verbs.get(0).equals("vbd"))
				instance.setValue(i(contains_verb, buildAttributeList()), 2);
			else if(verbs.get(0).equals("vbg"))
				instance.setValue(i(contains_verb, buildAttributeList()), 3);
			else if(verbs.get(0).equals("vbn"))
				instance.setValue(i(contains_verb, buildAttributeList()), 4);
			else if(verbs.get(0).equals("be"))
				instance.setValue(i(contains_verb, buildAttributeList()), 5);
		} catch (ArrayIndexOutOfBoundsException ignored) {}

		try{
			if(allLowercase)
				instance.setValue(i(term_case, buildAttributeList()), 0);
			else if(allUppercase)
				instance.setValue(i(term_case, buildAttributeList()), 3);
			else if(capitalizedWords == candidateTokens.size())
				instance.setValue(i(term_case, buildAttributeList()), 2);
			else if(capitalizedWords == 1 && Character.isUpperCase(candidateTokens.get(0).getToken().charAt(0)))
				instance.setValue(i(term_case, buildAttributeList()), 4);
			else
				instance.setValue(i(term_case, buildAttributeList()), 1);


		} catch (ArrayIndexOutOfBoundsException ignored) {}


		try{
			instance.setValue(i(candidate_size, buildAttributeList()), termSize);
		} catch (ArrayIndexOutOfBoundsException ignored) {}

		try {
			TaggedToken leftNeighbourToken = text.taggedTokenProvider().getLeftNeighbourToken(surfaceFormOccurrence);

			if(leftNeighbourToken.getPOSTag().equals("to")) {
				instance.setValue(i(pre_pos, buildAttributeList()), 0);
			}
			else if(leftNeighbourToken.getPOSTag().matches("[mnf].*")) {
				instance.setValue(i(pre_pos, buildAttributeList()), 1);
			}else if(leftNeighbourToken.getToken().matches("[aA][nN]?")) {
				instance.setValue(i(pre_pos, buildAttributeList()), 2);
			}

		} catch (ItemNotFoundException ignored) {

		} catch (ArrayIndexOutOfBoundsException ignored) {}


		try {

			if(leftContext.size() > 0) {

				if(leftContext.get(0).getPOSTag().equals("to")) {
					instance.setValue(i(pre_pos, buildAttributeList()), 0);
				}
				else if(leftContext.get(0).getPOSTag().matches("[mnf].*")) {
					instance.setValue(i(pre_pos, buildAttributeList()), 1);
				}else if(leftContext.get(0).getToken().matches("[aA][nN]?")) {
					instance.setValue(i(pre_pos, buildAttributeList()), 2);
				}
			}

		} catch (ArrayIndexOutOfBoundsException ignored) {}

		try{
			if (CandidateFeatures.quoted(surfaceFormOccurrence) == 1)
				instance.setValue(i(quoted, buildAttributeList()), 0);

		} catch (ArrayIndexOutOfBoundsException ignored) {}




		try {
			if(rightContext.size() > 0) {

				if(rightContext.get(0).getToken().equals("of")) {
					instance.setValue(i(next_pos, buildAttributeList()), 0);
				}else if(rightContext.get(0).getToken().equals("to")) {
					instance.setValue(i(next_pos, buildAttributeList()), 1);
				}else if(rightContext.get(0).getPOSTag().startsWith("be")) {
					instance.setValue(i(next_pos, buildAttributeList()), 2);
				}else if(rightContext.get(0).getPOSTag().startsWith("v")) {
					instance.setValue(i(next_pos, buildAttributeList()), 3);
				}
			}
		} catch (ArrayIndexOutOfBoundsException ignored) {}



		try {
			TaggedToken lastToken = candidateTokens.get(candidateTokens.size() - 1);


			if(lastToken.getPOSTag().equals("in")) {
				instance.setValue(i(ends_with, buildAttributeList()), 0);
			}
		} catch (ArrayIndexOutOfBoundsException ignored) {}



		/**
		 * Co-Occurrence data of the left neighbour token:
		 */


		if(left1 != null && firstTaggedTokenData != null && leftContext.size() > 0 && !leftContext.get(0).getPOSTag().matches(FUNCTION_WORD_PATTERN) && !leftContext.get(0).getPOSTag().equals("in")) {

			CoOccurrenceData bigramLeft = null;
			try {
				bigramLeft = dataProvider.getBigramData(left1, firstTaggedTokenData);
			} catch (ItemNotFoundException ignored) {}

			if(bigramLeft != null && bigramLeft.getUnitSignificanceWeb() > bigramLeftWebMin) {

				try{
					instance.setValue(i(bigram_left_significance_web, buildAttributeList()), bigramLeft.getUnitSignificanceWeb());
				} catch (ArrayIndexOutOfBoundsException ignored) {}

			}
		}

		/**
		 * Co-Occurrence data for the left trigram
		 */

		if(firstTaggedTokenData != null && secondTaggedTokenData != null && left1 != null) {

			CoOccurrenceData trigramLeft = null;
			try {
				trigramLeft = dataProvider.getTrigramData(left1, firstTaggedTokenData, secondTaggedTokenData);
			} catch (ItemNotFoundException ignored) {}

			if(trigramLeft != null && trigramLeft.getUnitCountWeb() > trigramLeftWebMin) {

				try{
					instance.setValue(i(trigram_left, buildAttributeList()), trigramLeft.getUnitCountWeb());
				} catch (ArrayIndexOutOfBoundsException ignored) {}

			}
		}


		if(lastTaggedTokenData != null && lastBut1TaggedTokenData != null && right1 != null) {

			CoOccurrenceData trigramRight = null;
			try {
				trigramRight = dataProvider.getTrigramData(lastBut1TaggedTokenData, lastTaggedTokenData, right1);
			} catch (ItemNotFoundException ignored) {}
			catch(NullPointerException ignored) {}

			if(trigramRight != null && trigramRight.getUnitCountWeb() > trigramRightWebMin) {

				try{
					instance.setValue(i(trigram_right, buildAttributeList()), trigramRight.getUnitCountWeb());
				} catch (ArrayIndexOutOfBoundsException ignored) {}

			}
		}



		/**
		 * Co-Occurrence data of the right neighbour token:
		 */

		if(lastTaggedTokenData != null && right1 != null && !rightContext.get(0).getPOSTag().matches(FUNCTION_WORD_PATTERN) && !rightContext.get(0).getPOSTag().equals("in")) {
			CoOccurrenceData bigramRight = null;
			try {
				bigramRight = dataProvider.getBigramData(lastTaggedTokenData, right1);
			} catch (ItemNotFoundException ignored) {}

			if(bigramRight != null && bigramRight.getUnitSignificanceWeb() > bigramRightWebMin) {

				try {
					instance.setValue(i(bigram_right_significance_web, buildAttributeList()), bigramRight.getUnitSignificanceWeb());
				} catch (ArrayIndexOutOfBoundsException ignored) {}

			}
		}


		if (verboseMode)
			explain(surfaceFormOccurrence, instance);

		return instance;
	}

}
