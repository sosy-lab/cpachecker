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
import platform
import re
import resource
import signal
import subprocess
import threading
import xml.etree.ElementTree as ET


CSV_SEPARATOR = "\t"

BUG_SUBSTRING_LIST = ['bug', 'unsafe']

MEMLIMIT = "memlimit"
TIMELIMIT = "timelimit"
BYTE_FACTOR = 1024 # byte in kilobyte

# colors for column status in terminal
USE_COLORS = True
COLOR_GREEN = "\033[32;1m{0}\033[m"
COLOR_RED = "\033[31;1m{0}\033[m"
COLOR_ORANGE = "\033[33;1m{0}\033[m"
COLOR_MAGENTA = "\033[35;1m{0}\033[m"
COLOR_DIC = {"correctSafe": COLOR_GREEN,
             "correctUnsafe": COLOR_GREEN,
             "unknown": COLOR_ORANGE,
             "error": COLOR_MAGENTA,
             "wrongUnsafe": COLOR_RED,
             "wrongSafe": COLOR_RED}


# dictionary for tools and their default and fallback executables
TOOLS = {"cbmc"      : ["cbmc",
                      "lib/native/x86_64-linux/cbmc" if platform.machine() == "x86_64" else \
                      "lib/native/x86-linux/cbmc"    if platform.machine() == "i386" else None],
         "satabs"    : ["satabs"],
         "wolverine" : ["wolverine"],
         "ufo"       : ["ufo.sh"],
         "acsar"     : ["acsar"],
         "feaver"    : ["feaver_cmd"],
         "cpachecker": ["cpa.sh", "scripts/cpa.sh"],
         "blast"     : ["pblast.opt"],
         "ecav"      : ["ecaverifier"],
         "safe"      : [],
         "unsafe"    : [],
         "random"    : [],
        }


# the number of digits after the decimal separator of the time column,
# for the other columns it can be configured in the xml-file
TIME_PRECISION = 2


# next lines are needed for stopping the script
WORKER_THREADS = []
SUB_PROCESSES = set()
SUB_PROCESSES_LOCK = threading.Lock()
STOPPED_BY_INTERRUPT = False


class Benchmark:
    """
    The class Benchmark manages the import of files, options, columns and
    the tool from a benchmarkFile.
    """

    def __init__(self, benchmarkFile):
        """
        The constructor of Benchmark reads the files, options, columns and the tool
        from the xml-file.
        """
        logging.debug("I'm loading the benchmark {0}.".format(benchmarkFile))

        self.benchmarkFile = benchmarkFile
        root = ET.ElementTree().parse(benchmarkFile)

        # get benchmark-name
        self.name = os.path.basename(benchmarkFile)[:-4] # remove ending ".xml"

        # get current date as String to avoid problems, if script runs over midnight
        currentTime = time.localtime()
        self.date = time.strftime("%y-%m-%d_%H%M", currentTime)
        self.dateISO = time.strftime("%y-%m-%d %H:%M", currentTime)

        # get tool
        self.tool = root.get("tool")
        if self.tool not in TOOLS.keys() :
            sys.exit("tool '{0}' is not supported".format(self.tool))
        self.exe = findExecutable(*TOOLS[self.tool])
        self.run_func = eval("run_" + self.tool)

        logging.debug("The tool to be benchmarked is {0}.".format(repr(self.tool)))

        self.rlimits = {}
        keys = list(root.keys())
        if MEMLIMIT in keys:
            self.rlimits[MEMLIMIT] = int(root.get(MEMLIMIT))
        if TIMELIMIT in keys:
            self.rlimits[TIMELIMIT] = int(root.get(TIMELIMIT))

        # override limits from xml with option-given limits
        if options.memorylimit != None:
            memorylimit = int(options.memorylimit)
            if memorylimit == -1: # infinity
                if MEMLIMIT in self.rlimits:
                    self.rlimits.pop(MEMLIMIT)                
            else:
                self.rlimits[MEMLIMIT] = memorylimit

        if options.timelimit != None:
            timelimit = int(options.timelimit)
            if timelimit == -1: # infinity
                if TIMELIMIT in self.rlimits:
                    self.rlimits.pop(TIMELIMIT)                
            else:
                self.rlimits[TIMELIMIT] = timelimit

        # get number of threads, default value is 1
        self.numOfThreads = int(root.get("threads")) if ("threads" in keys) else 1
        if options.numOfThreads != None:
            self.numOfThreads = int(options.numOfThreads)
        if self.numOfThreads < 1:
            logging.error("At least ONE thread must be given!")
            sys.exit()

        # get global options
        self.options = getOptions(root)

        # get columns
        self.columns = self.loadColumns(root.find("columns"))

        # get global files, they are tested in all tests
        globalSourcefiles = root.findall("sourcefiles")

        # get benchmarks
        self.tests = []
        for testTag in root.findall("test"):
            self.tests.append(Test(testTag, self, globalSourcefiles))

        self.outputHandler = OutputHandler(self)


    def loadColumns(self, columnsTag):
        """
        @param columnsTag: the columnsTag from the xml-file
        @return: a list of Columns()
        """

        logging.debug("I'm loading some columns for the outputfile.")
        columns = []
        if columnsTag != None: # columnsTag is optional in xml-file
            for columnTag in columnsTag.findall("column"):
                text = columnTag.text
                title = columnTag.get("title", text)
                numberOfDigits = columnTag.get("numberOfDigits") # digits behind comma
                column = Column(text, title, numberOfDigits)
                columns.append(column)
                logging.debug('Column "{0}" with title "{1}" loaded from xml-file.'
                          .format(column.text, column.title))
        return columns


