/**
 * Copyright 2011 Andrés García-Silva, Max Jakob
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbpedia.spotlight.web.client;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;


/** Jersey REST client generated for REST resource:Annotation [Annotate]<br>
 *  USAGE:<pre>
 *        AnnotationWebServiceClient client = new AnnotationWebServiceClient();
 *        Object response = client.XXX(...);
 *        // do whatever with response
 *        client.close();
 *  </pre>
 * @author Andrés
 */
public class AnnotationWebServiceClient {
    private WebResource webResource;
    private Client client;
    private static final String BASE_URI = "http://160.45.137.71:9090/SpotlightWebService";

    public AnnotationWebServiceClient() {
        com.sun.jersey.api.client.config.ClientConfig config = new com.sun.jersey.api.client.config.DefaultClientConfig();
        client = Client.create(config);
        webResource = client.resource(BASE_URI).path("Annotate");
        //webResource = webResource.queryParam("text", "Barak Obama.");
    }

    public String getAnnotationXML(String text, double conf, int support, String types, boolean coreferenceResolution) throws UniformInterfaceException {
        webResource = client.resource(BASE_URI).path("Annotate");
        return getXML(text, conf, support, types, coreferenceResolution);
    }

    public String getDisambiguationXML(String text, double conf, int support, String types, boolean coreferenceResolution) throws UniformInterfaceException {
        webResource = client.resource(BASE_URI).path("Disambiguate");
        return getXML(text, conf, support, types, coreferenceResolution);
    }
    
    public String getAnnotationJSON(String text, double conf, int support, String types, boolean coreferenceResolution) throws UniformInterfaceException {
        webResource = client.resource(BASE_URI).path("Annotate");
        return getJSON(text, conf, support, types, coreferenceResolution);
    }

    public String getDisambiguationJSON(String text, double conf, int support, String types, boolean coreferenceResolution) throws UniformInterfaceException {
        webResource = client.resource(BASE_URI).path("Disambiguate");
        return getJSON(text, conf, support, types, coreferenceResolution);
    }


    public void putJSON(Object requestEntity) throws UniformInterfaceException {
        webResource.type(javax.ws.rs.core.MediaType.APPLICATION_JSON).put(requestEntity);
    }

    public String getXML(String text, double conf, int support, String types, boolean coreferenceResolution) throws UniformInterfaceException {
        webResource=webResource.queryParam("text", text);
        webResource=webResource.queryParam("confidence", String.valueOf(conf));
        webResource=webResource.queryParam("support", String.valueOf(support));
        webResource=webResource.queryParam("types", String.valueOf(types));
        webResource=webResource.queryParam("coreferenceResolution", String.valueOf(coreferenceResolution));
        return webResource.accept(javax.ws.rs.core.MediaType.TEXT_XML).get(String.class);
    }

    public String getJSON(String text, double conf, int support, String types, boolean coreferenceResolution) throws UniformInterfaceException {
        webResource=webResource.queryParam("text", text);
        webResource=webResource.queryParam("confidence", String.valueOf(conf));
        webResource=webResource.queryParam("support", String.valueOf(support));
        webResource=webResource.queryParam("types", String.valueOf(types));
        webResource=webResource.queryParam("coreferenceResolution", String.valueOf(coreferenceResolution));
        return webResource.accept(javax.ws.rs.core.MediaType.APPLICATION_JSON).get(String.class);
    }

    public void close() {
        client.destroy();
    }

    public static void main(String [ ] args)throws Exception{
        AnnotationWebServiceClient client = new AnnotationWebServiceClient();
        //Object response = client.getXML("Barak Obama.");
        client.close();
    }

}
