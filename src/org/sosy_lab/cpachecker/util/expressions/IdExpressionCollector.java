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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Visitor that collects all {@link CIdExpression CIdExpressions} that appear in an expression. */
public class IdExpressionCollector
    implements CExpressionVisitor<Set<MemoryLocation>, CPATransferException> {

  @Override
  public Set<MemoryLocation> visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws CPATransferException {
    Set<MemoryLocation> ids = new HashSet<>();
    ids.addAll(pIastArraySubscriptExpression.getArrayExpression().accept(this));
    ids.addAll(pIastArraySubscriptExpression.getSubscriptExpression().accept(this));

    return ids;
  }

  @Override
  public Set<MemoryLocation> visit(CFieldReference pIastFieldReference)
      throws CPATransferException {
    return pIastFieldReference.getFieldOwner().accept(this);
  }

  @Override
  public Set<MemoryLocation> visit(CIdExpression pIastIdExpression) throws CPATransferException {
    CSimpleDeclaration idDeclaration = pIastIdExpression.getDeclaration();
    if (idDeclaration instanceof CVariableDeclaration || idDeclaration instanceof CParameterDeclaration) {
      return Collections.singleton(MemoryLocation.valueOf(idDeclaration.getQualifiedName()));
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public Set<MemoryLocation> visit(CPointerExpression pointerExpression)
      throws CPATransferException {
    return pointerExpression.getOperand().accept(this);
  }

  @Override
  public Set<MemoryLocation> visit(CComplexCastExpression complexCastExpression)
      throws CPATransferException {
    return complexCastExpression.getOperand().accept(this);
  }

  @Override
  public Set<MemoryLocation> visit(CBinaryExpression pIastBinaryExpression)
      throws CPATransferException {
    Set<MemoryLocation> ids = new HashSet<>();
    ids.addAll(pIastBinaryExpression.getOperand1().accept(this));
    ids.addAll(pIastBinaryExpression.getOperand2().accept(this));

    return ids;
  }

  @Override
  public Set<MemoryLocation> visit(CCastExpression pIastCastExpression)
      throws CPATransferException {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public Set<MemoryLocation> visit(CCharLiteralExpression pIastCharLiteralExpression)
      throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visit(CTypeIdExpression pIastTypeIdExpression)
      throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visit(CUnaryExpression pIastUnaryExpression)
      throws CPATransferException {
    return pIastUnaryExpression.getOperand().accept(this);
  }

  @Override
  public Set<MemoryLocation> visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws CPATransferException {
    return Collections.emptySet();
  }
}
