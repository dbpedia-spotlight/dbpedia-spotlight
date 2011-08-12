"""
Resolves redirects and marks disambiguation URIs in a
DBpedia Spotlight TSV file. Outputs to standard out.
"""

import sys
import re


ntRegex = re.compile("<([^>]+)> <([^>]+)> <([^>]+)> .")
def stripUri(uri):
    return uri.replace("http://dbpedia.org/resource/", "")

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

redirects = loadRedirects(redirectsFile)
disambiguations = loadDisambiguations(disambigFile)


if __name__ == "__main__":
    annotationFile = sys.argv[1]
    redirectsFile = sys.argv[2]
    disambigFile = sys.argv[3]

    sys.stderr.write("going through data\n")
    linesCount, redirectedCount, disambigsCount = 0, 0, 0
    for line in open(annotationFile):
        id, uri, sf, text, offset = line.strip().split("\t")
        
        if uri != "NIL":  # special annotation in TAC-KBP data
            newUri = redirects.get(uri)
            if newUri != None:
                uri = newUri
                sys.stderr.write("redirect: "+uri+" -> "+newUri+"\n")
                redirectedCount += 1

            if uri in disambiguations:
                id = id + "-DISAMBIG"
                sys.stderr.write("disambig: "+uri+"\n")
                disambigsCount += 1

        print id+"\t"+uri+"\t"+sf+"\t"+text+"\t"+offset
        linesCount += 1

    sys.stderr.write("lines: "+str(linesCount)+" redirected: "+str(redirectedCount)+" disambigs: "+str(disambigsCount)+"\n")

