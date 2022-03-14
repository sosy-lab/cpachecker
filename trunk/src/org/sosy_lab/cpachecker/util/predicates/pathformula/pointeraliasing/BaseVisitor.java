// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing.getFieldAccessName;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class BaseVisitor extends DefaultCExpressionVisitor<Variable, UnrecognizedCodeException> {

  BaseVisitor(
      final CFAEdge cfaEdge,
      final PointerTargetSetBuilder pts,
      final TypeHandlerWithPointerAliasing pTypeHandler) {
    this.cfaEdge = cfaEdge;
    this.pts = pts;
    typeHandler = pTypeHandler;
  }

  @Override
  protected Variable visitDefault(CExpression pExp) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("unexpected expression in lvalue", cfaEdge, pExp);
  }

  @Override
  public Variable visit(final CArraySubscriptExpression e) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public Variable visit(final CBinaryExpression e) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public Variable visit(final CCastExpression e) throws UnrecognizedCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public Variable visit(final CComplexCastExpression e) throws UnrecognizedCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public Variable visit(CFieldReference e) throws UnrecognizedCodeException {
    e = e.withExplicitPointerDereference();

    final Variable base = e.getFieldOwner().accept(this);
    if (base != null) {
      return Variable.create(
          getFieldAccessName(base.getName(), e), typeHandler.getSimplifiedType(e));
    } else {
      return null;
    }
  }

  @Override
  public Variable visit(final CIdExpression e) throws UnrecognizedCodeException {
    CType type = typeHandler.getSimplifiedType(e);
    if (!pts.isActualBase(e.getDeclaration().getQualifiedName())
        && !CTypeUtils.containsArray(type, e.getDeclaration())) {
      lastBase = Variable.create(e.getDeclaration().getQualifiedName(), type);
      return lastBase;
    } else {
      return null;
    }
  }

  @Override
  public Variable visit(final CIntegerLiteralExpression e) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public Variable visit(final CPointerExpression e) throws UnrecognizedCodeException {
    return null;
  }

  @Nullable Variable getLastBase() {
    return lastBase;
  }

  private final PointerTargetSetBuilder pts;
  private final TypeHandlerWithPointerAliasing typeHandler;
  private final CFAEdge cfaEdge;

  private @Nullable Variable lastBase = null;
}
