// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class CReturnStatementEdge extends AReturnStatementEdge implements CCfaEdge {

  private static final long serialVersionUID = 8753970625917047772L;

  public CReturnStatementEdge(
      String pRawStatement,
      CReturnStatement pReturnStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      FunctionExitNode pSuccessor) {

    super(pRawStatement, pReturnStatement, pFileLocation, pPredecessor, pSuccessor);
  }

  @Override
  public CReturnStatement getReturnStatement() {
    return (CReturnStatement) returnStatement;
  }

  @Override
  public Optional<CExpression> getExpression() {
    return getReturnStatement().getReturnValue();
  }

  @Override
  public Optional<CAssignment> asAssignment() {
    return getReturnStatement().asAssignment();
  }

  @Override
  public <R, X extends Exception> R accept(CCfaEdgeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }
}
