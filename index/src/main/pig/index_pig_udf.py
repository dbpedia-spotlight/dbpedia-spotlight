#!/usr/bin/python

####################
# Test the udf     #
####################
@outputSchema("word:chararray")
def helloworld():  
	  return 'Hello, World'

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
