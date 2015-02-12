#!/usr/bin/python
"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2014  Dirk Beyer
All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


CPAchecker web page:
  http://cpachecker.sosy-lab.org
"""

# prepare for Python 3
from __future__ import absolute_import, division, print_function, unicode_literals

import sys
sys.dont_write_bytecode = True # prevent creation of .pyc files

import argparse
import logging
import benchmark.runexecutor
import signal
from benchmark import util

"""
A simple command-line interface for the runexecutor module of BenchExec.
"""

_BYTE_FACTOR = 1000 # byte in kilobyte

def main(argv=None):
    if argv is None:
        argv = sys.argv

    # parse options
    parser = argparse.ArgumentParser(description=
        "Run a command with resource limits and measurements.")
    parser.add_argument("args", nargs="+", metavar="ARG",
                        help='command line to run (prefix with "--" to ensure all arguments are treated correctly)')
    parser.add_argument("--output", default="output.log", metavar="FILE",
                        help="file name for file with command output")
    parser.add_argument("--maxOutputSize", type=int, metavar="BYTES",
                        help="approximate size of command output after which it will be truncated")
    parser.add_argument("--memlimit", type=int, metavar="BYTES",
                        help="memory limit in bytes")
    parser.add_argument("--timelimit", type=int, metavar="SECONDS",
                        help="CPU time limit in seconds")
    parser.add_argument("--softtimelimit", type=int, metavar="SECONDS",
                        help='"soft" CPU time limit in seconds')
    parser.add_argument("--walltimelimit", type=int, metavar="SECONDS",
                        help='wall time limit in seconds (default is CPU time plus a few seconds)')
    parser.add_argument("--cores", type=util.parse_int_list, metavar="N,M-K",
                        help="the list of CPU cores to use")
    parser.add_argument("--memoryNodes", type=util.parse_int_list, metavar="N,M-K",
                        help="the list of memory nodes to use")
    parser.add_argument("--dir", metavar="DIR",
                        help="working directory for executing the command (default is current directory)")
    verbosity = parser.add_mutually_exclusive_group()
    verbosity.add_argument("--debug", action="store_true",
                           help="Show debug output")
    verbosity.add_argument("--quiet", action="store_true",
                           help="Show only warnings")
    options = parser.parse_args(argv[1:])

    # For integrating into some benchmarking frameworks,
    # there is a DEPRECATED special mode
    # where the first and only command-line argument is a serialized dict
    # with additional options
    env = {}
    if len(options.args) == 1 and options.args[0].startswith("{"):
        data = eval(options.args[0])
        options.args = data["args"]
        env = data.get("env", {})
        options.debug = data.get("debug", options.debug)
        if "maxLogfileSize" in data:
            options.maxOutputSize = data["maxLogfileSize"] * _BYTE_FACTOR * _BYTE_FACTOR # MB to bytes

    # setup logging
    logLevel = logging.INFO
    if options.debug:
        logLevel = logging.DEBUG
    elif options.quiet:
        logLevel = logging.WARNING
    logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                        level=logLevel)

    executor = benchmark.runexecutor.RunExecutor()

    # ensure that process gets killed on interrupt/kill signal
    def signal_handler_kill(signum, frame):
        executor.kill()
    signal.signal(signal.SIGTERM, signal_handler_kill)
    signal.signal(signal.SIGINT,  signal_handler_kill)

    logging.info('Starting command ' + ' '.join(options.args))
    logging.info('Writing output to ' + options.output)

    # actual run execution
    result = \
        executor.execute_run(args=options.args,
                            output_filename=options.output,
                            hardtimelimit=options.timelimit,
                            softtimelimit=options.softtimelimit,
                            walltimelimit=options.walltimelimit,
                            cores=options.cores,
                            memlimit=options.memlimit,
                            memory_nodes=options.memoryNodes,
                            environments=env,
                            workingDir=options.dir,
                            maxLogfileSize=options.maxOutputSize)

    # exit_code is a special number:
    # It is a 16bit int of which the lowest 7 bit are the signal number,
    # and the high byte is the real exit code of the process (here 0).
    exit_code = result['exitcode']
    return_value = exit_code // 256
    exitSignal = exit_code % 128

    def print_optional_result(key):
        if key in result:
            # avoid unicode literals such that the string can be parsed by Python 3.2
            print(key + "=" + str(result[key]).replace("'u", ''))

    # output results
    print_optional_result('terminationreason')
    print("exitcode=" + str(exit_code))
    if (exitSignal == 0) or (return_value != 0):
        print("returnvalue=" + str(return_value))
    if exitSignal != 0 :
        print("exitsignal=" + str(exitSignal))
    print("walltime=" + str(result['walltime']) + "s")
    print("cputime=" + str(result['cputime']) + "s")
    print_optional_result('memory')
    if 'energy' in result:
        for key, value in result['energy'].items():
            print("energy-{0}={1}".format(key, value))

if __name__ == "__main__":
    sys.exit(main())
