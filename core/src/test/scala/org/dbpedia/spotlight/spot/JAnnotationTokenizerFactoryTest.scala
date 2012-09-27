package org.dbpedia.spotlight.spot;

import junit.framework.TestCase
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.util.Version
import com.aliasi.dict.{DictionaryEntry, MapDictionary, Dictionary, ExactDictionaryChunker}
import com.aliasi.util.AbstractExternalizable
import java.io.File
import scala.collection.JavaConversions._
import org.apache.lucene.analysis.br.BrazilianAnalyzer
import org.apache.lucene.analysis.ca.CatalanAnalyzer
import io.Source
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
;

class JAnnotationTokenizerFactoryTest extends TestCase {

    val enDict = new MapDictionary[String]()
    val enWords = Set("Barack Obama", "fish", "oil")
    enWords.foreach(w=>enDict.addEntry(new DictionaryEntry[String](w, "")))

    val ptDict = new MapDictionary[String]()
    val ptWords = Set("Baraquinho", "peixe", "óleo", "l'Osasuna")
    ptWords.foreach(w=>ptDict.addEntry(new DictionaryEntry[String](w, "")))

    def testEnglishTokenizer() {
        val dictionaryChunker = new ExactDictionaryChunker(enDict,
            //IndoEuropeanTokenizerFactory.INSTANCE,  // splits "don't" into "don", "'" and "t"
            // AnnotationTokenizerFactory, //English only
            new JAnnotationTokenizerFactory(new EnglishAnalyzer(Version.LUCENE_36)),
            false,        // find all matches, including overlapping ones?
            false)  // case-sensitive matching?
        val text = "This is a test of English language with oil and Petrobras."
        val test = dictionaryChunker.chunk(text)
        println(test.chunkSet().map(chunk => {
            text.substring(chunk.start, chunk.end)
        }))
    }

    def testPortugueseTokenizer() {
        val dictionaryChunker = new ExactDictionaryChunker(ptDict,
            new JAnnotationTokenizerFactory(new BrazilianAnalyzer(Version.LUCENE_36)),
            false,        // find all matches, including overlapping ones?
            false)        // case-sensitive matching?

        val text = "Este é um teste da língua Portuguesa com l'Osasuna de óleo e Petrobrás."
        val test = dictionaryChunker.chunk(text)
        println(test.chunkSet().map(chunk => {
            text.substring(chunk.start, chunk.end)
        }))
    }

    def testLargeDict() {
        val inputFile = new File("data/surfaceForms.set")
        val dictionary = new MapDictionary[String]()
        val validChunks = Source.fromFile(inputFile).getLines().toSet[String]
        validChunks.foreach(w => dictionary.addEntry(new DictionaryEntry[String](w, "")))

        val serializedFile = new File("data/surfaceForms.set.spotterDictionary")
        AbstractExternalizable.compileTo(dictionary, serializedFile)
        val largeDict =
            AbstractExternalizable.readObject(serializedFile).asInstanceOf[Dictionary[String]]

        val dictionaryChunker = new ExactDictionaryChunker(largeDict,
            new JAnnotationTokenizerFactory(new CatalanAnalyzer(Version.LUCENE_36)),
            false,        // find all matches, including overlapping ones?
            false)        // case-sensitive matching?

        val text = "El Barça de Tito Vilanova suma per victòries els cinc primers partits de Lliga, després de superar la Reial Societat (5-1), l'Osasuna (1-2), el València (1-0), el Getafe (1-4) i el Granada (2-0). D'aquesta manera, en la propera jornada (dissabte al camp del Sevilla), l'equip té l'oportunitat d'igualar la millor arrancada de la història del Club superats sis partits en el campionat. Fins ara, són les sis victòries inicials obtingudes les campanyes 2009/10, 1997/98 1990/91 i 1929/30. En tres dels quatre casos, el Barça acabaria campió."

        val chunks = dictionaryChunker.chunk(text).chunkSet().map(chunk => {
            text.substring(chunk.start, chunk.end)
        })

        chunks.foreach( chunk => if (!validChunks.contains(chunk.toLowerCase)) println(chunk) )
    }


    def testLargeDictIter() {
        val inputFile = new File("data/surfaceForms.set")
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
                //new JAnnotationTokenizerFactory(new CatalanAnalyzer(Version.LUCENE_36)),
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
