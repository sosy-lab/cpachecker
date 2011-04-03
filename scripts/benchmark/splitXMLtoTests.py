#!/usr/bin/env python

import xml.etree.ElementTree as ET
import os.path


def split(filename, outputFolder):
    """
    This function split a XML-file to more XML-file.
    Each contains one test. Not all data is converted.
    """

    print "splitting xml ...",
    benchmarkTag = ET.ElementTree().parse(filename)

    for testTag in benchmarkTag.findall("test"):

        # copy information from benchmark to test
        date = benchmarkTag.get("date")
        testTag.set("date", date)
        testTag.set("benchmarkname", benchmarkTag.get("name"))
        testTag.set("tool", benchmarkTag.get("tool"))
        testTag.set("version", benchmarkTag.get("version"))
        testTag.set("memlimit", benchmarkTag.get("memlimit"))
        testTag.set("timelimit", benchmarkTag.get("timelimit"))
        testTag.append(benchmarkTag.find("systeminfo"))
        testTag.append(benchmarkTag.find("columns"))

        testName = testTag.get("name")
        if testName is not None:
            XMLTestFileName = outputFolder + benchmarkTag.get("name") \
                            + ".results.test." + testName + "." + date + ".xml"
        else:
            XMLTestFileName = outputFolder + benchmarkTag.get("name") \
                            + ".results.test." + date + ".xml"

        # write test to file
        XMLTestFile = open(XMLTestFileName, "w")
        XMLTestFile.write(XMLtoString(testTag))
        XMLTestFile.close()

    print "done"


def XMLtoString(elem):
    """
    Return a pretty-printed XML string for the Element.
    """
    from xml.dom import minidom
    rough_string = ET.tostring(elem, 'utf-8').replace("\n","").replace(
            ">  <","><").replace(">    <","><").replace(">      <","><")
    reparsed = minidom.parseString(rough_string)
    return reparsed.toprettyxml(indent="  ")


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

    split(options.filename, options.outputFolder)


if __name__ == "__main__":
    try:
        import sys
        sys.exit(main())
    except LookupError as e:
        print e
    except KeyboardInterrupt:
        print "script was interrupted by user"
        pass
