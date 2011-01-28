package org.dbpedia.spotlight.model

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 05-Jul-2010
 * Time: 10:41:17
 * To change this template use File | Settings | File Templates.
 */

class WikiPageContext (val resource : DBpediaResource, val context : Text)
{

    override def toString = resource + ":\n" + context

}