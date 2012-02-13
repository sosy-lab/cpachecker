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

import xml.etree.ElementTree as ET
import os.path
import glob
import shutil
import optparse
import time
import sys

from datetime import date
from decimal import *
from urllib import quote

OUTPUT_PATH = "test/results/"

NAME_START = "results" # first part of filename of html-table

CSV_SEPARATOR = '\t'


# string searched in filenames to determine correct or incorrect status.
# use lower case!
BUG_SUBSTRING_LIST = ['bad', 'bug', 'unsafe']


# space in front of a line in htmlcode (4 spaces)
HTML_SHIFT = '    '


# scoreValues taken from http://sv-comp.sosy-lab.org/
SCORE_CORRECT_SAFE = 2
SCORE_CORRECT_UNSAFE = 1
SCORE_UNKNOWN = 0
SCORE_WRONG_UNSAFE = -2
SCORE_WRONG_SAFE = -4


class Template():
    """
    a limited template "engine", similar to report-generator
    """

    def __init__(self, infile, outfile):
        self.infile = infile
        self.outfile = outfile

    def render(self, **kws):
        """
        This function replaces every appearance of "{{{key}}}"
        through the value of the key.
        """
        for line in self.infile:
            for key in kws:
                matcher = "{{{" + key + "}}}"
                if matcher in line:
                    line = line.replace(matcher, kws[key])
                    
            self.outfile.write(line)


def getListOfTests(file, filesFromXML=False):
    '''
    This function parses the input to get tests and columns.
    The param 'file' is either a xml-file containing the testfiles
    or a list of result-files (also called 'file', however used as list).

    If the files are from xml-file, param 'filesFromXML' should be true.
    Currently the tests are read also from xml-files.

    If columntitles are given in the xml-file,
    they will be searched in the testfiles.
    If no title is given, all columns of the testfile are taken.

    If result-files are parsed, all columns are taken.

    @return: a list of tuples,
    each tuple contains a testelement and a list of columntitles
    '''
    listOfTests = []

    if filesFromXML:
        tableGenFile = ET.ElementTree().parse(file)
        if 'table' != tableGenFile.tag:
            print ("ERROR:\n" \
                + "    The XML-file seems to be invalid.\n" \
                + "    The rootelement of table-definition-file is not named 'table'.")
            exit()

        for test in tableGenFile.findall('test'):
            columns = test.findall('column')
            filelist = getFileList(test.get('filename')) # expand wildcards
            appendTests(listOfTests, filelist, columns)

    else:
        appendTests(listOfTests, extendFileList(file)) # expand wildcards

    return listOfTests


def appendTests(listOfTests, filelist, columns=None):
    for resultFile in filelist:
        if os.path.exists(resultFile) and os.path.isfile(resultFile):
            print ('    ' + resultFile)

            resultElem = ET.ElementTree().parse(resultFile)

            if 'test' != resultElem.tag:
                print (("ERROR:\n" \
                    + "XML-file seems to be invalid.\n" \
                    + "The rootelement of testresult is not named 'test'.\n" \
                    + "If you want to run a table-definition-file,\n"\
                    + "you should use the option '-x' or '--xml'.").replace('\n','\n    '))
                exit()

            resultElem.set("filename", resultFile)

            availableColumnTitles = [column.get("title") for column in
                                resultElem.find('sourcefile').findall('column')]
            if columns: # not None
                    columnTitles = [column.get("title") for column in columns
                                    if column.get('title') in availableColumnTitles]
            else:
                columnTitles = availableColumnTitles

            insertLogFileNames(resultFile, resultElem)

            listOfTests.append((resultElem, columnTitles))
        else:
            print ('File {0} is not found.'.format(repr(resultFile)))
            exit()


def containEqualFiles(resultElem1, resultElem2):
    list1 = resultElem1.findall('sourcefile')
    list2 = resultElem2.findall('sourcefile')
    if len(list1) != len(list2):
        return False
    for (sf1, sf2) in zip(list1, list2):
        if sf1.get('name') != sf2.get('name'):
            return False
    return True


