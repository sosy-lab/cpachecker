#!/usr/bin/python

import sys
import benchmark.runexecutor as runexecutor

def main(argv=None):
    if argv is None:
        argv = sys.argv

    if len(argv) == 5:
        argStr = argv[1]
        args = argStr.split(" ");
        memlimit = int(argv[2])
        timelimit = int(argv[3])
        outputFileName = argv[4]

        rlimits={"memlimit":memlimit, "timelimit":timelimit}

        (wallTime, cpuTime, memUsage, returnvalue, output) = runexecutor.executeRun(args, rlimits, outputFileName);

        print("Walltime: " + str(wallTime))
        print("CpuTime: " + str(cpuTime))
        print("MemoryUsage: " + str(memUsage))
        print("Returnvalue: " + str(returnvalue))

        return returnvalue

    else:
        sys.exit("Wrong number of arguments, expected exactly 4: <command> <memlimit in MB> <timelimit in s> <output file name>")

if __name__ == "__main__":
    main()
