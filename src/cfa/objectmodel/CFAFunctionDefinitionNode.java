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
package cfa.objectmodel;

import cmdline.CPAMain;
import cfa.objectmodel.CFAExitNode;
import cfa.objectmodel.CFANode;


public abstract class CFAFunctionDefinitionNode extends CFANode
{
    private String functionName;
    private String containingFileLocation;
    // Check if call edges are added in the second pass
    private CFAExitNode exitNode;

    public CFAFunctionDefinitionNode (int lineNumber, String functionName, String containingFileLocation)
    {
        super (lineNumber);
        this.functionName = functionName;
        this.containingFileLocation = containingFileLocation;
    }

    @Override
    public String getFunctionName ()
    {
        return this.functionName;
    }

    @Override
    public void setFunctionName (String s)
    {
    	this.functionName = s;
    }

    public CFANode getExitNode ()
    {
        return this.exitNode;
    }

    public void setExitNode (CFAExitNode en)
    {
    	this.exitNode = en;
    }

    public String getContainingFileName(){
    	String filePath = containingFileLocation;
		String[] pathArray = filePath.split(CPAMain.cpaConfig.getProperty("analysis.programs"));
		String fileName = pathArray[1];
		fileName = fileName.replace("/", ".");
		// TODO we assume the file name ends with .c or .h
		fileName = fileName.substring(0, fileName.length()-2);
		return fileName;
    }

}
