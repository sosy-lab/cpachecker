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
import os
import subprocess
import time

from .benchmarkDataStructures import MEMLIMIT, TIMELIMIT, CORELIMIT
from . import util as Util


DEFAULT_CLOUD_TIMELIMIT = 300 # s
DEFAULT_CLOUD_MEMLIMIT = None

DEFAULT_CLOUD_MEMORY_REQUIREMENT = 7000 # MB
DEFAULT_CLOUD_CPUCORE_REQUIREMENT = 2 # one core with hyperthreading
DEFAULT_CLOUD_CPUMODEL_REQUIREMENT = "" # empty string matches every model

STOPPED_BY_INTERRUPT = False

_justReprocessResults = False

def init(config, benchmark):
    _justReprocessResults = config.reprocessResults

def executeBenchmark(benchmark, outputHandler):
    if not _justReprocessResults:
        # build input for cloud
        (cloudInput, numberOfRuns) = getCloudInput(benchmark)
        cloudInputFile = os.path.join(benchmark.logFolder, 'cloudInput.txt')
        Util.writeFile(cloudInput, cloudInputFile)
        outputHandler.allCreatedFiles.append(cloudInputFile)

        # install cloud and dependencies
        ant = subprocess.Popen(["ant", "resolve-benchmark-dependencies"], shell=Util.isWindows())
        ant.communicate()
        ant.wait()

        # start cloud and wait for exit
        logging.debug("Starting cloud.")
        if benchmark.config.debug:
            logLevel =  "FINER"
        else:
            logLevel = "INFO"
        heapSize = 100 + numberOfRuns//10 # 100 MB and 100 kB per run
        libDir = os.path.abspath(os.path.join(os.path.curdir, "lib", "java-benchmark"))
        cmdLine = ["java", "-Xmx"+str(heapSize)+"m", "-jar", os.path.join(libDir, "vcloud.jar"), "benchmark", "--loglevel", logLevel]
        if benchmark.config.cloudMaster:
            cmdLine.extend(["--master", benchmark.config.cloudMaster])
        if benchmark.config.debug:
            cmdLine.extend(["--print-new-files", "true"])
            
        wallTimeBefore = time.time()
            
        cloud = subprocess.Popen(cmdLine, stdin=subprocess.PIPE, shell=Util.isWindows())
        try:
            (out, err) = cloud.communicate(cloudInput.encode('utf-8'))
        except KeyboardInterrupt:
            killScriptCloud()
        returnCode = cloud.wait()

        wallTimeAfter = time.time()
        usedWallTime = wallTimeAfter - wallTimeBefore

        if returnCode:
            if STOPPED_BY_INTERRUPT:
                outputHandler.setError('interrupted')
            else:
                errorMsg = "Cloud return code: {0}".format(returnCode)
                logging.warn(errorMsg)
                outputHandler.setError(errorMsg)
    else:
        returnCode = 0    

    handleCloudResults(benchmark, outputHandler, usedWallTime)

    return returnCode


def kill():
    global STOPPED_BY_INTERRUPT
    STOPPED_BY_INTERRUPT = True
    # kill cloud-client, should be done automatically, when the subprocess is aborted


def toTabList(l):
    return "\t".join(map(str, l))


def getCloudInput(benchmark):

    (requirements, numberOfRuns, limitsAndNumRuns, runDefinitions, sourceFiles) = getBenchmarkDataForCloud(benchmark)
    (workingDir, toolpaths) = getToolDataForCloud(benchmark)

    # prepare cloud input, we make all paths absolute, TODO necessary?
    outputDir = benchmark.logFolder
    absOutputDir = os.path.abspath(outputDir)
    absWorkingDir = os.path.abspath(workingDir)
    absScriptsPath = os.path.abspath(os.path.join(os.path.dirname(__file__), os.path.pardir))
    absToolpaths = list(map(os.path.abspath, toolpaths))
    absSourceFiles = list(map(os.path.abspath, sourceFiles))
    absBaseDir = Util.commonBaseDir(absSourceFiles + absToolpaths + [absScriptsPath])

    if absBaseDir == "": sys.exit("No common base dir found.")

    numOfRunDefLinesAndPriorityStr = [numberOfRuns + 1] # add 1 for the headerline
    if benchmark.config.cloudPriority:
        numOfRunDefLinesAndPriorityStr.append(benchmark.config.cloudPriority)

    # build the input for the cloud,
    # see external vcloud/README.txt for details.
    cloudInput = [
                toTabList(absToolpaths + [absScriptsPath]),
                toTabList([absBaseDir, absOutputDir, absWorkingDir]),
                toTabList(requirements)
            ]
    if benchmark.resultFilesPattern:
        cloudInput.append(benchmark.resultFilesPattern)

    cloudInput.extend([
                toTabList(numOfRunDefLinesAndPriorityStr),
                toTabList(limitsAndNumRuns)
            ])
    cloudInput.extend(runDefinitions)
    return ("\n".join(cloudInput), numberOfRuns)


