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
import org.dbpedia.spotlight.io.CSVFeedbackStore;
import org.dbpedia.spotlight.io.FeedbackStore;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.web.rest.Server;
import org.dbpedia.spotlight.web.rest.ServerUtils;
import org.dbpedia.spotlight.web.rest.SpotlightInterface;
import org.dbpedia.spotlight.web.rest.output.Annotation;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * REST Web Service for feedback
 *
 * TODO bulk feedback: users can post a gzip with json or xml encoded feedback
 *
 * @author pablomendes
 */

@ApplicationPath(Server.APPLICATION_PATH)
@Path("/feedback")
@Consumes("text/plain")
public class Feedback {

    Log LOG = LogFactory.getLog(this.getClass());

    @Context
    private UriInfo context;

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({MediaType.TEXT_XML,MediaType.APPLICATION_XML})
    public Response getXML(@DefaultValue("") @FormParam("text") String text,
                           @DefaultValue("") @FormParam("doc_url") String docUrlString,
                           @DefaultValue("") @FormParam("entity_uri") String entityUri,
                           @DefaultValue("") @FormParam("surface_form") String surfaceForm,
                           @DefaultValue("") @FormParam("systems") String systemIds,
                           @DefaultValue("0") @FormParam("offset") int offset,
                           @DefaultValue("") @FormParam("feedback") String feedback,
                            @Context HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();

        try {
            if (docUrlString.equals("") && text.equals(""))
                throw new InputException("Either doc_url or text must be filled!");
            String response = "";
            String[] systems = systemIds.split(" ");
            URL docUrl = null;
            try {
                docUrl = new URL(docUrlString);
            } catch (MalformedURLException ignored) {}

            if (docUrl==null)
                docUrl = new URL("http://spotlight.dbpedia.org/id/"+text.hashCode());

            FeedbackStore output = new CSVFeedbackStore(System.out);
            output.add(docUrl,new Text(text),new DBpediaResource(entityUri),new SurfaceForm(surfaceForm),offset,feedback,systems);
            response = "ok";
            return ServerUtils.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.TEXT_HTML).build());
        }
    }



}
