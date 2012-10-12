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

class Candidate(val surfaceForm: SurfaceForm, val resource: DBpediaResource, val support: Int)
{

  def this(surfaceForm: SurfaceForm, resource: DBpediaResource) {
    this(surfaceForm, resource, 0)
  }

  override def equals(obj : Any) : Boolean = {
      obj match {
          case that: Candidate => surfaceForm.equals(that.surfaceForm) && resource.equals(that.resource)
          case _ => obj.equals(this)
      }
  }
  override def hashCode() = (surfaceForm.name + "/" + resource.uri).hashCode
  override def toString = "Candidate["+surfaceForm.name+", "+resource.uri + { if (support > 0) ", " + support else "" } + "]"

  def prior: Double = this.support / this.surfaceForm.annotatedCount.toDouble

}


object Candidate
{
    def apply(surfaceForm : SurfaceForm, resource: DBpediaResource) = {
        new Candidate(surfaceForm, resource)
    }
}