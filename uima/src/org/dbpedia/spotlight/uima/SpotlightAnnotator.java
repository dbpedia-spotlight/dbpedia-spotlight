package org.dbpedia.spotlight.uima;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import javax.ws.rs.core.MediaType;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dbpedia.spotlight.uima.response.Annotation;
import org.dbpedia.spotlight.uima.response.Resource;
import org.dbpedia.spotlight.uima.types.JCasResource;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper for the DbpediaSpotlight Annotate Web Service. This annotator assumes that the
 * web service endpoint specified in the configuration has already been started.
 * 
 * The annotator has no input size limitation, 
 * however it assumes the input is structured as one sentence at a line.
 * This is not a strict requirement though,
 * the annotator would still work fine as long as there are no lines containing extra-long text.
 *   
 * @author Mustafa Nural
 */
public class SpotlightAnnotator extends JCasAnnotator_ImplBase {

	Log LOG = LogFactory.getLog(this.getClass());
	
	private String SPOTLIGHT_ENDPOINT;

	// Default values for the web service parameters for the spotlight endpoint
	private double CONFIDENCE = 0.0;
	private int SUPPORT = 0;
	private String TYPES = "";
	private String SPARQL = "";
	private String POLICY = "whitelist";
	private boolean COREFERENCE_RESOLUTION = true;
	private String SPOTTER = "Default";
	private String DISAMBIGUATOR = "Default";

	private final int BATCH_SIZE = 10; 

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {

		if ((SPOTLIGHT_ENDPOINT = (String) aContext
				.getConfigParameterValue("endPoint")) == null) {
			throw new ResourceInitializationException(
					"Spotlight Endpoint can not be null", null);
		}
		if ((aContext.getConfigParameterValue("confidence")) != null) {
			CONFIDENCE = (Double) aContext
					.getConfigParameterValue("confidence");
		}
		if ((aContext.getConfigParameterValue("support")) != null) {
			SUPPORT = (Integer) aContext.getConfigParameterValue("support");
		}
		if ((aContext.getConfigParameterValue("types")) != null) {
			TYPES = (String) aContext.getConfigParameterValue("types");
		}
		if ((aContext.getConfigParameterValue("sparql")) != null) {
			SPARQL = (String) aContext.getConfigParameterValue("sparql");
		}
		if ((aContext.getConfigParameterValue("policy")) != null) {
			POLICY = (String) aContext.getConfigParameterValue("policy");
		}
		if ((aContext.getConfigParameterValue("coferenceResolution")) != null) {
			COREFERENCE_RESOLUTION = (Boolean) aContext
					.getConfigParameterValue("coferenceResolution");
		}
		if ((aContext.getConfigParameterValue("spotter")) != null) {
			SPOTTER = (String) aContext.getConfigParameterValue("spotter");
		}
		if ((aContext.getConfigParameterValue("disambiguator")) != null) {
			DISAMBIGUATOR = (String) aContext
					.getConfigParameterValue("disambiguator");
		}

	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		String documentText = aJCas.getDocumentText();


		Client c = Client.create();

		BufferedReader documentReader = new BufferedReader(new StringReader(documentText));
		//Send requests to the server by dividing the document into sentence chunks determined by BATCH_SIZE.
		int documentOffset = 0;
		int numLines = 0;
		boolean moreLines = true;
		while (moreLines){
			String request = "";
			for (int index = 0; index < BATCH_SIZE; index++) {
				String line = null;
				try {
					line = documentReader.readLine();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					LOG.error("Can't read from input file",e);
				}
				if (line == null) {
					moreLines = false;
					break;
				}else if (index !=0){
					request += "\n";
				}
				request += line;
				numLines++;
			}

			
			Annotation response = null;
			boolean retry = false;
			int retryCount = 0;
			do{
				try{

					LOG.info("Sending request to the server");

					WebResource r = c.resource(SPOTLIGHT_ENDPOINT);
					response =
							r.queryParam("text", request)
							.queryParam("confidence", "" + CONFIDENCE)
							.queryParam("support", "" + SUPPORT)
							.queryParam("types", TYPES)
							.queryParam("sparql", SPARQL)
							.queryParam("policy", POLICY)
							.queryParam("coreferenceResolution",
									Boolean.toString(COREFERENCE_RESOLUTION))
									.queryParam("spotter", SPOTTER)
									.queryParam("disambiguator", DISAMBIGUATOR)
									.type("application/x-www-form-urlencoded")
									.accept(MediaType.TEXT_XML).post(Annotation.class);
					retry = false;
				} catch (Exception e){
					//In case of a failure, try sending the request with a 2 second delay at least three times before throwing an exception
					LOG.error("Server request failed. Will try again in 2 seconds..", e);
					LOG.error("Failed request payload: " +request);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						LOG.error("Thread interrupted",e1);
					}
					if (retryCount++ < 3){
						retry = true;	
					} else {
						throw new AnalysisEngineProcessException("The server request failed", null);
					}
				}
			}while(retry);
					
					LOG.info("Server request completed. Writing to the index");
					/*
					 * Add the results to the AnnotationIndex
					 */
					for (Resource resource : response.getResources()) {
						JCasResource res = new JCasResource(aJCas);
						res.setBegin(documentOffset + new Integer(resource.getOffset()));
						res.setEnd(documentOffset + new Integer(resource.getOffset())
						+ resource.getSurfaceForm().length());
						res.setSimilarityScore(new Double(resource.getSimilarityScore()));
						res.setTypes(resource.getTypes());
						res.setSupport(new Integer(resource.getSupport()));
						res.setURI(resource.getURI());

						res.addToIndexes(aJCas);
					}

					documentOffset += request.length() + 1 ;

		}
		documentReader.close();

	}

	
}
