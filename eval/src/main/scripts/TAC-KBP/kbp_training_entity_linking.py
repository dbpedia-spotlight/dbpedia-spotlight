# -*- coding: utf-8 -*-

import xml.etree.ElementTree as ET
#from xml.etree.ElementTree import _escape_cdata
import xml.sax.saxutils

import pickle
import simplejson as json
from sgmllib import SGMLParser
import sgmllib
import string
import re

import csv, codecs, cStringIO

def find_all(a_str, sub):
    start = 0
    while True:
        start = a_str.find(sub, start)
        if start == -1: return
        yield start
        start += len(sub)

class UTF8Recoder:
    """
    Iterator that reads an encoded stream and reencodes the input to UTF-8
    """
    def __init__(self, f, encoding):
        self.reader = codecs.getreader(encoding)(f)

    def __iter__(self):
        return self

    def next(self):
        return self.reader.next().encode("utf-8")

class UnicodeReader:
    """
    A CSV reader which will iterate over lines in the CSV file "f",
    which is encoded in the given encoding.
    """

    def __init__(self, f, dialect=csv.excel, encoding="utf-8", **kwds):
        f = UTF8Recoder(f, encoding)
        self.reader = csv.reader(f, dialect=dialect, **kwds)

    def next(self):
        row = self.reader.next()
        return [unicode(s, "utf-8") for s in row]

    def __iter__(self):
        return self

class UnicodeWriter:
    """
    A CSV writer which will write rows to CSV file "f",
    which is encoded in the given encoding.
    """

    def __init__(self, f, dialect=csv.excel, encoding="utf-8", **kwds):
        # Redirect output to a queue
        self.queue = cStringIO.StringIO()
        self.writer = csv.writer(self.queue, dialect=dialect, **kwds)
        self.stream = f
        self.encoder = codecs.getincrementalencoder(encoding)()

    def writerow(self, row):
        self.writer.writerow([s.encode("utf-8") for s in row])
        # Fetch UTF-8 output from the queue ...
        data = self.queue.getvalue()
        data = data.decode("utf-8")
        # ... and reencode it into the target encodingclass TextExtractor(SGMLParser):
	def reset(self):
		SGMLParser.reset(self)
		self.urls = []
	
	def __init__(self, verbose=0):
		sgmllib.SGMLParser.__init__(self, verbose)
		self.title = self.data = None

	def handle_data(self, data):
		if self.data is not None:
			self.data.append(data)

	def start_post(self, attrs):
		self.data = []

	def end_post(self):
		self.text = u"".join(self.data)
		raise FoundPost # abort parsing!
        data = self.encoder.encode(data)
        # write to the target stream
        self.stream.write(data)
        # empty queue
        self.queue.truncate(0)

    def writerows(self, rows):
        for row in rows:
            self.writerow(row)


class FoundPost(Exception):
    pass

class FoundMultiplePosts(Exception):
    pass

class TextExtractor(SGMLParser):
	def reset(self):
		SGMLParser.reset(self)
		self.urls = []
	
	def __init__(self, sf, verbose=0):
		sgmllib.SGMLParser.__init__(self, verbose)
		self.title = self.data = None
		self.sf = sf

	def handle_data(self, data):
		if self.data is not None:
			self.data.append(data)

	def start_post(self, attrs):
		self.data = []

	def end_post(self):
		self.text = u"".join(self.data).replace("-\n", "-").replace(" \n", " ").replace("\n", " ")
		
		if self.sf in self.text:
			raise FoundPost # abort parsing!
		else:
			pass		


class TextExtractorNews(SGMLParser):
	def reset(self):
		SGMLParser.reset(self)
		self.urls = []
	
	def __init__(self, sf, verbose=0):
		sgmllib.SGMLParser.__init__(self, verbose)
		self.title = self.data = None
		self.texts = []
		self.sf = sf

	def handle_data(self, data):
		if self.data is not None:
			self.data.append(data)

	def start_body(self, attrs):
		self.data = []

	def end_body(self):
		self.text = u"".join(self.data).replace("-\n", "-").replace(" \n", " ").replace("\n", " ")
		
		if self.sf in self.text:
			self.texts.append(self.text)		
			#raise FoundPost # abort parsing!
		else:
			pass
	def start_doc(self, attrs):
		pass

	def end_doc(self):
		if len(self.texts) > 0:
			raise FoundMultiplePosts # abort parsing!

#Read entity/DBpedia Resource mapping
entities = pickle.load(open("entities.resolvedRedirects.pickle"))

entitiesForQueries = {}
def loadEntitiesForQueries(file):
	for line in open(file):
		lineS = line.split("\t")
		entitiesForQueries[lineS[0]] = lineS[1]

#1. XML file to read:
#xmlFile = "/data/joda/12/TAC_2010_KBP_Evaluation_Entity_Linking_Gold_Standard_V1.0/data/tac_2010_kbp_evaluation_entity_linking_queries.xml"
#xmlFile = "/data/joda/12/TAC_2011_KBP_English_Evaluation_Entity_Linking_Queries/data/tac_2011_kbp_english_evaluation_entity_linking_queries.xml"
#xmlFile = "/data/joda/12/TAC_2010_KBP_Evaluation_Entity_Linking_Gold_Standard_V1.0/TAC_2010_KBP_Evaluation_Entity_Linking_Gold_Standard_V1.1/data/tac_2010_kbp_evaluation_entity_linking_queries.xml"
#xmlFile = "/data/joda/12/TAC_2009_KBP_Evaluation_Entity_Linking_List/data/entity_linking_queries.xml"
xmlFile = "/data/joda/12/TAC_2010_KBP_Training_Entity_Linking_V2.0/data/tac_2010_kbp_training_entity_linking_queries.xml"



