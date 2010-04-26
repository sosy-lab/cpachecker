/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
/**
 *
 */
package org.sosy_lab.cpachecker.util.automaton.cfa;

import org.sosy_lab.cpachecker.util.automaton.Label;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;

/**
 * @author holzera
 *
 */
public class FunctionCallLabel implements Label<CFAEdge> {
  private String mFunctionName;

  public FunctionCallLabel(String pFunctionName) {
    mFunctionName = pFunctionName;
  }

  @Override
  public boolean matches(CFAEdge pEdge) {
    if (CFAEdgeType.FunctionCallEdge == pEdge.getEdgeType()) {
      return pEdge.getSuccessor().getFunctionName().equals(mFunctionName);
    }

    return false;
  }

  @Override
  public boolean equals(Object pObject) {
    if (pObject == null) {
      return false;
    }

    if (!(pObject instanceof FunctionCallLabel)) {
      return false;
    }

    FunctionCallLabel lLabel = (FunctionCallLabel)pObject;

    return mFunctionName.equals(lLabel.mFunctionName);
  }

  @Override
  public int hashCode() {
    return mFunctionName.hashCode();
  }

  @Override
  public String toString() {
    return "@CALL(" + mFunctionName + ")";
  }
}
