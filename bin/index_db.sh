#!/bin/bash
#+------------------------------------------------------------------------------------------------------------------------------+
#| DBpedia Spotlight - Create database-backed model                                                                             |
#| @author Joachim Daiber                                                                                                       |
#+------------------------------------------------------------------------------------------------------------------------------+

# $1 Working directory
# $2 Language (en)
# $3 Stopwords file
# $4 Analyzer
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

while getopts o: opt; do
  case $opt in
  o)
      opennlp=$OPTARG
      ;;
  esac
done

shift $((OPTIND - 1))

if [ $# != 5 ]
then
    usage
    exit
fi

BASE_DIR=$(pwd)

if [[ "$1" = /* ]]
then
   BASE_WDIR="$1"
else
   BASE_WDIR="$BASE_DIR/$1"
fi

WDIR="$1/$2"

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

cd $BASE_DIR

#Set up pig:
if [ -d $BASE_WDIR/pig ]; then
    echo "Updating PigNLProc..."
    cd $BASE_WDIR/pig/pignlproc
    git reset --hard HEAD
    git pull
    mvn -q assembly:assembly -Dmaven.test.skip=true
else
    echo "Setting up PigNLProc..."
    mkdir -p $BASE_WDIR/pig/
    cd $BASE_WDIR/pig/
    git clone --depth 1 https://github.com/dbpedia-spotlight/pignlproc.git
    cd pignlproc
    echo "Building PigNLProc..."
    mvn -q assembly:assembly -Dmaven.test.skip=true
fi

#Set up Spotlight:
cd $BASE_WDIR

if [ -d $1/dbpedia-spotlight ]; then
    echo "Updating DBpedia Spotlight..."
    cd dbpedia-spotlight
    git reset --hard HEAD
    git pull
else
    echo "Setting up DBpedia Spotlight..."
    git clone --depth 1 https://github.com/dbpedia-spotlight/dbpedia-spotlight.git
fi


#Load the dump into HDFS:
echo "Loading Wikipedia dump into HDFS..."
curl -# "http://dumps.wikimedia.org/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2" | bzcat | hadoop fs -put - ${LANGUAGE}wiki-latest-pages-articles.xml

#Load the stopwords into HDFS:
echo "Moving stopwords into HDFS..."
cd $BASE_DIR
hadoop fs -put $3 stopwords.$LANGUAGE.list

#Adapt pig params:
cd $BASE_DIR
cd $1/pig/pignlproc
sed -i s#%LANG#$LANGUAGE#g examples/indexing/token_counts.pig.params
sed -i s#ANALYZER_NAME=DutchAnalyzer#ANALYZER_NAME=$4#g examples/indexing/token_counts.pig.params
sed -i s#%PIG_PATH#$BASE_WDIR/pig/pignlproc#g examples/indexing/token_counts.pig.params

sed -i s#%LANG#${LANGUAGE}#g examples/indexing/names_and_entities.pig.params
sed -i s#%PIG_PATH#$BASE_WDIR/pig/pignlproc#g examples/indexing/names_and_entities.pig.params


#Run pig:
pig -m examples/indexing/token_counts.pig.params examples/indexing/token_counts.pig
pig -m examples/indexing/names_and_entities.pig.params examples/indexing/names_and_entities.pig

#Copy results to local:
cd $BASE_DIR
cd $WDIR
hadoop fs -cat /user/hadoop/${LANGUAGE}/tokenCounts/part* > tokenCounts
hadoop fs -cat /user/hadoop/${LANGUAGE}/names_and_entities/pairCounts/part* > pairCounts
hadoop fs -cat /user/hadoop/${LANGUAGE}/names_and_entities/uriCounts/part* > uriCounts
hadoop fs -cat /user/hadoop/${LANGUAGE}/names_and_entities/sfAndTotalCounts/part* > sfAndTotalCounts

#Create the model:
cd $BASE_DIR
cd $1/dbpedia-spotlight
mvn -q clean
mvn -q install

mvn -pl index exec:java -Dexec.mainClass=org.dbpedia.spotlight.db.CreateSpotlightModel -Dexec.args="$2 $WDIR $opennlp $5 $3 $4";

echo "Finished!"