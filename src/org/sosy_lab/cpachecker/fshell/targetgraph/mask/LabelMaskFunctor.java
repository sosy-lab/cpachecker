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
package org.sosy_lab.cpachecker.fshell.targetgraph.mask;

import org.jgrapht.graph.MaskFunctor;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fshell.targetgraph.Edge;
import org.sosy_lab.cpachecker.fshell.targetgraph.Node;

public class LabelMaskFunctor implements MaskFunctor<Node, Edge> {

  private String mLabel;

  public LabelMaskFunctor(String pLabel) {
    mLabel = pLabel;
  }

  @Override
  public boolean isEdgeMasked(Edge pArg0) {
    return !matches(pArg0.getSource().getCFANode());
  }

  private boolean matches(CFANode pNode) {
    if (pNode instanceof CFALabelNode) {
      CFALabelNode lLabelNode = (CFALabelNode)pNode;
      return mLabel.equals(lLabelNode.getLabel());
    }

    return false;
  }

  @Override
  public boolean isVertexMasked(Node pArg0) {
    CFANode lCFANode = pArg0.getCFANode();

    if (matches(lCFANode)) {
      return false;
    }

    for (int lIndex = 0; lIndex < lCFANode.getNumEnteringEdges(); lIndex++) {
      CFAEdge lCFAEdge = lCFANode.getEnteringEdge(lIndex);

      if (matches(lCFAEdge.getPredecessor())) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (!pOther.getClass().equals(getClass())) {
      return false;
    }

    LabelMaskFunctor lFunctor = (LabelMaskFunctor)pOther;

    return mLabel.equals(lFunctor.mLabel);
  }

  @Override
  public int hashCode() {
    return mLabel.hashCode() + 21928;
  }

}
