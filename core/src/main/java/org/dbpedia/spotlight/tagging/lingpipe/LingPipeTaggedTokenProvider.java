package org.dbpedia.spotlight.tagging.lingpipe;

import com.aliasi.sentences.SentenceModel;
import com.aliasi.tag.Tagger;
import com.aliasi.tag.Tagging;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterPOS;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.tagging.TaggedToken;
import org.dbpedia.spotlight.tagging.TaggedTokenProvider;

import java.util.*;


/**
 * TaggedToken provider based on LingPipe.
 * <p/>
 * This implementation uses LingPipe to do tokenization, sentence detection and Part-of-Speech
 * tagging.
 * <p/>
 * Once the text is tagged (initialize()), the part-of-speech tags for a range in the text can be
 * retrieved by using getTaggedTokens(int textOffsetStart, int textOffsetEnd) in O(log n) time.
 *
 * @author jodaiber
 */

public class LingPipeTaggedTokenProvider implements TaggedTokenProvider {

	private Log LOG = LogFactory.getLog(this.getClass());

	private List<TaggedToken> taggedTokens;
	private int[] sentenceBoundaries;


	public LingPipeTaggedTokenProvider() {

	}


	@Override
	public List<TaggedToken> getTaggedTokens(int textOffsetStart, int textOffsetEnd) {

		int firstTaggedToken = getFirstTaggedTokenAfterOffset(textOffsetStart);

		//Gather all tokens to be returned
		int i = firstTaggedToken + 1;
		while (i < taggedTokens.size() - 1
				&& (taggedTokens.get(i).getOffset() + taggedTokens.get(i).getToken().length()) <= textOffsetEnd) {

			i++;
		}

		List<TaggedToken> taggedTokensInRange = taggedTokens.subList(firstTaggedToken, i);

		return taggedTokensInRange;
	}

	@Override
	public List<TaggedToken> getTaggedTokens(SurfaceFormOccurrence surfaceFormOccurrence) {
		return getTaggedTokens(surfaceFormOccurrence.textOffset(),
				surfaceFormOccurrence.textOffset() + surfaceFormOccurrence.surfaceForm().name().length());
	}


	/**
	 * Get the list position of the first token after the offset specified as
	 * textOffsetStart. If textOffsetStart is inside of a token, this token
	 * will be returned.
	 *
	 * @param textOffsetStart	text offset of the start of the term
	 * @return list offset of the first token
	 */
	private int getFirstTaggedTokenAfterOffset(Integer textOffsetStart) {
		int posPositionOfOffset = Collections.binarySearch(taggedTokens, textOffsetStart);

		int firstTaggedToken;
		if (posPositionOfOffset >= 0) {

			//An exact match was found
			firstTaggedToken = posPositionOfOffset;
		} else {

			/**
			 * No exact match was found, take the next token or the token
			 * that the annotation is part of.
			 */

			if(taggedTokens.get((posPositionOfOffset * -1) -2).getOffset()
					+ taggedTokens.get((posPositionOfOffset * -1) -2).getToken().length() > textOffsetStart)
				firstTaggedToken = ((posPositionOfOffset * -1) - 2);
			else
				firstTaggedToken = (posPositionOfOffset * -1) - 1;
		}

		return firstTaggedToken;
	}

	@Override
	public TaggedToken getLeftNeighbourToken(int textOffsetStart, int textOffsetEnd) throws ItemNotFoundException {

		try {
			return getLeftContext(textOffsetStart, textOffsetEnd, 1).get(0);
		}catch (IndexOutOfBoundsException e) {
			throw new ItemNotFoundException("No left neighbour token.");
		}

	}

	@Override
	public TaggedToken getLeftNeighbourToken(SurfaceFormOccurrence surfaceFormOccurrence) throws ItemNotFoundException {
		return getLeftNeighbourToken(surfaceFormOccurrence.textOffset(),
				surfaceFormOccurrence.textOffset() + surfaceFormOccurrence.surfaceForm().name().length());
	}

	@Override
	public TaggedToken getRightNeighbourToken(int textOffsetStart, int textOffsetEnd) throws ItemNotFoundException {

		try {
			return getRightContext(textOffsetStart, textOffsetEnd, 1).get(0);
		}catch (IndexOutOfBoundsException e) {
			throw new ItemNotFoundException("No left neighbour token.");
		}


	}


	@Override
	public TaggedToken getRightNeighbourToken(SurfaceFormOccurrence surfaceFormOccurrence) throws ItemNotFoundException {
		return getRightNeighbourToken(surfaceFormOccurrence.textOffset(),
				surfaceFormOccurrence.textOffset() + surfaceFormOccurrence.surfaceForm().name().length());
	}
	

