"""
Reads in source files from DBpedia and Freebase and writes 
type files (TSV) for DBpedia Spotlight.

$ python types_freebase.py page_ids_en.nt.bz2 wikipedia_links_en.nt.bz2 freebase-simple-topic-dump.tsv.bz2

"""
from collections import defaultdict

import sys, csv, bz2, re
import rdflib
import getopt

#Read command line options
options, operands = getopt.getopt(sys.argv[4:], "", ["one_type_per_line"])
config = dict(options)
config.setdefault("one_type_per_line", False)

csv.field_size_limit(1000000000)

IGNORED_PREFIXES = ["/base/", "/user/", "/m/", "/common/topic", "/freebase",
                    "/influence", "/dataworld", "/common", "/type", "/atom"]
FREEBASE_RDF_PREFIX = "http://rdf.freebase.com/ns"

def filterTypes(freebase_type):

    #Ignore domains, since domain equality for Freebase is defined within DBpedia Spotlight (FreebaseType.equals(...))
    if freebase_type.count("/") < 2:
        return False

    #Ignore all of these prefixes:
    for prefix in IGNORED_PREFIXES:
        if freebase_type.startswith(prefix):
            return False
    
    return True

def getSubjectAndObject(line, predicate, safe=True):
    if safe:
        g = rdflib.Graph()
        g.parse(data=line, format="nt")
    
        subject = g.subjects().next()
        object = g.objects().next()
        return subject, object
    else:
        r = re.search("<(.*)> <" + predicate + "> <(.*)>.*", line)

        if r is not None :
            subject = r.group(1)
            object = r.group(2)
            return subject, object
        else:
            #E.g.:<http://en.wikipedia.org/wiki/AfroAsiaticLanguages> <http://dbpedia.org/ontology/wikiPageID> "40"^^<http://www.w3.org/2001/XMLSchema#integer> .
            r = re.search("<(.*)> <" + predicate + "> \"(.*)\".*", line)
            if r is not None :
                subject = r.group(1)
                object = r.group(2)
                return subject, object


def addToDictionary(ntfile, dictionary, predicate, label=None, safe=True, objects_as_list=False, reverse=False, sfunc=str, ofunc=str):

    if ntfile.endswith(".bz2"):
        f = bz2.BZ2File(ntfile)
    else:
        f = open(ntfile)

    i = 0
    for next in f:
        i += 1

        if next is None or next == "":
            break
        try:
            if reverse:
                object, subject = getSubjectAndObject(next, predicate, safe=safe)
            else:
                subject, object = getSubjectAndObject(next, predicate, safe=safe)
        except TypeError:
            continue

        subject = sfunc(subject)
        if subject not in dictionary:
            dictionary[subject] = {}

        if objects_as_list:
            if label not in dictionary[subject]:
                dictionary[subject][label] = [object]
            elif object not in dictionary[subject][label]:
                dictionary[subject][label].append(object)
        elif label is None:
            dictionary[subject] = ofunc(object)
        else:
            dictionary[subject][label] = ofunc(object)

    return dictionary


freebaseTypeDict = defaultdict(int)
def countFreebaseTypes(freebaseTypes):

    #Extend the list of types with the base types but only count them once (list -> set)
    freebaseTypesToCount = freebaseTypes[:]
    freebaseTypesToCount.extend(set(map(lambda x: "/" + x.split("/")[1], freebaseTypes)))

    for freebaseType in freebaseTypesToCount:
        freebaseTypeDict[freebaseType] += 1

def main():
    page_ids = sys.argv[1]
    wiki_links = sys.argv[2]
    freebase_dump = sys.argv[3]

    outfile_types = "types.freebase.tsv"
    outfile_freebase_identifier = "typestats.freebase.tsv"

    print >>sys.stderr, "Reading Wikipedia id to Wikipedia page mapping..."
    idToPage = addToDictionary(page_ids, {}, "http://dbpedia.org/ontology/wikiPageID", reverse=True, safe=False,
        ofunc=lambda x: str(x).replace("http://en.wikipedia.org/wiki/", ""))

    print >>sys.stderr, "Reading Wikipedia page to DBpedia mapping..."
    pageToDBpedia = addToDictionary(wiki_links, {}, "http://xmlns.com/foaf/0.1/primaryTopic", safe=False,
        sfunc=lambda x: str(x).replace("http://en.wikipedia.org/wiki/", ""),
        ofunc=lambda x: str(x).replace("http://dbpedia.org/resource/", ""))

    freebaseReader = csv.reader(bz2.BZ2File(freebase_dump), delimiter='\t')
    #freebaseTypes = {}

    print >>sys.stderr, "Processing Freebase dump..."
    typeWriter = csv.writer(open(outfile_types, "w"), delimiter='\t')
    for row in freebaseReader:
        if row[3].startswith("/wikipedia/en_"):
            #There is a match with Wikipedia:

            wikiID = row[3].replace("/wikipedia/en_id/", "")
            try:
                wikiPage = idToPage[wikiID]
                dbpediaID = pageToDBpedia[wikiPage]

                types = filter(filterTypes, row[4].split(","))
                types.extend(set(map(lambda x: x[:x.rfind("/")], types)))
                
                if len(types) > 0:
                    countFreebaseTypes(types)
                    #freebaseTypes[dbpediaID] = types
                    if config.get("one_type_per_line"):
                        for fbType in types:
                            typeWriter.writerow([dbpediaID, FREEBASE_RDF_PREFIX + fbType])
                    else:
                        typeWriter.writerow([dbpediaID, ",".join(types)])
                
            except KeyError:
                print row[0]
    
    typeIdentifierWriter = csv.writer(open(outfile_freebase_identifier, "w"), delimiter='\t')
    typeIdentifierWriter.writerows([[k, v] for (k, v) in freebaseTypeDict.items()])

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print __doc__
    else:
        main()