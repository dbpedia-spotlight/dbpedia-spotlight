# DBpedia Spotlight
#### Shedding Light on the Web of Documents

DBpedia Spotlight looks for ~3.5M things of ~320 types (or unknown type) in text and tries to link them to their global unique identifiers in [DBpedia](http://dbpedia.org). 

#### Demonstration

Go to our [demonstration](http://spotlight.dbpedia.org/demo/) page, copy+paste some text and play with the parameters to see how it works.

#### Call our web service

You can use our demonstration [Web Service](http://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Web-service) directly from your application.

    curl http://spotlight.dbpedia.org/rest/annotate \
      --data-urlencode "text=President Obama called Wednesday on Congress to extend a tax break
      for students included in last year's economic stimulus package, arguing
      that the policy provides more generous assistance." \
      --data "confidence=0.2" \
      --data "support=20"

#### Run your own server

If you need service reliability and lower response times, you can run DBpedia Spotlight in your own [InHouse-Server](http://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/InHouse_Server).

    wget http://spotlight.dbpedia.org/download/release-0.5/dbpedia-spotlight-quickstart.zip
    unzip dbpedia-spotlight-quickstart.zip
    cd dbpedia-spotlight-quickstart/
    ./run.sh

#### Build from source

We provide a [Java/Scala API](http://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Java%2FScala%20API) for you to use our code in your application.
More info [here](http://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Java%2FScala%20API).

[![Build Status](https://secure.travis-ci.org/dbpedia-spotlight/dbpedia-spotlight.png?branch=master)](http://travis-ci.org/dbpedia-spotlight/dbpedia-spotlight)

## Introduction

DBpedia Spotlight is a tool for automatically annotating mentions of DBpedia resources in text, providing a solution for linking unstructured information sources to the Linked Open Data cloud through DBpedia. DBpedia Spotlight recognizes that names of concepts or entities have been mentioned (e.g. "Michael Jordan"), and subsequently matches these names to unique identifiers (e.g. [dbpedia:Michael_I._Jordan](http://dbpedia.org/page/Michael_I._Jordan), the machine learning professor or [dbpedia:Michael_Jordan](http://dbpedia.org/page/Michael_Jordan) the basketball player). It can also be used for building your solution for [Named Entity Recognition](http://en.wikipedia.org/wiki/Named_entity_recognition), Keyphrase Extraction, Tagging, etc. amongst other information extraction tasks.

Text annotation has the potential of enhancing a wide range of applications, including search, faceted browsing and navigation. By connecting text documents with DBpedia, our system enables a range of interesting use cases. For instance, the ontology can be used as background knowledge to display complementary information on web pages or to enhance information retrieval tasks. Moreover, faceted browsing over documents and customization of web feeds based on semantics become feasible. Finally, by following links from DBpedia into other data sources, the Linked Open Data cloud is pulled closer to the Web of Documents.

Take a look at our [Known Uses] (http://dbpedia.org/spotlight/knownuses) page for other examples of how DBpedia Spotlight can be used. If you use DBpedia Spotlight in your project, please add a link to http://spotlight.dbpedia.org. If you use it in a paper, please use the citation available in the end of this page.

You can try out DBpedia Spotlight through our Web Application or Web Service endpoints. The Web Application is a user interface that allows you to enter text in a form and generates an HTML annotated version of the text with links to DBpedia. The Web Service endpoints provide programmatic access to the demo, allowing you to retrieve data also in XML or JSON.
## Documentation

We split the documentation according to the depth at which we give explanations. Please feel free to take a look at our:
  * [User's Manual](http://dbpedia.org/spotlight/usersmanual), if you are not interested in details of how things happen, but you would like to use the system in your website or software project.
  * [Technical Documentation](http://dbpedia.org/spotlight/technicaldocumentation), if you want to have an overview of technical details before you go into the source code.
  * [Source code](http://sourceforge.net/projects/dbp-spotlight/), if you really want to know every detail, our source code is open, free and loves to meet new people. ;)


## Downloads

DBpedia Spotlight looks for ~3.5M things of ~320 types in text and tries to disambiguate them to their global unique identifiers in DBpedia. It uses the entire Wikipedia in order to learn how to annotate DBpedia Resources, the entire dataset cannot be distributed alongside the code, and can be downloaded in varied sizes from the [download page](http://dbpedia.org/spotlight/downloads). A tiny dataset is included in the distribution for demonstration purposes only.
After you've downloaded the files, you need to modify the configuration in server.properties with the correct path to the files. More info [here](https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Installation).

## Licenses

The program can be used under the terms of the [Apache License, 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
Part of the code uses [LingPipe](http://alias-i.com/lingpipe/) under the [Royalty Free License](http://alias-i.com/lingpipe/licenses/lingpipe-license-1.txt). Therefore, this license may also apply to the output of the currently deployed web service.

The documentation on this website is shared as [Creative Commons Attribution-ShareAlike 3.0 Unported License](http://en.wikipedia.org/wiki/Wikipedia:Text_of_Creative_Commons_Attribution-ShareAlike_3.0_Unported_License)

## Citation

If you use this work on your research, please cite:

Pablo N. Mendes, Max Jakob, Andrés García-Silva and Christian Bizer. [DBpedia Spotlight: Shedding Light on the Web of Documents](http://www.wiwiss.fu-berlin.de/en/institute/pwo/bizer/research/publications/Mendes-Jakob-GarciaSilva-Bizer-DBpediaSpotlight-ISEM2011.pdf). *Proceedings of the 7th International Conference on Semantic Systems (I-Semantics)*. Graz, Austria, 7–9 September 2011. 

```bibtex
@inproceedings{isem2011mendesetal,
  title = {DBpedia Spotlight: Shedding Light on the Web of Documents},
  author = {Pablo N. Mendes and Max Jakob and Andr\'{e}s Garc\'{i}a-Silva and Christian Bizer},
  year = {2011},
  booktitle = {Proceedings of the 7th International Conference on Semantic Systems (I-Semantics)},
  abstract = {Interlinking text documents with Linked Open Data enables the Web of Data to be used as background knowledge within document-oriented applications such as search and faceted browsing. As a step towards interconnecting the Web of Documents with the Web of Data, we developed DBpedia Spotlight, a system for automatically annotating text documents with DBpedia URIs. DBpedia Spotlight allows users to configure the annotations to their specific needs through the DBpedia Ontology and quality measures such as prominence, topical pertinence, contextual ambiguity and disambiguation confidence. We compare our approach with the state of the art in disambiguation, and evaluate our results in light of three baselines and six publicly available annotation systems, demonstrating the competitiveness of our system. DBpedia Spotlight is shared as open source and deployed as a Web Service freely available for public use.}
}
```

The corpus used to evaluate DBpedia Spotlight in this work is described [here](http://wiki.dbpedia.org/spotlight/evaluation).

## Support and Feedback
The best way to get help with DBpedia Spotlight is to send a message to our [mailing list](https://lists.sourceforge.net/mailman/listinfo/dbp-spotlight-users) at *dbp-spotlight-users@lists.sourceforge.net*.

You can also join the #dbpedia-spotlight IRC channel on Freenode. We also hear [Tweets](http://search.twitter.com/search.atom?q=+dbpedia+spotlight).

We'd love if you gave us some feedback.



## Team

The DBpedia Spotlight team includes the names cited below. Individual contributions are acknowledged in the source code and publications.

#### Maintainers
[Pablo Mendes](http://www.wiwiss.fu-berlin.de/en/institute/pwo/bizer/team/MendesPablo.html) (Freie Universität Berlin), Jun 2010-present.

[Max Jakob](http://www.wiwiss.fu-berlin.de/en/institute/pwo/bizer/team/JakobMax.html) (Freie Universität Berlin), Jun 2010-Sep 2011, Apr 2012-present.

[Jo Daiber](http://jodaiber.de/) (Charles University in Prague), Mar 2011-present.

Prof. Dr. [Chris Bizer](http://www.wiwiss.fu-berlin.de/en/institute/pwo/bizer/team/BizerChristian.html) (Freie Universität Berlin),  supervisor, Jun 2010-present.

#### Collaborators
[Andrés García-Silva](http://grafias.dia.fi.upm.es/Sem4Tags/about.html) (Universidad Politécnica de Madrid), Jul-Dec 2010.

[Rohana Rajapakse](http://www.linkedin.com/pub/rohana-rajapakse/3/9a1/8) (Goss Interactive Ltd.), Oct-2011.


## Acknowledgements

This work has been funded by:
  * [Neofonie GmbH](http://www.neofonie.de/), a Berlin-based company offering leading technologies in the area of Web search, social media and mobile applications. (Jun 2010-Jun 2011)
  * The European Commission through the project [LOD2 - Creating Knowledge out of Linked Data](http://lod2.eu/). (Jun 2010-present)
