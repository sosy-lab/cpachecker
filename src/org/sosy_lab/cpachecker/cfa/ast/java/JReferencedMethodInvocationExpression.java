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

import org.sosy_lab.cpachecker.cfa.ast.CFileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;


public class JReferencedMethodInvocationExpression extends JMethodInvocationExpression {

  private final JSimpleDeclaration referencedVariable;

  private boolean hasKnownRunTimeBinding = false;
  private JClassType runTimeBinding = null;

  public JReferencedMethodInvocationExpression(CFileLocation pFileLocation, JType pType, JExpression pFunctionName,
      List<? extends JExpression> pParameters, JSimpleDeclaration pDeclaration, JSimpleDeclaration pReferencedVariable) {
    super(pFileLocation, pType, pFunctionName, pParameters, pDeclaration);
      referencedVariable = pReferencedVariable;

  }

  public JSimpleDeclaration getReferencedVariable() {
    return referencedVariable;
  }

  @Override
  public String toASTString() {
    return referencedVariable.getName() + "_" + super.toASTString();
  }

  public void setHasKnownRunTimeBinding(boolean hasKnownRunTimeBinding) {
    this.hasKnownRunTimeBinding = hasKnownRunTimeBinding;
  }

  public JClassType getRunTimeBinding() {
    return runTimeBinding;
  }

  public void setRunTimeBinding(JClassType runTimeBinding) {
    this.runTimeBinding = runTimeBinding;
  }

  public boolean hasKnownRunTimeBinding() {
      return hasKnownRunTimeBinding;
  }

}