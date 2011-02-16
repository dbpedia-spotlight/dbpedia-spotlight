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

package org.dbpedia.spotlight.util

import org.dbpedia.spotlight.io.OccurrenceSource
import org.dbpedia.spotlight.model.{Text, DBpediaResource, SurfaceForm, DBpediaResourceOccurrence}
import org.dbpedia.spotlight.string.ContextExtractor

/**
 * Filters sources of occurrences and WikiPageContext so that they contain only "good" data.
 *
 * Usage:
 * val occFilter = ConfigProperties.occurrenceFilter
 * for (cleanOccurrence <- occFilter.filter(someOccurrenceSource)) {
 *     // process
 * }
 *
 */

class OccurrenceFilter(val maximumSurfaceFormLength : Int = Integer.MAX_VALUE,
                       val minimumParagraphLength : Int = 0,
                       val maximumParagraphLength : Int = Integer.MAX_VALUE,
                       val redirectsTC : Map[String,String] = Map.empty,
                       val conceptURIs : Set[String] = Set.empty,
                       //var surfaceForms : Map[String,List[String]] = Map[String,List[String]](),  // uri -> List(Sfs)
                       val contextExtractor : ContextExtractor = null,
                       val lowerCaseSurfaceForms : Boolean = false)
{

    /**
     * Returns an Occurrence source that is free of "bad" occurrences.
     */
    def filter(occurrenceSource : OccurrenceSource) : OccurrenceSource =
    {
        new FilteredOccurrenceSource(occurrenceSource)
    }

//    /**
//      * Disregard surface forms that do not obey the configuration constraints.
//      */
//    def isGoodSurfaceFormForResource(sf : String, uri : String) : Boolean =
//    {
//        var sfString = sf
//        var validSurfaceForms = surfaceForms.get(uri).getOrElse(List[String]())
//        if (lowerCaseSurfaceForms) {
//            sfString = sf.toLowerCase
//            validSurfaceForms = validSurfaceForms.map(_.toLowerCase)
//        }
//        (sf.length <= maximumSurfaceFormLength) && (surfaceForms.nonEmpty && (validSurfaceForms contains sfString))
//    }
//
//    def isGoodSurfaceFormForResource(sf : SurfaceForm, res : DBpediaResource) : Boolean =
//    {
//        isGoodSurfaceFormForResource(sf.name, res.uri)
//    }

    /**
      * Disregard links to URIs that do not obey the configuration constraints.
      */
    def isGoodURI(uri : String) : Boolean =
    {
        conceptURIs.nonEmpty && (conceptURIs contains uri)
    }

    def isGoodResource(res : DBpediaResource) : Boolean =
    {
        isGoodURI(res.uri)
    }

    /**
      * Disregard Texts that do not obey the configuration constraints.
      */
    def isGoodText(textString : String) : Boolean =
    {
        if (textString.length < minimumParagraphLength || textString.length > maximumParagraphLength)
            return false

        true
    }
    def isGoodText(text : Text) : Boolean =
    {
        isGoodText(text.text)    
    }

    /**
      * Disregard Occurrences that do not obey the configuration constraints.
      */
    def isGoodOccurrence(occ : DBpediaResourceOccurrence) : Boolean =
    {
        if (!isGoodText(occ.context))
            return false

        if (!isGoodResource(occ.resource))
            return false

//        if (!isGoodSurfaceFormForResource(occ.surfaceForm, occ.resource))
//            return false

        //if (occ.textOffset < 0)
        //   return false

        true
    }

    /**
     * If the URI refers to a redirect page, follow the redirect chain until the end to the resource URI
     * and return a new DBpediaResourceOccurrence.
     */
    def resolveRedirects(occ : DBpediaResourceOccurrence) : DBpediaResourceOccurrence = {
        redirectsTC.get(occ.resource.uri) match {
            case Some(uri) => {
                val resolvedResource = new DBpediaResource(uri)
                new DBpediaResourceOccurrence(occ.id, resolvedResource, occ.surfaceForm, occ.context, occ.textOffset, occ.provenance)
            }
            case None => occ
        }
    }


    /**
     * Wrapper class that applies all filters when iterating.
     */
    private class FilteredOccurrenceSource(occurrenceSource : OccurrenceSource) extends OccurrenceSource {

        override def foreach[U](f : DBpediaResourceOccurrence => U) {

            for (occ <- occurrenceSource) {

                var thisOcc = resolveRedirects(occ)

//                // make title surface form if the found surface form is not allowed (too noisy) for this URI
//                if (!isGoodSurfaceFormForResource(thisOcc.surfaceForm, thisOcc.resource)) {
//                    val titleAsSurfaceForm = SurrogatesUtil.getSurfaceForm(thisOcc.resource)
//                    thisOcc = new DBpediaResourceOccurrence(occ.id, occ.resource, titleAsSurfaceForm, occ.context, -1, occ.provenance)
//                }

                if (contextExtractor != null) {
                    thisOcc = contextExtractor.narrowContext(thisOcc)
                }

                if (isGoodOccurrence(thisOcc)) {
                    if (lowerCaseSurfaceForms) {
                        thisOcc = new DBpediaResourceOccurrence(thisOcc.id, thisOcc.resource, new SurfaceForm(thisOcc.surfaceForm.name.toLowerCase), thisOcc.context, thisOcc.textOffset, thisOcc.provenance)
                    }
                    
                    f( thisOcc )
                }
            }
        }
    }

}