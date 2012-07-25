package org.dbpedia.spotlight.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.db.model.HashMapTopicalPriorStore;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.topic.TopicalClassifier;
import org.dbpedia.spotlight.topic.WekaMultiLabelClassifier;
import org.dbpedia.spotlight.topic.WekaSingleLabelClassifier;
import org.dbpedia.spotlight.topic.utility.TopicUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 6/24/12
 * Time: 11:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class TopicalClassificationConfiguration {

    private static Log LOG = LogFactory.getLog(TopicalClassificationConfiguration.class);
    Properties config = new Properties();

    static String CLASSIFIER_PATH = "org.dbpedia.spotlight.topic.model.path";
    static String CLASSIFIER_TYPE = "org.dbpedia.spotlight.topic.model.type";
    static String TOPICS_INFO = "org.dbpedia.spotlight.topic.topics.info";
    static String DICTIONARY="org.dbpedia.spotlight.topic.dictionary";
    static String DICTIONARY_MAXSIZE="org.dbpedia.spotlight.topic.dictionary.maxsize";
    static String TOPICAL_PRIORS="org.dbpedia.spotlight.topic.priors";

    //public enum DisambiguationPolicy { Document,Occurrences,CuttingEdge,Default }

    public TopicalClassificationConfiguration(String configFileName) throws ConfigurationException {
        //Read config properties:
        try {
            config.load(new FileInputStream(new File(configFileName)));
        } catch (IOException e) {
            throw new ConfigurationException("Cannot find configuration file "+configFileName,e);
        }

        if (config.getProperty(CLASSIFIER_TYPE) == null || config.getProperty(CLASSIFIER_PATH) == null || config.getProperty(DICTIONARY) == null || config.getProperty(DICTIONARY_MAXSIZE) == null)
            throw new ConfigurationException(String.format("Please validate your configuration in file %s for topical classification.",configFileName));

        File priorsDir =  new File(config.getProperty(TOPICAL_PRIORS));
        if(!priorsDir.exists())
            LOG.warn("Topical priors were not loaded!");
        else
            HashMapTopicalPriorStore.fromDir(priorsDir);

    }

    public TopicalClassifier getClassifier() {
        LOG.info("Loading topical classifier...");
        if(config.getProperty(CLASSIFIER_TYPE).equals("org.dbpedia.spotlight.topic.WekaSingleLabelClassifier")) {
            return new WekaSingleLabelClassifier(TopicUtil.getDictionary(config.getProperty(DICTIONARY), Integer.parseInt(config.getProperty(DICTIONARY_MAXSIZE))),
                                                 TopicUtil.getTopicInfo(config.getProperty(TOPICS_INFO)),
                                                 new File(config.getProperty(CLASSIFIER_PATH)));
        }

        if(config.getProperty(CLASSIFIER_TYPE).equals("org.dbpedia.spotlight.topic.WekaMultiLabelClassifier"))
          return new WekaMultiLabelClassifier(TopicUtil.getDictionary(config.getProperty(DICTIONARY), Integer.parseInt(config.getProperty(DICTIONARY_MAXSIZE))),
                                              TopicUtil.getTopicInfo(config.getProperty(TOPICS_INFO)),
                                              new File(config.getProperty(CLASSIFIER_PATH)));

        return null;
    }

}
