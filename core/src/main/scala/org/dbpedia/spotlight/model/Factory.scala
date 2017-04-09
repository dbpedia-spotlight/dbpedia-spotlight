/*
 * Copyright 2012 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.model

import collection.JavaConversions._
import org.dbpedia.spotlight.exceptions.{ItemNotFoundException, ConfigurationException}
import org.dbpedia.spotlight.log.SpotlightLog
import scala.collection.JavaConverters._
import org.dbpedia.spotlight.spot.{SpotSelector, AtLeastOneNounSelector, ShortSurfaceFormSelector}
import java.io.File
import org.dbpedia.extraction.util.WikiUtil

/**
 * Class containing methods to create model objects in many different ways
 *
 * @author pablomendes
 * @author Joachim Daiber (Tagger and Spotter methods)
 */
object Factory {
    /*
    * I like this style for the factory. group by return type and offer many .from* methods
    */
    object SurfaceFormOccurrence {
        def from(occ: DBpediaResourceOccurrence) : SurfaceFormOccurrence = {
            new SurfaceFormOccurrence(occ.surfaceForm, occ.context, occ.textOffset)
        }
    }

    object SurfaceForm {
        def fromString(name: String) = {
            new SurfaceForm(name.toString)
        }
        def fromDBpediaResourceURI(uri: String, lowerCased: Boolean): SurfaceForm = {
            // decode URI and truncate trailing parentheses
            val name = WikiUtil.wikiDecode(uri).replaceAll(""" \(.+?\)$""", "")
            fromString(if (lowerCased) name.toLowerCase else name)
        }
        def fromDBpediaResourceURI(resource: DBpediaResource, lowerCased: Boolean): SurfaceForm = {
            fromDBpediaResourceURI(resource.uri, lowerCased)
        }
        def fromWikiPageTitle(pageTitle: String, lowerCased: Boolean): SurfaceForm = {
            fromDBpediaResourceURI(pageTitle, lowerCased)
        }
    }

    object DBpediaResourceOccurrence {
        def from(sfOcc: SurfaceFormOccurrence, resource: DBpediaResource, score: Double) = {
            SpotlightLog.debug(this.getClass, "Factory test: support=%s, ctxScore=%s, sfOcc=%s", resource.support, score, sfOcc)
            new DBpediaResourceOccurrence("",  // there is no way to know this here
                resource,
                sfOcc.surfaceForm,
                sfOcc.context,
                sfOcc.textOffset,
                Provenance.Annotation,
                score,
                -1,         // there is no way to know percentage of second here
                score)      // to be set later
        }
        def from(sfOcc: SurfaceFormOccurrence, fullResource: DBpediaResource, score: Tuple2[Int,Double]) = {
            //TODO can take a mixture as param and use resource.score to mix with the other two scores in Tuple2
            new DBpediaResourceOccurrence("",  // there is no way to know this here
                fullResource, // support is also available from score._1, but types are only in fullResource
                sfOcc.surfaceForm,
                sfOcc.context,
                sfOcc.textOffset,
                Provenance.Annotation,
                score._2,
                -1,         // there is no way to know percentage of second here
                score._2)      // to be set later
        }
        //TODO take a mixture as param?
        def from(sfOcc: SurfaceFormOccurrence, resource: DBpediaResource, score: Tuple3[Int,Double,Double]) = {
            new DBpediaResourceOccurrence("",  // there is no way to know this here
                new DBpediaResource(resource.uri, score._1),
                sfOcc.surfaceForm,
                sfOcc.context,
                sfOcc.textOffset,
                Provenance.Annotation,
                score._2 * score._3,
                -1,         // there is no way to know percentage of second here
                score._2)   // to be set later
        }
    }

    //Workaround for Java:
    def ontologyType() = this.OntologyType
    object OntologyType {

