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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.web.rest.Server;
import org.dbpedia.spotlight.web.rest.ServerUtils;
import org.dbpedia.spotlight.web.rest.SpotlightInterface;

import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy;
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * REST Web Service for annotation: spotting, candidate selection, disambiguation, linking
 *
 * @author pablomendes
 * @author Paul Houle (patch for POST)
 */

@ApplicationPath(Server.APPLICATION_PATH)
@Path("/annotate")
@Consumes("text/plain")
public class Annotate {

    Log LOG = LogFactory.getLog(this.getClass());

    @Context
    private UriInfo context;

    // Annotation interface
    private static SpotlightInterface annotationInterface =  new SpotlightInterface("/annotate");
    
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getHTML(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                            @DefaultValue("Default") @QueryParam("spotter") String spotterName,
                            @DefaultValue("Default") @QueryParam("disambiguator") String disambiguatorName, 
                            @Context HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();

        try {
            String response = annotationInterface.getHTML(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, spotterName,  disambiguatorName);
            return ServerUtils.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.TEXT_HTML).build());
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public Response getRDFa(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                          @DefaultValue("Default") @QueryParam("spotter") String spotterName,
                          @DefaultValue("Default") @QueryParam("disambiguator") String disambiguatorName,
                          @Context HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();

        try {
            return ServerUtils.ok(annotationInterface.getRDFa(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, spotterName, disambiguatorName));
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.APPLICATION_XHTML_XML).build());
        }
    }

    @GET
    @Produces({MediaType.TEXT_XML,MediaType.APPLICATION_XML})
    public Response getXML(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                         @DefaultValue("Default") @QueryParam("spotter") String spotterName,
                         @DefaultValue("Default") @QueryParam("disambiguator") String disambiguatorName,
                          @Context HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();

        try {
           return ServerUtils.ok(annotationInterface.getXML(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, spotterName, disambiguatorName));
       } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.TEXT_XML).build());
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
                          @DefaultValue("Default") @QueryParam("spotter") String spotterName,
                          @DefaultValue("Default") @QueryParam("disambiguator") String disambiguatorName,
                          @Context HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();

        try {
            return ServerUtils.ok(annotationInterface.getJSON(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, spotterName, disambiguatorName));
       } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.APPLICATION_JSON).build());
        }
    }

    //Patch provided by Paul Houle

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response postHTML(
      @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
      @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @FormParam("url") String inUrl,
      @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @FormParam("confidence") Double confidence,
      @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @FormParam("support") int support,
      @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @FormParam("types") String dbpediaTypes,
      @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @FormParam("sparql") String sparqlQuery,
      @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @FormParam("policy") String policy,
      @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @FormParam("coreferenceResolution") boolean coreferenceResolution,
      @DefaultValue("Default") @FormParam("spotter") String spotterName,
      @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
      @Context HttpServletRequest request              
      ) {
        return getHTML(text,inUrl,confidence,support,dbpediaTypes,sparqlQuery,policy,coreferenceResolution,spotterName,disambiguatorName,request);
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public Response postRDFa(
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
        return getRDFa(text,inUrl,confidence,support,dbpediaTypes,sparqlQuery,policy,coreferenceResolution,spotter,disambiguatorName,request);
    }
    
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

}
