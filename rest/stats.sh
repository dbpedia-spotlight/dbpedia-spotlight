grep -P "INFO\s(\S+)\s" spotlight.log | sed -r "s/INFO\s(\S+)\s.*/\1/" > dates.list
sort -u dates.list | wc -l
grep "API: class org.dbpedia." spotlight.log -c

