/*
* Copyright 2012 DBpedia Spotlight Development Team
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
*
* Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
*/

package org.dbpedia.spotlight.sparql;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import org.apache.http.client.*;
import org.apache.http.impl.client.*;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.*;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.log4j.Logger;
import org.dbpedia.spotlight.exceptions.OutputException;
import org.dbpedia.spotlight.exceptions.SparqlExecutionException;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;


/**
 * Gets a list of DBpediaResources matching a given SPARQL query.
 * Will be used for filtering annotations.
 *
 * @author PabloMendes
 *
 */
public class SparqlQueryExecuter {

    private final static Logger LOG = Logger.getLogger(SparqlQueryExecuter.class);

    // Create an instance of HttpClient.
    private static HttpClient client = new DefaultHttpClient();

    String mainGraph;
    String sparqlUrl;

    public SparqlQueryExecuter(String mainGraph, String sparqlUrl) {
        this.mainGraph = mainGraph;
        this.sparqlUrl = sparqlUrl;
    }

    // this is the virtuoso way. subclasses can override for other implementations
    //http://dbpedia.org/sparql?default-graph-uri=http://dbpedia.org&query=select+distinct+%3Fpol+where+{%3Fpol+a+%3Chttp://dbpedia.org/ontology/Politician%3E+}&debug=on&timeout=&format=text/html&save=display&fname=
    protected URL getUrl(String query) throws UnsupportedEncodingException, MalformedURLException {
        String graphEncoded = URLEncoder.encode(mainGraph, "UTF-8");
        String formatEncoded = URLEncoder.encode("application/sparql-results+json", "UTF-8");
        String queryEncoded = URLEncoder.encode(query, "UTF-8");
        String url = sparqlUrl+"?"+"default-graph-uri="+graphEncoded+"&query="+queryEncoded+"&format="+formatEncoded+"&debug=on&timeout=";
        return new URL(url);
    }

    public List<DBpediaResource> query(String query) throws IOException, OutputException, SparqlExecutionException {
        if (query==null) return new ArrayList<DBpediaResource>();
        LOG.debug("--SPARQL QUERY: " + query.replace("\n", " "));

        URL url = getUrl(query);
        //LOG.trace(url);

        //FIXME Do some test with the returned results to see if there actually are results.
        List<DBpediaResource> uris = null;
        String response = null;
        try {
// uris = parse(readOutput(get(url)));
            response = request(url);
            uris = parse(response);
        } catch (JSONException e) {
            throw new OutputException(e+response);
        }
        LOG.debug(String.format("-- %s found.", uris.size()));
        return uris;
    }

    public String update(String query) throws SparqlExecutionException {
        String response = null;
        try {
            URL url = getUrl(query);
            response = request(url);
        } catch (Exception e) {
            throw new SparqlExecutionException(e);
        }
        return response;
    }

    public String request(URL url) throws SparqlExecutionException {
        HttpGet method = new HttpGet(url.toString());
        String response = null;

        // Provide custom retry handler is necessary
        method.getParams().setParameter("http.method.retry-handler", new DefaultHttpRequestRetryHandler(3, false));
        //method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpRequestRetryHandler(3, false));

        try {
            // Execute the method.
            HttpResponse httpResponse = client.execute(method);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            HttpEntity entity = httpResponse.getEntity();

            if (statusCode != HttpStatus.SC_OK || entity == null) {
                LOG.error("SparqlQuery failed: " + httpResponse.getStatusLine());
                throw new SparqlExecutionException(String.format("%s (%s). %s",
                        httpResponse.getStatusLine(),
                        method.getURI(),
                        EntityUtils.toString(entity)));
            }

            // Read the response body.
            byte[] responseBody = EntityUtils.toByteArray(entity);

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            response = new String(responseBody);

        //TODO: check this exception in the new API
        //} catch (HttpException e) {
        //    System.err.println("Fatal protocol violation: " + e.getMessage());
        //    throw new SparqlExecutionException(e);
        } catch (IOException e) {
            System.err.println("Fatal transport error: " + e.getMessage());
            throw new SparqlExecutionException(e);
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
        return response;

    }

    /**
     * Parses SPARQL+JSON output, getting a list of DBpedia URIs returned in *any* variable in the query.
     * Consider moving to a class on its own if we ever use this anywhere else in the code.
     *
     * @param jsonString string representation of SPARQL+JSON results
     * @return list of URIs as Strings contained in any variables in this result.
     * @throws org.json.JSONException
     */
    private static List<DBpediaResource> parse(String jsonString) throws JSONException {
        List<DBpediaResource> results = new ArrayList<DBpediaResource>();
        JSONObject root = new JSONObject(jsonString);
        JSONArray vars = root.getJSONObject("head").getJSONArray("vars");
        JSONArray bindings = root.getJSONObject("results").getJSONArray("bindings");

        for (int i = 0; i< bindings.length(); i++) {
            JSONObject row = bindings.getJSONObject(i);
            for (int v = 0; v < vars.length(); v++) {
                JSONObject typeValue = row.getJSONObject((String) vars.get(v));
                String uri = typeValue.getString("value").replace(SpotlightConfiguration.DEFAULT_NAMESPACE, "");
                results.add(new DBpediaResource(uri));
            }
        }

        return results;
    }


    public static void main(String[] args) throws Exception {

        String example = "SELECT ?resource ?label ?score WHERE {\n" +
                "?resource ?relation <http://dbpedia.org/resource/India> .\n" +
                "GRAPH ?g {\n" +
                "?resource <http://www.w3.org/2004/02/skos/core#altLabel> ?label.\n" +
                "}\n" +
                "?g <http://dbpedia.org/spotlight/score> ?score.\n" +
                "FILTER (REGEX(?label, \"woolworth\", \"i\"))\n" +
                "}";

        String example2 = "select distinct ?pol where {?pol a <http://dbpedia.org/ontology/Politician> }";

        String url = "http://dbpedia.org/sparql?default-graph-uri=http://dbpedia.org&query=select+distinct+%3Fpol+where+{%3Fpol+a+%3Chttp://dbpedia.org/ontology/Politician%3E+}&debug=on&timeout=&format=text/html&save=display&fname=";
        SparqlQueryExecuter e = new SparqlQueryExecuter("http://dbpedia.org", "http://dbpedia.org/sparql");

        List<DBpediaResource> uris = e.query(example2);
        System.out.println(uris);
    }

}