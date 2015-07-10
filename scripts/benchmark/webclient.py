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

import base64
import fnmatch
import io
import logging
import os
import random
import shutil
import threading
import zlib
import zipfile

from time import sleep
from time import time

import urllib.parse as urllib
import urllib.request as urllib2
from  http.client import HTTPConnection
from  http.client import HTTPSConnection
from concurrent.futures import ThreadPoolExecutor
from concurrent.futures import as_completed

from benchexec.model import MEMLIMIT, TIMELIMIT, CORELIMIT

RESULT_KEYS = ["cputime", "walltime"]

MAX_SUBMISSION_THREADS = 5

RESULT_FILE_LOG = 'output.log'
RESULT_FILE_STDERR = 'stderr'
RESULT_FILE_RUN_INFO = 'runInformation.txt'
RESULT_FILE_HOST_INFO = 'hostInformation.txt'
SPECIAL_RESULT_FILES = {RESULT_FILE_LOG, RESULT_FILE_STDERR, RESULT_FILE_RUN_INFO,
                        RESULT_FILE_HOST_INFO, 'runDescription.txt'}

class WebClientError(Exception):
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return repr(self.value)

_connectionTimeout = 600 #seconds
_thread_local = threading.local()
_print_lock = threading.Lock()
_unfinished_run_ids = set()
_groupId = None
_webclient = None
_base64_user_pwd = None
_svn_branch = None
_svn_revision = None

def resolveToolVersion(config, benchmark, webclient):
    global _svn_branch, _svn_revision

    if config.revision:
        tokens = config.revision.split(':')
        _svn_branch = tokens[0]
        if len(tokens) > 1:
            revision = config.revision.split(':')[1]
        else:
            revision = 'HEAD'
    else:
        _svn_branch = 'trunk'
        revision = 'HEAD'

    url = webclient + "tool/version?svnBranch=" + _svn_branch \
                         + "&revision=" + revision

    request = urllib2.Request(url)
    try:
        response = urllib2.urlopen(request)
        if (response.getcode() == 200):
            _svn_revision = response.read().decode("utf-8")
        else:
            logging.warning("Could not resolve {0}:{1}: {2}".format(config.revision.branch, revision, response.read()))
            sys.exit(1)
    except urllib2.HTTPError as e:
        try:
            if e.code == 404:
                message = 'Please check the URL given to --cloudMaster.'
            else:
                message = e.read() #not all HTTPErrors have a read() method
        except AttributeError:
            message = ""
        logging.warning("Could not resolve {0}:{1}: {2}".format(_svn_branch, revision, message))
        sys.exit(1)

    benchmark.tool_version = _svn_branch + ":" + _svn_revision
    logging.info('Using tool version {0}:{1}'.format(_svn_branch, _svn_revision))

def init(config, benchmark):
    global _webclient, _base64_user_pwd, _groupId

    if config.cloudMaster:
        if not benchmark.config.cloudMaster[-1] == '/':
            benchmark.config.cloudMaster += '/'

        webclient = benchmark.config.cloudMaster
        _webclient = urllib.urlparse(webclient)
        logging.info('Using webclient at {0}'.format(webclient))

        resolveToolVersion(config, benchmark, webclient)

    if config.cloudUser:
        _base64_user_pwd = base64.b64encode(config.cloudUser.encode("utf-8")).decode("utf-8")

    _groupId = str(random.randint(0, 1000000))

    benchmark.executable = 'scripts/cpa.sh'

def get_system_info():
    return None

def execute_benchmark(benchmark, output_handler):

    if (benchmark.tool_name != 'CPAchecker'):
        logging.warning("The web client does only support the CPAchecker.")
        return

    STOPPED_BY_INTERRUPT = False
    try:
        for runSet in benchmark.run_sets:
            if not runSet.should_be_executed():
                output_handler.output_for_skipping_run_set(runSet)
                continue

            output_handler.output_before_run_set(runSet)
            runIDs = _submitRunsParallel(runSet, benchmark)

            _getResults(runIDs, output_handler, benchmark)
            output_handler.output_after_run_set(runSet)

    except KeyboardInterrupt as e:
        STOPPED_BY_INTERRUPT = True
        raise e
    except:
        stop()
        raise
    finally:
        output_handler.output_after_benchmark(STOPPED_BY_INTERRUPT)

def stop():
    logging.debug("Stopping tasks...")
    executor = ThreadPoolExecutor(MAX_SUBMISSION_THREADS)
    global _unfinished_run_ids
    stopTasks = set()
    for runId in _unfinished_run_ids:
        stopTasks.add(executor.submit(_stop_run, runId))
    for task in stopTasks:
        task.result()
    executor.shutdown(wait=True)
    logging.debug("Stopped all tasks.")

