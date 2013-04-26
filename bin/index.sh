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
set +e

# train a linker (most simple is based on similarity-thresholds)
# mvn scala:run -Dlauncher=EvaluateDisambiguationOnly "-DjavaOpts.Xmx=$JAVA_XMX"
# while we don't have the linker training automatic, let's produce a "standard" file:
thresholds="0.0241645236572\r\n0.0395949799624\r\n0.0488782765424\r\n0.0556941448537\r\n0.06194598808\r\n0.0672960387421\r\n0.0715570383371\r\n0.0754084980644\r\n0.0795713729174\r\n0.0830899600878\r\n0.0863012272902\r\n0.088743171563\r\n0.0917658754118\r\n0.0943744768383\r\n0.096612928555\r\n0.0996444197929\r\n0.102073110029\r\n0.104368675373\r\n0.106208816062\r\n0.108575033583\r\n0.110589252546\r\n0.112719479642\r\n0.114495946091\r\n0.116974528994\r\n0.118974359866\r\n0.120632732467\r\n0.123172225538\r\n0.125192958899\r\n0.127369603551\r\n0.128870138355\r\n0.131319959108\r\n0.133446109027\r\n0.135405747784\r\n0.136766736891\r\n0.139116486444\r\n0.140971073113\r\n0.142823581581\r\n0.144367203305\r\n0.146888377373\r\n0.148761882459\r\n0.150123330793\r\n0.152674168694\r\n0.154680374701\r\n0.15668572694\r\n0.158387811504\r\n0.161113986441\r\n0.163194504717\r\n0.165485601531\r\n0.167067292069\r\n0.169824353944\r\n0.1721109576\r\n0.173790295525\r\n0.176754082456\r\n0.179122019778\r\n0.181691348974\r\n0.183669078305\r\n0.187026370881\r\n0.189687438514\r\n0.19239572159\r\n0.194530874269\r\n0.197855370759\r\n0.20090900173\r\n0.203355087712\r\n0.207153686815\r\n0.210359562736\r\n0.213639424401\r\n0.216273267679\r\n0.220610806217\r\n0.224122744443\r\n0.227704123059\r\n0.230654424628\r\n0.235545101765\r\n0.239918344657\r\n0.244335986961\r\n0.24818091988\r\n0.25384643872\r\n0.258849703886\r\n0.263380962816\r\n0.270346725018\r\n0.276121316708\r\n0.282770468918\r\n0.290077042798\r\n0.299243723252\r\n0.308449844483\r\n0.319319261339\r\n0.330448216246\r\n0.344333241807\r\n0.359974829253\r\n0.373630520856\r\n0.392304370298\r\n0.411133888626\r\n0.433076917824\r\n0.456508615749\r\n0.49034685703\r\n0.534267716746\r\n0.579004746732\r\n0.621946038242\r\n0.690212541034\r\n0.943869140635\r\n1.33364712037"
printf $thresholds > $OUTPUT_LOCATION/index-withSF-withTypes/similarity_thresholds.txt