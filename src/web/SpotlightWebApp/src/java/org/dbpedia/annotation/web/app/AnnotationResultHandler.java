/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbpedia.spotlight.web.app;

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Andrés
 */
public class AnnotationResultHandler extends DefaultHandler{

    boolean annotation = false;
    boolean entities = false;
    boolean entity = false;
    String text;
    String myUri;
    String surfaceForm="";
    int previousSurfaceFormLenght=0;
    int offset=-1;
    int previousOffset=-1;
    int offsetLimit=0;

    List<Occurrence> occList;

    public AnnotationResultHandler() {
        text="";
        occList = new ArrayList();
    }

    public List<Occurrence> getOccList() {
        return occList;
    }

    public String getText() {
        return text;
    }
    
    public void startElement(String uri, String localName,
                          String qName, Attributes attributes)throws SAXException {
  
//        System.out.println("Start Element :" + qName);

      if (qName.equalsIgnoreCase("ANNOTATION")) {
              annotation = true;
              text = resolveAttrib( uri, "text", attributes, "unknown" );
      }

      if (qName.equalsIgnoreCase("Resources")) {
              entities = true;
      }

      if (qName.equalsIgnoreCase("Resource")) {
              entity = true;
              myUri = resolveAttrib( uri, "URI", attributes, "unknown" );
              previousSurfaceFormLenght = surfaceForm.length();
              surfaceForm = resolveAttrib( uri, "surfaceForm", attributes, "unknown" );
              previousOffset = offset;
              offset = Integer.valueOf(resolveAttrib( uri, "offset", attributes, "unknown" )).intValue();
              Occurrence occ= new Occurrence(offset,myUri,surfaceForm);
              offsetLimit=previousOffset + previousSurfaceFormLenght;
              //System.out.println("surfaceForm:"+surfaceForm);
              //System.out.println("offset:"+offset+ " mayor que:" + offsetLimit);
              if (offset>=offsetLimit)
                  occList.add(occ);
      }
    }

    private String resolveAttrib( String uri, String localName,
			          Attributes attribs, String defaultValue ) {

	String tmp = attribs.getValue( uri, localName );
	return (tmp!=null)?(tmp):(defaultValue);
    }

//   public void endElement(String uri, String localName,
//    	                String qName)
//    	                throws SAXException {
//    	              System.out.println("End Element :" + qName);
//   }

//    public void characters(char ch[], int start, int length)
//    	            throws SAXException {
//
//    	          System.out.println(new String(ch, start, length));
//
//    	          if (spotlight) {
//    	            System.out.println("Annotation : "
//    	                + new String(ch, start, length));
//                   System.out.println("text :"+ text);
//    	            spotlight = false;
//    	          }
//
//    	          if (entities) {
//    	              System.out.println("entities : "
//    	                  + new String(ch, start, length));
//    	              entities = false;
//    	           }
//
//    	          if (entity) {
//    	              System.out.println("entity: " + new String(ch, start, length));
//                      System.out.println("uri :"+ myUri);
//                      System.out.println("surfaceForm :"+ surfaceForm);
//                      System.out.println("offset :"+ offset);
//                      Occurrence occ= new Occurrence(offset,myUri,surfaceForm);
//    	              entity = false;
//    	           }
//
//    	        }



}
