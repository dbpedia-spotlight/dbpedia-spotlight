-- Script to merge all occurrences for one resource.
-- These will be used later for indexing
--
-- @param dir	the directory where we find the part* files outputted by hadoop
-- @author pablomendes

SET job.name MergeResources;

------ LOADING AND CLEANING ------
occurrences = LOAD '$dir/*' USING PigStorage('\t') AS (id, uri, surfaceForm, context, offset);
grouped = GROUP occurrences BY uri;
counts = FOREACH grouped GENERATE FLATTEN(group) AS uri, group.surfaceForm, group.context;
sorted = ORDER counts BY uri;
STORE sorted INTO '$dir/resources.merged' USING PigDump();