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

import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.SpotterConfiguration;
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy;
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy;

import org.dbpedia.spotlight.web.rest.Server;
import org.dbpedia.spotlight.web.rest.ServerUtils;
import org.dbpedia.spotlight.web.rest.SpotlightInterface;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * REST Web Service
 * TODO Merge with Annotate (only difference is the SpotlightInterface object, which can be given in constructor)
 */

@ApplicationPath(Server.APPLICATION_PATH)
@Path("/disambiguate")
@Consumes("text/plain")
public class Disambiguate {
    @Context
    private UriInfo context;

    // Disambiguation interface
    private static SpotlightInterface disambigInterface = new SpotlightInterface("/disambiguate");

    @GET
    @Produces("text/html")
    public Response getHTML(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                          @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
                          @Context HttpServletRequest request
    ) {
        String clientIp = request.getRemoteAddr();
        try {
            return ServerUtils.ok(disambigInterface.getHTML(text,inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, SpotterPolicy.UserProvidedSpots.name(),disambiguatorName));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.TEXT_HTML).build());
        }
    }

    @GET
    @Produces("application/xhtml+xml")
    public Response getRDFa(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                          @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
                          @Context HttpServletRequest request
    ) {
        String clientIp = request.getRemoteAddr();
        try {
            return ServerUtils.ok(disambigInterface.getRDFa(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, SpotterPolicy.UserProvidedSpots.name() , disambiguatorName));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.APPLICATION_XHTML_XML).build());
        }
    }

    @GET
    @Produces("text/xml")
    public Response getXML(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                         @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
                         @Context HttpServletRequest request
    ) {
        String clientIp = request.getRemoteAddr();

        try {
            return ServerUtils.ok(disambigInterface.getXML(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, SpotterPolicy.UserProvidedSpots.name(), disambiguatorName));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.TEXT_XML).build());
        }
    }

    @GET
    @Produces("text/turtle")
    public Response getTurtle(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
			      @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
			      @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
			      @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
			      @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
			      @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
			      @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
			      @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
			      @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
			      @QueryParam("prefix") String prefix,
			      @DefaultValue("offset") @QueryParam("urirecipe") String recipe,
			      @DefaultValue("10") @QueryParam("context-length") int ctxLength,
			      @Context HttpServletRequest request
    ) {
        String clientIp = request.getRemoteAddr();

        try {
            return ServerUtils.ok(disambigInterface.getNIF(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, SpotterPolicy.UserProvidedSpots.name(), disambiguatorName, "turtle", prefix, recipe, ctxLength));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type("text/turtle").build());
        }
    }

    @GET
    @Produces("text/plain")
    public Response getNTriples(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
				@DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
				@DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
				@DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
				@DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
				@DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
				@DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
				@DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
				@DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
				@QueryParam("prefix") String prefix,
				@DefaultValue("offset") @QueryParam("urirecipe") String recipe,
				@DefaultValue("10") @QueryParam("context-length") int ctxLength,
				@Context HttpServletRequest request
    ) {
        String clientIp = request.getRemoteAddr();

        try {
            return ServerUtils.ok(disambigInterface.getNIF(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, SpotterPolicy.UserProvidedSpots.name(), disambiguatorName, "ntriples", prefix, recipe, ctxLength));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type("text/plain").build());
        }
    }

    @GET
    @Produces("application/rdf+xml")
    public Response getRdfXML(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
			      @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
			      @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
			      @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
			      @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
			      @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
			      @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
			      @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
			      @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
			      @QueryParam("prefix") String prefix,
			      @DefaultValue("offset") @QueryParam("urirecipe") String recipe,
			      @DefaultValue("10") @QueryParam("context-length") int ctxLength,
			      @Context HttpServletRequest request
    ) {
        String clientIp = request.getRemoteAddr();

        try {
            return ServerUtils.ok(disambigInterface.getNIF(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, SpotterPolicy.UserProvidedSpots.name(), disambiguatorName, "rdfxml", prefix, recipe, ctxLength));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type("application/rdf+xml").build());
        }
    }

    @GET
    @Produces("application/json")
    public Response getJSON(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                          @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
                          @Context HttpServletRequest request
    ) {
        String clientIp = request.getRemoteAddr();

        try {
            return ServerUtils.ok(disambigInterface.getJSON(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, SpotterPolicy.UserProvidedSpots.name(), disambiguatorName));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.APPLICATION_JSON).build());
        }
    }

    //----------------

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
      @DefaultValue("Default") @QueryParam("disambiguator") String disambiguatorName,
      @Context HttpServletRequest request
      ) {
        String clientIp = request.getRemoteAddr();
        try {
            return ServerUtils.ok(disambigInterface.getHTML(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, SpotterPolicy.UserProvidedSpots.name(), disambiguatorName));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.TEXT_HTML).build());
        }

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
      @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
      @Context HttpServletRequest request
      ) {
        try {
            String clientIp = request.getRemoteAddr();
            return ServerUtils.ok(disambigInterface.getHTML(text,inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, SpotterPolicy.UserProvidedSpots.name(), disambiguatorName));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.TEXT_HTML).build());
        }

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
      @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
      @Context HttpServletRequest request
      ) {
        try {
            String clientIp = request.getRemoteAddr();
            return ServerUtils.ok(disambigInterface.getXML(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, SpotterPolicy.UserProvidedSpots.name(), disambiguatorName));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.TEXT_XML).build());
        }

    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/turtle")
    public Response postTurtle(
      @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
      @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @FormParam("url") String inUrl,
      @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @FormParam("confidence") Double confidence,
      @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @FormParam("support") int support,
      @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @FormParam("types") String dbpediaTypes,
      @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @FormParam("sparql") String sparqlQuery,
      @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @FormParam("policy") String policy,
      @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @FormParam("coreferenceResolution") boolean coreferenceResolution,
      @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
      @FormParam("prefix") String prefix,
      @DefaultValue("offset") @FormParam("urirecipe") String recipe,
      @DefaultValue("10") @FormParam("context-length") int ctxLength,
      @Context HttpServletRequest request
      ) {
        try {
            String clientIp = request.getRemoteAddr();
            return ServerUtils.ok(disambigInterface.getNIF(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, SpotterPolicy.UserProvidedSpots.name(), disambiguatorName, "turtle", prefix, recipe, ctxLength));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type("text/turtle").build());
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/plain")
    public Response postNTriples(
      @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
      @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @FormParam("url") String inUrl,
      @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @FormParam("confidence") Double confidence,
      @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @FormParam("support") int support,
      @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @FormParam("types") String dbpediaTypes,
      @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @FormParam("sparql") String sparqlQuery,
      @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @FormParam("policy") String policy,
      @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @FormParam("coreferenceResolution") boolean coreferenceResolution,
      @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
      @FormParam("prefix") String prefix,
      @DefaultValue("offset") @FormParam("urirecipe") String recipe,
      @DefaultValue("10") @FormParam("context-length") int ctxLength,
      @Context HttpServletRequest request
      ) {
        try {
            String clientIp = request.getRemoteAddr();
            return ServerUtils.ok(disambigInterface.getNIF(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, SpotterPolicy.UserProvidedSpots.name(), disambiguatorName, "ntriples", prefix, recipe, ctxLength));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type("text/plain").build());
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/rdf+xml")
    public Response postRdfXML(
      @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
      @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @FormParam("url") String inUrl,
      @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @FormParam("confidence") Double confidence,
      @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @FormParam("support") int support,
      @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @FormParam("types") String dbpediaTypes,
      @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @FormParam("sparql") String sparqlQuery,
      @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @FormParam("policy") String policy,
      @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @FormParam("coreferenceResolution") boolean coreferenceResolution,
      @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
      @FormParam("prefix") String prefix,
      @DefaultValue("offset") @FormParam("urirecipe") String recipe,
      @DefaultValue("10") @FormParam("context-length") int ctxLength,
      @Context HttpServletRequest request
      ) {
        try {
            String clientIp = request.getRemoteAddr();
            return ServerUtils.ok(disambigInterface.getNIF(text, inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, SpotterPolicy.UserProvidedSpots.name(), disambiguatorName, "rdfxml", prefix, recipe, ctxLength));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type("application/rdf+xml").build());
        }
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
      @DefaultValue("Default") @FormParam("disambiguator") String disambiguatorName,
      @Context HttpServletRequest request
      ) {
        String clientIp = request.getRemoteAddr();
        try {
            return ServerUtils.ok(disambigInterface.getJSON(text,inUrl, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp, SpotterPolicy.UserProvidedSpots.name(), disambiguatorName));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.APPLICATION_JSON).build());
        }
      }
}
