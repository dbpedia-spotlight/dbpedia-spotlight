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

#Download:
echo "Downloading DBpedia dumps..."
cd $WDIR
if [ ! -f "redirects.nt" ]; then
  curl -# http://downloads.dbpedia.org/current/$LANGUAGE/redirects_$LANGUAGE.nt.bz2 | bzcat > redirects.nt
  curl -# http://downloads.dbpedia.org/current/$LANGUAGE/disambiguations_$LANGUAGE.nt.bz2 | bzcat > disambiguations.nt
  curl -# http://downloads.dbpedia.org/current/$LANGUAGE/instance_types_$LANGUAGE.nt.bz2 | bzcat > instance_types.nt
fi


if [ "$DATA_ONLY" != "true" ]; then

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

if [ "$local_mode" == "true" ]; then

  if [ ! -e "$BASE_WDIR/pig/pig-0.10.1/" ]; then
    #Install pig:
    cd $BASE_WDIR/pig
    wget http://apache.mirror.triple-it.nl/pig/pig-0.10.1/pig-0.10.1-src.tar.gz
    tar xvzf pig-0.10.1-src.tar.gz
    rm pig-0.10.1-src.tar.gz
    cd pig-0.10.1-src
    ant jar
  fi

  export PATH=$BASE_WDIR/pig/pig-0.10.1-src/bin:$PATH

  #Get the dump
  curl -# "http://dumps.wikimedia.org/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2" | bzcat > $WDIR/${LANGUAGE}wiki-latest-pages-articles.xml

else
  #Load the dump into HDFS:

  if hadoop fs -test -e ${LANGUAGE}wiki-latest-pages-articles.xml ; then
    echo "Dump already in HDFS."
  else
    echo "Loading Wikipedia dump into HDFS..."
    if [ "$eval" == "false" ]; then
        curl -# "http://dumps.wikimedia.org/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2" | bzcat | hadoop fs -put - ${LANGUAGE}wiki-latest-pages-articles.xml
    else
        curl -# "http://dumps.wikimedia.org/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2" | bzcat | python $BASE_WDIR/pig/pignlproc/utilities/split_train_test.py 12000 $WDIR/heldout.txt | hadoop fs -put - ${LANGUAGE}wiki-latest-pages-articles.xml
    fi
  fi

fi



#Load the stopwords into HDFS:
echo "Moving stopwords into HDFS..."
cd $BASE_DIR




if [ "$local_mode" == "false" ]; then

  hadoop fs -put $STOPWORDS stopwords.$LANGUAGE.list || echo "stopwords already in HDFS"

  if [ -e "$opennlp/$LANGUAGE-token.bin" ]; then
      hadoop fs -put "$opennlp/$LANGUAGE-token.bin" "$LANGUAGE.tokenizer_model" || echo "tokenizer model already in HDFS"
  else
      touch empty;
      hadoop fs -put empty "$LANGUAGE.tokenizer_model" || echo "tokenizer model already in HDFS"
      rm empty;
  fi

else

  cd $WDIR
  cp $STOPWORDS stopwords.$LANGUAGE.list || echo "stopwords already in HDFS"

  if [ -e "$opennlp/$LANGUAGE-token.bin" ]; then
      cp "$opennlp/$LANGUAGE-token.bin" "$LANGUAGE.tokenizer_model" || echo "tokenizer already exists"
  else
      touch "$LANGUAGE.tokenizer_model"
  fi

fi


#Adapt pig params:
cd $BASE_DIR
cd $1/pig/pignlproc

PIGNLPROC_JAR="$BASE_WDIR/pig/pignlproc/target/pignlproc-0.1.0-SNAPSHOT.jar"

