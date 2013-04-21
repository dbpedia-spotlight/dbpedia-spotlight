package org.dbpedia.spotlight.spot;

import junit.framework.TestCase
import lingpipe.AnnotationTokenizerFactory
import com.aliasi.dict.{DictionaryEntry, MapDictionary, ExactDictionaryChunker}
import java.io.File
import scala.collection.JavaConversions._
import io.Source
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
;

class JAnnotationTokenizerFactoryTest extends TestCase {

    def testLargeDictIter() {
        val inputFile = new File("/data/dbpedia-spotlight/evalSpotters/surfaceForms.set")
        val sfLines = Source.fromFile(inputFile).getLines().toList.map(_.toLowerCase)

        val text = "El Barça de Tito Vilanova suma per victòries els cinc primers partits de Lliga, després de " +
            "superar la Reial Societat (5-1), l'Osasuna (1-2), el València (1-0), el Getafe (1-4) i el Granada (2-0)." +
            "D'aquesta manera, en la propera jornada (dissabte al camp del Sevilla), l'equip té l'oportunitat " +
            "d'igualar la millor arrancada de la història del Club superats sis partits en el campionat. " +
            "Fins ara, són les sis victòries inicials obtingudes les campanyes 2009/10, 1997/98 1990/91 i 1929/30. " +
            "En tres dels quatre casos, el Barça acabaria campió."

        //for (i <- 1 until sfLines.length/1000) {
            //println(">> " + i)

            val dictionary = new MapDictionary[String]()
            val validChunks = sfLines
                //.slice(0, i*1000)
                .toSet[String]
            validChunks.foreach(w => dictionary.addEntry(new DictionaryEntry[String](w, "")))

            val dictionaryChunker = new ExactDictionaryChunker(dictionary,
                //new JAnnotationTokenizerFactory(),
                IndoEuropeanTokenizerFactory.INSTANCE,
                //AnnotationTokenizerFactory,
                false,        // find all matches, including overlapping ones?
                false)        // case-sensitive matching?

            println("chunking...")
            val chunks = dictionaryChunker.chunk(text).chunkSet()
                    .toList.sortBy(_.start)
                    .map(chunk => { text.substring(chunk.start, chunk.end) })

            val invalidChunks = chunks.filter(c => !validChunks.contains(c.toLowerCase))
            if (!invalidChunks.isEmpty) {
                invalidChunks.foreach(c => println("invalid chunk: " + c))
                assert(false, "there are invalid chunks")
            }
        //}

    }

}

