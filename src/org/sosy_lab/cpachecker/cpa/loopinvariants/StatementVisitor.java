/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.loopinvariants;

import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.AddExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Addition;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Constant;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Multiplication;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.PolynomExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Variable;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.visitors.Visitor.NoException;

public class StatementVisitor implements AStatementVisitor< PolynomExpression, NoException> {

  public StatementVisitor() { }

  @Override
  public PolynomExpression visit(AExpressionAssignmentStatement pAExpressionAssignmentStatement) {
    LoopInvariantsExpressionVisitor expressionVisitor = new LoopInvariantsExpressionVisitor();

    ALeftHandSide leftExpr = pAExpressionAssignmentStatement.getLeftHandSide();
    if (leftExpr instanceof AIdExpression) {
      AIdExpression idExpr = (AIdExpression) leftExpr;
      String f2 = idExpr.getName();
      ASimpleDeclaration declariation = idExpr.getDeclaration();
      f2 = declariation.getName() + "(n+1)";

      CExpression rightExpr = (CExpression) pAExpressionAssignmentStatement.getRightHandSide();
      PolynomExpression poly = rightExpr.accept(expressionVisitor);

      return new Addition(new Multiplication(new Constant(-1), new Variable(f2)), (AddExpression) poly);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public PolynomExpression visit(AExpressionStatement pAExpressionStatement){
    throw new UnsupportedOperationException();
  }

  @Override
  public PolynomExpression visit(AFunctionCallAssignmentStatement pAFunctionCallAssignmentStatement) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PolynomExpression visit(AFunctionCallStatement pAFunctionCallStatement) {
    throw new UnsupportedOperationException();
  }

}