if [ "$local_mode" == "true" ]; then

  mkdir -p $WDIR/pig_out/$LANGUAGE

  PIG_INPUT="$WDIR/${LANGUAGE}wiki-latest-pages-articles.xml"
  PIG_STOPWORDS="$WDIR/stopwords.$LANGUAGE.list"
  TOKEN_OUTPUT="$WDIR/pig_out/$LANGUAGE/tokenCounts"
  PIG_TEMPORARY_SFS="$WDIR/pig_out/$LANGUAGE/sf_lookup"
  PIG_NE_OUTPUT="$WDIR/pig_out/$LANGUAGE/names_and_entities"

  PIG_LOCAL="-x local"

else

  PIG_INPUT="/user/$USER/${LANGUAGE}wiki-latest-pages-articles.xml"
  PIG_STOPWORDS="/user/$USER/stopwords.$LANGUAGE.list"
  TOKEN_OUTPUT="/user/$USER/$LANGUAGE/tokenCounts"
  PIG_TEMPORARY_SFS="/user/$USER/$LANGUAGE/sf_lookup"
  PIG_NE_OUTPUT="/user/$USER/$LANGUAGE/names_and_entities"

  PIG_LOCAL=""

fi

#Run pig:
pig $PIG_LOCAL -param LANG="$LANGUAGE" \
    -param LOCALE="$2" \
    -param INPUT="$PIG_INPUT" \
    -param OUTPUT="$PIG_NE_OUTPUT" \
    -param TEMPORARY_SF_LOCATION="$PIG_TEMPORARY_SFS" \
    -param PIGNLPROC_JAR="$PIGNLPROC_JAR" \
    -param MACROS_DIR="$BASE_WDIR/pig/pignlproc/examples/macros/" \
    -m examples/indexing/names_and_entities.pig.params examples/indexing/names_and_entities.pig


pig $PIG_LOCAL -param LANG="$LANGUAGE" \
    -param ANALYZER_NAME="$4Analyzer" \
    -param INPUT="$PIG_INPUT" \
    -param OUTPUT_DIR="$TOKEN_OUTPUT" \
    -param STOPLIST_PATH="$PIG_STOPWORDS" \
    -param STOPLIST_NAME="stopwords.$LANGUAGE.list" \
    -param PIGNLPROC_JAR="$PIGNLPROC_JAR" \
    -param MACROS_DIR="$BASE_WDIR/pig/pignlproc/examples/macros/" \
    -m examples/indexing/token_counts.pig.params examples/indexing/token_counts.pig


#Copy results to local:
cd $BASE_DIR
cd $WDIR

if [ "$local_mode" == "true" ]; then

  cat $TOKEN_OUTPUT/part* > tokenCounts
  cat $PIG_NE_OUTPUT/pairCounts/part* > pairCounts
  cat $PIG_NE_OUTPUT/uriCounts/part* > uriCounts
  cat $PIG_NE_OUTPUT/sfAndTotalCounts/part* > sfAndTotalCounts

else

  hadoop fs -cat $LANGUAGE/tokenCounts/part* > tokenCounts
  hadoop fs -cat $LANGUAGE/names_and_entities/pairCounts/part* > pairCounts
  hadoop fs -cat $LANGUAGE/names_and_entities/uriCounts/part* > uriCounts
  hadoop fs -cat $LANGUAGE/names_and_entities/sfAndTotalCounts/part* > sfAndTotalCounts

fi


#Create the model:
cd $BASE_DIR
cd $1/dbpedia-spotlight

CREATE_MODEL="mvn -pl index exec:java -Dexec.mainClass=org.dbpedia.spotlight.db.CreateSpotlightModel -Dexec.args=\"$2 $WDIR $TARGET_DIR $opennlp $STOPWORDS $4Stemmer\";"

if [ "$data_only" == "true" ]; then
    echo "$CREATE_MODEL" >> create_models.job.sh
else
  eval "$CREATE_MODEL"
  
  if [ "$eval" == "true" ]; then
      mvn -pl eval exec:java -Dexec.mainClass=org.dbpedia.spotlight.evaluation.EvaluateSpotlightModel -Dexec.args="$TARGET_DIR $WDIR/heldout.txt" > $TARGET_DIR/evaluation.txt
  fi
fi

echo "Finished!"
set +e
