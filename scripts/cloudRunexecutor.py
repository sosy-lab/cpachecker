#!/usr/bin/python

import sys
import benchmark.runexecutor as runexecutor

MEMLIMIT = runexecutor.MEMLIMIT
TIMELIMIT = runexecutor.TIMELIMIT
CORELIMIT = runexecutor.CORELIMIT

def main(argv=None):
    if argv is None:
        argv = sys.argv
        
        #sys.stderr.write(str(argv)+"\n");

    if len(argv) >= 5 and len(argv) <=6:
        
        rlimits={}
        
        argStr = argv[1]
        args = argStr.split(" ");
        if(not (argv[2]=="-1" or argv[2]=="None")):
            rlimits[MEMLIMIT] = int(argv[2])
        rlimits[TIMELIMIT] = int(argv[3])
        outputFileName = argv[4]
        if(len(argv) == 6):
             rlimits[CORELIMIT] = int(argv[5])
        (wallTime, cpuTime, memUsage, returnvalue, output) = runexecutor.executeRun(args, rlimits, outputFileName);

        print("Walltime: " + str(wallTime))
        print("CpuTime: " + str(cpuTime))
        print("MemoryUsage: " + str(memUsage))
        print("Returnvalue: " + str(returnvalue))

        return returnvalue

    else:
        sys.exit("Wrong number of arguments, expected exactly 4 or 5: <command> <memlimit in MB> <timelimit in s> <output file name> <core limit(optional)>")

if __name__ == "__main__":
    main()
