// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.data;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Class which holds all collected predicate-analysis specific coverage data for a specific file.
 */
public class FilePredicateCoverageStatistics {
  public int previousPredicateRelevantVariablesLocationsSize = 0;
  public final Set<CFANode> allPredicateConsideredLocations = new LinkedHashSet<>();
  public final Set<CFANode> allPredicateRelevantVariablesLocations = new LinkedHashSet<>();
  public final Multiset<String> allVariableNames = HashMultiset.create();
  public final Multiset<String> relevantVariableNames = HashMultiset.create();

  public void addPredicateConsideredNode(CFANode node) {
    allPredicateConsideredLocations.add(node);
  }

  public void addPredicateRelevantVariablesNodes(CFANode node) {
    allPredicateRelevantVariablesLocations.add(node);
  }

  public void resetPredicateRelevantVariablesNodes() {
    previousPredicateRelevantVariablesLocationsSize = allPredicateRelevantVariablesLocations.size();
    allPredicateRelevantVariablesLocations.clear();
  }
}
