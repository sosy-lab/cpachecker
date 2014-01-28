#!/usr/bin/env python

"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2013  Dirk Beyer
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
from datetime import datetime, timedelta
import logging
import argparse
import os
import re
import resource
import signal
import subprocess
import threading
import json
import urllib2

from benchmark.benchmarkDataStructures import *
from benchmark.runexecutor import RunExecutor
import benchmark.runexecutor as runexecutor
import benchmark.util as Util
import benchmark.filewriter as filewriter
from benchmark.outputHandler import OutputHandler

MEMLIMIT = runexecutor.MEMLIMIT
TIMELIMIT = runexecutor.TIMELIMIT
CORELIMIT = runexecutor.CORELIMIT

DEFAULT_CLOUD_TIMELIMIT = 3600 # s
DEFAULT_CLOUD_MEMLIMIT = None

DEFAULT_CLOUD_MEMORY_REQUIREMENT = 15000 # MB
DEFAULT_CLOUD_CPUCORE_REQUIREMENT = 1 # one core
DEFAULT_CLOUD_CPUMODEL_REQUIREMENT = "" # empty string matches every model

DEFAULT_APPENGINE_URI = 'http://dev.ba-lab.appspot.com'
DEFAULT_APPENGINE_POLLINTERVAL = 180 # seconds (== 3 minutes)
APPENGINE_TIMEOUT_GRACE_PERIOD = 120 # seconds (== 2 minutes)
APPENGINE_SUBMITTER_THREAD = None
APPENGINE_POLLER_THREAD = None
APPENGINE_JOBS = []
# these will be fetched from the server
DEFAULT_APPENGINE_TIMELIMIT = 0
APPENGINE_RETRIES = 0
APPENGINE_STATS_FILENAME = ''
APPENGINE_ERROR_FILENAME = ''

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

        (run.wallTime, run.cpuTime, run.memUsage, returnvalue, output) = \
            self.runExecutor.executeRun(
                run.getCmdline(), run.benchmark.rlimits, run.logFile,
                myCpuIndex=self.numberOfThread,
                environments=run.benchmark.getEnvironments(),
                runningDir=run.benchmark.workingDirectory(),
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

class AppEngineSubmitter(threading.Thread):
    def __init__(self, runDefinitions, benchmark):
        threading.Thread.__init__(self)
        self.runDefinitions = runDefinitions
        self.benchmark = benchmark
        self.done = False
        self.submittedJobs = 0

    def run(self):
        for run in self.runDefinitions:
            if STOPPED_BY_INTERRUPT: break
            args = run['args']
            with open(run['sourcefile'], 'r') as f:
                args['programText'] = f.read()
            
            uri = config.appengineURI+'/jobs'
            data = json.dumps(args)
            headers = {'Content-type':'application/json', 'Accept':'application/json'}
            try:
                request = urllib2.Request(uri, data, headers)
                response = urllib2.urlopen(request)
                jobID = response.info()['Location'][len(uri)+1:]
                logging.debug('Submitted job: '+jobID)
                APPENGINE_JOBS.append({'jobID':jobID, 'logFile':run['logFile'], 'status':''})
            except urllib2.HTTPError as e:
                msg = e.read()
                try:
                    with open(run['logFile']+'.stdErr', 'w') as logFile:
                        logFile.write('NOT SUBMITTED\n{0}\n{1}\n{2}'.format(e.code, e.reason, msg))
                except:
                    pass
                logging.warn('Job could not be submitted. HTTP Error: {0}: {1}, Message: {2}, Sourcefile: {3}'.format(e.code, e.reason, msg, run['sourcefile']))
            except:
                sys.exit('Error while submitting jobs. {0}'.format(sys.exc_info()[0]))
            
            self.submittedJobs += 1

#             if self.submittedJobs == 10: break
        
        logging.debug('Submitting finished with %i submitted jobs.'%self.submittedJobs)
        self.done = True
                
class AppEnginePoller(threading.Thread):
    def __init__(self, benchmark):
        threading.Thread.__init__(self)
        self.benchmark = benchmark
        self.done = False
    
    def run(self):
        finishedJobs = 0
        nextJobIndex = 0
        while not self.done and not STOPPED_BY_INTERRUPT:
            self.done = (APPENGINE_SUBMITTER_THREAD.done and finishedJobs == len(APPENGINE_JOBS))

            if len(APPENGINE_JOBS) > nextJobIndex:
                job = APPENGINE_JOBS[nextJobIndex]
                jobID = job['jobID']
                if job['status'] in ['DONE', 'TIMEOUT', 'ERROR']:
                    nextJobIndex += 1
                else:
                    try:
                        logging.debug('Polling job: '+jobID)
                        uri = config.appengineURI+'/jobs/'+jobID
                        headers = {'Accept':'application/json'}
                        request = urllib2.Request(uri, headers=headers)
                        response = json.loads(urllib2.urlopen(request).read())
                        status = response['status']
                        if status in ['DONE', 'TIMEOUT', 'ERROR']:
                            if status == 'ERROR' and response['retries'] < APPENGINE_RETRIES:
                                logging.debug('Job {0} finished with status ERROR. It has retries left and therefore might still succeed eventually.')
                            else:
                                logging.debug('Job {0} finished. Status: {1}'.format(jobID,status))
                                job['status'] = status
                                self.saveResult(job, response)
                                finishedJobs += 1
                            nextJobIndex += 1
                        else:
                            if status == 'PENDING':
                                # TODO Detect stalled jobs.
                                # Sometimes jobs will never be started because of
                                # problems on GAE itself. 
                                logging.debug('Job %s is still pending.'%jobID)
                                nextJobIndex += 1
                            elif status == 'RUNNING':
                                logging.debug('Job %s is still running.'%jobID)
                            time.sleep(config.appenginePollInterval)
                            
                    except urllib2.HTTPError as e:
                        nextJobIndex += 1
#                         finishedJobs += 1
#                         job['status'] = 'ERROR'
#                         self.saveResult(job, None)
                        logging.warn('Server error while polling job {0}: {1} {2}. Polling will be retried in the next iteration.'.format(jobID,e.code,e.reason))
                    except:
                        sys.exit('Error while polling jobs. {0}'.format(sys.exc_info()[0]))
                
                # never go out of bounds    
                nextJobIndex = nextJobIndex % len(APPENGINE_JOBS)    
            else:
                time.sleep(config.appenginePollInterval)
    
    def saveResult(self, job, jsonObj):
        jobID = job['jobID']
        logFile = job['logFile']
        headers = {'Accept':'text/plain'}

        try:
            uri = config.appengineURI+'/jobs/'+jobID+'/files/' + APPENGINE_STATS_FILENAME
            request = urllib2.Request(uri, headers=headers)
            response = urllib2.urlopen(request).read()
            filewriter.writeFile(response, logFile)
        except:
            pass # no stats
        
        try:
            with open(logFile+'.stdOut', 'w') as stdOut:
                json.dump(jsonObj, stdOut)
        except:
            pass # no result

        # save errors
        status = job['status']
        if status in ['ERROR','TIMEOUT']:
            try:
                uri = config.appengineURI+'/jobs/'+jobID+'/files/' + APPENGINE_ERROR_FILENAME
                request = urllib2.Request(uri, headers=headers)
                response = urllib2.urlopen(request).read()
                filewriter.writeFile(response, logFile+'.stdErr')
            except:
                pass # no error file

        if config.appengineDeleteWhenDone:
            try:
                headers = {'Accept':'application/json'}
                uri = config.appengineURI+'/jobs/'+jobID
                request = urllib2.Request(uri, headers=headers)
                request.get_method = lambda: 'DELETE'
                urllib2.urlopen(request).read()
                logging.debug('Deleted job {0} from App Engine.'.format(jobID))
            except:
                logging.warn('The job {0} could not be deleted from App Engine.'
                             .format(jobID))

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
                    killScriptLocal()

            # get times after runSet
            wallTimeAfter = time.time()
            usedWallTime = wallTimeAfter - wallTimeBefore
            ruAfter = resource.getrusage(resource.RUSAGE_CHILDREN)
            usedCpuTime = (ruAfter.ru_utime + ruAfter.ru_stime) \
                        - (ruBefore.ru_utime + ruBefore.ru_stime)

            outputHandler.outputAfterRunSet(runSet, usedCpuTime, usedWallTime)

    outputHandler.outputAfterBenchmark(STOPPED_BY_INTERRUPT)

    if config.commit and not STOPPED_BY_INTERRUPT and runSetsExecuted > 0:
        Util.addFilesToGitRepository(OUTPUT_PATH, outputHandler.allCreatedFiles,
                                     config.commitMessage+'\n\n'+outputHandler.description)


def parseCloudResultFile(filePath):

    wallTime = None
    cpuTime = None
    memUsage = None
    returnValue = None

    with open(filePath, 'rt') as file:

        try:
            wallTime = float(file.readline().split(":")[-1])
        except ValueError:
            pass
        try:
            cpuTime = float(file.readline().split(":")[-1])
        except ValueError:
            pass
        try:
            memUsage = int(file.readline().split(":")[-1]);
        except ValueError:
            pass
        try:
            returnValue = int(file.readline().split(":")[-1])
        except ValueError:
            pass

    return (wallTime, cpuTime, memUsage, returnValue)


def parseAndSetCloudWorkerHostInformation(filePath, outputHandler):

    runToHostMap = {}
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

            # Parse second part of information about runs
            for line in file:
                line = line.strip()
                if not line:
                    continue # skip empty lines

                runInfo = line.split('\t')
                runToHostMap[runInfo[1].strip()] = runInfo[0].strip()
                # TODO one key + multiple values <==> one sourcefile + multiple configs

    except IOError:
        logging.warning("Host information file not found: " + filePath)
    return runToHostMap

def parseArgsForAppEngine(args, absWorkingDir):
    
    appengineArgs = {}
    options = {}
    
    if config.debug:
        logLevel = 'FINER'
    else:
        logLevel = 'INFO'
    options['log.level'] = logLevel
    
    for arg in args:
        if STOPPED_BY_INTERRUPT: break
        if '-noout' == arg:
            options['output.disable'] = True
        elif '-stats' == arg:
            options['statistics.export'] = True
        elif '-32' == arg:
            options['analysis.machineModel'] = 'Linux32'
        elif '-64' == arg:
            options['analysis.machineModel'] = 'Linux64'
        elif '-printUsedOptions' == arg:
            options['log.usedOptions.export'] = True
        elif '-nolog' == arg:
            options['log.level'] = 'OFF'
        elif '-config' == arg:
            conf = args[args.index('-config')+1]
            conf = conf if conf.endswith('.properties') else '{0}.properties'.format(conf)
            if os.path.isfile(os.path.join(absWorkingDir, 'config', conf)):
                appengineArgs['configuration'] = conf
            else:
                sys.exit('Given configuration file {0} is not a valid file.'.format(conf))  
        elif '-spec' == arg:
            spec = args[args.index('-spec')+1]
            spec = spec if spec.endswith('.spc') else '{0}.spc'.format(spec)
            if os.path.isfile(os.path.join(absWorkingDir, 'config/specification', spec)):
                appengineArgs['specification'] = spec
            else:
                sys.exit('Given specification file {0} is not a valid file.'.format(spec))
        elif arg.startswith('-'):
            if not 'configuration' in appengineArgs:
                argName = arg[1:]
                if os.path.isfile(os.path.join(absWorkingDir, 'config', argName + '.properties')):
                    appengineArgs['configuration'] = argName+'.properties'
    
    appengineArgs['options'] = options
    return appengineArgs

def toTabList(l):
    return "\t".join(map(str, l))


def commonBaseDir(l):
    # os.path.commonprefix returns the common prefix, not the common directory
    return os.path.dirname(os.path.commonprefix(l))


def getCloudInput(benchmark):

    (requirements, numberOfRuns, limitsAndNumRuns, runDefinitions, sourceFiles) = getBenchmarkDataForCloud(benchmark)
    (workingDir, toolpaths) = getToolDataForCloud(benchmark)
    
    # prepare cloud input, we make all paths absolute, TODO necessary?
    outputDir = benchmark.logFolder
    absOutputDir = os.path.abspath(outputDir)
    absWorkingDir = os.path.abspath(workingDir)
    absCloudRunExecutorDir = os.path.abspath(os.path.dirname(__file__))
    absToolpaths = list(map(os.path.abspath, toolpaths))
    absScriptsPath = os.path.abspath('scripts') # necessary files for non-CPAchecker-tools
    absSourceFiles = list(map(os.path.abspath, sourceFiles))
    absBaseDir = commonBaseDir(absSourceFiles + absToolpaths + [absScriptsPath] + [absCloudRunExecutorDir])

    if absBaseDir == "": sys.exit("No common base dir found.")

    numOfRunDefLinesAndPriorityStr = [numberOfRuns + 1] # add 1 for the headerline 
    if config.cloudPriority:
        numOfRunDefLinesAndPriorityStr.append(config.cloudPriority)

    # build the input for the cloud, 
    # see external vcloud/README.txt for details.
    cloudInput = [
                toTabList(absToolpaths + [absScriptsPath]),
                absCloudRunExecutorDir,
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
    return "\n".join(cloudInput)


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

            # we assume, that VCloud-client only splits its input at tabs,
            # so we can use all other chars for the info, that is needed to run the tool.
            # we build a string-representation of all this info (it's a map),
            # that can be parsed with python again in cloudRunexecutor.py (this is very easy with eval()) .
            argMap = {"args":run.getCmdline(), "env":env,
                      "debug":config.debug, "maxLogfileSize":config.maxLogfileSize}
            argString = repr(argMap)
            assert not "\t" in argString # cannot call toTabList(), if there is a tab

            logFile = os.path.relpath(run.logFile, benchmark.logFolder)
            runDefinitions.append(toTabList([argString, run.sourcefile, logFile]))
            sourceFiles.append(run.sourcefile)

    if not sourceFiles: sys.exit("Benchmark has nothing to run.")
        
    return (requirements, numberOfRuns, limitsAndNumRuns, runDefinitions, sourceFiles)


def handleCloudResults(benchmark, outputHandler):
    
    outputDir = benchmark.logFolder
    if not os.path.isdir(outputDir) or not os.listdir(outputDir):
        # outputDir does not exist or is empty
        logging.warning("Cloud produced no results. Output-directory is missing or empty: {0}".format(outputDir))

    # Write worker host informations in xml
    filePath = os.path.join(outputDir, "hostInformation.txt")
    runToHostMap = parseAndSetCloudWorkerHostInformation(filePath, outputHandler)

    # write results in runs and handle output after all runs are done
    executedAllRuns = True
    runsProducedErrorOutput = False
    for runSet in benchmark.runSets:
        if not runSet.shouldBeExecuted():
            outputHandler.outputForSkippingRunSet(runSet)
            continue

        outputHandler.outputBeforeRunSet(runSet)

        for run in runSet.runs:
            stdoutFile = run.logFile + ".stdOut"
            if not os.path.exists(stdoutFile):
                logging.warning("No results exist for file {0}.".format(run.sourcefile))
                executedAllRuns = False
                continue

            try:
                (run.wallTime, run.cpuTime, run.memUsage, returnValue) = parseCloudResultFile(stdoutFile)

                if run.sourcefile in runToHostMap:
                    run.host = runToHostMap[run.sourcefile]

                if returnValue is not None:
                    # Do not delete stdOut file if there was some problem
                    os.remove(stdoutFile)
                else:
                    executedAllRuns = False

            except EnvironmentError as e:
                logging.warning("Cannot extract measured values from output for file {0}: {1}".format(run.sourcefile, e))
                executedAllRuns = False
                continue

            if os.path.exists(run.logFile + ".stdError"):
                runsProducedErrorOutput = True

            outputHandler.outputBeforeRun(run)
            output = ''
            try:
                with open(run.logFile, 'rt') as outputFile:
                    # first 6 lines are for logging, rest is output of subprocess, see RunExecutor.py for details
                    output = '\n'.join(map(Util.decodeToString, outputFile.readlines()[6:]))
            except IOError as e:
                logging.warning("Cannot read log file: " + e.strerror)

            run.afterExecution(returnValue, output)
            outputHandler.outputAfterRun(run)

        outputHandler.outputAfterRunSet(runSet, None, None)

    outputHandler.outputAfterBenchmark(STOPPED_BY_INTERRUPT)

    if not executedAllRuns:
        logging.warning("Not all runs were executed in the cloud!")
    if runsProducedErrorOutput:
        logging.warning("Some runs produced unexpected warnings on stderr, please check the {0} files!"
                        .format(os.path.join(outputDir, '*.stdError')))
        
def parseAppEngineResult(run):
    withError = []
    withTimeout = []
    notSubmitted = []
    output = ''
    returnValue = 0
    hasTimedOut = False
    
    hasLogFile = (os.path.exists(run.logFile) and os.path.isfile(run.logFile))
    hasStdOutFile = (os.path.exists(run.logFile+'.stdOut') and os.path.isfile(run.logFile+'.stdOut'))
    hasErrOutFile = (os.path.exists(run.logFile+'.stdErr') and os.path.isfile(run.logFile+'.stdErr'))
    
    if hasLogFile:
        try:
            with open(run.logFile, 'rt') as outputFile:
                # first 6 lines are for logging, rest is output of subprocess, see RunExecutor.py for details
                output = '\n'.join(map(Util.decodeToString, outputFile.readlines()[6:]))

            os.remove(run.logFile)
        except IOError as e:
            logging.warning("Cannot read log file: " + e.strerror)
            
    if hasStdOutFile:
        try:
            with open(run.logFile+'.stdOut', 'rt') as file:
                result = json.loads(file.read())
                if result['status'] == 'ERROR':
                    returnValue = 18 # error
                    withError.append({'run':run, 'message':result['statusMessage']})
                if result['status'] == 'TIMEOUT':
                    returnValue = 9 # timeout
                    hasTimedOut = True
                    withTimeout.append(run)

                run.wallTime = timedelta(microseconds=result['statistic']['latency']).total_seconds()
                run.cpuTime = result['statistic']['CPUTime']
                run.host = result['statistic']['host']

            os.remove(run.logFile+'.stdOut') 
        except:
            pass # can't read std out file
            
    if hasErrOutFile:
        try:
            with open(run.logFile+'.stdErr', 'rt') as errFile:
                lines = errFile.readlines()
                if lines[0].strip() == 'NOT SUBMITTED':
                    # TODO really aborted? semantic!
                    returnValue = 6 # aborted
                    notSubmitted.append({'run':run, 'messages':lines[-1]})
                else:
                    result = json.loads(''.join(lines))
                    if result['status'] == 'ERROR':
                        returnValue = 18 # error
                        withError.append({'run':run, 'message':result['statusMessage']})
                    elif result['status'] == 'TIMEOUT':
                        returnValue = 9 # timeout
                        hasTimedOut = True
                        withTimeout.append(run)
        except:
            pass # can't read err file
    
    return (returnValue, output, hasTimedOut, withError, withTimeout, notSubmitted)
        
def handleAppEngineResults(benchmark, outputHandler):
    withError = []
    withTimeout = []
    notSubmitted = []
    
    for runSet in benchmark.runSets:
        if not runSet.shouldBeExecuted():
            outputHandler.outputForSkippingRunSet(runSet)
            continue

        outputHandler.outputBeforeRunSet(runSet)
        
        totalWallTime = 0
        for run in runSet.runs:
            outputHandler.outputBeforeRun(run)
            
            (returnValue, output, hasTimedOut, withErr, withTO, notSubmt) = \
                parseAppEngineResult(run)
                
            totalWallTime += run.wallTime

            withError = withError + withErr
            withTimeout = withTimeout + withTO
            notSubmitted = notSubmitted + notSubmt
            
            run.afterExecution(returnValue, output, hasTimedOut)
            outputHandler.outputAfterRun(run)

        outputHandler.outputAfterRunSet(runSet, None, totalWallTime)

    outputHandler.outputAfterBenchmark(STOPPED_BY_INTERRUPT)
    
    if len(notSubmitted) > 0:
        logging.warning("{0} runs were not submitted to App Engine!".format(len(notSubmitted)))
    if len(withError) > 0:
        logging.warning("{0} runs produced unexpected errors, please check the {1} files!"
                        .format(len(withError), os.path.join(benchmark.logFolder, '*.stdErr')))
    if len(withTimeout) > 0:
        logging.warning("{0} runs timed out!".format(len(withTimeout)))
    
def getBenchmarkDataForAppEngine(benchmark):
    # TODO default CPU model??
    cpuModel = benchmark.requirements.cpuModel
    
    timeLimit = benchmark.rlimits.get(TIMELIMIT, DEFAULT_APPENGINE_TIMELIMIT)
    if timeLimit > DEFAULT_APPENGINE_TIMELIMIT:
        logging.warn('Given timelimit is too large for App Engine. Using %i seconds instead.'%DEFAULT_APPENGINE_TIMELIMIT)
        timeLimit = DEFAULT_APPENGINE_TIMELIMIT

    numberOfRuns = sum(len(runSet.runs) for runSet in benchmark.runSets if runSet.shouldBeExecuted())
    
    workingDir = benchmark.workingDirectory()
    if not os.path.isdir(workingDir):
        sys.exit("Missing working directory {0}, cannot run tool.".format(workingDir))
    absWorkingDir = os.path.abspath(workingDir)
    
    sourceFiles = []
    runDefinitions = []
    for runSet in benchmark.runSets:
        if not runSet.shouldBeExecuted(): continue
        if STOPPED_BY_INTERRUPT: break

        for run in runSet.runs:
            if STOPPED_BY_INTERRUPT: break
            logFile = os.path.relpath(run.logFile, benchmark.logFolder)
            args = parseArgsForAppEngine(run.getCmdline()[1:-1], absWorkingDir)
            args['options']['limits.time.wall'] = str(timeLimit)+'s'
            args['sourceFileName'] = run.sourcefile
            runDefinitions.append({'args':args,
                                   'debug':config.debug,
                                   'maxLogfileSize':config.maxLogfileSize,
                                   'sourcefile':os.path.join(absWorkingDir, run.sourcefile),
                                   'logFile':os.path.join(benchmark.logFolder, logFile)})
            sourceFiles.append(run.sourcefile)

    if not sourceFiles: sys.exit("Benchmark has nothing to run.")
    
    return (cpuModel, timeLimit, numberOfRuns, runDefinitions, sourceFiles, absWorkingDir)

def executeBenchmarkInCloud(benchmark, outputHandler):

    # build input for cloud
    cloudInput = getCloudInput(benchmark)
    cloudInputFile = os.path.join(benchmark.logFolder, 'cloudInput.txt')
    filewriter.writeFile(cloudInput, cloudInputFile)
    outputHandler.allCreatedFiles.append(cloudInputFile)

    # install cloud and dependencies
    ant = subprocess.Popen(["ant", "resolve-benchmark-dependencies"])
    ant.communicate()
    ant.wait()

    # start cloud and wait for exit
    logging.debug("Starting cloud.")
    if config.debug:
        logLevel =  "FINER"
    else:
        logLevel = "INFO"
    libDir = os.path.abspath("./lib/java-benchmark")
    cmdLine = ["java", "-jar", libDir + "/vcloud.jar", "benchmark", "--loglevel", logLevel]
    if config.cloudMaster:
        cmdLine.extend(["--master", config.cloudMaster])
    if config.debug:
        cmdLine.extend(["--print-new-files", "true"])
    cloud = subprocess.Popen(cmdLine, stdin=subprocess.PIPE)
    try:
        (out, err) = cloud.communicate(cloudInput.encode('utf-8'))
    except KeyboardInterrupt:
        killScriptCloud()
    returnCode = cloud.wait()

    if returnCode and not STOPPED_BY_INTERRUPT:
        logging.warn("Cloud return code: {0}".format(returnCode))

    handleCloudResults(benchmark, outputHandler)

    if config.commit and not STOPPED_BY_INTERRUPT:
        Util.addFilesToGitRepository(OUTPUT_PATH, outputHandler.allCreatedFiles,
                                     config.commitMessage+'\n\n'+outputHandler.description)

    return returnCode

def executeBenchmarkInAppengine(benchmark, outputHandler):
    # Get settings from server. Also warms-up the server.
    uri = config.appengineURI + '/settings'
    logging.debug('Pulling settings from {0}.'.format(uri))
    try:
        headers = {'Accept':'application/json'}
        request = urllib2.Request(uri, headers=headers)
        response = json.loads(urllib2.urlopen(request).read())
        global DEFAULT_APPENGINE_TIMELIMIT, APPENGINE_STATS_FILENAME, APPENGINE_ERROR_FILENAME, APPENGINE_RETRIES
        DEFAULT_APPENGINE_TIMELIMIT = response['timeLimit']
        APPENGINE_STATS_FILENAME = response['statisticsFileName']
        APPENGINE_ERROR_FILENAME = response['errorFileName']
        APPENGINE_RETRIES = int(response['retries'])
        
        logging.debug('Settings were successfully retrieved.')
    except urllib2.URLError as e:
        sys.exit('The settings could not be pulled. {0} is not available. Error: {1}'.format(uri, e.reason))
    
    (cpuModel, timeLimit, numberOfRuns, runDefinitions, sourceFiles, absWorkingDir) = getBenchmarkDataForAppEngine(benchmark)
    
    # submit jobs
    global APPENGINE_SUBMITTER_THREAD
    APPENGINE_SUBMITTER_THREAD = AppEngineSubmitter(runDefinitions, benchmark)
    APPENGINE_SUBMITTER_THREAD.start()
    
    # poll jobs
    global APPENGINE_POLLER_THREAD
    APPENGINE_POLLER_THREAD = AppEnginePoller(benchmark)
    APPENGINE_POLLER_THREAD.start()
    try:
        while not APPENGINE_POLLER_THREAD.done:
            time.sleep(0.1)
    except KeyboardInterrupt:
        killScriptAppEngine()
    APPENGINE_POLLER_THREAD.join()
    
    handleAppEngineResults(benchmark, outputHandler)


def executeBenchmark(benchmarkFile):
    benchmark = Benchmark(benchmarkFile, config, OUTPUT_PATH)
    outputHandler = OutputHandler(benchmark)
    
    logging.debug("I'm benchmarking {0} consisting of {1} run sets.".format(
            repr(benchmarkFile), len(benchmark.runSets)))

    if config.cloud:
        return executeBenchmarkInCloud(benchmark, outputHandler)
    if config.appengine:
        return executeBenchmarkInAppengine(benchmark, outputHandler)
    else:
        return executeBenchmarkLocaly(benchmark, outputHandler)

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
                      help="Sets the URI to use when submitting jobs to App Engine.")
    
    parser.add_argument("--appenginePollInterval",
                      dest="appenginePollInterval",
                      metavar="INTERVAL",
                      default=DEFAULT_APPENGINE_POLLINTERVAL,
                      type=int,
                      help="Sets the interval in seconds after which App Engine is polled for results.")
    
    parser.add_argument("--appengineKeep",
                        dest="appengineDeleteWhenDone",
                        action="store_false",
                        help="If set a job will NOT be deleted from App Engine after it has successfully been executed.")

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

    returnCode = 0
    for arg in config.files:
        if STOPPED_BY_INTERRUPT: break
        logging.debug("Benchmark {0} is started.".format(repr(arg)))
        rc = executeBenchmark(arg)
        returnCode = returnCode or rc
        logging.debug("Benchmark {0} is done.".format(repr(arg)))

    logging.debug("I think my job is done. Have a nice day!")
    return returnCode


def killScriptLocal():
        # set global flag
        global STOPPED_BY_INTERRUPT
        STOPPED_BY_INTERRUPT = True

        # kill running jobs
        Util.printOut("killing subprocesses...")
        for worker in WORKER_THREADS:
            worker.stop()

        # wait until all threads are stopped
        for worker in WORKER_THREADS:
            worker.join()


def killScriptCloud():
        # set global flag
        global STOPPED_BY_INTERRUPT
        STOPPED_BY_INTERRUPT = True

        # kill cloud-client, should be done automatically, when the subprocess is aborted

def killScriptAppEngine():
    global STOPPED_BY_INTERRUPT
    STOPPED_BY_INTERRUPT = True
    
    Util.printOut("Killing subprocesses. May take up to %s seconds..."%config.appenginePollInterval)
    if not APPENGINE_POLLER_THREAD == None:
        APPENGINE_POLLER_THREAD.join()
    if not APPENGINE_SUBMITTER_THREAD == None:
        APPENGINE_SUBMITTER_THREAD.join()

def signal_handler_ignore(signum, frame):
    logging.warn('Received signal %d, ignoring it' % signum)

if __name__ == "__main__":
    # ignore SIGTERM
    signal.signal(signal.SIGTERM, signal_handler_ignore)
    try:
        sys.exit(main())
    except KeyboardInterrupt: # this block is reached, when interrupt is thrown before or after a run set execution
        if config.cloud:
            killScriptCloud()
        elif config.appengine:
            killScriptAppEngine()
        else:
            killScriptLocal()
        Util.printOut("\n\nScript was interrupted by user, some runs may not be done.")
