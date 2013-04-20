#!/bin/bash
#+------------------------------------------------------------------------------------------------------------------------------+
#| DBpedia Spotlight - Create database-backed model                                                                             |
#| @author Joachim Daiber                                                                                                       |
#+------------------------------------------------------------------------------------------------------------------------------+

# $1 Working directory
# $2 Locale (en_US)
# $3 Stopwords file
# $4 Analyzer+Stemmer language prefix e.g. Dutch(Analzyer|Stemmer)
# $5 Model target folder

export MVN_OPTS="-Xmx26G"

usage ()
{
     echo "index_db.sh"
     echo "usage: ./index_db.sh -o /data/spotlight/nl/opennlp wdir nl_NL /data/spotlight/nl/stopwords.nl.list DutchStemmer /data/spotlight/nl/final_model"
     echo "Create a database-backed model of DBpedia Spotlight for a specified language."
     echo " "
}


opennlp="None"
eval=""

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

#Download:
echo "Downloading DBpedia dumps..."
cd $WDIR
curl -# http://downloads.dbpedia.org/current/$LANGUAGE/redirects_$LANGUAGE.nt.bz2 | bzcat > redirects.nt
curl -# http://downloads.dbpedia.org/current/$LANGUAGE/disambiguations_$LANGUAGE.nt.bz2 | bzcat > disambiguations.nt
curl -# http://downloads.dbpedia.org/current/$LANGUAGE/instance_types_$LANGUAGE.nt.bz2 | bzcat > instance_types.nt


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

cd $BASE_DIR

#Set up pig:
if [ -d $BASE_WDIR/pig ]; then
    echo "Updating PigNLProc..."
    cd $BASE_WDIR/pig/pignlproc
    git reset --hard HEAD
    git pull
    mvn -T 1C -q assembly:assembly -Dmaven.test.skip=true
else
    echo "Setting up PigNLProc..."
    mkdir -p $BASE_WDIR/pig/
    cd $BASE_WDIR/pig/
    git clone --depth 1 https://github.com/dbpedia-spotlight/pignlproc.git
    cd pignlproc
    echo "Building PigNLProc..."
    mvn -T 1C -q assembly:assembly -Dmaven.test.skip=true
fi

# Stop processing if one step fails
set -e

#Load the dump into HDFS:
echo "Loading Wikipedia dump into HDFS..."

if [ "$eval" == "" ]; then
    curl -# "http://dumps.wikimedia.org/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2" | bzcat | hadoop fs -put - ${LANGUAGE}wiki-latest-pages-articles.xml
else
    curl -# "http://dumps.wikimedia.org/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2" | bzcat | python $BASE_WDIR/pig/pignlproc/utilities/split_train_test.py 12000 $WDIR/heldout.txt | hadoop fs -put - ${LANGUAGE}wiki-latest-pages-articles.xml
fi

#Load the stopwords into HDFS:
echo "Moving stopwords into HDFS..."
cd $BASE_DIR
hadoop fs -put $3 stopwords.$LANGUAGE.list

if [ -e "$opennlp/$LANGUAGE-token.bin" ]; then
    hadoop fs -put "$opennlp/$LANGUAGE-token.bin" "$LANGUAGE.tokenizer_model"
else
    touch empty;
    hadoop fs -put empty "$LANGUAGE.tokenizer_model";
    rm empty;
fi


#Adapt pig params:
cd $BASE_DIR
cd $1/pig/pignlproc

#Replace token parameters:
sed -i s#%LANG#$LANGUAGE#g examples/indexing/token_counts.pig.params
sed -i s#ANALYZER_NAME=DutchAnalyzer#ANALYZER_NAME=$4Analyzer#g examples/indexing/token_counts.pig.params
sed -i s#%PIG_PATH#$BASE_WDIR/pig/pignlproc#g examples/indexing/token_counts.pig.params

#Replace names+entities parameters:
sed -i s#%LANG#${LANGUAGE}#g examples/indexing/names_and_entities.pig.params
sed -i s#%LOCALE#$2#g examples/indexing/names_and_entities.pig.params
sed -i s#%PIG_PATH#$BASE_WDIR/pig/pignlproc#g examples/indexing/names_and_entities.pig.params

#Add username to params
sed -i s#/user/hadoop#/user/$USER#g examples/indexing/token_counts.pig.params
sed -i s#/user/hadoop#/user/$USER#g examples/indexing/names_and_entities.pig.params

#Run pig:
pig -m examples/indexing/token_counts.pig.params examples/indexing/token_counts.pig
pig -m examples/indexing/names_and_entities.pig.params examples/indexing/names_and_entities.pig

#Copy results to local:
cd $BASE_DIR
cd $WDIR
hadoop fs -cat ${LANGUAGE}/tokenCounts/part* > tokenCounts
hadoop fs -cat ${LANGUAGE}/names_and_entities/pairCounts/part* > pairCounts
hadoop fs -cat ${LANGUAGE}/names_and_entities/uriCounts/part* > uriCounts
hadoop fs -cat ${LANGUAGE}/names_and_entities/sfAndTotalCounts/part* > sfAndTotalCounts

#Create the model:
cd $BASE_DIR
cd $1/dbpedia-spotlight
mvn -q clean
mvn -q install

mvn -pl index exec:java -Dexec.mainClass=org.dbpedia.spotlight.db.CreateSpotlightModel -Dexec.args="$2 $WDIR $TARGET_DIR $opennlp $STOPWORDS $4Stemmer";

if [ "$eval" == "true" ]; then
    mvn -pl eval exec:java -Dexec.mainClass=org.dbpedia.spotlight.evaluation.EvaluateSpotlightModel -Dexec.args="$TARGET_DIR $WDIR/heldout.txt" > $TARGET_DIR/evaluation.txt
fi

echo "Finished!"
set +e

