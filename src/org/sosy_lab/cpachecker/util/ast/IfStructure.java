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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class IfStructure extends StatementStructure {

  private final ASTElement completeElement;
  private final ASTElement conditionElement;
  private final ASTElement thenElement;
  private final Optional<ASTElement> maybeElseElement;

  private Collection<CFAEdge> thenEdges = null;
  private Collection<CFAEdge> elseEdges = null;

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

  public Collection<CFAEdge> getElseEdges() {
    if (elseEdges == null) {
      elseEdges = findElseEdges();
    }
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
