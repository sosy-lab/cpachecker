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
package cpa.defuse;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.defuse.DefUseDefinition;

public class DefUseDefinition implements AbstractElement
{
    private String variableName;
    private CFAEdge assigningEdge;

    public DefUseDefinition (String variableName, CFAEdge assigningEdge)
    {
    	//System.out.println("DefUseDefinition: " + variableName + " + "+ assigningEdge.getPredecessor().getNodeNumber());
        this.variableName = variableName;
        this.assigningEdge = assigningEdge;
    }

    public String getVariableName ()
    {
        return variableName;
    }

    public CFAEdge getAssigningEdge ()
    {
        return assigningEdge;
    }

    @Override
    public int hashCode ()
    {
        return variableName.hashCode ();
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof DefUseDefinition))
            return false;

        DefUseDefinition otherDef = (DefUseDefinition) other;
        if (!otherDef.variableName.equals (this.variableName))
            return false;

        if (this.assigningEdge == null && otherDef.assigningEdge == null)
            return true;

        if ((this.assigningEdge == null && otherDef.assigningEdge != null) ||
            (this.assigningEdge != null && otherDef.assigningEdge == null))
            return false;

        if ((otherDef.assigningEdge.getPredecessor ().getNodeNumber () != this.assigningEdge.getPredecessor ().getNodeNumber ()) ||
            (otherDef.assigningEdge.getSuccessor ().getNodeNumber () != this.assigningEdge.getSuccessor ().getNodeNumber ()))
            return false;

        return true;
    }
}
