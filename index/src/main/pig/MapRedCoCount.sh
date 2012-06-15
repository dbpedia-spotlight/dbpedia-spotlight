hadoop fs -copyFromLocal ../../../output/occs.tsv .
pig -param inFile=occs.tsv -param outDir=. CooccurrencesCount.pig
hadoop fs -getmerge co-occs-count.tsv ../../../output/co-occs-count.tsv