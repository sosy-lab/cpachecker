/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2009  Dirk Beyer and Erkan Keremoglu.
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

import java.util.logging.Level;

import cmdline.CPAMain;
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
        // some additional checking is required for jump edges
        boolean deadEdge = false;
        if (this.isJumpEdge()) {
          // a null predecessor doesn't make sense
          assert (predecessor != null);
          int numLeavingEdges = predecessor.getNumLeavingEdges ();
          int numRemoved = 0;
          for (int idx = 0; idx < numLeavingEdges; ++idx)
          {
              CFAEdge edge = predecessor.getLeavingEdge(idx - numRemoved);
              // if the predecessor already has an edge leaving that is not
              // us, there must be dead code!
              if (edge != this) {
                if (edge.isJumpEdge()) {
                  // we're dead, there was a jump edge already
                  CPAMain.logManager.log(Level.INFO, "Dead code detected after line " + predecessor.getLineNumber() + ": " + this.getRawStatement());
                  deadEdge = true;
                } else {
                  // just remove the edge to temporarily dead code (we might also link it to some dummy node?)
                  // this may be a label or the like and be jumped to later on
                  predecessor.removeLeavingEdge(edge);
                  edge.getSuccessor().removeEnteringEdge(edge);
                  ++numRemoved;
                }
              }
          }
          // if there was a jump edge already (deadEdge == true) then no other
          // edge must have existed
          assert (!deadEdge || 1 == numLeavingEdges);
        } else if (predecessor != null && predecessor.hasJumpEdgeLeaving()) {
          // there is a jump edge already, no use adding this one
          // may happen if there is a return in a switch
          deadEdge = true;
        }
        
        if (!deadEdge) {
          setPredecessor (predecessor);
          setSuccessor (successor);
        }
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
            this.predecessor.addLeavingEdge (this);
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
