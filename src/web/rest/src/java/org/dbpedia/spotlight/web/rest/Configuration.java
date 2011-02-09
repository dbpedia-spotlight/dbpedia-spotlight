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
    final static String spotterFile    = "please-specify-spotter-dictionary-file";
    final static String indexDirectory = "please-specify-disambiguation-index-directory";

    final static String DEFAULT_TEXT = "";
    final static String DEFAULT_CONFIDENCE = "0.3";
    final static String DEFAULT_SUPPORT = "30";
    final static String DEFAULT_TYPES = "";
    final static String DEFAULT_SPARQL = "";
    final static String DEFAULT_POLICY = "whitelist";
    final static String DEFAULT_COREFERENCE_RESOLUTION = "true";

}
