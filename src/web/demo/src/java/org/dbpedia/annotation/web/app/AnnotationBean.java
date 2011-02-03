/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbpedia.spotlight.web.app;

import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.dbpedia.spotlight.web.client.AnnotationWebServiceClient;
import org.xml.sax.InputSource;



/**
 *
 * @author Andrés
 */
@ManagedBean(name="AnnotationBean")
@SessionScoped
public class AnnotationBean implements Serializable{

    String text;
    String annotatedText;
    String disambiguatedText;
    String targetTypes;

    // parameters
    double confidence=0.0;
    int support=0;
    boolean coreferenceResolution=false;

    // types
    boolean person=true;
    boolean place=true;
    boolean organisation=true;
    boolean work=true;
    boolean species=true;
    boolean otherTypes=true;
    boolean untyped=true;

    public String getTargetTypes() {
        return targetTypes;
    }

    public void setTargetTypes(String targetTypes) {
        this.targetTypes = targetTypes;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public int getSupport() {
        return support;
    }

    public void setSupport(int support) {
        this.support = support;
    }

    public boolean isCoreferenceResolution() {
        return coreferenceResolution;
    }

    public void setCoreferenceResolution(boolean coreferenceResolution) {
        this.coreferenceResolution = coreferenceResolution;
    }

    public boolean isOrganisation() {
        return organisation;
    }

    public void setOrganisation(boolean organisation) {
        this.organisation = organisation;
    }

    public boolean isOtherTypes() {
        return otherTypes;
    }

    public void setOtherTypes(boolean otherTypes) {
        this.otherTypes = otherTypes;
    }

    public boolean isPerson() {
        return person;
    }

    public void setPerson(boolean person) {
        this.person = person;
    }

    public boolean isPlace() {
        return place;
    }

    public void setPlace(boolean place) {
        this.place = place;
    }

    public boolean isWork() {
        return work;
    }

    public void setWork(boolean work) {
        this.work = work;
    }

    public boolean isUntyped() {
        return untyped;
    }

    public void setUntyped(boolean unknown) {
        this.untyped = unknown;
    }

    public boolean isSpecies() {
        return species;
    }

    public void setSpecies(boolean species) {
        this.species = species;
    }


//    public String getTypesString() {
//        if (person && place && organisation && work && species && otherTypes && untyped)
//            return "";
//
//        String targetTypes = "";
//        if (person)
//            targetTypes += "Person,";
//        if (place)
//            targetTypes += "Place,";
//        if (organisation)
//            targetTypes += "Organisation,";
//        if (work)
//            targetTypes += "Work,";
//        if (species)
//            targetTypes += "Species,";
//        if (otherTypes) {
//            targetTypes += "Event,Drug,Website,ChemicalCompound,Beverage,";
//            targetTypes += "Disease,OlympicResult,Painting,Award,EthnicGroup,";
//            targetTypes += "Infrastructure,Currency,Language,PersonFunction,";
//            targetTypes += "Activity,MusicGenre,Planet,Colour,Device,Sales,";
//            targetTypes += "SupremeCourtOfTheUnitedStatesCase,GovernmentType,";
//            targetTypes += "MeanOfTransportation,AnatomicalStructure,Protein,";
//        }
//        if (untyped)
//            targetTypes += "unknown,";
//
//        // erase last comma
//        targetTypes = targetTypes.substring(0, targetTypes.length()-1);
//        return targetTypes;
//    }

    public String getAnnotatedText() throws Exception{
        System.out.println("target Types:"+targetTypes);
        if (text.length()<=0){
            annotatedText="";
            return annotatedText;
        }        
        AnnotationWebServiceClient client = new AnnotationWebServiceClient();        
        Object response = client.getAnnotationXML(text, confidence, support, targetTypes, coreferenceResolution);
        annotatedText="";
        String annotationsXMLDoc = (String)response;
        annotatedText = processResult(annotationsXMLDoc);
        return annotatedText;
    }

    public void setAnnotatedText(String annotatedText) {
        this.annotatedText = annotatedText;
    }

    public String getDisambiguatedText() throws Exception{
        if (text.length()<=0){
            disambiguatedText="";
            return disambiguatedText;
        }
        AnnotationWebServiceClient client = new AnnotationWebServiceClient();
        Object response = client.getDisambiguationXML(text, confidence, support, targetTypes, coreferenceResolution);
        disambiguatedText="";
        String annotationsXMLDoc = (String)response;
        disambiguatedText = processResult(annotationsXMLDoc);
        return disambiguatedText;
    }

    public void setDisambiguatedText(String disambiguatedText) {
        this.disambiguatedText = disambiguatedText;
    }
    

    public String getText(){
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void annotate(){        
    }

    public void clear(){
        text="";
    }

    private String processResult(String xmlDoc) throws Exception{
        if (xmlDoc.length()<=0)
            return "";

        AnnotationResultHandler hd= new AnnotationResultHandler();
        SAXParserFactory factory = SAXParserFactory.newInstance();
    	SAXParser saxParser = factory.newSAXParser();
        Reader reader = new StringReader(xmlDoc);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");

        saxParser.parse(is, hd);

        String sourceText = hd.getText();
        //hd.getOccList();

        String output = produceOutputText(sourceText,hd.getOccList());
        return output;
    }

    private String produceOutputText(String sourceText, List<Occurrence> occList) throws Exception{
        int lengthAdded=0;
        String modifiedText="";
        String startText;
        modifiedText = sourceText;
        //put the annotations in the text
        for (Occurrence occ : occList){
//            System.out.println("lenghtAdded"+lengthAdded);
//            System.out.println("modifiedText 0 "+modifiedText);
            int endOfSurfaceform=occ.offset+lengthAdded+ occ.surfaceForm.length();
            startText = modifiedText.substring(0,occ.offset+lengthAdded);
//            System.out.println("modifiedText 1 "+startText);
            String annotationtoAdd ="<a href=\""+occ.uri+"\" title=\""+occ.uri+"\" target=\"_blank\">"+occ.surfaceForm+"</a>";
            modifiedText = startText + annotationtoAdd + modifiedText.substring(endOfSurfaceform);
            lengthAdded = lengthAdded + (annotationtoAdd.length()-occ.surfaceForm.length());
        }
        return modifiedText.replaceAll("\\n", "<br/>");
    };

    public AnnotationBean() {
        text ="President Obama called Wednesday on Congress to extend a tax break "
              + "for students included in last year's economic stimulus package, arguing that the policy "
              + "provides more generous assistance.";

        confidence = 0.3;
        support = 30;
        coreferenceResolution = true;
    }

}


