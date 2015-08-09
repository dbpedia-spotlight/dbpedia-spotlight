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


# TODO: call ranklib to train LLM and generate output

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
data_only="false"
local_mode="false"


while getopts "ledo:" opt; do
  case $opt in
    o) opennlp="$OPTARG";;
    e) eval="true";;
    d) data_only="true";;
    l) local_mode="true"
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


cd $BASE_DIR
echo "Setting up PigNLProc..."
mkdir -p $BASE_WDIR/pig/
cd $BASE_WDIR/pig/
git clone --depth 1 https://github.com/dbpedia-spotlight/pignlproc.git


# Stop processing if one step fails
set -e
echo "Generating train data..."
curl -O "http://dumps.wikimedia.org/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2"

bzcat ${LANGUAGE}wiki-latest-pages-articles.xml.bz2 | python $BASE_WDIR/pig/pignlproc/utilities/split_train_test.py 12000 $WDIR/heldout.txt > /dev/null


mvn -pl index exec:java -Dexec.mainClass=org.dbpedia.spotlight.db.TrainLLMWeights -Dexec.args="$2 $WDIR $TARGET_DIR";

# TODO: train using ranklib and generate outfile
