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
    #columnTitles td:first-child { font-family: monospace; font-size: x-small; }
    tbody tr:first-child td { border-top:3px solid black}
    tfoot tr:first-child td { border-top:3px solid black}
    .correctSafe, .correctUnsafe { text-align:center; color:green}
    .wrongSafe, .wrongUnsafe { text-align:center; color:red; font-weight: bold; }
    .unknown { text-align:center; color:orange; font-weight: bold; }
    .error { text-align:center; color:magenta; font-weight: bold; }
    .score { text-align:center; font-size:large; font-weight:bold; }
    a { color: inherit; text-decoration: none; display: block; } 
    a:hover, .clickable:hover { background: lime; cursor: pointer; }
    -->
</style>
'''

# TODO: copy external scripts to local repository? working offline?

SCRIPT_INCLUDES = '''
<script type="text/javascript" src="http://code.jquery.com/jquery-1.7.1.min.js"></script>

<script type="text/javascript">
function debug(logInfo) {
  if(!true) {
    console.log(logInfo);
  }
}
</script>
'''


CONTENT_PANE = '''
<script type="text/javascript">
function showContentPane() {
    // add function for cleanup, first unbind any old function
    $('#contentPaneBackground').unbind().click(hideContentPane).show();
    $('#contentPane').show();
}

function hideContentPane() {
    $('#contentPaneBackground').hide();
    $('#contentPane').hide().empty();
}
</script>

<style type="text/css">
    #contentPaneBackground { height:5000px; width:5000px;
             position:fixed; top:0px; left:0px;
             background-image: url(http://www.house-events.de/schnee.gif);
             background-color:grey; 
             opacity:0.5; display:none }
    #contentPane { height:90%; width:90%; position:fixed; top:5%; left:5%;
             border:solid 10px black; border-radius:15px;
             background-color:white; opacity:1; display:none }
</style>

<div id="contentPaneBackground"></div>
<div id="contentPane"></div>
'''


FILE_CONTENT_SCRIPT = '''
<script type="text/javascript">
function loadContentWrapper(event) {
    var url = $(event.target).attr("href");
    loadContent(url);
    return false;
}

