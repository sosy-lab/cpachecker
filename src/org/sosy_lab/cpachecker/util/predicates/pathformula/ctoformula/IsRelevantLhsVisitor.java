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
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;


class IsRelevantLhsVisitor extends DefaultCExpressionVisitor<Boolean, RuntimeException> {

  private final CtoFormulaConverter conv;

  IsRelevantLhsVisitor(CtoFormulaConverter pConv) {
    conv = pConv;
  }

  @Override
  public Boolean visit(final CArraySubscriptExpression e) {
    return e.getArrayExpression().accept(this);
  }

  @Override
  public Boolean visit(final CCastExpression e) {
    return e.getOperand().accept(this);
  }

  @Override
  public Boolean visit(final CComplexCastExpression e) {
    return e.getOperand().accept(this);
  }

  @Override
  public Boolean visit(final CFieldReference e) {
    CType fieldOwnerType = e.getFieldOwner().getExpressionType().getCanonicalType();
    if (fieldOwnerType instanceof CPointerType) {
      fieldOwnerType = ((CPointerType) fieldOwnerType).getType();
    }
    assert fieldOwnerType instanceof CCompositeType : "Field owner should have composite type";
    return conv.isRelevantField((CCompositeType) fieldOwnerType, e.getFieldName());
  }

  @Override
  public Boolean visit(final CIdExpression e) {
    return conv.isRelevantVariable(e.getDeclaration());
  }

  @Override
  public Boolean visit(CPointerExpression e) {
    return true;
  }

  @Override
  protected Boolean visitDefault(CExpression e) {
    throw new IllegalArgumentException("Undexpected left hand side: " + e.toString());
  }
}
