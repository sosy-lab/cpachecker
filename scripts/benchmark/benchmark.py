#!/usr/bin/env python

from string import Template
from xml.etree.ElementTree import ElementTree
from xml.parsers.expat import ExpatError
from datetime import date
from collections import defaultdict

import time
import glob
import logging
import os.path
import platform
import resource
import signal
import subprocess
import sys
import xml.etree.ElementTree as ET
import json

OUTPUT_PATH = "./test/results/"

CSV_SEPARATOR = "\t"

# the number of digits after the decimal separator of the time column,
# for the other columns it can be configured in the xml-file
TIME_PRECISION = 2


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

        ## looks like trouble with pyxml, better use lxml (http://codespeak.net/lxml/).
        # try:
        #     from xml.parsers.xmlproc  import xmlval
        #     validator = xmlval.XMLValidator()
        #     validator.parse_resource(benchmarkFile)
        # except ImportError:
        #     logging.debug("I cannot import xmlval so I'm skipping the validation.")
        #     logging.debug("If you want xml validation please install pyxml.")
        logging.debug("I'm loading the benchmark {0}.".format(benchmarkFile))
        tree = ElementTree()
        root = tree.parse(benchmarkFile)

        # get benchmark-name
        self.name = os.path.basename(benchmarkFile)[:-4] # remove ending ".xml"

        # get tool
        self.tool = (root.get("tool"))
        logging.debug("The tool to be benchmarked is {0}.".format(repr(self.tool)))

        self.rlimits = {}
        if ("memlimit" in root.keys()):
            limit = int(root.get("memlimit")) * 1024 * 1024
            self.rlimits[resource.RLIMIT_AS] = (limit, limit)
        if ("timelimit" in root.keys()):
            limit = int(root.get("timelimit"))
            self.rlimits[resource.RLIMIT_CPU] = (limit, limit)

        # get benchmarks
        self.tests = []
        for testTag in root.findall("test"):
            self.tests.append(Test(testTag))

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

    def __init__(self, testTag):
        """
        The constructor of Test reads testname and the filenames from testTag.
        Filenames can be included or excluded, and imported from a list of 
        names in another file. Wildcards and variables are expanded.
        @param testTag: a testTag from the xml-file
        """

        # get name of test, name is optional, the result can be "None"
        self.name = testTag.get("name")

        # get all sourcefiles
        self.sourcefiles = []
        for sourcefilesTag in testTag.findall("sourcefiles"):
            currentSourcefiles = []

            # get included sourcefiles
            for includedFiles in sourcefilesTag.findall("include"):
                includedFilesList = self.getFileList(includedFiles.text)
                currentSourcefiles += includedFilesList

            # get sourcefiles from list in file
            for includesFilesFile in sourcefilesTag.findall("includesfile"):

                for file in self.getFileList(includesFilesFile.text):

                    # read files from list
                    fileWithList = open(file,"r")
                    for line in fileWithList:
                        # strip() removes 'newline' behind the line
                        currentSourcefiles += self.getFileList(line.strip())
                    fileWithList.close()

            # remove excluded sourcefiles
            for excludedFiles in sourcefilesTag.findall("exclude"):
                excludedFilesList = self.getFileList(excludedFiles.text)
                for file in excludedFilesList:
                    while file in currentSourcefiles:
                        currentSourcefiles.remove(file)

            # collect all sourcefiles from all sourcefilesTags
            self.sourcefiles += currentSourcefiles

        # get all options
        self.options = []
        for option in testTag.find("options").findall("option"):
            self.options.append(option.get("name"))
            if option.text is not None:
                self.options.append(option.text)


    def getFileList(self, shortFile):
        """
        The function getFileList expands a short filename to a sorted list 
        of filenames. The short filename can contain variables and wildcards.
        """

        # expand tilde and variables
        expandedFile = os.path.expandvars(os.path.expanduser(shortFile))

        # expand wildcards
        fileList = glob.glob(expandedFile)

        # sort alphabetical, 
        # if list is emtpy, sorting returns None, so better do not sort
        if len(fileList) != 0:
            fileList.sort()

        if expandedFile != shortFile:
            logging.debug("I expanded a tilde and/or shell variables in expression {0} to {1}."
                .format(repr(shortFile), repr(expandedFile))) 
        if len(fileList) == 0:
            logging.warning("I found no files matching {0}."
                .format(repr(shortFile)))

        return fileList


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


