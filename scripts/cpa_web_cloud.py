#!/usr/bin/env python3

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

import argparse
import base64
import hashlib
import io
import logging
import os
import sys
import urllib
import urllib.request as request
import zlib
import zipfile


from  http.client import HTTPConnection
from  http.client import HTTPSConnection
from time import sleep
from time import time

sys.dont_write_bytecode = True # prevent creation of .pyc files

CONNECTION_TIMEOUT = 600 #seconds
DEFAULT_OUTPUT_PATH = "./"
RESULT_FILE_LOG = 'output.log'
RESULT_FILE_STDERR = 'stderr'
RESULT_FILE_RUN_INFO = 'runInformation.txt'
RESULT_FILE_HOST_INFO = 'hostInformation.txt'
RESULT_KEYS = ["cputime", "walltime", "returnvalue"]
SPECIAL_RESULT_FILES = {RESULT_FILE_LOG, RESULT_FILE_STDERR, RESULT_FILE_RUN_INFO,
                        RESULT_FILE_HOST_INFO, 'runDescription.txt'}

__webclient = None
__base64_user_pwd = None
__connection = None


class WebClientError(Exception):
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return repr(self.value)

def __create_argument_parser():
    """
    Create a parser for the command-line options.
    @return: an argparse.ArgumentParser instance
    """
    
    parser = argparse.ArgumentParser("Executes CPAchecker in the VerifierCloud and uses the web interface." \
                                      + "Command-line parameters can additionally be read from a file if file name prefixed with '@' is given as argument.",
                                    fromfile_prefix_chars='@',
                                    add_help=False) # conflict with -heap

    parser.add_argument("--cloudMaster",
                      dest="cloud_master",
                      default="http://vcloud.sosy-lab.org/webclient/",
                      metavar="HOST",
                      help="Sets the webclient host of the VerifierCloud instance to be used.")

    parser.add_argument("--cloudPriority",
                      dest="cloud_priority",
                      metavar="PRIORITY",
                      help="Sets the priority for this benchmark used in the VerifierCloud. Possible values are IDLE, LOW, HIGH, URGENT.")

    parser.add_argument("--cloudCPUModel",
                      dest="cpu_model", type=str, default=None,
                      metavar="CPU_MODEL",
                      help="Only execute runs in the VerifierCloud on CPU models that contain the given string.")

    parser.add_argument("--cloudUser",
                      dest="cloud_user",
                      metavar="USER:PWD",
                      help="The user and password for the VerifierCloud.")

    parser.add_argument("--revision",
                      dest="revision",
                      metavar="BRANCH:REVISION",
                      help="The svn revision of CPAchecker to use.")
     
    parser.add_argument("-d", "--debug",
                      action="store_true",
                      help="Enable debug output")

    parser.add_argument("-o", "--outputpath",
                      dest="output_path", type=str,
                      default=DEFAULT_OUTPUT_PATH,
                      help="Output prefix for the generated results. "
                            + "If the path is a folder files are put into it,"
                            + "otherwise it is used as a prefix for the resulting files.")
    parser.add_argument("--resultFilePattern",
                     dest="result_file_pattern", type=str,
                     default=None,
                     help="Only files matching this glob pattern are transported back to the client.")

    parser.add_argument("-T", "--timelimit",
                      dest="timelimit", default=None,
                      help="Time limit in seconds",
                      metavar="SECONDS")

    parser.add_argument("-M", "--memorylimit",
                      dest="memorylimit", default=None,
                      help="Memory limit in MB",
                      metavar="MB")

    parser.add_argument("-c", "--corelimit", dest="corelimit",
                      type=int, default=None,
                      metavar="N",
                      help="Limit the tool to N CPU cores.")

    return parser

def __setup_logging(config):
    """
    Configure the logging framework.
    """
    if config.debug:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                            level=logging.DEBUG)
    else:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                            level=logging.INFO)

