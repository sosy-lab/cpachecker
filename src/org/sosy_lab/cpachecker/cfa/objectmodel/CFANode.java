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

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;

public class CFANode implements Comparable<CFANode> {

  private static int          nextNodeNumber      = 0;

  private int                 nodeNumber;
  private final int           lineNumber;

  private final List<CFAEdge> leavingEdges        = new ArrayList<CFAEdge>();
  private final List<CFAEdge> enteringEdges       = new ArrayList<CFAEdge>();

  // is start node of a loop?
  private boolean             isLoopStart         = false;

  /** can rely-guarantee environmental edges be applied to this node? */
  //private boolean             envApplication            = true;

  /** does this node generate env transitions? */
  // TODO: move to an edge.
  //private boolean             generatesEnv            = true;

  // in which function is that node?
  private String        functionName;

  // list of summary edges
  private CallToReturnEdge    leavingSummaryEdge  = null;
  private CallToReturnEdge    enteringSummaryEdge = null;

  // topological sort id, smaller if it appears later in sorting
  private int                 topologicalSortId   = 0;

  public CFANode(int lineNumber, String functionName) {
    this.lineNumber = lineNumber;
    this.functionName = functionName;
    assert !functionName.isEmpty();
    this.nodeNumber = nextNodeNumber++;
  }


  public int getLineNumber() {
    return lineNumber;
  }

  public int getNodeNumber() {
    return nodeNumber;
  }

  public int getTopologicalSortId() {
    return topologicalSortId;
  }

  public void setTopologicalSortId(int i) {
    topologicalSortId = i;
  }

  public void addLeavingEdge(CFAEdge newLeavingEdge) {
    leavingEdges.add(newLeavingEdge);
  }

  public boolean removeLeavingEdge(CFAEdge edge) {
    return leavingEdges.remove(edge);
  }

  public int getNumLeavingEdges() {
    return leavingEdges.size();
  }

  public CFAEdge getLeavingEdge(int index) {
    return leavingEdges.get(index);
  }

  public void addEnteringEdge(CFAEdge enteringEdge) {
    enteringEdges.add(enteringEdge);
  }

  public boolean removeEnteringEdge(CFAEdge edge) {
    return enteringEdges.remove(edge);
  }

  public int getNumEnteringEdges() {
    return enteringEdges.size();
  }

  public CFAEdge getEnteringEdge(int index) {
    return enteringEdges.get(index);
  }

  public CFAEdge getEdgeTo(CFANode other) {
    for (CFAEdge edge : leavingEdges) {
      if (edge.getSuccessor() == other) {
        return edge;
      }
    }
    throw new IllegalArgumentException();
  }

  public boolean hasEdgeTo(CFANode other) {
    boolean hasEdge = false;
    for (CFAEdge edge : leavingEdges) {
      if (edge.getSuccessor() == other) {
        hasEdge = true;
        break;
      }
    }

    return hasEdge;
  }

  public boolean hasJumpEdgeLeaving() {
    for (CFAEdge edge : leavingEdges) {
      if (edge.isJumpEdge()) return true;
    }
    return false;
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

  public void addEnteringSummaryEdge(CallToReturnEdge edge) {
    enteringSummaryEdge = edge;
  }

  public void addLeavingSummaryEdge(CallToReturnEdge edge) {
    leavingSummaryEdge = edge;
  }

  public CallToReturnEdge getEnteringSummaryEdge() {
    return enteringSummaryEdge;
  }

  public CallToReturnEdge getLeavingSummaryEdge() {
    return leavingSummaryEdge;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) { return true; }
    if (other == null || !(other instanceof CFANode)) { return false; }
    return getNodeNumber() == ((CFANode) other).getNodeNumber();
  }

  @Override
  public int hashCode() {
    return getNodeNumber();
  }

  @Override
  public String toString() {
    return "N" + getNodeNumber();
  }

  @Override
  public int compareTo(CFANode o) {
    return getNodeNumber() - o.getNodeNumber();
  }

  public void setNodeNumber(int pNodeNumber) {
    nodeNumber = pNodeNumber;
  }


  public void setFunctionName(String pFunctionName) {
    functionName = pFunctionName;
  }






}
