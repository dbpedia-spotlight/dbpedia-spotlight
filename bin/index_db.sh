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

while getopts "ledo:" opt; do
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

#Download:
echo "Downloading DBpedia dumps..."
cd $WDIR
if [ ! -f "redirects.nt" ]; then
  curl -# http://downloads.dbpedia.org/current/$LANGUAGE/redirects_$LANGUAGE.nt.bz2 | bzcat > redirects.nt
  curl -# http://downloads.dbpedia.org/current/$LANGUAGE/disambiguations_$LANGUAGE.nt.bz2 | bzcat > disambiguations.nt
  curl -# http://downloads.dbpedia.org/current/$LANGUAGE/instance_types_$LANGUAGE.nt.bz2 | bzcat > instance_types.nt
fi

#Set up Spotlight:
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


cd $BASE_WDIR
git clone --depth 1 https://github.com/jodaiber/wikistatsextractor

cd $BASE_DIR

# Stop processing if one step fails
set -e
echo "Loading Wikipedia dump into HDFS..."
if [ "$eval" == "false" ]; then
  curl -# "http://dumps.wikimedia.org/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2" | bzcat > $WDIR/dump.xml
else
  curl -# "http://dumps.wikimedia.org/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2" | bzcat | python $BASE_WDIR/pig/pignlproc/utilities/split_train_test.py 12000 $WDIR/heldout.txt > $WDIR/dump.xml
fi

cd $WDIR
cp $STOPWORDS stopwords.$LANGUAGE.list || echo "stopwords already in HDFS"

if [ -e "$opennlp/$LANGUAGE-token.bin" ]; then
  cp "$opennlp/$LANGUAGE-token.bin" "$LANGUAGE.tokenizer_model" || echo "tokenizer already exists"
else
  touch "$LANGUAGE.tokenizer_model"
fi


#Copy results to local:
cd $BASE_WDIR/wikistatsextractor
mvn install exec:java -Dexec.args="--output_folder $WDIR $LANGUAGE $2 $4Stemmer $WDIR/dump.xml $WDIR/stopwords.$LANGUAGE.list"

#Create the model:
cd $BASE_WDIR/dbpedia-spotlight

mvn -pl index exec:java -Dexec.mainClass=org.dbpedia.spotlight.db.CreateSpotlightModel -Dexec.args="$2 $WDIR $TARGET_DIR $opennlp $STOPWORDS $4Stemmer"

if [ "$eval" == "true" ]; then
  mvn -pl eval exec:java -Dexec.mainClass=org.dbpedia.spotlight.evaluation.EvaluateSpotlightModel -Dexec.args="$TARGET_DIR $WDIR/heldout.txt" > $TARGET_DIR/evaluation.txt
fi

echo "Finished!"
set +e