class Test:
    """
    The class Test manages the import of files and options of a test.
    """

    def __init__(self, testTag, benchmark, globalSourcefileTags=[]):
        """
        The constructor of Test reads testname and the filenames from testTag.
        Filenames can be included or excluded, and imported from a list of
        names in another file. Wildcards and variables are expanded.
        @param testTag: a testTag from the xml-file
        """

        self.benchmark = benchmark

        # get name of test, name is optional, the result can be "None"
        self.name = testTag.get("name")

        # get all test-specific options from testTag
        self.options = getOptions(testTag)

        # get all runs, a run contains one sourcefile with options
        self.getRuns(globalSourcefileTags + testTag.findall("sourcefiles"))


    def getRuns(self, sourcefilesTagList):
        '''
        This function builds a list of Runs (filename with options).
        The files and their options are taken from the list of sourcefilesTags.
        '''
        # runs are structured as blocks, one block represents one soursefile-tag
        self.blocks = []
        self.runs = []

        for sourcefilesTag in sourcefilesTagList:
            # get list of filenames
            sourcefiles = self.getSourcefiles(sourcefilesTag)

            # get file-specific options for filenames
            fileOptions = getOptions(sourcefilesTag)

            runs = []
            for sourcefile in sourcefiles:
                runs.append(Run(sourcefile, fileOptions, self))
            self.runs.extend(runs)

            blockName = sourcefilesTag.get("name", str(sourcefilesTagList.index(sourcefilesTag)))
            self.blocks.append(Block(blockName,runs))


    def getSourcefiles(self, sourcefilesTag):
        sourcefiles = []
        baseDir = os.path.dirname(self.benchmark.benchmarkFile)

        # get included sourcefiles
        for includedFiles in sourcefilesTag.findall("include"):
            sourcefiles += self.getFileList(includedFiles.text, baseDir)

        # get sourcefiles from list in file
        for includesFilesFile in sourcefilesTag.findall("includesfile"):

            for file in self.getFileList(includesFilesFile.text, baseDir):

                # check for code (if somebody changes 'include' and 'includesfile')
                if Util.isCode(file):
                    logging.error("'" + file + "' is no includesfile (set-file).\n" + \
                        "please check your benchmark-xml-file or remove bracket '{' from this file.")
                    sys.exit()

                # read files from list
                fileWithList = open(file, "r")
                for line in fileWithList:

                    # strip() removes 'newline' behind the line
                    line = line.strip()

                    # ignore comments and empty lines
                    if not Util.isComment(line):
                        sourcefiles += self.getFileList(line, os.path.dirname(file))

                fileWithList.close()

        # remove excluded sourcefiles
        for excludedFiles in sourcefilesTag.findall("exclude"):
            excludedFilesList = self.getFileList(excludedFiles.text, baseDir)
            for excludedFile in excludedFilesList:
                sourcefiles = Util.removeAll(sourcefiles, excludedFile)

        return sourcefiles


    def getFileList(self, shortFile, root):
        """
        The function getFileList expands a short filename to a sorted list
        of filenames. The short filename can contain variables and wildcards.
        If root is given and shortFile is not absolute, root and shortFile are joined.
        """

        # store shortFile for fallback
        shortFileFallback = shortFile

        # replace vars like ${benchmark_path},
        # with converting to list and back, we can use the function 'substituteVars()'
        shortFileList = substituteVars([shortFile], self)
        assert len(shortFileList) == 1
        shortFile = shortFileList[0]

        # 'join' ignores root, if shortFile is absolute.
        # 'normpath' replaces 'A/foo/../B' with 'A/B', for pretty printing only
        shortFile = os.path.normpath(os.path.join(root, shortFile))

        # expand tilde and variables
        expandedFile = os.path.expandvars(os.path.expanduser(shortFile))

        # expand wildcards
        fileList = glob.glob(expandedFile)

        # sort alphabetical,
        # if list is emtpy, sorting returns None, so better do not sort
        if len(fileList) != 0:
            fileList.sort()

        if expandedFile != shortFile:
            logging.debug("Expanded tilde and/or shell variables in expression {0} to {1}."
                .format(repr(shortFile), repr(expandedFile)))

        if len(fileList) == 0 and root != "":

            if root != "":
                # try fallback for older test-sets
                fileList = self.getFileList(shortFileFallback, "")
                if len(fileList) != 0:
                    logging.warning("Test definition uses old-style paths. Please change the path {0} to be relative to {1}."
                                .format(repr(shortFileFallback), repr(root)))
                else:
                    logging.warning("No files found matching {0}."
                                .format(repr(shortFile)))

        return fileList


class Block():
    """
    A Block contains a list of runs and a name.
    """
    def __init__(self, name, runs):
        self.name = name
        self.runs = runs


class Run():
    """
    A Run contains one sourcefile and options.
    """

    def __init__(self, sourcefile, fileOptions, test):
        self.sourcefile = sourcefile
        self.options = fileOptions
        self.test = test
        self.benchmark = test.benchmark
        self.mergedOptions = []

        # copy columns for having own objects in run
        self.columns = [Column(c.text, c.title, c.numberOfDigits) for c in self.benchmark.columns]

        # dummy values, for output in case of interrupt
        self.resultline = None
        self.status = ""
        self.cpuTime = 0
        self.cpuTimeStr = ""
        self.wallTime = 0
        self.wallTimeStr = ""
        self.args = ""

    def getMergedOptions(self):
        """
        This function returns a list of Strings.
        It contains all options for this Run (global + testwide + local) without 'None'-Values.
        """

        if not self.mergedOptions: # cache mergeOptions
            # merge options to list
            currentOptions = mergeOptions(self.benchmark.options,
                                      self.test.options,
                                      self.options)

            # replace variables with special values
            currentOptions = substituteVars(currentOptions,
                                        self.test,
                                        self.sourcefile,
                                        self.benchmark.outputHandler.logFolder)
            self.mergedOptions = currentOptions

        return self.mergedOptions


    def run(self, numberOfThread):
        """
        This function runs the tool with a sourcefile with options.
        It also calls functions for output before and after the run.
        @param numberOfThread: runs are executed in different threads
        """
        logfile = self.benchmark.outputHandler.outputBeforeRun(self)

        (self.status, self.cpuTime, self.wallTime, self.args) = \
             self.benchmark.run_func(self.benchmark.exe,
                                     self.getMergedOptions(),
                                     self.sourcefile, 
                                     self.columns,
                                     self.benchmark.rlimits,
                                     numberOfThread,
                                     logfile)

        # Tools sometimes produce a result even after a timeout.
        # This should not be counted, so we overwrite the result with TIMEOUT
        # here. if this is the case.
        # However, we don't want to forget more specific results like SEGFAULT,
        # so we do this only if the result is a "normal" one like SAFE.
        if not self.status in ['SAFE', 'UNSAFE', 'UNKNOWN']:
            if TIMELIMIT in self.benchmark.rlimits:
                timeLimit = self.benchmark.rlimits[TIMELIMIT] + 20
                if self.wallTime > timeLimit or self.cpuTime > timeLimit:
                    self.status = "TIMEOUT"

        self.benchmark.outputHandler.outputAfterRun(self)


class Column:
    """
    The class Column contains text, title and numberOfDigits of a column.
    """

    def __init__(self, text, title, numOfDigits):
        self.text = text
        self.title = title
        self.numberOfDigits = numOfDigits
        self.value = ""


