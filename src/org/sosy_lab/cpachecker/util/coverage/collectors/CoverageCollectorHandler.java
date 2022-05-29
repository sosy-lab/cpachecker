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
  private final CounterexampleCoverageCollector counterexampleCoverageCollector;
  private final AnalysisIndependentCoverageCollector analysisIndependentCoverageCollector;
  private final PredicateAnalysisCoverageCollector predicateAnalysisCoverageCollector;
  private final boolean shouldCollectCoverage;

  public CoverageCollectorHandler(CFA cfa, boolean pShouldCollectCoverage) {
    shouldCollectCoverage = pShouldCollectCoverage;
    timeDependentCoverageHandler = new TimeDependentCoverageHandler();
    coverageMeasureHandler = new CoverageMeasureHandler();
    reachedSetCoverageCollector =
        new ReachedSetCoverageCollector(coverageMeasureHandler, timeDependentCoverageHandler, cfa);
    counterexampleCoverageCollector = new CounterexampleCoverageCollector();
    predicateAnalysisCoverageCollector =
        new PredicateAnalysisCoverageCollector(
            coverageMeasureHandler, timeDependentCoverageHandler, cfa);
    analysisIndependentCoverageCollector =
        new AnalysisIndependentCoverageCollector(
            coverageMeasureHandler, timeDependentCoverageHandler, cfa);
    timeDependentCoverageHandler.initPredicateAnalysisTDCG();
  }

  public CoverageCollectorHandler(CFA cfa) {
    this(cfa, false);
  }

  /**
   * This method is called in the end of the analysis. It is used to populate the coverage data for
   * all initialized measures. When adding a new coverage measure this method should be expanded by
   * its type.
   */
  public void collectAllData() {
    if (shouldCollectCoverage) {
      analysisIndependentCoverageCollector.collect(this);
      reachedSetCoverageCollector.collect(this);
      predicateAnalysisCoverageCollector.collect(this);
    }
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

  public CounterexampleCoverageCollector getCounterexampleCoverageCollector() {
    return counterexampleCoverageCollector;
  }

  public boolean shouldCollectCoverage() {
    return shouldCollectCoverage;
  }
}
