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
package org.sosy_lab.pcc.common;

import java.util.ArrayList;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class ARTNode {

  private int id;
  private CFANode cfaNode;
  private AbstractionType isAbstraction;
  private String abstraction;
  private ArrayList<ARTEdge> edges = new ArrayList<ARTEdge>();

  public ARTNode(int pID, CFANode pCFANode, AbstractionType pAbstractionType)
      throws IllegalArgumentException {
    if (pCFANode == null || pAbstractionType == AbstractionType.Abstraction) { throw new IllegalArgumentException(
        "Abstraction missing."); }
    id = pID;
    cfaNode = pCFANode;
    isAbstraction = pAbstractionType;
  }

  public ARTNode(int pID, CFANode pCFANode, AbstractionType pAbstractionType,
      String pAbstraction) throws IllegalArgumentException {
    if (pCFANode == null || abstraction == null || abstraction.length() == 0
        || pAbstractionType != AbstractionType.Abstraction) { throw new IllegalArgumentException(
        "Abstraction missing."); }
    id = pID;
    cfaNode = pCFANode;
    isAbstraction = pAbstractionType;
    abstraction = pAbstraction;
  }

  public void addEdge(ARTEdge pEdge) throws IllegalArgumentException {
    // check if it is a valid edge
    if (cfaNode.getEdgeTo(pEdge.getCorrespondingCFAEdge().getSuccessor()) != pEdge
        .getCorrespondingCFAEdge()) { throw new IllegalArgumentException(
        "Edge cannot be an edge from this ARTNode.\n"); }
    edges.add(pEdge);
  }

  public int getID() {
    return id;
  }

  public CFANode getCorrespondingCFANode() {
    return cfaNode;
  }

  public int getNumberOfEdges() {
    return edges.size();
  }

  public ARTEdge[] getEdges() {
    return (ARTEdge[]) edges.toArray();
  }

  public AbstractionType getAbstractionType() {
    return isAbstraction;
  }

  public String getAbstraction() {
    if (isAbstraction == AbstractionType.Abstraction) { return abstraction; }
    return null;
  }

  @Override
  public String toString() {
    if (isAbstraction == AbstractionType.Abstraction) {
      return "ART node " + id + " corresponding to CFA node " + cfaNode.getNodeNumber() + " Abstraction: "
          + abstraction;
    } else {
      return "ART node " + id + " corresponding to CFA node " + cfaNode.getNodeNumber();
    }
  }

}
