#!/usr/bin/env python

# import our own modules
import benchmark as BenchmarkScript
FileUtil = BenchmarkScript # only for different names in programm
TableGenerator = __import__('table-generator') # for '-' in module-name

import xml.etree.ElementTree as ET
import sys
import os


OUTPUT_PATH = 'test/regression/'


def cleanup(xmlFile):
    '''
    remove old and unused files
    '''
    filename = os.path.basename(xmlFile)[:-4]
    oldFiles = FileUtil.getFileList(OUTPUT_PATH + filename + '.??-??-??.????.results*.csv') + \
               FileUtil.getFileList(OUTPUT_PATH + filename + '.??-??-??.????.results*.txt') + \
               FileUtil.getFileList(OUTPUT_PATH + 'results.??????-????.table.csv')

    if len(oldFiles) > 0:
        print 'removing some old files:'
        for file in oldFiles:
            print '    ' + file
            os.remove(file)
    else:
        print '\n###   if you run the script for the first time,\n' + \
                '###   maybe some warnings were printed, please ignore them.'


def runBenchmark(xmlFile):
    '''
    run benchmark-script to get results
    '''
    global benchmark # 'global' for KeyboardInterrupt from user
    benchmark = BenchmarkScript
    
    # change some parameters
    benchmark.OUTPUT_PATH = OUTPUT_PATH
    benchmark.USE_ONLY_DATE = False # different filenames for several tests per day

    # run
    benchmark.main(['', xmlFile])


def generateHTML(fileList):
    '''
    create html-table for statistics
    '''
    tableGenerator = TableGenerator

    # change some parameters
    tableGenerator.OUTPUT_PATH = OUTPUT_PATH

    # run
    tableGenerator.main([''] +  fileList)


def compareResults(xmlFile, n=2):
    print '\ncomparing last {0} results ...'.format(n)
    
    filename = os.path.basename(xmlFile)[:-4]
    resultFiles = FileUtil.getFileList(OUTPUT_PATH + filename \
                    + '.??-??-??.????.results*.xml')[-n:] # last n files
    isDifferent = compareCollectedResults(resultFiles)

    if isDifferent:
        diffFiles = FileUtil.getFileList(OUTPUT_PATH + filename \
                    + '.??-??-??.????.diff.results*.xml')[-n:] # last n files
        generateHTML(diffFiles)


def compareCollectedResults(resultFiles):
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
                    sourcefileTags[0].get('name').ljust(maxLen) + \
                    oldStatus + ' --> ' + newStatus
            map(ET.Element.append, diffXMLList, sourcefileTags)

    # store result (differences) in xml-files
    if isDifferent:
        for elem, filename in zip(diffXMLList, resultFiles):            
            file = open(filename.replace('.results.', '.diff.results.'), 'w')
            file.write(FileUtil.XMLtoString(elem))
            file.close()
    else:
        print "\n---> NO DIFFERENCE FOUND IN COLUMN 'STATUS'"

    print ''
    return isDifferent


def allEqualResult(sourcefileTags):
    if len(sourcefileTags) == 1:
        return True
    else:
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


def main(args=None):
    
    if args is None:
        args = sys.argv

    if len(args) != 2:
        print 'exactly one xml-file needed'
        sys.exit()
    xmlFile = args[1]

    print "result will be stored in '" + OUTPUT_PATH + "'"

    cleanup(xmlFile) # remove old files before creating new stuff
    runBenchmark(xmlFile)
    
    lenOfHistory = 2 # how many files should be compared?
    
    compareResults(xmlFile, lenOfHistory)

    print 'regression done'


if __name__ == '__main__':
    try:
        sys.exit(main())
        
    # next block is copied from benchmark-script to avoid some stacktraces
    except KeyboardInterrupt:

        try:
            benchmark.timer.cancel() # Timer, that kills a subprocess after timelimit
        except NameError:
            pass # if no timer is defined, do nothing

        interruptMessage = "\n\nscript was interrupted by user, some tests may not be done"
        print(interruptMessage)
