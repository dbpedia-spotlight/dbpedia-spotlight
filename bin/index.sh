INDEX_CONFIG_FILE=../conf/index.properties

HEAPSPACE=2g


mkdir ../data

mvn scala:run -DmainClass=org.dbpedia.spotlight.util.SurrogatesUtil -DjvmArg=-Xmx$HEAPSPACE "-DaddArgs=$INDEX_CONFIG_FILE"

mvn scala:run -DmainClass=org.dbpedia.spotlight.util.SaveWikipediaDump2Occs -DjvmArg=-Xmx$HEAPSPACE "-DaddArgs=$INDEX_CONFIG_FILE|../data/occs.tsv"

sort -t$'\t' -k2 ../data/occs.tsv >../data/occs.uriSorted.tsv

mvn scala:run -DmainClass=org.dbpedia.spotlight.util.IndexMergedOccurrences -DjvmArg=-Xmx$HEAPSPACE "-DaddArgs=$INDEX_CONFIG_FILE|../data/occs.uriSorted.tsv"

mvn scala:run -DmainClass=org.dbpedia.spotlight.util.AddSurfaceFormsToIndex -DjvmArg=-Xmx$HEAPSPACE "-DaddArgs=$INDEX_CONFIG_FILE"

mvn scala:run -DmainClass=org.dbpedia.spotlight.util.AddTypesToIndex -DjvmArg=-Xmx$HEAPSPACE

mvn scala:run -DmainClass=org.dbpedia.spotlight.util.CompressIndex -DjvmArg=-Xmx$HEAPSPACE
