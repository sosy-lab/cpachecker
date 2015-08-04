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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

public class Wrapper {

  private CFA mCFA;
  private CFAEdge mAlphaEdge;
  private CFAEdge mOmegaEdge;

  public Wrapper(CFA pCFA, String pOriginalEntryFunction) {
    // pCFA already contains a wrapper function in the C code! See CFACreator.CPAtiger_MAIN

    mCFA = pCFA;

    determineAlphaAndOmegaEdges(mCFA.getFunctionHead(WrapperUtil.CPAtiger_MAIN), mCFA.getFunctionHead(pOriginalEntryFunction));
  }

  private void determineAlphaAndOmegaEdges(CFANode pInitialNode, CFANode pOriginalInitialNode) {
    assert(pInitialNode != null);

    Set<CFANode> lWorklist = new LinkedHashSet<>();
    Set<CFANode> lVisitedNodes = new HashSet<>();

    lWorklist.add(pInitialNode);

    while (!lWorklist.isEmpty()) {
      CFANode lCFANode = lWorklist.iterator().next();
      lWorklist.remove(lCFANode);

      if (lVisitedNodes.contains(lCFANode)) {
        continue;
      }

      lVisitedNodes.add(lCFANode);

      // determine successors
      FunctionSummaryEdge lCallToReturnEdge = lCFANode.getLeavingSummaryEdge();

      if (lCallToReturnEdge != null) {

        if (lCFANode.getNumLeavingEdges() != 1) {
          throw new IllegalArgumentException();
        }

        CFAEdge lEdge = lCFANode.getLeavingEdge(0);

        CFANode lPredecessor = lEdge.getPredecessor();
        CFANode lSuccessor = lEdge.getSuccessor();

        if (lSuccessor.equals(pOriginalInitialNode)) {
          if (!lEdge.getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
            throw new RuntimeException();
          }

          mAlphaEdge = lEdge;

          CFAEdge lSummaryEdge = lPredecessor.getLeavingSummaryEdge();

          if (lSummaryEdge == null) {
            throw new RuntimeException();
          }

          CFANode lSummarySuccessor = lSummaryEdge.getSuccessor();

          if (lSummarySuccessor.getNumEnteringEdges() != 1) {
            throw new RuntimeException("Summary successor has " + lSummarySuccessor.getNumEnteringEdges() + " entering CFA edges!");
          }

          mOmegaEdge = lSummarySuccessor.getEnteringEdge(0);

          break;
        }

        lWorklist.add(lCallToReturnEdge.getSuccessor());
      }
      else {
        int lNumberOfLeavingEdges = lCFANode.getNumLeavingEdges();

        for (int lEdgeIndex = 0; lEdgeIndex < lNumberOfLeavingEdges; lEdgeIndex++) {
          CFAEdge lEdge = lCFANode.getLeavingEdge(lEdgeIndex);

          CFANode lSuccessor = lEdge.getSuccessor();
          lWorklist.add(lSuccessor);
        }
      }
    }

    assert(mAlphaEdge != null);
    assert(mOmegaEdge != null);
  }

  public CFA getCFA() {
    return mCFA;
  }

  public CFAEdge getAlphaEdge() {
    return mAlphaEdge;
  }

  public CFAEdge getOmegaEdge() {
    return mOmegaEdge;
  }
/*
  public FunctionEntryNode getCFA(String pFunctionName) {
    return mCFA.getFunctionHead(pFunctionName);
  }

  public FunctionEntryNode getEntry() {
    return mCFA.getMainFunction();
  }*/

}