class Util:
    """
    This Class contains some useful functions for Strings, XML or Lists.
    """

    @staticmethod
    def printOut(value, end='\n'):
        """
        This function prints the given String immediately and flushes the output.
        """
        sys.stdout.write(value)
        sys.stdout.write(end)
        sys.stdout.flush()

    @staticmethod
    def isCode(filename):
        """
        This function returns True, if  a line of the file contains bracket '{'.
        """
        isCodeFile = False
        file = open(filename, "r")
        for line in file:
            # ignore comments and empty lines
            if not Util.isComment(line) \
                    and '{' in line: # <-- simple indicator for code
                if '${' not in line: # <-- ${abc} variable to substitute
                    isCodeFile = True
        file.close()
        return isCodeFile


    @staticmethod
    def isComment(line):
        return not line or line.startswith("#") or line.startswith("//")


    @staticmethod
    def containsAny(text, list):
        '''
        This function returns True, iff any string in list is a substring of text.
        '''
        for elem in list:
            if elem in text:
                return True
        return False


    @staticmethod
    def removeAll(list, elemToRemove):
        return [elem for elem in list if elem != elemToRemove]


    @staticmethod
    def toSimpleList(listOfPairs):
        """
        This function converts a list of pairs to a list.
        Each pair of key and value is divided into 2 listelements.
        All "None"-values are removed.
        """
        simpleList = []
        for (key, value) in listOfPairs:
            if key is not None:    simpleList.append(key)
            if value is not None:  simpleList.append(value)
        return simpleList


    @staticmethod
    def getCopyOfXMLElem(elem):
        """
        This method returns a shallow copy of a XML-Element.
        This method is for compatibility with Python 2.6 or earlier..
        In Python 2.7 you can use  'copyElem = elem.copy()'  instead.
        """

        copyElem = ET.Element(elem.tag, elem.attrib)
        for child in elem:
            copyElem.append(child)
        return copyElem


    @staticmethod
    def XMLtoString(elem):
        """
        Return a pretty-printed XML string for the Element.
        """
        from xml.dom import minidom
        rough_string = ET.tostring(elem, 'utf-8')
        reparsed = minidom.parseString(rough_string)
        return reparsed.toprettyxml(indent="  ")


    @staticmethod
    def decodeToString(toDecode):
        """
        This function is needed for Python 3,
        because a subprocess can return bytes instead of a string.
        """
        try: 
            return toDecode.decode('utf-8')
        except AttributeError: # bytesToDecode was of type string before
            return toDecode


    @staticmethod
    def formatNumber(number, numberOfDigits):
        """
        The function formatNumber() return a string-representation of a number
        with a number of digits after the decimal separator.
        If the number has more digits, it is rounded.
        If the number has less digits, zeros are added.

        @param number: the number to format
        @param digits: the number of digits
        """
        return "%.{0}f".format(numberOfDigits) % number


    @staticmethod
    def addFilesToGitRepository(files, description):
        """
        Add and commit all files given in a list into a git repository in the
        OUTPUT_PATH directory. Nothing is done if the git repository has
        local changes.

        @param files: the files to commit
        @param description: the commit message
        """
        if not os.path.isdir(OUTPUT_PATH):
            Util.printOut('Output path is not a directory, cannot add files to git repository.')
            return

        # find out root directory of repository
        gitRoot = subprocess.Popen(['git', 'rev-parse', '--show-toplevel'],
                                   cwd=OUTPUT_PATH,
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        stdout = gitRoot.communicate()[0]
        if gitRoot.returncode != 0:
            Util.printOut('Cannot commit results to repository: git rev-parse failed, perhaps output path is not a git directory?')
            return
        gitRootDir = Util.decodeToString(stdout).splitlines()[0]

        # check whether repository is clean
        gitStatus = subprocess.Popen(['git','status','--porcelain', '--untracked-files=no'],
                                     cwd=gitRootDir,
                                     stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        (stdout, stderr) = gitStatus.communicate()
        if gitStatus.returncode != 0:
            Util.printOut('Git status failed! Output was:\n' + Util.decodeToString(stderr))
            return

        if stdout:
            Util.printOut('Git repository has local changes, not commiting results.')
            return

        # add files to staging area
        files = [os.path.realpath(file) for file in files]
        gitAdd = subprocess.Popen(['git', 'add', '--'] + files,
                                   cwd=gitRootDir)
        if gitAdd.wait() != 0:
            Util.printOut('Git add failed, will not commit results!')
            return

        # commit files
        Util.printOut('Committing results files to git repository in ' + gitRootDir)
        gitCommit = subprocess.Popen(['git', 'commit', '--file=-', '--quiet'],
                                     cwd=gitRootDir,
                                     stdin=subprocess.PIPE)
        gitCommit.communicate(description.encode('UTF-8'))
        if gitCommit.returncode != 0:
            Util.printOut('Git commit failed!')
            return


class OutputHandler:
    """
    The class OutputHandler manages all outputs to the terminal and to files.
    """

    printLock = threading.Lock()

    def __init__(self, benchmark):
        """
        The constructor of OutputHandler creates the folder to store logfiles
        and collects information about the benchmark and the computer.
        """

        self.allCreatedFiles = []
        self.benchmark = benchmark
        self.statistics = Statistics()

        # create folder for file-specific log-files.
        # existing files (with the same name) will be OVERWRITTEN!
        self.logFolder = OUTPUT_PATH + self.benchmark.name + "." + self.benchmark.date + ".logfiles/"
        if not os.path.isdir(self.logFolder):
            os.makedirs(self.logFolder)

        # get information about computer
        (opSystem, cpuModel, numberOfCores, maxFrequency, memory, hostname) = self.getSystemInfo()
        version = self.getVersion(self.benchmark.tool)

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


    def storeHeaderInXML(self, version, memlimit, timelimit, opSystem,
                         cpuModel, numberOfCores, maxFrequency, memory, hostname):

        # store benchmarkInfo in XML
        self.XMLHeader = ET.Element("test",
                    {"benchmarkname": self.benchmark.name, "date": self.benchmark.dateISO,
                     "tool": self.getToolnameForPrinting(), "version": version})
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
                + "tool:".ljust(columnWidth) + self.getToolnameForPrinting()\
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

        testname = None
        if len(self.benchmark.tests) == 1:
            # in case there is only a single test, we can use this name
            testname = self.benchmark.tests[0].name
        elif options.testRunOnly and len(options.testRunOnly) == 1:
            # in case we run only a single test, we can use this name
            testname = options.testRunOnly[0]

        # write to file
        self.TXTContent = self.description
        TXTFileName = self.getFileName(testname, "txt")
        self.TXTFile = FileWriter(TXTFileName, self.TXTContent)
        self.allCreatedFiles.append(TXTFileName)

    def getToolnameForPrinting(self):
        tool = self.benchmark.tool.lower()
        names = {'cpachecker': 'CPAchecker',
                 'cbmc'      : 'CBMC',
                 'satabs'    : 'SatAbs',
                 'blast'     : 'BLAST',
                 'ecav'      : 'ECAverifier',
                 'wolverine' : 'WOLVERINE',
                 'ufo'       : 'UFO',
                 'acsar'     : 'Acsar',
                 'feaver'    : 'Feaver'}
        if tool in names:
            return names[tool]
        else:
            return str(self.benchmark.tool)


    def getVersion(self, tool):
        """
        This function return a String representing the version of the tool.
        """

        version = ''
        exe = self.benchmark.exe
        if (tool == "cpachecker"):
            try:
                versionHelpStr = subprocess.Popen([exe, '-help'],
                    stdout=subprocess.PIPE).communicate()[0]
                versionHelpStr = Util.decodeToString(versionHelpStr)
                version = ' '.join(versionHelpStr.splitlines()[0].split()[1:])  # first word is 'CPAchecker'
            except IndexError:
                logging.critical('IndexError! Have you built CPAchecker?\n') # TODO better message
                sys.exit()

        elif (tool == "cbmc"):
            version = subprocess.Popen([exe, '--version'],
                              stdout=subprocess.PIPE).communicate()[0].strip()

        elif (tool == "satabs"):
            version = subprocess.Popen([exe, '--version'],
                              stdout=subprocess.PIPE).communicate()[0].strip()

        elif (tool == "wolverine"):
            version = subprocess.Popen([exe, '--version'],
                              stdout=subprocess.PIPE).communicate()[0].split()[1].strip()

        elif (tool == "blast"):
            version = subprocess.Popen([exe],
                              stdout=subprocess.PIPE,
                              stderr=subprocess.STDOUT).communicate()[0][6:9]

        return Util.decodeToString(version)


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


    def outputBeforeTest(self, test):
        """
        The method outputBeforeTest() calculates the length of the
        first column for the output in terminal and stores information
        about the test in XML.
        @param test: current test with a list of testfiles
        """

        self.test = test
        numberOfTest = self.benchmark.tests.index(self.test) + 1

        if len(self.test.runs) == 1:
            logging.debug("test {0} consists of 1 sourcefile.".format(
                    numberOfTest))
        else:
            logging.debug("test {0} consists of {1} sourcefiles.".format(
                    numberOfTest, len(self.test.runs)))

        fileNames = [run.sourcefile for run in self.test.runs]

        # common prefix of file names
        self.commonPrefix = os.path.commonprefix(fileNames) # maybe with parts of filename
        self.commonPrefix = self.commonPrefix[: self.commonPrefix.rfind('/') + 1] # only foldername

        # length of the first column in terminal
        self.maxLengthOfFileName = max([len(file) for file in fileNames])
        self.maxLengthOfFileName = max(20, self.maxLengthOfFileName - len(self.commonPrefix))

        # write testname to terminal
        numberOfFiles = len(self.test.runs)
        Util.printOut("\nrunning test" + \
            (" '" + test.name + "'" if test.name is not None else "") + \
            ("     (1 file)" if numberOfFiles == 1
                        else "     ({0} files)".format(numberOfFiles)))

        # write information about the test into TXTFile
        self.writeTestInfoToLog()

        # build dummy-entries for output, later replaced by the results,
        # the dummy-xml-elems are shared over all runs of a test,
        # xml-structure is equal to self.runToXML(run)
        dummyElems = [ET.Element("column", {"title": "status", "value": ""}),
                      ET.Element("column", {"title": "cputime", "value": ""}),
                      ET.Element("column", {"title": "walltime", "value": ""})]
        for column in self.benchmark.columns:
            dummyElems.append(ET.Element("column", 
                        {"title": column.title, "value": ""}))
            
        for run in self.test.runs:
            run.resultline = self.formatSourceFileName(run.sourcefile)
            run.xml = ET.Element("sourcefile", {"name": run.sourcefile})
            for dummyElem in dummyElems: run.xml.append(dummyElem)

        # write (empty) results to TXTFile and XML
        self.TXTFile.replace(self.TXTContent + self.testToTXT(self.test))
        XMLTestFileName = self.getFileName(self.test.name, "xml")
        self.XMLTestFile = FileWriter(XMLTestFileName,
                       Util.XMLtoString(self.runsToXML(self.test, self.test.runs)))
        self.XMLTestFile.lastModifiedTime = time.time()
        self.allCreatedFiles.append(XMLTestFileName)


    def outputForSkippingTest(self, test, reason=None):
        '''
        This function writes a simple message to terminal and logfile,
        when a test is skipped.
        There is no message about skipping a test in the xml-file.
        '''

        # print to terminal
        Util.printOut("\nskipping test" +
               (" '" + test.name + "'" if test.name else "") +
               (" " + reason if reason else "")
              )

        # write into TXTFile
        numberOfTest = self.benchmark.tests.index(test) + 1
        testInfo = "\n\n"
        if test.name is not None:
            testInfo += test.name + "\n"
        testInfo += "test {0} of {1}: skipped {2}\n".format(
                numberOfTest, len(self.benchmark.tests), reason or "")
        self.TXTContent += testInfo
        self.TXTFile.append(testInfo)


    def writeTestInfoToLog(self):
        """
        This method writes the information about a test into the TXTFile.
        """

        numberOfTest = self.benchmark.tests.index(self.test) + 1
        testOptions = mergeOptions(self.benchmark.options, self.test.options)
        if self.test.name is None:
            testInfo = ""
        else:
            testInfo = self.test.name + "\n"
        testInfo += "test {0} of {1} with options: {2}\n\n".format(
                numberOfTest, len(self.benchmark.tests),
                " ".join(testOptions))

        self.test.titleLine = self.createOutputLine("sourcefile", "status", "cpu time",
                            "wall time", self.benchmark.columns, True)

        self.test.simpleLine = "-" * (len(self.test.titleLine))

        # write into TXTFile
        self.TXTContent += "\n\n" + testInfo
        self.TXTFile.append("\n\n" + testInfo + self.test.titleLine + "\n" + self.test.simpleLine  + "\n")


    def outputBeforeRun(self, run):
        """
        The method outputBeforeRun() prints the name of a file to terminal.
        It returns the name of the logfile.
        @param sourcefile: the name of a sourcefile
        """

        logging.debug("I'm running '{0} {1} {2}'.".format(
            self.getToolnameForPrinting(), " ".join(run.mergedOptions), run.sourcefile))

        # output in terminal
        try:
            OutputHandler.printLock.acquire()

            timeStr = time.strftime("%H:%M:%S", time.localtime()) + "   "
            if run.benchmark.numOfThreads == 1:
                Util.printOut(timeStr + self.formatSourceFileName(run.sourcefile), '')
            else:
                Util.printOut(timeStr + "starting   " + self.formatSourceFileName(run.sourcefile))
        finally:
            OutputHandler.printLock.release()

        # get name of file-specific log-file
        logfileName = self.logFolder
        if run.test.name is not None:
            logfileName += run.test.name + "."
        logfileName += os.path.basename(run.sourcefile) + ".log"
        self.allCreatedFiles.append(logfileName)
        return logfileName


    def outputAfterRun(self, run):
        """
        The method outputAfterRun() prints filename, result, time and status
        of a test to terminal and stores all data in XML
        """

        # format times, type is changed from float to string!
        run.cpuTimeStr = Util.formatNumber(run.cpuTime, TIME_PRECISION)
        run.wallTimeStr = Util.formatNumber(run.wallTime, TIME_PRECISION)

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

        # output in terminal/console
        statusRelation = self.isCorrectResult(run.sourcefile, run.status)
        if USE_COLORS and sys.stdout.isatty(): # is terminal, not file
            statusStr = COLOR_DIC[statusRelation].format(run.status.ljust(8))
        else:
            statusStr = run.status.ljust(8)

        try:
            OutputHandler.printLock.acquire()

            # if there was an interupt in run, we do not print the result
            if not STOPPED_BY_INTERRUPT:
                valueStr = statusStr + run.cpuTimeStr.rjust(8) + run.wallTimeStr.rjust(8)
                if run.benchmark.numOfThreads == 1:
                    Util.printOut(valueStr)
                else:
                    timeStr = time.strftime("%H:%M:%S", time.localtime()) + " "*14
                    Util.printOut(timeStr + self.formatSourceFileName(run.sourcefile) + valueStr)

            # store information in run
            run.resultline = self.createOutputLine(run.sourcefile, run.status,
                    run.cpuTimeStr, run.wallTimeStr, run.columns)
            run.xml = self.runToXML(run)

            # write result in TXTFile and XML
            self.TXTFile.replace(self.TXTContent + self.testToTXT(self.test))
            self.statistics.addResult(statusRelation)

            # we don't want to write this file to often, it can slow down the whole script,
            # so we wait at least 10 seconds between two write-actions
            currentTime = time.time()
            if currentTime - self.XMLTestFile.lastModifiedTime > 10:
                self.XMLTestFile.replace(Util.XMLtoString(self.runsToXML(self.test, self.test.runs)))
                self.XMLTestFile.lastModifiedTime = currentTime

        finally:
            OutputHandler.printLock.release()


    def outputAfterTest(self, cpuTimeTest, wallTimeTest):
        """
        The method outputAfterTest() stores the times of a test in XML.
        @params cpuTimeTest, wallTimeTest: times of the test
        """

        # format time, type is changed from float to string!
        self.test.cpuTimeStr = Util.formatNumber(cpuTimeTest, TIME_PRECISION)
        self.test.wallTimeStr = Util.formatNumber(wallTimeTest, TIME_PRECISION)

        # write testresults to files
        self.XMLTestFile.replace(Util.XMLtoString(self.runsToXML(self.test, self.test.runs)))
        CSVFileName = self.getFileName(self.test.name, "csv")
        FileWriter(CSVFileName, self.testToCSV(self.test))
        self.allCreatedFiles.append(CSVFileName)

        if len(self.test.blocks) > 1:
            for block in self.test.blocks:
                FileWriter(self.getFileName(self.test.name, block.name + ".xml"),
                    Util.XMLtoString(self.runsToXML(self.test, block.runs, block.name)))

        self.TXTContent += self.testToTXT(self.test, True)
        self.TXTFile.replace(self.TXTContent)


    def testToTXT(self, test, finished=False):
        lines = [test.titleLine, test.simpleLine]

        # store values of each run
        for run in test.runs: lines.append(run.resultline)

        lines.append(test.simpleLine)

        # write endline into TXTFile
        if finished:
            numberOfFiles = len(test.runs)
            numberOfTest = test.benchmark.tests.index(test) + 1
            if numberOfFiles == 1:
                endline = ("test {0} consisted of 1 sourcefile.".format(numberOfTest))
            else:
                endline = ("test {0} consisted of {1} sourcefiles.".format(
                    numberOfTest, numberOfFiles))

            lines.append(self.createOutputLine(endline, "done", test.cpuTimeStr,
                             test.wallTimeStr, []))

        return "\n".join(lines) + "\n"


    def runsToXML(self, test, runs, blockname=None):
        """
        This function dumps a list of runs of a test and their results to XML.
        """
        # copy benchmarkinfo, limits, columntitles, systeminfo from XMLHeader
        runsElem = Util.getCopyOfXMLElem(self.XMLHeader)
        testOptions = mergeOptions(test.benchmark.options, test.options)
        runsElem.set("options", " ".join(testOptions))
        if blockname is not None:
            runsElem.set("block", blockname)
            runsElem.set("name", ((test.name + ".") if test.name else "") + blockname)
        elif test.name is not None:
            runsElem.set("name", test.name)

        # collect XMLelements from all runs
        for run in runs: runsElem.append(run.xml)

        return runsElem


    def runToXML(self, run):
        """
        This function returns a xml-representation of a run.
        """
        runElem = ET.Element("sourcefile", {"name": run.sourcefile})
        if len(run.options) != 0:
            runElem.set("options", " ".join(mergeOptions(run.options)))
        runElem.append(ET.Element("column", {"title": "status", "value": run.status}))
        runElem.append(ET.Element("column", {"title": "cputime", "value": run.cpuTimeStr}))
        runElem.append(ET.Element("column", {"title": "walltime", "value": run.wallTimeStr}))

        for column in run.columns:
            runElem.append(ET.Element("column",
                        {"title": column.title, "value": column.value}))
        return runElem


    def testToCSV(self, test):
        """
        This function dumps a test with results into a CSV-file.
        """

        # store columntitles of tests
        CSVLines = [CSV_SEPARATOR.join(
                      ["sourcefile", "status", "cputime", "walltime"] \
                    + [column.title for column in test.benchmark.columns])]

        # store columnvalues of each run
        for run in test.runs:
            CSVLines.append(CSV_SEPARATOR.join(
                  [run.sourcefile, run.status, run.cpuTimeStr, run.wallTimeStr] \
                + [column.value for column in run.columns]))

        return "\n".join(CSVLines) + "\n"


    def isCorrectResult(self, filename, status):
        '''
        this function return a string,
        that shows the relation between status and file.
        '''
        status = status.lower()
        isSafeFile = not Util.containsAny(filename.lower(), BUG_SUBSTRING_LIST)

        if status == 'safe':
            if isSafeFile:
                return "correctSafe"
            else:
                return "wrongSafe"
        elif status == 'unsafe':
            if isSafeFile:
                return "wrongUnsafe"
            else:
                return "correctUnsafe"
        elif status == 'unknown':
            return 'unknown'
        else:
            return 'error'


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

        if STOPPED_BY_INTERRUPT:
            Util.printOut("\nscript was interrupted by user, some tests may not be done\n")


    def getFileName(self, testname, fileExtension):
        '''
        This function returns the name of the file of a test
        with an extension ("txt", "xml", "csv").
        '''

        fileName = OUTPUT_PATH + self.benchmark.name + "." \
                    + self.benchmark.date + ".results."

        if testname is not None:
            fileName += testname + "."

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
                    "wrongSafe": 0}


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


def getOptions(optionsTag):
    '''
    This function searches for options in a tag
    and returns a list with tuples of (name, value).
    '''
    return [(option.get("name"), option.text)
               for option in optionsTag.findall("option")]


def mergeOptions(benchmarkOptions, testOptions=[], fileOptions=[]):
    '''
    This function merges lists of optionpairs into one list.
    If a option is part of several lists,
    the option appears in the list several times.
    '''

    currentOptions = []

    # copy global options
    currentOptions.extend(benchmarkOptions)

    # insert testOptions
    currentOptions.extend(testOptions)

    # insert fileOptions
    currentOptions.extend(fileOptions)

    return Util.toSimpleList(currentOptions)


class FileWriter:
     """
     The class FileWrtiter is a wrapper for writing content into a file.
     """

     def __init__(self, filename, content):
         """
         The constructor of FileWriter creates the file.
         If the file exist, it will be OVERWRITTEN without a message!
         """

         self.__filename = filename
         file = open(self.__filename, "w")
         file.write(content)
         file.close()

     def append(self, content):
         file = open(self.__filename, "a")
         file.write(content)
         file.close()

     def replace(self, content):
         """
         replaces the content of a file.
         a tmp-file is used to avoid loss of data through an interrupt
         """
         tmpFilename = self.__filename + ".tmp"
         
         file = open(tmpFilename, "w")
         file.write(content)
         file.close()
         
         os.rename(tmpFilename, self.__filename)


def substituteVars(oldList, test, sourcefile=None, logFolder=None):
    """
    This method replaces special substrings from a list of string 
    and return a new list.
    """

    benchmark = test.benchmark

    # list with tuples (key, value): 'key' is replaced by 'value'
    keyValueList = [('${benchmark_name}', benchmark.name),
                    ('${benchmark_date}', benchmark.date),
                    ('${benchmark_path}', os.path.dirname(benchmark.benchmarkFile)),
                    ('${benchmark_path_abs}', os.path.abspath(os.path.dirname(benchmark.benchmarkFile))),
                    ('${benchmark_file}', os.path.basename(benchmark.benchmarkFile)),
                    ('${benchmark_file_abs}', os.path.abspath(os.path.basename(benchmark.benchmarkFile))),
                    ('${test_name}',      test.name if test.name is not None else 'noName')]

    if sourcefile:
        keyValueList.append(('${sourcefile_name}', os.path.basename(sourcefile)))
        keyValueList.append(('${sourcefile_path}', os.path.dirname(sourcefile)))
        keyValueList.append(('${sourcefile_path_abs}', os.path.dirname(os.path.abspath(sourcefile))))

    if logFolder:
        keyValueList.append(('${logfile_path}', os.path.dirname(logFolder)))
        keyValueList.append(('${logfile_path_abs}', os.path.abspath(logFolder)))

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


def findExecutable(program=None, fallback=None):
    def isExecutable(programPath):
        return os.path.isfile(programPath) and os.access(programPath, os.X_OK)

    if program is None:
        return None

    else:
        dirs = os.environ['PATH'].split(os.pathsep)
        dirs.append(".")

        for dir in dirs:
            name = os.path.join(dir, program)
            if isExecutable(name):
                return name

        if fallback is not None and isExecutable(fallback):
            return fallback

        sys.exit("ERROR: Could not find '{0}' executable".format(program))


def killSubprocess(process):
    '''
    this function kills the process and the children in its group.
    '''
    try:
        os.killpg(process.pid, signal.SIGTERM)
    except OSError: # process itself returned and exited before killing
        pass


def run(args, rlimits, numberOfThread, outputfilename):
    args = [os.path.expandvars(arg) for arg in args]
    args = [os.path.expanduser(arg) for arg in args]

    if options.limitCores:
        # use only one cpu for one subprocess
        # if there are more threads than cores, some threads share the same core
        import multiprocessing
        args = ['taskset', '-c', str(numberOfThread % multiprocessing.cpu_count())] + args

    outputfile = open(outputfilename, 'w') # override existing file
    outputfile.write(' '.join(args) + '\n\n\n' + '-'*80 + '\n\n\n')
    outputfile.flush()

    def preSubprocess():
        os.setpgrp() # make subprocess to group-leader
        if TIMELIMIT in rlimits:
            resource.setrlimit(resource.RLIMIT_CPU, (rlimits[TIMELIMIT], rlimits[TIMELIMIT]))
        if MEMLIMIT in rlimits:
            memresource = resource.RLIMIT_DATA if options.memdata else resource.RLIMIT_AS
            memlimit = rlimits[MEMLIMIT] * BYTE_FACTOR * BYTE_FACTOR # MB to Byte
            resource.setrlimit(memresource, (memlimit, memlimit))

    wallTimeBefore = time.time()

    try:
        p = subprocess.Popen(args,
                             stdout=outputfile, stderr=outputfile,
                             preexec_fn=preSubprocess)

        try:
            SUB_PROCESSES_LOCK.acquire()
            SUB_PROCESSES.add(p)
        finally:
            SUB_PROCESSES_LOCK.release()

        # if rlimit does not work, a seperate Timer is started to kill the subprocess,
        # Timer has 10 seconds 'overhead'
        if TIMELIMIT in rlimits:
          timelimit = rlimits[TIMELIMIT]
          timer = threading.Timer(timelimit + 10, killSubprocess, [p])
          timer.start()

        (pid, returnvalue, ru_child) = os.wait4(p.pid, 0)

        # calculation: returnvalue == (returncode * 256) + returnsignal
        returnsignal = returnvalue % 256
        returncode = returnvalue // 256
        assert pid == p.pid

    except OSError:
        logging.critical("I caught an OSError. Assure that the directory "
                         + "containing the tool to be benchmarked is included "
                         + "in the PATH environment variable or an alias is set.")
        sys.exit("A critical exception caused me to exit non-gracefully. Bye.")

    finally:
        try:
            SUB_PROCESSES_LOCK.acquire()
            assert p in SUB_PROCESSES
            SUB_PROCESSES.remove(p)
        finally:
            SUB_PROCESSES_LOCK.release()
        
        if (TIMELIMIT in rlimits) and timer.isAlive():
            timer.cancel()

        outputfile.close() # normally subprocess closes file, we do this again

    wallTimeAfter = time.time()
    wallTimeDelta = wallTimeAfter - wallTimeBefore
    cpuTimeDelta = (ru_child.ru_utime + ru_child.ru_stime)

    logging.debug("My subprocess returned returncode {0}.".format(returncode))

    outputfile = open(outputfilename, 'r') # re-open file for reading output
    output = ''.join(outputfile.readlines()[6:]) # first 6 lines are for logging, rest is output of subprocess
    outputfile.close()
    output = Util.decodeToString(output)

    return (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta)


def isTimeout(cpuTimeDelta, rlimits):
    ''' try to find out whether the tool terminated because of a timeout '''
    if TIMELIMIT in rlimits:
        limit = rlimits[TIMELIMIT]
    else:
        limit = float('inf')

    return cpuTimeDelta > limit*0.99


def run_cbmc(exe, options, sourcefile, columns, rlimits, numberOfThread, file):
    if ("--xml-ui" not in options):
        options = options + ["--xml-ui"]

    args = [exe] + options + [sourcefile]
    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, numberOfThread, file)

    #an empty tag cannot be parsed into a tree
    output = output.replace("<>", "<emptyTag>")
    output = output.replace("</>", "</emptyTag>")

    if ((returncode == 0) or (returncode == 10)):
        try:
            tree = ET.fromstring(output)
            status = tree.findtext('cprover-status')
        
            if status is None:
                def isErrorMessage(msg):
                    return msg.get('type', None) == 'ERROR'

                messages = list(filter(isErrorMessage, tree.getiterator('message')))
                if messages:
                    # for now, use only the first error message if there are several
                    msg = messages[0].findtext('text')
                    if msg == 'Out of memory':
                        status = 'OUT OF MEMORY'
                    elif msg:
                        status = 'ERROR (%s)'.format(msg)
                    else:
                        status = 'ERROR'
                else:
                    status = 'INVALID OUTPUT'
                    
            elif status == "FAILURE":
                assert returncode == 10
                reason = tree.find('goto_trace').find('failure').findtext('reason')
                if 'unwinding assertion' in reason:
                    status = "UNKNOWN"
                else:
                    status = "UNSAFE"
                    
            elif status == "SUCCESS":
                assert returncode == 0
                if "--no-unwinding-assertions" in options:
                    status = "UNKNOWN"
                else:
                    status = "SAFE"
                
        except Exception as e: # catch all exceptions
            if isTimeout(cpuTimeDelta, rlimits):
                # in this case an exception is expected as the XML is invaliddd
                status = 'TIMEOUT'
            elif 'Minisat::OutOfMemoryException' in output:
                status = 'OUT OF MEMORY'
            else:
                status = 'INVALID OUTPUT'
                logging.warning("Error parsing CBMC output for returncode %d: %s" % (returncode, e))
    
    elif returncode == 6:
        # parser error or something similar
        status = 'ERROR'

    elif returnsignal == 9 or returncode == (128+9):
        if isTimeout(cpuTimeDelta, rlimits):
            status = 'TIMEOUT'
        else:
            status = "KILLED BY SIGNAL 9"

    elif returnsignal == 6:
        status = "ABORTED"
    elif returnsignal == 15 or returncode == (128+15):
        status = "KILLED"
    else:
        status = "ERROR ({0})".format(returncode)

    return (status, cpuTimeDelta, wallTimeDelta, args)


