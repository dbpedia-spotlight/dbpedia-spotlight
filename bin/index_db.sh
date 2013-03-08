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

# test if we have two arguments on the command line
if [ $# != 5 ]
then
    usage
    exit
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

#Set up pig:
if [ -d $1/pig ]; then
    echo "Updating PigNLProc..."
    cd $1/pig/pignlproc
    git reset --hard HEAD
    git pull
    mvn -q assembly:assembly -Dmaven.test.skip=true
else
    echo "Setting up PigNLProc..."
    mkdir -p $1/pig/
    cd $1/pig/
    git clone --depth 1 https://github.com/dbpedia-spotlight/pignlproc.git
    cd pignlproc
    echo "Building PigNLProc..."
    mvn -q assembly:assembly -Dmaven.test.skip=true
fi

#Set up Spotlight:
cd $1
if [ -d $1/dbpedia-spotlight ]; then
    echo "Updating DBpedia Spotlight..."
    cd $1/dbpedia-spotlight
    git reset --hard HEAD
    git pull
else
    echo "Setting up DBpedia Spotlight..."
    cd $1
    git clone --depth 1 https://github.com/dbpedia-spotlight/dbpedia-spotlight.git
fi


#Load the dump into HDFS:
echo "Loading Wikipedia dump into HDFS..."
curl -# "http://dumps.wikimedia.org/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2" | bzcat | hadoop fs -put - ${LANGUAGE}wiki-latest-pages-articles.xml

#Load the stopwords into HDFS:
echo "Moving stopwords into HDFS..."
hadoop fs -put $3 stopwords.$LANGUAGE.list

#Adapt pig params:
cd $1/pig/pignlproc
sed -i s#%LANG#$LANGUAGE#g examples/indexing/token_counts.pig.params
sed -i s#ANALYZER_NAME=DutchAnalyzer#ANALYZER_NAME=$4#g examples/indexing/token_counts.pig.params
sed -i s#%PIG_PATH#$1/pig/pignlproc#g examples/indexing/token_counts.pig.params

sed -i s#%LANG#${LANGUAGE}#g examples/indexing/names_and_entities.pig.params

#Run pig:
pig -m examples/indexing/token_counts.pig.params examples/indexing/token_counts.pig
pig -no_multiquery -m examples/indexing/names_and_entities.pig.params examples/indexing/names_and_entities.pig

#Copy results to local:
cd $WDIR
hadoop fs -cat /user/hduser/${LANGUAGE}_tokencounts/token_counts.JSON.bz2/part* > tokenCounts
hadoop fs -cat /user/hduser/${LANGUAGE}_names_and_entities/pairCounts/part* > pairCounts
hadoop fs -cat /user/hduser/${LANGUAGE}_names_and_entities/uriCounts/part* > uriCounts
hadoop fs -cat /user/hduser/${LANGUAGE}_names_and_entities/sfAndTotalCounts/part* > sfAndTotalCounts

#Create the model:
cd $1/dbpedia-spotlight
mvn -q clean install
mvn -pl index exec:java -Dexec.mainClass=org.dbpedia.spotlight.db.CreateSpotlightModel -Dexec.args="$2 $WDIR $opennlp $5 $3 $4";

echo "Finished!"