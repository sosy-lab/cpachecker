// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SvLibFunctionCallAssignmentStatement extends AFunctionCallAssignmentStatement
    implements SvLibCfaEdgeStatement, SvLibAssignment, AFunctionCall {
  @Serial private static final long serialVersionUID = 7628340467069968610L;

  public SvLibFunctionCallAssignmentStatement(
      FileLocation pFileLocation,
      SvLibIdTermTuple pLeftHandSide,
      SvLibFunctionCallExpression pRightHandSide) {
    super(pFileLocation, pLeftHandSide, pRightHandSide);
  }

  @Override
  public SvLibFunctionCallExpression getFunctionCallExpression() {
    return (SvLibFunctionCallExpression) super.getFunctionCallExpression();
  }

  @Override
  public SvLibIdTermTuple getLeftHandSide() {
    return (SvLibIdTermTuple) super.getLeftHandSide();
  }

  @Override
  public SvLibFunctionCallExpression getRightHandSide() {
    return (SvLibFunctionCallExpression) super.getRightHandSide();
  }

  @Override
  public <R, X extends Exception> R accept(SvLibCfaEdgeStatementVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }
}