def run_satabs(exe, options, sourcefile, columns, rlimits, numberOfThread, file):
    args = [exe] + options + [sourcefile]

    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, numberOfThread, file)

    if "VERIFICATION SUCCESSFUL" in output:
        assert returncode == 0
        status = "SAFE"
    elif "VERIFICATION FAILED" in output:
        assert returncode == 10
        status = "UNSAFE"
    elif returnsignal == 9:
        status = "TIMEOUT"
    elif returnsignal == 6:
        if "Assertion `!counterexample.steps.empty()' failed" in output:
            status = 'COUNTEREXAMPLE FAILED' # TODO: other status?
        else:
            status = "OUT OF MEMORY"
    elif returncode == 1 and "PARSING ERROR" in output:
        status = "PARSING ERROR"
    else:
        status = "FAILURE"
    return (status, cpuTimeDelta, wallTimeDelta, args)


def run_wolverine(exe, options, sourcefile, columns, rlimits, numberOfThread, file):
    args = [exe] + options + [sourcefile]
    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, numberOfThread, file)
    if "VERIFICATION SUCCESSFUL" in output:
        assert returncode == 0
        status = "SAFE"
    elif "VERIFICATION FAILED" in output:
        assert returncode == 10
        status = "UNSAFE"
    elif returnsignal == 9:
        status = "TIMEOUT"
    elif returnsignal == 6 or (returncode == 6 and "Out of memory" in output):
        status = "OUT OF MEMORY"
    elif returncode == 6 and "PARSING ERROR" in output:
        status = "PARSING ERROR"
    else:
        status = "FAILURE"
    return (status, cpuTimeDelta, wallTimeDelta, args)


