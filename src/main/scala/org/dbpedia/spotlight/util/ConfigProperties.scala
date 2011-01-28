package org.dbpedia.spotlight.util

import org.apache.commons.logging.LogFactory
import java.util.Properties
import io.Source
import java.io.{PrintStream, FileOutputStream, FileInputStream, File}

/**
 * Class that holds configurations of the project.
 *
 * Defaults are not defined. All classes asking for properties have to take care
 * of the case that a property is not specified in the configuration file.
 */

//TODO: validate configuration file

object ConfigProperties
{
    private val LOG = LogFactory.getLog(this.getClass)

    var configFileName = "spotlight.config"

    private val properties : Properties = new Properties()

    // load properties as soon as they are asked for for the first time
    load

    def load(configFile : File) {
        properties.load(new FileInputStream(configFile))
        configFileName = configFile.getAbsolutePath
        LOG.info("Loaded configuration file '" + configFile.getAbsolutePath + "'.")
    }

    def load() {
        load(new File(configFileName))
    }

    def save(configFile : File) {
        properties.store(new FileOutputStream(configFile), "")
        LOG.info("Saved configuration file '" + configFile.getAbsolutePath + "'.")
    }

    def save() {
        save(new File(configFileName))
    }

    def get(key : String, defaultValue : String) : String = {
        properties.getProperty(key, defaultValue)
    }

    def get(key : String) : String = {
        get(key, null)
    }

    def set(key : String, value : String) {
        properties.setProperty(key, value)
        //properties.store(new FileOutputStream(configFileName), "changed "+key+" to "+value+" in "+configFileName)

        val sb = new StringBuilder
        for(line <- Source.fromFile(configFileName, "UTF-8").getLines) {
            if(line startsWith key+" ") {
                sb.append(key+"  "+value+"\n")
            }
            else {
                sb.append(line+"\n")
            }
        }

        val out = new PrintStream(configFileName, "UTF-8")
        out.print(sb.toString)
        out.close
    }
    
}