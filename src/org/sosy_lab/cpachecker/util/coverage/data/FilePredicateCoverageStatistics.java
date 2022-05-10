// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.data;

import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class FilePredicateCoverageStatistics {
  /* ##### Class Fields ##### */
  public int previousPredicateRelevantVariablesNodesSize = 0;
  public final Set<Integer> allPredicateConsideredNodes = new LinkedHashSet<>();
  public final Set<Integer> allPredicateRelevantVariablesNodes = new LinkedHashSet<>();

  /* ##### Public Methods ##### */
  public void addPredicateConsideredNode(CFANode node) {
    allPredicateConsideredNodes.add(node.getNodeNumber());
  }

  public void addPredicateRelevantVariablesNodes(CFANode node) {
    allPredicateRelevantVariablesNodes.add(node.getNodeNumber());
  }

  public void resetPredicateRelevantVariablesNodes() {
    previousPredicateRelevantVariablesNodesSize = allPredicateRelevantVariablesNodes.size();
    allPredicateRelevantVariablesNodes.clear();
  }

  /* ##### Getter and Setter ##### */
  public Set<Integer> getAllPredicateConsideredNodes() {
    return allPredicateConsideredNodes;
  }

  public Set<Integer> getAllPredicateRelevantVariablesNodes() {
    return allPredicateRelevantVariablesNodes;
  }
}
