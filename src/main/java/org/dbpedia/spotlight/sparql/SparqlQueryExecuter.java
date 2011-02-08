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

package org.dbpedia.spotlight.sparql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;

import org.apache.log4j.Logger;
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


    //http://dbpedia.org/sparql?default-graph-uri=http://dbpedia.org&query=select+distinct+%3Fpol+where+{%3Fpol+a+%3Chttp://dbpedia.org/ontology/Politician%3E+}&debug=on&timeout=&format=text/html&save=display&fname=

    String mainGraph = "http://dbpedia.org";
    String sparqlUrl = "http://dbpedia.org/sparql";

	public SparqlQueryExecuter() {}

    public SparqlQueryExecuter(String mainGraph, String sparqlUrl) {
        this.mainGraph = mainGraph;
        this.sparqlUrl = sparqlUrl;
    }

	public Set<String> query(String query) throws IOException{
		LOG.info("--SPARQL QUERY: "+query);

		String graphEncoded = URLEncoder.encode(mainGraph, "UTF-8");
		String formatEncoded = URLEncoder.encode("application/sparql-results+json", "UTF-8");
        String queryEncoded = URLEncoder.encode(query, "UTF-8");

        String url = "default-graph-uri="+graphEncoded+"&query="+queryEncoded+"&format="+formatEncoded+"&debug=on&timeout=";
		LOG.debug(url);
		//FIXME Do some test with the returned results to see if there actually are results.
        Set<String> uris = null;
        try {
            uris = parse(readOutput(get(url)));
        } catch (JSONException e) {
            throw new IOException(e);
        }
        return uris;
	}

	public URLConnection get(String sparqlCommand) throws IOException {
		URL queryURL = new URL(sparqlUrl+"?"+sparqlCommand);
		URLConnection urlConn = queryURL.openConnection();
		((HttpURLConnection) urlConn).setRequestMethod("GET");
		urlConn.setDoOutput(true);
		urlConn.setDoInput(true);
		urlConn.setUseCaches(false);

		OutputStream oStream = urlConn.getOutputStream();

		oStream.write(sparqlCommand.getBytes());
		oStream.flush();
		oStream.close();

		return urlConn;
	}

    /**
     * Parses SPARQL+JSON output, getting a list of DBpedia URIs returned in *any* variable in the query.
     * Consider moving to a class on its own if we ever use this anywhere else in the code.
     *
     * @param jsonString string representation of SPARQL+JSON results
     * @return list of URIs as Strings contained in any variables in this result.
     * @throws org.json.JSONException
     */
    private static Set<String> parse(String jsonString) throws JSONException {
        Set<String> results = new HashSet<String>();
        JSONObject root = new JSONObject(jsonString);
        JSONArray vars = root.getJSONObject("head").getJSONArray("vars");
        JSONArray bindings = root.getJSONObject("results").getJSONArray("bindings");

        for (int i = 0; i< bindings.length(); i++) {
            JSONObject row = bindings.getJSONObject(i);
            for (int v = 0; v < vars.length(); v++) {
                JSONObject typeValue = row.getJSONObject((String) vars.get(v));
                String uri = typeValue.getString("value").replace("http://dbpedia.org/resource/", "");
                results.add(uri);
            }
        }

        return results;
    }

	//TODO PABLO there is probably a better way to do this.
	private static String readOutput(URLConnection urlConn) throws IOException{
		BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
		StringBuilder response = new StringBuilder(); String aLine;
		while ((aLine = in.readLine()) != null) {
			response.append(aLine);
		}
		return response.toString();
	}

    public static void main(String[] args) throws IOException, JSONException {

        String example = "SELECT ?resource ?label ?score WHERE {\n" +
                "?resource ?relation <http://dbpedia.org/resource/India> .\n" +
                "GRAPH ?g {\n" +
                "?resource <http://www.w3.org/2004/02/skos/core#altLabel> ?label.\n" +
                "}\n" +
                "?g  <http://dbpedia.org/spotlight/score>  ?score.\n" +
                "FILTER (REGEX(?label, \"woolworth\", \"i\"))\n" +
                "}";

        String example2 = "select distinct ?pol where {?pol a <http://dbpedia.org/ontology/Politician> }";

        String url = "http://dbpedia.org/sparql?default-graph-uri=http://dbpedia.org&query=select+distinct+%3Fpol+where+{%3Fpol+a+%3Chttp://dbpedia.org/ontology/Politician%3E+}&debug=on&timeout=&format=text/html&save=display&fname=";
        SparqlQueryExecuter e = new SparqlQueryExecuter();

        Set<String> uris = e.query(example2);
        System.out.println(uris);
    }

}
