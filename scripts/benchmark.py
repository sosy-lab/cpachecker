#!/usr/bin/env python

"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2012  Dirk Beyer
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
from __future__ import absolute_import, unicode_literals

import sys
sys.dont_write_bytecode = True # prevent creation of .pyc files

from datetime import date

try:
  import Queue
except ImportError: # Queue was renamed to queue in Python 3
  import queue as Queue

import time
import glob
import logging
import argparse
import os
import re
import resource
import signal
import subprocess
import threading
import xml.etree.ElementTree as ET

import benchmark.filewriter as filewriter
import benchmark.result as result
import benchmark.runexecutor as runexecutor
import benchmark.util as Util


MEMLIMIT = runexecutor.MEMLIMIT
TIMELIMIT = runexecutor.TIMELIMIT

# colors for column status in terminal
USE_COLORS = True
COLOR_GREEN = "\033[32;1m{0}\033[m"
COLOR_RED = "\033[31;1m{0}\033[m"
COLOR_ORANGE = "\033[33;1m{0}\033[m"
COLOR_MAGENTA = "\033[35;1m{0}\033[m"
COLOR_DEFAULT = "{0}"
COLOR_DIC = {"correctSafe": COLOR_GREEN,
             "correctUnsafe": COLOR_GREEN,
             "unknown": COLOR_ORANGE,
             "error": COLOR_MAGENTA,
             "wrongUnsafe": COLOR_RED,
             "wrongSafe": COLOR_RED,
             None: COLOR_DEFAULT}
TERMINAL_TITLE = "\033kBenchmark {0}\033"


# the number of digits after the decimal separator of the time column,
# for the other columns it can be configured in the xml-file
TIME_PRECISION = 2


# next lines are needed for stopping the script
WORKER_THREADS = []
STOPPED_BY_INTERRUPT = False


"""
Naming conventions:

TOOL: a verifier program that should be executed
EXECUTABLE: the executable file that should be called for running a TOOL
SOURCEFILE: one file that contains code that should be verified
RUN: one execution of a TOOL on one SOURCEFILE
RUNSET: a set of RUNs of one TOOL with at most one RUN per SOURCEFILE
RUNDEFINITION: a template for the creation of a RUNSET with RUNS from one or more SOURCEFILESETs
BENCHMARK: a list of RUNDEFINITIONs and SOURCEFILESETs for one TOOL
OPTION: a user-specified option to add to the command-line of the TOOL when it its run
CONFIG: the configuration of this script consisting of the command-line arguments given by the user

"run" always denotes a job to do and is never used as a verb.
"execute" is only used as a verb (this is what is done with a run).
A benchmark or a run set can also be executed, which means to execute all contained runs.

Variables ending with "file" contain filenames.
Variables ending with "tag" contain references to XML tag objects created by the XML parser.
"""


class Benchmark:
    """
    The class Benchmark manages the import of source files, options, columns and
    the tool from a benchmarkFile.
    This class represents the <benchmark> tag.
    """

    def __init__(self, benchmarkFile):
        """
        The constructor of Benchmark reads the source files, options, columns and the tool
        from the XML in the benchmarkFile..
        """
        logging.debug("I'm loading the benchmark {0}.".format(benchmarkFile))

        self.benchmarkFile = benchmarkFile

        # get benchmark-name
        self.name = os.path.basename(benchmarkFile)[:-4] # remove ending ".xml"
        if config.name:
            self.name += "."+config.name

        # get current date as String to avoid problems, if script runs over midnight
        currentTime = time.localtime()
        self.date = time.strftime("%y-%m-%d_%H%M", currentTime)
        self.dateISO = time.strftime("%y-%m-%d %H:%M", currentTime)

        # parse XML
        rootTag = ET.ElementTree().parse(benchmarkFile)

        # get tool
        toolName = rootTag.get('tool')
        if not toolName:
            sys.exit('A tool needs to be specified in the benchmark definition file.')
        toolModule = "benchmark.tools." + toolName
        try:
            self.tool = __import__(toolModule, fromlist=['Tool']).Tool()
        except ImportError:
            sys.exit('Unsupported tool "{0}" specified.'.format(toolName))
        except AttributeError:
            sys.exit('The module for "{0}" does not define the necessary class.'.format(toolName))

        self.toolName = self.tool.getName()
        self.executable = self.tool.getExecutable()
        self.toolVersion = self.tool.getVersion(self.executable)

        logging.debug("The tool to be benchmarked is {0}.".format(repr(self.tool)))

        self.rlimits = {}
        keys = list(rootTag.keys())
        if MEMLIMIT in keys:
            self.rlimits[MEMLIMIT] = int(rootTag.get(MEMLIMIT))
        if TIMELIMIT in keys:
            self.rlimits[TIMELIMIT] = int(rootTag.get(TIMELIMIT))

        # override limits from XML with values from command line
        if config.memorylimit != None:
            memorylimit = int(config.memorylimit)
            if memorylimit == -1: # infinity
                if MEMLIMIT in self.rlimits:
                    self.rlimits.pop(MEMLIMIT)
            else:
                self.rlimits[MEMLIMIT] = memorylimit

        if config.timelimit != None:
            timelimit = int(config.timelimit)
            if timelimit == -1: # infinity
                if TIMELIMIT in self.rlimits:
                    self.rlimits.pop(TIMELIMIT)
            else:
                self.rlimits[TIMELIMIT] = timelimit

        # get number of threads, default value is 1
        self.numOfThreads = int(rootTag.get("threads")) if ("threads" in keys) else 1
        if config.numOfThreads != None:
            self.numOfThreads = config.numOfThreads
        if self.numOfThreads < 1:
            logging.error("At least ONE thread must be given!")
            sys.exit()

        # create folder for file-specific log-files.
        # existing files (with the same name) will be OVERWRITTEN!
        self.outputBase = OUTPUT_PATH + self.name + "." + self.date
        self.logFolder = self.outputBase + ".logfiles/"
        if not os.path.isdir(self.logFolder):
            os.makedirs(self.logFolder)

        # get global options
        self.options = getOptionsFromXML(rootTag)

        # get columns
        self.columns = self.loadColumns(rootTag.find("columns"))

        # get global source files, they are used in all run sets
        globalSourcefilesTags = rootTag.findall("sourcefiles")

        # get benchmarks
        self.runSets = []
        i = 1
        for rundefinitionTag in rootTag.findall("rundefinition"):
            self.runSets.append(RunSet(rundefinitionTag, self, i, globalSourcefilesTags))
            i += 1

        if not self.runSets:
            for rundefinitionTag in rootTag.findall("test"):
                self.runSets.append(RunSet(rundefinitionTag, self, i, globalSourcefilesTags))
                i += 1
            if self.runSets:
                logging.warning("Benchmark file {0} uses deprecated <test> tags. Please rename them to <rundefinition>.".format(benchmarkFile))
            else:
                logging.warning("Benchmark file {0} specifies no runs to execute (no <rundefinition> tags found).".format(benchmarkFile))

        self.outputHandler = OutputHandler(self)


    def loadColumns(self, columnsTag):
        """
        @param columnsTag: the columnsTag from the XML file
        @return: a list of Columns()
        """

        logging.debug("I'm loading some columns for the outputfile.")
        columns = []
        if columnsTag != None: # columnsTag is optional in XML file
            for columnTag in columnsTag.findall("column"):
                pattern = columnTag.text
                title = columnTag.get("title", pattern)
                numberOfDigits = columnTag.get("numberOfDigits") # digits behind comma
                column = Column(pattern, title, numberOfDigits)
                columns.append(column)
                logging.debug('Column "{0}" with title "{1}" loaded from XML file.'
                          .format(column.text, column.title))
        return columns


