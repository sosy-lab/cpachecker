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
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fshell.targetgraph.Edge;
import org.sosy_lab.cpachecker.fshell.targetgraph.Node;

public class FunctionEntriesMaskFunctor implements MaskFunctor<Node, Edge> {

  private static FunctionEntriesMaskFunctor mInstance = new FunctionEntriesMaskFunctor();

  private FunctionEntriesMaskFunctor() {

  }

  public static FunctionEntriesMaskFunctor getInstance() {
    return mInstance;
  }

  private boolean isFunctionEntryEdge(CFAEdge lEdge) {
    assert(lEdge != null);

    if (lEdge.getEdgeType().equals(CFAEdgeType.BlankEdge)) {
      BlankEdge lBlankEdge = (BlankEdge)lEdge;

      return lBlankEdge.getRawStatement().equals("Function start dummy edge");
    }

    return false;
  }

  @Override
  public boolean isEdgeMasked(Edge pArg0) {
    assert(pArg0 != null);

    return !isFunctionEntryEdge(pArg0.getCFAEdge());
  }

  @Override
  public boolean isVertexMasked(Node pArg0) {
    assert(pArg0 != null);

    CFANode lCFANode = pArg0.getCFANode();

    for (int lIndex = 0; lIndex < lCFANode.getNumEnteringEdges(); lIndex++) {
      if (isFunctionEntryEdge(lCFANode.getEnteringEdge(lIndex))) {
        return false;
      }
    }

    for (int lIndex = 0; lIndex < lCFANode.getNumLeavingEdges(); lIndex++) {
      if (isFunctionEntryEdge(lCFANode.getLeavingEdge(lIndex))) {
        return false;
      }
    }

    return true;
  }

}
