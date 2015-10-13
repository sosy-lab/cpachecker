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
import hashlib
import io
import logging
import os
import random
import shutil
import tempfile
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
from concurrent.futures import Future

MEMLIMIT = 'memlimit'
TIMELIMIT = 'timelimit'
SOFTTIMELIMIT = 'softtimelimit'
CORELIMIT = 'corelimit'

RESULT_KEYS = ["cputime", "walltime"]

MAX_SUBMISSION_THREADS = 5

RESULT_FILE_LOG = 'output.log'
RESULT_FILE_STDERR = 'stderr'
RESULT_FILE_RUN_INFO = 'runInformation.txt'
RESULT_FILE_HOST_INFO = 'hostInformation.txt'
SPECIAL_RESULT_FILES = {RESULT_FILE_LOG, RESULT_FILE_STDERR, RESULT_FILE_RUN_INFO,
                        RESULT_FILE_HOST_INFO, 'runDescription.txt'}

CONNECTION_TIMEOUT = 600 #seconds
HASH_CODE_CACHE_PATH = os.path.join(os.path.expanduser("~"), ".verifiercloud/cache/hashCodeCache")

class WebClientError(Exception):
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return repr(self.value)
    
class WebInterface:
    """
    The WebInterface is a executor like class for the submission of runs to the VerifierCloud
    """
    
    def __init__(self, web_interface_url, user_pwd, svn_branch='trunk', svn_revision='HEAD'):
        """
        Creates a new WebInterface object. 
        The given svn revision is resolved (e.g. 'HEAD' -> 17495).
        @param web_interface_url: the base URL of the VerifierCloud's web interface
        @param user_pwd: user name and password in the format '<user_name>:<password>' or none if no authentification is required
        @param svn_branch: the svn branch name or 'trunk', defaults to 'trunk'
        @param svn_revision: the svn revision number or 'HEAD', defaults to 'HEAD'
        """
        if not web_interface_url[-1] == '/':
            web_interface_url += '/'

        self.__webclient = urllib.urlparse(web_interface_url)
        logging.info('Using webclient at {0}'.format(web_interface_url))

        if user_pwd:
            self.__base64_user_pwd = base64.b64encode(user_pwd.encode("utf-8")).decode("utf-8")
        else:
            self.__base64_user_pwd = None


        self.__shutdown = threading.Event()
        self.__thread_local = threading.local() 
        self.__hash_code_cache = {}
        self.__group_id = str(random.randint(0, 1000000))
        self.__unfinished_runs = {}        
        self.__unfinished_runs_lock = threading.Lock()
        self.__read_hash_code_cache()
        self.__resolved__tool_revision(svn_branch, svn_revision)
                
        self.__executor = ThreadPoolExecutor(MAX_SUBMISSION_THREADS)        
        self.__state_poll_thread = threading.Thread(target=self.__get_results, name='web_interface_state_poll_thread')
        self.__state_poll_thread.start()
        
    def __read_hash_code_cache(self):
        if not os.path.isfile(HASH_CODE_CACHE_PATH):        
            return
            
        with open(HASH_CODE_CACHE_PATH, mode='r') as hashCodeCacheFile:
            for line in hashCodeCacheFile:
                tokens = line.strip().split('\t')
                if len(tokens) == 3:
                    self.__hash_code_cache[(tokens[0],tokens[1])]= tokens[2]

    def __write_hash_code_cache(self):
        directory = os.path.dirname(HASH_CODE_CACHE_PATH)
        os.makedirs(directory, exist_ok=True)
        with tempfile.NamedTemporaryFile(dir=directory, delete=False) as tmpFile:
            for (path, mTime), hashValue in self.__hash_code_cache.items():
                line = (path + '\t' + mTime + '\t' + hashValue + '\n').encode()
                tmpFile.write(line)
            
            os.renames(tmpFile.name, HASH_CODE_CACHE_PATH)

    def __resolved__tool_revision(self, svn_branch, svn_revision):
    
        path = self.__webclient.path + "tool/version?svnBranch=" + svn_branch \
                             + "&revision=" + svn_revision

        (resolved_svn_revision, _) = self.__request("GET", path)
        self.__svn_branch = svn_branch
        self.__svn_revision = resolved_svn_revision.decode("UTF-8")
        
    def tool_revision(self):
        return self.__svn_branch + ':' + self.__svn_revision
    
    def __get_sha1_hash(self, path):
        path = os.path.abspath(path)
        mTime = str(os.path.getmtime(path))
        if ((path, mTime) in self.__hash_code_cache):
            return self.__hash_code_cache[(path, mTime)]
        
        else:
            with open(path, 'rb') as file:
                hashValue = hashlib.sha1(file.read()).hexdigest()
                self.__hash_code_cache[(path, mTime)] = hashValue
                return hashValue
    
    def submit_witness_validation(self, witness_path, program_path, configuration=None, user_pwd=None):
        """
        Submits a single witness validation run to the VerifierCloud.
        @note: flush() should be called after the submission of the last run.
        @param witness_path: path to the file containing the witness
        @param program_path: path to the file containing the program
        @param configuration: name of configuration (optional)
        @param user_pwd: overrides the user name and password given in the constructor (optional)
        """
        
        # collect parameters
        params = {}
        
        with open(witness_path, 'rb') as witness_file:
            params['errorWitnessText'] = witness_file.read()
        
        with open(program_path, 'rb') as program_file:
            params['programText'] = program_file.read()
        
        if configuration:
            params['configuration'] = configuration
        
        # prepare request
        headers = {"Content-Type": "application/x-www-form-urlencoded",
                   "Content-Encoding": "deflate",
                   "Accept": "text/plain"}
    
        paramsCompressed = zlib.compress(urllib.urlencode(params, doseq=True).encode('utf-8'))
        path = self.__webclient.path + "runs/"
    
        (run_id, _) = self.__request("POST", path, paramsCompressed, headers, user_pwd=user_pwd)
        
        run_id = run_id.decode("UTF-8")
        logging.debug('Submitted witness validation run with id {0}'.format(run_id))
        
        result = Future()
        with self.__unfinished_runs_lock:
            self.__unfinished_runs[run_id] =  result
            
        return result      
    
    def submit(self, run, limits, cpu_model, result_files_pattern = None, priority = 'IDLE', user_pwd=None, svn_branch=None, svn_revision=None):
        """
        Submits a single run to the VerifierCloud.
        @note: flush() should be called after the submission of the last run.
        @param run: The input for the run:  command line options (run.options), 
                                            source files (run.sourcefiles), 
                                            property file (run.propertyfile), 
                                            identifier for error messages (run.identifier)
        @param limits: dict of limitations for the run (memlimit, timelimit, corelimit, softtimelimit)
        @param cpu_model: substring of CPU model to use or 'None' for no restriction
        @param result_files_pattern: the result is filtered with the given glob pattern, defaults to no restriction
        @param priority: the priority of the submitted run, defaults to 'IDLE'
        @param user_pwd: overrides the user name and password given in the constructor (optional)
        @param svn_branch: overrids the svn branch given in the constructor (optional)
        @param svn_revision: overrides the svn revision given in the constructor (optional)
        """
        return self.__submit(run, limits, cpu_model, result_files_pattern, priority, user_pwd, svn_branch, svn_revision)
    
    def __submit(self, run, limits, cpu_model, result_files_pattern, priority, user_pwd, svn_branch, svn_revision, counter = 0):
    
        programTextHashs = []
        for programPath in run.sourcefiles:
            programTextHashs.append(self.__get_sha1_hash(programPath))
        params = {'programTextHash': programTextHashs}
    
        params['svnBranch'] = svn_branch or self.__svn_branch
        params['revision'] = svn_revision or self.__svn_revision
    
        if run.propertyfile:
            with open(run.propertyfile, 'r') as propertyFile:
                propertyText = propertyFile.read()
                params['propertyText'] = propertyText
    
        if MEMLIMIT in limits:
            params['memoryLimitation'] = str(limits[MEMLIMIT]) + "MB"
        if TIMELIMIT in limits:
            params['timeLimitation'] = limits[TIMELIMIT]
        if SOFTTIMELIMIT in limits:
            params['softTimeLimitation'] = limits[SOFTTIMELIMIT]
        if CORELIMIT in limits:
            params['coreLimitation'] = limits[CORELIMIT]
        if cpu_model:
            params['cpuModel'] = cpu_model
    
        if result_files_pattern:
            params['resultFilesPattern'] = result_files_pattern;
        else:
            params['resultFilesPattern'] = ''
        if priority:
            params['priority'] = priority
    
        invalidOption = self.__handle_options(run, params, limits)
        if invalidOption:
            raise WebClientError('Command {0}  contains option "{1}" that is not usable with the webclient. '\
                .format(run.options, invalidOption))
    
        params['groupId'] = self.__group_id;
    
        # prepare request
        headers = {"Content-Type": "application/x-www-form-urlencoded",
                   "Content-Encoding": "deflate",
                   "Accept": "text/plain"}
    
        paramsCompressed = zlib.compress(urllib.urlencode(params, doseq=True).encode('utf-8'))
        path = self.__webclient.path + "runs/"
    
        (run_id, statusCode) = self.__request("POST", path, paramsCompressed, headers, [200, 412], user_pwd)
        
        # program files given as hash value are not known by the cloud system
        if statusCode == 412 and counter < 1: 
            headers = {"Content-Type": "application/octet-stream",
                   "Content-Encoding": "deflate"}
    
            # upload all used program files
            filePath = self.__webclient.path + "files/"
            for programPath in run.sourcefiles:
                with open(programPath, 'rb') as programFile:
                    compressedProgramText = zlib.compress(programFile.read(), 9)
                    self.__request('POST', filePath, compressedProgramText, headers, [200,204], user_pwd)
                    
            # retry submission of run
            return self.__submit(run, limits, cpu_model, result_files_pattern, priority, user_pwd, svn_branch, svn_revision, counter + 1)
        
        else:
            run_id = run_id.decode("UTF-8")
            logging.debug('Submitted run with id {0}'.format(run_id))
            result = Future()
            with self.__unfinished_runs_lock:
                self.__unfinished_runs[run_id] =  result
            return result
            
    def __handle_options(self, run, params, rlimits):
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
                    elif option == "-disable-java-assertions":
                        params['disableJavaAssertions'] = 'true' 
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
                        spec_path  = next(i)
                        with open(spec_path, 'r') as  spec_file:
                            file_text = spec_file.read()
                            if spec_path[-8:] == ".graphml":
                                params['errorWitnessText'] = file_text
                            elif spec_path[-4:] == ".prp":
                                params['propertyText'] = file_text
                            else:
                                params['specificationText'] = file_text
                            
                    elif option == "-config":
                        configPath = next(i)
                        tokens = configPath.split('/')
                        if not (tokens[0] == "config" and len(tokens) == 2):
                            logging.warning('Configuration {0} of run {1} is not from the default config directory.'.format(configPath, run.identifier))
                            return configPath
                        config  = next(i).split('/')[2].split('.')[0]
                        params['configuration'] = config
    
                    elif option == "-setprop":
                        options.append(next(i))
    
                    elif option[0] == '-' and 'configuration' not in params :
                        params['configuration'] = option[1:]
                    else:
                        return option
    
                except StopIteration:
                    break
    
        params['option'] = options
        return None
    
    def flush_runs(self):
        """
        Starts the execution of all previous submitted runs in the VerifierCloud.
        The web interface groups runs and submits them to the VerifierCloud only from time to time.
        This method forces the web interface to do this immediately.
        """
        headers = {"Content-Type": "application/x-www-form-urlencoded",
                   "Content-Encoding": "deflate",
                   "Connection": "Keep-Alive"}
    
        params = {"groupId": self.__group_id}
        paramsCompressed = zlib.compress(urllib.urlencode(params, doseq=True).encode('utf-8'))
        path = self.__webclient.path + "runs/flush"
    
        self.__request("POST", path, paramsCompressed, headers, expectedStatusCodes=[200,204])
    
    def __get_results(self):
        downloading_result_futures = {}
        state_request_executor = ThreadPoolExecutor(max_workers=MAX_SUBMISSION_THREADS)
        
        def callback(downloaded_result): 
            run_id = downloading_result_futures[downloaded_result]
            if not downloaded_result.exception():
                with self.__unfinished_runs_lock:
                    result_future = self.__unfinished_runs.pop(run_id, None)
                if result_future:
                    result_future.set_result(downloaded_result.result())
                del downloading_result_futures[downloaded_result]
                 
            else:
                logging.info('Could not get result of run {0}: {1}'.format(run_id, downloaded_result.exception()))

        # in every iteration the states of all unfinished runs are requested once
        while not self.__shutdown.is_set() :
            start = time()
            states = {}
            failed_runs = []
            
            with self.__unfinished_runs_lock:
                for run_id in self.__unfinished_runs.keys():
                    state_future = state_request_executor.submit(self.__is_finished, run_id)
                    states[state_future] = run_id
                              
            # Collect states of runs
            for state_future in as_completed(states.keys()):
                               
                run_id = states[state_future]
                state = state_future.result()
                if state == "FINISHED" or state == "UNKOWN":
                    if run_id not in downloading_result_futures.values(): # result is not downloaded
                        future  = self.__executor.submit(self.__download_result, run_id)
                        downloading_result_futures[future] = run_id
                        future.add_done_callback(callback)
                        
                elif state == "ERROR":
                    failed_runs.append(run_id)
                    
                elif self.__unfinished_runs[run_id].cancelled():
                    self.__stop_run(run_id)               
            
            # set exception for failed runs
            for run_id in failed_runs:
                self.__unfinished_runs[run_id].set_exception(WebClientError("Execution failed."))
                del self.__unfinished_runs[run_id]
                        
            end = time();
            duration = end - start
            if duration < 5 and not self.__shutdown:
                self.__shutdown.wait(5 - duration)
    
        state_request_executor.shutdown()
    
    def __is_finished(self, run_id):
        headers = {"Accept": "text/plain"}
        path = self.__webclient.path + "runs/" + run_id + "/state"
    
        try:
            (state, _) = self.__request("GET", path,"", headers)
    
            state = state.decode('utf-8')
            if state == "FINISHED":
                logging.debug('Run {0} finished.'.format(run_id))
    
            if state == "UNKNOWN":
                logging.debug('Run {0} is not known by the webclient, trying to get the result.'.format(run_id))
    
            return state
    
        except urllib2.HTTPError as e:
            logging.warning('Could not get run state {0}: {1}'.format(run_id, e.reason))
            return False
    
    def __download_result(self, run_id):
        # download result as zip file
        headers = {"Accept": "application/zip"}
        path = self.__webclient.path + "runs/" + run_id + "/result"
        (zip_content, _) = self.__request("GET", path, {}, headers)
        return zip_content
    
    def shutdown(self):
        """
        Cancels all unfinished runs and stops all internal threads.
        """
        self.__shutdown.set()
        
        if len(self.__unfinished_runs) > 0:
            logging.info("Stopping tasks on server...")
            stopTasks = set()
            with self.__unfinished_runs_lock:
                for runId in self.__unfinished_runs.keys():
                    stopTasks.add(self.__executor.submit(self.__stop_run, runId))
                    
            for task in stopTasks:
                task.result()
            self.__executor.shutdown(wait=True)
            logging.info("Stopped all tasks.")
            
        else:
            self.__executor.shutdown(wait=True)
        
        self.__write_hash_code_cache()

    def __stop_run(self, run_id):
        path = self.__webclient.path + "runs/" + run_id
        try:
            self.__request("DELETE", path, expectedStatusCodes = [200,204])
        except urllib2.HTTPError as e:
            logging.warn("Stopping of run {0} failed: {1}".format(run_id, e.reason))

    def __request(self, method, path, body={}, headers={}, expectedStatusCodes=[200], user_pwd=None):
        connection = self.__get_connection()
    
        headers["Connection"] = "Keep-Alive"
    
        if user_pwd:
            base64_user_pwd = base64.b64encode(user_pwd.encode("utf-8")).decode("utf-8")
            headers["Authorization"] = "Basic " + base64_user_pwd
        elif self.__base64_user_pwd:
            headers["Authorization"] = "Basic " + self.__base64_user_pwd            
    
        counter = 0
        while (counter < 5):
            counter+=1
            # send request
            try:
                connection.request(method, path, body=body, headers=headers)
                response = connection.getresponse()
            except Exception as e:
                if (counter < 5):
                    logging.debug("Exception during {} request to {}: {}".format(method, path, e))
                    # create new TCP connection and try to send the request
                    connection.close()
                    sleep(1)
                    continue
                else:
                    raise
    
            if response.status in expectedStatusCodes:
                return (response.read(), response.getcode())
    
            else:
                message = ""
                if response.status == 401:
                    message = 'Please check the URL given to --cloudMaster.\n'
                if response.status == 404:
                    message = 'Please check the URL given to --cloudMaster.'
                message += response.read().decode('UTF-8')
                logging.warn(message)
                raise urllib2.HTTPError(path, response.getcode(), message , response.getheaders(), None)
    
    def __get_connection(self):
        connection = getattr(self.__thread_local, 'connection', None)
    
        if connection is None:
            if self.__webclient.scheme == 'http':
                self.__thread_local.connection = HTTPConnection(self.__webclient.netloc, timeout=CONNECTION_TIMEOUT)
            elif self.__webclient.scheme == 'https':
                self.__thread_local.connection = HTTPSConnection(self.__webclient.netloc, timeout=CONNECTION_TIMEOUT)
            else:
                raise WebClientError("Unknown protocol {0}.".format(self.__webclient.scheme))
    
            connection = self.__thread_local.connection
    
        return connection

