package org.dbpedia.spotlight.web.rest.resources;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.model.AnnotationParameters;

/**
 * Created by dav009 on 20/08/2014.
 */
public class BaseRestResource {

    Log LOG;
    String apiName = "BaseRestResource";

    public void announce(String textString, AnnotationParameters params) {
        LOG.info("******************************** Parameters ********************************");
        LOG.info("API: " + getApiName());
        LOG.info("client ip: " + params.clientIp);
        LOG.info("text: " + textString);
        LOG.info("text length in chars: " + textString.length());
        LOG.info("disambiguation confidence: "+ params.disambiguationConfidence);
        LOG.info("spotterConfidence confidence: "+ params.spotterConfidence);
        LOG.info("support: "+ String.valueOf(params.support));
        LOG.info("types: "+ params.dbpediaTypes);
        LOG.info("sparqlQuery: "+ params.sparqlQuery);
        LOG.info("policy: "+ params.policy);
        LOG.info("coreferenceResolution: "+params.coreferenceResolution);
        LOG.info("spotter: "+ params.spotterName);
        LOG.info("disambiguator: "+ params.disambiguatorName);

    }

    public String getApiName(){
        return apiName;
    }
}
