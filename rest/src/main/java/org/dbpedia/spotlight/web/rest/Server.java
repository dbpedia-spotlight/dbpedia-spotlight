/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.web.rest;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.HttpServer;
import org.dbpedia.spotlight.annotate.Annotator;
import org.dbpedia.spotlight.annotate.DefaultAnnotator;
import org.dbpedia.spotlight.disambiguate.Disambiguator;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Instantiates Web Service that will execute annotation and disambiguation tasks.
 *
 * @author maxjakob, pablomendes
 */

public class Server {

    // Server reads configuration parameters into this static configuration object that will be used by other classes downstream
    protected static SpotlightConfiguration configuration;

    // Server will hold a singleton annotator that will be used by all Annotate and Disambiguate objects
    protected static Annotator annotator;

    private static volatile Boolean running = true;

    static String usage = "usage: java -jar dbpedia-spotlight.jar org.dbpedia.spotlight.web.rest.Server [config file]"
                        + "   or: mvn scala:run \"-DaddArgs=[config file]\"";

    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException, ClassNotFoundException, ConfigurationException {

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
        File indexDir = new File(configuration.getIndexDirectory()); //"/home/pablo/web/dbpedia36data/2.9.3/small/Index.wikipediaTraining.Merged.SnowballAnalyzer.DefaultSimilarity"
        File spotterFile = new File(configuration.getSpotterFile()); //"/home/pablo/eval/manual/Eval.spotterDictionary"

        // Set static annotator that will be used by Annotate and Disambiguate
        setAnnotator(new DefaultAnnotator(spotterFile, indexDir));

        // CHANGED TO GRIZZLY CONTAINER SO THAT WE CAN GET IP INSIDE THE RESOURCES
        /*
        ResourceConfig resources = new ClassNamesResourceConfig(
                Class.forName("org.dbpedia.spotlight.web.rest.resources.Annotate"),
                Class.forName("org.dbpedia.spotlight.web.rest.resources.Disambiguate"));
        resources.getProperties().put("com.sun.jersey.config.property.WadlGeneratorConfig", "org.dbpedia.spotlight.web.rest.wadl.ExternalUriWadlGeneratorConfig");
        HttpServer server = HttpServerFactory.create(serverURI, resources);
        server.start();
        */

        final Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
        initParams.put("com.sun.jersey.config.property.packages", "org.dbpedia.spotlight.web.rest.resources");
        initParams.put("com.sun.jersey.config.property.WadlGeneratorConfig", "org.dbpedia.spotlight.web.rest.wadl.ExternalUriWadlGeneratorConfig");

        SelectorThread threadSelector = GrizzlyWebContainerFactory.create(serverURI, initParams);
        threadSelector.start();

        System.err.println("Server started in " + System.getProperty("user.dir") + " listening on " + serverURI);

        //Open browser
        try {
            URI example = new URI(serverURI.toString() + "annotate?text=At%20a%20private%20dinner%20on%20Friday%20at%20the%20Canadian%20Embassy,%20finance%20officials%20from%20seven%20world%20economic%20powers%20focused%20on%20the%20most%20vexing%20international%20economic%20problem%20facing%20the%20Obama%20administration.%20Over%20seared%20scallops%20and%20beef%20tenderloin,%20Treasury%20Secretary%20Timothy%20F.%20Geithner%20urged%20his%20counterparts%20from%20Europe,%20Canada%20and%20Japan%20to%20help%20persuade%20China%20to%20let%20its%20currency,%20the%20renminbi,%20rise%20in%20value%20a%20crucial%20element%20in%20redressing%20the%20trade%20imbalances%20that%20are%20threatening%20recovery%20around%20the%20world.%20But%20the%20next%20afternoon,%20the%20annual%20meetings%20of%20the%20International%20Monetary%20Fund%20ended%20with%20a%20tepid%20statement%20that%20made%20only%20fleeting%20and%20indirect%20references%20to%20the%20simmering%20currency%20tensions&confidence=0.2&support=20");
            java.awt.Desktop.getDesktop().browse(example);
        }
        catch (Exception e) {
            System.err.println("Could not open browser. " + e);
        }

        while(running) {
            Thread.sleep(100);
        }

        //Stop the HTTP server
        //server.stop(0);
        threadSelector.stopEndpoint();
        System.exit(0);

    }

    private static void setAnnotator(Annotator a) throws ConfigurationException {
        if (annotator == null)
            annotator = a;
        else
            throw new ConfigurationException("Trying to overwrite singleton Server.annotator. Something fishy happened!");
    }

    public static Annotator getAnnotator() {
        return annotator;
    }

    public static Disambiguator getDisambiguator() {
        return annotator.disambiguator();
    }

    public static SpotlightConfiguration getConfiguration() {
        return configuration;
    }

}
