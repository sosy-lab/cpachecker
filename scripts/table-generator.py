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
try:
  from urllib import quote
except ImportError: # 'quote' was moved into 'parse' in Python 3
  from urllib.parse import quote


OUTPUT_PATH = "test/results/"

NAME_START = "results" # first part of filename of html-table

CSV_SEPARATOR = '\t'

TEMPLATE_FILE_NAME = os.path.join(os.path.dirname(__file__), 'table-generator-template.html')

# string searched in filenames to determine correct or incorrect status.
# use lower case!
BUG_SUBSTRING_LIST = ['bad', 'bug', 'unsafe']

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

    def __init__(self, infileName, outfileName):
        self.infileName = infileName
        self.outfileName = outfileName

    def render(self, **kws):
        """
        This function replaces every appearance of "{{{key}}}"
        through the value of the key.
        """
        infile = open(self.infileName, 'r')
        outfile = open(self.outfileName, 'w')

        for line in infile:
            for key in kws:
                matcher = "{{{" + key + "}}}"
                if matcher in line:
                    line = line.replace(matcher, kws[key])
            outfile.write(line)

        infile.close()
        outfile.close()


class Util:
    """
    This Class contains some useful functions for Strings, Files and Lists.
    """

    @staticmethod
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


    @staticmethod
    def extendFileList(filelist):
        '''
        This function takes a list of files, expands wildcards
        and returns a new list of files.
        '''
        return [file for wildcardFile in filelist for file in Util.getFileList(wildcardFile)]


    @staticmethod
    def containsAny(text, list):
        """
        This function returns True, iff any string in list is a substring of text.
        """
        for elem in list:
            if elem in text:
                return True
        return False


    @staticmethod
    def formatNumber(value, numberOfDigits):
        """
        If the value is a number (or number plus one char),
        this function returns a string-representation of the number
        with a number of digits after the decimal separator.
        If the number has more digits, it is rounded, else zeros are added.

        If the value is no number, it is returned unchanged.
        """
        lastChar = ""
        # if the number ends with "s" or another letter, remove it
        if (not value.isdigit()) and value[-2:-1].isdigit():
            lastChar = value[-1]
            value = value[:-1]
        try:
            floatValue = float(value)
            value = "{value:.{width}f}".format(width=numberOfDigits, value=floatValue)
        except ValueError: # if value is no float, don't format it
            pass
        return value + lastChar


    @staticmethod
    def toDecimal(s):
        s = s.strip()
        if s.endswith('s'): # '1.23s'
            s = s[:-1].strip() # remove last char
        elif s in ['-', '']:
            s = 0
        return Decimal(s)


    @staticmethod
    def collapseEqualValues(values, counts):
        """
        Take a tuple (values, counts), remove consecutive values and increment their count instead.
        """
        assert len(values) == len(counts)
        previousValue = values[0]
        previousCount = 0

        for value, count in zip(values, counts):
            if value != previousValue:
                yield (previousValue, previousCount)
                previousCount = 0
                previousValue = value
            previousCount += count

        yield (previousValue, previousCount)

    @staticmethod
    def getColumnValue(sourcefileTag, columnTitle, default=None):
        for column in sourcefileTag.findall('column'):
            if column.get('title') == columnTitle:
                    return column.get('value')
        return default

def parseTableDefinitionFile(file):
    '''
    This function parses the input to get tests and columns.
    The param 'file' is a xml-file defining the testfiles and columns.

    If columntitles are given in the xml-file,
    they will be searched in the testfiles.
    If no title is given, all columns of the testfile are taken.

    @return: a list of tuples,
    each tuple contains a test file and a list of columntitles
    '''
    print ("reading table definition from '{0}'...".format(file))
    if not os.path.isfile(file):
        print ('File {0!r} does not exist.'.format(file))
        exit()

    listOfTestFiles = []
    tableGenFile = ET.ElementTree().parse(file)
    if 'table' != tableGenFile.tag:
        print ("ERROR:\n" \
            + "    The XML-file seems to be invalid.\n" \
            + "    The rootelement of table-definition-file is not named 'table'.")
        exit()

    for test in tableGenFile.findall('test'):
        columnsToShow = test.findall('column')
        filelist = Util.getFileList(test.get('filename')) # expand wildcards
        listOfTestFiles += [(file, columnsToShow) for file in filelist]

    return listOfTestFiles



