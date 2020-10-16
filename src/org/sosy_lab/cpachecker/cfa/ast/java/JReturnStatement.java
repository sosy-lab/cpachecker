// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import com.google.common.base.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AbstractReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 *  This class represents the return statement AST node type.
 *
 *  ReturnStatement:
 *   return [ Expression ] ;
 *
 */
public class JReturnStatement extends AbstractReturnStatement implements JAstNode {

  // TODO refactor to be either abstract or final

  private static final long serialVersionUID = -7073556363348785665L;

  public JReturnStatement(FileLocation pFileLocation, Optional<JExpression> pExpression) {
    // TODO We absolutely need a correct assignment here that assigns pExpression to a special variable with the return type of the function.
    super(pFileLocation, pExpression, Optional.absent());

  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<JExpression> getReturnValue() {
    return (Optional<JExpression>) super.getReturnValue();
  }

  @Override
  public <R, X extends Exception> R accept(JAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JReturnStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