function loadContent(url) {
    var contentPane = $("<pre>").appendTo("#contentPane")
            .css("width", "100%").css("height", "100%")
            .css("margin", "0").css("overflow", "auto");

    $.ajax({
        async: false, // wait for isError
        url: url,
        cache: false,
        dataType: "text",
        beforeSend: function() {
            showContentPane();
            contentPane.html("loading...");
        },
        success: function(text){
            newtext = text.replace(/&/g, "&amp;")
                          .replace(/"/g, "&quot;")
                          .replace(/</g, "&lt;")
                          .replace(/>/g, "&gt;")
                          .replace(/\\n/g, "<br>");
            contentPane.html(newtext);
        },
        error: function() {
            contentPane.html("error while loading content.<br>" +
            "this could be a problem of the 'same-origin-policy' of your browser.<br><br>" + 
            "only firefox seems to be able to access files from local directories<br>" + 
            "and this works only if the file is in the same directory as this website.<br><br>" + 
            "you can try to download the file: <a href=" + url + ">" + url + "</a>");
        },
    });
}

$(document).ready(function(){
    var cellsWithUrls = $('a');
    //console.log(cellsWithUrls);
    cellsWithUrls.each(
        function(index, elem){
            $(elem).click(loadContentWrapper);
        });
});
</script>
'''


COLUMN_TOGGLE_SCRIPT = '''
<script>
function incColspan(col) {
    var span = parseInt(col.attr("colspan"));
    if (span == 0) { col.show(); }
    col.attr("colspan", span + 1);
}

function decColspan(col) {
    var span = parseInt(col.attr("colspan"));
    col.attr("colspan", span - 1);
    if (span == 1) { col.hide(); }
}

// this function shows or hides a column and enlarges or shrinkes the header-columns
// @param numlist: each num is the index of a column in header, 
//                 these columns are above the column to toggle
function toggleColumn (button, numList) {
    var len = numList.length-1; // last element in numlist is used for columntitles
    for (var i=0; i<len; i++) {
        var cell = $("#dataTable > thead > tr:eq(" + i + ") > td:eq(" + numList[i] + ")");
        button.checked ? incColspan(cell) : decColspan(cell);
    }

    var children = $("tbody > tr > td:nth-child(" + numList[len] + "), " +
                     "tfoot > tr > td:nth-child(" + numList[len] + "), " +
                     "#columnTitles > td:nth-child(" + numList[len] + ")");
    button.checked ? children.show() : children.hide();
}

function expandColSpanToNums(row) {
    var list = [];
    for (var i=0; i<row.cells.length; i++) {
      for (var j=0; j<parseInt(row.cells[i].colSpan); j++) {
        list.push(i);
      }
    }
    return list;
}


// we use a cache, because it is difficult to calculate the 
// colspan of headerrows with some hidden columns
var buttonListCache = null;


function createColumnToggleButtons() {
    if (buttonListCache == null) { // if not cached, for details see 5 lines above

        var tableHead = $("#dataTable > thead")[0];
        var listOfHeaderNums = []
        var numOfHeaderRows = 6;
        for (var i=0; i<numOfHeaderRows; i++) {
            listOfHeaderNums.push(expandColSpanToNums(tableHead.rows[i]));
        }
    
        var columnTitles = $("#columnTitles > td");
        var testNums = listOfHeaderNums[3];
        var testNum = testNums[1];
        var testName = tableHead.children[3].cells[testNum].textContent + " " + 
                       tableHead.children[4].cells[testNum].textContent;
    
        // TODO can we make next block more OO-like?
        buttonList = '<ul>' + testName;
        for (var i=1; i<columnTitles.length; i++) { // do not use first column (i!=0)
            if (testNum != testNums[i]) {
                testNum = testNums[i];
                testName = tableHead.children[3].cells[testNum].textContent + " " + 
                           tableHead.children[4].cells[testNum].textContent;
                buttonList += '</ul><ul>' + testName;
            }
    
            var toggleFunction = 'onclick="toggleColumn(this,[';
            for (var j=0; j<numOfHeaderRows; j++) {
                toggleFunction += listOfHeaderNums[j][i] + ',';
            }
            toggleFunction += (i+1) +'])"';
    
            var column = columnTitles[i];
            var button = '<input type="checkbox" id="check' + i + '" ' + toggleFunction + ' checked>';
            var label = '<label for="check' + i + '">' + column.textContent + '</label>';
            buttonList += '<li>' + button + label + '</li>';
        }
        buttonList += '</ul>';
        buttonListCache = $(buttonList);
    }

    $('<form id="toggleButtons" onsubmit="return false"></form>')
        .append(buttonListCache)
        .appendTo($("#contentPane"));
    showContentPane();
}

$(document).ready(function(){
    $('#columnTitles > td:first-child')
        .addClass("clickable")
        .click(function (event) { 
            return createColumnToggleButtons(); });
});
</script>

<style type="text/css">
    #toggleButtons ul { font-family:arial, sans serif; }
    #toggleButtons ul li { display: inline; }
    #toggleButtons ul li:hover { background-color:yellow; }
</style>
'''


PLOTTING_SCRIPT = '''
<script type="text/javascript" src="http://code.jquery.com/jquery-1.7.1.min.js"></script>
<script type="text/javascript" src="http://www.jqplot.com/src/jquery.jqplot.min.js"></script>
<script type="text/javascript" src="http://www.jqplot.com/src/plugins/jqplot.highlighter.min.js"></script>
<script type="text/javascript" src="http://www.jqplot.com/src/plugins/jqplot.cursor.min.js"></script>
<script type="text/javascript" src="http://www.jqplot.com/src/plugins/jqplot.canvasTextRenderer.min.js"></script>
<script type="text/javascript" src="http://www.jqplot.com/src/plugins/jqplot.canvasAxisTickRenderer.min.js"></script>
<script type="text/javascript" src="http://www.jqplot.com/src/plugins/jqplot.enhancedLegendRenderer.min.js"></script>

<style type="text/css">
    .jqplot-title {font-family:arial, sans serif; font-size:large }
    .jqplot-table-legend-swatch {width:20px; height:15px }
    .jqplot-table-legend { border-style:none; outline:none }
    .jqplot-table-legend tbody { border-style:none }
    .jqplot-table-legend tbody tr td { border-top:none; cursor:pointer }
    .jqplot-highlighter-tooltip {font-family:arial, sans serif; font-size:large;
             border:solid 1px black; padding:2px;
             border-radius:8px; border-bottom-left-radius:0px;
             background-color:white; opacity:0.8; }
    #chart { height:100%; width:100% }
    #button-trend { position:absolute; bottom:0px; }
</style>

<script type="text/javascript">

// this function collects the indices of columns with title "header"
function getColumnIndicesForHeader(header) {
    var columnIndizes = [];
    var cells = document.getElementById('columnTitles').cells;

    for(i = 0; i < cells.length; i++) {
      var currentHeader = cells[i].textContent;
      if (currentHeader == header) {
        columnIndizes.push(i);
      }
    }

    return columnIndizes;
};

// getTableData returns a list of arrays, 
// each array is of the form: [[file1, value1], [file2, value1], ...]
function getTableData(header) {
    debug("data for: " + header);
    var data = [];

    var indices = getColumnIndicesForHeader(header);
    for (j = 0; j < indices.length; j++) {
      data.push([]);
    }

    var tableBody = $('#dataTable > tbody')[0];

    for(i = 0; i < tableBody.rows.length; i++) {
      var currentRow = tableBody.rows[i];

      for (j = 0; j < indices.length; j++) {
        var index = indices[j];
        var currentCell = currentRow.cells[index];

        var value;
        if (header === 'status') {
            if (currentCell.className.indexOf('correct') == 0)     value = 1;
            else if (currentCell.className.indexOf('wrong') == 0)  value = 0;
            else                                                  value = -1;
        } else {
          value = parseFloat(currentCell.textContent)
        }
        data[j].push([i, value]);
      }
    }

    debug(data);
    return function inner(){ return data;};
};


// this method returns sorted data for showTrend().
function sortData(data) {
    var newData = [];
    for (i = 0; i < data.length; i++) {
        var line = data[i];
        var array = [];

        for (j = 0; j < line.length; j++) {
            if (line[j].length != 2) {debug("ERROR: data is invalid!");}
            array.push(line[j][1]);
        }

        array.sort( function(a, b) { return a - b;} ); // compare numbers!

        var newLine = [];
        for (j = 0; j < line.length; j++) {
            newLine.push([j, array[j]]);
        }

        newData.push(newLine);
    }
    return function inner(){ return newData;};
}

// get labels for x-direction
function getXTicks(){
    var xTicks = [];
    var maxLength = 40;
    var tableBody = $('#dataTable > tbody')[0];
    for(i = 0; i < tableBody.rows.length; i++) {
      var name = tableBody.rows[i].cells[0].textContent;
      if (name.length > maxLength) { name = name.substring(0, maxLength) + "..."; }
      xTicks.push([i, name]);
    }
    return xTicks;
}

// get labels for x-direction as [[0," 0"],[1," "],...] with a number in each 5th element
function getXTicksWithNumbers(){
    var xTicks = [];
    var maxLength = 40;
    var tableBody = $('#dataTable > tbody')[0];
    for(i = 0; i < tableBody.rows.length; i++) {
      xTicks.push([i, ((i%5)?" ":" " + i)]);
    }
    return xTicks;
}

// get labels for y-direction
function getYTicks(header) {
    if (header == "status") {
      return [[-1.5, " "], [-1, "wrong"], [0, "unknown"], [1, "correct"], [1.5, " "]];
    } else {
      return [];
    }
}

// returns a list of cells, each cell is multiplied by value of its colspan.
function expandColSpan(row) {
    var list = [];
    for (i=0; i<row.cells.length; i++) {
      var cell = row.cells[i];
      for (j=0; j<parseInt(cell.colSpan); j++) {
        list.push(cell);
      }
    }
    return list;
}


// returns label of a test: 'tool+test+date'.
function getLabels(header) {
    debug("labels for: " + header);
    var labels = [];

    var indices = getColumnIndicesForHeader(header);
    var tableHead = $('#dataTable > thead')[0];
    var toolRow = expandColSpan(tableHead.rows[0]);
    var dateRow = expandColSpan(tableHead.rows[3]);
    var testRow = expandColSpan(tableHead.rows[4]);

    // assertion
    if ((toolRow.length != dateRow.length) || 
        (toolRow.length != testRow.length)) {
        debug("ERROR: number of columns is invalid!");
    }

    for (i = 0; i < indices.length; i++) {
        var index = indices[i];
        labels.push(toolRow[index].textContent + " " +
                    testRow[index].textContent + " " +
                    dateRow[index].textContent);
    }

    debug(labels);
    return labels;
};


function addLegendActions() {
    var legendButtons = $('tr.jqplot-table-legend');
    var seriesLines = $('canvas.jqplot-series-canvas');

    // assertion
    if (legendButtons.length != seriesLines.length) {
        debug("ERROR: number of series does not match buttons!");
    }

    for (i = 0; i<legendButtons.length; i++) {
      var currentButton = legendButtons[i];
      var currentLine = seriesLines[i];

      currentButton.onclick = function(event) {
        var hideOpacity = 0.3;
        if (this.style.opacity == hideOpacity) {
            this.style.opacity = 1;
        } else {
            this.style.opacity = hideOpacity;
        }
      }

      currentButton.onmouseover = function(line) {
        return function(event){ line.style.zIndex = 5; }
      }(currentLine);

      currentButton.onmouseout = function(line) {
        return function(event){ line.style.zIndex = 0; }
      }(currentLine);
    }
}


function showPlot(header) {
    debug("show plot of: " + header);
    $('#contentPaneBackground').trigger('click');

    var yTicks = getYTicks(header);
    var xTicks = getXTicks(); // filenames for labels
    var data = getTableData(header);

    drawPlot(header, data, xTicks, yTicks, "plot");

    var button = $('#button-trend')[0];
    button.onclick = function() { showTrend(header); };
    button.textContent = 'Show Trend';
};


function showTrend(header) {
    debug("show trend of: " + header);
    $('#contentPaneBackground').trigger('click');

    var yTicks = getYTicks(header);
    var xTicks = getXTicksWithNumbers();
    var data = sortData(getTableData(header)());

    drawPlot(header, data, xTicks, yTicks, "trend");

    var button = $('#button-trend')[0];
    button.onclick = function() { showPlot(header); };
    button.textContent = 'Show Plot';
};


function getFormatter(labels, header) {
    return function(str, seriesIndex, pointIndex){
        debug(str, seriesIndex, pointIndex);
        var filename = labels[pointIndex][1];
        if (header == "status") {
            if (str == 1)       str = "correct";
            else if (str == 0)  str = "unknown";
            else                str = "wrong";
        }
        if (filename.indexOf(" ") == 0) { // for showTrend(), all labels start with space.
            filename = "";
        } else {
            filename = filename + "<br>";
        }
        return filename + str;
    };
}

function drawPlot(header, data, xTicks, yTicks, type) {
    $('#contentPane').append('<div id="chart"></div>',
                   '<button id="button-trend"></button>');

    showContentPane();
    $('#contentPaneBackground').click(function(event){
        $('#chart').empty();
    });

        // data array is empty, we use "columnRenderer" option to get data.
        var plot = $.jqplot('chart',[],{
          title: header,
          legend: {
            show:true,
            placement: 'outsideGrid',
            renderer: $.jqplot.EnhancedLegendRenderer,
            labels: getLabels(header),
            location: 's',
            rowSpacing: "0px",
            showSwatches: true,
          },
          dataRenderer: data,
          highlighter:{
            show: true,
            sizeAdjust: 10,
            showMarker: true,
            tooltipAxes: 'y',
            tooltipLocation: 'ne',
            tooltipContentEditor: getFormatter(xTicks, header),
          },
          seriesDefaults:{
            shadow: false,
          },
          cursor:{
            show: false,
            zoom: false,
            showTooltip: false,
          },
          axes:{
            xaxis:{
              ticks: xTicks,
              tickRenderer: $.jqplot.CanvasAxisTickRenderer,
              tickOptions: {
                fontSize: '9px',
                angle: -60,
              }
            },
            yaxis:{
              ticks: yTicks,
              pad: 1.2,
              tickOptions:{
                formatString:'%.2f'
              }
            }
          },
        });

    addLegendActions();
};


// this function adds the listeners to the table
$(document).ready(function(){
    var columnTitles = $('#columnTitles > td');
    for (i = 1; i< columnTitles.length; i++) { // do not use first column (i!=0)
      var column = columnTitles[i];
      debug(column);
      column.style.cursor = "pointer";
      column.onclick = function (event) {
          var header = event.target.textContent;
          return showPlot(header);
      }
    }
});

</script>
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

    rowsForHTML = []
    rowsForCSV = []
    fileList = listOfTests[0][0].findall('sourcefile')

    # get filenames
    fileNames = [file.get("name") for file in fileList]

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
                        .format(len(fileList), maxScore)]]


    # get common folder
    commonPrefix = os.path.commonprefix(fileNames) # maybe with parts of filename
    commonPrefix = commonPrefix[: commonPrefix.rfind('/') + 1] # only foldername

    # generate text for filenames
    for fileName in fileNames:
        filePath = getPathOfSourceFile(fileName)
        rowsForHTML.append(['<td><a href="{0}">{1}</a></td>'.
                            format(quote(filePath), fileName.replace(commonPrefix, '', 1))])
        rowsForCSV.append([fileName.replace(commonPrefix, '', 1)])

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
        for row, values in zip(rowsForHTML, valuesListHTML): row.extend(values)
        for row, values in zip(rowsForCSV, valuesListCSV): row.extend(values)

        # get statistics
        stats = getStatsOfTest(testResult.findall('sourcefile'), columns, valuesListCSV)
        for row, values in zip(rowsForStats, stats): row.extend(values)

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