__webclient = None
__print_lock = threading.Lock()

def init(config, benchmark):
    global __webclient
    
    if not benchmark.config.cpu_model:
        logging.warning("It is strongly recommended to set a CPU model('--cloudCPUModel'). "\
                        "Otherwise the used machines and CPU models are undefined.")

    if not config.cloudMaster:
        logging.warning("No URL of a VerifierCloud instance is given.")
        return
        
    if config.revision:
        tokens = config.revision.split(':')
        svn_branch = tokens[0]
        if len(tokens) > 1:
            svn_revision = config.revision.split(':')[1]
        else:
            svn_revision = 'HEAD'
    else:
        svn_branch = 'trunk'
        svn_revision = 'HEAD'
        
    __webclient = WebInterface(config.cloudMaster, config.cloudUser, svn_branch, svn_revision)

    benchmark.tool_version = __webclient.tool_revision()
    logging.info('Using tool version {0}.'.format(benchmark.tool_version))
    benchmark.executable = 'scripts/cpa.sh'

def get_system_info():
    return None

def execute_benchmark(benchmark, output_handler):
    global __webclient

    if (benchmark.tool_name != 'CPAchecker'):
        logging.warning("The web client does only support the CPAchecker.")
        return
    
    if not __webclient:
        logging.warning("No valid URL of a VerifierCloud instance is given.")
        return

    STOPPED_BY_INTERRUPT = False
    try:
        for runSet in benchmark.run_sets:
            if not runSet.should_be_executed():
                output_handler.output_for_skipping_run_set(runSet)
                continue

            output_handler.output_before_run_set(runSet)
            result_futures = _submitRunsParallel(runSet, benchmark)

            __handle_results(result_futures, output_handler, benchmark)
            output_handler.output_after_run_set(runSet)

    except KeyboardInterrupt as e:
        STOPPED_BY_INTERRUPT = True
        raise e
    except:
        stop()
        raise
    finally:
        output_handler.output_after_benchmark(STOPPED_BY_INTERRUPT)
        
    stop()

