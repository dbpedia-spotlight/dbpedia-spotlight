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
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.filter.annotations.CombineAllAnnotationFilters;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.web.rest.output.Annotation;
import org.dbpedia.spotlight.web.rest.output.OutputSerializer;
import org.dbpedia.spotlight.web.rest.output.Resource;
import org.dbpedia.spotlight.web.rest.output.Spot;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author maxjakob
 */

//TODO merge with SpotlightInterface

public abstract class SpotlightInterfaceNEW {

    Log LOG = LogFactory.getLog(this.getClass());

    public abstract void announceAPI();
    public abstract Annotation process(String text, double confidence, int support, List<OntologyType> dbpediaTypes,
                                       String sparqlQuery, boolean blacklist, boolean coreferenceResolution)
            throws SearchException, InputException, ItemNotFoundException;

    private SpotlightConfiguration config;
    protected CombineAllAnnotationFilters annotationFilter;

    private OutputSerializer output = new OutputSerializer();

    private SpotlightInterfaceNEW(SpotlightConfiguration config) {
        this.config = config;
        this.annotationFilter = new CombineAllAnnotationFilters(config);
    }

    /**
     * Initialize the interface to perform annotation
     * @param annotator
     * @return
     */
    public static SpotlightInterfaceNEW getInstance(Annotator annotator, SpotlightConfiguration config, int k) {
        return new CandidatesSpotlightInterface(annotator, config, k);
    }

    private static class CandidatesSpotlightInterface extends SpotlightInterfaceNEW {
        private Annotator annotator;
        private int k;

        public CandidatesSpotlightInterface(Annotator a, SpotlightConfiguration config, int k) {
            super(config);
            annotator = a;
            this.k = k;
        }

        public void announceAPI() {
            LOG.info("API: "+k+" best candidates with "+annotator.getClass());
        }

        /**
         * Does not do any filtering at the moment!!!
         */
        public Annotation process(String text, double confidence, int support, List<OntologyType> dbpediaTypes,
                                  String sparqlQuery, boolean blacklist, boolean coreferenceResolution)
                throws SearchException, ItemNotFoundException, InputException {
            Annotation annotation = new Annotation(text);
            List<Spot> spots = new LinkedList<Spot>();
            for(SurfaceFormOccurrence sfOcc : annotator.spotter().extract(new Text(text))) {
                Spot spot = Spot.getInstance(sfOcc);
                List<Resource> resources = new LinkedList<Resource>();
                for(DBpediaResourceOccurrence occ : annotator.disambiguator().bestK(sfOcc, k)) {
                    Resource resource = Resource.getInstance(occ);
                    resources.add(resource);
                }
                spot.setResources(resources);
                spots.add(spot);
            }
            annotation.setSpots(spots);
            return annotation;
        }

    }


    public Annotation getAnnotation(String text,
                                    double confidence,
                                    int support,
                                    String ontologyTypesString,
                                    String sparqlQuery,
                                    String policy,
                                    boolean coreferenceResolution,
                                    String clientIp) throws SearchException, InputException, ItemNotFoundException {

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
        LOG.info("types: "+ontologyTypesString);
        LOG.info("sparqlQuery: "+ sparqlQuery);
        LOG.info("policy: "+policy);
        LOG.info("coreferenceResolution: "+String.valueOf(coreferenceResolution));

        if (text.trim().equals("")) {
            throw new InputException("No text was specified in the &text parameter.");
        }

        List<OntologyType> ontologyTypes = new ArrayList<OntologyType>();
        String types[] = ontologyTypesString.trim().split(",");
        for (String t : types){
            if (!t.trim().equals("")) ontologyTypes.add(Factory.ontologyType().fromQName(t.trim()));
            //LOG.info("type:"+t.trim());
        }

        Annotation annotation = process(text, confidence, support, ontologyTypes, sparqlQuery, blacklist, coreferenceResolution);

        LOG.info("Shown: "+annotation.toXML());

        return annotation;
    }

    public String getXML(String text,
                         double confidence,
                         int support,
                         String dbpediaTypesString,
                         String spqarlQuery,
                         String policy,
                         boolean coreferenceResolution,
                         String clientIp) throws Exception {
        Annotation a = getAnnotation(text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution, clientIp);
        LOG.info("XML format");
        return output.toXML(a);
    }

    public String getJSON(String text,
                          double confidence,
                          int support,
                          String dbpediaTypesString,
                          String spqarlQuery,
                          String policy,
                          boolean coreferenceResolution,
                          String clientIp) throws Exception {
        Annotation a = getAnnotation(text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution, clientIp);
        LOG.info("JSON format");
        return output.toJSON(a);
    }

}