def run_ufo(exe, options, sourcefile, columns, rlimits, numberOfThread, file):
    args = [exe, sourcefile] + options
    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, numberOfThread, file)
    if returnsignal == 9 or returnsignal == (128+9):
        if isTimeout(cpuTimeDelta, rlimits):
            status = "TIMEOUT"
        else:
            status = "KILLED BY SIGNAL 9"
    elif returncode == 1 and "program correct: ERROR unreachable" in output:
        status = "SAFE"
    elif returncode != 0:
        status = "ERROR ({0})".format(returncode)
    elif "ERROR reachable" in output:
        status = "UNSAFE"
    elif "program correct: ERROR unreachable" in output:
        status = "SAFE"
    else:
        status = "FAILURE"
    return (status, cpuTimeDelta, wallTimeDelta, args)


def run_acsar(exe, options, sourcefile, columns, rlimits, numberOfThread, file):

    # create tmp-files for acsar, acsar needs special error-labels
    prepSourcefile = prepareSourceFileForAcsar(sourcefile)

    if ("--mainproc" not in options):
        options = options + ["--mainproc", "main"]

    args = [exe] + ["--file"] + [prepSourcefile] + options

    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, numberOfThread, file)
    if "syntax error" in output:
        status = "SYNTAX ERROR"

    elif "runtime error" in output:
        status = "RUNTIME ERROR"

    elif "error while loading shared libraries:" in output:
        status = "LIBRARY ERROR"

    elif "can not be used as a root procedure because it is not defined" in output:
        status = "NO MAIN"

    elif "For Error Location <<ERROR_LOCATION>>: I don't Know " in output:
        status = "TIMEOUT"

    elif "received signal 6" in output:
        status = "ABORT"

    elif "received signal 11" in output:
        status = "SEGFAULT"

    elif "received signal 15" in output:
        status = "KILLED"

    elif "Error Location <<ERROR_LOCATION>> is not reachable" in output:
        status = "SAFE"

    elif "Error Location <<ERROR_LOCATION>> is reachable via the following path" in output:
        status = "UNSAFE"

    else:
        status = "UNKNOWN"

    # delete tmp-files
    os.remove(prepSourcefile)

    return (status, cpuTimeDelta, wallTimeDelta, args)


