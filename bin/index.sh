# You are expected to run the commands in this script from inside the bin directory in your DBpedia Spotlight installation
# Adjust the paths here if you don't. This script is meant more as a step-by-step guidance than a real automated run-all.
# If this is your first time running the script, we advise you to copy/paste commands from here, closely watching the messages
# and the final output.
#
# @author maxjakob, pablomendes

here=`pwd`

INDEX_CONFIG_FILE=../conf/indexing.properties

# the indexing process merges occurrences in memory to speed up the process. the more memory the better
export JAVA_OPTS="-Xmx14G"
export MAVEN_OPTS="-Xmx14G"
export SCALA_OPTS="-Xmx14G"

# you have to run maven2 from the module that contains the indexing classes
cd ../index
# the indexing process will generate files in the directory below
mkdir output

# first step is to extract valid URIs, synonyms and surface forms from DBpedia
mvn scala:run -DmainClass=org.dbpedia.spotlight.util.ExtractCandidateMap "-DaddArgs=$INDEX_CONFIG_FILE"

# now we collect parts of Wikipedia dump where DBpedia resources occur and output those occurrences as Tab-Separated-Values
mvn scala:run -DmainClass=org.dbpedia.spotlight.lucene.index.ExtractOccsFromWikipedia "-DaddArgs=$INDEX_CONFIG_FILE|output/occs.tsv"

# (recommended) sorting the occurrences by URI will speed up context merging during indexing
sort -t $'\t' -k2 output/occs.tsv >output/occs.uriSorted.tsv

# create a lucene index out of the occurrences
mvn scala:run -DmainClass=org.dbpedia.spotlight.lucene.index.IndexMergedOccurrences "-DaddArgs=$INDEX_CONFIG_FILE|output/occs.uriSorted.tsv"

# (optional) make a backup copy of the index before you lose all the time you've put into this
cp -R output/index output/index-backup

# (optional) preprocess surface forms: produce acronyms, abbreviations, alternative spellings, etc.
#

# add surface forms to index
mvn scala:run -DmainClass=org.dbpedia.spotlight.lucene.index.AddSurfaceFormsToIndex "-DaddArgs=$INDEX_CONFIG_FILE"

# add entity types to index
mvn scala:run -DmainClass=org.dbpedia.spotlight.lucene.index.AddTypesToIndex "-DaddArgs=$INDEX_CONFIG_FILE"

# (optional) reduce index size by unstoring fields (attention: you won't be able to see contents of fields anymore)
mvn scala:run -DmainClass=org.dbpedia.spotlight.lucene.index.CompressIndex "-DaddArgs=$INDEX_CONFIG_FILE|10"

# train a linker (most simple is based on similarity-thresholds)
# mvn scala:run -DmainClass=org.dbpedia.spotlight.evaluation.EvaluateDisambiguationOnly