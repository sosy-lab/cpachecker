// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressOfExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.CastExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.NegationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.PointerExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Class for transforming {@link CExpression} objects into their {@link SymbolicExpression}
 * representation.
 *
 * <p>Always use {@link #transform} to create correct representations. Otherwise, correctness can't
 * be assured.
 */
public class CExpressionTransformer extends ExpressionTransformer
    implements CRightHandSideVisitor<SymbolicExpression, UnrecognizedCodeException> {

  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;

  public CExpressionTransformer(
      final String pFunctionName,
      final ValueAnalysisState pValueState,
      final MachineModel pMachineModel,
      final LogManagerWithoutDuplicates pLogger) {
    super(pFunctionName, pValueState);

    machineModel = pMachineModel;
    logger = pLogger;
  }

  public SymbolicExpression transform(final CExpression pExpression)
      throws UnrecognizedCodeException {
    return pExpression.accept(this);
  }

  @Override
  public SymbolicExpression visit(final CBinaryExpression pIastBinaryExpression)
      throws UnrecognizedCodeException {
    SymbolicExpression operand1Expression = pIastBinaryExpression.getOperand1().accept(this);

    if (operand1Expression == null) {
      return null;
    }

    SymbolicExpression operand2Expression = pIastBinaryExpression.getOperand2().accept(this);

    if (operand2Expression == null) {
      return null;
    }

    final Type expressionType = pIastBinaryExpression.getExpressionType();
    final Type calculationType = pIastBinaryExpression.getCalculationType();

    return BinarySymbolicExpression.of(
        operand1Expression,
        operand2Expression,
        expressionType,
        calculationType,
        pIastBinaryExpression.getOperator());
  }

  @Override
  public SymbolicExpression visit(final CUnaryExpression pIastUnaryExpression)
      throws UnrecognizedCodeException {
    final CUnaryExpression.UnaryOperator operator = pIastUnaryExpression.getOperator();
    final Type expressionType = pIastUnaryExpression.getExpressionType();

    switch (operator) {
      case MINUS, TILDE -> {
        SymbolicExpression operand = pIastUnaryExpression.getOperand().accept(this);

        if (operand == null) {
          return null;
        } else {
          return transformUnaryArithmetic(operator, operand, expressionType);
        }
      }
      default -> {
        return null;
        // TODO: amper, alignof, sizeof with own expressions
      }
    }
  }

  private SymbolicExpression transformUnaryArithmetic(
      final CUnaryExpression.UnaryOperator pOperator,
      final SymbolicExpression pOperand,
      final Type pExpressionType) {
    return switch (pOperator) {
      case MINUS -> NegationExpression.of(pOperand, pExpressionType);
      case TILDE -> BinaryNotExpression.of(pOperand, pExpressionType);
      case AMPER -> AddressOfExpression.of(pOperand, pExpressionType);
      default -> throw new AssertionError("No arithmetic operator: " + pOperator);
    };
  }

  @Override
  public SymbolicExpression visit(final CIdExpression pIastIdExpression)
      throws UnrecognizedCodeException {
    return super.visit(pIastIdExpression);
  }

  @Override
  public SymbolicExpression visit(final CCharLiteralExpression pIastCharLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pIastCharLiteralExpression);
  }

  @Override
  public SymbolicExpression visit(final CFloatLiteralExpression pIastFloatLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pIastFloatLiteralExpression);
  }

  @Override
  public SymbolicExpression visit(final CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pIastIntegerLiteralExpression);
  }

  @Override
  public SymbolicExpression visit(final CStringLiteralExpression pIastStringLiteralExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public SymbolicExpression visit(final CTypeIdExpression pIastTypeIdExpression)
      throws UnrecognizedCodeException {
    throw new AssertionError("Type id expression invalid for constraint");
  }

  @Override
  public SymbolicExpression visit(final CImaginaryLiteralExpression pIastLiteralExpression)
      throws UnrecognizedCodeException {
    throw new AssertionError("Imaginary literal invalid for constraint");
  }

  @Override
  public SymbolicExpression visit(final CAddressOfLabelExpression pAddressOfLabelExpression)
      throws UnrecognizedCodeException {
    throw new AssertionError("Address of label expression used in symbolic expression");
  }

  @Override
  public SymbolicExpression visit(final CArraySubscriptExpression pIastArraySubscriptExpression)
      throws UnrecognizedCodeException {
    return evaluateToValue(pIastArraySubscriptExpression);
  }

  @Override
  public SymbolicExpression visit(final CFieldReference pIastFieldReference)
      throws UnrecognizedCodeException {
    return evaluateToValue(pIastFieldReference);
  }

  private SymbolicExpression evaluateToValue(final CExpression pExpression)
      throws UnrecognizedCodeException {

    ExpressionValueVisitor valueVisitor = getValueVisitor(valueState);
    MemoryLocation memLoc = valueVisitor.evaluateMemoryLocation(pExpression);

    if (memLoc == null) {
      return null;

    } else if (valueState.contains(memLoc)) {
      Value value = valueState.getValueFor(memLoc);

      return ConstantSymbolicExpression.of(value, pExpression.getExpressionType());
    } else {
      return null;
    }
  }

  @Override
  public SymbolicExpression visit(final CPointerExpression pPointerExpression)
      throws UnrecognizedCodeException {
    SymbolicExpression operand = pPointerExpression.getOperand().accept(this);

    if (operand == null) {
      return null;

    } else {
      return PointerExpression.of(operand, pPointerExpression.getExpressionType());
    }
  }

  private ExpressionValueVisitor getValueVisitor(final ValueAnalysisState pState) {
    return new ExpressionValueVisitor(pState, functionName, machineModel, logger);
  }

  @Override
  public SymbolicExpression visit(final CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException(
        "Function calls can't be transformed to ConstraintExpressions");
  }

  @Override
  public SymbolicExpression visit(final CCastExpression pIastCastExpression)
      throws UnrecognizedCodeException {
    SymbolicExpression operand = pIastCastExpression.getOperand().accept(this);

    if (operand == null) {
      return null;

    } else {
      return CastExpression.of(operand, pIastCastExpression.getCastType());
    }
  }

  @Override
  public SymbolicExpression visit(final CComplexCastExpression complexCastExpression)
      throws UnrecognizedCodeException {
    throw new AssertionError("Complex cast not valid for constraint");
  }
}
