// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.collectors;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageUtility;

/**
 * Abstract Class Coverage Collector is used as basis functionality for every Coverage Collector
 * Implementation. It provides methods to save coverage data per file. And has access to the
 * coverage measures and time-dependent coverage graphs.
 */
public abstract class CoverageCollector {
  private final Set<CFANode> allLocations = new LinkedHashSet<>();
  private final Map<String, Multiset<Integer>> visitedLinesPerFile = new HashMap<>();
  private final SetMultimap<String, Integer> existingLinesPerFile = HashMultimap.create();
  private final Map<String, Multiset<String>> visitedFunctionsPerFile = new HashMap<>();
  private final Set<FunctionInfo> allFunctions = new HashSet<>();
  private final SetMultimap<String, AssumeEdge> visitedAssumesPerFile = HashMultimap.create();
  private final Set<AssumeEdge> allAssumes = new HashSet<>();
  private final Set<String> allVariables = new HashSet<>();

  final CoverageMeasureHandler coverageMeasureHandler;
  final TimeDependentCoverageHandler timeDependentCoverageHandler;

  CoverageCollector(
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler,
      CFA pCFA) {
    coverageMeasureHandler = pCoverageMeasureHandler;
    timeDependentCoverageHandler = pTimeDependentCoverageHandler;
    putCFA(pCFA);
    allLocations.addAll(pCFA.getAllNodes());
  }

  CoverageCollector() {
    coverageMeasureHandler = new CoverageMeasureHandler();
    timeDependentCoverageHandler = new TimeDependentCoverageHandler();
  }

  public void addVisitedEdge(final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    putExistingEdge(pEdge);

    final FileLocation loc = pEdge.getFileLocation();
    final int startingLine = loc.getStartingLineInOrigin();
    final int endingLine = loc.getEndingLineInOrigin();
    String file = loc.getFileName().toString();

    if (pEdge instanceof AssumeEdge) {
      visitedAssumesPerFile.put(file, (AssumeEdge) pEdge);
    }

    for (int line = startingLine; line <= endingLine; line++) {
      Multiset<Integer> visitedLines = visitedLinesPerFile.get(file);
      if (visitedLines != null) {
        visitedLinesPerFile.get(file).add(line);
      } else {
        Multiset<Integer> newVisitedLines = HashMultiset.create();
        newVisitedLines.add(line);
        visitedLinesPerFile.put(file, newVisitedLines);
      }
    }
  }

  void addVisitedFunction(FunctionEntryNode pEntryNode) {
    String file = pEntryNode.getFileLocation().getFileName().toString();
    Multiset<String> visitedFunctions = visitedFunctionsPerFile.get(file);
    if (visitedFunctions != null) {
      visitedFunctionsPerFile.get(file).add(pEntryNode.getFunctionName());
    } else {
      Multiset<String> newVisitedFunctions = HashMultiset.create();
      newVisitedFunctions.add(pEntryNode.getFunctionName());
      visitedFunctionsPerFile.put(file, newVisitedFunctions);
    }
  }

