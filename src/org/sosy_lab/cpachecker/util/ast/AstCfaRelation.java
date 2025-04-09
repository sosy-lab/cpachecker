// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

/** Contains information relating the CFA to the AST of the program. */
public final class AstCfaRelation {

  private record StartingLocation(int column, int line) implements Comparable<StartingLocation> {
    @Override
    public int compareTo(StartingLocation pStartingLocation) {
      return ComparisonChain.start()
          .compare(line, pStartingLocation.line)
          .compare(column, pStartingLocation.column)
          .result();
    }
  }

  private final ImmutableSet<IfElement> ifElements;

  private final ImmutableSet<IterationElement> iterationStructures;

  private final ImmutableSet<StatementElement> statementElements;

  private final ImmutableSortedSet<FileLocation> expressionLocations;

  @LazyInit
  private ImmutableSortedMap<StartingLocation, ASTElement> startingLocationToTightestStatement =
      null;

  private final ImmutableSortedMap<Integer, FileLocation> statementOffsetsToLocations;

  @LazyInit private ImmutableMap<CFAEdge, IfElement> conditionEdgesToIfStructure = null;

  @LazyInit
  private ImmutableMap<Pair<Integer, Integer>, IfElement> lineAndStartColumnToIfStructure = null;

  @LazyInit
  private ImmutableMap<StartingLocation, IterationElement> lineAndStartColumnToIterationStructure =
      null;

  // Static variables are currently not being considered, since it is somewhat unclear how to handle
  // them.
  private final Map<CFANode, Set<AVariableDeclaration>> cfaNodeToAstLocalVariablesInScope;
  private final Map<CFANode, Set<AParameterDeclaration>> cfaNodeToAstParametersInScope;
  private final Set<AVariableDeclaration> globalVariables;

