// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class PointerApproximatingVisitor
    extends DefaultCExpressionVisitor<Optional<PointerBase>, UnrecognizedCodeException>
    implements CRightHandSideVisitor<Optional<PointerBase>, UnrecognizedCodeException> {

  private final TypeHandlerWithPointerAliasing typeHandler;
  private final CFAEdge edge;

  PointerApproximatingVisitor(TypeHandlerWithPointerAliasing pTypeHandler, CFAEdge pEdge) {
    typeHandler = pTypeHandler;
    edge = pEdge;
  }

  @Override
  public Optional<PointerBase> visit(CArraySubscriptExpression e) throws UnrecognizedCodeException {
    return e.getArrayExpression().accept(this);
  }

  @Override
  public Optional<PointerBase> visit(CBinaryExpression e) throws UnrecognizedCodeException {
    final CType t = typeHandler.getSimplifiedType(e);
    if (t instanceof CPointerType || t instanceof CArrayType) {
      return e.getOperand1().accept(this);
    }
    return Optional.empty();
  }

  @Override
  public Optional<PointerBase> visit(CCastExpression e) throws UnrecognizedCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public Optional<PointerBase> visit(final CFieldReference e) throws UnrecognizedCodeException {
    CType t = typeHandler.getSimplifiedType(e.withExplicitPointerDereference().getFieldOwner());
    if (t instanceof CCompositeType cCompositeType) {
      return Optional.of(new PointerBase(getFieldAccessName(cCompositeType.getQualifiedName(), e)));
    } else {
      throw new UnrecognizedCodeException("Field owner of a non-composite type", edge, e);
    }
  }

  @Override
  public Optional<PointerBase> visit(CIdExpression e) throws UnrecognizedCodeException {
    return Optional.of(new PointerBase(e.getDeclaration()));
  }

  @Override
  public Optional<PointerBase> visit(CPointerExpression e) throws UnrecognizedCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public Optional<PointerBase> visit(CUnaryExpression e) throws UnrecognizedCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  protected Optional<PointerBase> visitDefault(CExpression pExp) {
    return Optional.empty();
  }

  @Override
  public Optional<PointerBase> visit(CFunctionCallExpression call)
      throws UnrecognizedCodeException {
    return Optional.empty();
  }
}
