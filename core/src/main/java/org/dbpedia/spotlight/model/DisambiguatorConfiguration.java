/*
 * Copyright 2011 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.exceptions.ConfigurationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import static org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy;

/**
 * Configuration for Disambiguation.
 *
 * @author pablomendes
 */
public class DisambiguatorConfiguration {

    private static Log LOG = LogFactory.getLog(DisambiguatorConfiguration.class);
    Properties config = new Properties();

    static String CONFIG_DISAMBIGUATORS = "org.dbpedia.spotlight.disambiguate.disambiguators";
    static String CONFIG_CONTEXT_INDEX_DIR = "org.dbpedia.spotlight.index.dir";


    protected String contextIndexDirectory = "";

    public boolean isContextIndexInMemory() {
        return contextIndexInMemory;
    }

    protected boolean contextIndexInMemory = false;

    public DisambiguatorConfiguration(String configFileName) throws ConfigurationException {

        //Read config properties:
        try {
            config.load(new FileInputStream(new File(configFileName)));
        } catch (IOException e) {
            throw new ConfigurationException("Cannot find configuration file "+configFileName,e);
        }

        if (config.getProperty(CONFIG_DISAMBIGUATORS) == null)
            throw new ConfigurationException(String.format("Please specify a list of disambiguators in %s within your configuration file %s.",CONFIG_DISAMBIGUATORS,configFileName));

        List<DisambiguationPolicy> policies = getDisambiguatorPolicies();
        if (policies.size()==0)
            throw new ConfigurationException(String.format("Please specify a list of disambiguators in %s within your configuration file %s.",CONFIG_DISAMBIGUATORS,configFileName));

        LOG.info(String.format("Will load disambiguators: %s.",policies));

        /* Validate parameters */
		contextIndexDirectory = config.getProperty(CONFIG_CONTEXT_INDEX_DIR,"").trim();
		if(contextIndexDirectory==null) {
            throw new ConfigurationException(String.format("Please specify the path to the lucene index directory (context index) in %s within your configuration file %s.", CONFIG_CONTEXT_INDEX_DIR, configFileName));
        } else if (!new File(contextIndexDirectory).isDirectory()) {
			throw new ConfigurationException(String.format("The value %s=%s does not exist or is not a directory. Please update your config in %s.", CONFIG_CONTEXT_INDEX_DIR, contextIndexDirectory, configFileName));
		}
        contextIndexInMemory = config.getProperty("org.dbpedia.spotlight.index.loadToMemory", "false").trim().equals("true");

    }

    public String getContextIndexDirectory() {
        return contextIndexDirectory;
    }

    public List<DisambiguationPolicy> getDisambiguatorPolicies() throws ConfigurationException {
        List<DisambiguationPolicy> policies = new ArrayList<DisambiguationPolicy>();
        String requestedDisambiguators = config.getProperty(CONFIG_DISAMBIGUATORS, "").trim();
        List<String> disambiguatorNames = Arrays.asList(requestedDisambiguators.split(","));
        if (requestedDisambiguators.isEmpty() || disambiguatorNames.size()==0) throw new ConfigurationException(String.format("Could not find '%s'. Please specify a comma-separated list of disambiguators to be loaded.",CONFIG_DISAMBIGUATORS));
        for (String s: disambiguatorNames) {
            try {
                policies.add(DisambiguationPolicy.valueOf(s.trim()));
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException(String.format("Unknown disambiguator '%s' specified in '%s'.",s,CONFIG_DISAMBIGUATORS));
            }
        }
        return policies;
    }


}