def prepareSourceFileForAcsar(sourcefile):
    content = open(sourcefile, "r").read()
    content = content.replace(
        "ERROR;", "ERROR_LOCATION;").replace(
        "ERROR:", "ERROR_LOCATION:").replace(
        "errorFn();", "goto ERROR_LOCATION; ERROR_LOCATION:;")
    newFilename = sourcefile + "_acsar.c"
    preparedFile = FileWriter(newFilename, content)
    return newFilename


def run_feaver(exe, options, sourcefile, columns, rlimits, numberOfThread, file):

    # create tmp-files for acsar, acsar needs special error-labels
    prepSourcefile = prepareSourceFileForFeaver(sourcefile)

    args = [exe] + options + [prepSourcefile]

    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, numberOfThread, file)
    if "collect2: ld returned 1 exit status" in output:
        status = "COMPILE ERROR"

    elif "Error (parse error" in output:
        status = "PARSE ERROR"

    elif "error: (\"model\":" in output:
        status = "MODEL ERROR"

    elif "Error: syntax error" in output:
        status = "SYNTAX ERROR"

    elif "error: " in output or "Error: " in output:
        status = "ERROR"

    elif "Error Found:" in output:
        status = "UNSAFE"

    elif "No Errors Found" in output:
        status = "SAFE"

    else:
        status = "UNKNOWN"

    # delete tmp-files
    for tmpfile in [prepSourcefile, prepSourcefile[0:-1] + "M",
                 "_modex_main.spn", "_modex_.h", "_modex_.cln", "_modex_.drv",
                 "model", "pan.b", "pan.c", "pan.h", "pan.m", "pan.t"]:
        try:
            os.remove(tmpfile)
        except OSError:
            pass

    return (status, cpuTimeDelta, wallTimeDelta, args)


