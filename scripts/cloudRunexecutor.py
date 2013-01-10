#!/usr/bin/python

import sys
import benchmark.runexecutor as runexecutor

def main(argv=None):
    if argv is None:
        argv = sys.argv

    if(len(argv) >= 4):
        args = argv[1]
        rlimits = argv[2] 
        outputFileName = argv[3]
        print args
        print rlimits
        print outputFileName
        
        (wallTime, cpuTime, returnvalue, output) = runexecutor.executeRun(args, rlimits, outputFileName);

        print wallTime
        print cpuTime
        print returnvalue
        print output

        result = args + "\n"
        result = result + "Walltime: " + str(wallTime) + "\n"
        result = result +  "CpuTime: " + str(wallTime) + "\n"
        result = result + "Returnvalue: " + str(returnvalue) + "\n"
        result = result + "\n"        
        result = result + "Output:\n" + output

        print result

        fobj = open(outputFileName, "w")
        fobj.write(result)
        fobj.close()


if __name__ == "__main__":
    main()
