// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import com.google.common.base.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class CReturnStatementEdge extends AReturnStatementEdge {


  private static final long serialVersionUID = 8753970625917047772L;

  public CReturnStatementEdge(String pRawStatement, CReturnStatement pRawAST,
      FileLocation pFileLocation, CFANode pPredecessor, FunctionExitNode pSuccessor) {

    super(pRawStatement, pRawAST, pFileLocation, pPredecessor, pSuccessor);

  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<CExpression> getExpression() {
    return (Optional<CExpression>)rawAST.getReturnValue();
  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<CAssignment> asAssignment() {
    return (Optional<CAssignment>)super.asAssignment();
  }

  @Override
  public Optional<CReturnStatement> getRawAST() {
    return Optional.of((CReturnStatement)rawAST);
  }

}
