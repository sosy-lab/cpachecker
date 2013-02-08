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

# prepare for Python 3
from __future__ import absolute_import, print_function, unicode_literals

import sys
sys.dont_write_bytecode = True # prevent creation of .pyc files

import xml.etree.ElementTree as ET
import collections
import os.path
import glob
import json
import argparse
import time
import tempita

from decimal import *

import benchmark.result as result

NAME_START = "results" # first part of filename of table

DEFAULT_OUTPUT_PATH = "test/results/"

LIB_URL = "http://www.sosy-lab.org/lib"
LIB_URL_OFFLINE = "lib/javascript"

TEMPLATE_FILE_NAME = os.path.join(os.path.dirname(__file__), 'table-generator-template.{format}')
TEMPLATE_FORMATS = ['html', 'csv']
TEMPLATE_ENCODING = 'UTF-8'


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
        # remove whitespaces and trailing 's' (e.g., in '1.23s')
        s = (s or '').lstrip(' \t').rstrip('s \t')
        if not s or s == '-':
            return Decimal()
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

    @staticmethod
    def flatten(list):
        return [value for sublist in list for value in sublist]

    @staticmethod
    def json(obj):
        return tempita.html(json.dumps(obj))


def parseTableDefinitionFile(file):
    '''
    This function parses the input to get run sets and columns.
    The param 'file' is an XML file defining the result files and columns.

    If column titles are given in the XML file,
    they will be searched in the result files.
    If no title is given, all columns of the result file are taken.

    @return: a list of RunSetResult objects
    '''
    print ("reading table definition from '{0}'...".format(file))
    if not os.path.isfile(file):
        print ('File {0!r} does not exist.'.format(file))
        exit()

    def extractColumnsFromTableDefinitionFile(xmltag):
        """
        Extract all columns mentioned in the result tag of a table definition file.
        """
        return [Column(c.get("title"), c.text, c.get("numberOfDigits"))
                for c in xmltag.findall('column')]

    runSetResults = []
    tableGenFile = ET.ElementTree().parse(file)
    if 'table' != tableGenFile.tag:
        print ("ERROR:\n" \
            + "    The XML file seems to be invalid.\n" \
            + "    The rootelement of table-definition-file is not named 'table'.")
        exit()

    defaultColumnsToShow = extractColumnsFromTableDefinitionFile(tableGenFile)

    baseDir = os.path.dirname(file)

    def getResultTags(rootTag):
        tags = rootTag.findall('result')
        if not tags:
            tags = rootTag.findall('test')
            if tags:
                print("Warning: file {0} contains deprecated 'test' tags, rename them to 'result'".format(file))
        return tags

    for resultTag in getResultTags(tableGenFile):
        columnsToShow = extractColumnsFromTableDefinitionFile(resultTag) or defaultColumnsToShow
        filelist = Util.getFileList(os.path.join(baseDir, resultTag.get('filename'))) # expand wildcards
        runSetResults += [RunSetResult.createFromXML(resultsFile, parseResultsFile(resultsFile), columnsToShow) for resultsFile in filelist]

    for unionTag in tableGenFile.findall('union'):
        columnsToShow = extractColumnsFromTableDefinitionFile(unionTag) or defaultColumnsToShow
        result = RunSetResult([], {}, columnsToShow)

        for resultTag in getResultTags(unionTag):
            filelist = Util.getFileList(os.path.join(baseDir, resultTag.get('filename'))) # expand wildcards
            for resultsFile in filelist:
                result.append(resultsFile, parseResultsFile(resultsFile))

        if result.filelist:
            runSetResults.append(result)

    return runSetResults


class Column:
    """
    The class Column contains title, pattern (to identify a line in logFile),
    and numberOfDigits of a column.
    It does NOT contain the value of a column.
    """
    def __init__(self, title, pattern, numOfDigits):
        self.title = title
        self.pattern = pattern
        self.numberOfDigits = numOfDigits


