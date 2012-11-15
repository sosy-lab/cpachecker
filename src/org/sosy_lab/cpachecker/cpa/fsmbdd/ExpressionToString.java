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
package org.sosy_lab.cpachecker.cpa.fsmbdd;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;


public class ExpressionToString implements CExpressionVisitor<String, RuntimeException> {

  @Override
  public String visit(CArraySubscriptExpression pIastArraySubscriptExpression) throws RuntimeException {
    throw new RuntimeException("CArraySubscriptExpression not supported.");
  }

  @Override
  public String visit(CBinaryExpression pIastBinaryExpression) throws RuntimeException {
    return String.format("(%s %s %s)",
        pIastBinaryExpression.getOperand1().accept(this),
        pIastBinaryExpression.getOperator().getOperator(),
        pIastBinaryExpression.getOperand2().accept(this));
  }

  @Override
  public String visit(CCastExpression pIastCastExpression) throws RuntimeException {
    throw new RuntimeException("CCastExpression not supported.");
  }

  @Override
  public String visit(CFieldReference pIastFieldReference) throws RuntimeException {
    throw new RuntimeException("CFieldReference not supported.");
  }

  @Override
  public String visit(CIdExpression pIastIdExpression) throws RuntimeException {
    return pIastIdExpression.getName();
  }

  @Override
  public String visit(CCharLiteralExpression pIastCharLiteralExpression) throws RuntimeException {
    return pIastCharLiteralExpression.getValue().toString();
  }

  @Override
  public String visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws RuntimeException {
    return pIastFloatLiteralExpression.getValue().toString();
  }

  @Override
  public String visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws RuntimeException {
    return pIastIntegerLiteralExpression.getValue().toString();
  }

  @Override
  public String visit(CStringLiteralExpression pIastStringLiteralExpression) throws RuntimeException {
    return pIastStringLiteralExpression.getValue();
  }

  @Override
  public String visit(CTypeIdExpression pIastTypeIdExpression) throws RuntimeException {
    throw new RuntimeException("CTypeIdExpression not supported.");
  }

  @Override
  public String visit(CUnaryExpression pIastUnaryExpression) throws RuntimeException {
    return String.format("%s(%s)", pIastUnaryExpression.getOperator().getOperator(), pIastUnaryExpression.getOperand().accept(this));
  }

}
