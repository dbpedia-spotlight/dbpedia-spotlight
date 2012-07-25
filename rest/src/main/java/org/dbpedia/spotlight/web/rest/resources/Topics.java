package org.dbpedia.spotlight.web.rest.resources;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.model.Topic;
import org.dbpedia.spotlight.topic.TopicalClassifier;
import org.dbpedia.spotlight.web.rest.Server;
import scala.Tuple2;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST Web Service for topical classification
**/
@ApplicationPath(Server.APPLICATION_PATH)
@Path("/topic")
@Consumes("text/plain")
public class Topics {
    XStream jsonStreamer;

    public Topics() {
        jsonStreamer = new XStream(new JettisonMappedXmlDriver());
        jsonStreamer.setMode(XStream.NO_REFERENCES);
        jsonStreamer.autodetectAnnotations(true);
    }

    // Sets the necessary headers in order to enable CORS
    private Response ok(String response) {
        return Response.ok().entity(response).header("Access-Control-Allow-Origin","*").build();
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getJSON(@DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @QueryParam("text") String text) {
        TopicalClassifier classifier = Server.getClassifier();
        Tuple2<Topic,Object>[] result = classifier.getPredictions(new Text(text));
        org.dbpedia.spotlight.web.rest.output.Topic[] topics = new org.dbpedia.spotlight.web.rest.output.Topic[result.length];

        for(int i = 0; i < result.length; i++) {
          topics[i] = new org.dbpedia.spotlight.web.rest.output.Topic(result[i]._1().getName(), (Double) result[i]._2());
        }

        return ok(jsonStreamer.toXML(topics));
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postJSON(
            @DefaultValue(SpotlightConfiguration.DEFAULT_TEXT) @FormParam("text") String text,
            @Context HttpServletRequest request
    ) {
        TopicalClassifier classifier = Server.getClassifier();
        Tuple2<Topic,Object>[] result = classifier.getPredictions(new Text(text));
        org.dbpedia.spotlight.web.rest.output.Topic[] topics = new org.dbpedia.spotlight.web.rest.output.Topic[result.length];

        for(int i = 0; i < result.length; i++) {
            topics[i] = new org.dbpedia.spotlight.web.rest.output.Topic(result[i]._1().getName(), (Double) result[i]._2());
        }

        return ok(jsonStreamer.toXML(topics));
    }

}