def _stop_run(runId):
    path = _webclient.path + "runs/" + runId
    try:
        _request("DELETE", path, "", {}, 204)
    except urllib2.HTTPError as e:
        logging.warn("Stopping of run {0} failed: {1}".format(runId, e.reason))

def _submitRunsParallel(runSet, benchmark):
    logging.info('Submitting runs...')

    runIDs = {}
    submissonCounter = 1
    #submit to executor
    executor = ThreadPoolExecutor(MAX_SUBMISSION_THREADS)
    runIDsFutures = {executor.submit(_submitRun, run, benchmark): run for run in runSet.runs}
    executor.shutdown(wait=False)

    #collect results to executor
    try:
        for future in as_completed(runIDsFutures.keys()):
            try:
                run = runIDsFutures[future]
                runID = future.result().decode("utf-8")
                runIDs[runID] = run
                _unfinished_run_ids.add(runID)
                logging.debug('Submitted run {0}/{1} with id {2}'.\
                        format(submissonCounter, len(runSet.runs), runID))
                if submissonCounter % 50 == 0:
                    logging.info('Submitted run {0}/{1}'.\
                                format(submissonCounter, len(runSet.runs)))


            except (urllib2.HTTPError, WebClientError) as e:
                try:
                    message = e.read() #not all HTTPErrors have a read() method
                except AttributeError:
                    message = ""
                logging.warning('Could not submit run {0}: {1}. {2}'.\
                    format(run.identifier, e, message))
            finally:
                submissonCounter += 1
    finally:
        for future in runIDsFutures.keys():
            future.cancel() # for example in case of interrupt

    _flushRuns()
    return runIDs

def _submitRun(run, benchmark, counter = 0):

    programTexts = []
    for programPath in run.sourcefiles:
        with open(programPath, 'r') as programFile:
            programText = programFile.read()
            programTexts.append(programText)
    params = {'programText': programTexts}

    params['svnBranch'] = _svn_branch
    params['revision'] = _svn_revision

    if run.propertyfile:
        with open(run.propertyfile, 'r') as propertyFile:
            propertyText = propertyFile.read()
            params['propertyText'] = propertyText

    limits = benchmark.rlimits
    if MEMLIMIT in limits:
        params['memoryLimitation'] = str(limits[MEMLIMIT]) + "MB"
    if TIMELIMIT in limits:
        params['timeLimitation'] = limits[TIMELIMIT]
    if CORELIMIT in limits:
        params['coreLimitation'] = limits[CORELIMIT]
    if benchmark.config.cpu_model:
        params['cpuModel'] = benchmark.config.cpu_model

    invalidOption = _handleOptions(run, params, limits)
    if invalidOption:
        raise WebClientError('Command {0} of run {1}  contains option that is not usable with the webclient. '\
            .format(run.options, run.identifier))

    params['groupId'] = _groupId;

    # prepare request
    headers = {"Content-Type": "application/x-www-form-urlencoded",
               "Content-Encoding": "deflate",
               "Accept": "text/plain"}

    paramsCompressed = zlib.compress(urllib.urlencode(params, doseq=True).encode('utf-8'))
    path = _webclient.path + "runs/"

    return _request("POST", path, paramsCompressed, headers)


def _flushRuns():
    headers = {"Content-Type": "application/x-www-form-urlencoded",
               "Content-Encoding": "deflate",
               "Connection": "Keep-Alive"}

    params = {"groupId": _groupId}
    paramsCompressed = zlib.compress(urllib.urlencode(params, doseq=True).encode('utf-8'))
    path = _webclient.path + "runs/flush"

    _request("POST", path, paramsCompressed, headers, expectedStatusCode=204)

    logging.info("Run submission finished.")

