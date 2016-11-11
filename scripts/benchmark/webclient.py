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
import platform
import random
import tempfile
import threading
import zipfile
import zlib

from time import sleep
from time import time

import requests
from requests import HTTPError
import urllib.parse
from concurrent.futures import ThreadPoolExecutor
from concurrent.futures import as_completed
from concurrent.futures import Future

try:
    import sseclient  # @UnresolvedImport
except:
    pass

"""
This module provides helpers for accessing the web interface of the VerifierCloud.
"""

__all__ = [
    'WebClientError', 'WebInterface', 'handle_result',
    'MEMLIMIT', 'TIMELIMIT', 'SOFTTIMELIMIT', 'CORELIMIT',
    'RESULT_FILE_LOG', 'RESULT_FILE_STDERR', 'RESULT_FILE_RUN_INFO', 'RESULT_FILE_HOST_INFO', 'RESULT_FILE_RUN_DESCRIPTION', 'SPECIAL_RESULT_FILES',
    ]

MEMLIMIT = 'memlimit'
TIMELIMIT = 'timelimit'
SOFTTIMELIMIT = 'softtimelimit'
CORELIMIT = 'cpuCores'

RESULT_FILE_LOG = 'output.log'
RESULT_FILE_STDERR = 'stderr'
RESULT_FILE_RUN_INFO = 'runInformation.txt'
RESULT_FILE_HOST_INFO = 'hostInformation.txt'
RESULT_FILE_RUN_DESCRIPTION = 'runDescription.txt'
SPECIAL_RESULT_FILES = {RESULT_FILE_LOG, RESULT_FILE_STDERR, RESULT_FILE_RUN_INFO,
                        RESULT_FILE_HOST_INFO, RESULT_FILE_RUN_DESCRIPTION}

MAX_SUBMISSION_THREADS = 5
CONNECTION_TIMEOUT = 600  # seconds
HASH_CODE_CACHE_PATH = os.path.join(os.path.expanduser("~"), ".verifiercloud/cache/hashCodeCache")

class WebClientError(Exception):
    def _init_(self, value):
        self.value = value
    def _str_(self):
        return repr(self.value)

class PollingResultDownloader:

    def __init__(self, web_interface, result_poll_interval, unfinished_runs={}):
        self._unfinished_runs = set()
        self._unfinished_runs_lock = threading.Lock()
        self._web_interface = web_interface
        self._result_poll_interval = result_poll_interval
        self._state_poll_executor = ThreadPoolExecutor(max_workers=web_interface.thread_count)
        self._state_poll_thread = threading.Thread(target=self._poll_run_states, name='web_interface_state_poll_thread')
        self._shutdown = threading.Event()

    def _poll_run_states(self):

        # in every iteration the states of all unfinished runs are requested once
        while not self._shutdown.is_set() :
            start = time()
            states = {}

            with self._web_interface._unfinished_runs_lock:
                for run_id in self._web_interface._unfinished_runs.keys():
                        state_future = self._state_poll_executor.submit(self._web_interface._is_finished, run_id)
                        states[state_future] = run_id

            # Collect states of runs
            for state_future in as_completed(states.keys()):

                run_id = states[state_future]
                state = state_future.result()

                if state == "FINISHED" or state == "UNKNOWN":
                    self._web_interface._download_result_async(run_id)

                elif state == "ERROR":
                    self._web_interface._run_failed(run_id)

            end = time();
            duration = end - start
            if duration < self._result_poll_interval and not self._shutdown.is_set():
                self._shutdown.wait(self._result_poll_interval - duration)

    def start(self):
        if (not self._shutdown.is_set()) and (not self._state_poll_thread.isAlive()):
            logging.info("Starting polling of run states.")
            self._state_poll_thread.start()

    def shutdown(self):
        self._shutdown.set()
        if self._state_poll_thread.is_alive():
            self._state_poll_thread.join()
        self._state_poll_executor.shutdown(wait=True)

