"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2015  Dirk Beyer
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

import fnmatch
import io
import logging
import os
import shutil
import threading
import zipfile

import urllib.request as urllib2
from concurrent.futures import ThreadPoolExecutor
from concurrent.futures import as_completed

from .webclient import *

"""
This module provides a BenchExec integration for the web interface of the VerifierCloud.
"""

_webclient = None
_print_lock = threading.Lock()

def init(config, benchmark):
    global _webclient
    
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
        
    _webclient = WebInterface(config.cloudMaster, config.cloudUser, svn_branch, svn_revision)

    benchmark.tool_version = _webclient.tool_revision()
    logging.info('Using tool version {0}.'.format(benchmark.tool_version))
    benchmark.executable = 'scripts/cpa.sh'

def get_system_info():
    return None

def execute_benchmark(benchmark, output_handler):
    global _webclient

    if (benchmark.tool_name != 'CPAchecker'):
        logging.warning("The web client does only support the CPAchecker.")
        return
    
    if not _webclient:
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

            _handle_results(result_futures, output_handler, benchmark)
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
    global _webclient
    _webclient.shutdown()
    _webclient = None
    
def _submitRunsParallel(runSet, benchmark):
    global _webclient
    
    logging.info('Submitting runs...')

    executor = ThreadPoolExecutor(MAX_SUBMISSION_THREADS)
    submission_futures = {}
    submissonCounter = 1
    limits = benchmark.rlimits
    cpu_model = benchmark.config.cpu_model
    result_files_pattern = benchmark.result_files_pattern
    priority = benchmark.config.cloudPriority
    
    for run in runSet.runs:
        submisson_future = executor.submit(_webclient.submit, run, limits, cpu_model, result_files_pattern, priority)
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

    _webclient.flush_runs()
    logging.info("Run submission finished.")
    return result_futures

def _handle_results(result_futures, output_handler, benchmark):
    executor = ThreadPoolExecutor(MAX_SUBMISSION_THREADS)

    for result_future in as_completed(result_futures.keys()):
        run = result_futures[result_future]
        result = result_future.result()
        executor.submit(_unzip_and_handle_result, result, run, output_handler, benchmark)

def _unzip_and_handle_result(result, run, output_handler, benchmark):
    
    # unzip result
    return_value = None
    try:
        try:
            with zipfile.ZipFile(io.BytesIO(result)) as resultZipFile:
                return_value = _parse_result(resultZipFile, run, output_handler, benchmark.result_files_pattern)
        except zipfile.BadZipfile:
            logging.warning('Server returned illegal zip file with results of run {}.'.format(run.identifier))
            # Dump ZIP to disk for debugging
            with open(run.log_file + '.zip', 'wb') as zipFile:
                zipFile.write(result)
    except IOError as e:
        logging.warning('Error while writing results of run {}: {}'.format(run.identifier, e))

    if return_value is not None:
        run.after_execution(return_value)
        
        with _print_lock:
            output_handler.output_before_run(run)
            output_handler.output_after_run(run)

def _parse_result(resultZipFile, run, output_handler, result_files_pattern):
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
            values = _parse_and_set_cloud_worker_host_information(hostInformation, output_handler, run.runSet)
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

def _parse_and_set_cloud_worker_host_information(file, output_handler, runSet):
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