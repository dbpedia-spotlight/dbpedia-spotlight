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

package org.dbpedia.spotlight.web.rest;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.db.SpotlightModel;
import org.dbpedia.spotlight.db.model.TextTokenizer;
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguatorJ;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.SpotlightFactory;
import org.dbpedia.spotlight.model.SpotterConfiguration;
import org.dbpedia.spotlight.sparql.SparqlQueryExecuter;
import org.dbpedia.spotlight.spot.Spotter;
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy;
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy;
import scala.collection.JavaConverters;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instantiates Web Service that will execute annotation and disambiguation tasks.
 *
 * @author maxjakob
 * @author pablomendes - added WADL generator config, changed to Grizzly
 */

public class Server {
    static Log LOG = LogFactory.getLog(Server.class);

    public static final String APPLICATION_PATH = "http://spotlight.dbpedia.org/rest";

    // Server reads configuration parameters into this static configuration object that will be used by other classes downstream
    protected static SpotlightConfiguration configuration;

    // Server will hold a few spotters that can be chosen from URL parameters
    protected static Map<SpotterPolicy,Spotter> spotters = new HashMap<SpotterConfiguration.SpotterPolicy,Spotter>();

    // Server will hold a few disambiguators that can be chosen from URL parameters
    protected static Map<DisambiguationPolicy,ParagraphDisambiguatorJ> disambiguators = new HashMap<SpotlightConfiguration.DisambiguationPolicy,ParagraphDisambiguatorJ>();

    private static volatile Boolean running = true;

    static String usage = "usage: java -jar dbpedia-spotlight.jar org.dbpedia.spotlight.web.rest.Server [config file]"
                        + "   or: mvn scala:run \"-DaddArgs=[config file]\"";

    //This is currently only used in the DB-based version.
    private static TextTokenizer tokenizer;

    private static String namespacePrefix = SpotlightConfiguration.DEFAULT_NAMESPACE;

    private static SparqlQueryExecuter sparqlExecuter = null;

    private static List<Double> similarityThresholds = new ArrayList<Double>();

    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException, ClassNotFoundException, InitializationException {

        URI serverURI = null;

        if(args[0].endsWith(".properties")) {
            //We are using the old-style configuration file:

            initByPropertiesFile(args[0]);
            serverURI = new URI(configuration.getServerURI());

        } else {
            //We are using a model folder:

            serverURI = new URI(args[1]);
            initByModel(args[0]);
        }

        //ExternalUriWadlGeneratorConfig.setUri(configuration.getServerURI()); //TODO get another parameter, maybe getExternalServerURI since Grizzly will use this in order to find out to which port to bind


        LOG.info(String.format("Initiated %d disambiguators.",disambiguators.size()));
        LOG.info(String.format("Initiated %d spotters.",spotters.size()));

        final Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
        initParams.put("com.sun.jersey.config.property.packages", "org.dbpedia.spotlight.web.rest.resources");
        initParams.put("com.sun.jersey.config.property.WadlGeneratorConfig", "org.dbpedia.spotlight.web.rest.wadl.ExternalUriWadlGeneratorConfig");


        SelectorThread threadSelector = GrizzlyWebContainerFactory.create(serverURI, initParams);
        threadSelector.start();

        System.err.println("Server started in " + System.getProperty("user.dir") + " listening on " + serverURI);

        Thread warmUp = new Thread() {
            public void run() {
                //factory.searcher().warmUp((int) (configuration.getMaxCacheSize() * 0.7));
            }
        };
        warmUp.start();


        while(running) {
            Thread.sleep(100);
        }

        //Stop the HTTP server
        //server.stop(0);
        threadSelector.stopEndpoint();
        System.exit(0);

    }


    private static void setSpotters(Map<SpotterPolicy,Spotter> s) throws InitializationException {
        if (spotters.size() == 0)
            spotters = s;
        else
            throw new InitializationException("Trying to overwrite singleton Server.spotters. Something fishy happened!");
    }

    private static void setDisambiguators(Map<SpotlightConfiguration.DisambiguationPolicy,ParagraphDisambiguatorJ> s) throws InitializationException {
        if (disambiguators.size() == 0)
            disambiguators = s;
        else
            throw new InitializationException("Trying to overwrite singleton Server.disambiguators. Something fishy happened!");
    }

    public static Spotter getSpotter(String name) throws InputException {
        SpotterPolicy policy = SpotterPolicy.Default;
        try {
            policy = SpotterPolicy.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new InputException(String.format("Specified parameter spotter=%s is invalid. Use one of %s.",name,SpotterPolicy.values()));
        }

        if (spotters.size() == 0)
            throw new InputException(String.format("No spotters were loaded. Please add one of %s.",spotters.keySet()));

        Spotter spotter = spotters.get(policy);
        if (spotter==null) {
            throw new InputException(String.format("Specified spotter=%s has not been loaded. Use one of %s.",name,spotters.keySet()));
        }
        return spotter;
    }