try:
    class ShouldReconnectSeeClient(sseclient.SSEClient):

        def __init__(self, url, should_reconnect, last_id=None, retry=3000, session=None, **kwargs):
            super().__init__(url, last_id, retry, session, **kwargs)
            self._should_reconnect = should_reconnect

        def _connect(self):
            (_, value, _) = sys.exc_info()
            if (value is None or self._should_reconnect(value)):
                super()._connect()

            else:
                raise StopIteration()

    class SseResultDownloader:

        def __init__(self, web_interface, result_poll_interval):
            logging.debug("Server-Send Events are used to get state of runs.")

            self._web_interface = web_interface
            self._run_finished_url = web_interface._web_interface_url + "runs/finished"
            self._result_poll_interval = result_poll_interval
            self._sse_client = None
            self._shutdown = False
            self._new_runs = False
            self._state_receive_executor = ThreadPoolExecutor(max_workers=1)

        def _log_future_exception_and_fallback(self, result):
            e = result.exception()
            if e is not None:
                if (self._shutdown and
                        isinstance(e, AttributeError) and
                        str(e) == "'NoneType' object has no attribute 'read'"):
                    # This is harmless, it occurs because SSEClient reads on closed connection.
                    logging.debug('Error during result processing:', exc_info=True)
                else:
                    logging.warning('Error during result processing:', exc_info=True)

                if not self._shutdown:
                    self._fall_back()

        def _should_reconnect(self, error):
            if self._new_runs:
                return False
            elif type(error) == HTTPError and error.response is not None \
                    and error.response.status >= 400 and error.response.status < 500 :
                logging.debug("Exception in SSE connection: %s", error)
                return False
            else:

                return True

        def _start_sse_connection(self):

            while(self._new_runs):
                run_ids = set(self._web_interface._unfinished_runs.keys())
                self._new_runs = False

                # nothing to do
                if len(run_ids) == 0:
                    return

                params = []
                for run_id in run_ids:
                    params.append(("run", run_id))

                headers = {"Accept-Encoding": "UTF-8"}

                logging.debug("Creating Server-Send Event connection.")
                try:
                    self._sse_client = ShouldReconnectSeeClient(
                        self._run_finished_url, self._should_reconnect,
                        session = self._web_interface._connection,
                        headers=headers,
                        data=params)

                except Exception as e:
                    logging.warning("Creating SSE connection failed: %s", e)
                    self._fall_back()
                    return

                for message in self._sse_client:
                    data = message.data
                    tokens = data.split(" ")
                    if len(tokens) == 2:
                        run_id = tokens[0]
                        state = tokens[1]

                        if state == "FINISHED":
                            if run_id in run_ids:
                                logging.debug('Run %s finished.', run_id)
                                self._web_interface._download_result_async(run_id)


                        elif state == "UNKNOWN":
                            logging.debug('Run %s is not known by the webclient, trying to get the result.', run_id)
                            self._web_interface._download_async(run_id)

                        elif state == "ERROR":
                            self._web_interface._run_failed(run_id)

                        else:
                            logging.warning('Received unknown run state %s for run %s.', state, run_id)

                        run_ids.discard(run_id)
                        if self._shutdown or self._new_runs or len(run_ids) == 0:
                            break;

                    else:
                        logging.warning("Received invalid message %s", data)

            self._sse_client = None

            # fall back to polling if Server-Send Event based approach failed
            if len(run_ids) != 0 and not self._shutdown:
                self._fall_back()

        def _fall_back(self):
            logging.info("Fall back to polling.")
            self._web_interface._result_downloader = PollingResultDownloader(self._web_interface, self._result_poll_interval)
            self._web_interface._result_downloader.start()
            self.shutdown(wait=False)

        def start(self):
            self._new_runs = True
            if self._sse_client:
                self._sse_client.resp.close()
            else:
                future = self._state_receive_executor.submit(self._start_sse_connection)
                future.add_done_callback(self._log_future_exception_and_fallback)

        def shutdown(self, wait=True):
            self._shutdown = True
            if self._sse_client:
                self._sse_client.resp.close()
            self._state_receive_executor.shutdown(wait=wait)

except:
    pass