class RunSetResult():
    """
    The Class RunSetResult contains all the results of one execution of a run set:
    the sourcefiles tags (with sourcefiles + values), the columns to show
    and the benchmark attributes.
    """
    def __init__(self, filelist, attributes, columns):
        self.filelist = filelist
        self.attributes = attributes
        self.columns = columns

    def getSourceFileNames(self):
        return [file.get('name') for file in self.filelist if Util.getColumnValue(file, 'status')]

    def append(self, resultFile, resultElem):
        def updateAttributes(newAttributes):
            for key in newAttributes:
                newValue = newAttributes[key]
                if key in self.attributes:
                    oldValue = self.attributes[key]
                    if not isinstance(oldValue, str):
                        if not newValue in oldValue:
                            oldValue.append(newValue)
                    else:
                        if (oldValue != newValue):
                            self.attributes[key] = [oldValue, newValue]
                else:
                    self.attributes[key] = newAttributes[key]

        self.filelist += resultElem.findall('sourcefile')
        updateAttributes(RunSetResult._extractAttributesFromResult(resultFile, resultElem))

        if not self.columns:
            self.columns = RunSetResult._extractExistingColumnsFromResult(resultFile, resultElem)

    @staticmethod
    def createFromXML(resultFile, resultElem, columns=None):
        '''
        This function extracts everything necessary for creating a RunSetResult object
        from the "result" XML tag of a benchmark result file.
        It returns a RunSetResult object.
        '''
        attributes = RunSetResult._extractAttributesFromResult(resultFile, resultElem)

        if not columns:
            columns = RunSetResult._extractExistingColumnsFromResult(resultFile, resultElem)

        return RunSetResult(resultElem.findall('sourcefile'),
                attributes, columns)

    @staticmethod
    def _extractExistingColumnsFromResult(resultFile, resultElem):
        if resultElem.find('sourcefile') is None:
            print("Empty resultfile found: " + resultFile)
            return []
        else: # show all available columns
            return [Column(c.get("title"), None, None)
                    for c in resultElem.find('sourcefile').findall('column')]

    @staticmethod
    def _extractAttributesFromResult(resultFile, resultTag):
        systemTag = resultTag.find('systeminfo')
        cpuTag = systemTag.find('cpu')
        attributes = {
                'timelimit': None,
                'memlimit':  None,
                'options':   ' ',
                'benchmarkname': resultTag.get('benchmarkname'),
                'name':      resultTag.get('name', resultTag.get('benchmarkname')),
                'branch':    os.path.basename(resultFile).split('#')[0] if '#' in resultFile else '',
                'os':        systemTag.find('os').get('name'),
                'cpu':       cpuTag.get('model'),
                'cores':     cpuTag.get('cores'),
                'freq':      cpuTag.get('frequency'),
                'ram':       systemTag.find('ram').get('size'),
                'host':      systemTag.get('hostname', 'unknown')
                }
        attributes.update(resultTag.attrib)
        return attributes


def parseResultsFile(resultFile):
    '''
    This function parses a XML file with the results of the execution of a run set.
    It returns the "result" XML tag
    '''
    if not os.path.isfile(resultFile):
        print ('File {0!r} is not found.'.format(resultFile))
        exit()

    print ('    ' + resultFile)

    resultElem = ET.ElementTree().parse(resultFile)

    if resultElem.tag not in ['result', 'test']:
        print (("ERROR:\n" \
            + "XML file with benchmark results seems to be invalid.\n" \
            + "The root element of the file is not named 'result' or 'test'.\n" \
            + "If you want to run a table-definition file,\n"\
            + "you should use the option '-x' or '--xml'.").replace('\n','\n    '))
        exit()

    insertLogFileNames(resultFile, resultElem)
    return resultElem

