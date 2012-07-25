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
import org.dbpedia.spotlight.extract.LuceneTagExtractor;
import org.dbpedia.spotlight.extract.TagExtractor;
import org.dbpedia.spotlight.model.Factory;
import org.dbpedia.spotlight.model.OntologyType;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.web.rest.ExtractTags;
import org.dbpedia.spotlight.web.rest.OutputSerializer;
import org.dbpedia.spotlight.web.rest.ServerUtils;
import scala.xml.Elem;
import scala.xml.Node;
import scala.xml.PrettyPrinter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;
import scala.collection.immutable.List;
import java.util.Set;

/**
 * REST Web Service
 */

@ApplicationPath(Extract.uri)
@Path("/extract")
@Consumes("text/plain")
public class Extract {

    public static final String uri = "http://160.45.137.71:2222/";

    Log LOG = LogFactory.getLog(this.getClass());

    // Sets the necessary headers in order to enable CORS
    private Response ok(String response) {
        return Response.ok().entity(response).header("Access-Control-Allow-Origin","*").build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJSON(@DefaultValue("") @QueryParam("text") String text,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String resourceTypesString,
                            @DefaultValue("250") @QueryParam("n") int nHits) {

        try {
            List<OntologyType> ontologyTypes = Factory.ontologyType().fromCSVString(resourceTypesString);
            return ok(asJSON(new Text(text), nHits, ontologyTypes));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.APPLICATION_JSON).build());
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response postJSON(@DefaultValue("") @FormParam("text") String text,
                             @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String resourceTypesString,
                            @DefaultValue("250") @FormParam("n") int nHits) {

        try {
            List<OntologyType> ontologyTypes = Factory.ontologyType().fromCSVString(resourceTypesString);
            return ok(asJSON(new Text(text), nHits, ontologyTypes));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.APPLICATION_JSON).build());
        }
    }

    public String asJSON(Text text, int nHits, List<OntologyType> ontologyTypes) {
        String response = OutputSerializer.tagsAsJson(text, ExtractTags.extractor().extract(text, nHits, ontologyTypes));
        LOG.info(String.format("Response: %s",response));
        return response;
    }

    @GET
    @Produces({MediaType.TEXT_XML,MediaType.APPLICATION_XML})
    public Response getXML(@DefaultValue("") @QueryParam("text") String text,
                           @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String resourceTypesString,
                            @DefaultValue("250") @QueryParam("n") int nHits) {

        try {
            List<OntologyType> ontologyTypes = Factory.ontologyType().fromCSVString(resourceTypesString);
            return ok(asXML(new Text(text), nHits, ontologyTypes));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.APPLICATION_XML).build());
        }
    }

    @POST
    @Produces({MediaType.TEXT_XML,MediaType.APPLICATION_XML})
    public Response postXML(@DefaultValue("") @FormParam("text") String textString,
                            @DefaultValue(SpotlightConfiguration.DEFAULT_TYPES) @QueryParam("types") String resourceTypesString,
                            @DefaultValue("250") @FormParam("n") int nHits) {

        try {
            List<OntologyType> ontologyTypes = Factory.ontologyType().fromCSVString(resourceTypesString);
            return ok(asXML(new Text(textString), nHits, ontologyTypes));
            //return ok(asXML(new Text(text), nHits));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(ServerUtils.print(e)).type(MediaType.APPLICATION_XML).build());
        }
    }

    public String asXML(Text text, int nHits, List<OntologyType> ontologyTypes) {
        //PrettyPrinter printer = new scala.xml.PrettyPrinter(80, 2);
        Node node = OutputSerializer.tagsAsXml(text, ExtractTags.extractor().extract(text, nHits, ontologyTypes));
        //StringBuilder builder = new StringBuilder();
        //printer.format(node, builder);
        String response = node.toString();
        LOG.info(String.format("Response: %s",response));
        return response;
    }

}
