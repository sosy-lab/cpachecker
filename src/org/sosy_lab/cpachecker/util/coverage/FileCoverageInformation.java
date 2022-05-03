// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class FileCoverageInformation {

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

  final Set<Integer> allNodes = new LinkedHashSet<>();
  final Set<Integer> allConsideredNodes = new LinkedHashSet<>();
  final Set<Integer> allPredicateConsideredNodes = new LinkedHashSet<>();
  final Set<Integer> allPredicateRelevantVariablesNodes = new LinkedHashSet<>();
  final Set<Integer> allAbstractStateCoveredNodes = new LinkedHashSet<>();
  final Multiset<Integer> visitedLines = LinkedHashMultiset.create();
  final Set<Integer> allLines = new LinkedHashSet<>();
  final Multiset<String> visitedFunctions = LinkedHashMultiset.create();
  final Set<FunctionInfo> allFunctions = new LinkedHashSet<>();
  final Set<AssumeEdge> allAssumes = new LinkedHashSet<>();
  final Set<AssumeEdge> visitedAssumes = new LinkedHashSet<>();

  int previousPredicateRelevantVariablesNodesSize = 0;

  public Multiset<Integer> getVisitedLines() {
    return visitedLines;
  }

  void addVisitedAssume(AssumeEdge pEdge) {
    visitedAssumes.add(pEdge);
  }

  void addExistingAssume(AssumeEdge pEdge) {
    allAssumes.add(pEdge);
  }

  void addVisitedFunction(String pName) {
    visitedFunctions.add(pName);
  }

  void addExistingFunction(String pName, int pFirstLine, int pLastLine) {
    allFunctions.add(new FunctionInfo(pName, pFirstLine, pLastLine));
  }

  void addVisitedLine(int pLine) {
    checkArgument(pLine > 0);
    visitedLines.add(pLine);
  }

  void addConsideredNode(int id) {
    allConsideredNodes.add(id);
  }

  void addExistingNode(int id) {
    allNodes.add(id);
  }

  int getVisitedLine(int pLine) {
    checkArgument(pLine > 0);
    return visitedLines.count(pLine);
  }

  void addExistingLine(int pLine) {
    checkArgument(pLine > 0);
    allLines.add(pLine);
  }

  void addPredicateConsideredNode(CFANode node) {
    allPredicateConsideredNodes.add(node.getNodeNumber());
  }

  void addPredicateRelevantVariablesNodes(CFANode node) {
    allPredicateRelevantVariablesNodes.add(node.getNodeNumber());
  }

  void addAbstractStateCoveredNodes(Set<CFANode> nodes) {
    for (CFANode node : nodes) {
      allAbstractStateCoveredNodes.add(node.getNodeNumber());
    }
  }

  void resetPredicateRelevantVariablesNodes() {
    previousPredicateRelevantVariablesNodesSize = allPredicateRelevantVariablesNodes.size();
    allPredicateRelevantVariablesNodes.clear();
  }
}