def insertLogFileNames(resultFile, resultElem):
    parts = os.path.basename(resultFile).split("#", 1)

    # get folder of logfiles
    date = resultElem.get('date').replace(':','').replace(' ','_') # from ISO-format to filename-format
    logFolder = resultElem.get('benchmarkname') + '.' + date + '.logfiles/'
    if len(parts) > 1:
        logFolder = parts[0] + '#' + logFolder
    logFolder = os.path.join(os.path.dirname(resultFile), resultElem.get('baseDir', ''), logFolder)

    # append begin of filename
    runSetName = resultElem.get('name')
    if runSetName is not None:
        blockname = resultElem.get('block')
        if blockname is None:
            logFolder += runSetName + "."
        elif blockname == runSetName:
            pass # real runSetName is empty
        else:
            assert runSetName.endswith("." + blockname)
            runSetName = runSetName[:-(1 + len(blockname))] # remove last chars
            logFolder += runSetName + "."

    # for each file: append original filename and insert logFileName into sourcefileElement
    for sourcefile in resultElem.findall('sourcefile'):
        logFileName = os.path.basename(sourcefile.get('name')) + ".log"
        sourcefile.logfile = logFolder + logFileName

def getDefaultLogFolder(resultElem):
    return logFolder


def mergeSourceFiles(runSetResults):
    """
    This function merges the filelists of all RunSetResult objects.
    If necessary, it can merge lists of names: [A,C] + [A,B] --> [A,B,C]
    and add dummy elements to the filelists.
    It also ensures the same order of files.
    Returns a list of filenames
    """
    nameList = []
    nameSet = set()
    for result in runSetResults:
        index = -1
        currentResultNameSet = set()
        for name in result.getSourceFileNames():
            if name in currentResultNameSet:
                print ("File {0} is present twice, skipping it.".format(name))
            else:
                currentResultNameSet.add(name)
                if name not in nameSet:
                    nameList.insert(index+1, name)
                    nameSet.add(name)
                    index += 1
                else:
                    index = nameList.index(name)

    mergeFilelists(runSetResults, nameList)
    return nameList

def mergeFilelists(runSetResults, filenames):
    """
    Set the filelists of all RunSetResult elements so that they contain the same files
    in the same order. For missing files a dummy element is inserted.
    """
    for result in runSetResults:
        # create mapping from name to sourcefile tag
        dic = dict([(file.get('name'), file) for file in result.filelist])
        result.filelist = [] # clear and repopulate filelist
        for filename in filenames:
            fileResult = dic.get(filename)
            if fileResult == None:
                fileResult = ET.Element('sourcefile') # create an empty dummy element
                fileResult.logfile = None
                print ('    no result for {0}'.format(filename))
            result.filelist.append(fileResult)


def findCommonSourceFiles(runSetResults):
    filesInFirstRunSet = runSetResults[0].getSourceFileNames()

    fileSet = set(filesInFirstRunSet)
    for result in runSetResults:
        fileSet = fileSet & set(result.getSourceFileNames())

    fileList = []
    if not fileSet:
        print('No files are present in all benchmark results.')
    else:
        fileList = [file for file in filesInFirstRunSet if file in fileSet]
        mergeFilelists(runSetResults, fileList)

    return fileList


class RunResult:
    """
    The class RunResult contains the results of a single verification run.
    """
    def __init__(self, status, category, logFile, columns, values):
        assert(len(columns) == len(values))
        self.status = status
        self.logFile = logFile
        self.columns = columns
        self.values = values
        self.category = category

    @staticmethod
    def createFromXML(sourcefileTag, listOfColumns, correctOnly):
        '''
        This function collects the values from one run.
        Only columns that should be part of the table are collected.
        '''

        def readLogfileLines(logfileName):
            if not logfileName: return []
            try:
                with open(logfileName) as logfile:
                    return logfile.readlines()
            except IOError as e:
                print('WARNING: Could not read value from logfile: {}'.format(e))
                return []

        def getValueFromLogfile(lines, identifier):
            """
            This method searches for values in lines of the content.
            The format of such a line must be:    "identifier:  value  (rest)".

            If a value is not found, the value is set to None.
            """
            # stop after the first line, that contains the searched text
            for line in lines:
                if identifier in line:
                    startPosition = line.find(':') + 1
                    endPosition = line.find('(') # bracket maybe not found -> (-1)
                    if (endPosition == -1):
                        return line[startPosition:].strip()
                    else:
                        return line[startPosition: endPosition].strip()
            return None

        status = Util.getColumnValue(sourcefileTag, 'status', '')
        category = result.getResultCategory(sourcefileTag.get('name'), status)
        score = result.calculateScore(category)
        logfileLines = None

        if status == '':
            values = [''] * len(listOfColumns)
            return RunResult(status, category, sourcefileTag.logfile, listOfColumns, values)

        values = []

        for column in listOfColumns: # for all columns that should be shown
            value = None # default value
            if column.title.lower() == 'score':
                value = str(score)
            elif column.title.lower() == 'status':
                value = status

            elif not correctOnly or score > 0:
                if not column.pattern: # collect values from XML
                    value = Util.getColumnValue(sourcefileTag, column.title)

                else: # collect values from logfile
                    if logfileLines is None: # cache content
                        logfileLines = readLogfileLines(sourcefileTag.logfile)

                    value = getValueFromLogfile(logfileLines, column.pattern)

            if column.numberOfDigits is not None:
                value = Util.formatNumber(value, column.numberOfDigits)

            values.append(value)

        return RunResult(status, category, sourcefileTag.logfile, listOfColumns, values)