def prepareSourceFileForFeaver(sourcefile):
    content = open(sourcefile, "r").read()
    content = content.replace("goto ERROR;", "assert(0);")
    newFilename = "tmp_benchmark_feaver.c"
    preparedFile = FileWriter(newFilename, content)
    return newFilename


# the next 3 functions are for imaginary tools, that return special results,
# perhaps someone can use these function again someday,
# to use them you need a normal benchmark-xml-file 
# with the tool and sourcefiles, however options are ignored
def run_safe(exe, options, sourcefile, columns, rlimits, numberOfThread, file):
    args = ['safe'] + options + [sourcefile]
    cpuTimeDelta = wallTimeDelta = 0
    return ('safe', cpuTimeDelta, wallTimeDelta, args)

def run_unsafe(exe, options, sourcefile, columns, rlimits, numberOfThread, file):
    args = ['unsafe'] + options + [sourcefile]
    cpuTimeDelta = wallTimeDelta = 0
    return ('unsafe', cpuTimeDelta, wallTimeDelta, args)

def run_random(exe, options, sourcefile, columns, rlimits, numberOfThread, file):
    args = ['random'] + options + [sourcefile]
    cpuTimeDelta = wallTimeDelta = 0
    from random import random
    status = 'safe' if random() < 0.5 else 'unsafe'
    return (status, cpuTimeDelta, wallTimeDelta, args)


def appendFileToFile(sourcename, targetname):
    source = open(sourcename, 'r')
    try:
        target = open(targetname, 'a')
        try:
            target.writelines(source.readlines())
        finally:
            target.close()
    finally:
        source.close()

def run_cpachecker(exe, options, sourcefile, columns, rlimits, numberOfThread, file):
    if ("-stats" not in options):
        options = options + ["-stats"]

    args = [exe] + options + [sourcefile]
    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, numberOfThread, file)

    status = getCPAcheckerStatus(returncode, returnsignal, output, rlimits, cpuTimeDelta)
    getCPAcheckerColumns(output, columns)

    # Segmentation faults reference a file with more information.
    # We append this file to the log.
    if status == 'SEGMENTATION FAULT' or status.startswith('ERROR'):
        next = False
        for line in output.splitlines():
            if next:
                try:
                    dumpFile = line.strip(' #')
                    appendFileToFile(dumpFile, file)
                    os.remove(dumpFile)
                except IOError as e:
                    logging.warn('Could not append additional segmentation fault information (%s)' % e.strerror)
                break
            if line == '# An error report file with more information is saved as:':
                next = True

    return (status, cpuTimeDelta, wallTimeDelta, args)


def getCPAcheckerStatus(returncode, returnsignal, output, rlimits, cpuTimeDelta):
    """
    @param returncode: code returned by CPAchecker
    @param returnsignal: signal, which terminated CPAchecker
    @param output: the output of CPAchecker
    @return: status of CPAchecker after running a testfile
    """

    def isOutOfNativeMemory(line):
        return ('std::bad_alloc'             in line # C++ out of memory exception (MathSAT)
             or 'Cannot allocate memory'     in line
             or line.startswith('out of memory')     # CuDD
             )

    if returnsignal == 0:
        status = None

    elif returnsignal == 6:
        status = "ABORTED (probably by Mathsat)"

    elif returnsignal == 9:
        if isTimeout(cpuTimeDelta, rlimits):
            status = 'TIMEOUT'
        else:
            status = "KILLED BY SIGNAL 9"

    elif returnsignal == (128+15):
        status = "KILLED"

    else:
        status = "ERROR ({0})".format(returnsignal)

    for line in output.splitlines():
        if 'java.lang.OutOfMemoryError' in line:
            status = 'OUT OF JAVA MEMORY'
        elif isOutOfNativeMemory(line):
            status = 'OUT OF NATIVE MEMORY'
        elif 'There is insufficient memory for the Java Runtime Environment to continue.' in line \
                or 'cannot allocate memory for thread-local data: ABORT' in line:
            status = 'OUT OF MEMORY'
        elif 'SIGSEGV' in line:
            status = 'SEGMENTATION FAULT'
        elif ((returncode == 0 or returncode == 1)
                and ('Exception' in line or 'java.lang.AssertionError' in line)
                and not line.startswith('cbmc')): # ignore "cbmc error output: ... Minisat::OutOfMemoryException"
            status = 'ASSERTION' if 'java.lang.AssertionError' in line else 'EXCEPTION'
        elif 'Could not reserve enough space for object heap' in line:
            status = 'JAVA HEAP ERROR'
        elif line.startswith('Error: '):
            status = 'ERROR'
        
        elif line.startswith('Verification result: '):
            line = line[21:].strip()
            if line.startswith('SAFE'):
                newStatus = 'SAFE'
            elif line.startswith('UNSAFE'):
                newStatus = 'UNSAFE'
            else:
                newStatus = 'UNKNOWN'
            status = newStatus if status is None else "{0} ({1})".format(status, newStatus)
            
        elif (status is None) and line.startswith('#Test cases computed:'):
            status = 'OK'
    if status is None:
        status = "UNKNOWN"
    return status


