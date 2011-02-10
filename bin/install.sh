mvn install:install-file -Dfile=lib/nxparser-1.1.jar -Dpackaging=jar -DgroupId=org.semanticweb.yars -DartifactId=nx.parser -Dversion=1.1
mvn install:install-file -Dfile=lib/lingpipe-4.0.0.jar -Dpackaging=jar -DgroupId=com.aliasi -DartifactId=lingpipe -Dversion=4.0.0
mvn install:install-file -Dfile=lib/cloud9.jar -Dpackaging=jar -DgroupId=edu.umd -DartifactId=cloud9 -Dversion=SNAPSHOT
mvn install
