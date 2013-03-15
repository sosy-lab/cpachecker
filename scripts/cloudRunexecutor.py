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

        (wallTime, cpuTime, memUsage, returnvalue, output) = runexecutor.executeRun(args, rlimits, outputFileName);

        result = argStr + "\n"
        result = result + "Walltime: " + str(wallTime) + "\n"
        result = result +  "CpuTime: " + str(cpuTime) + "\n"
        result = result +  "MemoryUsage: " + str(memUsage) + "\n"
        result = result + "Returnvalue: " + str(returnvalue) + "\n"
        result = result + "Output:\n" + output

        with open(outputFileName, "w") as f:
            f.write(result)

        return returnvalue

    else:
        print("to few arg; expected 4 arguments:")
        print("<command> <memlimit in MB> <timelimit in s> <output file name>")



if __name__ == "__main__":
    main()
