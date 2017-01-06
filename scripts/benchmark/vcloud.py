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

import collections
import sys
sys.dont_write_bytecode = True # prevent creation of .pyc files

import json
import logging
import os
import subprocess
import time

from benchexec.model import MEMLIMIT, TIMELIMIT, CORELIMIT
import benchexec.util as util


DEFAULT_CLOUD_TIMELIMIT = 300 # s
DEFAULT_CLOUD_MEMLIMIT = None

DEFAULT_CLOUD_MEMORY_REQUIREMENT = 7000000000 # 7 GB
DEFAULT_CLOUD_CPUCORE_REQUIREMENT = 2 # one core with hyperthreading
DEFAULT_CLOUD_CPUMODEL_REQUIREMENT = "" # empty string matches every model

STOPPED_BY_INTERRUPT = False

_ROOT_DIR=os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir, os.pardir))

_justReprocessResults = False

def init(config, benchmark):
    global _justReprocessResults
    _justReprocessResults = config.reprocessResults
    benchmark.executable = benchmark.tool.executable()
    benchmark.tool_version = benchmark.tool.version(benchmark.executable)
    environment = benchmark.environment()
    if (environment.get("keepEnv", None) or environment.get("additionalEnv", None)):
        sys.exit('Unsupported environment configuration in tool-info module, '
                 'only "newEnv" is supported by VerifierCloud')

def get_system_info():
    return None

def execute_benchmark(benchmark, output_handler):
    if not _justReprocessResults:
        # build input for cloud
        (cloudInput, numberOfRuns) = getCloudInput(benchmark)
        if benchmark.config.debug:
            cloudInputFile = os.path.join(benchmark.log_folder, 'cloudInput.txt')
            util.write_file(cloudInput, cloudInputFile)
            output_handler.all_created_files.add(cloudInputFile)
        meta_information = json.dumps({"tool": {"name": benchmark.tool_name,\
                                                "revision": benchmark.tool_version, \
                                                "benchexec-module" : benchmark.tool_module}, \
                                       "benchmark" : benchmark.name,
                                       "timestamp" : benchmark.instance,
                                       "generator": "benchmark.vcloud.py"})

        # install cloud and dependencies
        ant = subprocess.Popen(["ant", "resolve-benchmark-dependencies"],
                               cwd=_ROOT_DIR,
                               shell=util.is_windows())
        ant.communicate()
        ant.wait()

        # start cloud and wait for exit
        logging.debug("Starting cloud.")
        if benchmark.config.debug:
            logLevel =  "FINER"
        else:
            logLevel = "INFO"
        heapSize = benchmark.config.cloudClientHeap + numberOfRuns//10 # 100 MB and 100 kB per run
        lib = os.path.join(_ROOT_DIR, "lib", "java-benchmark", "vcloud.jar")
        cmdLine = ["java", "-Xmx"+str(heapSize)+"m", "-jar", lib, "benchmark", "--loglevel", logLevel, \
                   "--run-collection-meta-information", meta_information, \
                   "--environment", formatEnvironment(benchmark.environment()), \
                   "--max-log-file-size", str(benchmark.config.maxLogfileSize), \
                   "--debug", str(benchmark.config.debug)]
        if benchmark.config.cloudMaster:
            cmdLine.extend(["--master", benchmark.config.cloudMaster])
        if benchmark.config.debug:
            cmdLine.extend(["--print-new-files", "true"])

        walltime_before = time.time()

        cloud = subprocess.Popen(cmdLine, stdin=subprocess.PIPE, shell=util.is_windows())
        try:
            cloud.communicate(cloudInput.encode('utf-8'))
        except KeyboardInterrupt:
            stop()
        returnCode = cloud.wait()

        walltime_after = time.time()
        usedWallTime = walltime_after - walltime_before

        if returnCode:
            if STOPPED_BY_INTERRUPT:
                output_handler.set_error('interrupted')
            else:
                errorMsg = "Cloud return code: {0}".format(returnCode)
                logging.warning(errorMsg)
                output_handler.set_error(errorMsg)
    else:
        returnCode = 0
        usedWallTime = None

    handleCloudResults(benchmark, output_handler, usedWallTime)

    return returnCode


def stop():
    global STOPPED_BY_INTERRUPT
    STOPPED_BY_INTERRUPT = True
    # kill cloud-client, should be done automatically, when the subprocess is aborted

def formatEnvironment(environment):
    return ",".join(k + "=" + v for k,v in environment.get("newEnv", {}).items())

def toTabList(l):
    return "\t".join(map(str, l))


