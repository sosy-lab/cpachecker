// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage.analysisindependent;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.coverage.collectors.AnalysisIndependentCoverageCollector;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollectorHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageData;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageType;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageUtility;

public class AnalysisIndependentCoverageTransferRelation extends SingleEdgeTransferRelation {
  private final AnalysisIndependentCoverageCollector coverageCollector;
  private final TimeDependentCoverageData visitedTDCG;

  public AnalysisIndependentCoverageTransferRelation(
      CoverageCollectorHandler pCovCollectorHandler) {
    coverageCollector =
        Preconditions.checkNotNull(pCovCollectorHandler.getAnalysisIndependentCoverageCollector());
    visitedTDCG =
        pCovCollectorHandler.getTDCGHandler().getData(TimeDependentCoverageType.VisitedLines);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException {
    handleEdge(pCfaEdge);
    return Collections.singleton(pElement);
  }

  private void handleEdge(CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    coverageCollector.addVisitedEdge(pEdge);
    coverageCollector.addVisitedLocation(pEdge);
    visitedTDCG.addTimeStamp(coverageCollector.getTempVisitedCoverage());
    if (pEdge.getPredecessor() instanceof FunctionEntryNode) {
      coverageCollector.addVisitedFunction((FunctionEntryNode) pEdge.getPredecessor());
    }
  }
}
