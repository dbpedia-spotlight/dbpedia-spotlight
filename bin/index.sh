# You are expected to run this script from inside the bin directory in your DBpedia Spotlight installation
# Adjust the paths here if you don't.
# @author maxjakob, pablomendes

here = `pwd`

INDEX_CONFIG_FILE=../conf/index.properties

JAVA_OPTS=-Xmx2g

mkdir ../data

cd ../core
mvn scala:run -DmainClass=org.dbpedia.spotlight.util.SurrogatesUtil "-DaddArgs=$INDEX_CONFIG_FILE"

mvn scala:run -DmainClass=org.dbpedia.spotlight.util.SaveWikipediaDump2Occs "-DaddArgs=$INDEX_CONFIG_FILE|../data/occs.tsv"

sort -t$'\t' -k2 ../data/occs.tsv >../data/occs.uriSorted.tsv

mvn scala:run -DmainClass=org.dbpedia.spotlight.util.IndexMergedOccurrences "-DaddArgs=$INDEX_CONFIG_FILE|../data/occs.uriSorted.tsv"

mvn scala:run -DmainClass=org.dbpedia.spotlight.util.AddSurfaceFormsToIndex "-DaddArgs=$INDEX_CONFIG_FILE"

mvn scala:run -DmainClass=org.dbpedia.spotlight.util.AddTypesToIndex

mvn scala:run -DmainClass=org.dbpedia.spotlight.util.CompressIndex
