"""
Convert Freebase types to JSON file as source for the tree in the demo.
"""

import sys, json, csv

csv.field_size_limit(1000000000)

allTypes = {}
typesFile = sys.argv[1]

typeReader = csv.reader(open(typesFile), delimiter='\t', quotechar=None)

for row in typeReader:
	if row[0] == "":
		continue
	try:
		(_, fb_domain, fb_type) = row[0].split("/")
	except ValueError:
		"""
		There is only a single type which has more than two levels:
		/award/award_nominee/award_nominations
		"""
		continue
	
	try:
		if fb_type not in allTypes[fb_domain]:
			allTypes[fb_domain].append(fb_type)
	except KeyError:
		allTypes[fb_domain] = [fb_type]

domains = allTypes.keys()
domains.sort()

objectOut = {"children" : []}

def makeJSONObject(typeID):
	return {
		"data" : typeID,
		"attr" : {"id" : typeID}
	}

for domain in domains:
	typesInDomain = allTypes[domain]
	typesInDomain.sort()
	
	theDomain = makeJSONObject("/" + domain)
	theDomain["children"] = map(lambda x: makeJSONObject("/%s/%s" % (domain, x)), typesInDomain)
	
	objectOut["children"].append(theDomain)

json.dump(objectOut, open("tree.freebase.json", "w"))



