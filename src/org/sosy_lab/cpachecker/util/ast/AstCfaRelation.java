// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CfaCopyOrigins;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.LoopStructure;

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
  private ImmutableSortedMap<StartingLocation, IfElement> lineAndStartColumnToIfStructure = null;

  @LazyInit
  private ImmutableSortedMap<StartingLocation, IterationElement>
      lineAndStartColumnToIterationStructure = null;

  // Static variables are currently not being considered, since it is somewhat unclear how to handle
  // them.
  // As in the input program: without the variables that go out of scope at a node and without the
  // ones hidden by an inner declaration with the same name.
  private final ImmutableMap<CFANode, Set<AVariableDeclaration>> cfaNodeToAstLocalVariablesInScope;
  private final ImmutableMap<CFANode, Set<AParameterDeclaration>> cfaNodeToAstParametersInScope;
  // All declared ones, as recorded by the CFA builder, which gives a node the scope in which it was
  // created, e.g., the node after a for loop still has the variable of the loop.
  private final ImmutableMap<CFANode, Set<AVariableDeclaration>> cfaNodeToDeclaredLocalVariables;
  private final ImmutableMap<CFANode, Set<AParameterDeclaration>> cfaNodeToDeclaredParameters;
  private final ImmutableSet<AVariableDeclaration> globalVariables;

  // A post-processing of the CFA can copy nodes and edges, for example when a loop is unrolled.
  // The AST elements below are filled with the edges of the CFA as it is in the end, so they are
  // about the copies, while the maps that the parser filled are about the program. This field
  // connects both.
  private final CfaCopyOrigins copyOrigins;

  public AstCfaRelation(
      ImmutableSet<IfElement> pIfElements,
      ImmutableSet<IterationElement> pIterationStructures,
      ImmutableSortedMap<Integer, FileLocation> pStatementOffsetsToLocations,
      ImmutableSet<StatementElement> pStatementElements,
      ImmutableMap<CFANode, Set<AVariableDeclaration>> pCfaNodeToAstLocalVariablesInScope,
      ImmutableMap<CFANode, Set<AParameterDeclaration>> pCfaNodeToAstParametersVariablesInScope,
      ImmutableMap<CFANode, Set<AVariableDeclaration>> pCfaNodeToDeclaredLocalVariables,
      ImmutableMap<CFANode, Set<AParameterDeclaration>> pCfaNodeToDeclaredParameters,
      ImmutableSet<AVariableDeclaration> pGlobalVariables,
      ImmutableSortedSet<FileLocation> pExpressionLocations) {
    ifElements = pIfElements;
    iterationStructures = pIterationStructures;
    statementOffsetsToLocations = pStatementOffsetsToLocations;
    statementElements = pStatementElements;
    cfaNodeToAstLocalVariablesInScope = pCfaNodeToAstLocalVariablesInScope;
    cfaNodeToAstParametersInScope = pCfaNodeToAstParametersVariablesInScope;
    cfaNodeToDeclaredLocalVariables = pCfaNodeToDeclaredLocalVariables;
    cfaNodeToDeclaredParameters = pCfaNodeToDeclaredParameters;
    globalVariables = pGlobalVariables;
    expressionLocations = pExpressionLocations;
    copyOrigins = new CfaCopyOrigins();
  }

  private AstCfaRelation(AstCfaRelation pOther, CfaCopyOrigins pCopyOrigins) {
    ifElements = pOther.ifElements;
    iterationStructures = pOther.iterationStructures;
    statementOffsetsToLocations = pOther.statementOffsetsToLocations;
    statementElements = pOther.statementElements;
    cfaNodeToAstLocalVariablesInScope = pOther.cfaNodeToAstLocalVariablesInScope;
    cfaNodeToAstParametersInScope = pOther.cfaNodeToAstParametersInScope;
    cfaNodeToDeclaredLocalVariables = pOther.cfaNodeToDeclaredLocalVariables;
    cfaNodeToDeclaredParameters = pOther.cfaNodeToDeclaredParameters;
    globalVariables = pOther.globalVariables;
    expressionLocations = pOther.expressionLocations;
    copyOrigins = pCopyOrigins;
  }

  /**
   * This relation, resolving the nodes and edges that a post-processing of the CFA copied through
   * the given origins.
   */
  public AstCfaRelation withCopyOrigins(CfaCopyOrigins pCopyOrigins) {
    return new AstCfaRelation(this, pCopyOrigins);
  }

  /**
   * The node of the program that the given node stands for, which is the node itself unless a
   * post-processing of the CFA copied it.
   */
  public CFANode programNodeOf(CFANode pNode) {
    return copyOrigins.originalOf(pNode);
  }

  /**
   * The edge of the program that the given edge stands for, which is the edge itself unless a
   * post-processing of the CFA copied it. A copy does not have to be of the same kind: an unrolling
   * replaces the condition of a loop by a blank edge in every iteration, although the program
   * branches there.
   */
  public CFAEdge programEdgeOf(CFAEdge pEdge) {
    return copyOrigins.originalOf(pEdge);
  }

  /**
   * Whether the given node of the CFA is a copy that stands for the head of a loop of the program,
   * for example one of the copies that an unrolling of that loop made for its iterations. The CFA
   * does not have that loop anymore, so only this says that the program comes back to this place.
   */
  private boolean standsForALoopHead(CFANode pNode) {
    return copyOrigins.standsForALoopHead(pNode);
  }

  /**
   * Whether a block of a basic-block aggregation has to end at the given node, although the shape
   * of the CFA does not say so. This is the case at a node that stands for a loop head whose copies
   * were asked to keep being places at which an analysis has a state.
   */
  public boolean endsABlock(CFANode pNode) {
    return copyOrigins.endsABlock(pNode);
  }

  /**
   * Set the edges of the CFA that belong to the AST elements. This is done after finalizing the
   * CFA, since we first need to finish building the CFA before we know about all the edges due to
   * edges being added to the CFA like for example with function cloning for concurrency.
   */
  public void setEdgesForAstElements(ImmutableSet<CFAEdge> pEdges) {
    for (IfElement structure : ifElements) {
      structure.setEdges(pEdges);
    }
    for (IterationElement structure : iterationStructures) {
      structure.setEdges(pEdges);
    }
    for (StatementElement structure : statementElements) {
      structure.setEdges(pEdges);
    }
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
    // The iteration structures contain the nodes of the original program, so we translate
    // potentially copied nodes
    CFANode programNode = programNodeOf(pNode);
    Optional<IterationElement> result = Optional.empty();
    for (IterationElement structure : iterationStructures) {
      if (structure.getCompleteElement().edges().stream()
          .anyMatch(
              pEdge ->
                  programNodeOf(pEdge.getPredecessor()).equals(programNode)
                      || programNodeOf(pEdge.getSuccessor()).equals(programNode))) {
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

  /** Returns the iteration statement whose body is the given element, if there is one. */
  public Optional<IterationElement> getIterationStructureWithBody(ASTElement pBody) {
    return iterationStructures.stream()
        .filter(structure -> structure.getBody().equals(pBody))
        .findFirst();
  }

  /**
   * Returns the nodes of the CFA at which the invariants of the iteration statement starting at the
   * given line and column hold, i.e., the head of its loop.
   *
   * <p>A post-processing of the CFA may have replaced that loop by copies of its body, for example
   * when it unrolled it. The CFA then has no head for it anymore, and the invariants hold at every
   * node that stands for one: the program is at the head of its loop at each of them.
   *
   * @param line the line at which the iteration statement starts
   * @param column the column at which the iteration statement starts
   * @return the nodes at which the invariants hold, which is empty if they could not be determined
   */
  public ImmutableSet<CFANode> getNodesForIterationStatementLocation(
      int line, int column, LoopStructure pLoopStructure) {
    for (IterationElement structure : iterationStructures) {
      if (structure.getCompleteElement().location().getStartingLineInOrigin() == line
          && structure.getCompleteElement().location().getStartColumnInLine() == column) {
        Optional<CFANode> head = structure.getLoopHead(pLoopStructure);
        return head.isPresent()
            ? ImmutableSet.of(head.orElseThrow())
            : nodesStandingForTheLoopHead(structure);
      }
    }
    return ImmutableSet.of();
  }

  /**
   * The nodes of the CFA that stand for the head of the loop of the given iteration statement,
   * which a post-processing of the CFA left in its place when it, for example, unrolled that loop.
   * This is empty while the loop itself is still in the CFA.
   */
  private ImmutableSet<CFANode> nodesStandingForTheLoopHead(IterationElement pLoop) {
    return pLoop
        .getControllingExpression()
        .map(
            condition ->
                FluentIterable.from(condition.edges())
                    .transform(CFAEdge::getPredecessor)
                    .filter(this::standsForALoopHead)
                    .toSet())
        .orElse(ImmutableSet.of());
  }

  /**
   * Returns the node at which the invariants of the loop of the given iteration statement hold,
   * i.e., the node given by {@link IterationElement#getLoopHead}, if the statement is a loop of the
   * given loop structure.
   *
   * <p>A post-processing of the CFA may have replaced that loop by copies of its body, for example
   * when it unrolled it. The loop structure then has no head for it anymore, and the node of the
   * program that the copies of its head stand for is the one the invariants belong to.
   */
  public Optional<CFANode> getLoopInvariantNode(
      IterationElement pLoop, LoopStructure pLoopStructure) {
    Optional<CFANode> head = pLoop.getLoopHead(pLoopStructure);
    if (head.isPresent()) {
      return head;
    }

    ImmutableSet<CFANode> headsOfTheProgram =
        transformedImmutableSetCopy(nodesStandingForTheLoopHead(pLoop), this::programNodeOf);

    return headsOfTheProgram.size() == 1
        ? Optional.of(Iterables.getOnlyElement(headsOfTheProgram))
        : Optional.empty();
  }

  /**
   * Returns the nodes at which loop invariants hold, i.e., the nodes given by {@link
   * #getLoopInvariantNode} for all iteration statements.
   */
  public ImmutableSet<CFANode> getLoopInvariantNodes(LoopStructure pLoopStructure) {
    return FluentIterable.from(iterationStructures)
        .transformAndConcat(loop -> getLoopInvariantNode(loop, pLoopStructure).stream().toList())
        .toSet();
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
    if (startingLocationToTightestStatement == null) {
      initializeMapFromStartingLocationToTightestStatement();
    }

    ASTElement statement =
        Objects.requireNonNull(
                startingLocationToTightestStatement.floorEntry(new StartingLocation(column, line)))
            .getValue();

    if (statement.location().getStartingLineInOrigin() != line
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
    ImmutableSortedMap.Builder<StartingLocation, IfElement> builder =
        new ImmutableSortedMap.Builder<>(StartingLocation::compareTo);
    for (IfElement structure : ifElements) {
      FileLocation location = structure.getCompleteElement().location();
      StartingLocation key =
          new StartingLocation(location.getStartColumnInLine(), location.getStartingLineInOrigin());
      builder.put(key, structure);
    }
    lineAndStartColumnToIfStructure = builder.buildOrThrow();
  }

  /**
   * Returns the IfElement that starts after the given column at the same line.
   *
   * @param pColumn the column to look for. In case of empty, the first IfElement at the given line
   *     is returned
   * @param pLine the line to look for
   * @return the IfElement that starts after the given column at the same line
   */
  public Optional<IfElement> getIfStructureFollowingColumnAtTheSameLine(
      Integer pLine, OptionalInt pColumn) {
    if (lineAndStartColumnToIfStructure == null) {
      initializeMapFromLineAndStartColumnToIfStructure();
    }

    StartingLocation key = new StartingLocation(pColumn.orElse(-1), pLine);
    Entry<StartingLocation, IfElement> startingLocationIfElementEntry =
        lineAndStartColumnToIfStructure.ceilingEntry(key);
    if (startingLocationIfElementEntry != null
        // The lines must be equal, for the location to match the requested one
        && startingLocationIfElementEntry.getKey().line == key.line) {
      return Optional.ofNullable(startingLocationIfElementEntry.getValue());
    }

    return Optional.empty();
  }

  private void initializeMapFromLineAndStartColumnToIterationStructure() {
    if (lineAndStartColumnToIterationStructure != null) {
      return;
    }
    ImmutableSortedMap.Builder<StartingLocation, IterationElement> builder =
        new ImmutableSortedMap.Builder<>(StartingLocation::compareTo);
    for (IterationElement structure : iterationStructures) {
      FileLocation location = structure.getCompleteElement().location();
      StartingLocation key =
          new StartingLocation(location.getStartColumnInLine(), location.getStartingLineInOrigin());
      builder.put(key, structure);
    }
    lineAndStartColumnToIterationStructure = builder.buildOrThrow();
  }

  /**
   * Returns the IterationElement that starts after the given column at the same line.
   *
   * @param pColumn the column to look for. In case of empty, the first IterationElement at the
   *     given line is returned
   * @param pLine the line to look for
   * @return the IterationElement that starts after the given column at the same line
   */
  public Optional<IterationElement> getIterationStructureFollowingColumnAtTheSameLine(
      OptionalInt pColumn, Integer pLine) {
    if (lineAndStartColumnToIterationStructure == null) {
      initializeMapFromLineAndStartColumnToIterationStructure();
    }

    StartingLocation key = new StartingLocation(pColumn.orElse(-1), pLine);
    Entry<StartingLocation, IterationElement> startingLocationIterationElementEntry =
        lineAndStartColumnToIterationStructure.ceilingEntry(key);
    if (startingLocationIterationElementEntry != null
        // The lines must be equal, for the location to match the requested one
        && startingLocationIterationElementEntry.getKey().line == key.line) {
      return Optional.ofNullable(startingLocationIterationElementEntry.getValue());
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
              element.getCompleteElement().location().getStartingLineInOrigin());
      builder.put(key, element.getCompleteElement());
    }
    startingLocationToTightestStatement = builder.buildOrThrow();
  }

  public Optional<ASTElement> getTightestStatementForStarting(int pLine, OptionalInt pColumn) {
    if (startingLocationToTightestStatement == null) {
      initializeMapFromStartingLocationToTightestStatement();
    }

    if (pColumn.isPresent()) {
      StartingLocation key = new StartingLocation(pColumn.orElseThrow(), pLine);
      Entry<StartingLocation, ASTElement> startingLocationASTElementEntry =
          startingLocationToTightestStatement.floorEntry(key);
      if (startingLocationASTElementEntry != null
          && startingLocationASTElementEntry.getKey().line == key.line) {
        return Optional.ofNullable(startingLocationASTElementEntry.getValue());
      }
      return Optional.empty();
    } else {
      StartingLocation key = new StartingLocation(-1, pLine);
      // We want the first statement at the given line
      Entry<StartingLocation, ASTElement> startingLocationASTElementEntry =
          startingLocationToTightestStatement.ceilingEntry(key);
      if (startingLocationASTElementEntry != null
          && startingLocationASTElementEntry.getKey().line == key.line) {
        return Optional.ofNullable(startingLocationASTElementEntry.getValue());
      }
      return Optional.empty();
    }
  }

  /**
   * Returns the column of the opening parenthesis of a function call, which is the location by
   * which witnesses identify function calls.
   *
   * <p>This is only possible for calls whose function name is given by an identifier, i.e. not for
   * calls through function pointers. It also assumes that no whitespace separates the function name
   * from the parenthesis, since we do not track the parenthesis itself. See <a
   * href="https://gitlab.com/sosy-lab/software/cpachecker/-/work_items/1687">work item 1687</a>.
   *
   * @param pCall the call whose parenthesis we are looking for
   * @return the column of the parenthesis, or nothing if it cannot be computed
   */
  public OptionalInt getColumnOfFunctionCallParenthesis(AFunctionCallExpression pCall) {
    if (!(pCall.getFunctionNameExpression() instanceof AIdExpression functionName)) {
      return OptionalInt.empty();
    }

    FileLocation location = functionName.getFileLocation();
    if (!location.isRealLocation()) {
      return OptionalInt.empty();
    }

    // The name in the source is the original one, since renamings happen only inside CPAchecker
    return OptionalInt.of(
        location.getStartColumnInLine()
            + functionName.toASTString(AAstNodeRepresentation.ORIGINAL_NAMES).length());
  }

  /**
   * This method returns all variables and parameters in scope at the given node. It includes global
   * variables, local variables and parameters. As in the input program, it excludes the variables
   * that go out of scope at the node and the ones hidden by another declaration with the same name.
   *
   * <p>Whenever this is not possible, it returns an empty Optional instead. This usually happens
   * when some information was not passed/tracked correctly from the frontend and is likely a bug in
   * CPAchecker, so in case you encounter such a case please report it.
   *
   * <p>We return an empty Optional to avoid throwing an exception in case the information is not
   * available, since currently this information is only required when generating some output of
   * CPAchecker, in which case we can simply remove the information if it is not available.
   *
   * @param pNode The node for which we want to get the variables and parameters in scope
   * @return An Optional containing all variables and parameters in scope at the given node, or an
   *     empty Optional if the information is not available
   */
  public Optional<FluentIterable<AbstractSimpleDeclaration>> getVariablesAndParametersInScope(
      CFANode pNode) {
    if (!cfaNodeToAstParametersInScope.containsKey(pNode)
        || !cfaNodeToAstLocalVariablesInScope.containsKey(pNode)) {
      return Optional.empty();
    }

    Set<AVariableDeclaration> localVariables =
        Objects.requireNonNull(cfaNodeToAstLocalVariablesInScope.get(pNode));
    Set<AParameterDeclaration> parameters =
        Objects.requireNonNull(cfaNodeToAstParametersInScope.get(pNode));
    ImmutableSet<String> localNames =
        FluentIterable.concat(localVariables, parameters)
            .transform(AbstractSimpleDeclaration::getOrigName)
            .toSet();
    return Optional.of(
        FluentIterable.concat(
            localVariables,
            parameters,
            // A local variable hides a global one with the same name, and a static local variable
            // is also among the global variables
            FluentIterable.from(globalVariables)
                .filter(global -> !localNames.contains(global.getOrigName()))));
  }

  /**
   * Returns the name in the input program of the variable that CPAchecker calls {@code pName} at
   * the given node, e.g., {@code i} for the variable {@code i__1}, which CPAchecker renamed because
   * another variable {@code i} exists in the same function. Returns {@code pName} if no variable
   * with this name is in scope.
   *
   * <p>TODO: This information should be collected during parsing. Currently, this is only a simple
   * heuristic that looks for a variable with this name among the variables in scope.
   */
  public String getNameInInputProgram(CFANode pNode, String pName) {
    return getVariablesAndParametersInScope(pNode)
        .flatMap(
            variables ->
                variables.firstMatch(variable -> variable.getName().equals(pName)).toJavaUtil())
        .map(AbstractSimpleDeclaration::getOrigName)
        .orElse(pName);
  }

  /**
   * Returns the node before the given statement in the given function, i.e., not a node in a
   * function that the statement calls.
   */
  public Optional<CFANode> getNodeBeforeStatement(ASTElement pStatement, String pFunction) {
    return pStatement.edges().stream()
        .map(CFAEdge::getPredecessor)
        .filter(node -> node.getFunctionName().equals(pFunction))
        .findFirst();
  }

  /**
   * This method returns all the best approximation we have for the file location of the node in the
   * original program.
   *
   * <p>Whenever doing a matching is not possible, it returns an empty Optional instead. This can
   * happen with difficult control structures which may make it difficult to determine a single
   * location.
   *
   * @param pNode The node for which we want to get the file location.
   * @return An Optional containing the best approximation we have for the position of a CFANode in
   *     the input program, or an empty Optional if we cannot generate such a matching
   */
  public Optional<FileLocation> getStatementFileLocationForNode(CFANode pNode) {
    if (startingLocationToTightestStatement == null) {
      initializeMapFromStartingLocationToTightestStatement();
    }

    if (pNode.getNumLeavingEdges() != 0) {
      FileLocation closestFileLocationToNode =
          pNode.getAllLeavingEdges().transform(CFAEdge::getFileLocation).stream()
              .min(Comparator.naturalOrder())
              .orElseThrow();
      StartingLocation closestStartingLocationToNode =
          new StartingLocation(
              closestFileLocationToNode.getStartColumnInLine(),
              closestFileLocationToNode.getStartingLineInOrigin());

      Entry<StartingLocation, ASTElement> element =
          startingLocationToTightestStatement.floorEntry(closestStartingLocationToNode);

      // Could happen for example for the first node
      if (element == null) {
        return Optional.empty();
      }

      return Optional.of(element.getValue().location());
    } else if (pNode.getNumLeavingEdges() != 0) {
      FileLocation closestFileLocationToNode =
          pNode.getAllLeavingEdges().transform(CFAEdge::getFileLocation).stream()
              .max(Comparator.naturalOrder())
              .orElseThrow();
      StartingLocation closestStartingLocationToNode =
          new StartingLocation(
              closestFileLocationToNode.getStartColumnInLine(),
              closestFileLocationToNode.getStartingLineInOrigin());
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

  /**
   * Returns the location of the statement given by {@link #getStatementFileLocationForNode} if the
   * execution of this statement starts at the given node, e.g., not for a node inside a conditional
   * expression of the statement.
   */
  public Optional<FileLocation> getStatementStartingAtNode(CFANode pNode) {
    return getStatementFileLocationForNode(pNode)
        .filter(
            location -> {
              ASTElement statement =
                  startingLocationToTightestStatement.get(
                      new StartingLocation(
                          location.getStartColumnInLine(), location.getStartingLineInOrigin()));
              return statement != null && statement.startNodes().contains(pNode);
            });
  }

  public Optional<ImmutableSet<AVariableDeclaration>> getGlobalVariables() {
    if (globalVariables == null) {
      return Optional.empty();
    }
    return Optional.of(globalVariables);
  }

  public ImmutableSet<AVariableDeclaration> getAstLocalVariablesInScopeByCfaNode(CFANode pCfaNode) {
    if (!cfaNodeToAstLocalVariablesInScope.containsKey(pCfaNode)) {
      return ImmutableSet.of();
    }
    return ImmutableSet.copyOf(
        Objects.requireNonNull(cfaNodeToAstLocalVariablesInScope.get(pCfaNode)));
  }

  public ImmutableSet<AParameterDeclaration> getAstParametersInScopeByCfaNode(CFANode pCfaNode) {
    if (!cfaNodeToAstParametersInScope.containsKey(pCfaNode)) {
      return ImmutableSet.of();
    }
    return ImmutableSet.copyOf(Objects.requireNonNull(cfaNodeToAstParametersInScope.get(pCfaNode)));
  }

  /**
   * Returns the local variables declared in the blocks around the node, as recorded by the CFA
   * builder. Unlike {@link #getVariablesAndParametersInScope}, they include the ones that go out of
   * scope at the node and the ones hidden by another declaration with the same name.
   */
  public ImmutableSet<AVariableDeclaration> getDeclaredLocalVariablesByCfaNode(CFANode pCfaNode) {
    if (!cfaNodeToDeclaredLocalVariables.containsKey(pCfaNode)) {
      return ImmutableSet.of();
    }
    return ImmutableSet.copyOf(
        Objects.requireNonNull(cfaNodeToDeclaredLocalVariables.get(pCfaNode)));
  }

  /**
   * Returns the parameters of the function of the node, as recorded by the CFA builder. Unlike
   * {@link #getVariablesAndParametersInScope}, they include the ones that go out of scope at the
   * node and the ones hidden by a local variable with the same name.
   */
  public ImmutableSet<AParameterDeclaration> getDeclaredParametersByCfaNode(CFANode pCfaNode) {
    if (!cfaNodeToDeclaredParameters.containsKey(pCfaNode)) {
      return ImmutableSet.of();
    }
    return ImmutableSet.copyOf(Objects.requireNonNull(cfaNodeToDeclaredParameters.get(pCfaNode)));
  }
}