class Row:
    """
    The class Row contains all the results for one sourcefile (a list of RunResult instances).
    It corresponds to one complete row in the final tables.
    """
    def __init__(self, fileName):
        self.fileName = fileName
        self.results = []

    def addRunResult(self, runresult):
        self.results.append(runresult)

    def setRelativePath(self, commonPrefix, baseDir):
        """
        generate output representation of rows
        """
        # make path relative to directory of output file if necessary
        self.filePath = self.fileName if os.path.isabs(self.fileName) \
                                 else os.path.relpath(self.fileName, baseDir)

        self.shortFileName = self.fileName.replace(commonPrefix, '', 1)

def rowsToColumns(rows):
    """
    Convert a list of Rows into a column-wise list of list of RunResult
    """
    return zip(*[row.results for row in rows])


def getRows(runSetResults, fileNames, correctOnly):
    """
    Create list of rows with all data. Each row consists of several RunResults.
    """
    rows = [Row(fileName) for fileName in fileNames]

    # get values for each run set
    for result in runSetResults:
        # get values for each file in a run set
        for fileResult, row in zip(result.filelist, rows):
            row.addRunResult(RunResult.createFromXML(fileResult, result.columns, correctOnly))

    return rows


def filterRowsWithDifferences(rows):
    """
    Find all rows with differences in the status column.
    """
    if not rows:
        # empty table
        return []
    if len(rows[0].results) == 1:
        # table with single column
        return []
    
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
# TODO replace with call to log.debug when this script has logging
#            print ('    difference found:  {0} : {1} --> {2}'.format(
#                        row.fileName.ljust(maxLen), oldStatus, newStatus))


    if len(rowsDiff) == 0:
        print ("---> NO DIFFERENCE FOUND IN COLUMN 'STATUS'")
    elif len(rowsDiff) == len(rows):
        print ("---> DIFFERENCES FOUND IN ALL ROWS, NO NEED TO CREATE DIFFERENCE TABLE")
        return []

    return rowsDiff



def getTableHead(runSetResults, commonFileNamePrefix):

    # This list contains the number of columns each run set has
    # (the width of a run set in the final table)
    # It is used for calculating the column spans of the header cells.
    runSetWidths = [len(runSetResult.columns) for runSetResult in runSetResults]

    def getRow(rowName, format, collapse=False):
        values = [format.format(**runSetResult.attributes) for runSetResult in runSetResults]
        if not any(values): return None # skip row without values completely

        valuesAndWidths = list(Util.collapseEqualValues(values, runSetWidths)) \
                          if collapse else list(zip(values, runSetWidths))

        return tempita.bunch(id=rowName.lower().split(' ')[0],
                             name=rowName,
                             content=valuesAndWidths)

    benchmarkNames = [runSetResult.attributes['benchmarkname'] for runSetResult in runSetResults]
    allBenchmarkNamesEqual = benchmarkNames.count(benchmarkNames[0]) == len(benchmarkNames)

    titles      = [column.title for runSetResult in runSetResults for column in runSetResult.columns]
    runSetWidths1 = [1]*sum(runSetWidths)
    titleRow    = tempita.bunch(id='columnTitles', name=commonFileNamePrefix,
                                content=list(zip(titles, runSetWidths1)))

    return {'tool':    getRow('Tool', '{tool} {version}', collapse=True),
            'limit':   getRow('Limits', 'timelimit: {timelimit}, memlimit: {memlimit}', collapse=True),
            'host':    getRow('Host', '{host}', collapse=True),
            'os':      getRow('OS', '{os}', collapse=True),
            'system':  getRow('System', 'CPU: {cpu} with {cores} cores, frequency: {freq}; RAM: {ram}', collapse=True),
            'date':    getRow('Date of execution', '{date}', collapse=True),
            'runset':  getRow('Run set', '{name}' if allBenchmarkNamesEqual else '{benchmarkname}.{name}'),
            'branch':  getRow('Branch', '{branch}'),
            'options': getRow('Options', '{options}'),
            'title':   titleRow}