def insertLogFileNames(resultFile, resultElem):
    filename = os.path.basename(resultElem.get("filename"))
    parts = filename.split("#", 1)

    # get folder of logfiles
    logFolder = resultElem.get('benchmarkname') + "." + resultElem.get('date') + ".logfiles/"
    if len(parts) > 1:
        logFolder = "%s#%s" % (parts[0], logFolder)

    # append begin of filename
    testname = resultElem.get('name')
    if testname is not None:
        logFolder += testname + "."

    # for each file: append original filename and insert logFileName into sourcefileElement
    for sourcefile in resultElem.findall('sourcefile'):
        logFileName = os.path.basename(sourcefile.get('name')) + ".log"
        sourcefile.set('logfileForHtml', logFolder + logFileName)


def mergeFilelists(listOfTests):
    """
    This function returns a list of testelements (+ column), 
    so that each test has the same length and 
    the sourcefiles have the same order in all tests. 
    Invalid testsElements are removed.
    """
    mergedListOfTests = []
    for testResult, columns in listOfTests:
        # TODO handle missing files, similar to regression.py?
        # check for equal files in the tests
        if containEqualFiles(listOfTests[0][0], testResult):
            mergedListOfTests.append((testResult, columns))
        else:
            print ('    {0} contains different files, skipping resultfile'.
                        format(testResult.get("filename")))
            continue
    return mergedListOfTests


def getFileList(shortFile):
    """
    The function getFileList expands a short filename to a sorted list
    of filenames. The short filename can contain variables and wildcards.
    """

    # expand tilde and variables
    expandedFile = os.path.expandvars(os.path.expanduser(shortFile))

    # expand wildcards
    fileList = glob.glob(expandedFile)

    # sort alphabetical,
    # if list is emtpy, sorting returns None, so better do not sort
    if len(fileList) != 0:
        fileList.sort()
    else:
        print ('\nWarning: no file matches "{0}".'.format(shortFile))

    return fileList


def extendFileList(filelist):
    '''
    This function takes a list of files, expands wildcards
    and returns a new list of files.
    '''
    return [file for wildcardFile in filelist for file in getFileList(wildcardFile)]


def getTableHead(listOfTests):
    '''
    get tablehead (tools, limits, testnames, systeminfo, columntitles for html,
    testnames and columntitles for csv)
    '''

    (columnRow, testWidths, titleLine) = getColumnsRowAndTestWidths(listOfTests)
    (toolRow, toolLine) = getToolRow(listOfTests, testWidths)
    limitRow = getLimitRow(listOfTests, testWidths)
    systemRow = getSystemRow(listOfTests, testWidths)
    dateRow = getDateRow(listOfTests, testWidths)
    (testRow, testLine) = getTestRow(listOfTests, testWidths)
    testBranches = getBranchRow(listOfTests, testWidths)
    testOptions = getOptionsRow(listOfTests, testWidths)

    return (('\n' + HTML_SHIFT).join([HTML_SHIFT + '<thead>', toolRow,
            limitRow, systemRow, dateRow, testRow, testBranches, testOptions,
            columnRow]) + '\n</thead>',
            toolLine + '\n' + testLine + '\n' + titleLine + '\n')


def getColumnsRowAndTestWidths(listOfTests):
    '''
    get columnsRow and testWidths, for all columns that should be shown
    '''

    # get common folder of sourcefiles
    fileList = listOfTests[0][0].findall('sourcefile')
    fileNames = [file.get("name") for file in fileList]
    commonPrefix = os.path.commonprefix(fileNames) # maybe with parts of filename
    commonPrefix = commonPrefix[: commonPrefix.rfind('/') + 1] # only foldername

    columnsTitles = [commonPrefix]
    testWidths = []
    for testResult, columns in listOfTests:
        numberOfColumns = 0
        for columnTitle in columns:
            for column in testResult.find('columns').findall('column'):

                if columnTitle == column.get('title'):
                    numberOfColumns += 1
                    columnsTitles.append(columnTitle)
                    break

        testWidths.append(numberOfColumns)


    return ('<tr id="columnTitles"><td>' + \
                '</td><td>'.join(columnsTitles) + \
                '</td></tr>',
            testWidths,
            CSV_SEPARATOR.join(columnsTitles))


