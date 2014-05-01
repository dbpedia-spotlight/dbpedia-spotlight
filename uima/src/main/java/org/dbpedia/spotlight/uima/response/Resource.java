package org.dbpedia.spotlight.uima.response;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
/**
 * Simple POJO for reading the spotlight annotation results from the REST server.
 * A resource represents a single annotation in the spotlight output.
 * 
 *@author Mustafa Nural 
 */
@XmlRootElement(name="Resource")
public class Resource {
	
	private String URI;
	private String support;
	private String types;
	private String surfaceForm;
	private String offset;
	private String similarityScore;
	private String percentageOfSecondRank;
	
	@XmlAttribute(name="URI")
	public String getURI() {
		return URI;
	}
	public void setURI(String uRI) {
		URI = uRI;
	}
	
	@XmlAttribute
	public String getSupport() {
		return support;
	}
	public void setSupport(String support) {
		this.support = support;
	}
	
	@XmlAttribute
	public String getTypes() {
		return types;
	}
	public void setTypes(String types) {
		this.types = types;
	}
	
	@XmlAttribute
	public String getSurfaceForm() {
		return surfaceForm;
	}
	public void setSurfaceForm(String surfaceForm) {
		this.surfaceForm = surfaceForm;
	}
	
	@XmlAttribute
	public String getOffset() {
		return offset;
	}
	public void setOffset(String offset) {
		this.offset = offset;
	}
	
	@XmlAttribute
	public String getSimilarityScore() {
		return similarityScore;
	}
	public void setSimilarityScore(String similarityScore) {
		this.similarityScore = similarityScore;
	}
	
	@XmlAttribute
	public String getPercentageOfSecondRank() {
		return percentageOfSecondRank;
	}
	public void setPercentageOfSecondRank(String percentageofSecondRank) {
		this.percentageOfSecondRank = percentageofSecondRank;
	}
}
