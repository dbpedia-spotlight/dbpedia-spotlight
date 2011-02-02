package org.dbpedia.spotlight.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.annotate.Annotator;
import org.dbpedia.spotlight.disambiguate.Disambiguator;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;
import org.dbpedia.spotlight.model.DBpediaType;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.string.ParseSurfaceFormText;
import org.dbpedia.spotlight.util.AnnotationFilter;

public class SpotlightInterface {

    Log LOG = LogFactory.getLog(this.getClass());

    // only one of those two APIs can be set
    Annotator annotator;
    Disambiguator disambiguator;

    public SpotlightInterface(Annotator a) {
        annotator = a;
    }

    public SpotlightInterface(Disambiguator d) {
        disambiguator = d;
    }


    private OutputManager output = new OutputManager();

    /**
     * Retrieves representation of an instance of org.dbpedia.spotlight.web.Annotation
     * @return an instance of java.lang.String
     */
    public String getXML(String text, double confidence, int support, String targetTypes, boolean coreferenceResolution) throws Exception {
        String xml = "";

        LOG.info("******************************** Parameters ********************************");
        LOG.info("API: "+disambiguator.getClass());
        LOG.info("confidence: "+String.valueOf(confidence));
        LOG.info("support: "+String.valueOf(support));
        LOG.info("coreferenceResolution:" +String.valueOf(coreferenceResolution));

        List<DBpediaType> targetTypesList = new ArrayList<DBpediaType>();
        String types[] = targetTypes.split(",");
        for (String targetType : types){
            targetTypesList.add(new DBpediaType(targetType.trim()));
            //LOG.info("type:"+targetType.trim());
        }

        if (text != null){
            try {
                List<DBpediaResourceOccurrence> occList;

                // use API that was given to the constructor
                if(disambiguator == null && annotator != null) {
                    occList = annotator.annotate(text);
                }
                else if(disambiguator != null && annotator == null) {
                    List<SurfaceFormOccurrence> sfOccList = ParseSurfaceFormText.parse(text);
                    occList = disambiguator.disambiguate(sfOccList);
                }
                else {
                    throw new IllegalStateException("both annotator and disambiguator were not initialized");
                }

                List<DBpediaResourceOccurrence> filteredOccList = AnnotationFilter.filter(occList, confidence, support, targetTypesList, coreferenceResolution);
                xml = output.createXMLOutput(text,filteredOccList,confidence,support,targetTypes,coreferenceResolution);
            }
            catch (InputException e) {
                xml = output.createErrorXMLOutput(e.getMessage(),text,confidence,support,targetTypes,coreferenceResolution);
            }
        }

        return xml;
    }

    public String getJSON(String text, double confidence, int support, String targetTypes, boolean coreferenceResolution) throws Exception {
        String xml = getXML(text, confidence, support, targetTypes, coreferenceResolution);
        return output.xmltoJson(xml);
    }

    //TODO
    public String getRDF(String text, double confidence, int support, String targetTypes, boolean coreferenceResolution) throws Exception {
        //String xml = getXML(text, confidence, support, targetTypes, coreferenceResolution);
        return "";
    }

}