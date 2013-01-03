/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.ast.java;

import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;


public class JMethodInvocationExpression extends AFunctionCallExpression implements JRightHandSide {

  private boolean hasKnownRunTimeBinding = false;
  private JClassType runTimeBinding = null;

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

  public JClassOrInterfaceType getDeclaringClassType() {
    return getDeclaration().getDeclaringClass();
  }

  public JClassType getRunTimeBinding() {
    return runTimeBinding;
  }

  public void setRunTimeBinding(JClassType runTimeBinding) {
    this.runTimeBinding = runTimeBinding;
    hasKnownRunTimeBinding = true;
  }

  public boolean hasKnownRunTimeBinding() {
    return hasKnownRunTimeBinding;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (hasKnownRunTimeBinding ? 1231 : 1237);
    result = prime * result + ((runTimeBinding == null) ? 0 : runTimeBinding.hashCode());
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (!super.equals(obj)) { return false; }
    if (!(obj instanceof JMethodInvocationExpression)) { return false; }
    JMethodInvocationExpression other = (JMethodInvocationExpression) obj;
    if (hasKnownRunTimeBinding != other.hasKnownRunTimeBinding) { return false; }
    if (runTimeBinding == null) {
      if (other.runTimeBinding != null) { return false; }
    } else if (!runTimeBinding.equals(other.runTimeBinding)) { return false; }

    return super.equals(other);
  }

}