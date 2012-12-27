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
 * Text represents an input string for spotlight. It can be a document, paragraph or just a text snippet.
 * It basically holds a string and an optional identifier. Subclasses can add more attributes.
 * Our pipeline operates on objects of this class by adding annotations.
 */

class Text(var text : String) extends HasFeatures {
    // do some clean up on the text
    text = text.replace("’", "'")

    override def equals(that : Any) = {
        that match {
            case t: Text => this.text.equals(t.text)
            case _ => false
        }
    }

    override def hashCode() : Int = {
      (if (text != null) text.hashCode else 0)
    }
    
    override def toString = "Text["+text+"]"
}
