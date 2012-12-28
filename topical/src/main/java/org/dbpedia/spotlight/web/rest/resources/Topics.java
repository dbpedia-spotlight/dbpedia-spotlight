package org.dbpedia.spotlight.web.rest.resources;

import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.model.Topic;
import org.dbpedia.spotlight.topical.TopicalClassifier;
import org.dbpedia.spotlight.web.rest.Server;
import org.dbpedia.spotlight.web.rest.TopicalOutputSerializer;
import org.dbpedia.spotlight.web.rest.TopicalServer;
import scala.Tuple2;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


//TODO how to initialize topical classifier here
/**
 * REST Web Service for topical classification
**/
@ApplicationPath(Server.APPLICATION_PATH)
@Path("/topic")
@Consumes("text/plain")
public class Topics {
    // Sets the necessary headers in order to enable CORS
    private Response ok(String response) {
        return Response.ok().entity(response).header("Access-Control-Allow-Origin","*").build();
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getJSON(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text) {
        TopicalClassifier classifier = TopicalServer.getClassifier();
        Text textObj = new Text(text);
        Tuple2<Topic,Object>[] result = classifier.getPredictions(textObj);

        return ok(TopicalOutputSerializer.topicTagsAsJson(textObj, result).toString());
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postJSON(
            @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
            @Context HttpServletRequest request
    ) {
        TopicalClassifier classifier = TopicalServer.getClassifier();
        Text textObj = new Text(text);
        Tuple2<Topic,Object>[] result = classifier.getPredictions(textObj);

        return ok(TopicalOutputSerializer.topicTagsAsJson(textObj, result).toString());
    }

    @GET
    @Produces( MediaType.APPLICATION_XML )
    public Response getXML(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text) {
        TopicalClassifier classifier = TopicalServer.getClassifier();
        Text textObj = new Text(text);
        Tuple2<Topic,Object>[] result = classifier.getPredictions(textObj);

        return ok(TopicalOutputSerializer.topicTagsAsXml(textObj, result).toString());
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response postXML(
            @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
            @Context HttpServletRequest request
    ) {
        TopicalClassifier classifier = TopicalServer.getClassifier();
        Text textObj = new Text(text);
        Tuple2<Topic,Object>[] result = classifier.getPredictions(textObj);

        return ok(TopicalOutputSerializer.topicTagsAsXml(textObj, result).toString());
    }
}
