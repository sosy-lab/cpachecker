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

from datetime import datetime, timedelta
import json
import logging
import os
import threading
import time
import urllib2

from . import util as Util
from . import filewriter as filewriter


APPENGINE_SUBMITTER_THREAD = None
APPENGINE_POLLER_THREAD = None
APPENGINE_TASKS = {}
APPENGINE_SETTINGS = {} # these will be fetched from the server

STOPPED_BY_INTERRUPT = False

def setupBenchmarkForAppengine(benchmark):
    uri = benchmark.config.appengineURI + '/settings'
    logging.debug('Setting up benchmark for App Engine...')
    logging.debug('Pulling settings from {0}.'.format(uri))
    try:
        headers = {'Accept':'application/json'}
        request = urllib2.Request(uri, headers=headers)
        response = json.loads(urllib2.urlopen(request).read())
        global APPENGINE_SETTINGS
        APPENGINE_SETTINGS = response
        benchmark.toolVersion = response['cpacheckerVersion']

        logging.debug('Settings were successfully retrieved.')
    except urllib2.URLError as e:
        sys.exit('The settings could not be retrieved. {} is not available. Error: {}'.format(uri, e.reason))


def executeBenchmarkInAppengine(benchmark, outputHandler):
    formatString = '%m-%d-%YT%H:%M:%S.%f'
    timestampsFileName = benchmark.outputBase+'.Timestamps_'+datetime.strftime(datetime.now(), formatString)+'.txt'
    with open(timestampsFileName, 'a') as f:
        f.write('Start: '+datetime.strftime(datetime.now(), formatString)+'\n')

    tasksetKey = _getTasksetKeyForAppEngine(benchmark)
    logging.debug('Using taskset with key: '+tasksetKey)

    outputHandler.storeSystemInfo('unknown', 'unknown', 'unknown', APPENGINE_SETTINGS['CPUSpeed'], APPENGINE_SETTINGS['RAM'], 'Google App Engine')
    (cpuModel, numberOfRuns, runQueue, sourceFiles, absWorkingDir) = _getBenchmarkDataForAppEngine(benchmark)

    logging.debug('Will execute {} runs.'.format(str(numberOfRuns)))

    global APPENGINE_SUBMITTER_THREAD
    APPENGINE_SUBMITTER_THREAD = _AppEngineSubmitter(runQueue, tasksetKey, numberOfRuns, benchmark)
    APPENGINE_SUBMITTER_THREAD.start()

    global APPENGINE_POLLER_THREAD
    APPENGINE_POLLER_THREAD = _AppEnginePoller(benchmark, tasksetKey)
    APPENGINE_POLLER_THREAD.start()
    try:
        while not APPENGINE_POLLER_THREAD.done:
            time.sleep(0.1)
    except KeyboardInterrupt:
        with open(timestampsFileName, 'a') as f:
            f.write('Interrupt: '+datetime.strftime(datetime.now(), formatString)+'\n')
        killScriptAppEngine(benchmark.config)
    APPENGINE_POLLER_THREAD.join()

    _handleAppEngineResults(benchmark, outputHandler)

    with open(timestampsFileName, 'a') as f:
        f.write('Finish: '+datetime.strftime(datetime.now(), formatString)+'\n')


def killScriptAppEngine(config):
    global STOPPED_BY_INTERRUPT
    STOPPED_BY_INTERRUPT = True

    Util.printOut("Killing subprocesses. May take up to %s seconds..."%config.appenginePollInterval)
    if not APPENGINE_POLLER_THREAD == None:
        APPENGINE_POLLER_THREAD.join()
    if not APPENGINE_SUBMITTER_THREAD == None:
        APPENGINE_SUBMITTER_THREAD.join()


