/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbpedia.spotlight.web;

import java.util.ArrayList;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import java.io.File;
import java.util.List;
import org.dbpedia.spotlight.disambiguate.DefaultDisambiguator;
import org.dbpedia.spotlight.disambiguate.Disambiguator;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;
import org.dbpedia.spotlight.model.DBpediaType;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.string.ParseSurfaceFormText;
import org.dbpedia.spotlight.util.AnnotationFilter;

/**
 * REST Web Service
 *
 * @author Max
 */

@Path("Disambiguate")
@Consumes("text/plain")
public class Disambiguation {
    @Context
    private UriInfo context;

    private static Disambiguator disambiguator = new DefaultDisambiguator(new File(Configuration.indexDirectory));


    /**
     * Retrieves representation of an instance of org.dbpedia.spotlight.web.Annotation
     * @return an instance of java.lang.String
     */
    @GET
    @Produces("text/xml")
    public String getXML(@QueryParam("text") String text,
                          @DefaultValue("0.0") @QueryParam("confidence") double confidence,
                          @DefaultValue("0") @QueryParam("support") int support,
                          @DefaultValue("") @QueryParam("targetTypes") String targetTypes,
                          @DefaultValue("true") @QueryParam("coreferenceResolution") boolean coreferenceResolution) throws Exception {
       String xml="";
       String types[]=targetTypes.split(",");
       System.out.println("Parameters");
       System.out.println("Confidence:"+String.valueOf(confidence));
       System.out.println("support:"+String.valueOf(support));
       System.out.println("coreferenceResolution:"+String.valueOf(coreferenceResolution));

       List <DBpediaType> targetTypesList = new ArrayList();
       for (String targetType : types){
           targetTypesList.add(new DBpediaType(targetType.trim()));
           System.out.println("targetType:"+targetType.trim());
       }

       //System.out.println("Entro");
       if (text !=null){
        //System.out.println("text:"+text);
        //File scores = new File("C:\\Documents and Settings\\Andrés\\My Documents\\NetBeansProjects\\lib\\WebServicePackage\\failedTests.simScores");

        List<SurfaceFormOccurrence> sfOccList = ParseSurfaceFormText.parse(text);
        List<DBpediaResourceOccurrence> occList = disambiguator.disambiguate(sfOccList);
        text = ParseSurfaceFormText.eraseMarkup(text);
        List<DBpediaResourceOccurrence> filteredOccList = AnnotationFilter.filter(occList, confidence, support, targetTypesList, coreferenceResolution);

        OutputManager output= new OutputManager();
        xml = output.createXMLOutput(text,filteredOccList,confidence,support,targetTypes,coreferenceResolution);
       }
       return xml;


//        //TODO return proper representation object
//        String parameters= "<?xml version=\"1.0\"?><Parameters><text>"+text+"</text><conf>"+
//                confidence.toString()+"</conf><supp>"+
//                String.valueOf(support)+"</supp><targetTypes>";
//
//        String targetTypesList[]=targetTypes.split(",");
//        for (String targetType : targetTypesList)
//             parameters=parameters+"<targetType>"+targetType+"</targetType>";
//        parameters=parameters+"</targetTypes></Parameters>";
//
//        //call the spotlight service
//
//        //produce xml output
//        List<AnnotationResult> annotations = new ArrayList();
//        annotations.add(new AnnotationResult(0,"http://en.wikipedia.org/wiki/New_York_City","NewYork"));
//        annotations.add(new AnnotationResult(13,"http://en.wikipedia.org/wiki/City","city"));
//        annotations.add(new AnnotationResult(17,"http://en.wikipedia.org/wiki/United_States","USA"));
//

        //throw new UnsupportedOperationException();
    }

    @GET
    @Produces("application/json")
    public String getJson(@DefaultValue("") @QueryParam("text") String text,
                          @DefaultValue("0.7") @QueryParam("confidence") Double confidence,
                          @DefaultValue("100") @QueryParam("support") int support,
                          @DefaultValue("") @QueryParam("targetTypes") String targetTypes,
                          @DefaultValue("true") @QueryParam("coreferenceResolution") boolean coreferenceResolution) throws Exception {

        String xml=getXML(text, confidence, support, targetTypes, coreferenceResolution);
        OutputManager output= new OutputManager();
        return output.xmltoJson(xml);
    }

}
