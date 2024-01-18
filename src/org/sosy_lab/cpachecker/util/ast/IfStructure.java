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

  private Collection<CFAEdge> thenEdges = null;
  private Collection<CFAEdge> elseEdges = null;
  private Collection<CFAEdge> conditionEdges = null;

  private Collection<CFANode> nodesBetweenConditionAndThenBranch = null;
  private Collection<CFANode> nodesBetweenConditionAndElseBranch = null;

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

  public Collection<CFAEdge> getThenEdges() {
    if (thenEdges == null) {
      thenEdges = findThenEdges();
    }
    return thenEdges;
  }

  public Collection<CFAEdge> getConditionEdges() {
    if (conditionEdges == null) {
      conditionEdges = findConditionEdges();
    }
    return conditionEdges;
  }

  public Collection<CFAEdge> getElseEdges() {
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
    nodesBetweenConditionAndElseBranch = new HashSet<>(nodesBoundaryCondition);
    nodesBetweenConditionAndThenBranch = new HashSet<>(nodesBoundaryCondition);

    // TODO: Currently we over-approximate by taking both branches when there are no edges
    //  in both branches
    if (nodesThenBranch.isEmpty()) {
      if (maybeElseElement.isPresent()) {
        Set<CFANode> nodesElseBranch =
            transformedImmutableSetCopy(
                maybeElseElement.orElseThrow().edges(), CFAEdge::getPredecessor);
        nodesBetweenConditionAndThenBranch.removeAll(nodesElseBranch);
        nodesBetweenConditionAndElseBranch.retainAll(nodesElseBranch);
      }
    } else {
      nodesBetweenConditionAndThenBranch.retainAll(nodesThenBranch);
      nodesBetweenConditionAndElseBranch.removeAll(nodesThenBranch);
    }
  }

  public ImmutableSet<CFANode> getNodesBetweenConditionAndThenBranch() {
    if (nodesBetweenConditionAndThenBranch == null) {
      computeNodesBetweenConditionAndBranches();
    }
    return ImmutableSet.copyOf(nodesBetweenConditionAndThenBranch);
  }

  public ImmutableSet<CFANode> getNodesBetweenConditionAndElseBranch() {
    if (nodesBetweenConditionAndElseBranch == null) {
      computeNodesBetweenConditionAndBranches();
    }
    return ImmutableSet.copyOf(nodesBetweenConditionAndElseBranch);
  }

  private Collection<CFAEdge> findThenEdges() {
    return findBlockEdges(thenElement.edges());
  }

  private Collection<CFAEdge> findElseEdges() {
    return maybeElseElement.map(x -> findBlockEdges(x.edges())).orElse(ImmutableSet.of());
  }

  private Collection<CFAEdge> findConditionEdges() {
    return findBlockEdges(conditionElement.edges());
  }

  private Collection<CFAEdge> findBlockEdges(Collection<CFAEdge> target) {
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
