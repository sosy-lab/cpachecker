#!/usr/bin/env python

# import our own modules
TableGenerator = __import__('table-generator') # for '-' in module-name
FileUtil = TableGenerator # only for different names in programm

import xml.etree.ElementTree as ET
import sys
import os


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


def compareResults(xmlFiles):
    print '\ncomparing results ...'
    
    resultFiles = FileUtil.extendFileList(xmlFiles)
    
    if len(resultFiles) == 0:
        print 'Resultfile not found. Check your filenames!'
        sys.exit()
    
    listOfTestTags = [ET.ElementTree().parse(resultFile)
                       for resultFile in resultFiles]

    # copy some info from original data
    diffXMLList = []
    for elem in listOfTestTags:
        newElem = ET.Element("test", elem.attrib)
        newElem.extend(elem.findall('systeminfo'))
        newElem.extend(elem.findall('columns'))
        newElem.extend(elem.findall('time'))
        diffXMLList.append(newElem)

    isDifferent = False
    listOfSourcefileTags = [elem.findall('sourcefile') for elem in listOfTestTags]
    maxLen = max((len(file.get('name')) for file in listOfSourcefileTags[0]))
    
    # iterate all results parallel
    for sourcefileTags in zip(*listOfSourcefileTags):
        (allEqual, oldStatus, newStatus) = allEqualResult(sourcefileTags)
        if not allEqual:
            isDifferent = True
            print '    difference found:  ' + \
                    sourcefileTags[0].get('name').ljust(maxLen+2) + \
                    oldStatus + ' --> ' + newStatus
            map(ET.Element.append, diffXMLList, sourcefileTags)

    # store result (differences) in xml-files
    if isDifferent:
        diffFiles = []
        for filename in resultFiles:
            dir = os.path.dirname(filename) + '/diff/'
            if not os.path.isdir(dir):
                os.mkdir(dir)
            diffFiles.append(dir + os.path.basename(filename))
        for elem, diffFilename in zip(diffXMLList, diffFiles):
            file = open(diffFilename, 'w')
            file.write(XMLtoString(elem))
            file.close()
        generateHTML(diffFiles)
    else:
        print "\n---> NO DIFFERENCE FOUND IN COLUMN 'STATUS'"


def allEqualResult(sourcefileTags):
    if len(sourcefileTags) > 1:
        name = sourcefileTags[0].get('name')
        status = getStatus(sourcefileTags[0])
        
        for sourcefileTag in sourcefileTags:
            if name != sourcefileTag.get('name'):
                print 'wrong filename in xml'
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

    if len(args) < 2:
        print 'xml-file needed'
        sys.exit()
   
    compareResults(args[1:])


if __name__ == '__main__':
    sys.exit(main())
