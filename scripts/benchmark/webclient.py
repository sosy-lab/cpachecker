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

import logging
import os
import subprocess
import shutil
import zlib

from time import sleep
from zipfile import ZipFile

try:
    from httplib import HTTPConnection, HTTPResponse
    import urllib
    import urllib2
except ImportError:
    from http.client import HTTPConnection, HTTPResponse
    import urllib.parse as urllib
    import urllib.request as urllib2

try:
    from concurrent.futures import ThreadPoolExecutor
    from concurrent.futures import as_completed
except:
    pass

from benchexec.model import MEMLIMIT, TIMELIMIT, CORELIMIT
import benchexec.util as util

RESULT_KEYS = ["cputime", "walltime"]

MAX_SUBMISSION_THREADS = 5

PYTHON_VERSION = sys.version_info[0]

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
    benchmark.executable = ''

def get_system_info():
    return None

def execute_benchmark(benchmark, output_handler):

    if (benchmark.tool_name != 'CPAchecker'):
        logging.warn("The web client does only support the CPAchecker.")
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

            try:
                # python 3.2
                from concurrent.futures import ThreadPoolExecutor
                runIDs = _submitRunsPrallel(runSet, webclient, benchmark)
            except ImportError:
                runIDs = _submitRuns(runSet, webclient, benchmark)
                
            _getResults(runIDs, output_handler, webclient, benchmark)
            output_handler.output_after_run_set(runSet)

    except KeyboardInterrupt as e:
        STOPPED_BY_INTERRUPT = True
        raise e
    finally:
        output_handler.output_after_benchmark(STOPPED_BY_INTERRUPT)

def kill():
    # TODO: cancel runs on server
    pass

def _submitRunsPrallel(runSet, webclient, benchmark):
    
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
                message = e.read() #not all HTTPErrors have a read() method
            except:
                message = ""
            logging.warning('Could not submit run {0}: {1} {2}'.\
                format(run.identifier, e, message))
        finally:
            submissonCounter += 1

    return runIDs
    
def _submitRuns(runSet, webclient, benchmark):
    
    runIDs = {}
    submissonCounter = 1

    for run in runSet.runs:
        try:
            runID = _submitRun(run, webclient, benchmark)
            runIDs.update({runID:run})
            logging.info('Submitted run {0}/{1} with id {2}'.\
                format(submissonCounter, len(runSet.runs), runID))  

        except (urllib2.HTTPError, WebClientError) as e:
            try:
                message = e.read() #not all HTTPErrors have a read() method
            except:
                message = ""
            logging.warning('Could not submit run {0}: {1} {2}'.\
                format(run.identifier, e, message))
        finally:
            submissonCounter += 1
        
    return runIDs
    
def _submitRun(run, webclient, benchmark, counter = 0):
    invalidParams = False    
    programTexts = []
    for programPath in run.sourcefiles:
        with open(programPath, 'r') as programFile:
            programName = programPath.split('/')[-1]
            programText = programFile.read()
            programTexts.append(programText)
    params = {'programText': programTexts}

    if benchmark.config.revision:
        tokens = benchmark.config.revision.split(':')
        params.update({'svnBranch':tokens[0]})
        if len(tokens)>1:
            params.update({'revision':tokens[1]})

    if run.propertyfile:
        with open(run.propertyfile, 'r') as propertyFile:
            propertyText = propertyFile.read()      
            params.update({'propertyText':propertyText})
    
    limits = benchmark.rlimits
    if MEMLIMIT in limits:
        params.update({'memoryLimitation':str(limits[MEMLIMIT]) + "MB"})     
    if TIMELIMIT in limits:
        params.update({'timeLimitation':limits[TIMELIMIT]})  
    if CORELIMIT in limits:
        params.update({'coreLimitation':limits[CORELIMIT]})  

    if benchmark.config.cpu_model:
        params.update({'cpuModel':benchmark.config.cpu_model})                

 
    invalidOption = _handleOptions(run, params)
    invalidParams |= invalidOption   

    if invalidParams:
        raise WebClientError('Command {0} of run {1}  contains option that is not usable with the webclient. '\
            .format(run.options, run.identifier))        

    paramsEncoded = urllib.urlencode(params, True)
    headers = {"Content-type": "application/x-www-form-urlencoded", \
               "Accept": "text/plain"}
  
    if (PYTHON_VERSION == 3):
        paramsCompressed = zlib.compress(paramsEncoded.encode('utf-8'))
        headers.update({"Content-Encoding":"deflate"})
        resquest = urllib2.Request(webclient + "runs/", paramsCompressed, headers)
    else:
        resquest = urllib2.Request(webclient + "runs/", paramsEncoded, headers)

    # send request
    try:
         response = urllib2.urlopen(resquest)

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

