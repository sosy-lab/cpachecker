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
    
    if not config.cpu_model:
        logging.warning("It is strongly recommended to set a CPU model('--cloudCPUModel'). "\
                        "Otherwise the used machines and CPU models are undefined.")

    if not config.cloud_master:
        logging.warning("No URL of a VerifierCloud instance is given.")
        return
        
    (svn_branch, svn_revision) = _get_revision(config)
        
    _webclient = WebInterface(config.cloud_master, config.cloud_user, svn_branch, svn_revision)

    logging.info('Using tool version {0}.'.format(_webclient.tool_revision()))

def _get_revision(config):
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
   
    
def _submit_run(config, cpachecker_args, counter=0):
    """
    Submits a single run using the web interface of the VerifierCloud.
    @return: the run's result
    """    
    limits = {}
    if config.memorylimit:
        limits['memlimit'] = config.memorylimit + "MB"
    if config.timelimit:
        limits['timelimit'] = config.timelimit
    if config.corelimit:
        limits['corelimit'] = config.corelimit
    
    run = _parse_cpachecker_args(cpachecker_args)
    
    print(run.options)
    print(run.sourcefiles)
    
    run_result_future = _webclient.submit(run, limits, config.cpu_model, \
                              config.result_file_pattern, config.cloud_priority )
    _webclient.flush_runs()
    return run_result_future.result()

def _parse_cpachecker_args(cpachecker_args):
    """
    Parses the given CPAchecker arguments.
    @return:  Run object with options, identifier and list of source files
    """
    class Run:
        options = []
        identifier = None
        sourcefiles = []
        propertyfile = None 
   
    run = Run()
    run.identifier = cpachecker_args

    i = iter(cpachecker_args)
    while True:
        try:
            option=next(i)
            if option in ["-heap", "-timelimit", "-entryfunction", "-spec", "-config", "-setprop"]:
                run.options.append(option)
                run.options.append(next(i))

            elif option[0] == '-':
                run.options.append(option)
            
            else:
                run.sourcefiles.append(option)

        except StopIteration:
            break

    return run
    
def _parse_result(zip_content, config, cpachecker_args):
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
                return_value = _handle_result(result_zip_file, config, cpachecker_args)
        
        except zipfile.BadZipfile:
            logging.warning('Server returned illegal zip file with results of run {}.'.format(cpachecker_args))
            # Dump ZIP to disk for debugging
            with open(config.output_path + '.zip', 'wb') as zip_file:
                zip_file.write(zip_content)
                
    except IOError as e:
        logging.warning('Error while writing results of run {}: {}'.format(cpachecker_args, e))

    return return_value


def _handle_result(resultZipFile, config, cpachecker_args):
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
    Executes a single CPAchecker run in the VerifierCloud vis the web front end.
    All informations are given by the command line arguments.
    @return: the return value of CPAchecker
    """
    arg_parser = _create_argument_parser()
    (config, cpachecker_args) = arg_parser.parse_known_args()
    _setup_logging(config)
    _init(config)
    
    try:
        run_result = _submit_run(config, cpachecker_args)
        return _parse_result(run_result, config, cpachecker_args)
    
    except request.HTTPError as e:
        logging.warn(e.reason)
    except WebClientError as e:
        logging.warn(str(e))
    
    finally:
        _webclient.shutdown()
        

if _name_ == "_main_":
    sys.exit(_execute())
