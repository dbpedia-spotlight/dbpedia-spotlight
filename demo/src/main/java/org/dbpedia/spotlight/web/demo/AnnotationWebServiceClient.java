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

package org.dbpedia.spotlight.web.demo;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;


/**
 * @author Andrés
 */
public class AnnotationWebServiceClient {
    private WebResource webResource;
    private Client client;

    //TODO make this configurable
    private static final String BASE_URI = "http://spotlight.dbpedia.org/rest/";


    public AnnotationWebServiceClient() {
        com.sun.jersey.api.client.config.ClientConfig config = new com.sun.jersey.api.client.config.DefaultClientConfig();
        client = Client.create(config);
        webResource = client.resource(BASE_URI).path("annotate");
    }

    public String getAnnotationHTML(String text, double conf, int support, String types) throws UniformInterfaceException {
        webResource = client.resource(BASE_URI).path("annotate");
        return getHTML(text, conf, support, types);
    }

    public String getDisambiguationHTML(String text, double conf, int support, String types) throws UniformInterfaceException {
        webResource = client.resource(BASE_URI).path("disambiguate");
        return getHTML(text, conf, support, types);
    }

    public String getHTML(String text, double conf, int support, String types) throws UniformInterfaceException {
        webResource=webResource.queryParam("text", text);
        webResource=webResource.queryParam("confidence", String.valueOf(conf));
        webResource=webResource.queryParam("support", String.valueOf(support));
        webResource=webResource.queryParam("types", String.valueOf(types));
        return webResource.accept(javax.ws.rs.core.MediaType.TEXT_HTML).get(String.class);
    }

    public void close() {
        client.destroy();
    }

    /*
    public String getAnnotationXML(String text, double conf, int support, String types, boolean coreferenceResolution) throws UniformInterfaceException {
        webResource = client.resource(BASE_URI).path("annotate");
        return getXML(text, conf, support, types, coreferenceResolution);
    }

    public String getDisambiguationXML(String text, double conf, int support, String types, boolean coreferenceResolution) throws UniformInterfaceException {
        webResource = client.resource(BASE_URI).path("disambiguate");
        return getXML(text, conf, support, types, coreferenceResolution);
    }

    public String getAnnotationJSON(String text, double conf, int support, String types, boolean coreferenceResolution) throws UniformInterfaceException {
        webResource = client.resource(BASE_URI).path("annotate");
        return getJSON(text, conf, support, types, coreferenceResolution);
    }

    public String getDisambiguationJSON(String text, double conf, int support, String types, boolean coreferenceResolution) throws UniformInterfaceException {
        webResource = client.resource(BASE_URI).path("disambiguate");
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
        //webResource=webResource.queryParam("coreferenceResolution", String.valueOf(coreferenceResolution));
        return webResource.accept(javax.ws.rs.core.MediaType.TEXT_XML).get(String.class);
    }

    public String getJSON(String text, double conf, int support, String types, boolean coreferenceResolution) throws UniformInterfaceException {
        webResource=webResource.queryParam("text", text);
        webResource=webResource.queryParam("confidence", String.valueOf(conf));
        webResource=webResource.queryParam("support", String.valueOf(support));
        webResource=webResource.queryParam("types", String.valueOf(types));
        //webResource=webResource.queryParam("coreferenceResolution", String.valueOf(coreferenceResolution));
        return webResource.accept(javax.ws.rs.core.MediaType.APPLICATION_JSON).get(String.class);
    }
    */

}