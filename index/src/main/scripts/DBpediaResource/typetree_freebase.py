"""
Convert Freebase types to JSON file as source for the tree in the demo.
"""
from collections import defaultdict
import sys, json, csv

OWL_THING = "http://www.w3.org/2002/07/owl#Thing"

csv.field_size_limit(1000000000)

allTypes = defaultdict(list)
typesFile = sys.argv[1]


typeReader = csv.reader(open(typesFile), delimiter='\t', quotechar=None)

typeCounts = {}

for row in typeReader:
    if row[0] == "":
        continue
    elif row[0] == "owl#Thing":
        owlThingCount = int(row[1])
        continue
        
    srow = row[0].split("/")
    if len(srow) == 3:
        (_, fb_domain, fb_type) = srow

        allTypes[fb_domain].append(fb_type)
    
    elif len(srow) == 2:
        (_, fb_domain) = srow
    else:
        #There is only a single type which has more than two levels:"/award/award_nominee/award_nominations
        continue


    count = int(row[1])
    typeCounts[row[0]] = count

domains = allTypes.keys()
domains.sort()

objectOut = []

special_names = {"tv" : "TV", "cvg" : "Computer and video games", "api" : "API"}
def normalizeTitle(fID):
    return fID[0].upper() + fID[1:].replace("_", " ") if fID not in special_names else special_names[fID]


import locale
locale.setlocale(locale.LC_ALL, 'en_US')

def getCounts(fID):
    return " (" + locale.format("%d", typeCounts[fID], grouping=True) + ")"

for domain in domains:
    if domain == "":
        continue

    typesInDomain = allTypes[domain]
    typesInDomain.sort()

    theDomain = {"attr": {"id": "/" + domain}, "data": normalizeTitle(domain) + getCounts("/" + domain),
                 "children": map(lambda x:
                                     {
                                     "attr": {"id": "/" + domain + "/" + x},
                                     "data": normalizeTitle(x) + getCounts("/" + domain + "/" + x)
                                 }, typesInDomain)}

    objectOut.append(theDomain)

print json.dumps([{
                "state": "open",
                "attr": {"id": OWL_THING},
                "data": "Thing (%s)" % locale.format("%d", owlThingCount, grouping=True),
                "children": objectOut
            },
            {"attr": {"id": "unknown"}, "data": "unknown type"}
        ]
    )