def stop():
    global __webclient
    __webclient.shutdown()
    __webclient = None
    
def _submitRunsParallel(runSet, benchmark):
    global __webclient
    
    logging.info('Submitting runs...')

    executor = ThreadPoolExecutor(MAX_SUBMISSION_THREADS)
    submission_futures = {}
    submissonCounter = 1
    limits = benchmark.rlimits
    cpu_model = benchmark.config.cpu_model
    result_files_pattern = benchmark.result_files_pattern
    priority = benchmark.config.cloudPriority
    
    for run in runSet.runs:
        submisson_future = executor.submit(__webclient.submit, run, limits, cpu_model, result_files_pattern, priority)
        submission_futures[submisson_future] = run
        
    executor.shutdown(wait=False)

    #collect results futures
    result_futures = {}
    try:
        for future in as_completed(submission_futures.keys()):
            try:
                run = submission_futures[future]
                result_futures[future.result()] = run

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
        for future in submission_futures.keys():
            future.cancel() # for example in case of interrupt

    __webclient.flush_runs()
    logging.info("Run submission finished.")
    return result_futures

def __handle_results(result_futures, output_handler, benchmark):
    executor = ThreadPoolExecutor(MAX_SUBMISSION_THREADS)

    for result_future in as_completed(result_futures.keys()):
        run = result_futures[result_future]
        result = result_future.result()
        executor.submit(__unzip_and_handle_result, result, run, output_handler, benchmark)

