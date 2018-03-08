**IMPORTANT: No longer in active development.  Please use https://github.com/dbpedia-spotlight/dbpedia-spotlight-model instead.**

---


# DBpedia Spotlight 


### Links

website - http://www.dbpedia-spotlight.org

status service	- http://status.dbpedia-spotlight.org

download service - http://download.dbpedia-spotlight.org

demo service - http://demo.dbpedia-spotlight.org

CI -http://jenkins.dbpedia-spotlight.org



### General Notes

Since v1.0, DBpedia Spotlight was split into two versions, under the same API,  as follow:

  - DBpedia-Spotlight-Model: described in [Improving Efficiency and Accuracy in Multilingual Entity Extraction](http://jodaiber.de/doc/entity.pdf) - Repo: https://github.com/dbpedia-spotlight/dbpedia-spotlight-model

  - DBpedia-Spotlight-Lucene: described in [DBpedia Spotlight: Shedding Light on the Web of Documents](http://www.dbpedia-spotlight.org/docs/spotlight.pdf)  - Repo: - https://github.com/dbpedia-spotlight/dbpedia-spotlight - No longer in active development**
  
We will keep this repository just to historical references. Every issue opened should be closed and reopened in their respective repositories.

This important movement was the way that we found to deliver faster fixes and new releases, providing solutions for each annotation approach.

Our first achievement is related with licensing. DBpedia Spotlight Model is now full compliance with Apache 2.0. It means that you can use it without any commercial restrictions.

We are so excited because there's even more great news to come.

If you require any further information, feel free to contact us via dbpedia@infai.org. We are already very excited to spend time with you on further community meetings and to publish new DBpedia releases.

Keep annotating,

All the best




#### Shedding Light on the Web of Documents

DBpedia Spotlight looks for ~3.5M things of unknown or ~320 known types in text and tries to link them to their global unique identifiers in [DBpedia](http://dbpedia.org). 

#### Demonstration

Go to our [Demonstration](http://dbpedia-spotlight.github.io/demo/) page, copy+paste some text and play with the parameters to see how it works.

#### Call our web service

You can use our demonstration [Web Service](http://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Web-service) directly from your application.

    curl http://model.dbpedia-spotlight.org/en/annotate  \
      --data-urlencode "text=President Obama called Wednesday on Congress to extend a tax break
      for students included in last year's economic stimulus package, arguing
      that the policy provides more generous assistance." \
      --data "confidence=0.35"

or for JSON:

    curl http://model.dbpedia-spotlight.org/en/annotate  \
      --data-urlencode "text=President Obama called Wednesday on Congress to extend a tax break
      for students included in last year's economic stimulus package, arguing
      that the policy provides more generous assistance." \
      --data "confidence=0.35" \
      -H "Accept: application/json"

#### Run your own server

If you need service reliability and lower response times, you can run DBpedia Spotlight in your own [In-House Server](https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Installation). Just download a model and Spotlight from [here](http://model.dbpedia-spotlight.org) to get started.

    wget http://downloads.dbpedia-spotlight.org/spotlight/dbpedia-spotlight-0.7.1.jar
    wget http://downloads.dbpedia-spotlight.org/2016-04/en/model/en.tar.gz
    tar xzf en.tar.gz
    java -jar dbpedia-spotlight-latest.jar en http://localhost:2222/rest

#### Models and data

Models and raw data for most languages are available [here](http://model.dbpedia-spotlight.org).

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


More information on citation and how to cite the deprecated Lucene version can be found [here](http://www.dbpedia-spotlight.org/publications).

## Documentation

More documentation is available from the [DBpedia Spotlight wiki](https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki).

## FAQ

Check the [FAQ here](https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/faq) 


## Maintainers

<a href="http://infai.org"><img src="http://infai.org/de/Presse/Logos/files?get=infai_logo_en_rgb_300dpi.jpg" align="left" height="20%" width="20%" ></a>
