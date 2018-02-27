/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.expressions;

import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Visitor that collects all {@link CIdExpression CIdExpressions} that appear in an expression.
 */
public class IdExpressionCollector
    implements CExpressionVisitor<Set<CSimpleDeclaration>, CPATransferException> {

  @Override
  public Set<CSimpleDeclaration> visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws CPATransferException {
    Set<CSimpleDeclaration> ids = new HashSet<>();
    ids.addAll(pIastArraySubscriptExpression.getArrayExpression().accept(this));
    ids.addAll(pIastArraySubscriptExpression.getSubscriptExpression().accept(this));

    return ids;
  }

  @Override
  public Set<CSimpleDeclaration> visit(CFieldReference pIastFieldReference) throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<CSimpleDeclaration> visit(CIdExpression pIastIdExpression) throws CPATransferException {
    if (pIastIdExpression.getDeclaration() instanceof CVariableDeclaration) {
      return Collections.singleton(pIastIdExpression.getDeclaration());
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public Set<CSimpleDeclaration> visit(CPointerExpression pointerExpression) throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<CSimpleDeclaration> visit(CComplexCastExpression complexCastExpression)
      throws CPATransferException {
    return complexCastExpression.getOperand().accept(this);
  }

  @Override
  public Set<CSimpleDeclaration> visit(CBinaryExpression pIastBinaryExpression)
      throws CPATransferException {
    Set<CSimpleDeclaration> ids = new HashSet<>();
    ids.addAll(pIastBinaryExpression.getOperand1().accept(this));
    ids.addAll(pIastBinaryExpression.getOperand2().accept(this));

    return ids;
  }

  @Override
  public Set<CSimpleDeclaration> visit(CCastExpression pIastCastExpression) throws CPATransferException {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public Set<CSimpleDeclaration> visit(CCharLiteralExpression pIastCharLiteralExpression)
      throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<CSimpleDeclaration> visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<CSimpleDeclaration> visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<CSimpleDeclaration> visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<CSimpleDeclaration> visit(CTypeIdExpression pIastTypeIdExpression)
      throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<CSimpleDeclaration> visit(CUnaryExpression pIastUnaryExpression)
      throws CPATransferException {
    return pIastUnaryExpression.getOperand().accept(this);
  }

  @Override
  public Set<CSimpleDeclaration> visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<CSimpleDeclaration> visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws CPATransferException {
    return Collections.emptySet();
  }
}
