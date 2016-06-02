/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.coverage;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.coverage.CoverageData.CoverageMode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.Collection;
import java.util.Collections;

public class CoverageTransferRelation extends SingleEdgeTransferRelation {

  private final CoverageData cov;

  public CoverageTransferRelation(CFA pCFA, CoverageData pCov) {
    cov = Preconditions.checkNotNull(pCov);
    Preconditions.checkArgument(cov.getCoverageMode() == CoverageMode.TRANSFER);

    // ------------ Existing lines ----------------
    for (CFANode node : pCFA.getAllNodes()) {
      // This part adds lines, which are only on edges, such as "return" or "goto"
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        cov.handleEdgeCoverage(edge, false);
      }
    }

    // ------------ Existing functions -------------
    for (FunctionEntryNode entryNode : pCFA.getAllFunctionHeads()) {
      cov.putExistingFunction(entryNode);
    }

  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    handleEdge(pCfaEdge);
    return Collections.singleton(pElement);
  }

  private void handleEdge(CFAEdge pEdge) {

    cov.handleEdgeCoverage(pEdge, true);

    if (pEdge.getPredecessor() instanceof FunctionEntryNode) {
      cov.addVisitedFunction((FunctionEntryNode) pEdge.getPredecessor());
    }
  }
}