def _getBenchmarkDataForAppEngine(benchmark):
    # TODO default CPU model??
    cpuModel = benchmark.requirements.cpuModel

    numberOfRuns = sum(len(runSet.runs) for runSet in benchmark.runSets if runSet.shouldBeExecuted())

    workingDir = benchmark.workingDirectory()
    if not os.path.isdir(workingDir):
        sys.exit("Missing working directory {}, cannot run tool.".format(workingDir))
    absWorkingDir = os.path.abspath(workingDir)

    sourceFiles = []
    runQueue = Queue.Queue(maxsize=0)
    for runSet in benchmark.runSets:
        if not runSet.shouldBeExecuted(): continue
        if STOPPED_BY_INTERRUPT: break

        for run in runSet.runs:
            if STOPPED_BY_INTERRUPT: break
            args = {'commandline':' '.join(run.getCmdline()), 'programName':run.sourcefile}
            try:
                with open(run.propertyfile, 'r') as f:
                    args['properties'] = f.read()
            except:
                sys.exit("Cannot read properties file {}.".format(run.propertyfile))
            try:
                with open(run.sourcefile, 'r') as f:
                    args['programText'] = f.read()
            except:
                sys.exit("Cannot read program file {}.".format(run.sourcefile))
            runQueue.put({'payload':args,
                          'logFile':os.path.abspath(run.logFile),
                          'debug':benchmark.config.debug,
                          'maxLogfileSize':benchmark.config.maxLogfileSize})
            sourceFiles.append(run.sourcefile)

    if not sourceFiles: sys.exit("Benchmark has nothing to run.")

    return (cpuModel, numberOfRuns, runQueue, sourceFiles, absWorkingDir)


def _getTasksetKeyForAppEngine(benchmark):
        uri = benchmark.config.appengineURI+'/tasksets'
        headers = {'Content-type':'application/json', 'Accept':'application/json'}
        try:
            request = urllib2.Request(uri, '', headers)
            return urllib2.urlopen(request).read()
        except urllib2.HTTPError as e:
            sys.exit('Taskset could not be created. HTTP Error: {}: {}, Message: {}'.format(e.code, e.reason, e.msg))
        except:
            sys.exit('Error while submitting tasks. {}'.format(sys.exc_info()[0]))


def _handleAppEngineResults(benchmark, outputHandler):
    withError = 0
    withTimeout = 0
    notSubmitted = 0
    isOverQuota = False

    for runSet in benchmark.runSets:
        if not runSet.shouldBeExecuted():
            outputHandler.outputForSkippingRunSet(runSet)
            continue

        outputHandler.outputBeforeRunSet(runSet)

        totalWallTime = 0
        for run in runSet.runs:
            outputHandler.outputBeforeRun(run)

            (returnValue, output, hasErr, hasTO, isNotSubmt, overQuota) = \
                _parseAppEngineResult(run)

            totalWallTime += run.wallTime

            if hasErr: withError += 1
            if hasTO: withTimeout += 1
            if isNotSubmt: notSubmitted += 1
            isOverQuota = True if overQuota or isOverQuota else False

            run.afterExecution(returnValue, output, hasTO)
            outputHandler.outputAfterRun(run)

        outputHandler.outputAfterRunSet(runSet, wallTime=totalWallTime)

    outputHandler.outputAfterBenchmark(STOPPED_BY_INTERRUPT)

    if notSubmitted > 0:
        logging.warning("{} runs were not submitted to App Engine!".format(notSubmitted))
    if withError > 0:
        logging.warning("{} runs produced unexpected errors, please check the {} files!"
                        .format(withError, os.path.join(benchmark.logFolder, '*.stdErr')))
    if withTimeout > 0:
        logging.warning("{} runs timed out!".format(withTimeout))
    if isOverQuota:
        logging.warning("Quota exceeded! Not all runs could be processed!")


