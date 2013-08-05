package org.dbpedia.spotlight.model

import java.util.Date
import java.net.URL

/**
 * RSS item wrapper class.
 */
class RssItem(val title: Text, val date: Date, val description: Text, val link: URL)
