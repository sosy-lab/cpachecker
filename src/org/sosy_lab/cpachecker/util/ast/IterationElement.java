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
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
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
      Optional<FileLocation> pMaybeIterationExpression,
      ImmutableSet<CFAEdge> pEdges) {
    super(pIterationStatementLocation, pEdges);
    clause = pClauseLocation.map(x -> determineElement(x, pEdges));
    body = determineElement(pBodyLocation, pEdges);
    controllingExpression = pControllingExpression.map(x -> determineElement(x, pEdges));
    initClause = pMaybeInitClause.map(x -> determineElement(x, pEdges));
    iterationExpression = pMaybeIterationExpression.map(x -> determineElement(x, pEdges));
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

  public Optional<CFANode> getLoopHead() {
    if (controllingExpression.isEmpty()) {
      return Optional.empty();
    }

    // First try to get the loop head normally by iterating over all the edges.
    // This will fail if there is a loop inside the loop body.
    ImmutableSet<CFANode> loopStartNodesCandidates =
        getCompleteElement().edges().stream()
            .map(CFAEdge::getPredecessor)
            .filter(CFANode::isLoopStart)
            .collect(ImmutableSet.toImmutableSet());
    if (loopStartNodesCandidates.size() == 1) {
      return Optional.of(loopStartNodesCandidates.iterator().next());
    }

    // If we found no, or more than one loop start nodes, we try to find the loop head
    // by looking at the edges of the controlling expression.
    ImmutableSet<CFANode> loopStartNodes =
        controllingExpression.orElseThrow().edges().stream()
            .map(CFAEdge::getPredecessor)
            .filter(CFANode::isLoopStart)
            .collect(ImmutableSet.toImmutableSet());

    if (loopStartNodes.size() != 1) {
      return Optional.empty();
    }

    return Optional.of(loopStartNodes.iterator().next());
  }

  private void computeNodesBetweenConditionAndBody() throws BoundaryNodesComputationFailed {
    if (controllingExpression.isEmpty()) {
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

  public ImmutableSet<CFANode> getNodesBetweenConditionAndBody()
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

  public FluentIterable<CFANode> getControllingExpressionNodes() {
    if (controllingExpression.isEmpty()) {
      return FluentIterable.of();
    }

    return FluentIterable.from(controllingExpression.orElseThrow().edges())
        .transformAndConcat(CFAUtils::nodes);
  }
}
