// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SvLibTermAssignmentCfaStatement extends AExpressionAssignmentStatement
    implements SvLibAssignment {
  @Serial private static final long serialVersionUID = -8354088861609486179L;

  public SvLibTermAssignmentCfaStatement(
      SvLibIdTerm pLeftHandSide, SvLibTerm pRightHandSide, FileLocation pFileLocation) {
    super(pFileLocation, pLeftHandSide, pRightHandSide);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibCfaEdgeStatementVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public SvLibIdTerm getLeftHandSide() {
    return (SvLibIdTerm) super.getLeftHandSide();
  }

  @Override
  public SvLibTerm getRightHandSide() {
    return (SvLibTerm) super.getRightHandSide();
  }
}
