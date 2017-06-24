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

import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.AddExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Addition;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Constant;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.ExpoExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.MultExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Multiplication;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.PolynomExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Variable;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.visitors.Visitor.NoException;

public class LoopInvariantsExpressionVisitor
    implements CExpressionVisitor<PolynomExpression, NoException> {

  public LoopInvariantsExpressionVisitor() {}

  @Override
  public PolynomExpression visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
    return pIastArraySubscriptExpression.accept(this); //oder new Variable

  }

  @Override
  public PolynomExpression visit(CFieldReference pIastFieldReference) {
    //not necessary
    throw new UnsupportedOperationException("CFieldReference");
  }

  @Override
  public PolynomExpression visit(CIdExpression pIastIdExpression) {
    String variableName = pIastIdExpression.getDeclaration().getName() + "(n)";
    if (variableName.contains("CPAchecker")) {
      return null;
    } else {
      return new Variable(variableName);
    }
  }

  @Override
  public PolynomExpression visit(CPointerExpression pPointerExpression) {
    //not necessary
    throw new UnsupportedOperationException("CPointerExpression");
  }

  @Override
  public PolynomExpression visit(CComplexCastExpression pComplexCastExpression) {
    //not necessary
    throw new UnsupportedOperationException("CComplexCastExpression");
  }

  @Override
  public PolynomExpression visit(CBinaryExpression pIastBinaryExpression) {
    CBinaryExpression binExpr = pIastBinaryExpression;
    CExpression operand1 = binExpr.getOperand1();
    CExpression operand2 = binExpr.getOperand2();
    BinaryOperator operator = binExpr.getOperator();

    PolynomExpression op1Exp = operand1.accept(this);
    PolynomExpression op2Exp = operand2.accept(this);

    if (op1Exp != null && op2Exp != null) {
      switch (operator.getOperator()) {
        case "+":
          return new Addition((MultExpression) op1Exp,
              (AddExpression) operand2.accept(this));
        case "-":
          return new Addition((MultExpression) op1Exp,
              new Multiplication(new Constant(-1), (MultExpression) op2Exp));
        //              if (operand1.accept(this) instanceof Variable && operand2.accept(this) )
        //              return new Exponent((Variable) operand1.accept(this), operand2.accept(this));
        case "*":
          return new Multiplication((ExpoExpression) op1Exp,
              (MultExpression) op2Exp);
        default:
          return null;
      }
    } else {
      return null;
    }
  }

  @Override
  public PolynomExpression visit(CCastExpression pIastCastExpression) {
    //not necessary
    throw new UnsupportedOperationException("CCastExpression");
  }

  @Override
  public PolynomExpression visit(CCharLiteralExpression pIastCharLiteralExpression) {
    //not necessary
    throw new UnsupportedOperationException("CCharLiteralExpression");
  }

  @Override
  public PolynomExpression visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
    return new Constant(pIastFloatLiteralExpression.getValue().doubleValue());
  }

  @Override
  public PolynomExpression visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
    return new Constant(pIastIntegerLiteralExpression.getValue().doubleValue());
  }

  @Override
  public PolynomExpression visit(CStringLiteralExpression pIastStringLiteralExpression) {
    //not necessary
    throw new UnsupportedOperationException("CStringLiteralExpression");
  }

  @Override
  public PolynomExpression visit(CTypeIdExpression pIastTypeIdExpression) {
    //not necessary
    throw new UnsupportedOperationException("CTypedIdExpression");
  }

  @Override
  public PolynomExpression visit(CUnaryExpression pIastUnaryExpression) {
    CExpression operand = pIastUnaryExpression.getOperand();
    UnaryOperator operator = pIastUnaryExpression.getOperator();
    String op = operator.getOperator();
    PolynomExpression expr = operand.accept(this);
    if (expr != null) {
      if (op.equals("-") && expr instanceof MultExpression) { return new Multiplication(
          new Constant(-1), (MultExpression) expr); }
      throw new UnsupportedOperationException("Not supported unary Operation: " + op);
    } else {
      return null;
    }
  }

  @Override
  public PolynomExpression visit(CImaginaryLiteralExpression PIastLiteralExpression)  {
    //not necessary
    throw new UnsupportedOperationException("CImaginaryLiteralExpression");
  }

  @Override
  public PolynomExpression visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
    //not necessary
    throw new UnsupportedOperationException("CAddressOfLabelExpression");
  }
}
