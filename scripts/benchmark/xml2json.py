#!/usr/bin/env python

import xml.etree.ElementTree as ET
import os.path
import json

from collections import defaultdict
from datetime import date


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


def convert(filename, outputFolder):
    """
    This function convert a XML-file to a JSON-file.
    Not all data is converted.
    """

    print "converting xml to json ...",
    benchmarkTag = ET.ElementTree().parse(filename)
    resultSet = ResultSet()

    for testTag in benchmarkTag.findall("test"):
        for sourcefileTag in testTag.findall("sourcefile"):
            resultSet.addResult(sourcefileTag.get("name"), 
                                benchmarkTag.get("tool"), 
                                testTag.get("name"), 
                                {
                                 'status':         sourcefileTag.get("status"),
                                 'cpuTimeDelta':   sourcefileTag.find("time").get("cpuTime"),
                                 'wallTimeDelta':  sourcefileTag.find("time").get("wallTime")
                                 })

    # write to file    
    JSONFileName = outputFolder + benchmarkTag.get("name") + ".results." + str(date.today()) + ".json"
    JSONFile = open(JSONFileName, "w")
    JSONFile.write(resultSet.toJson())
    JSONFile.close()
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
