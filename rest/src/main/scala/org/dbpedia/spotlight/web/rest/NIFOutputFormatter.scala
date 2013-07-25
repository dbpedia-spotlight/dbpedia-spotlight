/**
 * Copyright 2011 DBpedia Spotlight Development Team
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

package org.dbpedia.spotlight.web.rest

import org.dbpedia.spotlight.model.{DBpediaResourceOccurrence, SurfaceFormOccurrence}
import java.util.List
import org.aksw.spotlight.nif.bean.NIFBean
import java.util
import scala.collection.JavaConversions._
import org.aksw.spotlight.nif.business.NIFManager

/**
 * Class that handles the output of DBpedia Spotlight annotations
 * in the NLP Interchange Format (NIF).
 *
 * @author Marcus Nitzschke
 */
object NIFOutputFormatter {

  /**
   * Method for processing the spotlight annotations to NIF format.
   *
   * @param text the original input text
   * @param occs a list of the resource occurrences of the input text
   * @param format requested format
   * @param prefix url
   * @return the NIF representation of the annotated input text
   */
  def fromResourceOccs(text: String, occs: List[DBpediaResourceOccurrence], format: String, prefix: String): String = {

    val list = new util.LinkedList[NIFBean]
    val root = new NIFBean
    root.setURL(prefix)
    root.setContent(text)
    root.setOffset(0)
    root.setSize(text.length)
    list.add(root)

    occs.toList.foreach(occ => {

      var child = new NIFBean
      child.setURL(prefix)
      child.setOffset(occ.textOffset)
      child.setSize(occ.surfaceForm.name.length)
      child.setContent(occ.surfaceForm.name)
      val types = occ.resource.getTypes.map(t => t.typeID).toList
      child.setResourceTypes(types)
      child.setReferenceContextURL(root.getURL)

      list.add(child)
    })

    getNIFOutput(format, new NIFManager(list))
  }

  /**
   * Return a content based on the requested format
   * @param format
   * @param manager
   * @return
   */
  private def getNIFOutput(format:String,manager:NIFManager):String=
  {
    format match {
      case "rdfxml" =>  manager.getRDFxml
      case "ntriples" =>  manager.getNTriples
      case "turtle" =>  manager.getTurtle
      case _ =>  manager.getTurtle
    }
  }


  /**
   * Method for processing the spotlight annotations to NIF format.
   *
   * @param text the original input text
   * @param occs a list of the surface occurrences of the input text
   * @param format requested format
   * @param prefix url
   * @return the NIF representation of the annotated input text
   */
  def fromSurfaceFormOccs(text: String, occs: List[SurfaceFormOccurrence], format: String, prefix: String): String = {

    val list = new util.LinkedList[NIFBean]
    val root = new NIFBean
    root.setURL(prefix)
    root.setContent(text)
    root.setOffset(0)
    root.setSize(text.length)
    list.add(root)

    occs.toList.foreach(occ => {

      var child = new NIFBean
      child.setURL(prefix)
      child.setOffset(occ.textOffset)
      child.setSize(occ.surfaceForm.name.length)
      child.setContent(occ.surfaceForm.name)
      child.setReferenceContextURL(root.getURL)

      list.add(child)
    })

    getNIFOutput(format, new NIFManager(list))

  }

}
