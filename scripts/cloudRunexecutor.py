#!/usr/bin/python

import sys
import benchmark.runexecutor as runexecutor

def main(argv=None):
    if argv is None:
        argv = sys.argv

    if(len(argv) >= 5):
        argStr = argv[1]
        args = argStr.split(" ");
        memlimit = int(argv[2])
        timelimit = int(argv[3])
        outputFileName = argv[4]

        rlimits={"memlimit":memlimit, "timelimit":timelimit}
        print args
        print rlimits
        print outputFileName
        
        (wallTime, cpuTime, memUsage, returnvalue, output) = runexecutor.executeRun(args, rlimits, outputFileName);

        print wallTime
        print cpuTime
        print returnvalue
        print output

        result = argStr + "\n"
        result = result + "Walltime: " + str(wallTime) + "\n"
        result = result +  "CpuTime: " + str(cpuTime) + "\n"
        result = result +  "MemoryUsage: " + str(memUsage) + "\n"
        result = result + "Returnvalue: " + str(returnvalue) + "\n"   
        result = result + "Output:\n" + output

        print result

        fobj = open(outputFileName, "w")
        fobj.write(result)
        fobj.close()
	
	return returnvalue

    else:
        print "to few arg; expected 4 arguments:"
	print "<command> <memlimit in MB> <timelimit in s> <output file name>"



if __name__ == "__main__":
    main()
