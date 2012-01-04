#!/usr/bin/env python

"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2011  Dirk Beyer
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

from datetime import date
from threading import Timer

import time
import glob
import logging
import os
import platform
import resource
import signal
import subprocess
import sys
import xml.etree.ElementTree as ET

CSV_SEPARATOR = "\t"

BUG_SUBSTRING_LIST = ['bug', 'unsafe']


BYTE_FACTOR = 1024 # byte in kilobyte

# colors for column status in terminal
USE_COLORS = True
COLOR_GREEN = "\033[32;1m{0}\033[m"
COLOR_RED = "\033[31;1m{0}\033[m"
COLOR_ORANGE = "\033[33;1m{0}\033[m" # not orange, magenta
COLOR_MAGENTA = "\033[35;1m{0}\033[m"
COLOR_DIC = {"correctSafe": COLOR_GREEN,
             "correctUnsafe": COLOR_GREEN,
             "unknown": COLOR_ORANGE,
             "error": COLOR_MAGENTA,
             "wrongUnsafe": COLOR_RED,
             "wrongSafe": COLOR_RED}


# the number of digits after the decimal separator of the time column,
# for the other columns it can be configured in the xml-file
TIME_PRECISION = 2

USE_ONLY_DATE = False # use date or date+time for filenames

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
        if USE_ONLY_DATE:
            self.date = str(date.today())
        else:
            self.date = time.strftime("%y-%m-%d.%H%M", time.localtime())

        # get tool
        self.tool = root.get("tool")
        logging.debug("The tool to be benchmarked is {0}.".format(repr(self.tool)))

        self.rlimits = {}
        keys = list(root.keys())
        if ("memlimit" in keys):
            limit = int(root.get("memlimit")) * BYTE_FACTOR * BYTE_FACTOR
            self.rlimits[resource.RLIMIT_AS] = (limit, limit)
        if ("timelimit" in keys):
            limit = int(root.get("timelimit"))
            self.rlimits[resource.RLIMIT_CPU] = (limit, limit)

        # get global options
        self.options = getOptions(root)

        # get global files, they are tested in all tests
        globalSourcefiles = root.findall("sourcefiles")

        # get benchmarks
        self.tests = []
        for testTag in root.findall("test"):
            self.tests.append(Test(testTag, self, globalSourcefiles))

        # get columns
        self.columns = self.loadColumns(root.find("columns"))


    def loadColumns(self, columnsTag):
        """
        @param columnsTag: the columnsTag from the xml-file
        @return: a list of Columns()
        """

        logging.debug("I'm loading some columns for the outputfile.")
        columns = []
        if columnsTag != None: # columnsTag is optional in xml-file
            for columnTag in columnsTag.findall("column"):
                column = Column(columnTag)
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
        self.runs = self.getRuns(globalSourcefileTags + testTag.findall("sourcefiles"))


    def getRuns(self, sourcefilesTagList):
        '''
        This function returns a list of Runs (filename with options).
        The files and their options are taken from the list of sourcefilesTags.
        '''
        runs = []

        for sourcefilesTag in sourcefilesTagList:
            # get list of filenames
            sourcefiles = self.getSourcefiles(sourcefilesTag)

            # get file-specific options for filenames
            fileOptions = getOptions(sourcefilesTag)

            for sourcefile in sourcefiles:
                runs.append(Run(sourcefile, fileOptions, self))

        return runs


    def getSourcefiles(self, sourcefilesTag):
        sourcefiles = []

        # get included sourcefiles
        for includedFiles in sourcefilesTag.findall("include"):
            sourcefiles += self.getFileList(includedFiles.text)

        # get sourcefiles from list in file
        for includesFilesFile in sourcefilesTag.findall("includesfile"):

            for file in self.getFileList(includesFilesFile.text):
                fileDir = os.path.dirname(file)

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
                        sourcefiles += self.getFileList(line, fileDir)

                fileWithList.close()

        # remove excluded sourcefiles
        for excludedFiles in sourcefilesTag.findall("exclude"):
            excludedFilesList = self.getFileList(excludedFiles.text)
            for excludedFile in excludedFilesList:
                sourcefiles = Util.removeAll(sourcefiles, excludedFile)

        return sourcefiles


    def getFileList(self, shortFile, root=""):
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

        if len(fileList) == 0:

            if root == "":
                logging.warning("No files found matching {0}."
                                .format(repr(shortFile)))

            else: # Fallback for older test-sets
                logging.warning("Perpaps old or invalid test-set. Trying fallback for {0}."
                                .format(repr(shortFileFallback)))
                fileList = self.getFileList(shortFileFallback)
                if len(fileList) != 0:
                    logging.warning("Fallback has found some files for {0}."
                                .format(repr(shortFileFallback)))

        return fileList