def getToolRow(listOfTests, testWidths):
    '''
    get toolRow, each cell of it spans over all tests of this tool
    '''

    toolRow = '<tr><td>Tool</td>'
    toolLine = ['tool']
    tool = (listOfTests[0][0].get('tool'), listOfTests[0][0].get('version'))
    toolWidth = 0

    for (testResult, _), numberOfColumns in zip(listOfTests, testWidths):
        newTool = (testResult.get('tool'), testResult.get('version'))
        if newTool != tool:
            toolRow += '<td colspan="{0}">{1} {2}</td>'.format(toolWidth, *tool)
            toolWidth = 0
            tool = newTool
        toolWidth += numberOfColumns
        for i in range(toolWidth):
            toolLine.append(newTool[0] + ' ' + newTool[1])
    toolRow += '<td colspan="{0}">{1} {2}</td></tr>'.format(toolWidth, *tool)

    return (toolRow, CSV_SEPARATOR.join(toolLine))


def getLimitRow(listOfTests, testWidths):
    '''
    get limitRow, each cell of it spans over all tests with this limit
    '''

    limitRow = '<tr><td>Limits</td>'
    limitWidth = 0
    limit = (listOfTests[0][0].get('timelimit'), listOfTests[0][0].get('memlimit'))

    for (testResult, _), numberOfColumns in zip(listOfTests, testWidths):
        newLimit = (testResult.get('timelimit'), testResult.get('memlimit'))
        if newLimit != limit:
            limitRow += '<td colspan="{0}">timelimit: {1}, memlimit: {2}</td>'\
                            .format(limitWidth, *limit)
            limitWidth = 0
            limit = newLimit
        limitWidth += numberOfColumns
    limitRow += '<td colspan="{0}">timelimit: {1}, memlimit: {2}</td></tr>'\
                    .format(limitWidth, *limit)

    return limitRow


def getSystemRow(listOfTests, testWidths):
    '''
    get systemRow, each cell of it spans over all tests with this system
    '''

    def getSystem(systemTag):
        cpuTag = systemTag.find('cpu')
        system = (systemTag.find('os').get('name'),
                  cpuTag.get('model'),
                  cpuTag.get('cores'),
                  cpuTag.get('frequency'),
                  systemTag.find('ram').get('size'),
                  systemTag.get('hostname', 'unknown'))
        return system

    systemFormatString = '<td colspan="{0}">host: {6}<br>os: {1}<br>'\
                       + 'cpu: {2}<br>cores: {3}, frequency: {4}, ram: {5}</td>'
    systemLine = '<tr><td>System</td>'
    systemWidth = 0
    systemTag = listOfTests[0][0].find('systeminfo')
    system = getSystem(systemTag)

    for (testResult, columns), numberOfColumns in zip(listOfTests, testWidths):
        systemTag = testResult.find('systeminfo')
        newSystem = getSystem(systemTag)
        if newSystem != system:
            systemLine += systemFormatString.format(systemWidth, *system)
            systemWidth = 0
            system = newSystem
        systemWidth += numberOfColumns
    systemLine += systemFormatString.format(systemWidth, *system) + '</tr>'

    return systemLine


def getDateRow(listOfTests, testWidths):
    '''
    get dateRow, each cell of it spans over all tests with this date
    '''

    dateRow = '<tr><td>Date of run</td>'
    dateWidth = 0
    date = listOfTests[0][0].get('date')

    for (testResult, _), numberOfColumns in zip(listOfTests, testWidths):
        newDate = testResult.get('date')
        if newDate != date:
            dateRow += '<td colspan="{0}">{1}</td>'.format(dateWidth, date)
            dateWidth = 0
            date = newDate
        dateWidth += numberOfColumns
    dateRow += '<td colspan="{0}">{1}</td></tr>'.format(dateWidth, date)

    return dateRow


def getTestRow(listOfTests, testWidths):
    '''
    create testRow, each cell spans over all columns of this test
    '''

    testNames = [testResult.get('name', testResult.get('benchmarkname'))
                                for (testResult, _) in listOfTests]
    tests = ['<td colspan="{0}">{1}</td>'.format(width, testName)
             for (testName, width) in zip(testNames, testWidths) if width]
    testLine = CSV_SEPARATOR.join(['test'] + [CSV_SEPARATOR.join([testName]*width)
             for (testName, width) in zip(testNames, testWidths) if width])

    return ('<tr><td>Test set</td>' + ''.join(tests) + '</tr>',
            testLine)


