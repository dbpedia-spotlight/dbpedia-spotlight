##
# Script for the preparation of Types (DBpedia ontology types, Freebase types, Schema.org types).
# This script needs zipped NTriple files from the latest DBpedia release and
# the simple Freebase TSV dump.
# 
# Files this script produces:
# - types.dbpedia.tsv: Import file for DBpedia types
# - types.freebase.tsv: Import file for Freebase types
# - typemapping.schema_org.tsv: Type mapping file from DBpedia types to Schema.org types
# - typestats.freebase.tsv: The number of instances in each Freebase type
# - brokenFreebaseWikipediaLinks.tsv: Broken links between Freebase and DBpedia
# - tree.dbpedia.json: JSON file containing the DBpedia type hierarchy for the demo
# - tree.freebase.json: JSON file containing the Freebase type hierarchy for the demo
# - tree.freebase.json: JSON file containing the Freebase type hierarchy for the demo
# - tree.schema.json: JSON file containing the Schema type hierarchy for the demo
#
# @author Joachim Daiber
##

#Read DBpedia instance types:
bzcat instance_types_en.nt.bz2 | sed 's|<http://dbpedia.org/resource/\([^>]*\)> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <\([^>]*\)> .|\1     \2|' > types.dbpedia.tsv

#Read and extract Freebase types:
python types_freebase.py page_ids_en.nt.bz2 wikipedia_links_en.nt.bz2 freebase-simple-topic-dump.tsv.bz2 > brokenFreebaseWikipediaLinks.tsv
sort -k2n,2r typestats.freebase.tsv -o typestats.freebase.tsv

#Read Schema.org type mapping from the ontology file:
python typemapping_schema.py dbpedia_3.7.owl > typemapping.schema_org.tsv

#Write updated type selection trees to the demo directory
python typetree_freebase.py typestats.freebase.tsv > ../../../../demo/tree.freebase.json
python typetree_dbpedia.py types.dbpedia.tsv > ../../../../demo/tree.dbpedia.json
python typetree_schema.py typemapping.schema_org.tsv > ../../../../demo/tree.schema.json
