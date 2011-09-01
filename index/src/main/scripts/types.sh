##
# Script for the preparation of Types (DBpedia ontology types, Freebase types).
# This script needs the zipped NTriple files from the latest DBpedia release and
# the simple Freebase TSV dump.
# 
# Files this script produces:
# - types.dbpedia.tsv: Import file for DBpedia types
# - types.freebase.tsv: Import file for Freebase types
# - typestats.freebase.tsv: The number of instances in each Freebase type
# - brokenFreebaseWikipediaLinks.tsv: Broken links between Freebase and DBpedia
# - tree.dbpedia.json: JSON file containing the DBpedia type hierarchy for the demo
# - tree.freebase.json: JSON file containing the Freebase type hierarchy for the demo
#
# @author Joachim Daiber
##

bzcat instance_types_en.nt.bz2 | sed 's|<http://dbpedia.org/resource/\([^>]*\)> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <\([^>]*\)> .|\1     \2|' > types.dbpedia.tsv
python types_freebase.py page_ids_en.nt.bz2 wikipedia_links_en.nt.bz2 freebase-simple-topic-dump.tsv.bz2 > brokenFreebaseWikipediaLinks.tsv
sort -k2n,2r typestats.freebase.tsv -o typestats.freebase.tsv
python types_freebase_to_JSON.py typestats.freebase.tsv
python types_dbpedia_to_JSON.py types.dbpedia.tsv
cp tree.freebase.json ../../../../demo/
cp tree.dbpedia.json ../../../../demo/