class OutputHandler:
    """
    The class OutputHandler manages all outputs to the terminal and to files.
    """

    def __init__(self, benchmark):
        """
        The constructor of OutputHandler initializes some variables
        (logFolder, outputLogFileName, outputCSVFileName)
        and prints the heads into log- and CSV-file.
        """

        self.benchmark = benchmark

        # create folder for file-specific log-files.
        # if the folder exists, it will be used.
        # if there are files in the folder (with the same name than the testfiles), 
        # they will be OVERWRITTEN without a message!
        self.logFolder = OUTPUT_PATH + self.benchmark.name + ".logfiles." + str(date.today()) + "/"
        if not os.path.isdir(self.logFolder):
            os.mkdir(self.logFolder)

        # create header of outputLogFile
        columnWidth = 20
        headLine = "   BENCHMARK INFORMATION\n" +\
                    "benchmark:".ljust(columnWidth) + self.benchmark.name + "\n"
        dateLine = "date:".ljust(columnWidth) + str(date.today()) + "\n"
        toolLine = "tool:".ljust(columnWidth) + self.benchmark.tool +\
                    " " + self.getVersion(self.benchmark.tool) + "\n"
  
        if (9 in self.benchmark.rlimits): # 9 is key of memlimit, convert value to MB
            toolLine += "memlimit:".ljust(columnWidth)\
                        + str(self.benchmark.rlimits[9][0] / 1024 / 1024) + "\n"
        if (0 in self.benchmark.rlimits): # 0 is key of timelimit
            toolLine += "timelimit:".ljust(columnWidth)\
                        + str(self.benchmark.rlimits[0][0]) + "\n"
        simpleLine = "-" * (len(headLine) + 15) + "\n"
        header = headLine + dateLine + toolLine + simpleLine + "\n"
        
        # get information about computer
        (opSystem, cpuModel, numberOfCores, maxFrequency, memory) = self.getSystemInfo()
        systemInfo = "   SYSTEM INFORMATION\n"\
                    + "os:".ljust(columnWidth) + opSystem + "\n"\
                    + "cpu:".ljust(columnWidth) + cpuModel + "\n"\
                    + "- cores:".ljust(columnWidth) + numberOfCores + "\n"\
                    + "- max frequency:".ljust(columnWidth) + maxFrequency + "\n"\
                    + "ram:".ljust(columnWidth) + memory + "\n"\
                    + simpleLine

        # create outputLogFile
        # if the file exist, it will be OVERWRITTEN without a message!
        outputLogFileName = OUTPUT_PATH + self.benchmark.name + ".results." + str(date.today()) + ".txt"
        self.outputLog = FileWriter(outputLogFileName, header + systemInfo)
        logging.debug("OutputLogFile {0} created.".format(repr(outputLogFileName)))

        # create outputCSVFile with titleLine
        # if the file exist, it will be OVERWRITTEN without a message!
        outputCSVFileName = OUTPUT_PATH + self.benchmark.name + ".results." + str(date.today()) + ".csv"
        CSVtitleLine = CSV_SEPARATOR.join(["sourcefile", "status", "cpu time", "wall time"])
        for column in self.benchmark.columns:
            CSVtitleLine += CSV_SEPARATOR + column.title
        self.outputCSV = FileWriter(outputCSVFileName, CSVtitleLine + "\n")
        logging.debug("OutputCSVFile {0} created.".format(repr(outputCSVFileName)))

        # resultSet for JSON-file
        if options.json:
            global resultSet    # 'global' only for printing after keyboard-interrupt
            resultSet = ResultSet()
            self.resultSet = resultSet


    def getVersion(self, tool):
        """
        This function return a String representing the version of the tool.
        """

        version = ""
        if (tool == "cpachecker"):

            # get info about the local svn-directory of CPAchecker
            exe = findExecutable("cpachecker", "scripts/cpa.sh")
            cpaFolder = subprocess.Popen(['which', exe], 
                                  stdout=subprocess.PIPE).communicate()[0].strip('\n')
            output = subprocess.Popen(['svn', 'info', cpaFolder], 
                                  stdout=subprocess.PIPE).communicate()[0]
                                  
            # parse output and get revision
            svnInfo = dict(map(lambda str: tuple(str.split(': ')),
                        output.strip('\n').split('\n')))
            version = ""
            if 'Revision' in svnInfo:
                version = "(Revision: " + svnInfo['Revision'] + ")"

        elif (tool == "cbmc") or (tool == "satabs"):
            exe = findExecutable(tool, None)
            version += subprocess.Popen([exe, '--version'], 
                              stdout=subprocess.PIPE).communicate()[0].strip()                

        return version

    def getSystemInfo(self):
        """
        This function returns some information about the computer.
        """

        # get info about OS
        import os
        (sysname, name, kernel, version, machine) = os.uname()
        opSystem = sysname + " " + kernel + " " + machine

        # get info about CPU
        cpuInfoFile = open('/proc/cpuinfo', "r")
        cpuInfo = dict(map(lambda str: tuple(str.split(':')),
                            cpuInfoFile.read()
                            .replace('\n\n','\n').replace('\t','')
                            .strip('\n').split('\n')))
        cpuInfoFile.close()

        if 'model name' in cpuInfo:
            cpuModel = cpuInfo['model name'].strip()
        if 'cpu cores' in cpuInfo:
            numberOfCores = cpuInfo['cpu cores'].strip()

        # modern cpus may not work with full speed the whole day
        # read list of frequencies from file and take first number
        frequencyInfoFile = open('/sys/devices/system/cpu/cpu0/cpufreq/'\
                              + 'scaling_available_frequencies', "r")
        maxFrequency = frequencyInfoFile.read().strip('\n').split()[0]
        frequencyInfoFile.close()
        maxFrequency = str(int(maxFrequency) / 1000) + ' MHz'

        # get info about memory
        memInfoFile = open('/proc/meminfo', "r")
        memInfo = dict(map(lambda str: tuple(str.split(': ')),
                            memInfoFile.read()
                            .replace('\t','')
                            .strip('\n').split('\n')))
        memInfoFile.close()

        if 'MemTotal' in memInfo:
            memTotal = memInfo['MemTotal'].strip()

        return (opSystem, cpuModel, numberOfCores, maxFrequency, memTotal)


    def outputBeforeTest(self, test):
        """
        The method outputBeforeTest() prints the head of a test into the log-file.
        @param test: current test with a list of testfiles
        """

        self.test = test
        numberOfTest = self.benchmark.tests.index(self.test) + 1

        if len(self.test.sourcefiles) == 1:
            logging.debug("The {0} test consists of 1 sourcefile.".format(
                    self.ordinalNumeral(numberOfTest)))
        else:
            logging.debug("The {0} test consists of {1} sourcefiles.".format(
                    self.ordinalNumeral(numberOfTest),
                    len(self.test.sourcefiles)))

        # values for the table with the results of currentTest
        self.maxLengthOfFileName = 40
        for sourcefile in self.test.sourcefiles:
            self.maxLengthOfFileName = max(len(sourcefile), self.maxLengthOfFileName)

        # write headline and columntitles for currentTest to file
        options = " ".join(self.test.options)
        optionLine = "\n\n"
        if self.test.name is not None:
            optionLine += self.test.name + "\n"
        optionLine += "test {0} of {1} with options: {2}\n\n".format(
                    numberOfTest, len(self.benchmark.tests), options)
        titleLine = self.createOutputLine("sourcefile", "status", 
                                     "cpu time", "wall time", self.benchmark.columns, True)
        self.simpleLine = "-" * (len(titleLine)) + "\n"
        self.outputLog.append(optionLine + titleLine + "\n" + self.simpleLine)


    def outputBeforeRun(self, sourcefile):
        """
        The method outputBeforeRun() prints the name of a file to terminal.
        @param sourcefile: the name of a sourcefile
        """

        options = " ".join(self.test.options)
        logging.debug("I'm running '{0} {1} {2}'.".format(
            self.benchmark.tool, options, sourcefile))

        # output in terminal
        sys.stdout.write(sourcefile.ljust(self.maxLengthOfFileName + 4))
        sys.stdout.flush()


    def outputAfterRun(self, sourcefile, status, cpuTimeDelta, wallTimeDelta, output):
        """
        The method outputAfterRun() prints (filename,) result, time and status 
        of a test to terminal and into the log and CSV-file. 
        The output is written to a file-specific log-file.
        """

        # format times, type is changed from float to string!
        cpuTimeDelta = self.formatNumber(cpuTimeDelta, TIME_PRECISION)
        wallTimeDelta = self.formatNumber(wallTimeDelta, TIME_PRECISION)

        # format numbers, numberOfDigits is optional, so it can be None
        for column in self.benchmark.columns:
            if column.numberOfDigits is not None:

                # if the number ends with "s" or another letter, remove it
                if (not column.value.isdigit()) and column.value[-2:-1].isdigit():
                    column.value = column.value[:-1]

                try:
                    floatValue = float(column.value)
                    column.value = self.formatNumber(floatValue, column.numberOfDigits)
                except ValueError: # if value is no float, don't format it
                    pass

        # write output to file-specific log-file
        logFileName = self.logFolder
        if self.test.name is not None:
            logFileName += self.test.name + "."
        logFileName += os.path.basename(sourcefile) + ".log"
        FileWriter(logFileName, output)

        # output in terminal/console
        print status.ljust(8) + cpuTimeDelta.rjust(8) + wallTimeDelta.rjust(8)

        # output in log-file
        self.outputLog.append(self.createOutputLine(sourcefile, status, 
                            cpuTimeDelta, wallTimeDelta, self.benchmark.columns, False) + "\n")

        # output in CSV-file
        outputCSVLine = CSV_SEPARATOR.join([sourcefile, status, cpuTimeDelta, wallTimeDelta])
        for column in self.benchmark.columns:
            outputCSVLine += CSV_SEPARATOR + column.value
        self.outputCSV.append(outputCSVLine + "\n")
        
        # store result for JSON-file
        if options.json:
            self.resultSet.addResult(sourcefile, self.benchmark.tool, self.test.name, {
                          'status':         status,
                          'cpuTimeDelta':   cpuTimeDelta,
                          'wallTimeDelta':  wallTimeDelta
                          })


    def outputAfterTest(self, cpuTimeTest, wallTimeTest):
        """
        The method outputAfterTest() prints number of files and the time of a 
        test into the log-file. 
        @params cpuTimeTest, wallTimeTest: times of the test
        """

        # format time, type is changed from float to string!
        cpuTimeTest = self.formatNumber(cpuTimeTest, TIME_PRECISION)
        wallTimeTest = self.formatNumber(wallTimeTest, TIME_PRECISION)

        numberOfTest = self.benchmark.tests.index(self.test) + 1

        if len(self.test.sourcefiles) == 1:
            endline = ("The {0} test consisted of 1 sourcefile.".format(
                    self.ordinalNumeral(numberOfTest)))
        else:
            endline = ("The {0} test consisted of {1} sourcefiles.".format(
                    self.ordinalNumeral(numberOfTest), len(self.test.sourcefiles)))
        endline = self.createOutputLine(endline, "done", cpuTimeTest, wallTimeTest, [], False) + "\n"
        self.outputLog.append(self.simpleLine + endline)

        # write output to JSON-file
        if (options.json):
            outputJSONFileName = OUTPUT_PATH + self.benchmark.name + ".results."\
                                + str(date.today()) + ".json"
            FileWriter(outputJSONFileName, self.resultSet.toJson())


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

        outputLine = sourcefile.ljust(self.maxLengthOfFileName + 4) +\
                     status.ljust(lengthOfStatus) +\
                     cpuTimeDelta.rjust(lengthOfTime) +\
                     wallTimeDelta.rjust(lengthOfTime)

        for column in columns:
            columnLength = max(minLengthOfColumns, len(column.title)) + 2

            if isFirstLine: 
                value = column.title
            else:
                value = column.value

            outputLine = outputLine + str(value).rjust(columnLength)

        return outputLine


    def ordinalNumeral(self, number):
        last_cipher = number % 10
        if last_cipher == 1:
            return "{0}st".format(number)
        elif last_cipher == 2:
            return "{0}nd".format(number)
        elif last_cipher == 3:
            return "{0}rd".format(number)
        else:
            return "{0}th".format(number)
    
    
    def formatNumber(self, number, numberOfDigits):
            """
            The function formatNumber() return a string-representation of a number
            with a number of digits after the decimal separator.
            If the number has more digits, it is rounded.
            If the number has less digits, zeros are added.
    
            @param number: the number to format
            @param digits: the number of digits
            """
    
            return "%.{0}f".format(numberOfDigits) % number

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


