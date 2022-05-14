// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.data;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class which holds all collected coverage data for all files. Therefore, this class is summary for
 * all FileCoverageStatistics.
 */
public class CoverageStatistics {
  /* ##### Class Fields ##### */
  public long numTotalConditions = 0;
  public long numTotalFunctions = 0;
  public long numTotalLines = 0;

  public long numVisitedConditions = 0;
  public long numVisitedFunctions = 0;
  public long numVisitedLines = 0;

  public long numTotalNodes = 0;
  public long numReachedNodes = 0;
  public long numPredicateConsideredLocations = 0;
  public long numPredicateRelevantVariablesLocations = 0;

  public Set<Integer> predicateConsideredNodes = new HashSet<>();
  public Set<Integer> predicateRelevantVariablesConsideredNodes = new HashSet<>();
  public Multiset<Integer> visitedLines = LinkedHashMultiset.create();
  public Multiset<Integer> visitedLocations = LinkedHashMultiset.create();
  public Multiset<Integer> reachedLocations = LinkedHashMultiset.create();
  public Multiset<String> allVariableNames = HashMultiset.create();
  public Multiset<String> relevantVariableNames = HashMultiset.create();

  /* ##### Constructors ##### */
  public CoverageStatistics(Map<String, FileCoverageStatistics> infosPerFile) {
    for (FileCoverageStatistics info : infosPerFile.values()) {
      predicateConsideredNodes.addAll(info.predicateStatistics.allPredicateConsideredLocations);
      predicateRelevantVariablesConsideredNodes.addAll(
          info.predicateStatistics.allPredicateRelevantVariablesLocations);
      visitedLines.addAll(info.visitedLines);
      visitedLocations.addAll(info.visitedLocations);
      reachedLocations.addAll(info.allReachedNodes);
      allVariableNames.addAll(info.predicateStatistics.allVariableNames);
      relevantVariableNames.addAll(info.predicateStatistics.relevantVariableNames);

      numTotalFunctions += info.allFunctions.size();
      numVisitedFunctions += info.visitedFunctions.elementSet().size();

      numTotalConditions += info.allAssumes.size();
      numVisitedConditions += info.visitedAssumes.size();

      numTotalLines += info.allLines.size();
      numVisitedLines += info.visitedLines.elementSet().size();

      numTotalNodes += info.allNodes.size();
      numReachedNodes += info.allReachedNodes.elementSet().size();
      numPredicateConsideredLocations +=
          info.predicateStatistics.allPredicateConsideredLocations.size();
      numPredicateRelevantVariablesLocations +=
          Math.max(
              info.predicateStatistics.allPredicateRelevantVariablesLocations.size(),
              info.predicateStatistics.previousPredicateRelevantVariablesLocationsSize);
    }
  }
}
