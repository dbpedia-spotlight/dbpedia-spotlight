/*
 * Copyright 2012 DBpedia Spotlight Development Team
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

package org.dbpedia.spotlight.web.rest.related;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.web.rest.RelatedResources;
import org.dbpedia.spotlight.web.rest.Server;
import org.dbpedia.spotlight.web.rest.ServerUtils;
import org.dbpedia.spotlight.web.rest.SpotlightInterface;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * REST Web Service
 */

@ApplicationPath(Related.uri)
@Path("/related")
@Consumes("text/plain")
public class Related {

    public static final String uri = "http://160.45.137.71:2222/";

    Log LOG = LogFactory.getLog(this.getClass());
    RelatedResources searcher = new RelatedResources();

    // Sets the necessary headers in order to enable CORS
    private Response ok(String response) {
        return Response.ok().entity(response).header("Access-Control-Allow-Origin","*").build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJSON(@DefaultValue("") @QueryParam("uri") String uri,
                            @DefaultValue("10") @QueryParam("n") int nHits) {

//        if (uri==null || uri.equals(""))
//            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity("The parameter &uri cannot be null.").type(MediaType.APPLICATION_JSON).build());
        try {
            //if (uri==null || uri.equals("")) {
                Set<String> uriSet = new HashSet<String>(Arrays.asList(uri.split(" ")));
                String response = searcher.queryAsJson(uriSet, nHits);
                LOG.info(String.format("Response: %s",response));
                return ok(response);
            //} else {
            //    return Response.status(Response.Status.BAD_REQUEST). entity("The parameter &uri cannot be null.").type(MediaType.APPLICATION_JSON).build();
            //}
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.APPLICATION_JSON).build());
        }
    }

}