class ResultSet:
    """
    The class ResultSet is a collection of all results of a whole benchmark execution
    """

    __resultSet = defaultdict(lambda: defaultdict())

    def __init__(self):
        """
        The constructor of ResultSet
        """

    def addResult(self, sourceFileName, toolName, testName, result):
        """
        This method adds a result, associated to a source file and tool, to the result set.

        @param sourceFileName: the name of the source file associated with the result
        @param sourceFileName: the name of the tool associated with the result
        @param sourceFileName: the result to store
        """
        self.__resultSet[sourceFileName][toolName + "_" + testName] = result

    def toJson(self):
        """
        This method returns a human-readable JSON-formatted string representation of the result set

        @return: a human-readable JSON-formatted string representation of the result set
        """

        return json.dumps(self.__resultSet, sort_keys=True, indent=4)


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
    
    raise LookupError("Could not find %s executable" % program) 


def run(args, rlimits):
    args = map(lambda arg: os.path.expandvars(arg), args)
    args = map(lambda arg: os.path.expanduser(arg), args)
    def setrlimits():
        for rsrc, limits in rlimits.items():
            resource.setrlimit(rsrc, limits)

    ru_before = resource.getrusage(resource.RUSAGE_CHILDREN)
    wallTimeBefore = time.time()

    try:
        p = subprocess.Popen(args,
                             stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                             preexec_fn=setrlimits)
    except OSError:
        logging.critical("I caught an OSError. Assure that the directory "
                         + "containing the tool to be benchmarked is included "
                         + "in the PATH environment variable or an alias is set.")
        sys.exit("A critical exception caused me to exit non-gracefully. Bye.")
    
    output = p.stdout.read()
    returncode = p.wait()

    wallTimeAfter = time.time()
    wallTimeDelta = wallTimeAfter - wallTimeBefore
    ru_after = resource.getrusage(resource.RUSAGE_CHILDREN)
    cpuTimeDelta = (ru_after.ru_utime + ru_after.ru_stime)\
        - (ru_before.ru_utime + ru_before.ru_stime)

    logging.debug("My subprocess returned returncode {0}.".format(returncode))
    return (returncode, output, cpuTimeDelta, wallTimeDelta)


