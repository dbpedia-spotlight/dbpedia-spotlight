MAVEN_OPTS='-Xmx15G' mvn -pl eval exec:java  -Dexec.mainClass=org.dbpedia.spotlight.evaluation.EvaluateParagraphDisambiguator -Dexec.args="../data/spotlight-model-data"
