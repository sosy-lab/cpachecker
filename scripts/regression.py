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

# import our own modules
TableGenerator = __import__('table-generator') # for '-' in module-name
Util = TableGenerator.Util # only for different names in programm

import xml.etree.ElementTree as ET
import sys
import os
import optparse


OUTPUT_PATH = 'test/results/'


def generateHTML(fileList):
    '''
    create html-table for statistics
    '''
    tableGenerator = TableGenerator

    # change some parameters
    tableGenerator.OUTPUT_PATH = OUTPUT_PATH
    tableGenerator.NAME_START = 'diff'

    # run
    tableGenerator.main([''] +  fileList)


def getDiffXMLList(listOfTestTags):
    '''
    this function copies the header of each tests into the diff-files
    '''
    diffXMLList = []
    emptyElemList = getEmptyElements(listOfTestTags)
    for elem in listOfTestTags:
        newElem = ET.Element('test', elem.attrib)
        extendXMLElem(newElem, elem.findall('systeminfo'))
        extendXMLElem(newElem, elem.findall('columns'))
        extendXMLElem(newElem, elem.findall('time'))
        diffXMLList.append(newElem)
    return diffXMLList


def getEmptyElements(listOfTestTags):
    '''
    this function creates empty elements (dummies) for sourcefiles,
    that do not appear in all xmlfiles
    '''
    emptyElemList = []
    for elem in listOfTestTags:
        emptyElem = ET.Element('sourcefile')
        for column in elem.findall('sourcefile')[0].findall('column'):
            colElem = ET.Element('column')
            colElem.set('title', column.get('title'))
            colElem.set('value', '-')
            emptyElem.append(colElem)
        emptyElemList.append(emptyElem)
    return emptyElemList


def getSourcefileDics(listOfSourcefileTags):
    '''
    this function return a list of dictionaries
    with key=filename and value=resultElem.
    '''
    listOfSourcefileDics = []
    for sourcefileTags in listOfSourcefileTags:
        dic = {}
        for sourcefileTag in sourcefileTags:
            filename = sourcefileTag.get('name')

            if filename in dic: # file tested twice in benchmark, should not happen
                print ("file '{0}' is used twice. skipping file.".format(filename))
            else:
                dic[filename] = sourcefileTag

        listOfSourcefileDics.append(dic)
    return listOfSourcefileDics


def getAllFilenames(listOfSourcefileDics):
    '''
    this function returns all filenames for a 'complete' diff,
    in alphabetical order.
    '''
    # casting to set and back to list removes doubles and mixes the filenames
    allFilenames = list(set(filename
                            for sfDic in listOfSourcefileDics
                            for filename in sfDic))
    allFilenames.sort()
    return allFilenames


def getFilenameList(listOfSourcefileTags, listOfSourcefileDics, compare):
    '''
    this function return a list of sourcefiles.
    the value 'compare' chooses the list to return:
        'a'    -->  all sourcefiles in alphabetical order
        number -->  sourcefiles from one resultfile
    '''
    if compare is None:
        compare = '1' # default value, first resultfile

    if compare.lower() == 'a':
        return getAllFilenames(listOfSourcefileDics)
    else:
        numberOfList = int(compare) - 1 # lists start with position 0
        if numberOfList < 0 or numberOfList >= len(listOfSourcefileTags):
            print ('ERROR: number for list is invalid, give a number in range 1 to n')
            sys.exit()
        return [file.get('name') for file in listOfSourcefileTags[numberOfList]]


def status(currentFile):
    fileName = currentFile.get('name').lower()
    isSafeFile = not Util.containsAny(fileName, TableGenerator.BUG_SUBSTRING_LIST)
    status = [x for x in currentFile.findall("column") if x.get("title") == "status"][0].get("value").lower()

    if status not in ('safe', 'unsafe'):
        return 'unknown'
    return "correct" if (status == 'safe') == isSafeFile else "wrong"