def getCloudInput(benchmark):

    (requirements, numberOfRuns, limitsAndNumRuns, runDefinitions, sourceFiles) = getBenchmarkDataForCloud(benchmark)
    (workingDir, toolpaths) = getToolDataForCloud(benchmark)

    # prepare cloud input, we make all paths absolute, TODO necessary?
    outputDir = benchmark.log_folder
    absOutputDir = os.path.abspath(outputDir)
    absWorkingDir = os.path.abspath(workingDir)
    absToolpaths = list(map(os.path.abspath, toolpaths))
    absSourceFiles = list(map(os.path.abspath, sourceFiles))
    absBaseDir = util.common_base_dir(absSourceFiles + absToolpaths)

    if absBaseDir == "": sys.exit("No common base dir found.")

    numOfRunDefLinesAndPriorityStr = [numberOfRuns + 1] # add 1 for the headerline
    if benchmark.config.cloudPriority:
        numOfRunDefLinesAndPriorityStr.append(benchmark.config.cloudPriority)

    # build the input for the cloud,
    # see external vcloud/README.txt for details.
    cloudInput = [
                toTabList(absToolpaths),
                toTabList([absBaseDir, absOutputDir, absWorkingDir]),
                toTabList(requirements)
            ]
    if benchmark.result_files_patterns:
        if len(benchmark.result_files_patterns) > 1:
            sys.exit("Multiple result-files patterns not supported in cloud mode.")
        cloudInput.append(benchmark.result_files_patterns[0])

    cloudInput.extend([
                toTabList(numOfRunDefLinesAndPriorityStr),
                toTabList(limitsAndNumRuns)
            ])
    cloudInput.extend(runDefinitions)
    return ("\n".join(cloudInput), numberOfRuns)


def getBenchmarkDataForCloud(benchmark):

    # get requirements
    r = benchmark.requirements
    requirements = [bytes_to_mb(DEFAULT_CLOUD_MEMORY_REQUIREMENT if r.memory is None else r.memory),
                    DEFAULT_CLOUD_CPUCORE_REQUIREMENT if r.cpu_cores is None else r.cpu_cores,
                    DEFAULT_CLOUD_CPUMODEL_REQUIREMENT if r.cpu_model is None else r.cpu_model]

    # get limits and number of Runs
    timeLimit = benchmark.rlimits.get(TIMELIMIT, DEFAULT_CLOUD_TIMELIMIT)
    memLimit  = bytes_to_mb(benchmark.rlimits.get(MEMLIMIT,  DEFAULT_CLOUD_MEMLIMIT))
    coreLimit = benchmark.rlimits.get(CORELIMIT, None)
    numberOfRuns = sum(len(runSet.runs) for runSet in benchmark.run_sets if runSet.should_be_executed())
    limitsAndNumRuns = [numberOfRuns, timeLimit, memLimit]
    if coreLimit is not None: limitsAndNumRuns.append(coreLimit)

    # get Runs with args and sourcefiles
    sourceFiles = []
    runDefinitions = []
    for runSet in benchmark.run_sets:
        if not runSet.should_be_executed(): continue
        if STOPPED_BY_INTERRUPT: break

        # get runs
        for run in runSet.runs:
            cmdline = run.cmdline()
            cmdline = list(map(util.force_linux_path, cmdline))

            # we assume, that VCloud-client only splits its input at tabs,
            # so we can use all other chars for the info, that is needed to run the tool.
            argString = json.dumps(cmdline)
            assert not "\t" in argString # cannot call toTabList(), if there is a tab

            log_file = os.path.relpath(run.log_file, benchmark.log_folder)
            if os.path.exists(run.identifier):
                runDefinitions.append(toTabList([argString, log_file] + run.sourcefiles + run.required_files))
            else:
                runDefinitions.append(toTabList([argString, log_file] + run.required_files))
            sourceFiles.extend(run.sourcefiles)

    if not runDefinitions: sys.exit("Benchmark has nothing to run.")

    return (requirements, numberOfRuns, limitsAndNumRuns, runDefinitions, sourceFiles)


def getToolDataForCloud(benchmark):

    workingDir = benchmark.working_directory()
    if not os.path.isdir(workingDir):
        sys.exit("Missing working directory {0}, cannot run tool.".format(workingDir))
    logging.debug("Working dir: " + workingDir)

    toolpaths = benchmark.required_files()
    for file in toolpaths:
        if not os.path.exists(file):
            sys.exit("Missing file {0}, cannot run benchmark within cloud.".format(os.path.normpath(file)))

    return (workingDir, toolpaths)


