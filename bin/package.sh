#!/bin/bash
#rm tmp/ -rf
deb=dbpedia-spotlight-0.4.9.deb
cd ../rest
mvn package
mkdir tmp
cd tmp
ar -x ../$deb
cat debian-binary control.tar.gz data.tar.gz > combined-contents
gpg -abs -o _gpgorigin combined-contents
ar rc $deb \
 _gpgorigin debian-binary control.tar.gz data.tar.gz
cp $deb ../
