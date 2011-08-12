import xml.etree.ElementTree as ET
import pickle
import os
import sys
import re
import urllib

kbPath = "/data/joda/12/DVD2/data/"
output = "entities.resolvedRedirects.pickle"

entities = {}

def parse(kbFile):
    tree = ET.parse(kbPath + kbFile)
    root = tree.getroot()

    for entity in root:
        occId = entity.attrib["id"]
        uri = encode(entity.attrib["wiki_title"])
        
        resolvedUri = redirects.get(uri, uri)
        if uri in disambiguations:
            print "CAUTION: found disambiguation URI: "+uri

        entities[occId] = resolvedUri

ntRegex = re.compile("<([^>]+)> <([^>]+)> <([^>]+)> .")
def stripUri(uri):
    return uri.replace("http://dbpedia.org/resource/", "")

def encode(s):
    if not s:
        return s
    encoded = s.replace(" ", "_");
    # normalize duplicate underscores
    #encoded = re.sub("_+", "_", encoded)
    # trim underscores from start
    #encoded = re.sub("^_", "", encoded)
    # trim underscores from end
    #encoded = re.sub("_$", "", encoded)
    #encoded = encoded[0].upper() + encoded[1:]
    # URL-encode everything but '/' and ':' - just like MediaWiki
    #encoded = urllib.quote(encoded)
    #encoded = re.sub("%3A", ":", encoded)
    #encoded = re.sub("%2F", "/", encoded)
    #encoded = re.sub("%26", "&", encoded)
    #encoded = re.sub("%2C", ",", encoded)
    
    encoded = re.sub("_\(", "_%28", encoded)
    encoded = re.sub("\)$", "%29", encoded)

    return encoded

def loadRedirects(fileName):
    sys.stderr.write("loading redirects\n")
    redirects = {}
    for line in open(fileName):
        m = ntRegex.search(line)
        redirects[stripUri(m.group(1))] = stripUri(m.group(3))
    sys.stderr.write(str(len(redirects)) + " redirects loaded\n")

    # transitive closure of redirects
    for s,t in redirects.iteritems():
        cyclePrevention = set()
        while t in redirects and not t in cyclePrevention:
            t = redirects[t]
            cyclePrevention.add(t)
            redirects[s] = t
    sys.stderr.write(str(len(redirects)) + " redirects loaded after transitive closure\n")

    return redirects

def loadDisambiguations(fileName):
    sys.stderr.write("loading disambiguations\n")
    disambigs = set()
    for line in open(fileName):
        m = ntRegex.search(line)
        disambigs.add(stripUri(m.group(1)))
    sys.stderr.write(str(len(disambigs)) + " disambiguations loaded\n")
    return disambigs


redirectsFile = sys.argv[1]
disambigFile = sys.argv[2]

redirects = loadRedirects(redirectsFile)
disambiguations = loadDisambiguations(disambigFile)

for kbFile in os.listdir(kbPath):
    if kbFile not in [".", ".."]:
        parse(kbFile)
		
print "Read %d entities" % len(entities)
pickle.dump(entities, open(output, "w"))

# to write out TAC-KB
#open("kb.set", "w").write("\n".join(pickle.load(open(output)).values())
