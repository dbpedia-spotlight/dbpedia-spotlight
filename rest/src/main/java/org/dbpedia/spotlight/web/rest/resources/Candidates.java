/*
 * Copyright 2011 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.web.rest.resources;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguatorJ;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.exceptions.SpottingException;
import org.dbpedia.spotlight.filter.annotations.FilterPolicy$;
import org.dbpedia.spotlight.filter.visitor.FilterElement;
import org.dbpedia.spotlight.filter.visitor.FilterOccsImpl;
import org.dbpedia.spotlight.filter.visitor.OccsFilter;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.spot.Spotter;
import org.dbpedia.spotlight.web.rest.Server;
import org.dbpedia.spotlight.web.rest.ServerUtils;
import org.dbpedia.spotlight.web.rest.output.Annotation;
import org.dbpedia.spotlight.web.rest.output.Resource;
import org.dbpedia.spotlight.web.rest.output.Spot;

import scala.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * REST Web Service for /candidates API, which outputs the n-best disambiguations for each surface form
 *
 * @author maxjakob
 * @author pablomendes - refactored, added support for spotter and disambiguator parameters, friendlier error messages
 */

@ApplicationPath(Server.APPLICATION_PATH)
@Path("/candidates")
@Consumes("text/plain")
public class Candidates {

    private static int k = 100; //TODO configuration

    @Context
    private UriInfo context;

    Log LOG = LogFactory.getLog(this.getClass());

    // Annotation interface
    public Annotation process(String text, double confidence, int support, String ontologyTypesString,
                              String sparqlQuery, boolean blacklist, boolean coreferenceResolution, Spotter spotter, ParagraphDisambiguatorJ disambiguator)
            throws SearchException, ItemNotFoundException, InputException, SpottingException {

        Annotation annotation = new Annotation(text);
        List<Spot> spots = new LinkedList<Spot>();

        Text textObject = new Text(text);
        textObject.setFeature(new Score("confidence", confidence));

        if(Server.getTokenizer() != null)
            Server.getTokenizer().tokenizeMaybe(textObject);

        List<SurfaceFormOccurrence> entityMentions = spotter.extract(textObject);
        if (entityMentions.size()==0) return annotation; //nothing to disambiguate
        Paragraph paragraph = Factory.paragraph().fromJ(entityMentions);
        LOG.info(String.format("Spotted %d entity mentions.",entityMentions.size()));

        Map<SurfaceFormOccurrence,List<DBpediaResourceOccurrence>> entityCandidates = disambiguator.bestK(paragraph,k);
        LOG.info(String.format("Disambiguated %d candidates with %s.",entityCandidates.size(),disambiguator.name()));

        Enumeration.Value listColor = blacklist ? FilterPolicy$.MODULE$.Blacklist() : FilterPolicy$.MODULE$.Whitelist();

        /*The previous addition of filter to the Candidates requests (which has usability questioned) produce the error described at issue #136.
          To solve it, this feature for this argument (Candidates) is disabled, setting coreferenceResolution to false ever. Ignoring the user's configuration.
        */
        Boolean unableCoreferenceResolution = false;
        FilterElement filter = new OccsFilter(confidence, support, ontologyTypesString, sparqlQuery, blacklist, unableCoreferenceResolution, Server.getSimilarityThresholds(), Server.getSparqlExecute());

        Map<SurfaceFormOccurrence,List<DBpediaResourceOccurrence>> filteredEntityCandidates = new HashMap<SurfaceFormOccurrence,List<DBpediaResourceOccurrence>>();;

        for (Map.Entry<SurfaceFormOccurrence,List<DBpediaResourceOccurrence>> entry : entityCandidates.entrySet())
        {
            List<DBpediaResourceOccurrence> result = filter.accept(new FilterOccsImpl() ,entry.getValue());

            if (!result.isEmpty())
                filteredEntityCandidates.put(entry.getKey(), result);
        }

        for(SurfaceFormOccurrence sfOcc : filteredEntityCandidates.keySet()) {
            Spot spot = Spot.getInstance(sfOcc);
            List<Resource> resources = new LinkedList<Resource>();
            for(DBpediaResourceOccurrence occ : filteredEntityCandidates.get(sfOcc)) {
                Resource resource = Resource.getInstance(occ);
                resources.add(resource);
            }
            spot.setResources(resources);
            spots.add(spot);
        }
        annotation.setSpots(spots);
        return annotation;
    }