class Run():
    """
    A Run contains one sourcefile and options.
    """

    def __init__(self, sourcefile, fileOptions, test):
        self.sourcefile = sourcefile
        self.options = fileOptions
        self.test = test


    def getMergedOptions(self):
        """
        This function returns a list of Strings.
        It contains all options for this Run (global + testwide + local) without 'None'-Values.
        """

        # merge options to list
        currentOptions = mergeOptions(self.test.benchmark.options,
                                      self.test.options,
                                      self.options)

        # replace variables with special values
        currentOptions = substituteVars(currentOptions,
                                        self.test,
                                        self.sourcefile,
                                        self.test.benchmark.logFolder)
        return currentOptions


class Column:
    """
    The class Column sets text, title and numberOfDigits of a column.
    """

    def __init__(self, columnTag):
        # get text
        self.text = columnTag.text

        # get title (title is optional, default: get text)
        self.title = columnTag.get("title", self.text)

        # get number of digits behind comma
        self.numberOfDigits = columnTag.get("numberOfDigits")


class Util:
    """
    This Class contains some useful functions for Strings, XML or Lists.
    """

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


class OutputHandler:
    """
    The class OutputHandler manages all outputs to the terminal and to files.
    """

    def __init__(self, benchmark):
        """
        The constructor of OutputHandler creates the folder to store logfiles
        and collects information about the benchmark and the computer.
        """

        self.benchmark = benchmark
        self.statistics = Statistics()

        # create folder for file-specific log-files.
        # existing files (with the same name) will be OVERWRITTEN!
        self.logFolder = OUTPUT_PATH + self.benchmark.name + "." + self.benchmark.date + ".logfiles/"
        if not os.path.isdir(self.logFolder):
            os.makedirs(self.logFolder)
        self.benchmark.logFolder = self.logFolder # inject benchmark with logFolder

        # get information about computer
        (opSystem, cpuModel, numberOfCores, maxFrequency, memory, hostname) = self.getSystemInfo()
        version = self.getVersion(self.benchmark.tool)

        memlimit = None
        timelimit = None
        if (resource.RLIMIT_AS in self.benchmark.rlimits):
            memlimit = str(self.benchmark.rlimits[resource.RLIMIT_AS][0] // BYTE_FACTOR // BYTE_FACTOR) + " MB"
        if (resource.RLIMIT_CPU in self.benchmark.rlimits):
            timelimit = str(self.benchmark.rlimits[resource.RLIMIT_CPU][0]) + " s"

        self.storeHeaderInXML(version, memlimit, timelimit, opSystem, cpuModel,
                              numberOfCores, maxFrequency, memory, hostname)
        self.writeHeaderToLog(version, memlimit, timelimit, opSystem, cpuModel,
                              numberOfCores, maxFrequency, memory, hostname)

        # write columntitles of tests in CSV-files, this overwrites existing files
        self.CSVFiles = dict()
        CSVLine = CSV_SEPARATOR.join(
                      ["sourcefile", "status", "cputime", "walltime"] \
                    + [column.title for column in self.benchmark.columns])
        for test in benchmark.tests:
            if options.testRunOnly is None \
                    or options.testRunOnly == test.name:
                CSVFileName = self.getFileName(test.name, "csv")
                self.CSVFiles[CSVFileName] = FileWriter(CSVFileName, CSVLine + "\n")


    def storeHeaderInXML(self, version, memlimit, timelimit, opSystem,
                         cpuModel, numberOfCores, maxFrequency, memory, hostname):

        # store benchmarkInfo in XML
        self.XMLHeader = ET.Element("test",
                    {"benchmarkname": self.benchmark.name, "date": self.benchmark.date,
                     "tool": self.getToolnameForPrinting(), "version": version})
        if memlimit is not None:
            self.XMLHeader.set("memlimit", memlimit)
        if timelimit is not None:
            self.XMLHeader.set("timelimit", timelimit)

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
                + "date:".ljust(columnWidth) + self.benchmark.date + "\n"\
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

        # write to file
        self.TXTFile = FileWriter(self.getFileName(None, "txt"), header + systemInfo)


    def getToolnameForPrinting(self):
        tool = self.benchmark.tool.lower()
        names = {'cpachecker': 'CPAchecker',
                 'cbmc'      : 'CBMC',
                 'satabs'    : 'SatAbs',
                 'blast'     : 'BLAST',
                 'wolverine' : 'WOLVERINE',
                 'acsar'     : 'Acsar'}
        if tool in names:
            return names[tool]
        else:
            return str(self.benchmark.tool)

# this function only for development, currently unused
    def getVersionOfCPAchecker(self):
        '''
        get info about CPAchecker from local svn- or git-svn-directory
        '''
        version = ''
        exe = findExecutable("cpachecker", "scripts/cpa.sh")
        try:
            cpaFolder = subprocess.Popen(['which', exe],
                              stdout=subprocess.PIPE).communicate()[0].strip('\n')

            # try to get revision with SVN
            output = subprocess.Popen(['svn', 'info', cpaFolder],
                              stdout=subprocess.PIPE,
                              stderr=subprocess.STDOUT).communicate()[0]

            # parse output and get revision
            svnInfoList = [line for line in output.strip('\n').split('\n')
                           if ': ' in line]
            svnInfo = dict(tuple(str.split(': ')) for str in svnInfoList)

            if 'Revision' in svnInfo: # revision from SVN successful
                version = 'r' + svnInfo['Revision']

            else: # try to get revision with GIT-SVN
                output = subprocess.Popen(['git', 'svn', 'info'],
                                  stdout=subprocess.PIPE,
                                  stderr=subprocess.STDOUT).communicate()[0]

                # parse output and get revision
                svnInfoList = [line for line in output.strip('\n').split('\n')
                               if ': ' in line]
                svnInfo = dict(tuple(str.split(': ')) for str in svnInfoList)

                if 'Revision' in svnInfo: # revision from GIT-SVN successful
                    version = 'r' + svnInfo['Revision']

                else:
                    logging.warning('revision of CPAchecker could not be read.')
        except OSError:
            pass
        return version


    def getVersion(self, tool):
        """
        This function return a String representing the version of the tool.
        """

        version = ''
        if (tool == "cpachecker"):
            exe = findExecutable("cpachecker", "scripts/cpa.sh")
            try:
                versionHelpStr = subprocess.Popen([exe, '-help'],
                    stdout=subprocess.PIPE).communicate()[0]
                version = ' '.join(versionHelpStr.splitlines()[0].split()[1:])  # first word is 'CPAchecker'
            except IndexError:
                logging.critical('IndexError! Have you built CPAchecker?\n') # TODO better message
                sys.exit()

        elif (tool == "cbmc"):
            defaultExe = None
            if platform.machine() == "x86_64":
                defaultExe = "lib/native/x86_64-linux/cbmc"
            elif platform.machine() == "i386":
                defaultExe = "lib/native/x86-linux/cbmc"

            exe = findExecutable("cbmc", defaultExe)
            version = subprocess.Popen([exe, '--version'],
                              stdout=subprocess.PIPE).communicate()[0].strip()

        elif (tool == "satabs"):
            exe = findExecutable("satabs", None)
            version = subprocess.Popen([exe, '--version'],
                              stdout=subprocess.PIPE).communicate()[0].strip()

        elif (tool == "wolverine"):
            exe = findExecutable("wolverine", None)
            version = subprocess.Popen([exe, '--version'],
                              stdout=subprocess.PIPE).communicate()[0].split()[1].strip()

        elif (tool == "blast"):
            exe = findExecutable("pblast.opt", None)
            version = subprocess.Popen([exe],
                              stdout=subprocess.PIPE,
                              stderr=subprocess.STDOUT).communicate()[0][6:9]

        return Util.decodeToString(version)


    def getSystemInfo(self):
        """
        This function returns some information about the computer.
        """

        # get info about OS
        import os
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

        # length of the first column in terminal
        self.maxLengthOfFileName = 20
        for run in self.test.runs:
            self.maxLengthOfFileName = max(len(run.sourcefile), self.maxLengthOfFileName)

        # write testname to terminal
        numberOfFiles = len(self.test.runs)
        print("\nrunning test" + \
            (" '" + test.name + "'" if test.name is not None else "") + \
            ("     (1 file)" if numberOfFiles == 1
                        else "     ({0} files)".format(numberOfFiles)))

        # store testname and options in XML,
        # copy benchmarkinfo, limits, columntitles, systeminfo from XMLHeader
        self.testElem = Util.getCopyOfXMLElem(self.XMLHeader)
        testOptions = mergeOptions(self.benchmark.options, self.test.options)
        self.testElem.set("options", " ".join(testOptions))
        if self.test.name is not None:
            self.testElem.set("name", self.test.name)

        # write information about the test into TXTFile
        self.writeTestInfoToLog()


    def outputForSkippingTest(self, test):
        '''
        This function writes a simple message to terminal and logfile,
        when a test is skipped.
        There is no message about skipping a test in the xml-file.
        '''

        # print to terminal
        print("\nskipping test" + \
            (" '" + test.name + "'" if test.name is not None else ""))

        # write into TXTFile
        numberOfTest = self.benchmark.tests.index(test) + 1
        testInfo = "\n\n"
        if test.name is not None:
            testInfo += test.name + "\n"
        testInfo += "test {0} of {1}: skipped\n".format(
                numberOfTest, len(self.benchmark.tests))
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

        titleLine = self.createOutputLine("sourcefile", "status", "cpu time",
                            "wall time", self.benchmark.columns, True)

        self.test.simpleLine = "-" * (len(titleLine)) + "\n"

        # write into TXTFile
        self.TXTFile.append("\n\n" + testInfo + titleLine + "\n" + self.test.simpleLine)


    def outputBeforeRun(self, sourcefile, currentOptions):
        """
        The method outputBeforeRun() prints the name of a file to terminal.
        It returns the name of the logfile.
        @param sourcefile: the name of a sourcefile
        """

        options = " ".join(currentOptions)
        logging.debug("I'm running '{0} {1} {2}'.".format(
            self.getToolnameForPrinting(), options, sourcefile))

        # output in terminal
        sys.stdout.write(time.strftime("%H:%M:%S", time.localtime()) \
            + '   ' + sourcefile.ljust(self.maxLengthOfFileName + 4))
        sys.stdout.flush()

        # write output to file-specific log-file
        logfileName = self.logFolder
        if self.test.name is not None:
            logfileName += self.test.name + "."
        logfileName += os.path.basename(sourcefile) + ".log"
        
        return logfileName


    def outputAfterRun(self, sourcefile, fileOptions, status,
                       cpuTimeDelta, wallTimeDelta, args):
        """
        The method outputAfterRun() prints filename, result, time and status
        of a test to terminal and stores all data in XML
        """

        # format times, type is changed from float to string!
        cpuTimeDelta = Util.formatNumber(cpuTimeDelta, TIME_PRECISION)
        wallTimeDelta = Util.formatNumber(wallTimeDelta, TIME_PRECISION)

        # format numbers, numberOfDigits is optional, so it can be None
        for column in self.benchmark.columns:
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
        statusRelation = self.isCorrectResult(sourcefile, status)
        if USE_COLORS and sys.stdout.isatty(): # is terminal, not file
            statusStr = COLOR_DIC[statusRelation].format(status.ljust(8))
        else:
            statusStr = status.ljust(8)
        print(statusStr + cpuTimeDelta.rjust(8) + wallTimeDelta.rjust(8))

        # store filename, status, times, columns in XML
        fileElem = ET.Element("sourcefile", {"name": sourcefile})
        if len(fileOptions) != 0:
            fileElem.set("options", " ".join(mergeOptions(fileOptions)))
        fileElem.append(ET.Element("column", {"title": "status", "value": status}))
        fileElem.append(ET.Element("column", {"title": "cputime", "value": cpuTimeDelta}))
        fileElem.append(ET.Element("column", {"title": "walltime", "value": wallTimeDelta}))

        for column in self.benchmark.columns:
            fileElem.append(ET.Element("column",
                    {"title": column.title, "value": column.value}))

        self.testElem.append(fileElem)

        # write resultline in TXTFile
        resultline = self.createOutputLine(sourcefile, status,
                    cpuTimeDelta, wallTimeDelta, self.benchmark.columns, False)
        self.TXTFile.append(resultline + "\n")

        # write columnvalues of the test into CSVFile
        CSVLine = CSV_SEPARATOR.join(
                  [sourcefile, status, cpuTimeDelta, wallTimeDelta] \
                + [column.value for column in self.benchmark.columns])
        self.CSVFiles[self.getFileName(self.test.name, "csv")].append(CSVLine + "\n")

        self.statistics.addResult(statusRelation)


    def outputAfterTest(self, cpuTimeTest, wallTimeTest):
        """
        The method outputAfterTest() stores the times of a test in XML.
        @params cpuTimeTest, wallTimeTest: times of the test
        """

        # format time, type is changed from float to string!
        cpuTimeTest = Util.formatNumber(cpuTimeTest, TIME_PRECISION)
        wallTimeTest = Util.formatNumber(wallTimeTest, TIME_PRECISION)

        # store testtime in XML
        timesElem = ET.Element("time", {"cputime": cpuTimeTest, "walltime": wallTimeTest})
        self.testElem.append(timesElem)

        # write XML-file
        FileWriter(self.getFileName(self.test.name, "xml"), Util.XMLtoString(self.testElem))

        # write endline into TXTFile
        numberOfFiles = len(self.test.runs)
        numberOfTest = self.benchmark.tests.index(self.test) + 1
        if numberOfFiles == 1:
            endline = ("test {0} consisted of 1 sourcefile.".format(numberOfTest))
        else:
            endline = ("test {0} consisted of {1} sourcefiles.".format(
                    numberOfTest, numberOfFiles))

        endline = self.createOutputLine(endline, "done", cpuTimeTest,
                             wallTimeTest, [], False)

        self.TXTFile.append(self.test.simpleLine + endline + "\n")


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


    def createOutputLine(self, sourcefile, status, cpuTimeDelta, wallTimeDelta, columns, isFirstLine):
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

        outputLine = sourcefile.ljust(self.maxLengthOfFileName + 4) + \
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
        print ('\n'.join(['\nStatistics:' + str(self.dic["counter"]).rjust(13) + ' Files',
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


def findExecutable(program, default):
    def isExecutable(programPath):
        return os.path.isfile(programPath) and os.access(programPath, os.X_OK)

    dirs = os.environ['PATH'].split(os.pathsep)
    dirs.append(".")

    for dir in dirs:
        name = os.path.join(dir, program)
        if isExecutable(name):
            return name

    if default is not None and isExecutable(default):
        return default

    sys.exit("ERROR: Could not find %s executable" % program)


def killSubprocess(process):
    '''
    this function kills the process and the children in its group.
    '''
    os.killpg(process.pid, signal.SIGTERM)


def run(args, rlimits, outputfilename):
    args = [os.path.expandvars(arg) for arg in args]
    args = [os.path.expanduser(arg) for arg in args]

    outputfile = open(outputfilename, 'w') # override existing file
    outputfile.write(' '.join(args) + '\n\n\n' + '-'*80 + '\n\n\n')
    outputfile.flush()

    def preSubprocess():
        os.setpgrp() # make subprocess to group-leader
        for rsrc in rlimits:
            resource.setrlimit(rsrc, rlimits[rsrc])

    wallTimeBefore = time.time()

    try:
        p = subprocess.Popen(args,
                             stdout=outputfile, stderr=outputfile,
                             preexec_fn=preSubprocess)

        # if rlimit does not work, a seperate Timer is started to kill the subprocess,
        # Timer has 10 seconds 'overhead'
        if (resource.RLIMIT_CPU in rlimits):
          timelimit = rlimits[resource.RLIMIT_CPU][0]
          timer = Timer(timelimit + 10, killSubprocess, [p])
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

    except KeyboardInterrupt:
        print ("killing subprocess...")
        killSubprocess(p)
        raise KeyboardInterrupt # throw again to stop script

    finally:
        if (resource.RLIMIT_CPU in rlimits) and timer.isAlive():
            timer.cancel()

    wallTimeAfter = time.time()
    wallTimeDelta = wallTimeAfter - wallTimeBefore
    cpuTimeDelta = (ru_child.ru_utime + ru_child.ru_stime)

    logging.debug("My subprocess returned returncode {0}.".format(returncode))

 
    outputfile.close() # normally subprocess closes file, we do this again

    outputfile = open(outputfilename, 'r') # re-open file for reading output
    output = ''.join(outputfile.readlines()[6:]) # first 6 lines are for logging, rest is output of subprocess
    outputfile.close()
    output = Util.decodeToString(output)

    return (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta)


def isTimeout(cpuTimeDelta, rlimits):
    ''' try to find out whether the tool terminated because of a timeout '''
    if resource.RLIMIT_CPU in rlimits:
        limit = rlimits.get(resource.RLIMIT_CPU)[0]
    else:
        limit = float('inf')

    return cpuTimeDelta > limit*0.99


def run_cbmc(options, sourcefile, columns, rlimits, file):
    if ("--xml-ui" not in options):
        options = options + ["--xml-ui"]

    defaultExe = None
    if platform.machine() == "x86_64":
        defaultExe = "lib/native/x86_64-linux/cbmc"
    elif platform.machine() == "i386":
        defaultExe = "lib/native/x86-linux/cbmc"

    exe = findExecutable("cbmc", defaultExe)
    args = [exe] + options + [sourcefile]
    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, file)

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

                if any(map(isErrorMessage, tree.getiterator('message'))):
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
                
        except Exception, e: # catch all exceptions
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


def run_satabs(options, sourcefile, columns, rlimits, file):
    exe = findExecutable("satabs", None)
    args = [exe] + options + [sourcefile]

    def doNothing(signum, frame):
        pass

    # on timeout of rlimit the signal SIGTERM is thrown, catch and ignore it
    signal.signal(signal.SIGTERM, doNothing)

    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, file)

    # reset signal-handler
    signal.signal(signal.SIGTERM, signal_handler_ignore)

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


def run_wolverine(options, sourcefile, columns, rlimits, file):
    exe = findExecutable("wolverine", None)
    args = [exe] + options + [sourcefile]
    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, file)
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


def run_acsar(options, sourcefile, columns, rlimits, file):
    exe = findExecutable("acsar", None)

    # create tmp-files for acsar, acsar needs special error-labels
    prepSourcefile = prepareSourceFileForAcsar(sourcefile)

    if ("--mainproc" not in options):
        options = options + ["--mainproc", "main"]

    args = [exe] + ["--file"] + [prepSourcefile] + options

    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, file)
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


