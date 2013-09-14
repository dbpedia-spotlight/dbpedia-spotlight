package org.dbpedia.spotlight.msm2013.model

import java.io.{FilenameFilter, File}
import org.dbpedia.spotlight.model.{OntologyType, DBpediaResource}
import org.dbpedia.spotlight.io.NTripleSource
import org.dbpedia.spotlight.model.Factory.OntologyType
import org.dbpedia.spotlight.model.OntologyType
import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.db.disk.JDBMStore

/**
 * Loads NT files containing mappings between URIs and types through rdfs:types relationships
 * @author dirk
 * @author pablomendes
 */
class NTFileTypeMapper(dbFile:File) extends OntologyTypeMapper {

    private val typeMap: JDBMStore[String,String] = new JDBMStore[String,String](dbFile.getAbsolutePath)

    private val TYPE_PREDICATE99 = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
    private val TYPE_PREDICATE00 = "http://www.w3.org/2000/01/rdf-schema#type"

    def loadFromDirOrFile(dirOrFile:File) = {
        if (dirOrFile.exists())
            if(dirOrFile.isDirectory) {
                dirOrFile.listFiles(new FilenameFilter {
                    def accept(p1: File, p2: String) = p2.endsWith(".nt") ||  p2.endsWith(".nq")
                }).foreach(file => {
                    SpotlightLog.info(this.getClass, "Loading file: %s", file.getName)
                    loadFromFile(file)
                })
            }
            else {
                SpotlightLog.info(this.getClass, "Loading file: %s", dirOrFile.getName)
                loadFromFile(dirOrFile)
            }

        typeMap.commit()
        SpotlightLog.info(this.getClass, "Files completely stored into the db file.")
    }

    private def loadFromFile(file: File) = {
        NTripleSource.fromFile(file).foreach {
            case (subj, pred, obj) => {
                if (pred.equals(TYPE_PREDICATE00)||pred.equals(TYPE_PREDICATE99)) {
                    val resource = new DBpediaResource(subj)
                    if (!resource.isExternalURI) {
                        var typeUris = typeMap.get(resource.uri)
                        if (typeUris == null) {
                            typeUris = obj
                        }
                        else
                            typeUris += "\t"+obj

                        typeMap.add(resource.uri,typeUris)
                    }
                }
        }}
    }

    def getTypesForResource(resource: DBpediaResource): Set[OntologyType] = {
        Option(typeMap.get(resource.uri)) match {
            case Some(types) => types.split("\t").foldLeft[Set[OntologyType]](Set[OntologyType]()) ( (acc,uri) => acc + (OntologyType.fromURI(uri)))
            case None => Set[OntologyType]()
        }
    }


}

object NTFileTypeMapper {
    //Small tutorial
    def main(args:Array[String]) {
        val tm = new NTFileTypeMapper(new File("db.file"))
        /* 'types' is a directory with several NT files */
        tm.loadFromDirOrFile(new File("types"))
        val testResult = tm.getTypesForResource(new DBpediaResource("Abraham_Lincoln"))
        println(testResult)
        println("yippi")
    }
}