def getBranchRow(listOfTests, testWidths):
    '''
    create branchRow, each cell spans over the columns of a test
    '''
    testBranches = [os.path.basename(testResult.get('filename', '?')) for (testResult, _) in listOfTests]
    if not any("#" in branch for branch in testBranches):
        return ""
    testBranches = [testBranch.split("#", 1)[0] for testBranch in testBranches]
    branches = ['<td colspan="{0}">{1}</td>'.format(width, testBranch)
             for (testBranch, width) in zip(testBranches, testWidths) if width]
    return '<tr id="branch"><td>branch</td>' + ''.join(branches) + '</tr>'


def getOptionsRow(listOfTests, testWidths):
    '''
    create optionsRow, each cell spans over the columns of a test
    '''

    testOptions = [testResult.get('options', ' ') for (testResult, _) in listOfTests]
    options = ['<td colspan="{0}">{1}</td>'.format(width, testOption.replace(' -','<br>-'))
             for (testOption, width) in zip(testOptions, testWidths) if width]
    return '<tr id="options"><td>Options</td>' + ''.join(options) + '</tr>'


def getTableBody(listOfTests):
    '''
    This function build the body and the foot of the table.
    It collects all values from the tests for the columns in the table.
    The foot contains some statistics.
    '''

    listsOfFiles = [test.findall('sourcefile') for test, columns in listOfTests]
    listsOfColumns = [columns for test, columns in listOfTests]

    # get filenames
    fileNames = [file.get("name") for file in listsOfFiles[0]]

    rowsForHTML = [[] for _ in fileNames]
    rowsForCSV  = [[] for _ in fileNames]

    # get values for each test
    for files, columns in zip(listsOfFiles, listsOfColumns):
        valuesListHTML = []
        valuesListCSV = []

        # get values for each file in a test
        for fileResult in files:
            (valuesHTML, valuesCSV) = getValuesOfFileXTest(fileResult, columns)
            valuesListHTML.append(valuesHTML)
            valuesListCSV.append(valuesCSV)

        # append values to html and csv
        for row, values in zip(rowsForHTML, valuesListHTML): row.append(values)
        for row, values in zip(rowsForCSV, valuesListCSV): row.append(values)

    maxLen = max((len(file.get('name')) for file in listsOfFiles[0]))

    # get differences
    rowsForHTMLdiff = []
    rowsForCSVdiff = []
    fileNamesDiff = []
    listsOfFilesDiff = [[] for tests in listsOfFiles]
    isDifference = False
    for elem in zip(rowsForHTML, rowsForCSV, *listsOfFiles):
        HTMLrow, CSVrow, listOfFiles = elem[0], elem[1], elem [2:]

        (allEqual, oldStatus, newStatus) = allEqualResult(listOfFiles)
        if not allEqual:
            isDifference = True
            filename = listOfFiles[0].get('name')
            rowsForHTMLdiff.append(HTMLrow)
            rowsForCSVdiff.append(CSVrow)
            fileNamesDiff.append(filename)
            for list, file in zip(listsOfFilesDiff, listOfFiles): list.append(file)
            print ('    difference found:  {0} : {1} --> {2}'.format(
                        filename.ljust(maxLen), oldStatus, newStatus))

    if len(listOfTests) > 1 and not isDifference:
        print ("\n---> NO DIFFERENCE FOUND IN COLUMN 'STATUS'")

    rowsForStats = getStatsHTML(listsOfFiles, fileNames, listsOfColumns, rowsForCSV)
    if isDifference:
        rowsForStatsDiff = getStatsHTML(listsOfFilesDiff, fileNamesDiff, listsOfColumns, rowsForCSVdiff)

    # get common folder
    commonPrefix = os.path.commonprefix(fileNames) # maybe with parts of filename
    commonPrefix = commonPrefix[: commonPrefix.rfind('/') + 1] # only foldername

    # generate text for filenames, insert it as first column
    # this implicitly adds filenames into diff-lists, too
    for fileName, HTMLrow, CSVrow in zip(fileNames, rowsForHTML, rowsForCSV):
        filePath = getPathOfSourceFile(fileName)
        HTMLrow.insert(0, ['<td><a href="{0}">{1}</a></td>'.
                    format(quote(filePath), fileName.replace(commonPrefix, '', 1))])
        CSVrow.insert(0, [fileName.replace(commonPrefix, '', 1)])

    # join all listelements to strings
    rowsHTML = '</tr>\n{0}<tr>'.format(HTML_SHIFT).join(joinRows(rowsForHTML))
    statsHTML = '</tr>\n{0}<tr>'.format(HTML_SHIFT).join(map(''.join, rowsForStats))
    HTMLbody = '<tbody>\n{0}<tr>{1}</tr>\n</tbody>'.format(HTML_SHIFT, rowsHTML)
    HTMLfooter = '<tfoot>\n{0}<tr>{1}</tr>\n</tfoot>'.format(HTML_SHIFT, statsHTML)
    CSVbody = '\n'.join(joinRows(rowsForCSV, CSV_SEPARATOR))

    if isDifference:
        rowsHTMLdiff = '</tr>\n{0}<tr>'.format(HTML_SHIFT).join(joinRows(rowsForHTMLdiff))
        statsHTMLdiff = '</tr>\n{0}<tr>'.format(HTML_SHIFT).join(map(''.join, rowsForStatsDiff))
        HTMLdiff = '<tbody>\n{0}<tr>{1}</tr>\n</tbody>'.format(HTML_SHIFT, rowsHTMLdiff)
        HTMLdiffFooter = '<tfoot>\n{0}<tr>{1}</tr>\n</tfoot>'.format(HTML_SHIFT, statsHTMLdiff)
        CSVdiff = '\n'.join(joinRows(rowsForCSVdiff, CSV_SEPARATOR))
    else:
        HTMLdiff = ''
        HTMLdiffFooter = ''
        CSVdiff = ''

    return (HTMLbody, HTMLfooter, HTMLdiff, HTMLdiffFooter, CSVbody, CSVdiff)


