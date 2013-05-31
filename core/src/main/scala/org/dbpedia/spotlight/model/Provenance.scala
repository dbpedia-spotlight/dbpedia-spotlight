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

package org.dbpedia.spotlight.model

/**
 * Sources of Occurrences.
 */
object Provenance extends Enumeration
{
    val Wikipedia : Value = Value("Wikipedia")
    val Web : Value = Value("Web")
    val Annotation : Value = Value("Annotation")
    val Manual : Value = Value("Manual")    // constructed example (for testing etc.)
    val Undefined : Value = Value("Undefined")
//    class ProvenanceValue(name: String, var score : Double) extends Val(nextId, name)
//    protected final def Value(name: String, score : Double): ProvenanceValue = new ProvenanceValue(name,score)
}



