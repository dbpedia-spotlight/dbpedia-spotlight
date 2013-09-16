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
import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.web.rest.Server
import java.util.Properties
import java.io.{File, FileInputStream}


class SpotlightContextListener extends ServletContextListener {

  def contextInitialized(event: ServletContextEvent) {

    SpotlightLog.debug(this.getClass, "Initializing Spotlight context...")

    val config: Properties = new Properties
    config.load(new FileInputStream(new File(getClass.getClassLoader.getResource("tomcat_spotlight.properties").getPath)))

    val property = config.getProperty("org.dbpedia.spotlight.config.filename", "server.properties")

    if (property.contains(".properties")) {

      val configFileName = getClass.getClassLoader.getResource(property).getPath
      Server.initSpotlightConfiguration(configFileName)

    }
    else {

      Server.initSpotlightConfiguration(property)

    }


    SpotlightLog.debug(this.getClass, "done!")

  }


  def contextDestroyed(event: ServletContextEvent) {

    SpotlightLog.debug(this.getClass, "Spotlight context has destroyed")

  }
}