def __init(config):
    """
    Sets __webclient and __base64_user_pwd if they are defined in the given config.
    """
    global __webclient, __base64_user_pwd

    if config.cloud_master:
        if not config.cloud_master[-1] == '/':
            config.cloud_master += '/'

        webclient = config.cloud_master
        __webclient = urllib.parse.urlparse(webclient)
        logging.info('Using webclient at {0}'.format(webclient))

    if config.cloud_user:
        __base64_user_pwd = base64.b64encode(config.cloud_user.encode("utf-8")).decode("utf-8")

def __request(method, path, body, headers, expectedStatusCodes=[200]):
    """
    Sends a request specified by the parameters.
    @raise urlib.request.HTTPError:  if the request fails more than 5 times
    @return: (response body, status code)
    """
    connection = __get_connection()

    headers["Connection"] = "Keep-Alive"

    if __base64_user_pwd:
        headers["Authorization"] = "Basic " + __base64_user_pwd

    counter = 0
    while (counter < 5):
        counter+=1
        # send request
        try:
            connection.request(method, path, body=body, headers=headers)
            response = connection.getresponse()
        except:
            if (counter < 5):
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
                message = 'Please check the user and password given to --cloudUser.\n'
            if response.status == 404:
                message = 'Please check the URL given to --cloudMaster.'
            message += response.read().decode('UTF-8')
            raise request.HTTPError(path, response.getcode(), message , response.getheaders(), None)

def __get_connection():
    """
    Creates and caches a HTTPConnection or HTTPSConnection. 
    A previously created connection object will be reused, otherwise a new connection Object is created.
    @requires: __webclient must be set (see __init(config)).
    @return: http.client.HTTPConnection object
    """
    global __connection

    if __connection is None:
        if __webclient.scheme == 'http':
            __connection = HTTPConnection(__webclient.netloc, timeout=CONNECTION_TIMEOUT)
        elif __webclient.scheme == 'https':
            __connection = HTTPSConnection(__webclient.netloc, timeout=CONNECTION_TIMEOUT)
        else:
            raise WebClientError("Unknown protocol {0}.".format(__webclient.scheme))

    return __connection

def __get_revision(config):
    """
    Extracts branch and revision number from the given config parameter.
    @return: (branch, revision number)
    """
    if config.revision:
        tokens = config.revision.split(':')
        svn_branch = tokens[0]
        if len(tokens) > 1:
            revision = config.revision.split(':')[1]
        else:
            revision = 'HEAD'
        return (svn_branch, revision)
    else:
        return ('trunk', 'HEAD')      

def __compute_sha1_hash(path):
    """
    Returns the SHA-1 hash of a file as string in hexadecimal format.
    """
    with open(path, 'rb') as file:
        return hashlib.sha1(file.read()).hexdigest()
    
def __parse_cpachecker_args(cpachecker_args):
    """
    Parses the given CPAchecker arguments.
    @return:  (list of invalid options, list of source files, dict of POST parameters).
    """
    params = {}
    options = []
    invalid_options = []
    source_files = []

    i = iter(cpachecker_args)
    while True:
        try:
            option=next(i)
            if option == "-heap":
                params['heap'] = next(i)

            elif option == "-noout":
                options.append("output.disable=true")
            elif option == "-stats":
                options.append("statistics.print=true")
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
                    logging.warning('Configuration {0} of is not from the default config directory.'.format(configPath))
                    invalid_options.append(option)
                    invalid_options.append(configPath)
                config  = next(i).split('/')[2].split('.')[0]
                params['configuration'] = config

            elif option == "-setprop":
                options.append(next(i))

            # example: -predicateAnalysis
            elif option[0] == '-' and 'configuration' not in params :
                params['configuration'] = option[1:]
                
            elif os.path.isfile(option):
                source_files.append(option)
                
            else:
                invalid_options.append(option)

        except StopIteration:
            break

    params['option'] = options
    return (invalid_options, source_files, params)
    
