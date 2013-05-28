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

package org.dbpedia.spotlight.exceptions;

/**
 * Thrown when trying to execute SPARQL queries.
 * Should appropriately inform users that their SPARQL syntax was wrong, or that the server is down, etc.
 * 
 * @author pablomendes
 */
public class SparqlExecutionException extends AnnotationException {

    public SparqlExecutionException(String msg, Exception e) {
        super(msg,e);
    }

    public SparqlExecutionException(String msg) {
        super(msg);
    }

    public SparqlExecutionException(Exception e) {
        super(e);
    }

}