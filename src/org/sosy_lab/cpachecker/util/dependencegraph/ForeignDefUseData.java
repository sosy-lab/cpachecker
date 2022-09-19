// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

final class ForeignDefUseData {

  private final ImmutableMap<AFunctionDeclaration, ImmutableSet<MemoryLocation>> foreignDefs;
  private final ImmutableMap<AFunctionDeclaration, ImmutableSet<MemoryLocation>> foreignUses;

  private ForeignDefUseData(
      ImmutableMap<AFunctionDeclaration, ImmutableSet<MemoryLocation>> pForeignDefs,
      ImmutableMap<AFunctionDeclaration, ImmutableSet<MemoryLocation>> pForeignUses) {

    foreignDefs = pForeignDefs;
    foreignUses = pForeignUses;
  }

  public ImmutableSet<MemoryLocation> getForeignDefs(AFunctionDeclaration pFunction) {
    return foreignDefs.get(pFunction);
  }

  public ImmutableSet<MemoryLocation> getForeignUses(AFunctionDeclaration pFunction) {
    return foreignUses.get(pFunction);
  }

  public static ForeignDefUseData extract(
      CFA pCfa, EdgeDefUseData.Extractor pDefUseExtractor, GlobalPointerState pPointerState) {

    List<CFAEdge> edges = new ArrayList<>();
    for (CFANode node : pCfa.getAllNodes()) {
      Iterables.addAll(edges, CFAUtils.allLeavingEdges(node));
    }

    Map<AFunctionDeclaration, Set<AFunctionDeclaration>> calledFunctions = new HashMap<>();
    for (CFAEdge edge : edges) {
      if (edge instanceof CFunctionSummaryEdge) {
        CFunctionSummaryEdge summaryEdge = (CFunctionSummaryEdge) edge;
        AFunctionDeclaration function = summaryEdge.getPredecessor().getFunction();
        AFunctionDeclaration calledFunction = summaryEdge.getFunctionEntry().getFunction();
        calledFunctions.computeIfAbsent(function, key -> new HashSet<>()).add(calledFunction);
      }
    }

    Map<AFunctionDeclaration, Set<MemoryLocation>> foreignDefs = new HashMap<>();
    Map<AFunctionDeclaration, Set<MemoryLocation>> foreignUses = new HashMap<>();

    collectDirectForeign(foreignDefs, foreignUses, edges, pDefUseExtractor, pPointerState);
    collectIndirectForeign(foreignDefs, foreignUses, calledFunctions);

    return new ForeignDefUseData(createImmutable(foreignDefs), createImmutable(foreignUses));
  }

  private static <K, V> ImmutableMap<K, ImmutableSet<V>> createImmutable(Map<K, Set<V>> pMap) {

    var mapBuilder = ImmutableMap.<K, ImmutableSet<V>>builderWithExpectedSize(pMap.size());
    for (Map.Entry<K, Set<V>> entry : pMap.entrySet()) {
      mapBuilder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
    }

    return mapBuilder.buildOrThrow();
  }

  private static boolean isForeignMemoryLocation(
      MemoryLocation pMemoryLocation, AFunctionDeclaration pFunction) {

    return !pMemoryLocation.isOnFunctionStack()
        || !pMemoryLocation.getFunctionName().equals(pFunction.getQualifiedName());
  }

  private static void collectForeignMemoryLocations(
      AFunctionDeclaration pFunction,
      CFAEdge pEdge,
      Set<CExpression> pPointees,
      GlobalPointerState pPointerState,
      Set<MemoryLocation> pForeignMemoryLocations) {

    for (CExpression expression : pPointees) {

      Set<MemoryLocation> possibleDefs = pPointerState.getPossiblePointees(pEdge, expression);
      assert possibleDefs != null && !possibleDefs.isEmpty() : "No possible pointees";

      for (MemoryLocation memoryLocation : possibleDefs) {
        if (isForeignMemoryLocation(memoryLocation, pFunction)) {
          pForeignMemoryLocations.add(memoryLocation);
        }
      }
    }
  }

  private static void collectDirectForeign(
      Map<AFunctionDeclaration, Set<MemoryLocation>> pForeignDefs,
      Map<AFunctionDeclaration, Set<MemoryLocation>> pForeignUses,
      List<CFAEdge> pEdges,
      EdgeDefUseData.Extractor pDefUseExtractor,
      GlobalPointerState pPointerState) {

    for (CFAEdge edge : pEdges) {

      AFunctionDeclaration function = edge.getPredecessor().getFunction();
      EdgeDefUseData defUseData = pDefUseExtractor.extract(edge);

      Set<MemoryLocation> foreignDefs =
          pForeignDefs.computeIfAbsent(function, key -> new HashSet<>());
      collectForeignMemoryLocations(
          function, edge, defUseData.getPointeeDefs(), pPointerState, foreignDefs);
      for (MemoryLocation def : defUseData.getDefs()) {
        if (!def.isOnFunctionStack()) {
          foreignDefs.add(def);
        }
      }

      Set<MemoryLocation> foreignUses =
          pForeignUses.computeIfAbsent(function, key -> new HashSet<>());
      collectForeignMemoryLocations(
          function, edge, defUseData.getPointeeUses(), pPointerState, foreignUses);
      for (MemoryLocation use : defUseData.getUses()) {
        if (!use.isOnFunctionStack()) {
          foreignUses.add(use);
        }
      }
    }
  }

  private static void collectIndirectForeign(
      Map<AFunctionDeclaration, Set<MemoryLocation>> pForeignDefs,
      Map<AFunctionDeclaration, Set<MemoryLocation>> pForeignUses,
      Map<AFunctionDeclaration, Set<AFunctionDeclaration>> pCalledFunctions) {

    boolean changed = true;
    while (changed) {
      changed = false;

      for (Map.Entry<AFunctionDeclaration, Set<AFunctionDeclaration>> entry :
          pCalledFunctions.entrySet()) {

        AFunctionDeclaration function = entry.getKey();

        Set<MemoryLocation> functionDefs = pForeignDefs.get(function);
        for (AFunctionDeclaration calledFunction : entry.getValue()) {

          Set<MemoryLocation> calledFunctionDefs = pForeignDefs.get(calledFunction);

          for (MemoryLocation defVar : calledFunctionDefs) {
            if (isForeignMemoryLocation(defVar, function)) {
              if (functionDefs.add(defVar)) {
                changed = true;
              }
            }
          }

          Set<MemoryLocation> functionUses = pForeignUses.get(function);
          Set<MemoryLocation> calledFunctionUses = pForeignUses.get(calledFunction);

          for (MemoryLocation useVar : calledFunctionUses) {
            if (isForeignMemoryLocation(useVar, function)) {
              if (functionUses.add(useVar)) {
                changed = true;
              }
            }
          }
        }
      }
    }
  }

  @Override
  public String toString() {
    return String.format("[foreign-defs: %s, foreign-uses: %s]", foreignDefs, foreignUses);
  }
}