class RunSet:
    """
    The class RunSet manages the import of files and options of a run set.
    """

    def __init__(self, rundefinitionTag, benchmark, index, globalSourcefilesTags=[]):
        """
        The constructor of RunSet reads run-set name and the source files from rundefinitionTag.
        Source files can be included or excluded, and imported from a list of
        names in another file. Wildcards and variables are expanded.
        @param rundefinitionTag: a rundefinitionTag from the XML file
        """

        self.benchmark = benchmark

        # get name of run set, name is optional, the result can be "None"
        self.realName = rundefinitionTag.get("name")

        # index is the number of the run set
        self.index = index

        self.logFolder = benchmark.logFolder
        if self.realName:
            self.logFolder += self.realName + "."

        # get all run-set-specific options from rundefinitionTag
        self.options = benchmark.options + getOptionsFromXML(rundefinitionTag)

        # get all runs, a run contains one sourcefile with options
        self.blocks = self.extractRunsFromXML(globalSourcefilesTags + rundefinitionTag.findall("sourcefiles"))
        self.runs = [run for block in self.blocks for run in block.runs]

        names = [self.realName]
        if len(self.blocks) == 1:
            # there is exactly one source-file set to run, append its name to run-set name
            names.append(self.blocks[0].realName)
        self.name = '.'.join(filter(None, names))
        self.fullName = self.benchmark.name + (("." + self.name) if self.name else "")


    def shouldBeExecuted(self):
        return not config.selectedRunDefinitions \
            or self.realName in config.selectedRunDefinitions


    def extractRunsFromXML(self, sourcefilesTagList):
        '''
        This function builds a list of SourcefileSets (containing filename with options).
        The files and their options are taken from the list of sourcefilesTags.
        '''
        # runs are structured as sourcefile sets, one set represents one sourcefiles tag
        blocks = []
        baseDir = os.path.dirname(self.benchmark.benchmarkFile)

        for index, sourcefilesTag in enumerate(sourcefilesTagList):
            sourcefileSetName = sourcefilesTag.get("name")
            matchName = sourcefileSetName or str(index)
            if (config.selectedSourcefileSets and matchName not in config.selectedSourcefileSets):
                continue

            # get list of filenames
            sourcefiles = self.getSourcefilesFromXML(sourcefilesTag, baseDir)

            # get file-specific options for filenames
            fileOptions = getOptionsFromXML(sourcefilesTag)

            currentRuns = []
            for sourcefile in sourcefiles:
                currentRuns.append(Run(sourcefile, fileOptions, self))

            blocks.append(SourcefileSet(sourcefileSetName, index, currentRuns))
        return blocks


    def getSourcefilesFromXML(self, sourcefilesTag, baseDir):
        sourcefiles = []

        # get included sourcefiles
        for includedFiles in sourcefilesTag.findall("include"):
            sourcefiles += self.expandFileNamePattern(includedFiles.text, baseDir)

        # get sourcefiles from list in file
        for includesFilesFile in sourcefilesTag.findall("includesfile"):

            for file in self.expandFileNamePattern(includesFilesFile.text, baseDir):

                # check for code (if somebody confuses 'include' and 'includesfile')
                if Util.isCode(file):
                    logging.error("'" + file + "' seems to contain code instead of a set of source file names.\n" + \
                        "Please check your benchmark definition file or remove bracket '{' from this file.")
                    sys.exit()

                # read files from list
                fileWithList = open(file, "r")
                for line in fileWithList:

                    # strip() removes 'newline' behind the line
                    line = line.strip()

                    # ignore comments and empty lines
                    if not Util.isComment(line):
                        sourcefiles += self.expandFileNamePattern(line, os.path.dirname(file))

                fileWithList.close()

        # remove excluded sourcefiles
        for excludedFiles in sourcefilesTag.findall("exclude"):
            excludedFilesList = self.expandFileNamePattern(excludedFiles.text, baseDir)
            for excludedFile in excludedFilesList:
                sourcefiles = Util.removeAll(sourcefiles, excludedFile)

        return sourcefiles


    def expandFileNamePattern(self, pattern, baseDir):
        """
        The function expandFileNamePattern expands a filename pattern to a sorted list
        of filenames. The pattern can contain variables and wildcards.
        If baseDir is given and pattern is not absolute, baseDir and pattern are joined.
        """

        # store pattern for fallback
        shortFileFallback = pattern

        # replace vars like ${benchmark_path},
        # with converting to list and back, we can use the function 'substituteVars()'
        expandedPattern = substituteVars([pattern], self)
        assert len(expandedPattern) == 1
        expandedPattern = expandedPattern[0]

        # 'join' ignores baseDir, if expandedPattern is absolute.
        # 'normpath' replaces 'A/foo/../B' with 'A/B', for pretty printing only
        expandedPattern = os.path.normpath(os.path.join(baseDir, expandedPattern))

        # expand tilde and variables
        expandedPattern = os.path.expandvars(os.path.expanduser(expandedPattern))

        # expand wildcards
        fileList = glob.glob(expandedPattern)

        # sort alphabetical,
        fileList.sort()

        if expandedPattern != pattern:
            logging.debug("Expanded tilde and/or shell variables in expression {0} to {1}."
                .format(repr(pattern), repr(expandedPattern)))

        if not fileList and baseDir:
            # try fallback for old syntax of run definitions
            fileList = self.expandFileNamePattern(shortFileFallback, "")
            if fileList:
                logging.warning("Run definition uses old-style paths. Please change the path {0} to be relative to {1}."
                            .format(repr(shortFileFallback), repr(baseDir)))
            else:
                logging.warning("No files found matching {0}."
                            .format(repr(pattern)))

        return fileList


