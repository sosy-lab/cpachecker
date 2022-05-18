// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.collectors;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;

/**
 * Coverage collector handler which holds all coverage collectors. It handles the initialization and
 * access of collectors.
 */
public class CoverageCollectorHandler {
  private final CoverageMeasureHandler coverageMeasureHandler;
  private final TimeDependentCoverageHandler timeDependentCoverageHandler;
  private final ReachedSetCoverageCollector reachedSetCoverageCollector;
  private AnalysisIndependentCoverageCollector analysisIndependentCoverageCollector;
  private PredicateAnalysisCoverageCollector predicateAnalysisCoverageCollector;
  private final boolean shouldCollectPredicateCoverage;

  public CoverageCollectorHandler(CFA cfa, boolean pShouldCollectPredicateCoverage) {
    shouldCollectPredicateCoverage = pShouldCollectPredicateCoverage;
    timeDependentCoverageHandler = new TimeDependentCoverageHandler();
    coverageMeasureHandler = new CoverageMeasureHandler();
    reachedSetCoverageCollector =
        new ReachedSetCoverageCollector(coverageMeasureHandler, timeDependentCoverageHandler, cfa);
  }

  public void initPredicateCollectors(CFA cfa) {
    predicateAnalysisCoverageCollector =
        new PredicateAnalysisCoverageCollector(
            coverageMeasureHandler, timeDependentCoverageHandler, cfa);
  }

  public void initAnalysisIndependentCollectors(CFA cfa) {
    analysisIndependentCoverageCollector =
        new AnalysisIndependentCoverageCollector(
            coverageMeasureHandler, timeDependentCoverageHandler, cfa);
  }

  public TimeDependentCoverageHandler getTDCGHandler() {
    return timeDependentCoverageHandler;
  }

  public CoverageMeasureHandler getCoverageHandler() {
    return coverageMeasureHandler;
  }

  public AnalysisIndependentCoverageCollector getAnalysisIndependentCollector() {
    return analysisIndependentCoverageCollector;
  }

  public PredicateAnalysisCoverageCollector getPredicateAnalysisCollector() {
    return predicateAnalysisCoverageCollector;
  }

  public ReachedSetCoverageCollector getReachedSetCoverageCollector() {
    return reachedSetCoverageCollector;
  }

  public boolean shouldCollectPredicateCoverage() {
    return shouldCollectPredicateCoverage;
  }
}
