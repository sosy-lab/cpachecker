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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;


class BaseVisitor implements CExpressionVisitor<Variable, UnrecognizedCCodeException>{

  public BaseVisitor(final CToFormulaWithUFConverter conv, final CFAEdge cfaEdge, final PointerTargetSetBuilder pts) {
    this.conv = conv;
    this.cfaEdge = cfaEdge;
    this.pts = pts;
  }

  @Override
  public Variable visit(final CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Variable visit(final CBinaryExpression e) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Variable visit(final CCastExpression e) throws UnrecognizedCCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public Variable visit(final CComplexCastExpression e) throws UnrecognizedCCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public Variable visit(CFieldReference e) throws UnrecognizedCCodeException {

    e = ExpressionToFormulaWithUFVisitor.eliminateArrow(e, cfaEdge);

    final Variable base = e.getFieldOwner().accept(this);
    if (base != null) {
      return Variable.create(base.getName()  + CToFormulaWithUFConverter.FIELD_NAME_SEPARATOR + e.getFieldName(),
                             PointerTargetSet.simplifyType(e.getExpressionType()));
    } else {
      return null;
    }
  }

  @Override
  public Variable visit(final CIdExpression e) throws UnrecognizedCCodeException {
    if (!pts.isActualBase(e.getDeclaration().getQualifiedName()) &&
        !PointerTargetSet.containsArray(PointerTargetSet.simplifyType(e.getExpressionType()))) {
      return lastBase = conv.scopedIfNecessary(e, null, null);
    } else {
      return null;
    }
  }

  @Override
  public Variable visit(final CCharLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Char literal in place of lvalue", cfaEdge, e);
  }

  @Override
  public Variable visit(final CFloatLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Float literal in place of lvalue", cfaEdge, e);
  }

  @Override
  public Variable visit(final CIntegerLiteralExpression e) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Variable visit(final CStringLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("String literal in place of lvalue", cfaEdge, e);
  }

  @Override
  public Variable visit(CImaginaryLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Imaginary literal in place of lvalue", cfaEdge, e);
  }

  @Override
  public Variable visit(final CTypeIdExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("TypeId in place of lvalue", cfaEdge, e);
  }

  @Override
  public Variable visit(final CTypeIdInitializerExpression e) throws UnrecognizedCCodeException {
    // TODO: Type id initializers should be supported
    throw new UnrecognizedCCodeException("Typeid initializers are currently unsupported", cfaEdge);
  }

  @Override
  public Variable visit(final CUnaryExpression e) throws UnrecognizedCCodeException {
    switch (e.getOperator()) {
    case AMPER:
      throw new UnrecognizedCCodeException("Address in place of lvalue", cfaEdge, e);
    case NOT:
    case TILDE:
    case MINUS:
    case PLUS:
      throw new UnrecognizedCCodeException("Arithmetic in place of lvalue", cfaEdge, e);
    case SIZEOF:
      throw new UnrecognizedCCodeException("Constant in place of lvalue", cfaEdge, e);
    default:
      throw new UnrecognizedCCodeException("Unrecognized code in place of lvalue", cfaEdge, e);
    }
  }

  @Override
  public Variable visit(final CPointerExpression e) throws UnrecognizedCCodeException {
    return null;
  }

  public Variable getLastBase() {
    return lastBase;
  }

  private final CToFormulaWithUFConverter conv;
  private final PointerTargetSetBuilder pts;
  private final CFAEdge cfaEdge;

  private Variable lastBase = null;
}