def _parseAppEngineResult(run):
    error = False
    timeout = False
    notSubmitted = False
    output = ''
    returnValue = 0
    overQuota = False

    hasLogFile = (os.path.exists(run.logFile) and os.path.isfile(run.logFile))
    hasStdOutFile = (os.path.exists(run.logFile+'.stdOut') and os.path.isfile(run.logFile+'.stdOut'))
    hasErrOutFile = (os.path.exists(run.logFile+'.stdErr') and os.path.isfile(run.logFile+'.stdErr'))

    if hasLogFile:
        try:
            with open(run.logFile, 'rt') as outputFile:
                output = '\n'.join(map(Util.decodeToString, outputFile.readlines()))
        except IOError as e:
            logging.warning("Cannot read log file: " + e.strerror)

    if hasStdOutFile:
        try:
            with open(run.logFile+'.stdOut', 'rt') as file:
                lines = file.read()
                output += '\n'+lines
                result = json.loads(lines)
                if result['status'] == 'ERROR':
                    returnValue = 256 # error; returncode != 0
                    error = True
                if result['status'] == 'TIMEOUT':
                    returnValue = 9 # timeout
                    timeout = True
                if result['status'] == 'OVER_QUOTA':
                    returnValue = 6 # aborted
                    overQuota = True

                run.wallTime = timedelta(microseconds=result['statistic']['latency']).total_seconds()
                run.cpuTime = result['statistic']['CPUTime']
                run.host = result['statistic']['host']
        except:
            pass # can't read std out file

    if hasErrOutFile:
        try:
            with open(run.logFile+'.stdErr', 'rt') as errFile:
                lines = errFile.read()
                output += '\n'+lines
                if 'NOT SUBMITTED' in lines:
                    returnValue = 6 # aborted
                    notSubmitted = True
                else:
                    result = json.loads(lines)
                    if result['status'] == 'ERROR':
                        returnValue = 256 # error; returncode != 0
                        error = True
                    elif result['status'] == 'TIMEOUT':
                        returnValue = 9 # timeout
                        timeout = True
        except:
            pass # can't read err file

    return (returnValue, output, error, timeout, notSubmitted, overQuota)


class _AppEngineSubmitter(threading.Thread):
    def __init__(self, runQueue, tasksetKey, numberOfRuns, benchmark):
        threading.Thread.__init__(self)
        self.queue = runQueue
        self.tasksetKey = tasksetKey
        self.numberOfRuns = numberOfRuns
        self.benchmark = benchmark
        self.done = False
        self.submittedTasks = 0

    def run(self):
        while not self.queue.empty():
            if STOPPED_BY_INTERRUPT: break

            runs = []
            combinedFileSize = 0
            payloadComplete = False
            while not payloadComplete:
                if STOPPED_BY_INTERRUPT: break
                run = self.queue.get()
                combinedFileSize += len(run['payload']['programText'])
                if combinedFileSize < 3145728 and len(runs) < 50: # 3MB
                    runs.append(run)
                    self.queue.task_done()
                else:
                    self.queue.task_done()
                    self.queue.put(run)
                    payloadComplete = True

                if self.queue.empty():
                    payloadComplete = True

            logging.info('Submitting {} of {} tasks.'.format(str(len(runs)), str(self.numberOfRuns-self.submittedTasks)))

            payload = []
            for index, run in enumerate(runs):
                args = run['payload']
                args['taskset'] = self.tasksetKey
                args['identifier'] = index
                payload.append(args)

            uri = self.benchmark.config.appengineURI+'/tasksets/'+self.tasksetKey+'/tasks'
            headers = {'Content-type':'application/json', 'Accept':'application/json'}
            try:
                data = json.dumps(payload)
                request = urllib2.Request(uri, data, headers)
                response = urllib2.urlopen(request).read()
                taskIDs = json.loads(response)
                for key in taskIDs:
                    logging.debug('Task created: '+key)
                    APPENGINE_TASKS[key] = runs[int(taskIDs[key])]
                    self.submittedTasks += 1
                    try:
                        with open(self.benchmark.outputBase+'.Submitted_Tasks.txt', 'a') as f:
                            f.write(key+'\n')
                    except:
                        pass
            except urllib2.HTTPError as e:
                msg = e.read()
                logging.warn('Tasks could not be submitted. HTTP Error: {}: {}, Message: {}'.format(e.code, e.reason, msg))
                logging.debug('Submitting will be retried later.')
                for run in runs:
                    self.queue.put(run)
            except:
                sys.exit('Error while submitting tasks. {}'.format(sys.exc_info()[0]))

        logging.debug('Submitting finished with %i submitted tasks.'%self.submittedTasks)
        self.done = True


