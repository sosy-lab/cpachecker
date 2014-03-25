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
package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.List;

import com.google.common.primitives.Ints;

public class CFANode implements Comparable<CFANode> {

  private static int nextNodeNumber = 0;

  private final int nodeNumber;

  private final List<CFAEdge> leavingEdges = new ArrayList<>(1);
  private final List<CFAEdge> enteringEdges = new ArrayList<>(1);

  // is start node of a loop?
  private boolean isLoopStart = false;

  // in which function is that node?
  private final String functionName;

  // list of summary edges
  private FunctionSummaryEdge leavingSummaryEdge = null;
  private FunctionSummaryEdge enteringSummaryEdge = null;

  // reverse postorder sort id, smaller if it appears later in sorting
  private int reversePostorderId = 0;

  public CFANode(String pFunctionName) {
    assert !pFunctionName.isEmpty();

    functionName = pFunctionName;
    nodeNumber = nextNodeNumber++;
  }

  public int getNodeNumber() {
    return nodeNumber;
  }

  public int getReversePostorderId() {
    return reversePostorderId;
  }

  public void setReversePostorderId(int pId) {
    reversePostorderId = pId;
  }

  public void addLeavingEdge(CFAEdge pNewLeavingEdge) {
    checkArgument(pNewLeavingEdge.getPredecessor() == this,
        "Cannot add edge \"%s\" to node %s as leaving edge", pNewLeavingEdge, this);
    leavingEdges.add(pNewLeavingEdge);
  }

  public void removeLeavingEdge(CFAEdge pEdge) {
    boolean removed = leavingEdges.remove(pEdge);
    checkArgument(removed,
        "Cannot remove non-existing leaving edge \"%s\" from node %s", pEdge, this);
  }

  public int getNumLeavingEdges() {
    return leavingEdges.size();
  }

  public CFAEdge getLeavingEdge(int pIndex) {
    return leavingEdges.get(pIndex);
  }

  public void addEnteringEdge(CFAEdge pEnteringEdge) {
    checkArgument(pEnteringEdge.getSuccessor() == this,
        "Cannot add edge \"%s\" to node %s as entering edge", pEnteringEdge, this);
    enteringEdges.add(pEnteringEdge);
  }

  public void removeEnteringEdge(CFAEdge pEdge) {
    boolean removed = enteringEdges.remove(pEdge);
    checkArgument(removed,
        "Cannot remove non-existing entering edge \"%s\" from node %s", pEdge, this);
  }

  public int getNumEnteringEdges() {
    return enteringEdges.size();
  }

  public CFAEdge getEnteringEdge(int pIndex) {
    return enteringEdges.get(pIndex);
  }

  public CFAEdge getEdgeTo(CFANode pOther) {
    for (CFAEdge edge : leavingEdges) {
      if (edge.getSuccessor() == pOther) {
        return edge;
      }
    }

    throw new IllegalArgumentException();
  }

  public boolean hasEdgeTo(CFANode pOther) {
    boolean hasEdge = false;
    for (CFAEdge edge : leavingEdges) {
      if (edge.getSuccessor() == pOther) {
        hasEdge = true;
        break;
      }
    }

    return hasEdge;
  }

  public void setLoopStart() {
    isLoopStart = true;
  }

  public boolean isLoopStart() {
    return isLoopStart;
  }

  public String getFunctionName() {
    return functionName;
  }

  public void addEnteringSummaryEdge(FunctionSummaryEdge pEdge) {
    checkState(enteringSummaryEdge == null,
        "Cannot add two entering summary edges to node %s", this);
    enteringSummaryEdge = pEdge;
  }

  public void addLeavingSummaryEdge(FunctionSummaryEdge pEdge) {
    checkState(leavingSummaryEdge == null,
        "Cannot add two leaving summary edges to node %s", this);
    leavingSummaryEdge = pEdge;
  }

  public FunctionSummaryEdge getEnteringSummaryEdge() {
    return enteringSummaryEdge;
  }

  public FunctionSummaryEdge getLeavingSummaryEdge() {
    return leavingSummaryEdge;
  }

  public void removeEnteringSummaryEdge(FunctionSummaryEdge pEdge) {
    checkArgument(enteringSummaryEdge == pEdge,
        "Cannot remove non-existing entering summary edge \"%s\" from node \"%s\"", pEdge, this);
    enteringSummaryEdge = null;
  }

  public void removeLeavingSummaryEdge(FunctionSummaryEdge pEdge) {
    checkArgument(leavingSummaryEdge == pEdge,
        "Cannot remove non-existing leaving summary edge \"%s\" from node \"%s\"", pEdge, this);
    leavingSummaryEdge = null;
  }

  @Override
  public String toString() {
    return "N" + nodeNumber;
  }

  @Override
  public int compareTo(CFANode pOther) {
    return Ints.compare(this.nodeNumber, pOther.nodeNumber);
  }
}
