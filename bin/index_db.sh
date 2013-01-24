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
     echo "usage: ./index_db.sh /data/spotlight/nl nl stopwords.nl.list DutchStemmer /data/spotlight/nl/final_model"
     echo "Create a database-backed model of DBpedia Spotlight for a specified language."
     echo " "
}

# test if we have two arguments on the command line
if [ $# != 5 ]
then
    usage
    exit
fi

mkdir -p $1/processed/

#Download:
cd $1/processed/
curl http://downloads.dbpedia.org/current/$2/redirects_$2.nt.bz2 | bzcat > redirects.nt
curl http://downloads.dbpedia.org/current/$2/disambiguations_$2.nt.bz2 | bzcat > disambiguations.nt
curl http://downloads.dbpedia.org/current/$2/instance_types_$2.nt.bz2 | bzcat > instance_types.nt

#Set up pig:
mkdir -p $1/pig/
cd $1/pig/
git clone --depth 1 https://github.com/dbpedia-spotlight/pignlproc.git
cd pignlproc
mvn assembly:assembly -Dmaven.test.skip=true

#Load the dump into HDFS:
curl http://dumps.wikimedia.org/$2wiki/latest/$2wiki-latest-pages-articles.xml.bz2 | python utilities/split_train_test.py 0.05 $1/processed/test.txt | hadoop fs -put - $2wiki-latest-pages-articles.xml

#Load the stopwords into HDFS:
hadoop fs -put $3 stopwords.$2.list

#Adapt pig params:
sed -i s#nl#$2#g examples/indexing/token_counts.pig
sed -i s#ANALYZER_NAME=DutchAnalyzer#ANALYZER_NAME=$4#g examples/indexing/token_counts.pig
sed -i s#PIGNLPROC_JAR=/home/jodaiber/Desktop/spotlight-nl/data/pignlproc/target/pignlproc-0.1.0-SNAPSHOT.jar#PIGNLPROC_JAR=$1/pig/pignlproc/target/pignlproc-0.1.0-SNAPSHOT.jar#g examples/indexing/token_counts.pig

sed -i s#nl#$2#g examples/indexing/names_and_entities.pig

#Run pig:
pig -m examples/indexing/token_counts.pig.params examples/indexing/token_counts.pig
pig -m examples/indexing/names_and_entities.pig.params examples/indexing/names_and_entities.pig

#Copy results to local:
cd $1/processed/
hadoop fs -cat /user/hduser/$2_tokencounts/token_counts.JSON.bz2/part* > tokenCounts
hadoop fs -cat /user/hduser/$2_names_and_entities/pairCounts/part* > pairCounts
hadoop fs -cat /user/hduser/$2_names_and_entities/uriCounts/part* > uriCounts
hadoop fs -cat /user/hduser/$2_names_and_entities/sfAndTotalCounts/part* > sfAndTotalCounts

#Create the model:
cd $1
git clone --depth 1 https://github.com/jodaiber/dbpedia-spotlight.git
cd dbpedia-spotlight
mvn clean install; mvn -pl index package exec:java -Dexec.mainClass=org.dbpedia.spotlight.db.CreateSpotlightModel -Dexec.args="$2 $1/processed/ $5 $1/opennlp $3 $4";

echo "Finished!"