	@Override
	public List<TaggedToken> getLeftContext(SurfaceFormOccurrence surfaceFormOccurrence, int length) throws ItemNotFoundException {

		return getLeftContext(surfaceFormOccurrence.textOffset(),
				surfaceFormOccurrence.textOffset() + surfaceFormOccurrence.surfaceForm().name().length(), length);

	}

	public List<TaggedToken> getLeftContext(int textOffsetStart, int textOffsetEnd, int length) throws ItemNotFoundException {
		Pair<Integer, Integer> sentencePosition = getSentencePosition(textOffsetStart, textOffsetEnd);

		int firstCandidateToken = getFirstTaggedTokenAfterOffset(textOffsetStart);
		int firstContextToken = Math.max(firstCandidateToken - length, sentencePosition.a());


		List<TaggedToken> leftContext = new LinkedList<TaggedToken>();
		List<TaggedToken> leftContextTokens = taggedTokens.subList(firstContextToken, firstCandidateToken);

		for(int i = leftContextTokens.size() - 1; i >= 0; i--) {
			leftContext.add(leftContextTokens.get(i));
		}

		return leftContext;
	}


	@Override
	public List<TaggedToken> getRightContext(SurfaceFormOccurrence surfaceFormOccurrence, int length) throws ItemNotFoundException {

		return getRightContext(surfaceFormOccurrence.textOffset(),
				surfaceFormOccurrence.textOffset() + surfaceFormOccurrence.surfaceForm().name().length(), length);

	}

	public List<TaggedToken> getRightContext(int textOffsetStart, int textOffsetEnd, int length) throws ItemNotFoundException {

		Pair<Integer, Integer> sentencePosition = getSentencePosition(textOffsetStart, textOffsetEnd);
		int firstContextToken = Math.min(sentencePosition.b(), getFirstTaggedTokenAfterOffset(textOffsetEnd - 1) + 1);
		int lastContextToken = Math.min(sentencePosition.b(), firstContextToken + length);

		return taggedTokens.subList(firstContextToken, lastContextToken);
		
	}



	private Pair<Integer, Integer> getSentencePosition(SurfaceFormOccurrence surfaceFormOccurrence)
			throws ItemNotFoundException {
		
		return getSentencePosition(surfaceFormOccurrence.textOffset(),
				surfaceFormOccurrence.textOffset() + surfaceFormOccurrence.surfaceForm().name().length());
	}


	public Pair<Integer, Integer> getSentencePosition(int textOffsetStart, int textOffsetEnd) throws ItemNotFoundException {

		int firstTaggedToken = getFirstTaggedTokenAfterOffset(textOffsetStart);

		int sentenceStart = 0;
		int sentenceEnd = 0;

		for (int currentSentenceEnd : sentenceBoundaries) {
			
			if(currentSentenceEnd >= firstTaggedToken) {
				sentenceEnd = currentSentenceEnd;
				break;
			}

			sentenceStart = currentSentenceEnd + 1;
		}

		if(sentenceEnd >= firstTaggedToken)
			return new Pair<Integer, Integer>(sentenceStart, sentenceEnd);
		else
			return new Pair<Integer, Integer>(sentenceStart, taggedTokens.size() - 1);



	//	/**
	//	 * The sentence was not found, there may be only a single sentence
	//	 */
	//	if(sentenceBoundaries.length == 0)
	//		return new Pair<Integer, Integer>(0, taggedTokens.size() - 1);
	//	else
		//throw new ItemNotFoundException("Could not find sentence");
		
	}

	
	@Override
	public List<TaggedToken> getSentenceTokens(int textOffsetStart, int textOffsetEnd)
			throws ItemNotFoundException {

		Pair<Integer, Integer> sentencePosition = getSentencePosition(textOffsetStart, textOffsetEnd);

		if(sentencePosition != null)
			return taggedTokens.subList(sentencePosition.a(), sentencePosition.b() + 1);
		else
			return null;

	}
	

	@Override
	public List<TaggedToken> getSentenceTokens(SurfaceFormOccurrence surfaceFormOccurrence)
			throws ItemNotFoundException {
		
		return getSentenceTokens(surfaceFormOccurrence.textOffset(),
				surfaceFormOccurrence.textOffset() + surfaceFormOccurrence.surfaceForm().name().length());
	}