def run_cbmc(options, sourcefile, columns, rlimits):
    if ("--xml-ui" not in options):
        options = options + ["--xml-ui"]
        
    defaultExe = None
    if platform.machine() == "x86_64":
        defaultExe = "lib/native/x86_64-linux/cbmc"
    elif platform.machine() == "i386":
        defaultExe = "lib/native/x86-linux/cbmc"
    
    exe = findExecutable("cbmc", defaultExe)
    args = [exe] + options + [sourcefile]
    (returncode, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits)
    
    #an empty tag cannot be parsed into a tree
    output = output.replace("<>", "<emptyTag>")
    output = output.replace("</>", "</emptyTag>")
    
    if ((returncode == 0) or (returncode == 10)):
        try:
            tree = ET.fromstring(output)
            status = tree.findtext('cprover-status', 'ERROR')
        except ExpatError as e:
            status = 'ERROR'
            sys.stdout.write("Error parsing CBMC output: %s " % e)

        if status == "FAILURE":
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

    elif returncode == -9:
        status = "TIMEOUT"
    elif returncode == 134:
        status = "ABORTED"
    elif returncode == 137:
        status = "KILLED BY SIGNAL 9"
    elif returncode == 143:
        status = "KILLED"
    else:
        status = "ERROR ({0})".format(returncode)

    return (status, cpuTimeDelta, wallTimeDelta, output)


