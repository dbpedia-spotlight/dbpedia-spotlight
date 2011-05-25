package org.dbpedia.spotlight.tagging;

import com.aliasi.util.Pair;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;

import java.util.List;


/**
 * A TaggedTokenProvider provides Part-of-Speech data for a range specified by two text offsets.
 * <p/>
 * General interface for providing TaggedToken objects. A TaggedToken contains the token,
 * the proposed part-of-speech tag, a confidence value and possible alternative tags.
 * <p/>
 * A TaggedTokenProvider will be initialized with a text and will return lists of
 * TaggedTokens for the words between two character positions in the text.
 *
 * @author jodaiber
 */
public interface TaggedTokenProvider {


	/**
	 * Returns a List of {@link TaggedToken}s between the positions given as start and
	 * end text offsets.
	 *
	 * @param textOffsetStart text offset for start of candidate term
	 * @param textOffsetEnd   text offset for end of candidate term
	 * @return List of TaggedTokens
	 */
	public List<TaggedToken> getTaggedTokens(int textOffsetStart, int textOffsetEnd);

	
	/**
	 * Returns a List of {@link TaggedToken}s between the positions given as start and
	 * end text offsets.
	 *
	 * @param surfaceFormOccurrence surface form occurrence of the term candidate
	 * @return List of TaggedTokens
	 */
	public List<TaggedToken> getTaggedTokens(SurfaceFormOccurrence surfaceFormOccurrence);


	/**
	 * Returns the immediate left neighbour of the term candidate (defined by start
	 * and end text offset).
	 *
	 * @param textOffsetStart text offset for start of candidate term
	 * @param textOffsetEnd   text offset for end of candidate term
	 * @return TaggedToken immediately left of a candidate
	 */
	public TaggedToken getLeftNeighbourToken(int textOffsetStart, int textOffsetEnd) throws ItemNotFoundException;


	/**
	 * Returns the immediate left neighbour of the term candidate (defined by start
	 * and end text offset).
	 *
	 * @param surfaceFormOccurrence surface form occurrence of the term candidate
	 * @return TaggedToken immediately left of a candidate
	 */
	public TaggedToken getLeftNeighbourToken(SurfaceFormOccurrence surfaceFormOccurrence) throws ItemNotFoundException;


	/**
	 * Returns the immediate right neighbour of the term candidate (defined by start
	 * and end text offset).
	 *
	 * @param textOffsetStart text offset for start of candidate term
	 * @param textOffsetEnd   text offset for end of candidate term
	 * @return TaggedToken immediately left of a candidate
	 */
	public TaggedToken getRightNeighbourToken(int textOffsetStart, int textOffsetEnd) throws ItemNotFoundException;


	/**
	 * Returns the immediate right neighbour of the term candidate (defined by start
	 * and end text offset).
	 *
	 * @param surfaceFormOccurrence surface form occurrence of the term candidate
	 * @return TaggedToken immediately left of a candidate
	 */
	public TaggedToken getRightNeighbourToken(SurfaceFormOccurrence surfaceFormOccurrence) throws ItemNotFoundException;


	/**
	 * <p>
	 * Return a List of tokens in the left context of a surface form. The integers <code>start</code> and <code>end</code>
	 * define the range of tokens to be returned where 0 is the surface form.
	 * </p>
	 * <p>
	 * getContextTokens(surfaceForm, -1, 1)
	 *     returns one token to the left and one token to the right of the surface form
	 * </p>
	 *
	 * @param surfaceFormOccurrence Surface form occurence of the term candidate
	 * @return
	 */
	public List<TaggedToken> getLeftContext(SurfaceFormOccurrence surfaceFormOccurrence, int length) throws ItemNotFoundException;
	public List<TaggedToken> getLeftContext(int textOffsetStart, int textOffsetEnd, int length) throws ItemNotFoundException;

	public List<TaggedToken> getRightContext(SurfaceFormOccurrence surfaceFormOccurrence, int length) throws ItemNotFoundException;


	/**
	 * Returns {@link TaggedToken}s for the sentence containing the term candidate (defined by start and
	 * end text offset).
	 *
	 * @param textOffsetStart text offset for start of candidate term
	 * @param textOffsetEnd   text offset for end of candidate term
	 * @return List of TaggedTokens in the sentence
	 */
	public List<TaggedToken> getSentenceTokens(int textOffsetStart, int textOffsetEnd) throws ItemNotFoundException;

	/**
	 * Returns {@link TaggedToken}s for the sentence containing the term candidate (defined by start and
	 * end text offset).
	 *
	 * @param surfaceFormOccurrence surface form occurrence of the term candidate
	 * @return List of TaggedTokens in the sentence
	 */
	public List<TaggedToken> getSentenceTokens(SurfaceFormOccurrence surfaceFormOccurrence) throws ItemNotFoundException;
	
	public Pair<String, Integer> getSentence(int textOffsetStart, int textOffsetEnd) throws ItemNotFoundException;
	public Pair<String, Integer> getSentence(SurfaceFormOccurrence surfaceFormOccurrence) throws ItemNotFoundException;


	/**
	 * Is the term candidate the first token in a sentence?
	 *
	 * @param textOffsetStart text offset for start of candidate term
	 * @param textOffsetEnd   text offset for end of candidate term
	 * @return true iff the term candidate is at the beginning of the sentence
	 */
	public boolean isSentenceInitial(int textOffsetStart, int textOffsetEnd);

	/**
	 * Is the term candidate the first token in a sentence?
	 *
	 * @param surfaceFormOccurrence surface form occurrence of the term candidate
	 * @return true iff the term candidate is at the beginning of the sentence
	 */
	public boolean isSentenceInitial(SurfaceFormOccurrence surfaceFormOccurrence);


	/**
	 * Initialize the TaggedTokenProvider.
	 *
	 * @param text String that should be tagged.
	 */
	public void initialize(String text);


	public List<SurfaceFormOccurrence> getUnigramCandidates();
}
