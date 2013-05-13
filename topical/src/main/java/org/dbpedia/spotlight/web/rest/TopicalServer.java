package org.dbpedia.spotlight.web.rest;

import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.model.TopicalClassificationConfiguration;
import org.dbpedia.spotlight.topical.TopicalClassifier;
import org.dbpedia.spotlight.topical.TopicalClassifierFactory$;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author: dirk
 * Date: 12/24/12
 * Time: 2:15 PM
 */
public class TopicalServer {

    public static void main(String[] args) throws InterruptedException, URISyntaxException, ClassNotFoundException, InitializationException, IOException {
        TopicalClassificationConfiguration conf = null;
        try {
            conf = new TopicalClassificationConfiguration(args[0]);
        } catch (ConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        classifier = TopicalClassifierFactory$.MODULE$.fromFile(conf.getModelFile(), conf.getClassifierType()).get();

        Server.main(args);
    }

    private static TopicalClassifier classifier;

    public static TopicalClassifier getClassifier() {
        return classifier;
    }
}
