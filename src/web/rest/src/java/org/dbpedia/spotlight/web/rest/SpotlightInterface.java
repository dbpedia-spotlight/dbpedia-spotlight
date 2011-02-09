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
    Annotator annotator;
    Disambiguator disambiguator;

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
                                                          String spqarlQuery,
                                                          String policy,
                                                          boolean coreferenceResolution) throws Exception {

        LOG.info("******************************** Parameters ********************************");
        if(disambiguator == null && annotator != null) {
            LOG.info("API: "+annotator.getClass());
        }
        else if(disambiguator != null && annotator == null) {
            LOG.info("API: "+disambiguator.getClass());
        }
        else {
            LOG.info("API: not properly set in SpotlightInterface!!!");
        }

        boolean blacklist = false;
        if(policy.trim().equalsIgnoreCase("blacklist")) {
            blacklist = true;
            policy = "blacklist";
        }
        else {
            policy = "whitelist";
        }

        LOG.info("confidence: "+String.valueOf(confidence));
        LOG.info("support: "+String.valueOf(support));
        LOG.info("policy: " +policy);
        LOG.info("coreferenceResolution: " +String.valueOf(coreferenceResolution));

        List<DBpediaType> dbpediaTypes = new ArrayList<DBpediaType>();
        String types[] = dbpediaTypesString.split(",");
        for (String t : types){
            dbpediaTypes.add(new DBpediaType(t.trim()));
            //LOG.info("type:"+targetType.trim());
        }

        List<DBpediaResourceOccurrence> occList = new ArrayList<DBpediaResourceOccurrence>(0);
        if (text != null){

            // use API that was given to the constructor
            if(disambiguator == null && annotator != null) {
                occList = annotator.annotate(text);
            }
            else if(disambiguator != null && annotator == null) {
                List<SurfaceFormOccurrence> sfOccList = ParseSurfaceFormText.parse(text);
                occList = disambiguator.disambiguate(sfOccList);
            }
            else {
                throw new IllegalStateException("both annotator and disambiguator were not initialized");
            }

            return AnnotationFilter.filter(occList, confidence, support, dbpediaTypes, spqarlQuery, blacklist, coreferenceResolution);
        }

        return occList;
    }

    public String getXML(String text,
                         double confidence,
                         int support,
                         String dbpediaTypesString,
                         String spqarlQuery,
                         String policy,
                         boolean coreferenceResolution) throws Exception {
        try {
            List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
            return output.makeXML(text, occs, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
        }
        catch (InputException e) {
            return output.makeErrorXML(e.getMessage(), text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
        }
    }

    public String getJSON(String text,
                          double confidence,
                          int support,
                          String dbpediaTypesString,
                          String spqarlQuery,
                          String policy,
                          boolean coreferenceResolution) throws Exception {
        String xml = getXML(text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
        return output.xml2json(xml);
    }

    public String getHTML(String text,
                          double confidence,
                          int support,
                          String dbpediaTypesString,
                          String spqarlQuery,
                          String policy,
                          boolean coreferenceResolution) throws Exception {
        try {
            List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
            return output.makeHTML(text, occs);
        }
        catch (InputException e) {
            return "<html><body><b>ERROR:</b> <i>"+e.getMessage()+"</i></body></html>";
        }

    }

}