def __submit_run(config, cpachecker_args, counter=0):
    """
    Submits a single run using the web interface of the VerifierCloud.
    @return: the run's identifier
    """
    
    (invalid_options, source_files, params) = __parse_cpachecker_args(cpachecker_args)
    if len (invalid_options) > 0:
        raise WebClientError('Command {0} contains option that is not usable with the webclient: {1} '\
            .format(cpachecker_args, invalid_options))

    print(params)

    #revision
    (svn_branch, svn_revision) = __get_revision(config)
    params['svnBranch'] = svn_branch
    params['revision'] = svn_revision

    # limitations
    if config.memorylimit:
        params['memoryLimitation'] = config.memorylimit + "MB"
    if config.timelimit:
        params['timeLimitation'] = config.timelimit
    if config.corelimit:
        params['coreLimitation'] = config.corelimit
    if config.cpu_model:
        params['cpuModel'] = config.cpu_model

    if config.result_file_pattern:
        params['resultFilesPattern'] = config.result_file_pattern;
    if config.cloud_priority:
        params['priority'] = config.cloud_priority
    
    #source files
    source_file_Hashs = []
    for source_file in source_files:
        source_file_Hashs.append(__compute_sha1_hash(source_file))
    params['programTextHash'] = source_file_Hashs

    # prepare request
    headers = {"Content-Type": "application/x-www-form-urlencoded",
               "Content-Encoding": "deflate",
               "Accept": "text/plain"}

    paramsCompressed = zlib.compress(urllib.parse.urlencode(params, doseq=True).encode('utf-8'))
    path = __webclient.path + "runs/"

    (run_id, status_code) = __request("POST", path, paramsCompressed, headers, [200, 412])
    
    # source files given as hash value are not known by the cloud system
    if status_code == 412 and counter < 1: 
        headers = {"Content-Type": "application/octet-stream",
               "Content-Encoding": "deflate"}

        # upload all used source files
        filePath = __webclient.path + "files/"
        for source_path in source_files:
            with open(source_path, 'rb') as source_file:
                compressedProgramText = zlib.compress(source_file.read(), 9)
                __request('POST', filePath, compressedProgramText, headers, [200,204])
                
        # retry submission of run
        return __submit_run(config, cpachecker_args, counter + 1)
    
    else:
        decoded_run_id = run_id.decode()
        logging.debug("Submitted run {0}".format(decoded_run_id))
        return decoded_run_id

def __get_results(runID, config, cpachecker_args):
    """
    Waits until the run given by its runID id finished, downloads and parses the result.
    @return: the return value of CPAchecker or -1 if the execution failed
    """

    logging.info("Waiting for result of run {0}.".format(runID))

    while True:
        start = time()
        state = __get_state(runID)
        
        if state == "FINISHED" or state == "UNKOWN":
            result_value = __download_and_parse_result(runID, config, cpachecker_args)
            if not result_value is None:
                return result_value # download of result was successful
            
        elif state == "ERROR":
            logging.warn("Run execution failed.")
            return -1

        end = time();
        duration = end - start
        if duration < 5:
            sleep(5 - duration)

def __get_state(runID):
    """
    @return:  the current state of the run given by its runID
    """
    headers = {"Accept": "text/plain"}
    path = __webclient.path + "runs/" + runID + "/state"

    try:
        (state, _) = __request("GET", path,"", headers)

        state = state.decode('utf-8')
        if state == "FINISHED":
            logging.debug('Run {0} finished.'.format(runID))

        if state == "UNKNOWN":
            logging.debug('Run {0} is not known by the webclient, trying to get the result.'.format(runID))

        return state

    except request.HTTPError as e:
        logging.warning('Could not get run state {0}: {1}'.format(runID, e.reason))
        return "UNKNOWN"