def joinRows(rows, str=""):
    """
    This function joins all values in each test in each row.
    It returns an iterator over the rows.
    """
    return (str.join(value for test in row for value in test) for row in rows)


def allEqualResult(listOfFiles):
    for file in listOfFiles:
        if listOfFiles[0].status != file.status:
            return (False, listOfFiles[0].status, file.status)
    return (True, None, None)


def getPathOfSourceFile(filename):
    '''
    This method expand a filename of a sourcefile to a path to the sourcefile.
    An absolute filename will not be changed,
    a filename, that is relative to CPAchackerDir, will get a prefix.
    '''
    if not filename.startswith('/'): # not absolute -> relative, TODO: windows?
        filename = os.path.relpath(filename, OUTPUT_PATH)
    return filename


def getValuesOfFileXTest(currentFile, listOfColumns):
    '''
    This function collects the values from all tests for one file.
    Only columns, that should be part of the table, are collected.
    '''

    currentFile.status = 'unknown'

    valuesForHTML = []
    valuesForCSV = []
    for columnTitle in listOfColumns: # for all columns that should be shown
        for column in currentFile.findall('column'):
            if columnTitle == column.get('title'):

                value = column.get('value')

                valuesForCSV.append(value)

                if columnTitle == 'status':
                    # different colors for correct and incorrect results
                    status = value.lower()
                    fileName = currentFile.get('name').lower()
                    isSafeFile = not containsAny(fileName, BUG_SUBSTRING_LIST)

                    if status == 'safe':
                        if isSafeFile:
                            currentFile.status = 'correctSafe'
                        else:
                            currentFile.status = 'wrongSafe'
                    elif status == 'unsafe':
                        if isSafeFile:
                            currentFile.status = 'wrongUnsafe'
                        else:
                            currentFile.status = 'correctUnsafe'
                    elif status == 'unknown':
                        currentFile.status = 'unknown'
                    else:
                        currentFile.status = 'error'

                    valuesForHTML.append('<td class="{0}"><a href="{1}">{2}</a></td>'
                            .format(currentFile.status, quote(str(currentFile.get('logfileForHtml'))), status))

                else:
                    valuesForHTML.append('<td>{0}</td>'.format(value))
                break

    return (valuesForHTML, valuesForCSV)


def containsAny(text, list):
    '''
    This function returns True, iff any string in list is a substring of text.
    '''
    for elem in list:
        if text.find(elem) != -1:
            return True
    return False


def toDecimal(s):
    s = s.strip()
    if s.endswith('s'): # '1.23s'
        s = s[:-1].strip() # remove last char
    elif s == '-':
        s = 0
    return Decimal(s)


