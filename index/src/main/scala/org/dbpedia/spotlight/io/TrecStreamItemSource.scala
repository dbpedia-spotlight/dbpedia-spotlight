package org.dbpedia.spotlight.io

import java.io.{FileInputStream, FilenameFilter, File}
import org.dbpedia.spotlight.live.model.trec.{ThriftReader, StreamItem}
import org.dbpedia.spotlight.model.{DBpediaCategory, DBpediaResourceOccurrence}
import org.dbpedia.spotlight.live.model.trec.ThriftReader.TBaseCreator
import org.apache.thrift.{TFieldIdEnum, TBase}
import org.tukaani.xz.XZInputStream
import collection.TraversableOnce

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 7/19/12
 * Time: 4:11 PM
 * To change this template use File | Settings | File Templates.
 */

object TrecStreamItemSource {

  def fromDirectory(dir:File) : Traversable[StreamItem] = new TrecStreamItemSource(dir)

  private class TrecStreamItemSource(dir:File) extends Traversable[StreamItem] {
    override def foreach[U](f : StreamItem => U)  = {
      val xzFilter = new FilenameFilter {
        def accept(p1: File, p2: String): Boolean = p2.endsWith(".xz")
      }
      val reader = new ThriftReader(new TBaseCreator {
        def create(): TBase[_ <: TBase[_, _], _ <: TFieldIdEnum] = new StreamItem()
      })

      dir.listFiles(xzFilter).foreach(corpusFile => {
        val inputStream = new XZInputStream(new FileInputStream(corpusFile))
        reader.open(inputStream)

        while (reader.hasNext) {
          try {
            f(reader.read().asInstanceOf[StreamItem])
          }
        }
      })

    }
  }

}
