/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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