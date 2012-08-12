package org.dbpedia.spotlight.run

import org.dbpedia.spotlight.model._
import xml.XML
import org.dbpedia.spotlight.feed._
import org.dbpedia.spotlight.trainer.TopicLiveTrainer
import java.io.{BufferedReader, InputStreamReader, File}
import org.dbpedia.spotlight.db.model.HashMapTopicalPriorStore
import org.apache.commons.logging.LogFactory
import collection.mutable._


object RunIncrementalTopicTraining {

    private val LOG = LogFactory.getLog(getClass)

    /*main(Array("/home/dirk/Dropbox/workspace/dbpedia-spotlight/conf/server.properties",
    "/home/dirk/Dropbox/workspace/dbpedia-spotlight/conf/topic_descriptions.xml","en","10000","0.3","/home/dirk/eval.txt","1"))*/

    /**
     * example: "/.../dbpedia-spotlight/conf/server.properties","/.../dbpedia-spotlight/index/src/main/resources/topic_descriptions.xml","en","10000","0.3","eval.txt","100"
     * @param args topical classifier configuration file (like server.properties), topic descriptions (in resources folder), language tag ("en"), rss feed update interval (in ms),
     *             minimal confidence of training (just train if models prediction is higher than minimal confidence, for details see TopicLiveTrainer),
     *             optional: evaluation file, optional: evaluation interval
     */
    def main(args: Array[String]) {
        val descriptions = TopicDescription.fromDescriptionFile(new File(args(1)))

        val config = new TopicalClassificationConfiguration(args(0))
        val classifier = config.getClassifier
        val languageTag = args(2)

        var trainer: TopicLiveTrainer = null
        if (args.length>6)
          trainer = new TopicLiveTrainer(classifier,args(4).toDouble, new File(args(5)), args(6).toInt)
        else if (args.length>5)
          trainer = new TopicLiveTrainer(classifier,args(4).toDouble, new File(args(5)))
        else
          trainer = new TopicLiveTrainer(classifier,args(4).toDouble)


        val wikiUpdateConfig = getClass.getClassLoader.getResource("pedia_uima_harvester/descriptors/CPE/HTTPCR_parser_wst_category_externalConsumer_CPE.xml").getPath

        val configXml = XML.loadFile(wikiUpdateConfig)
        val port = ((configXml \\ "nameValuePair").find(node => (node \\ "name").head.text.equals("Ports")).get \\ "integer").text.toInt

        WikipediaUpdateFeed.startFeed(
            languageTag + ".wikipedia", "localhost", port, languageTag + "wikipediaorgtest", languageTag,
            getClass.getClassLoader.getResource("pedia_uima_harvester/resources/articlefilter/redirects.list").getPath,
            getClass.getClassLoader.getResource("pedia_uima_harvester/resources/articlefilter/nonarticle_titles_prefixes.list").getPath,
            wikiUpdateConfig)

        //subscribe to wiki feed (wait until it is registered)
        var wikiFeeds = FeedRegistry.getFeeds[(DBpediaResource, Set[DBpediaResourceOccurrence], Set[DBpediaCategory], Text)]
        while (wikiFeeds.isEmpty) {
            Thread.sleep(1000)
            wikiFeeds = FeedRegistry.getFeeds[(DBpediaResource, Set[DBpediaResourceOccurrence], Set[DBpediaCategory], Text)]
        }

        val wikiTopicFeed = new WikiTopicInferenceFeed(HashMapTopicalPriorStore, wikiFeeds.head.asInstanceOf[Feed[(DBpediaResource, Set[DBpediaResourceOccurrence], Set[DBpediaCategory], Text)]])
        wikiTopicFeed.start()

        descriptions.foreach(description => {
            if (description.rssFeeds.size > 0) {
                //new RssTopicFeed(description.topic, description.rssFeeds.toArray, args(3).toLong).start()
            }
        })

        //subscribe to rss feeds
        //trainer.subscribeToAll

        LOG.info("Training started!")

        var curLine = ""
        LOG.info("Type 'q' to quit training and write updated model to disk!")

        val converter = new InputStreamReader(System.in)
        val in = new BufferedReader(converter)

        while (!((curLine == "q"))) {
            curLine = in.readLine
        }

        //trainer.stopTraining
        //trainer.saveModel
        System.exit(0)
    }
}