#!/usr/bin/python

# prepare for Python 3
from __future__ import absolute_import, print_function, unicode_literals

import sys
sys.dont_write_bytecode = True # prevent creation of .pyc files

import os
import signal
import logging
import benchmark.runexecutor as runexecutor
from benchmark.runexecutor import RunExecutor

MEMLIMIT = runexecutor.MEMLIMIT
TIMELIMIT = runexecutor.TIMELIMIT
CORELIMIT = runexecutor.CORELIMIT

WALLTIME_STR    = "wallTime"
CPUTIME_STR     = "cpuTime"
MEMORYUSAGE_STR = "memoryUsage"
RETURNVALUE_STR = "returnvalue"
ENERGY_STR      = "energy"


def main(argv=None):
    if argv is None:
        argv = sys.argv

        #sys.stderr.write(str(argv)+"\n")

    if len(argv) >= 5 and len(argv) <=6:


        rlimits={}

        data = eval(argv[1]) # arg[1] is a string-representation of a data-structure
        args = data.get("args", [])
        env = data.get("env", {})
        debugEnabled = data.get("debug", False)
        logfileSize = data.get("maxLogfileSize", 20) # MB

        if debugEnabled:
            logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                            level=logging.DEBUG)
        else:
            logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s")

        if not (argv[2]=="-1" or argv[2]=="None"):
            rlimits[MEMLIMIT] = int(argv[2])
        rlimits[TIMELIMIT] = int(argv[3])
        outputFileName = argv[4]
        if(len(argv) == 6):
             rlimits[CORELIMIT] = int(argv[5])

        global runExecutor
        runExecutor = RunExecutor()

        logging.debug("runExecutor.executeRun() started.")

        (wallTime, cpuTime, memUsage, returnvalue, output, energy) = \
            runExecutor.executeRun(args, rlimits, outputFileName, environments=env, maxLogfileSize=logfileSize);

        logging.debug("runExecutor.executeRun() ended.")

        out = {WALLTIME_STR    : wallTime,
               CPUTIME_STR     : cpuTime,
               MEMORYUSAGE_STR : memUsage,
               RETURNVALUE_STR : returnvalue,
               ENERGY_STR      : energy,
              }

        # this line dumps the result to the stdout-file.
        # the stdout-file is automatically copied back to VCloud-client.
        print(repr(out))

    else:
        sys.exit("Wrong number of arguments, expected exactly 4 or 5: <command> <memlimit in MB> <timelimit in s> <output file name> <core limit(optional)>")

def signal_handler_kill_script(signum, frame):
    runExecutor.kill()

if __name__ == "__main__":

    signal.signal(signal.SIGTERM, signal_handler_kill_script)

    main()