class Column:
    """
    The class Column contains title, text (to identify a line in logFile),
    and numberOfDigits of a column.
    It does NOT contain the value of a column.
    """
    def __init__(self, title, text, numOfDigits):
        self.title = title
        self.text = text
        self.numberOfDigits = numOfDigits


class Result():
    """
    The Class Result is a wrapper for some columns to show and a filelist.
    """
    def __init__(self, resultXML, filename, columns):
        self.filename = filename
        self.filelist = resultXML.findall('sourcefile')
        self.columns = columns

        systemTag = resultXML.find('systeminfo')
        cpuTag = systemTag.find('cpu')
        self.attributes = {
                'timelimit': None,
                'memlimit':  None,
                'options':   ' ',
                'name':      resultXML.get('name', resultXML.get('benchmarkname')),
                'branch':    os.path.basename(filename).split('#')[0] if '#' in filename else '',
                'os':        systemTag.find('os').get('name'),
                'cpu':       cpuTag.get('model'),
                'cores':     cpuTag.get('cores'),
                'freq':      cpuTag.get('frequency'),
                'ram':       systemTag.find('ram').get('size'),
                'host':      systemTag.get('hostname', 'unknown')
                }
        self.attributes.update(resultXML.attrib)

    def getSourceFileNames(self):
        return [file.get('name') for file in self.filelist]

def parseTestFile(resultFile, columnsToShow=None):
    '''
    This function parses the resultfile to a resultElem and collects
    all columntitles from the resultfile, that should be part of the table.
    It returns a Result object.
    '''
    if not os.path.isfile(resultFile):
        print ('File {0!r} is not found.'.format(resultFile))
        exit()

    print ('    ' + resultFile)

    resultElem = ET.ElementTree().parse(resultFile)

    if 'test' != resultElem.tag:
        print (("ERROR:\n" \
            + "XML-file seems to be invalid.\n" \
            + "The rootelement of testresult is not named 'test'.\n" \
            + "If you want to run a table-definition-file,\n"\
            + "you should use the option '-x' or '--xml'.").replace('\n','\n    '))
        exit()

    if columnsToShow: # not None
        columns = [Column(c.get("title"), c.text, c.get("numberOfDigits"))
                   for c in columnsToShow]
    else: # show all available columns
        columns = [Column(c.get("title"), None, None)
                   for c in resultElem.find('sourcefile').findall('column')]

    insertLogFileNames(resultFile, resultElem)
    return Result(resultElem, resultFile, columns)


def insertLogFileNames(resultFile, resultElem):
    resultFile = os.path.basename(resultFile)
    parts = resultFile.split("#", 1)

    # get folder of logfiles
    logFolder = '{benchmarkname}.{date}.logfiles/'.format(**resultElem.attrib)
    if len(parts) > 1:
        logFolder = parts[0] + '#' + logFolder

    # append begin of filename
    testname = resultElem.get('name')
    if testname is not None:
        logFolder += testname + "."

    # for each file: append original filename and insert logFileName into sourcefileElement
    for sourcefile in resultElem.findall('sourcefile'):
        logFileName = os.path.basename(sourcefile.get('name')) + ".log"
        sourcefile.logfile = logFolder + logFileName



def mergeSourceFiles(listOfTests):
    """
    This function merges the filelists of all Result objects.
    If necessary, it can merge lists of names: [A,C] + [A,B] --> [A,B,C]
    and add dummy elements to the filelists.
    It also ensures the same order of files.
    Returns a list of filenames
    """
    nameList = []
    nameSet = set()
    for result in listOfTests:
        index = -1
        currentResultNameSet = set()
        for name in result.getSourceFileNames():
            if name in currentResultNameSet:
                print ("File {0} is present twice in {1}, skipping it.".format(name, result.filename))
            else:
                currentResultNameSet.add(name)
                if name not in nameSet:
                    nameList.insert(index+1, name)
                    nameSet.add(name)
                    index += 1
                else:
                    index = nameList.index(name)

    mergeFilelists(listOfTests, nameList)
    return nameList

def mergeFilelists(listOfTests, filenames):
    """
    Set the filelists of all Result elements so that they contain the same files
    in the same order. For missing files a dummy element is inserted.
    """
    for result in listOfTests:
        # create mapping from name to sourcefile tag
        dic = dict([(file.get('name'), file) for file in result.filelist])
        result.filelist = [] # clear and repopulate filelist
        for filename in filenames:
            fileResult = dic.get(filename)
            if fileResult == None:
                fileResult = ET.Element('sourcefile') # create an empty dummy element
                fileResult.logfile = None
                print ('    no result for {0} in {1}'.format(filename, result.filename))
            result.filelist.append(fileResult)


