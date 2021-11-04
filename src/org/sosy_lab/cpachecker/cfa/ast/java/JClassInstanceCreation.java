// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;

/**
 * This class represents the class instance creation expression AST node type.
 *
 * <pre>{@code
 * ClassInstanceCreation:
 *       [ Expression . ]
 *           new [ < Type { , Type } > ]
 *           Type ( [ Expression { , Expression } ] )
 *           [ AnonymousClassDeclaration ]
 * }</pre>
 *
 * The functionname is in most cases a {@link JIdExpression}.
 *
 * <p>Not all node arragements will represent legal Java constructs. In particular, it is nonsense
 * if the functionname does not contain a {@link JIdExpression}.
 */
public class JClassInstanceCreation extends JMethodInvocationExpression implements JRightHandSide {

  // TODO refactor to be either abstract or final

  // TODO Type Variables , AnonymousClassDeclaration

  private static final long serialVersionUID = -8480398251628288918L;

  public JClassInstanceCreation(
      FileLocation pFileLocation,
      JClassOrInterfaceType pType,
      JExpression pFunctionName,
      List<? extends JExpression> pParameters,
      JConstructorDeclaration pDeclaration) {

    super(pFileLocation, pType, pFunctionName, pParameters, pDeclaration);
  }

  @Override
  @Nullable
  public JConstructorDeclaration getDeclaration() {
    return (JConstructorDeclaration) super.getDeclaration();
  }

  @Override
  public JClassOrInterfaceType getExpressionType() {
    return (JClassOrInterfaceType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(boolean pQualified) {
    return "new "
        + getExpressionType().toASTString(getFunctionNameExpression().toASTString(pQualified));
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

    if (!(obj instanceof JClassInstanceCreation)) {
      return false;
    }

    return super.equals(obj);
  }
}
