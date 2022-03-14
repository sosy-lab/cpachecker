// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import java.util.OptionalLong;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetPattern.PointerTargetPatternBuilder;

class LvalueToPointerTargetPatternVisitor
    extends DefaultCExpressionVisitor<PointerTargetPatternBuilder, UnrecognizedCodeException> {

  private final TypeHandlerWithPointerAliasing typeHandler;
  private final PointerTargetSetBuilder pts;
  private final CFAEdge cfaEdge;

  LvalueToPointerTargetPatternVisitor(
      final TypeHandlerWithPointerAliasing pTypeHandler,
      final CFAEdge pCfaEdge,
      final PointerTargetSetBuilder pPts) {
    typeHandler = pTypeHandler;
    cfaEdge = pCfaEdge;
    pts = pPts;
  }

  private class PointerTargetEvaluatingVisitor
      extends DefaultCExpressionVisitor<PointerTargetPatternBuilder, UnrecognizedCodeException> {

    @Override
    protected PointerTargetPatternBuilder visitDefault(final CExpression e)
        throws UnrecognizedCodeException {
      return null;
    }

    @Override
    public PointerTargetPatternBuilder visit(final CBinaryExpression e)
        throws UnrecognizedCodeException {
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

        case MINUS:
          {
            final PointerTargetPatternBuilder result = operand1.accept(this);
            if (result != null) {
              final Integer offset = tryEvaluateExpression(operand2);
              final Long oldOffset = result.getProperOffset();
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

        case PLUS:
          {
            PointerTargetPatternBuilder result = operand1.accept(this);
            final Integer offset;
            if (result == null) {
              result = operand2.accept(this);
              offset = tryEvaluateExpression(operand1);
            } else {
              offset = tryEvaluateExpression(operand2);
            }
            if (result != null) {
              final Long remaining = result.getRemainingOffset(typeHandler);
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
          throw new UnrecognizedCodeException("Unhandled binary operator", cfaEdge, e);
      }
    }

    @Override
    public PointerTargetPatternBuilder visit(final CCastExpression e)
        throws UnrecognizedCodeException {
      return e.getOperand().accept(this);
    }

    @Override
    public PointerTargetPatternBuilder visit(final CIdExpression e)
        throws UnrecognizedCodeException {
      final CType expressionType = typeHandler.getSimplifiedType(e);
      final String name = e.getDeclaration().getQualifiedName();
      if (!pts.isBase(name, expressionType)
          && !CTypeUtils.containsArray(expressionType, e.getDeclaration())) {
        return null;
      } else {
        return PointerTargetPatternBuilder.forBase(name);
      }
    }

    @Override
    public PointerTargetPatternBuilder visit(final CUnaryExpression e)
        throws UnrecognizedCodeException {
      final CExpression operand = e.getOperand();
      switch (e.getOperator()) {
        case AMPER:
          return operand.accept(LvalueToPointerTargetPatternVisitor.this);
        case MINUS:
        case TILDE:
          return null;
        case SIZEOF:
          throw new UnrecognizedCodeException("Illegal unary operator", cfaEdge, e);
        default:
          throw new UnrecognizedCodeException("Unrecognized unary operator", cfaEdge, e);
      }
    }

    @Override
    public PointerTargetPatternBuilder visit(final CPointerExpression e)
        throws UnrecognizedCodeException {
      return null;
    }
  }

  @Override
  protected PointerTargetPatternBuilder visitDefault(final CExpression e)
      throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("Illegal expression in lhs", cfaEdge, e);
  }

  @Override
  public PointerTargetPatternBuilder visit(final CArraySubscriptExpression e)
      throws UnrecognizedCodeException {
    final CExpression arrayExpression = e.getArrayExpression();
    PointerTargetPatternBuilder result =
        arrayExpression.accept(new PointerTargetEvaluatingVisitor());
    if (result == null) {
      result = PointerTargetPatternBuilder.any();
    }
    CType containerType = typeHandler.getSimplifiedType(arrayExpression);
    if (containerType instanceof CArrayType || containerType instanceof CPointerType) {
      final CType elementType;
      if (containerType instanceof CPointerType) {
        elementType = ((CPointerType) containerType).getType();
        containerType =
            new CArrayType(
                containerType.isConst(), // TODO: Set array size
                containerType.isVolatile(),
                elementType,
                null);
      } else {
        elementType = ((CArrayType) containerType).getType();
      }
      result.shift(containerType);
      final Integer index = tryEvaluateExpression(e.getSubscriptExpression());
      if (index != null) {
        result.setProperOffset(index * typeHandler.getSizeof(elementType));
      }
      return result;
    } else {
      throw new UnrecognizedCodeException("Array expression has incompatible type", cfaEdge, e);
    }
  }

  @Override
  public PointerTargetPatternBuilder visit(final CCastExpression e)
      throws UnrecognizedCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public PointerTargetPatternBuilder visit(CFieldReference e) throws UnrecognizedCodeException {
    e = e.withExplicitPointerDereference();

    final CExpression ownerExpression = e.getFieldOwner();
    final PointerTargetPatternBuilder result = ownerExpression.accept(this);
    if (result != null) {
      final CType containerType = typeHandler.getSimplifiedType(ownerExpression);
      if (containerType instanceof CCompositeType) {
        assert ((CCompositeType) containerType).getKind() != ComplexTypeKind.ENUM
            : "Enums are not composites!";

        final OptionalLong offset =
            typeHandler.getOffset((CCompositeType) containerType, e.getFieldName());
        if (!offset.isPresent()) {
          return null; // TODO this looses values of bit fields
        }
        result.shift(containerType, offset.orElseThrow());
        return result;
      } else {
        throw new UnrecognizedCodeException(
            "Field owner expression has incompatible type", cfaEdge, e);
      }
    } else {
      return null;
    }
  }

  @Override
  public PointerTargetPatternBuilder visit(final CIdExpression e) throws UnrecognizedCodeException {
    final CType expressionType = typeHandler.getSimplifiedType(e);
    final String name = e.getDeclaration().getQualifiedName();
    if (!pts.isActualBase(name) && !CTypeUtils.containsArray(expressionType, e.getDeclaration())) {
      return null;
    } else {
      return PointerTargetPatternBuilder.forBase(name);
    }
  }

  @Override
  public PointerTargetPatternBuilder visit(final CUnaryExpression e)
      throws UnrecognizedCodeException {
    switch (e.getOperator()) {
      case AMPER:
      case MINUS:
      case SIZEOF:
      case TILDE:
        throw new UnrecognizedCodeException("Illegal unary operator", cfaEdge, e);
      default:
        throw new UnrecognizedCodeException("Unhandled unary operator", cfaEdge, e);
    }
  }

  @Override
  public PointerTargetPatternBuilder visit(final CPointerExpression e)
      throws UnrecognizedCodeException {
    final CExpression operand = e.getOperand();
    final CType type = typeHandler.getSimplifiedType(operand);
    final PointerTargetPatternBuilder result =
        e.getOperand().accept(new PointerTargetEvaluatingVisitor());
    if (type instanceof CPointerType) {
      if (result != null) {
        result.clear();
        return result;
      } else {
        return PointerTargetPatternBuilder.any();
      }
    } else if (type instanceof CArrayType) {
      if (result != null) {
        result.shift(type, 0);
        return result;
      } else {
        return PointerTargetPatternBuilder.any();
      }
    } else {
      throw new UnrecognizedCodeException("Dereferencing non-pointer expression", cfaEdge, e);
    }
  }

  private static @Nullable Integer tryEvaluateExpression(CExpression e) {
    if (e instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression) e).getValue().intValue();
    }
    return null;
  }
}
