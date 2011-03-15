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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * REST Web Service
 * TODO Merge with Annotate (only difference is the SpotlightInterface object, which can be given in constructor)
 */

@ApplicationPath("http://spotlight.dbpedia.org/rest")
@Path("/disambiguate")
@Consumes("text/plain")
public class Disambiguate {
    @Context
    private UriInfo context;

    // Disambiguation interface
    private static SpotlightInterface disambigInterface = SpotlightInterface.getInstance(Server.getDisambiguator(), Server.getConfiguration());

    // Sets the necessary headers in order to enable CORS
    private Response ok(String response) {
        return Response.ok().entity(response).header("Access-Control-Allow-Origin","*").build();
    }

    @GET
    @Produces("text/html")
    public Response getHTML(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                          @Context HttpServletRequest request
    ) throws Exception {
        String clientIp = request.getRemoteAddr();
        return ok(disambigInterface.getHTML(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp));
    }

    @GET
    @Produces("application/xhtml+xml")
    public Response getRDFa(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                          @Context HttpServletRequest request
    ) throws Exception {
        String clientIp = request.getRemoteAddr();
        return ok(disambigInterface.getRDFa(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp));
    }

    @GET
    @Produces("text/xml")
    public Response getXML(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                         @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                          @Context HttpServletRequest request
    ) throws Exception {
        String clientIp = request.getRemoteAddr();

        return ok(disambigInterface.getXML(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp));
    }

    @GET
    @Produces("application/json")
    public Response getJSON(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                          @DefaultValue(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution,
                          @Context HttpServletRequest request
    ) throws Exception {
        String clientIp = request.getRemoteAddr();

        return ok(disambigInterface.getJSON(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution, clientIp));
    }

}
