# Runs ExtractStopwords.java to get a list of top terms from the index, then cleans it up a bit

#java -jar dbpedia-spotlight.jar ExtractStopwords index CONTEXT 2000 > top-df-terms.set
cut -f 1 -d " " top-df-terms.set | sed s/CONTEXT:// | egrep -v "[0-9]+" | sort -u > stopwords.set

