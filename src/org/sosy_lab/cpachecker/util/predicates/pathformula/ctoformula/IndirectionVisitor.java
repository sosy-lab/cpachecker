/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.types.CtoFormulaTypeUtils;

class IndirectionVisitor implements CExpressionVisitor<Integer, RuntimeException> {

  private static final IndirectionVisitor indirectionVisitor = new IndirectionVisitor();

  public static int getIndirectionLevel(CExpression c) {
    return c.accept(indirectionVisitor);
  }

  @Override
  public Integer visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
    return pIastArraySubscriptExpression
        .getArrayExpression().accept(this) + 1;
  }

  @Override
  public Integer visit(CBinaryExpression pIastBinaryExpression) {
    return
    Math.max(
        pIastBinaryExpression.getOperand1().accept(this),
        pIastBinaryExpression.getOperand2().accept(this));
  }

  @Override
  public Integer visit(CCastExpression pIastCastExpression) {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public Integer visit(CComplexCastExpression pIastCastExpression) {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public Integer visit(CFieldReference pIastFieldReference) {
    return CtoFormulaTypeUtils.getRealFieldOwner(pIastFieldReference).accept(this);
  }

  @Override
  public Integer visit(CIdExpression pIastIdExpression) {
    return 0;
  }

  @Override
  public Integer visit(CCharLiteralExpression pIastCharLiteralExpression) {
    return 0;
  }

  @Override
  public Integer visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
    return 0;
  }

  @Override
  public Integer visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
    return 0;
  }

  @Override
  public Integer visit(CImaginaryLiteralExpression pIastLiteralExpression) {
    return 0;
  }

  @Override
  public Integer visit(CStringLiteralExpression pIastStringLiteralExpression) {
    return 0;
  }

  @Override
  public Integer visit(CTypeIdExpression pIastTypeIdExpression) {
    return 0;
  }

  @Override
  public Integer visit(CTypeIdInitializerExpression pCTypeIdInitializerExpression) {
    return 0;
  }

  @Override
  public Integer visit(CUnaryExpression pIastUnaryExpression) {
    return pIastUnaryExpression.getOperand().accept(this) + 1;
  }

  @Override
  public Integer visit(CPointerExpression pIastUnaryExpression) {
    return pIastUnaryExpression.getOperand().accept(this) + 1;
  }
}