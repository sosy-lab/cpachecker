#!/usr/bin/env python

from string import Template
from xml.etree.ElementTree import ElementTree

import glob
import itertools
import logging
import os
import resource
import subprocess
import sys
import xml.etree.ElementTree as ET

OUTPUT_PATH = "./test/output/"

CSV_SEPARATOR = "\t"

class Benchmark:
    pass

class Test:
    pass

class Column:
    pass


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
    timedelta = round((ru_after.ru_utime + ru_after.ru_stime)\
        - (ru_before.ru_utime + ru_before.ru_stime), 3)
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
    
    status = getCPAcheckerStatus(returncode, stdoutdata)
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

            columns.append(column)
            logging.debug('Column "{0}" with title "{1}" loaded from xml-file.'
                      .format(column.text, column.title))
    return columns


def runBenchmark(benchmarkFile):
    benchmark = loadBenchmark(benchmarkFile)

    assert benchmark.tool in ["cbmc", "satabs", "cpachecker"]
    run_func = eval("run_" + benchmark.tool)

    # create folder for file-specific log-files.
    # if the folder exists, it will be used.
    # if there are files in the folder (with the same name than the testfiles), 
    # they will be OVERWRITTEN without a message!
    from datetime import date
    logFolder = OUTPUT_PATH + benchmark.name + ".logfiles." + str(date.today()) + "/"
    if not os.path.isdir(logFolder):
        os.mkdir(logFolder)

    # create outputLogFile
    # if the file exist, it will be OVERWRITTEN without a message!
    outputLogFileName = OUTPUT_PATH + benchmark.name + ".results." + str(date.today()) + ".txt"
    outputLogFile = open(outputLogFileName, "w")

    # create head of outputLogFile
    headLine = "benchmark: " + benchmark.name + "\n"
    dateLine = "date:      " + str(date.today()) + "\n"
    toolLine = "tool:      " + benchmark.tool    + "\n"
    if (9 in benchmark.rlimits): # 9 is key of memlimit, convert value to MB
        toolLine += "memlimit:  " + str(benchmark.rlimits[9][0] / 1024 / 1024) + "\n"
    if (0 in benchmark.rlimits): # 0 is key of timelimit
        toolLine += "timelimit: " + str(benchmark.rlimits[0][0]) + "\n"
    simpleLine = "-" * (len(headLine) + 5) + "\n"
    
    outputLogFile.write(headLine + dateLine + toolLine + simpleLine)
    outputLogFile.close()
    logging.debug("OutputLogFile {0} created.".format(repr(outputLogFileName)))

    # create outputCSVFile with titleLine
    # if the file exist, it will be OVERWRITTEN without a message!
    outputCSVFileName = OUTPUT_PATH + benchmark.name + ".results." + str(date.today()) + ".csv"
    CSVtitleLine = CSV_SEPARATOR.join(["sourcefile", "status", "time"])
    for column in benchmark.columns:
        CSVtitleLine += CSV_SEPARATOR + column.title
    outputCSVFile = open(outputCSVFileName, "w")
    outputCSVFile.write(CSVtitleLine + "\n")
    outputCSVFile.close()
    logging.debug("OutputCSVFile {0} created.".format(repr(outputCSVFileName)))


    if len(benchmark.tests) == 1:
        logging.debug("I'm benchmarking {0} consisting of 1 test.".format(repr(benchmarkFile)))
    else:
        logging.debug("I'm benchmarking {0} consisting of {1} tests.".format(
                repr(benchmarkFile), len(benchmark.tests)))

    for test in benchmark.tests:
        if len(test.sourcefiles) == 1:
            logging.debug("The {0} test consists of 1 sourcefile.".format(
                    ordinalNumeral(benchmark.tests.index(test) + 1)))
        else:
            logging.debug("The {0} test consists of {1} sourcefiles.".format(
                    ordinalNumeral(benchmark.tests.index(test) + 1),
                    len(test.sourcefiles)))

        # get resource usage (time) before test
        ruBefore = resource.getrusage(resource.RUSAGE_CHILDREN)

        # values for the table with the results of a test
        numberOfBenchmark = benchmark.tests.index(test) + 1
        maxLengthOfFileName = 40
        for sourcefile in test.sourcefiles:
            maxLengthOfFileName = max(len(sourcefile), maxLengthOfFileName)

        # write headline and columntitles for this test to file
        options = " ".join(test.options)
        optionLine = "\n\n"
        if test.name is not None:
            optionLine += test.name + "\n"
        optionLine += "test {0} of {1} with options: {2}\n\n".format(
                    numberOfBenchmark, len(benchmark.tests), options)
        titleLine = createOutputLine("sourcefile", maxLengthOfFileName, 
                                     "status", "time", benchmark.columns, True)
        simpleLine = "-" * (len(titleLine)) + "\n"
        outputLogFile = open(outputLogFileName, "a")
        outputLogFile.write(optionLine + titleLine + "\n" + simpleLine)
        outputLogFile.close()

        for sourcefile in test.sourcefiles:
            logging.debug("I'm running '{0} {1} {2}'.".format(
                    benchmark.tool, options, sourcefile))

            # output in terminal/console
            sys.stdout.write(sourcefile)
            sys.stdout.flush()

            # run test
            (status, timedelta, columnValues, stdoutdata, stderrdata) =\
                run_func(test.options, sourcefile, benchmark.columns, benchmark.rlimits)

            # write stderrdata and stdoutdata to file-specific log-file
            if test.name is None:
                logFileName = logFolder + os.path.basename(sourcefile) + ".log"
            else:
                logFileName = logFolder + test.name + "." + os.path.basename(sourcefile) + ".log"
            logFile = open(logFileName, "w")
            logFile.write(stderrdata + stdoutdata)
            logFile.close()

            # output in terminal/console
            print " ".join([" "*(maxLengthOfFileName - len(sourcefile)),
                            status, " "*(8 - len(status)), str(timedelta)])

            # output in log-file
            outputLogFile = open(outputLogFileName, "a")
            outputLogFile.write(createOutputLine(sourcefile, maxLengthOfFileName, 
                                status, timedelta, benchmark.columns, False) + "\n")
            outputLogFile.close()
            
            # output in CSV-file
            outputCSVFile = open(outputCSVFileName, "a")
            outputCSVLine = CSV_SEPARATOR.join([sourcefile, status, str(timedelta)])
            for column in benchmark.columns:
                outputCSVLine += CSV_SEPARATOR + column.value
            outputCSVFile.write(outputCSVLine + "\n")
            outputCSVFile.close()

        # get resource usage (time) after test
        ruAfter = resource.getrusage(resource.RUSAGE_CHILDREN)
        testTime = round((ruAfter.ru_utime + ruAfter.ru_stime)\
        - (ruBefore.ru_utime + ruBefore.ru_stime), 3)

        # write endline of this test to file
        if len(test.sourcefiles) == 1:
            endline = ("The {0} test consisted of 1 sourcefile.".format(
                    ordinalNumeral(numberOfBenchmark)))
        else:
            endline = ("The {0} test consisted of {1} sourcefiles.".format(
                    ordinalNumeral(numberOfBenchmark), len(test.sourcefiles)))
        endline = createOutputLine(endline, maxLengthOfFileName, 
                                "done", testTime, [], False) + "\n"
        outputLogFile = open(outputLogFileName, "a")
        outputLogFile.write(simpleLine + endline)
        outputLogFile.close()


def createOutputLine(sourcefile, maxLengthOfFileName, status, time, columns, isFirstLine):
    """
    @param sourcefile: title of a sourcefile
    @param maxLengthOfFileName: number for columnlength
    @param status: status of programm 
    @param time: total time from running the programm
    @param columns: list of columns with a title or a value
    @param isFirstLine: boolean for different output of headline and other lines
    @return: a line for the outputFile
    """

    lengthOfStatus = 8
    lengthOfTime = 10
    minLengthOfColumns = 8

    outputLine = "".join([sourcefile,
                 " "*(maxLengthOfFileName - len(sourcefile) + 2),
                 status, " "*(lengthOfStatus - len(status) + 2),
                 str(time), " "*(lengthOfTime - len(str(time)) + 2)])

    for column in columns:
        columnLength = max(minLengthOfColumns, len(column.title)) + 2

        if isFirstLine: 
            value = column.title
        else:
            value = column.value

        outputLine = outputLine + str(value).rjust(columnLength)

    return outputLine


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
