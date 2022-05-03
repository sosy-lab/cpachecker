// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage;

import java.io.PrintStream;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

public class CoverageReportStdoutSummary {

  public static void write(CoverageData pCoverage, PrintStream pStdOut) {
    long numTotalConditions = 0;
    long numTotalFunctions = 0;
    long numTotalLines = 0;

    long numVisitedConditions = 0;
    long numVisitedFunctions = 0;
    long numVisitedLines = 0;

    long numTotalNodes = 0;
    long numConsideredNodes = 0;
    long numPredicateConsideredNodes = 0;
    long numAbstractStateCoveredNodes = 0;
    long numPredicateRelevantVariablesNodes = 0;

    double predicateCoverage = pCoverage.getPredicateCoverage();

    for (FileCoverageInformation info : pCoverage.getInfosPerFile().values()) {
      numTotalFunctions += info.allFunctions.size();
      numVisitedFunctions += info.visitedFunctions.entrySet().size();

      numTotalConditions += info.allAssumes.size();
      numVisitedConditions += info.visitedAssumes.size();

      numTotalLines += info.allLines.size();
      numVisitedLines += info.visitedLines.entrySet().size();

      numTotalNodes += info.allNodes.size();
      numConsideredNodes += info.allConsideredNodes.size();
      numPredicateConsideredNodes += info.allPredicateConsideredNodes.size();
      numAbstractStateCoveredNodes += info.allAbstractStateCoveredNodes.size();
      numPredicateRelevantVariablesNodes +=
          Math.max(
              info.allPredicateRelevantVariablesNodes.size(),
              info.previousPredicateRelevantVariablesNodesSize);
    }

    if (numTotalFunctions > 0) {
      final double functionCoverage = numVisitedFunctions / (double) numTotalFunctions;
      StatisticsUtils.write(
          pStdOut, 1, 25, "Function coverage", String.format("%.3f", functionCoverage));
    }

    if (numTotalLines > 0) {
      final double lineCoverage = numVisitedLines / (double) numTotalLines;
      StatisticsUtils.write(pStdOut, 1, 25, "Visited lines", numVisitedLines);
      StatisticsUtils.write(pStdOut, 1, 25, "Total lines", numTotalLines);
      StatisticsUtils.write(pStdOut, 1, 25, "Line coverage", String.format("%.3f", lineCoverage));
    }

    if (numTotalConditions > 0) {
      final double conditionCoverage = numVisitedConditions / (double) numTotalConditions;
      StatisticsUtils.write(pStdOut, 1, 25, "Visited conditions", numVisitedConditions);
      StatisticsUtils.write(pStdOut, 1, 25, "Total conditions", numTotalConditions);
      StatisticsUtils.write(
          pStdOut, 1, 25, "Condition coverage", String.format("%.3f", conditionCoverage));
    }

    if (numTotalNodes > 0) {
      final double consideredCoverage = numConsideredNodes / (double) numTotalNodes;
      final double predicateConsideredCoverage =
          numPredicateConsideredNodes / (double) numTotalNodes;
      final double predicateRelevantVariablesCoverage =
          numPredicateRelevantVariablesNodes / (double) numTotalNodes;
      final double abstractStateCoveredNodesCoverage =
          numAbstractStateCoveredNodes / (double) numTotalNodes;
      StatisticsUtils.write(pStdOut, 1, 25, "Considered nodes", numConsideredNodes);
      StatisticsUtils.write(
          pStdOut, 1, 25, "Predicate-considered nodes", numPredicateConsideredNodes);
      StatisticsUtils.write(
          pStdOut, 1, 25, "Predicate-relevant-variables nodes", numPredicateRelevantVariablesNodes);
      StatisticsUtils.write(pStdOut, 1, 25, "Predicate-covered nodes", numAbstractStateCoveredNodes);
      StatisticsUtils.write(pStdOut, 1, 25, "Total nodes", numTotalNodes);

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

    if (predicateCoverage > 0.0) {
      StatisticsUtils.write(
          pStdOut,
          1,
          25,
          "Total amount of used predicates",
          String.format("%.0f", predicateCoverage));
    }
  }
}
