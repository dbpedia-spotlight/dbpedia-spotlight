#!/bin/bash

for file in ../lib/*; do
artifact="${file:4}"
echo "<dependency>
  <groupId>org.wiki.harvester.dependency</groupId>
  <artifactId>${artifact/.jar/}</artifactId>
  <version>1.0</version>
  <scope>system</scope>
  <systemPath>\${basedir}/$file</systemPath>
</dependency>"
done
