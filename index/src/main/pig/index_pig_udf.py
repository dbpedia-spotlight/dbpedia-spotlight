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


############################
# Check doc para id format #
############################
#@outputSchema("isDocParaPair:int")
#def checkDocParaFormat(str):
#	if searchDocParaPair(str):
#		return 1
#	else:
#		return 0

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
# Get Combinations #
####################
@outputSchema("y:bag{t:tuple(ent1:chararray,ent2:chararray)}")
def getCombination(bag):
	outBag = []
	for i in range(len(bag)):
		for j in range(i+1,len(bag)):
			if bag[i] < bag[j]:
				outBag.append((bag[i][0],bag[j][0]))
			else:
				outBag.append((bag[j][0],bag[i][0]))
	return outBag
