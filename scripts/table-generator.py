#!/usr/bin/env python

import xml.etree.ElementTree as ET
import os.path
import glob
import shutil
import optparse
import time

from datetime import date
from decimal import *

OUTPUT_PATH = "test/results/"

LOGFILES_IN_HTML = True # create links to logfiles in html

CSV_SEPARATOR = '\t'


# string searched in filenames to determine correct or incorrect status.
# use lower case!
BUG_SUBSTRING_LIST = ['bug', 'unsafe']


DOCTYPE = '''
<!DOCTYPE HTML>
'''


CSS = '''
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">

<style type="text/css"> 
    <!--
    table { outline:3px solid black; border-spacing:0px; font-family:arial, sans serif}
    thead { text-align:center}
    tbody { text-align:right}
    tfoot { text-align:center}
    tr:hover { background-color:yellow}
    td { border:1px solid black}
    td:first-child { text-align:left; white-space:nowrap}
    tbody td:first-child { font-family: monospace; }
    #options td:not(:first-child) {  text-align:left; font-size: x-small; 
                                     font-family: monospace; }
    tbody tr:first-child td { border-top:3px solid black}
    tfoot tr:first-child td { border-top:3px solid black}
    .correctSafe, .correctUnsafe { text-align:center; color:green}
    .wrongSafe, .wrongUnsafe { text-align:center; color:red; font-weight: bold; }
    .unknown { text-align:center; color:orange; font-weight: bold; }
    .score { text-align:center; font-size:large; font-weight:bold; }
    a { color: inherit; text-decoration: none; display: block; }
    a:hover { background: lime }
    -->
</style> 
'''


TITLE = '''
<title>table of tests</title>
'''

# space in front of a line in htmlcode (4 spaces)
HTML_SHIFT = '    '


# scoreValues taken from http://sv-comp.sosy-lab.org/
SCORE_CORRECT_SAFE = 2
SCORE_CORRECT_UNSAFE = 1
SCORE_UNKNOWN = 0
SCORE_WRONG_UNSAFE = -2
SCORE_WRONG_SAFE = -4


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
            print '    ' + resultFile

            resultElem = ET.ElementTree().parse(resultFile)

            if 'test' != resultElem.tag:
                print ("ERROR:\n" \
                    + "XML-file seems to be invalid.\n" \
                    + "The rootelement of testresult is not named 'test'.\n" \
                    + "If you want to run a table-definition-file,\n"\
                    + "you should use the option '-x' or '--xml'.").replace('\n','\n    ')
                exit()

            # check for equal files in the tests
            if len(listOfTests) and not containEqualFiles(listOfTests[0][0], resultElem):
                print ('        resultfile contains different files, skipping resultfile')
                continue

            availableColumnTitles = [column.get("title") for column in 
                                resultElem.find('sourcefile').findall('column')]
            if columns: # not None
                    columnTitles = [column.get("title") for column in columns
                                    if column.get('title') in availableColumnTitles]
            else:
                columnTitles = availableColumnTitles

            if LOGFILES_IN_HTML: insertLogFileNames(resultElem)

            listOfTests.append((resultElem, columnTitles))
        else:
            print 'File {0} is not found.'.format(repr(resultFile))
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


