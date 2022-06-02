// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.report;

import java.io.PrintStream;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollector;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollectorHandler;
import org.sosy_lab.cpachecker.util.coverage.collectors.ReachedSetCoverageCollector;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasure;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
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
    CoverageMeasureHandler coverageHandler = collector.getCoverageHandler();
    ReachedSetCoverageCollector reachedSetCollector = collector.getReachedSetCoverageCollector();
    writeFunctionCoverage(reachedSetCollector, pStdOut);
    writeConditionCoverage(reachedSetCollector, pStdOut);
    writeLineRelatedCoverage(reachedSetCollector, pStdOut);
    writeLocationRelatedCoverage(coverageHandler, pStdOut);
    writeVariableRelatedCoverage(coverageHandler, pStdOut);
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
          String.format("%.2f", functionCoverage));
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
          String.format("%.2f", conditionCoverage));
    }
  }

  private static void writeLineRelatedCoverage(CoverageCollector collector, PrintStream pStdOut) {
    int totalLinesCount = collector.getExistingLinesCount();
    int visitedLinesCount = collector.getVisitedLinesCount();
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        "Line coverage",
        String.format("%.2f", visitedLinesCount / (double) totalLinesCount));
    StatisticsUtils.write(
        pStdOut, INDENT_LEVEL, FIELD_COLUMN_WIDTH, "Visited lines", visitedLinesCount);
    StatisticsUtils.write(
        pStdOut, INDENT_LEVEL, FIELD_COLUMN_WIDTH, "Total lines", totalLinesCount);
  }

  private static void writeLocationRelatedCoverage(
      CoverageMeasureHandler handler, PrintStream pStdOut) {
    CoverageMeasure reached = handler.getData(CoverageMeasureType.REACHED_LOCATIONS);
    if (reached == null || reached.getMaxValue() <= 0) {
      return;
    }
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        "Total locations",
        String.format("%.0f", reached.getMaxValue()));
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.REACHED_LOCATIONS.getCoverageName(),
        String.format("%.2f", reached.getNormalizedValue()));
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.REACHED_LOCATIONS.getName(),
        String.format("%.0f", reached.getValue()));

    CoverageMeasure predicateConsidered =
        handler.getData(CoverageMeasureType.PREDICATE_CONSIDERED_LOCATIONS);
    if (predicateConsidered == null || predicateConsidered.getMaxValue() <= 0) {
      return;
    }
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.PREDICATE_CONSIDERED_LOCATIONS.getCoverageName(),
        String.format("%.2f", predicateConsidered.getNormalizedValue()));
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.PREDICATE_CONSIDERED_LOCATIONS.getName(),
        String.format("%.0f", predicateConsidered.getValue()));

    CoverageMeasure relevantVar =
        handler.getData(CoverageMeasureType.PREDICATE_RELEVANT_VARIABLES_LOCATIONS);
    if (relevantVar == null || relevantVar.getMaxValue() <= 0) {
      return;
    }
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.PREDICATE_RELEVANT_VARIABLES_LOCATIONS.getCoverageName(),
        String.format("%.2f", relevantVar.getNormalizedValue()));
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.PREDICATE_RELEVANT_VARIABLES_LOCATIONS.getName(),
        String.format("%.0f", relevantVar.getValue()));
  }

  private static void writeVariableRelatedCoverage(
      CoverageMeasureHandler handler, PrintStream pStdOut) {
    CoverageMeasure abstractionVariables =
        handler.getData(CoverageMeasureType.PREDICATE_ABSTRACTION_VARIABLES);
    if (abstractionVariables == null || abstractionVariables.getMaxValue() <= 0) {
      return;
    }
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.PREDICATE_ABSTRACTION_VARIABLES.getCoverageName(),
        String.format("%.2f", abstractionVariables.getNormalizedValue()));
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.PREDICATE_ABSTRACTION_VARIABLES.getName(),
        String.format("%.0f", abstractionVariables.getValue()));
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        "Total variables",
        String.format("%.0f", abstractionVariables.getMaxValue()));

    CoverageMeasure visitedVariables = handler.getData(CoverageMeasureType.VISITED_VARIABLES);
    if (visitedVariables == null || visitedVariables.getMaxValue() <= 0) {
      return;
    }
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.VISITED_VARIABLES.getCoverageName(),
        String.format("%.2f", visitedVariables.getNormalizedValue()));
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        CoverageMeasureType.VISITED_VARIABLES.getName(),
        String.format("%.0f", visitedVariables.getValue()));
    StatisticsUtils.write(
        pStdOut,
        INDENT_LEVEL,
        FIELD_COLUMN_WIDTH,
        "Total variables",
        String.format("%.0f", visitedVariables.getMaxValue()));
  }
}