def getStatsHTML(listsOfFiles, fileNames, listsOfColumns, valuesList):
    maxScore = sum([SCORE_CORRECT_UNSAFE
                    if containsAny(name.lower(), BUG_SUBSTRING_LIST)
                    else SCORE_CORRECT_SAFE
                        for name in fileNames])
    rowsForStats = [['<td>total files</td>'],
                    ['<td title="(no bug exists + result is SAFE) OR ' + \
                     '(bug exists + result is UNSAFE)">correct results</td>'],
                    ['<td title="bug exists + result is SAFE">false negatives</td>'],
                    ['<td title="no bug exists + result is UNSAFE">false positives</td>'],
                    ['<td>score ({0} files, max score: {1})</td>'
                        .format(len(fileNames), maxScore)]]

    # get statistics
    for elem in zip(listsOfFiles, listsOfColumns, *valuesList):
        files, columns, values = elem[0], elem[1], elem[2:]
        stats = getStatsOfTest(files, columns, values)
        for row, values in zip(rowsForStats, stats): row.extend(values)

    return rowsForStats


def getStatsOfTest(fileResult, columns, valuesList):
    """
    This function return HTML for the table-footer.
    """

    # list for status of bug vs tool
    statusList = [file.status for file in fileResult]
    assert len(valuesList) == len(statusList)

    # convert:
    # [['SAFE', 0,1], ['UNSAFE', 0,2]] -->  [['SAFE', 'UNSAFE'], [0,1, 0,2]]
    # in python2 this is a list, in python3 this is the iterator of the list 
    # this works, because we iterate over the list some lines below
    listsOfValues = zip(*valuesList)

    # collect some statistics
    sumRow = []
    sumCorrectRow = []
    wrongSafeRow = []
    wrongUnsafeRow = []
    scoreRow = []

    for columnTitle, column in zip(columns, listsOfValues):

        # count different elems in statusList
        if columnTitle == 'status':
            correctSafeNr = statusList.count('correctSafe')
            correctUnsafeNr = statusList.count('correctUnsafe')
            wrongSafeNr = statusList.count('wrongSafe')
            wrongUnsafeNr = statusList.count('wrongUnsafe')

            sumRow.append('<td>{0}</td>'.format(len(statusList)))
            sumCorrectRow.append('<td>{0}</td>'.format(correctSafeNr + correctUnsafeNr))
            wrongSafeRow.append('<td>{0}</td>'.format(wrongSafeNr))
            wrongUnsafeRow.append('<td>{0}</td>'.format(wrongUnsafeNr))
            scoreRow.append('<td class="score">{0}</td>'.format(
                                SCORE_CORRECT_SAFE * correctSafeNr + \
                                SCORE_CORRECT_UNSAFE * correctUnsafeNr + \
                                SCORE_WRONG_SAFE * wrongSafeNr + \
                                SCORE_WRONG_UNSAFE * wrongUnsafeNr))

        # get sums for correct, wrong, etc
        else:
            (sum, correctSum, wrongSafeNumber, wrongUnsafeNumber) \
                = getStatsOfNumber(column, statusList)
            sumRow.append('<td>{0}</td>'.format(sum))
            sumCorrectRow.append('<td>{0}</td>'.format(correctSum))
            wrongSafeRow.append('<td>{0}</td>'.format(wrongSafeNumber))
            wrongUnsafeRow.append('<td>{0}</td>'.format(wrongUnsafeNumber))
            scoreRow.append('<td></td>')

    # convert numbers to strings for output
    return (sumRow, sumCorrectRow, wrongSafeRow, wrongUnsafeRow, scoreRow)


def getStatsOfNumber(column, statusList):
    assert len(column) == len(statusList)
    try:
        valueList = [toDecimal(v) for v in column]
    except InvalidOperation:
        print ("Warning: NumberParseException. Statistics may be wrong.")
        return (0, 0, 0, 0)

    correctSum = sum([value
                      for value, status in zip(valueList, statusList)
                      if (status == 'correctSafe' or status == 'correctUnsafe')])
    wrongSafeNumber = sum([value
                      for value, status in zip(valueList, statusList)
                      if (status == 'wrongSafe')])
    wrongUnsafeNumber = sum([value
                      for value, status in zip(valueList, statusList)
                      if (status == 'wrongUnsafe')])

    return (sum(valueList), correctSum, wrongSafeNumber, wrongUnsafeNumber)