def _handleOptions(run, params, rlimits):
    # TODO use code from CPAchecker module, it add -stats and sets -timelimit,
    # instead of doing it here manually, too
    options = ["statistics.print=true"]
    if 'softtimelimit' in rlimits and not '-timelimit' in options:
        options.append("limits.time.cpu=" + str(rlimits['softtimelimit']) + "s")

    if run.options:
        i = iter(run.options)
        while True:
            try:
                option=next(i)
                if option == "-heap":
                    params['heap'] = next(i)

                elif option == "-noout":
                    options.append("output.disable=true")
                elif option == "-stats":
                    #ignore, is always set by this script
                    pass
                elif option == "-java":
                    options.append("language=JAVA")
                elif option == "-32":
                    options.append("analysis.machineModel=Linux32")
                elif option == "-64":
                    options.append("analysis.machineModel=Linux64")
                elif option == "-entryfunction":
                    options.append("analysis.entryFunction=" + next(i))
                elif option == "-timelimit":
                    options.append("limits.time.cpu=" + next(i))
                elif option == "-skipRecursion":
                    options.append("cpa.callstack.skipRecursion=true")
                    options.append("analysis.summaryEdges=true")

                elif option == "-spec":
                    spec  = next(i)[-1].split('.')[0]
                    if spec[-8:] == ".graphml":
                        with open(spec, 'r') as  errorWitnessFile:
                            errorWitnessText = errorWitnessFile.read()
                            params['errorWitnessText'] = errorWitnessText
                    else:
                        params['specification'] = spec
                elif option == "-config":
                    configPath = next(i)
                    tokens = configPath.split('/')
                    if not (tokens[0] == "config" and len(tokens) == 2):
                        logging.warning('Configuration {0} of run {1} is not from the default config directory.'.format(configPath, run.identifier))
                        return True
                    config  = next(i).split('/')[2].split('.')[0]
                    params['configuration'] = config

                elif option == "-setprop":
                    options.append(next(i))

                elif option[0] == '-' and 'configuration' not in params :
                    params['configuration'] = option[1:]
                else:
                    return True

            except StopIteration:
                break

    params['option'] = options
    return False

def _getResults(runIDs, output_handler, benchmark):
    executor = ThreadPoolExecutor(MAX_SUBMISSION_THREADS)

    while len(runIDs) > 0 :
        start = time()
        runIDsFutures = {}
        for runID in runIDs:
            if _isFinished(runID, benchmark):
                run = runIDs[runID]
                future  = executor.submit(_getAndHandleResult, runID, run, output_handler, benchmark)
                runIDsFutures[future] = runID

        # remove all finished run from _unfinished_run_ids
        for future in as_completed(runIDsFutures.keys()):
            if future.result():
                del runIDs[runIDsFutures[future]]
                _unfinished_run_ids.remove(runIDsFutures[future])

        end = time();
        duration = end - start
        if duration < 5:
            sleep(5 - duration)


def _isFinished(runID, benchmark):
    headers = {"Accept": "text/plain"}
    path = _webclient.path + "runs/" + runID + "/state"

    try:
        state = _request("GET", path,"", headers).decode('utf-8')


        if state == "FINISHED":
            logging.debug('Run {0} finished.'.format(runID))
            return True

        # UNKNOWN is returned for unknown runs. This happens,
        # when the webclient is restarted since the submission of the runs.
        if state == "UNKNOWN":
            logging.debug('Run {0} is not known by the webclient, trying to get the result.'.format(runID))
            return True

        else:
            return False

    except urllib2.HTTPError as e:
        logging.warning('Could not get run state {0}: {1}'.format(runID, e.reason))
        return False

def _getAndHandleResult(runID, run, output_handler, benchmark):

    # download result as zip file
    headers = {"Accept": "application/zip"}
    path = _webclient.path + "runs/" + runID + "/result"

    try:
        zipContent = _request("GET", path, {}, headers)
    except urllib2.HTTPError as e:
        logging.info('Could not get result of run {0}: {1}'.format(run.identifier, e))
        return False

    # unzip result
    return_value = None
    try:
        try:
            with zipfile.ZipFile(io.BytesIO(zipContent)) as resultZipFile:
                return_value = _handleResult(resultZipFile, run, output_handler, benchmark.result_files_pattern)
        except zipfile.BadZipfile:
            logging.warning('Server returned illegal zip file with results of run {}.'.format(run.identifier))
            # Dump ZIP to disk for debugging
            with open(run.log_file + '.zip', 'wb') as zipFile:
                zipFile.write(zipContent)
    except IOError as e:
        logging.warning('Error while writing results of run {}: {}'.format(run.identifier, e))

    if return_value is not None:
        run.after_execution(return_value)
        try:
            _print_lock.acquire()
            output_handler.output_before_run(run)
            output_handler.output_after_run(run)
        finally:
            _print_lock.release()
    return True

