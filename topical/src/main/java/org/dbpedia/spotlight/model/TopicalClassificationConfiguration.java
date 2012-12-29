package org.dbpedia.spotlight.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.db.model.TopicalStatInformation;
import org.dbpedia.spotlight.db.model.WordIdDictionary;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.topical.util.TopicUtil;
import scala.collection.Seq;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


/**
 * Configuration class for topical classification
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
    static String TOPIC_DESCRIPTION="org.dbpedia.spotlight.topic.description";

    private static Seq<TopicDescription> descriptions;

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

        File priors =  new File(config.getProperty(TOPICAL_PRIORS));
        if(!priors.exists())
            LOG.warn("Topical priors do not exist!");

        File topicDescription =  new File(config.getProperty(TOPIC_DESCRIPTION));
        if(!topicDescription.exists())
            LOG.warn("Topical descriptions were not loaded!");
        else
            descriptions = TopicDescription.fromDescriptionFile(topicDescription);
    }

    public Seq<TopicDescription> getDescription(){
        return descriptions;
    }

    public TopicalStatInformation loadTopicInfo() {
        String path = config.getProperty(TOPICS_INFO);
        if(path.isEmpty())
            return null;
        return TopicUtil.getTopicInfo(path);
    }

    public WordIdDictionary loadDictionary() {
        String path = config.getProperty(DICTIONARY);
        if(path.isEmpty())
            return null;
        return TopicUtil.getDictionary(path, Integer.parseInt(config.getProperty(DICTIONARY_MAXSIZE)));
    }

    public String getClassifierType(){
        return config.getProperty(CLASSIFIER_TYPE);
    }

    public File getModelFile(){
        return new File(config.getProperty(CLASSIFIER_PATH));
    }

    public File getPriorsDir(){
        return new File(config.getProperty(TOPICAL_PRIORS));
    }

}
