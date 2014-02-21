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
package org.sosy_lab.cpachecker.util.predicates.pathformula.withUF;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTargetPattern;


class LvalueToPointerTargetPatternVisitor
extends DefaultCExpressionVisitor<PointerTargetPattern, UnrecognizedCCodeException> {

  public LvalueToPointerTargetPatternVisitor(final CToFormulaWithUFConverter conv,
                                             final CFAEdge cfaEdge,
                                             final PointerTargetSetBuilder pts) {
    this.pointerVisitor = new PointerTargetEvaluatingVisitor();
    this.conv = conv;
    this.cfaEdge = cfaEdge;
    this.pts = pts;
  }

  private class PointerTargetEvaluatingVisitor
    extends DefaultCExpressionVisitor<PointerTargetPattern, UnrecognizedCCodeException> {

    @Override
    protected PointerTargetPattern visitDefault(final CExpression e) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public PointerTargetPattern visit(final CBinaryExpression e) throws UnrecognizedCCodeException {
      final CExpression operand1 = e.getOperand1();
      final CExpression operand2 = e.getOperand2();

      switch (e.getOperator()) {
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
      case DIVIDE:
      case EQUALS:
      case GREATER_EQUAL:
      case GREATER_THAN:
      case LESS_EQUAL:
      case LESS_THAN:
      case MODULO:
      case MULTIPLY:
      case NOT_EQUALS:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
        return null;

      case MINUS: {
        final PointerTargetPattern result = operand1.accept(this);
        if (result != null) {
          final Integer offset = tryEvaluateExpression(operand2);
          final Integer oldOffset = result.getProperOffset();
          if (offset != null && oldOffset != null && offset < oldOffset) {
            result.setProperOffset(oldOffset - offset);
          } else {
            result.retainBase();
          }
          return result;
        } else {
          return null;
        }
      }

      case PLUS: {
        PointerTargetPattern result = operand1.accept(this);
        final Integer offset;
        if (result == null) {
          result = operand2.accept(pointerVisitor);
          offset = tryEvaluateExpression(operand1);
        } else {
          offset = tryEvaluateExpression(operand2);
        }
        if (result != null) {
          final Integer remaining = result.getRemainingOffset(conv.ptsMgr);
          if (offset != null && remaining != null && offset < remaining) {
            assert result.getProperOffset() != null : "Unexpected nondet proper offset";
            result.setProperOffset(result.getProperOffset() + offset);
          } else {
            result.retainBase();
          }
          return result;
        } else {
          return null;
        }
      }

      default:
        throw new UnrecognizedCCodeException("Unhandled binary operator", cfaEdge, e);
      }
    }

    @Override
    public PointerTargetPattern visit(final CCastExpression e) throws UnrecognizedCCodeException {
      return e.getOperand().accept(this);
    }

    @Override
    public PointerTargetPattern visit(final CIdExpression e) throws UnrecognizedCCodeException {
      final Variable variable = conv.scopedIfNecessary(e);
      final CType expressionType = CTypeUtils.simplifyType(e.getExpressionType());
      if (!pts.isBase(variable.getName(), expressionType) && !CTypeUtils.containsArray(expressionType)) {
        return null;
      } else {
        return new PointerTargetPattern(variable.getName(), 0, 0);
      }
    }

    @Override
    public PointerTargetPattern visit(final CUnaryExpression e) throws UnrecognizedCCodeException {
      final CExpression operand = e.getOperand();
      switch (e.getOperator()) {
      case AMPER:
        return operand.accept(LvalueToPointerTargetPatternVisitor.this);
      case MINUS:
      case PLUS:
      case NOT:
      case TILDE:
        return null;
      case SIZEOF:
        throw new UnrecognizedCCodeException("Illegal unary operator", cfaEdge, e);
      default:
        throw new UnrecognizedCCodeException("Unrecognized unary operator", cfaEdge, e);
      }
    }

    @Override
    public PointerTargetPattern visit(final CPointerExpression e) throws UnrecognizedCCodeException {
      return null;
    }
  }

  @Override
  protected PointerTargetPattern visitDefault(final CExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Illegal expression in lhs", cfaEdge, e);
  }

  @Override
  public PointerTargetPattern visit(final CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    final CExpression arrayExpression = e.getArrayExpression();
    PointerTargetPattern result = arrayExpression.accept(pointerVisitor);
    if (result == null) {
      result = new PointerTargetPattern();
    }
    CType containerType = CTypeUtils.simplifyType(arrayExpression.getExpressionType());
    if (containerType instanceof CArrayType || containerType instanceof CPointerType) {
      final CType elementType;
      if (containerType instanceof CPointerType) {
        elementType = ((CPointerType) containerType).getType();
        containerType = new CArrayType(containerType.isConst(), // TODO: Set array size
                                       containerType.isVolatile(),
                                       elementType,
                                       null);
      } else {
        elementType = ((CArrayType) containerType).getType();
      }
      result.shift(containerType);
      final Integer index = tryEvaluateExpression(e.getSubscriptExpression());
      if (index != null) {
        result.setProperOffset(index * conv.getSizeof(elementType));
      }
      return result;
    } else {
      throw new UnrecognizedCCodeException("Array expression has incompatible type", cfaEdge, e);
    }
  }

  @Override
  public PointerTargetPattern visit(final CCastExpression e) throws UnrecognizedCCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public PointerTargetPattern visit(CFieldReference e) throws UnrecognizedCCodeException {

    e = ExpressionToFormulaWithUFVisitor.eliminateArrow(e, cfaEdge);

    final CExpression ownerExpression = e.getFieldOwner();
    final PointerTargetPattern result = ownerExpression.accept(this);
    if (result != null) {
      final CType containerType = CTypeUtils.simplifyType(ownerExpression.getExpressionType());
      if (containerType instanceof CCompositeType) {
        assert  ((CCompositeType) containerType).getKind() != ComplexTypeKind.ENUM : "Enums are not composites!";
        result.shift(containerType, conv.ptsMgr.getOffset((CCompositeType) containerType, e.getFieldName()));
        return result;
      } else {
        throw new UnrecognizedCCodeException("Field owner expression has incompatible type", cfaEdge, e);
      }
    } else {
      return null;
    }
  }

  @Override
  public PointerTargetPattern visit(final CIdExpression e) throws UnrecognizedCCodeException {
    final Variable variable = conv.scopedIfNecessary(e);
    if (!pts.isActualBase(variable.getName()) &&
        !CTypeUtils.containsArray(CTypeUtils.simplifyType(e.getExpressionType()))) {
      return null;
    } else {
      return new PointerTargetPattern(variable.getName(), 0, 0);
    }
  }

  @Override
  public PointerTargetPattern visit(final CUnaryExpression e) throws UnrecognizedCCodeException {
    final CExpression operand = e.getOperand();
    switch (e.getOperator()) {
    case AMPER:
    case MINUS:
    case NOT:
    case SIZEOF:
    case TILDE:
      throw new UnrecognizedCCodeException("Illegal unary operator", cfaEdge, e);
    case PLUS:
      return operand.accept(this);
    default:
      throw new UnrecognizedCCodeException("Unhandled unary operator", cfaEdge, e);
    }
  }

  @Override
  public PointerTargetPattern visit(final CPointerExpression e) throws UnrecognizedCCodeException {
    final CExpression operand = e.getOperand();
    final CType type = CTypeUtils.simplifyType(operand.getExpressionType());
    final PointerTargetPattern result = e.getOperand().accept(pointerVisitor);
    if (type instanceof CPointerType) {
      if (result != null) {
        result.clear();
        return result;
      } else {
        return new PointerTargetPattern();
      }
    } else if (type instanceof CArrayType) {
      if (result != null) {
        result.shift(type, 0);
        return result;
      } else {
        return new PointerTargetPattern();
      }
    } else {
      throw new UnrecognizedCCodeException("Dereferencing non-pointer expression", cfaEdge, e);
    }
  }

  private Integer tryEvaluateExpression(CExpression e) {
    if (e instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression)e).getValue().intValue();
    }
    return null;
  }

  private final PointerTargetEvaluatingVisitor pointerVisitor;

  private final CToFormulaWithUFConverter conv;
  private final PointerTargetSetBuilder pts;
  private final CFAEdge cfaEdge;
}
