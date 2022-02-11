// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.java;

import com.google.common.base.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JAssignment;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JReturnStatement;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class JReturnStatementEdge extends AReturnStatementEdge {


  private static final long serialVersionUID = -6303184113368127372L;

  public JReturnStatementEdge(String pRawStatement, JReturnStatement pRawAST,
      FileLocation pFileLocation, CFANode pPredecessor, FunctionExitNode pSuccessor) {

    super(pRawStatement, pRawAST, pFileLocation, pPredecessor, pSuccessor);

  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<JExpression> getExpression() {
    return (Optional<JExpression>)rawAST.getReturnValue();
  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<JAssignment> asAssignment() {
    return (Optional<JAssignment>)super.asAssignment();
  }

  @Override
  public Optional<JReturnStatement> getRawAST() {
    return Optional.of((JReturnStatement)rawAST);
  }

}
