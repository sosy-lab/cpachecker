// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.constraint;

import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAValueVisitor;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Class for transforming {@link CExpression} objects into their {@link SymbolicExpression}
 * representation.
 *
 * <p>Always use {@link #transform} to create correct representations. Otherwise, correctness can't
 * be assured.
 */
public class ExpressionTransformer
    implements CRightHandSideVisitor<
        Collection<SymbolicExpressionAndSMGState>, CPATransferException> {

  // Initial edge of the call to this transformer
  private final CFAEdge edge;

  private final SMGState smgState;

  private final MachineModel machineModel;

  private final LogManagerWithoutDuplicates logger;

  private final SMGOptions options;

  private final SMGCPAExpressionEvaluator evaluator;

  private final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

  public ExpressionTransformer(
      final CFAEdge pEdge,
      final SMGState pSmgState,
      final MachineModel pMachineModel,
      final LogManagerWithoutDuplicates pLogger,
      SMGOptions pOptions,
      SMGCPAExpressionEvaluator pEvaluator) {
    edge = pEdge;
    smgState = pSmgState;
    machineModel = pMachineModel;
    logger = pLogger;
    options = pOptions;
    evaluator = pEvaluator;
  }

  public Collection<SymbolicExpressionAndSMGState> transform(final CExpression pExpression)
      throws CPATransferException {
    return pExpression.accept(this);
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(
      final CBinaryExpression pIastBinaryExpression) throws CPATransferException {

    ImmutableList.Builder<SymbolicExpressionAndSMGState> builder = ImmutableList.builder();
    for (SymbolicExpressionAndSMGState operand1ExpressionAndState :
        pIastBinaryExpression.getOperand1().accept(this)) {

      SMGState currentState = operand1ExpressionAndState.getState();
      SymbolicExpression operand1Expression = operand1ExpressionAndState.getSymbolicExpression();

      if (operand1Expression instanceof AddressExpression addrExpr) {
        if (addrExpr.getOffset().asNumericValue().bigIntegerValue().equals(BigInteger.ZERO)) {
          // TODO: for pointer comparisons etc. we need to unpack the correct value. We can
          // currently handle this only for concrete values, and that is done by the valueVisitor.
          // So we can't handle it here better.
          // Dirty fix: if we end up here, it means we had a unknown before.
          // We return a unknown again by creating one
          operand1Expression = factory.asConstant(addrExpr.getMemoryAddress(), addrExpr.getType());
        }
      }

      ExpressionTransformer newTransformerForNewState =
          new ExpressionTransformer(edge, currentState, machineModel, logger, options, evaluator);

      for (SymbolicExpressionAndSMGState operand2ExpressionAndState :
          pIastBinaryExpression.getOperand2().accept(newTransformerForNewState)) {

        currentState = operand2ExpressionAndState.getState();
        SymbolicExpression operand2Expression = operand2ExpressionAndState.getSymbolicExpression();

        if (operand2Expression instanceof AddressExpression addrExpr) {
          if (addrExpr.getOffset().asNumericValue().bigIntegerValue().equals(BigInteger.ZERO)) {
            // TODO: for pointer comparisons etc. we need to unpack the correct value. We can
            // currently handle this only for concrete values, and that is done by the valueVisitor.
            // So we can't handle it here better.
            operand2Expression =
                factory.asConstant(addrExpr.getMemoryAddress(), addrExpr.getType());
          }
        }

        final Type expressionType = pIastBinaryExpression.getExpressionType();
        final Type calculationType = pIastBinaryExpression.getCalculationType();

        switch (pIastBinaryExpression.getOperator()) {
          case PLUS:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.add(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          case MINUS:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.minus(
                        operand1Expression, operand2Expression, expressionType, calculationType),
                    currentState));
            continue;
          case MULTIPLY:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.multiply(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          case DIVIDE:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.divide(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          case MODULO:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.modulo(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          case SHIFT_LEFT:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.shiftLeft(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          case SHIFT_RIGHT:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.shiftRightSigned(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          case BINARY_AND:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.binaryAnd(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          case BINARY_OR:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.binaryOr(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          case BINARY_XOR:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.binaryXor(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          case EQUALS:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.equal(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          case NOT_EQUALS:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.notEqual(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          case LESS_THAN:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.lessThan(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          case LESS_EQUAL:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.lessThanOrEqual(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          case GREATER_THAN:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.greaterThan(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          case GREATER_EQUAL:
            builder.add(
                SymbolicExpressionAndSMGState.of(
                    factory.greaterThanOrEqual(
                        operand1Expression, operand2Expression, calculationType, calculationType),
                    currentState));
            continue;
          default:
            throw new AssertionError(
                "Unhandled binary operation " + pIastBinaryExpression.getOperator());
        }
      }
    }
    return builder.build();
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(
      final CUnaryExpression pIastUnaryExpression) throws CPATransferException {
    return evaluateToValue(pIastUnaryExpression);
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(final CIdExpression pIastIdExpression)
      throws CPATransferException {
    return evaluateToValue(pIastIdExpression);
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(
      final CCharLiteralExpression pIastCharLiteralExpression) throws CPATransferException {
    final long castValue = pIastCharLiteralExpression.getCharacter();
    final Type charType = pIastCharLiteralExpression.getExpressionType();

    return ImmutableList.of(
        SymbolicExpressionAndSMGState.of(
            SymbolicValueFactory.getInstance().asConstant(createNumericValue(castValue), charType),
            smgState));
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(
      final CFloatLiteralExpression pIastFloatLiteralExpression) throws CPATransferException {
    final BigDecimal value = pIastFloatLiteralExpression.getValue();
    final Type floatType = pIastFloatLiteralExpression.getExpressionType();

    return ImmutableList.of(
        SymbolicExpressionAndSMGState.of(
            SymbolicValueFactory.getInstance().asConstant(createNumericValue(value), floatType),
            smgState));
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(
      final CIntegerLiteralExpression pIastIntegerLiteralExpression) throws CPATransferException {
    final BigInteger value = pIastIntegerLiteralExpression.getValue();
    final Type intType = pIastIntegerLiteralExpression.getExpressionType();

    return ImmutableList.of(
        SymbolicExpressionAndSMGState.of(
            SymbolicValueFactory.getInstance().asConstant(createNumericValue(value), intType),
            smgState));
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(
      final CStringLiteralExpression pIastStringLiteralExpression) throws CPATransferException {
    // This should be a array of chars instead!
    throw new AssertionError("This should never be called.");
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(
      final CTypeIdExpression pIastTypeIdExpression) throws CPATransferException {
    throw new AssertionError("Type id expression invalid for constraint");
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(
      final CImaginaryLiteralExpression pIastLiteralExpression) throws CPATransferException {
    throw new AssertionError("Imaginary literal invalid for constraint");
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(
      final CAddressOfLabelExpression pAddressOfLabelExpression) throws CPATransferException {
    throw new AssertionError("Address of label expression used in symbolic expression");
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(
      final CArraySubscriptExpression pIastArraySubscriptExpression) throws CPATransferException {
    return evaluateToValue(pIastArraySubscriptExpression);
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(final CFieldReference pIastFieldReference)
      throws CPATransferException {
    return evaluateToValue(pIastFieldReference);
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(
      final CPointerExpression pPointerExpression) throws CPATransferException {
    return evaluateToValue(pPointerExpression);
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(
      final CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
    throw new UnsupportedOperationException(
        "Function calls can't be transformed to ConstraintExpressions");
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(final CCastExpression pIastCastExpression)
      throws CPATransferException {
    return evaluateToValue(pIastCastExpression);
  }

  @Override
  public Collection<SymbolicExpressionAndSMGState> visit(
      final CComplexCastExpression complexCastExpression) throws CPATransferException {
    throw new AssertionError("Complex cast not valid for constraint");
  }

  private Collection<SymbolicExpressionAndSMGState> evaluateToValue(final CExpression pExpression)
      throws CPATransferException {

    final SMGCPAValueVisitor vv = getNewValueVisitor(smgState);
    final CType type = SMGCPAExpressionEvaluator.getCanonicalType(pExpression);
    ImmutableList.Builder<SymbolicExpressionAndSMGState> builder = ImmutableList.builder();
    for (ValueAndSMGState valueAndState : vv.evaluate(pExpression, type)) {
      final Value idValue = valueAndState.getValue();
      final SMGState stateAfterEval = valueAndState.getState();

      // TODO: UNKNOWN is a VALID possibility here!
      assert !idValue.isUnknown();

      // The vv takes care of the transformations for us
      builder.add(
          SymbolicExpressionAndSMGState.of(
              SymbolicValueFactory.getInstance()
                  .asConstant(idValue, type)
                  .copyForState(stateAfterEval),
              stateAfterEval));
    }
    return builder.build();
  }

  private SMGCPAValueVisitor getNewValueVisitor(final SMGState pState) {
    return new SMGCPAValueVisitor(evaluator, pState, edge, logger, options);
  }

  private Value createNumericValue(long pValue) {
    return new NumericValue(pValue);
  }

  private Value createNumericValue(BigDecimal pValue) {
    return new NumericValue(pValue);
  }

  private Value createNumericValue(BigInteger pValue) {
    return new NumericValue(pValue);
  }
}
