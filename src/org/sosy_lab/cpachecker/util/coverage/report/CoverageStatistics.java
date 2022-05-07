// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.report;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.util.coverage.CoverageData;

public class CoverageStatistics {
  public long numTotalConditions = 0;
  public long numTotalFunctions = 0;
  public long numTotalLines = 0;

  public long numVisitedConditions = 0;
  public long numVisitedFunctions = 0;
  public long numVisitedLines = 0;

  public long numTotalNodes = 0;
  public long numConsideredNodes = 0;
  public long numPredicateConsideredNodes = 0;
  public long numAbstractStateCoveredNodes = 0;
  public long numPredicateRelevantVariablesNodes = 0;
  public double predicateCoverage;

  public Set<Integer> predicateConsideredNodes = new HashSet<>();
  public Set<Integer> predicateRelevantVariablesConsideredNodes = new HashSet<>();
  public Multiset<Integer> visitedLines = LinkedHashMultiset.create();

  public CoverageStatistics(CoverageData pCoverage) {
    predicateCoverage = pCoverage.getPredicateCoverage();
    for (FileCoverageStatistics info : pCoverage.getInfosPerFile().values()) {
      predicateConsideredNodes.addAll(info.getAllPredicateConsideredNodes());
      predicateRelevantVariablesConsideredNodes.addAll(
          info.getAllPredicateRelevantVariablesNodes());
      visitedLines.addAll(info.visitedLines);

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
  }
}
