// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.io.Serial;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class represents the qualified method invocation expression AST node type.
 *
 * <p>Qualified MethodInvocation:
 *
 * <pre>{@code
 * Expression .
 *    [ < Type { , Type } > ]
 *    Identifier ( [ Expression { , Expression } ] )
 *
 * }</pre>
 *
 * Note that the qualification only consist of variables. In the CFA, all method names are
 * transformed to have unique names.
 */
public final class JReferencedMethodInvocationExpression extends JMethodInvocationExpression {

  @Serial private static final long serialVersionUID = -3779312927011479073L;
  private final JIdExpression qualifier;

  public JReferencedMethodInvocationExpression(
      FileLocation pFileLocation,
      JType pType,
      JExpression pFunctionName,
      List<? extends JExpression> pParameters,
      JMethodDeclaration pDeclaration,
      JIdExpression pQualifier) {
    super(pFileLocation, pType, pFunctionName, pParameters, pDeclaration);
    qualifier = pQualifier;
  }

  public JIdExpression getReferencedVariable() {
    return qualifier;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return qualifier.toASTString(pAAstNodeRepresentation)
        + "_"
        + super.toASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(qualifier);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof JReferencedMethodInvocationExpression other
        && super.equals(obj)
        && Objects.equals(other.qualifier, qualifier);
  }
}