	@Override
	public Pair<String, Integer> getSentence(int textOffsetStart, int textOffsetEnd) throws ItemNotFoundException {

		List<TaggedToken> sentenceTokens = getSentenceTokens(textOffsetStart, textOffsetEnd);

		StringBuilder sentence = new StringBuilder();
		for(TaggedToken taggedToken : sentenceTokens) {
			sentence.append(taggedToken.getToken());
			sentence.append(taggedToken.getWhite());
		}

		int sentenceOffset = sentenceTokens.get(0).getOffset();

		return new Pair<String, Integer>(sentence.toString(), sentenceOffset);
	}

	@Override
	public Pair<String, Integer> getSentence(SurfaceFormOccurrence surfaceFormOccurrence) throws ItemNotFoundException {
		return getSentence(surfaceFormOccurrence.textOffset(),
				surfaceFormOccurrence.textOffset() + surfaceFormOccurrence.surfaceForm().name().length());
	}


	@Override
	public boolean isSentenceInitial(int textOffsetStart, int textOffsetEnd) {

		int startToken = getFirstTaggedTokenAfterOffset(textOffsetStart);
		boolean isSentenceInitial = Arrays.binarySearch(sentenceBoundaries, startToken - 1) > 0;

		return isSentenceInitial;

	}

	@Override
	public boolean isSentenceInitial(SurfaceFormOccurrence surfaceFormOccurrence) {

		return isSentenceInitial(surfaceFormOccurrence.textOffset(),
				surfaceFormOccurrence.textOffset() + surfaceFormOccurrence.surfaceForm().name().length());


	}


	@Override
	public void initialize(String text) {

		taggedTokens = new ArrayList<TaggedToken>();

		//Load the POS model:
		Tagger posTagger = LingPipeFactory.createPOSTagger();

		//1.) Tokenization
		long start = System.currentTimeMillis();
		List<String> tokenList = new ArrayList<String>();
		List<String> whiteList = new ArrayList<String>();
		Tokenizer tokenizer = LingPipeFactory.getTokenizerFactory().tokenizer(text.toCharArray(),
				0, text.length());
		tokenizer.tokenize(tokenList, whiteList);
		LOG.trace("Tokenization took " + (System.currentTimeMillis() - start) + "ms.");


		//2.) Sentence detection
		start = System.currentTimeMillis();
		String[] tokens = new String[tokenList.size()];
		String[] whites = new String[whiteList.size()];
		tokenList.toArray(tokens);
		whiteList.toArray(whites);

		SentenceModel sentenceModel = LingPipeFactory.createSentenceModel();
		sentenceBoundaries = sentenceModel.boundaryIndices(tokens, whites);
		LOG.trace("Sentence segmentation took " + (System.currentTimeMillis() - start) + "ms.");


		//3.) Part-of-Speech tagging
		start = System.currentTimeMillis();
		int sentStartToken = 0;
		int sentEndToken;
		int textOffset = whites[0].length();


		/**
		 * Tag every sentence with final punctuation (i < sentenceBoundaries.length), if there is
		 * text without final punctuation, treat the rest of the text as a single sentence.
		 */

		for (int i = 0; (i < sentenceBoundaries.length || sentStartToken < tokens.length); ++i) {

			if (i < sentenceBoundaries.length) {
				//We are between two sentence-final punctuation tokens.

				sentEndToken = sentenceBoundaries[i];
			} else {
				//We are beyond the last sentence-final punctuation: Tag the rest of the text.

				sentEndToken = tokens.length - 1;
			}


			Tagging<String> tags = posTagger.tag(tokenList.subList(sentStartToken, sentEndToken + 1));
			for (int j = 0; j < tags.size(); j++) {
				TaggedToken taggedToken = new TaggedToken(tags.token(j), whiteList.get(sentStartToken + j + 1), tags.tag(j), textOffset, null);
				taggedTokens.add(taggedToken);
				textOffset += tokens[sentStartToken + j].length() + whites[sentStartToken + j + 1].length();
			}

			sentStartToken = sentEndToken + 1;
		}

		LOG.trace("POS tagging took " + (System.currentTimeMillis() - start) + "ms.");

	}

	@Override
	public List<SurfaceFormOccurrence> getUnigramCandidates() {
		FilterPOS filterPOS = new FilterPOS();
		List<SurfaceFormOccurrence> surfaceFormOccurrences = new LinkedList<SurfaceFormOccurrence>();

		for(TaggedToken taggedToken : taggedTokens) {

			if(!filterPOS.isOnUnigramBlacklist(taggedToken.getPOSTag())) {
				surfaceFormOccurrences.add(new SurfaceFormOccurrence(new SurfaceForm(taggedToken.getToken()), null, taggedToken.getOffset()));
			}

		}

		return surfaceFormOccurrences;
	}


}
