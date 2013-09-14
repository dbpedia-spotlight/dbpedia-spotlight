package org.dbpedia.spotlight.feed

import hu.sztaki.pedia.uima.consumer.util.IExternalProcessor
import org.apache.uima.SentenceAnnotation
import org.apache.uima.WikiArticleAnnotation
import org.apache.uima.WikiCategoryAnnotation
import org.apache.uima.WikiLinkAnnotation
import org.apache.uima.cas.text.AnnotationIndex
import org.apache.uima.jcas.tcas.Annotation
import org.dbpedia.spotlight.log.SpotlightLog
import java.io.File
import hu.sztaki.pedia.uima.reader.util.{WikiIRCBot, WikiArticleFilter}
import hu.sztaki.pedia.uima.RunCPE
import collection.mutable._
import java.util.regex.Pattern
import org.dbpedia.spotlight.model._

/**
 * Feed of paragraphs of wikipedia updates, which is an implementation of wikipedia harvester's external processor.
 * The first entry is the article the update was made in.
 */
class WikipediaUpdateFeed extends Feed[(DBpediaResource, Set[DBpediaResourceOccurrence], Set[DBpediaCategory], Text)](false) with IExternalProcessor {
    protected var interval = Long.MaxValue

    override def initialize(argv: Array[String]) {
        System.out.println("Init params:")
        for (arg <- argv) {
            System.out.println(arg)
        }
        interval = argv(0).toLong
    }

    def act {}

    override def processAnnotations(annotationIndexMap: java.util.Map[java.lang.Integer, AnnotationIndex[Annotation]], sofaTextToName: java.util.Map[String, String]) {
        val articleIndex: AnnotationIndex[Annotation] = annotationIndexMap.get(WikiArticleAnnotation.`type`)

        val articleAnnotation: WikiArticleAnnotation = articleIndex.iterator().get.asInstanceOf[WikiArticleAnnotation]

        val article = new DBpediaResource(articleAnnotation.getTitle)

        var links = Map[DBpediaResource, (Int, Int)]()
        var categories = Set[DBpediaCategory]()
        var text = ""

        val linkIt = annotationIndexMap.get(WikiLinkAnnotation.`type`).iterator()
        while (linkIt.hasNext) {
            val annotation = linkIt.next().asInstanceOf[WikiLinkAnnotation]
            if (annotation.getTitle != null)
                links += (new DBpediaResource(annotation.getTitle) ->(annotation.getBegin, annotation.getEnd))
        }

        val sentenceIt = annotationIndexMap.get(SentenceAnnotation.`type`).iterator()

        while (sentenceIt.hasNext) {
            val annotation = sentenceIt.next().asInstanceOf[SentenceAnnotation]
            if (annotation.getCoveredText != null)
                text += annotation.getCoveredText
        }

        val catIt = annotationIndexMap.get(WikiCategoryAnnotation.`type`).iterator()
        while (catIt.hasNext) {
            val annotation = catIt.next().asInstanceOf[WikiCategoryAnnotation]
            if (annotation.getName != null)
                categories += (new DBpediaCategory(annotation.getName))
        }

        var offset = 0
        var result = List[(Text, Set[DBpediaResourceOccurrence])]()

        val notAllowedPattern = Pattern.compile("Category:.*")
        var paragraphCtr = 0
        var occCount = 0

        text.split("\n").foreach(paragraph => {
            paragraphCtr += 1
            offset = text.indexOf(paragraph)
            result +:=
                ((new Text(paragraph), Set[DBpediaResourceOccurrence]() ++ links.filter {
                    case (_, (begin, end)) => begin >= offset && begin < offset + paragraph.length
                }.foldLeft(Set[DBpediaResourceOccurrence]()) {
                    case (set, (resource, (begin, end))) => {
                        occCount += 1
                        set + new DBpediaResourceOccurrence(
                            article.uri + "-p" + paragraphCtr + "l" + occCount,
                            resource,
                            new SurfaceForm(text.substring(begin, end + 1)),
                            new Text(paragraph),
                            begin - offset,
                            Provenance.Wikipedia
                        )
                    }
                }))

        })
        result = result.filterNot(_._2.isEmpty)

        result.foreach {
            case (paragraph, resources) =>
                notifyListeners((article, resources, categories, paragraph))
        }
    }
}

/**
 * Starts the wikipedia update feed
 */
object WikipediaUpdateFeed {

    /**
     * example params: en.wikipedia localhost 8080 enwikipediaorgtest en resources/articlefilter/redirects.list resources/articlefilter/nonarticle_titles_prefixes.list descriptors/CPE/HTTPCR_parser_wst_category_externalConsumer_CPE.xml YourWikiUser YourWikiPassword
     * <br/><br/>
     * see: {@link http://code.google.com/p/sztakipedia-harvester/source/browse/trunk/hu.sztaki.pedia.harvester/src/hu/sztaki/pedia/uima/reader/standalone/IRCReader.java}<br/>
     * see: {@link http://code.google.com/p/sztakipedia-harvester/source/browse/trunk/hu.sztaki.pedia.harvester/src/hu/sztaki/pedia/uima/RunCPE.java}<br/>
     * see: {@link http://code.google.com/p/sztakipedia-harvester/wiki/HowtoExternal}
     * @param domain
     * @param destinationHostname
     * @param destinationPort
     * @param applicationName
     * @param language
     * @param redirectsPath
     * @param nonArticleTitlesPath
     * @param configurationXml
     * @param apiUser
     * @param apiPassword
     */
    def startFeed(domain: String, destinationHostname: String, destinationPort: Int,
                  applicationName: String, language: String, redirectsPath: String,
                  nonArticleTitlesPath: String, configurationXml: String, apiUser: String = null, apiPassword: String = null) {

        val domainUrl = domain + ".org"
        val ircChannel = "#" + domain

        val botThread = new Thread(new Runnable {
            @Override def run {
                var ircBot: WikiIRCBot = null
                try {
                    val redirectsFile = new File(redirectsPath)
                    val nonArticleTitlesFile = new File(nonArticleTitlesPath)
                    var articleFilter: WikiArticleFilter = null
                    if (redirectsFile.exists && nonArticleTitlesFile.exists) {
                        try {
                            articleFilter = new WikiArticleFilter(nonArticleTitlesFile, redirectsFile)
                        }
                        catch {
                            case e => {
                                articleFilter = null
                                SpotlightLog.error(this.getClass, "Error creating WikiArticleFilter %s", e)
                            }
                        }
                        ircBot = new WikiIRCBot(ircChannel, domainUrl, destinationHostname, destinationPort, articleFilter, applicationName, language, apiUser, apiPassword)
                        ircBot.start
                    }
                    else {
                        SpotlightLog.error(this.getClass, "Files %s , and/or %s does not exist! Exiting!", redirectsPath, nonArticleTitlesPath)
                    }
                }
                catch {
                    case e => {
                        e.printStackTrace
                        System.exit(-1)
                    }
                }
            }
        })

        val feedThread = new Thread(new Runnable {
            def run() {
                new RunCPE(Array(configurationXml))
            }
        })

        feedThread.start()
        botThread.start()
    }

}
