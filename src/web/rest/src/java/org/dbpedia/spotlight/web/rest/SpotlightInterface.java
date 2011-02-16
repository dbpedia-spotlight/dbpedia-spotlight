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
import org.dbpedia.spotlight.exceptions.OutputException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;
import org.dbpedia.spotlight.model.DBpediaType;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.string.ParseSurfaceFormText;
import org.dbpedia.spotlight.util.AnnotationFilter;

import java.util.ArrayList;
import java.util.List;

public class SpotlightInterface {

    Log LOG = LogFactory.getLog(this.getClass());

    // only one of those two APIs can be set
    private Annotator annotator;
    private Disambiguator disambiguator;

    public SpotlightInterface(Annotator a) {
        annotator = a;
    }

    public SpotlightInterface(Disambiguator d) {
        disambiguator = d;
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
                                                          boolean coreferenceResolution) throws SearchException, InputException {

        LOG.info("******************************** Parameters ********************************");
        if(disambiguator == null && annotator != null) {
            LOG.info("API: "+annotator.getClass());
        }
        else if(disambiguator != null && annotator == null) {
            LOG.info("API: "+disambiguator.getClass());
        }
        else {
            throw new IllegalStateException("either neither or both of annotator and disambiguator were initialized");
        }

        boolean blacklist = false;
        if(policy.trim().equalsIgnoreCase("blacklist")) {
            blacklist = true;
            policy = "blacklist";
        }
        else {
            policy = "whitelist";
        }

        LOG.info("text: " + text);
        LOG.info("text length in chars: "+text.length());
        LOG.info("confidence: "+String.valueOf(confidence));
        LOG.info("support: "+String.valueOf(support));
        LOG.info("types: "+dbpediaTypesString);
        LOG.info("sparqlQuery: "+ sparqlQuery);
        LOG.info("policy: "+policy);
        LOG.info("coreferenceResolution: "+String.valueOf(coreferenceResolution));

        if (text.trim().equals("")) {
            throw new InputException("did not find any text");
        }

        List<DBpediaType> dbpediaTypes = new ArrayList<DBpediaType>();
        String types[] = dbpediaTypesString.split(",");
        for (String t : types){
            dbpediaTypes.add(new DBpediaType(t.trim()));
            //LOG.info("type:"+targetType.trim());
        }

        List<DBpediaResourceOccurrence> occList = new ArrayList<DBpediaResourceOccurrence>(0);
        // use API that was given to the constructor
        if(disambiguator == null && annotator != null) {
            occList = annotator.annotate(text);
        }
        else if(disambiguator != null && annotator == null) {
            List<SurfaceFormOccurrence> sfOccList = ParseSurfaceFormText.parse(text);
            occList = disambiguator.disambiguate(sfOccList);
        }

        occList = AnnotationFilter.filter(occList, confidence, support, dbpediaTypes, sparqlQuery, blacklist, coreferenceResolution);

        LOG.info("Shown:");
        for(DBpediaResourceOccurrence occ : occList) {
            LOG.info(occ.resource());
        }

        return occList;
    }

    public String getHTML(String text,
                          double confidence,
                          int support,
                          String dbpediaTypesString,
                          String spqarlQuery,
                          String policy,
                          boolean coreferenceResolution) throws Exception {
        String result;
        try {
            List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
            result = output.makeHTML(text, occs);
        }
        catch (InputException e) {
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
                          boolean coreferenceResolution) throws Exception {
        String result;
        try {
            List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
            result = output.makeRDFa(text, occs);
        }
        catch (InputException e) {
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
                         boolean coreferenceResolution) throws OutputException {
        String result;
        try {
            List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
            result = output.makeXML(text, occs, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
        }
        catch (Exception e) {
            LOG.info("ERROR: "+e.getMessage());
            result = output.makeErrorXML(e.getMessage(), text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
        }
        LOG.info("XML format");
        return result;
    }

    public String getJSON(String text,
                          double confidence,
                          int support,
                          String dbpediaTypesString,
                          String spqarlQuery,
                          String policy,
                          boolean coreferenceResolution) throws Exception {
        String result;
        String xml = getXML(text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
        result = output.xml2json(xml);
        LOG.info("JSON format");
        return result;
    }

}