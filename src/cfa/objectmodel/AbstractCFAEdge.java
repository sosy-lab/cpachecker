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
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import exceptions.CFAGenerationRuntimeException;


public abstract class AbstractCFAEdge implements CFAEdge
{
    protected CFANode predecessor;
    protected CFANode successor;

    private String rawStatement;

    public AbstractCFAEdge (String rawStatement)
    {
        this.predecessor = null;
        this.successor = null;

        this.rawStatement = rawStatement;
    }

    public void initialize (CFANode predecessor, CFANode successor)
    {
        setPredecessor (predecessor);
        setSuccessor (successor);
    }

    public CFANode getPredecessor ()
    {
        return predecessor;
    }

    public void setPredecessor (CFANode predecessor) throws CFAGenerationRuntimeException
    {
        if (this.predecessor != null)
            this.predecessor.removeLeavingEdge (this);

        this.predecessor = predecessor;
        if (this.predecessor != null)
        {
            if (this.isJumpEdge ())
            {
                if (predecessor.hasJumpEdgeLeaving ())
                {
                    System.out.println ("Warning: Should not have multiple jump edges leaving at line: " + predecessor.getLineNumber ());
                    return;
                }

                // This edge is to be the only edge leaving the predecessor
                int numLeavingEdges = predecessor.getNumLeavingEdges ();
                for (int idx = numLeavingEdges - 1; idx >= 0; idx--)
                {
                    CFAEdge removedEdge = predecessor.getLeavingEdge (idx);
                    CFANode nullNode = new CFANode (predecessor.getLineNumber ());
                    removedEdge.setPredecessor (nullNode);
                }
            }

            if (predecessor.hasJumpEdgeLeaving ())
            {
                // TODO: Do nothing? Or add null node?
                CFANode nullNode = new CFANode (predecessor.getLineNumber ());
                this.predecessor = nullNode;
            }

            this.predecessor.addLeavingEdge (this);
        }
    }

    public CFANode getSuccessor ()
    {
        return successor;
    }

    public void setSuccessor (CFANode successor) throws CFAGenerationRuntimeException
    {
        if (this.successor != null)
            this.successor.removeEnteringEdge (this);

        this.successor = successor;
        if (this.successor != null)
            this.successor.addEnteringEdge (this);
    }

    public String getRawStatement ()
    {
        return rawStatement;
    }

    public boolean isJumpEdge ()
    {
        return false;
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof AbstractCFAEdge))
            return false;

        AbstractCFAEdge otherEdge = (AbstractCFAEdge) other;

        if ((otherEdge.predecessor != this.predecessor) ||
            (otherEdge.successor != this.successor))
            return false;

        return true;
    }

    @Override
    public String toString() {
        return "(" + getPredecessor() + " -{" +
                getRawStatement().replaceAll("\n", " ") +
               "}-> " + getSuccessor() + ")";
    }
}
