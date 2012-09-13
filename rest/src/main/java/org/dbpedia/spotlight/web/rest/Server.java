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
import org.dbpedia.spotlight.annotate.Annotator;
import org.dbpedia.spotlight.disambiguate.Disambiguator;
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguatorJ;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.SpotlightFactory;
import org.dbpedia.spotlight.model.SpotterConfiguration;
import org.dbpedia.spotlight.spot.Spotter;
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy;
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
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

    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException, ClassNotFoundException, InitializationException {

        //Initialization, check values
        try {
            String configFileName = args[0];
            configuration = new SpotlightConfiguration(configFileName);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("\n"+usage);
            System.exit(1);
        }

        URI serverURI = new URI(configuration.getServerURI());       // "http://localhost:"+args[0]+"/rest/"
        //ExternalUriWadlGeneratorConfig.setUri(configuration.getServerURI()); //TODO get another parameter, maybe getExternalServerURI since Grizzly will use this in order to find out to which port to bind

        // Set static annotator that will be used by Annotate and Disambiguate
        final SpotlightFactory factory = new SpotlightFactory(configuration);
        setDisambiguators(factory.disambiguators());
        setSpotters(factory.spotters());

        LOG.info(String.format("Initiated %d disambiguators.",disambiguators.size()));
        LOG.info(String.format("Initiated %d spotters.",spotters.size()));

        final Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
        initParams.put("com.sun.jersey.config.property.packages", "org.dbpedia.spotlight.web.rest.resources");
        initParams.put("com.sun.jersey.config.property.WadlGeneratorConfig", "org.dbpedia.spotlight.web.rest.wadl.ExternalUriWadlGeneratorConfig");


        SelectorThread threadSelector = GrizzlyWebContainerFactory.create(serverURI, initParams);
        threadSelector.start();

        System.err.println("Server started in " + System.getProperty("user.dir") + " listening on " + serverURI);

        //Open browser
        try {
            String example1 = "annotate?text=At%20a%20private%20dinner%20on%20Friday%20at%20the%20Canadian%20Embassy,%20finance%20officials%20from%20seven%20world%20economic%20powers%20focused%20on%20the%20most%20vexing%20international%20economic%20problem%20facing%20the%20Obama%20administration.%20Over%20seared%20scallops%20and%20beef%20tenderloin,%20Treasury%20Secretary%20Timothy%20F.%20Geithner%20urged%20his%20counterparts%20from%20Europe,%20Canada%20and%20Japan%20to%20help%20persuade%20China%20to%20let%20its%20currency,%20the%20renminbi,%20rise%20in%20value%20a%20crucial%20element%20in%20redressing%20the%20trade%20imbalances%20that%20are%20threatening%20recovery%20around%20the%20world.%20But%20the%20next%20afternoon,%20the%20annual%20meetings%20of%20the%20International%20Monetary%20Fund%20ended%20with%20a%20tepid%20statement%20that%20made%20only%20fleeting%20and%20indirect%20references%20to%20the%20simmering%20currency%20tensions&confidence=0.2&support=20";
            String example2 = "annotate?text=Brazilian%20oil%20giant%20Petrobras%20and%20U.S.%20oilfield%20service%20company%20Halliburton%20have%20signed%20a%20technological%20cooperation%20agreement,%20Petrobras%20announced%20Monday.%20%20%20%20The%20two%20companies%20agreed%20on%20three%20projects:%20studies%20on%20contamination%20of%20fluids%20in%20oil%20wells,%20laboratory%20simulation%20of%20well%20production,%20and%20research%20on%20solidification%20of%20salt%20and%20carbon%20dioxide%20formations,%20said%20Petrobras.%20Twelve%20other%20projects%20are%20still%20under%20negotiation.&confidence=0.0&support=0";
            URI example = new URI(serverURI.toString() + example2);

            java.awt.Desktop.getDesktop().browse(example);
        }
        catch (Exception e) {
            System.err.println("Could not open browser. " + e);
        }

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

}
