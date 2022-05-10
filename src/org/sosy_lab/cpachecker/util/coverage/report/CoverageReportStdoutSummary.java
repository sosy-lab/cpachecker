// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.report;

import java.io.PrintStream;
import java.util.Map;
import org.sosy_lab.cpachecker.util.coverage.data.CoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.data.FileCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureType;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

/**
 * Class with only purpose of printing all given data to the statistics panel. These coverage
 * statistics are shown i.e. in the Statistic Tab within the report.html.
 */
public class CoverageReportStdoutSummary {
  /* ##### Private Constants ##### */
  private static final int INDENT_LEVEL = 1;
  private static final int FIELD_COLUMN_WIDTH = 25;

  /* ##### Public Methods ##### */
  /**
   * Write for every verification coverage category a summary statistics.
   *
   * @param infosPerFile coverage statistics per source file.
   * @param pStdOut output print stream.
   */
  public static void write(Map<String, FileCoverageStatistics> infosPerFile, PrintStream pStdOut) {
    CoverageStatistics covStatistics = new CoverageStatistics(infosPerFile);
    writeFunctionCoverage(covStatistics, pStdOut);
    writeConditionCoverage(covStatistics, pStdOut);
    writeLineRelatedCoverage(covStatistics, pStdOut);
    writeLocationRelatedCoverage(covStatistics, pStdOut);
  }

  /* ##### Helper Methods ##### */
  private static void writeFunctionCoverage(CoverageStatistics covStats, PrintStream pStdOut) {
    if (covStats.numTotalFunctions > 0) {
      final double functionCoverage =
          covStats.numVisitedFunctions / (double) covStats.numTotalFunctions;
      StatisticsUtils.write(
          pStdOut,
          INDENT_LEVEL,
          FIELD_COLUMN_WIDTH,
          "Function coverage",
          String.format("%.3f", functionCoverage));
    }
  }

  private static void writeConditionCoverage(CoverageStatistics covStats, PrintStream pStdOut) {
    if (covStats.numTotalConditions > 0) {
      final double conditionCoverage =
          covStats.numVisitedConditions / (double) covStats.numTotalConditions;
      StatisticsUtils.write(
          pStdOut,
          INDENT_LEVEL,
          FIELD_COLUMN_WIDTH,
          "Visited conditions",
          covStats.numVisitedConditions);
      StatisticsUtils.write(
          pStdOut,
          INDENT_LEVEL,
          FIELD_COLUMN_WIDTH,
          "Total conditions",
          covStats.numTotalConditions);
      StatisticsUtils.write(
          pStdOut,
          INDENT_LEVEL,
          FIELD_COLUMN_WIDTH,
          "Condition coverage",
          String.format("%.3f", conditionCoverage));
    }
  }

  private static void writeLineRelatedCoverage(CoverageStatistics covStats, PrintStream pStdOut) {
    if (covStats.numTotalLines > 0) {
      final double lineCoverage = covStats.numVisitedLines / (double) covStats.numTotalLines;
      StatisticsUtils.write(
          pStdOut, INDENT_LEVEL, FIELD_COLUMN_WIDTH, "Visited lines", covStats.numVisitedLines);
      StatisticsUtils.write(
          pStdOut, INDENT_LEVEL, FIELD_COLUMN_WIDTH, "Total lines", covStats.numTotalLines);
      StatisticsUtils.write(
          pStdOut,
          INDENT_LEVEL,
          FIELD_COLUMN_WIDTH,
          "Line coverage",
          String.format("%.3f", lineCoverage));
    }
  }

  private static void writeLocationRelatedCoverage(
      CoverageStatistics covStats, PrintStream pStdOut) {
    if (covStats.numTotalNodes > 0) {
      final double reachedCoverage = covStats.numReachedNodes / (double) covStats.numTotalNodes;
      final double predicateConsideredCoverage =
          covStats.numPredicateConsideredLocations / (double) covStats.numTotalNodes;
      final double predicateRelevantVariablesCoverage =
          covStats.numPredicateRelevantVariablesLocations / (double) covStats.numTotalNodes;
      StatisticsUtils.write(
          pStdOut,
          INDENT_LEVEL,
          FIELD_COLUMN_WIDTH,
          CoverageMeasureType.ReachedLocations.getName(),
          covStats.numReachedNodes);
      StatisticsUtils.write(
          pStdOut,
          INDENT_LEVEL,
          FIELD_COLUMN_WIDTH,
          CoverageMeasureType.PredicateConsidered.getName(),
          covStats.numPredicateConsideredLocations);
      StatisticsUtils.write(
          pStdOut,
          INDENT_LEVEL,
          FIELD_COLUMN_WIDTH,
          CoverageMeasureType.PredicateRelevantVariables.getName(),
          covStats.numPredicateRelevantVariablesLocations);
      StatisticsUtils.write(
          pStdOut, INDENT_LEVEL, FIELD_COLUMN_WIDTH, "Total nodes", covStats.numTotalNodes);
      StatisticsUtils.write(
          pStdOut,
          INDENT_LEVEL,
          FIELD_COLUMN_WIDTH,
          CoverageMeasureType.ReachedLocations.getCoverageName(),
          String.format("%.3f", reachedCoverage));
      StatisticsUtils.write(
          pStdOut,
          INDENT_LEVEL,
          FIELD_COLUMN_WIDTH,
          CoverageMeasureType.PredicateConsidered.getCoverageName(),
          String.format("%.3f", predicateConsideredCoverage));
      StatisticsUtils.write(
          pStdOut,
          INDENT_LEVEL,
          FIELD_COLUMN_WIDTH,
          CoverageMeasureType.PredicateRelevantVariables.getCoverageName(),
          String.format("%.3f", predicateRelevantVariablesCoverage));
    }
  }
}
