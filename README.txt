=======================
   DBpedia Spotlight
=======================

To run the RESTful server:
  1. type 'mvn clean install'
  2. configure the values in conf/server.properties (you can also create a new properties file)
  3. go to the rest directory
  4. type 'mvn scala:run "-DaddArgs=[configfile]"'
     with [configfile] being conf/server.properties (or another properties file)
