// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import com.google.common.base.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class AReturnStatementEdge extends AbstractCFAEdge {

  private static final long serialVersionUID = -6181479727890105919L;
  protected final AReturnStatement rawAST;

  protected AReturnStatementEdge(String pRawStatement, AReturnStatement pRawAST,
      FileLocation pFileLocation, CFANode pPredecessor, FunctionExitNode pSuccessor) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    rawAST = pRawAST;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.ReturnStatementEdge;
  }

  public Optional<? extends AExpression> getExpression() {
    return rawAST.getReturnValue();
  }

  /** See {@link AReturnStatement#asAssignment()}. */
  public Optional<? extends AAssignment> asAssignment() {
    return rawAST.asAssignment();
  }

  @Override
  public Optional<? extends AReturnStatement> getRawAST() {
    return Optional.of(rawAST);
  }

  @Override
  public String getCode() {
    return rawAST.toASTString();
  }

  @Override
  public FunctionExitNode getSuccessor() {
    // the constructor enforces that the successor is always a FunctionExitNode
    return (FunctionExitNode)super.getSuccessor();
  }

}
