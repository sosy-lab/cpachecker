// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.report;

import java.io.PrintStream;
import org.sosy_lab.cpachecker.util.coverage.collectors.AnalysisIndependentCoverageCollector;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollector;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollectorHandler;
import org.sosy_lab.cpachecker.util.coverage.collectors.PredicateAnalysisCoverageCollector;
import org.sosy_lab.cpachecker.util.coverage.collectors.ReachedSetCoverageCollector;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureType;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

/**
 * Class with only purpose of printing all given data to the statistics panel. These coverage
 * statistics are shown i.e. in the Statistic Tab within the report.html.
 */
public class CoverageReportStdoutSummary {
  private static final int INDENT_LEVEL = 1;
  private static final int FIELD_COLUMN_WIDTH = 25;

  /**
   * Write for every verification coverage category a summary statistics.
   *
   * @param collector coverage collector for all source files.
   * @param pStdOut output print stream.
   */
  public static void write(CoverageCollectorHandler collector, PrintStream pStdOut) {
    ReachedSetCoverageCollector reachedCollector = collector.getReachedSetCoverageCollector();
    writeFunctionCoverage(reachedCollector, pStdOut);
    writeConditionCoverage(reachedCollector, pStdOut);
    writeLineRelatedCoverage(reachedCollector, pStdOut);
    writeLocationRelatedCoverage(collector, pStdOut);
  }

  private static void writeFunctionCoverage(CoverageCollector collector, PrintStream pStdOut) {
    int totalFunctionsCount = collector.getAllFunctions().size();
    if (totalFunctionsCount > 0) {
      double functionCoverage = collector.getVisitedFunctionsCount() / (double) totalFunctionsCount;
      StatisticsUtils.write(
          pStdOut,
          INDENT_LEVEL,
          FIELD_COLUMN_WIDTH,
          "Function coverage",
          String.format("%.3f", functionCoverage));
    }
  }

  private static void writeConditionCoverage(CoverageCollector collector, PrintStream pStdOut) {
    int totalConditionsCount = collector.getAllAssumes().size();
    int visitedConditionsCount = collector.getVisitedAssumesCount();
    if (totalConditionsCount > 0) {
      double conditionCoverage = visitedConditionsCount / (double) totalConditionsCount;
      StatisticsUtils.write(
          pStdOut, INDENT_LEVEL, FIELD_COLUMN_WIDTH, "Visited conditions", visitedConditionsCount);
      StatisticsUtils.write(
          pStdOut, INDENT_LEVEL, FIELD_COLUMN_WIDTH, "Total conditions", totalConditionsCount);
      StatisticsUtils.write(
          pStdOut,
          INDENT_LEVEL,
          FIELD_COLUMN_WIDTH,
          "Condition coverage",
          String.format("%.3f", conditionCoverage));
    }
  }

  private static void writeLineRelatedCoverage(CoverageCollector collector, PrintStream pStdOut) {
    int totalLines = collector.getExistingLinesCount();
    int visitedLines = collector.getVisitedLinesCount();
    if (totalLines > 0) {
      final double lineCoverage = visitedLines / (double) totalLines;
      StatisticsUtils.write(
          pStdOut, INDENT_LEVEL, FIELD_COLUMN_WIDTH, "Visited lines", visitedLines);
      StatisticsUtils.write(pStdOut, INDENT_LEVEL, FIELD_COLUMN_WIDTH, "Total lines", totalLines);
      StatisticsUtils.write(
          pStdOut,
          INDENT_LEVEL,
          FIELD_COLUMN_WIDTH,
          "Line coverage",
          String.format("%.3f", lineCoverage));
    }
  }

  private static void writeLocationRelatedCoverage(
      CoverageCollectorHandler collector, PrintStream pStdOut) {
    AnalysisIndependentCoverageCollector analysisIndependentCollector =
        collector.getAnalysisIndependentCollector();
    ReachedSetCoverageCollector reachedCollector = collector.getReachedSetCoverageCollector();
    if (analysisIndependentCollector == null
        || analysisIndependentCollector.getTotalLocationCount() <= 0) {
      return;
    }
    int totalLocations = analysisIndependentCollector.getTotalLocationCount();
    int reachedLocations = reachedCollector.getReachedLocationsCount();
    StatisticsUtils.write(
        pStdOut, INDENT_LEVEL, FIELD_COLUMN_WIDTH, "Total locations", totalLocations);
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.ReachedLocations.getName(),
        reachedLocations);
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.ReachedLocations.getCoverageName(),
        String.format("%.3f", reachedLocations / (double) totalLocations));

    PredicateAnalysisCoverageCollector predicateCollector =
        collector.getPredicateAnalysisCollector();
    if (predicateCollector == null) {
      return;
    }
    int predicateConsideredLocations = predicateCollector.getPredicateConsideredLocations().size();
    int predicateRelevantVariablesLocations =
        predicateCollector.getPredicateRelevantConsideredLocationsCount();
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.PredicateConsidered.getName(),
        predicateConsideredLocations);
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.PredicateRelevantVariables.getName(),
        predicateRelevantVariablesLocations);
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.PredicateConsidered.getCoverageName(),
        String.format("%.3f", predicateConsideredLocations / (double) totalLocations));
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.PredicateRelevantVariables.getCoverageName(),
        String.format("%.3f", predicateRelevantVariablesLocations / (double) totalLocations));
  }
}
