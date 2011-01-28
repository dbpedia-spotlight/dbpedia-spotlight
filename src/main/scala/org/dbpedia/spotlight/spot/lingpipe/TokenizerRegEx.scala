package org.dbpedia.spotlight.spot.lingpipe

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 21.09.2010
 * Time: 15:59:00
 * To change this template use File | Settings | File Templates.
 */

object TokenizerRegEx
{
//    val BASE_REGEX = ("('(ll|re|ve|s|d|m|n)|n't|[\\p{L}\\p{N}]+"
//                        + "(?=(\\.$|\\.([\\{Pf}\"']*)|n't))"
//                        + "|[\\p{L}\\p{N}\\.]+|[^\\p{Z}])")

    val BASE_REGEX = ("('(ll|re|ve|s|d|m|n)|'t|[\\p{L}\\p{N}]+"
                        + "(?=(\\.$|\\.([\\{Pf}\"']*)|'t))"
                        + "|[\\p{L}\\p{N}\\.]+|[^\\p{Z}])")

}