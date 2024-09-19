// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import static org.sosy_lab.cpachecker.util.ast.AstUtils.computeNodesConditionBoundaryNodes;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

public final class IfElement extends StatementElement {
  private final ASTElement conditionElement;
  private final ASTElement thenElement;
  private final Optional<ASTElement> maybeElseElement;

  @LazyInit private ImmutableSet<CFAEdge> thenEdges = null;
  @LazyInit private ImmutableSet<CFAEdge> elseEdges = null;
  @LazyInit private ImmutableSet<CFAEdge> conditionEdges = null;
  @LazyInit private ImmutableSet<CFANode> nodesBetweenConditionAndThenBranch = null;
  @LazyInit private ImmutableSet<CFANode> nodesBetweenConditionAndElseBranch = null;

  public IfElement(
      FileLocation pIfLocation,
      FileLocation pConditionLocation,
      FileLocation pThenLocation,
      Optional<FileLocation> pMaybeElseLocation,
      ImmutableSet<CFAEdge> pEdges) {
    super(pIfLocation, pEdges);
    conditionElement = determineElement(pConditionLocation, pEdges);
    thenElement = determineElement(pThenLocation, pEdges);
    maybeElseElement = pMaybeElseLocation.map(x -> determineElement(x, pEdges));
  }

  public ASTElement getConditionElement() {
    return conditionElement;
  }

  public ASTElement getThenElement() {
    return thenElement;
  }

  public Optional<ASTElement> getMaybeElseElement() {
    return maybeElseElement;
  }

  public ImmutableSet<CFAEdge> getThenEdges() {
    if (thenEdges == null) {
      thenEdges = findThenEdges();
    }
    return thenEdges;
  }

  public ImmutableSet<CFAEdge> getConditionEdges() {
    if (conditionEdges == null) {
      conditionEdges = findConditionEdges();
    }
    return conditionEdges;
  }

  public ImmutableSet<CFAEdge> getElseEdges() {
    if (elseEdges == null) {
      elseEdges = findElseEdges();
    }
    return elseEdges;
  }

  private void computeNodesBetweenConditionAndBranches() {
    Pair<ImmutableSet<CFANode>, ImmutableSet<CFANode>> borderElements =
        computeNodesConditionBoundaryNodes(
            conditionElement.edges(),
            thenElement.edges().isEmpty() ? Optional.empty() : Optional.of(thenElement.edges()),
            maybeElseElement.isPresent()
                ? Optional.of(maybeElseElement.orElseThrow().edges())
                : Optional.empty());

    nodesBetweenConditionAndThenBranch = borderElements.getFirst();
    nodesBetweenConditionAndElseBranch = borderElements.getSecond();
  }

  /**
   * Returns the nodes between the condition and the then branch.
   *
   * @return the nodes between the condition and the then branch
   */
  public ImmutableSet<CFANode> getNodesBetweenConditionAndThenBranch() {
    if (nodesBetweenConditionAndThenBranch == null) {
      computeNodesBetweenConditionAndBranches();
    }
    return nodesBetweenConditionAndThenBranch;
  }

  /**
   * Returns the nodes between the condition and the else branch.
   *
   * @return the nodes between the condition and the else branch
   */
  public ImmutableSet<CFANode> getNodesBetweenConditionAndElseBranch() {
    if (nodesBetweenConditionAndElseBranch == null) {
      computeNodesBetweenConditionAndBranches();
    }
    return nodesBetweenConditionAndElseBranch;
  }

  public ImmutableSet<CFANode> getConditionNodes() {
    return FluentIterable.from(conditionElement.edges())
        .transformAndConcat(CFAUtils::nodes)
        .toSet();
  }

  private ImmutableSet<CFAEdge> findThenEdges() {
    return findBlockEdges(thenElement.edges());
  }

  private ImmutableSet<CFAEdge> findElseEdges() {
    return maybeElseElement.map(x -> findBlockEdges(x.edges())).orElse(ImmutableSet.of());
  }

  private ImmutableSet<CFAEdge> findConditionEdges() {
    return findBlockEdges(conditionElement.edges());
  }

  private ImmutableSet<CFAEdge> findBlockEdges(Collection<CFAEdge> target) {
    ImmutableSet.Builder<CFAEdge> result = ImmutableSet.builder();
    for (CFAEdge e : conditionElement.edges()) {
      for (CFAEdge successor : CFAUtils.leavingEdges(e.getSuccessor())) {
        if (target.contains(successor)) {
          result.add(successor);
        }
      }
    }
    return result.build();
  }
}