  public AstCfaRelation(
      ImmutableSet<IfElement> pIfElements,
      ImmutableSet<IterationElement> pIterationStructures,
      ImmutableSortedMap<Integer, FileLocation> pStatementOffsetsToLocations,
      ImmutableSet<StatementElement> pStatementElements,
      Map<CFANode, Set<AVariableDeclaration>> pCfaNodeToAstLocalVariablesInScope,
      Map<CFANode, Set<AParameterDeclaration>> pCfaNodeToAstParametersVariablesInScope,
      Set<AVariableDeclaration> pGlobalVariables,
      ImmutableSortedSet<FileLocation> pExpressionLocations) {
    ifElements = pIfElements;
    iterationStructures = pIterationStructures;
    statementOffsetsToLocations = pStatementOffsetsToLocations;
    statementElements = pStatementElements;
    cfaNodeToAstLocalVariablesInScope = pCfaNodeToAstLocalVariablesInScope;
    cfaNodeToAstParametersInScope = pCfaNodeToAstParametersVariablesInScope;
    globalVariables = pGlobalVariables;
    expressionLocations = pExpressionLocations;
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
  public Optional<IfElement> getIfStructureForConditionEdge(CFAEdge pEdge) {
    if (conditionEdgesToIfStructure == null) {
      initializeMapFromConditionEdgesToIfStructures();
    }

    IfElement result = conditionEdgesToIfStructure.getOrDefault(pEdge, null);

    return Optional.ofNullable(result);
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

  /**
   * Returns the node that starts the iteration statement at the given line and column.
   *
   * @param line the line at which the iteration statement starts
   * @param column the column at which the iteration statement starts
   * @return the node that starts the iteration statement at the given line and column if it could
   *     be uniquely determined
   */
  public Optional<CFANode> getNodeForIterationStatementLocation(int line, int column) {
    for (IterationElement structure : iterationStructures) {
      if (structure.getCompleteElement().location().getStartingLineNumber() == line
          && structure.getCompleteElement().location().getStartColumnInLine() == column) {
        return structure.getLoopHead();
      }
    }
    return Optional.empty();
  }

  /**
   * Returns the node that starts the statement at the given line and column.
   *
   * @param line the line at which the iteration statement starts
   * @param column the column at which the iteration statement starts
   * @return the node that starts the iteration statement at the given line and column if it could
   *     be uniquely determined
   */
  public Optional<CFANode> getNodeForStatementLocation(int line, int column) {
    ASTElement statement =
        Objects.requireNonNull(
                startingLocationToTightestStatement.floorEntry(new StartingLocation(column, line)))
            .getValue();

    if (statement.location().getStartingLineNumber() != line
        || statement.location().getStartColumnInLine() != column) {
      // We only want to match the exact starting location of the statement
      return Optional.empty();
    }

    return statement.edges().stream().map(CFAEdge::getPredecessor).findFirst();
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

  private void initializeMapFromLineAndStartColumnToIterationStructure() {
    if (lineAndStartColumnToIterationStructure != null) {
      return;
    }
    ImmutableMap.Builder<StartingLocation, IterationElement> builder = new ImmutableMap.Builder<>();
    for (IterationElement structure : iterationStructures) {
      FileLocation location = structure.getCompleteElement().location();
      StartingLocation key =
          new StartingLocation(location.getStartColumnInLine(), location.getStartingLineNumber());
      builder.put(key, structure);
    }
    lineAndStartColumnToIterationStructure = builder.buildOrThrow();
  }

  /**
   * Returns the IterationElement that starts at the given column and line.
   *
   * @param pColumn the column
   * @param pLine the line
   * @return the IterationElement that starts at the given column and line
   */
  public Optional<IterationElement> getIterationStructureStartingAtColumn(
      Integer pColumn, Integer pLine) {
    if (lineAndStartColumnToIterationStructure == null) {
      initializeMapFromLineAndStartColumnToIterationStructure();
    }

    StartingLocation key = new StartingLocation(pColumn, pLine);
    if (lineAndStartColumnToIterationStructure.containsKey(key)) {
      return Optional.ofNullable(lineAndStartColumnToIterationStructure.get(key));
    }

    return Optional.empty();
  }

  /**
   * Get the expression whose location has an offset which is greater than or equal to the one of
   * the location being provided and whose offset is closer than all other expressions to the given
   * offset
   *
   * @param pLocation The location for which an expression is being searched for
   * @return the location of the expression whose offset is greater than or equal to the one of *
   *     the location being provided and whose offset is closer than all other expressions to the
   *     given offset
   */
  public Optional<FileLocation> getNextExpressionLocationBasedOnOffset(FileLocation pLocation) {
    return Optional.ofNullable(expressionLocations.ceiling(pLocation));
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

  public FluentIterable<AbstractSimpleDeclaration> getVariablesAndParametersInScope(CFANode pNode) {
    return FluentIterable.concat(
        Objects.requireNonNull(cfaNodeToAstLocalVariablesInScope.get(pNode)),
        Objects.requireNonNull(cfaNodeToAstParametersInScope.get(pNode)),
        globalVariables);
  }

  public Optional<FileLocation> getStatementFileLocationForNode(CFANode pNode) {
    if (startingLocationToTightestStatement == null) {
      initializeMapFromStartingLocationToTightestStatement();
    }

    if (pNode.getNumLeavingEdges() != 0) {
      FileLocation closestFileLocationToNode =
          CFAUtils.allLeavingEdges(pNode).transform(CFAEdge::getFileLocation).stream()
              .min(Comparator.naturalOrder())
              .orElseThrow();
      StartingLocation closestStartingLocationToNode =
          new StartingLocation(
              closestFileLocationToNode.getStartColumnInLine(),
              closestFileLocationToNode.getStartingLineNumber());

      Entry<StartingLocation, ASTElement> element =
          startingLocationToTightestStatement.floorEntry(closestStartingLocationToNode);

      // Could happen for example for the first node
      if (element == null) {
        return Optional.empty();
      }

      return Optional.of(element.getValue().location());
    } else if (pNode.getNumEnteringEdges() != 0) {
      FileLocation closestFileLocationToNode =
          CFAUtils.allLeavingEdges(pNode).transform(CFAEdge::getFileLocation).stream()
              .max(Comparator.naturalOrder())
              .orElseThrow();
      StartingLocation closestStartingLocationToNode =
          new StartingLocation(
              closestFileLocationToNode.getStartColumnInLine(),
              closestFileLocationToNode.getStartingLineNumber());
      Entry<StartingLocation, ASTElement> element =
          startingLocationToTightestStatement.ceilingEntry(closestStartingLocationToNode);

      // Could happen for example for the last node
      if (element == null) {
        return Optional.empty();
      }

      return Optional.of(element.getValue().location());
    } else {
      // Could happen if a node is not connected to the CFA
      return Optional.empty();
    }
  }
}
