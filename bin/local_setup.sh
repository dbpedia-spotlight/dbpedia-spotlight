#!/bin/bash
#+------------------------------------------------------------------------------------------------------------------------------+
#| DBpedia Spotlight - Create database-backed model                                                                             |
#| @author Joachim Daiber                                                                                                       |
#| @author Uwe Hartwig                                                                                                          |
#+------------------------------------------------------------------------------------------------------------------------------+

# $1 Working directory
# $2 Locale (de_DE)
# $3 Stopwords file

export MAVEN_OPTS="-Xmx26G"

usage () {
     echo -e "\n\"local_setup.sh\":"
     echo -e "\tSetup necessary Applications and Data Files for DBpedia Spotligh Model Generation in local mode."
     echo -e "usage:\t ./local_setup.sh /data/spotlight-2015 de_DE ~/stopwords/stopwords.list\n"
}

# Options
OPEN_NLP="None"
APACHE_PIG_VERSION="0.10.1"

shift $((OPTIND - 1))

if [ $# != 3 ]
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

WDIR="$BASE_WDIR/$2"

if [[ "$OPEN_NLP" == "None" ]]; then
    echo "No opennlp data";
elif [[ "$OPEN_NLP" != /* ]]; then
    OPEN_NLP="$BASE_DIR/$OPEN_NLP";
    echo "set opennlp data to \"$OPEN_NLP\" ..."
fi

LANGUAGE=`echo $2 | sed "s/_.*//g"`

echo -e "\n##### EXECUTION OPTIONS #####"
echo "language:\"$LANGUAGE\", working directory: \"$WDIR\""
echo "Apache Pig (version: $APACHE_PIG_VERSION)"
echo -e "#####\n"

echo "INFO: Create working directory \"$WDIR\" ... "
mkdir -p $WDIR

# check existence of dbpedia core files and latest page article dump
echo "INFO: Check existence of DBpedia core files ... "
cd $WDIR
if [ ! -f "redirects.nt" ]; then
  echo "WARN: \"redirects.nt\" is missing, downloading ALL DBpedia core files ..."
  wget -O - http://downloads.dbpedia.org/current/$LANGUAGE/redirects_$LANGUAGE.nt.bz2 | bzcat > redirects.nt
  wget -O - http://downloads.dbpedia.org/current/$LANGUAGE/disambiguations_$LANGUAGE.nt.bz2 | bzcat > disambiguations.nt
  wget -O - http://downloads.dbpedia.org/current/$LANGUAGE/instance_types_$LANGUAGE.nt.bz2 | bzcat > instance_types.nt
else
  echo "INFO: DBpedia core files already present, no need to download ..."
fi
echo "INFO: Check existence of \"$WDIR/${LANGUAGE}wiki-latest-pages-articles.xml\""
if [ ! -f "$WDIR/${LANGUAGE}wiki-latest-pages-articles.xml" ] ; then
  # file missing, start HUGE download (14GB)
  echo "INFO: Latest DBpedia Dump File missing, start download ..."
  wget -O - "http://dumps.wikimedia.org/${LANGUAGE}wiki/latest/${LANGUAGE}wiki-latest-pages-articles.xml.bz2" | bzcat > $WDIR/${LANGUAGE}wiki-latest-pages-articles.xml
else
  echo "INFO: DBpedia latest Data already downloaded into \"$WDIR\", no need to download the whole dump ..."
fi

# set up dbpedia spotlight:
cd $BASE_WDIR
echo "INFO: Create DBpedia Spotlight from github ..."
git clone --depth 1 https://github.com/dbpedia-spotlight/dbpedia-spotlight.git
cd dbpedia-spotlight
mvn -T 2.0C -q clean install

# set up apache pig
cd $BASE_DIR
# create pig directory if not exists
if [ -d $BASE_WDIR/pig ]; then
  echo "INFO: Directory for apache pig already existing ... "
else
  echo "INFO: Create directory for apache pig ..."
  mkdir -p $BASE_WDIR/pig/
fi

echo "INFO: Build Apache Pig Nlproc from dbpedia-spotlight github Respository into \"$BASE_WDIR/pig/pignlproc\" ..."
cd $BASE_WDIR/pig
git clone --depth 1 https://github.com/dbpedia-spotlight/pignlproc.git
cd pignlproc
mvn -T 2.0C -q clean install -Dmaven.test.skip

echo "INFO: Build Apache Pig ($APACHE_PIG_VERSION) from Apache Archive ... "
APACHE_PIG_FOLDER="$BASE_WDIR/pig/pig-$APACHE_PIG_VERSION/"
if [ -d $APACHE_PIG_FOLDER ]; then
    echo "WARN: Apache Pig Folder existed, will be removed!"
    rm -rf $APACHE_PIG_FOLDER
fi
echo "INFO: Download, extract and build Apache Pig ($APACHE_PIG_VERSION) ..."
cd $BASE_WDIR/pig
wget http://archive.apache.org/dist/pig/pig-$APACHE_PIG_VERSION/pig-$APACHE_PIG_VERSION-src.tar.gz
tar xvzf pig-$APACHE_PIG_VERSION-src.tar.gz
rm pig-$APACHE_PIG_VERSION-src.tar.gz
cd pig-$APACHE_PIG_VERSION-src
ant jar

# add recently build pig application to PATH
export PATH=$BASE_WDIR/pig/pig-$APACHE_PIG_VERSION-src/bin:$PATH

# copy stopwords into working directory
echo "INFO: Moving stopwords \"$STOPWORDS\"(lang:$LANGUAGE) into \"stopwords.$LANGUAGE.list\" ..."
cd $BASE_DIR
cd $WDIR
cp $STOPWORDS stopwords.$LANGUAGE.list
touch "$LANGUAGE.tokenizer_model"
