/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
function CreateTableFromJSON() {
    var myData = [
        {
            "e": "Current:  StepChildren: [6, 7] StepParents: [4] StateID: 5",
            "e ~> e'": "Storage:  [Current:  StepChildren: [] StepParents: [5] StateID: 6, "
                       + "Current:  StepChildren: [] StepParents: [5] StateID: 7]",
            "e new = merge (e', e'')": "2-1",
            "stop": "3-1"
        },
        {
            "e": "Current:  StepChildren: [10] StepParents: [8, 11] StateID: 13",
            "e ~> e'": "Storage:  [Current:  StepChildren: [] StepParents: [13] StateID: 14",
            "e new = merge (e', e'')": "Storage:  [Current:  StepChildren: [10] StepParents: [8] StateID: 9,"
                                       + " Current:  StepChildren: [] StepParents: [11] StateID: 12"
                                       + ", Current:  StepChildren: [10] StepParents: [8, 11] StateID: 13"
                                       + "]",
            "stop": "3-2"
        },
        {
            "e": "3",
            "e ~> e'": "1-3",
            "e new = merge (e', e'')": "2-3",
            "stop": "3-3"
        }
    ]

    // EXTRACT VALUE FOR HTML HEADER.
    // ('Book ID', 'Book Name', 'Category' and 'Price')
    var col = [];
    for (var i = 0; i < myData.length; i++) {
        for (var key in myData[i]) {
            if (col.indexOf(key) === -1) {
                col.push(key);
            }
        }
    }

    // CREATE DYNAMIC TABLE.
    var table = document.createElement("table");

    // CREATE HTML TABLE HEADER ROW USING THE EXTRACTED HEADERS ABOVE.

    var row = table.insertRow(-1);                   // TABLE ROW.

    for (var i = 0; i < col.length; i++) {
        var th = document.createElement("th");      // TABLE HEADER.
        th.innerHTML = col[i];
        row.appendChild(th);
    }

    // ADD JSON DATA TO THE TABLE AS ROWS.
    for (var i = 0; i < myData.length; i++) {

        row = table.insertRow(-1);

        for (var j = 0; j < col.length; j++) {
            var tabCell = row.insertCell(-1);
            tabCell.innerHTML = myData[i][col[j]];
        }
    }

    // FINALLY ADD THE NEWLY CREATED TABLE WITH JSON DATA TO A CONTAINER.
    var divContainer = document.getElementById("showData");
    divContainer.innerHTML = "";
    divContainer.appendChild(table);
}