def _handleResult(resultZipFile, run, output_handler, result_files_pattern):
    resultDir = run.log_file + ".output"
    files = set(resultZipFile.namelist())

    # extract values
    if RESULT_FILE_RUN_INFO in files:
        with resultZipFile.open(RESULT_FILE_RUN_INFO) as runInformation:
            (run.walltime, run.cputime, return_value, values) = _parseCloudResultFile(runInformation)
            run.values.update(values)
    else:
        return_value = None
        logging.warning('Missing result for {}.'.format(run.identifier))

    if RESULT_FILE_HOST_INFO in files:
        with resultZipFile.open(RESULT_FILE_HOST_INFO) as hostInformation:
            values = _parseAndSetCloudWorkerHostInformation(hostInformation, output_handler, run.runSet)
            run.values.update(values)
    else:
        logging.warning('Missing host information for run {}.'.format(run.identifier))

    # extract log file
    if RESULT_FILE_LOG in files:
        with open(run.log_file, 'wb') as log_file:
            log_header = " ".join(run.cmdline()) + "\n\n\n--------------------------------------------------------------------------------\n"
            log_file.write(log_header.encode('utf-8'))
            with resultZipFile.open(RESULT_FILE_LOG) as result_log_file:
                for line in result_log_file:
                    log_file.write(line)
    else:
        logging.warning('Missing log file for run {}.'.format(run.identifier))

    if RESULT_FILE_STDERR in files:
        resultZipFile.extract(RESULT_FILE_STDERR, resultDir)
        shutil.move(os.path.join(resultDir, RESULT_FILE_STDERR), run.log_file + ".stdError")
        os.rmdir(resultDir)

    # extract result files:
    if result_files_pattern:
        files = files - SPECIAL_RESULT_FILES
        files = fnmatch.filter(files, result_files_pattern)
        if files:
            resultZipFile.extractall(resultDir, files)

    return return_value

def _parseAndSetCloudWorkerHostInformation(file, output_handler, runSet):
    values = _parseFile(file)

    values["host"] = values.pop("@vcloud-name", "-")
    name = values["host"]
    osName = values.pop("@vcloud-os", "-")
    memory = values.pop("@vcloud-memory", "-")
    cpuName = values.pop("@vcloud-cpuModel", "-")
    frequency = values.pop("@vcloud-frequency", "-")
    cores = values.pop("@vcloud-cores", "-")
    output_handler.store_system_info(osName, cpuName, cores, frequency, memory, name,
                                     runSet=runSet)

    return values


def _parseCloudResultFile(file):
    values = _parseFile(file)

    return_value = int(values["@vcloud-exitcode"])
    walltime = float(values["walltime"].strip('s'))
    cputime = float(values["cputime"].strip('s'))
    if "@vcloud-memory" in values:
        values["memUsage"] = int(values.pop("@vcloud-memory").strip('B'))

    # remove irrelevant columns
    values.pop("@vcloud-command", None)
    values.pop("@vcloud-timeLimit", None)
    values.pop("@vcloud-coreLimit", None)

    return (walltime, cputime, return_value, values)

def _parseFile(file):
    values = {}

    for line in file:
        (key, value) = line.decode('utf-8').split("=", 1)
        value = value.strip()
        if key in RESULT_KEYS or key.startswith("energy"):
            values[key] = value
        else:
            # "@" means value is hidden normally
            values["@vcloud-" + key] = value

    return values

def _request(method, path, body, headers, expectedStatusCode=200):
    connection = _get_connection()

    headers["Connection"] = "Keep-Alive"

    if _base64_user_pwd:
        headers["Authorization"] = "Basic " + _base64_user_pwd

    counter = 0
    while (counter < 5):
        counter+=1
        # send request
        connection.request(method, path, body=body, headers=headers)
        try:
            response = connection.getresponse()
        except:
            if (counter < 5):
                # create new TCP connection and try to send the request
                connection.close()
                sleep(1)
                continue
            else:
                raise

        if response.status == expectedStatusCode:
            return response.read()

        else:
            message = ""
            if response.status == 401:
                message = 'Please check the URL given to --cloudMaster.\n'
            if response.status == 404:
                message = 'Please check the URL given to --cloudMaster.'
            message += response.read().decode('UTF-8')
            logging.warn(message)
            raise urllib2.HTTPError(path, response.getcode(), message , response.getheaders(), None)

def _get_connection():
    connection = getattr(_thread_local, 'connection', None)

    if connection is None:
        if _webclient.scheme == 'http':
            _thread_local.connection = HTTPConnection(_webclient.netloc, timeout=_connectionTimeout)
        elif _webclient.scheme == 'https':
            _thread_local.connection = HTTPSConnection(_webclient.netloc, timeout=_connectionTimeout)
        else:
            raise WebClientError("Unknown protocol {0}.".format(_webclient.scheme))

        connection = _thread_local.connection

    return connection
