// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.constraint;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAValueVisitor;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

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

  // Initial edge of the call to this transformer, may be null for transformations that don't use
  // the value visitor (i.e. only memory access checks)
  @Nullable private final CFAEdge edge;

  private final SMGState smgState;

  private final MachineModel machineModel;

  private final LogManagerWithoutDuplicates logger;

  private final SMGOptions options;

  private final SMGCPAExpressionEvaluator evaluator;

  private final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

  public ExpressionTransformer(
      @Nullable final CFAEdge pEdge,
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
            // So we can't handle it better here.
            operand2Expression =
                factory.asConstant(addrExpr.getMemoryAddress(), addrExpr.getType());
          }
        }

        final Type expressionType = pIastBinaryExpression.getExpressionType();
        final Type calculationType = pIastBinaryExpression.getCalculationType();

        final SymbolicExpression resultExpression =
            switch (pIastBinaryExpression.getOperator()) {
              case PLUS ->
                  factory.add(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case MINUS ->
                  factory.minus(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case MULTIPLY ->
                  factory.multiply(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case DIVIDE ->
                  factory.divide(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case MODULO ->
                  factory.modulo(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case SHIFT_LEFT ->
                  factory.shiftLeft(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case SHIFT_RIGHT ->
                  factory.shiftRightSigned(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case BINARY_AND ->
                  factory.binaryAnd(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case BINARY_OR ->
                  factory.binaryOr(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case BINARY_XOR ->
                  factory.binaryXor(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case EQUALS ->
                  factory.equal(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case NOT_EQUALS ->
                  factory.notEqual(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case LESS_THAN ->
                  factory.lessThan(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case LESS_EQUAL ->
                  factory.lessThanOrEqual(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case GREATER_THAN ->
                  factory.greaterThan(
                      operand1Expression, operand2Expression, expressionType, calculationType);
              case GREATER_EQUAL ->
                  factory.greaterThanOrEqual(
                      operand1Expression, operand2Expression, expressionType, calculationType);
            };
        builder.add(SymbolicExpressionAndSMGState.of(resultExpression, currentState));
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
    final FloatValue value = pIastFloatLiteralExpression.getValue();
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
      Value idValue = valueAndState.getValue();
      final SMGState stateAfterEval = valueAndState.getState();

      if (options.crashOnUnknownInConstraint()) {
        assert !idValue.isUnknown();
      } else if (idValue.isUnknown()) {
        // Unknown is top, so we create a new value that does not have any constraints and put it in
        // the constraint
        SymbolicValueFactory svf = SymbolicValueFactory.getInstance();
        idValue = svf.asConstant(svf.newIdentifier(null), type);
      }

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

  public Constraint getNotEqualsZeroConstraint(
      Value valueNotZero, CType calculationType, SMGState currentState) {
    SymbolicExpression zeroValue =
        SymbolicValueFactory.getInstance()
            .asConstant(createNumericValue(BigInteger.ZERO), calculationType);

    SymbolicExpression memoryRegionSizeValue =
        SymbolicValueFactory.getInstance()
            .asConstant(valueNotZero, calculationType)
            .copyForState(currentState);

    // size != 0
    return (Constraint)
        factory.notEqual(memoryRegionSizeValue, zeroValue, calculationType, calculationType);
  }

  /**
   * Builds a constraint for the equality of the given size to 0.
   *
   * @param memoryRegionSizeInBits size of the memory region in bits.
   * @param currentState current {@link SMGState}
   * @return a {@link Constraint} size == 0
   */
  public Constraint checkMemorySizeEqualsZero(
      Value memoryRegionSizeInBits, CType calculationType, SMGState currentState) {
    SymbolicExpression zeroValue =
        SymbolicValueFactory.getInstance()
            .asConstant(createNumericValue(BigInteger.ZERO), calculationType);

    SymbolicExpression memoryRegionSizeValue =
        SymbolicValueFactory.getInstance()
            .asConstant(memoryRegionSizeInBits, calculationType)
            .copyForState(currentState);

    // size == 0
    return factory.equal(memoryRegionSizeValue, zeroValue, calculationType, calculationType);
  }

  public Collection<Constraint> checkValidMemoryAccess(
      Value offsetInBits,
      Value readSizeInBits,
      Value memoryRegionSizeInBits,
      CType comparisonType,
      SMGState currentState) {
    ImmutableSet.Builder<Constraint> constraintBuilder = ImmutableSet.builder();

    SymbolicExpression symbOffsetValue =
        SymbolicValueFactory.getInstance()
            .asConstant(offsetInBits, comparisonType)
            .copyForState(currentState);

    SymbolicExpression zeroValue =
        SymbolicValueFactory.getInstance()
            .asConstant(createNumericValue(BigInteger.ZERO), comparisonType);

    SymbolicExpression readSizeValue =
        SymbolicValueFactory.getInstance()
            .asConstant(readSizeInBits, comparisonType)
            .copyForState(currentState);

    SymbolicExpression offsetPlusReadSize =
        factory.add(symbOffsetValue, readSizeValue, comparisonType, comparisonType);

    SymbolicExpression memoryRegionSizeValue =
        SymbolicValueFactory.getInstance()
            .asConstant(memoryRegionSizeInBits, comparisonType)
            .copyForState(currentState);

    // offset < 0
    SymbolicExpression offsetLessZero =
        factory.lessThan(symbOffsetValue, zeroValue, comparisonType, comparisonType);
    constraintBuilder.add((Constraint) offsetLessZero);

    // offset + read size > size of memory region
    SymbolicExpression offsetPlusSizeGTRegion =
        factory.greaterThan(
            offsetPlusReadSize, memoryRegionSizeValue, comparisonType, comparisonType);
    constraintBuilder.add((Constraint) offsetPlusSizeGTRegion);

    return constraintBuilder.build();
  }

  public Constraint getUnequalConstraint(
      SymbolicValue symbolicValueUnequalTo,
      Value valueUnequalTo,
      CType comparisonType,
      SMGState currentState) {
    SymbolicExpression constSymbolicValueUnequalTo =
        SymbolicValueFactory.getInstance()
            .asConstant(symbolicValueUnequalTo, comparisonType)
            .copyForState(currentState);
    SymbolicExpression constValueUnequalTo =
        SymbolicValueFactory.getInstance()
            .asConstant(valueUnequalTo, comparisonType)
            .copyForState(currentState);

    return (Constraint)
        factory.notEqual(
            constSymbolicValueUnequalTo, constValueUnequalTo, comparisonType, comparisonType);
  }

  public Constraint getEqualConstraint(
      Value arbitrarySymbolicExpression,
      Value valueEqualTo,
      CType comparisonType,
      SMGState currentState) {
    SymbolicExpression constSymbolicValueEqualTo =
        SymbolicValueFactory.getInstance()
            .asConstant(arbitrarySymbolicExpression, comparisonType)
            .copyForState(currentState);
    SymbolicExpression constValueEqualTo =
        SymbolicValueFactory.getInstance()
            .asConstant(valueEqualTo, comparisonType)
            .copyForState(currentState);

    return factory.equal(
        constSymbolicValueEqualTo, constValueEqualTo, comparisonType, comparisonType);
  }

  public List<Constraint> getValidMemoryAccessConstraints(
      Value offsetInBits,
      Value readSizeInBits,
      Value memoryRegionSizeInBits,
      CType comparisonType,
      SMGState currentState) {
    ImmutableList.Builder<Constraint> constraintBuilder = ImmutableList.builder();

    SymbolicExpression symbOffsetValue =
        SymbolicValueFactory.getInstance()
            .asConstant(offsetInBits, comparisonType)
            .copyForState(currentState);

    SymbolicExpression zeroValue =
        SymbolicValueFactory.getInstance()
            .asConstant(createNumericValue(BigInteger.ZERO), comparisonType);

    SymbolicExpression readSizeValue =
        SymbolicValueFactory.getInstance()
            .asConstant(readSizeInBits, comparisonType)
            .copyForState(currentState);

    SymbolicExpression offsetPlusReadSize =
        factory.add(symbOffsetValue, readSizeValue, comparisonType, comparisonType);

    SymbolicExpression memoryRegionSizeValue =
        SymbolicValueFactory.getInstance()
            .asConstant(memoryRegionSizeInBits, comparisonType)
            .copyForState(currentState);

    // offset >= 0
    SymbolicExpression offsetLessZero =
        factory.greaterThanOrEqual(symbOffsetValue, zeroValue, comparisonType, comparisonType);
    constraintBuilder.add((Constraint) offsetLessZero);

    // offset + read size <= size of memory region
    SymbolicExpression offsetPlusSizeGTRegion =
        factory.lessThanOrEqual(
            offsetPlusReadSize, memoryRegionSizeValue, comparisonType, comparisonType);
    constraintBuilder.add((Constraint) offsetPlusSizeGTRegion);

    return constraintBuilder.build();
  }

  private SMGCPAValueVisitor getNewValueVisitor(final SMGState pState) {
    return new SMGCPAValueVisitor(evaluator, pState, edge, logger, options);
  }

  private Value createNumericValue(long pValue) {
    return new NumericValue(pValue);
  }

  private Value createNumericValue(FloatValue pValue) {
    return new NumericValue(pValue);
  }

  private Value createNumericValue(BigInteger pValue) {
    return new NumericValue(pValue);
  }
}
