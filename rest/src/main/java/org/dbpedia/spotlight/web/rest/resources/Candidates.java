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

package org.dbpedia.spotlight.web.rest.resources;

import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.web.rest.Server;
import org.dbpedia.spotlight.web.rest.SpotlightInterface;
import org.dbpedia.spotlight.web.rest.SpotlightInterfaceNEW;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * REST Web Service
 * @author pablomendes
 * @author Paul Houle (patch for POST)
 * @author majakob
 */

@ApplicationPath("http://spotlight.dbpedia.org/rest")
@Path("/candidates")
@Consumes("text/plain")
public class Candidates {

    private static int candidatesNumber = 100; //FIXME this is hard-coded !!!!!

    @Context
    private UriInfo context;

    // Annotation interface
    private static SpotlightInterfaceNEW candidatesInterface = SpotlightInterfaceNEW.getInstance(Server.getAnnotator(), Server.getConfiguration(), candidatesNumber);

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
                           @Context HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();

        try {
            return ok(candidatesInterface.getXML(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp));
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
                            @Context HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();

        try {
            return ok(candidatesInterface.getJSON(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp));
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
            @Context HttpServletRequest request
    ) {
        return getXML(text,confidence,support,dbpediaTypes,sparqlQuery,policy,coreferenceResolution,request);
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
            @Context HttpServletRequest request
    ) {
        return getJSON(text,confidence,support,dbpediaTypes,sparqlQuery,policy,coreferenceResolution,request);
    }

}