class _AppEnginePoller(threading.Thread):
    def __init__(self, benchmark, tasksetKey):
        threading.Thread.__init__(self)
        self.benchmark = benchmark
        self.tasksetKey = tasksetKey
        self.done = False
        self.finishedTasks = 0

    def run(self):
        time.sleep(self.benchmark.config.appenginePollInterval)
        while not self.done and not STOPPED_BY_INTERRUPT:
            try:
                logging.info('Polling tasks')
                uri = self.benchmark.config.appengineURI+'/tasksets/'+self.tasksetKey+'/tasks?finished=true&limit=50'
                headers = {'Accept':'application/json'}
                request = urllib2.Request(uri, headers=headers)
                tasks = json.loads(urllib2.urlopen(request).read())
                for task in tasks:
                    if not task['key'] in APPENGINE_TASKS:
                        try:
                            uri = self.benchmark.config.appengineURI+'/tasksets/'+self.tasksetKey+'/tasks'
                            request = urllib2.Request(uri, json.dumps(task['key']), headers=headers)
                            request.get_method = lambda: 'PUT'
                            urllib2.urlopen(request)
                        except: pass
                        continue
                    self.saveResult(APPENGINE_TASKS[task['key']], task)
                logging.debug('Received results are fully processed.')

            except urllib2.HTTPError as e:
                if 'OVER_QUOTA' in e.read():
                    logging.warn('A resource on App Engine has been depleted and no more requests can be processed!')
                    self.done = True
                else:
                    logging.warn('Server error while polling tasks: {} {}. Polling will be retried later.'.format(e.code, e.reason))
            except:
                logging.warn('Error while polling tasks. {}'.format(sys.exc_info()[0]))

            self.done = (APPENGINE_SUBMITTER_THREAD.done and self.finishedTasks >= len(APPENGINE_TASKS))
            if not self.done: time.sleep(self.benchmark.config.appenginePollInterval)

    def saveResult(self, run, task):
        taskKey = task['key']
        logFile = run['logFile']
        headers = {'Accept':'text/plain'}

        fileNames = []
        for file in task['files']:
            fileNames.append(file['name'])

        try:
            filewriter.writeFile(json.dumps(task), logFile+'.stdOut')
        except:
            logging.debug('Could not save task '+taskKey)

        statisticsProcessed = False
        if APPENGINE_SETTINGS['statisticsFileName'] in fileNames:
            try:
                uri = self.benchmark.config.appengineURI+'/tasks/'+taskKey+'/files/' + APPENGINE_SETTINGS['statisticsFileName']
                request = urllib2.Request(uri, headers=headers)
                response = urllib2.urlopen(request).read()
                filewriter.writeFile(response, logFile)
                statisticsProcessed = True
            except:
                statisticsProcessed = False
                logging.debug('Could not save statistics '+taskKey)
        else:
            statisticsProcessed = True


        if APPENGINE_SETTINGS['errorFileName'] in fileNames:
            try:
                uri = self.benchmark.config.appengineURI+'/tasks/'+taskKey+'/files/' + APPENGINE_SETTINGS['errorFileName']
                request = urllib2.Request(uri, headers=headers)
                response = urllib2.urlopen(request).read()
                response = 'Task Key: {}\n{}'.format(task['key'], response)
                filewriter.writeFile(response, logFile+'.stdErr')
            except: pass

        headers = {'Content-type':'application/json', 'Accept':'application/json'}
        markedAsProcessed = False
        if statisticsProcessed:
            try:
                uri = self.benchmark.config.appengineURI+'/tasksets/'+self.tasksetKey+'/tasks'
                request = urllib2.Request(uri, json.dumps([taskKey]), headers=headers)
                request.get_method = lambda: 'PUT'
                urllib2.urlopen(request)
                self.finishedTasks += 1
                markedAsProcessed = True
                logging.info('Stored result of task {0} in file {1}'.format(taskKey, logFile))
                try:
                    with open(self.benchmark.outputBase+'.Processed_Tasks.txt', 'a') as f:
                        f.write(taskKey+'\n')
                except: pass
                logging.debug('Task {} finished. Status: {}'.format(taskKey, task['status']))
            except:
                logging.debug('The task {} could not be marked as processed.'.format(taskKey))

        if self.benchmark.config.appengineDeleteWhenDone and markedAsProcessed:
            try:
                uri = self.benchmark.config.appengineURI+'/tasks/'+taskKey
                request = urllib2.Request(uri, headers=headers)
                request.get_method = lambda: 'DELETE'
                urllib2.urlopen(request).read()
            except:
                logging.warn('The task {} could not be deleted.'.format(taskKey))