class RunResultFuture(Future):

    def __init__(self, web_interface, run_id):
        super().__init__()
        self._web_interface = web_interface
        self._run_id = run_id

    def cancel(self):
        canceled = super().cancel()
        if canceled:
            try:
                self._web_interface._stop_run(self._run_id)
            except:
                logging.warning("Stopping of run %s failed", self._run_id)

        return canceled

class WebInterface:
    """
    The WebInterface is a executor like class for the submission of runs to the VerifierCloud
    """

    def __init__(self, web_interface_url, user_pwd, svn_branch='trunk', svn_revision='HEAD',
                 thread_count=1, result_poll_interval=2, user_agent=None, version=None):
        """
        Creates a new WebInterface object.
        The given svn revision is resolved (e.g. 'HEAD' -> 17495).
        @param web_interface_url: the base URL of the VerifierCloud's web interface
        @param user_pwd: user name and password in the format '<user_name>:<password>' or none if no authentification is required
        @param svn_branch: the svn branch name or 'trunk', defaults to 'trunk'
        @param svn_revision: the svn revision number or 'HEAD', defaults to 'HEAD'
        @param thread_count: the number of threads for fetching results in parallel
        @param result_poll_interval: the number of seconds to wait between polling results
        """
        if not (1 <= thread_count <= MAX_SUBMISSION_THREADS):
            sys.exit("Invalid number {} of client threads, needs to be between 1 and {}.".format(thread_count, MAX_SUBMISSION_THREADS))
        if not 1 <= result_poll_interval:
            sys.exit("Poll interval {} is too small, needs to be at least 1s.".format(result_poll_interval))
        if not web_interface_url[-1] == '/':
            web_interface_url += '/'

        default_headers = {'Connection': 'Keep-Alive'}
        if user_agent:
            default_headers['User-Agent'] = \
                '{}/{} (Python/{} {}/{})'.format(user_agent, version, platform.python_version(), platform.system(), platform.release())

        urllib.parse.urlparse(web_interface_url) # sanity check
        self._web_interface_url = web_interface_url
        logging.info('Using VerifierCloud at %s', web_interface_url)

        self._connection = requests.Session()
        self._connection.headers.update(default_headers)
        self._connection.verify='/etc/ssl/certs'
        if user_pwd:
            self._connection.auth = (user_pwd.split(":")[0], user_pwd.split(":")[1])
            self._base64_user_pwd = base64.b64encode(user_pwd.encode("utf-8")).decode("utf-8")
        else:
            self._base64_user_pwd = None

        self._unfinished_runs = {}
        self._unfinished_runs_lock = threading.Lock()
        self._downloading_result_futures = {}
        self._download_attempts = {}
        self.thread_count = thread_count
        self._executor = ThreadPoolExecutor(thread_count)
        self._thread_local = threading.local()
        self._hash_code_cache = {}
        self._group_id = str(random.randint(0, 1000000))
        self._read_hash_code_cache()
        self._resolved_tool_revision(svn_branch, svn_revision)
        self._tool_name = self._request_tool_name()

        try:
            self._result_downloader = SseResultDownloader(self, result_poll_interval)
        except:
            self._result_downloader = PollingResultDownloader(self, result_poll_interval)

    def _read_hash_code_cache(self):
        if not os.path.isfile(HASH_CODE_CACHE_PATH):
            return

        with open(HASH_CODE_CACHE_PATH, mode='r') as hashCodeCacheFile:
            for line in hashCodeCacheFile:
                tokens = line.strip().split('\t')
                if len(tokens) == 3:
                    self._hash_code_cache[(tokens[0], tokens[1])] = tokens[2]

    def _write_hash_code_cache(self):
        directory = os.path.dirname(HASH_CODE_CACHE_PATH)
        try:
            os.makedirs(directory, exist_ok=True)
            with tempfile.NamedTemporaryFile(dir=directory, delete=False) as tmpFile:
                for (path, mTime), hashValue in self._hash_code_cache.items():
                    line = (path + '\t' + mTime + '\t' + hashValue + '\n').encode()
                    tmpFile.write(line)

                os.renames(tmpFile.name, HASH_CODE_CACHE_PATH)
        except OSError as e:
            logging.warning("Could not write hash-code cache file to %s: %s", HASH_CODE_CACHE_PATH, e.strerror)

    def _resolved_tool_revision(self, svn_branch, svn_revision):

        path = "tool/version?svnBranch=" + svn_branch + "&revision=" + svn_revision

        (resolved_svn_revision, _) = self._request("GET", path)
        self._svn_branch = svn_branch
        self._svn_revision = resolved_svn_revision.decode("UTF-8")

    def _request_tool_name(self):
        path = "tool/name"
        (tool_name, _) = self._request("GET", path)
        return tool_name.decode("UTF-8")

    def tool_revision(self):
        return self._svn_branch + ':' + self._svn_revision

    def tool_name(self):
        return self._tool_name

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

    def _create_and_add_run_future(self, run_id):
        result = RunResultFuture(self, run_id)
        with self._unfinished_runs_lock:
            self._unfinished_runs[run_id] = result
        return result

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

        paramsCompressed = zlib.compress(urllib.parse.urlencode(params, doseq=True).encode('utf-8'))
        path = "runs/witness_validation/"

        (run_id, _) = self._request("POST", path, paramsCompressed, headers, user_pwd=user_pwd)

        run_id = run_id.decode("UTF-8")
        logging.debug('Submitted witness validation run with id %s', run_id)

        return self._create_and_add_run_future(run_id)

    def submit(self, run, limits, cpu_model, result_files_pattern=None, meta_information=None, \
               priority='IDLE', user_pwd=None, svn_branch=None, svn_revision=None,
               result_files_patterns=[], required_files=[]):
        """
        Submits a single run to the VerifierCloud.
        @note: flush() should be called after the submission of the last run.
        @param run: The input for the run:  command line options (run.options),
                                            source files (run.sourcefiles),
                                            property file (run.propertyfile),
                                            identifier for error messages (run.identifier)
        @param limits: dict of limitations for the run (memlimit, timelimit, corelimit, softtimelimit)
        @param cpu_model: substring of CPU model to use or 'None' for no restriction
        @param result_files_pattern: the result is filtered with the given glob pattern, '**' is no restriction and None or the empty string do not match any file.
        @param meta_information: meta information about the submitted run as JSON string
        @param priority: the priority of the submitted run, defaults to 'IDLE'
        @param user_pwd: overrides the user name and password given in the constructor (optional)
        @param svn_branch: overrids the svn branch given in the constructor (optional)
        @param svn_revision: overrides the svn revision given in the constructor (optional)
        @param result_files_patterns: list of result_files_pattern (optional)
        @param required_files: list of additional file required to execute the run (optional)
        @raise WebClientError: if the HTTP request could not be created
        @raise HTTPError: if the HTTP request was not successful
        """
        if result_files_pattern:
            if result_files_patterns:
                raise ValueError("Cannot specify result_files_pattern and result_files_patterns "
                                 "at the same time.")
            result_files_patterns = [result_files_pattern]

        return self._submit(run, limits, cpu_model, required_files, result_files_patterns,
                            meta_information, priority, user_pwd, svn_branch, svn_revision)

    def _submit(self, run, limits, cpu_model, required_files, result_files_patterns,
                    meta_information, priority, user_pwd, svn_branch, svn_revision, counter=0):

        params = []
        opened_files = [] # open file handles are passed to the request library

        for programPath in run.sourcefiles:
            norm_path = self._normalize_path_for_cloud(programPath)
            params.append(('programTextHash', (norm_path, self._get_sha1_hash(programPath))))
  
        for required_file in required_files:
            norm_path = self._normalize_path_for_cloud(required_file)
            params.append(('requiredFileHash', (norm_path, self._get_sha1_hash(required_file))))

        params.append(('svnBranch', svn_branch or self._svn_branch))
        params.append(('revision', svn_revision or self._svn_revision))

        if run.propertyfile:
            file = self._add_file_to_params(params, 'propertyText', run.propertyfile)
            opened_files.append(file)

        if MEMLIMIT in limits:
            params.append(('memoryLimitation', str(limits[MEMLIMIT])))
        if TIMELIMIT in limits:
            params.append(('timeLimitation', str(limits[TIMELIMIT])))
        if SOFTTIMELIMIT in limits:
            params.append(('softTimeLimitation', str(limits[SOFTTIMELIMIT])))
        if CORELIMIT in limits:
            params.append(('coreLimitation', str(limits[CORELIMIT])))
        if cpu_model:
            params.append(('cpuModel', cpu_model))

        if result_files_patterns:
            for pattern in result_files_patterns:
                params.append(('resultFilesPattern', pattern))
        else:
            params.append(('resultFilesPattern', ''))

        if priority:
            params.append(('priority', priority))

        (invalidOption, files) = self._handle_options(run, params, limits)
        opened_files.extend(files)
        if invalidOption:
            raise WebClientError('Command {0}  contains option "{1}" that is not usable with the webclient. '\
                .format(run.options, invalidOption))

        params.append(('groupId', str(self._group_id)))
        if meta_information:
            params.append(('metaInformation', meta_information))

        # prepare request
        headers = {"Accept": "text/plain"}
        path = "runs/"
        (run_id, statusCode) = self._request("POST", path, files=params, headers=headers, \
                                             expectedStatusCodes=[200, 412], user_pwd=user_pwd)

        for opened_file in opened_files:
            opened_file.close()

        # program files or required files given as hash value are not known by the cloud system
        if statusCode == 412 and counter < 1:
            headers = {"Content-Type": "application/octet-stream",
                   "Content-Encoding": "deflate"}
            filePath = "files/"

            # upload all used program files
            for programPath in run.sourcefiles:
                with open(programPath, 'rb') as programFile:
                    compressedProgramText = zlib.compress(programFile.read(), 9)
                    self._request('POST', filePath, data=compressedProgramText, headers=headers,\
                                   expectedStatusCodes=[200, 204], user_pwd=user_pwd)

            # upload all required files
            for required_file_path in required_files:
                with open(required_file_path, 'rb') as required_file:
                    compressed_required_file = zlib.compress(required_file.read(), 9)
                    self._request('POST', filePath, data=compressed_required_file, headers=headers,\
                                   expectedStatusCodes=[200, 204], user_pwd=user_pwd)

            # retry submission of run
            return self._submit(run, limits, cpu_model, required_files, result_files_patterns, meta_information,
                                priority, user_pwd, svn_branch, svn_revision, counter + 1)

        else:
            run_id = run_id.decode("UTF-8")
            logging.debug('Submitted run with id %s', run_id)
            return self._create_and_add_run_future(run_id)

    def _handle_options(self, run, params, rlimits):
        opened_files = []
        config = None

        # TODO use code from CPAchecker module, it add -stats and sets -timelimit,
        # instead of doing it here manually, too
        if self._tool_name == "CPAchecker":
            params.append(("option", "statistics.print=true"))

            if 'softtimelimit' in rlimits:
                params.append(("option", "limits.time.cpu=" + str(rlimits['softtimelimit']) + "s"))

        if run.options:
            i = iter(run.options)
            while True:
                try:
                    option = next(i)
                    if len(option) == 0:
                        continue

                    if option == "-heap":
                        params.append(('heap', next(i)))
                    elif option == "-stack":
                        params.append(('stack', next(i)))

                    elif option == "-noout":
                        params.append(("option", "output.disable=true"))
                    elif option == "-outputpath":
                        params.append(("option", "output.path=" + next(i)))
                    elif option == "-logfile":
                        params.append(("option", "log.file=" + next(i)))
                    elif option == "-nolog":
                        params.append(("option", "log.level=OFF"))
                        params.append(("option", "log.consoleLevel=OFF"))
                    elif option == "-stats":
                        # ignore, is always set by this script
                        pass
                    elif option == "-disable-java-assertions":
                        params.append(('disableJavaAssertions', 'true'))
                    elif option == "-java":
                        params.append(("option", "language=JAVA"))
                    elif option == "-32":
                        params.append(("option", "analysis.machineModel=Linux32"))
                    elif option == "-64":
                        params.append(("option", "analysis.machineModel=Linux64"))
                    elif option == "-entryfunction":
                        params.append(("option", "analysis.entryFunction=" + next(i)))
                    elif option == "-timelimit":
                        params.append(("option", "limits.time.cpu=" + next(i)))
                    elif option == "-skipRecursion":
                        params.append(("option", "cpa.callstack.skipRecursion=true"))
                        params.append(("option", "analysis.summaryEdges=true"))
                    elif option == "-cbmc":
                        params.append(("option", "analysis.checkCounterexamples=true"))
                        params.append(("option", "counterexample.checker=CBMC"))
                    elif option == "-preprocess":
                        params.append(("option", "parser.usePreprocessor=true"))
                    elif option == "-generateReport":
                        params.append(('generateReport', 'true'))

                    elif option == "-spec":
                        spec_path = next(i)
                        file = self._add_file_to_params(params, "specificationText", spec_path)
                        opened_files.append(file)

                    elif option == "-config":
                        configPath = next(i)
                        tokens = configPath.split('/')
                        if (tokens[0] == "config" and len(tokens) == 2):
                            config = tokens[1].split('.')[0]
                            params.append(('configuration', config))
                        else:
                            params.append(("option", "configuration.file=" + configPath))

                    elif option == "-setprop":
                        params.append(("option", next(i)))

                    elif option[0] == '-':
                        if config:
                            raise WebClientError("More than one configuration: '{}' and '{}'".format(config, option[1:]))
                        else:
                            params.append(('configuration', option[1:]))
                            config = option[1:]
                    else:
                        return (option, opened_files)

                except StopIteration:
                    break

        return (None, opened_files)

    def _add_file_to_params(self, params, name, path):
        norm_path = self._normalize_path_for_cloud(path)
        file = open(path, 'rb')
        params.append((name, (norm_path, file)))
        return file

    def _normalize_path_for_cloud(self, path):
        norm_path = os.path.normpath(path)
        if '..' in norm_path or os.path.isabs(norm_path):
            norm_path = os.path.basename(norm_path)
        return norm_path

    def flush_runs(self):
        """
        Starts the execution of all previous submitted runs in the VerifierCloud.
        The web interface groups runs and submits them to the VerifierCloud only from time to time.
        This method forces the web interface to do this immediately and starts downloading of results.
        @return: the ids of the RunCollections created since the last flush request
        """
        headers = {"Content-Type": "application/x-www-form-urlencoded",
                   "Connection": "Keep-Alive"}

        params = {"groupId": self._group_id}
        path = "runs/flush"
        (response, _) = self._request("POST", path, data=params, headers=headers, expectedStatusCodes=[200, 204])
        run_collections = response.decode('utf-8').split("\n")
        if len(run_collections) == 0:
            logging.warning('No runs were submitted to the VerifierCloud before or a rate limit is hit.')
        else:
            logging.info('Submitted %s run collection: %s', len(run_collections), ",".join(run_collections))
            self._result_downloader.start()

        return run_collections

    def _is_finished(self, run_id):
        headers = {"Accept": "text/plain"}
        path = "runs/" + run_id + "/state"

        try:
            (state, _) = self._request("GET", path, headers=headers)

            state = state.decode('utf-8')
            if state == "FINISHED":
                logging.debug('Run %s finished.', run_id)

            if state == "UNKNOWN":
                logging.debug('Run %s is not known by the webclient, trying to get the result.', run_id)

            return state

        except requests.HTTPError as e:
            logging.warning('Could not get run state %s: %s', run_id, e.response)
            return False

    def _download_result(self, run_id):
        # download result as zip file
        headers = {"Accept": "application/zip"}
        path = "runs/" + run_id + "/result"
        (zip_content, _) = self._request("GET", path, headers=headers)
        return zip_content

    def _download_result_async(self, run_id):

        def callback(downloaded_result):
            run_id = self._downloading_result_futures.pop(downloaded_result)
            exception = downloaded_result.exception()

            if not exception:
                with self._unfinished_runs_lock:
                    result_future = self._unfinished_runs.pop(run_id, None)
                if result_future:
                    result_future.set_result(downloaded_result.result())

            else:
                logging.info('Could not get result of run %s: %s', run_id, downloaded_result.exception())

                # client error
                if type(exception) is HTTPError and exception.response and  \
                    400 <= exception.response.status_code and exception.response.status_code <= 499:

                    attempts = self._download_attempts.pop(run_id, 1);
                    if attempts < 10:
                        self._download_attempts[run_id] = attempts + 1;
                        self._download_result_async(run_id)
                    else:
                        self._run_failed(run_id)

                else:
                    # retry it
                    self._download_result_async(run_id)

        if run_id not in self._downloading_result_futures.values():  # result is not downloaded
            future = self._executor.submit(self._download_result, run_id)
            self._downloading_result_futures[future] = run_id
            future.add_done_callback(callback)

    def _run_failed(self, run_id):
        run_result_future = self._unfinished_runs.pop(run_id, None)
        if run_result_future:
            logging.warning('Execution of run %s failed.', run_id)
            run_result_future.set_exception(WebClientError("Execution failed."))

    def shutdown(self):
        """
        Cancels all unfinished runs and stops all internal threads.
        """
        self._result_downloader.shutdown()

        if len(self._unfinished_runs) > 0:
            logging.info("Stopping tasks on server...")
            stop_executor = ThreadPoolExecutor(max_workers=5 * self.thread_count)
            stop_tasks = set()
            with self._unfinished_runs_lock:
                for runId in self._unfinished_runs.keys():
                    stop_tasks.add(stop_executor.submit(self._stop_run, runId))
                    self._unfinished_runs[runId].set_exception(WebClientError("WebInterface was stopped."))
                self._unfinished_runs.clear()

            for task in stop_tasks:
                task.result()
            stop_executor.shutdown(wait=True)
            logging.info("Stopped all tasks.")

        self._write_hash_code_cache()
        self._executor.shutdown(wait=True)
        self._connection.close()

    def _stop_run(self, run_id):
        with self._unfinished_runs_lock:
            self._unfinished_runs.pop(run_id, None)

        path = "runs/" + run_id
        try:
            self._request("DELETE", path, expectedStatusCodes=[200, 204, 404])
        except HTTPError as e:
            logging.info("Stopping of run %s failed: %s", run_id, e.reason)


    def _request(self, method, path, data=None, headers=None, files=None, expectedStatusCodes=[200], user_pwd=None):
        url = self._web_interface_url + path
        if user_pwd:
            auth = (user_pwd.split(":")[0], user_pwd.split(":")[1])
        else:
            auth = None

        counter = 0
        while (counter < 5):
            counter += 1
            # send request
            try:
                response = self._connection.request(method, url, data=data, files=files, headers=headers, auth=auth)

            except Exception as e:
                if (counter < 5):
                    logging.debug("Exception during %s request to %s: %s", method, path, e)
                    sleep(1)
                    continue
                else:
                    raise

            if response.status_code in expectedStatusCodes:
                return (response.content, response.status_code)

            else:
                message = ""
                if response.status_code == 401:
                    message = 'Error 401: Permission denied. Please check the URL given to --cloudMaster and specify credentials if necessary.'

                elif response.status_code == 404:
                    message = 'Error 404: Not found. Please check the URL given to --cloudMaster.'

                elif response.status_code == 503:
                    message = 'Error 503: Service Unavailable.'
                    if counter < 5:
                        logging.debug(message)
                        sleep(60)
                        continue

                else:
                    message += response.content.decode('UTF-8')

                logging.warning(message)
                raise requests.HTTPError(path, message, response=response)