def insertLogFileNames(resultElem):
    # get folder of logfiles
    logFolder = resultElem.get('benchmarkname') + "." + resultElem.get('date') + ".logfiles/"

    # create folder for txt-files (copies of the logfiles)
    txtFolder = 'table/' + logFolder
    if not os.path.isdir(OUTPUT_PATH + txtFolder):
            os.makedirs(OUTPUT_PATH + txtFolder)

    # append begin of filename
    testname = resultElem.get('name')
    if testname is not None:
        logFolder += testname + "."
        txtFolder += testname + "."

    # for each file: append original filename and insert logFileName into sourcefileElement
    for sourcefile in resultElem.findall('sourcefile'):
        logFileName = os.path.basename(sourcefile.get('name'))

        # copy logfiles to extra folder and rename them to '.txt'
        logFile = OUTPUT_PATH + logFolder + logFileName + ".log"
        txtFile = OUTPUT_PATH + txtFolder + logFileName + ".txt"
        try:
            shutil.copyfile(logFile, txtFile)
        except IOError:
            print 'logfile not found or not copied:\n' + logFile

        sourcefile.set('logfileForHtml', txtFolder + logFileName + ".txt")


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
        print '\nWarning: no file matches "{0}".'.format(shortFile)

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
    toolRow = getToolRow(listOfTests, testWidths)
    limitRow = getLimitRow(listOfTests, testWidths)
    systemRow = getSystemRow(listOfTests, testWidths)
    dateRow = getDateRow(listOfTests, testWidths)
    (testRow, testLine) = getTestRow(listOfTests, testWidths)
    testOptions = getOptionsRow(listOfTests, testWidths)

    return (('\n' + HTML_SHIFT).join([HTML_SHIFT + '<thead>', toolRow,
            limitRow, systemRow, dateRow, testRow, testOptions, columnRow]) + '\n</thead>',
            testLine + '\n' + titleLine + '\n')


def getColumnsRowAndTestWidths(listOfTests):
    '''
    get columnsRow and testWidths, for all columns that should be shown
    '''

    columnsTitles = []
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
    
    return ('<tr id="columnTitles"><td>' + '</td><td>'.join([' '] + columnsTitles) + '</td></tr>',
            testWidths,
            CSV_SEPARATOR.join(['filename'] + columnsTitles))


def getToolRow(listOfTests, testWidths):
    '''
    get toolRow, each cell of it spans over all tests of this tool
    '''

    toolRow = '<tr><td>tool</td>'
    tool = (listOfTests[0][0].get('tool'), listOfTests[0][0].get('version'))
    toolWidth = 0

    for (testResult, _), numberOfColumns in zip(listOfTests, testWidths):
        newTool = (testResult.get('tool'), testResult.get('version'))
        if newTool != tool:
            toolRow += '<td colspan="{0}">{1} {2}</td>'.format(toolWidth, *tool)
            toolWidth = 0
            tool = newTool
        toolWidth += numberOfColumns
    toolRow += '<td colspan="{0}">{1} {2}</td></tr>'.format(toolWidth, *tool)

    return toolRow


def getLimitRow(listOfTests, testWidths):
    '''
    get limitRow, each cell of it spans over all tests with this limit
    '''

    limitRow = '<tr><td>limits</td>'
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

    systemLine = '<tr><td>system</td>'
    systemWidth = 0
    systemTag = listOfTests[0][0].find('systeminfo')
    cpuTag = systemTag.find('cpu')
    system = (systemTag.find('os').get('name'), cpuTag.get('model'), cpuTag.get('cores'),
                cpuTag.get('frequency'), systemTag.find('ram').get('size'))

    for (testResult, columns), numberOfColumns in zip(listOfTests, testWidths):
        systemTag = testResult.find('systeminfo')
        cpuTag = systemTag.find('cpu')
        newSystem = (systemTag.find('os').get('name'), cpuTag.get('model'), cpuTag.get('cores'),
                    cpuTag.get('frequency'), systemTag.find('ram').get('size'))
        if newSystem != system:
            systemLine += '<td colspan="{0}">os: {1}<br>cpu: {2}<br>cores: {3}, \
            frequency: {4}, ram: {5}</td>'.format(systemWidth, *system)
            systemWidth = 0
            system = newSystem
        systemWidth += numberOfColumns
    systemLine += '<td colspan="{0}">os: {1}<br>cpu: {2}<br>cores: {3}, \
            frequency: {4}, ram: {5}</td></tr>'.format(systemWidth, *system)

    return systemLine


def getDateRow(listOfTests, testWidths):
    '''
    get dateRow, each cell of it spans over all tests with this date
    '''

    dateRow = '<tr><td>date of test</td>'
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
    testLine = CSV_SEPARATOR.join([CSV_SEPARATOR.join([testName]*width)
             for (testName, width) in zip(testNames, testWidths) if width])

    return ('<tr><td>test</td>' + ''.join(tests) + '</tr>',
            testLine)


