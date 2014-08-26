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
import org.dbpedia.spotlight.model.AnnotationParameters;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.web.rest.*;
import org.dbpedia.spotlight.web.rest.output.Annotation;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * REST Web Service for annotation: spotting, candidate selection, disambiguation, linking
 *
 * @author pablomendes
 */

@ApplicationPath(Server.APPLICATION_PATH)
@Path("/spot")
@Consumes("text/plain")
public class Spot extends BaseRestResource{


    public Spot(){
        LOG = LogFactory.getLog(this.getClass());
        apiName = "spot";
    }

    private OutputManager outputManager = new OutputManager();

    private String spot(AnnotationParameters params, OutputManager.OutputFormat outputType, String textToProcess) throws Exception{
        announce(textToProcess, params);
        List<SurfaceFormOccurrence> spots = Server.model.spot(new Text(textToProcess), params);
        Annotation annotation = new Annotation(new Text(textToProcess), spots);
        outputManager.makeOutput(textToProcess, annotation, outputType, params);
        String response = new Annotation(new Text(textToProcess), spots).toXML();
        return response;
    }

    @Context
    private UriInfo context;

    @GET
    @Produces({MediaType.TEXT_XML,MediaType.APPLICATION_XML})
    public Response getXML(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
                            //@DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                            //@DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                            @DefaultValue("Default") @QueryParam("spotter") String spotterName,
                            @Context HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        AnnotationParameters params = new AnnotationParameters();
        params.spotterName = spotterName;
        params.inUrl = inUrl;
        params.clientIp = clientIp;

       // announce(text, params);

        try {
            String textToProcess = ServerUtils.getTextToProcess(text, inUrl);
            String response = spot(params, OutputManager.OutputFormat.TEXT_XML, textToProcess);
            return ServerUtils.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.TEXT_HTML).build());
        }
    }

    @GET
    @Produces({"text/turtle", "text/plain", "application/rdf+xml"})
    public Response getNIF( @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
			                @DefaultValue("Default") @QueryParam("spotter") String spotterName,
			                @QueryParam("prefix") String prefix,
			                @DefaultValue("offset") @QueryParam("urirecipe") String recipe,
			                @DefaultValue("10") @QueryParam("context-length") int ctxLength,
                            @Context HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        AnnotationParameters params = new AnnotationParameters();
        params.spotterName = spotterName;
        params.inUrl = inUrl;
        params.clientIp = clientIp;

	    // when no prefix argument specified and url param is used the prefix
	    // is set to the given url
	    if (prefix == null && !inUrl.equals(""))
	        prefix = inUrl + "#";

	    // when no prefix argument specified and text param is used the prefix
	    // is set to the spotlight url + the given text
	    else if (prefix == null && !text.equals(""))
	        prefix = "http://spotlight.dbpedia.org/rest/document/?text="+text+"#";

        OutputManager.OutputFormat format = OutputManager.OutputFormat.TURTLE;
        String accept = request.getHeader("accept");
        if (accept.equalsIgnoreCase("text/turtle"))
            format = OutputManager.OutputFormat.TURTLE;
        else if (accept.equalsIgnoreCase("text/plain"))
            format = OutputManager.OutputFormat.NTRIPLES;
        else if (accept.equalsIgnoreCase("application/rdf+xml"))
            format = OutputManager.OutputFormat.RDFXML;

        try {
            String textToProcess = ServerUtils.getTextToProcess(text, inUrl);

            List<SurfaceFormOccurrence> spots = Server.model.spot(new Text(textToProcess), params);
	        String response = NIFOutputFormatter.fromSurfaceFormOccs(text, spots, format, prefix);
            return ServerUtils.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(accept).build());
        }
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJSON(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @QueryParam("url") String inUrl,
                            //@DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                            //@DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                            @DefaultValue("Default") @QueryParam("spotter") String spotterName,
                            @Context HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        AnnotationParameters params = new AnnotationParameters();
        params.spotterName = spotterName;
        params.inUrl = inUrl;
        params.clientIp = clientIp;

        try {
            String textToProcess = ServerUtils.getTextToProcess(text, inUrl);
            String response = spot(params, OutputManager.OutputFormat.JSON, textToProcess);
            return ServerUtils.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.TEXT_HTML).build());
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({MediaType.TEXT_XML,MediaType.APPLICATION_XML})
    public Response postXML(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @FormParam("url") String inUrl,
                            //@DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                            //@DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                            @DefaultValue("Default") @FormParam("spotter") String spotterName,
                            @Context HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        AnnotationParameters params = new AnnotationParameters();
        params.spotterName = spotterName;
        params.inUrl = inUrl;
        params.clientIp = clientIp;

        try {
            String textToProcess = ServerUtils.getTextToProcess(text, inUrl);
            String response = spot(params, OutputManager.OutputFormat.TEXT_XML, textToProcess);
            return ServerUtils.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.TEXT_HTML).build());
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({"text/turtle", "text/plain", "application/rdf+xml"})
    public Response postNIF(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
			    @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @FormParam("url") String inUrl,
			    @DefaultValue("Default") @FormParam("spotter") String spotterName,
			    @FormParam("prefix") String prefix,
			    @DefaultValue("offset") @FormParam("urirecipe") String recipe,
			    @DefaultValue("10") @FormParam("context-length") int ctxLength,
			    @Context HttpServletRequest request) {

	return getNIF(text, inUrl, spotterName, prefix, recipe, ctxLength, request);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postJSON(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
                             @DefaultValue(SpotlightConfiguration.DEFAULT_URL) @FormParam("url") String inUrl,
                            //@DefaultValue(SpotlightConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                            //@DefaultValue(SpotlightConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                            @DefaultValue("Default") @FormParam("spotter") String spotterName,
                            @Context HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        AnnotationParameters params = new AnnotationParameters();
        params.spotterName = spotterName;
        params.inUrl = inUrl;
        params.clientIp = clientIp;

        try {
            String textToProcess = ServerUtils.getTextToProcess(text, inUrl);
            String response = spot(params, OutputManager.OutputFormat.JSON, textToProcess);
            return ServerUtils.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.TEXT_HTML).build());
        }
    }


}
