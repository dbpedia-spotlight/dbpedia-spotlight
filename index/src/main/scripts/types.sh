# Script for the preparation of Types (DBpedia ontology types, Freebase types).
# This script needs the zipped NTriple files from the latest DBpedia release and
# the Freebase simple TSV dump.
# 
# Files this script produces:
# - types.dbpedia.tsv: Import file for DBpedia types
# - types.freebase.tsv: Import file for Freebase types
# - typestats.freebase.tsv: The number of instances in each Freebase type
# - tree.freebase.json: JSON file containing the display of the types for demo
# - missing.tsv: Broken links between Freebase and DBpedia
#
# @author Joachim Daiber

bzcat instance_types_en.nt.bz2 | sed 's|<http://dbpedia.org/resource/\([^>]*\)> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <\([^>]*\)> .|\1     \2|' > types.dbpedia.tsv
python types_freebase.py page_ids_en.nt.bz2 wikipedia_links_en.nt.bz2 freebase-simple-topic-dump.tsv.bz2 > missing.tsv
sort -k2n,2r typestats.freebase.tsv -o typestats.freebase.tsv
python types_freebase_to_JSON.py typestats.freebase.tsv