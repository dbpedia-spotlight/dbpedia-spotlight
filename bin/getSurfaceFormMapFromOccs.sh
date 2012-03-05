cat output/occs.uriSorted.tsv | cut -d$'\t' -f 2,3 > output/surfaceForms-fromOccs.tsv
sort output/surfaceForms-fromOccs.tsv | uniq -c > output/surfaceForms-fromOccs.count
grep -Pv "      [123] " output/surfaceForms-fromOccs.count | sed -r "s|\s+[0-9]+\s(.+)|\1|" > output/surfaceForms-fromOccs-thresh3.tsv