#!/bin/bash
#+------------------------------------------------------------------------------------------------------------------------------+
#| DBpedia Spotlight - Create database-backed model                                                                             |
#| @author Joachim Daiber                                                                                                       |
#+------------------------------------------------------------------------------------------------------------------------------+

# $1 Working directory
# $2 Locale (en_US)
# $3 Stopwords file
# $4 Analyzer+Stemmer language prefix e.g. Dutch
# $5 Model target folder

export MAVEN_OPTS="-Xmx26G"

usage ()
{
     echo "index_db.sh"
     echo "usage: ./index_db.sh -o /data/spotlight/nl/opennlp wdir nl_NL /data/spotlight/nl/stopwords.nl.list Dutch /data/spotlight/nl/final_model"
     echo "Create a database-backed model of DBpedia Spotlight for a specified language."
     echo " "
}


opennlp="None"
eval="false"

while getopts "eo:" opt; do
  case $opt in
    o) opennlp="$OPTARG";;
    e) eval="true";;
  esac
done


shift $((OPTIND - 1))

if [ $# != 5 ]
then
    usage
    exit
fi

BASE_DIR=$(pwd)

if [[ "$1"  = /* ]]
then
   BASE_WDIR="$1"
else
   BASE_WDIR="$BASE_DIR/$1"
fi

if [[ "$5" = /* ]]
then
   TARGET_DIR="$5"
else
   TARGET_DIR="$BASE_DIR/$5"
fi

if [[ "$3" = /* ]]
then
   STOPWORDS="$3"
else
   STOPWORDS="$BASE_DIR/$3"
fi

WDIR="$BASE_WDIR/$2"

if [[ "$opennlp" == "None" ]]; then
    echo "";
elif [[ "$opennlp" != /* ]]; then
    opennlp="$BASE_DIR/$opennlp";
fi


LANGUAGE=`echo $2 | sed "s/_.*//g"`

echo "Language: $LANGUAGE"
echo "Working directory: $WDIR"

mkdir -p $WDIR

########################################################################################################
# Preparing the data.
########################################################################################################

echo "Loading Wikipedia dump..."
if [ -z "$WIKI_MIRROR" ]; then
  WIKI_MIRROR="http://dumps.wikimedia.org/"
fi

if [ "$eval" == "false" ]; then
  curl -# "$WIKI_MIRROR/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2" | bzcat > $WDIR/dump.xml
else
  curl -# "$WIKI_MIRROR/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2" | bzcat | python $BASE_DIR/scripts/split_train_test.py 1200 $WDIR/heldout.txt > $WDIR/dump.xml
fi

cd $WDIR
cp $STOPWORDS stopwords.$LANGUAGE.list

if [ -e "$opennlp/$LANGUAGE-token.bin" ]; then
  cp "$opennlp/$LANGUAGE-token.bin" "$LANGUAGE.tokenizer_model" || echo "tokenizer already exists"
else
  touch "$LANGUAGE.tokenizer_model"
fi


########################################################################################################
# DBpedia extraction:
########################################################################################################

#Download:
echo "Creating DBpedia nt files..."
cd $BASE_WDIR

if [ -d extraction-framework ]; then
    echo "Updating DBpedia Spotlight..."
    cd extraction-framework
    git reset --hard HEAD
    git pull
    mvn install
else
    echo "Setting up DEF..."
    git clone git://github.com/dbpedia/extraction-framework.git
    cd extraction-framework
    mvn install
fi

cd dump

mkdir -p $WDIR/${LANGUAGE}wiki/$(date +%Y%m%d)/
ln -s $WDIR/dump.xml $WDIR/${LANGUAGE}wiki/$(date +%Y%m%d)/${LANGUAGE}wiki-$(date +%Y%m%d)-dump.xml

cat << EOF > dbpedia.properties
base-dir=$WDIR
source=dump.xml
require-download-complete=false
languages=$LANGUAGE
ontology=../ontology.xml
mappings=../mappings
uri-policy.uri=uri:en; generic:en; xml-safe-predicates:*
format.nt.gz=n-triples;uri-policy.uri
EOF

if [[ ",ga,ar,be,bg,bn,ced,cs,cy,da,eo,et,fa,fi,gl,hi,hr,hu,id,ja,lt,lv,mk,mt,sk,sl,sr,tr,ur,vi,war,zh," == *",$LANGUAGE,"* ]]; then #Languages with no disambiguation definitions
     echo "extractors=.RedirectExtractor,.MappingExtractor" >> dbpedia.properties
else
     echo "extractors=.RedirectExtractor,.DisambiguationExtractor,.MappingExtractor" >> dbpedia.properties
fi

../run extraction dbpedia.properties

zcat $WDIR/${LANGUAGE}wiki/$(date +%Y%m%d)/${LANGUAGE}wiki-$(date +%Y%m%d)-instance-types*.nt.gz  > $WDIR/instance_types.nt
zcat $WDIR/${LANGUAGE}wiki/$(date +%Y%m%d)/${LANGUAGE}wiki-$(date +%Y%m%d)-disambiguations-unredirected.nt.gz > $WDIR/disambiguations.nt
zcat $WDIR/${LANGUAGE}wiki/$(date +%Y%m%d)/${LANGUAGE}wiki-$(date +%Y%m%d)-redirects.nt.gz > $WDIR/redirects.nt


########################################################################################################
# Setting up Spotlight:
########################################################################################################

cd $BASE_WDIR

if [ -d dbpedia-spotlight ]; then
    echo "Updating DBpedia Spotlight..."
    cd dbpedia-spotlight
    git reset --hard HEAD
    git pull
    mvn -T 1C -q clean install
else
    echo "Setting up DBpedia Spotlight..."
    git clone --depth 1 https://github.com/dbpedia-spotlight/dbpedia-spotlight.git
    cd dbpedia-spotlight
    mvn -T 1C -q clean install
fi


########################################################################################################
# Extracting wiki stats:
########################################################################################################

cd $BASE_WDIR
rm -Rf wikistatsextractor
git clone --depth 1 https://github.com/jodaiber/wikistatsextractor

# Stop processing if one step fails
set -e

#Copy results to local:
cd $BASE_WDIR/wikistatsextractor
mvn install exec:java -Dexec.args="--output_folder $WDIR $LANGUAGE $2 $4Stemmer $WDIR/dump.xml $WDIR/stopwords.$LANGUAGE.list"

########################################################################################################
# Building Spotlight model:
########################################################################################################

#Create the model:
cd $BASE_WDIR/dbpedia-spotlight

mvn -pl index exec:java -Dexec.mainClass=org.dbpedia.spotlight.db.CreateSpotlightModel -Dexec.args="$2 $WDIR $TARGET_DIR $opennlp $STOPWORDS $4Stemmer"

if [ "$eval" == "true" ]; then
  mvn -pl eval exec:java -Dexec.mainClass=org.dbpedia.spotlight.evaluation.EvaluateSpotlightModel -Dexec.args="$TARGET_DIR $WDIR/heldout.txt" > $TARGET_DIR/evaluation.txt
fi

curl https://raw.githubusercontent.com/dbpedia-spotlight/model-quickstarter/master/model_readme.txt > $TARGET_DIR/README.txt
curl "$WIKI_MIRROR/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2-rss.xml" | grep link | sed -e 's/^.*<link>//' -e 's/<[/]link>.*$//' | uniq >> $TARGET_DIR/README.txt

echo "Finished! Cleaning up..."
rm -f $WDIR/dump.xml

echo "Collecting data..."
cd $BASE_DIR
mkdir -p data/$LANGUAGE
mv $WDIR/*Counts data/$LANGUAGE

set +e
