package org.dbpedia.spotlight.candidate.cooccurrence.features.training;

import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClass;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.TaggedText;
import org.dbpedia.spotlight.tagging.TaggedToken;

import java.util.List;

/**
 * An instance of a surface form occurrence is a single surface form occurence that contains relevant
 * information for a human annotator.
 *
 * @author Joachim Daiber
 */

public class OccurrenceInstance extends SurfaceFormOccurrence {
	
	private String annotationURI;
	private String annotationTitle;
	private String annotationAbstract;
	private CandidateClass candidateClass;
	private int offset;
	private String surfaceForm;
	private TaggedText text;


	public OccurrenceInstance(String surfaceForm, int offset, TaggedText text, String annotationURI,
							  String annotationTitle, String annotationAbstract,
							  CandidateClass candidateClass) {

		super(new SurfaceForm(surfaceForm), text, offset);

		this.annotationURI = annotationURI;
		this.annotationTitle = annotationTitle;
		this.annotationAbstract = annotationAbstract;
		this.candidateClass = candidateClass;
		this.offset = offset;
		this.text = text;
		this.surfaceForm = surfaceForm;
	}

	public String getAnnotationURI() {
		return annotationURI;
	}

	public void setAnnotationURI(String annotationURI) {
		this.annotationURI = annotationURI;
	}

	public String getAnnotationTitle() {
		return annotationTitle;
	}

	public void setAnnotationTitle(String annotationTitle) {
		this.annotationTitle = annotationTitle;
	}

	public String getAnnotationAbstract() {
		return annotationAbstract;
	}

	public void setAnnotationAbstract(String annotationAbstract) {
		this.annotationAbstract = annotationAbstract;
	}

	public CandidateClass getCandidateClass() {
		return candidateClass;
	}

	public void setCandidateClass(CandidateClass candidateClass) {
		this.candidateClass = candidateClass;
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof OccurrenceInstance)) return false;
		if (!super.equals(o)) return false;

		OccurrenceInstance that = (OccurrenceInstance) o;

		if (annotationAbstract != null ? !annotationAbstract.equals(that.annotationAbstract) : that.annotationAbstract != null)
			return false;
		if (annotationTitle != null ? !annotationTitle.equals(that.annotationTitle) : that.annotationTitle != null)
			return false;
		if (annotationURI != null ? !annotationURI.equals(that.annotationURI) : that.annotationURI != null)
			return false;
		if (candidateClass != that.candidateClass) return false;

		return true;
	}

	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (annotationURI != null ? annotationURI.hashCode() : 0);
		result = 31 * result + (annotationTitle != null ? annotationTitle.hashCode() : 0);
		result = 31 * result + (annotationAbstract != null ? annotationAbstract.hashCode() : 0);
		result = 31 * result + candidateClass.hashCode();
		return result;
	}

	public int getOffset() {
		return offset;
	}

	public String getTextString() {
		return text.text();
	}

	public TaggedText getText() {
		return text;
	}

	public String getSurfaceForm() {
		return surfaceForm;
	}

	@Override
	public String toString() {

		List<TaggedToken> taggedSentence = null;
		try {
			taggedSentence = text.taggedTokenProvider().getSentenceTokens(((SurfaceFormOccurrence) this));


		} catch (ItemNotFoundException e) {
			return "";
		}

		StringBuilder taggedSentenceBuilder = new StringBuilder();
		for (TaggedToken taggedToken : taggedSentence) {
			taggedSentenceBuilder.append(taggedToken);
		}


	return "OccurrenceInstance["
			+ surfaceForm + ", "
			+ taggedSentenceBuilder.toString()
			+ ']';
	}
}
