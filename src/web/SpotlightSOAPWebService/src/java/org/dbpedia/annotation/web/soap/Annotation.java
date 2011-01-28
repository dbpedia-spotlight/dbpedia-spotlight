/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbpedia.spotlight.web.soap;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.ejb.Stateless;

import java.util.ArrayList;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.util.List;
import org.dbpedia.spotlight.annotate.Annotator;
import org.dbpedia.spotlight.DefaultAnnotator;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;
import org.dbpedia.spotlight.model.DBpediaType;
import org.dbpedia.spotlight.util.AnnotationFilter;
/**
 *
 * @author Andrés
 */
@WebService(serviceName="SpotlightSOAPWebService")
@Stateless()
public class Annotation {
    @Context
    private UriInfo context;

    //TODO make this configurable
    final static String spotterFile    = "e:\\dbpa\\web\\TitRedDis.spotterDictionary";
    final static String indexDirectory = "e:\\dbpa\\web\\DisambigIndex.singleSFs-plusTypes";
    private static Annotator annotator = new DefaultAnnotator(new File(spotterFile), new File(indexDirectory));


    /**
     * Web service operation
     */
    @WebMethod(operationName = "Annotate")
    public String Annotate(@WebParam(name = "text")
    String text, @WebParam(name = "confidence")
    double confidence, @WebParam(name = "support")
    int support, @WebParam(name = "types")
    String targetTypes, @WebParam(name = "coreferenceResolution")
    boolean coreferenceResolution) throws Exception {
               String xml="";
       String types[]=targetTypes.split(",");
       System.out.println("Parameters");
       System.out.println("Confidence:"+String.valueOf(confidence));
       System.out.println("support:"+String.valueOf(support));
       System.out.println("coreferenceResolution:"+String.valueOf(coreferenceResolution));

       List <DBpediaType> targetTypesList = new ArrayList();
       for (String targetType : types){
           targetTypesList.add(new DBpediaType(targetType.trim()));
           System.out.println("type:"+targetType.trim());
       }

       //System.out.println("Entro");
       if (text !=null){
        //System.out.println("text:"+text);
        //File scores = new File("C:\\Documents and Settings\\Andrés\\My Documents\\NetBeansProjects\\lib\\WebServicePackage\\failedTests.simScores");

        List<DBpediaResourceOccurrence> occList = annotator.annotate(text);
        List<DBpediaResourceOccurrence> filteredOccList = AnnotationFilter.filter(occList, confidence, support, targetTypesList, coreferenceResolution);

        OutputManager output= new OutputManager();
        xml = output.createXMLOutput(text,filteredOccList,confidence,support,targetTypes,coreferenceResolution);
       }
       return xml;

    }
}
