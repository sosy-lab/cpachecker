// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.Pair;

public class ASTStructure {

  private final ImmutableSet<IfStructure> ifStructures;

  private final ImmutableSet<IterationStructure> iterationStructures;

  private final ImmutableMap<Integer, FileLocation> statementOffsetsToLocations;

  @LazyInit private ImmutableMap<CFAEdge, IfStructure> conditionEdgesToIfStructure = null;

  private final Map<Pair<Integer, Integer>, IfStructure> lineAndStartColumnToIfStructure =
      new HashMap<>();

  public ASTStructure(
      ImmutableSet<IfStructure> pIfStructures,
      ImmutableSet<IterationStructure> pIterationStructures,
      ImmutableMap<Integer, FileLocation> pStatementOffsetsToLocations) {
    ifStructures = pIfStructures;
    iterationStructures = pIterationStructures;
    statementOffsetsToLocations = pStatementOffsetsToLocations;
  }

  public FileLocation nextStartStatementLocation(Integer offset) {
    for (int counter = offset;
        counter < Collections.max(statementOffsetsToLocations.keySet());
        counter++) {
      if (statementOffsetsToLocations.containsKey(counter)) {
        return statementOffsetsToLocations.get(counter);
      }
    }
    return null;
  }

  private void initializeMapFromConditionEdgesToIfStructures() {
    if (conditionEdgesToIfStructure != null) {
      return;
    }
    Builder<CFAEdge, IfStructure> builder = new Builder<>();
    for (IfStructure structure : ifStructures) {
      for (CFAEdge edge : structure.getConditionElement().edges()) {
        builder.put(edge, structure);
      }
    }
    conditionEdgesToIfStructure = builder.buildOrThrow();
  }

  public IfStructure getIfStructureForConditionEdge(CFAEdge pEdge) {
    if (conditionEdgesToIfStructure == null) {
      initializeMapFromConditionEdgesToIfStructures();
    }
    return conditionEdgesToIfStructure.getOrDefault(pEdge, null);
  }

  public Optional<IterationStructure> getTightestIterationStructureForNode(CFANode pNode) {
    Optional<IterationStructure> result = Optional.empty();
    for (IterationStructure structure : iterationStructures) {
      if (structure.getCompleteElement().edges().stream()
          .anyMatch(pEdge -> pEdge.getPredecessor() == pNode || pEdge.getSuccessor() == pNode)) {
        if (result.isPresent()) {
          if (result
              .orElseThrow()
              .getCompleteElement()
              .edges()
              .containsAll(structure.getCompleteElement().edges())) {
            result = Optional.of(structure);
          }
        } else {
          result = Optional.of(structure);
        }
      }
    }
    return result;
  }

  public IfStructure getIfStructureStartingAtColumn(Integer pColumn, Integer pLine) {
    Pair<Integer, Integer> key = Pair.of(pColumn, pLine);
    if (lineAndStartColumnToIfStructure.containsKey(key)) {
      return lineAndStartColumnToIfStructure.get(key);
    }

    for (IfStructure structure : ifStructures) {
      FileLocation location = structure.getCompleteElement().location();
      if (location.getStartColumnInLine() == pColumn && location.getStartingLineNumber() == pLine) {
        lineAndStartColumnToIfStructure.put(key, structure);
        return structure;
      }
    }

    return null;
  }
}
