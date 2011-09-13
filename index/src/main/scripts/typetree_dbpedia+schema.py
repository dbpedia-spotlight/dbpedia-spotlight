

"""
Convert DBpedia types to JSON file as source for the tree in the demo.
"""
from collections import defaultdict

import sys, json, csv
import locale, re
import getopt

#Read command line options
options, operands = getopt.getopt(sys.argv[2:], "", ["out_dbpedia=", "out_schema="])
config = dict(options)
config.setdefault("--out_dbpedia","tree.dbpedia.json")
config.setdefault("--out_schema", "tree.dbpedia.json")

csv.field_size_limit(1000000000)
locale.setlocale(locale.LC_ALL, 'en_US')

PREFIX_ONTOLOGY = "http://dbpedia.org/ontology/"
PREFIX_SCHEMA = "http://schema.org/"
OWL_THING = "http://www.w3.org/2002/07/owl#Thing"

uriToName = lambda uri: uri[uri.rfind("/")+1:]

allTypes = {}

typesFile = sys.argv[1]
typeReader = csv.reader(open(typesFile), delimiter='\t', quotechar=None)

typeCountsDBpedia = defaultdict(int)
typeCountsSchema = defaultdict(int)

objectOut = []

currentTypesDBpedia = []
currentTypesSchema = []

def processCurrent():
    """
    Count up the instances for the types and all their parents of the current URI.
    """

    #if currentTypesDBpedia in [
    #        ["PersonFunction", "owl#Thing"],
    #        ["AutomobileEngine", "Device", "owl#Thing"],
    #        ["Sales", "owl#Thing"]
    #    ]:
    currentTypesDBpedia.reverse() #This seems to be new in 3.7: TODO, check order...

    if len(currentTypesSchema) > 0:
        currentTypesSchema.insert(0, "owl#Thing")

    for theTypes in (currentTypesDBpedia[0:x] for x in range(1, len(currentTypesDBpedia)+1)):
        typeCountsDBpedia["/".join(theTypes)] += 1

    for theTypes in (currentTypesSchema[0:x] for x in range(1, len(currentTypesSchema)+1)):
        typeCountsSchema["/".join(theTypes)] += 1


currentURI = ""
for (uri, theType) in typeReader:

    if uri != currentURI:
        processCurrent()
        currentURI = uri
        currentTypesDBpedia = []
        currentTypesSchema = []

    if (theType.startswith(PREFIX_ONTOLOGY) or theType == OWL_THING) and re.match(r".*__[1-9]+", uri) is None:
        currentTypesDBpedia.append(uriToName(theType))

    if theType.startswith(PREFIX_SCHEMA):
        currentTypesSchema.append(uriToName(theType))

    currentURI = uri

processCurrent()


def jstreeFromTypeCounts(typeCounts):
    countedTypes = {}
    for (typeStr, count) in typeCounts.items():
        c = countedTypes
        for theType in typeStr.split("/"):
            try:
                c = c[theType]
            except KeyError:
                c[theType] = {}
                c = c[theType]
        c["__count"] = count
    
    return countedTypes


def keyToName(theKey):
    if theKey == "owl#Thing":
        return "Thing"
    else:
        return theKey

def getTreeObject(theKey, theObject):
    label = keyToName(theKey) + " (" + locale.format("%d", theObject["__count"], grouping=True) + ")"

    #Remove count
    del theObject["__count"]
    
    outObject = {"attr": {"id": theKey}, "data": label}

    children = map(lambda subKey: getTreeObject(subKey, theObject[subKey]), sorted(theObject.keys()))
    if len(children) > 0:
        outObject["children"] = children

    return outObject

##
# DBpedia:
##
countedTypes = jstreeFromTypeCounts(typeCountsDBpedia)
outObjectDBpedia = getTreeObject("owl#Thing", countedTypes["owl#Thing"])

#The owl#Thing element should be open:
outObjectDBpedia["state"] = "open"
unknown = {"attr": {"id": "unknown"}, "data": "unknown type"}

json.dump([outObjectDBpedia, unknown], open(config["--out_dbpedia"], "w"))

##
# Schema.org:
##
countedTypes = jstreeFromTypeCounts(typeCountsSchema)
outObjectSchema = getTreeObject("owl#Thing", countedTypes["owl#Thing"])
outObjectSchema["state"] = "open"
unknown = {"attr": {"id": "unknown"}, "data": "unknown type"}
json.dump([outObjectSchema, unknown], open(config["--out_schema"], "w"))