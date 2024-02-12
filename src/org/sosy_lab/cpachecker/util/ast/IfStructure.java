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
import java.util.Collection;
import java.util.HashSet;
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

  private void computeNodesBetweenConditionAndBranches() {
    Set<CFANode> nodesThenBranch =
        transformedImmutableSetCopy(thenElement.edges(), CFAEdge::getPredecessor);
    Set<CFANode> nodesBoundaryCondition =
        transformedImmutableSetCopy(conditionElement.edges(), CFAEdge::getSuccessor);
    nodesBoundaryCondition = new HashSet<>(nodesBoundaryCondition);
    nodesBoundaryCondition.removeAll(
        transformedImmutableSetCopy(conditionElement.edges(), CFAEdge::getPredecessor));
    Set<CFANode> collectorNodesBetweenConditionAndElseBranch =
        new HashSet<>(nodesBoundaryCondition);
    Set<CFANode> collectorNnodesBetweenConditionAndThenBranch =
        new HashSet<>(nodesBoundaryCondition);

    // TODO: Currently we over-approximate by taking both branches when there are no edges
    //  in both branches
    if (nodesThenBranch.isEmpty()) {
      if (maybeElseElement.isPresent()) {
        Set<CFANode> nodesElseBranch =
            transformedImmutableSetCopy(
                maybeElseElement.orElseThrow().edges(), CFAEdge::getPredecessor);
        collectorNnodesBetweenConditionAndThenBranch.removeAll(nodesElseBranch);
        collectorNodesBetweenConditionAndElseBranch.retainAll(nodesElseBranch);
      }
    } else {
      collectorNnodesBetweenConditionAndThenBranch.retainAll(nodesThenBranch);
      collectorNodesBetweenConditionAndElseBranch.removeAll(nodesThenBranch);
    }
    nodesBetweenConditionAndElseBranch =
        ImmutableSet.copyOf(collectorNodesBetweenConditionAndElseBranch);
    nodesBetweenConditionAndThenBranch =
        ImmutableSet.copyOf(collectorNnodesBetweenConditionAndThenBranch);
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