def ensureEqualSourceFiles(listOfTests):
    # take the files of the first test
    fileNames = listOfTests[0].getSourceFileNames()
    # check for equal files
    def equalFiles(result):
        if fileNames == result.getSourceFileNames(): return True
        else: print ('    {0} contains different files, skipping resultfile'.format(result.filename))

    listOfTests = filter(equalFiles, listOfTests)
    return fileNames, listOfTests



class Test:
    """
    The class Test contains the results of one test for one file.
    """
    def __init__(self, status, category, logFile, columns, values):
        assert(len(columns) == len(values))
        self.status = status
        self.logFile = logFile
        self.columns = columns
        self.values = values
        self.category = category

    @staticmethod
    def createTestFromXML(sourcefileTag, listOfColumns, fileIsUnsafe):
        '''
        This function collects the values from one tests for one file.
        Only columns, that should be part of the table, are collected.
        '''

        def getResultCategory(status):
            status = status.lower()
            if status == 'safe':
                return 'correctSafe' if not fileIsUnsafe else 'wrongSafe'
            elif status == 'unsafe':
                return 'wrongUnsafe' if not fileIsUnsafe else 'correctUnsafe'
            elif status == 'unknown':
                return 'unknown'
            else:
                return 'error'

        def getValueFromLogfile(content, identifier):
            """
            This method searches for values in lines of the content.
            The format of such a line must be:    "identifier:  value  (rest)".

            If a value is not found, the value is set to "-".
            """
            # stop after the first line, that contains the searched text
            value = "-" # default value
            for line in content.splitlines():
                if identifier in line:
                    startPosition = line.find(':') + 1
                    endPosition = line.find('(') # bracket maybe not found -> (-1)
                    if (endPosition == -1):
                        value = line[startPosition:].strip()
                    else:
                        value = line[startPosition: endPosition].strip()
                    break
            return value

        status = Util.getColumnValue(sourcefileTag, 'status', 'unknown')
        category = getResultCategory(status)
        logfileContent = None

        values = []

        for column in listOfColumns: # for all columns that should be shown
            value = "-" # default value
            if column.text == None: # collect values from XML
                value = Util.getColumnValue(sourcefileTag, column.title, '-')

            elif sourcefileTag.logfile != None: # collect values from logfile
                if logfileContent == None: # cache content
                    logfileContent = open(OUTPUT_PATH + sourcefileTag.logfile).read()

                value = getValueFromLogfile(logfileContent, column.text)

            if column.numberOfDigits is not None:
                value = Util.formatNumber(value, column.numberOfDigits)

            values.append(value)

        return Test(status, category, sourcefileTag.logfile, listOfColumns, values)

    def toHTML(self):
        """
        This function returns a String for HTML.
        If the columnTitle is 'status', different colors are used,
        else the value is only wrapped in a table-cell.
        """
        result = []
        for column, value in zip(self.columns, self.values):
            if column.title == 'status' and self.logFile != None:
                # different colors for correct and incorrect results

                result.append('<td class="{0}"><a href="{1}">{2}</a></td>'.format(
                            self.category, quote(self.logFile), value.lower()))

            else:
                result.append('<td>{0}</td>'.format(value))
        return "".join(result)


class Row:
    """
    The class Row contains the results for one file (a list of Tests).
    """
    def __init__(self, fileName):
        self.fileName = fileName
        self.results = []

    def addTest(self, test):
        self.results.append(test)

    def fileIsUnsafe(self):
        return Util.containsAny(self.fileName.lower(), BUG_SUBSTRING_LIST)

    def getPathToSourceFile(self):
        '''
        This method expand a filename of a sourcefile to a path to the sourcefile.
        An absolute filename will not be changed,
        a filename, that is relative to CPAcheckerDir, will get a prefix.
        '''
        if not os.path.isabs(self.fileName): # not absolute -> relative
            return os.path.relpath(self.fileName, OUTPUT_PATH)
        return self.fileName

    def toCSV(self, commonPrefix):
        """
        generate CSV representation of rows with filename as first column
        """
        fileName = self.fileName.replace(commonPrefix, '', 1)
        allValues = [value for test in self.results for value in test.values]
        return CSV_SEPARATOR.join([fileName] + allValues)

    def toHTML(self, commonPrefix):
        """
        generate HTML representation of rows with filename as first column
        """
        filePath = quote(self.getPathToSourceFile())
        fileName = self.fileName.replace(commonPrefix, '', 1)

        HTMLrow = [test.toHTML() for test in self.results]
        return '<tr><td><a href="{0}">{1}</a></td>{2}</tr>'.format(filePath, fileName, "".join(HTMLrow))

