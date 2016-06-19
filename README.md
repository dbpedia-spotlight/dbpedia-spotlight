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

##### Download jar file and data

If you need service reliability and lower response times, you can run DBpedia Spotlight in your own [In-House Server](https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Installation). Just download a model and Spotlight from [here](http://spotlight.sztaki.hu/downloads/) to get started.

1. wget http://spotlight.sztaki.hu/downloads/dbpedia-spotlight-latest.jar
2. wget http://spotlight.sztaki.hu/downloads/latest_models/en.tar.gz
3. tar xzf en.tar.gz
4. java -jar dbpedia-spotlight-latest.jar en http://localhost:2222/rest

Note that `en` above represents the path to the english model downloaded from step 2, and
`http://localhost:2222/rest` is the mountpoint of the spotlight server.
Although you can change the base address and port, you cannot change the `/rest` mountpoint.

##### Build From source

If you want to run the latest version of spotlight (to be packaged as v0.8), you should do the following:

1. Clone this repository
2. Checkout development branch (`git checkout -b development origin/development`, where `origin` is the name of the official spotlight remote)
3. Build the package `cd dbpedia-spotlight && mvn clean package` (needs java 7 and maven installed)
4. Download an entity model (as per step 2 in the subsection above)
5. Uncompress the language model tarball (as per step 3 in the subsection above)
6. Run the spotlight server (as per step 4 in the subsection above, the jar file is build on `dist/target` folder)

Note: the current development branch (v0.8) works with the vanilla datasets provided in the steps above,
but it also works with datasets containing (i) weights for a Log-Linear Model for disambiguation,
and (ii) serialized dense vector representations (word2vec) that would be loaded and used in the desambiguation step.
The weights comprise of a simple `ranklib-model.txt` file that should be included in the language model's folder (if it's not there already) with content as follows:

```
## Coordinate Ascent
## Restart = 5
## MaxIteration = 25
## StepBase = 0.05
## StepScale = 2.0
## Tolerance = 0.001
## Regularized = false
## Slack = 0.001
1:0.37391416006434364 2:0.07140601847073497 3:0.2616870643056067 4:0.07643781575763943 5:0.21655494140167517
```

The serialized dense vectors should be placed under a `word2vec` folder inside the spotlight language model's root.
Most of the work done in the general of these vectors is done by Idio's [wiki2vec](https://github.com/idio/wiki2vec) plus some tooling.
The use of these models is extremely experimental, so testing and bug reporting is very welcome.
A full wiki on how to generate these dense vector representations and obtain the LLVM weights is in the works, but for a tentative guide see (this document)[https://github.com/phdowling/gsoc-progress/wiki/Final-Summary]

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
