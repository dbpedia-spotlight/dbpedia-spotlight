package org.dbpedia.spotlight.spot.cooccurrence.training;

import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.spot.cooccurrence.classification.SpotClass;
import org.dbpedia.spotlight.spot.cooccurrence.classification.SpotClass;
import org.dbpedia.spotlight.tagging.TaggedToken;

import java.util.List;

/**
 * An instance of a surface form occurrence is a single surface form occurrence that contains relevant
 * information for a human annotator.
 *
 * @author Joachim Daiber
 */

public class AnnotatedSurfaceFormOccurrence extends SurfaceFormOccurrence {

	private String annotationURI;
	private String annotationTitle;
	private String annotationAbstract;
	private SpotClass spotClass;
	private int offset;
	private String surfaceForm;
	private TaggedText text;


	public AnnotatedSurfaceFormOccurrence(String surfaceForm, int offset, TaggedText text, String annotationURI,
										  String annotationTitle, String annotationAbstract,
										  SpotClass spotClass) {

		super(new SurfaceForm(surfaceForm), text, offset);

		this.annotationURI = annotationURI;
		this.annotationTitle = annotationTitle;
		this.annotationAbstract = annotationAbstract;
		this.spotClass = spotClass;
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

	public SpotClass getSpotClass() {
		return spotClass;
	}

	public void setSpotClass(SpotClass spotClass) {
		this.spotClass = spotClass;
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AnnotatedSurfaceFormOccurrence)) return false;
		if (!super.equals(o)) return false;

		AnnotatedSurfaceFormOccurrence that = (AnnotatedSurfaceFormOccurrence) o;

		if (annotationAbstract != null ? !annotationAbstract.equals(that.annotationAbstract) : that.annotationAbstract != null)
			return false;
		if (annotationTitle != null ? !annotationTitle.equals(that.annotationTitle) : that.annotationTitle != null)
			return false;
		if (annotationURI != null ? !annotationURI.equals(that.annotationURI) : that.annotationURI != null)
			return false;
		if (spotClass != that.spotClass) return false;

		return true;
	}

	/**
	 * Equality test for surface form occurrences.
	 *
	 * @param surfaceFormOccurrence
	 * @return
	 */
	public boolean equals(SurfaceFormOccurrence surfaceFormOccurrence) {

		if(!surfaceFormOccurrence.context().equals(text))
			return false;
		if(!surfaceFormOccurrence.surfaceForm().equals(surfaceForm))
			return false;
		if(!(surfaceFormOccurrence.textOffset() == offset))
			return false;

		return true;

	}

	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (annotationURI != null ? annotationURI.hashCode() : 0);
		result = 31 * result + (annotationTitle != null ? annotationTitle.hashCode() : 0);
		result = 31 * result + (annotationAbstract != null ? annotationAbstract.hashCode() : 0);
		result = 31 * result + spotClass.hashCode();
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

		StringBuilder taggedSentenceStringBuilder = new StringBuilder();
		for (TaggedToken taggedToken : taggedSentence) {
			taggedSentenceStringBuilder.append(taggedToken);
		}


		return "AnnotatedSurfaceFormOccurrence["
				+ surfaceForm + ", "
				+ taggedSentenceStringBuilder.toString()
				+ ']';
	}

	public SurfaceFormOccurrence toSurfaceFormOccurrence() {
		return new SurfaceFormOccurrence(new SurfaceForm(this.surfaceForm), this.text, offset);
	}

	public DBpediaResourceOccurrence toDBpediaResourceOccurrence() {
		return new DBpediaResourceOccurrence(new DBpediaResource(this.annotationURI), new SurfaceForm(this.surfaceForm), this.text, this.offset);
	}
}
