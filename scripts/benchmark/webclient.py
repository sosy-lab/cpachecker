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
from __future__ import absolute_import, print_function, unicode_literals

import sys
sys.dont_write_bytecode = True # prevent creation of .pyc files

import logging
import os
import subprocess
import shutil
import zlib
import urllib2

from time import sleep
from zipfile import ZipFile

try:
    from httplib import HTTPConnection, HTTPResponse
    import urllib
except:
    from http.client import HTTPConnection, HTTPResponse
    import urllib.parse as urllib

from .benchmarkDataStructures import MEMLIMIT, TIMELIMIT, CORELIMIT
from . import filewriter as filewriter
from . import util as Util

RESULT_KEYS = ["cpuTime", "wallTime", "energy" ]

def executeBenchmarkInCloud(benchmark, outputHandler):
    if not benchmark.config.cloudMaster[-1] == '/':
        benchmark.config.cloudMaster += '/'
    webclient = benchmark.config.cloudMaster   
    logging.info('Using webclient at {0}'.format(webclient))

    #authentification 
    _auth(webclient, benchmark)
    
    for runSet in benchmark.runSets:
        if not runSet.shouldBeExecuted():
            outputHandler.outputForSkippingRunSet(runSet)
            continue

        outputHandler.outputBeforeRunSet(runSet)

        runIDs = _submitRuns(runSet, benchmark.rlimits, webclient, benchmark)
        
        _getResults(runIDs, outputHandler, webclient, benchmark)
        
        outputHandler.outputAfterRunSet(runSet)

    outputHandler.outputAfterBenchmark(False)
    
def _submitRuns(runSet, limits, webclient, benchmark):
    
    runIDs = {}
    counter = 0

    for run in runSet.runs:
        # handler parameters
        invalidParams = False    
        programTexts = []
        for programPath in run.sourcefiles:
            with open(programPath, 'r') as programFile:
                programName = programPath.split('/')[-1]
                programText = programFile.read()
                programTexts.append(programText)
        params = {'programText': programTexts}

        if run.propertyfile:
            propertyText = open(run.propertyfile, 'r').read()      
            params.update({'propertyText':propertyText})
        
        if MEMLIMIT in limits:
            params.update({'memoryLimitation':str(limits[MEMLIMIT]) + "MB"})     
        if TIMELIMIT in limits:
            params.update({'timeLimitation':limits[TIMELIMIT]})  
        if CORELIMIT in limits:
            params.update({'coreLimitation':limits[CORELIMIT]})  

        if benchmark.config.cloudCPUModel:
            params.update({'cpuModel':benchmark.config.cloudCPUModel})                

 
        invalidOption = _handleOptions(run, params)
	invalidParams |= invalidOption   

        if invalidParams:
            logging.warning('Command {0} of run {1}  contains option that is not usable with the webclient. '.format(run.options, run.identifier))
            continue         

        paramsEncoded = urllib.urlencode(params, True)
        #paramsCompressed = zlib.compress(params)
        headers = {"Content-type": "application/x-www-form-urlencoded", \
                   "Accept": "text/plain"}
       
        # send request
        resquest = urllib2.Request(webclient + "runs/", paramsEncoded, headers)
        try:
             response = urllib2.urlopen(resquest)
        except urllib2.HTTPError as e:
             logging.info('Could not submit run with id {0}: {1}'.format(run.identifier, e))
	     _auth(webclient, benchmark)
             continue          
        finally:
           counter += 1

        if response.getcode() == 200:
            runID = response.read()
            logging.info('Submitted run {0}/{1} with id {2}'.format(counter, len(runSet.runs), runID))  
            runIDs.update({runID:run})
            
        else:
            logging.warning('Could not submit run {0}: {1}'.format(run.identifier, response.read()))
        
    return runIDs

def _handleOptions(run, params):
    if run.options:
        options = []
        option = ""
        i = iter(run.options)
        while True: 
      	    try: 
   	        option=i.next()
   	        if option == "-heap":
   	            params.update({'heap':i.next()})

   	        elif option == "-noout":
   	            options.append("output.disable=true")
   	        elif option == "-java":
   	            options.append("language=JAVA")
                elif option == "-32":
   	            options.append("analysis.machineModel=Linux32")
                elif option == "-64":
   	            options.append("analysis.machineModel=Linux64")
                elif option == "-entryfunction":
   	            options.append("analysis.entryFunction=" + i.next())
	        elif option == "-timelimit":
	            options.append("limits.time.cpu =" + i.next())

 	        elif option == "-spec":
	            spec  = i.next()[-1].split('.')[0]
	            params.update({'specification':spec})
	        elif option == "-config":
	            configPath = i.next()
	            tokens = configPath.split('/')
	            if not (tokens[0] == "config" and len(tokens) == 2):
	                logging.warning('Configuration {0} of run {1} is not from the default config directory.'.format(configPath, run.identifier))  
                        return True
	            config  = i.next().split('/')[2].split('.')[0]
	            params.update({'configuration':config})

	        elif option == "-setprop":
	            options.append(i.next())

	        elif option[0] == '-' and 'configuration' not in params :
	            params.update({'configuration': option[1:]})
	        else:
	            return True

	    except StopIteration: 
	        break

        if len(options) > 0:
            params.update({'option':options})

    return False   

