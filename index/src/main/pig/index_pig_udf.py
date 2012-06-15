#!/usr/bin/python
import sys

sys.path.append('/opt/jython/Lib/site-packages')
sys.path.append('/opt/jython/Lib')

import re

####################
#   Get doc id     #
####################
@outputSchema("reducedId:chararray")
def getDocId(fullId):
	return fullId.split("-")[0] #the portion before - is the page name

def searchDocParaPair(str):
	pattern = "(.*?p[0-9]+)l[0-9]+"
	return re.search(pattern,str) #use regex to get the doc and para

####################
# Get doc para id  #
####################
@outputSchema("reducedId:chararray")
def getDocParaId(fullId):
	result = searchDocParaPair(fullId)
	if result:
		return result.group(1)
	else:
		return ''

####################
# Get short type   #
####################
@outputSchema("type:chararray")
def getShortType(fullType):
	pattern = "<http://dbpedia.org/ontology/(.*?)>"
	result = re.search(pattern,fullType)
	if result:
		return result.group(1)
	else:
		return ''

####################
# Get short uri    #
####################
@outputSchema("uri:chararray")
def getShortUri(fullUri):
	pattern = "^<http://dbpedia.org/resource/(.*?)>$"
	result = re.search(pattern,fullUri)
	if result:
		return result.group(1)
	else:
		return ''
