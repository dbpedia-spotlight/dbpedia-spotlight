package org.dbpedia.spotlight.util

import org.dbpedia.spotlight.util.WebOccurrenceSearcher
import org.dbpedia.spotlight.model.DBpediaResource

import org.junit.Test

/**
 * Created by IntelliJ IDEA.
 * User: pablo
 * Date: 4/13/11
 * Time: 5:23 PM
 * To change this template use File | Settings | File Templates.
 */

class IndexPriorYSmootherTests {

    @Test
    def uriToKeywords {
        val examples = Map("Huge"->"+\"Huge\"",
            "Huge_(TV_series)"->"+\"Huge\" +\"TV series\"",
            "Huge_cardinal"->"+\"Huge cardinal\"",
            "Apple_(disambiguation)"->"+\"Apple\"",
            "Apple_%28disambiguation%29"->"+\"Apple\"");

        examples.keys.foreach( title => {
            val s = WebOccurrenceSearcher.createKeywordsFromDBpediaResourceURI(new DBpediaResource(title))
            printf("%-30s=%30s \n",examples(title),s)
            assert(s.equals(examples(title)));
        });
    }

    @Test
    def augmentKeywords {
        val examples = Map("Huge"->"+\"Huge\"",
            "Huge_(TV_series)"->"+\"Huge\" +\"TV series\"",
            "Huge_cardinal"->"+\"Huge cardinal\"",
            "Apple_(disambiguation)"->"+\"Apple\"",
            "Apple_%28disambiguation%29"->"+\"Apple\"");

        examples.keys.foreach( title => {
            val r = new DBpediaResource(title)
            println(r);
            println(WebOccurrenceSearcher.augmentKeywords(r))
            println;
        });

    }



}