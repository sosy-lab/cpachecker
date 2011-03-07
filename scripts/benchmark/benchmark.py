#!/usr/bin/env python

from string import Template
from xml.etree.ElementTree import ElementTree
from datetime import date

import glob
import logging
import os.path
import resource
import subprocess
import sys
import xml.etree.ElementTree as ET

OUTPUT_PATH = "./test/results/"

CSV_SEPARATOR = "\t"

# the number of digits after the decimal separator of the time column,
# for the other columns it can be configured in the xml-file
TIME_PRECISION = 2

class Benchmark:
    pass

class Test:
    pass

class Column:
    pass

class OutputHandler:
    """
    The class OutputHandler manages all outputs to the terminal and to files.
    """

    def __init__(self, benchmark):
        """
        The constructor of Outputhandler initialises some variables
        (logFolder, outputLogFileName, outputCSVFileName)
        and prints the heads into log- and CSV-file
        """

        self.benchmark = benchmark

        # create folder for file-specific log-files.
        # if the folder exists, it will be used.
        # if there are files in the folder (with the same name than the testfiles), 
        # they will be OVERWRITTEN without a message!
        self.logFolder = OUTPUT_PATH + self.benchmark.name + ".logfiles." + str(date.today()) + "/"
        if not os.path.isdir(self.logFolder):
            os.mkdir(self.logFolder)

        # create head of outputLogFile
        headLine = "benchmark: " + self.benchmark.name + "\n"
        dateLine = "date:      " + str(date.today()) + "\n"
        toolLine = "tool:      " + self.benchmark.tool + "\n"
        if (9 in self.benchmark.rlimits): # 9 is key of memlimit, convert value to MB
            toolLine += "memlimit:  " + str(self.benchmark.rlimits[9][0] / 1024 / 1024) + "\n"
        if (0 in self.benchmark.rlimits): # 0 is key of timelimit
            toolLine += "timelimit: " + str(self.benchmark.rlimits[0][0]) + "\n"
        simpleLine = "-" * (len(headLine) + 5) + "\n"

        # create outputLogFile
        # if the file exist, it will be OVERWRITTEN without a message!
        outputLogFileName = OUTPUT_PATH + self.benchmark.name + ".results." + str(date.today()) + ".txt"
        self.outputLog = FileWriter(outputLogFileName, headLine + dateLine + toolLine + simpleLine)
        logging.debug("OutputLogFile {0} created.".format(repr(outputLogFileName)))

        # create outputCSVFile with titleLine
        # if the file exist, it will be OVERWRITTEN without a message!
        outputCSVFileName = OUTPUT_PATH + self.benchmark.name + ".results." + str(date.today()) + ".csv"
        CSVtitleLine = CSV_SEPARATOR.join(["sourcefile", "status", "time"])
        for column in self.benchmark.columns:
            CSVtitleLine += CSV_SEPARATOR + column.title
        self.outputCSV = FileWriter(outputCSVFileName, CSVtitleLine + "\n")
        logging.debug("OutputCSVFile {0} created.".format(repr(outputCSVFileName)))


    def outputBeforeTest(self, test):
        """
        The method outputBeforeTest() prints the head of a test into the log-file
        @param test: current test with a list of testfiles
        """

        self.test = test
        numberOfTest = self.benchmark.tests.index(self.test) + 1

        if len(self.test.sourcefiles) == 1:
            logging.debug("The {0} test consists of 1 sourcefile.".format(
                    ordinalNumeral(numberOfTest)))
        else:
            logging.debug("The {0} test consists of {1} sourcefiles.".format(
                    ordinalNumeral(numberOfTest),
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
                                     "time", self.benchmark.columns, True)
        self.simpleLine = "-" * (len(titleLine)) + "\n"
        self.outputLog.append(optionLine + titleLine + "\n" + self.simpleLine)


    def outputBeforeRun(self, sourcefile):
        """
        The method outputBeforeRun() prints the name of a file to terminal
        @param sourcefile: the name of a sourcefile
        """

        options = " ".join(self.test.options)
        logging.debug("I'm running '{0} {1} {2}'.".format(
            self.benchmark.tool, options, sourcefile))

        # output in terminal
        sys.stdout.write(sourcefile.ljust(self.maxLengthOfFileName + 4))
        sys.stdout.flush()


    def outputAfterRun(self, sourcefile, status, timedelta, columnValues, stdoutdata, stderrdata):
        """
        The method outputAfterRun() prints (filename,) result, time and status 
        of a test to terminal and into the log and CSV-file. 
        stderrdata and stdoutdata are written to a file-specific log-file.
        """

        # format time, type is changed from float to string!
        timedelta = formatNumber(timedelta, TIME_PRECISION)

        # format numbers, numberOfDigits is optional, so it can be None
        for column in self.benchmark.columns:
            if column.numberOfDigits is not None:

                # if the number ends with "s" or another letter, remove it
                if (not column.value.isdigit()) and column.value[-2:-1].isdigit():
                    column.value = column.value[:-1]

                try:
                    floatValue = float(column.value)
                    column.value = formatNumber(floatValue, column.numberOfDigits)
                except ValueError: # if value is no float, don't format it
                    pass

        # write stderrdata and stdoutdata to file-specific log-file
        logFileName = self.logFolder
        if self.test.name is not None:
            logFileName += self.test.name + "."
        logFileName += os.path.basename(sourcefile) + ".log"
        FileWriter(logFileName, stderrdata + stdoutdata)

        # output in terminal/console
        print status.ljust(8) + timedelta.rjust(8)

        # output in log-file
        self.outputLog.append(self.createOutputLine(sourcefile, status, 
                            timedelta, self.benchmark.columns, False) + "\n")

        # output in CSV-file
        outputCSVLine = CSV_SEPARATOR.join([sourcefile, status, timedelta])
        for column in self.benchmark.columns:
            outputCSVLine += CSV_SEPARATOR + column.value
        self.outputCSV.append(outputCSVLine + "\n")


    def outputAfterTest(self, testTime):
        """
        The method outputAfterTest() prints number of files and the time of a 
        test into the log-file. 
        @param testTime: whole time, that the test needed
        """

        # format time, type is changed from float to string!
        testTime = formatNumber(testTime, TIME_PRECISION)
        
        numberOfTest = self.benchmark.tests.index(self.test) + 1

        if len(self.test.sourcefiles) == 1:
            endline = ("The {0} test consisted of 1 sourcefile.".format(
                    ordinalNumeral(numberOfTest)))
        else:
            endline = ("The {0} test consisted of {1} sourcefiles.".format(
                    ordinalNumeral(numberOfTest), len(self.test.sourcefiles)))
        endline = self.createOutputLine(endline, "done", testTime, [], False) + "\n"
        self.outputLog.append(self.simpleLine + endline)


    def createOutputLine(self, sourcefile, status, time, columns, isFirstLine):
        """
        @param sourcefile: title of a sourcefile
        @param status: status of programm 
        @param time: total time from running the programm
        @param columns: list of columns with a title or a value
        @param isFirstLine: boolean for different output of headline and other lines
        @return: a line for the outputFile
        """

        lengthOfStatus = 8
        lengthOfTime = 10
        minLengthOfColumns = 8

        outputLine = sourcefile.ljust(self.maxLengthOfFileName + 4) +\
                     status.ljust(lengthOfStatus) +\
                     time.rjust(lengthOfTime)

        for column in columns:
            columnLength = max(minLengthOfColumns, len(column.title)) + 2

            if isFirstLine: 
                value = column.title
            else:
                value = column.value

            outputLine = outputLine + str(value).rjust(columnLength)

        return outputLine


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


def run(args, rlimits):
    args = map(lambda arg: os.path.expandvars(arg), args)
    args = map(lambda arg: os.path.expanduser(arg), args)
    def setrlimits():
        for rsrc, limits in rlimits.items():
            resource.setrlimit(rsrc, limits)
    ru_before = resource.getrusage(resource.RUSAGE_CHILDREN)
    try:
        p = subprocess.Popen(args,
                             stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                             preexec_fn=setrlimits)
    except OSError:
        logging.critical("I caught an OSError. Assure that the directory "
                         + "containing the tool to be benchmarked is included "
                         + "in the PATH environment variable or an alias is set.")
        sys.exit("A critical exception caused me to exit non-gracefully. Bye.")
    (stdoutdata, stderrdata) = p.communicate()
    ru_after = resource.getrusage(resource.RUSAGE_CHILDREN)
    timedelta = (ru_after.ru_utime + ru_after.ru_stime)\
        - (ru_before.ru_utime + ru_before.ru_stime)
    returncode = p.returncode
    logging.debug("My subprocess returned returncode {0}.".format(returncode))
    return (returncode, stdoutdata, stderrdata, timedelta)


def run_cbmc(options, sourcefile, columns, rlimits):
    if ("--xml-ui" not in options):
        options = options + ["--xml-ui"]
    args = ["cbmc"] + options + [sourcefile]
    (returncode, stdoutdata, stderrdata, timedelta) = run(args, rlimits)
    
    #an empty tag cannot be parsed into a tree
    stdoutdata = stdoutdata.replace("<>", "<emptyTag>")
    stdoutdata = stdoutdata.replace("</>", "</emptyTag>")
    
    tree = ET.fromstring(stdoutdata)
    status = tree.findtext('cprover-status')
    if (status == "FAILURE"):
        status = tree.find('goto_trace').find('failure').findtext('reason')
        if ('unwinding assertion' in status):
            status = "UNKNOWN"
        else:
            status = "UNSAFE"
    elif (status == "SUCCESS"):
        if ("--no-unwinding-assertions" in options):
            status = "UNKNOWN"
        else:
            status = "SAFE"
    return (status, timedelta, [], stdoutdata, stderrdata)


def run_satabs(options, sourcefile, columns, rlimits):
    args = ["satabs"] + options + [sourcefile]
    (returncode, stdoutdata, stderrdata, timedelta) = run(args, rlimits)
    if "VERIFICATION SUCCESSFUL" in stdoutdata:
        status = "SUCCESS"
    else:
        status = "FAILURE"
    return (status, timedelta, [], stdoutdata, stderrdata)


def run_cpachecker(options, sourcefile, columns, rlimits):
    args = ["cpachecker"] + options + [sourcefile]
    (returncode, stdoutdata, stderrdata, timedelta) = run(args, rlimits)
    
    status = getCPAcheckerStatus(returncode, stdoutdata + stderrdata)
    columnValueList = getCPAcheckerColumns(stdoutdata, columns)
    
    return (status, timedelta, columnValueList, stdoutdata, stderrdata)


def getCPAcheckerStatus(returncode, stdoutdata):
    """
    @param returncode: code returned by CPAchecker 
    @param stoutdata: the output of CPAchecker
    @return: status of CPAchecker after running a testfile
    """

    if returncode == 0:
        status = None
    elif returncode == 134:
        status = "ABORTED (probably by Mathsat)"
    elif returncode == 137:
        status = "KILLED BY SIGNAL 9 (probably ulimit)"
    elif returncode == 143:
        status = "KILLED"
    else:
        status = "ERROR ({0})".format(returncode)
    for line in stdoutdata.splitlines():
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


def getCPAcheckerColumns(stdoutdata, columns):
    """
    @param stoutdata: the output of CPAchecker
    @param columns: a list with columns
    """

    for column in columns:

        # search for the text in stdoutdata and get its value,
        # stop after the first line, that contains the searched text
        column.value = "-" # default value
        for line in stdoutdata.splitlines():
            if (line.find(column.text) != -1):
                startPosition = line.find(':') + 1
                endPosition = line.find('(') # bracket maybe not found -> (-1)
                if (endPosition == -1):
                    column.value = line[startPosition:].strip()
                else:
                    column.value = line[startPosition: endPosition].strip()
                break


def ordinalNumeral(number):
    last_cipher = number % 10
    if last_cipher == 1:
        return "{0}st".format(number)
    elif last_cipher == 2:
        return "{0}nd".format(number)
    elif last_cipher == 3:
        return "{0}rd".format(number)
    else:
        return "{0}th".format(number)


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


def loadBenchmark(benchmarkPath):
    ## looks like trouble with pyxml, better use lxml (http://codespeak.net/lxml/).
    # try:
    #     from xml.parsers.xmlproc  import xmlval
    #     validator = xmlval.XMLValidator()
    #     validator.parse_resource(benchmarkPath)
    # except ImportError:
    #     logging.debug("I cannot import xmlval so I'm skipping the validation.")
    #     logging.debug("If you want xml validation please install pyxml.")
    logging.debug("I'm loading the benchmark {0}.".format(benchmarkPath))
    tree = ElementTree()
    root = tree.parse(benchmarkPath)
    benchmark = Benchmark()

    # get benchmark-name
    benchmark.name = os.path.basename(benchmarkPath)[:-4] # remove ending ".xml"

    # get tool
    benchmark.tool = (root.get("tool"))
    logging.debug("The tool to be benchmarked is {0}.".format(repr(benchmark.tool)))

    benchmark.rlimits = {}
    if ("memlimit" in root.keys()):
        limit = int(root.get("memlimit")) * 1024 * 1024
        benchmark.rlimits[resource.RLIMIT_AS] = (limit, limit)
    if ("timelimit" in root.keys()):
        limit = int(root.get("timelimit"))
        benchmark.rlimits[resource.RLIMIT_CPU] = (limit, limit)

    # get benchmarks
    benchmark.tests = []
    for testTag in root.findall("test"):
        benchmark.tests.append(loadTest(testTag))

    # get columns
    benchmark.columns = loadColumns(root.find("columns"))

    testnum = len(benchmark.tests)
    return benchmark


def loadTest(testTag):
    """
    @param testTag: a testTag from the xml-file
    @return: the Test() from a testTag in the xml-file
    """

    test = Test()

    # get name of test, name is optional, the result can be "None"
    test.name = testTag.get("name")

    # get all sourcefiles
    test.sourcefiles = []
    sourcefiles_tags = testTag.findall("sourcefiles")
    for sourcefiles in sourcefiles_tags:
        sourcefiles_path = os.path.expandvars(os.path.expanduser(sourcefiles.text))
        if sourcefiles_path != sourcefiles.text:
            logging.debug("I expanded a tilde and/or shell variables in expression {0} to {1}."
                .format(repr(sourcefiles.text), repr(sourcefiles_path))) 
        pathnames = glob.glob(sourcefiles_path)
        if len(pathnames) == 0:
            logging.debug("I found no pathnames matching {0}."
                          .format(repr(sourcefiles_path)))
        else:
            pathnames.sort() # alphabetical order of files
            test.sourcefiles += pathnames

    # get all options
    test.options = []
    for option in testTag.find("options").findall("option"):
        test.options.append(option.get("name"))
        if option.text is not None:
            test.options.append(option.text)

    return test


def loadColumns(columnsTag):
    """
    @param columnsTag: the columnsTag from the xml-file
    @return: a list of Columns()
    """

    logging.debug("I'm loading some columns for the outputfile.")
    columns = []
    if columnsTag != None: # columnsTag is optional in xml-file
        for columnTag in columnsTag.findall("column"):
            column = Column()

            # get text
            column.text = columnTag.text

            # get title (title is optional, default: get text)
            column.title = columnTag.get("title", column.text)
            
            # get number of digits behind comma
            column.numberOfDigits = columnTag.get("digitsBehindComma")

            columns.append(column)
            logging.debug('Column "{0}" with title "{1}" loaded from xml-file.'
                      .format(column.text, column.title))
    return columns


def runBenchmark(benchmarkFile):
    benchmark = loadBenchmark(benchmarkFile)

    assert benchmark.tool in ["cbmc", "satabs", "cpachecker"]
    run_func = eval("run_" + benchmark.tool)

    if len(benchmark.tests) == 1:
        logging.debug("I'm benchmarking {0} consisting of 1 test.".format(repr(benchmarkFile)))
    else:
        logging.debug("I'm benchmarking {0} consisting of {1} tests.".format(
                repr(benchmarkFile), len(benchmark.tests)))

    outputHandler = OutputHandler(benchmark)

    for test in benchmark.tests:

        # get resource usage (time) before test
        ruBefore = resource.getrusage(resource.RUSAGE_CHILDREN)

        outputHandler.outputBeforeTest(test)

        for sourcefile in test.sourcefiles:

            outputHandler.outputBeforeRun(sourcefile)

            # run test
            (status, timedelta, columnValues, stdoutdata, stderrdata) =\
                run_func(test.options, sourcefile, benchmark.columns, benchmark.rlimits)

            outputHandler.outputAfterRun(sourcefile, status, timedelta,
                                         columnValues, stdoutdata, stderrdata)

        # get resource usage (time) after test
        ruAfter = resource.getrusage(resource.RUSAGE_CHILDREN)
        testTime = (ruAfter.ru_utime + ruAfter.ru_stime)\
        - (ruBefore.ru_utime + ruBefore.ru_stime)

        outputHandler.outputAfterTest(testTime)


def main(argv=None):
    if argv is None:
        argv = sys.argv
    from optparse import OptionParser
    parser = OptionParser()
    parser.add_option("-d", "--debug",
                      action="store_true",
                      help="enable debug output")
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

if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        interruptMessage = "script was interrupted by user, some tests may not be done"
        logging.debug(interruptMessage)
        print interruptMessage
        pass
