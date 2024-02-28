// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class IfStructure extends StatementStructure {

  private final ASTElement completeElement;
  private final ASTElement conditionElement;
  private final ASTElement thenElement;
  private final Optional<ASTElement> maybeElseElement;

  private ImmutableSet<CFAEdge> thenEdges = null;
  private ImmutableSet<CFAEdge> elseEdges = null;
  private ImmutableSet<CFAEdge> conditionEdges = null;

  private ImmutableSet<CFANode> nodesBetweenConditionAndThenBranch = null;
  private ImmutableSet<CFANode> nodesBetweenConditionAndElseBranch = null;

  IfStructure(
      FileLocation pIfLocation,
      FileLocation pConditionLocation,
      FileLocation pThenLocation,
      Optional<FileLocation> pMaybeElseLocation,
      ImmutableSet<CFAEdge> pEdges) {
    completeElement = determineElement(pIfLocation, pEdges);
    conditionElement = determineElement(pConditionLocation, pEdges);
    thenElement = determineElement(pThenLocation, pEdges);
    maybeElseElement = pMaybeElseLocation.map(x -> determineElement(x, pEdges));
  }

  @Override
  public ASTElement getCompleteElement() {
    return completeElement;
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

  /**
   * This method computes the nodes which are at the boundary between the condition of an if
   * statement and the nodes inside each of the branches. This information can be used to determine
   * which branch is being taken based only a {@link CFANode}.
   *
   * <p>To do this, first every node in the boundary of the condition is computed. These are all
   * nodes which are successors of edges in the condition but not predecessors, we call these
   * boundary condition nodes. If the then branch contains at least one node, the
   * intersection/difference of nodes which are a predecessor of an edge in the then branch with the
   * boundary condition nodes is taken to get the nodes between the condition and the then/else
   * branch. If the then branch is empty, the intersection/difference of the boundary condition
   * nodes with the nodes which are a predecessor of an edge in the else branch is taken to get the
   * nodes between the condition and the else/then branch.
   *
   * <p>For example, consider the following graph for an if condition, where node 6 and 5 are the
   * first nodes in the 'if' and 'else' branches respectively. Then the nodes at the boundary are 3
   * and 4, where 3 is at the boundary for the 'if' branch and 4 is at the boundary to the 'else'
   * branch.
   *
   * <pre>
   *    1
   *   / \
   *  2---3
   *  |   |
   *  4   5
   *  |
   *  6
   *  </pre>
   */
  private void computeNodesBetweenConditionAndBranches() {
    Set<CFANode> nodesBoundaryCondition =
        Sets.difference(
            transformedImmutableSetCopy(conditionElement.edges(), CFAEdge::getSuccessor),
            transformedImmutableSetCopy(conditionElement.edges(), CFAEdge::getPredecessor));
    Set<CFANode> collectorNodesBetweenConditionAndElseBranch = nodesBoundaryCondition;
    Set<CFANode> collectorNodesBetweenConditionAndThenBranch = nodesBoundaryCondition;

    // TODO: Currently we over-approximate by taking both branches when there are no edges
    //  in both branches
    Set<CFANode> nodesThenBranch =
        transformedImmutableSetCopy(thenElement.edges(), CFAEdge::getPredecessor);

    if (nodesThenBranch.isEmpty()) {
      if (maybeElseElement.isPresent()) {
        Set<CFANode> nodesElseBranch =
            transformedImmutableSetCopy(
                maybeElseElement.orElseThrow().edges(), CFAEdge::getPredecessor);
        collectorNodesBetweenConditionAndThenBranch =
            Sets.difference(nodesBoundaryCondition, nodesElseBranch);
        collectorNodesBetweenConditionAndElseBranch =
            Sets.intersection(nodesBoundaryCondition, nodesElseBranch);
      }
    } else {
      collectorNodesBetweenConditionAndThenBranch =
          Sets.intersection(nodesBoundaryCondition, nodesThenBranch);
      collectorNodesBetweenConditionAndElseBranch =
          Sets.difference(nodesBoundaryCondition, nodesThenBranch);
    }
    nodesBetweenConditionAndElseBranch =
        ImmutableSet.copyOf(collectorNodesBetweenConditionAndElseBranch);
    nodesBetweenConditionAndThenBranch =
        ImmutableSet.copyOf(collectorNodesBetweenConditionAndThenBranch);
  }

  public ImmutableSet<CFANode> getNodesBetweenConditionAndThenBranch() {
    if (nodesBetweenConditionAndThenBranch == null) {
      computeNodesBetweenConditionAndBranches();
    }
    return nodesBetweenConditionAndThenBranch;
  }

  public ImmutableSet<CFANode> getNodesBetweenConditionAndElseBranch() {
    if (nodesBetweenConditionAndElseBranch == null) {
      computeNodesBetweenConditionAndBranches();
    }
    return nodesBetweenConditionAndElseBranch;
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
