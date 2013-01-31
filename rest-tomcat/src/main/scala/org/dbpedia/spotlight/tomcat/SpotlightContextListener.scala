/**
 *
 * Copyright 2011 DBpedia Spotlight Development Team
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
 *
 */

package org.dbpedia.spotlight.tomcat

import javax.servlet.{ServletContextEvent, ServletContextListener}
import org.apache.commons.logging.{Log, LogFactory}
import org.dbpedia.spotlight.model.{SpotlightFactory, SpotlightConfiguration}
import java.lang.String
import org.dbpedia.spotlight.web.rest.Server
import org.dbpedia.spotlight.filter.annotations.CombineAllAnnotationFilters


class SpotlightContextListener extends  ServletContextListener {

  val log: Log= LogFactory.getLog("SpotlightContextListener")

  def contextInitialized(event:ServletContextEvent) {

    log.debug("Initializing Spotlight default context")

    log.debug("Loading server.properties from resources path..")
    val configFileName:String = getClass.getClassLoader.getResource("server.properties").getPath

    val configuration:SpotlightConfiguration =  new SpotlightConfiguration(configFileName)
    log.debug("Server.properties loaded from path  =  " + configFileName)

    var factory: SpotlightFactory = new SpotlightFactory(configuration)
    log.debug("Spotlight factory loaded")

    log.debug("Setting disambiguators, spotters and configuration on Server...")
    Server.setDisambiguators(factory.disambiguators)
    Server.setSpotters(factory.spotters)
    Server.setConfiguration(configuration)

    Server.setCombinedFilters(new CombineAllAnnotationFilters(Server.getConfiguration))

    log.debug("done!")

  }


  def contextDestroyed(event:ServletContextEvent) {

    log.debug("Spotlight default context has destroyed")

  }
}
