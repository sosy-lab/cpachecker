#!/usr/bin/env python

import xml.etree.ElementTree as ET
import os.path

from datetime import date


CSV_SEPARATOR = "\t"


def convert(filename, outputFolder):
    """
    This function convert a XML-file to a CSV-file.
    Not all data is converted.
    """

    print "converting xml to csv ...",
    benchmarkTag = ET.ElementTree().parse(filename)

    CSVtitleLine = CSV_SEPARATOR.join(["sourcefile", "status", "cpu time", "wall time"])
    for column in benchmarkTag.find("columns"):
        CSVtitleLine += CSV_SEPARATOR + column.get("title")

    CSVContent = CSVtitleLine + "\n"

    for testTag in benchmarkTag.findall("test"):
        for sourcefileTag in testTag.findall("sourcefile"):

            outputCSVLine = CSV_SEPARATOR.join([sourcefileTag.get("name"),
                                                sourcefileTag.get("status"),
                                                sourcefileTag.find("time").get("cpuTime"),
                                                sourcefileTag.find("time").get("wallTime")])
            for column in sourcefileTag.findall("column"):
                outputCSVLine += CSV_SEPARATOR + column.get("value")

            CSVContent += outputCSVLine + "\n"

    # write to file
    CSVFileName = outputFolder + benchmarkTag.get("name") + ".results." + str(date.today()) + ".csv"
    CSVFile = open(CSVFileName, "w")
    CSVFile.write(CSVContent)
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
