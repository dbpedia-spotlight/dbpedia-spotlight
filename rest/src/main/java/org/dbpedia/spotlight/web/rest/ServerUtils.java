package org.dbpedia.spotlight.web.rest;

import javax.ws.rs.core.Response;

/**
 * @author pablomendes
 */
public class ServerUtils {

    // Sets the necessary headers in order to enable CORS
    public static Response ok(String response) {
        return Response.ok().entity(response).header("Access-Control-Allow-Origin","*").build();
    }

    public static String print(Exception exception) {  //TODO need a nicer way to send error messages to client
        String eMessage = exception.getMessage();
        StackTraceElement[] elements = exception.getStackTrace();
        StringBuilder msg = new StringBuilder();
        msg.append(exception);
        msg.append(eMessage);
        for (StackTraceElement e: elements) {
            msg.append(e.toString());
            msg.append("\n");
        }
        return msg.toString();
    }

}
