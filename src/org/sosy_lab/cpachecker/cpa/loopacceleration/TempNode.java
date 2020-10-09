// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.cpa.loopacceleration;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class TempNode {

  private CFANode node;
  private int nextEdge;

  public TempNode(CFANode cNode, int nextEdgeT) {
    node = cNode;
    nextEdge = nextEdgeT;
  }

  public void setNextEdge(int nextEdge) {
    this.nextEdge = nextEdge;
  }

  public int getNextEdge() {
    return nextEdge;
  }

  public CFANode getNode() {
    return node;
  }
}