# the next 3 functions are for imaginary tools, that return special results,
# perhaps someone can use these function again someday,
# to use them you need a normal benchmark-xml-file 
# with the tool and sourcefiles, however options are ignored
def run_safe(options, sourcefile, columns, rlimits, file):
    args = ['safe'] + options + [sourcefile]
    cpuTimeDelta = wallTimeDelta = 0
    return ('safe', cpuTimeDelta, wallTimeDelta, args)

def run_unsafe(options, sourcefile, columns, rlimits, file):
    args = ['unsafe'] + options + [sourcefile]
    cpuTimeDelta = wallTimeDelta = 0
    return ('unsafe', cpuTimeDelta, wallTimeDelta, args)

def run_random(options, sourcefile, columns, rlimits, file):
    args = ['random'] + options + [sourcefile]
    cpuTimeDelta = wallTimeDelta = 0
    from random import random
    status = 'safe' if random() < 0.5 else 'unsafe'
    return (status, cpuTimeDelta, wallTimeDelta, args)


def run_cpachecker(options, sourcefile, columns, rlimits, file):
    if ("-stats" not in options):
        options = options + ["-stats"]

    exe = findExecutable("cpachecker", "scripts/cpa.sh")
    args = [exe] + options + [sourcefile]
    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, file)

    status = getCPAcheckerStatus(returncode, returnsignal, output, rlimits, cpuTimeDelta)
    getCPAcheckerColumns(output, columns)

    return (status, cpuTimeDelta, wallTimeDelta, args)


