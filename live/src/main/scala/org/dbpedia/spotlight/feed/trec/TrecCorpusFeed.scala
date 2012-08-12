package org.dbpedia.spotlight.feed.trec

import org.dbpedia.spotlight.model.{Text, DBpediaResource}
import org.dbpedia.spotlight.feed.Feed
import java.util.Date
import java.io.{FilenameFilter, File}
import org.apache.commons.lang.time.DateUtils
import scala.collection.mutable._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.io.TrecStreamItemSource

/**
 * Simple feed implementation that loads trec-kba streaming corpus 2012 from disk and notifies its listeners of annotated
 * content.
 *
 * @param corpusDir path to corpus
 * @param startDate date of where to begin the stream
 * @param endDate date of the end of streaming
 * @param trecJudgmentsFile file which contains annotations
 * @param clear if true, start from scratch, i.e. ignore 'seen' files
 *
 * @author Dirk Weissenborn
 */
class TrecCorpusFeed(val corpusDir: File, val startDate: Date, val endDate: Date, val trecJudgmentsFile: File, val clear: Boolean = false) extends Feed[(Set[DBpediaResource], Text)](true) {

    private val LOG = LogFactory.getLog(getClass)

    class TrecAnnotation(val entity: String, val score: Int) {
        override def equals(obj: Any): Boolean = {
            obj match {
                case that: TrecAnnotation => this.entity.equals(that.entity)
                case _ => obj.equals(this)
            }
        }
    }

    private val annotations = Map[String, Set[TrecAnnotation]]()
    var targetEntities = Set[DBpediaResource]()
    private var stop = false

    def stopFeed {
        stop = true
    }

    // NIST-TREC	annotators	1317998536-f91195671001a20f5d53add7bdbd0591	Nassim_Nicholas_Taleb	0	-1	1
    // Load annotations into memory
    scala.io.Source.fromFile(trecJudgmentsFile).getLines.foreach(line => {
        if (!line.isEmpty && !line.trim.startsWith("#")) {
            val split = line.trim.split("\t")
            annotations.getOrElseUpdate(split(2), Set[TrecAnnotation]()) += (new TrecAnnotation(split(3), split(5).toInt))
            targetEntities += (new DBpediaResource(split(3)))
        }
    })

    def act {
        val dirFilter = new FilenameFilter {
            def accept(p1: File, p2: String): Boolean = p1.isDirectory
        }

        var dayDirs: List[(Date, File)] = null

        while ((dayDirs == null || dayDirs.size > 0) && !stop) {
            if (dayDirs == null) {
                dayDirs = corpusDir.listFiles(dirFilter).foldLeft(List[(Date, File)]())(
                    (list, file) => {
                        (DateUtils.parseDate(file.getName, Array("yyyy-MM-dd-HH")), file) :: list
                    }).filter {
                    case (date, _) => date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0
                }.sortBy(_._1)
            }
            else
                dayDirs = corpusDir.listFiles(dirFilter).foldLeft(List[(Date, File)]())(
                    (list, file) => {
                        (DateUtils.parseDate(file.getName, Array("yyyy-MM-dd-HH")), file) :: list
                    }).filter {
                    case (date, _) => date.compareTo(dayDirs.last._1) > 0 && date.compareTo(endDate) <= 0
                }.sortBy(_._1)

            val iterator = dayDirs.iterator

            while (iterator.hasNext && !stop) {
                val (_, dir) = iterator.next()
                if (clear || !dir.list.contains("seen")) {
                    //sleep until download is done
                    while (!dir.list.contains("done"))
                        Thread.sleep(10000)

                    LOG.info("Processing " + dir.getName)

                    TrecStreamItemSource.fromDirectory(dir).foreach(streamItem => {
                        val text = new String(streamItem.getBody.getCleansed, streamItem.getBody.getEncoding)
                        if (!text.equals("")) {
                            val streamAnnotations = annotations.getOrElse(streamItem.getStream_id, null)
                            if (streamAnnotations != null) {
                                val resources = streamAnnotations.foldLeft(Set[DBpediaResource]())((resources, annotation) => {
                                    if (annotation.score >= 0)
                                        resources + (new DBpediaResource(annotation.entity))
                                    else
                                        resources
                                })

                                if (resources.size > 0) {
                                    notifyListeners((resources, new Text(text)))
                                }
                            }
                        }
                    })
                    new File(dir, "seen").createNewFile()
                }
                else LOG.info(dir.getName + " was already processed")
            }
        }
    }


}
