package org.dbpedia.spotlight.web.rest;

import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.model.TopicalClassificationConfiguration;
import org.dbpedia.spotlight.topical.TopicalClassifier;
import org.dbpedia.spotlight.topical.TopicalClassifierLoader;
import org.dbpedia.spotlight.web.rest.Server;

/**
 * @author: dirk
 * Date: 12/24/12
 * Time: 2:15 PM
 */
public class TopicalServer {


    private static TopicalClassifier classifier = null;

    public static TopicalClassifier getClassifier() {
        if(classifier!=null)
            return classifier;

        TopicalClassificationConfiguration conf = null;
        try {
            conf = new TopicalClassificationConfiguration(Server.getConfigurationPath());
        } catch (ConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        classifier = TopicalClassifierLoader.fromConfig(conf);
        return classifier;
    }
}
