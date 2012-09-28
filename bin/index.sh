# You are expected to run the commands in this script from inside the bin directory in your DBpedia Spotlight installation
# Adjust the paths here if you don't. This script is meant more as a step-by-step guidance than a real automated run-all.
# If this is your first time running the script, we advise you to copy/paste commands from here, closely watching the messages
# and the final output.
#
# @author maxjakob, pablomendes

export DBPEDIA_WORKSPACE=/usr/local/spotlight/dbpedia_data


export INDEX_CONFIG_FILE=../conf/indexing.properties

# the indexing process merges occurrences in memory to speed up the process. the more memory the better
ulimit -v unlimited
export JAVA_OPTS="-Xmx14G  -XX:+UseConcMarkSweepGC -XX:MaxPermSize=7G"
export MAVEN_OPTS="-Xmx14G -XX:+UseConcMarkSweepGC -XX:MaxPermSize=7G"
export SCALA_OPTS="-Xmx14G -XX:+UseConcMarkSweepGC -XX:MaxPermSize=7G"
ulimit -v unlimited

# you have to run maven2 from the module that contains the indexing classes
cd ../index
# the indexing process will generate files in the directory below
if [ -e $DBPEDIA_WORKSPACE/data/output  ]; then
    echo "$DBPEDIA_WORKSPACE"'/data/output already exist.'
else
    mkdir -p $DBPEDIA_WORKSPACE/data/output
fi

# first step is to extract valid URIs, synonyms and surface forms from DBpedia
mvn scala:run -DmainClass=org.dbpedia.spotlight.util.ExtractCandidateMap "-DaddArgs=$INDEX_CONFIG_FILE"

# now we collect parts of Wikipedia dump where DBpedia resources occur and output those occurrences as Tab-Separated-Values
echo -e "Parsing Wikipedia dump to extract occurrences...\n"
mvn scala:run -DmainClass=org.dbpedia.spotlight.lucene.index.ExtractOccsFromWikipedia "-DaddArgs=$INDEX_CONFIG_FILE|$DBPEDIA_WORKSPACE/data/output/occs.tsv"

# (recommended) sorting the occurrences by URI will speed up context merging during indexing
echo -e "Sorting occurrences to speed up indexing...\n"
sort -t$'\t' -k2 $DBPEDIA_WORKSPACE/data/output/occs.tsv >$DBPEDIA_WORKSPACE/data/output/occs.uriSorted.tsv

# (optional) preprocess surface forms however you want: produce acronyms, abbreviations, alternative spellings, etc.
#            in the example below we scan paragraphs for uri->sf mappings that occurred together more than 3 times.
echo -e "Extracting Surface Forms...\n"
cat $DBPEDIA_WORKSPACE/data/output/occs.uriSorted.tsv | cut -d$'\t' -f 2,3 |  perl -F/\\t/ -lane 'print "$F[1]\t$F[0]";' > $DBPEDIA_WORKSPACE/data/output/surfaceForms-fromOccs.tsv
sort $DBPEDIA_WORKSPACE/data/output/surfaceForms-fromOccs.tsv | uniq -c > $DBPEDIA_WORKSPACE/data/output/surfaceForms-fromOccs.count
grep -Pv "      [123] " $DBPEDIA_WORKSPACE/data/output/surfaceForms-fromOccs.count | sed -r "s|\s+[0-9]+\s(.+)|\1|" > $DBPEDIA_WORKSPACE/data/output/surfaceForms-fromOccs-thresh3.tsv

cp $DBPEDIA_WORKSPACE/data/output/surfaceForms.tsv $DBPEDIA_WORKSPACE/data/output/surfaceForms-fromTitRedDis.tsv
cat $DBPEDIA_WORKSPACE/data/output/surfaceForms-fromTitRedDis.tsv $DBPEDIA_WORKSPACE/data/output/surfaceForms-fromOccs.tsv > $DBPEDIA_WORKSPACE/data/output/surfaceForms.tsv

# now that we have our set of surfaceForms, we can build a simple dictionary-based spotter from them
mvn scala:run -DmainClass= org.dbpedia.spotlight.spot.lingpipe.IndexLingPipeSpotter "-DaddArgs=$INDEX_CONFIG_FILE"

set -e
# create a lucene index out of the occurrences
echo -e "Creating a context index from occs.tsv...\n"
mvn scala:run -DmainClass=org.dbpedia.spotlight.lucene.index.IndexMergedOccurrences "-DaddArgs=$INDEX_CONFIG_FILE|$DBPEDIA_WORKSPACE/data/output/occs.uriSorted.tsv"
# NOTE: if you get an out of memory error from the command above, try editing ../index/pom.xml with correct jvmArg and file arguments, then run:
#mvn scala:run -Dtarget=indexer "-DaddArgs=$INDEX_CONFIG_FILE|$DBPEDIA_WORKSPACE/data/output/occs.uriSorted.tsv"

# (optional) make a backup copy of the index before you lose all the time you've put into this
#cp -R $DBPEDIA_WORKSPACE/data/output/index $DBPEDIA_WORKSPACE/data/output/index-backup
# add surface forms to index
echo -e "Adding Surface Forms to index...\n"
 mvn scala:run -DmainClass=org.dbpedia.spotlight.lucene.index.AddSurfaceFormsToIndex "-DaddArgs=$INDEX_CONFIG_FILE|$DBPEDIA_WORKSPACE/data/output/index"
# or
 mvn scala:run -DmainClass=org.dbpedia.spotlight.lucene.index.CandidateIndexer "-DaddArgs=$DBPEDIA_WORKSPACE/data/output/surfaceForms.tsv|$DBPEDIA_WORKSPACE/data/output/candidateIndex|3|case-insensitive|overwrite"

# add entity types to index
mvn scala:run -DmainClass=org.dbpedia.spotlight.lucene.index.AddTypesToIndex "-DaddArgs=$INDEX_CONFIG_FILE|$DBPEDIA_WORKSPACE/data/output/index-withSF"

# (optional) reduce index size by unstoring fields (attention: you won't be able to see contents of fields anymore)
mvn scala:run -DmainClass=org.dbpedia.spotlight.lucene.index.CompressIndex "-DaddArgs=$INDEX_CONFIG_FILE|10|$DBPEDIA_WORKSPACE/data/output/index-withSF-withTypes"
set +e

# train a linker (most simple is based on similarity-thresholds)
# mvn scala:run -DmainClass=org.dbpedia.spotlight.evaluation.EvaluateDisambiguationOnly
