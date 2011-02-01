/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbpedia.spotlight.web;

import java.io.*;
// SAX classes.
import java.util.List;
import org.xml.sax.helpers.*;
//JAXP 1.1
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.sax.*;

//Jason classes
import net.sf.json.JSON;
import net.sf.json.xml.XMLSerializer;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;


/**
 *
 * @author Andrés
 */
public class OutputManager {

    public TransformerHandler initXMLDoc(ByteArrayOutputStream out) throws Exception{
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

public String createXMLOutput(String text, List<DBpediaResourceOccurrence> occList, double confidence, int support, String targetTypesString, boolean coreferenceResolution) throws Exception{
        // PrintWriter from a Servlet
        String xmlDoc="";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TransformerHandler hd = initXMLDoc(out);

        //Create Annotation element
        //First create text attribute
        AttributesImpl atts = new AttributesImpl();

        atts.addAttribute("","","text","CDATA",text);
        atts.addAttribute("","","confidence","CDATA",String.valueOf(confidence));
        atts.addAttribute("","","support","CDATA",String.valueOf(support));
        atts.addAttribute("","","types","CDATA",targetTypesString);
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

          atts.addAttribute("","","surfaceForm","CDATA",occ.surfaceForm().name());
          atts.addAttribute("","","offset","CDATA",String.valueOf(occ.textOffset()));
          atts.addAttribute("","","similarityScore","CDATA",String.valueOf(occ.similarityScore()));
          atts.addAttribute("","","percentageOfSecondRank","CDATA",String.valueOf(occ.percentageOfSecondRank()));
          
          hd.startElement("","","Resource",atts);
          hd.endElement("","","Resource");
          i++;
        }
        if (i>0)
          hd.endElement("","","Resources");

        hd.endElement("","","Annotation");
        hd.endDocument();
        xmlDoc = out.toString("utf-8");
        System.out.println(xmlDoc);
        return xmlDoc;
    }


    public String xmltoJson(String xmlDoc) throws Exception{

        XMLSerializer xmlSerializer = new XMLSerializer();
        JSON json = xmlSerializer.read( xmlDoc );
        //System.out.println( "--- Json version ----");
        System.out.println( json.toString(2) );
        return json.toString(2);
    }

    
}
