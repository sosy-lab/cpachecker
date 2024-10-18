/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.objectmodel;

import org.sosy_lab.cpachecker.cfa.ast.IASTNode;

import com.google.common.base.Preconditions;


public abstract class AbstractCFAEdge implements CFAEdge
{
    private final CFANode predecessor;
    private final CFANode successor;

    private final String rawStatement;

    private final int lineNumber;


    private boolean localRead;
    private boolean globalRead;
    private boolean localWrite;
    private boolean globalWrite;


    @Override
    public boolean isLocalRead() {
      return localRead;
    }

    public void setLocalRead(boolean pLocalRead) {
      localRead = pLocalRead;
    }

    @Override
    public boolean isGlobalRead() {
      return globalRead;
    }

    public void setGlobalRead(boolean pGlobalRead) {
      globalRead = pGlobalRead;
    }

    @Override
    public boolean isLocalWrite() {
      return localWrite;
    }

    public void setLocalWrite(boolean pLocalWrite) {
      localWrite = pLocalWrite;
    }

    @Override
    public boolean isGlobalWrite() {
      return globalWrite;
    }

    public void setGlobalWrite(boolean pGlobalWrite) {
      globalWrite = pGlobalWrite;
    }

    public AbstractCFAEdge(String rawStatement, int lineNumber, CFANode predecessor, CFANode successor) {
      Preconditions.checkNotNull(rawStatement);
      Preconditions.checkNotNull(predecessor);
      Preconditions.checkNotNull(successor);
      this.predecessor = predecessor;
      this.successor = successor;
      this.rawStatement = rawStatement;
      this.lineNumber = lineNumber;
    }

    @Override
    public CFANode getPredecessor ()
    {
        return predecessor;
    }

    @Override
    public CFANode getSuccessor ()
    {
        return successor;
    }

    @Override
    public String getRawStatement ()
    {
        return rawStatement;
    }

    @Override
    public IASTNode getRawAST() {
      return null;
    }

    @Override
    public boolean isJumpEdge ()
    {
        return false;
    }

    @Override
    public int getLineNumber() {
      return lineNumber;
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
