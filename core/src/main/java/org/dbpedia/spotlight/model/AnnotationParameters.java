package org.dbpedia.spotlight.model;

import org.dbpedia.spotlight.sparql.SparqlQueryExecuter;
import org.dbpedia.spotlight.spot.Spotter;

import java.util.List;

/**
 * Created by dav009 on 19/08/2014.
 */
public class AnnotationParameters {

        public String inUrl;
        public Double disambiguationConfidence;
        public Double spotterConfidence;
        public int support;
        public String dbpediaTypes;
        public String sparqlQuery;
        public Boolean policy;
        public Boolean coreferenceResolution;
        public String spotterName;
        public String disambiguatorName;
        public Spotter spotter;
        public List<Double> similarityThresholds;
        public SparqlQueryExecuter sparqlExecuter;

}
