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

def executeBenchmarkInCloud(benchmark, outputHandler):
    if not benchmark.config.cloudMaster[-1] == '/':
        benchmark.config.cloudMaster += '/'
    webclient = benchmark.config.cloudMaster   

    #authentification 
    tokens = benchmark.config.cloudUser.split(':')
    if len(tokens) < 2:
        logging.serve('Invalid username password format, expected {user}:{pwd}')
        return  
    username = tokens[0]
    password = tokens[1]
    auth_handler = urllib2.HTTPBasicAuthHandler()
    auth_handler.add_password(realm='SoSy-Lab VerifierCloud',\
                    uri=webclient,\
                    user=username,\
                    passwd=password)
    opener = urllib2.build_opener(auth_handler)
    # install it globally so it can be used with urlopen
    urllib2.install_opener(opener)   
    
    logging.info('Using webclient at {0}'.format(webclient))
    
    for runSet in benchmark.runSets:
        if not runSet.shouldBeExecuted():
            outputHandler.outputForSkippingRunSet(runSet)
            continue

        outputHandler.outputBeforeRunSet(runSet)

        runIDs = _submitRuns(runSet, benchmark.rlimits, benchmark.config.cloudCPUModel, webclient)
        
        _getResults(runIDs, outputHandler, webclient)
        
        outputHandler.outputAfterRunSet(runSet)

    outputHandler.outputAfterBenchmark(False)
    
def _submitRuns(runSet, limits, cpuModel, webclient):
    
    runIDs = {}
    
    for run in runSet.runs:
            
        programTexts = []
        for programPath in run.sourcefiles:
            with open(programPath, 'r') as programFile:
                programName = programPath.split('/')[-1]
                programText = programFile.read()
                programTexts.append(programText)
        params = {'programText': programTexts}

        if run.propertyfile:
            propertyText = open(run.propertyfile, 'r').read()      
            params.update({'specificationText':propertyText})
        
        if MEMLIMIT in limits:
            params.update({'memoryLimitation':str(limits[MEMLIMIT]) + "MB"})     
        if TIMELIMIT in limits:
            params.update({'timeLimitation':limits[TIMELIMIT]})  
        if CORELIMIT in limits:
            params.update({'coreLimitation':limits[CORELIMIT]})  

        if cpuModel:
            params.update({'cpuModel':cpuModel})                

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
                        params.update({'setprop':'noout'})
                    elif option == "-spec":
                        spec  = i.next().split('/')[-1].split('.')[0]
                        params.update({'specification':spec})
                    elif option == "-config":
                        config  = i.next().split('/')[-1].split('.')[0]
                        params.update({'configuration':config})
                    elif option == "-setprop":
                        options.append(i.next())
                    elif option == "-timelimit":
                        options.append("limits.time.cpu =" + i.next())
                    elif option[0] == '-':
                        params.update({'configuration': option[1:]})
                except StopIteration: 
                     break
                 
        if len(options) > 0:
            params.update({'option':options})
        
        paramsEncoded = urllib.urlencode(params, True)
        #paramsCompressed = zlib.compress(params)
        headers = {"Content-type": "application/x-www-form-urlencoded", \
                   "Accept": "text/plain"}

        resquest = urllib2.Request(webclient + "runs/", paramsEncoded, headers)
        response = urllib2.urlopen(resquest)
        
        if response.getcode() == 200:
            runID = response.read()
            logging.info('Submitted run with id {0}'.format(runID))  
            runIDs.update({runID:run})
            
        else:
            logging.warning('Could not submit run {0}: {1}'.format(run.identifier, response.read()))
        
    return runIDs
 
def _getResults(runIDs, outputHandler, webclient):
    while len(runIDs) > 0 :
	finishedRunIDs = []
        for runID in runIDs.iterkeys():
            if _isFinished(runID, webclient):
                _getAndHandleResult(runID, runIDs[runID], outputHandler, webclient)
		finishedRunIDs.append(runID)

        for runID in finishedRunIDs:
             del runIDs[runID]

def _isFinished(runID, webclient):

    resquest = urllib2.Request(webclient + "runs/" + runID + "/state")
    response = urllib2.urlopen(resquest)
    
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

    counter = 0
    sucess = False
    while (not sucess and counter < 30):
        resquest = urllib2.Request(webclient + "runs/" + runID + "/result")
        response = urllib2.urlopen(resquest)
        counter += 1

        if response.getcode() == 200:
            with open(zipFilePath, 'w+b') as zipFile:
                zipFile.write(response.read())
                sucess = True
        else:
            sleep(1)
                
    if sucess:
       resultDir = run.logFile + ".files"
       outputHandler.outputBeforeRun(run)
       resultZipFile = ZipFile(zipFilePath)
       resultZipFile.extractall(resultDir)
       
       logFile = resultDir + "/stdout"
       if os.path.isfile(logFile):
           shutil.copyfile(logFile, run.logFile)

       (run.wallTime, run.cpuTime, memUsage, returnValue, energy) = _parseCloudResultFile(resultDir + "/runInformation.txt")
       run.values['memUsage'] = memUsage
       #run.values['energy'] = energy
       host = _parseAndSetCloudWorkerHostInformation(resultDir + "/hostInformation.txt", outputHandler)
       run.values['host'] = host
       run.afterExecution(returnValue)
       outputHandler.outputAfterRun(run)
    else:
        logging.warning('Could not get run result: {0}'.format(runID))     
    
def _parseAndSetCloudWorkerHostInformation(filePath, outputHandler):
    try:
        with open(filePath, 'rt') as file:
            outputHandler.allCreatedFiles.append(filePath)

            # Parse first part of information about hosts until first blank line
            while True:
                line = file.readline().strip()
                if not line:
                    break
                name = line.split("=")[-1].strip()
                osName = file.readline().split("=")[-1].strip()
                memory = file.readline().split("=")[-1].strip()
                cpuName = file.readline().split("=")[-1].strip()
                frequency = file.readline().split("=")[-1].strip()
                cores = file.readline().split("=")[-1].strip()
                outputHandler.storeSystemInfo(osName, cpuName, cores, frequency, memory, name)

            # Ignore second part of information about runs
            # (we read the run-to-host mapping from the .data file for each run).

    except IOError:
        logging.warning("Host information file not found: " + filePath)
   
    return name


def _parseCloudResultFile(filePath):

    wallTime = None
    cpuTime = None
    memUsage = None
    returnValue = None
    energy = None

    with open(filePath, 'rt') as file:
        command = file.readline().strip()
        returnValue = int(file.readline().split("=")[-1].strip())
	wallTime    = float(file.readline().split("=")[-1].strip().strip('s'))
        cpuTime     = float(file.readline().split("=")[-1].strip().strip('s'))
        memUsage    = int(file.readline().split("=")[-1].strip().strip('B'))
        energy =  None        
    
    print(wallTime, cpuTime, memUsage, returnValue, energy)
    return (wallTime, cpuTime, memUsage, returnValue, energy)

