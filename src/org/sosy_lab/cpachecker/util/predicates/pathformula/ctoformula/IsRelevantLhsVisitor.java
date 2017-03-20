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
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;


public class IsRelevantLhsVisitor extends DefaultCExpressionVisitor<Boolean, RuntimeException> {

  private final CtoFormulaConverter conv;

  public IsRelevantLhsVisitor(CtoFormulaConverter pConv) {
    conv = pConv;
  }

  @Override
  public Boolean visit(final CArraySubscriptExpression e) {
    return e.getArrayExpression().accept(this);
  }

  @Override
  public Boolean visit(final CCastExpression e) {
    CType resultType = e.getExpressionType();
    CExpression operand = e.getOperand();
    if (resultType instanceof CPointerType && operand instanceof CIntegerLiteralExpression &&
        ((CIntegerLiteralExpression)operand).asLong() != 0) {
      return false;
    } else {
      return operand.accept(this);
    }
  }

  @Override
  public Boolean visit(final CComplexCastExpression e) {
    return e.getOperand().accept(this);
  }

  @Override
  public Boolean visit(final CFieldReference e) {
    if (!e.getFieldOwner().accept(this)) {
      return false;
    }
    CType fieldOwnerType = e.getFieldOwner().getExpressionType().getCanonicalType();
    if (fieldOwnerType instanceof CPointerType) {
      fieldOwnerType = ((CPointerType) fieldOwnerType).getType();
    }
    assert fieldOwnerType instanceof CCompositeType : "Field owner should have composite type";
    return conv.isRelevantField((CCompositeType) fieldOwnerType, e.getFieldName());
  }

  @Override
  public Boolean visit(final CIdExpression e) {
    CSimpleDeclaration sDecl = e.getDeclaration();
    if (sDecl instanceof CVariableDeclaration) {
      if (((CVariableDeclaration)sDecl).isGlobal()) {
        return false;
      }
    }
    return conv.isRelevantVariable(e.getDeclaration());
  }

  @Override
  public Boolean visit(CPointerExpression e) {
    return e.getOperand().accept(this);
  }

  @Override
  public Boolean visit(CBinaryExpression e) {
    return e.getOperand1().accept(this) && e.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(CIntegerLiteralExpression e) {
    return true;
  }

  @Override
  public Boolean visit(CStringLiteralExpression e) {
    return true;
  }

  @Override
  public Boolean visit(CCharLiteralExpression e) {
    return true;
  }


  @Override
  public Boolean visit(CFloatLiteralExpression e) {
   return true;
  }

  @Override
  public Boolean visit(CTypeIdExpression e) {
    return true;
  }

  @Override
  public Boolean visit(CUnaryExpression e) throws RuntimeException {
    // Inside casts an arbitrary expression may appear on the LHS
    return e.getOperand().accept(this);
  }

  @Override
  protected Boolean visitDefault(CExpression e) {
    throw new IllegalArgumentException("Undexpected left hand side: " + e.toString());
  }
}
