#!/usr/bin/env python

import sys

reload(sys)
sys.setdefaultencoding('utf-8')

filepath = sys.argv[1]
newFileName = filepath + '.new'
data = open(filepath, 'r').read()
newFile = open(newFileName,'a')
for line in data.splitlines():
    newLine = line.decode('unicode-escape') + '\n'
    newFile.write(newLine)
newFile.close() 