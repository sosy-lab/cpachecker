// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class IterationStructure extends BranchingStructure {

  private final ASTElement clause;
  private final ASTElement body;
  private final ASTElement completeElement;
  private final ASTElement controllingExpression;
  private final Optional<ASTElement> initClause;
  private final Optional<ASTElement> iterationExpression;

  IterationStructure(
      FileLocation pIterationStatementLocation,
      FileLocation pClauseLocation,
      FileLocation pControllingExpression,
      FileLocation pBodyLocation,
      Optional<FileLocation> pMaybeInitClause,
      Optional<FileLocation> pMaybeIterationExpression,
      ImmutableSet<CFAEdge> pEdges) {
    clause = determineElement(pClauseLocation, pEdges);
    body = determineElement(pBodyLocation, pEdges);
    completeElement = determineElement(pIterationStatementLocation, pEdges);
    controllingExpression = determineElement(pControllingExpression, pEdges);
    initClause = pMaybeInitClause.map(x -> determineElement(x, pEdges));
    iterationExpression = pMaybeIterationExpression.map(x -> determineElement(x, pEdges));
  }

  @Override
  public ASTElement getClause() {
    return clause;
  }

  @Override
  public ASTElement getCompleteElement() {
    return completeElement;
  }

  @Override
  public ASTElement getControllingExpression() {
    return controllingExpression;
  }

  public ASTElement getBody() {
    return null;
  }

  public Optional<ASTElement> getInitClause() {
    return Optional.empty();
  }

  public Optional<ASTElement> getIterationExpression() {
    return Optional.empty();
  }
}
