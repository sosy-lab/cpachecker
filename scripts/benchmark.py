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
from __future__ import absolute_import, print_function, unicode_literals

import sys
sys.dont_write_bytecode = True # prevent creation of .pyc files


try:
  import Queue
except ImportError: # Queue was renamed to queue in Python 3
  import queue as Queue

import time
import logging
import argparse
import os
import re
import resource
import signal
import subprocess
import threading

from benchmark.benchmarkDataStructures import *
from benchmark.runexecutor import RunExecutor
import benchmark.runexecutor as runexecutor
import benchmark.util as Util
from benchmark.outputHandler import OutputHandler


MEMLIMIT = runexecutor.MEMLIMIT
TIMELIMIT = runexecutor.TIMELIMIT
CORELIMIT = runexecutor.CORELIMIT

DEFAULT_APPENGINE_URI = 'http://dev.ba-lab.appspot.com'
DEFAULT_APPENGINE_POLLINTERVAL = 60 # seconds (== 10 minutes)

# next lines are needed for stopping the script
WORKER_THREADS = []
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


class Worker(threading.Thread):
    """
    A Worker is a deamonic thread, that takes jobs from the workingQueue and runs them.
    """
    workingQueue = Queue.Queue()

    def __init__(self, number, outputHandler):
        threading.Thread.__init__(self) # constuctor of superclass
        self.numberOfThread = number
        self.outputHandler = outputHandler
        self.runExecutor = RunExecutor()
        self.setDaemon(True)
        self.start()

    def run(self):
        while not Worker.workingQueue.empty() and not STOPPED_BY_INTERRUPT:
            currentRun = Worker.workingQueue.get_nowait()
            try:
                self.execute(currentRun)
            except BaseException as e:
                print(e)
            Worker.workingQueue.task_done()


    def execute(self, run):
        """
        This function executes the tool with a sourcefile with options.
        It also calls functions for output before and after the run.
        """
        self.outputHandler.outputBeforeRun(run)

        benchmark = run.runSet.benchmark
        (run.wallTime, run.cpuTime, run.memUsage, returnvalue, output, run.energy) = \
            self.runExecutor.executeRun(
                run.getCmdline(), benchmark.rlimits, run.logFile,
                myCpuIndex=self.numberOfThread,
                environments=benchmark.getEnvironments(),
                runningDir=benchmark.workingDirectory(),
                maxLogfileSize=config.maxLogfileSize)

        if self.runExecutor.PROCESS_KILLED:
            # If the run was interrupted, we ignore the result and cleanup.
            run.wallTime = 0
            run.cpuTime = 0
            try:
                if config.debug:
                   os.rename(run.logFile, run.logFile + ".killed")
                else:
                   os.remove(run.logFile)
            except OSError:
                pass
            return

        run.afterExecution(returnvalue, output)
        self.outputHandler.outputAfterRun(run)


    def stop(self):
        # asynchronous call to runexecutor, 
        # the worker will stop asap, but not within this method.
        self.runExecutor.kill()

def executeBenchmarkLocaly(benchmark, outputHandler):
    
    runSetsExecuted = 0

    logging.debug("I will use {0} threads.".format(benchmark.numOfThreads))

    # iterate over run sets
    for runSet in benchmark.runSets:

        if STOPPED_BY_INTERRUPT: break

        (mod, rest) = config.moduloAndRest

        if not runSet.shouldBeExecuted() \
                or (runSet.index % mod != rest):
            outputHandler.outputForSkippingRunSet(runSet)

        elif not runSet.runs:
            outputHandler.outputForSkippingRunSet(runSet, "because it has no files")

        else:
            runSetsExecuted += 1
            # get times before runSet
            ruBefore = resource.getrusage(resource.RUSAGE_CHILDREN)
            wallTimeBefore = time.time()
            energyBefore = Util.getEnergy()

            outputHandler.outputBeforeRunSet(runSet)

            # put all runs into a queue
            for run in runSet.runs:
                Worker.workingQueue.put(run)

            # create some workers
            for i in range(benchmark.numOfThreads):
                WORKER_THREADS.append(Worker(i, outputHandler))

            # wait until all tasks are done,
            # instead of queue.join(), we use a loop and sleep(1) to handle KeyboardInterrupt
            finished = False
            while not finished and not STOPPED_BY_INTERRUPT:
                try:
                    Worker.workingQueue.all_tasks_done.acquire()
                    finished = (Worker.workingQueue.unfinished_tasks == 0)
                finally:
                    Worker.workingQueue.all_tasks_done.release()

                try:
                    time.sleep(0.1) # sleep some time
                except KeyboardInterrupt:
                    killScript()

            # get times after runSet
            wallTimeAfter = time.time()
            energyAfter = Util.getEnergy()
            energy = (energyAfter - energyBefore) if (energyAfter and energyBefore) else None
            usedWallTime = wallTimeAfter - wallTimeBefore
            ruAfter = resource.getrusage(resource.RUSAGE_CHILDREN)
            usedCpuTime = (ruAfter.ru_utime + ruAfter.ru_stime) \
                        - (ruBefore.ru_utime + ruBefore.ru_stime)

            outputHandler.outputAfterRunSet(runSet, cpuTime=usedCpuTime, wallTime=usedWallTime, energy=energy)

    outputHandler.outputAfterBenchmark(STOPPED_BY_INTERRUPT)


