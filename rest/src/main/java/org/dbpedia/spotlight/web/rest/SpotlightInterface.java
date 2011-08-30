/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.web.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.disambiguate.Disambiguator;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.filter.annotations.CombineAllAnnotationFilters;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.spot.Spotter;
import org.dbpedia.spotlight.spot.WikiMarkupSpotter;
import org.dbpedia.spotlight.web.rest.output.Resource;

import javax.annotation.Resources;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller that interfaces between the REST API and the DBpedia Spotlight core.
 * Needs to be constructed from SpotlightInterface.getInstance(Annotator) or SpotlightInterface.getInstance(Disambiguator).
 *
 * @author maxjakob, pablomendes
 */
public class SpotlightInterface  {

    Log LOG = LogFactory.getLog(this.getClass());

    // Name of the REST api so that we can announce it in the log (can be disambiguate, annotate, candidates)
    String apiName;
    // List of available spotting behaviors
    Map<SpotterConfiguration.SpotterPolicy,Spotter> spotters;
    // Disambiguator implementation chosen (TODO could also be a list)
    Disambiguator disambiguator;

    private OutputManager outputManager = new OutputManager();

    private SpotlightInterface(Map<SpotterConfiguration.SpotterPolicy,Spotter> spotters, Disambiguator disambiguator) {
        this.spotters = spotters;
        this.disambiguator = disambiguator;
    }

    /**
     * If no spotterName is specified, just runs the first spotter it finds.
     * @param text
     * @return
     * @throws SearchException
     * @throws InputException
     */
    public List<DBpediaResourceOccurrence> process(String text) throws SearchException, InputException {
        return process(text, this.spotters.keySet().iterator().next());
    }

    public List<DBpediaResourceOccurrence> process(String text, SpotterConfiguration.SpotterPolicy spotter) throws SearchException, InputException {
        List<DBpediaResourceOccurrence> resources = new ArrayList<DBpediaResourceOccurrence>();
        List<SurfaceFormOccurrence> spots = this.spotters.get(spotter).extract(new Text(text));

        for(SurfaceFormOccurrence sfOcc: spots) {
            try {
                resources.add(disambiguator.disambiguate(sfOcc));
            } catch (ItemNotFoundException e) {
                LOG.error("SurfaceForm not found. Using incompatible spotter.dict and index?",e);
            }
        }
        return resources;
    }

    /**
     * Initialize the interface to perform disambiguation
     * @param disambiguator
     * @return
     */
    public static SpotlightInterface getInstance(Disambiguator disambiguator) {
        HashMap<SpotterConfiguration.SpotterPolicy,Spotter> spotters = new HashMap<SpotterConfiguration.SpotterPolicy,Spotter>();
        spotters.put(SpotterConfiguration.SpotterPolicy.UserProvidedSpots, new WikiMarkupSpotter());
        SpotlightInterface controller = new SpotlightInterface(spotters, disambiguator);
        controller.setApiName("/disambiguate");
        return controller;
    }

    /**
     * Initialize the interface to perform annotation
     */
    public static SpotlightInterface getInstance(Map<SpotterConfiguration.SpotterPolicy,Spotter> spotters, Disambiguator disambiguator) {
        SpotlightInterface controller = new SpotlightInterface(spotters, disambiguator);
        controller.setApiName("/annotate");
        return controller;
    }


    /**
     * Retrieves representation of an instance of org.dbpedia.spotlight.web.Annotation
     * @return an instance of java.lang.String
     */
    public List<DBpediaResourceOccurrence> getOccurrences(String text,
                                                          double confidence,
                                                          int support,
                                                          String ontologyTypesString,
                                                          String sparqlQuery,
                                                          String policy,
                                                          boolean coreferenceResolution,
                                                          String clientIp,
                                                          SpotterConfiguration.SpotterPolicy spotter
                                                          ) throws SearchException, InputException {

        LOG.info("******************************** Parameters ********************************");
        LOG.info("API: " + getApiName());
        boolean blacklist = false;
        if(policy.trim().equalsIgnoreCase("blacklist")) {
            blacklist = true;
            policy = "blacklist";
        }
        else {
            policy = "whitelist";
        }
        LOG.info("client ip: " + clientIp);
        LOG.info("text: " + text);
        LOG.info("text length in chars: "+text.length());
        LOG.info("confidence: "+String.valueOf(confidence));
        LOG.info("support: "+String.valueOf(support));
        LOG.info("types: "+ontologyTypesString);
        LOG.info("sparqlQuery: "+ sparqlQuery);
        LOG.info("policy: "+policy);
        LOG.info("coreferenceResolution: "+String.valueOf(coreferenceResolution));
        LOG.info("spotter: "+String.valueOf(spotter));

        if (text.trim().equals("")) {
            throw new InputException("No text was specified in the &text parameter.");
        }

        List<OntologyType> ontologyTypes = new ArrayList<OntologyType>();
        String types[] = ontologyTypesString.trim().split(",");
        for (String t : types){
            if (!t.trim().equals("")) ontologyTypes.add(Factory.ontologyType().fromQName(t.trim()));
            //LOG.info("type:"+t.trim());
        }

        // Call annotation or disambiguation
        List<DBpediaResourceOccurrence> occList = process(text, spotter);

        // Filter: Old monolithic way
        CombineAllAnnotationFilters annotationFilter = new CombineAllAnnotationFilters(Server.getConfiguration());
        occList = annotationFilter.filter(occList, confidence, support, ontologyTypes, sparqlQuery, blacklist, coreferenceResolution);

        // Filter: TODO run occurrences through a list of annotation filters (which can be passed by parameter)
        // Map<String,AnnotationFilter> annotationFilters = buildFilters(occList, confidence, support, dbpediaTypes, sparqlQuery, blacklist, coreferenceResolution);
        //AnnotationFilter annotationFilter = annotationFilters.get(CombineAllAnnotationFilters.class.getSimpleName());

        LOG.info("Shown:");
        for(DBpediaResourceOccurrence occ : occList) {
            LOG.info(String.format("%s <- %s; score: %s, ctxscore: %3.2f, support: %s, prior: %s", occ.resource(),occ.surfaceForm(), occ.similarityScore(), occ.contextualScore(),  occ.resource().support(), occ.resource().prior()));
        }

        return occList;
    }

