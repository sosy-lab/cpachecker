/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fshell.targetgraph.Edge;
import org.sosy_lab.cpachecker.fshell.targetgraph.Node;

public class FunctionNameMaskFunctor implements MaskFunctor<Node, Edge> {

  private String mFunctionName;

  public FunctionNameMaskFunctor(String pFunctionName) {
    assert(pFunctionName != null);

    mFunctionName = pFunctionName;
  }

  @Override
  public boolean isEdgeMasked(Edge pArg0) {
    assert(pArg0 != null);

    return isVertexMasked(pArg0.getSource()) || isVertexMasked(pArg0.getTarget());
  }

  @Override
  public boolean isVertexMasked(Node pArg0) {
    assert(pArg0 != null);

    CFANode lCFANode = pArg0.getCFANode();

    return !lCFANode.getFunctionName().equals(mFunctionName);
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
      FunctionNameMaskFunctor lFunctor = (FunctionNameMaskFunctor)pOther;

      return mFunctionName.equals(lFunctor.mFunctionName);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 23477723 + mFunctionName.hashCode();
  }

}
