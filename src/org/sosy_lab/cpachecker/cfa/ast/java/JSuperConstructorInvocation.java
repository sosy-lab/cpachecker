// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;

/**
 * This class represents the super constructor invocation statement AST node type.
 *
 * SuperConstructorInvocation:
 *    [ Expression . ]
 *        [ < Type { , Type } > ]
 *        super ( [ Expression { , Expression } ] ) ;
 *
 */
public final class JSuperConstructorInvocation extends JClassInstanceCreation {

  private static final long serialVersionUID = 1241406733020430434L;

  public JSuperConstructorInvocation(FileLocation pFileLocation, JClassType pType, JExpression pFunctionName,
      List<? extends JExpression> pParameters, JConstructorDeclaration pDeclaration) {
    super(pFileLocation, pType, pFunctionName, pParameters, pDeclaration);

  }

  @Override
  public String toASTString(boolean pQualified) {
    return toASTString();
  }

  @Override
  public String toASTString() {
    return getExpressionType().toASTString("super");
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

    if (!(obj instanceof JSuperConstructorInvocation)) {
      return false;
    }

    return super.equals(obj);
  }
}
