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
import hashlib
import io
import logging
import os
import random
import tempfile
import threading
import zipfile
import zlib

from time import sleep
from time import time

import urllib.parse as urllib
import urllib.request as urllib2
from  http.client import HTTPConnection
from  http.client import HTTPSConnection
from concurrent.futures import ThreadPoolExecutor
from concurrent.futures import as_completed
from concurrent.futures import Future

"""
This module provides helpers for accessing the web interface of the VerifierCloud.
"""

__all__ = [
    'WebClientError', 'WebInterface', 'handle_result',
    'MEMLIMIT', 'TIMELIMIT', 'SOFTTIMELIMIT', 'CORELIMIT',
    'RESULT_KEYS',
    'MAX_SUBMISSION_THREADS',
    'RESULT_FILE_LOG', 'RESULT_FILE_STDERR', 'RESULT_FILE_RUN_INFO', 'RESULT_FILE_HOST_INFO', 'SPECIAL_RESULT_FILES',
    ]

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
    def _init_(self, value):
        self.value = value
    def _str_(self):
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

        self._webclient = urllib.urlparse(web_interface_url)
        logging.info('Using webclient at {0}'.format(web_interface_url))

        if user_pwd:
            self._base64_user_pwd = base64.b64encode(user_pwd.encode("utf-8")).decode("utf-8")
        else:
            self._base64_user_pwd = None


        self._shutdown = threading.Event()
        self._thread_local = threading.local() 
        self._hash_code_cache = {}
        self._group_id = str(random.randint(0, 1000000))
        self._unfinished_runs = {}        
        self._unfinished_runs_lock = threading.Lock()
        self._read_hash_code_cache()
        self._resolved_tool_revision(svn_branch, svn_revision)
                
        self._executor = ThreadPoolExecutor(MAX_SUBMISSION_THREADS)        
        self._state_poll_thread = threading.Thread(target=self._get_results, name='web_interface_state_poll_thread')
        self._state_poll_thread.start()
        
    def _read_hash_code_cache(self):
        if not os.path.isfile(HASH_CODE_CACHE_PATH):        
            return
            
        with open(HASH_CODE_CACHE_PATH, mode='r') as hashCodeCacheFile:
            for line in hashCodeCacheFile:
                tokens = line.strip().split('\t')
                if len(tokens) == 3:
                    self._hash_code_cache[(tokens[0],tokens[1])]= tokens[2]

    def _write_hash_code_cache(self):
        directory = os.path.dirname(HASH_CODE_CACHE_PATH)
        os.makedirs(directory, exist_ok=True)
        with tempfile.NamedTemporaryFile(dir=directory, delete=False) as tmpFile:
            for (path, mTime), hashValue in self._hash_code_cache.items():
                line = (path + '\t' + mTime + '\t' + hashValue + '\n').encode()
                tmpFile.write(line)
            
            os.renames(tmpFile.name, HASH_CODE_CACHE_PATH)

    def _resolved_tool_revision(self, svn_branch, svn_revision):
    
        path = self._webclient.path + "tool/version?svnBranch=" + svn_branch \
                             + "&revision=" + svn_revision

        (resolved_svn_revision, _) = self._request("GET", path)
        self._svn_branch = svn_branch
        self._svn_revision = resolved_svn_revision.decode("UTF-8")
        
    def tool_revision(self):
        return self._svn_branch + ':' + self._svn_revision
    
    def _get_sha1_hash(self, path):
        path = os.path.abspath(path)
        mTime = str(os.path.getmtime(path))
        if ((path, mTime) in self._hash_code_cache):
            return self._hash_code_cache[(path, mTime)]
        
        else:
            with open(path, 'rb') as file:
                hashValue = hashlib.sha1(file.read()).hexdigest()
                self._hash_code_cache[(path, mTime)] = hashValue
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
        path = self._webclient.path + "runs/witness_validation/"
    
        (run_id, _) = self._request("POST", path, paramsCompressed, headers, user_pwd=user_pwd)
        
        run_id = run_id.decode("UTF-8")
        logging.debug('Submitted witness validation run with id {0}'.format(run_id))
        
        result = Future()
        with self._unfinished_runs_lock:
            self._unfinished_runs[run_id] =  result
            
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
        return self._submit(run, limits, cpu_model, result_files_pattern, priority, user_pwd, svn_branch, svn_revision)
    
    def _submit(self, run, limits, cpu_model, result_files_pattern, priority, user_pwd, svn_branch, svn_revision, counter = 0):
    
        programTextHashs = []
        for programPath in run.sourcefiles:
            programTextHashs.append(self._get_sha1_hash(programPath))
        params = {'programTextHash': programTextHashs}
    
        params['svnBranch'] = svn_branch or self._svn_branch
        params['revision'] = svn_revision or self._svn_revision
    
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
    
        invalidOption = self._handle_options(run, params, limits)
        if invalidOption:
            raise WebClientError('Command {0}  contains option "{1}" that is not usable with the webclient. '\
                .format(run.options, invalidOption))
    
        params['groupId'] = self._group_id;
    
        # prepare request
        headers = {"Content-Type": "application/x-www-form-urlencoded",
                   "Content-Encoding": "deflate",
                   "Accept": "text/plain"}
    
        paramsCompressed = zlib.compress(urllib.urlencode(params, doseq=True).encode('utf-8'))
        path = self._webclient.path + "runs/"
    
        (run_id, statusCode) = self._request("POST", path, paramsCompressed, headers, [200, 412], user_pwd)
        
        # program files given as hash value are not known by the cloud system
        if statusCode == 412 and counter < 1: 
            headers = {"Content-Type": "application/octet-stream",
                   "Content-Encoding": "deflate"}
    
            # upload all used program files
            filePath = self._webclient.path + "files/"
            for programPath in run.sourcefiles:
                with open(programPath, 'rb') as programFile:
                    compressedProgramText = zlib.compress(programFile.read(), 9)
                    self._request('POST', filePath, compressedProgramText, headers, [200,204], user_pwd)
                    
            # retry submission of run
            return self._submit(run, limits, cpu_model, result_files_pattern, priority, user_pwd, svn_branch, svn_revision, counter + 1)
        
        else:
            run_id = run_id.decode("UTF-8")
            logging.debug('Submitted run with id {0}'.format(run_id))
            result = Future()
            with self._unfinished_runs_lock:
                self._unfinished_runs[run_id] =  result
            return result
            
    def _handle_options(self, run, params, rlimits):
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
    
        params = {"groupId": self._group_id}
        paramsCompressed = zlib.compress(urllib.urlencode(params, doseq=True).encode('utf-8'))
        path = self._webclient.path + "runs/flush"
    
        self._request("POST", path, paramsCompressed, headers, expectedStatusCodes=[200,204])
    
    def _get_results(self):
        downloading_result_futures = {}
        state_request_executor = ThreadPoolExecutor(max_workers=MAX_SUBMISSION_THREADS)
        
        def callback(downloaded_result): 
            run_id = downloading_result_futures[downloaded_result]
            if not downloaded_result.exception():
                with self._unfinished_runs_lock:
                    result_future = self._unfinished_runs.pop(run_id, None)
                if result_future:
                    result_future.set_result(downloaded_result.result())
                del downloading_result_futures[downloaded_result]
                 
            else:
                logging.info('Could not get result of run {0}: {1}'.format(run_id, downloaded_result.exception()))

        # in every iteration the states of all unfinished runs are requested once
        while not self._shutdown.is_set() :
            start = time()
            states = {}
            
            with self._unfinished_runs_lock:
                for run_id in self._unfinished_runs.keys():
                    if self._unfinished_runs[run_id].cancelled():
                        self.state_request_executor.submit(self._stop_run, run_id)
                    else:
                        state_future = state_request_executor.submit(self._is_finished, run_id)
                        states[state_future] = run_id
                              
            # Collect states of runs
            for state_future in as_completed(states.keys()):
                               
                run_id = states[state_future]
                state = state_future.result()
                
                if state == "FINISHED" or state == "UNKNOWN":
                    if run_id not in downloading_result_futures.values(): # result is not downloaded
                        future  = self._executor.submit(self._download_result, run_id)
                        downloading_result_futures[future] = run_id
                        future.add_done_callback(callback)
                        
                elif state == "ERROR":
                    self._unfinished_runs[run_id].set_exception(WebClientError("Execution failed."))
                    del self._unfinished_runs[run_id]
                    
                        
            end = time();
            duration = end - start
            if duration < 5 and not self._shutdown.is_set():
                self._shutdown.wait(5 - duration)
    
        state_request_executor.shutdown()
    
    def _is_finished(self, run_id):
        headers = {"Accept": "text/plain"}
        path = self._webclient.path + "runs/" + run_id + "/state"
    
        try:
            (state, _) = self._request("GET", path,"", headers)
    
            state = state.decode('utf-8')
            if state == "FINISHED":
                logging.debug('Run {0} finished.'.format(run_id))
    
            if state == "UNKNOWN":
                logging.debug('Run {0} is not known by the webclient, trying to get the result.'.format(run_id))
    
            return state
    
        except urllib2.HTTPError as e:
            logging.warning('Could not get run state {0}: {1}'.format(run_id, e.reason))
            return False
    
    def _download_result(self, run_id):
        # download result as zip file
        headers = {"Accept": "application/zip"}
        path = self._webclient.path + "runs/" + run_id + "/result"
        (zip_content, _) = self._request("GET", path, {}, headers)
        return zip_content
    
    def shutdown(self):
        """
        Cancels all unfinished runs and stops all internal threads.
        """
        self._shutdown.set()
        
        if len(self._unfinished_runs) > 0:
            logging.info("Stopping tasks on server...")
            stopTasks = set()
            with self._unfinished_runs_lock:
                for runId in self._unfinished_runs.keys():
                    stopTasks.add(self._executor.submit(self._stop_run, runId))
                    
            for task in stopTasks:
                task.result()
            self._executor.shutdown(wait=True)
            logging.info("Stopped all tasks.")
            
        else:
            self._executor.shutdown(wait=True)
        
        self._write_hash_code_cache()

    def _stop_run(self, run_id):
        path = self._webclient.path + "runs/" + run_id
        try:
            self._request("DELETE", path, expectedStatusCodes = [200,204])
            with self._unfinished_runs_lock:
                del self._unfinished_runs[run_id]
        except urllib2.HTTPError as e:
            logging.warn("Stopping of run {0} failed: {1}".format(run_id, e.reason))

    def _request(self, method, path, body={}, headers={}, expectedStatusCodes=[200], user_pwd=None):
        connection = self._get_connection()
    
        headers["Connection"] = "Keep-Alive"
    
        if user_pwd:
            base64_user_pwd = base64.b64encode(user_pwd.encode("utf-8")).decode("utf-8")
            headers["Authorization"] = "Basic " + base64_user_pwd
        elif self._base64_user_pwd:
            headers["Authorization"] = "Basic " + self._base64_user_pwd            
    
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
    
    def _get_connection(self):
        connection = getattr(self._thread_local, 'connection', None)
    
        if connection is None:
            if self._webclient.scheme == 'http':
                self._thread_local.connection = HTTPConnection(self._webclient.netloc, timeout=CONNECTION_TIMEOUT)
            elif self._webclient.scheme == 'https':
                self._thread_local.connection = HTTPSConnection(self._webclient.netloc, timeout=CONNECTION_TIMEOUT)
            else:
                raise WebClientError("Unknown protocol {0}.".format(self._webclient.scheme))
    
            connection = self._thread_local.connection
    
        return connection


