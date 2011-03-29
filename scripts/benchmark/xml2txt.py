#!/usr/bin/env python

import xml.etree.ElementTree as ET
import os.path

from datetime import date


def convert(filename, outputFolder):
    """
    This function convert a XML-file to a TXT-file.
    Not all data is converted.
    """

    print "converting xml to txt ...",
    benchmarkTag = ET.ElementTree().parse(filename)

    # get header and systemInfo
    columnWidth = 20
    simpleLine1 = "-" * (60) + "\n\n"

    header = "   BENCHMARK INFORMATION\n"\
                + "benchmark:".ljust(columnWidth) + benchmarkTag.get("name") + "\n"\
                + "date:".ljust(columnWidth) + benchmarkTag.get("date") + "\n"\
                + "tool:".ljust(columnWidth) + benchmarkTag.get("tool")\
                + " " + benchmarkTag.get("version") + "\n"

    memlimit = benchmarkTag.get("memlimit")
    if (memlimit != None):
        header += "memlimit:".ljust(columnWidth) + memlimit + "\n"
    timelimit = benchmarkTag.get("timelimit")
    if (timelimit != None):
        header += "timelimit:".ljust(columnWidth) + timelimit + "\n"
    header += simpleLine1

    systemInfoTag = benchmarkTag.find("systeminfo")
    systemInfo = "   SYSTEM INFORMATION\n"\
            + "os:".ljust(columnWidth) + systemInfoTag.find("os").get("name") + "\n"\
            + "cpu:".ljust(columnWidth) + systemInfoTag.find("cpu").get("model") + "\n"\
            + "- cores:".ljust(columnWidth) + systemInfoTag.find("cpu").get("cores") + "\n"\
            + "- max frequency:".ljust(columnWidth) + systemInfoTag.find("cpu").get("frequency") + "\n"\
            + "ram:".ljust(columnWidth) + systemInfoTag.find("ram").get("size") + "\n"\
            + simpleLine1

    TXTcontent = header + systemInfo

    for testTag in benchmarkTag.findall("test"):

        # get length of first column in table
        maxLengthOfFileName = 40
        for sourcefileTag in testTag.findall("sourcefile"):
            maxLengthOfFileName = max(len(sourcefileTag.get("name")), maxLengthOfFileName)

        # get info and titles of test
        numberOfTest = benchmarkTag.findall("test").index(testTag) + 1
        testName = testTag.get("name")
        if testName is None:
            testInfo = ""
        else:
            testInfo = testTag.get("name") + "\n" 
        testInfo += "test {0} of {1} with options: {2}\n\n".format(
                                    numberOfTest, 
                                    len(benchmarkTag.findall("test")),
                                    testTag.get("options"))

        titleLine = createLine("sourcefile", "status", "cpu time", "wall time",
                               benchmarkTag.find("columns"), maxLengthOfFileName, True)

        simpleLine2 = "-" * (len(titleLine)) + "\n"

        TXTcontent += "\n" + testInfo + titleLine + simpleLine2

        for sourcefileTag in testTag.findall("sourcefile"):
            TXTcontent += createLine(sourcefileTag.get("name"),
                                    sourcefileTag.get("status"),
                                    sourcefileTag.find("time").get("cpuTime"),
                                    sourcefileTag.find("time").get("wallTime"),
                                    sourcefileTag.findall("column"),
                                    maxLengthOfFileName, False)

        # get endline of test
        numberOfFiles = len(testTag.findall("sourcefile"))
        if numberOfFiles == 1:
            endline = ("test {0} consisted of 1 sourcefile.".format(numberOfTest))
        else:
            endline = ("test {0} consisted of {1} sourcefiles.".format(
                    numberOfTest, numberOfFiles))

        endline = createLine(endline, "done",
                             testTag.find("time").get("cpuTime"),
                             testTag.find("time").get("wallTime"),
                             [], maxLengthOfFileName, False) + "\n"
        TXTcontent +=  simpleLine2 + endline

    # write to file    
    TXTFileName = outputFolder + benchmarkTag.get("name") + ".results." + str(date.today()) + ".txt"
    TXTFile = open(TXTFileName, "w")
    TXTFile.write(TXTcontent)
    TXTFile.close()
    print "done"


def createLine(sourcefile, status, cpuTimeDelta, wallTimeDelta, columns, maxLengthOfFileName, isFirstLine):
    """
    This function creates one line of the table in the TXT-file

    @param sourcefile: title of a sourcefile
    @param status: status of programm 
    @param cpuTimeDelta: time from running the programm
    @param wallTimeDelta: time from running the programm
    @param columns: list of columns with a title or a value
    @param maxLengthOfFileName: length of first column
    @param isFirstLine: boolean for different output of headline and other lines
    @return: a line for the outputFile
    """

    lengthOfStatus = 8
    lengthOfTime = 11
    minLengthOfColumns = 8

    outputLine = sourcefile.ljust(maxLengthOfFileName + 4) + \
                 status.ljust(lengthOfStatus) + \
                 cpuTimeDelta.rjust(lengthOfTime) + \
                 wallTimeDelta.rjust(lengthOfTime)

    for column in columns:
        columnLength = max(minLengthOfColumns, len(column.get("title"))) + 2

        if isFirstLine: 
            value = column.get("title")
        else:
            value = column.get("value")

        outputLine += str(value).rjust(columnLength)

    return outputLine + "\n"


def main(argv=None):

    if argv is None:
        argv = sys.argv

    from optparse import OptionParser
    parser = OptionParser()
    parser.add_option("-i", "--input",
                      dest="filename",
                      help="file to convert")

    parser.add_option("-o", "--outputFolder",
                      dest="outputFolder",
                      help="folder for output")

    (options, args) = parser.parse_args(argv)


    # check parameters
    if options.filename == None or options.outputFolder == None:
        print "error: invalid number of arguments."
        parser.print_help()
        exit()
    if not os.path.exists(options.filename) or not os.path.isfile(options.filename):
            print "File {0} does not exist.".format(repr(options.filename))
            parser.print_help()
            exit()
    if not os.path.exists(options.outputFolder) or not os.path.isdir(options.outputFolder):
            print "Folder {0} does not exist.".format(repr(options.outputFolder))
            parser.print_help()
            exit()

    # convert
    convert(options.filename, options.outputFolder)


if __name__ == "__main__":
    try:
        import sys
        sys.exit(main())
    except LookupError as e:
        print e
    except KeyboardInterrupt:
        print "script was interrupted by user"
        pass
