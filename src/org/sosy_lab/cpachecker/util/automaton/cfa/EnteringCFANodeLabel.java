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

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.automaton.Label;

/**
 * @author holzera
 *
 */
public class EnteringCFANodeLabel implements Label<CFAEdge> {

  private CFANode mNode;

  public EnteringCFANodeLabel(CFANode pNode) {
    assert(pNode != null);

    mNode = pNode;
  }

  @Override
  public boolean matches(CFAEdge pE) {
    assert(pE != null);

    return mNode.equals(pE.getSuccessor());
  }

  @Override
  public boolean equals(Object pOther) {
    if (pOther == null) {
      return false;
    }

    if (!(pOther instanceof EnteringCFANodeLabel)) {
      return false;
    }

    EnteringCFANodeLabel lOther = (EnteringCFANodeLabel)pOther;

    return mNode.equals(lOther.mNode);
  }

  @Override
  public int hashCode() {
    return mNode.hashCode();
  }

  @Override
  public String toString() {
    return "MATCH(" + mNode.toString() + ")";
  }
}
