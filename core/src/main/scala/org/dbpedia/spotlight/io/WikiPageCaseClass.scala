package org.dbpedia.spotlight.io

import org.dbpedia.extraction.sources.WikiPage

object WikiPageUtil {
    def copyWikiPage(wikiPage: WikiPage, source: String) = {
        new WikiPage(wikiPage.title, wikiPage.redirect, wikiPage.id, wikiPage.revision, wikiPage.timestamp, source)
    }
}

