# DBpedia Spotlight
#### Shedding Light on the Web of Documents

DBpedia Spotlight looks for ~3.5M things of unknown or ~320 known types in text and tries to link them to their global unique identifiers in [DBpedia](http://dbpedia.org). 

#### Demonstration

Go to our [Demonstration](http://spotlight.dbpedia.org/demo/) page, copy+paste some text and play with the parameters to see how it works.

#### Call our web service

You can use our demonstration [Web Service](http://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Web-service) directly from your application.

    curl http://spotlight.dbpedia.org/rest/annotate \
      --data-urlencode "text=President Obama called Wednesday on Congress to extend a tax break
      for students included in last year's economic stimulus package, arguing
      that the policy provides more generous assistance." \
      --data "confidence=0.2" \
      --data "support=20"

#### Run your own server

If you need service reliability and lower response times, you can run DBpedia Spotlight in your own [In-House Server](https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Installation). Try our automated setup script for [dpkg](https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Debian-Package-Installation:-How-To):

    wget http://spotlight.sztaki.hu/downloads/dbpedia-spotlight-0.6.deb
    dpkg -i dbpedia-spotlight-0.6.deb
    #Follow the installation assistant
    /usr/bin/dbpedia-spotlight-[language]

If you want to run the latest version of spotlight (v0.7), you should do the following:

1. Download the jar file: `wget http://spotlight.sztaki.hu/downloads/dbpedia-spotlight-0.7.jar`
2. Download an entity model from http://spotlight.sztaki.hu/downloads/. For English, you can simply do: `wget http://spotlight.sztaki.hu/downloads/en_2+2.tar.gz`
3. Uncompress the language model tarball 
4. Run it: `java -Xmx5G -Xms5G -jar dbpedia-spotlight-0.7.jar /path/to/language/model/folder http://localhost:2222/rest 5. Test it by pointing your browser to `http://localhost:2222/rest/anotate?text=Berlin&confidence=0.5`

Note 1: If you want to be on the cutting edge, you can clone this repo `git clone https://github.com/dbpedia-spotlight/dbpedia-spotlight.git` and run `mvn package` (you need maven installed). This will generate a `dbpedia-spotlight-0.7-with-dependencies.jar` file in `dist/target`. You can point to this jar file in step 4.

Note 2: This will run spotlight with a dictionary-based spotter. If you want to use an opennlp based spotter, you should create an `opennlp` folder within your uncompressed language model folder (from step 3), if it doesn't exist already, and download sentence-tokenizer, tokenizer, and entity models from `http://opennlp.sourceforge.net/models-1.5/` for your language. You should rename these to `opennlp/token.bin`, `opennlp/sent.bin`, and `opennlp/ner-*.bin` (e.g. ner-person.bin).

#### Build from source

We provide a [Java/Scala API](http://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Java%2FScala%20API) for you to use our code in your application.

[![Build Status](https://secure.travis-ci.org/dbpedia-spotlight/dbpedia-spotlight.png?branch=master)](http://travis-ci.org/dbpedia-spotlight/dbpedia-spotlight)

## Licenses

All the original code produced for DBpedia Spotlight is licensed under  [Apache License, 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). Some modules have dependencies on [LingPipe](http://alias-i.com/lingpipe/) under the [Royalty Free License](http://alias-i.com/lingpipe/licenses/lingpipe-license-1.txt). Some of our original code (currently) depends on GPL-licensed or LGPL-licensed code and is therefore also GPL or LGPL, respectively. We are currently cleaning up the dependencies to release two builds, one purely GPL and one purely Apache License, 2.0.

The documentation on this website is shared as [Creative Commons Attribution-ShareAlike 3.0 Unported License](http://en.wikipedia.org/wiki/Wikipedia:Text_of_Creative_Commons_Attribution-ShareAlike_3.0_Unported_License).

## Citation

If you use the current (statistical version) of DBpedia Spotlight, please cite the following paper. This is the version that's available for download [here](http://spotlight.sztaki.hu/downloads/) and that powers the demos [here](http://dbpedia-spotlight.github.io/demo/).

```bibtex
@inproceedings{isem2013daiber,
  title = {Improving Efficiency and Accuracy in Multilingual Entity Extraction},
  author = {Joachim Daiber and Max Jakob and Chris Hokamp and Pablo N. Mendes},
  year = {2013},
  booktitle = {Proceedings of the 9th International Conference on Semantic Systems (I-Semantics)}
}
```


If you use the Lucene-based version of DBpedia Spotlight, please cite the following paper:


```bibtex
@inproceedings{isem2011mendesetal,
  title = {DBpedia Spotlight: Shedding Light on the Web of Documents},
  author = {Pablo N. Mendes and Max Jakob and Andres Garcia-Silva and Christian Bizer},
  year = {2011},
  booktitle = {Proceedings of the 7th International Conference on Semantic Systems (I-Semantics)},
  abstract = {Interlinking text documents with Linked Open Data enables the Web of Data to be used as background knowledge within document-oriented applications such as search and faceted browsing. As a step towards interconnecting the Web of Documents with the Web of Data, we developed DBpedia Spotlight, a system for automatically annotating text documents with DBpedia URIs. DBpedia Spotlight allows users to configure the annotations to their specific needs through the DBpedia Ontology and quality measures such as prominence, topical pertinence, contextual ambiguity and disambiguation confidence. We compare our approach with the state of the art in disambiguation, and evaluate our results in light of three baselines and six publicly available annotation systems, demonstrating the competitiveness of our system. DBpedia Spotlight is shared as open source and deployed as a Web Service freely available for public use.}
}
```

## Documentation

More documentation is available from the [DBpedia Spotlight wiki](https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki).