class SourcefileSet():
    """
    A SourcefileSet contains a list of runs and a name.
    """
    def __init__(self, name, index, runs):
        self.realName = name # this name is optional
        self.name = name or str(index) # this name is always non-empty
        self.runs = runs


class Run():
    """
    A Run contains one sourcefile and options.
    """

    def __init__(self, sourcefile, fileOptions, runSet):
        self.sourcefile = sourcefile
        self.runSet = runSet
        self.benchmark = runSet.benchmark
        self.specificOptions = fileOptions # options that are specific for this run
        self.logFile = runSet.logFolder + os.path.basename(sourcefile) + ".log"
        self.options = substituteVars(runSet.options + fileOptions, # all options to be used when executing this run
                                      runSet,
                                      sourcefile)

        # Copy columns for having own objects in run
        # (we need this for storing the results in them).
        self.columns = [Column(c.text, c.title, c.numberOfDigits) for c in self.benchmark.columns]

        # dummy values, for output in case of interrupt
        self.status = ""
        self.cpuTime = 0
        self.wallTime = 0
        
        self.tool = self.benchmark.tool
        args = self.tool.getCmdline(self.benchmark.executable, self.options, self.sourcefile)
        args = [os.path.expandvars(arg) for arg in args]
        args = [os.path.expanduser(arg) for arg in args]
        self.args = args;



    def afterExecution(self, returnvalue, output):
        
        rlimits = self.benchmark.rlimits
        
        # calculation: returnvalue == (returncode * 256) + returnsignal
        # highest bit of returnsignal shows only whether a core file was produced, we clear it
        returnsignal = returnvalue & 0x7F
        returncode = returnvalue >> 8
        logging.debug("My subprocess returned {0}, code {1}, signal {2}.".format(returnvalue, returncode, returnsignal))
        self.status = self.tool.getStatus(returncode, returnsignal, output, self._isTimeout())
        self.tool.addColumnValues(output, self.columns)
       
        # Tools sometimes produce a result even after a timeout.
        # This should not be counted, so we overwrite the result with TIMEOUT
        # here. if this is the case.
        # However, we don't want to forget more specific results like SEGFAULT,
        # so we do this only if the result is a "normal" one like SAFE.
        if not self.status in ['SAFE', 'UNSAFE', 'UNKNOWN']:
            if TIMELIMIT in rlimits:
                timeLimit = rlimits[TIMELIMIT] + 20
                if self.wallTime > timeLimit or self.cpuTime > timeLimit:
                    self.status = "TIMEOUT"
                    
        self.benchmark.outputHandler.outputAfterRun(self)

    def execute(self, numberOfThread):
        """
        This function executes the tool with a sourcefile with options.
        It also calls functions for output before and after the run.
        @param numberOfThread: runs are executed in different threads
        """
        self.benchmark.outputHandler.outputBeforeRun(self)

        rlimits = self.benchmark.rlimits

        (self.wallTime, self.cpuTime, self.memUsage, returnvalue, output) = runexecutor.executeRun(self.args, rlimits, self.logFile, numberOfThread, config.limitCores)

        if STOPPED_BY_INTERRUPT:
            # If the run was interrupted, we ignore the result and cleanup.
            self.wallTime = 0
            self.cpuTime = 0
            try:
                os.remove(self.logFile)
            except OSError:
                pass
            return

        self.afterExecution(returnvalue, output)

    def _isTimeout(self):
        ''' try to find out whether the tool terminated because of a timeout '''
        rlimits = self.benchmark.rlimits
        if TIMELIMIT in rlimits:
            limit = rlimits[TIMELIMIT]
        else:
            limit = float('inf')

        return self.cpuTime > limit*0.99


class Column:
    """
    The class Column contains text, title and numberOfDigits of a column.
    """

    def __init__(self, text, title, numOfDigits):
        self.text = text
        self.title = title
        self.numberOfDigits = numOfDigits
        self.value = ""


