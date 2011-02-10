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

import org.dbpedia.spotlight.model.DBpediaType;
import org.xml.sax.helpers.*;
//JAXP 1.1
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.sax.*;

//JSON classes
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
        return xmlDoc;
    }

    private XMLSerializer xmlSerializer = new XMLSerializer();
    protected String xml2json(String xmlDoc) throws Exception{
        return xmlSerializer.read(xmlDoc).toString(2);
    }

    private WebCodeFormatter htmlFormat = new HTMLFormatter();
    protected String makeHTML(String text, List<DBpediaResourceOccurrence> occList) {
        return makeWebRepresentation(text, occList, htmlFormat);
    }

    private WebCodeFormatter rdfaFormat = new RDFaFormatter();
    protected String makeRDFa(String text, List<DBpediaResourceOccurrence> occList) {
        return makeWebRepresentation(text, occList, rdfaFormat);
    }


    private String makeWebRepresentation(String text, List<DBpediaResourceOccurrence> occList, WebCodeFormatter formatter) {
        text = getText(text, occList);

        if(occList.isEmpty()) {
            return formatter.getMain(text);
        }
        int lengthAdded = 0;
        String modifiedText = text;
        String startText;
        for (DBpediaResourceOccurrence occ : occList){
            int endOfSurfaceform = occ.textOffset() + lengthAdded + occ.surfaceForm().name().length();
            startText = modifiedText.substring(0, occ.textOffset() + lengthAdded);
            String fullUri = occ.resource().getFullUri();
            String annotationAdd = formatter.getLink(fullUri, occ.surfaceForm().name(), occ.resource().getTypes());
            modifiedText = startText + annotationAdd + modifiedText.substring(endOfSurfaceform);
            lengthAdded = lengthAdded + (annotationAdd.length()-occ.surfaceForm().name().length());
        }
        return formatter.getMain(modifiedText.replaceAll("\\n", "<br/>"));
    }

    private interface WebCodeFormatter {
        // surrounds the marked-up text with main tags
        public String getMain(String content);

        // produces an HTML link, potentially with semantic markup
        public String getLink(String uri, String surfaceForm, List<DBpediaType> types);
    }

    private class HTMLFormatter implements WebCodeFormatter {
        private final static String main = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n<html>\n<head>\n<title>DBpedia Spotlight annotation</title>\n<meta http-equiv=\"Content-type\" content=\"text/html;charset=UTF-8\">\n</head>\n<body>\n<div>\n%s\n</div>\n</body>\n</html>";
        private final static String link = "<a href=\"%s\" title=\"%s\" target=\"_blank\">%s</a>";

        public String getLink(String uri, String surfaceForm, List<DBpediaType> types) {
            return String.format(link, uri, uri, surfaceForm);
        }

        public String getMain(String content) {
            return String.format(main, content.replaceAll("\\n", "<br/>"));
        }
    }

    private class RDFaFormatter implements WebCodeFormatter {
        /**
         * <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML+RDFa 1.0//EN" "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
	xmlns:foaf="http://xmlns.com/foaf/0.1/"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:vcard="http://www.w3.org/2006/03/hcard/"
        xmlns:lexvo="http://lexvo.org/ontology#" xmlns:dbpedia="http://dbpedia.org/resource/" xmlns:dbpo="http://dbpedia.org/ontology/"
>
<head>
<title>DBpedia Spotlight annotation</title>
</head>
<body>
<div>
<a about="http://dbpedia.org/resource/Barack_Obama" instanceof="http://dbpedia.org/ontology/President" href="http://dbpedia.org/resource/Barack_Obama" title="http://dbpedia.org/resource/Barack_Obama" property="lexvo:label">President Obama</a> called Wednesday on <a about="http://dbpedia.org/resource/United_States_Congress" typeof="http://dbpedia.org/ontology/Legislature" href="http://dbpedia.org/resource/United_States_Congress" title="http://dbpedia.org/resource/United_States_Congress">Congress</a> to extend a <a about="http://dbpedia.org/resource/Tax_break" href="http://dbpedia.org/resource/Tax_break" title="http://dbpedia.org/resource/Tax_break" target="_blank">tax break</a> for <a about="http://dbpedia.org/resource/Student" href="http://dbpedia.org/resource/Student" title="http://dbpedia.org/resource/Student" target="_blank">students</a> included in last year's economic stimulus package, arguing that the <a about="http://dbpedia.org/resource/Policy" href="http://dbpedia.org/resource/Policy" title="http://dbpedia.org/resource/Policy" target="_blank">policy</a> provides more generous assistance.
</div>
</body>
</html>
         */
        private final static String main = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML+RDFa 1.0//EN\" \"http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd\">\n<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:lexvo=\"http://lexvo.org/ontology#\" xmlns:dbpedia=\"http://dbpedia.org/resource/\" xmlns:dbpo=\"http://dbpedia.org/ontology/\">\n<head>\n<title>DBpedia Spotlight annotation</title>\n</head>\n<body>\n<div rel=\"rdfs:seeAlso\">\n%s\n</div>\n</body>\n</html>";
        private final static String link = "<a about=\"%s\" href=\"%s\" title=\"%s\" target=\"_blank\" property=\"lexvo:label\">%s</a>";
        private final static String typeLink= "<a about=\"%s\" typeof=\"%s\" href=\"%s\" title=\"%s\" property=\"lexvo:label\">%s</a>";

        public String getLink(String uri, String surfaceForm, List<DBpediaType> types) {
            if(types == null || types.isEmpty()) {
                return String.format(link, uri, uri, uri, surfaceForm);
            }
            else {
                String mostSpecificType = types.get(types.size()-1).getFullUri();
                return String.format(typeLink, uri, mostSpecificType, uri, uri, surfaceForm);
            }
        }

        public String getMain(String content) {
            return String.format(main, content.replaceAll("\\n", "<br/>"));
        }
    }

}