        def fromURI(uri: String) : OntologyType = {
            if (uri.startsWith(DBpediaType.DBPEDIA_ONTOLOGY_PREFIX))
                return new DBpediaType(uri)

            if (uri.startsWith(FreebaseType.FREEBASE_RDF_PREFIX))
                return FreebaseType.fromTypeString(uri)

            if (uri.startsWith(SchemaOrgType.SCHEMAORG_PREFIX))
                return new SchemaOrgType(uri)

            if (uri.startsWith(OpenCycConcept.OPENCYCCONCEPT_PREFIX))
                return new OpenCycConcept(uri)

            if (uri.startsWith(WikidataType.WIKIDATATYPE_PREFIX))
                return new WikidataType(uri)

            if (uri.startsWith(DULType.DULTYPE_PREFIX))
                return new DULType(uri)

            new DBpediaType(uri)
        }

        def fromQName(ontologyType : String) : OntologyType = {
            val r = """^([A-Za-z]*):(.*)""".r
            try {
                val r(prefix, suffix) = ontologyType

                suffix.toLowerCase match {
                    case "unknown" => DBpediaType.UNKNOWN
                    case _ => {     
                        prefix.toLowerCase match {
                            case "d" | "dbpedia"  => new DBpediaType(suffix)
                            case "f" | "freebase" => FreebaseType.fromTypeString(suffix)
                            case "s" | "schema" => new SchemaOrgType(suffix)
                            case "o" | "opencyc" => new OpenCycConcept(suffix)
                            case "w" | "wikidata" => new WikidataType(suffix)
                            case "dul" => new DULType(suffix)
                            case _ => new DBpediaType(ontologyType)
                        }
                    }
                }
                
            }catch{
                //The default type for non-prefixed type strings:
                case e: scala.MatchError => new DBpediaType(ontologyType)
            }
        }

        def fromCSVString(ontologyTypesString: String) : List[OntologyType] = {
//            val ontologyTypes: java.util.List[OntologyType] = new java.util.ArrayList[OntologyType]
//            val types: Array[String] = ontologyTypesString.trim.split(",")
//            for (t <- types) {
//                if (!(t.trim == "")) ontologyTypes.add(Factory.ontologyType.fromQName(t.trim))
//            }
//            ontologyTypes
            val types = ontologyTypesString.split(",").filterNot(t => t.trim.isEmpty)
            if (types.size>0) types.map(Factory.ontologyType.fromQName).toList else List[OntologyType]()
        }
    }

    def paragraph() = Paragraph
    object Paragraph {
        def from(a: AnnotatedParagraph) = {
            new Paragraph(a.id,a.text,a.occurrences.map( dro => Factory.SurfaceFormOccurrence.from(dro)))
        }
        def fromJ(occs: java.util.List[SurfaceFormOccurrence]) = {
            from(occs.asScala.toList)
        }
        def from(occs: List[SurfaceFormOccurrence]) = {
            if (occs.size==0) throw new IllegalArgumentException("Cannot create paragraph out of empty occurrence list.")
            val first = occs.head
            new Paragraph("",first.context,occs.sortBy(o => o.textOffset))
        }
    }


    /*** left here for compatibility with Java. used by IndexEnricher ****/
    def createSurfaceFormFromDBpediaResourceURI(resource: DBpediaResource, lowercased: Boolean) = {
        SurfaceForm.fromDBpediaResourceURI(resource, lowercased)
    }




    object SpotSelector {
        def fromNameList(commaSeparatedNames: String) : List[SpotSelector] = {
            commaSeparatedNames.split(",").flatMap(name => if (name.isEmpty) None else Some(fromName(name.trim))).toList
        }
        def fromName(name: String) : SpotSelector = { //TODO use reflection
            if (name.isEmpty) throw new IllegalArgumentException("You need to pass a SpotSelector name.")
            name match {
                case "ShortSurfaceFormSelector" => new ShortSurfaceFormSelector
                case "AtLeastOneNounSelector" => new AtLeastOneNounSelector
                case _ => throw new ConfigurationException("SpotSelector of name %s has not been configured in the properties file.".format(name))
            }
        }
    }


}


