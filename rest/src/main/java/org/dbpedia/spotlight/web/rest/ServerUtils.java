package org.dbpedia.spotlight.web.rest;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.exceptions.InputException;

import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author pablomendes
 */
public class ServerUtils {
    static Log LOG = LogFactory.getLog("ServerUtils");

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

    /**
     * Read in the content passed by both &url and &text in query, return the text to be further processed
     * &text got higher priority, it will be returned if not empty
     * if &text is not empty, the main content of the webpage pointed by the URL will be returned.
     * @param text text by the &text query
     * @param inUrl url by the &url query
     * @return String about the main content extracted from the website linked from the URL
     * @throws org.dbpedia.spotlight.exceptions.InputException Thrown when both input from &text and &url are empty
     */
    public static String getTextToProcess(String text, String inUrl) throws InputException {
        String textToProcess = "";
        if (!text.equals("")){
            textToProcess = text;
        }else if (!inUrl.equals("")) {
            LOG.info("Parsing URL to get main content");
            URL url = null;
            try {
                url = new URL(inUrl);
                textToProcess = ArticleExtractor.INSTANCE.getText(url);
            } catch (MalformedURLException e) {
                // e.printStackTrace();
                LOG.error("Input URL is not valid");
                textToProcess = "";
            } catch (BoilerpipeProcessingException e) {
                e.printStackTrace();
                LOG.error("Boilerpipe Cannot process the web page");
                textToProcess = "";
            }

        }else{
            throw new InputException("No input was specified in the &text nor the &url parameter.");
        }
        return textToProcess;
    }

}
