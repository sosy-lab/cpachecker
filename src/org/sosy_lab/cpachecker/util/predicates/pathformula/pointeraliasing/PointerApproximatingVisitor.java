/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing.getFieldAccessName;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

class PointerApproximatingVisitor
    extends DefaultCExpressionVisitor<Optional<String>, UnrecognizedCCodeException>
    implements CRightHandSideVisitor<Optional<String>, UnrecognizedCCodeException> {

  private final TypeHandlerWithPointerAliasing typeHandler;
  private final CFAEdge edge;

  PointerApproximatingVisitor(TypeHandlerWithPointerAliasing pTypeHandler, CFAEdge pEdge) {
    typeHandler = pTypeHandler;
    edge = pEdge;
  }

  @Override
  public Optional<String> visit(CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    return e.getArrayExpression().accept(this);
  }

  @Override
  public Optional<String> visit(CBinaryExpression e) throws UnrecognizedCCodeException {
    final CType t = typeHandler.getSimplifiedType(e);
    if (t instanceof CPointerType || t instanceof CArrayType) {
      return e.getOperand1().accept(this);
    }
    return Optional.empty();
  }

  @Override
  public Optional<String> visit(CCastExpression e) throws UnrecognizedCCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public Optional<String> visit(final CFieldReference e) throws UnrecognizedCCodeException {
    CType t = typeHandler.getSimplifiedType(e.withExplicitPointerDereference().getFieldOwner());
    if (t instanceof CCompositeType) {
      return Optional.of(getFieldAccessName(((CCompositeType) t).getQualifiedName(), e));
    } else {
      throw new UnrecognizedCCodeException("Field owner of a non-composite type", edge, e);
    }
  }

  @Override
  public Optional<String> visit(CIdExpression e) throws UnrecognizedCCodeException {
    return Optional.of(e.getDeclaration().getQualifiedName());
  }

  @Override
  public Optional<String> visit(CPointerExpression e) throws UnrecognizedCCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public Optional<String> visit(CUnaryExpression e) throws UnrecognizedCCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  protected Optional<String> visitDefault(CExpression pExp) throws RuntimeException {
    return Optional.empty();
  }

  @Override
  public Optional<String> visit(CFunctionCallExpression call) throws UnrecognizedCCodeException {
    return Optional.empty();
  }
}
