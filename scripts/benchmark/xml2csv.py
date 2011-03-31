#!/usr/bin/env python

import xml.etree.ElementTree as ET
import os.path


CSV_SEPARATOR = "\t"


def convert(filename, outputFolder):
    """
    This function convert a XML-file to a CSV-file.
    Not all data is converted.
    """

    print "converting xml to csv ...",
    benchmarkTag = ET.ElementTree().parse(filename)
    date = benchmarkTag.get("date")

    # remove old CSV-files
    for testTag in benchmarkTag.findall("test"):
        testName = testTag.get("name")
        if testName is None:
            CSVFileName = outputFolder + benchmarkTag.get("name") \
                            + ".results." + date + ".csv"
        else:
            CSVFileName = outputFolder + benchmarkTag.get("name") \
                            + "." + testName + ".results." + date + ".csv"
        if os.path.isfile(CSVFileName):
            os.remove(CSVFileName)

    # write new files
    CSVtitleLine = CSV_SEPARATOR.join(["sourcefile", "status", "cpu time", "wall time"])
    for column in benchmarkTag.find("columns"):
        CSVtitleLine += CSV_SEPARATOR + column.get("title")

    CSVtitleLine = CSVtitleLine + "\n"
    contentWithoutTestName = ""

    for testTag in benchmarkTag.findall("test"):

        CSVContent = ""
        for sourcefileTag in testTag.findall("sourcefile"):

            outputCSVLine = CSV_SEPARATOR.join([sourcefileTag.get("name"),
                                                sourcefileTag.get("status"),
                                                sourcefileTag.find("time").get("cputime"),
                                                sourcefileTag.find("time").get("walltime")])
            for column in sourcefileTag.findall("column"):
                outputCSVLine += CSV_SEPARATOR + column.get("value")

            CSVContent += outputCSVLine + "\n"

        # write to file if name exists, else store in contentWithoutTestName
        testName = testTag.get("name")
        if testName is None:
            contentWithoutTestName += CSVContent
        else:
            CSVFileName = outputFolder + benchmarkTag.get("name") \
                            + "." + testName + ".results." + date + ".csv"
            if os.path.isfile(CSVFileName):
                CSVFile = open(CSVFileName, "a")
                CSVFile.write(CSVContent)
            else:
                CSVFile = open(CSVFileName, "w")
                CSVFile.write(CSVtitleLine + CSVContent)
            CSVFile.close()

    if contentWithoutTestName is not "":
        CSVFileName = outputFolder + benchmarkTag.get("name") \
                        + ".results." + date + ".csv"
        CSVFile = open(CSVFileName, "w")
        CSVFile.write(CSVtitleLine + contentWithoutTestName)
        CSVFile.close()
    print "done"


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
