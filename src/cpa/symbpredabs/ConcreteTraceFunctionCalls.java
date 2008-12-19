/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.symbpredabs;

import java.util.Vector;


public class ConcreteTraceFunctionCalls implements ConcreteTrace {
    private Vector<String> functionNames;

    public ConcreteTraceFunctionCalls() {
        functionNames = new Vector<String>();
    }

    public void add(String fn) {
        if (functionNames.isEmpty() ||
                !functionNames.lastElement().equals(fn)) {
            functionNames.add(fn);
        }
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("\nSequence of function calls:\n" +
                   "---------------------------");
        for (String fn : functionNames) {
            buf.append("\n\t");
            buf.append(fn);
        }
        buf.append("\n");
        return buf.toString();
    }
}
