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

	goal = int(split[column])
	feas = int(split[column+1])
	infeas = int(split[column+2])
	buggy = int(split[column+4])
	#print(str(goal) + " " + str(feas) + " " + str(infeas) + " " +str(buggy))
	
	ratioX = (feas + infeas + buggy) * 100 / goal 


	line = linesY[i]
	split = shlex.split(line)

	goal = int(split[column])
	feas = int(split[column+1])
	infeas = int(split[column+2])
	buggy = int(split[column+4])
	#print(str(goal) + " " + str(feas) + " " + str(infeas) + " " +str(buggy))
	
	ratioY = (feas + infeas + buggy) * 100 / goal 
	data.write(str(ratioX)+"\t"+str(ratioY)+"\n")
	print(str(ratioX)+"\t"+str(ratioY))



fileX.close()
fileY.close()
data.close()

