"""
Convert Freebase types to JSON file as source for the tree in the demo.
"""

import sys, json, csv

csv.field_size_limit(1000000000)

allTypes = {}
typesFile = sys.argv[1]


typeReader = csv.reader(open(typesFile), delimiter='\t', quotechar=None)

typeCounts = {}

for row in typeReader:
    if row[0] == "":
        continue

    srow = row[0].split("/")
    if len(srow) == 3:
        (_, fb_domain, fb_type) = srow

        try:
            if fb_type not in allTypes[fb_domain]:
                allTypes[fb_domain].append(fb_type)
        except KeyError:
            allTypes[fb_domain] = [fb_type]

    elif len(srow) == 2:
        (_, fb_domain) = srow
    else:
        #There is only a single type which has more than two levels:""/award/award_nominee/award_nominations
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

json.dumps(objectOut)