def getOptionsRow(listOfTests, testWidths):
    '''
    create optionsRow, each cell spans over the columns of a test
    '''

    testOptions = [testResult.get('options', ' ') for (testResult, _) in listOfTests]
    options = ['<td colspan="{0}">{1}</td>'.format(width, testOption.replace(' -','<br>-'))
             for (testOption, width) in zip(testOptions, testWidths) if width]
    return '<tr id="options"><td>options</td>' + ''.join(options) + '</tr>'


def getTableBody(listOfTests):
    '''
    This function build the body and the foot of the table.
    It collects all values from the tests for the columns in the table.
    The foot contains some statistics.
    '''

    rowsForHTML = []
    rowsForCSV = []
    fileList = listOfTests[0][0].findall('sourcefile')
    rowsForStats = [['<td>total sum</td>'],
                    ['<td title="(no bug exists + result is SAFE) OR ' + \
                     '(bug exists + result is UNSAFE)">correct results</td>'],
                    ['<td title="bug exists + result is SAFE">false positives</td>'],
                    ['<td title="no bug exists + result is UNSAFE">false negatives</td>'],
                    ['<td>score ({0} files)</td>'.format(len(fileList))]]

    # get filenames
    for file in fileList:
        fileName = file.get("name")
        filePath = getPathOfSourceFile(fileName)

        rowsForHTML.append(['<td><a href="{0}">{1}</a></td>'.format(filePath, fileName)])
        rowsForCSV.append([fileName])

    # get values for each test
    for testResult, columns in listOfTests:

        valuesListHTML = []
        valuesListCSV = []

        # get values for each file in a test
        for fileResult in testResult.findall('sourcefile'):
            (valuesHTML, valuesCSV) = getValuesOfFileXTest(fileResult, columns)
            valuesListHTML.append(valuesHTML)
            valuesListCSV.append(valuesCSV)

        # append values to html and csv
        map(list.extend, rowsForHTML, valuesListHTML)
        map(list.extend, rowsForCSV, valuesListCSV)

        # get statistics
        stats = getStatsOfTest(testResult.findall('sourcefile'), columns, valuesListCSV)
        map(list.extend, rowsForStats, stats)

    rowsHTML = '</tr>\n{0}<tr>'.format(HTML_SHIFT).join(map(''.join, rowsForHTML))
    statsHTML = '</tr>\n{0}<tr>'.format(HTML_SHIFT).join(map(''.join, rowsForStats))

    return ('<tbody>\n{0}<tr>{1}</tr>\n</tbody>'.format(HTML_SHIFT, rowsHTML),
            '<tfoot>\n{0}<tr>{1}</tr>\n</tfoot>'.format(HTML_SHIFT, statsHTML),
            '\n'.join(map(CSV_SEPARATOR.join, rowsForCSV)))


def getPathOfSourceFile(filename):
    '''
    This method expand a filename of a sourcefile to a path to the sourcefile.
    An absolute filename will not be changed, 
    a filename, that is relative to CPAchackerDir, will get a prefix.
    '''
    if not filename.startswith('/'): # not absolute -> relative, TODO: windows?
        filename = './' + '../'*OUTPUT_PATH.count("/") + filename

    return filename


def getValuesOfFileXTest(currentFile, listOfColumns):
    '''
    This function collects the values from all tests for one file.
    Only columns, that should be part of the table, are collected.
    '''

    currentFile.status = 'unknownStatus'

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
                    else:
                            currentFile.status = 'unknown'

                    if LOGFILES_IN_HTML:                
                        valuesForHTML.append('<td class="{0}"><a href="{1}">{2}</a></td>'
                            .format(currentFile.status, str(currentFile.get('logfileForHtml')), status))
                    else:
                        valuesForHTML.append('<td class="{0}">{1}</td>'
                            .format(currentFile.status, status))

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


