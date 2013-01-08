mvn install:install-file  -Dfile=../lib/hunposchain0.6_mod.jar \
                          -DgroupId=org.wiki.harvester.dependency \
                          -DartifactId=hunposchain0.6_mod \
                          -Dversion=1.0 \
                          -Dpackaging=jar

mvn install:install-file  -Dfile=../lib/morphadorner.jar \
                          -DgroupId=org.wiki.harvester.dependency \
                          -DartifactId=morphadorner \
                          -Dversion=1.0 \
                          -Dpackaging=jar

mvn install:install-file  -Dfile=../lib/pedia.uima.harvester.jar \
                          -DgroupId=org.wiki.harvester.dependency \
                          -DartifactId=pedia.uima.harvester \
                          -Dversion=1.0 \
                          -Dpackaging=jar

mvn install:install-file  -Dfile=../lib/sztakipedia-parser-0.1.1b.jar \
                          -DgroupId=org.wiki.harvester.dependency \
                          -DartifactId=sztakipedia-parser-0.1.1b \
                          -Dversion=1.0 \
                          -Dpackaging=jar

mvn install:install-file  -Dfile=../lib/textcat-1.0.1.jar \
                          -DgroupId=org.wiki.harvester.dependency \
                          -DartifactId=textcat-1.0.1 \
                          -Dversion=1.0 \
                          -Dpackaging=jar
