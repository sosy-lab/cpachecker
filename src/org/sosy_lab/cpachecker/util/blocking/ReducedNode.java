// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
