package org.dbpedia.spotlight.model

import org.dbpedia.spotlight.string.ModifiedWikiUtil


class SurfaceForm(var name : String)
{
    name = if (ModifiedWikiUtil.isEncoded(name)) {
               ModifiedWikiUtil.wikiDecode(name)
           }
           else {
               ModifiedWikiUtil.cleanSpace(name)
           }

    def equals(that : SurfaceForm) : Boolean =
    {
        //TODO: instead of equalsIgnoreCase, fix the Spotter // (should be fixed now)
        name.equalsIgnoreCase(that.name)
    }

    override def toString = "SurfaceForm["+name+"]"
}
