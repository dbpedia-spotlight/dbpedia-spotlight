/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbpedia.spotlight.web.soap.client;

import org.dbpedia.spotlight.web.soap.client2.Exception_Exception;

/**
 *
 * @author Andrés
 */
public class SpotlightSOAPClient {

 public static void main(String[] args) throws Exception_Exception, Exception_Exception {
        String text="Durant plays in NBA";
        double confidence =0.2;
        int support =1;
        String targetTypes ="";
        boolean coreferenceResolution=false;
        String result="";
        result = annotate(text,confidence, support,targetTypes, coreferenceResolution);
        System.out.println(result);
    }
 
    private static String disambiguate(java.lang.String text, double confidence, int support, java.lang.String targetTypes, boolean coreferenceResolution) throws Exception_Exception, org.dbpedia.spotlight.web.soap.client.Exception_Exception {
        org.dbpedia.spotlight.web.soap.client.SpotlightSOAPWebService service = new org.dbpedia.spotlight.web.soap.client.SpotlightSOAPWebService();
        org.dbpedia.spotlight.web.soap.client.Disambiguation port = service.getDisambiguationPort();
        return port.disambiguate(text, confidence, support, targetTypes, coreferenceResolution);
    }

    private static String annotate(java.lang.String text, double confidence, int support, java.lang.String types, boolean coreferenceResolution) throws org.dbpedia.spotlight.web.soap.client2.Exception_Exception {
        org.dbpedia.spotlight.web.soap.client2.SpotlightSOAPWebService service = new org.dbpedia.spotlight.web.soap.client2.SpotlightSOAPWebService();
        org.dbpedia.spotlight.web.soap.client2.Annotation port = service.getAnnotationPort();
        return port.annotate(text, confidence, support, types, coreferenceResolution);
    }

    
}