def _open_output_log(output_path):
    log_file_path = output_path + "output.log"
    logging.info('Log file is written to ' + log_file_path + '.')
    return open(log_file_path, 'wb')

def _handle_run_info(values):
    values["memUsage"] = int(values.pop("memory").strip('B'))

    # remove irrelevant columns
    values.pop("command", None)
    values.pop("returnvalue", None)
    values.pop("timeLimit", None)
    values.pop("coreLimit", None)
    values.pop("memoryLimit", None)
    values.pop("outerwalltime", None)
    values.pop("cpuCores", None)
    values.pop("cpuCoresDetails", None)
    values.pop("memoryNodes", None)
    values.pop("memoryNodesAllocation", None)

    print("Run Information:")
    for key in sorted(values.keys()):
        if not key.startswith("@"):
            print ('\t' + str(key) + ": " + str(values[key]))

    return int(values["exitcode"])

def _handle_host_info(values):
    print("Host Information:")
    for key in sorted(values.keys()):
        print ('\t' + str(key) + ": " + str(values[key]))

def _handle_special_files(result_zip_file, files, output_path):
    logging.info("Results are written to %s", output_path)
    for file in SPECIAL_RESULT_FILES:
        if file in files and file != RESULT_FILE_LOG:
            result_zip_file.extract(file, output_path)