def getCPAcheckerStatus(returncode, returnsignal, output, rlimits, cpuTimeDelta):
    """
    @param returncode: code returned by CPAchecker
    @param returnsignal: signal, which terminated CPAchecker
    @param output: the output of CPAchecker
    @return: status of CPAchecker after running a testfile
    """

    def isOutOfMemory(line):
        return ('java.lang.OutOfMemoryError' in line
             or 'std::bad_alloc'             in line # C++ out of memory exception
             or 'Cannot allocate memory'     in line
             or line.startswith('out of memory')
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
        if isOutOfMemory(line):
            status = 'OUT OF MEMORY'
        elif 'SIGSEGV' in line:
            status = 'SEGMENTATION FAULT'
        elif (returncode == 0 or returncode == 1) and ('Exception' in line):
            status = 'EXCEPTION'
        elif 'Could not reserve enough space for object heap' in line:
            status = 'JAVA HEAP ERROR'
        elif (status is None) and line.startswith('Verification result: '):
            line = line[21:].strip()
            if line.startswith('SAFE'):
                status = 'SAFE'
            elif line.startswith('UNSAFE'):
                status = 'UNSAFE'
            else:
                status = 'UNKNOWN'
        if (status is None) and line.startswith('#Test cases computed:'):
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


def run_blast(options, sourcefile, columns, rlimits, file):
    exe = findExecutable("pblast.opt", None)
    args = [exe] + options + [sourcefile]
    (returncode, returnsignal, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits, file)

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


def runBenchmark(benchmarkFile):
    benchmark = Benchmark(benchmarkFile)

    assert benchmark.tool in ["cbmc", "satabs", "cpachecker", "blast", "acsar", "wolverine",
                              "safe", "unsafe", "random"]
    run_func = eval("run_" + benchmark.tool)

    if len(benchmark.tests) == 1:
        logging.debug("I'm benchmarking {0} consisting of 1 test.".format(repr(benchmarkFile)))
    else:
        logging.debug("I'm benchmarking {0} consisting of {1} tests.".format(
                repr(benchmarkFile), len(benchmark.tests)))

    outputHandler = OutputHandler(benchmark)

    for test in benchmark.tests:

        if options.testRunOnly is not None \
                and options.testRunOnly != test.name:
            outputHandler.outputForSkippingTest(test)

        else:
            # get times before test
            ruBefore = resource.getrusage(resource.RUSAGE_CHILDREN)
            wallTimeBefore = time.time()

            outputHandler.outputBeforeTest(test)

            for run in test.runs:

                currentOptions = run.getMergedOptions()

                logfile = outputHandler.outputBeforeRun(run.sourcefile, currentOptions)

                # run test
                (status, cpuTimeDelta, wallTimeDelta, args) = \
                    run_func(currentOptions, run.sourcefile, benchmark.columns, benchmark.rlimits, logfile)

                outputHandler.outputAfterRun(run.sourcefile, run.options, status,
                                             cpuTimeDelta, wallTimeDelta, args)

            # get times after test
            wallTimeAfter = time.time()
            wallTimeTest = wallTimeAfter - wallTimeBefore
            ruAfter = resource.getrusage(resource.RUSAGE_CHILDREN)
            cpuTimeTest = (ruAfter.ru_utime + ruAfter.ru_stime)\
            - (ruBefore.ru_utime + ruBefore.ru_stime)

            outputHandler.outputAfterTest(cpuTimeTest, wallTimeTest)

    outputHandler.outputAfterBenchmark()


def main(argv=None):

    if argv is None:
        argv = sys.argv
    from optparse import OptionParser
    parser = OptionParser(usage="usage: %prog [OPTION]... [FILE]...\n\n" + \
        "INFO: documented example-files can be found in 'doc/examples'\n")

    parser.add_option("-d", "--debug",
                      action="store_true",
                      help="enable debug output")

    parser.add_option("-t", "--test", dest="testRunOnly",
                      help="run only a special TEST from xml-file",
                      metavar="TEST")

    parser.add_option("-o", "--outputpath",
                      dest="output_path", type="string",
                      default="./test/results/",
                      help="Output folder for the generated results")

    global options, OUTPUT_PATH
    (options, args) = parser.parse_args(argv)
    OUTPUT_PATH = options.output_path

    if len(args) < 2:
        parser.error("invalid number of arguments")
    if (options.debug):
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                            level=logging.DEBUG)
    else:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s")

    for arg in args[1:]:
        if not os.path.exists(arg) or not os.path.isfile(arg):
            parser.error("File {0} does not exist.".format(repr(arg)))

    for arg in args[1:]:
        logging.debug("Benchmark {0} is started.".format(repr(arg)))
        runBenchmark(arg)
        logging.debug("Benchmark {0} is done.".format(repr(arg)))

    logging.debug("I think my job is done. Have a nice day!")


def signal_handler_ignore(signum, frame):
    logging.warn('Received signal %d, ignoring it' % signum)

if __name__ == "__main__":
    # ignore SIGTERM
    signal.signal(signal.SIGTERM, signal_handler_ignore)
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print ("\n\nscript was interrupted by user, some tests may not be done")