def getStats(rows):
    countUnsafe = len([row for row in rows if result.fileIsUnsafe(row.fileName)])
    countSafe = len([row for row in rows if result.fileIsSafe(row.fileName)])
    maxScore = result.SCORE_CORRECT_UNSAFE * countUnsafe + result.SCORE_CORRECT_SAFE * countSafe

    stats = [getStatsOfRunSet(runResults) for runResults in rowsToColumns(rows)] # column-wise
    rowsForStats = list(map(Util.flatten, zip(*stats))) # row-wise

    return [tempita.bunch(default=None, title='total files', content=rowsForStats[0]),
            tempita.bunch(default=None, title='correct results', description='(no bug exists + result is SAFE) OR (bug exists + result is UNSAFE)', content=rowsForStats[1]),
            tempita.bunch(default=None, title='false negatives', description='bug exists + result is SAFE', content=rowsForStats[2]),
            tempita.bunch(default=None, title='false positives', description='no bug exists + result is UNSAFE', content=rowsForStats[3]),
            tempita.bunch(default=None, title='score ({0} files, max score: {1})'.format(len(rows), maxScore), id='score', description='{0} safe files, {1} unsafe files'.format(countSafe, countUnsafe), content=rowsForStats[4])
            ]

def getStatsOfRunSet(runResults):
    """
    This function returns the numbers of the statistics.
    @param runResults: All the results of the execution of one run set (as list of RunResult objects) 
    """

    # convert:
    # [['SAFE', 0,1], ['UNSAFE', 0,2]] -->  [['SAFE', 'UNSAFE'], [0,1, 0,2]]
    # in python2 this is a list, in python3 this is the iterator of the list
    # this works, because we iterate over the list some lines below
    listsOfValues = zip(*[runResult.values for runResult in runResults])

    columns = runResults[0].columns
    statusList = [runResult.category for runResult in runResults]

    # collect some statistics
    sumRow = []
    correctRow = []
    wrongSafeRow = []
    wrongUnsafeRow = []
    scoreRow = []

    for column, values in zip(columns, listsOfValues):
        if column.title == 'status':
            countCorrectSafe, countCorrectUnsafe, countWrongSafe, countWrongUnsafe = getCategoryCount(statusList)

            sum     = StatValue(len(statusList))
            correct = StatValue(countCorrectSafe + countCorrectUnsafe)
            score   = StatValue(
                                result.SCORE_CORRECT_SAFE   * countCorrectSafe + \
                                result.SCORE_CORRECT_UNSAFE * countCorrectUnsafe + \
                                result.SCORE_WRONG_SAFE     * countWrongSafe + \
                                result.SCORE_WRONG_UNSAFE   * countWrongUnsafe,
                                )
            wrongSafe   = StatValue(countWrongSafe)
            wrongUnsafe = StatValue(countWrongUnsafe)

        else:
            sum, correct, wrongSafe, wrongUnsafe = getStatsOfNumberColumn(values, statusList)
            score = ''

        if (sum.sum, correct.sum, wrongSafe.sum, wrongUnsafe.sum) == (0,0,0,0):
            (sum, correct, wrongSafe, wrongUnsafe) = (None, None, None, None)

        sumRow.append(sum)
        correctRow.append(correct)
        wrongSafeRow.append(wrongSafe)
        wrongUnsafeRow.append(wrongUnsafe)
        scoreRow.append(score)

    return (sumRow, correctRow, wrongSafeRow, wrongUnsafeRow, scoreRow)


