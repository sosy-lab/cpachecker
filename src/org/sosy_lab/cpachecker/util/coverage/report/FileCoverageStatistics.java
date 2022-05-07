// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.report;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class FileCoverageStatistics {

  static class FunctionInfo {
    final String name;
    final int firstLine;
    final int lastLine;

    FunctionInfo(String pName, int pFirstLine, int pLastLine) {
      name = pName;
      firstLine = pFirstLine;
      lastLine = pLastLine;
    }
  }

  public final Set<Integer> allNodes = new LinkedHashSet<>();
  public final Set<Integer> allConsideredNodes = new LinkedHashSet<>();
  public final Set<Integer> allPredicateConsideredNodes = new LinkedHashSet<>();
  public final Set<Integer> allPredicateRelevantVariablesNodes = new LinkedHashSet<>();
  public final Set<Integer> allAbstractStateCoveredNodes = new LinkedHashSet<>();
  public final Multiset<Integer> visitedLines = LinkedHashMultiset.create();
  public final Set<Integer> allLines = new LinkedHashSet<>();
  public final Multiset<String> visitedFunctions = LinkedHashMultiset.create();
  public final Set<FunctionInfo> allFunctions = new LinkedHashSet<>();
  public final Set<AssumeEdge> allAssumes = new LinkedHashSet<>();
  public final Set<AssumeEdge> visitedAssumes = new LinkedHashSet<>();

  int previousPredicateRelevantVariablesNodesSize = 0;

  public Multiset<Integer> getVisitedLines() {
    return visitedLines;
  }

  public Set<Integer> getAllPredicateConsideredNodes() {
    return allPredicateConsideredNodes;
  }

  public Set<Integer> getAllPredicateRelevantVariablesNodes() {
    return allPredicateRelevantVariablesNodes;
  }

  public void addVisitedAssume(AssumeEdge pEdge) {
    visitedAssumes.add(pEdge);
  }

  public void addExistingAssume(AssumeEdge pEdge) {
    allAssumes.add(pEdge);
  }

  public void addVisitedFunction(String pName) {
    visitedFunctions.add(pName);
  }

  public void addExistingFunction(String pName, int pFirstLine, int pLastLine) {
    allFunctions.add(new FunctionInfo(pName, pFirstLine, pLastLine));
  }

  public void addVisitedLine(int pLine) {
    checkArgument(pLine > 0);
    visitedLines.add(pLine);
  }

  public void addConsideredNode(int id) {
    allConsideredNodes.add(id);
  }

  public void addExistingNode(int id) {
    allNodes.add(id);
  }

  public int getVisitedLine(int pLine) {
    checkArgument(pLine > 0);
    return visitedLines.count(pLine);
  }

  public void addExistingLine(int pLine) {
    checkArgument(pLine > 0);
    allLines.add(pLine);
  }

  public void addPredicateConsideredNode(CFANode node) {
    allPredicateConsideredNodes.add(node.getNodeNumber());
  }

  public void addPredicateRelevantVariablesNodes(CFANode node) {
    allPredicateRelevantVariablesNodes.add(node.getNodeNumber());
  }

  public void addAbstractStateCoveredNodes(Set<CFANode> nodes) {
    for (CFANode node : nodes) {
      allAbstractStateCoveredNodes.add(node.getNodeNumber());
    }
  }

  public void resetPredicateRelevantVariablesNodes() {
    previousPredicateRelevantVariablesNodesSize = allPredicateRelevantVariablesNodes.size();
    allPredicateRelevantVariablesNodes.clear();
  }
}
