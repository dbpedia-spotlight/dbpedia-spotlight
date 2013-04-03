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
import org.apache.commons.httpclient.methods.PostMethod;
import org.dbpedia.spotlight.exceptions.AnnotationException;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.string.XmlParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Easiest to use.
 * Required a post, and specific header, but good documentation.
 * Simple parameters, simple response output.
 * No positions.
 * 
 * @author pablomendes
 */
public class AlchemyClient extends AnnotationClient {    

    private static String url ="http://access.alchemyapi.com/calls/text/TextGetRankedNamedEntities";

    String apikey;

    public AlchemyClient(String apikey) {
        this.apikey = apikey;
    }

    /*
    HTTP POST
    Content-Type header: application/x-www-form-urlencoded
    outputMode=json
     */
    protected String process(String text) throws AnnotationException {
        PostMethod method = new PostMethod(url);
        method.setRequestHeader("Content-type","application/x-www-form-urlencoded");
        NameValuePair[] params = {new NameValuePair("text",text), new NameValuePair("apikey",this.apikey)};
        method.setRequestBody(params);
        String response = request(method);
        //System.out.println(response);
        return response;
    }

    public List<DBpediaResource> extract(Text text) throws AnnotationException {
        List<DBpediaResource> entities = new ArrayList<DBpediaResource>();
        String response = process(text.text());
        Element root = null;
        try {
            root = XmlParser.parse(response);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SAXException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ParserConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        NodeList list = XmlParser.getNodes("/results/entities/entity", root);

        for(int i=0; i<list.getLength(); i++) {
            NodeList attributes = list.item(i).getChildNodes();
            //System.out.println(list.item(i).getAttributes());
            for(int j=0; j<attributes.getLength(); j++) {
                Node n = attributes.item(j);            
                String name = n.getNodeName();
                String value = "";
                if (n.getNodeType()!=Node.TEXT_NODE) {
                    value = n.getFirstChild().getNodeValue();
                    LOG.trace(String.format("Name:%s, Value: %s",name,value));
                }
                if (name.equals("text")) {
                    entities.add(new DBpediaResource(value)); //TODO could have actually gotten DBpediaResourceOccurrences and set the relevance
                }
            }
        }
        LOG.debug(String.format("Extracted: %s",entities));
        return entities;
    }

    public static void main(String[] args) throws Exception {

        String apikey = args[0];
        String baseDir = "C:\\cygwin\\home\\PabloMendes\\DBpediaSpotlight\\";

        AlchemyClient client = new AlchemyClient(apikey);

        File manualEvalOutput = new File("/home/pablo/eval/manual/systems/AnnotationText-Alchemy.txt.list");
        File manualEvalInput = new File("/home/pablo/eval/manual/AnnotationText.txt");
        //client.evaluate(manualEvalInput, manualEvalOutput);

        File cucerzanEvalInput = new File("/home/pablo/eval/cucerzan/cucerzan.txt");
        File cucerzanEvalOutput = new File("/home/pablo/eval/cucerzan/systems/cucerzan-Alchemy2.set");

        File input = new File("/home/pablo/eval/csaw/gold/paragraphs.txt");
        File output = new File("/home/pablo/eval/csaw/systems/Alchemy.list");

        client.evaluate(input, output);
    }

}