class OutputHandler:
    """
    The class OutputHandler manages all outputs to the terminal and to files.
    """

    printLock = threading.Lock()

    def __init__(self, benchmark):
        """
        The constructor of OutputHandler collects information about the benchmark and the computer.
        """

        self.allCreatedFiles = []
        self.benchmark = benchmark
        self.statistics = Statistics()

        # get information about computer
        (opSystem, cpuModel, numberOfCores, maxFrequency, memory, hostname) = self.getSystemInfo()
        version = self.benchmark.toolVersion

        memlimit = None
        timelimit = None
        if MEMLIMIT in self.benchmark.rlimits:
            memlimit = str(self.benchmark.rlimits[MEMLIMIT]) + " MB"
        if TIMELIMIT in self.benchmark.rlimits:
            timelimit = str(self.benchmark.rlimits[TIMELIMIT]) + " s"

        self.storeHeaderInXML(version, memlimit, timelimit, opSystem, cpuModel,
                              numberOfCores, maxFrequency, memory, hostname)
        self.writeHeaderToLog(version, memlimit, timelimit, opSystem, cpuModel,
                              numberOfCores, maxFrequency, memory, hostname)

        self.XMLFileNames = []


    def storeHeaderInXML(self, version, memlimit, timelimit, opSystem,
                         cpuModel, numberOfCores, maxFrequency, memory, hostname):

        # store benchmarkInfo in XML
        self.XMLHeader = ET.Element("result",
                    {"benchmarkname": self.benchmark.name, "date": self.benchmark.dateISO,
                     "tool": self.benchmark.toolName, "version": version})
        if memlimit is not None:
            self.XMLHeader.set(MEMLIMIT, memlimit)
        if timelimit is not None:
            self.XMLHeader.set(TIMELIMIT, timelimit)

        # store systemInfo in XML
        osElem = ET.Element("os", {"name": opSystem})
        cpuElem = ET.Element("cpu",
            {"model": cpuModel, "cores": numberOfCores, "frequency" : maxFrequency})
        ramElem = ET.Element("ram", {"size": memory})

        systemInfo = ET.Element("systeminfo", {"hostname": hostname})
        systemInfo.append(osElem)
        systemInfo.append(cpuElem)
        systemInfo.append(ramElem)
        self.XMLHeader.append(systemInfo)

        # store columnTitles in XML
        columntitlesElem = ET.Element("columns")
        columntitlesElem.append(ET.Element("column", {"title": "status"}))
        columntitlesElem.append(ET.Element("column", {"title": "cputime"}))
        columntitlesElem.append(ET.Element("column", {"title": "walltime"}))
        for column in self.benchmark.columns:
            columnElem = ET.Element("column", {"title": column.title})
            columntitlesElem.append(columnElem)
        self.XMLHeader.append(columntitlesElem)

        # Build dummy entries for output, later replaced by the results,
        # The dummy XML elements are shared over all runs.
        self.XMLDummyElems = [ET.Element("column", {"title": "status", "value": ""}),
                      ET.Element("column", {"title": "cputime", "value": ""}),
                      ET.Element("column", {"title": "walltime", "value": ""})]
        for column in self.benchmark.columns:
            self.XMLDummyElems.append(ET.Element("column",
                        {"title": column.title, "value": ""}))


    def writeHeaderToLog(self, version, memlimit, timelimit, opSystem,
                         cpuModel, numberOfCores, maxFrequency, memory, hostname):
        """
        This method writes information about benchmark and system into TXTFile.
        """

        columnWidth = 20
        simpleLine = "-" * (60) + "\n\n"

        header = "   BENCHMARK INFORMATION\n"\
                + "benchmark:".ljust(columnWidth) + self.benchmark.name + "\n"\
                + "date:".ljust(columnWidth) + self.benchmark.dateISO + "\n"\
                + "tool:".ljust(columnWidth) + self.benchmark.toolName\
                + " " + version + "\n"

        if memlimit is not None:
            header += "memlimit:".ljust(columnWidth) + memlimit + "\n"
        if timelimit is not None:
            header += "timelimit:".ljust(columnWidth) + timelimit + "\n"
        header += simpleLine

        systemInfo = "   SYSTEM INFORMATION\n"\
                + "host:".ljust(columnWidth) + hostname + "\n"\
                + "os:".ljust(columnWidth) + opSystem + "\n"\
                + "cpu:".ljust(columnWidth) + cpuModel + "\n"\
                + "- cores:".ljust(columnWidth) + numberOfCores + "\n"\
                + "- max frequency:".ljust(columnWidth) + maxFrequency + "\n"\
                + "ram:".ljust(columnWidth) + memory + "\n"\
                + simpleLine

        self.description = header + systemInfo

        runSetName = None
        runSets = [runSet for runSet in self.benchmark.runSets if runSet.shouldBeExecuted()]
        if len(runSets) == 1:
            # in case there is only a single run set to to execute, we can use its name
            runSetName = runSets[0].name

        # write to file
        TXTFileName = self.getFileName(runSetName, "txt")
        self.TXTFile = filewriter.FileWriter(TXTFileName, self.description)
        self.allCreatedFiles.append(TXTFileName)

    def getSystemInfo(self):
        """
        This function returns some information about the computer.
        """

        # get info about OS
        (sysname, name, kernel, version, machine) = os.uname()
        opSystem = sysname + " " + kernel + " " + machine

        # get info about CPU
        cpuInfo = dict()
        maxFrequency = 'unknown'
        cpuInfoFilename = '/proc/cpuinfo'
        if os.path.isfile(cpuInfoFilename) and os.access(cpuInfoFilename, os.R_OK):
            cpuInfoFile = open(cpuInfoFilename, "r")
            cpuInfo = dict(tuple(str.split(':')) for str in
                            cpuInfoFile.read()
                            .replace('\n\n', '\n').replace('\t', '')
                            .strip('\n').split('\n'))
            cpuInfoFile.close()
        cpuModel = cpuInfo.get('model name', 'unknown').strip()
        numberOfCores = cpuInfo.get('cpu cores', 'unknown').strip()
        if 'cpu MHz' in cpuInfo:
            maxFrequency = cpuInfo['cpu MHz'].split('.')[0].strip() + ' MHz'

        # modern cpus may not work with full speed the whole day
        # read the number from cpufreq and overwrite maxFrequency from above
        freqInfoFilename = '/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq'
        if os.path.isfile(freqInfoFilename) and os.access(freqInfoFilename, os.R_OK):
            frequencyInfoFile = open(freqInfoFilename, "r")
            maxFrequency = frequencyInfoFile.read().strip('\n')
            frequencyInfoFile.close()
            maxFrequency = str(int(maxFrequency) // 1000) + ' MHz'

        # get info about memory
        memInfo = dict()
        memInfoFilename = '/proc/meminfo'
        if os.path.isfile(memInfoFilename) and os.access(memInfoFilename, os.R_OK):
            memInfoFile = open(memInfoFilename, "r")
            memInfo = dict(tuple(str.split(': ')) for str in
                            memInfoFile.read()
                            .replace('\t', '')
                            .strip('\n').split('\n'))
            memInfoFile.close()
        memTotal = memInfo.get('MemTotal', 'unknown').strip()

        return (opSystem, cpuModel, numberOfCores, maxFrequency, memTotal, name)


    def outputBeforeRunSet(self, runSet):
        """
        The method outputBeforeRunSet() calculates the length of the
        first column for the output in terminal and stores information
        about the runSet in XML.
        @param runSet: current run set
        """

        self.runSet = runSet
        numberOfFiles = len(runSet.runs)

        logging.debug("Run set {0} consists of {1} sourcefiles.".format(
                runSet.index, numberOfFiles))

        sourcefiles = [run.sourcefile for run in runSet.runs]

        # common prefix of file names
        self.commonPrefix = os.path.commonprefix(sourcefiles) # maybe with parts of filename
        self.commonPrefix = self.commonPrefix[: self.commonPrefix.rfind('/') + 1] # only foldername

        # length of the first column in terminal
        self.maxLengthOfFileName = max([len(file) for file in sourcefiles])
        self.maxLengthOfFileName = max(20, self.maxLengthOfFileName - len(self.commonPrefix))

        # write run set name to terminal
        numberOfFiles = ("     (1 file)" if numberOfFiles == 1
                        else "     ({0} files)".format(numberOfFiles))
        Util.printOut("\nexecuting run set"
            + (" '" + runSet.name + "'" if runSet.name else "")
            + numberOfFiles
            + (TERMINAL_TITLE.format(runSet.fullName) if USE_COLORS and sys.stdout.isatty() else ""))

        # write information about the run set into TXTFile
        self.writeRunSetInfoToLog(runSet)

        # prepare information for text output
        for run in runSet.runs:
            run.resultline = self.formatSourceFileName(run.sourcefile)

        # prepare XML structure for each run and runSet
            run.xml = ET.Element("sourcefile", {"name": run.sourcefile})
            if run.specificOptions:
                run.xml.set("options", " ".join(run.specificOptions))
            run.xml.extend(self.XMLDummyElems)

        runSet.xml = self.runsToXML(runSet, runSet.runs)

        # write (empty) results to TXTFile and XML
        self.TXTFile.append(self.runSetToTXT(runSet), False)
        XMLFileName = self.getFileName(runSet.name, "xml")
        self.XMLFile = filewriter.FileWriter(XMLFileName,
                       Util.XMLtoString(runSet.xml))
        self.XMLFile.lastModifiedTime = time.time()
        self.allCreatedFiles.append(XMLFileName)
        self.XMLFileNames.append(XMLFileName)


    def outputForSkippingRunSet(self, runSet, reason=None):
        '''
        This function writes a simple message to terminal and logfile,
        when a run set is skipped.
        There is no message about skipping a run set in the xml-file.
        '''

        # print to terminal
        Util.printOut("\nSkipping run set" +
               (" '" + runSet.name + "'" if runSet.name else "") +
               (" " + reason if reason else "")
              )

        # write into TXTFile
        runSetInfo = "\n\n"
        if runSet.name:
            runSetInfo += runSet.name + "\n"
        runSetInfo += "Run set {0} of {1}: skipped {2}\n".format(
                runSet.index, len(self.benchmark.runSets), reason or "")
        self.TXTFile.append(runSetInfo)


    def writeRunSetInfoToLog(self, runSet):
        """
        This method writes the information about a run set into the TXTFile.
        """

        runSetInfo = "\n\n"
        if runSet.name:
            runSetInfo += runSet.name + "\n"
        runSetInfo += "Run set {0} of {1} with options: {2}\n\n".format(
                runSet.index, len(self.benchmark.runSets),
                " ".join(runSet.options))

        titleLine = self.createOutputLine("sourcefile", "status", "cpu time",
                            "wall time", self.benchmark.columns, True)

        runSet.simpleLine = "-" * (len(titleLine))

        runSetInfo += titleLine + "\n" + runSet.simpleLine + "\n"

        # write into TXTFile
        self.TXTFile.append(runSetInfo)


    def outputBeforeRun(self, run):
        """
        The method outputBeforeRun() prints the name of a file to terminal.
        It returns the name of the logfile.
        @param run: a Run object
        """
        # output in terminal
        try:
            OutputHandler.printLock.acquire()

            timeStr = time.strftime("%H:%M:%S", time.localtime()) + "   "
            if self.benchmark.numOfThreads == 1:
                progressIndicator = " ({0}/{1})".format(self.runSet.runs.index(run), len(self.runSet.runs))
                terminalTitle = TERMINAL_TITLE.format(self.runSet.fullName + progressIndicator) if USE_COLORS and sys.stdout.isatty() else ""
                Util.printOut(terminalTitle
                              + timeStr + self.formatSourceFileName(run.sourcefile), '')
            else:
                Util.printOut(timeStr + "starting   " + self.formatSourceFileName(run.sourcefile))
        finally:
            OutputHandler.printLock.release()

        # get name of file-specific log-file
        self.allCreatedFiles.append(run.logFile)


    def outputAfterRun(self, run):
        """
        The method outputAfterRun() prints filename, result, time and status
        of a run to terminal and stores all data in XML
        """

        # format times, type is changed from float to string!
        cpuTimeStr = Util.formatNumber(run.cpuTime, TIME_PRECISION)
        wallTimeStr = Util.formatNumber(run.wallTime, TIME_PRECISION)

        # format numbers, numberOfDigits is optional, so it can be None
        for column in run.columns:
            if column.numberOfDigits is not None:

                # if the number ends with "s" or another letter, remove it
                if (not column.value.isdigit()) and column.value[-2:-1].isdigit():
                    column.value = column.value[:-1]

                try:
                    floatValue = float(column.value)
                    column.value = Util.formatNumber(floatValue, column.numberOfDigits)
                except ValueError: # if value is no float, don't format it
                    pass

        # store information in run
        run.resultline = self.createOutputLine(run.sourcefile, run.status,
                cpuTimeStr, wallTimeStr, run.columns)
        self.addValuesToRunXML(run, cpuTimeStr, wallTimeStr)

        # output in terminal/console
        statusRelation = result.getResultCategory(run.sourcefile, run.status)
        if USE_COLORS and sys.stdout.isatty(): # is terminal, not file
            statusStr = COLOR_DIC[statusRelation].format(run.status.ljust(8))
        else:
            statusStr = run.status.ljust(8)

        try:
            OutputHandler.printLock.acquire()

            valueStr = statusStr + cpuTimeStr.rjust(8) + wallTimeStr.rjust(8)
            if self.benchmark.numOfThreads == 1:
                Util.printOut(valueStr)
            else:
                timeStr = time.strftime("%H:%M:%S", time.localtime()) + " "*14
                Util.printOut(timeStr + self.formatSourceFileName(run.sourcefile) + valueStr)

            # write result in TXTFile and XML
            self.TXTFile.append(self.runSetToTXT(run.runSet), False)
            self.statistics.addResult(statusRelation)

            # we don't want to write this file to often, it can slow down the whole script,
            # so we wait at least 10 seconds between two write-actions
            currentTime = time.time()
            if currentTime - self.XMLFile.lastModifiedTime > 10:
                self.XMLFile.replace(Util.XMLtoString(run.runSet.xml))
                self.XMLFile.lastModifiedTime = currentTime

        finally:
            OutputHandler.printLock.release()


    def outputAfterRunSet(self, runSet, cpuTime, wallTime):
        """
        The method outputAfterRunSet() stores the times of a run set in XML.
        @params cpuTime, wallTime: accumulated times of the run set
        """

        # write results to files
        self.XMLFile.replace(Util.XMLtoString(runSet.xml))

        if len(runSet.blocks) > 1:
            for block in runSet.blocks:
                blockFileName = self.getFileName(runSet.name, block.name + ".xml")
                filewriter.writeFile(blockFileName,
                    Util.XMLtoString(self.runsToXML(runSet, block.runs, block.name)))
                self.allCreatedFiles.append(blockFileName)

        self.TXTFile.append(self.runSetToTXT(runSet, True, cpuTime, wallTime))


    def runSetToTXT(self, runSet, finished=False, cpuTime=0, wallTime=0):
        lines = []

        # store values of each run
        for run in runSet.runs: lines.append(run.resultline)

        lines.append(runSet.simpleLine)

        # write endline into TXTFile
        if finished:
            endline = ("Run set {0}".format(runSet.index))

            # format time, type is changed from float to string!
            if(cpuTime == None):
                cpuTimeStr = str(cpuTime)
            else:
                cpuTimeStr = Util.formatNumber(cpuTime, TIME_PRECISION)
            if(wallTime == None):
                wallTimeStr = str(wallTime)
            else:
                wallTimeStr = Util.formatNumber(wallTime, TIME_PRECISION)

            lines.append(self.createOutputLine(endline, "done", cpuTimeStr,
                             wallTimeStr, []))

        return "\n".join(lines) + "\n"

    def runsToXML(self, runSet, runs, blockname=None):
        """
        This function creates the XML structure for a list of runs
        """
        # copy benchmarkinfo, limits, columntitles, systeminfo from XMLHeader
        runsElem = Util.getCopyOfXMLElem(self.XMLHeader)
        runsElem.set("options", " ".join(runSet.options))
        if blockname is not None:
            runsElem.set("block", blockname)
            runsElem.set("name", ((runSet.realName + ".") if runSet.realName else "") + blockname)
        elif runSet.realName:
            runsElem.set("name", runSet.realName)

        # collect XMLelements from all runs
        for run in runs: runsElem.append(run.xml)

        return runsElem


    def addValuesToRunXML(self, run, cpuTimeStr, wallTimeStr):
        """
        This function adds the result values to the XML representation of a run.
        """
        runElem = run.xml
        for elem in list(runElem):
            runElem.remove(elem)
        runElem.append(ET.Element("column", {"title": "status", "value": run.status}))
        runElem.append(ET.Element("column", {"title": "cputime", "value": cpuTimeStr}))
        runElem.append(ET.Element("column", {"title": "walltime", "value": wallTimeStr}))
        if run.memUsage is not None:
            runElem.append(ET.Element("column", {"title": "memUsage", "value": run.memUsage}))

        for column in run.columns:
            runElem.append(ET.Element("column",
                        {"title": column.title, "value": column.value}))


    def createOutputLine(self, sourcefile, status, cpuTimeDelta, wallTimeDelta, columns, isFirstLine=False):
        """
        @param sourcefile: title of a sourcefile
        @param status: status of programm
        @param cpuTimeDelta: time from running the programm
        @param wallTimeDelta: time from running the programm
        @param columns: list of columns with a title or a value
        @param isFirstLine: boolean for different output of headline and other lines
        @return: a line for the outputFile
        """

        lengthOfStatus = 8
        lengthOfTime = 11
        minLengthOfColumns = 8

        outputLine = self.formatSourceFileName(sourcefile) + \
                     status.ljust(lengthOfStatus) + \
                     cpuTimeDelta.rjust(lengthOfTime) + \
                     wallTimeDelta.rjust(lengthOfTime)

        for column in columns:
            columnLength = max(minLengthOfColumns, len(column.title)) + 2

            if isFirstLine:
                value = column.title
            else:
                value = column.value

            outputLine = outputLine + str(value).rjust(columnLength)

        return outputLine


    def outputAfterBenchmark(self):
        self.statistics.printToTerminal()

        if self.XMLFileNames:
            Util.printOut("In order to get HTML and CSV tables, run\n{0} '{1}'"
                          .format(os.path.join(os.path.dirname(__file__), 'table-generator.py'),
                                  "' '".join(self.XMLFileNames)))

        if STOPPED_BY_INTERRUPT:
            Util.printOut("\nScript was interrupted by user, some runs may not be done.\n")


    def getFileName(self, runSetName, fileExtension):
        '''
        This function returns the name of the file for a run set
        with an extension ("txt", "xml").
        '''

        fileName = self.benchmark.outputBase + ".results."

        if runSetName:
            fileName += runSetName + "."

        return fileName + fileExtension


    def formatSourceFileName(self, fileName):
        '''
        Formats the file name of a program for printing on console.
        '''
        fileName = fileName.replace(self.commonPrefix, '', 1)
        return fileName.ljust(self.maxLengthOfFileName + 4)


class Statistics:

    def __init__(self):
        self.dic = {"counter": 0,
                    "correctSafe": 0,
                    "correctUnsafe": 0,
                    "unknown": 0,
                    "wrongUnsafe": 0,
                    "wrongSafe": 0,
                    None: 0}


    def addResult(self, statusRelation):
        self.dic["counter"] += 1
        if statusRelation == 'error':
            statusRelation = 'unknown'
        assert statusRelation in self.dic
        self.dic[statusRelation] += 1


    def printToTerminal(self):
        Util.printOut('\n'.join(['\nStatistics:' + str(self.dic["counter"]).rjust(13) + ' Files',
                 '    correct:        ' + str(self.dic["correctSafe"] + \
                                              self.dic["correctUnsafe"]).rjust(4),
                 '    unknown:        ' + str(self.dic["unknown"]).rjust(4),
                 '    false positives:' + str(self.dic["wrongUnsafe"]).rjust(4) + \
                 '        (file is safe, result is unsafe)',
                 '    false negatives:' + str(self.dic["wrongSafe"]).rjust(4) + \
                 '        (file is unsafe, result is safe)',
                 '']))


def getOptionsFromXML(optionsTag):
    '''
    This function searches for options in a tag
    and returns a list with command-line arguments.
    '''
    return Util.toSimpleList([(option.get("name"), option.text)
               for option in optionsTag.findall("option")])


def substituteVars(oldList, runSet, sourcefile=None):
    """
    This method replaces special substrings from a list of string
    and return a new list.
    """

    benchmark = runSet.benchmark

    # list with tuples (key, value): 'key' is replaced by 'value'
    keyValueList = [('${benchmark_name}', benchmark.name),
                    ('${benchmark_date}', benchmark.date),
                    ('${benchmark_path}', os.path.dirname(benchmark.benchmarkFile)),
                    ('${benchmark_path_abs}', os.path.abspath(os.path.dirname(benchmark.benchmarkFile))),
                    ('${benchmark_file}', os.path.basename(benchmark.benchmarkFile)),
                    ('${benchmark_file_abs}', os.path.abspath(os.path.basename(benchmark.benchmarkFile))),
                    ('${logfile_path}',   os.path.dirname(runSet.logFolder)),
                    ('${logfile_path_abs}', os.path.abspath(runSet.logFolder)),
                    ('${rundefinition_name}', runSet.realName if runSet.realName else ''),
                    ('${test_name}',      runSet.realName if runSet.realName else '')]

    if sourcefile:
        keyValueList.append(('${sourcefile_name}', os.path.basename(sourcefile)))
        keyValueList.append(('${sourcefile_path}', os.path.dirname(sourcefile)))
        keyValueList.append(('${sourcefile_path_abs}', os.path.dirname(os.path.abspath(sourcefile))))

    # do not use keys twice
    assert len(set((key for (key, value) in keyValueList))) == len(keyValueList)

    newList = []

    for oldStr in oldList:
        newStr = oldStr
        for (key, value) in keyValueList:
            newStr = newStr.replace(key, value)
        if '${' in newStr:
            logging.warn("a variable was not replaced in '{0}'".format(newStr))
        newList.append(newStr)

    return newList



class Worker(threading.Thread):
    """
    A Worker is a deamonic thread, that takes jobs from the workingQueue and runs them.
    """
    workingQueue = Queue.Queue()

    def __init__(self, number):
        threading.Thread.__init__(self) # constuctor of superclass
        self.number = number
        self.setDaemon(True)
        self.start()

    def run(self):
        while not Worker.workingQueue.empty() and not STOPPED_BY_INTERRUPT:
            currentRun = Worker.workingQueue.get_nowait()
            currentRun.execute(self.number)
            Worker.workingQueue.task_done()
            
def executeBenchmarkLocaly(benchmark):
    outputHandler = benchmark.outputHandler
    runSetsExecuted = 0

    logging.debug("I will use {0} threads.".format(benchmark.numOfThreads))

    # iterate over run sets
    for runSet in benchmark.runSets:

        if STOPPED_BY_INTERRUPT: break

        (mod, rest) = config.moduloAndRest

        if not runSet.shouldBeExecuted() \
                or (runSet.index % mod != rest):
            outputHandler.outputForSkippingRunSet(runSet)

        elif not runSet.runs:
            outputHandler.outputForSkippingRunSet(runSet, "because it has no files")

        else:
            runSetsExecuted += 1
            # get times before runSet
            ruBefore = resource.getrusage(resource.RUSAGE_CHILDREN)
            wallTimeBefore = time.time()

            outputHandler.outputBeforeRunSet(runSet)

            # put all runs into a queue
            for run in runSet.runs:
                Worker.workingQueue.put(run)

            # create some workers
            for i in range(benchmark.numOfThreads):
                WORKER_THREADS.append(Worker(i))

            # wait until all tasks are done,
            # instead of queue.join(), we use a loop and sleep(1) to handle KeyboardInterrupt
            finished = False
            while not finished and not STOPPED_BY_INTERRUPT:
                try:
                    Worker.workingQueue.all_tasks_done.acquire()
                    finished = (Worker.workingQueue.unfinished_tasks == 0)
                finally:
                    Worker.workingQueue.all_tasks_done.release()

                try:
                    time.sleep(0.1) # sleep some time
                except KeyboardInterrupt:
                    killScript()

            # get times after runSet
            wallTimeAfter = time.time()
            usedWallTime = wallTimeAfter - wallTimeBefore
            ruAfter = resource.getrusage(resource.RUSAGE_CHILDREN)
            usedCpuTime = (ruAfter.ru_utime + ruAfter.ru_stime) \
                        - (ruBefore.ru_utime + ruBefore.ru_stime)

            outputHandler.outputAfterRunSet(runSet, usedCpuTime, usedWallTime)

    outputHandler.outputAfterBenchmark()

    if config.commit and not STOPPED_BY_INTERRUPT and runSetsExecuted > 0:
        Util.addFilesToGitRepository(OUTPUT_PATH, outputHandler.allCreatedFiles,
                                     config.commitMessage+'\n\n'+outputHandler.description)


def parseCloudResultFile(filePath):
    try:
        file = open(filePath)
            
        command = file.readline()
        wallTime = float(file.readline().split(":")[-1])
        cpuTime = float(file.readline().split(":")[-1])
        returnValue = int(file.readline().split(":")[-1])
    
        output = "".join(file.readlines())
     
        file.close
        return (wallTime, cpuTime, returnValue, output)
    
    except IOError:
        logging.warning("Result file not found: " + filePath)
        return (0,0,1,"")
 
def executeBenchmarkInCloud(benchmark):
    
    outputHandler = benchmark.outputHandler

    absWorkingDir = os.path.abspath(os.curdir)
    logging.debug("Working dir: " + absWorkingDir)
    toolpaths = benchmark.tool.getProgrammFiles(benchmark.executable)
    requirements = "2000\t1"  # TODO memory numerOfCpuCores
    cloudRunExecutorDir = os.path.abspath("./scripts")
    outputDir = os.path.join(OUTPUT_PATH , benchmark.name + "." + benchmark.date + ".logfiles")
    logging.debug("Output path: " + str(outputDir))
    absOutputDir = os.path.abspath(outputDir)
    if(not(os.access(absOutputDir, os.F_OK))):
        os.makedirs(absOutputDir)                   
    
    runDefinitions = ""
    absSourceFiles = []
    numOfRunDefLines = 0
    
    # iterate over run sets
    for runSet in benchmark.runSets:
        
        if STOPPED_BY_INTERRUPT: break
        
        numOfRunDefLines += (len(runSet.runs) + 1)
        
        runSetHeadLine = str(len(runSet.runs)) + "\t" + \
                        runSet.name + "\t" + \
                        str(benchmark.rlimits[TIMELIMIT]) + "\t" + \
                       str(benchmark.rlimits[MEMLIMIT]) + "\n"
         
        runDefinitions += runSetHeadLine;
        
        # iterate over runs
        for run in runSet.runs:
                argString = " ".join(run.args)
                runDefinitions += argString + "\t" + run.sourcefile + "\n"
                absSourceFiles.append(os.path.abspath(run.sourcefile))
    
    if(len(absSourceFiles)==0):
        sys.exit("No source files given.")                              
    
    #preparing cloud input
    absToolpaths = []
    for toolpath in toolpaths:
        absToolpaths.append(os.path.abspath(toolpath))
    seperatedToolpaths = "\t".join(absToolpaths)
    sourceFilesBaseDir = os.path.commonprefix(absSourceFiles)
    logging.debug("source files dir: " + sourceFilesBaseDir)
    toolPathsBaseDir = os.path.commonprefix(absToolpaths)
    logging.debug("tool paths base dir: " + toolPathsBaseDir)
    baseDir = os.path.commonprefix([sourceFilesBaseDir, toolPathsBaseDir, cloudRunExecutorDir])
    logging.debug("base dir: " + baseDir)

    if(baseDir == ""):
        sys.exit("No common base dir found.")

    cloudInput = seperatedToolpaths + "\n" + \
                cloudRunExecutorDir + "\n" + \
                baseDir + "\t" + absOutputDir + "\t" + absWorkingDir +"\n" + \
                requirements + "\n" + \
                str(numOfRunDefLines) + "\n" + \
                runDefinitions
                
     # start cloud and wait for exit
    logging.debug("Starting cloud.")
    if(config.debug):
        logLevel =  "ALL"
    else:
        logLevel = "INFO"
    cloud = subprocess.Popen(["java", "-jar", config.cloudPath, "benchmark", "--master", config.cloudMasterName, "--loglevel", logLevel], stdin=subprocess.PIPE)
    (out, err) = cloud.communicate(cloudInput)
    returnCode = cloud.wait()
    logging.debug("Cloud return code: {0}".format(returnCode))
    
    #write results in runs and
    #handle output after all runs are done
    for runSet in benchmark.runSets:
        outputHandler.outputBeforeRunSet(runSet)     
        for run in runSet.runs:
            outputHandler.outputBeforeRun(run)
            (notUsed,sourceFileName) = os.path.split(run.sourcefile)
            file = os.path.join(outputDir, runSet.name + "." + sourceFileName + ".log")
            (run.wallTime, run.cpuTime, returnValue, output) = parseCloudResultFile(file)
            run.afterExecution(returnValue, output)
        outputHandler.outputAfterRunSet(runSet, None, None)
        
    outputHandler.outputAfterBenchmark()


def executeBenchmark(benchmarkFile):
    benchmark = Benchmark(benchmarkFile)

    logging.debug("I'm benchmarking {0} consisting of {1} run sets.".format(
            repr(benchmarkFile), len(benchmark.runSets)))
    
    if(config.cloud):
        executeBenchmarkInCloud(benchmark)
    else:
        executeBenchmarkLocaly(benchmark)


def main(argv=None):

    if argv is None:
        argv = sys.argv
    parser = argparse.ArgumentParser(description=
        """Run benchmarks with a verification tool.
        Documented example files for the benchmark definitions
        can be found as 'doc/examples/benchmark*.xml'.
        Use the table-generator.py script to create nice tables
        from the output of this script.""")

    parser.add_argument("files", nargs='+', metavar="FILE",
                      help="XML file with benchmark definition")
    parser.add_argument("-d", "--debug",
                      action="store_true",
                      help="Enable debug output")

    parser.add_argument("-r", "--rundefinition", dest="selectedRunDefinitions",
                      action="append",
                      help="Run only the specified RUN_DEFINITION from the benchmark definition file. "
                            + "This option can be specified several times.",
                      metavar="RUN_DEFINITION")

    parser.add_argument("-t", "--test", dest="selectedRunDefinitions",
                      action="append",
                      help="Same as -r/--rundefinition (deprecated)",
                      metavar="TEST")

    parser.add_argument("-s", "--sourcefiles", dest="selectedSourcefileSets",
                      action="append",
                      help="Run only the files from the sourcefiles tag with SOURCE as name. "
                            + "This option can be specified several times.",
                      metavar="SOURCES")

    parser.add_argument("-n", "--name",
                      dest="name", default=None,
                      help="Set name of benchmark execution to NAME",
                      metavar="NAME")

    parser.add_argument("-o", "--outputpath",
                      dest="output_path", type=str,
                      default="./test/results/",
                      help="Output prefix for the generated results. "
                            + "If the path is a folder files are put into it,"
                            + "otherwise it is used as a prefix for the resulting files.")

    parser.add_argument("-T", "--timelimit",
                      dest="timelimit", default=None,
                      help="Time limit in seconds for each run (-1 to disable)",
                      metavar="SECONDS")

    parser.add_argument("-M", "--memorylimit",
                      dest="memorylimit", default=None,
                      help="Memory limit in MB (-1 to disable)",
                      metavar="MB")

    parser.add_argument("-N", "--numOfThreads",
                      dest="numOfThreads", default=None, type=int,
                      help="Run n benchmarks in parallel",
                      metavar="n")

    parser.add_argument("-x", "--moduloAndRest",
                      dest="moduloAndRest", default=(1,0), nargs=2, type=int,
                      help="Run only a subset of run definitions for which (i %% a == b) holds" +
                            "with i being the index of the run definition in the benchmark definition file " +
                            "(starting with 1).",
                      metavar=("a","b"))

    parser.add_argument("-c", "--limitCores", dest="limitCores",
                      type=int, default=None,
                      metavar="N",
                      help="Limit each run of the tool to N CPU cores.")

    parser.add_argument("--commit", dest="commit",
                      action="store_true",
                      help="If the output path is a git repository without local changes,"
                            + "add and commit the result files.")

    parser.add_argument("--message",
                      dest="commitMessage", type=str,
                      default="Results for benchmark run",
                      help="Commit message if --commit is used.")
    
    parser.add_argument("--cloud",
                      dest="cloud",
                      action="store_true",
                      help="Use cloud.")

    parser.add_argument("--cloudPath",
                      dest="cloudPath",
                     default="vecip.jar",
                      help="The path to jar file of cloud client.")

    parser.add_argument("--cloudMaster",
                      dest="cloudMasterName",
                      default="localhost",
                      help="Use given cloud master if -cloud is used.")

    global config, OUTPUT_PATH
    config = parser.parse_args(argv[1:])
    if os.path.isdir(config.output_path):
        OUTPUT_PATH = os.path.normpath(config.output_path) + os.sep
    else:
        OUTPUT_PATH = config.output_path


    if config.debug:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                            level=logging.DEBUG)
    else:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s")

    for arg in config.files:
        if not os.path.exists(arg) or not os.path.isfile(arg):
            parser.error("File {0} does not exist.".format(repr(arg)))

    try:
        processes = subprocess.Popen(['ps', '-eo', 'cmd'], stdout=subprocess.PIPE).communicate()[0]
        if len(re.findall("python.*benchmark\.py", Util.decodeToString(processes))) > 1:
            logging.warn("Already running instance of this script detected. " + \
                         "Please make sure to not interfere with somebody else's benchmarks.")
    except OSError:
        pass # this does not work on Windows

    # do this after logger has been configured
    if(not config.cloud):
        runexecutor.init(config.limitCores)

    for arg in config.files:
        if STOPPED_BY_INTERRUPT: break
        logging.debug("Benchmark {0} is started.".format(repr(arg)))
        executeBenchmark(arg)
        logging.debug("Benchmark {0} is done.".format(repr(arg)))

    logging.debug("I think my job is done. Have a nice day!")


def killScript():
        # set global flag
        global STOPPED_BY_INTERRUPT
        STOPPED_BY_INTERRUPT = True

        # kill running jobs
        Util.printOut("killing subprocesses...")
        runexecutor.killAllProcesses()

        # wait until all threads are stopped
        for worker in WORKER_THREADS:
            worker.join()


def signal_handler_ignore(signum, frame):
    logging.warn('Received signal %d, ignoring it' % signum)

if __name__ == "__main__":
    # ignore SIGTERM
    signal.signal(signal.SIGTERM, signal_handler_ignore)
    try:
        sys.exit(main())
    except KeyboardInterrupt: # this block is reached, when interrupt is thrown before or after a run set execution
        killScript()
        Util.printOut("\n\nScript was interrupted by user, some runs may not be done.")
