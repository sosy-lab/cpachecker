// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.coverage.collectors.AnalysisIndependentCoverageCollector;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollectorHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageData;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageType;

public class CoverageTransferRelation extends SingleEdgeTransferRelation {
  private final AnalysisIndependentCoverageCollector coverageCollector;
  private final TimeDependentCoverageData visitedTDCG;
  private final boolean shouldCollectCoverage;
  private boolean isFirstTransferRelation = true;

  public CoverageTransferRelation(
      CoverageCollectorHandler pCovCollectorHandler, boolean pShouldCollectCoverage) {
    shouldCollectCoverage = pShouldCollectCoverage;
    coverageCollector =
        Preconditions.checkNotNull(pCovCollectorHandler.getAnalysisIndependentCollector());
    visitedTDCG =
        pCovCollectorHandler.getTDCGHandler().getData(TimeDependentCoverageType.VisitedLines);
    if (shouldCollectCoverage) {
      coverageCollector.addAllProgramVariables();
    }
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException {
    if (shouldCollectCoverage) {
      addInitialTDCGTimestamps();
      handleEdge(pCfaEdge);
    }
    return Collections.singleton(pElement);
  }

  private void addInitialTDCGTimestamps() {
    if (isFirstTransferRelation) {
      visitedTDCG.addTimestamp(0);
      isFirstTransferRelation = false;
    }
  }

  private void handleEdge(CFAEdge pEdge) {
    coverageCollector.addVisitedEdge(pEdge);
    coverageCollector.addVisitedLocation(pEdge);
    coverageCollector.addVisitedVariables(pEdge);
    visitedTDCG.addTimestamp(coverageCollector.getTempVisitedCoverage());
  }
}
