// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.report;

import java.io.PrintStream;
import org.sosy_lab.cpachecker.util.coverage.CoverageData;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

public class CoverageReportStdoutSummary {

  public static void write(CoverageData pCoverage, PrintStream pStdOut) {
    CoverageStatistics covStatistics = new CoverageStatistics(pCoverage);

    if (covStatistics.numTotalFunctions > 0) {
      final double functionCoverage =
          covStatistics.numVisitedFunctions / (double) covStatistics.numTotalFunctions;
      StatisticsUtils.write(
          pStdOut, 1, 25, "Function coverage", String.format("%.3f", functionCoverage));
    }

    if (covStatistics.numTotalLines > 0) {
      final double lineCoverage =
          covStatistics.numVisitedLines / (double) covStatistics.numTotalLines;
      StatisticsUtils.write(pStdOut, 1, 25, "Visited lines", covStatistics.numVisitedLines);
      StatisticsUtils.write(pStdOut, 1, 25, "Total lines", covStatistics.numTotalLines);
      StatisticsUtils.write(pStdOut, 1, 25, "Line coverage", String.format("%.3f", lineCoverage));
    }

    if (covStatistics.numTotalConditions > 0) {
      final double conditionCoverage =
          covStatistics.numVisitedConditions / (double) covStatistics.numTotalConditions;
      StatisticsUtils.write(
          pStdOut, 1, 25, "Visited conditions", covStatistics.numVisitedConditions);
      StatisticsUtils.write(pStdOut, 1, 25, "Total conditions", covStatistics.numTotalConditions);
      StatisticsUtils.write(
          pStdOut, 1, 25, "Condition coverage", String.format("%.3f", conditionCoverage));
    }

    if (covStatistics.numTotalNodes > 0) {
      final double consideredCoverage =
          covStatistics.numConsideredNodes / (double) covStatistics.numTotalNodes;
      final double predicateConsideredCoverage =
          covStatistics.numPredicateConsideredNodes / (double) covStatistics.numTotalNodes;
      final double predicateRelevantVariablesCoverage =
          covStatistics.numPredicateRelevantVariablesNodes / (double) covStatistics.numTotalNodes;
      final double abstractStateCoveredNodesCoverage =
          covStatistics.numAbstractStateCoveredNodes / (double) covStatistics.numTotalNodes;
      StatisticsUtils.write(pStdOut, 1, 25, "Considered nodes", covStatistics.numConsideredNodes);
      StatisticsUtils.write(
          pStdOut, 1, 25, "Predicate-considered nodes", covStatistics.numPredicateConsideredNodes);
      StatisticsUtils.write(
          pStdOut,
          1,
          25,
          "Predicate-relevant-variables nodes",
          covStatistics.numPredicateRelevantVariablesNodes);
      StatisticsUtils.write(
          pStdOut, 1, 25, "Predicate-covered nodes", covStatistics.numAbstractStateCoveredNodes);
      StatisticsUtils.write(pStdOut, 1, 25, "Total nodes", covStatistics.numTotalNodes);

      StatisticsUtils.write(
          pStdOut, 1, 25, "Considered coverage", String.format("%.3f", consideredCoverage));
      StatisticsUtils.write(
          pStdOut,
          1,
          25,
          "Predicate-considered coverage",
          String.format("%.3f", predicateConsideredCoverage));
      StatisticsUtils.write(
          pStdOut,
          1,
          25,
          "Predicate-relevant-variables coverage",
          String.format("%.3f", predicateRelevantVariablesCoverage));
      StatisticsUtils.write(
          pStdOut,
          1,
          25,
          "Predicate-covered-nodes coverage",
          String.format("%.3f", abstractStateCoveredNodesCoverage));
    }

    if (covStatistics.predicateCoverage > 0.0) {
      StatisticsUtils.write(
          pStdOut,
          1,
          25,
          "Total amount of used predicates",
          String.format("%.0f", covStatistics.predicateCoverage));
    }
  }
}
