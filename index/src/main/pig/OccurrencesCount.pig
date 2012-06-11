-- Script to merge all occurrences for one resource.
-- And count the occurrences of the resource

-- @param occPath	the path to occs.tsv or occs.uriSorted.tsv files outputted by ExtractOccsFromWikipedia
-- @param typesPath	the path to instance_type_en_nt
-- @param outDir the path to output
-- output format  uri, occ count, {list of occ id}, {list of type}, if occ id does not have para id and line id, it is a self occurrence

SET job.name OccurrencesCountedWithType;

--register udf
Register 'index_pig_udf.py' using jython as funcs;

%default occPath occs.tsv
%default typesPath instance_types_en.nt
%default outDir . -- do not include slash at the end

------ LOADING AND CLEANING FOR OCCS ------
occurrences = LOAD '$occPath' USING PigStorage('\t') AS (id:chararray, uri:chararray, surfaceForm:chararray, context:chararray, offset:chararray);
cleaned = FILTER occurrences BY (surfaceForm is not null) AND (uri is not null);

/*
------ LOAD TYPES ------
fullTypes = LOAD '$typesPath' USING PigStorage(' ') AS (uri:chararray, property,type:chararray);
types = FOREACH fullTypes GENERATE (funcs.getShortUri(uri)), (funcs.getShortType(type));

withtype = JOIN cleaned BY uri, types BY uri;


grouped = GROUP withtype BY cleaned::uri;

counts = FOREACH grouped {
    id = withtype.cleaned::id;
    uri = withtype.cleaned::uri;
    type = withtype.types::type;
    GENERATE group,COUNT(uri),id,type;
} */

grouped = GROUP cleaned BY uri;

counts = FOREACH grouped{
    uri = cleaned.uri;
    id = cleaned.id;
    GENERATE group, COUNT(uri), id;
}

--this line is likedly to create memory problem
--result = ORDER counts BY group;

STORE counts INTO '$outDir/occurrences-count-ids.tsv' USING PigStorage();
