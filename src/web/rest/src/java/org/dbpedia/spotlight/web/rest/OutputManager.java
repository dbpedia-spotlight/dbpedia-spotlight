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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbpedia.spotlight.web.rest;

import java.io.*;
// SAX classes.
import java.util.List;
import org.xml.sax.helpers.*;
//JAXP 1.1
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.sax.*;

//JSON classes
import net.sf.json.JSON;
import net.sf.json.xml.XMLSerializer;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;


/**
 *
 * @author Andrés
 */
public class OutputManager {

    private TransformerHandler initXMLDoc(ByteArrayOutputStream out) throws Exception{
        StreamResult streamResult = new StreamResult(out);
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        // SAX2.0 ContentHandler.
        TransformerHandler hd = tf.newTransformerHandler();
        Transformer serializer = hd.getTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING,"utf-8");
        //serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,"users.dtd");
        serializer.setOutputProperty(OutputKeys.INDENT,"yes");
        hd.setResult(streamResult);
        hd.startDocument();
        return hd;
    }

    private String getText(String t, List<DBpediaResourceOccurrence> occList) {
        if(occList == null || occList.isEmpty()) {
            return t.replaceAll("\\[\\[(.*?)\\]\\]", "$1");
        }
        else {
            return occList.get(0).context().text();
        }
    }

    protected String makeXML(String text, List<DBpediaResourceOccurrence> occList, double confidence, int support, String targetTypesString, String sparqlQuery, String policy, boolean coreferenceResolution) throws Exception{
        // PrintWriter from a Servlet
        String xml = "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TransformerHandler hd = initXMLDoc(out);

        text = getText(text, occList);

        //Create Annotation element
        //First create text attribute
        AttributesImpl atts = new AttributesImpl();

        atts.addAttribute("","","text","CDATA",text);
        atts.addAttribute("","","confidence","CDATA",String.valueOf(confidence));
        atts.addAttribute("","","support","CDATA",String.valueOf(support));
        atts.addAttribute("","","types","CDATA",targetTypesString);
        atts.addAttribute("","","sparql","CDATA",sparqlQuery);
        atts.addAttribute("","","policy","CDATA",policy);
        //atts.addAttribute("","","coreferenceResolution","CDATA",String.valueOf(coreferenceResolution));
        hd.startElement("","","Annotation",atts);
        int i=0;
        String dbpediaPrefix = "http://dbpedia.org/resource/";
        for (DBpediaResourceOccurrence occ : occList){
          if (i==0){
              atts.clear();
              hd.startElement("","","Resources",atts);
          }

          atts.addAttribute("","","URI","CDATA",dbpediaPrefix+occ.resource().uri());
          atts.addAttribute("","","support","CDATA",String.valueOf(occ.resource().support()));
          atts.addAttribute("","","types","CDATA",occ.resource().types().mkString(","));
          // support and types should go to resource

          atts.addAttribute("", "", "surfaceForm", "CDATA", occ.surfaceForm().name());
          atts.addAttribute("","","offset","CDATA",String.valueOf(occ.textOffset()));
          atts.addAttribute("", "", "similarityScore", "CDATA", String.valueOf(occ.similarityScore()));
          atts.addAttribute("","","percentageOfSecondRank","CDATA",String.valueOf(occ.percentageOfSecondRank()));
          
          hd.startElement("","","Resource",atts);
          hd.endElement("","","Resource");
          i++;
        }
        if (i>0)
          hd.endElement("","","Resources");

        hd.endElement("","","Annotation");
        hd.endDocument();
        xml = out.toString("utf-8");
        System.out.println(xml);
        return xml;
    }

    protected String makeErrorXML(String message, String text, double confidence, int support, String targetTypesString, String sparqlQuery, String policy, boolean coreferenceResolution) throws Exception{
        // PrintWriter from a Servlet
        String xmlDoc="";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TransformerHandler hd = initXMLDoc(out);

        text = getText(text, null);

        //Create Annotation element
        //First create text attribute
        AttributesImpl atts = new AttributesImpl();

        atts.addAttribute("","","text","CDATA",text);
        atts.addAttribute("","","confidence","CDATA",String.valueOf(confidence));
        atts.addAttribute("","","support","CDATA",String.valueOf(support));
        atts.addAttribute("","","types","CDATA",targetTypesString);
        //atts.addAttribute("","","coreferenceResolution","CDATA",String.valueOf(coreferenceResolution));
        atts.addAttribute("","","sparql","CDATA",sparqlQuery);
        atts.addAttribute("","","policy","CDATA",policy);
        hd.startElement("","","Annotation",atts);

        atts.clear();
        atts.addAttribute("","","message","CDATA",message);
        hd.startElement("","","Error",atts);
        hd.endElement("","","Error");

        hd.endElement("","","Annotation");
        hd.endDocument();
        xmlDoc = out.toString("utf-8");
        System.out.println(xmlDoc);
        return xmlDoc;
    }

    protected String xml2json(String xmlDoc) throws Exception{
        XMLSerializer xmlSerializer = new XMLSerializer();
        String json = xmlSerializer.read(xmlDoc).toString(2);
        System.out.println(json);
        return json;
    }


    private final String htmlTemplate = "<html>\n<body>\n%s\n</body>\n</html>";

    private final String htmlLinkTemplate = "<a href=\"%s\" title=\"%s\" target=\"_blank\">%s</a>";

    protected String makeHTML(String text, List<DBpediaResourceOccurrence> occList) {
        text = getText(text, occList);

        if(occList.isEmpty()) {
            return String.format(htmlTemplate, text);
        }

        int lengthAdded = 0;
        String modifiedText = text;
        String startText;
        for (DBpediaResourceOccurrence occ : occList){
            int endOfSurfaceform = occ.textOffset() + lengthAdded + occ.surfaceForm().name().length();
            startText = modifiedText.substring(0, occ.textOffset() + lengthAdded);
            String annotationAdd = String.format(htmlLinkTemplate, occ.resource().getFullUri(), occ.resource().getFullUri(), occ.surfaceForm().name());
            modifiedText = startText + annotationAdd + modifiedText.substring(endOfSurfaceform);
            lengthAdded = lengthAdded + (annotationAdd.length()-occ.surfaceForm().name().length());
        }
        return String.format(htmlTemplate, modifiedText.replaceAll("\\n", "<br/>"));
    }



}
