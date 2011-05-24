package org.dbpedia.spotlight.io

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

import org.junit.Test
import java.io.{PrintStream, File}

/**
 * 
 * @author pablomendes
 */
object IndexedOccurrencesSourceTest {

    def main(args: Array[String]) {

        val conf = "conf/eval.properties"
//        val out = new PrintStream("/home/pablo/data/confusableWithCommon/commonURIs/uris.counts");
//        IndexedOccurrencesSource.fromConfigFile(new File(conf)).foreach( o => out.println(o.resource.uri+"\t"+o.resource.support))
//        out.close()

        //IndexedOccurrencesSource.fromConfigFile(new File(conf)).foreach( o => println(o.resource.uri+"\t"+o.resource.support))
        IndexedOccurrencesSource.fromFile(new File("/home/pablo/data/Company/2.9.3/MergedIndex.wikipediaTraining.50")).foreach( o => println(o.resource.uri+"\t"+o.resource.support))

    }
}
