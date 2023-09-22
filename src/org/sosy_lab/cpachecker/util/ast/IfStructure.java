// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class IfStructure extends StatementStructure {

  private final ASTElement completeElement;
  private final ASTElement conditionElement;
  private final ASTElement thenElement;
  private final Optional<ASTElement> maybeElseElement;

  private final Collection<CFAEdge> thenEdges;
  private final Collection<CFAEdge> elseEdges;

  IfStructure(
      FileLocation pIfLocation,
      FileLocation pConditionLocation,
      FileLocation pThenLocation,
      Optional<FileLocation> pMaybeElseLocation,
      Set<CFAEdge> pEdges) {
    completeElement = determineElement(pIfLocation, pEdges);
    conditionElement = determineElement(pConditionLocation, pEdges);
    thenElement = determineElement(pThenLocation, pEdges);
    maybeElseElement = pMaybeElseLocation.map(x -> determineElement(x, pEdges));
    thenEdges = findThenEdges();
    elseEdges = findElseEdges();
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
    return thenEdges;
  }

  public Collection<CFAEdge> getElseEdges() {
    return elseEdges;
  }

  private Collection<CFAEdge> findThenEdges() {
    return findBlockEdges(thenElement.edges());
  }

  private Collection<CFAEdge> findElseEdges() {
    return maybeElseElement.map(x -> findBlockEdges(x.edges())).orElse(ImmutableSet.of());
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