def rowsToColumns(rows):
    """
    Convert a list of Rows into a column-wise list of list of Tests
    """
    return zip(*[row.results for row in rows])


def getRows(listOfTests, fileNames):
    """
    Create list of rows with all data. Each row consists of several tests.
    """
    rows = [Row(fileName) for fileName in fileNames]

    # get values for each test
    for result in listOfTests:
        # get values for each file in a test
        for fileResult, row in zip(result.filelist, rows):
            row.addTest(Test.createTestFromXML(fileResult, result.columns, row.fileIsUnsafe()))

    return rows


def filterRowsWithDifferences(rows):
    """
    Find all rows with differences in the status column.
    """
    def allEqualResult(listOfResults):
        for result in listOfResults:
            if listOfResults[0].status != result.status:
                return (False, listOfResults[0].status, result.status)
        return (True, None, None)

    maxLen = max(len(row.fileName) for row in rows)
    rowsDiff = []
    for row in rows:
        (allEqual, oldStatus, newStatus) = allEqualResult(row.results)
        if not allEqual:
            rowsDiff.append(row)
            print ('    difference found:  {0} : {1} --> {2}'.format(
                        row.fileName.ljust(maxLen), oldStatus, newStatus))


    if len(rowsDiff) == 0:
        print ("---> NO DIFFERENCE FOUND IN COLUMN 'STATUS'")
    elif len(rowsDiff) == len(rows):
        print ("---> DIFFERENCES FOUND IN ALL ROWS, NO NEED TO CREATE DIFFERENCE TABLE")
        return []

    return rowsDiff



def getTableHead(listOfTests, commonFileNamePrefix):
    '''
    get tablehead (tools, limits, testnames, systeminfo, columntitles for html,
    testnames and columntitles for csv)
    '''
    def formatLine(str):
        return [str.format(**test.attributes) for test in listOfTests]

    def getHtmlRow(rowName, values, widths, collapse=False, id=None):
        if not any(values): return '' # skip row without values completely
        if not id:
            id = rowName.lower().split(' ')[0]

        valuesAndWidths = Util.collapseEqualValues(values, widths) \
                          if collapse else zip(values, widths)

        cells = ['<td colspan="{0}">{1}</td>'.format(width, value) for value, width in valuesAndWidths if width]
        return '<tr id="{0}"><td>{1}</td>{2}</tr>'.format(id, rowName, "".join(cells))

    def getCsvRow(rowName, values, widths):
        cells = [CSV_SEPARATOR.join([value]*width) for value, width in zip(values, widths) if width]

        return CSV_SEPARATOR.join([rowName.lower()] + cells)


    testWidths = [len(test.columns) for test in listOfTests]

    tools       = formatLine('{tool} {version}')
    toolRow     = getHtmlRow('Tool', tools, testWidths, collapse=True)
    toolLine    = getCsvRow('Tool', tools, testWidths)

    limits      = formatLine('timelimit: {timelimit}, memlimit: {memlimit}')
    limitRow    = getHtmlRow('Limits', limits, testWidths, collapse=True)

    systems     = formatLine('host: {host}<br>os: {os}<br>cpu: {cpu}<br>cores: {cores}, frequency: {freq}, ram: {ram}')
    systemRow   = getHtmlRow('System', systems, testWidths, collapse=True)

    dates       = formatLine('{date}')
    dateRow     = getHtmlRow('Date of run', dates, testWidths, collapse=True)

    tests       = formatLine('{name}')
    testRow     = getHtmlRow('Test', tests, testWidths)
    testLine    = getCsvRow('Test', tests, testWidths)

    branches    = formatLine('{branch}')
    branchesRow = getHtmlRow('Branch', branches, testWidths)

    options     = formatLine('{options}')
    optionsRow  = getHtmlRow('Options', options, testWidths).replace(' -', '<br>-')

    titles      = [column.title for test in listOfTests for column in test.columns]
    testWidths1 = [1]*sum(testWidths)
    titleRow    = getHtmlRow(commonFileNamePrefix, titles, testWidths1, id='columnTitles')
    titleLine   = getCsvRow(commonFileNamePrefix, titles, testWidths1)

    return ('\n'.join([toolRow, limitRow, systemRow, dateRow, testRow, branchesRow, optionsRow, titleRow]),
            '\n'.join([toolLine, testLine, titleLine]))



