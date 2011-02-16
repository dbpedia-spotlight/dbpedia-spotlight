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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbpedia.spotlight.web.rest;

/**
 *
 * @author pablo
 */
public class Configuration {

        //TODO make this configurable
    final static String spotterFile    = "/home/pablo/web/TitRedDis.spotterDictionary";
    final static String indexDirectory = "/home/pablo/web/DisambigIndex.singleSFs-plusTypes.SnowballAnalyzer.DefaultSimilarity";

    //final static String spotterFile= "/home/pablo/web/dbpedia36data/2.9.3/surface_forms-Wikipedia-TitRedDis.thresh3.spotterDictionary";
    //final static String indexDirectory = "/home/pablo/web/dbpedia36data/2.9.3/Index.wikipediaTraining.Merged.SnowballAnalyzer.DefaultSimilarity";

    final static String DEFAULT_TEXT = "";
    final static String DEFAULT_CONFIDENCE = "0.5";
    final static String DEFAULT_SUPPORT = "30";
    final static String DEFAULT_TYPES = "";
    final static String DEFAULT_SPARQL = "";
    final static String DEFAULT_POLICY = "whitelist";
    final static String DEFAULT_COREFERENCE_RESOLUTION = "true";

}
