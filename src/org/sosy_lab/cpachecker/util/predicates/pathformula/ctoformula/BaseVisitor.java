/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;


class BaseVisitor implements CExpressionVisitor<String, UnrecognizedCCodeException>{

  public BaseVisitor(final CFAEdge cfaEdge, final PointerTargetSetBuilder pts) {
    this.cfaEdge = cfaEdge;
    this.pts = pts;
  }

  @Override
  public String visit(final CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    assert e.getArrayExpression().accept(this) == null : "Array access can't be encoded as a varaible";
    return null;
  }

  @Override
  public String visit(final CBinaryExpression e) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public String visit(final CCastExpression e) throws UnrecognizedCCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public String visit(final CFieldReference e) throws UnrecognizedCCodeException {
    final String base = e.getFieldOwner().accept(this);
    if (base != null) {
      return base + NAME_SEPARATOR + e.getFieldName();
    } else {
      return null;
    }
  }

  @Override
  public String visit(final CIdExpression e) throws UnrecognizedCCodeException {
    final String name = e.getName();
    if (!pts.isBase(name)) {
      return name;
    } else {
      return null;
    }
  }

  @Override
  public String visit(final CCharLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Char literal in place of lvalue", cfaEdge, e);
  }

  @Override
  public String visit(final CFloatLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Float literal in place of lvalue", cfaEdge, e);
  }

  @Override
  public String visit(final CIntegerLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Integer literal in place of lvalue", cfaEdge, e);
  }

  @Override
  public String visit(final CStringLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("String literal in place of lvalue", cfaEdge, e);
  }

  @Override
  public String visit(final CTypeIdExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("TypeId in place of lvalue", cfaEdge, e);
  }

  @Override
  public String visit(final CTypeIdInitializerExpression e) throws UnrecognizedCCodeException {
    // TODO: Type id initializers should be supported
    throw new UnrecognizedCCodeException("Typeid initializers are currently unsupported", cfaEdge);
  }

  @Override
  public String visit(final CUnaryExpression e) throws UnrecognizedCCodeException {
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
    case STAR:
      return null;
    default:
      throw new UnrecognizedCCodeException("Unrecognized code in place of lvalue", cfaEdge, e);
    }
  }

  private final PointerTargetSetBuilder pts;
  private final CFAEdge cfaEdge;
  static final String NAME_SEPARATOR = "$";
}
