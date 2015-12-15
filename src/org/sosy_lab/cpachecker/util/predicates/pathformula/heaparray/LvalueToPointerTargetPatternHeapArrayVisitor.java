/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.heaparray;

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
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetPattern;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetManager;

/**
 * A visitor for Lvalue expressions to create a {@link PointerTargetPattern} for the heap array
 * converter.
 *
 * @see PointerTargetPattern
 * @see PointerTargetPatternHeapArray
 */
public class LvalueToPointerTargetPatternHeapArrayVisitor
    extends DefaultCExpressionVisitor<PointerTargetPattern, UnrecognizedCCodeException> {

  private final CtoFormulaTypeHandler typeHandler;
  private final PointerTargetSetManager ptsMgr;
  private final PointerTargetSetBuilder pts;
  private final CFAEdge cfaEdge;

  /**
   * Creates a new visitor for Lvalue expressions.
   *
   * @param pTypeHandler A handler for types.
   * @param pPointerTargetSetManager The manager of pointer target sets.
   * @param pCFAEdge The current edge of the CFA.
   * @param pPointerTargetSetBuilder The builder for pointer target sets.
   */
  LvalueToPointerTargetPatternHeapArrayVisitor(
      final CtoFormulaTypeHandler pTypeHandler,
      final PointerTargetSetManager pPointerTargetSetManager,
      final CFAEdge pCFAEdge,
      final PointerTargetSetBuilder pPointerTargetSetBuilder) {
    typeHandler = pTypeHandler;
    ptsMgr = pPointerTargetSetManager;
    pts = pPointerTargetSetBuilder;
    cfaEdge = pCFAEdge;
  }

  /**
   * The default visiting method for arbitrary C expressions.
   *
   * <p>Calling this method will throw an {@link UnrecognizedCCodeException} as we do not handle
   * any arbitrary expressions here but only specific ones.</p>
   *
   * @param pExpression The expression.
   * @return Nothing will be returned, instead an {@code UnrecognizedCCodeException} is thrown.
   * @throws UnrecognizedCCodeException Every time this method gets called.
   */
  @Override
  protected PointerTargetPattern visitDefault(CExpression pExpression)
      throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Illegal expression in lhs", cfaEdge, pExpression);
  }

  /**
   * Creates a {@code PointerTargetPattern} for an array subscript expression.
   *
   * @param pExpression The expression to visit.
   * @return A corresponding {@code PointerTargetPattern}.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public PointerTargetPattern visit(final CArraySubscriptExpression pExpression)
      throws UnrecognizedCCodeException {
    final CExpression arrayExpression = pExpression.getArrayExpression();
    PointerTargetPattern result = arrayExpression.accept(new PointerTargetEvaluatingVisitor());
    if (result == null) {
      result = PointerTargetPatternHeapArray.any();
    }

    CType containerType = CTypeUtils.simplifyType(arrayExpression.getExpressionType());
    if (containerType instanceof CArrayType || containerType instanceof CPointerType) {
      final CType elementType;
      if (containerType instanceof CPointerType) {
        elementType = ((CPointerType) containerType).getType();
        containerType =
            new CArrayType(containerType.isConst(), containerType.isVolatile(), elementType, null);
      } else {
        elementType = ((CArrayType) containerType).getType();
      }

      result.shift(containerType);

      final Integer index = tryEvaluateExpression(pExpression.getSubscriptExpression());
      if (index != null) {
        result.setProperOffset(index * typeHandler.getSizeof(elementType));
      }

      return result;
    } else {
      throw new UnrecognizedCCodeException(
          "Array expression has incompatible type", cfaEdge, pExpression);
    }
  }

  /**
   * Creates a {@code PointerTargetPattern} for a case expression.
   *
   * @param pExpression The expression to visit.
   * @return A corresponding {@code PointerTargetPattern}.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public PointerTargetPattern visit(final CCastExpression pExpression)
      throws UnrecognizedCCodeException {
    return pExpression.getOperand().accept(this);
  }

  /**
   * Creates a {@code PointerTargetPattern} for a field reference expression.
   *
   * @param pExpression The expression to visit.
   * @return A corresponding {@code PointerTargetPattern}.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public PointerTargetPattern visit(CFieldReference pExpression) throws UnrecognizedCCodeException {
    pExpression = CToFormulaConverterWithHeapArray.eliminateArrow(pExpression, cfaEdge);

    final CExpression ownerExpression = pExpression.getFieldOwner();
    final PointerTargetPattern result = ownerExpression.accept(this);
    if (result != null) {
      final CType containerType = CTypeUtils.simplifyType(ownerExpression.getExpressionType());
      if (containerType instanceof CCompositeType) {
        assert ((CCompositeType) containerType).getKind() != ComplexTypeKind.ENUM
            : "Enums are " + "not composites!";
        result.shift(
            containerType,
            ptsMgr.getOffset((CCompositeType) containerType, pExpression.getFieldName()));
        return result;
      } else {
        throw new UnrecognizedCCodeException(
            "Field owner expression has incompatible type", cfaEdge, pExpression);
      }
    } else {
      return null;
    }
  }

  /**
   * Creates a {@code PointerTargetPattern} for an ID expression.
   *
   * @param pExpression The expression to visit.
   * @return A corresponding {@code PointerTargetPattern}.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public PointerTargetPattern visit(final CIdExpression pExpression)
      throws UnrecognizedCCodeException {
    final CType expressionType = CTypeUtils.simplifyType(pExpression.getExpressionType());
    final String name = pExpression.getDeclaration().getQualifiedName();
    if (!pts.isActualBase(name) && !CTypeUtils.containsArray(expressionType)) {
      return null;
    } else {
      return PointerTargetPatternHeapArray.forBase(name);
    }
  }

  /**
   * Creates a {@code PointerTargetPattern} for an unary expression.
   *
   * @param pExpression The expression to visit.
   * @return A corresponding {@code PointerTargetPattern}.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public PointerTargetPattern visit(final CUnaryExpression pExpression)
      throws UnrecognizedCCodeException {
    switch (pExpression.getOperator()) {
      case AMPER:
      case MINUS:
      case SIZEOF:
      case TILDE:
        throw new UnrecognizedCCodeException("Illegal unary operator", cfaEdge, pExpression);
      default:
        throw new UnrecognizedCCodeException("Unhandled unary operator", cfaEdge, pExpression);
    }
  }

  /**
   * Creates a {@code PointerTargetPattern} for a pointer expression.
   *
   * @param pExpression The expression to visit.
   * @return A corresponding {@code PointerTargetPattern}.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public PointerTargetPattern visit(final CPointerExpression pExpression)
      throws UnrecognizedCCodeException {
    final CExpression operand = pExpression.getOperand();
    final CType type = CTypeUtils.simplifyType(operand.getExpressionType());
    final PointerTargetPattern result =
        pExpression.getOperand().accept(new PointerTargetEvaluatingVisitor());

    if (type instanceof CPointerType) {
      if (result == null) {
        return PointerTargetPatternHeapArray.any();
      } else {
        result.clear();
        return result;
      }
    } else if (type instanceof CArrayType) {
      if (result == null) {
        return PointerTargetPatternHeapArray.any();
      } else {
        result.shift(type, 0);
        return result;
      }
    } else {
      throw new UnrecognizedCCodeException(
          "De-referencing non-pointer expression", cfaEdge, pExpression);
    }
  }

  /**
   * Tries to evaluate the value of an expression, i.e., if we get an
   * {@link CIntegerLiteralExpression} here, we return its value. If any other expression type is
   * passed to this method, {@code null} will be returned.
   *
   * @param pExpression The expression to evaluate the value of.
   * @return {@code null} if the expression is not an {@code CIntegerLiteralExpression},
   *         otherwise its value.
   */
  private static Integer tryEvaluateExpression(final CExpression pExpression) {
    if (pExpression instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression) pExpression).getValue().intValue();
    }
    return null;
  }

  /**
   * A helper visitor to evaluate a pointer's target.
   */
  private class PointerTargetEvaluatingVisitor
      extends DefaultCExpressionVisitor<PointerTargetPattern, UnrecognizedCCodeException> {

    /**
     * The default method for arbitrary C expressions.
     *
     * <p>Every time this method is called, {@code null} will be returned.</p>
     *
     * @param pExpression The expression to visit.
     * @return {@code null}
     * @throws UnrecognizedCCodeException Never, as {@code null} will be returned every time.
     */
    @Override
    protected PointerTargetPattern visitDefault(final CExpression pExpression)
        throws UnrecognizedCCodeException {
      return null;
    }

    /**
     * Creates a {@code PointerTargetPattern} for a binary expression.
     *
     * @param pExpression The expression to visit.
     * @return A corresponding {@code PointerTargetPattern}.
     * @throws UnrecognizedCCodeException If the C code was unrecognizable.
     */
    @Override
    public PointerTargetPattern visit(final CBinaryExpression pExpression)
        throws UnrecognizedCCodeException {
      final CExpression operand1 = pExpression.getOperand1();
      final CExpression operand2 = pExpression.getOperand2();

      switch (pExpression.getOperator()) {
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

        case PLUS:
          {
            PointerTargetPattern result = operand1.accept(this);
            final Integer offset;

            if (result == null) {
              result = operand2.accept(this);
              offset = tryEvaluateExpression(operand1);
            } else {
              offset = tryEvaluateExpression(operand2);
            }

            if (result == null) {
              return null;
            } else {
              final Integer remaining = result.getRemainingOffset(ptsMgr);
              if (offset != null && remaining != null && offset < remaining) {
                assert result.getProperOffset() != null
                    : "Unexpected non-deterministic proper offset";
                result.setProperOffset(result.getProperOffset() + offset);
              } else {
                result.retainBase();
              }
              return result;
            }
          }

        case MINUS:
          {
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

        default:
          throw new UnrecognizedCCodeException("Unhandled binary operator", cfaEdge, pExpression);
      }
    }

    /**
     * Creates a {@code PointerTargetPattern} for a cast expression.
     *
     * @param pExpression The expression to visit.
     * @return A corresponding {@code PointerTargetPattern}.
     * @throws UnrecognizedCCodeException If the C code was unrecognizable.
     */
    @Override
    public PointerTargetPattern visit(final CCastExpression pExpression)
        throws UnrecognizedCCodeException {
      return pExpression.getOperand().accept(this);
    }

    /**
     * Creates a {@code PointerTargetPattern} for an ID expression.
     *
     * @param pExpression The expression to visit.
     * @return A corresponding {@code PointerTargetPattern}.
     * @throws UnrecognizedCCodeException If the C code was unrecognizable.
     */
    @Override
    public PointerTargetPattern visit(final CIdExpression pExpression)
        throws UnrecognizedCCodeException {
      final CType expressionType = CTypeUtils.simplifyType(pExpression.getExpressionType());
      final String name = pExpression.getDeclaration().getQualifiedName();

      if (!pts.isBase(name, expressionType) && !CTypeUtils.containsArray(expressionType)) {
        return null;
      } else {
        return PointerTargetPatternHeapArray.forBase(name);
      }
    }

    /**
     * Creates a {@code PointerTargetPattern} for an unary expression.
     *
     * @param pExpression The expression to visit.
     * @return A corresponding {@code PointerTargetPattern}.
     * @throws UnrecognizedCCodeException If the C code was unrecognizable.
     */
    @Override
    public PointerTargetPattern visit(final CUnaryExpression pExpression)
        throws UnrecognizedCCodeException {
      final CExpression operand = pExpression.getOperand();
      switch (pExpression.getOperator()) {
        case AMPER:
          return operand.accept(LvalueToPointerTargetPatternHeapArrayVisitor.this);
        case MINUS:
        case TILDE:
          return null;
        case SIZEOF:
          throw new UnrecognizedCCodeException("Illegal unary operator", cfaEdge, pExpression);
        default:
          throw new UnrecognizedCCodeException("Unrecognized unary operator", cfaEdge, pExpression);
      }
    }

    /**
     * Creates a {@code PointerTargetPattern} for a pointer expression.
     *
     * @param pExpression The expression to visit.
     * @return A corresponding {@code PointerTargetPattern}.
     * @throws UnrecognizedCCodeException If the C code was unrecognizable.
     */
    @Override
    public PointerTargetPattern visit(final CPointerExpression pExpression)
        throws UnrecognizedCCodeException {
      return null;
    }
  }
}