def getBenchmarkDataForCloud(benchmark):

    # get requirements
    r = benchmark.requirements
    requirements = [DEFAULT_CLOUD_MEMORY_REQUIREMENT if r.memory is None else r.memory,
                    DEFAULT_CLOUD_CPUCORE_REQUIREMENT if r.cpuCores is None else r.cpuCores,
                    DEFAULT_CLOUD_CPUMODEL_REQUIREMENT if r.cpuModel is None else r.cpuModel]

    # get limits and number of Runs
    timeLimit = benchmark.rlimits.get(TIMELIMIT, DEFAULT_CLOUD_TIMELIMIT)
    memLimit  = benchmark.rlimits.get(MEMLIMIT,  DEFAULT_CLOUD_MEMLIMIT)
    coreLimit = benchmark.rlimits.get(CORELIMIT, None)
    numberOfRuns = sum(len(runSet.runs) for runSet in benchmark.runSets if runSet.shouldBeExecuted())
    limitsAndNumRuns = [numberOfRuns, timeLimit, memLimit]
    if coreLimit is not None: limitsAndNumRuns.append(coreLimit)

    # get tool-specific environment
    env = benchmark.getEnvironments()

    # get Runs with args and sourcefiles
    sourceFiles = []
    runDefinitions = []
    for runSet in benchmark.runSets:
        if not runSet.shouldBeExecuted(): continue
        if STOPPED_BY_INTERRUPT: break

        # get runs
        for run in runSet.runs:
            cmdline = run.getCmdline()
            cmdline = list(map(Util.forceLinuxPath, cmdline))

            # we assume, that VCloud-client only splits its input at tabs,
            # so we can use all other chars for the info, that is needed to run the tool.
            # we build a string-representation of all this info (it's a map),
            # that can be parsed with python again in cloudRunexecutor.py (this is very easy with eval()) .
            argMap = {"args":cmdline, "env":env,
                      "debug":benchmark.config.debug, "maxLogfileSize":benchmark.config.maxLogfileSize}
            argString = repr(argMap)
            assert not "\t" in argString # cannot call toTabList(), if there is a tab

            logFile = os.path.relpath(run.logFile, benchmark.logFolder)
            if os.path.exists(run.identifier):
                runDefinitions.append(toTabList([argString, logFile] + run.sourcefiles + run.requiredFiles))
            else:
                runDefinitions.append(toTabList([argString, logFile] + run.requiredFiles))
            sourceFiles.extend(run.sourcefiles)

    if not runDefinitions: sys.exit("Benchmark has nothing to run.")

    return (requirements, numberOfRuns, limitsAndNumRuns, runDefinitions, sourceFiles)


def getToolDataForCloud(benchmark):

    workingDir = benchmark.workingDirectory()
    if not os.path.isdir(workingDir):
        sys.exit("Missing working directory {0}, cannot run tool.".format(workingDir))
    logging.debug("Working dir: " + workingDir)

    toolpaths = benchmark.requiredFiles()
    for file in toolpaths:
        if not os.path.exists(file):
            sys.exit("Missing file {0}, cannot run benchmark within cloud.".format(os.path.normpath(file)))

    return (workingDir, toolpaths)