def getStatsHTML(rows):
    maxScore = sum([SCORE_CORRECT_UNSAFE if row.fileIsUnsafe() else SCORE_CORRECT_SAFE
                        for row in rows])
    rowsForStats = [['<td>total files</td>'],
                    ['<td title="(no bug exists + result is SAFE) OR ' + \
                     '(bug exists + result is UNSAFE)">correct results</td>'],
                    ['<td title="bug exists + result is SAFE">false negatives</td>'],
                    ['<td title="no bug exists + result is UNSAFE">false positives</td>'],
                    ['<td>score ({0} files, max score: {1})</td>'
                        .format(len(rows), maxScore)]]

    # get statistics
    for tests in rowsToColumns(rows):
        stats = getStatsOfTest(tests)
        for row, values in zip(rowsForStats, stats): row.extend(values)

    return ['<tr>{0}</tr>'.format("".join(row)) for row in rowsForStats]


def getStatsOfTest(tests):
    """
    This function return HTML for the table-footer.
    """

    # convert:
    # [['SAFE', 0,1], ['UNSAFE', 0,2]] -->  [['SAFE', 'UNSAFE'], [0,1, 0,2]]
    # in python2 this is a list, in python3 this is the iterator of the list
    # this works, because we iterate over the list some lines below
    listsOfValues = zip(*[test.values for test in tests])

    columns = tests[0].columns
    statusList = [test.category for test in tests]

    # collect some statistics
    sumRow = []
    correctRow = []
    wrongSafeRow = []
    wrongUnsafeRow = []
    scoreRow = []

    for column, values in zip(columns, listsOfValues):
        if column.title == 'status':
            sum, correctSafe, correctUnsafe, wrongSafe, wrongUnsafe = getStatsOfStatusColumn(statusList)

            correct = correctSafe + correctUnsafe
            score   = SCORE_CORRECT_SAFE   * correctSafe + \
                      SCORE_CORRECT_UNSAFE * correctUnsafe + \
                      SCORE_WRONG_SAFE     * wrongSafe + \
                      SCORE_WRONG_UNSAFE   * wrongUnsafe

        else:
            sum, correct, wrongSafe, wrongUnsafe = getStatsOfNumberColumn(values, statusList)
            score = ''

        sumRow.append(sum)
        correctRow.append(correct)
        wrongSafeRow.append(wrongSafe)
        wrongUnsafeRow.append(wrongUnsafe)
        scoreRow.append(score)

    sumRow         = map('<td>{0}</td>'.format, sumRow)
    correctRow     = map('<td>{0}</td>'.format, correctRow)
    wrongSafeRow   = map('<td>{0}</td>'.format, wrongSafeRow)
    wrongUnsafeRow = map('<td>{0}</td>'.format, wrongUnsafeRow)
    scoreRow       = map('<td class="score">{0}</td>'.format, scoreRow)

    return (sumRow, correctRow, wrongSafeRow, wrongUnsafeRow, scoreRow)


def getStatsOfStatusColumn(categoryList):
    # count different elems in statusList
    total         = len(categoryList)
    correctSafe   = categoryList.count('correctSafe')
    correctUnsafe = categoryList.count('correctUnsafe')
    wrongSafe     = categoryList.count('wrongSafe')
    wrongUnsafe   = categoryList.count('wrongUnsafe')

    return (total, correctSafe, correctUnsafe, wrongSafe, wrongUnsafe)


def getStatsOfNumberColumn(values, categoryList):
    assert len(values) == len(categoryList)
    try:
        valueList = [Util.toDecimal(v) for v in values]
    except InvalidOperation:
        print ("Warning: NumberParseException. Statistics may be wrong.")
        return (0, 0, 0, 0)

    correctSum = sum([value
                      for value, status in zip(valueList, categoryList)
                      if (status == 'correctSafe' or status == 'correctUnsafe')])
    wrongSafe = sum([value
                      for value, status in zip(valueList, categoryList)
                      if (status == 'wrongSafe')])
    wrongUnsafe = sum([value
                      for value, status in zip(valueList, categoryList)
                      if (status == 'wrongUnsafe')])

    return (sum(valueList), correctSum, wrongSafe, wrongUnsafe)



