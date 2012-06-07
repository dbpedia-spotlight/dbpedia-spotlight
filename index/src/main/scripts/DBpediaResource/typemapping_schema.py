import rdflib, sys
from rdflib import Graph

g = Graph()
g.parse(sys.argv[1], format="xml")
equivalencePredicate = rdflib.term.URIRef('http://www.w3.org/2002/07/owl#equivalentClass')
prefixSchema = "http://schema.org/"
types = set()

for s, _, o in filter(lambda (s, p, o): p == equivalencePredicate and o.startswith(prefixSchema), g):
    print s + '\t' + o
