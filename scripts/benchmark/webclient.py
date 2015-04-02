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

import io
import logging
import os
import shutil
import zlib
import zipfile

from time import sleep

import urllib.parse as urllib
import urllib.request as urllib2
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

def init(config, benchmark):
    if config.cloudMaster:
        if config.revision:
            benchmark.tool_version = config.revision
        else:
            benchmark.tool_version = "trunk:HEAD"
    benchmark.executable = 'scripts/cpa.sh'

def get_system_info():
    return None

def execute_benchmark(benchmark, output_handler):

    if (benchmark.tool_name != 'CPAchecker'):
        logging.warning("The web client does only support the CPAchecker.")
        return

    if not benchmark.config.cloudMaster[-1] == '/':
        benchmark.config.cloudMaster += '/'
    webclient = benchmark.config.cloudMaster
    logging.info('Using webclient at {0}'.format(webclient))

    #authentification
    _auth(webclient, benchmark)

    STOPPED_BY_INTERRUPT = False
    try:
        for runSet in benchmark.run_sets:
            if not runSet.should_be_executed():
                output_handler.output_for_skipping_run_set(runSet)
                continue

            output_handler.output_before_run_set(runSet)
            runIDs = _submitRunsParallel(runSet, webclient, benchmark)

            _getResults(runIDs, output_handler, webclient, benchmark)
            output_handler.output_after_run_set(runSet)

    except KeyboardInterrupt as e:
        STOPPED_BY_INTERRUPT = True
        raise e
    finally:
        output_handler.output_after_benchmark(STOPPED_BY_INTERRUPT)

def stop():
    # TODO: cancel runs on server
    pass

def _submitRunsParallel(runSet, webclient, benchmark):

    logging.info('Submitting runs')

    runIDs = {}
    submissonCounter = 1
    #submit to executor
    executor = ThreadPoolExecutor(MAX_SUBMISSION_THREADS)
    runIDsFutures = {executor.submit(_submitRun, run, webclient, benchmark): run for run in runSet.runs}
    executor.shutdown(wait=False)

    #collect results to executor
    for future in as_completed(runIDsFutures.keys()):
        try:
            run = runIDsFutures[future]
            runID = future.result().decode("utf-8")
            runIDs.update({runID:run})
            logging.info('Submitted run {0}/{1} with id {2}'.\
                format(submissonCounter, len(runSet.runs), runID))

        except (urllib2.HTTPError, WebClientError) as e:
            try:
                if e.code == 401:
                    message = 'Please specify username and password with --cloudUser.'
                elif e.code == 404:
                    message = 'Please check the URL given to --cloudMaster.'
                else:
                    message = e.read() #not all HTTPErrors have a read() method
            except AttributeError:
                message = ""
            logging.warning('Could not submit run {0}: {1}. {2}'.\
                format(run.identifier, e, message))
        finally:
            submissonCounter += 1

    return runIDs

def _submitRun(run, webclient, benchmark, counter = 0):
    programTexts = []
    for programPath in run.sourcefiles:
        with open(programPath, 'r') as programFile:
            programText = programFile.read()
            programTexts.append(programText)
    params = {'programText': programTexts}

    if benchmark.config.revision:
        tokens = benchmark.config.revision.split(':')
        params['svnBranch'] = tokens[0]
        if len(tokens)>1:
            params['revision'] = tokens[1]

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

    headers = {"Content-Type": "application/x-www-form-urlencoded",
               "Content-Encoding": "deflate",
               "Accept": "text/plain"}
    paramsCompressed = zlib.compress(urllib.urlencode(params, doseq=True).encode('utf-8'))
    request = urllib2.Request(webclient + "runs/", paramsCompressed, headers)

    # send request
    try:
        response = urllib2.urlopen(request)

    except urllib2.HTTPError as e:
        if (e.code == 401 and counter < 3):
            _auth(webclient, benchmark)
            return _submitRun(run, webclient, benchmark, counter + 1)
        else:
            raise e

    if response.getcode() == 200:
        runID = response.read()
        return runID

    else:
        raise urllib2.HTTPError(response.read(), response.getcode())

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

def _getResults(runIDs, output_handler, webclient, benchmark):
    while len(runIDs) > 0 :
        finishedRunIDs = []
        for runID in runIDs.keys():
            if _isFinished(runID, webclient, benchmark):
                if(_getAndHandleResult(runID, runIDs[runID], output_handler, webclient, benchmark)):
                    finishedRunIDs.append(runID)

        for runID in finishedRunIDs:
            del runIDs[runID]