def executeBenchmark(benchmarkFile):
    benchmark = Benchmark(benchmarkFile, config, OUTPUT_PATH)
    # settings must be retrieved here to set the correct tool version
    if config.appengine:
        appengine.setupBenchmarkForAppengine(benchmark)
    outputHandler = OutputHandler(benchmark)
    
    logging.debug("I'm benchmarking {0} consisting of {1} run sets.".format(
            repr(benchmarkFile), len(benchmark.runSets)))

    if config.cloud:
        result = vcloud.executeBenchmarkInCloud(benchmark, outputHandler)
    elif config.appengine:
        result = appengine.executeBenchmarkInAppengine(benchmark, outputHandler)
    else:
        result = executeBenchmarkLocaly(benchmark, outputHandler)

    if config.commit and not STOPPED_BY_INTERRUPT:
        Util.addFilesToGitRepository(OUTPUT_PATH, outputHandler.allCreatedFiles,
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

    parser.add_argument("-t", "--test", dest="selectedRunDefinitions",
                      action="append",
                      help="Same as -r/--rundefinition (deprecated)",
                      metavar="TEST")

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

    parser.add_argument("-x", "--moduloAndRest",
                      dest="moduloAndRest", default=(1,0), nargs=2, type=int,
                      help="Run only a subset of run definitions for which (i %% a == b) holds" +
                            "with i being the index of the run definition in the benchmark definition file " +
                            "(starting with 1).",
                      metavar=("a","b"))

    parser.add_argument("-c", "--limitCores", dest="corelimit",
                      type=int, default=None,
                      metavar="N",
                      help="Limit each run of the tool to N CPU cores (-1 to disable).")

    parser.add_argument("--commit", dest="commit",
                      action="store_true",
                      help="If the output path is a git repository without local changes,"
                            + "add and commit the result files.")

    parser.add_argument("--message",
                      dest="commitMessage", type=str,
                      default="Results for benchmark run",
                      help="Commit message if --commit is used.")

    parser.add_argument("--cloud",
                      dest="cloud",
                      action="store_true",
                      help="Use cloud to execute benchmarks.")

    parser.add_argument("--cloudMaster",
                      dest="cloudMaster",
                      metavar="HOST",
                      help="Sets the master host of the cloud to be used.")

    parser.add_argument("--cloudPriority",
                      dest="cloudPriority",
                      metavar="PRIORITY",
                      help="Sets the priority for this benchmark used in the cloud. Possible values are IDLE, LOW, HIGH, URGENT.")

    parser.add_argument("--cloudCPUModel",
                      dest="cloudCPUModel", type=str, default=None,
                      metavar="CPU_MODEL",
                      help="Only execute runs on CPU models that contain the given string.")
    
    parser.add_argument("--maxLogfileSize",
                      dest="maxLogfileSize", type=int, default=20,
                      metavar="SIZE",
                      help="Shrink logfiles to SIZE in MB, if they are too big. (-1 to disable, default value: 20 MB).")
    
    parser.add_argument("--appengine",
                      dest="appengine",
                      action="store_true",
                      help="Use Google App Engine to execute benchmarks.")
    
    parser.add_argument("--appengineURI",
                      dest="appengineURI",
                      metavar="URI",
                      default=DEFAULT_APPENGINE_URI,
                      type=str,
                      help="Sets the URI to use when submitting tasks to App Engine.")
    
    parser.add_argument("--appenginePollInterval",
                      dest="appenginePollInterval",
                      metavar="INTERVAL",
                      default=DEFAULT_APPENGINE_POLLINTERVAL,
                      type=int,
                      help="Sets the interval in seconds after which App Engine is polled for results.")
    
    parser.add_argument("--appengineKeep",
                        dest="appengineDeleteWhenDone",
                        action="store_false",
                        help="If set a task will NOT be deleted from App Engine after it has successfully been executed.")

    global config, OUTPUT_PATH
    config = parser.parse_args(argv[1:])
    if os.path.isdir(config.output_path):
        OUTPUT_PATH = os.path.normpath(config.output_path) + os.sep
    else:
        OUTPUT_PATH = config.output_path


    if config.debug:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                            level=logging.DEBUG)
    else:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s")

    for arg in config.files:
        if not os.path.exists(arg) or not os.path.isfile(arg):
            parser.error("File {0} does not exist.".format(repr(arg)))

    if not config.cloud and not config.appengine:
        try:
            processes = subprocess.Popen(['ps', '-eo', 'cmd'], stdout=subprocess.PIPE).communicate()[0]
            if len(re.findall("python.*benchmark\.py", Util.decodeToString(processes))) > 1:
                logging.warn("Already running instance of this script detected. " + \
                             "Please make sure to not interfere with somebody else's benchmarks.")
        except OSError:
            pass # this does not work on Windows

    if config.cloud:
        global vcloud
        import benchmark.vcloud as vcloud
        killScriptSpecific = vcloud.killScriptCloud
    elif config.appengine:
        global appengine
        import benchmark.appengine as appengine
        killScriptSpecific = (lambda: appengine.killScriptAppEngine(config))
    else:
        killScriptSpecific = killScriptLocal

    returnCode = 0
    for arg in config.files:
        if STOPPED_BY_INTERRUPT: break
        logging.debug("Benchmark {0} is started.".format(repr(arg)))
        rc = executeBenchmark(arg)
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

def killScriptLocal():
    # kill running jobs
    Util.printOut("killing subprocesses...")
    for worker in WORKER_THREADS:
        worker.stop()

    # wait until all threads are stopped
    for worker in WORKER_THREADS:
        worker.join()


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