    public String getHTML(String text,
                          double confidence,
                          int support,
                          String dbpediaTypesString,
                          String sparqlQuery,
                          String policy,
                          boolean coreferenceResolution,
                          String clientIp,
                          SpotterConfiguration.SpotterPolicy spotter
    ) throws Exception {
        String result;
        try {
            List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, sparqlQuery, policy, coreferenceResolution, clientIp, spotter);
            result = outputManager.makeHTML(text, occs);
        }
        catch (InputException e) { //TODO throw exception up to Annotate for WebApplicationException to handle.
            LOG.info("ERROR: "+e.getMessage());
            result = "<html><body><b>ERROR:</b> <i>"+e.getMessage()+"</i></body></html>";
        }
        LOG.info("HTML format");
        return result;
    }

    public String getRDFa(String text,
                          double confidence,
                          int support,
                          String dbpediaTypesString,
                          String sparqlQuery,
                          String policy,
                          boolean coreferenceResolution,
                          String clientIp,
                          SpotterConfiguration.SpotterPolicy spotter
    ) throws Exception {
        String result;
        try {
            List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, sparqlQuery, policy, coreferenceResolution, clientIp, spotter);
            result = outputManager.makeRDFa(text, occs);
        }
        catch (InputException e) { //TODO throw exception up to Annotate for WebApplicationException to handle.
            LOG.info("ERROR: "+e.getMessage());
            result = "<html><body><b>ERROR:</b> <i>"+e.getMessage()+"</i></body></html>";
        }
        LOG.info("RDFa format");
        return result;
    }

    public String getXML(String text,
                         double confidence,
                         int support,
                         String dbpediaTypesString,
                         String sparqlQuery,
                         String policy,
                         boolean coreferenceResolution,
                         String clientIp,
                         SpotterConfiguration.SpotterPolicy spotter
   ) throws Exception {
        String result;
//        try {
            List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, sparqlQuery, policy, coreferenceResolution, clientIp, spotter);
            result = outputManager.makeXML(text, occs, confidence, support, dbpediaTypesString, sparqlQuery, policy, coreferenceResolution);
//        }
//        catch (Exception e) { //TODO throw exception up to Annotate for WebApplicationException to handle.
//            LOG.info("ERROR: "+e.getMessage());
//            result = outputManager.makeErrorXML(e.getMessage(), text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
//        }
        LOG.info("XML format");
        return result;
    }

    public String getCandidateXML(String text,
                         double confidence,
                         int support,
                         String dbpediaTypesString,
                         String sparqlQuery,
                         String policy,
                         boolean coreferenceResolution,
                         String clientIp,
                         SpotterConfiguration.SpotterPolicy spotter
   ) throws Exception {
        String result;
        List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, sparqlQuery, policy, coreferenceResolution, clientIp, spotter);
        result = outputManager.makeXML(text, occs, confidence, support, dbpediaTypesString, sparqlQuery, policy, coreferenceResolution);
        LOG.info("XML format");
        return result;
    }

    public String getJSON(String text,
                          double confidence,
                          int support,
                          String dbpediaTypesString,
                          String sparqlQuery,
                          String policy,
                          boolean coreferenceResolution,
                          String clientIp,
                          SpotterConfiguration.SpotterPolicy spotter
    ) throws Exception {
        String result;
        String xml = getXML(text, confidence, support, dbpediaTypesString, sparqlQuery, policy, coreferenceResolution, clientIp, spotter);
        result = outputManager.xml2json(xml);
        LOG.info("JSON format");
        return result;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

}