def _isFinished(runID, webclient, benchmark):

    headers = {"Accept": "text/plain"}
    request = urllib2.Request(webclient + "runs/" + runID + "/state", headers=headers)
    try:
        response = urllib2.urlopen(request)
    except urllib2.HTTPError as e:
        logging.info('Could get result of run with id {0}: {1}'.format(runID, e))
        _auth(webclient, benchmark)
        sleep(10)
        return False

    if response.getcode() == 200:
        state = response.read().decode('utf-8')

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

    else:
        logging.warning('Could not get run state: {0}'.format(runID))

        return False

def _getAndHandleResult(runID, run, output_handler, webclient, benchmark):
    # download result as zip file
    counter = 0
    success = False
    while (not success and counter < 10):
        counter += 1
        request = urllib2.Request(webclient + "runs/" + runID + "/result")
        try:
            response = urllib2.urlopen(request)
        except urllib2.HTTPError as e:
            logging.info('Could not get result of run {0}: {1}'.format(run.identifier, e))
            _auth(webclient, benchmark)
            sleep(10)
            return False

        if response.getcode() == 200:
            zipContent = response.read()
            success = True
        else:
            sleep(1)

    if success:
        # unzip result
        output_handler.output_before_run(run)
        return_value = -1
        try:
            try:
                with zipfile.ZipFile(io.BytesIO(zipContent)) as resultZipFile:
                    return_value = _handleResult(resultZipFile, run, output_handler)
            except zipfile.BadZipfile:
                logging.warning('Server returned illegal zip file with results of run {}.'.format(run.identifier))
                # Dump ZIP to disk for debugging
                with open(run.log_file + '.zip', 'wb') as zipFile:
                    zipFile.write(zipContent)
        except IOError as e:
            logging.warning('Error while writing results of run {}: {}'.format(run.identifier, e))

        run.after_execution(return_value)

        output_handler.output_after_run(run)
        return True

    else:
        logging.warning('Could not get run result, run is not finished: {0}'.format(runID))
        return False

def _handleResult(resultZipFile, run, output_handler):
    resultDir = run.log_file + ".output"
    files = set(resultZipFile.namelist())

    # extract values
    if RESULT_FILE_RUN_INFO in files:
        with resultZipFile.open(RESULT_FILE_RUN_INFO) as runInformation:
            (run.walltime, run.cputime, return_value, values) = _parseCloudResultFile(runInformation)
            run.values.update(values)
    else:
        return_value = -1
        logging.warning('Missing result for {}.'.format(run.identifier))

    if RESULT_FILE_HOST_INFO in files:
        with resultZipFile.open(RESULT_FILE_HOST_INFO) as hostInformation:
            values = _parseAndSetCloudWorkerHostInformation(hostInformation, output_handler)
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

    files = files - SPECIAL_RESULT_FILES
    if files:
        resultZipFile.extractall(resultDir, files)

    return return_value

def _parseAndSetCloudWorkerHostInformation(file, output_handler):
    values = _parseFile(file)

    values["host"] = values.pop("@vcloud-name", "-")
    name = values["host"]
    osName = values.pop("@vcloud-os", "-")
    memory = values.pop("@vcloud-memory", "-")
    cpuName = values.pop("@vcloud-cpuModel", "-")
    frequency = values.pop("@vcloud-frequency", "-")
    cores = values.pop("@vcloud-cores", "-")
    output_handler.store_system_info(osName, cpuName, cores, frequency, memory, name)

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
    values.pop("@vcloud-memoryLimit", None)

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

def _auth(webclient, benchmark):
    if benchmark.config.cloudUser:
        tokens = benchmark.config.cloudUser.split(':')
        if not len(tokens) == 2:
            logging.warning('Invalid username password format, expected {user}:{pwd}')
            return
        username = tokens[0]
        password = tokens[1]
        auth_handler = urllib2.HTTPBasicAuthHandler(urllib2.HTTPPasswordMgrWithDefaultRealm())
        auth_handler.add_password(realm=None,\
                        uri=webclient,\
                        user=username,\
                        passwd=password)
        opener = urllib2.build_opener(auth_handler)
        # install it globally so it can be used with urlopen
        urllib2.install_opener(opener)
