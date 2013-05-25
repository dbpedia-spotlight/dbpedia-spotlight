package org.dbpedia.spotlight.uima.response;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
/**
 * POJO for reading the spotlight annotation results from the REST server.
 * 
 *@author Mustafa Nural 
 */
@XmlRootElement(name ="Annotation")
public class Annotation {
	
	
	private List<Resource> Resources = new ArrayList<Resource>();

	
	@XmlElementWrapper(name="Resources")
	@XmlElement(name="Resource")
	public List<Resource> getResources() {
		return Resources;
	}

	public void setResources(List<Resource> resources) {
		Resources = resources;
	}
	
}
