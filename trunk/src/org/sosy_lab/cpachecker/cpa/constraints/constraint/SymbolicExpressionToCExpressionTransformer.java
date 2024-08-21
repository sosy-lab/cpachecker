// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressOfExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryXorExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.CastExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.DivisionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.EqualsExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanOrEqualExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ModuloExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.MultiplicationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.NegationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.PointerExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftLeftExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftRightExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SubtractionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.UnarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.ValueToCExpressionTransformer;

/**
 * Transforms {@link SymbolicExpression}s into {@link CExpression}s.
 *
 * @see org.sosy_lab.cpachecker.cpa.value.symbolic.ExpressionTransformer
 */
public class SymbolicExpressionToCExpressionTransformer
    implements SymbolicValueVisitor<CExpression> {

  private static final FileLocation DUMMY_LOCATION = FileLocation.DUMMY;

  @Override
  public CExpression visit(SymbolicIdentifier pValue) {
    throw new UnsupportedOperationException(
        "SymbolicIdentifier should be handled in handling of ConstantSymbolicExpression");
  }

  @Override
  public CExpression visit(ConstantSymbolicExpression pExpression) {
    Value value = pExpression.getValue();
    CType type = getCType(pExpression.getType());

    return transformValue(value, type);
  }

  private CExpression transformValue(Value pValue, CType pType) {

    if (pValue instanceof SymbolicIdentifier) {
      return getIdentifierCExpression((SymbolicIdentifier) pValue, pType);
    } else if (pValue instanceof SymbolicValue) {
      return ((SymbolicValue) pValue).accept(this);

    } else {
      return pValue.accept(new ValueToCExpressionTransformer(pType));
    }
  }

  /* We can't use function calls in expressions and they are no CExpressions,
   so we can't use builtin functions to represent NaN and infinity.
   Instead, a hack is used below that _should_ produce the correct values, but is
   implementation dependent.

    CType functionType = CFunctionType.functionTypeWithReturnType(pType);

    if (Double.isNaN(pValue) || Double.isInfinite(pValue)) {
      String functionName;

      if (Double.isNaN(pValue)) {
        functionName = getNaNFunctionName(pType);

      } else {
        functionName = getInfFunctionName(pType);
      }

      CIdExpression functionNameExp =
          new CIdExpression(DUMMY_LOCATION, functionType, functionName, null);

      return new CFunctionCallExpression(DUMMY_LOCATION, pType, functionNameExp, Collections.<CExpression>emptyList(),
          null);
  }

  private String getInfFunctionName(CSimpleType pType) {
    if (pType.getType() == CBasicType.FLOAT) {
      return BuiltinFunctions.INFINITY_FLOAT;

    } else {
      assert pType.getType() == CBasicType.DOUBLE;

      if (pType.isLong()) {
        return BuiltinFunctions.INFINITY_LONG_DOUBLE;

      } else {
        return BuiltinFunctions.INFINITY;
      }
    }
  }

  private String getNaNFunctionName(CSimpleType pType) {
    if (pType.getType() == CBasicType.FLOAT) {
      return BuiltinFunctions.NOT_A_NUMBER_FLOAT;

    } else {
      assert pType.getType() == CBasicType.DOUBLE;

      if (pType.isLong()) {
        return BuiltinFunctions.NOT_A_NUMBER_LONG_DOUBLE;

      } else {
        return BuiltinFunctions.NOT_A_NUMBER;
      }
    }
  }*/

  private CType getCType(Type pType) {
    if (pType instanceof CType) {
      return (CType) pType;

    } else {
      assert pType instanceof JType;

      throw new UnsupportedOperationException("Java types not yet supported");
    }
  }

  private CExpression getIdentifierCExpression(SymbolicIdentifier pIdentifier, CType pType) {
    String name = SymbolicIdentifier.Converter.getInstance().convertToStringEncoding(pIdentifier);
    CSimpleDeclaration declaration = getIdentifierDeclaration(name, pType);

    return new CIdExpression(DUMMY_LOCATION, pType, name, declaration);
  }

  private CVariableDeclaration getIdentifierDeclaration(String pIdentifierName, CType pType) {
    return new CVariableDeclaration(
        DUMMY_LOCATION,
        false,
        CStorageClass.AUTO,
        pType,
        pIdentifierName,
        pIdentifierName,
        pIdentifierName,
        null);
  }

  private CExpression createBinaryExpression(
      BinarySymbolicExpression pExpression, CBinaryExpression.BinaryOperator pOperator) {
    return new CBinaryExpression(
        DUMMY_LOCATION,
        getCType(pExpression.getType()),
        getCType(pExpression.getCalculationType()),
        pExpression.getOperand1().accept(this),
        pExpression.getOperand2().accept(this),
        pOperator);
  }

  private CExpression createUnaryExpression(
      UnarySymbolicExpression pExpression, CUnaryExpression.UnaryOperator pOperator) {
    return new CUnaryExpression(
        DUMMY_LOCATION,
        getCType(pExpression.getType()),
        pExpression.getOperand().accept(this),
        pOperator);
  }

  @Override
  public CExpression visit(AdditionExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.PLUS);
  }

  @Override
  public CExpression visit(SubtractionExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.MINUS);
  }

  @Override
  public CExpression visit(MultiplicationExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.MULTIPLY);
  }

  @Override
  public CExpression visit(DivisionExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.DIVIDE);
  }

  @Override
  public CExpression visit(ModuloExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.MODULO);
  }

  @Override
  public CExpression visit(BinaryAndExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.BINARY_AND);
  }

  @Override
  public CExpression visit(BinaryNotExpression pExpression) {
    return createUnaryExpression(pExpression, CUnaryExpression.UnaryOperator.TILDE);
  }

  @Override
  public CExpression visit(BinaryOrExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.BINARY_OR);
  }

  @Override
  public CExpression visit(BinaryXorExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.BINARY_XOR);
  }

  @Override
  public CExpression visit(ShiftRightExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.SHIFT_RIGHT);
  }

  @Override
  public CExpression visit(ShiftLeftExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.SHIFT_LEFT);
  }

  @Override
  public CExpression visit(LogicalNotExpression pExpression) {
    SymbolicExpression operand = pExpression.getOperand();

    if (operand instanceof LogicalNotExpression) {
      return ((LogicalNotExpression) operand).getOperand().accept(this);

    } else {
      assert operand instanceof BinarySymbolicExpression;
      BinarySymbolicExpression innerExpression = (BinarySymbolicExpression) operand;

      if (operand instanceof EqualsExpression) {
        return createBinaryExpression(innerExpression, CBinaryExpression.BinaryOperator.NOT_EQUALS);

      } else if (operand instanceof LessThanExpression) {
        return createBinaryExpression(
            innerExpression, CBinaryExpression.BinaryOperator.GREATER_EQUAL);

      } else if (operand instanceof LessThanOrEqualExpression) {
        return createBinaryExpression(
            innerExpression, CBinaryExpression.BinaryOperator.GREATER_THAN);

      } else {
        throw new AssertionError("Unhandled operation " + operand);
      }
    }
  }

  @Override
  public CExpression visit(LessThanOrEqualExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.LESS_EQUAL);
  }

  @Override
  public CExpression visit(LessThanExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.LESS_THAN);
  }

  @Override
  public CExpression visit(EqualsExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.EQUALS);
  }

  @Override
  public CExpression visit(LogicalOrExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.BINARY_OR);
  }

  @Override
  public CExpression visit(LogicalAndExpression pExpression) {
    return createBinaryExpression(pExpression, CBinaryExpression.BinaryOperator.BINARY_AND);
  }

  @Override
  public CExpression visit(CastExpression pExpression) {
    CType cType = getCType(pExpression.getType());
    CExpression operandExpression = pExpression.getOperand().accept(this);

    return new CCastExpression(DUMMY_LOCATION, cType, operandExpression);
  }

  @Override
  public CExpression visit(PointerExpression pExpression) {
    CType cType = getCType(pExpression.getType());
    CExpression operandExpression = pExpression.getOperand().accept(this);

    return new CPointerExpression(DUMMY_LOCATION, cType, operandExpression);
  }

  @Override
  public CExpression visit(AddressOfExpression pExpression) {
    return createUnaryExpression(pExpression, CUnaryExpression.UnaryOperator.AMPER);
  }

  @Override
  public CExpression visit(NegationExpression pExpression) {
    return createUnaryExpression(pExpression, CUnaryExpression.UnaryOperator.MINUS);
  }
}
