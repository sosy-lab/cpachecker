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
  private boolean fullART;

  public ARTNode(int pID, CFANode pCFANode, AbstractionType pAbstractionType, boolean pFullART)
      throws IllegalArgumentException {
    if (pCFANode == null || pAbstractionType == AbstractionType.Abstraction) { throw new IllegalArgumentException(
        "Abstraction missing."); }
    id = pID;
    cfaNode = pCFANode;
    isAbstraction = pAbstractionType;
    fullART = pFullART;
  }

  public ARTNode(int pID, CFANode pCFANode, AbstractionType pAbstractionType,
      String pAbstraction, boolean pFullART) throws IllegalArgumentException {
    if (pCFANode == null || pAbstraction == null || pAbstraction.length() == 0
        || pAbstractionType != AbstractionType.Abstraction) { throw new IllegalArgumentException(
        "Abstraction missing."); }
    id = pID;
    cfaNode = pCFANode;
    isAbstraction = pAbstractionType;
    abstraction = pAbstraction;
    fullART = pFullART;
  }

  public void addEdge(ARTEdge pEdge) throws IllegalArgumentException {
    if (isEdgeContained(pEdge)) { throw new IllegalArgumentException("Edge already exists."); }
    // check if it is a valid edge
    if (fullART) {
      if (cfaNode.getEdgeTo(((WithCorrespondingCFAEdgeARTEdge) pEdge).getCorrespondingCFAEdge().getSuccessor()) != ((WithCorrespondingCFAEdgeARTEdge) pEdge)
          .getCorrespondingCFAEdge()) { throw new IllegalArgumentException(
          "Edge cannot be an edge from this ARTNode.\n"); }
    }
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
    if(fullART){
      return edges.toArray(new WithCorrespondingCFAEdgeARTEdge[edges.size()]);
    }
    return edges.toArray(new ARTEdge[edges.size()]);
  }

  public AbstractionType getAbstractionType() {
    return isAbstraction;
  }

  public String getAbstraction() {
    if (isAbstraction == AbstractionType.Abstraction) { return abstraction; }
    return null;
  }

  public boolean isEdgeContained(ARTEdge pEdge) {
    if (edges == null) { return false; }
    ARTEdge edge;
    for (int i = 0; i < edges.size(); i++) {
      edge = edges.get(i);
      if (edge.equals(pEdge)) { return true; }
    }
    return false;
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