def getCPAcheckerColumns(output, columns):
    """
    The method getCPAcheckerColumns() searches the columnvalues in the output
    and adds the values to the column-objects.
    If a value is not found, the value is set to "-".
    @param output: the output of CPAchecker
    @param columns: a list with columns
    """

    for column in columns:

        # search for the text in output and get its value,
        # stop after the first line, that contains the searched text
        column.value = "-" # default value
        for line in output.splitlines():
            if column.text in line:
                startPosition = line.find(':') + 1
                endPosition = line.find('(') # bracket maybe not found -> (-1)
                if (endPosition == -1):
                    column.value = line[startPosition:].strip()
                else:
                    column.value = line[startPosition: endPosition].strip()
                break


def run_blast(exe, options, sourcefile, columns, rlimits, numberOfThread, file):
    args = [exe] + options + [sourcefile]
    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, numberOfThread, file)

    status = "UNKNOWN"
    for line in output.splitlines():
        if line.startswith('Error found! The system is unsafe :-('):
            status = 'UNSAFE'
        elif line.startswith('No error found.  The system is safe :-)'):
            status = 'SAFE'
        elif (returncode == 2) and line.startswith('Fatal error: out of memory.'):
            status = 'OUT OF MEMORY'
        elif (returncode == 2) and line.startswith('Fatal error: exception Sys_error("Broken pipe")'):
            status = 'EXCEPTION'
        elif (returncode == 2) and line.startswith('Ack! The gremlins again!: Sys_error("Broken pipe")'):
            status = 'TIMEOUT'

    return (status, cpuTimeDelta, wallTimeDelta, args)

def run_ecav(exe, options, sourcefile, columns, rlimits, numberOfThread, file):
    args = [exe] + options + [sourcefile]
    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, numberOfThread, file)

    status = "UNKNOWN"
    for line in output.splitlines():
        if line.startswith('0 safe, 1 unsafe'):
            status = 'UNSAFE'
        elif line.startswith('1 safe, 0 unsafe'):
            status = 'SAFE'
        elif returnsignal == 9:
            if isTimeout(cpuTimeDelta, rlimits):
                status = 'TIMEOUT'
            else:
                status = "KILLED BY SIGNAL 9"

    return (status, cpuTimeDelta, wallTimeDelta, args)


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
            currentRun.run(self.number)
            Worker.workingQueue.task_done()


def runBenchmark(benchmarkFile):
    benchmark = Benchmark(benchmarkFile)

    if len(benchmark.tests) == 1:
        logging.debug("I'm benchmarking {0} consisting of 1 test.".format(repr(benchmarkFile)))
    else:
        logging.debug("I'm benchmarking {0} consisting of {1} tests.".format(
                repr(benchmarkFile), len(benchmark.tests)))

    outputHandler = benchmark.outputHandler
    testsRun = 0

    logging.debug("I will use {0} threads.".format(benchmark.numOfThreads))

    # iterate over tests and runs
    for test in benchmark.tests:

        if STOPPED_BY_INTERRUPT: break

        testnumber = benchmark.tests.index(test) + 1 # the first test has number 1
        (mod, rest) = options.moduloAndRest

        if (options.testRunOnly and test.name not in options.testRunOnly) \
                or (testnumber % mod != rest):
            outputHandler.outputForSkippingTest(test)

        elif not test.runs:
            outputHandler.outputForSkippingTest(test, "because it has no files")

        else:
            testsRun += 1
            # get times before test
            ruBefore = resource.getrusage(resource.RUSAGE_CHILDREN)
            wallTimeBefore = time.time()

            outputHandler.outputBeforeTest(test)

            # put all runs into a queue
            for run in test.runs:
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
                
            assert (len(SUB_PROCESSES) == 0) or STOPPED_BY_INTERRUPT

            # get times after test
            wallTimeAfter = time.time()
            wallTimeTest = wallTimeAfter - wallTimeBefore
            ruAfter = resource.getrusage(resource.RUSAGE_CHILDREN)
            cpuTimeTest = (ruAfter.ru_utime + ruAfter.ru_stime)\
            - (ruBefore.ru_utime + ruBefore.ru_stime)

            outputHandler.outputAfterTest(cpuTimeTest, wallTimeTest)

    outputHandler.outputAfterBenchmark()
    if options.commit and not STOPPED_BY_INTERRUPT and testsRun > 0:
        Util.addFilesToGitRepository(outputHandler.allCreatedFiles,
                                     options.commitMessage+'\n\n'+outputHandler.description)


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

    parser.add_argument("-t", "--test", dest="testRunOnly",
                      action="append",
                      help="Run only the specified TEST from the benchmark definition. "
                            + "This option can be specified several times.",
                      metavar="TEST")

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
                      dest="numOfThreads", default=None,
                      help="Run n benchmarks in parallel",
                      metavar="n")

    parser.add_argument("-x", "--moduloAndRest",
                      dest="moduloAndRest", default=(1,0), nargs=2, type=int,
                      help="Run only a subset of tests for which (i %% a == b) holds" +
                            "with i being the index of the test in the benhcmark definition file " +
                            "(starting with 1).",
                      metavar=("a","b"))

    parser.add_argument("-D", "--memdata", dest="memdata",
                      action="store_true",
                      help="When limiting memory usage, restrict only the data segments instead of the virtual address space.")

    parser.add_argument("-c", "--limitCores", dest="limitCores",
                      action="store_true",
                      help="Limit each run of the tool to a single CPU core.")

    parser.add_argument("--commit", dest="commit",
                      action="store_true",
                      help="If the output path is a git repository without local changes,"
                            + "add and commit the result files.")

    parser.add_argument("--message",
                      dest="commitMessage", type=str,
                      default="Results for benchmark run",
                      help="Commit message if --commit is used.")

    global options, OUTPUT_PATH
    options = parser.parse_args(argv[1:])
    if os.path.isdir(options.output_path):
        OUTPUT_PATH = os.path.normpath(options.output_path) + os.sep
    else:
        OUTPUT_PATH = options.output_path


    if (options.debug):
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                            level=logging.DEBUG)
    else:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s")

    for arg in options.files:
        if not os.path.exists(arg) or not os.path.isfile(arg):
            parser.error("File {0} does not exist.".format(repr(arg)))

    try:
        processes = subprocess.Popen(['ps', '-eo', 'cmd'], stdout=subprocess.PIPE).communicate()[0]
        if len(re.findall("python.*benchmark\.py", Util.decodeToString(processes))) > 1:
            logging.warn("Already running instance of this script detected. " + \
                         "Please make sure to not interfere with somebody else's benchmarks.")
    except OSError:
        pass # this does not work on Windows

    for arg in options.files:
        if STOPPED_BY_INTERRUPT: break
        logging.debug("Benchmark {0} is started.".format(repr(arg)))
        runBenchmark(arg)
        logging.debug("Benchmark {0} is done.".format(repr(arg)))

    logging.debug("I think my job is done. Have a nice day!")


def killScript():
        # set global flag
        global STOPPED_BY_INTERRUPT
        STOPPED_BY_INTERRUPT = True

        # kill running jobs
        Util.printOut("killing subprocesses...")
        try:
            SUB_PROCESSES_LOCK.acquire()
            for process in SUB_PROCESSES:
                killSubprocess(process)
        finally:
            SUB_PROCESSES_LOCK.release()

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
    except KeyboardInterrupt: # this block is reached, when interrupt is thrown before or after a test
        killScript()
        Util.printOut("\n\nscript was interrupted by user, some tests may not be done")
