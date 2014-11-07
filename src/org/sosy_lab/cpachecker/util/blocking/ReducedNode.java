/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.blocking;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

class ReducedNode {
  private static int uniqueNodeIdSequence = 0;

  private final CFANode wrappedNode;
  private final int uniqueNodeId;
  private int summarizations;
  private int functionCallId;
  private final boolean isLoopHead;

  public ReducedNode(CFANode pWrappedNode) {
    this(pWrappedNode, false);
  }

  public ReducedNode(CFANode pWrappedNode, boolean pIsLoopHead) {
    this.wrappedNode = pWrappedNode;
    this.uniqueNodeId = ReducedNode.uniqueNodeIdSequence++;
    this.summarizations = 0;
    this.functionCallId = 0;
    this.isLoopHead = pIsLoopHead;
  }

  public CFANode getWrapped() {
    return this.wrappedNode;
  }

  public int getUniqueNodeId() {
    return this.uniqueNodeId;
  }

  public int getSummarizations() {
    return this.summarizations;
  }

  public void incSummarizations(int pIncBy) {
    this.summarizations += pIncBy;
  }

  public boolean isFunctionEntry() {
    return getWrapped() instanceof FunctionEntryNode;
  }

  public boolean isFunctionExit() {
    return getWrapped() instanceof FunctionExitNode;
  }

  public boolean isLoopHead() {
    return isLoopHead;
  }

  public String getNodeKindText() {
    if (isLoopHead()) {
      return "LoopHead";
    } else if (isFunctionEntry()) {
      return "FunctEntry";
    } else if (isFunctionExit()) {
      return "FunctExit";
    } else {
      return "Generic";
    }
  }

  public void setFunctionCallId(int pCallId) {
    this.functionCallId = pCallId;
  }

  public int getFunctionCallId() {
    return this.functionCallId;
  }
}
