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

import cfa.objectmodel.AbstractCFAEdge;
import cfa.objectmodel.CFAEdgeType;


public class BlankEdge extends AbstractCFAEdge
{
    private final boolean jumpEdge;

    public BlankEdge(String rawStatement, int lineNumber, CFANode predecessor, CFANode successor) {
      this(rawStatement, lineNumber, predecessor, successor, false);
    }

    public BlankEdge(String rawStatement, int lineNumber, CFANode predecessor, CFANode successor, boolean jumpEdge)
    {
        super(rawStatement, lineNumber, predecessor, successor);
        this.jumpEdge = jumpEdge;
    }

    /**
     * Gives information whether this edge is a jump as produced by a goto, 
     * continue and break statements.
     */
    @Override
    public boolean isJumpEdge ()
    {
        return jumpEdge;
    }

    public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.BlankEdge;
    }

}
