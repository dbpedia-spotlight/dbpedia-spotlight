

"""
Convert DBpedia types to JSON file as source for the tree in the demo.
"""
from collections import defaultdict

import sys, json, csv
import locale

csv.field_size_limit(1000000000)
locale.setlocale(locale.LC_ALL, 'en_US')

PREFIX_ONTOLOGY = "http://dbpedia.org/ontology/"

uriToName = lambda uri: uri[uri.rfind("/")+1:]

allTypes = {}

typesFile = sys.argv[1]
typeReader = csv.reader(open(typesFile), delimiter='\t', quotechar=None)
typeCounts = defaultdict(int)

objectOut = []

currentURI = ""
currentTypes = []

def processCurrent():
    """
    Count up the instances for the types and all their parents of the current URI.
    """

    if currentTypes in [
            ["PersonFunction", "owl#Thing"],
            ["AutomobileEngine", "Device", "owl#Thing"],
            ["Sales", "owl#Thing"]
        ]:
        currentTypes.reverse()

    for theTypes in (currentTypes[0:x] for x in range(1, len(currentTypes)+1)):
        typeCounts["/".join(theTypes)] += 1


for (uri, theType) in typeReader:
    
    if uri != currentURI:
        processCurrent()
        currentURI = uri
        currentTypes = []

    currentTypes.append(uriToName(theType))
    currentURI = uri

processCurrent()


countedTypes = {}
for (typeStr, count) in typeCounts.items():
    #elements = filter(lambda x: len(x.split("/")) == i, typeCounts)
    #print i, elements
    print typeStr
    c = countedTypes
    for theType in typeStr.split("/"):
        try:
            c = c[theType]
        except KeyError:
            c[theType] = {}
            c = c[theType]
    c["__count"] = count

def objectToOut(theKey, theObject):
    label = theKey + " (" + locale.format("%d", theObject["__count"], grouping=True) + ")"

    #Remove count
    del theObject["__count"]
    
    outObject = {"attr": {"id": theKey}, "data": label}

    children = map(lambda subKey: objectToOut(subKey, theObject[subKey]), sorted(theObject.keys()))
    if len(children) > 0:
        outObject["children"] = children

    return outObject

outObject = objectToOut("owl#Thing", countedTypes["owl#Thing"])

#The owl#Thing element should be open:
outObject["state"] = "open"
unknown = {"attr": {"id": "unknown"}, "data": "unknown type"}

json.dumps([outObject, unknown])