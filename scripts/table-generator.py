#!/usr/bin/env python

import xml.etree.ElementTree as ET
import os.path
import glob

from datetime import date


OUTPUT_PATH = "test/results/"

LOGFILES_IN_HTML = True # create links to logfiles in hmtl

CSV_SEPARATOR = '\t'


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
    tr:hover { background-color:yellow}
    td { border:1px solid black}
    td:first-child { text-align:left; white-space:nowrap}
    tbody td:first-child { font-family: monospace; }
    #options td:not(:first-child) {  text-align:left; font-size: x-small; 
                                     font-family: monospace; }
    #columnTitles td { border-bottom:3px solid black}
    .correctStatus { text-align:center; color:green}
    .wrongStatus { text-align:center; color:red; font-weight: bold; }
    a { color: inherit; text-decoration: none; display: block; }
    a:hover { background: orange }
    -->
</style> 
'''


TITLE = '''
<title>table of tests</title>
'''

# space in front of a line in htmlcode (4 spaces)
HTML_SHIFT = '    '


def getListOfTests(file):
    '''
    This function parses the input. The inputfile is an 
    xml-file containing the testfiles and columns.
    Currently the tests are read also from xml-files.

    If columntitles are given in the inputfile,
    they will be searched in the testfiles.
    If no title is given, all columns of the testfile are taken.

    @return: a list of tuples, 
    each tuple contains a testelement and a list of columntitles
    '''

    tableGenFile = ET.ElementTree().parse(file)
    listOfTests = []
    for test in tableGenFile.findall('test'):

        for resultFile in getFileList(test.get('filename')): # expand wildcards
            if os.path.exists(resultFile) and os.path.isfile(resultFile):
                columns = test.findall('column')
                resultElem = ET.ElementTree().parse(resultFile)
                if columns: # not empty
                    columnTitles = [column.get("title") for column in columns]
                else:
                    columnTitles = [column.get("title") for column in 
                                    resultElem.find('sourcefile').findall('column')]
    
                if LOGFILES_IN_HTML: insertLogFileNames(resultElem)

                listOfTests.append((resultElem, columnTitles))
            else:
                print 'File {0} is not found.'.format(repr(resultFile))
                exit()

    return listOfTests


def insertLogFileNames(resultElem):
    # get folder of logfiles
    logFolder = resultElem.get('benchmarkname') + "." + resultElem.get('date') + ".logfiles/"

    # append begin of filename
    testname = resultElem.get('name')
    if testname is not None:
        logFolder += testname + "."

    # for each file: append original filename and insert logFileName into sourcefileElement
    for sourcefile in resultElem.findall('sourcefile'):
        logFileName = logFolder + os.path.basename(sourcefile.get('name')) + ".log"
        sourcefile.set('logfileForHtml', logFileName)


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
    This function build the body of the table.
    It collects all values from the tests for the columns in the table.
    '''

    # get list of lists (test X file) and convert to list of lists (file X test)
    listOfFiles = zip(*[[fileTag for fileTag in test[0].findall('sourcefile')] 
                        for test in listOfTests])
    rowsForHTML = []
    rowsForCSV = []
    for file in listOfFiles:
        columnValuesForHTML = []
        columnValuesForCSV = []
        for testResult, test in zip(file, listOfTests):
            (valuesForHTML, valuesForCSV) = getValuesOfFileXTest(testResult, test[1])
            columnValuesForHTML += valuesForHTML
            columnValuesForCSV += valuesForCSV

        fileName = file[0].get("name")
        rowsForHTML.append(HTML_SHIFT 
                    + '<tr><td>{0}</td>'.format(fileName) \
                    + ''.join(columnValuesForHTML) + '</tr>\n')
        rowsForCSV.append(CSV_SEPARATOR.join([fileName] + columnValuesForCSV))

    return ('<tbody>\n' + "".join(rowsForHTML) + '</tbody>',
            '\n'.join(rowsForCSV))


def getValuesOfFileXTest(currentFile, listOfColumns):
    '''
    This function collects the values from all tests for one file.
    Only columns, that should be part of the table, are collected.
    '''

    valuesForHTML = []
    valuesForCSV = []
    for columnTitle in listOfColumns: # for all columns that should be shown
        for column in currentFile.findall('column'):
            if columnTitle == column.get('title'):

                value = column.get('value')

                valuesForCSV.append(value)

                if columnTitle == 'status':
                    # different colors for correct and incorrect results
                    fileName = currentFile.get('name')
                    status = value.lower()
                    isSafeFile = fileName.lower().find('bug') == -1

                    if (isSafeFile and status == 'safe') or (
                        not isSafeFile and status == 'unsafe'):
                        valueForHTML = '<td class="correctStatus">'
                    else:
                        valueForHTML = '<td class="wrongStatus">'

                    if LOGFILES_IN_HTML:                
                        valuesForHTML.append(valueForHTML + '<a href="{0}">{1}</a></td>'
                            .format(str(currentFile.get('logfileForHtml')), status))
                    else:
                        valuesForHTML.append(valueForHTML + '{0}</td>'.format(status))

                else:
                    valuesForHTML.append('<td>{0}</td>'.format(value))
                break

    return (valuesForHTML, valuesForCSV)


def createTable(file):
    '''
    parse inputfile, create html-code and write it to file
    '''

    print 'generating html ...',

    listOfTests = getListOfTests(file)

    if len(listOfTests) == 0:
        print '\nError! No file with testresults found.\n' \
            + 'Please check the filenames in your XML-file.'
        exit()

    (tableHeadHTML, tableHeadCSV) = getTableHead(listOfTests)
    (tableBodyHTML, tableBodyCSV) = getTableBody(listOfTests)
    
    tableCode = '<table>\n' \
                + tableHeadHTML.replace('\n','\n' + HTML_SHIFT) \
                + '\n' + HTML_SHIFT \
                + tableBodyHTML.replace('\n','\n' + HTML_SHIFT) \
                + '\n</table>\n\n'

    htmlCode = DOCTYPE + '<html>\n\n<head>\n' + CSS + TITLE + '\n</head>\n\n<body>\n\n' \
                + tableCode + '</body>\n\n</html>'

    HTMLFile = open(OUTPUT_PATH + os.path.basename(file)[:-3] + "table.html", "w")
    HTMLFile.write(htmlCode)
    HTMLFile.close()

    CSVCode = tableHeadCSV + tableBodyCSV

    CSVFile = open(OUTPUT_PATH + os.path.basename(file)[:-3] + "table.csv", "w")
    CSVFile.write(CSVCode)
    CSVFile.close()

    print 'done'


def allEqual(list):
    if list is None:
        return false # should not be not needed
    else:
        # how often does the first element appear in list?
        return list.count(list[0]) == len(list)


def main(args=None):

    if args is None:
        args = sys.argv    

    # check parameters
    if len(args) < 2:
        print 'please insert a file'
        exit()

    for file in args[1:]:
        if not os.path.exists(file) or not os.path.isfile(file):
            print 'File {0} does not exist.'.format(repr(file))
            exit()
        else:
            createTable(file)


if __name__ == '__main__':
    try:
        import sys
        sys.exit(main())
    except LookupError as e:
        print e
    except KeyboardInterrupt:
        print 'script was interrupted by user'
        pass
