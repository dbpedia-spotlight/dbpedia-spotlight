package org.dbpedia.spotlight.topic.wikipedia.util

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.{HttpStatus, HttpResponse}
import java.net.{URISyntaxException, UnknownHostException, URI}
import org.apache.http.util.EntityUtils

/**
 * Utility object for requesting the wikipedia api
 */
object WikipediaApiRequest {

    private val APILINK = "http://en.wikipedia.org/w/api.php?action=query&format=xml&"

    /**
     *
     * @param requestParameters e.g.: "prop=extracts&titles=Outline_of_mathematics"
     * @return string, which is actually the xml response
     */
    def request(requestParameters: String): String = {
        val page = APILINK + requestParameters
        val client = new DefaultHttpClient
        var ret = ""
        try {
            val uri = new URI(page)
            val get = new HttpGet(uri)
            val response: HttpResponse = client.execute(get)
            if (response.getStatusLine.getStatusCode == HttpStatus.SC_OK) {
                ret = EntityUtils.toString(response.getEntity)
            }
        } catch {
            case e: UnknownHostException => e.printStackTrace
            case e: URISyntaxException => e.printStackTrace
        }
        client.getConnectionManager.shutdown

        return ret
    }

}
