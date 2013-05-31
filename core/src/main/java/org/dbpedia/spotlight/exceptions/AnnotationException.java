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
 * Used by (external) annotation clients to communicate an error when trying to annotate text.
 *
 * @author pablomendes
 */
public class AnnotationException extends Exception {

    public AnnotationException(String msg, Exception e) {
        super(msg,e);
    }

    public AnnotationException(String msg) {
        super(msg);
    }

    public AnnotationException(Exception e) {
        super(e);
    }

}