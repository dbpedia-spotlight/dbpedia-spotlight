#!/bin/bash
#+------------------------------------------------------------------------------------------------------------------------------+
#| DBpedia Spotlight - Create database-backed model                                                                             |
#| @author Joachim Daiber                                                                                                       |
#| @author Philipp Dowling                                                                                                      |
#+------------------------------------------------------------------------------------------------------------------------------+

# $1 Working directory
# $2 Locale (en_US)
# $3 Stopwords file
# $4 Analyzer+Stemmer language prefix e.g. Dutch
# $5 Model target folder

// TODO test run, fix usage string, integrate into index_db.sh

export MAVEN_OPTS="-Xmx26G"

usage ()
{
     echo "index_db.sh"
     echo "usage: ./train_llm.sh wdir en_US /data/spotlight/stopwords.list English /data/spotlight/output_model_folder"
     echo "Train weights for the log-linear model used by Spotlight's vector-based context similarity."
     echo " "
}


opennlp="None"
eval="false"
data_only="false"
local_mode="false"


while getopts "ledo:" opt; do
  case $opt in
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

# Stop processing if one step fails
set -e

cd $BASE_DIR
#Set up pig:
if [ -d $BASE_WDIR/pig ]; then
    echo "Updating PigNLProc..."
    cd $BASE_WDIR/pig/pignlproc
    git reset --hard HEAD
    git pull
else
    echo "Setting up PigNLProc..."
    mkdir -p $BASE_WDIR/pig/
    cd $BASE_WDIR/pig/
    git clone --depth 1 https://github.com/dbpedia-spotlight/pignlproc.git
    cd pignlproc
    echo "Building PigNLProc..."
fi


echo "Generating train data."
mkdir -p $BASE_WDIR/wikipedia/
cd $BASE_WDIR/wikipedia/
echo "Downloading wikipedia dump..."
curl -O "http://dumps.wikimedia.org/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2"

echo "Splitting off train set..."
bzcat ${LANGUAGE}wiki-latest-pages-articles.xml.bz2 | python $BASE_WDIR/pig/pignlproc/utilities/split_train_test.py 12000 $WDIR/heldout.txt > /dev/null

echo "Downloading DBpedia redirects and disambiguations..."
cd $WDIR
if [ ! -f "redirects.nt" ]; then
  curl -# http://downloads.dbpedia.org/current/$LANGUAGE/redirects_$LANGUAGE.nt.bz2 | bzcat > redirects.nt
  curl -# http://downloads.dbpedia.org/current/$LANGUAGE/disambiguations_$LANGUAGE.nt.bz2 | bzcat > disambiguations.nt
fi

echo "Downloading ranklib..."
mkdir -p $BASE_WDIR/ranklib/
cd $BASE_WDIR/ranklib/
curl -L -o RankLib-2.1-patched.jar http://downloads.sourceforge.net/project/lemur/lemur/RankLib-2.1/RankLib-2.1-patched.jar?r=http%3A%2F%2Fsourceforge.net%2Fprojects%2Flemur%2Ffiles%2Flemur%2FRankLib-2.1%2F&ts=1439317425&use_mirror=skylink

cd $BASE_DIR
echo "Generating features and writing ranklib train data..."
MAVEN_OPTS='-Xmx15G' mvn -pl index exec:java -Dexec.mainClass=org.dbpedia.spotlight.db.TrainLLMWeights -Dexec.args="$2 $WDIR $TARGET_DIR";

echo "Training model using ranklib..."
java -jar $BASE_WDIR/ranklib/RankLib-2.1-patched.jar  -ranker 4 -train $TARGET_DIR/ranklib-training-data.txt -save $TARGET_DIR/ranklib-model.txt -metric2t ERR@1