def getStatsOfTest(fileResult, columns, valuesList):

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
        HTMLOutFileName = OUTPUT_PATH + os.path.basename(file)[:-3] + "table.html"
        CSVOutFileName = OUTPUT_PATH + os.path.basename(file)[:-3] + "table.csv"
    else:
        listOfTests = getListOfTests(file)
        timestamp = time.strftime("%y%m%d-%H%M", time.localtime())
        HTMLOutFileName = OUTPUT_PATH + NAME_START + "." + timestamp + ".table.html"
        CSVOutFileName = OUTPUT_PATH + NAME_START + "." + timestamp + ".table.csv"

    if len(listOfTests) == 0:
        print ('\nError! No file with testresults found.\n' \
            + 'Please check the filenames in your XML-file.')
        exit()

    print ('generating html into %s ...' % (HTMLOutFileName, ))

    (tableHeadHTML, tableHeadCSV) = getTableHead(listOfTests)
    (tableBodyHTML, tableFootHTML, tableBodyCSV) = getTableBody(listOfTests)

    tableCode = '<table id="dataTable">\n' \
                + tableHeadHTML.replace('\n','\n' + HTML_SHIFT) \
                + '\n' + HTML_SHIFT \
                + tableFootHTML.replace('\n','\n' + HTML_SHIFT) \
                + '\n' + HTML_SHIFT \
                + tableBodyHTML.replace('\n','\n' + HTML_SHIFT) \
                + '\n</table>\n\n'

    htmlCode = DOCTYPE + '<html>\n\n<head>\n' + \
                CSS + SCRIPT_INCLUDES + FILE_CONTENT_SCRIPT + COLUMN_TOGGLE_SCRIPT + TITLE + \
                '\n</head>\n\n<body>\n\n' + CONTENT_PANE + '\n\n'
    if options.enablePlotting: htmlCode += PLOTTING_SCRIPT + '\n'
    htmlCode += tableCode + '</body>\n\n</html>'

    if not os.path.isdir(OUTPUT_PATH): os.makedirs(OUTPUT_PATH)
    HTMLFile = open(HTMLOutFileName, "w")
    HTMLFile.write(htmlCode)
    HTMLFile.close()

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
    parser.add_option("-p", "--plot", 
        action="store_true", dest="enablePlotting", default=False,
        help="put JavaScript in html-code that enables plotting functionality in the resulting table."
    )

    global options
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
