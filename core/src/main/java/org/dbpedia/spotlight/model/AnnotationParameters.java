package org.dbpedia.spotlight.model;

import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguatorJ;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.sparql.SparqlQueryExecuter;
import org.dbpedia.spotlight.spot.Spotter;

import java.util.List;

/**
 * Created by dav009 on 19/08/2014.
 */
public class AnnotationParameters {

        public String inUrl = SpotlightConfiguration.DEFAULT_URL;
        public Double disambiguationConfidence = Double.parseDouble(SpotlightConfiguration.DEFAULT_CONFIDENCE);
        public Double spotterConfidence = Double.parseDouble(SpotlightConfiguration.DEFAULT_CONFIDENCE);
        public int support = Integer.parseInt(SpotlightConfiguration.DEFAULT_SUPPORT);
        public String dbpediaTypes = SpotlightConfiguration.DEFAULT_TYPES;
        public String sparqlQuery = SpotlightConfiguration.DEFAULT_SPARQL;
        public Boolean blacklist = Boolean.FALSE;
        public String policy = "whitelist";
        public Boolean coreferenceResolution = Boolean.parseBoolean(SpotlightConfiguration.DEFAULT_COREFERENCE_RESOLUTION);
        public String spotterName = "Default";
        public String disambiguatorName = "Default";
        public ParagraphDisambiguatorJ disambiguator;
        public String clientIp = "";
        public SparqlQueryExecuter sparqlExecuter;
        public List<Double> similarityThresholds;
        public String prefix = "";
        public String requestedUrl = "";

        public void setPolicyValue(String stringPolicy){
            blacklist = false;
            if(stringPolicy.trim().equalsIgnoreCase("blacklist")) {
                blacklist = true;
                policy = "blacklist";
            }
            else {
                policy = "whitelist";
            }
        }
}
