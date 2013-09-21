#!/bin/bash
#+------------------------------------------------------------------------------------------------------------------------------+
#| DBpedia Spotlight - Download script                                                                                          |
#+------------------------------------------------------------------------------------------------------------------------------+
PROGNAME=$(basename $0)

##### Config parameters (adjust according your target language and folder)

# Windows workspace path example
#export dbpedia_workspace="E:/Spotlight"

# Linux workspace path example
export dbpedia_workspace="/home/ubuntu/Spotlight"

export lang_i18n=pt
export language=portuguese
export all_languages=(en bg ca cs de el es fr hu it ko pl pt ru sl tr)
export dbpedia_version=3.8
RELEASE_VERSION="0.6"

# Paths to all the directories we are going to need
DATA=$dbpedia_workspace/data
DBPEDIA_DATA=$DATA/dbpedia
JENA_DATA=$DATA/jena
OPENNLP_DATA=$DATA/opennlp
OUTPUT_DATA=$DATA/output
WIKIPEDIA_DATA=$DATA/wikipedia
RESOURCES_DATA=$DATA/resources

# All the download URLs used
OPENNLP_DOWNLOADS="http://opennlp.sourceforge.net/models-1.5"
DBPEDIA_DOWNLOADS="http://downloads.dbpedia.org"/$dbpedia_version
SPOTLIGHT_DOWNLOADS="http://spotlight.dbpedia.org/download/release-0.5"
GITHUB_DOWNLOADS1="--no-check-certificate https://raw.github.com/sandroacoelho/lucene-quickstarter/4a6f571d06ab5ebb303f96eb9e6ad84e9cdd0425"
GITHUB_DOWNLOADS2="--no-check-certificate https://raw.github.com/dbpedia-spotlight/dbpedia-spotlight/release-"$RELEASE_VERSION"/dist/src/deb/control/data/usr/share/dbpedia-spotlight"
WIKIMEDIA_DOWNLOADS="http://dumps.wikimedia.org/"$lang_i18n"wiki/latest"

#+------------------------------------------------------------------------------------------------------------------------------+
#| Functions                                                                                                                    |
#+------------------------------------------------------------------------------------------------------------------------------+

# Error_exit function by William Shotts. http://stackoverflow.com/questions/64786/error-handling-in-bash
function error_exit
{
    echo -e "${PROGNAME}: ${1:-"Unknown Error"}" 1>&2
    exit 1
}

# The function used to create all the directories needed
function create_dir()
{
    if [ -e $1 ]; then
        echo -e $1" already exists. Skipping creating this directory!"
    else
        mkdir $1
    fi
}

# A helper function to download files from a given path. The first parameter is the path from where to download the file
# without the file name, the second states the file name, and the third is where to save that file
function download_file()
{
    # Only downloads if there is no current file or there is a newer version
    echo "$#"
    case "$#" in
        "3")
            wget -q --spider $1/$2
            if [ $? -eq 0 ] ; then
                wget -N $1/$2 --directory-prefix=$3
            else
                # The file can't be found. We can extract a substring with the file name and show it to the user
                error_exit "ERROR: The file '"$2"' cannot be found for download.\n"
            fi
            ;;
        "4")
            echo "panda"
            wget -q --spider $1 $2/$3
            if [ $? -eq 0 ] ; then
                wget -N $1 $2/$3 --directory-prefix=$4
            else
                # The file can't be found. We can extract a substring with the file name and show it to the user
                error_exit "ERROR: The file '"$3"' cannot be found for download.\n"
            fi
            ;;
        *)
            error_exit "ERROR: Incorrect number of parameters!";
    esac
    echo -e "done!\n"
}

# A slightly modified version of the download function. This time if we don't find the initial language files
# we change the path to get the english OpenNLP files
function dl_opennlp_file()
{
    local path=$OPENNLP_DOWNLOADS/$1'-'$2
    wget -q --spider $path
    if [ $? -eq 0 ] ; then
        wget -N $path --directory-prefix=$3
    else
        echo "$file not found. Getting an English version..."
        local string_to_replace=$1'-'
        local new_path=$(echo $path | sed -e s/"$string_to_replace"/en-/g)
        echo $new_path
        wget -q --spider $new_path
        if [ $? -eq 0 ] ; then
            wget -N $new_path --directory-prefix=$3
        else
            string_to_replace="en-"
            new_path=$(echo $new_path | sed -e s/"$string_to_replace"/en-ner-/g)
            wget -N $new_path --directory-prefix=$3
        fi
    fi
}

# The function used to test the languages used as complement to the initial language
function test_languages_array
{
    # Loop through the languages array
    for i in ${all_languages[@]}
    do
       # Checking if the complement languages supplied are valid
       if ([ $(expr length $i) -ne 2 ] | [ $( expr match $i [a-zA-Z]\. ) -ne 2 ]); then
           error_exit "ERROR: Invalid complement languages!"
       fi
    done
}

function unicodeEscape()
{
    bunzip2 -df $1/$2
    # Run the unicodeEscape.py script.
    local new_file_name=$(echo "$2" | sed "s/\([^\.]*\)\(\.bz2\)/\1/g")
    /usr/bin/python unicodeEscape.py "$1"/"$new_file_name"
    rm $1/$new_file_name
    mv $1/$new_file_name".new" $1/$new_file_name
    bzip2 -z -k $1/$new_file_name
}

#+------------------------------------------------------------------------------------------------------------------------------+
#| Main                                                                                                                         |
#+------------------------------------------------------------------------------------------------------------------------------+