def createTable(file, filesFromXML=False):
    '''
    parse inputfile(s), create html-code and write it to file
    '''

    print ('collecting files ...')

    if filesFromXML:
        listOfTests = getListOfTests(file, True)
        prefix = OUTPUT_PATH + os.path.basename(file)[:-3]
    else:
        listOfTests = getListOfTests(file)
        timestamp = time.strftime("%y%m%d-%H%M", time.localtime())
        prefix = OUTPUT_PATH + NAME_START + "." + timestamp

    HTMLOutFileName = prefix + ".table.html"
    HTMLdiffOutFileName = prefix + ".diff.html"
    CSVOutFileName = prefix + ".table.csv"

    if len(listOfTests) == 0:
        print ('\nError! No file with testresults found.\n' \
            + 'Please check the filenames in your XML-file.')
        exit()


    # merge list of tests, so that all tests contain the same filenames
    print ('merging files ...')
    listOfTests = mergeFilelists(listOfTests)

    print ('generating table ...')
    (tableHeadHTML, tableHeadCSV) = getTableHead(listOfTests)
    (tableBodyHTML, tableFootHTML, tableBodyDiffHTML, tableFootDiffHTML, tableBodyCSV, CSVdiff) \
            = getTableBody(listOfTests)

    tableCode = tableHeadHTML.replace('\n','\n' + HTML_SHIFT) \
                + '\n' + HTML_SHIFT \
                + tableFootHTML.replace('\n','\n' + HTML_SHIFT) \
                + '\n' + HTML_SHIFT \
                + tableBodyHTML.replace('\n','\n' + HTML_SHIFT)

    if tableBodyDiffHTML != '':
        tableDiffCode = tableHeadHTML.replace('\n','\n' + HTML_SHIFT) \
                + '\n' + HTML_SHIFT \
                + tableFootDiffHTML.replace('\n','\n' + HTML_SHIFT) \
                + '\n' + HTML_SHIFT \
                + tableBodyDiffHTML.replace('\n','\n' + HTML_SHIFT)

    if not os.path.isdir(OUTPUT_PATH): os.makedirs(OUTPUT_PATH)

    # write HTML to file
    templateFileName = os.path.join(os.path.dirname(__file__),
                               'table-generator-template.html')

    print ('writing html into %s ...' % (HTMLOutFileName, ))

    templateFile = open(templateFileName, 'r')
    HTMLOutFile = open(HTMLOutFileName, 'w')
    Template(templateFile, HTMLOutFile).render(
                title="table of tests",
                table=tableCode
                )
    templateFile.close()
    HTMLOutFile.close()

    if tableBodyDiffHTML != '':
        templateFile = open(templateFileName, 'r')
        HTMLdiffOutFile = open(HTMLdiffOutFileName, 'w')
        Template(templateFile, HTMLdiffOutFile).render(
                title="differences",
                table=tableDiffCode
                )
        templateFile.close()
        HTMLdiffOutFile.close()

    # write CSV to file
    CSVCode = tableHeadCSV + tableBodyCSV
    CSVFile = open(CSVOutFileName, "w")
    CSVFile.write(CSVCode)
    CSVFile.close()

    print ('done')


def main(args=None):

    if args is None:
        args = sys.argv

    parser = optparse.OptionParser('%prog [options] sourcefile\n\n' + \
        "INFO: documented example-files can be found in 'doc/examples'\n")

    parser.add_option("-x", "--xml",
        action="store",
        type="string",
        dest="xmltablefile",
        help="use xmlfile for table. the xml-file should define resultfiles and columns."
    )
    parser.add_option("-o", "--outputpath",
        action="store",
        type="string",
        dest="outputPath",
        help="outputPath for table. if it does not exist, it is created."
    )

    options, args = parser.parse_args(args)

    if options.outputPath:
        global OUTPUT_PATH
        OUTPUT_PATH = options.outputPath if options.outputPath.endswith('/') \
                 else options.outputPath + '/'

    if options.xmltablefile:
        print ("reading table definition from '" + options.xmltablefile + "'...")
        if not os.path.exists(options.xmltablefile) \
                or not os.path.isfile(options.xmltablefile):
            print ('File {0} does not exist.'.format(repr(options.xmltablefile)))
            exit()
        else:
            createTable(options.xmltablefile, True)

    elif len(args) > 1:
        createTable(args[1:])

    else: # default case
        print ("searching resultfiles in '" + OUTPUT_PATH + "' ...")
        createTable([OUTPUT_PATH + '*.results*.xml'])


if __name__ == '__main__':
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print ('script was interrupted by user')
        pass
