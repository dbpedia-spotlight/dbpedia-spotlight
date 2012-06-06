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

import org.dbpedia.spotlight.string.ModifiedWikiUtil


class SurfaceForm(var name : String) extends Serializable
{

  var support: Int = 0
  var id: Int = 0

  def this(name: String, id: Int, support: Int) {
    this(name)
    this.id = id
    this.support = support
  }

  name = name.replace("’", "'")

  name = if (ModifiedWikiUtil.isEncoded(name)) {
    ModifiedWikiUtil.wikiDecode(name)
  }
  else {
    ModifiedWikiUtil.cleanSpace(name)
  }


  //TODO: instead of equalsIgnoreCase, fix the Spotter // (should be fixed now)
  override def equals(that : Any) = {
    that match {
      case t: SurfaceForm => name.equalsIgnoreCase(that.asInstanceOf[SurfaceForm].name)
      case _ => false
    }
  }

  override def hashCode() : Int = {
    (if (name != null) name.hashCode else 0)
  }

  override def toString = "SurfaceForm["+name+"]"

}