def compareResults(xmlFiles, options):
    compare = options.compare
    print ('\ncomparing results ...')

    resultFiles = Util.extendFileList(xmlFiles)

    if len(resultFiles) == 0:
        print ('Resultfile not found. Check your filenames!')
        sys.exit()

    listOfTestTags = []
    for resultFile in resultFiles:
        testTag = ET.ElementTree().parse(resultFile)
        testTag.set("filename", os.path.basename(resultFile))
        listOfTestTags.append(testTag)

    # copy some info from original data
    # collect all filenames for 'complete' diff
    diffXMLList = getDiffXMLList(listOfTestTags)
    emptyElemList = getEmptyElements(listOfTestTags)
    listOfSourcefileTags = [elem.findall('sourcefile') for elem in listOfTestTags]
    statusList = [[status(file) for file in filetag] for filetag in listOfSourcefileTags]
    maxLen = max((len(file.get('name')) for file in listOfSourcefileTags[0]))
    listOfSourcefileDics = getSourcefileDics(listOfSourcefileTags)

    # get list of filenames for table
    filenames = getFilenameList(listOfSourcefileTags, listOfSourcefileDics, compare)

    # iterate all results parallel
    isDifferent = False
    for filename in filenames:
        sourcefileTags = []
        for dic, emptyElem in zip(listOfSourcefileDics, emptyElemList):
            if filename in dic:
                sourcefileTag = dic[filename]
            else:
                sourcefileTag = copyXMLElem(emptyElem) # make copy, because it is changed
                sourcefileTag.set('name', filename)
            sourcefileTags.append(sourcefileTag)

        (allEqual, oldStatus, newStatus) = allEqualResult(sourcefileTags)
        if not allEqual:
            isDifferent = True
            print ('    difference found:  ' + \
                    sourcefileTags[0].get('name').ljust(maxLen+2) + \
                    oldStatus + ' --> ' + newStatus)
            for elem, tag in zip(diffXMLList, sourcefileTags):
                elem.append(tag)

    # store result (differences) in xml-files
    if isDifferent:
        diffFiles = []
        for filename in resultFiles:
            dir = os.path.join(os.path.dirname(filename), 'diff/')
            if not os.path.isdir(dir):
                os.mkdir(dir)
            diffFiles.append(dir + os.path.basename(filename))
        for elem, diffFilename in zip(diffXMLList, diffFiles):
            file = open(diffFilename, 'w')
            file.write(XMLtoString(elem).replace('  \n', ''))
            file.close()
        generateHTML(diffFiles)
    else:
        print ("\n---> NO DIFFERENCE FOUND IN COLUMN 'STATUS'")

    if options.dump_counts:
        print ("STATS")
        for elem in statusList:
            correct, wrong, unknown = elem.count("correct"), elem.count("wrong"), elem.count("unknown")
            print (str(correct) + " " + str(wrong) + " " + str(unknown))


def copyXMLElem(elem):
    '''
    this function is needed for python < 2.7
    '''
    copy = ET.Element(elem.tag)
    extendXMLElem(copy, elem.getchildren())
    for key, value in elem.attrib:
        copy.set(key, value)
    return copy


def extendXMLElem(elem, list):
    '''
    this function is needed for python < 2.7
    '''
    for child in list:
        elem.append(child)


def allEqualResult(sourcefileTags):
    if len(sourcefileTags) > 1:
        name = sourcefileTags[0].get('name')
        status = getStatus(sourcefileTags[0])

        for sourcefileTag in sourcefileTags:
            if name != sourcefileTag.get('name'):
                print ('wrong filename in xml')
                sys.exit()
            currentStatus = getStatus(sourcefileTag)
            if status != currentStatus:
                return (False, status, currentStatus)
    return (True, None, None)


def getStatus(sourcefileTag):
    for column in sourcefileTag.findall('column'):
        if column.get('title') == 'status':
            return column.get('value')
    return None


def XMLtoString(elem):
        """
        Return a pretty-printed XML string for the Element.
        """
        from xml.dom import minidom
        rough_string = ET.tostring(elem, 'utf-8')
        reparsed = minidom.parseString(rough_string)
        return reparsed.toprettyxml(indent="  ")


def main(args=None):

    if args is None:
        args = sys.argv

    parser = optparse.OptionParser('%prog [options] result_1.xml ... result_n.xml')
    parser.add_option("-c", "--compare",
        action="store", type="string", dest="compare",
        help="Which sourcefiles should be compared? " + \
             "Use 'a' for 'all' or a number for the position."
    )
    parser.add_option("-d", "--dump",
        action="store_true", dest="dump_counts",
        help="Should the good, bad, unknown counts be printed? "
    )
    options, args = parser.parse_args(args)

    if len(args) < 2:
        print ('xml-file needed')
        sys.exit()

    compareResults(args[1:], options)


if __name__ == '__main__':
    sys.exit(main())
