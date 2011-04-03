#!/usr/bin/env python

import xml.etree.ElementTree as ET
import os.path

from datetime import date


OUTPUT_PATH = "test/results/"


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
    td { border:1px solid black}
    td:first-child { text-align:left}
    #columnTitles td { border-bottom:3px solid black}
    .correctStatus { text-align:center; color:green}
    .wrongStatus { text-align:center; color:red}
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
        resultFile = test.get('filename')
        if os.path.exists(resultFile) and os.path.isfile(resultFile):
            columns = test.findall('column')
            resultElem = ET.ElementTree().parse(resultFile)
            if columns: # not empty
                columnTitles = [column.get("title") for column in columns]
            else:
                columnTitles = [column.get("title") for column in 
                                resultElem.find('sourcefile').findall('column')]

            listOfTests.append((resultElem, columnTitles))
        else:
            print 'File {0} is not found.'.format(repr(resultFile))
            exit()
    return listOfTests


def getTableHead(listOfTests):
    '''
    get tablehead (tools, limits, testnames, systeminfo)
    '''

    (columnRow, testWidths) = getColumnsRowAndTestWidths(listOfTests)
    toolRow = getToolRow(listOfTests, testWidths)
    limitRow = getLimitRow(listOfTests, testWidths)
    systemRow = getSystemRow(listOfTests, testWidths)
    testRow = getTestRow(listOfTests, testWidths)

    return ('\n' + HTML_SHIFT).join([HTML_SHIFT + '<thead>', toolRow,
            limitRow, systemRow, testRow, columnRow]) + '\n</thead>'


def getColumnsRowAndTestWidths(listOfTests):
    '''
    get columnsRow and testWidths, for all columns that should be shown
    '''

    columnsTitles = [' ']
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
    
    return ('<tr id="columnTitles"><td>' + '</td><td>'.join(columnsTitles) + '</td></tr>',
            testWidths)


def getToolRow(listOfTests, testWidths):
    '''
    get toolRow, each cell of it spans over all tests of this tool
    '''

    toolRow = '<tr><td>tool</td>'
    tool = (listOfTests[0][0].get('tool'), listOfTests[0][0].get('version'))
    toolWidth = 0

    for (testResult, _), numberOfColumns in zip(listOfTests, testWidths):
        newTool = (testResult.get('tool'), testResult.get('version'))
        if newTool == tool:
            toolWidth += numberOfColumns
        else:
            toolRow += '<td colspan="{0}">{1} {2}</td>'.format(toolWidth, *tool)
            toolWidth = 0
            tool = newTool
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
        if newLimit == limit:
            limitWidth += numberOfColumns
        else:
            limitRow += '<td colspan="{0}">timelimit: {1}, memlimit: {2}</td>'\
                            .format(limitWidth, *limit)
            limitWidth = 0
            limit = newLimit
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
        if newSystem == system:
            systemWidth += numberOfColumns
        else:
            systemLine += '<td colspan="{0}">os: {1}<br>cpu: {2}<br>cores: {3}, \
            frequency: {4}, ram: {5}</td>'.format(systemWidth, *system)
            systemWidth = 0
            system = newSystem
    systemLine += '<td colspan="{0}">os: {1}<br>cpu: {2}<br>cores: {3}, \
            frequency: {4}, ram: {5}</td></tr>'.format(systemWidth, *system)

    return systemLine


def getTestRow(listOfTests, testWidths):
    '''
    create testRow, each cell spans over all columns of this test
    '''

    testNames = [testResult.get('name', ' ') for (testResult, _) in listOfTests]
    tests = ['<td colspan="{0}">{1}</td>'.format(width, testName)
             for (testName, width) in zip(testNames, testWidths) if width]
    return '<tr><td>test</td>' + ''.join(tests) + '</tr>'


def getTableBody(listOfTests):
    '''
    This function build the body of the table.
    It collects all values from the tests for the columns in the table.
    '''

    # get list of lists (test X file) and convert to list of lists (file X test)
    listOfFiles = zip(*[[fileTag for fileTag in test[0].findall('sourcefile')] 
                        for test in listOfTests])
    rows = []
    for file in listOfFiles:
        columnValues = []
        for testResult, test in zip(file, listOfTests):
            columnValues += getValuesOfFileXTest(testResult, test[1])
        rows.append(HTML_SHIFT 
                    + '<tr><td>{0}</td>'.format(file[0].get("name")) \
                    + ''.join(columnValues) + '</tr>\n')
    return '<tbody>\n' + "".join(rows) + '</tbody>'


def getValuesOfFileXTest(currentFile, listOfColumns):
    '''
    This function collects the values from all tests for one file.
    Only columns, that should be part of the table, are collected.
    '''

    valuesOfLine = []
    for columnTitle in listOfColumns: # for all columns that should be shown
        for column in currentFile.findall('column'):
            if columnTitle == column.get('title'):

                if columnTitle == 'status':
                    # different colors for correct and incorrect results
                    fileName = currentFile.get('name')
                    status = column.get('value').lower()
                    isSafeFile = fileName.lower().find('bug') == -1
                    if (isSafeFile and status == 'safe') or (
                        not isSafeFile and status == 'unsafe'):
                        valuesOfLine.append('<td class="correctStatus">{0}</td>'.format(status))
                    else:
                        valuesOfLine.append('<td class="wrongStatus">{1}</td>'.format(status))

                else:
                    valuesOfLine.append('<td>{0}</td>'.format(column.get('value')))
                break
    return valuesOfLine


def createHTML(file):
    '''
    parse inputfile, create html-code and write it to file
    '''

    listOfTests = getListOfTests(file)

    tableCode = '<table>\n' \
                + getTableHead(listOfTests).replace('\n','\n' + HTML_SHIFT) \
                + '\n' + HTML_SHIFT \
                + getTableBody(listOfTests).replace('\n','\n' + HTML_SHIFT) \
                + '\n</table>\n\n'

    htmlCode = DOCTYPE + '<html>\n\n<head>\n' + CSS + TITLE + '\n</head>\n\n<body>\n\n' \
                + tableCode + '</body>\n\n</html>'


    print htmlCode

    HTMLFile = open(OUTPUT_PATH + os.path.basename(file)[:-3] + "table.html", "w")
    HTMLFile.write(htmlCode)
    HTMLFile.close()

    print '....................done'


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
            createHTML(file)


if __name__ == '__main__':
    try:
        import sys
        sys.exit(main())
    except LookupError as e:
        print e
    except KeyboardInterrupt:
        print 'script was interrupted by user'
        pass
