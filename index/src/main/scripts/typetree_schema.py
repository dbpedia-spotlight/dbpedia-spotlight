"""
Convert Schema types to JSON file as source for the tree in the demo.
"""

import sys, json, csv
prefixSchema = "http://schema.org/"


csv.field_size_limit(1000000000)
typeReader = csv.reader(open(sys.argv[1]), delimiter='\t', quotechar=None)

typeCounts = {}

types = sorted([s.replace(prefixSchema, "") for (dbp, s) in typeReader])

objectOut = map(lambda x: {
                               "attr": {"id": x},
                               "data": x
                           }, types)

print json.dumps(objectOut)