def handle_result(zip_content, output_path, run_identifier, result_files_pattern=None,
                  open_output_log=_open_output_log,
                  handle_run_info=_handle_run_info,
                  handle_host_info=_handle_host_info,
                  handle_special_files=_handle_special_files,
                  result_files_patterns=['*'],
                  ):
    """
    Parses the given result ZIP archive: Extract meta information
    and pass it on to the given handler functions.
    The default handler functions print some relevant info and write it all to 'output_path'.
    @return: the return value of CPAchecker
    """
    if result_files_pattern:
        if result_files_patterns:
            raise ValueError("Cannot specify result_files_pattern and result_files_patterns "
                             "at the same time.")
        result_files_patterns = [result_files_pattern]

    # unzip and read result
    return_value = None
    try:
        try:
            with zipfile.ZipFile(io.BytesIO(zip_content)) as result_zip_file:
                return_value = _handle_result(result_zip_file, output_path,
                    open_output_log, handle_run_info, handle_host_info, handle_special_files,
                    result_files_patterns, run_identifier)

        except zipfile.BadZipfile:
            logging.warning('Server returned illegal zip file with results of run %s.', run_identifier)
            # Dump ZIP to disk for debugging
            with open(output_path + '.zip', 'wb') as zip_file:
                zip_file.write(zip_content)

    except IOError as e:
        logging.warning('Error while writing results of run %s: %s', run_identifier, e)

    return return_value


