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

import com.google.common.base.Preconditions;

import cpa.common.CPAchecker;


public abstract class AbstractCFAEdge implements CFAEdge
{
    private final CFANode predecessor;
    private final CFANode successor;

    private final String rawStatement;

    public AbstractCFAEdge(String rawStatement, CFANode predecessor, CFANode successor) {
      Preconditions.checkNotNull(rawStatement);
      Preconditions.checkNotNull(predecessor);
      Preconditions.checkNotNull(successor);
      this.predecessor = predecessor;
      this.successor = successor;
      this.rawStatement = rawStatement;
    }

    /**
     * This method registers adds this edge to the leaving and entering edges
     * of its predecessor and successor respectively.
     */
    public void addToCFA()
    {
        CFANode predecessor = getPredecessor();
        // some additional checking is required for jump edges
        boolean deadEdge = false;
        if (this.isJumpEdge()) {
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
                  CPAchecker.logger.log(Level.INFO, "Dead code detected after line " + predecessor.getLineNumber() + ": " + this.getRawStatement());
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
        } else if (predecessor.hasJumpEdgeLeaving()) {
          // there is a jump edge already, no use adding this one
          // may happen if there is a return in a switch
          deadEdge = true;
        }
        
        if (!deadEdge) {
          predecessor.addLeavingEdge(this);
          getSuccessor().addEnteringEdge(this);
        }
    }

    public CFANode getPredecessor ()
    {
        return predecessor;
    }

    public CFANode getSuccessor ()
    {
        return successor;
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
    public int hashCode() {
      return 31 * predecessor.hashCode() + successor.hashCode();
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
