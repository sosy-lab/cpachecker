#!/usr/bin/env python

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


import logging
import argparse
import os
import signal
import time

from benchmark.benchmarkDataStructures import Benchmark
import benchmark.util as Util
from benchmark.outputHandler import OutputHandler


DEFAULT_APPENGINE_URI = 'http://cpachecker.appspot.com'
DEFAULT_APPENGINE_POLLINTERVAL = 60 # seconds

# next lines are needed for stopping the script
STOPPED_BY_INTERRUPT = False

"""
Naming conventions:

TOOL: a verifier program that should be executed
EXECUTABLE: the executable file that should be called for running a TOOL
SOURCEFILE: one file that contains code that should be verified
RUN: one execution of a TOOL on one SOURCEFILE
RUNSET: a set of RUNs of one TOOL with at most one RUN per SOURCEFILE
RUNDEFINITION: a template for the creation of a RUNSET with RUNS from one or more SOURCEFILESETs
BENCHMARK: a list of RUNDEFINITIONs and SOURCEFILESETs for one TOOL
OPTION: a user-specified option to add to the command-line of the TOOL when it its run
CONFIG: the configuration of this script consisting of the command-line arguments given by the user

"run" always denotes a job to do and is never used as a verb.
"execute" is only used as a verb (this is what is done with a run).
A benchmark or a run set can also be executed, which means to execute all contained runs.

Variables ending with "file" contain filenames.
Variables ending with "tag" contain references to XML tag objects created by the XML parser.
"""


def executeBenchmark(benchmarkFile, executor, config, outputPath):
    benchmark = Benchmark(benchmarkFile, config, outputPath,
                          config.startTime or time.localtime())
    executor.init(config, benchmark)
    outputHandler = OutputHandler(benchmark)
    
    logging.debug("I'm benchmarking {0} consisting of {1} run sets.".format(
            repr(benchmarkFile), len(benchmark.runSets)))

    result = executor.executeBenchmark(benchmark, outputHandler)

    if config.commit and not STOPPED_BY_INTERRUPT:
        Util.addFilesToGitRepository(outputPath, outputHandler.allCreatedFiles,
                                     config.commitMessage+'\n\n'+outputHandler.description)
    return result