def getStatsOfTest(fileResult, columns, valuesList):

    # list for status of bug vs tool
    statusList = [file.status for file in fileResult]
    assert len(valuesList) == len(statusList)

    # convert:
    # [['SAFE', 0,1], ['UNSAFE', 0,2]] -->  [['SAFE', 'UNSAFE'], [0,1, 0,2]]
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
            try:
                (sum, correctSum, wrongSafeNumber, wrongUnsafeNumber) \
                    = getStatsOfNumber(map(toDecimal, column), statusList)
            except InvalidOperation:
                (sum, correctSum, wrongSafeNumber, wrongUnsafeNumber) = (0, 0, 0, 0)
                print ("Warning: NumberParseException. Statistics may be wrong.")
            sumRow.append('<td>{0}</td>'.format(sum))
            sumCorrectRow.append('<td>{0}</td>'.format(correctSum))
            wrongSafeRow.append('<td>{0}</td>'.format(wrongSafeNumber))
            wrongUnsafeRow.append('<td>{0}</td>'.format(wrongUnsafeNumber))
            scoreRow.append('<td></td>')

    # convert numbers to strings for output
    return (sumRow, sumCorrectRow, wrongSafeRow, wrongUnsafeRow, scoreRow)


def getStatsOfNumber(valueList, statusList=None):
    assert len(valueList) == len(statusList)
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

    print 'collecting files ...'

    if filesFromXML:
        listOfTests = getListOfTests(file, True)
        HTMLOutFileName = OUTPUT_PATH + os.path.basename(file)[:-3] + "table.html"
        CSVOutFileName = OUTPUT_PATH + os.path.basename(file)[:-3] + "table.csv"
    else:
        listOfTests = getListOfTests(file)
        timestamp = time.strftime("%y%m%d-%H%M", time.localtime())
        HTMLOutFileName = OUTPUT_PATH + "results." + timestamp + ".table.html"
        CSVOutFileName = OUTPUT_PATH + "results." + timestamp + ".table.csv"

    if len(listOfTests) == 0:
        print '\nError! No file with testresults found.\n' \
            + 'Please check the filenames in your XML-file.'
        exit()

    print 'generating html ...'

    (tableHeadHTML, tableHeadCSV) = getTableHead(listOfTests)
    (tableBodyHTML, tableFootHTML, tableBodyCSV) = getTableBody(listOfTests)

    tableCode = '<table>\n' \
                + tableHeadHTML.replace('\n','\n' + HTML_SHIFT) \
                + '\n' + HTML_SHIFT \
                + tableFootHTML.replace('\n','\n' + HTML_SHIFT) \
                + '\n' + HTML_SHIFT \
                + tableBodyHTML.replace('\n','\n' + HTML_SHIFT) \
                + '\n</table>\n\n'

    htmlCode = DOCTYPE + '<html>\n\n<head>\n' + CSS + TITLE + '\n</head>\n\n<body>\n\n' \
                + tableCode + '</body>\n\n</html>'

    HTMLFile = open(HTMLOutFileName, "w")
    HTMLFile.write(htmlCode)
    HTMLFile.close()

    CSVCode = tableHeadCSV + tableBodyCSV

    CSVFile = open(CSVOutFileName, "w")
    CSVFile.write(CSVCode)
    CSVFile.close()

    print 'done'


def main(args=None):

    parser = optparse.OptionParser('%prog [options] sourcefile')
    parser.add_option("-x", "--xml", 
        action="store", 
        type="string", 
        dest="xmltablefile",
        help="xmlfile for table. If this option is used, other args are ignored."
    )
    options, args = parser.parse_args()

    if args is None:
        args = sys.argv    

    if options.xmltablefile:
        print ("reading table definition from '" + options.xmltablefile + "'...")
        if not os.path.exists(options.xmltablefile) \
                or not os.path.isfile(options.xmltablefile):
            print 'File {0} does not exist.'.format(repr(options.xmltablefile))
            exit()
        else:
            createTable(options.xmltablefile, True)

    elif len(args) > 0:
        createTable(args)

    else: # default case
        print ("searching resultfiles in '" + OUTPUT_PATH + "'...")
        createTable([OUTPUT_PATH + '*.????-??-??.results*.xml'])


if __name__ == '__main__':
    try:
        import sys
        sys.exit(main())
    except LookupError as e:
        print e
    except KeyboardInterrupt:
        print 'script was interrupted by user'
        pass