def __download_and_parse_result(runID, config, cpachecker_args):
    """
    Downloads the result of the run given by its id, parses the meta information 
    and writes all files to the 'output_path' defined in the config parameter.
    @return: the return value of CPAchecker
    """
    # download result as zip file
    headers = {"Accept": "application/zip"}
    path = __webclient.path + "runs/" + runID + "/result"

    try:
        (zip_content, _) = __request("GET", path, {}, headers, expectedStatusCodes=[200])

    except request.HTTPError as e:
        logging.info('Could not get result of run {0}: {1}'.format(runID, e))
        return None

    # unzip and read result
    return_value = None
    try:
        try:
            with zipfile.ZipFile(io.BytesIO(zip_content)) as result_zip_file:
                return_value = __handle_result(result_zip_file, config, cpachecker_args)
        
        except zipfile.BadZipfile:
            logging.warning('Server returned illegal zip file with results of run {}.'.format(runID))
            # Dump ZIP to disk for debugging
            with open(config.output_path + '.zip', 'wb') as zip_file:
                zip_file.write(zip_content)
                
    except IOError as e:
        logging.warning('Error while writing results of run {}: {}'.format(runID, e))

    return return_value


def __handle_result(resultZipFile, config, cpachecker_args):
    """
    Extraxts all files from the given zip file, parses the meta information 
    and writes all files to the 'output_path' defined in the config parameter.
    @return: the return value of CPAchecker.
    """
    result_dir = config.output_path
    files = set(resultZipFile.namelist())

    # extract run info
    if RESULT_FILE_RUN_INFO in files:
        with resultZipFile.open(RESULT_FILE_RUN_INFO) as runInformation:
            (_, _, return_value, values) = __parse_cloud_result_file(runInformation)
            print("run information:")
            for key, value in values.items():
                print ('\t' + str(key) + ": " + str(value))
    else:
        return_value = None
        logging.warning('Missing log file.')
        
    # extract host info
    if RESULT_FILE_HOST_INFO in files:
        with resultZipFile.open(RESULT_FILE_HOST_INFO) as hostInformation:
            values = __parse_worker_host_information(hostInformation)
            print("host information:")
            for key, value in values.items():
                print ('\t' + str(key) + ": " + str(value))
    else:
        logging.warning('Missing host information.')

    # extract log file
    if RESULT_FILE_LOG in files:
        log_file_path = config.output_path + "output.log"
        with open(log_file_path, 'wb') as log_file:
            with resultZipFile.open(RESULT_FILE_LOG) as result_log_file:
                for line in result_log_file:
                    log_file.write(line)
        
        logging.info('Log file is written to ' + log_file_path + '.')
        
    else:
        logging.warning('Missing log file .')

    if RESULT_FILE_STDERR in files:
        resultZipFile.extract(RESULT_FILE_STDERR, result_dir)

    resultZipFile.extractall(result_dir, files)
    
    logging.info("Results are written to {0}".format(result_dir))

    return return_value

def __parse_worker_host_information(file):
    """
    Parses the mete file containing information about the host executed the run.
    Returns a dict of all values.
    """
    values = __parse_file(file)

    values["host"] = values.pop("@vcloud-name", "-")
    values.pop("@vcloud-os", "-")
    values.pop("@vcloud-memory", "-")
    values.pop("@vcloud-cpuModel", "-")
    values.pop("@vcloud-frequency", "-")
    values.pop("@vcloud-cores", "-")
    return values

def __parse_cloud_result_file(file):
    """
    Parses the mete file containing information about the run.
    Returns a dict of all values.
    """
    values = __parse_file(file)

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

def __parse_file(file):
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

def __execute():
    """
    Executes a single CPAchecker run in the VerifierCloud vis the web front end.
    All informations are given by the command line arguments.
    @return: the return value of CPAchecker
    """
    arg_parser = __create_argument_parser()
    (config, cpachecker_args) = arg_parser.parse_known_args()
    __setup_logging(config)
    __init(config)
    
    try:
        run_id = __submit_run(config, cpachecker_args)
        return __get_results(run_id, config, cpachecker_args)
    
    except request.HTTPError as e:
        logging.warn(e.reason)
    except WebClientError as e:
        logging.warn(str(e))
        

if __name__ == "__main__":
    sys.exit(__execute())
