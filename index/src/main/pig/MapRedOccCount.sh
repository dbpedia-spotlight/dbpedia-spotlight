hadoop fs -copyFromLocal /mnt/windows/Data/Researches/nlp/data/DBpedia/index/instance_types_en.nt .
hadoop fs -copyFromLocal ../../../output/occs.tsv .
pig OccurrencesCount.pig
hadoop fs -getmerge occurrences-count-ids.tsv ../../../output/occurrences-count-ids.tsv

