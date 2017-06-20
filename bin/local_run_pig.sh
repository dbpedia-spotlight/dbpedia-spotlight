#!/bin/bash
#+------------------------------------------------------------------------------------------------------------------------------+
#| DBpedia Spotlight - Create database-backed model                                                                             |
#| @author Joachim Daiber                                                                                                       |
#| @author Uwe Hartwig                                                                                                          |  
#+------------------------------------------------------------------------------------------------------------------------------+

#1 working directory
#2 language (de_DE)
#3 location of stopword file in working directory
#4 language for Stemmer
#5 location where to store the final model data


export MAVEN_OPTS="-Xmx24G"

usage () {
    echo -e "\n\"local_run_pig.sh\":"
    echo "Run Apache Pig and Dbpedia spotlight pignlproc to process DBpedia Dump via embedded Hadoop for german DBpedia Spotlight Model without using opennlp."
    echo -e "usage:\t ./local_run_pig.sh /data/model-2015 de_DE /data/model-2015/de/stopwords.de.list German /data/model-2015/de_DE/final-model\n"
}

# options
OPEN_NLP="None"
APACHE_PIG_VERSION="0.10.1"
MUST_EVALUATE=false

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

if [[ "$3" = /* ]]
then
   STOPWORDS="$3"
else
   STOPWORDS="$BASE_DIR/$3"
fi

if [ ! -f $STOPWORDS ]; then
    echo -e "\nERROR:\t\"$STOPWORDS\" is no valid file! Exit process ... "
    usage
    exit
fi

if [[ "$5" = /* ]]
then
   TARGET_DIR="$5"
else
   TARGET_DIR="$BASE_DIR/$5"
fi

WDIR="$BASE_WDIR/$2"
if [ ! -d $WDIR ]; then
    echo -e "\nERROR: \"$WDIR\" doesn't exist! Exit process ... "
    exit
fi

if [[ "$OPEN_NLP" == "None" ]]; then
    echo "No opennlp data";
elif [[ "$OPEN_NLP" != /* ]]; then
    OPEN_NLP="$BASE_DIR/$OPEN_NLP";
    echo "set opennlp data to \"$OPEN_NLP\" ..."
fi

LANGUAGE=`echo $2 | sed "s/_.*//g"`

echo -e "\n##### EXECUTION OPTIONS #####"
echo "opennlp: \"$OPEN_NLP\", language:\"$LANGUAGE\", working directory:\"$WDIR\", target directory:\"$TARGET_DIR\""
echo "Apache Pig (version: $APACHE_PIG_VERSION)"
echo -e "#####\n"

if [ -d "$BASE_WDIR/pig/pig-$APACHE_PIG_VERSION-src" ]; then
    echo "found $BASE_WDIR/pig/pig-$APACHE_PIG_VERSION-src ..., export to PATH"
    export PATH=$BASE_WDIR/pig/pig-$APACHE_PIG_VERSION-src/bin:$PATH
else
    echo "can't find pig binary, exiting ... "
    exit
fi

#Adapt pig params:
cd $BASE_DIR
cd $1/pig/pignlproc

PIGNLPROC_JAR="$BASE_WDIR/pig/pignlproc/target/pignlproc-0.1.0-SNAPSHOT.jar"

mkdir -p $WDIR/pig_out/$LANGUAGE
PIG_INPUT="$WDIR/${LANGUAGE}wiki-latest-pages-articles.xml"
PIG_STOPWORDS="$WDIR/stopwords.$LANGUAGE.list"
TOKEN_OUTPUT="$WDIR/pig_out/$LANGUAGE/tokenCounts"
PIG_TEMPORARY_SFS="$WDIR/pig_out/$LANGUAGE/sf_lookup"
PIG_NE_OUTPUT="$WDIR/pig_out/$LANGUAGE/names_and_entities"
PIG_LOCAL="-x local"

# Before running any pig scripts,request more memory
# cf. https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Run-Apache-Pig-in-local-mode
export PIG_HEAPSIZE="16192"
echo "set PIG_HEAPSIZE to \"$PIG_HEAPSIZE\""

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

# Copy results to local:
cd $BASE_DIR
cd $WDIR
echo "INFO: copy results into \"$WDIR\" ... "
cat $TOKEN_OUTPUT/part* > tokenCounts
cat $PIG_NE_OUTPUT/pairCounts/part* > pairCounts
cat $PIG_NE_OUTPUT/uriCounts/part* > uriCounts
cat $PIG_NE_OUTPUT/sfAndTotalCounts/part* > sfAndTotalCounts

#Create the model:
cd $BASE_DIR
cd ..

echo "Create model with -Dexec.args=\"$2 $WDIR $TARGET_DIR $OPEN_NLP $STOPWORDS $4Stemmer\""
CREATE_MODEL="mvn -pl index exec:java -Dexec.mainClass=org.dbpedia.spotlight.db.CreateSpotlightModel -Dexec.args=\"$2 $WDIR $TARGET_DIR $OPEN_NLP $STOPWORDS $4Stemmer\";"

if [ ! $MUST_EVALUATE ]; then
    echo "$CREATE_MODEL" > create_models.job.sh
else
  eval "$CREATE_MODEL"
  if [ $MUST_EVALUATE ]; then
      mvn -pl eval exec:java -Dexec.mainClass=org.dbpedia.spotlight.evaluation.EvaluateSpotlightModel -Dexec.args="$TARGET_DIR $WDIR/heldout.txt" > $TARGET_DIR/evaluation.txt
  fi
fi

echo "Finished!"

