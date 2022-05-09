// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.collectors;

import java.util.LinkedHashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.util.coverage.data.FileCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;

public class CoverageCollectorHandler {
  private final Map<String, FileCoverageStatistics> infosPerFile;
  private final CoverageMeasureHandler coverageMeasureHandler;
  private final TimeDependentCoverageHandler timeDependentCoverageHandler;

  private AnalysisIndependentCoverageCollector analysisIndependentCoverageCollector;
  private PredicateAnalysisCoverageCollector predicateAnalysisCoverageCollector;
  private CounterexampleCoverageCollector counterexampleCoverageCollector;
  private ReachedSetCoverageCollector reachedSetCoverageCollector;

  public CoverageCollectorHandler(CFA cfa) {
    this();
    initAllCollectors(cfa);
  }

  public CoverageCollectorHandler() {
    infosPerFile = new LinkedHashMap<>();
    timeDependentCoverageHandler = new TimeDependentCoverageHandler();
    coverageMeasureHandler = new CoverageMeasureHandler();
    analysisIndependentCoverageCollector =
        new AnalysisIndependentCoverageCollector(
            infosPerFile, coverageMeasureHandler, timeDependentCoverageHandler);
    predicateAnalysisCoverageCollector =
        new PredicateAnalysisCoverageCollector(
            infosPerFile, coverageMeasureHandler, timeDependentCoverageHandler);
    counterexampleCoverageCollector =
        new CounterexampleCoverageCollector(
            infosPerFile, coverageMeasureHandler, timeDependentCoverageHandler);
    reachedSetCoverageCollector =
        new ReachedSetCoverageCollector(
            infosPerFile, coverageMeasureHandler, timeDependentCoverageHandler);
  }

  void initAllCollectors(CFA cfa) {
    analysisIndependentCoverageCollector =
        new AnalysisIndependentCoverageCollector(
            infosPerFile, coverageMeasureHandler, timeDependentCoverageHandler, cfa);
    predicateAnalysisCoverageCollector =
        new PredicateAnalysisCoverageCollector(
            infosPerFile, coverageMeasureHandler, timeDependentCoverageHandler, cfa);
    counterexampleCoverageCollector =
        new CounterexampleCoverageCollector(
            infosPerFile, coverageMeasureHandler, timeDependentCoverageHandler, cfa);
    reachedSetCoverageCollector =
        new ReachedSetCoverageCollector(
            infosPerFile, coverageMeasureHandler, timeDependentCoverageHandler, cfa);
  }

  public TimeDependentCoverageHandler getTDCGHandler() {
    return timeDependentCoverageHandler;
  }

  public CoverageMeasureHandler getCoverageHandler() {
    return coverageMeasureHandler;
  }

  public Map<String, FileCoverageStatistics> getInfosPerFile() {
    return infosPerFile;
  }

  public AnalysisIndependentCoverageCollector getAnalysisIndependentCoverageCollector() {
    return analysisIndependentCoverageCollector;
  }

  public PredicateAnalysisCoverageCollector getPredicateAnalysisCoverageCollector() {
    return predicateAnalysisCoverageCollector;
  }

  public CounterexampleCoverageCollector getCounterexampleCoverageCollector() {
    return counterexampleCoverageCollector;
  }

  public ReachedSetCoverageCollector getReachedSetCoverageCollector() {
    return reachedSetCoverageCollector;
  }
}
