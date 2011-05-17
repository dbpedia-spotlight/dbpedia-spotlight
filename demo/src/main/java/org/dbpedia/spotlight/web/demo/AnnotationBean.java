/**
 * Copyright 2011 Andrés García-Silva, Max Jakob
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

package org.dbpedia.spotlight.web.demo;

import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;


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
    String sparql;

    // parameters
    double confidence=0.0;
    int support=0;

    // types
    boolean person=true;
    boolean place=true;
    boolean organisation=true;
    boolean work=true;
    boolean species=true;
    boolean otherTypes=true;
    boolean untyped=true;


    public String getSparql() {
        return sparql;
    }

    public void setSparql(String sparql) {
        this.sparql = sparql;
    }

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

    /**
     * Needed because web service returns complete HTML documents.  The </html>
     * would close the complete result page. That is why the main outer markup
     * is cleaned.
     * This is not the prettiest and best way of doing this by the way.
     * @param html document string
     * @return only <div> annotation text </div> remains
     */
    private String deleteMainHtmlMarkup(String html) {
        html = html.replaceAll("<!DOCTYPE[^>]*>", "");
        html = html.replaceAll("</?html[^>]*>", "");
        html = html.replaceAll("</?head>", "");
        html = html.replaceAll("<title>.*</title>", "");
        html = html.replaceAll("<meta[^>]*>", "");
        html = html.replaceAll("</?body>", "");
        System.out.println(html);
        return html;
    }

    public String getAnnotatedText() throws Exception{
        System.out.println("types: "+targetTypes);
        if (text.length() <= 0){
            annotatedText = "";
            return annotatedText;
        }
        AnnotationWebServiceClient client = new AnnotationWebServiceClient();
        String responseHtml = client.getAnnotationHTML(text, confidence, support, targetTypes);
        annotatedText = deleteMainHtmlMarkup(responseHtml);
        return annotatedText;
    }

    public void setAnnotatedText(String t) {
        annotatedText = t;
    }

    public String getDisambiguatedText() throws Exception{
        System.out.println("types: "+targetTypes);
        if (text.length() <= 0){
            disambiguatedText = "";
            return disambiguatedText;
        }
        AnnotationWebServiceClient client = new AnnotationWebServiceClient();
        String responseHtml = client.getDisambiguationHTML(text, confidence, support, targetTypes);
        disambiguatedText = deleteMainHtmlMarkup(responseHtml);
        return disambiguatedText;
    }

    public void setDisambiguatedText(String t) {
        disambiguatedText = t;
    }

    public String getText(){
        return text;
    }

    public void setText(String t) {
        text = t;
    }

    public void annotate(){
    }

    public void clear(){
        text = "";
    }


    public AnnotationBean() {
        text = "The revelations follow the publication of former Defence Secretary Donald Rumsfeld's memoirs, in which he said Iraq had no programme for weapons of mass destruction.\n\n"
             + "The alleged existence of biological weapons and WMD in Iraq was used by US officials as justification for sending troops to topple Saddam Hussein.";

        confidence = 0.5;
        support = 30;
    }

}