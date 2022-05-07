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
  public final Set<Integer> allNodes = new LinkedHashSet<>();
  public final Multiset<Integer> allReachedNodes = LinkedHashMultiset.create();
  public final Set<Integer> allAbstractStateCoveredNodes = new LinkedHashSet<>();
  public final Multiset<Integer> visitedLines = LinkedHashMultiset.create();
  public final Set<Integer> allLines = new LinkedHashSet<>();
  public final Multiset<String> visitedFunctions = LinkedHashMultiset.create();
  public final Set<FunctionInfo> allFunctions = new LinkedHashSet<>();
  public final Set<AssumeEdge> allAssumes = new LinkedHashSet<>();
  public final Set<AssumeEdge> visitedAssumes = new LinkedHashSet<>();
  public final Multiset<Integer> visitedLocations = LinkedHashMultiset.create();

  public FilePredicateCoverageStatistics predicateStatistics =
      new FilePredicateCoverageStatistics();

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

  public void addExistingLine(int pLine) {
    checkArgument(pLine > 0);
    allLines.add(pLine);
  }

  public void addAbstractStateCoveredNodes(Set<CFANode> nodes) {
    for (CFANode node : nodes) {
      allAbstractStateCoveredNodes.add(node.getNodeNumber());
    }
  }

  public Multiset<Integer> getVisitedLines() {
    return visitedLines;
  }

  public int getVisitedLine(int pLine) {
    checkArgument(pLine > 0);
    return visitedLines.count(pLine);
  }
}
