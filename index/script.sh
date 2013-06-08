#!/bin/sh
# simple sorting
echo "Test tiny occs"
sort -u -t "`/bin/echo -e '\t'`" -k 2,2 -k 1,1 -T /media/Data/Wikipedia/tmp /media/DirksHDD/GSOC2012/tinyoccs

sort -u -t "`/bin/echo -e '\t'`" -k 2,2 -k 1,1 -T /media/Data/Wikipedia/tmp /media/DirksHDD/GSOC2012/sortedOccs-ac > /media/DirksHDD/GSOC2012/sortedOccs--ac
rm /media/DirksHDD/GSOC2012/sortedOccs-ac
sort -u -t "`/bin/echo -e '\t'`" -k 2,2 -k 1,1 -T /media/Data/Wikipedia/tmp /media/DirksHDD/GSOC2012/sortedOccs-ad > /media/DirksHDD/GSOC2012/sortedOccs--ad
rm /media/DirksHDD/GSOC2012/sortedOccs-ad
sort -u -t "`/bin/echo -e '\t'`" -k 2,2 -k 1,1 -T /media/Data/Wikipedia/tmp /media/DirksHDD/GSOC2012/sortedOccs-ae > /media/DirksHDD/GSOC2012/sortedOccs--ae
rm /media/DirksHDD/GSOC2012/sortedOccs-ae
sort -u -t "`/bin/echo -e '\t'`" -k 2,2 -k 1,1 -T /media/Data/Wikipedia/tmp /media/DirksHDD/GSOC2012/sortedOccs-af > /media/DirksHDD/GSOC2012/sortedOccs--af
rm /media/DirksHDD/GSOC2012/sortedOccs-af
sort -u -t "`/bin/echo -e '\t'`" -k 2,2 -k 1,1 -T /media/Data/Wikipedia/tmp /media/DirksHDD/GSOC2012/sortedOccs-aa > /media/DirksHDD/GSOC2012/sortedOccs--aa
rm /media/DirksHDD/GSOC2012/sortedOccs-aa
sort -u -t "`/bin/echo -e '\t'`" -k 2,2 -k 1,1 -T /media/Data/Wikipedia/tmp /media/DirksHDD/GSOC2012/sortedOccs-ab > /media/DirksHDD/GSOC2012/sortedOccs--ab
rm /media/DirksHDD/GSOC2012/sortedOccs-ab

sort -u -t "`/bin/echo -e '\t'`" -k 2,2 -k 1,1 -T /media/Data/Wikipedia/tmp -m /media/DirksHDD/GSOC2012/sortedOccs--* > /media/DirksHDD/GSOC2012/sortedOccs

mvn scala:run -DmainClass=org.dbpedia.spotlight.topic.wikipedia.SplitOccsByCategories