    //TODO think if there is a way to output HTML / RDFa for candidates API
//    @GET
//    @Produces(MediaType.TEXT_HTML)
//    public Response getHTML(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
//                            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
//                            @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
//                            @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
//                            @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
//                            @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
//                            @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
//                            @Context HttpServletRequest request) {
//        String clientIp = request.getRemoteAddr();
//
//        try {
//            String response = candidatesInterface.getHTML(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp);
//            return ServerUtils.ok(response);
//        } catch (Exception e) {
//            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(e.getMessage()).type(MediaType.TEXT_HTML).build());
//        }
//    }
//
//
//    @GET
//    @Produces(MediaType.APPLICATION_XHTML_XML)
//    public Response getRDFa(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
//                          @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
//                          @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
//                          @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
//                          @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
//                          @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
//                          @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
//                          @Context HttpServletRequest request) {
//        String clientIp = request.getRemoteAddr();
//
//        try {
//            return ServerUtils.ok(candidatesInterface.getRDFa(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp));
//        } catch (Exception e) {
//            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(e.getMessage()).type(MediaType.APPLICATION_XHTML_XML).build());
//        }
//    }

    @GET
    @Produces(MediaType.TEXT_XML)
    public Response getXML(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                           @DefaultValue("Default") @QueryParam("spotter") String spotter,
                           @DefaultValue("Default") @QueryParam("disambiguator") String disambiguatorName,
                           @Context HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();

        try {
            String textToProcess = ServerUtils.getTextToProcess(text, inUrl);
            Annotation a = getAnnotation(textToProcess, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, spotter, disambiguatorName, clientIp);
            LOG.info("XML format");
            String content = a.toXML();
            return ServerUtils.ok(content);
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(e.getMessage()).type(MediaType.TEXT_XML).build());
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJSON(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                            @DefaultValue("Default") @QueryParam("spotter") String spotter,
                            @DefaultValue("Default") @QueryParam("disambiguator") String disambiguatorName,
                            @Context HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();

        try {
            String textToProcess = ServerUtils.getTextToProcess(text, inUrl);
            Annotation a = getAnnotation(textToProcess, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, spotter, disambiguatorName, clientIp);
            LOG.info("JSON format");
            String content = a.toJSON();
            return ServerUtils.ok(content);
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build());
        }
    }

//
//    @POST
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    @Produces(MediaType.TEXT_HTML)
//    public Response postHTML(
//      @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
//      @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @FormParam("confidence") Double confidence,
//      @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @FormParam("support") int support,
//      @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @FormParam("types") String dbpediaTypes,
//      @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @FormParam("sparql") String sparqlQuery,
//      @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @FormParam("policy") String policy,
//      @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @FormParam("coreferenceResolution") boolean coreferenceResolution,
//      @Context HttpServletRequest request
//      ) {
//        return getHTML(text,confidence,support,dbpediaTypes,sparqlQuery,policy,coreferenceResolution,request);
//    }
//
//    @POST
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    @Produces(MediaType.APPLICATION_XHTML_XML)
//    public Response postRDFa(
//      @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
//      @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @FormParam("confidence") Double confidence,
//      @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @FormParam("support") int support,
//      @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @FormParam("types") String dbpediaTypes,
//      @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @FormParam("sparql") String sparqlQuery,
//      @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @FormParam("policy") String policy,
//      @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @FormParam("coreferenceResolution") boolean coreferenceResolution,
//      @Context HttpServletRequest request
//      ) {
//        return getRDFa(text,confidence,support,dbpediaTypes,sparqlQuery,policy,coreferenceResolution,request);
//    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_XML)
    public Response postXML(
            @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
            @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @FormParam("url") String inUrl,
            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @FormParam("confidence") Double confidence,
            @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @FormParam("support") int support,
            @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @FormParam("types") String dbpediaTypes,
            @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @FormParam("sparql") String sparqlQuery,
            @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @FormParam("policy") String policy,
            @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @FormParam("coreferenceResolution") boolean coreferenceResolution,
            @DefaultValue("Default") @FormParam("spotter") String spotter,
            @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
            @Context HttpServletRequest request
    ) {
        return getXML(text,inUrl,confidence,support,dbpediaTypes,sparqlQuery,policy,coreferenceResolution,spotter,disambiguatorName,request);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postJSON(
            @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
            @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @FormParam("url") String inUrl,
            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @FormParam("confidence") Double confidence,
            @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @FormParam("support") int support,
            @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @FormParam("types") String dbpediaTypes,
            @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @FormParam("sparql") String sparqlQuery,
            @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @FormParam("policy") String policy,
            @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @FormParam("coreferenceResolution") boolean coreferenceResolution,
            @DefaultValue("Default") @FormParam("spotter") String spotter,
            @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
            @Context HttpServletRequest request
    ) {
        return getJSON(text,inUrl,confidence,support,dbpediaTypes,sparqlQuery,policy,coreferenceResolution,spotter,disambiguatorName,request);
    }

    public Annotation getAnnotation(String text,
                                    double confidence,
                                    int support,
                                    String ontologyTypesString,
                                    String sparqlQuery,
                                    String policy,
                                    boolean coreferenceResolution,
                                    String spotterName,
                                    String disambiguatorName,
                                    String clientIp) throws SearchException, InputException, ItemNotFoundException, SpottingException, MalformedURLException, BoilerpipeProcessingException {

        LOG.info("******************************** Parameters ********************************");
        //announceAPI();

        boolean blacklist = false;
        if(policy.trim().equalsIgnoreCase("blacklist")) {
            blacklist = true;
            policy = "blacklist";
        }
        else {
            policy = "whitelist";
        }
        LOG.info("client ip: " + clientIp);
        LOG.info("text to be processed: " + text);
        LOG.info("text length in chars: "+ text.length());
        LOG.info("confidence: "+String.valueOf(confidence));
        LOG.info("support: "+String.valueOf(support));
        LOG.info("types: "+ontologyTypesString);
        LOG.info("sparqlQuery: "+ sparqlQuery);
        LOG.info("policy: "+policy);
        LOG.info("coreferenceResolution: "+String.valueOf(coreferenceResolution));
        LOG.info("spotter: "+ spotterName);
        LOG.info("disambiguator: " + disambiguatorName);

        /* Validating parameters */

        if (text.trim().equals("")) {
            throw new InputException("No text was specified in the &text parameter.");
        }

        /* Setting defaults */
        if (Server.getTokenizer() == null && disambiguatorName==SpotlightConfiguration.DisambiguationPolicy.Default.name()
                && text.length() > 1200) {
            disambiguatorName = SpotlightConfiguration.DisambiguationPolicy.Document.name();
            LOG.info(String.format("Text length: %d. Using %s to disambiguate.",text.length(),disambiguatorName));
        }

        Spotter spotter = Server.getSpotter(spotterName);
        ParagraphDisambiguatorJ disambiguator = Server.getDisambiguator(disambiguatorName);

        /* Running Annotation */

        Annotation annotation = process(text, confidence, support, ontologyTypesString, sparqlQuery, blacklist, coreferenceResolution, spotter, disambiguator);

        LOG.debug("Shown: "+annotation.toXML());
        LOG.debug("****************************************************************");

        return annotation;
    }


}
