#bug for join with -d\t ...
tab=$(printf \t)
cat $DBPEDIA_WORKSPACE/data/output/occs.uriSorted.tsv | cut -d"$tab" -f 2,3 |  perl -F/\\t/ -lane 'print "$F[1]\t$F[0]";' > $DBPEDIA_WORKSPACE/data/output/surfaceForms-fromOccs.tsv
sort $DBPEDIA_WORKSPACE/data/output/surfaceForms-fromOccs.tsv | uniq -c > $DBPEDIA_WORKSPACE/data/output/surfaceForms-fromOccs.count
grep -Pv "      [123] " $DBPEDIA_WORKSPACE/data/output/surfaceForms-fromOccs.count | sed -r "s|\s+[0-9]+\s(.+)|\1|" > $DBPEDIA_WORKSPACE/data/output/surfaceForms-fromOccs-thresh3.tsv
