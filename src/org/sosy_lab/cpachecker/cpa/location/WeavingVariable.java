/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.location;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;

public class WeavingVariable {
  public WeavingVariable(
      CVariableDeclaration pVarDecl,
      CExpressionAssignmentStatement pIncrement,
      CExpression pAssumption) {
    this.varDecl = pVarDecl;
    this.increment = pIncrement;
    this.assumption = pAssumption;
  }

  CVariableDeclaration varDecl;
  CExpressionAssignmentStatement increment;
  CExpression assumption;

  public CVariableDeclaration getVarDecl() {
    return varDecl;
  }

  public CExpressionAssignmentStatement getIncrement() {
    return increment;
  }

  public CExpression getAssumption() {
    return assumption;
  }
}