/*
 * *
 *  * Copyright 2011 Pablo Mendes, Max Jakob
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.dbpedia.spotlight.graph


import sjson.json.{JsonSerialization, DefaultProtocol, Serializer}
import JsonSerialization._
import DefaultProtocol._

object SerializerTest {

    def main(args: Array[String]) {


        val t1 = List("a", "b", "compiler.properties")

        val serializer = Serializer.SJSON

        serializer.in[List[String]](serializer.out(t1))

        println(serializer.out(t1).toString)
        println(tojson(t1))
        println(tojson(Map("jj" -> t1)))
        println(tojson(t1.foldLeft(Map[String,Double]())( (acc,t) => acc + (t -> 1.0))))

    }

}