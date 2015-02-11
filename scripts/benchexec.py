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


"""
Main script of BenchExec for executing a whole benchmark (suite).

This script can be called from the command line, or from within Python
(by instantiating the BenchExec class
and either calling "instance.start()" or "main(instance)").

Naming conventions used within BenchExec:

TOOL: a (verification) tool that should be executed
EXECUTABLE: the executable file that should be called for running a TOOL
SOURCEFILE: one input file for the TOOL
RUN: one execution of a TOOL on one SOURCEFILE
RUNSET: a set of RUNs of one TOOL with at most one RUN per SOURCEFILE
RUNDEFINITION: a template for the creation of a RUNSET with RUNS from one or more SOURCEFILESETs
BENCHMARK: a list of RUNDEFINITIONs and SOURCEFILESETs for one TOOL
OPTION: a user-specified option to add to the command-line of the TOOL when it its run
CONFIG: the configuration of this script consisting of the command-line arguments given by the user
EXECUTOR: a module for executing a BENCHMARK

"run" always denotes a job to do and is never used as a verb.
"execute" is only used as a verb (this is what is done with a run).
A benchmark or a run set can also be executed, which means to execute all contained runs.

Variables ending with "file" contain filenames.
Variables ending with "tag" contain references to XML tag objects created by the XML parser.
"""

class BenchExec(object):
    """
    The main class of BenchExec.
    It is designed to be extended by inheritance, and for example
    allows configuration options to be added and the executor to be replaced.
    By default, it uses an executor that executes all runs on the local machine.
    """

    def __init__(self):
        self.executor = None
        self.stopped_by_interrupt = False

    def start(self, argv):
        """
        Start BenchExec.
        @param argv: command-line options for BenchExec
        """
        parser = self.createArgumentParser()
        self.config = parser.parse_args(argv[1:])

        for arg in self.config.files:
            if not os.path.exists(arg) or not os.path.isfile(arg):
                parser.error("File {0} does not exist.".format(repr(arg)))

        if os.path.isdir(self.config.output_path):
            self.config.output_path = os.path.normpath(self.config.output_path) + os.sep

        self.setupLogging()

        self.executor = self.loadExecutor()

        returnCode = 0
        for arg in self.config.files:
            if self.stopped_by_interrupt: break
            logging.debug("Benchmark {0} is started.".format(repr(arg)))
            rc = self.executeBenchmark(arg)
            returnCode = returnCode or rc
            logging.debug("Benchmark {0} is done.".format(repr(arg)))

        logging.debug("I think my job is done. Have a nice day!")
        return returnCode


    def createArgumentParser(self):
        """
        Create a parser for the command-line options.
        May be overwritten for adding more configuration options.
        @return: an argparse.ArgumentParser instance
        """
        parser = argparse.ArgumentParser(description=
            """Run benchmarks with a (verification) tool.
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

        parser.add_argument("--maxLogfileSize",
                          dest="maxLogfileSize", type=int, default=20,
                          metavar="MB",
                          help="Shrink logfiles to given size in MB, if they are too big. (-1 to disable, default value: 20 MB).")

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

        return parser


    def setupLogging(self):
        """
        Configure the logging framework.
        """
        if self.config.debug:
            logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                                level=logging.DEBUG)
        else:
            logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                                level=logging.INFO)


    def loadExecutor(self):
        """
        Create and return the executor module that should be used for benchmarking.
        May be overridden for replacing the executor,
        for example with an implementation that delegates to some cloud service.
        """
        import benchmark.localexecution as executor
        return executor


    def executeBenchmark(self, benchmarkFile):
        """
        Execute a single benchmark as defined in a file.
        If called directly, ensure that config and executor attributes are set up.
        @param benchmarkFile: the name of a benchmark-definition XML file
        @return: a result value from the executor module
        """
        benchmark = Benchmark(benchmarkFile, self.config,
                              self.config.startTime or time.localtime())
        self.checkExistingResults(benchmark)

        self.executor.init(self.config, benchmark)
        outputHandler = OutputHandler(benchmark, self.executor.getSystemInfo())

        logging.debug("I'm benchmarking {0} consisting of {1} run sets.".format(
                repr(benchmarkFile), len(benchmark.runSets)))

        result = self.executor.executeBenchmark(benchmark, outputHandler)

        if self.config.commit and not self.stopped_by_interrupt:
            Util.addFilesToGitRepository(self.config.output_path, outputHandler.allCreatedFiles,
                                         self.config.commitMessage+'\n\n'+outputHandler.description)
        return result


    def checkExistingResults(self, benchmark):
        """
        Check and abort if the target directory for the benchmark results
        already exists in order to avoid overwriting results.
        """
        if os.path.exists(benchmark.logFolder):
            # we refuse to overwrite existing results
            sys.exit('Output directory {0} already exists, will not overwrite existing results.'.format(benchmark.logFolder))


    def stop(self):
        """
        Stop the execution of a benchmark.
        This instance cannot be used anymore afterwards.
        Timely termination is not guaranteed, and this method may return before
        everything is terminated.
        """
        self.stopped_by_interrupt = True

        if self.executor:
            self.executor.kill()


def parse_time_arg(s):
    """
    Parse a time stamp in the "year-month-day hour-minute" format.
    """
    try:
        return time.strptime(s, "%Y-%m-%d %H:%M")
    except ValueError as e:
        raise argparse.ArgumentTypeError(e)


def signal_handler_ignore(signum, frame):
    """
    Log and ignore all signals.
    """
    logging.warn('Received signal %d, ignoring it' % signum)

def main(benchexec, argv=None):
    """
    The main method of BenchExec for use in a command-line script.
    In addition to calling benchexec.start(argv),
    it also handles signals and keyboard interrupts.
    It does not return but calls sys.exit().
    @param benchexec: An instance of BenchExec for executing benchmarks.
    @param argv: optionally the list of command-line options to use
    """
    # ignore SIGTERM
    signal.signal(signal.SIGTERM, signal_handler_ignore)
    try:
        sys.exit(benchexec.start(argv or sys.argv))
    except KeyboardInterrupt: # this block is reached, when interrupt is thrown before or after a run set execution
        benchexec.stop()
        Util.printOut("\n\nScript was interrupted by user, some runs may not be done.")

if __name__ == "__main__":
    main(BenchExec())
