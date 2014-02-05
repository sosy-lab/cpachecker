#!/usr/bin/env python

import subprocess, shlex, sys, re, time

if len(sys.argv) < 4:
        sys.stderr.write("Usage: resultsX resultsY column name\n")
        exit(1)

fileX = open(sys.argv[1],'r')
fileY = open(sys.argv[2],'r')
column = int(sys.argv[3])
data=  open(sys.argv[4],'w')

skip=2
i=0

linesX = fileX.readlines()
linesY = fileY.readlines()

for i in range(skip+1,len(linesX)):
	line = linesX[i]
	split = shlex.split(line)
	valX = split[column]
	
	line = linesY[i]
	split = shlex.split(line)
	valY = split[column]
	
	data.write(valX+"\t"+valY+"\n")
	print(valX+"\t"+valY)



fileX.close()
fileY.close()
data.close()

