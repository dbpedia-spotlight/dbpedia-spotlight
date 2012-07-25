package org.dbpedia.spotlight.live.model

import java.util.Date
import java.net.URL
import org.dbpedia.spotlight.model.Text

/**
 * RSS item wrapper class.
 */
class RssItem(val title: Text, val date: Date, val description: Text, val link: URL)
