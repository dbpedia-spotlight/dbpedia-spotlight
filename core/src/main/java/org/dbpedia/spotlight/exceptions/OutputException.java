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
 * Thrown when we fail in creating an output for the user
 * 
 * @author pablomendes
 */
public class OutputException extends AnnotationException {

    public OutputException(String msg, Exception e) {
        super(msg,e);
    }

    public OutputException(String msg) {
        super(msg);
    }

    public OutputException(Exception e) {
        super(e);
    }

}