def getCounts(rows): # for options.dumpCounts
    countsList = []

    for testResults in rowsToColumns(rows):
        statusList = [test.category for test in testResults]
        sum, correctSafe, correctUnsafe, wrongSafe, wrongUnsafe = getStatsOfStatusColumn(statusList)

        correct = correctSafe + correctUnsafe
        wrong = wrongSafe + wrongUnsafe
        unknown = len(statusList) - correct - wrong

        countsList.append((correct, wrong, unknown))

    return countsList



def createTables(name, listOfTests, fileNames, rows, rowsDiff):
    '''
    create tables and write them to files
    '''

    # get common folder of sourcefiles
    commonPrefix = os.path.commonprefix(fileNames) # maybe with parts of filename
    commonPrefix = commonPrefix[: commonPrefix.rfind('/') + 1] # only foldername

    HTMLhead, CSVhead = getTableHead(listOfTests, commonPrefix)

    def writeTable(outfile, name, rows):
        print ('writing html into {0} ...'.format(outfile + ".html"))

        HTMLbody = '\n'.join([row.toHTML(commonPrefix) for row in rows])
        HTMLfoot = '\n'.join(getStatsHTML(rows))
        CSVbody  = '\n'.join([row.toCSV(commonPrefix) for row in rows])

        # write HTML to file
        Template(TEMPLATE_FILE_NAME, outfile + ".html").render(
                    title=name,
                    head=HTMLhead,
                    body=HTMLbody,
                    foot=HTMLfoot
                    )

        # write CSV to file
        CSVFile = open(outfile + ".csv", "w")
        CSVFile.write(CSVhead)
        CSVFile.write('\n')
        CSVFile.write(CSVbody)
        CSVFile.close()


    # write normal tables
    writeTable(OUTPUT_PATH + name + ".table", name, rows)

    # write difference tables
    if len(rowsDiff) > 1:
        writeTable(OUTPUT_PATH + name + ".diff", name + " differences", rowsDiff)


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
    parser.add_option("-d", "--dump",
        action="store_true", dest="dumpCounts",
        help="Should the good, bad, unknown counts be printed?"
    )
    parser.add_option("-m", "--merge",
        action="store_true", dest="merge",
        help="If resultfiles with distinct sourcefiles are found, " \
            + "should the sourcefilenames be merged?"
    )

    options, args = parser.parse_args(args)
    args = args[1:] # skip args[0] which is the name of this script

    if options.outputPath:
        global OUTPUT_PATH
        OUTPUT_PATH = options.outputPath if options.outputPath.endswith('/') \
                 else options.outputPath + '/'
    if not os.path.isdir(OUTPUT_PATH): os.makedirs(OUTPUT_PATH)

    if options.xmltablefile:
        if args:
            print ("Invalid additional arguments '{}'".format(" ".join(args)))
            exit()
        listOfTestFiles = parseTableDefinitionFile(options.xmltablefile)
        name = os.path.basename(options.xmltablefile)[:-4] # remove ending '.xml'

    else:
        if args:
            inputFiles = args
        else:
            print ("searching resultfiles in '{}'...".format(OUTPUT_PATH))
            inputFiles = [os.path.join(OUTPUT_PATH, '*.results*.xml')]

        inputFiles = Util.extendFileList(inputFiles) # expand wildcards
        listOfTestFiles = [(file, None) for file in inputFiles]

        name = NAME_START + "." + time.strftime("%y%m%d-%H%M", time.localtime())


    # parse test files
    listOfTests = [parseTestFile(file, columnsToShow) for file, columnsToShow in listOfTestFiles]

    if not listOfTests:
        print ('\nError! No file with testresults found.')
        if options.xmltablefile:
            print ('Please check the filenames in your XML-file.')
        exit()

    print ('merging files ...')
    if options.merge:
        # merge list of tests, so that all tests contain the same filenames
        fileNames = mergeSourceFiles(listOfTests)
    else:
        fileNames, listOfTests = ensureEqualSourceFiles(listOfTests)

    # collect data and find out rows with differences
    rows     = getRows(listOfTests, fileNames)
    rowsDiff = filterRowsWithDifferences(rows)

    print ('generating table ...')
    createTables(name, listOfTests, fileNames, rows, rowsDiff)

    print ('done')

    if options.dumpCounts: # print some stats for Buildbot
        countsList = getCounts(rows)
        print ("STATS")
        for counts in countsList:
            print (" ".join(str(e) for e in counts))


if __name__ == '__main__':
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print ('script was interrupted by user')
        pass