def main(argv=None):

    if argv is None:
        argv = sys.argv
    parser = argparse.ArgumentParser(description=
        """Run benchmarks with a verification tool.
        Documented example files for the benchmark definitions
        can be found as 'doc/examples/benchmark*.xml'.
        Use the table-generator.py script to create nice tables
        from the output of this script.""")

    parser.add_argument("files", nargs='+', metavar="FILE",
                      help="XML file with benchmark definition")
    parser.add_argument("-d", "--debug",
                      action="store_true",
                      help="Enable debug output")

    parser.add_argument("-r", "--rundefinition", dest="selectedRunDefinitions",
                      action="append",
                      help="Run only the specified RUN_DEFINITION from the benchmark definition file. "
                            + "This option can be specified several times.",
                      metavar="RUN_DEFINITION")

    parser.add_argument("-s", "--sourcefiles", dest="selectedSourcefileSets",
                      action="append",
                      help="Run only the files from the sourcefiles tag with SOURCE as name. "
                            + "This option can be specified several times.",
                      metavar="SOURCES")

    parser.add_argument("-n", "--name",
                      dest="name", default=None,
                      help="Set name of benchmark execution to NAME",
                      metavar="NAME")

    parser.add_argument("-o", "--outputpath",
                      dest="output_path", type=str,
                      default="./test/results/",
                      help="Output prefix for the generated results. "
                            + "If the path is a folder files are put into it,"
                            + "otherwise it is used as a prefix for the resulting files.")

    parser.add_argument("-T", "--timelimit",
                      dest="timelimit", default=None,
                      help="Time limit in seconds for each run (-1 to disable)",
                      metavar="SECONDS")

    parser.add_argument("-M", "--memorylimit",
                      dest="memorylimit", default=None,
                      help="Memory limit in MB (-1 to disable)",
                      metavar="MB")

    parser.add_argument("-N", "--numOfThreads",
                      dest="numOfThreads", default=None, type=int,
                      help="Run n benchmarks in parallel",
                      metavar="n")

    parser.add_argument("-c", "--limitCores", dest="corelimit",
                      type=int, default=None,
                      metavar="N",
                      help="Limit each run of the tool to N CPU cores (-1 to disable).")

    parser.add_argument("--commit", dest="commit",
                      action="store_true",
                      help="If the output path is a git repository without local changes, "
                            + "add and commit the result files.")

    parser.add_argument("--message",
                      dest="commitMessage", type=str,
                      default="Results for benchmark run",
                      help="Commit message if --commit is used.")

    parser.add_argument("--startTime",
                      dest="startTime",
                      type=parse_time_arg,
                      default=None,
                      metavar="'YYYY-MM-DD hh:mm'",
                      help='Set the given date and time as the start time of the benchmark.')

    vcloud_args = parser.add_argument_group('Options for using VerifierCloud')
    vcloud_args.add_argument("--cloud",
                      dest="cloud",
                      action="store_true",
                      help="Use VerifierCloud to execute benchmarks.")

    vcloud_args.add_argument("--cloudMaster",
                      dest="cloudMaster",
                      metavar="HOST",
                      help="Sets the master host of the VerifierCloud instance to be used. If this is a HTTP URL, the web interface is used.")

    vcloud_args.add_argument("--cloudPriority",
                      dest="cloudPriority",
                      metavar="PRIORITY",
                      help="Sets the priority for this benchmark used in the VerifierCloud. Possible values are IDLE, LOW, HIGH, URGENT.")

    vcloud_args.add_argument("--cloudCPUModel",
                      dest="cloudCPUModel", type=str, default=None,
                      metavar="CPU_MODEL",
                      help="Only execute runs in the VerifierCloud on CPU models that contain the given string.")
   
    vcloud_args.add_argument("--cloudUser",
                      dest="cloudUser",
                      metavar="USER:PWD",
                      help="The user and password for the VerifierCloud (if using the web interface).")

    vcloud_args.add_argument("--revision",
                      dest="revision",
                      metavar="BRANCH:REVISION",
                      help="The svn revision of CPAchecker to use  (if using the web interface of the VerifierCloud).")

    vcloud_args.add_argument("--justReprocessResults",
                      dest="reprocessResults",
                      action="store_true",
                      help="Do not run the benchmarks. Assume that the benchmarks were already executed in the VerifierCloud and the log files are stored (use --startTime to point the script to the results).")
    
    parser.add_argument("--maxLogfileSize",
                      dest="maxLogfileSize", type=int, default=20,
                      metavar="SIZE",
                      help="Shrink logfiles to SIZE in MB, if they are too big. (-1 to disable, default value: 20 MB).")

    appengine_args = parser.add_argument_group('Options for using CPAchecker in the AppEngine')
    appengine_args.add_argument("--appengine",
                      dest="appengine",
                      action="store_true",
                      help="Use Google App Engine to execute benchmarks.")
    
    appengine_args.add_argument("--appengineURI",
                      dest="appengineURI",
                      metavar="URI",
                      default=DEFAULT_APPENGINE_URI,
                      type=str,
                      help="Sets the URI to use when submitting tasks to App Engine.")
    
    appengine_args.add_argument("--appenginePollInterval",
                      dest="appenginePollInterval",
                      metavar="INTERVAL",
                      default=DEFAULT_APPENGINE_POLLINTERVAL,
                      type=int,
                      help="Sets the interval in seconds after which App Engine is polled for results.")
    
    appengine_args.add_argument("--appengineKeep",
                        dest="appengineDeleteWhenDone",
                        action="store_false",
                        help="If set a task will NOT be deleted from App Engine after it has successfully been executed.")

    config = parser.parse_args(argv[1:])
    if os.path.isdir(config.output_path):
        outputPath = os.path.normpath(config.output_path) + os.sep
    else:
        outputPath = config.output_path


    if config.debug:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                            level=logging.DEBUG)
    else:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                            level=logging.INFO)

    for arg in config.files:
        if not os.path.exists(arg) or not os.path.isfile(arg):
            parser.error("File {0} does not exist.".format(repr(arg)))

    # Allow local execution of benchmarks to be easily replaced
    # by a different module that delegates to some cloud service.
    if config.cloud:
        if config.cloudMaster and "http" in config.cloudMaster:
            import benchmark.webclient as executor
        else:
            import benchmark.vcloud as executor
    elif config.appengine:
        import benchmark.appengine as executor
    else:
        import benchmark.localexecution as executor
    killScriptSpecific = executor.kill

    returnCode = 0
    for arg in config.files:
        if STOPPED_BY_INTERRUPT: break
        logging.debug("Benchmark {0} is started.".format(repr(arg)))
        rc = executeBenchmark(arg, executor, config, outputPath)
        returnCode = returnCode or rc
        logging.debug("Benchmark {0} is done.".format(repr(arg)))

    logging.debug("I think my job is done. Have a nice day!")
    return returnCode


def killScript():
    # set global flag
    global STOPPED_BY_INTERRUPT
    STOPPED_BY_INTERRUPT = True

    killScriptSpecific()

def killScriptSpecific():
    pass


def parse_time_arg(s):
    try:
        return time.strptime(s, "%Y-%m-%d %H:%M")
    except ValueError as e:
        raise argparse.ArgumentTypeError(e)


def signal_handler_ignore(signum, frame):
    logging.warn('Received signal %d, ignoring it' % signum)

if __name__ == "__main__":
    # ignore SIGTERM
    signal.signal(signal.SIGTERM, signal_handler_ignore)
    try:
        sys.exit(main())
    except KeyboardInterrupt: # this block is reached, when interrupt is thrown before or after a run set execution
        killScript()
        Util.printOut("\n\nScript was interrupted by user, some runs may not be done.")