def _handleOptions(run, params):
    options = ["statistics.print=true"]
    if run.options:
        option = ""
        i = iter(run.options)
        while True: 
            try: 
                option=next(i)
                if option == "-heap":
                    params.update({'heap':next(i)})

                elif option == "-noout":
                    options.append("output.disable=true")
                elif option == "-stat":
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
                     options.append("limits.time.cpu =" + next(i)) 
                elif option == "-skipRecursion":
                     options.append("cpa.callstack.skipRecursion=true")
                     options.append("analysis.summaryEdges=true")

                elif option == "-spec":
                     spec  = next(i)[-1].split('.')[0]
                     if spec[-8:] == ".graphml":
                          with open(spec, 'r') as  errorWitnessFile:
                              errorWitnessText = errorWitnessFile.read()      
                              params.update({'errorWitnessText':errorWitnessText})
                     else:
                         params.update({'specification':spec})
                elif option == "-config":
                     configPath = next(i)
                     tokens = configPath.split('/')
                     if not (tokens[0] == "config" and len(tokens) == 2):
                         logging.warning('Configuration {0} of run {1} is not from the default config directory.'.format(configPath, run.identifier))  
                         return True
                     config  = next(i).split('/')[2].split('.')[0]
                     params.update({'configuration':config})

                elif option == "-setprop":
                     options.append(next(i))

                elif option[0] == '-' and 'configuration' not in params :
                     params.update({'configuration': option[1:]})
                else:
                     return True

            except StopIteration: 
                break

    params.update({'option':options})
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
    resquest = urllib2.Request(webclient + "runs/" + runID + "/state", headers=headers)
    try:
        response = urllib2.urlopen(resquest)
    except urllib2.HTTPError as e:
        logging.info('Could get result of run with id {0}: {1}'.format(runID, e))
        _auth(webclient, benchmark)
        sleep(10)
        return False        
    
    if response.getcode() == 200:
        state = response.read()
        
        if PYTHON_VERSION == 3:
            state = state.decode('utf-8')
        
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
    zipFilePath = run.log_file + ".zip"    

    # download result as zip file
    counter = 0
    sucess = False
    while (not sucess and counter < 10):
        counter += 1
        resquest = urllib2.Request(webclient + "runs/" + runID + "/result")
        try:
             response = urllib2.urlopen(resquest)
        except urllib2.HTTPError as e:
             logging.info('Could get result of run with id {0}: {1}'.format(run.identifier, e))
             _auth(webclient, benchmark)
             sleep(10)
             return False        

        if response.getcode() == 200:
            with open(zipFilePath, 'w+b') as zipFile:
                zipFile.write(response.read())
                sucess = True
        else:
            sleep(1)
                
    if sucess:
       # unzip result
       resultDir = run.log_file + ".output"
       output_handler.output_before_run(run)
       with ZipFile(zipFilePath) as resultZipFile:
           resultZipFile.extractall(resultDir)
       os.remove(zipFilePath)
       
       # move logfile and stderr
       with open(run.log_file, 'w') as log_file:
           log_file.write(" ".join(run.cmdline()) + "\n\n\n\n\n------------------------------------------\n")
           toolLog = resultDir + "/output.log"
           if os.path.isfile(toolLog):
               for line in open(toolLog):
                   log_file.write(line)
               os.remove(toolLog)
       stderr = resultDir + "/stderr"
       if os.path.isfile(stderr):
           shutil.move(stderr, run.log_file + ".stdError")

       # extract values
       (run.walltime, run.cputime, return_value, values) = _parseCloudResultFile(resultDir + "/runInformation.txt")
       run.values.update(values)
       values = _parseAndSetCloudWorkerHostInformation(resultDir + "/hostInformation.txt", output_handler)
       run.values.update(values)
       run.after_execution(return_value)

       # remove no longer needed files
       os.remove(resultDir + "/hostInformation.txt")
       os.remove(resultDir + "/runInformation.txt")
       if os.listdir(resultDir) == []: 
           os.rmdir(resultDir)        

       output_handler.output_after_run(run)
       return True
       
    else:
        logging.warning('Could not get run result, run is not finished: {0}'.format(runID))
        return False     
    
def _parseAndSetCloudWorkerHostInformation(filePath, output_handler):
    try:
        output_handler.all_created_files.append(filePath)
        values = _parseFile(filePath)

        values["host"] = values.get("@vcloud-name", "-")
        name = values["host"]
        osName = values.get("@vcloud-os", "-")
        memory = values.get("@vcloud-memory", "-")
        cpuName = values.get("@vcloud-cpuModel", "-")
        frequency = values.get("@vcloud-frequency", "-")
        cores = values.get("@vcloud-cores", "-")
        output_handler.store_system_info(osName, cpuName, cores, frequency, memory, name)

    except IOError:
        logging.warning("Host information file not found: " + filePath)
   
    return values


def _parseCloudResultFile(filePath):

    walltime = None
    cputime = None
    memUsage = None
    return_value = None
    
    values = _parseFile(filePath)   

    return_value = int(values["@vcloud-exitcode"])
    walltime = float(values["walltime"].strip('s'))
    cputime = float(values["cputime"].strip('s'))
    values["memUsage"] = int(values["@vcloud-memory"].strip('B'))     
    
    return (walltime, cputime, return_value, values)

def _parseFile(filePath):
    values = {}

    with open(filePath, 'rt') as file:
        for line in file:
            (key, value) = line.split("=", 1)
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
            logging.serve('Invalid username password format, expected {user}:{pwd}')
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
