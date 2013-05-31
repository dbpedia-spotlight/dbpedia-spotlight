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
 * Thrown when the user provides not acceptable input (e.g. too short of a text).
 * 
 * @author maxjakob
 */
public class InputException extends AnnotationException {

    public InputException(String msg, Exception e) {
        super(msg,e);
    }

    public InputException(String msg) {
        super(msg);
    }

    public InputException(Exception e) {
        super(e);
    }

}