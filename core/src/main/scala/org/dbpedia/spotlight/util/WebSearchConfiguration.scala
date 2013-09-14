package org.dbpedia.spotlight.util

import org.dbpedia.spotlight.exceptions.ConfigurationException
import org.dbpedia.spotlight.log.SpotlightLog
import java.util.Properties
import java.io.{FileInputStream, File}

/**
 * Created by IntelliJ IDEA.
 * User: pablo
 * Date: 4/14/11
 * Time: 3:50 PM
 * To change this template use File | Settings | File Templates.
 */

class WebSearchConfiguration (val configFile: File) {

    def this(fileName: String) {
        this(new File(fileName))
    }

    private val properties : Properties = new Properties()

    SpotlightLog.info(this.getClass, "Loading configuration file %s", configFile)
    properties.load(new FileInputStream(configFile))
    validate

    //TODO copied from IndexingConfiguration
    def get(key : String, defaultValue : String) : String = {
        properties.getProperty(key, defaultValue)
    }
    //TODO copied from IndexingConfiguration
    def get(key : String) : String = {
        val value = get(key, null)
        if(value == null) {
            throw new ConfigurationException(key+" not specified in "+configFile)
        }
        value
    }

    //TODO validate yahoo data ...
    private def validate {

        get("org.dbpedia.spotlight.yahoo.appID") // will throw an exception if it cannot find

//        val dumpFile = new File(get("org.dbpedia.spotlight.data.wikipediaDump"))
//        if(!dumpFile.isFile) {
//            throw new ConfigurationException("specified Wikipedia dump not found: "+dumpFile)
//        }

    }
}