def _handle_result(resultZipFile, output_path,
                   open_output_log, handle_run_info, handle_host_info, handle_special_files,
                   result_files_patterns, run_identifier):

    files = set(resultZipFile.namelist())

    # extract run info
    if RESULT_FILE_RUN_INFO in files:
        with resultZipFile.open(RESULT_FILE_RUN_INFO) as runInformation:
            return_value = handle_run_info(_parse_cloud_file(runInformation))
    else:
        return_value = None
        logging.warning('Missing result for run %s.', run_identifier)

    # extract host info
    if RESULT_FILE_HOST_INFO in files:
        with resultZipFile.open(RESULT_FILE_HOST_INFO) as hostInformation:
            handle_host_info(_parse_cloud_file(hostInformation))
    else:
        logging.warning('Missing host information for run %s.', run_identifier)

    # extract log file
    if RESULT_FILE_LOG in files:
        with open_output_log(output_path) as log_file:
            with resultZipFile.open(RESULT_FILE_LOG) as result_log_file:
                for line in result_log_file:
                    log_file.write(line)
    else:
        logging.warning('Missing log file for run %s.', run_identifier)

    handle_special_files(resultZipFile, files, output_path)

    # extract result files:
    if result_files_patterns:
        result_files = set()
        for pattern in result_files_patterns:
            result_files.update(fnmatch.filter(files, pattern))
        result_files = result_files - SPECIAL_RESULT_FILES
        if result_files:
            resultZipFile.extractall(output_path, result_files)

    return return_value

def _parse_cloud_file(file):
    """
    Parses a file containing key value pairs in each line.
    @return:  a dict of the parsed key value pairs.
    """
    values = {}

    for line in file:
        (key, value) = line.decode('utf-8').split("=", 1)
        value = value.strip()
        values[key] = value

    return values