# Create the installation directory
mkdir $dbpedia_workspace
touch $dbpedia_workspace/foo && rm -f $dbpedia_workspace/foo || error_exit "ERROR: The directory '$dbpedia_workspace' is not writable! Change its permissions or choose another 'dbpedia_workspace' in download.sh"

set -e

# Test the languages array
test_languages_array

# Creating all the directories needed
echo -e "\nCreating base directories..."
create_dir $DATA
create_dir $OUTPUT_DATA
create_dir $WIKIPEDIA_DATA
create_dir $DBPEDIA_DATA
create_dir $OPENNLP_DATA
create_dir $JENA_DATA
create_dir $RESOURCES_DATA

# Loop through the languages array
for i in ${all_languages[@]}
do
    create_dir $OUTPUT_DATA/$i
    create_dir $OUTPUT_DATA/$i/index
    create_dir $WIKIPEDIA_DATA/$i
    create_dir $DBPEDIA_DATA/$i
    create_dir $OPENNLP_DATA/$i
    create_dir $JENA_DATA/$i
    create_dir $JENA_DATA/$i/TDB
    create_dir $RESOURCES_DATA/$i
done

# The next step is to download all the needed files.
set +e

echo -e "\nGetting DBpedia Files..."
# The download_file function parameters are: 1) path/to/file 2) file_name 3) where/to/save
download_file $DBPEDIA_DOWNLOADS/$lang_i18n labels_$lang_i18n.nt.bz2 $DBPEDIA_DATA/$lang_i18n
download_file $DBPEDIA_DOWNLOADS/$lang_i18n redirects_$lang_i18n.nt.bz2 $DBPEDIA_DATA/$lang_i18n
download_file $DBPEDIA_DOWNLOADS/$lang_i18n disambiguations_$lang_i18n.nt.bz2 $DBPEDIA_DATA/$lang_i18n
download_file $DBPEDIA_DOWNLOADS/$lang_i18n instance_types_$lang_i18n.nt.bz2 $DBPEDIA_DATA/$lang_i18n

echo "Getting Wikipedia Dump..."
download_file $WIKIMEDIA_DOWNLOADS $lang_i18n"wiki-latest-pages-articles.xml.bz2" $WIKIPEDIA_DATA/$lang_i18n
bunzip2 -fk $WIKIPEDIA_DATA/$lang_i18n/$lang_i18n"wiki-latest-pages-articles.xml.bz2" > $WIKIPEDIA_DATA/$lang_i18n/$lang_i18n"wiki-latest-pages-articles.xml"

echo "Getting CoOccurrenceBased Spot Selector Statistics..."
download_file $GITHUB_DOWNLOADS2 "spotter.dict" $RESOURCES_DATA

echo "Getting Spot Selector..."
download_file $SPOTLIGHT_DOWNLOADS "spot_selector.tgz" $RESOURCES_DATA
tar -xvf $RESOURCES_DATA/spot_selector.tgz --force-local -C $RESOURCES_DATA

echo "Getting the tiny Lucene Context Index..."
# Hard coded version release to 0.5. The URL to version 0.6 has a folder and getting it from GitHub is not working
wget -N --no-check-certificate https://raw.github.com/dbpedia-spotlight/dbpedia-spotlight/release-0.5/dist/src/deb/control/data/usr/share/dbpedia-spotlight/index.tgz --directory-prefix=$RESOURCES_DATA
# download_file $GITHUB_DOWNLOADS2 "index.tgz" $RESOURCES_DATA
tar -xvf $RESOURCES_DATA/index.tgz --force-local -C $OUTPUT_DATA/$lang_i18n

echo "Copying Hidden Markov Model to the resources folder..."
cp ../core/src/main/resources/pos-en-general-brown.HiddenMarkovModel $RESOURCES_DATA

echo "Getting the stop words list... (direct link to the pt version at the moment)"
download_file $GITHUB_DOWNLOADS1/$lang_i18n "stopwords.list" $RESOURCES_DATA/$lang_i18n

echo "Getting the URI blacklisted patterns list... (direct link to the pt version at the moment)"
download_file $GITHUB_DOWNLOADS1/$lang_i18n "blacklistedURIPatterns."$lang_i18n".list" $RESOURCES_DATA/$lang_i18n

echo "Getting Apache OpenNLP models..."
# The download_opennlp_file function parameters are: 1) language 2) model_name 3) where/to/save
dl_opennlp_file $lang_i18n "chunker.bin" $OPENNLP_DATA/$lang_i18n
dl_opennlp_file $lang_i18n "location.bin" $OPENNLP_DATA/$lang_i18n
dl_opennlp_file $lang_i18n "organization.bin" $OPENNLP_DATA/$lang_i18n
dl_opennlp_file $lang_i18n "person.bin" $OPENNLP_DATA/$lang_i18n
dl_opennlp_file $lang_i18n "pos-maxent.bin" $OPENNLP_DATA/$lang_i18n
dl_opennlp_file $lang_i18n "sent.bin" $OPENNLP_DATA/$lang_i18n
dl_opennlp_file $lang_i18n "token.bin" $OPENNLP_DATA/$lang_i18n

# The unicodeEscape parameters are: 1) path/to/dir 2) bz2_file_name
unicodeEscape $DBPEDIA_DATA/$lang_i18n "disambiguations_$lang_i18n.nt.bz2"
unicodeEscape $DBPEDIA_DATA/$lang_i18n "instance_types_$lang_i18n.nt.bz2"
unicodeEscape $DBPEDIA_DATA/$lang_i18n "labels_$lang_i18n.nt.bz2"
unicodeEscape $DBPEDIA_DATA/$lang_i18n "redirects_$lang_i18n.nt.bz2"

echo -e "\nAll the downloads are done!"

