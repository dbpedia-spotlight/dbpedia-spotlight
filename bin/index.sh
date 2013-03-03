# You are expected to run the commands in this script from inside the bin directory in your DBpedia Spotlight installation
# Adjust the paths here if you don't. This script is meant more as a step-by-step guidance than a real automated run-all.
# If this is your first time running the script, we advise you to copy/paste commands from here, closely watching the messages
# and the final output.

export lang_i18n=en
export OUTPUT_LOCATION=../dbpedia_data/data/output/$lang_i18n
export INDEX_CONFIG_FILE=../conf/indexing.$lang_i18n.properties

JAVA_XMX=5g

# you have to run maven from the module that contains the indexing classes
cd ../index
# the indexing process will generate files in the directory below
if [ -e OUTPUT_LOCATION  ]; then
    echo "$OUTPUT_LOCATION already exists."
else
    mkdir -p $OUTPUT_LOCATION
fi

# Ensure Maven3 is used.
MVN_VERSION=`mvn -version | grep Apache | sed -r "s/Apache Maven (.+)/\1/"`
[[ $MVN_VERSION =~ ([0-9][.][0-9.]*) ]] && version="${BASH_REMATCH[1]}"
if ! awk -v ver="$version" 'BEGIN { if (ver < 3) exit 1; }'; then
    printf 'ERROR: Maven version 3 or higher required\n' "mvn"
fi

# first step is to extract valid URIs, synonyms and surface forms from DBpedia
mvn scala:run -Dlauncher=ExtractCandidateMap "-DjavaOpts.Xmx=$JAVA_XMX" "-DaddArgs=$INDEX_CONFIG_FILE"

# now we collect parts of Wikipedia dump where DBpedia resources occur and output those occurrences as Tab-Separated-Values
echo -e "Parsing Wikipedia dump to extract occurrences...\n"
mvn scala:run -Dlauncher=ExtractOccsFromWikipedia "-DjavaOpts.Xmx=$JAVA_XMX" "-DaddArgs=$INDEX_CONFIG_FILE|$OUTPUT_LOCATION/occs.tsv"

# (recommended) sorting the occurrences by URI will speed up context merging during indexing
echo -e "Sorting occurrences to speed up indexing...\n"
sort -t$'\t' -k2 $OUTPUT_LOCATION/occs.tsv >$OUTPUT_LOCATION/occs.uriSorted.tsv

# (optional) preprocess surface forms however you want: produce acronyms, abbreviations, alternative spellings, etc.
#            in the example below we scan paragraphs for uri->sf mappings that occurred together more than 3 times.
echo -e "Extracting Surface Forms...\n"
cat $OUTPUT_LOCATION/occs.uriSorted.tsv | cut -d$'\t' -f 2,3 |  perl -F/\\t/ -lane 'print "$F[1]\t$F[0]";' > $OUTPUT_LOCATION/surfaceForms-fromOccs.tsv
sort $OUTPUT_LOCATION/surfaceForms-fromOccs.tsv | uniq -c > $OUTPUT_LOCATION/surfaceForms-fromOccs.count
grep -Pv "      [123] " $OUTPUT_LOCATION/surfaceForms-fromOccs.count | sed -r "s|\s+[0-9]+\s(.+)|\1|" > $OUTPUT_LOCATION/surfaceForms-fromOccs-thresh3.tsv

cp $OUTPUT_LOCATION/surfaceForms.tsv $OUTPUT_LOCATION/surfaceForms-fromTitRedDis.tsv
cat $OUTPUT_LOCATION/surfaceForms-fromTitRedDis.tsv $OUTPUT_LOCATION/surfaceForms-fromOccs.tsv > $OUTPUT_LOCATION/surfaceForms.tsv

# now that we have our set of surfaceForms, we can build a simple dictionary-based spotter from them
mvn scala:run -Dlauncher=IndexLingPipeSpotter "-DjavaOpts.Xmx=$JAVA_XMX" "-DaddArgs=$INDEX_CONFIG_FILE"
cp  $OUTPUT_LOCATION/surfaceForms.tsv.spotterDictionary $OUTPUT_LOCATION/spotter.$lang_i18n.dict

set -e
# create a lucene index out of the occurrences
echo -e "Creating a context index from occs.tsv...\n"
mvn scala:run -Dlauncher=IndexMergedOccurrences "-DjavaOpts.Xmx=$JAVA_XMX" "-DaddArgs=$INDEX_CONFIG_FILE|$OUTPUT_LOCATION/occs.uriSorted.tsv"
# NOTE: if you get an out of memory error from the command above, try editing ../index/pom.xml with correct jvmArg and file arguments.

# (optional) make a backup copy of the index before you lose all the time you've put into this
#cp -R $OUTPUT_LOCATION/index $OUTPUT_LOCATION/index-backup
# add surface forms to index
echo -e "Adding Surface Forms to index...\n"
mvn scala:run -Dlauncher=AddSurfaceFormsToIndex "-DjavaOpts.Xmx=$JAVA_XMX" "-DaddArgs=$INDEX_CONFIG_FILE|$OUTPUT_LOCATION/index"
# or:
# mvn scala:run -Dlauncher=CandidateIndexer "-DjavaOpts.Xmx=$JAVA_XMX" "-DaddArgs=$OUTPUT_LOCATION/surfaceForms.tsv|$OUTPUT_LOCATION/candidateIndex|3|case-insensitive|overwrite"

# add entity types to index
mvn scala:run -Dlauncher=AddTypesToIndex "-DjavaOpts.Xmx=$JAVA_XMX" "-DaddArgs=$INDEX_CONFIG_FILE|$OUTPUT_LOCATION/index-withSF"

# (optional) reduce index size by unstoring fields (attention: you won't be able to see contents of fields anymore)
mvn scala:run -Dlauncher=CompressIndex "-DjavaOpts.Xmx=$JAVA_XMX" "-DaddArgs=$INDEX_CONFIG_FILE|10|$OUTPUT_LOCATION/index-withSF-withTypes"
cp $OUTPUT_LOCATION/tiny.en.index/similarity-thresholds.txt $DBPEDIA_WORKSPACE/data/output/index-withSF-withTypes
set +e

# train a linker (most simple is based on similarity-thresholds)
# mvn scala:run -Dlauncher=EvaluateDisambiguationOnly "-DjavaOpts.Xmx=$JAVA_XMX"
