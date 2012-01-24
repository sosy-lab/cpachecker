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
import org.sosy_lab.cpachecker.fshell.targetgraph.Edge;
import org.sosy_lab.cpachecker.fshell.targetgraph.Node;

public class FunctionEntryMaskFunctor implements MaskFunctor<Node, Edge> {

  private MaskFunctor<Node, Edge> mFunctionNameMaskFunctor;
  private MaskFunctor<Node, Edge> mFunctionEntriesMaskFunctor;

  public FunctionEntryMaskFunctor(String pFunctionName) {
    assert(pFunctionName != null);

    mFunctionNameMaskFunctor = new FunctionNameMaskFunctor(pFunctionName);
    mFunctionEntriesMaskFunctor = FunctionEntriesMaskFunctor.getInstance();
  }

  @Override
  public boolean isEdgeMasked(Edge pArg0) {
    return mFunctionNameMaskFunctor.isEdgeMasked(pArg0) || mFunctionEntriesMaskFunctor.isEdgeMasked(pArg0);
  }

  @Override
  public boolean isVertexMasked(Node pArg0) {
    return mFunctionNameMaskFunctor.isVertexMasked(pArg0) || mFunctionEntriesMaskFunctor.isVertexMasked(pArg0);
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
      FunctionEntryMaskFunctor lFunctor = (FunctionEntryMaskFunctor)pOther;

      return mFunctionNameMaskFunctor.equals(lFunctor.mFunctionNameMaskFunctor) && mFunctionEntriesMaskFunctor.equals(lFunctor.mFunctionEntriesMaskFunctor);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 2391767 + mFunctionNameMaskFunctor.hashCode() + mFunctionEntriesMaskFunctor.hashCode();
  }

}
