// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.coverage.CoverageData;

public class CoverageTransferRelation extends SingleEdgeTransferRelation {

  private final CoverageData cov;

  public CoverageTransferRelation(CoverageData pCov) {
    cov = Preconditions.checkNotNull(pCov);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException {

    handleEdge(pCfaEdge);
    return Collections.singleton(pElement);
  }

  private void handleEdge(CFAEdge pEdge) {

    cov.addVisitedEdge(pEdge);

    if (pEdge.getPredecessor() instanceof FunctionEntryNode) {
      cov.addVisitedFunction((FunctionEntryNode) pEdge.getPredecessor());
    }
  }
}