def handleCloudResults(benchmark, output_handler, usedWallTime):

    outputDir = benchmark.log_folder
    if not os.path.isdir(outputDir) or not os.listdir(outputDir):
        # outputDir does not exist or is empty
        logging.warning("Cloud produced no results. Output-directory is missing or empty: %s", outputDir)

    # Write worker host informations in xml
    parseAndSetCloudWorkerHostInformation(outputDir, output_handler, benchmark)

    # write results in runs and handle output after all runs are done
    executedAllRuns = True
    runsProducedErrorOutput = False
    for runSet in benchmark.run_sets:
        if not runSet.should_be_executed():
            output_handler.output_for_skipping_run_set(runSet)
            continue

        output_handler.output_before_run_set(runSet)

        for run in runSet.runs:
            dataFile = run.log_file + ".data"
            if os.path.exists(dataFile) and os.path.exists(run.log_file):
                try:
                    values = parseCloudRunResultFile(dataFile)
                    if not benchmark.config.debug:
                        os.remove(dataFile)
                except IOError as e:
                    logging.warning("Cannot extract measured values from output for file %s: %s",
                                    run.identifier, e)
                    output_handler.all_created_files.add(dataFile)
                    executedAllRuns = False
                else:
                    output_handler.output_before_run(run)
                    run.set_result(values, ['host'])
                    output_handler.output_after_run(run)
            else:
                logging.warning("No results exist for file %s.", run.identifier)
                executedAllRuns = False

            if os.path.exists(run.log_file + ".stdError"):
                runsProducedErrorOutput = True

        output_handler.output_after_run_set(runSet, walltime=usedWallTime)

    output_handler.output_after_benchmark(STOPPED_BY_INTERRUPT)

    if not executedAllRuns:
        logging.warning("Some expected result files could not be found!")
    if runsProducedErrorOutput and not benchmark.config.debug:
        logging.warning("Some runs produced unexpected warnings on stderr, please check the %s files!",
                        os.path.join(outputDir, '*.stdError'))


def parseAndSetCloudWorkerHostInformation(outputDir, output_handler, benchmark):
    filePath = os.path.join(outputDir, "hostInformation.txt")
    try:
        with open(filePath, 'rt') as file:
            # Parse first part of information about hosts until first blank line
            line = file.readline().strip()
            while True:
                if not line:
                    break
                name = line.split("=")[-1].strip()
                osName = file.readline().split("=")[-1].strip()
                memory = file.readline().split("=")[-1].strip()
                cpuName = file.readline().split("=")[-1].strip()
                frequency = file.readline().split("=")[-1].strip()
                cores = file.readline().split("=")[-1].strip()
                turboBoostSupported = False
                turboBoostEnabled = False
                line = file.readline().strip()
                if line.startswith('turboboost-supported='):
                    turboBoostSupported = line.split("=")[1].lower() == 'true'
                    line = file.readline().strip()
                if line.startswith('turboboost-enabled='):
                    turboBoostEnabled = line.split("=")[1].lower() == 'true'
                    line = file.readline().strip()
                output_handler.store_system_info(osName, cpuName, cores, frequency, memory, name, None, {}, turboBoostEnabled if turboBoostSupported else None)

            # Ignore second part of information about runs
            # (we read the run-to-host mapping from the .data file for each run).

        if benchmark.config.debug:
            output_handler.all_created_files.add(filePath)
        else:
            os.remove(filePath)
    except IOError:
        logging.warning("Host information file not found: " + filePath)


IGNORED_VALUES = set(['command', 'timeLimit', 'coreLimit', 'returnvalue', 'exitsignal'])
"""result values that are ignored because they are redundant"""

def parseCloudRunResultFile(filePath):
    values = collections.OrderedDict()

    def parseTimeValue(s):
        if s[-1] != 's':
            raise ValueError('Cannot parse "{0}" as a time value.'.format(s))
        return float(s[:-1])

    with open(filePath, 'rt') as file:
        for line in file:
            (key, value) = line.split("=", 1)
            value = value.strip()
            if key == 'cputime':
                values['cputime'] = parseTimeValue(value)
            elif key == 'walltime':
                values['walltime'] = parseTimeValue(value)
            elif key == 'memory':
                values['memory'] = int(value)
            elif key == 'exitcode':
                values['exitcode'] = int(value)
            elif (key == "host" or key == "terminationreason" or
                  key.startswith("blkio-") or
                  key.startswith("cpuenergy") or
                  key.startswith("energy-") or key.startswith("cputime-cpu")):
                values[key] = value
            elif key not in IGNORED_VALUES:
                values["vcloud-" + key] = value
    return values

def bytes_to_mb(mb):
    if mb is None:
        return None
    return int(mb / 1000 / 1000)