  public void addAllProgramVariables() {
    for (CFANode node : allLocations) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        Optional<String> variable = getNewVariableFromCFAEdge(edge);
        if (variable.isPresent()) {
          allVariables.add(variable.orElseThrow());
        }
      }
    }
  }

  public Set<String> getAllVariables() {
    return Collections.unmodifiableSet(allVariables);
  }

  public Set<FunctionInfo> getAllFunctions() {
    return Collections.unmodifiableSet(allFunctions);
  }

  public Map<String, Multiset<String>> getVisitedFunctionsPerFile() {
    return visitedFunctionsPerFile;
  }

  public int getVisitedFunctionsCount() {
    return visitedFunctionsPerFile.values().stream()
        .map(x -> x.elementSet().size())
        .mapToInt(i -> i)
        .sum();
  }

  public Set<AssumeEdge> getAllAssumes() {
    return Collections.unmodifiableSet(allAssumes);
  }

  public int getVisitedAssumesCount() {
    return visitedAssumesPerFile.values().size();
  }

  public ImmutableSetMultimap<String, Integer> getExistingLinesPerFile() {
    return ImmutableSetMultimap.copyOf(existingLinesPerFile);
  }

  public ImmutableMap<String, ImmutableMultiset<Integer>> getVisitedLinesPerFile() {
    Map<String, ImmutableMultiset<Integer>> lVisitedLinesPerFile = new HashMap<>();
    for (Entry<String, Multiset<Integer>> entry : visitedLinesPerFile.entrySet()) {
      lVisitedLinesPerFile.put(entry.getKey(), ImmutableMultiset.copyOf(entry.getValue()));
    }
    return ImmutableMap.copyOf(lVisitedLinesPerFile);
  }

  public int getVisitedLinesCount() {
    return visitedLinesPerFile.values().stream()
        .map(x -> x.elementSet().size())
        .mapToInt(i -> i)
        .sum();
  }

  public int getExistingLinesCount() {
    return existingLinesPerFile.values().size();
  }

  public int getTotalLocationCount() {
    return allLocations.size();
  }

  Optional<String> getNewVariableFromCFAEdge(CFAEdge edge) {
    if (edge.getEdgeType() != CFAEdgeType.DeclarationEdge) {
      return Optional.empty();
    }
    String CPACHECKER_TMP_PREFIX = "__CPACHECKER_TMP";
    String SCOPE_SEPARATOR = "::";
    CDeclaration declaration = ((CDeclarationEdge) edge).getDeclaration();
    String functionName = edge.getPredecessor().getFunctionName();
    String origVariableName = declaration.getOrigName();
    String variableName = declaration.getName();
    String fileName =
        Iterators.getLast(
            Splitter.on('/').split(declaration.getFileLocation().getNiceFileName()).iterator());
    if (declaration instanceof CFunctionDeclaration
        || variableName == null
        || origVariableName == null
        || variableName.toUpperCase().startsWith(CPACHECKER_TMP_PREFIX)) {
      return Optional.empty();
    }
    if (declaration.isGlobal()) {
      return Optional.of(fileName + SCOPE_SEPARATOR + origVariableName);
    } else {
      return Optional.of(functionName + SCOPE_SEPARATOR + origVariableName);
    }
  }

  private void putCFA(CFA pCFA) {
    for (CFANode node : pCFA.getAllNodes()) {
      // This part adds lines, which are only on edges, such as "return" or "goto"
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        putExistingEdge(edge);
      }
    }
    for (FunctionEntryNode entryNode : pCFA.getAllFunctionHeads()) {
      putExistingFunction(entryNode);
    }
  }

  private void putExistingEdge(final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    final FileLocation loc = pEdge.getFileLocation();

    final int startingLine = loc.getStartingLineInOrigin();
    final int endingLine = loc.getEndingLineInOrigin();
    String file = loc.getFileName().toString();

    for (int line = startingLine; line <= endingLine; line++) {
      existingLinesPerFile.put(file, line);
    }

    if (pEdge instanceof AssumeEdge) {
      allAssumes.add((AssumeEdge) pEdge);
    }
  }

  private void putExistingFunction(FunctionEntryNode pNode) {
    final String functionName = pNode.getFunctionName();
    final FileLocation loc = pNode.getFileLocation();
    if (!loc.isRealLocation()) {
      return;
    }
    final int startingLine = loc.getStartingLineInOrigin();
    final int endingLine = loc.getEndingLineInOrigin();

    allFunctions.add(new FunctionInfo(functionName, startingLine, endingLine));
  }

  public static class FunctionInfo {
    private final String name;
    private final int firstLine;
    private final int lastLine;

    FunctionInfo(String pName, int pFirstLine, int pLastLine) {
      name = pName;
      firstLine = pFirstLine;
      lastLine = pLastLine;
    }

    public String getName() {
      return name;
    }

    public int getFirstLine() {
      return firstLine;
    }

    public int getLastLine() {
      return lastLine;
    }
  }
}
