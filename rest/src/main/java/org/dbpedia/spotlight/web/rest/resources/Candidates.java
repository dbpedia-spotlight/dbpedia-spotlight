/*
 * *
 *  * Copyright 2011 Pablo Mendes, Max Jakob
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.dbpedia.spotlight.web.rest.resources;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguatorJ;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.web.rest.Server;
import org.dbpedia.spotlight.web.rest.output.Annotation;
import org.dbpedia.spotlight.web.rest.output.OutputSerializer;
import org.dbpedia.spotlight.web.rest.output.Resource;
import org.dbpedia.spotlight.web.rest.output.Spot;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * REST Web Service
 * @author maxjakob
 */

@ApplicationPath("http://spotlight.dbpedia.org/rest")
@Path("/candidates")
@Consumes("text/plain")
public class Candidates {

    private static int k = 100; //FIXME this is hard-coded !!!!!

    @Context
    private UriInfo context;

     Log LOG = LogFactory.getLog(this.getClass());

    // Annotation interface
    /**
     * Does not do any filtering at the moment!!!
     */
    public Annotation process(String text, double confidence, int support, List<OntologyType> dbpediaTypes,
                              String sparqlQuery, boolean blacklist, boolean coreferenceResolution, SpotterConfiguration.SpotterPolicy spotter, SpotlightConfiguration.DisambiguationPolicy disambiguatorName)
            throws SearchException, ItemNotFoundException, InputException {

        Annotation annotation = new Annotation(text);
        List<Spot> spots = new LinkedList<Spot>();

        List<SurfaceFormOccurrence> entityMentions = Server.getSpotters().get(spotter).extract(new Text(text));
        Paragraph paragraph = Factory.paragraph().fromJ(entityMentions);
        LOG.info(String.format("Spotted %d entity mentions.",entityMentions.size()));

        ParagraphDisambiguatorJ disambiguator = Server.getDisambiguators().get(disambiguatorName);

        Map<SurfaceFormOccurrence,List<DBpediaResourceOccurrence>> entityCandidates = disambiguator.bestK(paragraph,k);
        LOG.info(String.format("Disambiguated %d candidates with %s (%s).",entityCandidates.size(),disambiguatorName,disambiguator.name()));

        for(SurfaceFormOccurrence sfOcc : entityCandidates.keySet()) {
            Spot spot = Spot.getInstance(sfOcc);
            List<Resource> resources = new LinkedList<Resource>();
            for(DBpediaResourceOccurrence occ : entityCandidates.get(sfOcc)) {
                Resource resource = Resource.getInstance(occ);
                resources.add(resource);
            }
            spot.setResources(resources);
            spots.add(spot);
        }
        annotation.setSpots(spots);
        return annotation;
    }

    // Sets the necessary headers in order to enable CORS
    private Response ok(String response) {
        return Response.ok().entity(response).header("Access-Control-Allow-Origin","*").build();
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
//            return ok(response);
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
//            return ok(candidatesInterface.getRDFa(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp));
//        } catch (Exception e) {
//            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(e.getMessage()).type(MediaType.APPLICATION_XHTML_XML).build());
//        }
//    }

    @GET
    @Produces(MediaType.TEXT_XML)
    public Response getXML(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_SPOTTER) @QueryParam("spotter") String spotter,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_DISAMBIGUATOR) @QueryParam("disambiguator") String disambiguatorName,
                           @Context HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();

        try {
            Annotation a = getAnnotation(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, SpotterConfiguration.SpotterPolicy.valueOf(spotter), SpotlightConfiguration.DisambiguationPolicy.valueOf(disambiguatorName), clientIp);
            LOG.info("XML format");
            String content = output.toXML(a);
            return ok(content);
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(e.getMessage()).type(MediaType.TEXT_XML).build());
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJSON(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_SPOTTER) @QueryParam("spotter") String spotter,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_DISAMBIGUATOR) @QueryParam("disambiguator") String disambiguatorName,
                            @Context HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();

        try {
            Annotation a = getAnnotation(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, SpotterConfiguration.SpotterPolicy.valueOf(spotter), SpotlightConfiguration.DisambiguationPolicy.valueOf(disambiguatorName), clientIp);
            LOG.info("JSON format");
            String content = output.toJSON(a);
            return ok(content);
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build());
        }
    }