#2. Query->Entity mapping from TAB file:
#loadEntitiesForQueries("/data/joda/12/TAC_2010_KBP_Evaluation_Entity_Linking_Gold_Standard_V1.0/data/tac_2010_kbp_evaluation_entity_linking_query_types.tab")
#loadEntitiesForQueries("/data/joda/12/TAC_2010_KBP_Evaluation_Entity_Linking_Gold_Standard_V1.0/TAC_2010_KBP_Evaluation_Entity_Linking_Gold_Standard_V1.1/data/tac_2010_kbp_evaluation_entity_linking_query_types.tab")
#loadEntitiesForQueries("/data/joda/12/TAC_2009_KBP_Gold_Standard_Entity_Linking_Entity_Type_List/data/Gold_Standard_Entity_Linking_List_with_Entity_Types.tab")
loadEntitiesForQueries("/data/joda/12/TAC_2010_KBP_Training_Entity_Linking_V2.0/data/tac_2010_kbp_training_entity_linking_query_types.tab")

#3. Output file to write:
outFile = "TAC-KBP-training-2010.tsv"


def getEntityForQuery(queryID):
	return entitiesForQueries[queryID]


#Path to text directory
textPath = "/data/joda/12/DVD1/TAC_2010_KBP_Source_Data/data/"
sourceFolder = {
	"AFP_ENG" : "2009/nw/afp_eng",
	"APW_ENG" : "2009/nw/apw_eng",
	"CNA_ENG" : "2009/nw/cna_eng",
	"LTW_ENG" : "2009/nw/ltw_eng",
	"NYT_ENG" : "2009/nw/nyt_eng",
	"REU_ENG" : "2009/nw/reu_eng",
	"XIN_ENG" : "2009/nw/xin_eng",
	"default1": "2009/wb/",
	"default2": "2010/wb/"
}

def getSourceFolder(docID):
	folderID = docID.split(".")[0]
	for (sID, sPath) in sourceFolder.items():
		if folderID.startswith(sID):
			return (sPath + "/" + folderID[8:] + "/",)
	return (sourceFolder["default1"], sourceFolder["default2"])

def getEntity(entityID):
	if entityID.startswith("NIL"):
		return u"NIL"
	else:
		return entities[entityID]

def getText(docID, sf):
	if docID == 'eng-WL-11-174588-12938415':
		print "HERE, ", sf

	if len(getSourceFolder(docID)) == 1:
		#It's news
		t = TextExtractorNews(sf)
	else:
		#It's anything else
		t = TextExtractor(sf)

	try:
		for sF in getSourceFolder(docID):
			try:			
				t.feed(codecs.open(textPath + sF + docID + ".sgm", "r", "utf-8").read())
				print textPath + sF + docID + ".sgm", "r", "utf-8"
			except IOError:
				pass
		t.close()
	except FoundPost:
		return t.text.replace("\n", " ")
	except FoundMultiplePosts:
		return "\n".join(t.texts).replace("\n", " ")

	print "I SHOULD NOT BE HERE!", docID, sf
	
	

tree = ET.parse(xmlFile)
root = tree.getroot() #ET.Element("kbpentlink")

queries = []
for query in root:
	annotation = { "qid" : query.attrib["id"] }

	for child in query:
		if child.tag == "name":
			annotation["surfaceForm"] = xml.sax.saxutils.unescape(child.text)
		if child.tag == "docid":
			annotation["docID"] = child.text
		if child.tag == "entity":
			annotation["resource"] = getEntity(child.text)
	
	if "resource" not in annotation and len(entitiesForQueries) > 0:
		annotation["resource"] = getEntity(getEntityForQuery(annotation["qid"]))
	elif "resource" not in annotation and len(entitiesForQueries) == 0:
		annotation["resource"] = ""
	
	#Append the annotation query
	queries.append(annotation)


annotatedTexts = {}
i = 0

class out_dialect(csv.Dialect): 
	delimiter = '\t' 
	skipinitialspace = True 
	quotechar = None
	doublequote = False
	quoting = csv.QUOTE_NONE
	lineterminator = '\r\n'
	escapechar = '\\'

writer = UnicodeWriter(open(outFile, "w"), dialect=out_dialect)

fails = 0
success = 0
for annotation in queries:
	if annotation["qid"] not in annotatedTexts:
		annotatedTexts[annotation["qid"]] = {
			"text" :getText(annotation["docID"], annotation["surfaceForm"]),
			"annotations" : []
		}
	
	for offset in find_all(annotatedTexts[annotation["qid"]]["text"], annotation["surfaceForm"]):
		try:
			writer.writerow((annotation["qid"], annotation["resource"], annotation["surfaceForm"], annotatedTexts[annotation["qid"]]["text"], unicode(offset)))
			success += 1
			i += 1
		except UnicodeDecodeError:
			fails += 1
		break #Only output the first!
	
	if i % 1000 == 0:
		print u"Wrote %d annotations." % i

print "Wrote %d annotations (fails because of codec: %d, success: %d)." % (fails+success, fails, success)



