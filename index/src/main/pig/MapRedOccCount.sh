hadoop fs -copyFromLocal ../../../output/occs.tsv .
hadoop fs -copyFromLocal /mnt/windows/Data/Researches/nlp/data/DBpedia/index/instance_types_en.nt .
pig OccurrencesCount.pig
hadoop fs -copyToLocal occurrences-count-ids-types-tsv ../../../output/occurrences-count-ids-types.tsv
