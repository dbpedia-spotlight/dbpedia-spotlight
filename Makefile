jar-with-toy-index:
	cd rest
	mvn package
	cp 	target/dbpedia-spotlight-*.*-jar-with-dependencies.jar ../..
	cd 	..

.PHONY: jar-with-toy-index