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

package org.dbpedia.spotlight.evaluation.external;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.exceptions.AnnotationException;
import org.dbpedia.spotlight.exceptions.AuthenticationException;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.Text;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * The External Clients were translated to Scala but this class was not.
 * Because the Ontos Semantic API site (http://wdm.cs.waikato.ac.nz/services) is broken and
 * the contact page (http://www.ontos.com/semantic-api/getting-started/index.php?page_id=596), for request api key, is unavailable.
 * As result of that, there is no api key to test and/or use the client.
 *
 * Last Tested: 08/27th/2013 by Alexandre Cançado Cardoso
 */

/**
 * Annotates text via Ontos API
 *  
 * Notes about the Ontos API:
 *  - Only maps to DBpedia for Person and Company. All other annotations would be misses to the evaluation, since their URIs are proprietary (hash-style).
 *    For those entities with no mappings to DBpedia we use a 'URIzed' label (commonly works for DBpedia). That increases their hits.
 *  - Fantastic support from the dev group
 *  - Returns triples
 *  - Requires encoding
 *  - Undescriptive error messages.
 *
 * @author PabloMendes
 * Some parts (indicated) are adapted from example code provided by 
 * @author Alex Klebeck, Ontos AG, (C)2010
 */

public class OntosClient extends AnnotationClient {

    //public static final String dbpediaPrefix = "http://dbpedia.org/resource/";

    public Log LOG = LogFactory.getLog(this.getClass());

    // Create an instance of HttpClient.
    HttpClient client = new HttpClient();
    boolean authenticated = false;
    String authToken;

    public OntosClient(String user, String password) throws AuthenticationException {
        this.authenticated = authenticate(user,password);
    }

    private boolean authenticate(String user, String password) {
                    // Create a method instance.
        GetMethod method = new GetMethod("http://news.ontos.com/token?j_username="+user+"&j_password="+password);
//        method.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
        try {
            this.authToken = request(method);
        } catch (AnnotationException e) {
            LOG.error("Could not execute authentication request.", e);
            e.printStackTrace();
        }
        return this.authToken != null; //TODO test this.
    }

    private String clean(Text text) {
        return text.text().replaceAll("\\s+"," ").replaceAll("[\'\"]","");
    }
    
    public String annotate(Text text) throws AnnotationException {
        if (!authenticated) {
            throw new AuthenticationException("Client is not authenticated with Ontos. Please call authenticate(user, password) first.");
        }
        String url = "http://news.ontos.com/api/miner;jsessionid="+this.authToken+"?query="+
                URLEncoder.encode(
                "{" +
                "\"get\":\"process\"," +
                "\"ontology\":\"common.english\"," +
                "\"format\":\"NTRIPLES\"," +
                "\"text\":\""+clean(text)+"\""+                "}");
        GetMethod method = new GetMethod(url);
        String response = request(method);
        
        return response;
    }

    public List<DBpediaResource> extract(Text text) throws AnnotationException {
        if (!authenticated) {
            throw new AuthenticationException("Client is not authenticated with Ontos. Please call authenticate(user, password) first.");
        }
        String url = "http://news.ontos.com/api/miner;jsessionid="+this.authToken+"?query="+
                URLEncoder.encode(
                "{" +
                "\"get\":\"process\"," +
                "\"ontology\":\"common.english\"," +
                "\"format\":\"NTRIPLES\"," +
                "\"text\":\""+clean(text)+"\"" +
                "}");
        JSONObject json = request(url);
        Vector<String> entities = getEntities(json);
        List<DBpediaResource> dbpediaEntities = dereference(entities);        
        return dbpediaEntities;
    }

    /**
     * Adapted from:
     * @author Alex Klebeck, Ontos AG, (C)2010
     * @param response
     * @return
     * @throws AnnotationException
     */
    protected final String ONTOS_COMMON_ENGLISH_TYPE_INSTANCEOF = "http://sofa.semanticweb.org/sofa/v1.0/system#__INSTANCEOF_REL";
    protected final String ONTOS_COMMON_ENGLISH_TYPE_PERSON = "http://www.ontosearch.com/2008/02/ontosminer-ns/domain/common/english#Person";
    protected final String ONTOS_COMMON_ENGLISH_TYPE_ORGANIZATION = "http://www.ontosearch.com/2008/02/ontosminer-ns/domain/common/english#Organization";
    protected final String ONTOS_COMMON_ENGLISH_SAMEAS = "http://www.ontosearch.com/2008/02/ontosminer-ns/domain/common/english/dbpedia#sameAs";
    protected final String ONTOS_ANNOTATION = "http://www.ontosearch.com/2007/12/spotlight-ns#spotlight";
    protected final String ONTOS_URL_ONTOLOGY = "http://news.ontos.com/api/ontology;jsessionid=%1$s?query=";
    protected final String ONTOS_QUERY_ENTITY = "{'get':'ent'," + "'uri':'%1$s'}";

    private Vector<String> getEntities(JSONObject json) throws AnnotationException {
        JSONArray triples = new JSONArray();
        try {
            triples = json.getJSONArray("result");
        } catch (JSONException e) {
            LOG.error(e.getMessage());
        }

        Vector<String> entities = new Vector<String>();
		for (int i = 0; i < triples.length(); i++) {
			try {
				String subject = triples.getJSONObject(i).getString("s");
				String predicate = triples.getJSONObject(i).getString("p");
				String object = triples.getJSONObject(i).getString("o");

                //As planned by Pablo
//				if ((ONTOS_COMMON_ENGLISH_TYPE_INSTANCEOF).equals(predicate)) {
//                    if (!ONTOS_ANNOTATION.equals(object))
//                        entities.add(subject);
//                }
                
                // As advised by Alex
                if ( ((ONTOS_COMMON_ENGLISH_TYPE_INSTANCEOF).equals(predicate) && (ONTOS_COMMON_ENGLISH_TYPE_PERSON).equals(object)) ||
                        ((ONTOS_COMMON_ENGLISH_TYPE_INSTANCEOF).equals(predicate) && (ONTOS_COMMON_ENGLISH_TYPE_ORGANIZATION).equals(object)) ) {
                    entities.add(subject);
                }

			} catch (JSONException e) {
				LOG.error(e.getMessage());
			}
		}
		return entities;
    }

    public List<DBpediaResource> dereference(List<String> entities) throws AnnotationException {
        List<DBpediaResource> resources = new ArrayList<DBpediaResource>();
        for (String eid: entities) {
            resources.add(dereference(eid));
        }
        return resources;
    }
   /**
	 * lookups the entity data by sending a request to the ONTOS Service
	 *
	 * @param eid
	 * @return JSONObject with detailed entity data
    *
    * Parts adapted from:
    * @author Alex Klebeck, Ontos AG, (C)2010
    */
   public DBpediaResource dereference(String eid) throws AnnotationException {
       JSONObject entity = requestDereference(eid);

       List<String> sameAs = new ArrayList<String>();
       // This will be the result if we can't get anything better
       DBpediaResource dbpediaSameAs = new DBpediaResource(eid);
       try {
           String label = entity.getString("label");
           // This will be the result if there is no proper link to DBpedia
           if (label!=null)
            dbpediaSameAs = new DBpediaResource(label);
           // Now we try to get the proper link
           JSONObject props = entity.getJSONObject("props");
           JSONArray sameAsIds = props.getJSONArray(ONTOS_COMMON_ENGLISH_SAMEAS);
           for (int i = 0; i < sameAsIds.length(); i++) {
               String uri = sameAsIds.getString(i);
               sameAs.add(uri);
               // If there is a proper link, then we add it.
               if (uri.startsWith(SpotlightConfiguration.DEFAULT_NAMESPACE))
                dbpediaSameAs = new DBpediaResource(uri.replace(SpotlightConfiguration.DEFAULT_NAMESPACE, ""));
           }
       } catch (JSONException e) {
           LOG.error(e.getMessage());
       }

       return dbpediaSameAs;
   }

    private JSONObject requestDereference(String eid) throws AnnotationException {
        String path = String.format(ONTOS_URL_ONTOLOGY, this.authToken);
        String query = String.format(ONTOS_QUERY_ENTITY, eid);
        try {
            query = URLEncoder.encode(query, "utf-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error(e.getMessage());
            throw new AnnotationException(e);
        }
        JSONObject json = request(path+query);
        JSONObject entity = new JSONObject();
        try {
             entity = json.getJSONObject("result");
        } catch (JSONException e) {
            LOG.error(e.getMessage());
        }
         return entity;
    }

    /*
     * Adapted from:
     * @author Alex Klebeck, Ontos AG, (C)2010
	 */
    private JSONObject request(String url) throws AnnotationException {
       GetMethod method = new GetMethod(url);
       String response = request(method);
       JSONObject json = new JSONObject();
       try {
           json = new JSONObject(response);
           if (!(json.getBoolean("success")))
               throw new AnnotationException("Could not execute dereference query to Ontos. "+url);
       } catch (JSONException e) {
           LOG.error(e.getMessage());
       }
        return json;
    }

    
    public static void main(String[] args) {
        String username = args[0];
        String password = args[1];


        //String baseDir = "/home/pablo/eval/cucerzan/";

//        File outputFile = new File(baseDir+"AnnotationText-Ontos.txt.list");
//        File inputFile = new File(baseDir+"AnnotationText.txt");

//        File inputFile = new File("/home/pablo/eval/cucerzan/cucerzan.txt");
//        File outputFile = new File("/home/pablo/eval/cucerzan/systems/cucerzan-Ontos.txt");

        File inputFile = new File("/home/pablo/eval/manual/AnnotationText.txt");
        File outputFile = new File("/home/pablo/eval/manual/Ontos.txt");

        try {
            OntosClient client = new OntosClient(username,password);
            client.evaluate(inputFile, outputFile);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }
}
