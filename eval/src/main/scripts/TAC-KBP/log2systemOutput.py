import pickle
import sys

m = pickle.load(open("/home/jodaiber/Desktop/entities.resolvedRedirects.pickle"))
d = dict([(v,k) for k,v in m.iteritems()])

nilDict = {}
nilCounter = 0

first = True
out = {}
for line in open(sys.argv[1]):
    if first:
        first=False
        continue
    
    lineS = line.strip().split("\t")
    occId = lineS[0]
    surfaceForm = lineS[2]
    dbpediaUri = lineS[4]
    fields = dbpediaUri.split("/") # what happens with pure NILs?
    dbpediaUri = fields[0]
    if len(fields)<2:
	alternativeUri = surfaceForm
    else: 
    	alternativeUri = fields[1]     # this is what we found before we decided to output NIL 
    
    if dbpediaUri == "NIL":
	if alternativeUri not in nilDict:
		nilCounter += 1
		nilDict[alternativeUri] = "NIL"+str(nilCounter).zfill(3)
	dbpediaUri = nilDict[alternativeUri] 
    if occId not in out:
	out[occId] = []
    out[occId].append(dbpediaUri)

from itertools import groupby as g

for (occId, uris) in out.items():
    uri = max(g(sorted(uris)), key=lambda(x, v):(len(list(v)),-uris.index(x)))[0]
    
    print occId + "\t" + d.get(uri, uri)



