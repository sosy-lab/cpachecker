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

  private final Optional<ASTElement> clause;
  private final ASTElement body;
  private final ASTElement completeElement;
  private final Optional<ASTElement> controllingExpression;
  private final Optional<ASTElement> initClause;
  private final Optional<ASTElement> iterationExpression;

  IterationStructure(
      FileLocation pIterationStatementLocation,
      Optional<FileLocation> pClauseLocation,
      Optional<FileLocation> pControllingExpression,
      FileLocation pBodyLocation,
      Optional<FileLocation> pMaybeInitClause,
      Optional<FileLocation> pMaybeIterationExpression,
      ImmutableSet<CFAEdge> pEdges) {
    clause = pClauseLocation.map(x -> determineElement(x, pEdges));
    body = determineElement(pBodyLocation, pEdges);
    completeElement = determineElement(pIterationStatementLocation, pEdges);
    controllingExpression = pControllingExpression.map(x -> determineElement(x, pEdges));
    initClause = pMaybeInitClause.map(x -> determineElement(x, pEdges));
    iterationExpression = pMaybeIterationExpression.map(x -> determineElement(x, pEdges));
  }

  @Override
  public Optional<ASTElement> getClause() {
    return clause;
  }

  @Override
  public ASTElement getCompleteElement() {
    return completeElement;
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
}
