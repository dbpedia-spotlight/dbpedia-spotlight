-- Script to merge all occurrences for one resource.
-- These will be used later for indexing
--
-- @param dir	the directory where we find the part* files outputted by hadoop
-- @author pablomendes

SET job.name AmbiguousSurfaceFormOccurrencesSortedByF1;

------ LOADING AND CLEANING ------
occurrences = LOAD '$dir/part*' USING PigStorage('\t') AS (id, uri, surfaceForm, context, offset);
cleaned = FILTER occurrences BY (surfaceForm is not null) AND (uri is not null);
grouped = GROUP cleaned BY surfaceForm PARALLEL 6;
filtered = FILTER grouped BY ($1 is not null) AND (COUNT($1) > 1); 
reformat = FOREACH filtered GENERATE FLATTEN($1) AS (uri, surfaceForm, context, offset);
sorted = ORDER reformat BY uri;
STORE sorted INTO '$dir/wikipediaOccurrences.ambiguous.tsv' USING PigStorage();
