#+------------------------------------------------------------------------------------------------------------------------------+
#| DBpedia Spotlight - Download script                                                                                          |
#| @author @sandroacoelho                                                                                                       |
#+------------------------------------------------------------------------------------------------------------------------------+

#Config parameters (adjust according your target language and folder)
export lang_i18n=pt
export language=portuguese
export dbpedia_workspace=/usr/local/spotlight
export dbpedia_version=3.8


echo 'Getting DBpedia Files...'
wget http://downloads.dbpedia.org/$dbpedia_version/$lang_i18n/labels_$lang_i18n.nt.bz2
wget http://downloads.dbpedia.org/$dbpedia_version/$lang_i18n/redirects_$lang_i18n.nt.bz2
wget http://downloads.dbpedia.org/$dbpedia_version/$lang_i18n/disambiguations_$lang_i18n.nt.bz2
wget http://downloads.dbpedia.org/$dbpedia_version/$lang_i18n/instance_types_$lang_i18n.nt.bz2
echo 'done!'

echo 'Getting Wikipedia Dump...'
wget "http://dumps.wikimedia.org/"$lang_i18n"wiki/latest/"$lang_i18n"wiki-latest-pages-articles.xml.bz2"
echo 'done!'

echo 'Getting LingPipe Spotter...'
wget http://dbp-spotlight.svn.sourceforge.net/viewvc/dbp-spotlight/tags/release-0.5/dist/src/deb/control/data/usr/share/dbpedia-spotlight/spotter.dict
echo 'done!'

echo 'Getting Spot Selector...'
wget http://spotlight.dbpedia.org/download/release-0.5/spot_selector.tgz
echo 'done!'

echo 'Getting Index...'
wget http://dbp-spotlight.svn.sourceforge.net/viewvc/dbp-spotlight/tags/release-0.5/dist/src/deb/control/data/usr/share/dbpedia-spotlight/index.tgz
echo 'done!'

echo 'Getting Index...'
wget http://dbp-spotlight.svn.sourceforge.net/viewvc/dbp-spotlight/tags/release-0.5/dist/src/deb/control/data/usr/share/dbpedia-spotlight/pos-en-general-brown.HiddenMarkovModel
echo 'done!'

echo 'Getting Apache OpenNLP models...'
wget -q --spider http://opennlp.sourceforge.net/models-1.5/$lang_i18n-chunker.bin
if [ $? -eq 0 ] ; then
   wget http://opennlp.sourceforge.net/models-1.5/$lang_i18n-chunker.bin
else
   echo "$lang_i18n"'-chunker.bin not found. Getting an English version...'
   wget http://opennlp.sourceforge.net/models-1.5/en-chunker.bin -O $lang_i18n-chunker.bin
fi

wget -q --spider http://opennlp.sourceforge.net/models-1.5/$lang_i18n-ner-location.bin
if [ $? -eq 0 ] ; then
   wget http://opennlp.sourceforge.net/models-1.5/$lang_i18n-ner-location.bin
else
   echo "$lang_i18n"'-ner-location.bin not found. Getting an English version...'
   wget http://opennlp.sourceforge.net/models-1.5/en-ner-location.bin -O $lang_i18n-ner-location.bin
fi

wget -q --spider http://opennlp.sourceforge.net/models-1.5/$lang_i18n-ner-organization.bin
if [ $? -eq 0 ] ; then
   wget http://opennlp.sourceforge.net/models-1.5/$lang_i18n-ner-organization.bin
else
   echo "$lang_i18n"'-ner-organization.bin not found. Getting an English version...'
   wget http://opennlp.sourceforge.net/models-1.5/en-ner-organization.bin -O $lang_i18n-ner-organization.bin
fi

wget -q --spider http://opennlp.sourceforge.net/models-1.5/$lang_i18n-ner-person.bin
if [ $? -eq 0 ] ; then
   wget http://opennlp.sourceforge.net/models-1.5/$lang_i18n-ner-person.bin
else
   echo "$lang_i18n"'-ner-person.bin not found. Getting an English version...'
   wget http://opennlp.sourceforge.net/models-1.5/en-ner-person.bin -O $lang_i18n-ner-person.bin
fi

wget  -q --spider http://opennlp.sourceforge.net/models-1.5/$lang_i18n-pos-maxent.bin
if [ $? -eq 0 ] ; then
   wget http://opennlp.sourceforge.net/models-1.5/$lang_i18n-pos-maxent.bin
else
   echo "$lang_i18n"'-ner-pos-maxent.bin not found. Getting an English version...'
   wget http://opennlp.sourceforge.net/models-1.5/en-pos-maxent.bin -O $lang_i18n-pos-maxent.bin
fi

wget  -q --spider http://opennlp.sourceforge.net/models-1.5/$lang_i18n-sent.bin
if [ $? -eq 0 ] ; then
   wget http://opennlp.sourceforge.net/models-1.5/$lang_i18n-sent.bin
else
   echo "$lang_i18n"'-sent.bin not found. Getting an English version...'
   wget http://opennlp.sourceforge.net/models-1.5/en-sent.bin -O $lang_i18n-sent.bin
fi

wget  -q --spider http://opennlp.sourceforge.net/models-1.5/$lang_i18n-token.bin
if [ $? -eq 0 ] ; then
   wget http://opennlp.sourceforge.net/models-1.5/$lang_i18n-token.bin