class StatValue:
    def __init__(self, sum, min=None, max=None, avg=None, median=None):
        self.sum = sum
        self.min = min
        self.max = max
        self.avg = avg
        self.median = median

    def __str__(self):
        return str(self.sum)

    @classmethod
    def fromList(cls, values):
        if not values:
            return StatValue(0)

        return StatValue(sum(values),
                         min    = min(values),
                         max    = max(values),
                         avg    = sum(values) / len(values),
                         median = sorted(values)[len(values)//2],
                         )


def getCategoryCount(categoryList):
    # count different elems in statusList
    counts = collections.defaultdict(int)
    for category in categoryList:
        counts[category] += 1

    return (counts['correctSafe'], counts['correctUnsafe'],
            counts['wrongSafe'], counts['wrongUnsafe'])


def getStatsOfNumberColumn(values, categoryList):
    assert len(values) == len(categoryList)
    try:
        valueList = [Util.toDecimal(v) for v in values]
    except InvalidOperation as e:
        print("Warning: {0}. Statistics may be wrong.".format(e))
        return (StatValue(0), StatValue(0), StatValue(0), StatValue(0))

    valuesPerCategory = collections.defaultdict(list)
    for value, category in zip(valueList, categoryList):
        if category and category.startswith('correct'):
            category = 'correct'
        valuesPerCategory[category] += [value]

    return (StatValue.fromList(valueList),
            StatValue.fromList(valuesPerCategory['correct']),
            StatValue.fromList(valuesPerCategory['wrongSafe']),
            StatValue.fromList(valuesPerCategory['wrongUnsafe']),
            )


def getCounts(rows): # for options.dumpCounts
    countsList = []

    for runResults in rowsToColumns(rows):
        statusList = [runResult.category for runResult in runResults]
        correctSafe, correctUnsafe, wrongSafe, wrongUnsafe = getCategoryCount(statusList)

        correct = correctSafe + correctUnsafe
        wrong = wrongSafe + wrongUnsafe
        unknown = len(statusList) - correct - wrong

        countsList.append((correct, wrong, unknown))

    return countsList


def createTables(name, runSetResults, fileNames, rows, rowsDiff, outputPath, outputFilePattern, libUrl):
    '''
    create tables and write them to files
    '''

    # get common folder of sourcefiles
    commonPrefix = os.path.commonprefix(fileNames) # maybe with parts of filename
    commonPrefix = commonPrefix[: commonPrefix.rfind('/') + 1] # only foldername
    list(map(lambda row: Row.setRelativePath(row, commonPrefix, outputPath), rows))

    head = getTableHead(runSetResults, commonPrefix)
    runSetsData = [runSetResult.attributes for runSetResult in runSetResults]
    runSetsColumns = [[column.title for column in runSet.columns] for runSet in runSetResults]

    templateNamespace={'flatten': Util.flatten,
                       'json': Util.json,
                       'relpath': os.path.relpath,
                       }

    def writeTable(type, title, rows):
        stats = getStats(rows)

        for format in TEMPLATE_FORMATS:
            outfile = os.path.join(outputPath, outputFilePattern.format(name=name, type=type, ext=format))
            print ('writing {0} into {1} ...'.format(format.upper().ljust(4), outfile))

            # read template
            Template = tempita.HTMLTemplate if format == 'html' else tempita.Template
            template = Template.from_filename(TEMPLATE_FILE_NAME.format(format=format),
                                              namespace=templateNamespace,
                                              encoding=TEMPLATE_ENCODING)

            # write file
            with open(outfile, 'w') as file:
                file.write(template.substitute(
                        title=title,
                        head=head,
                        body=rows,
                        foot=stats,
                        runSets=runSetsData,
                        columns=runSetsColumns,
                        lib_url=libUrl,
                        baseDir=outputPath,
                        ))


    # write normal tables
    writeTable("table", name, rows)

    # write difference tables
    if rowsDiff:
        writeTable("diff", name + " differences", rowsDiff)


def main(args=None):

    if args is None:
        args = sys.argv

    parser = argparse.ArgumentParser(
        description="""Create table with the results of one or more benchmark executions.
        Documented example files for the table definitions can be found in 'doc/examples'\n"""
    )

    parser.add_argument("tables",
        metavar="RESULT",
        type=str,
        nargs='*',
        help="XML file with the results from the benchmark script"
    )
    parser.add_argument("-x", "--xml",
        action="store",
        type=str,
        dest="xmltablefile",
        help="XML file with the table definition."
    )
    parser.add_argument("-o", "--outputpath",
        action="store",
        type=str,
        dest="outputPath",
        help="Output path for the tables."
    )
    parser.add_argument("-d", "--dump",
        action="store_true", dest="dumpCounts",
        help="Print summary statistics for the good, bad, and unknown counts."
    )
    parser.add_argument("-c", "--common",
        action="store_true", dest="common",
        help="Put only sourcefiles into the table for which all benchmarks contain results."
    )
    parser.add_argument("--no-diff",
        action="store_false", dest="writeDiffTable",
        help="Do not output a table with result differences between benchmarks."
    )
    parser.add_argument("--correct-only",
        action="store_true", dest="correctOnly",
        help="Clear all results (e.g., time) in cases where the result was not correct."
    )
    parser.add_argument("--offline",
        action="store_const", dest="libUrl",
        const=LIB_URL_OFFLINE,
        default=LIB_URL,
        help="Don't insert links to http://www.sosy-lab.org, instead expect JS libs in libs/javascript."
    )

    options = parser.parse_args(args[1:])

    outputPath = options.outputPath
    outputFilePattern = "{name}.{type}.{ext}"

    if options.xmltablefile:
        if options.tables:
            print ("Invalid additional arguments '{}'".format(" ".join(options.tables)))
            exit()
        runSetResults = parseTableDefinitionFile(options.xmltablefile)
        name = os.path.basename(options.xmltablefile)
        if name.endswith(".xml"):
            name = name[:-4]

        if not outputPath:
            outputPath = os.path.dirname(options.xmltablefile)

    else:
        if options.tables:
            inputFiles = options.tables
        else:
            searchDir = outputPath or DEFAULT_OUTPUT_PATH
            print ("searching result files in '{}'...".format(searchDir))
            inputFiles = [os.path.join(searchDir, '*.results*.xml')]

        inputFiles = Util.extendFileList(inputFiles) # expand wildcards
        runSetResults = [RunSetResult.createFromXML(file, parseResultsFile(file)) for file in inputFiles]

        if len(inputFiles) == 1:
            name = os.path.basename(inputFiles[0])
            if name.endswith(".xml"):
                name = name[:-4]
            outputFilePattern = "{name}.{ext}"
        else:
            name = NAME_START + "." + time.strftime("%y-%m-%d_%H%M", time.localtime())

        if inputFiles and not outputPath:
            dir = os.path.dirname(inputFiles[0])
            if all(dir == os.path.dirname(file) for file in inputFiles):
                outputPath = dir
            else:
                outputPath = DEFAULT_OUTPUT_PATH

    if not outputPath:
        outputPath = '.'

    if not runSetResults:
        print ('\nError! No file with benchmark results found.')
        if options.xmltablefile:
            print ('Please check the filenames in your XML-file.')
        exit()

    print ('merging results ...')
    if options.common:
        fileNames = findCommonSourceFiles(runSetResults)
    else:
        # merge list of run sets, so that all run sets contain the same filenames
        fileNames = mergeSourceFiles(runSetResults)

    # collect data and find out rows with differences
    print ('collecting data ...')
    rows     = getRows(runSetResults, fileNames, options.correctOnly)
    if not rows:
        print ('Warning: No results found, no tables produced.')
        sys.exit()

    rowsDiff = filterRowsWithDifferences(rows) if options.writeDiffTable else []

    print ('generating table ...')
    if not os.path.isdir(outputPath): os.makedirs(outputPath)
    createTables(name, runSetResults, fileNames, rows, rowsDiff, outputPath, outputFilePattern, options.libUrl)

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
