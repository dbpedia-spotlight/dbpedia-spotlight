# You are expected to run this script from inside the bin directory in your DBpedia Spotlight installation
# Adjust the paths here if you don't.
# @author maxjakob, pablomendes

here = `pwd`

INDEX_CONFIG_FILE=../conf/indexing.properties

JAVA_OPTS=-Xmx5g

# you have to run maven2 from the module that contains the indexing classes
cd ../index
# the indexing process will generate files in the directory below
mkdir output

# first step is to extract valid URIs, synonyms and surface forms from DBpedia
mvn scala:run -DmainClass=org.dbpedia.spotlight.util.SurrogatesUtil "-DaddArgs=$INDEX_CONFIG_FILE"

# now we collect parts of Wikipedia dump where DBpedia resources occur and output those occurrences as Tab-Separated-Values
mvn scala:run -DmainClass=org.dbpedia.spotlight.lucene.index.ExtractOccsFromWikipedia "-DaddArgs=$INDEX_CONFIG_FILE|output/occs.tsv"

# sorting the occurrences by URI will speed up context merging during indexing
sort -t'   ' -k2 output/occs.tsv >output/occs.uriSorted.tsv

# create a lucene index out of the occurrences
mvn scala:run -DmainClass=org.dbpedia.spotlight.util.IndexMergedOccurrences "-DaddArgs=$INDEX_CONFIG_FILE|output/occs.uriSorted.tsv"

# (optional) preprocess surface forms
#

# add surface forms to index
mvn scala:run -DmainClass=org.dbpedia.spotlight.util.AddSurfaceFormsToIndex "-DaddArgs=$INDEX_CONFIG_FILE"

# add entity types to index
mvn scala:run -DmainClass=org.dbpedia.spotlight.util.AddTypesToIndex

# (optional) make a backup copy of the index before you lose all the time you've put into this
#

# (optional) reduce index size by unstoring fields (attention: you won't be able to see contents of fields anymore)
mvn scala:run -DmainClass=org.dbpedia.spotlight.util.CompressIndex
