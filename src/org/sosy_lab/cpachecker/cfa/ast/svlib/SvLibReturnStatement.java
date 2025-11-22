// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AbstractReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SvLibReturnStatement extends AbstractReturnStatement implements SvLibAstNode {
  @Serial private static final long serialVersionUID = 1094313136965912314L;

  private SvLibReturnStatement(
      FileLocation pFileLocation,
      SvLibIdTermTuple pReturnExpression,
      SvLibAssignment pAssignmentOfReturnValue) {
    super(pFileLocation, Optional.of(pReturnExpression), Optional.of(pAssignmentOfReturnValue));
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Optional<SvLibIdTermTuple> getReturnValue() {
    return (Optional<SvLibIdTermTuple>) super.getReturnValue();
  }
}