//    //Patch provided by Paul Houle
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
            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @FormParam("confidence") Double confidence,
            @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @FormParam("support") int support,
            @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @FormParam("types") String dbpediaTypes,
            @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @FormParam("sparql") String sparqlQuery,
            @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @FormParam("policy") String policy,
            @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @FormParam("coreferenceResolution") boolean coreferenceResolution,
            @DefaultValue(SpotlightConfiguration.DEFAULT_SPOTTER) @QueryParam("spotter") String spotter,
            @DefaultValue(SpotlightConfiguration.DEFAULT_DISAMBIGUATOR) @QueryParam("disambiguator") String disambiguatorName,
            @Context HttpServletRequest request
    ) {
        return getXML(text,confidence,support,dbpediaTypes,sparqlQuery,policy,coreferenceResolution,spotter,disambiguatorName,request);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postJSON(
            @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @FormParam("confidence") Double confidence,
            @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @FormParam("support") int support,
            @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @FormParam("types") String dbpediaTypes,
            @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @FormParam("sparql") String sparqlQuery,
            @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @FormParam("policy") String policy,
            @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @FormParam("coreferenceResolution") boolean coreferenceResolution,
            @DefaultValue(SpotlightConfiguration.DEFAULT_SPOTTER) @QueryParam("spotter") String spotter,
            @DefaultValue(SpotlightConfiguration.DEFAULT_DISAMBIGUATOR) @QueryParam("disambiguator") String disambiguatorName,
            @Context HttpServletRequest request
    ) {
        return getJSON(text,confidence,support,dbpediaTypes,sparqlQuery,policy,coreferenceResolution,spotter,disambiguatorName,request);
    }

    private OutputSerializer output = new OutputSerializer();

    public Annotation getAnnotation(String text,
                                    double confidence,
                                    int support,
                                    String ontologyTypesString,
                                    String sparqlQuery,
                                    String policy,
                                    boolean coreferenceResolution,
                                    SpotterConfiguration.SpotterPolicy spotter,
                                    SpotlightConfiguration.DisambiguationPolicy disambiguatorName,
                                    String clientIp) throws SearchException, InputException, ItemNotFoundException {

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
        LOG.info("text: " + text);
        LOG.info("text length in chars: "+text.length());
        LOG.info("confidence: "+String.valueOf(confidence));
        LOG.info("support: "+String.valueOf(support));
        LOG.info("types: "+ontologyTypesString);
        LOG.info("sparqlQuery: "+ sparqlQuery);
        LOG.info("policy: "+policy);
        LOG.info("coreferenceResolution: "+String.valueOf(coreferenceResolution));
        LOG.info("spotter: "+String.valueOf(spotter));
        LOG.info("disambiguator: "+disambiguatorName.name());

        if (text.trim().equals("")) {
            throw new InputException("No text was specified in the &text parameter.");
        }

        if (disambiguatorName==SpotlightConfiguration.DisambiguationPolicy.Default
                && text.length() > 2000) {
            disambiguatorName = SpotlightConfiguration.DisambiguationPolicy.Document;
            LOG.info(String.format("Text length: %d. Using %s to disambiguate.",text.length(),disambiguatorName));
        }

        List<OntologyType> ontologyTypes = new ArrayList<OntologyType>();
        String types[] = ontologyTypesString.trim().split(",");
        for (String t : types){
            if (!t.trim().equals("")) ontologyTypes.add(Factory.ontologyType().fromQName(t.trim()));
            //LOG.info("type:"+t.trim());
        }

        Annotation annotation = process(text, confidence, support, ontologyTypes, sparqlQuery, blacklist, coreferenceResolution, spotter, disambiguatorName);

        LOG.info("Shown: "+annotation.toXML());

        return annotation;
    }


}