    public static ParagraphDisambiguatorJ getDisambiguator(String name) throws InputException {
        DisambiguationPolicy policy = DisambiguationPolicy.Default;
        try {
            policy = DisambiguationPolicy.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new InputException(String.format("Specified parameter disambiguator=%s is invalid. Use one of %s.",name,DisambiguationPolicy.values()));
        }

        if (disambiguators.size() == 0)
            throw new InputException(String.format("No disambiguators were loaded. Please add one of %s.",disambiguators.keySet()));

        ParagraphDisambiguatorJ disambiguator = disambiguators.get(policy);
        if (disambiguator == null)
            throw new InputException(String.format("Specified disambiguator=%s has not been loaded. Use one of %s.",name,disambiguators.keySet()));
        return disambiguator;

    }

//    public static Spotter getSpotter(SpotterPolicy policy) throws InputException {
//        Spotter spotter = spotters.get(policy);
//        if (spotters.size()==0 || spotter==null) {
//            throw new InputException(String.format("Specified spotter=%s has not been loaded. Use one of %s.",policy,spotters.keySet()));
//        }
//        return spotter;
//    }
//
//    public static ParagraphDisambiguatorJ getDisambiguator(DisambiguationPolicy policy) throws InputException {
//        ParagraphDisambiguatorJ disambiguator = disambiguators.get(policy);
//        if (disambiguators.size() == 0 || disambiguators == null)
//            throw new InputException(String.format("Specified disambiguator=%s has not been loaded. Use one of %s.",policy,disambiguators.keySet()));
//        return disambiguator;
//    }

    public static SpotlightConfiguration getConfiguration() {
        return configuration;
    }

    public static TextTokenizer getTokenizer() {
        return tokenizer;
    }

    public static void setTokenizer(TextTokenizer tokenizer) {
        Server.tokenizer = tokenizer;
    }

    public static String getPrefixedDBpediaURL(DBpediaResource resource) {
        return namespacePrefix + resource.uri();
    }

    public static void setNamespacePrefix(String namespacePrefix) {
        Server.namespacePrefix = namespacePrefix;
    }

    private static void setSparqlExecuter(String endpoint, String graph)
    {
        if (endpoint == null || endpoint.equals(""))  endpoint= "http://dbpedia.org/sparql";
        if (graph == null || graph.equals(""))  endpoint= "http://dbpedia.org";

        Server.sparqlExecuter = new SparqlQueryExecuter(graph, endpoint);
    }

    public static SparqlQueryExecuter getSparqlExecute(){
        return sparqlExecuter;
    }

    private static void setSimilarityThresholds( List<Double> similarityThresholds){
       Server.similarityThresholds =  similarityThresholds;
    }

    public static  List<Double> getSimilarityThresholds(){
       return similarityThresholds;
    }


    public static void initSpotlightConfiguration(String configFileName) throws InitializationException {

        if(configFileName.endsWith(".properties")) {

            initByPropertiesFile(configFileName);

        } else {

            //We are using a model folder:
            initByModel(configFileName);

        }

        LOG.info(String.format("Initiated %d disambiguators.",disambiguators.size()));

        LOG.info(String.format("Initiated %d spotters.",spotters.size()));

    }

    private static void initByPropertiesFile(String configFileName) throws  InitializationException {

        //We are using the old-style configuration file:
        //Initialization, check values
        try {
            configuration = new SpotlightConfiguration(configFileName);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("\n"+ usage);
            System.exit(1);
        }
        // Set static annotator that will be used by Annotate and Disambiguate
        final SpotlightFactory factory  = new SpotlightFactory(configuration);
        setDisambiguators(factory.disambiguators());
        setSpotters(factory.spotters());
        setNamespacePrefix(configuration.getDbpediaResource());
        setSparqlExecuter(configuration.getSparqlEndpoint(), configuration.getSparqlMainGraph());
        setSimilarityThresholds(configuration.getSimilarityThresholds());

    }

    private static void initByModel(String folder) throws InitializationException {

        File modelFolder = null;

        try {
            modelFolder = new File(folder);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("\n"+usage);
            System.exit(1);
        }


        SpotlightModel db = SpotlightModel.fromFolder(modelFolder);

        setNamespacePrefix(db.properties().getProperty("namespace"));
        setTokenizer(db.tokenizer());
        setSpotters(db.spotters());
        setDisambiguators(db.disambiguators());
        setSparqlExecuter(db.properties().getProperty("endpoint", ""),db.properties().getProperty("graph", ""));

    }
}