def _getResults(runIDs, outputHandler, webclient, benchmark):
    while len(runIDs) > 0 :
	finishedRunIDs = []
        for runID in runIDs.iterkeys():
            if _isFinished(runID, webclient, benchmark):
                _getAndHandleResult(runID, runIDs[runID], outputHandler, webclient)
		finishedRunIDs.append(runID)

        for runID in finishedRunIDs:
             del runIDs[runID]

def _isFinished(runID, webclient, benchmark):

    resquest = urllib2.Request(webclient + "runs/" + runID + "/state")
    try:
        response = urllib2.urlopen(resquest)
    except urllib2.HTTPError as e:
        logging.info('Could get result of run with id {0}: {1}'.format(runID, e))
        _auth(webclient, benchmark)
        sleep(10)
        return False        
    
    if response.getcode() == 200:
        state = response.read()
        if state == "FINISHED":
            logging.info('Run {0} finished.'.format(runID))  
            return True
        
        else:
            return False
        
    else:
        logging.warning('Could not get run state: {0}'.format(runID))    
        
        return False

def _getAndHandleResult(runID, run, outputHandler, webclient):
    zipFilePath = run.logFile + ".zip"    

    # download result as zip file
    counter = 0
    sucess = False
    while (not sucess and counter < 30):
        counter += 1
        resquest = urllib2.Request(webclient + "runs/" + runID + "/result")
        try:
             response = urllib2.urlopen(resquest)
        except urllib2.HTTPError as e:
             logging.info('Could get result of run with id {0}: {1}'.format(run.identifier, e))
             _auth(webclient, benchmark)
             sleep(10)
             continue        

        if response.getcode() == 200:
            with open(zipFilePath, 'w+b') as zipFile:
                zipFile.write(response.read())
                sucess = True
        else:
            sleep(1)
                
    if sucess:
       # unzip result
       resultDir = run.logFile + ".output"
       outputHandler.outputBeforeRun(run)
       with ZipFile(zipFilePath) as resultZipFile:
           resultZipFile.extractall(resultDir)
       os.remove(zipFilePath)
       
       # move logfile and stderr
       logFile = resultDir + "/stdout"
       if os.path.isfile(logFile):
           shutil.move(logFile, run.logFile)
       stderr = resultDir + "/stderr"
       if os.path.isfile(stderr):
           shutil.move(stderr, run.logFile + ".stdError")

       # extract values
       (run.wallTime, run.cpuTime, returnValue, values) = _parseCloudResultFile(resultDir + "/runInformation.txt")
       run.values.update(values)
       values = _parseAndSetCloudWorkerHostInformation(resultDir + "/hostInformation.txt", outputHandler)
       run.values.update(values)
       run.afterExecution(returnValue)

       # remove no longer needed files
       os.remove(resultDir + "/hostInformation.txt")
       os.remove(resultDir + "/runInformation.txt")
       if os.listdir(resultDir) == []: 
           os.rmdir(resultDir)        

       outputHandler.outputAfterRun(run)
    else:
        logging.warning('Could not get run result: {0}'.format(runID))     
    
def _parseAndSetCloudWorkerHostInformation(filePath, outputHandler):
    try:
        outputHandler.allCreatedFiles.append(filePath)
        values = _parseFile(filePath)

        values["host"] = values.get("@vcloud-name", "-")
        name = values["host"]
        osName = values.get("@vcloud-os", "-")
        memory = values.get("@vcloud-memory", "-")
        cpuName = values.get("@vcloud-cpuModel", "-")
        frequency = values.get("@vcloud-frequency", "-")
        cores = values.get("@vcloud-cores", "-")
        outputHandler.storeSystemInfo(osName, cpuName, cores, frequency, memory, name)

    except IOError:
        logging.warning("Host information file not found: " + filePath)
   
    return values


def _parseCloudResultFile(filePath):

    wallTime = None
    cpuTime = None
    memUsage = None
    returnValue = None
    energy = None
    
    values = _parseFile(filePath)   

    returnValue = int(values["@vcloud-exitCode"])
    wallTime = float(values["wallTime"].strip('s'))
    cpuTime = float(values["cpuTime"].strip('s'))
    values["memUsage"] = int(values["@vcloud-usedMemory"].strip('B'))     
    
    return (wallTime, cpuTime, returnValue, values)

def _parseFile(filePath):
    values = {}

    with open(filePath, 'rt') as file:
        for line in file:
            (key, value) = line.split("=", 2)
            value = value.strip()
            if key in RESULT_KEYS:
                values[key] = value
            else:
                # "@" means value is hidden normally
                values["@vcloud-" + key] = value

    return values

def _auth(webclient, benchmark):
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