def run_satabs(options, sourcefile, columns, rlimits):
    exe = findExecutable("satabs", None)
    args = [exe] + options + [sourcefile]
    (returncode, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits)
    if "VERIFICATION SUCCESSFUL" in output:
        status = "SUCCESS"
    else:
        status = "FAILURE"
    return (status, cpuTimeDelta, wallTimeDelta, output)

def run_cpachecker(options, sourcefile, columns, rlimits):
    exe = findExecutable("cpachecker", "scripts/cpa.sh")
    args = [exe] + options + [sourcefile]
    (returncode, output, cpuTimeDelta, wallTimeDelta) = run(args, rlimits)

    if resource.RLIMIT_CPU in rlimits:
        limit = rlimits.get(resource.RLIMIT_CPU)[0]
    else:
        limit = float('inf')

    if returncode == 137 and (cpuTimeDelta+0.5) > limit:
        # if return code is "KILLED BY SIGNAL 9" and
        # used CPU time is larger than the time limit (approximately at least)
        status = 'TIMEOUT'
    else:
        status = getCPAcheckerStatus(returncode, output)

    getCPAcheckerColumns(output, columns)

    return (status, cpuTimeDelta, wallTimeDelta, output)


def getCPAcheckerStatus(returncode, output):
    """
    @param returncode: code returned by CPAchecker 
    @param output: the output of CPAchecker
    @return: status of CPAchecker after running a testfile
    """

    if returncode == 0:
        status = None
    elif returncode == 134:
        status = "ABORTED (probably by Mathsat)"
    elif returncode == 137:
        status = "KILLED BY SIGNAL 9"
    elif returncode == 143:
        status = "KILLED"
    else:
        status = "ERROR ({0})".format(returncode)
    for line in output.splitlines():
        if (line.find('java.lang.OutOfMemoryError') != -1) or line.startswith('out of memory'):
            status = 'OUT OF MEMORY'
        elif (line.find('SIGSEGV') != -1):
            status = 'SEGMENTATION FAULT'
        elif (status is None or status == "ERROR (1)") and (line.find('Exception') != -1):
            status = 'EXCEPTION'
        elif (status is None) and line.startswith('Given specification violated?'):
            line = line[30:].strip()
            if line.startswith('NO'):
                status = 'SAFE'
            elif line.startswith('YES'):
                status = 'UNSAFE'
            else:
                status = 'UNKNOWN'
        elif (line.find('std::bad_alloc') != -1):
            status = 'OUT OF MEMORY'
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
            if (line.find(column.text) != -1):
                startPosition = line.find(':') + 1
                endPosition = line.find('(') # bracket maybe not found -> (-1)
                if (endPosition == -1):
                    column.value = line[startPosition:].strip()
                else:
                    column.value = line[startPosition: endPosition].strip()
                break


