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
import org.dbpedia.spotlight.web.rest.OutputManager;
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
public class Candidates extends BaseRestResource {

    public Candidates(){
        LOG = LogFactory.getLog(this.getClass());
        apiName = "candidates";
    }

    @Context
    private UriInfo context;
    private OutputManager outputManager = new OutputManager();

    Log LOG = LogFactory.getLog(this.getClass());

    public String getCandidates(String text, AnnotationParameters params, int numberOfCandidates, OutputManager.OutputFormat format) throws Exception{
        announce(text, params);
        String response = "";
        Map<SurfaceFormOccurrence, List<DBpediaResourceOccurrence>> filteredEntityCandidates = Server.model.nBest(text, params, numberOfCandidates);

        Annotation annotation = new Annotation(text);
        List<Spot> spots = new LinkedList<Spot>();


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

        response = outputManager.makeOutput(text, annotation, format, params);

        System.out.println("****************************filtered candidates:" + filteredEntityCandidates.size());

        return response;
    }


    @GET
    @Produces(MediaType.TEXT_XML)
    public Response getXML(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double disambiguationConfidence,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("spotterConfidence") Double spotterConfidence,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                           @DefaultValue("Default") @QueryParam("spotter") String spotter,
                           @DefaultValue("Default") @QueryParam("disambiguator") String disambiguatorName,
                           @Context HttpServletRequest request) {

        System.out.println("****************************parsing args" );
        String clientIp = request.getRemoteAddr();

                AnnotationParameters params = new AnnotationParameters();
                params.spotterName = spotter;
                params.inUrl = inUrl;
                params.clientIp = clientIp;
                params.disambiguationConfidence = disambiguationConfidence;
                params.spotterConfidence = spotterConfidence;
                params.support = support;
                params.dbpediaTypes = dbpediaTypes;
                params.sparqlQuery = sparqlQuery;
                params.setPolicyValue(policy);
                params.coreferenceResolution = coreferenceResolution;
                params.similarityThresholds = Server.getSimilarityThresholds();
                params.sparqlExecuter = Server.getSparqlExecute();

        System.out.println("****************************about to call" );

        try {
            String textToProcess = ServerUtils.getTextToProcess(text, inUrl);
            String result =  getCandidates(textToProcess, params, 100, OutputManager.OutputFormat.TEXT_XML);
            System.out.println("****************************finished call" );

           // LOG.info("XML format");
           // String content = a.toXML();
            return ServerUtils.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_XML).build());
        }
    }



    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJSON(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double disambiguationConfidence,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double spotterConfidence,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                            @DefaultValue("Default") @QueryParam("spotter") String spotter,
                            @DefaultValue("Default") @QueryParam("disambiguator") String disambiguatorName,
                            @Context HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();

        AnnotationParameters params = new AnnotationParameters();
        params.spotterName = spotter;
        params.inUrl = inUrl;
        params.clientIp = clientIp;
        params.disambiguationConfidence = disambiguationConfidence;
        params.spotterConfidence = spotterConfidence;
        params.support = support;
        params.dbpediaTypes = dbpediaTypes;
        params.sparqlQuery = sparqlQuery;
        params.setPolicyValue(policy);
        params.coreferenceResolution = coreferenceResolution;
        params.similarityThresholds = Server.getSimilarityThresholds();
        params.sparqlExecuter = Server.getSparqlExecute();


        try {
            String textToProcess = ServerUtils.getTextToProcess(text, inUrl);
            String result =  getCandidates(textToProcess, params, 100, OutputManager.OutputFormat.JSON);
            return ServerUtils.ok(result);
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build());
        }
    }


    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_XML)
    public Response postXML(
            @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
            @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @FormParam("url") String inUrl,
            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @FormParam("confidence") Double disambiguationConfidence,
            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @FormParam("confidence") Double spotterConfidence,
            @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @FormParam("support") int support,
            @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @FormParam("types") String dbpediaTypes,
            @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @FormParam("sparql") String sparqlQuery,
            @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @FormParam("policy") String policy,
            @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @FormParam("coreferenceResolution") boolean coreferenceResolution,
            @DefaultValue("Default") @FormParam("spotter") String spotter,
            @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
            @Context HttpServletRequest request
    ) {


        return getXML(text,inUrl,disambiguationConfidence,spotterConfidence,support,dbpediaTypes,sparqlQuery,policy,coreferenceResolution,spotter,disambiguatorName,request);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postJSON(
            @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
            @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @FormParam("url") String inUrl,
            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @FormParam("confidence") Double disambiguationConfidence,
            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @FormParam("confidence") Double spotterConfidence,
            @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @FormParam("support") int support,
            @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @FormParam("types") String dbpediaTypes,
            @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @FormParam("sparql") String sparqlQuery,
            @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @FormParam("policy") String policy,
            @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @FormParam("coreferenceResolution") boolean coreferenceResolution,
            @DefaultValue("Default") @FormParam("spotter") String spotter,
            @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
            @Context HttpServletRequest request
    ) {
        return getJSON(text,inUrl,disambiguationConfidence,spotterConfidence,support,dbpediaTypes,sparqlQuery,policy,coreferenceResolution,spotter,disambiguatorName,request);
    }


}

