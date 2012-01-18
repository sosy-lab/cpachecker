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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fshell.targetgraph.Edge;
import org.sosy_lab.cpachecker.fshell.targetgraph.Node;

public class LineNumberMaskFunctor implements MaskFunctor<Node, Edge> {

  private int mLineNumber;

  public LineNumberMaskFunctor(int pLineNumber) {
    assert(pLineNumber > 0);

    mLineNumber = pLineNumber;
  }

  @Override
  public boolean isEdgeMasked(Edge pArg0) {
    assert(pArg0 != null);

    return pArg0.getTarget().getCFANode().getLineNumber() != mLineNumber;
  }

  @Override
  public boolean isVertexMasked(Node pArg0) {
    assert(pArg0 != null);

    CFANode lCFANode = pArg0.getCFANode();

    if (pArg0.getCFANode().getLineNumber() != mLineNumber) {
      for (int lIndex = 0; lIndex < lCFANode.getNumLeavingEdges(); lIndex++) {
        CFAEdge lCFAEdge = lCFANode.getLeavingEdge(lIndex);

        if (lCFAEdge.getSuccessor().getLineNumber() == mLineNumber) {
          // predecessor has correct line number and thus we have to keep this
          // vertex to preserve the edge
          return false;
        }
      }

      return true;
    }
    else {
      return false;
    }
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (pOther.getClass() == getClass()) {
      LineNumberMaskFunctor lFunctor = (LineNumberMaskFunctor)pOther;

      return (mLineNumber == lFunctor.mLineNumber);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 234677 + mLineNumber;
  }

}
