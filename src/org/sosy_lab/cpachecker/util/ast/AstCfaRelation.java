// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.Pair;

/** Contains information relating the CFA to the AST of the program. */
public class AstCfaRelation {

  private record StartingLocation(int column, int line) implements Comparable<StartingLocation> {
    @Override
    public int compareTo(StartingLocation pStartingLocation) {
      if (line != pStartingLocation.line) {
        return Integer.compare(line, pStartingLocation.line);
      } else {
        return Integer.compare(column, pStartingLocation.column);
      }
    }
  }

  private final ImmutableSet<IfElement> ifElements;

  private final ImmutableSet<IterationElement> iterationStructures;

  private ImmutableSet<StatementElement> statementElements;

  @LazyInit
  private ImmutableSortedMap<StartingLocation, ASTElement> startingLocationToTightestStatement =
      null;

  private final ImmutableSortedMap<Integer, FileLocation> statementOffsetsToLocations;

  @LazyInit private ImmutableMap<CFAEdge, IfElement> conditionEdgesToIfStructure = null;

  @LazyInit
  private ImmutableMap<Pair<Integer, Integer>, IfElement> lineAndStartColumnToIfStructure = null;

  public AstCfaRelation(
      ImmutableSet<IfElement> pIfElements,
      ImmutableSet<IterationElement> pIterationStructures,
      ImmutableSortedMap<Integer, FileLocation> pStatementOffsetsToLocations,
      ImmutableSet<StatementElement> pStatementElements) {
    ifElements = pIfElements;
    iterationStructures = pIterationStructures;
    statementOffsetsToLocations = pStatementOffsetsToLocations;
    statementElements = pStatementElements;
  }

  /**
   * Returns the next location after the given offset at which a statement starts.
   *
   * @param offset the offset to start from
   * @return the next location at which a statement starts after the given offset
   */
  public FileLocation nextStartStatementLocation(Integer offset) {
    return Objects.requireNonNull(statementOffsetsToLocations.ceilingEntry(offset)).getValue();
  }

  private void initializeMapFromConditionEdgesToIfStructures() {
    if (conditionEdgesToIfStructure != null) {
      return;
    }
    ImmutableMap.Builder<CFAEdge, IfElement> builder = new ImmutableMap.Builder<>();
    for (IfElement structure : ifElements) {
      for (CFAEdge edge : structure.getConditionElement().edges()) {
        builder.put(edge, structure);
      }
    }
    conditionEdgesToIfStructure = builder.buildOrThrow();
  }

  /**
   * Returns the IfElement that contains the given edge as a condition.
   *
   * @param pEdge the edge to look for
   * @return the IfElement that contains the given edge as a condition
   */
  public IfElement getIfStructureForConditionEdge(CFAEdge pEdge) {
    if (conditionEdgesToIfStructure == null) {
      initializeMapFromConditionEdgesToIfStructures();
    }
    return conditionEdgesToIfStructure.getOrDefault(pEdge, null);
  }

  /**
   * Returns the tightest iteration structure that contains the given node. This means that if two
   * loops contain the same node and one contains a subset of the edges of the other, then the
   * subset loop is returned.
   *
   * <p>There is no guarantee what happens when more than one loop contains the given node and their
   * edges are not in a strict subset relation.
   *
   * @param pNode the node to look for
   * @return the tightest iteration structure that contains the given node
   */
  public Optional<IterationElement> getTightestIterationStructureForNode(CFANode pNode) {
    Optional<IterationElement> result = Optional.empty();
    for (IterationElement structure : iterationStructures) {
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

  private void initializeMapFromLineAndStartColumnToIfStructure() {
    if (lineAndStartColumnToIfStructure != null) {
      return;
    }
    ImmutableMap.Builder<Pair<Integer, Integer>, IfElement> builder = new ImmutableMap.Builder<>();
    for (IfElement structure : ifElements) {
      FileLocation location = structure.getCompleteElement().location();
      Pair<Integer, Integer> key =
          Pair.of(location.getStartColumnInLine(), location.getStartingLineNumber());
      builder.put(key, structure);
    }
    lineAndStartColumnToIfStructure = builder.buildOrThrow();
  }

  /**
   * Returns the IfElement that starts at the given column and line.
   *
   * @param pColumn the column to look for
   * @param pLine the line to look for
   * @return the IfElement that starts at the given column and line
   */
  public Optional<IfElement> getIfStructureStartingAtColumn(Integer pColumn, Integer pLine) {
    if (lineAndStartColumnToIfStructure == null) {
      initializeMapFromLineAndStartColumnToIfStructure();
    }

    Pair<Integer, Integer> key = Pair.of(pColumn, pLine);
    if (lineAndStartColumnToIfStructure.containsKey(key)) {
      return Optional.ofNullable(lineAndStartColumnToIfStructure.get(key));
    }

    return Optional.empty();
  }

  private void initializeMapFromStartingLocationToTightestStatement() {
    if (startingLocationToTightestStatement != null) {
      return;
    }
    ImmutableSortedMap.Builder<StartingLocation, ASTElement> builder =
        ImmutableSortedMap.naturalOrder();
    for (StatementElement element : statementElements) {
      StartingLocation key =
          new StartingLocation(
              element.getCompleteElement().location().getStartColumnInLine(),
              element.getCompleteElement().location().getStartingLineNumber());
      builder.put(key, element.getCompleteElement());
    }
    startingLocationToTightestStatement = builder.buildOrThrow();
  }

  public ASTElement getTightestStatementForStarting(int pLine, int pColumn) {
    if (startingLocationToTightestStatement == null) {
      initializeMapFromStartingLocationToTightestStatement();
    }

    return Objects.requireNonNull(
            startingLocationToTightestStatement.floorEntry(new StartingLocation(pColumn, pLine)))
        .getValue();
  }
}
