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
package org.sosy_lab.cpachecker.cfa.objectmodel;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class CFAExitNode extends CFANode{

    private String functionName;

    public CFAExitNode (int lineNumber, String functionName)
    {
        super (lineNumber);
        this.functionName = functionName;
    }

    @Override
    public String getFunctionName ()
    {
        return functionName;
    }

    @Override
    public void setFunctionName (String s)
    {
    	functionName = s;
    }

}