else
   echo "$lang_i18n"'-token.bin not found. Getting an English version...'
   wget http://opennlp.sourceforge.net/models-1.5/en-token.bin -O $lang_i18n-token.bin
fi

echo 'done!'

# Creating directories
echo 'Creating directories...'
if [ -e $dbpedia_workspace/dbpedia_data ]; then 
    echo "$dbpedia_workspace"'/dbpedia_data/ already exist.'
else
    mkdir $dbpedia_workspace/dbpedia_data
fi

if [ -e $dbpedia_workspace/dbpedia_data/original ]; then 
    echo "$dbpedia_workspace"'/dbpedia_data/original already exist.'
else
    mkdir $dbpedia_workspace/dbpedia_data/original
fi

if [ -e $dbpedia_workspace/dbpedia_data/original/wikipedia/  ]; then 
    echo "$dbpedia_workspace"'/dbpedia_data/original/wikipedia/ already exist.'
else
    mkdir $dbpedia_workspace/dbpedia_data/original/wikipedia/
fi

if [ -e $dbpedia_workspace/dbpedia_data/original/wikipedia/$lang_i18n  ]; then 
    echo "$dbpedia_workspace"'/dbpedia_data/original/wikipedia/'"$lang_i18n"' already exist.'
else
    mkdir $dbpedia_workspace/dbpedia_data/original/wikipedia/$lang_i18n
fi

if [ -e $dbpedia_workspace/dbpedia_data/original/dbpedia  ]; then 
    echo "$dbpedia_workspace"'/dbpedia_data/original/dbpedia already exist.'
else
    mkdir $dbpedia_workspace/dbpedia_data/original/dbpedia
fi

if [ -e $dbpedia_workspace/dbpedia_data/original/dbpedia/$lang_i18n  ]; then 
    echo "$dbpedia_workspace"'/dbpedia_data/original/dbpedia/'"$lang_i18n"'already exist.'
else
    mkdir $dbpedia_workspace/dbpedia_data/original/dbpedia/$lang_i18n
fi

if [ -e $dbpedia_workspace/dbpedia_data/data  ]; then 
    echo "$dbpedia_workspace"'/data already exist.'
else
    mkdir $dbpedia_workspace/dbpedia_data/data
fi


if [ -e $dbpedia_workspace/dbpedia_data/data/output  ]; then
    echo "$dbpedia_workspace"'/data/output already exist.'
else
    mkdir $dbpedia_workspace/dbpedia_data/data/output
fi

if [ -e $dbpedia_workspace/dbpedia_data/data/opennlp  ]; then 
    echo "$dbpedia_workspace"'/data/opennlp already exist.'
else
    mkdir $dbpedia_workspace/dbpedia_data/data/opennlp
fi

if [ -e $dbpedia_workspace/dbpedia_data/data/opennlp/$language  ]; then 
    echo "$dbpedia_workspace"'/data/opennlp already exist.'
else
    mkdir $dbpedia_workspace/dbpedia_data/data/opennlp/$language
fi

#-------------------------------------DBPedia Dumps -------------------------------------------------
mv labels_$lang_i18n.nt.bz2 $dbpedia_workspace/dbpedia_data/original/dbpedia/$lang_i18n
mv redirects_$lang_i18n.nt.bz2 $dbpedia_workspace/dbpedia_data/original/dbpedia/$lang_i18n
mv disambiguations_$lang_i18n.nt.bz2 $dbpedia_workspace/dbpedia_data/original/dbpedia/$lang_i18n
mv instance_types_$lang_i18n.nt.bz2 $dbpedia_workspace/dbpedia_data/original/dbpedia/$lang_i18n
#-------------------------------------Wikipedia Dumps -------------------------------------------------
#Moving files
mv $lang_i18n"wiki-latest-pages-articles.xml.bz2" $dbpedia_workspace/dbpedia_data/original/wikipedia/$lang_i18n
#------------------------------------- Runtime Files --------------------------------------------------
mv spotter.dict $dbpedia_workspace/dbpedia_data/data
mv pos-en-general-brown.HiddenMarkovModel $dbpedia_workspace/dbpedia_data/data
#index
tar xvf index.tgz
mv index $dbpedia_workspace/dbpedia_data/data/output
#spot selector
tar xvf spot_selector.tgz
mv spotsel $dbpedia_workspace/dbpedia_data/data
#Moving OpenNLP files
mv $lang_i18n-chunker.bin $dbpedia_workspace/dbpedia_data/data/opennlp/$language
mv $lang_i18n-ner-location.bin $dbpedia_workspace/dbpedia_data/data/opennlp/$language
mv $lang_i18n-ner-organization.bin $dbpedia_workspace/dbpedia_data/data/opennlp/$language
mv $lang_i18n-ner-person.bin $dbpedia_workspace/dbpedia_data/data/opennlp/$language
mv $lang_i18n-pos-maxent.bin $dbpedia_workspace/dbpedia_data/data/opennlp/$language
mv $lang_i18n-sent.bin $dbpedia_workspace/dbpedia_data/data/opennlp/$language
mv $lang_i18n-token.bin $dbpedia_workspace/dbpedia_data/data/opennlp/$language

#------------------------------------- Original Data  --------------------------------------------------
mv index.tgz  $dbpedia_workspace/dbpedia_data/original
mv spot_selector.tgz  $dbpedia_workspace/dbpedia_data/original