def runBenchmark(benchmarkFile):
    benchmark = Benchmark(benchmarkFile)

    assert benchmark.tool in ["cbmc", "satabs", "cpachecker"]
    run_func = eval("run_" + benchmark.tool)

    if len(benchmark.tests) == 1:
        logging.debug("I'm benchmarking {0} consisting of 1 test.".format(repr(benchmarkFile)))
    else:
        logging.debug("I'm benchmarking {0} consisting of {1} tests.".format(
                repr(benchmarkFile), len(benchmark.tests)))

    outputHandler = OutputHandler(benchmark)

    for test in benchmark.tests:

        # get times before test
        ruBefore = resource.getrusage(resource.RUSAGE_CHILDREN)
        wallTimeBefore = time.time()

        outputHandler.outputBeforeTest(test)

        for sourcefile in test.sourcefiles:

            outputHandler.outputBeforeRun(sourcefile)

            # run test
            (status, cpuTimeDelta, wallTimeDelta, output) =\
                run_func(test.options, sourcefile, benchmark.columns, benchmark.rlimits)

            outputHandler.outputAfterRun(sourcefile, status, cpuTimeDelta, 
                                         wallTimeDelta, output)

        # get times after test
        wallTimeAfter = time.time()
        wallTimeTest = wallTimeAfter - wallTimeBefore
        ruAfter = resource.getrusage(resource.RUSAGE_CHILDREN)
        cpuTimeTest = (ruAfter.ru_utime + ruAfter.ru_stime)\
        - (ruBefore.ru_utime + ruBefore.ru_stime)

        outputHandler.outputAfterTest(cpuTimeTest, wallTimeTest)


def main(argv=None):

    if argv is None:
        argv = sys.argv
    from optparse import OptionParser
    parser = OptionParser()
    parser.add_option("-d", "--debug",
                      action="store_true",
                      help="enable debug output")

    parser.add_option("-j", "--json",
                      action="store_true",
                      help="enable json output")

    global options
    (options, args) = parser.parse_args(argv)

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
    except LookupError as e:
        print e
    except KeyboardInterrupt:
        interruptMessage = "script was interrupted by user, some tests may not be done"
        logging.debug(interruptMessage)

        print resultSet.toJson()

        print interruptMessage
        pass

