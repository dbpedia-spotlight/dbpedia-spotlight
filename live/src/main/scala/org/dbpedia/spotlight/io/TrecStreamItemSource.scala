package org.dbpedia.spotlight.io

import java.io.{FileInputStream, FilenameFilter, File}
import org.dbpedia.spotlight.model.trec.{ThriftReader, StreamItem}
import org.dbpedia.spotlight.model.trec.ThriftReader.TBaseCreator
import org.apache.thrift.{TFieldIdEnum, TBase}
import org.tukaani.xz.XZInputStream

/**
 * Class that allows iterating over all corpus items of the Trec KBA 2012 streaming corpus
 *
 * @author dirk
 */
object TrecStreamItemSource {

    def fromDirectory(dir: File): Traversable[StreamItem] = new TrecStreamItemSource(dir)

    private class TrecStreamItemSource(dir: File) extends Traversable[StreamItem] {
        override def foreach[U](f: StreamItem => U) = {
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