def __unzip_and_handle_result(result, run, output_handler, benchmark):
    
    # unzip result
    return_value = None
    try:
        try:
            with zipfile.ZipFile(io.BytesIO(result)) as resultZipFile:
                return_value = __parse_result(resultZipFile, run, output_handler, benchmark.result_files_pattern)
        except zipfile.BadZipfile:
            logging.warning('Server returned illegal zip file with results of run {}.'.format(run.identifier))
            # Dump ZIP to disk for debugging
            with open(run.log_file + '.zip', 'wb') as zipFile:
                zipFile.write(result)
    except IOError as e:
        logging.warning('Error while writing results of run {}: {}'.format(run.identifier, e))

    if return_value is not None:
        run.after_execution(return_value)
        
        with __print_lock:
            output_handler.output_before_run(run)
            output_handler.output_after_run(run)

def __parse_result(resultZipFile, run, output_handler, result_files_pattern):
    resultDir = run.log_file + ".output"
    files = set(resultZipFile.namelist())

    # extract values
    if RESULT_FILE_RUN_INFO in files:
        with resultZipFile.open(RESULT_FILE_RUN_INFO) as runInformation:
            (run.walltime, run.cputime, return_value, values) = _parse_cloud_result_file(runInformation)
            run.values.update(values)
    else:
        return_value = None
        logging.warning('Missing result for {}.'.format(run.identifier))

    if RESULT_FILE_HOST_INFO in files:
        with resultZipFile.open(RESULT_FILE_HOST_INFO) as hostInformation:
            values = __parse_and_set_cloud_worker_host_information(hostInformation, output_handler, run.runSet)
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

def __parse_and_set_cloud_worker_host_information(file, output_handler, runSet):
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


def _parse_cloud_result_file(file):
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