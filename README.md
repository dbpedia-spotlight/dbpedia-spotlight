# DBpedia Spotlight
#### Shedding Light on the Web of Documents

DBpedia Spotlight looks for ~3.5M things of unknown or ~320 known types in text and tries to link them to their global unique identifiers in [DBpedia](http://dbpedia.org). 

#### Demonstration

Go to our [Demonstration](http://dbpedia-spotlight.github.io/demo/) page, copy+paste some text and play with the parameters to see how it works.

#### Call our web service

You can use our demonstration [Web Service](http://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Web-service) directly from your application.

    curl http://spotlight.sztaki.hu:2222/rest/annotate \
      --data-urlencode "text=President Obama called Wednesday on Congress to extend a tax break
      for students included in last year's economic stimulus package, arguing
      that the policy provides more generous assistance." \
      --data "confidence=0.35"

or for JSON:

    curl http://spotlight.sztaki.hu:2222/rest/annotate \
      --data-urlencode "text=President Obama called Wednesday on Congress to extend a tax break
      for students included in last year's economic stimulus package, arguing
      that the policy provides more generous assistance." \
      --data "confidence=0.35" \
      -H "Accept: application/json"

#### Run your own server

If you need service reliability and lower response times, you can run DBpedia Spotlight in your own [In-House Server](https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Installation). Just download a model and Spotlight from [here](http://spotlight.sztaki.hu/downloads/) to get started.

    wget http://spotlight.sztaki.hu/downloads/dbpedia-spotlight-latest.jar
    wget http://spotlight.sztaki.hu/downloads/latest_models/en.tar.gz
    tar xzf en.tar.gz
    java -jar dbpedia-spotlight-latest.jar en http://localhost:2222/rest

#### Models and data

Models and raw data for most languages are available [here](http://spotlight.sztaki.hu/downloads/).

[![Build Status](https://secure.travis-ci.org/dbpedia-spotlight/dbpedia-spotlight.png?branch=master)](http://travis-ci.org/dbpedia-spotlight/dbpedia-spotlight)

## Citation

If you use DBpedia Spotlight in your research, please cite the following paper:

```bibtex
@inproceedings{isem2013daiber,
  title = {Improving Efficiency and Accuracy in Multilingual Entity Extraction},
  author = {Joachim Daiber and Max Jakob and Chris Hokamp and Pablo N. Mendes},
  year = {2013},
  booktitle = {Proceedings of the 9th International Conference on Semantic Systems (I-Semantics)}
}
```


## Licenses

All the original code produced for DBpedia Spotlight is licensed under  [Apache License, 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). Some modules have dependencies on [LingPipe](http://alias-i.com/lingpipe/) under the [Royalty Free License](http://alias-i.com/lingpipe/licenses/lingpipe-license-1.txt). Some of our original code (currently) depends on GPL-licensed or LGPL-licensed code and is therefore also GPL or LGPL, respectively. We are currently cleaning up the dependencies to release two builds, one purely GPL and one purely Apache License, 2.0.

The documentation on this website is shared as [Creative Commons Attribution-ShareAlike 3.0 Unported License](http://en.wikipedia.org/wiki/Wikipedia:Text_of_Creative_Commons_Attribution-ShareAlike_3.0_Unported_License).


More information on citation and how to cite the deprecated Lucene version can be found [here](https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Citation).

## Documentation

More documentation is available from the [DBpedia Spotlight wiki](https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki).

## FAQ

Check the [FAQ](https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/FAQ) here