def handle_result(zip_content, output_path, run_identifier):
    """
    Parses the given result: Extract meta information,
    print information, and write all files to the 'output_path'.
    @return: the return value of CPAchecker
    """

    # unzip and read result
    return_value = None
    try:
        try:
            with zipfile.ZipFile(io.BytesIO(zip_content)) as result_zip_file:
                return_value = _handle_result(result_zip_file, output_path)

        except zipfile.BadZipfile:
            logging.warning('Server returned illegal zip file with results of run {}.'.format(run_identifier))
            # Dump ZIP to disk for debugging
            with open(output_path + '.zip', 'wb') as zip_file:
                zip_file.write(zip_content)

    except IOError as e:
        logging.warning('Error while writing results of run {}: {}'.format(run_identifier, e))

    return return_value


def _handle_result(resultZipFile, output_path):
    """
    Extraxts all files from the given zip file, parses the meta information
    and writes all files to the 'output_path'.
    @return: the return value of CPAchecker.
    """
    files = set(resultZipFile.namelist())

    # extract run info
    if RESULT_FILE_RUN_INFO in files:
        with resultZipFile.open(RESULT_FILE_RUN_INFO) as runInformation:
            (_, _, return_value, values) = _parse_cloud_result_file(runInformation)
            print("Run Information:")
            for key in sorted(values.keys()):
                if not key.startswith("@"):
                    print ('\t' + str(key) + ": " + str(values[key]))

    else:
        return_value = None
        logging.warning('Missing log file.')

    # extract host info
    if RESULT_FILE_HOST_INFO in files:
        with resultZipFile.open(RESULT_FILE_HOST_INFO) as hostInformation:
            values = _parse_worker_host_information(hostInformation)
            print("Host Information:")
            for key, value in values.items():
                print ('\t' + str(key) + ": " + str(value))
    else:
        logging.warning('Missing host information.')

    # extract log file
    if RESULT_FILE_LOG in files:
        log_file_path = output_path + "output.log"
        with open(log_file_path, 'wb') as log_file:
            with resultZipFile.open(RESULT_FILE_LOG) as result_log_file:
                for line in result_log_file:
                    log_file.write(line)

        logging.info('Log file is written to ' + log_file_path + '.')

    else:
        logging.warning('Missing log file .')

    if RESULT_FILE_STDERR in files:
        resultZipFile.extract(RESULT_FILE_STDERR, output_path)

    resultZipFile.extractall(output_path, files)

    logging.info("Results are written to {0}".format(output_path))

    return return_value

def _parse_worker_host_information(file):
    """
    Parses the mete file containing information about the host executed the run.
    Returns a dict of all values.
    """
    values = _parse_file(file)

    values["host"] = values.pop("@vcloud-name", "-")
    values.pop("@vcloud-os", "-")
    values.pop("@vcloud-memory", "-")
    values.pop("@vcloud-cpuModel", "-")
    values.pop("@vcloud-frequency", "-")
    values.pop("@vcloud-cores", "-")
    return values

def _parse_cloud_result_file(file):
    """
    Parses the mete file containing information about the run.
    Returns a dict of all values.
    """
    values = _parse_file(file)

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

def _parse_file(file):
    """
    Parses a file containing key value pairs in each line.
    @return:  a dict of the parsed key value pairs.
    """
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