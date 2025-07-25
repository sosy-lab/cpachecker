// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.primitives.UnsignedLongs;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.FunctionValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.BuiltinFloatFunctions;
import org.sosy_lab.cpachecker.util.BuiltinFunctions;
import org.sosy_lab.cpachecker.util.BuiltinOverflowFunctions;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.RoundingMode;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This visitor visits values mostly on the right hand side to get values (SMG or not) (but also on
 * the left hand side, for example concrete values used in array access) Important: we need to reuse
 * the state given back by other visitors and use the state in this class just to give it to the
 * next/innermost visitor! Read operations have side effects, hence why using the most up-to-date
 * state is important.
 */
public class SMGCPAValueVisitor
    extends DefaultCExpressionVisitor<List<ValueAndSMGState>, CPATransferException>
    implements CRightHandSideVisitor<List<ValueAndSMGState>, CPATransferException> {

  /**
   * length of type LONG in Java (in bit). Needed to determine if a C type fits into a Java type.
   */
  private static final int SIZE_OF_JAVA_LONG = 64;

  // The evaluator translates C expressions into the SMG counterparts and vice versa.
  private final SMGCPAExpressionEvaluator evaluator;
  private final MachineModel machineModel;

  private final SMGState state;

  /** This edge is only to be used for debugging/logging! */
  private final CFAEdge cfaEdge;

  private final LogManagerWithoutDuplicates logger;

  private final SMGOptions options;

  public SMGCPAValueVisitor(
      SMGCPAExpressionEvaluator pEvaluator,
      SMGState currentState,
      CFAEdge edge,
      LogManagerWithoutDuplicates pLogger,
      SMGOptions pOptions) {
    evaluator = pEvaluator;
    machineModel = evaluator.getMachineModel();
    state = currentState;
    cfaEdge = edge;
    logger = pLogger;
    options = pOptions;
  }

  /**
   * This method returns the value of an expression, reduced to match the type. This method handles
   * overflows and casts. If necessary warnings for the user are printed. This method does not touch
   * {@link AddressExpression}s or {@link SymbolicIdentifier}s with {@link MemoryLocation}s, as they
   * carry location information for further evaluation.
   *
   * @param pExp expression to evaluate
   * @param pTargetType the type of the left side of an assignment
   * @return if evaluation successful, then value, else null
   * @throws CPATransferException in case of critical visitor or SMG error
   */
  public List<ValueAndSMGState> evaluate(final CRightHandSide pExp, final CType pTargetType)
      throws CPATransferException {
    List<ValueAndSMGState> uncastedValuesAndStates = pExp.accept(this);
    ImmutableList.Builder<ValueAndSMGState> result = ImmutableList.builder();
    for (ValueAndSMGState uncastedValueAndState : uncastedValuesAndStates) {
      Value castedValue = castCValue(uncastedValueAndState.getValue(), pTargetType);
      result.add(ValueAndSMGState.of(castedValue, uncastedValueAndState.getState()));
    }
    return result.build();
  }

  /**
   * Only use if this is the right hand side of an assignment. This matters because of abstracted
   * lists, in that if the value is a pointer from an abstracted list next field to outside an
   * abstracted list, we need special handling to getting the correct pointer. This method returns
   * the value of an expression, reduced to match the type. This method handles overflows and casts.
   * If necessary warnings for the user are printed. This method does not touch {@link
   * AddressExpression}s or {@link SymbolicIdentifier}s with {@link MemoryLocation}s, as they carry
   * location information for further evaluation.
   *
   * @param pExp expression to evaluate
   * @param pTargetType the type of the left side of an assignment
   * @return if evaluation successful, then value, else null
   * @throws CPATransferException in case of critical visitor or SMG error
   */
  public List<ValueAndSMGState> evaluateAssignmentValue(
      final CRightHandSide pExp, final CType pTargetType) throws CPATransferException {

    // Look up the structure of the access, if we find something that might be ptr->next for
    // abstracted lists with ptr being the last pointer, we might not want to materialize yet
    if (pExp instanceof CFieldReference fieldRef
        && fieldRef.getFieldOwner() instanceof CIdExpression ptrExpr) {
      for (ValueAndSMGState pointerValuesAndState :
          ptrExpr.accept(new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger, options))) {
        SMGState currentState = pointerValuesAndState.getState();
        Value pointerValue = pointerValuesAndState.getValue();
        Value ptrTargetOffset = new NumericValue(BigInteger.ZERO);
        if (pointerValue instanceof AddressExpression addrValue) {
          pointerValue = addrValue.getMemoryAddress();
          ptrTargetOffset = addrValue.getOffset();
        }
        if (!currentState.getMemoryModel().isPointer(pointerValue)) {
          break;
        }

        Optional<SMGStateAndOptionalSMGObjectAndOffset> maybePtrTarget =
            currentState.dereferencePointerWithoutMaterilization(pointerValue);
        if (maybePtrTarget.isEmpty() || !maybePtrTarget.orElseThrow().hasSMGObjectAndOffset()) {
          break;
        }
        SMGObject ptrTarget = maybePtrTarget.orElseThrow().getSMGObject();
        ptrTargetOffset =
            evaluator.addBitOffsetValues(
                ptrTargetOffset, maybePtrTarget.orElseThrow().getOffsetForObject());
        currentState = maybePtrTarget.orElseThrow().getSMGState();

        // Nesting level 0 means either no abstraction or the very end of the list
        // TODO: use last indicator
        int nestingLvl = currentState.getMemoryModel().getNestingLevel(pointerValue);

        if (ptrTarget instanceof SMGSinglyLinkedListSegment linkedListObj && nestingLvl == 0) {
          CFieldReference explicitFieldRef = fieldRef.withExplicitPointerDereference();
          CType returnType =
              SMGCPAExpressionEvaluator.getCanonicalType(explicitFieldRef.getExpressionType());
          BigInteger readSize = evaluator.getBitSizeof(currentState, returnType);
          BigInteger fieldOffset =
              evaluator.getFieldOffsetInBits(
                  SMGCPAExpressionEvaluator.getCanonicalType(explicitFieldRef),
                  explicitFieldRef.getFieldName());
          Value finalReadOffset = evaluator.addBitOffsetValues(ptrTargetOffset, fieldOffset);

          if (finalReadOffset.isExplicitlyKnown()
              && finalReadOffset
                  .asNumericValue()
                  .bigIntegerValue()
                  .equals(linkedListObj.getNextOffset())
              && finalReadOffset.isExplicitlyKnown()) {

            ValueAndSMGState fieldReadAndState =
                currentState.readValueWithoutMaterialization(
                    linkedListObj,
                    finalReadOffset.asNumericValue().bigIntegerValue(),
                    readSize,
                    returnType);
            // This is now the next pointer from the last element of the list (this is the ptr->next
            // part)
            currentState = fieldReadAndState.getState();
            Value readFieldValue = fieldReadAndState.getValue();
            Value fieldTargetOffset = new NumericValue(BigInteger.ZERO);
            if (!(readFieldValue instanceof AddressExpression)) {
              readFieldValue = AddressExpression.of(readFieldValue, returnType, fieldTargetOffset);
            }
            return ImmutableList.of(ValueAndSMGState.of(readFieldValue, currentState));
          }
        }
      }
    }

    return evaluate(pExp, pTargetType);
  }

  @Override
  protected List<ValueAndSMGState> visitDefault(CExpression pExp) throws CPATransferException {
    // Just get a default value and log
    logger.logf(
        Level.INFO,
        "%s, Default value: CExpression %s could not be recognized and the default value %s was"
            + " used for its value. Related CFAEdge: %s",
        cfaEdge.getFileLocation(),
        pExp,
        SMGValue.zeroValue(),
        cfaEdge.getRawStatement());
    return ImmutableList.of(ValueAndSMGState.of(UnknownValue.getInstance(), state));
  }

  @Override
  public List<ValueAndSMGState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {

    return handleFunctions(pIastFunctionCallExpression);
  }

  @Override
  public List<ValueAndSMGState> visit(CArraySubscriptExpression e) throws CPATransferException {
    // Array subscript is default Java array usage. Example: array[5]
    // In C this can be translated to *(array + 5), but the array may be on the stack/heap (or
    // global, but we throw global and stack together when reading). Note: this is commutative!

    // The expression is split into array and subscript expression
    // Use the array expression in the visitor again to get the array address
    // The type of the arrayExpr may be pointer or array, depending on stack/heap
    CExpression arrayExpr = e.getArrayExpression();

    // Return type is either a structure/array in which we simply add the offset
    // Or a CPointerType in which we read and wrap in AddressExpression
    // Or in all other cases just read and return
    CType returnType = SMGCPAExpressionEvaluator.getCanonicalType(e.getExpressionType());
    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    for (ValueAndSMGState arrayValueAndState : arrayExpr.accept(this)) {
      // The arrayValue is either AddressExpression for pointer + offset
      // SymbolicIdentifier with MemoryLocation for variable name + offset
      // Or an invalid value
      Value arrayValue = arrayValueAndState.getValue();
      SMGState currentState = arrayValueAndState.getState();

      if (currentState.getMemoryModel().isPointer(arrayValue)) {
        arrayValue =
            AddressExpression.withZeroOffset(
                arrayValue, SMGCPAExpressionEvaluator.getCanonicalType(arrayExpr));
      } else if (!SMGCPAExpressionEvaluator.valueIsAddressExprOrVariableOffset(arrayValue)) {
        // Not a valid pointer/address
        resultBuilder.add(
            ValueAndSMGState.ofUnknownValue(
                currentState,
                "Returned unknown value due to an invalid address or offset in subscript expression"
                    + " in ",
                cfaEdge));
        continue;
      }
      // Lonely pointers in arrayValue signal local array access

      // Evaluate the subscript as far as possible
      CExpression subscriptExpr = e.getSubscriptExpression();
      List<ValueAndSMGState> subscriptValueAndStates =
          subscriptExpr.accept(
              new SMGCPAValueVisitor(evaluator, currentState, cfaEdge, logger, options));

      for (ValueAndSMGState subscriptValueAndState : subscriptValueAndStates) {
        Value subscriptValue = subscriptValueAndState.getValue();
        SMGState newState = subscriptValueAndState.getState();
        // If the subscript is an unknown value, we can't read anything and return unknown
        // We also overapproximate the access and assume unsafe
        if (!subscriptValue.isNumericValue() && !options.trackErrorPredicates()) {
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  newState.withUnknownOffsetMemoryAccess(),
                  "Returned unknown value due to a unknown address or offset in subscript"
                      + " expression with a memory error in ",
                  cfaEdge));
          continue;
        }

        // Calculate the offset out of the subscript value and the type
        BigInteger typeSizeInBits = evaluator.getBitSizeof(newState, returnType);
        Value subscriptOffset = evaluator.multiplyBitOffsetValues(subscriptValue, typeSizeInBits);

        if (arrayExpr.getExpressionType() instanceof CPointerType) {
          Preconditions.checkArgument(arrayValue instanceof AddressExpression);
        } else if (arrayExpr.getExpressionType() instanceof CCompositeType
            || arrayExpr.getExpressionType() instanceof CElaboratedType
            || arrayExpr.getExpressionType() instanceof CArrayType
            || arrayExpr.getExpressionType() instanceof CTypedefType) {
          if (arrayValue instanceof SymbolicIdentifier) {
            Preconditions.checkArgument(
                ((SymbolicIdentifier) arrayValue).getRepresentedLocation().isPresent());
          } else {
            Preconditions.checkArgument(arrayValue instanceof AddressExpression);
          }

        } else {
          if (arrayValue instanceof SymbolicIdentifier) {
            Preconditions.checkArgument(
                ((SymbolicIdentifier) arrayValue).getRepresentedLocation().isEmpty());
          }
          Preconditions.checkArgument(!(arrayValue instanceof AddressExpression));
        }

        resultBuilder.addAll(
            evaluateReadOfValueAndOffset(arrayValue, subscriptOffset, returnType, newState, e));
      }
    }
    return resultBuilder.build();
  }

  private List<ValueAndSMGState> evaluateReadOfValueAndOffset(
      Value arrayValue,
      Value additionalOffset,
      CType pReturnType,
      SMGState pCurrentState,
      CExpression exprThatCalledThis)
      throws CPATransferException {
    SMGState newState = pCurrentState;
    CType returnType = SMGCPAExpressionEvaluator.getCanonicalType(pReturnType);
    BigInteger typeSizeInBits = evaluator.getBitSizeof(newState, returnType);
    if (arrayValue instanceof AddressExpression arrayAddr) {
      Value addrOffsetValue = arrayAddr.getOffset();

      Value finalOffset = evaluator.addBitOffsetValues(addrOffsetValue, additionalOffset);

      if (SMGCPAExpressionEvaluator.isStructOrUnionType(returnType)
          || returnType instanceof CArrayType
          || returnType instanceof CFunctionType) {
        return ImmutableList.of(
            ValueAndSMGState.of(arrayAddr.copyWithNewOffset(finalOffset), newState));

      } else if (returnType instanceof CPointerType) {
        // This of course does not need to be a pointer value! If this is an unknown we just
        // return unknown.
        // All else gets wrapped and the final read/deref will throw an error
        ImmutableList.Builder<ValueAndSMGState> returnBuilder = ImmutableList.builder();
        for (ValueAndSMGState readPointerAndState :
            evaluator.readValueWithPointerDereference(
                newState,
                arrayAddr.getMemoryAddress(),
                finalOffset,
                typeSizeInBits,
                returnType,
                exprThatCalledThis)) {

          newState = readPointerAndState.getState();
          if (readPointerAndState.getValue().isUnknown()) {
            returnBuilder.add(
                ValueAndSMGState.ofUnknownValue(
                    newState,
                    "Returned unknown value due to an invalid address or offset in read expression"
                        + " with dereference in ",
                    cfaEdge));
          } else {
            returnBuilder.add(
                ValueAndSMGState.of(
                    AddressExpression.withZeroOffset(readPointerAndState.getValue(), returnType),
                    newState));
          }
        }
        return returnBuilder.build();

      } else {
        return evaluator.readValueWithPointerDereference(
            newState,
            arrayAddr.getMemoryAddress(),
            finalOffset,
            typeSizeInBits,
            returnType,
            exprThatCalledThis);
      }
    } else if (arrayValue instanceof SymbolicIdentifier symbolicIdentifier
        && symbolicIdentifier.getRepresentedLocation().isPresent()) {
      MemoryLocation memloc = symbolicIdentifier.getRepresentedLocation().orElseThrow();
      String qualifiedVarName = memloc.getIdentifier();
      Value finalOffset =
          evaluator.addBitOffsetValues(additionalOffset, BigInteger.valueOf(memloc.getOffset()));

      if (SMGCPAExpressionEvaluator.isStructOrUnionType(returnType)
          || returnType instanceof CArrayType
          || returnType instanceof CFunctionType) {

        if (!additionalOffset.isNumericValue()) {
          throw new RuntimeException(
              "Missing case in SMGCPAValueVisitor. Report to CPAchecker issue tracker for SMG2"
                  + " analysis.");
        }

        return ImmutableList.of(
            ValueAndSMGState.of(
                SymbolicValueFactory.getInstance()
                    .newIdentifier(
                        memloc.withAddedOffset(additionalOffset.asNumericValue().longValue())),
                newState));

      } else if (returnType instanceof CPointerType) {
        // This of course does not need to be a pointer value! If this is an unknown we just
        // return unknown. All else gets wrapped and the final read/deref will throw an error
        ImmutableList.Builder<ValueAndSMGState> returnBuilder = ImmutableList.builder();
        for (ValueAndSMGState readPointerAndState :
            evaluator.readStackOrGlobalVariable(
                newState, qualifiedVarName, finalOffset, typeSizeInBits, returnType)) {

          newState = readPointerAndState.getState();
          if (readPointerAndState.getValue().isUnknown()) {
            returnBuilder.add(
                ValueAndSMGState.ofUnknownValue(
                    newState,
                    "Returned unknown value due to an invalid address or offset in read with"
                        + " dereference expression in ",
                    cfaEdge));
          } else {
            returnBuilder.add(
                ValueAndSMGState.of(
                    AddressExpression.withZeroOffset(readPointerAndState.getValue(), returnType),
                    newState));
          }
        }
        return returnBuilder.build();

      } else {
        return evaluator.readStackOrGlobalVariable(
            newState, qualifiedVarName, finalOffset, typeSizeInBits, returnType);
      }

    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              newState,
              "Returned unknown value due to unknown target address in read with dereference"
                  + " expression in ",
              cfaEdge));
    }
  }

  @Override
  public List<ValueAndSMGState> visit(CBinaryExpression e) throws CPATransferException {
    // binary expression, examples: +, -, *, /, ==, !=, < ....
    // visit left and right, then use the expression and return it. This also means we need to
    // create new SMG values (symbolic value ranges) for them, but don't save them in the SMG right
    // away (save, not write!) as this is only done when write is used.

    final CExpression lVarInBinaryExp = e.getOperand1();
    final CExpression rVarInBinaryExp = e.getOperand2();
    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();

    for (ValueAndSMGState leftValueAndState : lVarInBinaryExp.accept(this)) {
      Value leftValue = leftValueAndState.getValue();
      SMGState currentState = leftValueAndState.getState();
      // We can't work with unknowns
      // Return the unknown value directly and not a new one! The mapping to the Value
      // object is important!
      if (leftValue.isUnknown()) {
        resultBuilder.add(
            ValueAndSMGState.ofUnknownValue(
                currentState,
                "Returned unknown value due unknown left value in binary expression in ",
                cfaEdge));
        continue;
      }

      for (ValueAndSMGState rightValueAndState :
          rVarInBinaryExp.accept(
              new SMGCPAValueVisitor(
                  evaluator, leftValueAndState.getState(), cfaEdge, logger, options))) {

        currentState = rightValueAndState.getState();

        Value rightValue = rightValueAndState.getValue();
        if (rightValue.isUnknown()) {
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown value due unknown right value in binary expression in ",
                  cfaEdge));
          continue;
        }

        resultBuilder.addAll(handleBinaryOperation(leftValue, rightValue, e, currentState));
      }
    }
    return resultBuilder.build();
  }

  private List<ValueAndSMGState> handleBinaryOperation(
      Value leftValue, Value rightValue, CBinaryExpression e, SMGState currentState)
      throws CPATransferException {
    final BinaryOperator binaryOperator = e.getOperator();
    final CType calculationType = e.getCalculationType();
    final CExpression lVarInBinaryExp = e.getOperand1();
    final CExpression rVarInBinaryExp = e.getOperand2();
    final CType returnType = SMGCPAExpressionEvaluator.getCanonicalType(e.getExpressionType());
    Preconditions.checkArgument(!leftValue.isUnknown());
    Preconditions.checkArgument(!rightValue.isUnknown());

    ValueAndSMGState castLeftValue = castCValue(leftValue, calculationType, currentState);
    leftValue = castLeftValue.getValue();
    currentState = castLeftValue.getState();
    if (binaryOperator != BinaryOperator.SHIFT_LEFT
        && binaryOperator != BinaryOperator.SHIFT_RIGHT) {
      /* For SHIFT-operations we do not cast the second operator.
       * We do not even need integer-promotion,
       * because the maximum SHIFT of 64 is lower than MAX_CHAR.
       *
       * ISO-C99 (6.5.7 #3): Bitwise shift operators
       * The integer promotions are performed on each of the operands.
       * The type of the result is that of the promoted left operand.
       * If the value of the right operand is negative or is greater than
       * or equal to the width of the promoted left operand,
       * the behavior is undefined.
       */
      ValueAndSMGState castRightValue = castCValue(rightValue, calculationType, currentState);
      rightValue = castRightValue.getValue();
      currentState = castRightValue.getState();
    }

    // TODO: extract this mess into a method and clean it up
    if ((leftValue instanceof AddressExpression
            || rightValue instanceof AddressExpression
            || (evaluator.isPointerValue(rightValue, currentState)
                && evaluator.isPointerValue(leftValue, currentState))
            || ((leftValue instanceof ConstantSymbolicExpression
                    && evaluator.isPointerValue(
                        ((ConstantSymbolicExpression) leftValue).getValue(), currentState))
                && (rightValue instanceof ConstantSymbolicExpression
                    && evaluator.isPointerValue(
                        ((ConstantSymbolicExpression) rightValue).getValue(), currentState))))
        && !(leftValue.isNumericValue() && rightValue.isNumericValue())) {

      // It is possible that addresses get cast to int or smth like it
      // Then the SymbolicIdentifier is returned not in an AddressExpression
      // They might be wrapped in a ConstantSymbolicExpression
      // We don't remove this wrapping for the rest of the analysis as they might actually get
      // treated as ints or something
      Value nonConstRightValue = rightValue;
      if (rightValue instanceof ConstantSymbolicExpression
          && evaluator.isPointerValue(
              ((ConstantSymbolicExpression) rightValue).getValue(), currentState)) {
        nonConstRightValue = ((ConstantSymbolicExpression) rightValue).getValue();
      }
      Value nonConstLeftValue = leftValue;
      if (leftValue instanceof ConstantSymbolicExpression
          && evaluator.isPointerValue(
              ((ConstantSymbolicExpression) leftValue).getValue(), currentState)) {
        nonConstLeftValue = ((ConstantSymbolicExpression) leftValue).getValue();
      }

      switch (binaryOperator) {
        case EQUALS -> {
          Preconditions.checkArgument(returnType instanceof CSimpleType);
          if ((!(nonConstLeftValue instanceof AddressExpression)
                  && !evaluator.isPointerValue(nonConstLeftValue, currentState))
              || (!(nonConstRightValue instanceof AddressExpression)
                  && !evaluator.isPointerValue(nonConstRightValue, currentState))) {
            return ImmutableList.of(
                ValueAndSMGState.ofUnknownValue(
                    currentState,
                    "Returned unknown value due non-address value in binary expression evaluated as"
                        + " pointer arithmetics in ",
                    cfaEdge));
          }
          // address == address or address == not address
          return ImmutableList.of(
              ValueAndSMGState.of(
                  evaluator.checkEqualityForAddresses(
                      nonConstLeftValue, nonConstRightValue, currentState),
                  currentState));
        }

        case NOT_EQUALS -> {
          Preconditions.checkArgument(returnType instanceof CSimpleType);
          // address != address or address != not address
          return ImmutableList.of(
              ValueAndSMGState.of(
                  evaluator.checkNonEqualityForAddresses(
                      nonConstLeftValue, nonConstRightValue, currentState),
                  currentState));
        }

        case PLUS, MINUS -> {
          Value leftAddrExpr = nonConstLeftValue;
          if (!(nonConstLeftValue instanceof AddressExpression)
              && evaluator.isPointerValue(nonConstLeftValue, currentState)
              && !leftAddrExpr.isExplicitlyKnown()) {
            leftAddrExpr =
                AddressExpression.withZeroOffset(
                    nonConstLeftValue, SMGCPAExpressionEvaluator.getCanonicalType(lVarInBinaryExp));
          }
          Value rightAddrExpr = nonConstRightValue;
          if (!(nonConstRightValue instanceof AddressExpression)
              && evaluator.isPointerValue(nonConstRightValue, currentState)
              && !rightAddrExpr.isExplicitlyKnown()) {
            rightAddrExpr =
                AddressExpression.withZeroOffset(
                    nonConstRightValue,
                    SMGCPAExpressionEvaluator.getCanonicalType(rVarInBinaryExp));
          }
          // Pointer arithmetics case and fall through (handled inside the method)
          // i.e. address + 3
          // (This only handles address +- value!)
          return calculatePointerArithmetics(
              leftAddrExpr,
              rightAddrExpr,
              binaryOperator,
              e.getExpressionType(),
              calculationType,
              SMGCPAExpressionEvaluator.getCanonicalType(e.getOperand1().getExpressionType()),
              SMGCPAExpressionEvaluator.getCanonicalType(e.getOperand2().getExpressionType()),
              currentState);
        }

        case GREATER_EQUAL, LESS_EQUAL, GREATER_THAN, LESS_THAN -> {
          // < <= > >=
          // For the same memory, we can check < etc.
          // First check that left and right point to the SAME memory region
          // Check that both Values are truly addresses
          ValueAndSMGState leftValueAndState = evaluator.unpackAddressExpression(leftValue, state);
          leftValue = leftValueAndState.getValue();
          ValueAndSMGState rightValueAndState =
              evaluator.unpackAddressExpression(rightValue, leftValueAndState.getState());
          rightValue = rightValueAndState.getValue();
          currentState = rightValueAndState.getState();
          if (!evaluator.isPointerValue(rightValue, currentState)
              || !evaluator.isPointerValue(leftValue, currentState)) {
            return ImmutableList.of(
                ValueAndSMGState.ofUnknownValue(
                    currentState,
                    "Returned unknown value due to non-address value in binary pointer comparison"
                        + " expression in ",
                    cfaEdge));
          }
          if (!currentState.pointsToSameMemoryRegion(leftValue, rightValue)) {
            // This is undefined behavior in C99/C11
            // But since we don't really handle this we just return unknown :D
            return ImmutableList.of(
                ValueAndSMGState.ofUnknownValue(
                    currentState,
                    "Returned unknown value due to address values in binary pointer comparison"
                        + " expression not pointing to the same target memory in ",
                    cfaEdge));
          }

          // Then get the offsets
          Value offsetLeft = currentState.getPointerOffset(leftValue);
          Value offsetRight = currentState.getPointerOffset(rightValue);
          if (offsetLeft.isUnknown() || offsetRight.isUnknown()) {
            return ImmutableList.of(
                ValueAndSMGState.ofUnknownValue(
                    currentState,
                    "Returned unknown value due to unknown offset value(s) in binary pointer"
                        + " comparison expression in ",
                    cfaEdge));
          }

          // Create binary expr with offsets and restart this with it
          return handleBinaryOperation(offsetLeft, offsetRight, e, currentState);
        }

        default -> {
          // handled below
        }
      }
    }

    if (leftValue instanceof FunctionValue || rightValue instanceof FunctionValue) {
      return ImmutableList.of(
          ValueAndSMGState.of(
              calculateExpressionWithFunctionValue(binaryOperator, rightValue, leftValue),
              currentState));
    }

    if (leftValue instanceof SymbolicValue || rightValue instanceof SymbolicValue) {
      if (leftValue instanceof SymbolicIdentifier) {
        Preconditions.checkArgument(
            ((SymbolicIdentifier) leftValue).getRepresentedLocation().isEmpty());
      } else if (rightValue instanceof SymbolicIdentifier) {
        Preconditions.checkArgument(
            ((SymbolicIdentifier) rightValue).getRepresentedLocation().isEmpty());
      }
      return ImmutableList.of(
          ValueAndSMGState.of(
              calculateSymbolicBinaryExpression(leftValue, rightValue, e), currentState));
    }

    if (!leftValue.isNumericValue() || !rightValue.isNumericValue()) {
      logger.logf(
          Level.FINE,
          "Parameters to binary operation '%s %s %s' are no numeric values. Returned unknown value"
              + " in %s",
          leftValue,
          binaryOperator,
          rightValue,
          cfaEdge);
      return ImmutableList.of(ValueAndSMGState.ofUnknownValue(currentState));
    }

    if (isArithmeticOperation(binaryOperator)) {
      // Actual computations
      Value arithResult =
          arithmeticOperation(
              (NumericValue) leftValue, (NumericValue) rightValue, binaryOperator, calculationType);
      return ImmutableList.of(castCValue(arithResult, e.getExpressionType(), currentState));

    } else if (isComparison(binaryOperator)) {
      // comparisons
      Value returnValue =
          comparisonOperation(
              (NumericValue) leftValue, (NumericValue) rightValue, binaryOperator, calculationType);
      // we do not cast here, because 0 and 1 are small enough for every type.
      return ImmutableList.of(ValueAndSMGState.of(returnValue, currentState));
    } else {
      throw new AssertionError("Unhandled binary operator in the value visitor.");
    }
  }

  /**
   * TODO: this currently only casts values. What happens if i try smth like this:
   *
   * <p>int array[] = ...;
   *
   * <p>char * bla = (char *) array;
   */
  @Override
  public List<ValueAndSMGState> visit(CCastExpression e) throws CPATransferException {
    // Casts are not trivial within SMGs as there might be type reinterpretations used inside the
    // SMGs,
    // but this should be taken care of by the SMGCPAExpressionEvaluator and no longer be a
    // problem here!
    // Get the type and value from the nested expression (might be SMG) and cast the value
    // Also most of this code is taken from the value analysis CPA and modified
    CType targetType = e.getExpressionType();
    ImmutableList.Builder<ValueAndSMGState> builder = new ImmutableList.Builder<>();
    for (ValueAndSMGState valueAndState : e.getOperand().accept(this)) {
      SMGState currentState = valueAndState.getState();
      Value value = valueAndState.getValue();

      builder.add(castCValue(value, targetType, currentState));
    }
    return builder.build();
  }

  public ValueAndSMGState castCValue(Value value, CType targetType, SMGState currentState)
      throws CPATransferException {
    if (targetType instanceof CPointerType) {
      if (value instanceof AddressExpression || value instanceof NumericValue) {
        return ValueAndSMGState.of(value, currentState);

      } else if (evaluator.isPointerValue(value, currentState)) {
        return ValueAndSMGState.of(
            AddressExpression.withZeroOffset(value, targetType), currentState);

      } else if (value.isNumericValue() && options.isCastMemoryAddressesToNumeric()) {
        logger.logf(Level.FINE, "Numeric Value '%s' interpreted as memory address.", value);
        return evaluator.getPointerFromNumeric(value, currentState);

      } else if (options.trackPredicates() && value instanceof SymbolicValue) {
        return ValueAndSMGState.of(castSymbolicValue(value, targetType), currentState);

      } else {
        return ValueAndSMGState.of(UnknownValue.getInstance(), currentState);
      }
    }

    // Interpret address as numeric, try to calculate the operation based on the numeric
    // A pointer deref on a numeric (or a cast) should return it to an address expr or pointer
    if (targetType instanceof CSimpleType cSimpleType && !cSimpleType.hasComplexSpecifier()) {
      if (((value instanceof AddressExpression) || evaluator.isPointerValue(value, currentState))
          && options.isCastMemoryAddressesToNumeric()) {

        logger.logf(Level.FINE, "Memory address '%s' interpreted as numeric value.", value);
        return ValueAndSMGState.of(
            currentState.transformAddressIntoNumericValue(value).orElseThrow(), currentState);
      }
    }

    if (!value.isExplicitlyKnown()) {
      return ValueAndSMGState.of(castSymbolicValue(value, targetType), currentState);
    }

    // We only use numeric/symbolic/unknown values anyway, and we can't cast unknowns
    if (!value.isNumericValue()) {
      logger.logf(
          Level.FINE, "Can not cast C value %s to %s", value.toString(), targetType.toString());
      return ValueAndSMGState.of(value, currentState);
    }
    NumericValue numericValue = (NumericValue) value;

    CType type = targetType.getCanonicalType();
    final int size;
    if (type instanceof CSimpleType) {
      size = evaluator.getBitSizeof(currentState, type).intValue();
    } else if (type instanceof CBitFieldType) {
      size = ((CBitFieldType) type).getBitFieldSize();
      type = ((CBitFieldType) type).getType();
    } else {
      return ValueAndSMGState.of(value, currentState);
    }

    return ValueAndSMGState.of(castNumeric(numericValue, type, size), currentState);
  }

  @Override
  public List<ValueAndSMGState> visit(CFieldReference e) throws CPATransferException {
    // This is the field of a struct/union, so smth like struct.field or struct->field.
    // In the later case it's a pointer dereference.
    // Read the value of the field from the object.

    // First we transform x->f into (*x).f per default. This might fail for non pointer x.
    // In such a case we get a SymbolicIdentifier with the variable name and offset of x.
    CFieldReference explicitReference = e.withExplicitPointerDereference();

    // Owner expression; the struct/union with this field. Use this to get the address of the
    // general object.
    CExpression ownerExpression = explicitReference.getFieldOwner();
    CType returnType =
        SMGCPAExpressionEvaluator.getCanonicalType(explicitReference.getExpressionType());

    // For (*pointer).field case or struct.field case the visitor returns the Value as
    // AddressExpression. For struct.field it's a SymbolicIdentifier with MemoryLocation
    ImmutableList.Builder<ValueAndSMGState> builder = new ImmutableList.Builder<>();
    for (ValueAndSMGState valueAndState : ownerExpression.accept(this)) {
      SMGState currentState = valueAndState.getState();
      // This value is either an AddressValue for pointers i.e. (*struct).field or a general
      // SymbolicValue
      Value structValue = valueAndState.getValue();
      if (structValue.isUnknown()) {
        builder.add(
            ValueAndSMGState.ofUnknownValue(
                currentState,
                "Returned unknown value due to unknown address value for struct in field reference"
                    + " expression in ",
                cfaEdge));
        continue;
      }

      // Now get the offset of the current field
      BigInteger fieldOffset =
          evaluator.getFieldOffsetInBits(
              SMGCPAExpressionEvaluator.getCanonicalType(ownerExpression),
              explicitReference.getFieldName());

      if (ownerExpression.getExpressionType() instanceof CPointerType) {
        Preconditions.checkArgument(structValue instanceof AddressExpression);
      } else if (ownerExpression.getExpressionType() instanceof CCompositeType
          || ownerExpression.getExpressionType() instanceof CElaboratedType
          || ownerExpression.getExpressionType() instanceof CArrayType
          || ownerExpression.getExpressionType() instanceof CTypedefType) {
        if (structValue instanceof SymbolicIdentifier symbolicIdentifier) {
          Preconditions.checkArgument(symbolicIdentifier.getRepresentedLocation().isPresent());
        } else {
          Preconditions.checkArgument(structValue instanceof AddressExpression);
        }
      } else {
        Preconditions.checkArgument(!(structValue instanceof SymbolicIdentifier));
        Preconditions.checkArgument(!(structValue instanceof AddressExpression));
      }

      builder.addAll(
          evaluateReadOfValueAndOffset(
              structValue, new NumericValue(fieldOffset), returnType, currentState, e));
    }
    return builder.build();
  }

  @Override
  public List<ValueAndSMGState> visit(CIdExpression e) throws CPATransferException {
    // essentially stack or global variables
    // Either CEnumerator, CVariableDeclaration, CParameterDeclaration
    // Could also be a type/function declaration, one of which is malloc().
    // We either read the stack/global variable for non pointer and non struct/unions, or package it
    // in an AddressExpression for pointers
    // or SymbolicValue with a memory location and the name of the variable inside.

    CSimpleDeclaration varDecl = e.getDeclaration();
    CType returnType = SMGCPAExpressionEvaluator.getCanonicalType(e.getExpressionType());
    if (varDecl == null) {
      // The variable was not declared
      throw new SMGException("Usage of undeclared variable: " + e.getName() + ".");
    }

    String variableName = varDecl.getQualifiedName();

    ImmutableList.Builder<SMGState> creationBuilder = ImmutableList.builder();
    if (!state.isLocalOrGlobalVariablePresent(variableName)
        && !state.isLocalVariablePresentOnPreviousStackFrame(variableName)) {
      if (varDecl instanceof CVariableDeclaration cVariableDeclaration) {
        creationBuilder.addAll(
            evaluator.handleVariableDeclarationWithoutInizializer(state, cVariableDeclaration));
      } else if (varDecl instanceof CParameterDeclaration cParameterDeclaration) {
        creationBuilder.addAll(
            evaluator.handleVariableDeclarationWithoutInizializer(
                state, cParameterDeclaration.asVariableDeclaration()));
      } else {
        throw new SMGException("Unhandled on-the-fly variable creation type: " + varDecl);
      }
    } else {
      creationBuilder.add(state);
    }

    ImmutableList.Builder<ValueAndSMGState> finalStatesBuilder = ImmutableList.builder();
    for (SMGState currentState : creationBuilder.build()) {
      if (returnType instanceof CArrayType) {
        // If the variable is a pointer to an array, deref that and return the array

        // if the variable is an array, create/search new pointer to the array and return that
        finalStatesBuilder.add(
            evaluator.createAddressForLocalOrGlobalVariable(variableName, currentState));

      } else if (SMGCPAExpressionEvaluator.isStructOrUnionType(returnType)) {
        // Struct/Unions on the stack/global; return the memory location in a
        // SymbolicIdentifier. This is then used as interpretation such that the Value
        // of the memory location (on the stack) is used. This is used by assignments only as far as
        // I know, i.e. when assigning a complete struct to a new variable.
        finalStatesBuilder.add(
            ValueAndSMGState.of(
                SymbolicValueFactory.getInstance()
                    .newIdentifier(MemoryLocation.forIdentifier(variableName).withOffset(0)),
                currentState));

      } else if (returnType instanceof CPointerType || returnType instanceof CFunctionType) {
        // Pointer/Array/Function types should return a Value that internally can be translated into
        // a
        // SMGValue that leads to an SMGPointsToEdge that leads to the correct object (with
        // potential
        // offsets inside the points to edge). These have to be packaged into an AddressExpression
        // with a 0 offset. Modifications of the offset of the address can be done by subsequent
        // methods. (The check is fine because we already filtered out structs/unions)
        BigInteger sizeInBits = evaluator.getBitSizeof(currentState, e.getExpressionType());
        // Now use the qualified name to get the actual global/stack memory location
        for (ValueAndSMGState readValueAndState :
            evaluator.readStackOrGlobalVariable(
                currentState,
                varDecl.getQualifiedName(),
                new NumericValue(BigInteger.ZERO),
                sizeInBits,
                SMGCPAExpressionEvaluator.getCanonicalType(e))) {
          Value readValue = readValueAndState.getValue();
          SMGState newState = readValueAndState.getState();

          if (returnType instanceof CFunctionType
              || ((CPointerType) returnType).getType() instanceof CFunctionType) {
            // TODO: lift into more general place
            if (newState.isPointingToMallocZero(readValue)) {
              newState = newState.withInvalidReadOfMallocZeroPointer(readValue);
            }
          }

          Value addressValue;
          if (evaluator.isPointerValue(readValue, newState)) {
            addressValue = AddressExpression.withZeroOffset(readValue, returnType);
          } else {
            // Not a known pointer value, most likely an unknown value as symbolic identifier
            addressValue = readValue;
          }

          finalStatesBuilder.add(ValueAndSMGState.of(addressValue, newState));
        }

      } else {
        // Everything else should be readable and returnable directly; just return the Value
        BigInteger sizeInBits = evaluator.getBitSizeof(currentState, e.getExpressionType());
        // Now use the qualified name to get the actual global/stack memory location
        finalStatesBuilder.addAll(
            evaluator.readStackOrGlobalVariable(
                currentState,
                varDecl.getQualifiedName(),
                new NumericValue(BigInteger.ZERO),
                sizeInBits,
                SMGCPAExpressionEvaluator.getCanonicalType(e)));
      }
    }
    return finalStatesBuilder.build();
  }

  @Override
  public List<ValueAndSMGState> visit(CCharLiteralExpression e) throws CPATransferException {
    // Simple character expression; We use the numeric value
    int value = e.getCharacter();

    // We simply return the Value, as if a mapping to SMGValue is needed only after Value is written
    // into the memory, but when writing a mapping is created anyway
    return ImmutableList.of(ValueAndSMGState.of(new NumericValue(value), state));
  }

  @Override
  public List<ValueAndSMGState> visit(CFloatLiteralExpression e) throws CPATransferException {
    // Floating point value expression
    FloatValue value = e.getValue();

    // We simply return the Value, as if a mapping to SMGValue is needed only after Value is written
    // into the memory, but when writing a mapping is created anyway
    return ImmutableList.of(ValueAndSMGState.of(new NumericValue(value), state));
  }

  @Override
  public List<ValueAndSMGState> visit(CIntegerLiteralExpression e) throws CPATransferException {
    // Simple int expression
    BigInteger value = e.getValue();

    // We simply return the Value, as if a mapping to SMGValue is needed only after Value is written
    // into the memory, but when writing a mapping is created anyway
    return ImmutableList.of(ValueAndSMGState.of(new NumericValue(value), state));
  }

  @Override
  public List<ValueAndSMGState> visit(CStringLiteralExpression e) throws CPATransferException {
    // TODO: both the value and old SMG analysis simply return unknown in this case
    // String string = e.getContentString();
    // ImmutableList.Builder<ValueAndSMGState> builder = ImmutableList.builder();
    logger.log(Level.WARNING, "Analysis approximated string literal expression in " + cfaEdge);
    return ImmutableList.of(ValueAndSMGState.ofUnknownValue(state));
  }

  @Override
  public List<ValueAndSMGState> visit(CTypeIdExpression e) throws CPATransferException {
    // Operators:
    // sizeOf, typeOf and
    // _Alignof or alignof = the number of bytes between successive addresses, careful because of
    // padding!

    final TypeIdOperator idOperator = e.getOperator();
    final CType innerType = e.getType();

    return switch (idOperator) {
      case SIZEOF -> {
        BigInteger size = evaluator.getBitSizeof(state, innerType);
        yield ImmutableList.of(ValueAndSMGState.of(new NumericValue(size), state));
      }
      case ALIGNOF -> {
        BigInteger align = evaluator.getAlignOf(innerType);
        yield ImmutableList.of(ValueAndSMGState.of(new NumericValue(align), state));
      }
      case TYPEOF -> {
        // This can't really be solved here as we can only return Values
        logger.log(
            Level.WARNING,
            "Approximated unknown value due to missing handling of type id expression in "
                + cfaEdge);
        yield ImmutableList.of(ValueAndSMGState.ofUnknownValue(state));
      }
    };
  }

  @Override
  public List<ValueAndSMGState> visit(CUnaryExpression e) throws CPATransferException {
    // Unary expression types like & (address of operator), sizeOf(), - (unary minus), TILDE
    // (bitwise not) and alignof

    UnaryOperator unaryOperator = e.getOperator();
    CExpression unaryOperand = e.getOperand();
    CType returnType = SMGCPAExpressionEvaluator.getCanonicalType(e.getExpressionType());
    CType operandType =
        SMGCPAExpressionEvaluator.getCanonicalType(unaryOperand.getExpressionType());

    switch (unaryOperator) {
      case SIZEOF -> {
        BigInteger sizeInBits = evaluator.getBitSizeof(state, operandType);
        return ImmutableList.of(
            ValueAndSMGState.of(new NumericValue(sizeInBits.divide(BigInteger.valueOf(8))), state));
      }
      case ALIGNOF -> {
        return ImmutableList.of(
            ValueAndSMGState.of(
                new NumericValue(machineModel.getAlignof(unaryOperand.getExpressionType())),
                state));
      }
      case AMPER -> {
        // Note: this returns AddressExpressions! Unwrap before saving!
        return evaluator.createAddress(unaryOperand, state, cfaEdge);
      }
      default -> {}
    }

    ImmutableList.Builder<ValueAndSMGState> builder = new ImmutableList.Builder<>();
    for (ValueAndSMGState valueAndState : unaryOperand.accept(this)) {
      SMGState currentState = valueAndState.getState();
      Value value = valueAndState.getValue();

      if (value instanceof SymbolicValue) {
        builder.add(
            ValueAndSMGState.of(
                createSymbolicExpression(value, operandType, unaryOperator, returnType),
                currentState));
        continue;
      } else if (!value.isNumericValue()) {
        logger.logf(
            Level.FINE,
            "Returned unknown due to invalid argument %s for unary operator %s.",
            value,
            unaryOperator);
        builder.add(ValueAndSMGState.ofUnknownValue(currentState));
        continue;
      }

      final NumericValue numericValue = (NumericValue) value;
      switch (unaryOperator) {
        case MINUS -> {
          builder.add(ValueAndSMGState.of(numericValue.negate(), currentState));
          continue;
        }
        case TILDE -> {
          builder.add(
              ValueAndSMGState.of(new NumericValue(~numericValue.longValue()), currentState));
          continue;
        }
        default -> throw new AssertionError("Unknown unary operator: " + unaryOperator);
      }
    }
    return builder.build();
  }

  @Override
  public List<ValueAndSMGState> visit(CPointerExpression e) throws CPATransferException {
    // This should subevaluate to an AddressExpression in the visit call in the beginning as we
    // always evaluate to the address, but only
    // dereference and read it if it's not a struct/union as those will be dereferenced
    // by the field expression

    // Get the type of the target
    CType returnType = SMGCPAExpressionEvaluator.getCanonicalType(e.getExpressionType());
    // Get the expression that is dereferenced
    CExpression expr = e.getOperand();
    // Evaluate the expression to a Value; this should return a Symbolic Value with the address of
    // the target and an offset. If this fails this returns an UnknownValue.
    ImmutableList.Builder<ValueAndSMGState> builder = new ImmutableList.Builder<>();
    for (ValueAndSMGState valueAndState : expr.accept(this)) {
      SMGState currentState = valueAndState.getState();
      // Try to disassemble the values (AddressExpression)
      Value value = valueAndState.getValue();
      if (value.isUnknown()) {
        // A possibility is that the program tries to deref a nondet for example
        builder.add(
            ValueAndSMGState.ofUnknownValue(
                currentState.withUnknownPointerDereferenceWhenReading(value, cfaEdge), cfaEdge));
        continue;
      }

      if (!(value instanceof AddressExpression) && evaluator.isPointerValue(value, currentState)) {
        // For pointer deref on arrays only
        value =
            AddressExpression.of(value, e.getExpressionType(), new NumericValue(BigInteger.ZERO));
      }

      if (!(value instanceof AddressExpression)) {
        // Non-pointer dereference, either numeric or symbolic
        Preconditions.checkArgument(
            (value.isNumericValue()
                    && value.asNumericValue().bigIntegerValue().equals(BigInteger.ZERO))
                || !evaluator.isPointerValue(value, currentState));
        builder.add(
            ValueAndSMGState.ofUnknownValue(
                currentState.withUnknownPointerDereferenceWhenReading(value, cfaEdge),
                "Returned unknown value due to non-pointer dereference with invalid-deref in"
                    + " expression in ",
                cfaEdge));
        continue;
      }

      AddressExpression pointerValue = (AddressExpression) value;

      // The offset part of the pointer; its either numeric or we can't get a concrete value
      Value offset = pointerValue.getOffset();
      if (!offset.isNumericValue() && !options.trackErrorPredicates()) {
        // If the offset is not numericly known we can't read a value, return unknown iff we don't
        // check with SMT solvers later
        builder.add(
            ValueAndSMGState.ofUnknownValue(
                currentState.withUnknownOffsetMemoryAccess(),
                "Returned unknown value due to unknown offset value in pointer dereference"
                    + " expression in ",
                cfaEdge));
        continue;
      }

      BigInteger sizeInBits = evaluator.getBitSizeof(currentState, returnType);

      if (SMGCPAExpressionEvaluator.isStructOrUnionType(returnType)) {
        if (!offset.isNumericValue()) {
          // If the offset is not numericly known we can't read a value
          builder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState.withUnknownOffsetMemoryAccess(),
                  "Returned unknown value due to unknown offset value in pointer dereference"
                      + " expression in ",
                  cfaEdge));
          continue;
        }
        // We don't want to read struct/union! In those cases we return the AddressExpression
        // such that the following visitor methods can dereference the fields correctly
        builder.add(ValueAndSMGState.of(value, currentState));

      } else if (returnType instanceof CArrayType) {
        // Arrays in C are wierd....
        // Essentially, they might be treated as pointers, but are not really pointers.
        // Since they are used in a subscript expr after this (else it would be the else case below)
        // we return the pointer so that the subscript works
        return ImmutableList.of(ValueAndSMGState.of(pointerValue, currentState));

      } else {
        // "Normal" return types
        // Default case either *pointer or *(pointer + smth), but both get transformed into a
        // AddressExpression Value type with the correct offset build in, so
        // just dereference the pointer with the correct type

        // Dereference the Value and return it. The read checks for validity etc.
        // The precondition is a precondition for get(0) because of no state split
        Preconditions.checkArgument(
            !currentState.getMemoryModel().pointsToZeroPlus(pointerValue.getMemoryAddress()));
        ValueAndSMGState readValueAndState =
            evaluator
                .readValueWithPointerDereference(
                    currentState,
                    pointerValue.getMemoryAddress(),
                    offset,
                    sizeInBits,
                    returnType,
                    e)
                .get(0);
        currentState = readValueAndState.getState();

        if (returnType instanceof CPointerType) {
          // In the pointer case we would need to encapsulate it again
          builder.add(
              ValueAndSMGState.of(
                  AddressExpression.withZeroOffset(readValueAndState.getValue(), returnType),
                  currentState));
        } else {
          builder.add(readValueAndState);
        }
      }
    }
    return builder.build();
  }

  @Override
  public List<ValueAndSMGState> visit(CAddressOfLabelExpression e) throws CPATransferException {
    // && expression
    // This is not in the C standard, just gcc
    // https://gcc.gnu.org/onlinedocs/gcc/Labels-as-Values.html
    // Returns an address to a function
    // TODO:

    return visitDefault(e);
  }

  @Override
  public List<ValueAndSMGState> visit(CImaginaryLiteralExpression e) throws CPATransferException {
    // TODO: do we even need those?
    // Imaginary part for complex numbers
    return visitDefault(e);
  }

  @Override
  public List<ValueAndSMGState> visit(CComplexCastExpression e) throws CPATransferException {
    // TODO: do we need those?
    // Cast for complex numbers?
    return visitDefault(e);
  }

  // Copied from value CPA
  private Value createSymbolicExpression(
      Value pValue, CType pOperandType, UnaryOperator pUnaryOperator, CType pExpressionType) {
    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    SymbolicExpression operand = factory.asConstant(pValue, pOperandType);

    return switch (pUnaryOperator) {
      case MINUS -> factory.negate(operand, pExpressionType);
      case TILDE -> factory.binaryNot(operand, pExpressionType);
      default -> throw new AssertionError("Unhandled unary operator " + pUnaryOperator);
    };
  }

  // ++++++++++++++++++++ Below this point casting helper methods

  /** Taken from the value analysis CPA and modified. Casts symbolic {@link Value}s. */
  private Value castSymbolicValue(Value pValue, Type pTargetType) {
    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

    if (pValue instanceof SymbolicValue symbolicValue && pTargetType instanceof CSimpleType) {
      return factory.cast(symbolicValue, pTargetType);
    }

    // If the value is not symbolic, just return it.
    return pValue;
  }

  /** Taken from the value analysis CPA. TODO: check that all casts are correct and add missing. */
  private Value castNumeric(
      @NonNull final NumericValue numericValue, final CType type, final int size) {

    if (!(type instanceof CSimpleType st)) {
      return numericValue;
    }

    switch (st.getType()) {
      case BOOL -> {
        return convertToBool(numericValue);
      }
      case INT128, INT, CHAR -> {
        // TODO: look more closely at the INT/CHAR cases, especially at the loggedEdges stuff
        // TODO: check for overflow(source larger than the highest number we can store in target
        // etc.)

        boolean targetIsSigned = machineModel.isSigned(st);
        BigInteger integerValue;

        // Convert the value to integer
        if (numericValue.hasFloatType()) {
          // Casting from a floating point value to BigInteger
          Optional<BigInteger> maybeInteger = numericValue.getFloatValue().toInteger();
          if (maybeInteger.isEmpty()) {
            // If the value was NaN or Infinity the result of the conversion is undefined
            return UnknownValue.getInstance();
          } else {
            integerValue = maybeInteger.orElseThrow();
          }
        } else {
          // Casting from Rational or one of the integer types
          integerValue = numericValue.bigIntegerValue();
        }

        // Calculate bounds for overflow
        final BigInteger maxValue = BigInteger.ONE.shiftLeft(size); // 2^size
        BigInteger signedUpperBound;
        BigInteger signedLowerBound;
        if (targetIsSigned) {
          // signed value must be put in interval [-(maxValue/2), (maxValue/2)-1]
          // upper bound maxValue / 2 - 1
          signedUpperBound = maxValue.divide(BigInteger.valueOf(2)).subtract(BigInteger.ONE);
          // lower bound -maxValue / 2
          signedLowerBound = maxValue.divide(BigInteger.valueOf(2)).negate();
        } else {
          signedUpperBound = maxValue.subtract(BigInteger.ONE);
          signedLowerBound = BigInteger.ZERO;
        }

        BigInteger result;

        // Check for overflows
        if (numericValue.hasFloatType()) {
          // Casting from a floating point value
          if (isGreaterThan(integerValue, signedUpperBound)
              || isLessThan(integerValue, signedLowerBound)) {
            // If the number does not fit into the target type the result is undefined
            return UnknownValue.getInstance();
          } else {
            result = integerValue;
          }
        } else {
          // Casting from Rational or an integer type
          result = integerValue.remainder(maxValue); // shrink to number of bits

          if (isGreaterThan(result, signedUpperBound)) {
            // if result overflows, let it 'roll around' and add overflow to lower bound
            result = result.subtract(maxValue);
          } else if (isLessThan(result, signedLowerBound)) {
            result = result.add(maxValue);
          }
        }

        // transform result to a long and fail if it doesn't fit
        if (size < SIZE_OF_JAVA_LONG || (size == SIZE_OF_JAVA_LONG && targetIsSigned)) {
          return new NumericValue(result.longValueExact());
        } else {
          return new NumericValue(result);
        }
      }

      case FLOAT, DOUBLE, FLOAT128 -> {
        FloatValue.Format target;

        // Find the target format
        final int bitPerByte = machineModel.getSizeofCharInBits();
        if (size == machineModel.getSizeofFloat() * bitPerByte) {
          target = FloatValue.Format.Float32;
        } else if (size == machineModel.getSizeofDouble() * bitPerByte) {
          target = FloatValue.Format.Float64;
        } else if (size == machineModel.getSizeofLongDouble() * bitPerByte) {
          // Must be Linux32 or Linux64, otherwise the second clause would have matched
          target = FloatValue.Format.Float80;
        } else if (size == machineModel.getSizeofFloat128() * bitPerByte) {
          target = FloatValue.Format.Float128;
        } else {
          // Unsupported target format
          throw new AssertionError(
              String.format(
                  "Unsupported target format. Value `%s` with bitwidth %d can't be cast to type"
                      + "`%s`",
                  numericValue, size, st.getType()));
        }

        Number result;

        // Convert to target format
        // TODO: Add warnings for lossy conversions?
        if (numericValue.hasFloatType()) {
          // Casting from a floating point value
          if (numericValue.getNumber() instanceof FloatValue floatValue) {
            // Already a FloatValue
            // We just need to adjust the precision
            result = floatValue.withPrecision(target);
          } else {
            // Either Double or Float
            // Cast to double and then convert
            result = FloatValue.fromDouble(numericValue.doubleValue());
          }
        } else if (numericValue.hasIntegerType()) {
          // Casting from an integer
          result = FloatValue.fromInteger(target, numericValue.bigIntegerValue());
        } else if (numericValue.getNumber() instanceof Rational rationalValue) {
          // Casting from a rational
          result = FloatValue.fromRational(target, rationalValue);
        } else {
          // Unsupported value type
          throw new AssertionError(
              String.format(
                  "Unsupported type. Value `%s` has type `%s`, but only integers, floating points"
                      + "and rationals are allowed.",
                  numericValue, numericValue.getNumber().getClass().getSimpleName()));
        }

        return new NumericValue(result);
      }

      default -> throw new AssertionError("Unhandled type: " + type);
    }
  }

  /** Taken from the value analysis CPA. */
  private Value convertToBool(final NumericValue pValue) {
    Number n = pValue.getNumber();
    if (isBooleanFalseRepresentation(n)) {
      return new NumericValue(0);
    } else {
      return new NumericValue(1);
    }
  }

  /** Taken from the value analysis CPA. */
  private boolean isBooleanFalseRepresentation(final Number n) {
    return ((n instanceof Float || n instanceof Double) && 0 == n.doubleValue())
        || (n instanceof BigInteger && BigInteger.ZERO.equals(n))
        || (n instanceof BigDecimal bigDecimal && bigDecimal.compareTo(BigDecimal.ZERO) == 0)
        || 0 == n.longValue();
  }

  /**
   * Returns whether first integer is greater than second integer. Taken from the value analysis CPA
   */
  private boolean isGreaterThan(BigInteger i1, BigInteger i2) {
    return i1.compareTo(i2) > 0;
  }

  /**
   * Returns whether first integer is less than second integer. Taken from the value analysis CPA
   */
  private boolean isLessThan(BigInteger i1, BigInteger i2) {
    return i1.compareTo(i2) < 0;
  }

  // +++++++++++++++++++ Below this point methods for handling functions

  private boolean isUnspecifiedType(CType pType) {
    return pType instanceof CSimpleType cSimpleType
        && cSimpleType.getType() == CBasicType.UNSPECIFIED;
  }

  /** Cast the argument to a floating point type */
  private static FloatValue castToFloat(
      MachineModel pMachineModel, CSimpleType pTargetType, NumericValue pValue) {
    checkArgument(pTargetType.getType().isFloatingPointType());
    FloatValue.Format precision = FloatValue.Format.fromCType(pMachineModel, pTargetType);
    return pValue.floatingPointValue(precision);
  }

  /**
   * Helper method to handle unary builtin function in {@link
   * AbstractExpressionValueVisitor#visit(CFunctionCallExpression)}
   */
  private List<ValueAndSMGState> handleBuiltinFunction1(
      String pName,
      List<Value> pArguments,
      SMGState pState,
      Function<FloatValue, Value> pOperation) {
    final Value parameter = Iterables.getOnlyElement(pArguments);
    if (parameter.isExplicitlyKnown()) {
      // Cast the argument to match the function type
      FloatValue value =
          castToFloat(
              machineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pName),
              (NumericValue) parameter);

      return ImmutableList.of(ValueAndSMGState.of(pOperation.apply(value), pState));
    } else {
      return ImmutableList.of(ValueAndSMGState.ofUnknownValue(pState));
    }
  }

  /**
   * Helper method to handle binary builtin function in {@link
   * AbstractExpressionValueVisitor#visit(CFunctionCallExpression)}
   */
  private List<ValueAndSMGState> handleBuiltinFunction2(
      String pName,
      List<Value> pArguments,
      SMGState pState,
      BiFunction<FloatValue, FloatValue, Value> pOperation) {
    checkArgument(pArguments.size() == 2);
    Value parameter1 = pArguments.get(0);
    Value parameter2 = pArguments.get(1);

    if (parameter1.isExplicitlyKnown() && parameter2.isExplicitlyKnown()) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pName);
      FloatValue value1 = castToFloat(machineModel, targetType, (NumericValue) parameter1);
      FloatValue value2 = castToFloat(machineModel, targetType, (NumericValue) parameter2);

      return ImmutableList.of(ValueAndSMGState.of(pOperation.apply(value1, value2), pState));
    } else {
      return ImmutableList.of(ValueAndSMGState.ofUnknownValue(pState));
    }
  }

  /*
   * Handles ALL function calls
   */
  private List<ValueAndSMGState> handleFunctions(
      CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
    CExpression functionNameExp = pIastFunctionCallExpression.getFunctionNameExpression();

    // We only handle builtin functions
    if (functionNameExp instanceof CIdExpression cIdExpression) {
      String calledFunctionName = cIdExpression.getName();

      if (BuiltinFunctions.isBuiltinFunction(calledFunctionName)) {

        CType functionType = BuiltinFunctions.getFunctionType(calledFunctionName);

        if (isUnspecifiedType(functionType) && !calledFunctionName.equals("__builtin_alloca")) {
          // unsupported formula
          return ImmutableList.of(
              ValueAndSMGState.ofUnknownValue(
                  state,
                  "Returned unknown value due to unknown function "
                      + calledFunctionName
                      + " in expression in ",
                  cfaEdge));
        }

        List<CExpression> parameterExpressions =
            pIastFunctionCallExpression.getParameterExpressions();
        ImmutableList.Builder<Value> parameterValuesBuilder = ImmutableList.builder();

        // Evaluate all parameters
        SMGState currentState = state;
        for (CExpression currParamExp : parameterExpressions) {
          // Here we expect only 1 result value
          SMGCPAValueVisitor vv =
              new SMGCPAValueVisitor(evaluator, currentState, cfaEdge, logger, options);
          List<ValueAndSMGState> newValuesAndStates =
              vv.evaluate(currParamExp, SMGCPAExpressionEvaluator.getCanonicalType(currParamExp));
          Preconditions.checkArgument(newValuesAndStates.size() == 1);
          Value newValue = newValuesAndStates.get(0).getValue();
          // CPA access has side effects! Always take the newest state!
          currentState = newValuesAndStates.get(0).getState();

          parameterValuesBuilder.add(newValue);
        }
        List<Value> parameterValues = parameterValuesBuilder.build();

        // TODO: split this mess into functions
        if (BuiltinOverflowFunctions.isBuiltinOverflowFunction(calledFunctionName)) {
          /*
           * Problem: this method needs an AbstractExpressionValueVisitor as input (this)
           * but this class is not correctly abstracted such that we can inherit it here
           * (because it essentially is the same except for 1 method that would need to be
           * abstract)
           *
           * return BuiltinOverflowFunctions.evaluateFunctionCall(
           *   pIastFunctionCallExpression, this, machineModel, logger);
           */
          return ImmutableList.of(ValueAndSMGState.of(UnknownValue.getInstance(), currentState));

        } else if (BuiltinFloatFunctions.matchesAbsolute(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> new NumericValue(arg.abs()));

        } else if (BuiltinFloatFunctions.matchesHugeVal(calledFunctionName)
            || BuiltinFloatFunctions.matchesInfinity(calledFunctionName)) {
          checkArgument(parameterValues.isEmpty());
          FloatValue.Format precision =
              FloatValue.Format.fromCType(
                  machineModel,
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName));
          return ImmutableList.of(
              ValueAndSMGState.of(new NumericValue(FloatValue.infinity(precision)), currentState));

        } else if (BuiltinFloatFunctions.matchesNaN(calledFunctionName)) {
          // FIXME: Add support for NaN payloads
          checkArgument(parameterValues.size() < 2);
          FloatValue.Format precision =
              FloatValue.Format.fromCType(
                  machineModel,
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName));
          return ImmutableList.of(
              ValueAndSMGState.of(new NumericValue(FloatValue.nan(precision)), currentState));

        } else if (BuiltinFloatFunctions.matchesIsNaN(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> new NumericValue(arg.isNan() ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesIsInfinity(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> new NumericValue(arg.isInfinite() ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesIsInfinitySign(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) ->
                  new NumericValue(arg.isInfinite() ? (arg.isNegative() ? -1 : 1) : 0));

        } else if (BuiltinFloatFunctions.matchesFinite(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> new NumericValue((arg.isInfinite() || arg.isNan()) ? 0 : 1));

        } else if (BuiltinFloatFunctions.matchesFloor(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> new NumericValue(arg.round(FloatValue.RoundingMode.FLOOR)));

        } else if (BuiltinFloatFunctions.matchesCeil(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> new NumericValue(arg.round(FloatValue.RoundingMode.CEILING)));

        } else if (BuiltinFloatFunctions.matchesRound(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) ->
                  new NumericValue(arg.round(FloatValue.RoundingMode.NEAREST_AWAY)));

        } else if (BuiltinFloatFunctions.matchesLround(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> {
                FloatValue value = arg.round(RoundingMode.NEAREST_AWAY);
                return switch (machineModel.getSizeofLongInt()) {
                  case Integer.BYTES -> new NumericValue(value.integerValue());
                  case Long.BYTES -> new NumericValue(value.longValue());
                  default -> Value.UnknownValue.getInstance();
                };
              });

        } else if (BuiltinFloatFunctions.matchesLlround(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> {
                FloatValue value = arg.round(RoundingMode.NEAREST_AWAY);
                return switch (machineModel.getSizeofLongLongInt()) {
                  case Integer.BYTES -> new NumericValue(value.integerValue());
                  case Long.BYTES -> new NumericValue(value.longValue());
                  default -> Value.UnknownValue.getInstance();
                };
              });

        } else if (BuiltinFloatFunctions.matchesTrunc(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> new NumericValue(arg.round(FloatValue.RoundingMode.TRUNCATE)));

        } else if (BuiltinFloatFunctions.matchesFdim(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue(
                      arg1.lessOrEqual(arg2)
                          ? FloatValue.zero(arg1.getFormat())
                          : arg1.subtract(arg2)));

        } else if (BuiltinFloatFunctions.matchesFmax(calledFunctionName)) {
          // TODO: Add a warning message for fmax(0.0,-0.0) and fmax(-0.0, 0.0)
          // The value is undefined and we simply pick 0.0 in those cases, but gcc will always
          // return the first argument.
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue(
                      switch (arg1.compareWithTotalOrder(arg2)) {
                        case -1 -> arg2.isNan() ? arg1 : arg2;
                        case +1 -> arg1.isNan() ? arg2 : arg1;
                        default -> arg1;
                      }));

        } else if (BuiltinFloatFunctions.matchesFmin(calledFunctionName)) {
          // FIXME: Add a warning message for fmin(0.0,-0.0) and fmin(-0.0, 0.0)
          // The value is undefined and we pick -0.0 in those cases, but gcc will return the first
          // argument for `float` or `double` and the second for `long double`
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue(
                      switch (arg1.compareWithTotalOrder(arg2)) {
                        case -1 -> arg1.isNan() ? arg2 : arg1;
                        case +1 -> arg2.isNan() ? arg1 : arg2;
                        default -> arg1;
                      }));

        } else if (BuiltinFloatFunctions.matchesSignbit(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> new NumericValue(arg.isNegative() ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesCopysign(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg1, FloatValue arg2) -> new NumericValue(arg1.copySign(arg2)));

        } else if (BuiltinFloatFunctions.matchesFloatClassify(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> {
                int fpClass;
                if (arg.isNan()) {
                  fpClass = 0;
                } else if (arg.isInfinite()) {
                  fpClass = 1;
                } else if (arg.isZero()) {
                  fpClass = 2;
                } else if (arg.abs().lessThan(FloatValue.minNormal(arg.getFormat()))) {
                  // Subnormal numbers
                  fpClass = 3;
                } else {
                  fpClass = 4;
                }
                return new NumericValue(fpClass);
              });

        } else if (BuiltinFloatFunctions.matchesModf(calledFunctionName)) {
          // FIXME: Assign the integer part to the pointer from the second parameter
          if (parameterValues.size() == 2) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              FloatValue arg =
                  castToFloat(
                      machineModel,
                      BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName),
                      (NumericValue) value);

              if (arg.isInfinite()) {
                // Return zero if the number is infinite
                return ImmutableList.of(
                    ValueAndSMGState.of(
                        new NumericValue(
                            arg.isNegative()
                                ? FloatValue.negativeZero(arg.getFormat())
                                : FloatValue.zero(arg.getFormat())),
                        currentState));
              } else {
                // Otherwise, get the fractional part
                return ImmutableList.of(
                    ValueAndSMGState.of(
                        new NumericValue(arg.modulo(FloatValue.one(arg.getFormat()))),
                        currentState));
              }
            }
          }

        } else if (BuiltinFloatFunctions.matchesFremainder(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg1, FloatValue arg2) -> new NumericValue(arg1.remainder(arg2)));

        } else if (BuiltinFloatFunctions.matchesFmod(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg1, FloatValue arg2) -> new NumericValue(arg1.modulo(arg2)));

        } else if (BuiltinFloatFunctions.matchesIsgreater(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue(arg1.greaterThan(arg2) ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesIsgreaterequal(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue(arg1.greaterOrEqual(arg2) ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesIsless(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg1, FloatValue arg2) -> new NumericValue(arg1.lessThan(arg2) ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesIslessequal(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue(arg1.lessOrEqual(arg2) ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesIslessgreater(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue(arg1.lessOrGreater(arg2) ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesIsunordered(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue((arg1.isNan() || arg2.isNan()) ? 1 : 0));
        }
      }
      // This checks and uses builtins and also unknown functions based on the options
      SMGCPABuiltins smgBuiltins = evaluator.getBuiltinFunctionHandler();
      return smgBuiltins.handleFunctionCall(
          pIastFunctionCallExpression, calledFunctionName, state, cfaEdge);
    }
    return ImmutableList.of(
        ValueAndSMGState.ofUnknownValue(
            state,
            "Returned unknown value due to unknown function "
                + pIastFunctionCallExpression.getFunctionNameExpression()
                + " in expression in ",
            cfaEdge));
  }

  /* ++++++++++++++++++ Below this point value arithmetics and comparisons  ++++++++++++++++++ */

  // TODO: check if we can really just change the ordering / that all possible calculations are
  // commutative
  private Value calculateExpressionWithFunctionValue(
      BinaryOperator binaryOperator, Value val1, Value val2) {
    if (val1 instanceof FunctionValue functionValue) {
      return calculateOperationWithFunctionValue(binaryOperator, functionValue, val2);
    } else if (val2 instanceof FunctionValue functionValue) {
      return calculateOperationWithFunctionValue(binaryOperator, functionValue, val1);
    } else {
      return new Value.UnknownValue();
    }
  }

  /**
   * Calculates pointer/address arithmetic expressions. Valid is only address + value or value +
   * address and address minus value or address minus address. All others are simply unknown value!
   * One of the 2 entered values must be an AddressExpression, no other preconditions have to be
   * met.
   *
   * @param leftValue left hand side value of the arithmetic operation.
   * @param rightValue right hand side value of the arithmetic operation.
   * @param binaryOperator {@link BinaryOperator} in between the values.
   * @param expressionType {@link CType} of the final expression.
   * @param calculationType {@link CType} of the calculation. (Should be int for pointers)
   * @param currentState current {@link SMGState}
   * @return {@link ValueAndSMGState} with the result Value that may be {@link AddressExpression} /
   *     {@link UnknownValue} or a symbolic/numeric one depending on input + the new up-to-date
   *     state.
   */
  private List<ValueAndSMGState> calculatePointerArithmetics(
      Value leftValue,
      Value rightValue,
      BinaryOperator binaryOperator,
      CType expressionType,
      CType calculationType,
      CType leftValueType,
      CType rightValueType,
      SMGState currentState)
      throws CPATransferException {
    // Find the address, check that the other is a numeric value and use as offset, else if both
    // are addresses we allow the distance, else unknown (we can't dereference symbolics)
    // TODO: stop for illegal pointer arith?
    if (binaryOperator != BinaryOperator.PLUS && binaryOperator != BinaryOperator.MINUS) {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to invalid pointer arithmetics operator "
                  + binaryOperator
                  + " in ",
              cfaEdge));
    }

    // The canonical type is the return type of the pointer expression!
    CType canonicalReturnType = expressionType;
    if (calculationType instanceof CPointerType) {
      canonicalReturnType = ((CPointerType) expressionType).getType();
    }

    if (leftValue instanceof AddressExpression addressValue
        && !(rightValue instanceof AddressExpression)) {
      Value addressOffset = addressValue.getOffset();
      if (!options.trackPredicates()
          && (!rightValue.isNumericValue() || !addressOffset.isNumericValue())) {
        return ImmutableList.of(
            ValueAndSMGState.ofUnknownValue(
                currentState,
                "Returned unknown value due to symbolic or unknown offset value in pointer"
                    + " arithmetics expression without predicate tracking in ",
                cfaEdge));
      }

      Value correctlyTypedOffset;
      if (calculationType instanceof CPointerType) {
        // This is the pointer++; case for example.
        // We need the correct types here; the types of the returned value after the pointer
        // expression!
        correctlyTypedOffset =
            calculateArithmeticOperationWithBitPromotion(
                new NumericValue(evaluator.getBitSizeof(currentState, canonicalReturnType)),
                leftValueType,
                rightValue,
                rightValueType,
                BinaryOperator.MULTIPLY);
      } else {
        // If it's a casted pointer, i.e. ((unsigned int) pointer) + 8;
        // then this is just the numeric value * 8 and then the operation.
        correctlyTypedOffset =
            calculateArithmeticOperationWithBitPromotion(
                new NumericValue(BigInteger.valueOf(8)),
                leftValueType,
                rightValue,
                rightValueType,
                BinaryOperator.MULTIPLY);
      }

      Value finalOffset =
          calculateArithmeticOperationWithBitPromotion(
              addressOffset, leftValueType, correctlyTypedOffset, rightValueType, binaryOperator);

      if (finalOffset instanceof SymbolicExpression symOffset) {
        int currentOffsetTypeBits =
            evaluator
                .getMachineModel()
                .getSizeofInBits((CType) symOffset.getType())
                .intValueExact();
        int pointerTypeInBits =
            evaluator
                .getMachineModel()
                .getSizeofInBits(CPointerType.POINTER_TO_CHAR)
                .intValueExact();
        Preconditions.checkArgument(currentOffsetTypeBits >= (pointerTypeInBits + 3));
      }

      return ImmutableList.of(
          ValueAndSMGState.of(addressValue.copyWithNewOffset(finalOffset), currentState));

    } else if (!(leftValue instanceof AddressExpression)
        && rightValue instanceof AddressExpression addressValue) {
      Value addressOffset = addressValue.getOffset();
      if (!leftValue.isNumericValue()
          || !addressOffset.isNumericValue()
          || binaryOperator == BinaryOperator.MINUS) {
        // TODO: symbolic values if possible
        return ImmutableList.of(
            ValueAndSMGState.ofUnknownValue(
                currentState,
                "Returned unknown value due to unknown offset value in pointer arithmetics"
                    + " expression in ",
                cfaEdge));
      }
      Value correctlyTypedOffset;
      if (calculationType instanceof CPointerType) {
        correctlyTypedOffset =
            arithmeticOperation(
                new NumericValue(evaluator.getBitSizeof(currentState, canonicalReturnType)),
                (NumericValue) leftValue,
                BinaryOperator.MULTIPLY,
                machineModel.getPointerSizedIntType());
      } else {
        // If it's a casted pointer, i.e. ((unsigned int) pointer) + 8;
        // then this is just the numeric value * 8 and then the operation.
        correctlyTypedOffset =
            arithmeticOperation(
                new NumericValue(BigInteger.valueOf(8)),
                (NumericValue) leftValue,
                BinaryOperator.MULTIPLY,
                calculationType);
      }

      Value finalOffset =
          arithmeticOperation(
              (NumericValue) correctlyTypedOffset,
              (NumericValue) addressOffset,
              binaryOperator,
              calculationType);

      return ImmutableList.of(
          ValueAndSMGState.of(addressValue.copyWithNewOffset(finalOffset), currentState));

    } else {
      // Either we have 2 address expressions or 2 numeric 0
      if (rightValue.isNumericValue()
          && leftValue.isNumericValue()
          && rightValue
              .asNumericValue()
              .getNumber()
              .equals(leftValue.asNumericValue().getNumber())) {
        return ImmutableList.of(ValueAndSMGState.of(new NumericValue(0), currentState));
      }
      // Both are pointers, we allow minus here to get the distance
      AddressExpression addressRight = (AddressExpression) rightValue;
      AddressExpression addressLeft = (AddressExpression) leftValue;
      Value leftOffset = addressLeft.getOffset();
      Value rightOffset = addressRight.getOffset();

      // This fails if the underlying structure is not the same!
      // We need the non-equal method for SMGs here as it might be that due to abstraction 2 values
      // are not equal but refer to the same structure!
      if (binaryOperator != BinaryOperator.MINUS
          || !rightOffset.isNumericValue()
          || !leftOffset.isNumericValue()) {
        // TODO: symbolic values if possible
        return ImmutableList.of(
            ValueAndSMGState.ofUnknownValue(
                currentState,
                "Returned unknown value due to unknown offset value in pointer dereference"
                    + " expression in ",
                cfaEdge));
      }

      ImmutableList.Builder<ValueAndSMGState> returnBuilder = ImmutableList.builder();
      // Our offsets are in bits here! This also checks that it's the same underlying memory object.
      for (ValueAndSMGState distanceInBitsAndState :
          evaluator.calculateAddressDistance(
              currentState, addressLeft.getMemoryAddress(), addressRight.getMemoryAddress())) {

        Value distanceInBits = distanceInBitsAndState.getValue();
        currentState = distanceInBitsAndState.getState();
        if (!distanceInBits.isNumericValue()) {
          returnBuilder.add(ValueAndSMGState.of(distanceInBits, currentState));
          continue;
        }

        // distance in bits / type size = distance
        // The type in both pointers is the same, we need the return type from one of them
        NumericValue size;
        if (leftValueType instanceof CPointerType cPointerType) {
          size = new NumericValue(evaluator.getBitSizeof(currentState, cPointerType.getType()));
        } else if (addressRight.getType() instanceof CArrayType) {
          size =
              new NumericValue(
                  evaluator.getBitSizeof(currentState, ((CArrayType) leftValueType).getType()));
        } else {
          returnBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown value due to type error in pointer arithmetics expression in ",
                  cfaEdge));
          continue;
        }
        // Undefined behavior if this assertion does not hold
        assert leftValueType.equals(rightValueType);
        Value distance =
            arithmeticOperation(
                (NumericValue) distanceInBits,
                size,
                BinaryOperator.DIVIDE,
                machineModel.getPointerSizedIntType());

        returnBuilder.add(ValueAndSMGState.of(distance, currentState));
      }
      return returnBuilder.build();
    }
  }

  private Value calculateArithmeticOperationWithBitPromotion(
      Value leftValue,
      CType leftValueType,
      Value rightValue,
      CType rightValueType,
      BinaryOperator binOp)
      throws CPATransferException {
    if (rightValue instanceof NumericValue numValueRight
        && leftValue instanceof NumericValue numValueLeft) {
      return arithmeticOperation(
          numValueLeft, numValueRight, binOp, evaluator.getCTypeForBitPreciseMemoryAddresses());
    } else {
      // Symbolic offset
      return createSymbolicExpression(
          leftValue,
          leftValueType,
          rightValue,
          rightValueType,
          binOp,
          evaluator.getCTypeForBitPreciseMemoryAddresses(),
          evaluator.getCTypeForBitPreciseMemoryAddresses());
    }
  }

  /**
   * Join a symbolic expression with something else using a binary expression.
   *
   * <p>e.g. joining `a` and `5` with `+` will produce `a + 5`
   *
   * @param pLValue left hand side value
   * @param pRValue right hand side value
   * @param pExpression the binary expression with the operator
   * @return the calculated Value
   */
  public Value calculateSymbolicBinaryExpression(
      Value pLValue, Value pRValue, final CBinaryExpression pExpression) throws SMGException {

    // While the offsets and even the values of the addresses may be symbolic, the address
    // expressions themselves may never be handled in such a way
    if (pLValue instanceof AddressExpression || pRValue instanceof AddressExpression) {
      logger.logf(
          Level.ALL,
          "Could not determine result of %s operation on one or more memory addresses.",
          pExpression);
      return UnknownValue.getInstance();
    }

    final BinaryOperator operator = pExpression.getOperator();

    final CType leftOperandType = pExpression.getOperand1().getExpressionType();
    final CType rightOperandType = pExpression.getOperand2().getExpressionType();
    final CType expressionType = pExpression.getExpressionType();
    final CType calculationType = pExpression.getCalculationType();

    // Evaluate == symbolics if possible
    if (operator.equals(BinaryOperator.EQUALS)
        && pLValue.equals(pRValue)
        && !(calculationType instanceof CSimpleType simpleType
            && simpleType.getType().isFloatingPointType())) {
      return new NumericValue(1);
    }

    return createSymbolicExpression(
        pLValue,
        leftOperandType,
        pRValue,
        rightOperandType,
        operator,
        expressionType,
        calculationType);
  }

  private Value createSymbolicExpression(
      Value pLeftValue,
      CType pLeftType,
      Value pRightValue,
      CType pRightType,
      CBinaryExpression.BinaryOperator pOperator,
      CType pExpressionType,
      CType pCalculationType)
      throws SMGException {

    if (pLeftValue.isNumericValue() && pRightValue.isNumericValue()) {
      throw new SMGException(
          "Error when creating a symbolic expression. Please inform the maintainer of SMG2.");
    }

    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    SymbolicExpression leftOperand = factory.asConstant(pLeftValue, pLeftType);
    SymbolicExpression rightOperand = factory.asConstant(pRightValue, pRightType);

    // Simplify floating point expressions
    // TODO Add more simplifications
    // TODO Move this code to the methods in SymbolicValueFactory?
    if (pLeftValue.isNumericValue() && pLeftValue.asNumericValue().hasFloatType()) {
      FloatValue leftNum = pLeftValue.asNumericValue().getFloatValue();
      if (ImmutableList.of(
                  BinaryOperator.PLUS,
                  BinaryOperator.MINUS,
                  BinaryOperator.MULTIPLY,
                  BinaryOperator.DIVIDE)
              .contains(pOperator)
          && leftNum.isNan()) {
        return pLeftValue;
      }
    }
    if (pRightValue.isNumericValue() && pRightValue.asNumericValue().hasFloatType()) {
      FloatValue rightNum = pRightValue.asNumericValue().getFloatValue();
      if (ImmutableList.of(
                  BinaryOperator.PLUS,
                  BinaryOperator.MINUS,
                  BinaryOperator.MULTIPLY,
                  BinaryOperator.DIVIDE)
              .contains(pOperator)
          && rightNum.isNan()) {
        return pRightValue;
      }
    }

    if (pLeftValue.isNumericValue()) {
      BigInteger leftNum = pLeftValue.asNumericValue().bigIntegerValue();
      rightOperand = factory.asConstant(pRightValue, pRightType);
      if ((pOperator == BinaryOperator.PLUS && leftNum.equals(BigInteger.ZERO))
          || (pOperator == BinaryOperator.MULTIPLY && leftNum.equals(BigInteger.ONE))) {
        if (!pLeftType.equals(pExpressionType)) {
          return factory.cast(rightOperand, pExpressionType);
        }
        return rightOperand;
      } else if (pOperator == BinaryOperator.MINUS && leftNum.equals(BigInteger.ZERO)) {
        return factory.negate(rightOperand, pExpressionType);
      } else if ((pOperator == BinaryOperator.MULTIPLY && leftNum.equals(BigInteger.ZERO))
          || (pOperator == BinaryOperator.DIVIDE && leftNum.equals(BigInteger.ZERO))) {
        return new NumericValue(BigInteger.ZERO);
      }
    } else if (pRightValue.isNumericValue()) {
      leftOperand = factory.asConstant(pLeftValue, pLeftType);
      BigInteger rightNum = pRightValue.asNumericValue().bigIntegerValue();
      if ((pOperator == BinaryOperator.MULTIPLY && rightNum.equals(BigInteger.ONE))
          || (pOperator == BinaryOperator.PLUS && rightNum.equals(BigInteger.ZERO))
          || (pOperator == BinaryOperator.MINUS && rightNum.equals(BigInteger.ZERO))) {
        if (!pLeftType.equals(pExpressionType)) {
          return factory.cast(leftOperand, pExpressionType);
        }
        return leftOperand;
      } else if (pOperator == BinaryOperator.MULTIPLY && rightNum.equals(BigInteger.ZERO)) {
        return new NumericValue(BigInteger.ZERO);
      }
    }

    return switch (pOperator) {
      case PLUS -> factory.add(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MINUS -> factory.minus(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MULTIPLY ->
          factory.multiply(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case DIVIDE -> factory.divide(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MODULO -> factory.modulo(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case SHIFT_LEFT ->
          factory.shiftLeft(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case SHIFT_RIGHT ->
          factory.shiftRightSigned(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_AND ->
          factory.binaryAnd(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_OR ->
          factory.binaryOr(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_XOR ->
          factory.binaryXor(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case EQUALS -> factory.equal(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case NOT_EQUALS ->
          factory.notEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LESS_THAN ->
          factory.lessThan(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LESS_EQUAL ->
          factory.lessThanOrEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case GREATER_THAN ->
          factory.greaterThan(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case GREATER_EQUAL ->
          factory.greaterThanOrEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
    };
  }

  private NumericValue calculateOperationWithFunctionValue(
      BinaryOperator binaryOperator, FunctionValue val1, Value val2) {
    return switch (binaryOperator) {
      case EQUALS -> new NumericValue(val1.equals(val2) ? 1 : 0);
      case NOT_EQUALS -> new NumericValue(val1.equals(val2) ? 0 : 1);
      default ->
          throw new AssertionError(
              "Operation " + binaryOperator + " is not supported for function values");
    };
  }

  /**
   * Calculate an arithmetic operation on two Value types.
   *
   * @param lNum left hand side value
   * @param rNum right hand side value
   * @param op the binary operator
   * @param calculationType The type the result of the calculation should have
   * @return the resulting values
   */
  private Value arithmeticOperation(
      final NumericValue lNum,
      final NumericValue rNum,
      final BinaryOperator op,
      final CType calculationType) {

    // At this point we're only handling values of simple types.
    final CSimpleType type = getArithmeticType(calculationType);
    if (type == null) {
      logger.logf(
          Level.FINE, "unsupported type %s for result of binary operation %s", calculationType, op);
      return Value.UnknownValue.getInstance();
    }

    try {
      switch (type.getType()) {
        case INT -> {
          // Both l and r must be of the same type, which in this case is INT, so we can cast to
          // long.
          long lVal = lNum.getNumber().longValue();
          long rVal = rNum.getNumber().longValue();
          long result = arithmeticOperation(lVal, rVal, op, calculationType);
          return new NumericValue(result);
        }
        case INT128 -> {
          BigInteger lVal = lNum.bigIntegerValue();
          BigInteger rVal = rNum.bigIntegerValue();
          BigInteger result = arithmeticOperation(lVal, rVal, op);
          return new NumericValue(result);
        }
        case FLOAT, DOUBLE, FLOAT128 -> {
          return new NumericValue(
              arithmeticOperation(
                  op,
                  castToFloat(machineModel, type, lNum),
                  castToFloat(machineModel, type, rNum)));
        }
        default -> {
          logger.logf(
              Level.FINE, "unsupported type for result of binary operation %s", type.toString());
          return Value.UnknownValue.getInstance();
        }
      }
    } catch (ArithmeticException e) { // log warning and ignore expression
      logger.logf(
          Level.WARNING,
          "expression causes arithmetic exception (%s): %s %s %s",
          e.getMessage(),
          lNum,
          op.getOperator(),
          rNum);
      return Value.UnknownValue.getInstance();
    }
  }

  /**
   * Calculate an arithmetic operation on two integer types.
   *
   * @param l left hand side value
   * @param r right hand side value
   * @param op the binary operator
   * @param calculationType The type the result of the calculation should have
   * @return the resulting value
   */
  private long arithmeticOperation(
      final long l, final long r, final BinaryOperator op, final CType calculationType) {

    // special handling for UNSIGNED_LONGLONG (32 and 64bit), UNSIGNED_LONG (64bit)
    // because Java only has SIGNED_LONGLONG
    CSimpleType st = getArithmeticType(calculationType);
    if (st != null) {
      if (machineModel.getSizeofInBits(st) >= SIZE_OF_JAVA_LONG && st.hasUnsignedSpecifier()) {
        switch (op) {
          case DIVIDE -> {
            if (r == 0) {
              logger.logf(Level.SEVERE, "Division by Zero (%d / %d)", l, r);
              return 0;
            }
            return UnsignedLongs.divide(l, r);
          }
          case MODULO -> {
            return UnsignedLongs.remainder(l, r);
          }
          case SHIFT_RIGHT -> {
            /*
             * from http://docs.oracle.com/javase/tutorial/java/nutsandbolts/op3.html
             *
             * The unsigned right shift operator ">>>" shifts a zero
             * into the leftmost position, while the leftmost position
             * after ">>" depends on sign extension.
             */
            return l >>> r;
          }
          default -> {}
        }
      }
    }

    switch (op) {
      case PLUS -> {
        return l + r;
      }
      case MINUS -> {
        return l - r;
      }
      case DIVIDE -> {
        if (r == 0) {
          logger.logf(Level.SEVERE, "Division by Zero (%d / %d)", l, r);
          return 0;
        }
        return l / r;
      }
      case MODULO -> {
        return l % r;
      }
      case MULTIPLY -> {
        return l * r;
      }
      case SHIFT_LEFT -> {
        /* There is a difference in the SHIFT-operation in Java and C.
         * In C a SHIFT is a normal SHIFT, in Java the rVal is used as (r%64).
         *
         * http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.19
         *
         * If the promoted type of the left-hand operand is long, then only the
         * six lowest-order bits of the right-hand operand are used as the
         * shift distance. It is as if the right-hand operand were subjected to
         * a bitwise logical AND operator & (§15.22.1) with the mask value 0x3f.
         * The shift distance actually used is therefore always in the range 0 to 63.
         */
        return (r >= SIZE_OF_JAVA_LONG) ? 0 : l << r;
      }
      case SHIFT_RIGHT -> {
        return l >> r;
      }
      case BINARY_AND -> {
        return l & r;
      }
      case BINARY_OR -> {
        return l | r;
      }
      case BINARY_XOR -> {
        return l ^ r;
      }
      default -> throw new AssertionError("unknown binary operation: " + op);
    }
  }

  /**
   * Calculate an arithmetic operation on two int128 types.
   *
   * @param l left hand side value
   * @param r right hand side value
   * @param op the binary operator
   * @return the resulting value
   */
  private BigInteger arithmeticOperation(
      final BigInteger l, final BigInteger r, final BinaryOperator op) {

    switch (op) {
      case PLUS -> {
        return l.add(r);
      }
      case MINUS -> {
        return l.subtract(r);
      }
      case DIVIDE -> {
        if (r.equals(BigInteger.ZERO)) {
          // this matches the behavior of long
          logger.logf(Level.SEVERE, "Division by Zero (%s / %s)", l.toString(), r.toString());
          return BigInteger.ZERO;
        }
        return l.divide(r);
      }
      case MODULO -> {
        return l.mod(r);
      }
      case MULTIPLY -> {
        return l.multiply(r);
      }
      case SHIFT_LEFT -> {
        // (C11, 6.5.7p3) "If the value of the right operand is negative
        // or is greater than or equal to the width of the promoted left operand,
        // the behavior is undefined"
        if (r.compareTo(BigInteger.valueOf(128)) <= 0 && r.signum() != -1) {
          return l.shiftLeft(r.intValue());
        } else {
          logger.logf(
              Level.SEVERE,
              "Right-hand side (%s) of the bitshift is larger than 128 or negative.",
              r.toString());
          return BigInteger.ZERO;
        }
      }
      case SHIFT_RIGHT -> {
        if (r.compareTo(BigInteger.valueOf(128)) <= 0 && r.signum() != -1) {
          return l.shiftRight(r.intValue());
        } else {
          return BigInteger.ZERO;
        }
      }
      case BINARY_AND -> {
        return l.and(r);
      }
      case BINARY_OR -> {
        return l.or(r);
      }
      case BINARY_XOR -> {
        return l.xor(r);
      }
      default -> throw new AssertionError("Unknown binary operation: " + op);
    }
  }

  /**
   * Calculate an arithmetic operation on two floating point values.
   *
   * @param pOperation the binary operator
   * @param pArg1 left hand side value
   * @param pArg2 right hand side value
   * @return the resulting value
   */
  private FloatValue arithmeticOperation(
      final BinaryOperator pOperation, final FloatValue pArg1, final FloatValue pArg2) {

    return switch (pOperation) {
      case PLUS -> pArg1.add(pArg2);
      case MINUS -> pArg1.subtract(pArg2);
      case DIVIDE -> pArg1.divide(pArg2);
      case MODULO -> pArg1.modulo(pArg2);
      case MULTIPLY -> pArg1.multiply(pArg2);
      case SHIFT_LEFT, SHIFT_RIGHT, BINARY_AND, BINARY_OR, BINARY_XOR ->
          throw new UnsupportedOperationException(
              "Trying to perform " + pOperation + " on floating point operands");
      default -> throw new IllegalArgumentException("Unknown binary operation: " + pOperation);
    };
  }

  private Value comparisonOperation(
      final NumericValue l,
      final NumericValue r,
      final BinaryOperator op,
      final CType calculationType) {

    // At this point we're only handling values of simple types.
    final CSimpleType type = getArithmeticType(calculationType);
    if (type == null) {
      logger.logf(
          Level.FINE, "unsupported type %s for result of binary operation %s", calculationType, op);
      return Value.UnknownValue.getInstance();
    }

    switch (type.getType()) {
      case INT128, CHAR, INT -> {
        // TODO: test this in particular!
        BigInteger leftBigInt = l.bigIntegerValue();
        BigInteger rightBigInt = r.bigIntegerValue();
        final int cmp = leftBigInt.compareTo(rightBigInt);

        // returns True, iff cmp fulfills the boolean operation.
        boolean result =
            switch (op) {
              case GREATER_THAN -> cmp > 0;
              case GREATER_EQUAL -> cmp >= 0;
              case LESS_THAN -> cmp < 0;
              case LESS_EQUAL -> cmp <= 0;
              case EQUALS -> cmp == 0;
              case NOT_EQUALS -> cmp != 0;
              default -> throw new AssertionError("Unknown binary operation: " + op);
            };

        // return 1 if expression holds, 0 otherwise
        return new NumericValue(result ? 1 : 0);
      }
      case FLOAT, DOUBLE, FLOAT128 -> {
        boolean result =
            comparisonOperation(
                op, castToFloat(machineModel, type, l), castToFloat(machineModel, type, r));
        return new NumericValue(result ? 1 : 0);
      }
      default -> {
        logger.logf(
            Level.FINE,
            "unsupported type %s for result of binary operation %s",
            type.toString(),
            op);
        return Value.UnknownValue.getInstance();
      }
    }
  }

  /**
   * Calculate a comparison operation on two floating point values.
   *
   * @param pOperation the binary operator
   * @param pArg1 left hand side value
   * @param pArg2 right hand side value
   * @return the resulting value
   */
  private boolean comparisonOperation(
      final BinaryOperator pOperation, final FloatValue pArg1, final FloatValue pArg2) {

    return switch (pOperation) {
      case GREATER_THAN -> pArg1.greaterThan(pArg2);
      case GREATER_EQUAL -> pArg1.greaterOrEqual(pArg2);
      case LESS_THAN -> pArg1.lessThan(pArg2);
      case LESS_EQUAL -> pArg1.lessOrEqual(pArg2);
      case EQUALS -> pArg1.equalTo(pArg2);
      case NOT_EQUALS -> !pArg1.equalTo(pArg2);
      default -> throw new AssertionError("unknown binary operation: " + pOperation);
    };
  }

  /**
   * This method returns the input-value, cast to match the type. If the value matches the type, it
   * is returned unchanged. This method handles overflows and print warnings for the user. Example:
   * This method is called, when a value of type 'integer' is assigned to a variable of type 'char'.
   *
   * @param value will be cast.
   * @param targetType value will be cast to targetType.
   * @return the cast Value
   */
  public Value castCValue(@NonNull final Value value, final CType targetType) {

    if (!value.isExplicitlyKnown()) {
      if (value instanceof AddressExpression
          || (value instanceof SymbolicIdentifier symbolicIdentifier
              && symbolicIdentifier.getRepresentedLocation().isPresent())) {
        // Don't cast pointers or values carrying location information
        return value;
      } else {
        return castIfSymbolic(value, targetType);
      }
    }

    // For now can only cast numeric value's
    if (!value.isNumericValue()) {
      logger.logf(
          Level.FINE, "Can not cast C value %s to %s", value.toString(), targetType.toString());
      return value;
    }
    NumericValue numericValue = (NumericValue) value;

    CType type = targetType.getCanonicalType();
    final int size;
    if (type instanceof CSimpleType st) {
      size = machineModel.getSizeofInBits(st);
    } else if (type instanceof CBitFieldType) {
      size = ((CBitFieldType) type).getBitFieldSize();
      type = ((CBitFieldType) type).getType();

    } else {
      return value;
    }

    return castNumeric(numericValue, type, size);
  }

  private static Value castIfSymbolic(Value pValue, Type pTargetType) {
    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

    if (pValue instanceof SymbolicValue symbolicValue
        && (pTargetType instanceof JSimpleType || pTargetType instanceof CSimpleType)) {

      return factory.cast(symbolicValue, pTargetType);
    }

    // If the value is not symbolic, just return it.
    return pValue;
  }

  /**
   * Returns a numeric type that can be used to perform arithmetics on an instance of the type
   * directly, or null if none.
   *
   * <p>Most notably, CPointerType will be converted to the unsigned integer type of correct size.
   *
   * @param type the input type
   */
  public static @Nullable CSimpleType getArithmeticType(CType type) {
    type = type.getCanonicalType();
    if (type instanceof CPointerType) {
      return CNumericTypes.INT;
    } else if (type instanceof CSimpleType) {
      return (CSimpleType) type;
    } else {
      return null;
    }
  }

  private boolean isArithmeticOperation(BinaryOperator binaryOperator) {
    return switch (binaryOperator) {
      case PLUS,
          MINUS,
          DIVIDE,
          MODULO,
          MULTIPLY,
          SHIFT_LEFT,
          SHIFT_RIGHT,
          BINARY_AND,
          BINARY_OR,
          BINARY_XOR ->
          true;
      default -> false;
    };
  }

  private boolean isComparison(BinaryOperator binaryOperator) {
    return switch (binaryOperator) {
      case EQUALS, NOT_EQUALS, GREATER_THAN, GREATER_EQUAL, LESS_THAN, LESS_EQUAL -> true;
      default -> false;
    };
  }

  /**
   * Only accessible for subclasses.
   *
   * @return the {@link SMGState} given to this visitor when it was created.
   */
  protected SMGState getInitialVisitorState() {
    return state;
  }

  /**
   * Only accessible for subclasses.
   *
   * @return the {@link SMGOptions} given to this visitor when it was created.
   */
  protected SMGOptions getInitialVisitorOptions() {
    return options;
  }

  /**
   * Only accessible for subclasses.
   *
   * @return the {@link CFAEdge} given to this visitor when it was created.
   */
  protected CFAEdge getInitialVisitorCFAEdge() {
    return cfaEdge;
  }

  /**
   * Only accessible for subclasses.
   *
   * @return the {@link SMGCPAExpressionEvaluator} given to this visitor when it was created.
   */
  protected SMGCPAExpressionEvaluator getInitialVisitorEvaluator() {
    return evaluator;
  }

  /**
   * Only accessible for subclasses.
   *
   * @return the {@link LogManagerWithoutDuplicates} given to this visitor when it was created.
   */
  protected LogManagerWithoutDuplicates getInitialVisitorLogger() {
    return logger;
  }
}
