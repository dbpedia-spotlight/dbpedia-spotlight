-- Script to merge all occurrences for one resource.
-- These will be used later for indexing
--
-- @param dir	the directory where we find the part* files outputted by hadoop
-- @author pablomendes

SET job.name OccurrencesByTypeSortedByURI;

------ LOADING AND CLEANING ------
occurrences = LOAD '$dir/part*' USING PigStorage('\t') AS (id, uri, surfaceForm, context, offset);
types = LOAD '/user/pablo/dbpa/ontology/instance_types_en.tsv.gz' AS (uri, type);
cleaned = FILTER occurrences BY (surfaceForm is not null) AND (uri is not null);
withtype = JOIN cleaned BY uri, types BY uri;
reformat = FOREACH withtype GENERATE cleaned::uri, surfaceForm, context, offset, types::type;
STORE reformat INTO '$dir/wikipediaOccurrences.ambiguous.withtype.tsv' USING PigStorage();
