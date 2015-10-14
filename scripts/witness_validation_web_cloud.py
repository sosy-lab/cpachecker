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
import io
import logging
import sys
import urllib.request as request
import zipfile

from benchmark.webclient import WebInterface, WebClientError

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

_webclient = None


def _create_argument_parser():
    """
    Create a parser for the command-line options.
    @return: an argparse.ArgumentParser instance
    """
    
    parser = argparse.ArgumentParser("Executes a witness validation task in the VerifierCloud and uses the web interface." \
                                      + "Command-line parameters can additionally be read from a file if file name prefixed with '@' is given as argument.",
                                    fromfile_prefix_chars='@')

    parser.add_argument("--cloudMaster",
                      dest="cloud_master",
                      default="http://vcloud.sosy-lab.org/webclient/",
                      metavar="HOST",
                      help="Sets the webclient host of the VerifierCloud instance to be used.")

    parser.add_argument("--cloudUser",
                      dest="cloud_user",
                      metavar="USER:PWD",
                      help="The user and password for the VerifierCloud.")
    
    parser.add_argument("--programFile",
                      dest="program_file",
                      metavar="FILE",
                      help="The path to the program file.",
                      required=True)
     
    parser.add_argument("--witnessFile",
                      dest="witness_file",
                      metavar="FILE",
                      help="The path to the witness file.",
                      required=True)
    
    parser.add_argument("--configuration",
                      dest="configuration",
                      metavar="CONFIG",
                      help="The configuration used for the validation.")

    parser.add_argument("-d", "--debug",
                      action="store_true",
                      help="Enable debug output")

    parser.add_argument("-o", "--outputpath",
                      dest="output_path", type=str,
                      default=DEFAULT_OUTPUT_PATH,
                      help="Output prefix for the generated results. "
                            + "If the path is a folder files are put into it,"
                            + "otherwise it is used as a prefix for the resulting files.")

    return parser

def _setup_logging(config):
    """
    Configure the logging framework.
    """
    if config.debug:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                            level=logging.DEBUG)
    else:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                            level=logging.INFO)

def _init(config):
    """
    Sets _webclient if it is defined in the given config.
    """
    global _webclient
    
    if not config.cloud_master:
        logging.warning("No URL of a VerifierCloud instance is given.")
        return
        
    _webclient = WebInterface(config.cloud_master, config.cloud_user)   
    
def _submit_run(config):
    """
    Submits a single run using the web interface of the VerifierCloud.
    @return: the run's result
    """            
    run_result_future = _webclient.submit_witness_validation(\
          config.witness_file, config.program_file, config.configuration, config.cloud_user)
    _webclient.flush_runs()
    return run_result_future.result()
    
def _parse_result(zip_content, config):
    """
    Parses the given result: Extract meta information 
    and write all files to the 'output_path' defined in the config parameter.
    @return: the return value of CPAchecker
    """

    # unzip and read result
    return_value = None
    try:
        try:
            with zipfile.ZipFile(io.BytesIO(zip_content)) as result_zip_file:
                return_value = _handle_result(result_zip_file, config)
        
        except zipfile.BadZipfile:
            logging.warning('Server returned illegal zip file with results of run.')
            # Dump ZIP to disk for debugging
            with open(config.output_path + '.zip', 'wb') as zip_file:
                zip_file.write(zip_content)
                
    except IOError as e:
        logging.warning('Error while writing results of run: {}'.format(e))

    return return_value


def _handle_result(resultZipFile, config):
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
            (_, _, return_value, values) = _parse_cloud_result_file(runInformation)
            print("run information:")
            for key, value in values.items():
                print ('\t' + str(key) + ": " + str(value))
    else:
        return_value = None
        logging.warning('Missing log file.')
        
    # extract host info
    if RESULT_FILE_HOST_INFO in files:
        with resultZipFile.open(RESULT_FILE_HOST_INFO) as hostInformation:
            values = _parse_worker_host_information(hostInformation)
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

def _execute():
    """
    Executes a single CPAchecker run in the VerifierCloud via the web front end.
    All informations are given by the command line arguments.
    @return: the return value of CPAchecker
    """
    arg_parser = _create_argument_parser()
    config = arg_parser.parse_args()
    _setup_logging(config)
    _init(config)
    
    try:
        run_result = _submit_run(config)
        return _parse_result(run_result, config)
    
    except request.HTTPError as e:
        logging.warn(e.reason)
    except WebClientError as e:
        logging.warn(str(e))
    
    finally:
        _webclient.shutdown()
        

if __name__ == "__main__":
    sys.exit(_execute())
