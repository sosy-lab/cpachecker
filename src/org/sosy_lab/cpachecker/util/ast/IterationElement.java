// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;
import static org.sosy_lab.cpachecker.util.ast.AstUtils.computeNodesConditionBoundaryNodes;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.Traverser;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ast.AstUtils.BoundaryNodesComputationFailed;

public final class IterationElement extends BranchingElement {

  private final Optional<ASTElement> clause;
  private final ASTElement body;
  private final Optional<ASTElement> controllingExpression;
  private final Optional<ASTElement> initClause;
  private final Optional<ASTElement> iterationExpression;

  // This considers the body to be as in a while loop, including the iteration expression
  @LazyInit private ImmutableSet<CFANode> nodesBetweenConditionAndBody = null;
  @LazyInit private ImmutableSet<CFANode> nodesBetweenConditionAndExit = null;

  public IterationElement(
      FileLocation pIterationStatementLocation,
      Optional<FileLocation> pClauseLocation,
      Optional<FileLocation> pControllingExpression,
      FileLocation pBodyLocation,
      Optional<FileLocation> pMaybeInitClause,
      Optional<FileLocation> pMaybeIterationExpression) {
    super(pIterationStatementLocation);
    clause = pClauseLocation.map(x -> determineElement(x));
    body = determineElement(pBodyLocation);
    controllingExpression = pControllingExpression.map(x -> determineElement(x));
    initClause = pMaybeInitClause.map(x -> determineElement(x));
    iterationExpression = pMaybeIterationExpression.map(x -> determineElement(x));
  }

  @Override
  protected void setEdges(ImmutableSet<CFAEdge> pEdges) {
    super.setEdges(pEdges);
    clause.ifPresent(x -> x.setEdges(pEdges));
    body.setEdges(pEdges);
    controllingExpression.ifPresent(x -> x.setEdges(pEdges));
    initClause.ifPresent(x -> x.setEdges(pEdges));
    iterationExpression.ifPresent(x -> x.setEdges(pEdges));
  }

  @Override
  public Optional<ASTElement> getClause() {
    return clause;
  }

  @Override
  public Optional<ASTElement> getControllingExpression() {
    return controllingExpression;
  }

  public ASTElement getBody() {
    return body;
  }

  public Optional<ASTElement> getInitClause() {
    return initClause;
  }

  public Optional<ASTElement> getIterationExpression() {
    return iterationExpression;
  }

  /**
   * Returns the node at which an invariant of this loop holds, i.e., the node directly before the
   * evaluation of its controlling expression, if the statement is a loop of the given loop
   * structure. For a while or a for loop this is the loop head of the loop structure.
   */
  public Optional<CFANode> getLoopHead(LoopStructure pLoopStructure) {
    if (controllingExpression.isEmpty()) {
      return Optional.empty();
    }

    FluentIterable<@NonNull CFANode> boundaryNodes;
    if (controllingExpression.orElseThrow().edges().isEmpty()) {
      // Can happen in the case of while(1) { ... }
      try {
        boundaryNodes =
            FluentIterable.from(getNodesBetweenConditionAndBody())
                .transformAndConcat(CFAUtils::allPredecessorsOf);
      } catch (BoundaryNodesComputationFailed e) {
        // In this case, all our heuristics for computing a loop head failed.
        return Optional.empty();
      }
    } else {
      boundaryNodes =
          FluentIterable.from(controllingExpression.orElseThrow().edges())
              .transform(CFAEdge::getPredecessor);
    }

    ImmutableSet<CFANode> heads;
    if (controllingExpression.orElseThrow().location().getNodeOffset()
        > body.location().getNodeOffset()) {
      // The controlling expression of a do-while loop is evaluated after the body, so the
      // invariant holds there and not at the loop head
      heads =
          FluentIterable.from(nodesBefore(boundaryNodes.toSet(), ImmutableSet.of()))
              .filter(
                  node ->
                      pLoopStructure.getAllLoops().stream()
                          .anyMatch(loop -> loop.getLoopNodes().contains(node)))
              .toSet();
    } else {
      Set<CFANode> loopHeads = pLoopStructure.getAllLoopHeads();
      heads = boundaryNodes.filter(loopHeads::contains).toSet();
      if (heads.isEmpty()) {
        // Side effects of the controlling expression, e.g., `i++` in `while (i++ < n)`, are
        // evaluated between the loop head and the condition
        heads =
            FluentIterable.from(nodesBefore(boundaryNodes.toSet(), loopHeads))
                .filter(loopHeads::contains)
                .toSet();
      }
    }

    if (heads.size() != 1) {
      return Optional.empty();
    }

    return Optional.of(heads.iterator().next());
  }

  /**
   * Returns the nodes at which the paths backwards from the given nodes end that only use edges of
   * this iteration statement outside of its body, e.g., the edges for side effects of the
   * controlling expression. A path also ends at one of the given stop nodes.
   */
  private ImmutableSet<CFANode> nodesBefore(Set<CFANode> pNodes, Set<CFANode> pStopNodes) {
    ImmutableSet<CFAEdge> statementEdges = getCompleteElement().edges();
    SuccessorsFunction<CFANode> predecessorsOutsideBody =
        node ->
            pStopNodes.contains(node)
                ? ImmutableSet.of()
                : node.getEnteringEdges()
                    .filter(edge -> statementEdges.contains(edge) && !body.edges().contains(edge))
                    .transform(CFAEdge::getPredecessor)
                    .toSet();
    return FluentIterable.from(Traverser.forGraph(predecessorsOutsideBody).breadthFirst(pNodes))
        .filter(node -> Iterables.isEmpty(predecessorsOutsideBody.successors(node)))
        .toSet();
  }

  private void computeNodesBetweenConditionAndBody() throws BoundaryNodesComputationFailed {
    if (controllingExpression.isEmpty()
        // Can happen in the case of while(1) { ... }
        || controllingExpression.orElseThrow().edges().isEmpty()) {
      nodesBetweenConditionAndBody =
          ImmutableSet.copyOf(
              Sets.difference(
                  transformedImmutableSetCopy(body.edges(), CFAEdge::getPredecessor),
                  transformedImmutableSetCopy(body.edges(), CFAEdge::getSuccessor)));
      nodesBetweenConditionAndExit = ImmutableSet.of();
      return;
    }

    Pair<ImmutableSet<CFANode>, ImmutableSet<CFANode>> borderElements =
        computeNodesConditionBoundaryNodes(
            controllingExpression.orElseThrow().edges(),
            Optional.of(body.edges()),
            Optional.empty());
    nodesBetweenConditionAndBody = borderElements.getFirst();
    nodesBetweenConditionAndExit = borderElements.getSecond();
  }

  public ImmutableSet<@NonNull CFANode> getNodesBetweenConditionAndBody()
      throws BoundaryNodesComputationFailed {
    if (nodesBetweenConditionAndBody == null) {
      computeNodesBetweenConditionAndBody();
    }
    return nodesBetweenConditionAndBody;
  }

  public ImmutableSet<CFANode> getNodesBetweenConditionAndExit()
      throws BoundaryNodesComputationFailed {
    if (nodesBetweenConditionAndExit == null) {
      computeNodesBetweenConditionAndBody();
    }
    return nodesBetweenConditionAndExit;
  }

  public FluentIterable<@NonNull CFANode> getControllingExpressionNodes() {
    if (controllingExpression.isEmpty()) {
      return FluentIterable.of();
    }

    return FluentIterable.from(controllingExpression.orElseThrow().edges())
        .transformAndConcat(CFAUtils::nodes);
  }
}