def handleCloudResults(benchmark, outputHandler, usedWallTime):

    outputDir = benchmark.logFolder
    if not os.path.isdir(outputDir) or not os.listdir(outputDir):
        # outputDir does not exist or is empty
        logging.warning("Cloud produced no results. Output-directory is missing or empty: {0}".format(outputDir))

    # Write worker host informations in xml
    filePath = os.path.join(outputDir, "hostInformation.txt")
    parseAndSetCloudWorkerHostInformation(filePath, outputHandler)

    # write results in runs and handle output after all runs are done
    executedAllRuns = True
    runsProducedErrorOutput = False
    for runSet in benchmark.runSets:
        if not runSet.shouldBeExecuted():
            outputHandler.outputForSkippingRunSet(runSet)
            continue

        outputHandler.outputBeforeRunSet(runSet)

        for run in runSet.runs:
            dataFile = run.logFile + ".data"
            if os.path.exists(dataFile):
                try:
                    (run.cpuTime, run.wallTime, returnValue, values) = parseCloudRunResultFile(dataFile)
                    run.values.update(values)
                    if returnValue is not None and not benchmark.config.debug:
                        # Do not delete .data file if there was some problem
                        os.remove(dataFile)
                except IOError as e:
                    logging.warning("Cannot extract measured values from output for file {0}: {1}".format(
                                    outputHandler.formatSourceFileName(run.identifier), e))
                    outputHandler.allCreatedFiles.append(dataFile)
                    executedAllRuns = False
                    returnValue = None
            else:
                logging.warning("No results exist for file {0}.".format(outputHandler.formatSourceFileName(run.identifier)))
                executedAllRuns = False
                returnValue = None

            if os.path.exists(run.logFile + ".stdError"):
                runsProducedErrorOutput = True

            outputHandler.outputBeforeRun(run)

            run.afterExecution(returnValue)
            outputHandler.outputAfterRun(run)

        outputHandler.outputAfterRunSet(runSet, wallTime=usedWallTime)

    outputHandler.outputAfterBenchmark(STOPPED_BY_INTERRUPT)

    if not executedAllRuns:
        logging.warning("Some expected result files could not be found!")
    if runsProducedErrorOutput and not benchmark.config.debug:
        logging.warning("Some runs produced unexpected warnings on stderr, please check the {0} files!"
                        .format(os.path.join(outputDir, '*.stdError')))


def parseAndSetCloudWorkerHostInformation(filePath, outputHandler):
    try:
        with open(filePath, 'rt') as file:
            outputHandler.allCreatedFiles.append(filePath)

            # Parse first part of information about hosts until first blank line
            while True:
                line = file.readline().strip()
                if not line:
                    break
                name = line.split("=")[-1].strip()
                osName = file.readline().split("=")[-1].strip()
                memory = file.readline().split("=")[-1].strip()
                cpuName = file.readline().split("=")[-1].strip()
                frequency = file.readline().split("=")[-1].strip()
                cores = file.readline().split("=")[-1].strip()
                outputHandler.storeSystemInfo(osName, cpuName, cores, frequency, memory, name)

            # Ignore second part of information about runs
            # (we read the run-to-host mapping from the .data file for each run).

    except IOError:
        logging.warning("Host information file not found: " + filePath)


def parseCloudRunResultFile(filePath):
    values = {}
    cpuTime = None
    wallTime = None
    returnValue = None

    def parseTimeValue(s):
        if s[-1] != 's':
            raise ValueError('Cannot parse "{0}" as a time value.'.format(s))
        return float(s[:-1])

    with open(filePath, 'rt') as file:
        for line in file:
            (key, value) = line.split("=", 2)
            value = value.strip()
            if key == 'cputime':
                cpuTime = parseTimeValue(value)
            elif key == 'walltime':
                wallTime = parseTimeValue(value)
            elif key == 'cpuTime':
                cpuTime = float(value)
            elif key == 'wallTime':
                wallTime = float(value)
            elif key == 'memory':
                values['memUsage'] = value
            elif key == 'exitcode':
                returnValue = int(value)
                values['@exitcode'] = value
            elif key == "host":
                values[key] = value
            else:
                # "@" means value is hidden normally
                values["@vcloud-" + key] = value

    return (cpuTime, wallTime, returnValue, values)
