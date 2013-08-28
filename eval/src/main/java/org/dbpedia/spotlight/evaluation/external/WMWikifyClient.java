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

import org.apache.commons.httpclient.methods.GetMethod;
import org.dbpedia.spotlight.exceptions.AnnotationException;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.string.XmlParser;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * The External Clients were translated to Scala but this class was not.
 * Because the Wikipedia Miner service (http://wdm.cs.waikato.ac.nz/services) and contact page are unavailable.
 * As result of that, this client is no more working.
 *
 * Last Tested: 08/27th/2013 by Alexandre Cançado Cardoso
 */

/**
 * Distributed under GNU
 * 
 * @author pablomendes
 */

public class WMWikifyClient extends AnnotationClient {

    //TODO minProbability:0.3 &minProbability=0.8
    //repeatMode: 0=mark all, 1=mark first, 2=mark first in each section
    private static String url_pattern ="http://wdm.cs.waikato.ac.nz:8080/service?task=wikify&xml&repeatMode=0&source=%s";

    public WMWikifyClient() {    }

    /*
    HTTP POST
    Content-Type header: application/x-www-form-urlencoded
    outputMode=json
     */
    protected String process(String text) throws AnnotationException {
        String url = "";
        try {
            url = String.format(url_pattern, URLEncoder.encode(text, "UTF8"));
        } catch (UnsupportedEncodingException e) {
            throw new AnnotationException(e);
        }
        GetMethod method = new GetMethod(url);
        String response = request(method);
        //System.out.println(response);
        return response;
    }

    /**
     *
<?xml version="1.0" encoding="UTF-8"?>
<WikipediaMinerResponse server_path="http://wdm.cs.waikato.ac.nz:8080/" service_name="service">
   <WikifierResponse bannedTopics="" minProbability="0.500000" repeatMode="2" sourceMode="0">
      <Source>Wikipedia is a free, multilingual encyclopedia project supported by the non-profit Wikimedia Foundation.</Source>
      <Result documentScore="2.358837" outputMode="3">[[Wikipedia]] is a free, [[Multilingualism|multilingual]] [[encyclopedia]] project supported by the [[Non-profit organization|non-profit]] [[Wikimedia Foundation]].</Result>
      <DetectedTopicList>
         <DetectedTopic id="18618509" title="Wikimedia Foundation" weight="0.955502"/>
         <DetectedTopic id="5043734" title="Wikipedia" weight="0.844628"/>
         <DetectedTopic id="72487" title="Non-profit organization" weight="0.753518"/>
         <DetectedTopic id="9253" title="Encyclopedia" weight="0.726951"/>
         <DetectedTopic id="1570983" title="Multilingualism" weight="0.652405"/>
         <DetectedTopic id="693306" title="Foundation (nonprofit organization)" weight="0.561188"/>
      </DetectedTopicList>
   </WikifierResponse>
</WikipediaMinerResponse>
     * @param text
     * @return
     */
    public List<DBpediaResource> extract(Text text) throws AnnotationException {
        List<DBpediaResource> entities = new ArrayList<DBpediaResource>();
        String response = process(text.text());
        //LOG.trace(response);
        Element root = null;
        try {
            root = XmlParser.parse(response);
        } catch (Exception e) { //IOException, ParseException, ParserConfigurationException
            throw new AnnotationException("Could not parse XML response.",e);
        }
        NodeList list = XmlParser.getNodes("//WikipediaMinerResponse/WikifierResponse/DetectedTopicList/DetectedTopic", root);

        for(int i=0; i<list.getLength(); i++) {
            Node node = list.item(i);
            NamedNodeMap attributes = node.getAttributes();
            for(int j=0; j<attributes.getLength(); j++) {
                Node n = attributes.item(j);
                String name = n.getNodeName();
                //LOG.trace(String.format("Name:%s", name));
                String value = "";
                if (n.getNodeType()!=Node.TEXT_NODE) {
                    value = n.getFirstChild().getNodeValue();
                    //LOG.trace(String.format("Name:%s, Value: %s", name, value));
                }
                if (name.equals("title")) {
                    entities.add(new DBpediaResource(value)); //TODO could have actually gotten DBpediaResourceOccurrences and set the relevance from weight param
                }
            }
        }
        LOG.debug(String.format("Extracted: %s",entities));
        return entities;
    }

    public static void main(String[] args) throws Exception {

        WMWikifyClient client = new WMWikifyClient();

        File manualEvalOutput = new File("/home/pablo/eval/manual/systems/WMWikify.list");
        File manualEvalInput = new File("/home/pablo/eval/manual/AnnotationText.txt");
        client.evaluate(manualEvalInput, manualEvalOutput);

        File cucerzanEvalInput = new File("/home/pablo/eval/cucerzan/cucerzan.txt");
        File cucerzanEvalOutput = new File("/home/pablo/eval/cucerzan/systems/WMWikify.list");
//        client.evaluate(cucerzanEvalInput, cucerzanEvalOutput);

        File wikifyEvalInput = new File("/home/pablo/eval/wikify/gold/WikifyAllInOne.txt");
        File wikifyEvalOutput = new File("/home/pablo/eval/wikify/systems/WMWikify.list");
        //client.evaluate(wikifyEvalInput, wikifyEvalOutput);

        File CSAWEvalInput = new File("/home/pablo/eval/csaw/gold/paragraphs.txt");
        File CSAWEvalOutput = new File("/home/pablo/eval/csaw/systems/WMWikify.list");
        client.evaluate(CSAWEvalInput, CSAWEvalOutput);
    }

}