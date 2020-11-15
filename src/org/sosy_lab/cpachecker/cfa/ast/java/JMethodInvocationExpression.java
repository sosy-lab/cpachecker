// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class represents the unqualified method invocation expression AST node type.
 *
 * Unqualified MethodInvocation:
 *        [ < Type { , Type } > ]
 *        Identifier ( [ Expression { , Expression } ] )
 *
 * Note that in the cfa, all method names are transformed to have unique names.
 * It is therefore unnecessary to have Qualifiers for methods with the same simple name.
 */
public class JMethodInvocationExpression extends AFunctionCallExpression implements JRightHandSide {

  // TODO refactor to be either abstract or final

  //TODO Type parameters

  private static final long serialVersionUID = 4603127283599981678L;
  // TODO erase these two fields and change the algorithm to find known run time type bindings,
  private boolean hasKnownRunTimeBinding = false;
  private JClassOrInterfaceType runTimeBinding = null;

  public JMethodInvocationExpression(FileLocation pFileLocation, JType pType, JExpression pFunctionName,
      List<? extends JExpression> pParameters, JMethodDeclaration pDeclaration) {
    super(pFileLocation, pType, pFunctionName, pParameters, pDeclaration);
  }

  @Override
  public JExpression getFunctionNameExpression() {
    return (JExpression) super.getFunctionNameExpression();
  }

  @Override
  public JType getExpressionType() {

    return (JType) super.getExpressionType();
  }

  @Override
  public JMethodDeclaration getDeclaration() {
    return (JMethodDeclaration) super.getDeclaration();
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<? extends JExpression> getParameterExpressions() {
    return (List<? extends JExpression>) super.getParameterExpressions();
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public JClassOrInterfaceType getDeclaringType() {
    return getDeclaration().getDeclaringClass();
  }

  public JClassOrInterfaceType getRunTimeBinding() {
    return runTimeBinding;
  }

  public void setRunTimeBinding(JClassOrInterfaceType runTimeBinding) {
    this.runTimeBinding = runTimeBinding;
    hasKnownRunTimeBinding = true;
  }

  public boolean hasKnownRunTimeBinding() {
    return hasKnownRunTimeBinding;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + (hasKnownRunTimeBinding ? 1231 : 1237);
    result = prime * result + Objects.hashCode(runTimeBinding);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JMethodInvocationExpression)
        || !super.equals(obj)) {
      return false;
    }

    JMethodInvocationExpression other = (JMethodInvocationExpression) obj;

    return other.hasKnownRunTimeBinding == hasKnownRunTimeBinding
            && Objects.equals(other.runTimeBinding, runTimeBinding);
  }
}