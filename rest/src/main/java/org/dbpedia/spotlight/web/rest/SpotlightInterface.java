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
import org.dbpedia.spotlight.annotate.Annotator;
import org.dbpedia.spotlight.disambiguate.Disambiguator;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.filter.annotations.CombineAllAnnotationFilters;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;
import org.dbpedia.spotlight.model.DBpediaType;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.string.ParseSurfaceFormText;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class that is extended by inner classes according to annotation or disambiguation behaviours.
 * Needs to be constructed from SpotlightInterface.getInstance(Annotator) or SpotlightInterface.getInstance(Disambiguator).
 *
 * @author maxjakob, pablomendes
 */
public abstract class SpotlightInterface  {

    Log LOG = LogFactory.getLog(this.getClass());

    public abstract void announceAPI();
    public abstract List<DBpediaResourceOccurrence> process(String text) throws SearchException, InputException;

    private SpotlightConfiguration config;
    private CombineAllAnnotationFilters annotationFilter;

    private SpotlightInterface(SpotlightConfiguration config) {
        this.config = config;
        this.annotationFilter = new CombineAllAnnotationFilters(config);
    }
    /**
     * Initialize the interface to perform disambiguation
     * @param disambiguator
     * @return
     */
    public static SpotlightInterface getInstance(Disambiguator disambiguator, SpotlightConfiguration config) {
        return new DisambiguatorSpotlightInterface(disambiguator, config);
    }

    /**
     * Initialize the interface to perform annotation
     * @param annotator
     * @return
     */
    public static SpotlightInterface getInstance(Annotator annotator, SpotlightConfiguration config) {
        return new AnnotatorSpotlightInterface(annotator, config);
    }

    private static class DisambiguatorSpotlightInterface extends SpotlightInterface {
        private Disambiguator disambiguator;
        public DisambiguatorSpotlightInterface(Disambiguator d, SpotlightConfiguration config) {
            super(config);
            disambiguator = d;
        }
        public void announceAPI() {
                LOG.info("API: "+disambiguator.getClass());
        }
        public List<DBpediaResourceOccurrence> process(String text) throws SearchException, InputException {
            List<SurfaceFormOccurrence> sfOccList = ParseSurfaceFormText.parse(text);
            return disambiguator.disambiguate(sfOccList);
       }
    }

    private static class AnnotatorSpotlightInterface extends SpotlightInterface {
        private Annotator annotator;
        public AnnotatorSpotlightInterface(Annotator a, SpotlightConfiguration config) {
            super(config);
            annotator = a;
        }
        public void announceAPI() {
                LOG.info("API: "+annotator.getClass());
        }
       public List<DBpediaResourceOccurrence> process(String text) throws SearchException, InputException {
            return annotator.annotate(text);
        }
    }


    private OutputManager output = new OutputManager();


    /**
     * Retrieves representation of an instance of org.dbpedia.spotlight.web.Annotation
     * @return an instance of java.lang.String
     */
    public List<DBpediaResourceOccurrence> getOccurrences(String text,
                                                          double confidence,
                                                          int support,
                                                          String dbpediaTypesString,
                                                          String sparqlQuery,
                                                          String policy,
                                                          boolean coreferenceResolution,
                                                          String clientIp) throws SearchException, InputException {

        LOG.info("******************************** Parameters ********************************");
        announceAPI();
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
        LOG.info("types: "+dbpediaTypesString);
        LOG.info("sparqlQuery: "+ sparqlQuery);
        LOG.info("policy: "+policy);
        LOG.info("coreferenceResolution: "+String.valueOf(coreferenceResolution));

        if (text.trim().equals("")) {
            throw new InputException("No text was specified in the &text parameter.");
        }

        List<DBpediaType> dbpediaTypes = new ArrayList<DBpediaType>();
        String types[] = dbpediaTypesString.split(",");
        for (String t : types){
            dbpediaTypes.add(new DBpediaType(t.trim()));
            //LOG.info("type:"+targetType.trim());
        }

        // Call annotation or disambiguation
        List<DBpediaResourceOccurrence> occList = process(text);

        occList = annotationFilter.filter(occList, confidence, support, dbpediaTypes, sparqlQuery, blacklist, coreferenceResolution);

        LOG.info("Shown:");
        for(DBpediaResourceOccurrence occ : occList) {
            LOG.info(occ.resource()+" <- "+occ.surfaceForm());
        }

        return occList;
    }

    public String getHTML(String text,
                          double confidence,
                          int support,
                          String dbpediaTypesString,
                          String spqarlQuery,
                          String policy,
                          boolean coreferenceResolution,
                          String clientIp) throws Exception {
        String result;
        try {
            List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution, clientIp);
            result = output.makeHTML(text, occs);
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
                          String spqarlQuery,
                          String policy,
                          boolean coreferenceResolution,
                          String clientIp) throws Exception {
        String result;
        try {
            List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution, clientIp);
            result = output.makeRDFa(text, occs);
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
                         String spqarlQuery,
                         String policy,
                         boolean coreferenceResolution,
                          String clientIp) throws Exception {
        String result;
//        try {
            List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution, clientIp);
            result = output.makeXML(text, occs, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
//        }
//        catch (Exception e) { //TODO throw exception up to Annotate for WebApplicationException to handle.
//            LOG.info("ERROR: "+e.getMessage());
//            result = output.makeErrorXML(e.getMessage(), text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
//        }
        LOG.info("XML format");
        return result;
    }

    public String getJSON(String text,
                          double confidence,
                          int support,
                          String dbpediaTypesString,
                          String spqarlQuery,
                          String policy,
                          boolean coreferenceResolution,
                          String clientIp) throws Exception {
        String result;
        String xml = getXML(text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution, clientIp);
        result = output.xml2json(xml);
        LOG.info("JSON format");
        return result;
    }

}