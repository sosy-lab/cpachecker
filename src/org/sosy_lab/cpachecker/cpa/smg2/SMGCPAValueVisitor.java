// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.BITWISE_AND;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.BITWISE_OR;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.BITWISE_XOR;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.DIVIDE;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.EQUALS;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.GREATER_EQUAL;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.GREATER_THAN;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.LESS_EQUAL;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.LESS_THAN;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.MINUS;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.MULTIPLY;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.NOT_EQUALS;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.PLUS;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.REMAINDER;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.SHIFT_LEFT;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.SHIFT_RIGHT;

import com.google.common.base.Function;
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
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.CastExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.NegationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.FunctionValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.BuiltinFloatFunctions;
import org.sosy_lab.cpachecker.util.BuiltinFunctions;
import org.sosy_lab.cpachecker.util.BuiltinOverflowFunctions;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.RoundingMode;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
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
          Value readSizeValue = evaluator.getBitSizeof(currentState, returnType, cfaEdge);
          // If this fails, hand through Value instead of BigInt
          checkState(
              readSizeValue.isNumericValue(),
              "Expected concrete value, but found symbolic value for type size");
          BigInteger readSize = readSizeValue.asNumericValue().bigIntegerValue();

          BigInteger fieldOffset =
              evaluator.getFieldOffsetInBits(
                  SMGCPAExpressionEvaluator.getCanonicalType(explicitFieldRef),
                  explicitFieldRef.getFieldName());
          Value finalReadOffset = evaluator.addBitOffsetValues(ptrTargetOffset, fieldOffset);

          if (finalReadOffset instanceof NumericValue numericFinalReadOffset
              && numericFinalReadOffset.bigIntegerValue().equals(linkedListObj.getNextOffset())) {

            ValueAndSMGState fieldReadAndState =
                currentState.readValueWithoutMaterialization(
                    linkedListObj, numericFinalReadOffset.bigIntegerValue(), readSize, returnType);
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
    return ImmutableList.of(
        ValueAndSMGState.ofUnknownValue(
            state,
            "Unknown value assumed for default handling of unhandled expression " + pExp + "  in ",
            cfaEdge));
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

      if (currentState.getMemoryModel().isPointer(arrayValue)
          && !(arrayValue instanceof NumericValue)) {
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
        if (!(subscriptValue instanceof NumericValue) && !options.trackErrorPredicates()) {
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  newState.withUnknownOffsetMemoryAccess(),
                  "Returned unknown value due to a unknown address or offset in subscript"
                      + " expression with a memory error in ",
                  cfaEdge));
          continue;
        }

        // Calculate the offset out of the subscript value and the type
        Value typeSizeInBits = evaluator.getBitSizeof(newState, returnType, cfaEdge);
        Value subscriptOffset = evaluator.multiplyBitOffsetValues(subscriptValue, typeSizeInBits);

        if (arrayExpr.getExpressionType() instanceof CPointerType) {
          checkArgument(
              arrayValue instanceof AddressExpression || arrayValue instanceof NumericValue);
        } else if (arrayExpr.getExpressionType() instanceof CCompositeType
            || arrayExpr.getExpressionType() instanceof CElaboratedType
            || arrayExpr.getExpressionType() instanceof CArrayType
            || arrayExpr.getExpressionType() instanceof CTypedefType) {
          if (arrayValue instanceof SymbolicIdentifier) {
            checkArgument(((SymbolicIdentifier) arrayValue).getRepresentedLocation().isPresent());
          } else {
            checkArgument(
                arrayValue instanceof AddressExpression || arrayValue instanceof NumericValue);
          }

        } else {
          if (arrayValue instanceof SymbolicIdentifier) {
            checkArgument(((SymbolicIdentifier) arrayValue).getRepresentedLocation().isEmpty());
          }
          checkArgument(!(arrayValue instanceof AddressExpression));
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
    Value typeSizeInBitsValue = evaluator.getBitSizeof(newState, returnType, cfaEdge);
    // If this fails, hand through Value instead of BigInt
    checkState(
        typeSizeInBitsValue.isNumericValue(),
        "Expected concrete value, but found symbolic value for type size");
    BigInteger typeSizeInBits = typeSizeInBitsValue.asNumericValue().bigIntegerValue();

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
          Value readValue = readPointerAndState.getValue();
          if (readValue.isUnknown()) {
            returnBuilder.add(
                ValueAndSMGState.ofUnknownValue(
                    newState,
                    "Returned unknown value due to an invalid address or offset in read expression"
                        + " with dereference in ",
                    cfaEdge));
          } else if (readValue instanceof NumericValue || !newState.isPointer(readValue)) {
            returnBuilder.add(ValueAndSMGState.of(readValue, newState));
          } else {
            returnBuilder.add(
                ValueAndSMGState.of(
                    AddressExpression.withZeroOffset(readValue, returnType), newState));
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

        if (!(additionalOffset instanceof NumericValue numAdditionalOffset)) {
          throw new RuntimeException(
              "Missing case in SMGCPAValueVisitor. Report to CPAchecker issue tracker for SMG2"
                  + " analysis.");
        }

        return ImmutableList.of(
            ValueAndSMGState.of(
                SymbolicValueFactory.getInstance()
                    .newIdentifier(memloc.withAddedOffset(numAdditionalOffset.longValue())),
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

          } else if (readPointerAndState.getValue() instanceof NumericValue
              || !newState.isPointer(readPointerAndState.getValue())) {
            returnBuilder.add(ValueAndSMGState.of(readPointerAndState.getValue(), newState));

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

    return handleBinaryOperation(lVarInBinaryExp, rVarInBinaryExp, e);
  }

  private List<ValueAndSMGState> handleBinaryOperation(
      CExpression lVarInBinaryExp, CExpression rVarInBinaryExp, CBinaryExpression e)
      throws CPATransferException {

    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    for (ValueAndSMGState leftValueAndState : lVarInBinaryExp.accept(this)) {
      Value leftValue = leftValueAndState.getValue();
      SMGState currentState = leftValueAndState.getState();

      for (ValueAndSMGState rightValueAndState :
          rVarInBinaryExp.accept(
              new SMGCPAValueVisitor(evaluator, currentState, cfaEdge, logger, options))) {

        currentState = rightValueAndState.getState();
        Value rightValue = rightValueAndState.getValue();
        resultBuilder.addAll(handleBinaryOperation(leftValue, rightValue, e, currentState));
      }
    }
    return resultBuilder.build();
  }

  private List<ValueAndSMGState> handleBinaryOperation(
      final Value pLeftValue, final Value pRightValue, CBinaryExpression e, SMGState currentState)
      throws CPATransferException {
    final BinaryOperator binaryOperator = e.getOperator();
    final CType calculationType = e.getCalculationType();
    final CType leftType = e.getOperand1().getExpressionType().getCanonicalType();
    final CType rightType = e.getOperand2().getExpressionType().getCanonicalType();

    ValueAndSMGState castLeftValueAndState = castCValue(pLeftValue, calculationType, currentState);
    final Value leftValueWithCorrectType = castLeftValueAndState.getValue();
    currentState = castLeftValueAndState.getState();
    final Value rightValueWithCorrectType;
    if (binaryOperator != SHIFT_LEFT && binaryOperator != SHIFT_RIGHT) {
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
      ValueAndSMGState castRightValue = castCValue(pRightValue, calculationType, currentState);
      rightValueWithCorrectType = castRightValue.getValue();
      currentState = castRightValue.getState();
    } else {
      rightValueWithCorrectType = pRightValue;
    }

    // TODO: remove this check once the SV-COMP benchmark set has been ran once (to make sure it
    // never triggers)
    checkArgument(
        !(leftValueWithCorrectType instanceof FunctionValue)
            && !(rightValueWithCorrectType instanceof FunctionValue),
        "Found FunctionValues that are not supported by this analysis");

    if (isPointerArithmetics(
        leftValueWithCorrectType, rightValueWithCorrectType, e, currentState)) {
      // Pointer arithmetics/comparisons

      // TODO: we now allow unknown to end up in the values, handle here as well!
      checkArgument(
          !(leftValueWithCorrectType instanceof AddressExpression
                  || rightValueWithCorrectType instanceof AddressExpression
                  || state.isPointer(leftValueWithCorrectType)
                  || state.isPointer(rightValueWithCorrectType))
              || !(leftValueWithCorrectType.isUnknown() || rightValueWithCorrectType.isUnknown()));
      return handlePointerArithmetics(
          leftValueWithCorrectType,
          leftType,
          rightValueWithCorrectType,
          rightType,
          e,
          currentState,
          binaryOperator,
          calculationType);
    }

    // We don't want AddressExpressions beyond this point, as they might be handled/bundled as
    // symbolicExpressions, and this is only correct if we don't use the wrapping of the
    // AddressExpressions, but full pointers
    checkArgument(
        !(leftValueWithCorrectType instanceof AddressExpression
            || rightValueWithCorrectType instanceof AddressExpression));

    // We also don't want location representations in SymbolicIdentifiers (as we use them for arrays
    // in SMG2)
    checkArgument(
        !(leftValueWithCorrectType instanceof SymbolicIdentifier leftSymIdent)
            || leftSymIdent.getRepresentedLocation().isEmpty());
    checkArgument(
        !(rightValueWithCorrectType instanceof SymbolicIdentifier rightSymIdent)
            || rightSymIdent.getRepresentedLocation().isEmpty());
    // TODO: unwrap consts to check as well?

    if (binaryOperator.isLogicalOperator()) {
      // Comparisons, i.e. ==, !=, <, >, <=, >=
      Value returnValue =
          handleComparisonOperation(
              leftValueWithCorrectType, rightValueWithCorrectType, binaryOperator, e);
      // We do not cast here, because 0 and 1 are small enough for every type.
      return ImmutableList.of(ValueAndSMGState.of(returnValue, currentState));

    } else {
      // Arithmetic and bitwise operations, i.e. +, -, *, /, %, <<, >>, |, &, ^
      Value arithResult =
          handleBinaryArithmeticOrBitwiseOperation(
              leftValueWithCorrectType, rightValueWithCorrectType, binaryOperator, e);
      return ImmutableList.of(castCValue(arithResult, e.getExpressionType(), currentState));
    }
  }

  private List<ValueAndSMGState> handlePointerArithmetics(
      Value leftValue,
      CType leftValueType,
      Value rightValue,
      CType rightValueType,
      CBinaryExpression e,
      SMGState currentState,
      BinaryOperator binaryOperator,
      CType calculationType)
      throws CPATransferException {
    // At least one value is a pointer.
    // This pointer may not be numeric (0).
    // If both are pointers, they may not be both numeric (0).
    // Non-pointers may be numeric, symbolic or unknown.

    if (leftValue.isUnknown() || rightValue.isUnknown()) {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Assume unknown return value due to at least one expression being unknown in " + e,
              cfaEdge));
    }
    // Values might still be symbolic non-pointers!

    // TODO: check the types used in the calculations! Make sure that we do use larger types when
    //  handling bits instead of bytes!

    // It is possible that addresses get cast to int or smth like it
    // Then the SymbolicIdentifier is returned not in an AddressExpression
    // They might be wrapped in a ConstantSymbolicExpression
    // We don't remove this wrapping for the rest of the analysis as they might actually get
    // treated as the cast value!
    Value nonConstRightValue = rightValue;
    if (rightValue instanceof ConstantSymbolicExpression rightConst
        && currentState.isPointer(rightConst.getValue())) {
      nonConstRightValue = rightConst.getValue();
    }
    Value nonConstLeftValue = leftValue;
    if (leftValue instanceof ConstantSymbolicExpression constLeft
        && currentState.isPointer(constLeft.getValue())) {
      nonConstLeftValue = constLeft.getValue();
    }

    ValueAndSMGState leftValueAndState =
        evaluator.unpackAddressExpression(nonConstLeftValue, currentState);
    nonConstLeftValue = leftValueAndState.getValue();
    ValueAndSMGState rightValueAndState =
        evaluator.unpackAddressExpression(nonConstRightValue, leftValueAndState.getState());
    nonConstRightValue = rightValueAndState.getValue();
    currentState = rightValueAndState.getState();
    // From this pointer forward, there is no AddressExpressions anymore

    boolean leftIsNumeric = nonConstLeftValue instanceof NumericValue;
    boolean rightIsNumeric = nonConstRightValue instanceof NumericValue;
    checkState(!(leftIsNumeric && rightIsNumeric));

    return switch (binaryOperator) {
      case EQUALS -> {
        // address == address or address == not address
        if (leftIsNumeric) {
          yield ImmutableList.of(
              ValueAndSMGState.of(
                  getAddressEqualityWithNumber(
                      nonConstRightValue,
                      rightValueType,
                      nonConstLeftValue,
                      leftValueType,
                      currentState,
                      calculationType),
                  currentState));
        } else if (rightIsNumeric) {
          yield ImmutableList.of(
              ValueAndSMGState.of(
                  getAddressEqualityWithNumber(
                      nonConstLeftValue,
                      leftValueType,
                      nonConstRightValue,
                      rightValueType,
                      currentState,
                      calculationType),
                  currentState));
        }

        // only address == address
        yield ImmutableList.of(
            ValueAndSMGState.of(
                evaluator.checkEqualityForAddresses(
                    nonConstLeftValue, nonConstRightValue, currentState),
                currentState));
      }

      case NOT_EQUALS -> {
        // address != address or address != not address
        if (leftIsNumeric || rightIsNumeric) {
          Value equalityRes;
          if (leftIsNumeric) {
            equalityRes =
                getAddressEqualityWithNumber(
                    nonConstRightValue,
                    rightValueType,
                    nonConstLeftValue,
                    leftValueType,
                    currentState,
                    calculationType);
          } else {
            equalityRes =
                getAddressEqualityWithNumber(
                    nonConstLeftValue,
                    leftValueType,
                    nonConstRightValue,
                    rightValueType,
                    currentState,
                    calculationType);
          }
          Value inequalityRes = equalityRes;
          if (equalityRes instanceof NumericValue numEqualityRes) {
            if (numEqualityRes.bigIntegerValue().equals(BigInteger.ZERO)) {
              inequalityRes = new NumericValue(BigInteger.ONE);
            } else {
              checkState(numEqualityRes.bigIntegerValue().equals(BigInteger.ONE));
              inequalityRes = new NumericValue(BigInteger.ZERO);
            }
          } else {
            checkState(equalityRes.isUnknown());
          }
          yield ImmutableList.of(ValueAndSMGState.of(inequalityRes, currentState));
        }

        yield ImmutableList.of(
            ValueAndSMGState.of(
                evaluator.checkNonEqualityForAddresses(
                    nonConstLeftValue, nonConstRightValue, currentState),
                currentState));
      }

      case PLUS, MINUS ->
          // TODO: make sure this handled normal pointers only (no addressExpr)!
          // Only arithmetic operators are allowed here
          calculatePointerArithmetics(
              nonConstLeftValue,
              nonConstRightValue,
              binaryOperator,
              e.getExpressionType(),
              calculationType,
              SMGCPAExpressionEvaluator.getCanonicalType(e.getOperand1().getExpressionType()),
              SMGCPAExpressionEvaluator.getCanonicalType(e.getOperand2().getExpressionType()),
              currentState,
              e);

      case GREATER_EQUAL, LESS_EQUAL, GREATER_THAN, LESS_THAN -> {
        // TODO: this is wrong for abstracted targets! Fix!
        if (!currentState.pointsToSameMemoryRegion(nonConstLeftValue, nonConstRightValue)) {
          // This is undefined behavior in C99/C11
          // But since we don't really handle this we just return unknown :D
          yield ImmutableList.of(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown value due to address values in binary pointer comparison"
                      + " expression not pointing to the same target memory in ",
                  cfaEdge));
        }

        // Then get the offsets
        Value offsetLeft = currentState.getPointerOffset(nonConstLeftValue);
        Value offsetRight = currentState.getPointerOffset(nonConstRightValue);
        if (offsetLeft.isUnknown() || offsetRight.isUnknown()) {
          yield ImmutableList.of(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown value due to unknown offset value(s) in binary pointer"
                      + " comparison expression in ",
                  cfaEdge));
        }

        // Create binary expr with offsets and restart this with it
        yield handleBinaryOperation(offsetLeft, offsetRight, e, currentState);
      }

      // Can never happen
      default ->
          throw new IllegalStateException(
              "Unexpected binary operator " + binaryOperator + " for pointer arithmetics");
    };
  }

  /**
   * Calculates the equality (==) between an address and a non-address (symbolic of numeric number).
   * Returns either numeric true/false, unknown or throws (instead of unknown, depending on
   * options). This method guarantees that if true or false is returned, the negation also holds
   * (i.e. that !(ptr == 123) == ptr != 123)
   */
  private Value getAddressEqualityWithNumber(
      Value address,
      CType addressType,
      Value nonAddressValue,
      CType nonAddressValueType,
      SMGState pCurrentState,
      CType calculationType)
      throws SMGException, SMGSolverException {
    checkArgument(pCurrentState.isPointer(address));

    // TODO: the types will be useful later to determine in/equality w type info
    checkNotNull(addressType);
    checkNotNull(nonAddressValueType);

    if (pCurrentState.isPointer(nonAddressValue)) {
      return evaluator.checkEqualityForAddresses(address, nonAddressValue, pCurrentState);
    }

    if (nonAddressValue instanceof NumericValue numericValue
        && numericValue.bigIntegerValue().equals(BigInteger.ZERO)) {
      // We generally expect that not both are numeric and the address is non-zero (as that would
      // mean they are actually 2 numbers)
      checkArgument(!address.isNumericValue());
      checkArgument(
          pCurrentState.getMemoryModel().getSMGValueFromValue(address).isEmpty()
              || !pCurrentState
                  .getMemoryModel()
                  .getSMGValueFromValue(address)
                  .orElseThrow()
                  .isZero());
      // Known to be false
      return new NumericValue(0);
    }

    // Suppose you have a memory region with size 4 bytes. It can not be allocated at 0,
    // but possibly at 1, hence [1, 5). If we compare the initial pointer to this region with
    // the number 1 or 100, we find that they can be equal, but does not have to.
    // However, suppose we increment the pointer to the memory region by 1, we now know that
    // comparing the pointer for equality with the number 1 can never yield true.

    // Types are relevant, as truly negative values in the numeric can never be equal.
    // Also, if the non-address value (and the used calculation type allows it) is larger than the
    // possible address-range, they can never be equal.
    if (!(calculationType.getCanonicalType() instanceof CSimpleType simpleCalcType)
        || machineModel.isSigned(simpleCalcType)
        || machineModel.getSizeof(simpleCalcType) > machineModel.getSizeofPtr()) {
      // TODO: handle
      // TODO: most cases are trivial and may just be handed to below
    }

    // Simple check whether the number of bytes fits in between zero and the offset of the address
    // to its origin
    Optional<SMGStateAndOptionalSMGObjectAndOffset> deref =
        pCurrentState.dereferencePointerWithoutMaterilization(address);
    checkState(deref.isPresent());
    checkState(deref.orElseThrow().hasSMGObjectAndOffset());
    // SMGObject obj = deref.orElseThrow().getSMGObject();
    Value offsetToObjectSourceInBits = deref.orElseThrow().getOffsetForObject();

    // We already know that nonAddressValue is either numeric and > 0 or symbolic (no restrictions)
    if (offsetToObjectSourceInBits instanceof NumericValue numericOffsetToObjectSourceInBits
        && nonAddressValue instanceof NumericValue numericValue) {
      // Note: numericValue is in Bytes here!
      BigInteger numericOffsetToObjectSourceInBytes =
          numericOffsetToObjectSourceInBits
              .bigIntegerValue()
              .divide(BigInteger.valueOf(machineModel.getSizeofCharInBits()));
      if (numericValue
              .bigIntegerValue()
              .compareTo(numericOffsetToObjectSourceInBytes.add(BigInteger.ONE))
          < 0) {
        // Guaranteed to be not-equal
        return new NumericValue(0);
      }
      // Fallthrough for Unknown (can be equal or inequal)

    } else if (options.trackPredicates()) {
      // TODO: handle
      throw new SMGException(
          "Complex relations between addresses and their locations are currently not supported");
    }

    // Unknown (can be equal or inequal)
    return handleOverapproximatedMemoryAddressRelation(pCurrentState);
  }

  /**
   * Complex cases of pointer relations (e.g. pointer equals some number: ptr1 == 1234 && ptr2 ==
   * 1234 so that ptr1 != ptr2) may need to remember relation to found values and to other
   * addresses, or simple cases need to be overapproximated (e.g. ptr == 1234, with results true and
   * false possible). This method either returns UNKNOWN or throws, depending on options.
   */
  private UnknownValue handleOverapproximatedMemoryAddressRelation(SMGState currentStateForLogging)
      throws SMGException {
    if (options.overapproximateMemoryAddressRelations()) {
      // TODO: return a list with 1 and 0? This is guaranteed boolean.
      currentStateForLogging.logUnknownValue(
          "Returned unknown value for result of complex pointer relation that could not be handled"
              + " ",
          cfaEdge);
      return UnknownValue.getInstance();
    } else {
      throw new SMGException(
          "Complex relations between addresses and their locations are currently not supported");
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
        // TODO: warn on non 0 numeric pointers
        return ValueAndSMGState.of(value, currentState);

      } else if (evaluator.isPointerValue(value, currentState)) {
        checkArgument(
            !(value instanceof ConstantSymbolicExpression constValue)
                || !(constValue.getValue() instanceof NumericValue));
        return ValueAndSMGState.of(
            AddressExpression.withZeroOffset(value, targetType), currentState);
      } else if (value instanceof ConstantSymbolicExpression constValue
          && currentState.isPointer(constValue.getValue())) {
        checkArgument(!(constValue.getValue() instanceof NumericValue));
        return ValueAndSMGState.of(
            AddressExpression.withZeroOffset(constValue.getValue(), targetType), currentState);

      } else if (value instanceof SymbolicValue) {
        return ValueAndSMGState.of(castSymbolicValue(value, targetType), currentState);

      } else {
        return ValueAndSMGState.ofUnknownValue(
            currentState,
            "Overapproximated cast to type "
                + targetType
                + " and returned unknown value due to unhandled case ",
            cfaEdge);
      }
    }

    if (!value.isExplicitlyKnown()) {
      // TODO: Unknowns end up here, NOT below!
      return ValueAndSMGState.of(castSymbolicValue(value, targetType), currentState);
    }

    // We only use numeric/symbolic/unknown values anyway, and we can't cast unknowns
    if (!(value instanceof NumericValue numericValue)) {
      logger.logf(
          Level.FINE, "Can not cast C value %s to %s", value.toString(), targetType.toString());
      return ValueAndSMGState.of(value, currentState);
    }

    CType type = targetType.getCanonicalType();
    final int size;
    if (type instanceof CSimpleType) {
      size =
          evaluator
              .getBitSizeof(currentState, type, cfaEdge)
              .asNumericValue()
              .bigIntegerValue()
              .intValueExact();
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
        checkArgument(structValue instanceof AddressExpression);
      } else if (ownerExpression.getExpressionType() instanceof CCompositeType
          || ownerExpression.getExpressionType() instanceof CElaboratedType
          || ownerExpression.getExpressionType() instanceof CArrayType
          || ownerExpression.getExpressionType() instanceof CTypedefType) {
        if (structValue instanceof SymbolicIdentifier symbolicIdentifier) {
          checkArgument(symbolicIdentifier.getRepresentedLocation().isPresent());
        } else {
          checkArgument(structValue instanceof AddressExpression);
        }
      } else {
        checkArgument(!(structValue instanceof SymbolicIdentifier));
        checkArgument(!(structValue instanceof AddressExpression));
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
            evaluator.handleVariableDeclarationWithoutInitializer(
                state, cVariableDeclaration, cfaEdge));
      } else if (varDecl instanceof CParameterDeclaration cParameterDeclaration) {
        creationBuilder.addAll(
            evaluator.handleVariableDeclarationWithoutInitializer(
                state, cParameterDeclaration.asVariableDeclaration(), cfaEdge));
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
            evaluator.createAddressForLocalOrGlobalVariable(
                variableName, returnType, currentState));

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
        // Pointer/Array/Function types should return a Value that internally can be translated
        // into a SMGValue that leads to an SMGPointsToEdge that leads to the correct object
        // (with potential offsets inside the points to edge).
        // These have to be packaged into an AddressExpression with a 0 offset.
        // Modifications of the offset of the address can be done by subsequent methods.
        // (The check is fine because we already filtered out structs/unions)
        Value sizeInBitsValue =
            evaluator.getBitSizeof(currentState, e.getExpressionType(), cfaEdge);
        BigInteger sizeInBits = sizeInBitsValue.asNumericValue().bigIntegerValue();
        // If this fails, hand through Value instead of BigInt
        checkState(
            sizeInBitsValue.isNumericValue(),
            "Expected concrete value, but found symbolic value for type size");
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
          if (evaluator.isPointerValue(readValue, newState)
              && !(readValue instanceof NumericValue)) {
            addressValue = AddressExpression.withZeroOffset(readValue, returnType);
          } else {
            // Not a known pointer value, most likely an unknown value as symbolic identifier
            addressValue = readValue;
          }

          finalStatesBuilder.add(ValueAndSMGState.of(addressValue, newState));
        }

      } else {
        // Everything else should be readable and returnable directly; just return the Value
        Value sizeInBitsValue =
            evaluator.getBitSizeof(currentState, e.getExpressionType(), cfaEdge);
        // If this fails hand through Value
        checkState(sizeInBitsValue.isNumericValue());
        BigInteger sizeInBits = sizeInBitsValue.asNumericValue().bigIntegerValue();
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
    return ImmutableList.of(
        ValueAndSMGState.ofUnknownValue(
            state,
            "Analysis overapproximated string literal expression " + e + " to unknown in ",
            cfaEdge));
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
      case SIZEOF ->
          ImmutableList.of(
              ValueAndSMGState.of(evaluator.getBitSizeof(state, innerType, cfaEdge), state));

      case ALIGNOF -> {
        BigInteger align = evaluator.getAlignOf(innerType);
        yield ImmutableList.of(ValueAndSMGState.of(new NumericValue(align), state));
      }
      case TYPEOF -> // This can't really be solved here as we can only return Values
          ImmutableList.of(
              ValueAndSMGState.ofUnknownValue(
                  state,
                  "Returned unknown value due to missing handling of typeof expression "
                      + e
                      + " in ",
                  cfaEdge));
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
        Value sizeInBitsValue = evaluator.getBitSizeof(state, operandType, cfaEdge);
        // If this fails hand through Value
        checkState(sizeInBitsValue.isNumericValue());
        BigInteger sizeInBits = sizeInBitsValue.asNumericValue().bigIntegerValue();
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
        // Address operator
        // Note: this returns AddressExpressions so that we can easily reuse the offsets etc.!
        // Unwrap before saving!
        return evaluator.createAddress(unaryOperand, state, cfaEdge);
      }
      default -> {}
    }

    ImmutableList.Builder<ValueAndSMGState> builder = new ImmutableList.Builder<>();
    for (ValueAndSMGState valueAndState : unaryOperand.accept(this)) {
      SMGState currentState = valueAndState.getState();
      Value value = valueAndState.getValue();

      ValueAndSMGState newValueAndState;
      if (value instanceof SymbolicValue) {
        newValueAndState =
            ValueAndSMGState.of(
                createBinarySymbolicExpression(value, operandType, unaryOperator, returnType),
                currentState);
      } else if (!(value instanceof NumericValue numericValue)) {
        newValueAndState =
            ValueAndSMGState.ofUnknownValue(
                currentState,
                "Returned unknown due to non-numeric argument for unary expression " + e + " in ",
                cfaEdge);
      } else {

        final NumericValue newValue =
            switch (unaryOperator) {
              case MINUS -> numericValue.negate();
              case TILDE -> new NumericValue(~numericValue.longValue());
              default -> throw new AssertionError("Unknown unary operator: " + unaryOperator);
            };
        newValueAndState = ValueAndSMGState.of(newValue, currentState);
      }
      builder.add(newValueAndState);
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
                currentState.withUnknownPointerDereferenceWhenReading(value, cfaEdge),
                "Returned unknown value for pointer dereference of unknown value in ",
                cfaEdge));
        continue;
      }

      if (!(value instanceof AddressExpression)
          && evaluator.isPointerValue(value, currentState)
          && !(value instanceof NumericValue)) {
        // For pointer deref on arrays only
        value =
            AddressExpression.of(value, e.getExpressionType(), new NumericValue(BigInteger.ZERO));
      }

      if (!(value instanceof AddressExpression pointerValue)) {
        // Non-pointer dereference, either numeric or symbolic
        checkArgument(
            (value instanceof NumericValue numValue
                    && numValue.bigIntegerValue().equals(BigInteger.ZERO))
                || !evaluator.isPointerValue(value, currentState));
        builder.add(
            ValueAndSMGState.ofUnknownValue(
                currentState.withUnknownPointerDereferenceWhenReading(value, cfaEdge),
                "Returned unknown value due to non-pointer dereference with invalid-deref in"
                    + " expression in ",
                cfaEdge));
        continue;
      }

      // The offset part of the pointer; its either numeric or we can't get a concrete value
      Value offset = pointerValue.getOffset();
      if (!(offset instanceof NumericValue) && !options.trackErrorPredicates()) {
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

      Value sizeInBitsValue = evaluator.getBitSizeof(currentState, returnType, cfaEdge);
      // If this fails hand through Value
      checkState(sizeInBitsValue.isNumericValue());
      BigInteger sizeInBits = sizeInBitsValue.asNumericValue().bigIntegerValue();

      if (SMGCPAExpressionEvaluator.isStructOrUnionType(returnType)) {
        if (!(offset instanceof NumericValue)) {
          // If the offset is not numerically known we can't read a value
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
        checkArgument(
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
                .getFirst();
        currentState = readValueAndState.getState();

        if (returnType instanceof CPointerType
            && currentState.isPointer(readValueAndState.getValue())
            && !(readValueAndState.getValue() instanceof NumericValue)) {
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
  private Value createBinarySymbolicExpression(
      Value pValue, CType pOperandType, UnaryOperator pUnaryOperator, CType pExpressionType) {
    SymbolicExpression operand = ConstantSymbolicExpression.of(pValue, pOperandType);

    return switch (pUnaryOperator) {
      case MINUS -> NegationExpression.of(operand, pExpressionType);
      case TILDE -> BinaryNotExpression.of(operand, pExpressionType);
      default -> throw new AssertionError("Unhandled unary operator " + pUnaryOperator);
    };
  }

  // ++++++++++++++++++++ Below this point casting helper methods

  /** Taken from the value analysis CPA and modified. Casts symbolic {@link Value}s. */
  private Value castSymbolicValue(Value pValue, Type pTargetType) {

    if (pValue instanceof SymbolicValue symbolicValue && pTargetType instanceof CSimpleType) {
      return CastExpression.of(symbolicValue, pTargetType);
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
            state.logUnknownValue(
                "Returned unknown value due to undefined conversion of float value "
                    + numericValue.getFloatValue()
                    + " to integer value ",
                cfaEdge);
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
            state.logUnknownValue(
                "Returned unknown value due to undefined conversion of float value "
                    + numericValue.getFloatValue()
                    + " to integer value ",
                cfaEdge);
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
        Format target;

        // Find the target format
        final int bitPerByte = machineModel.getSizeofCharInBits();
        if (size == machineModel.getSizeofFloat() * bitPerByte) {
          target = Format.Float32;
        } else if (size == machineModel.getSizeofDouble() * bitPerByte) {
          target = Format.Float64;
        } else if (size == machineModel.getSizeofLongDouble() * bitPerByte) {
          // Must be Linux32 or Linux64, otherwise the second clause would have matched
          target = Format.Float80;
        } else if (size == machineModel.getSizeofFloat128() * bitPerByte) {
          target = Format.Float128;
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

  /**
   * Cast the argument to a floating point type. If it already is, will modify the precision to the
   * correct one if necessary. If the input type is already correct, this will do nothing.
   */
  private static FloatValue castToFloat(
      MachineModel pMachineModel, CSimpleType pTargetType, NumericValue pValue) {
    checkArgument(pTargetType.getType().isFloatingPointType());
    Format precision = Format.fromCType(pMachineModel, pTargetType);
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
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue value =
          castToFloat(
              machineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pName),
              numParameter);

      return ImmutableList.of(ValueAndSMGState.of(pOperation.apply(value), pState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              pState,
              "Returned unknown value due to unhandled builtin function " + pName + " in ",
              cfaEdge));
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
    Value parameter1 = pArguments.getFirst();
    Value parameter2 = pArguments.get(1);

    if (parameter1.isExplicitlyKnown() && parameter2.isExplicitlyKnown()) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pName);
      FloatValue value1 = castToFloat(machineModel, targetType, (NumericValue) parameter1);
      FloatValue value2 = castToFloat(machineModel, targetType, (NumericValue) parameter2);

      return ImmutableList.of(ValueAndSMGState.of(pOperation.apply(value1, value2), pState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              pState,
              "Returned unknown value due to unhandled builtin function " + pName + " in ",
              cfaEdge));
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
          checkArgument(newValuesAndStates.size() == 1);
          Value newValue = newValuesAndStates.getFirst().getValue();
          // CPA access has side effects! Always take the newest state!
          currentState = newValuesAndStates.getFirst().getState();

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
          return ImmutableList.of(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown result of unhandled builtin function "
                      + calledFunctionName
                      + " ",
                  cfaEdge));

        } else if (BuiltinFloatFunctions.matchesAbsolute(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> new NumericValue(arg.abs()));

        } else if (BuiltinFloatFunctions.matchesHugeVal(calledFunctionName)
            || BuiltinFloatFunctions.matchesInfinity(calledFunctionName)) {
          checkArgument(parameterValues.isEmpty());
          Format precision =
              Format.fromCType(
                  machineModel,
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName));
          return ImmutableList.of(
              ValueAndSMGState.of(new NumericValue(FloatValue.infinity(precision)), currentState));

        } else if (BuiltinFloatFunctions.matchesNaN(calledFunctionName)) {
          // FIXME: Add support for NaN payloads
          checkArgument(parameterValues.size() < 2);
          Format precision =
              Format.fromCType(
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
              (FloatValue arg) -> new NumericValue(arg.round(RoundingMode.FLOOR)));

        } else if (BuiltinFloatFunctions.matchesCeil(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> new NumericValue(arg.round(RoundingMode.CEILING)));

        } else if (BuiltinFloatFunctions.matchesRound(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> new NumericValue(arg.round(RoundingMode.NEAREST_AWAY)));

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
                  default -> {
                    state.logUnknownValue(
                        "Returned unknown value due to undefined conversion of float value "
                            + value
                            + " in builtin function "
                            + calledFunctionName
                            + " ",
                        cfaEdge);
                    yield UnknownValue.getInstance();
                  }
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
                  default -> {
                    state.logUnknownValue(
                        "Returned unknown value due to undefined conversion of float value "
                            + value
                            + " in builtin function "
                            + calledFunctionName
                            + " ",
                        cfaEdge);
                    yield UnknownValue.getInstance();
                  }
                };
              });

        } else if (BuiltinFloatFunctions.matchesTrunc(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              currentState,
              (FloatValue arg) -> new NumericValue(arg.round(RoundingMode.TRUNCATE)));

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
            Value value = parameterValues.getFirst();
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
      return smgBuiltins.handleFunctionCallWithoutBody(
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

  /**
   * Calculates pointer/address arithmetic expressions. Valid is only address + value or value +
   * address and address minus value or address minus address. All others are simply unknown value!
   * One of the 2 entered values must be a pointer and none may be an {@link AddressExpression}, no
   * other preconditions have to be met.
   *
   * @param leftValue left hand side value of the pointer arithmetic operation.
   * @param rightValue right hand side value of the pointer arithmetic operation.
   * @param binaryOperator {@link BinaryOperator} in between the values.
   * @param expressionType {@link CType} of the final expression.
   * @param calculationType {@link CType} of the calculation. (Should be int for pointers)
   * @param currentState current {@link SMGState}
   * @param originalExpression {@link CBinaryExpression} that caused this evaluation. Needed to
   *     retrieve type information in certain circumstances from cast pointers.
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
      SMGState currentState,
      CBinaryExpression originalExpression)
      throws CPATransferException {
    // Find the address, check that the other is a numeric value and use as offset, else if both
    // are addresses we allow the distance, else unknown (we can't dereference symbolics)
    checkState(binaryOperator == PLUS || binaryOperator == MINUS);
    boolean leftIsPointer = currentState.isPointer(leftValue);
    boolean rightIsPointer = currentState.isPointer(rightValue);
    if (!leftIsPointer && !rightIsPointer) {
      // We allow only 0 for now here, see if someone does something like: 0 + 3
      checkArgument(
          rightValue instanceof NumericValue numRightValue
              && leftValue instanceof NumericValue
              && numRightValue.bigIntegerValue().equals(BigInteger.ZERO));
      return ImmutableList.of(ValueAndSMGState.of(new NumericValue(0), currentState));
    }
    // At least one is a pointer from this point onward

    // The canonical type is the return type of the pointer expression!
    CType canonicalReturnType = expressionType.getCanonicalType();
    if (calculationType instanceof CPointerType) {
      canonicalReturnType = ((CPointerType) expressionType).getType();
    }

    boolean leftIsZero =
        leftValue instanceof NumericValue numLeftValue
            && numLeftValue.bigIntegerValue().equals(BigInteger.ZERO);
    boolean rightIsZero =
        rightValue instanceof NumericValue numRightValue
            && numRightValue.bigIntegerValue().equals(BigInteger.ZERO);
    checkArgument(
        !(leftValue instanceof ConstantSymbolicExpression leftConst)
            || !(leftConst.getValue() instanceof NumericValue));
    checkArgument(
        !(rightValue instanceof ConstantSymbolicExpression rightConst)
            || !(rightConst.getValue() instanceof NumericValue));

    if (rightIsZero) {
      return ImmutableList.of(ValueAndSMGState.of(leftValue, currentState));
    } else if (leftIsZero) {
      return ImmutableList.of(ValueAndSMGState.of(rightValue, currentState));
    } else if (leftValue == rightValue
        && binaryOperator == MINUS
        && (leftValueType instanceof CSimpleType simpleLeftType
            && rightValueType instanceof CSimpleType simpleRightType
            && calculationType instanceof CSimpleType simpleCalcType
            && machineModel.getSizeofInBits(simpleLeftType)
                >= machineModel.getSizeofInBits(machineModel.getPointerSizedIntType())
            && machineModel.getSizeofInBits(simpleRightType)
                >= machineModel.getSizeofInBits(machineModel.getPointerSizedIntType())
            && machineModel.getSizeofInBits(simpleCalcType)
                >= machineModel.getSizeofInBits(machineModel.getPointerSizedIntType()))) {
      // Types are not allowed to restrict the address (i.e. be smaller than the address)
      // x - x == 0
      return ImmutableList.of(ValueAndSMGState.of(new NumericValue(0), currentState));
    }

    if (leftIsPointer && !rightIsPointer) {

      // e.g. pointer + 3
      return calculatePointerArithmeticsOperationWithPointerAndNonPointer(
          leftValue,
          leftValueType,
          rightValue,
          rightValueType,
          binaryOperator,
          calculationType,
          currentState,
          originalExpression,
          canonicalReturnType);

    } else if (!leftIsPointer) {

      // e.g. 3 + pointer
      // Just the reverse of above (with the caveat that 3 - pointer does not make sense ;D)
      checkArgument(binaryOperator != MINUS);
      return calculatePointerArithmeticsOperationWithPointerAndNonPointer(
          rightValue,
          rightValueType,
          leftValue,
          leftValueType,
          binaryOperator,
          calculationType,
          currentState,
          originalExpression,
          canonicalReturnType);

    } else {

      // Both are pointers, we allow minus here to get the distance between them
      checkArgument(currentState.isPointer(rightValue));
      checkArgument(currentState.isPointer(leftValue));
      checkArgument(binaryOperator == MINUS);

      ImmutableList.Builder<ValueAndSMGState> returnBuilder = ImmutableList.builder();
      // Our offsets are in bits here! This also checks that it's the same underlying memory object.
      for (ValueAndSMGState distanceInBitsAndState :
          evaluator.calculateAddressDistance(currentState, leftValue, rightValue)) {

        Value distanceInBits = distanceInBitsAndState.getValue();
        currentState = distanceInBitsAndState.getState();
        if (!(distanceInBits instanceof NumericValue numDistanceInBits)) {
          returnBuilder.add(ValueAndSMGState.of(distanceInBits, currentState));
          continue;
        }

        // For pointer types (distance is in objects for pointers):
        // distance in bits / object type size in bits = distance
        // The type in both pointers should be the same, we need the return type from one of them.
        // For integer types (distance is in bytes for numeric addresses):
        // distance in bits / byte size in bits = distance
        CType objectType;
        // TODO: we need to make sure that at no point a type is used (e.g. casts) that lose part of
        // the address (e.g. (int *) (char) pointer).
        if (leftValueType instanceof CPointerType cPointerType) {
          objectType = cPointerType.getType();
        } else if (leftValueType.getCanonicalType() instanceof CArrayType arrayType) {
          objectType = arrayType.getType();
        } else if (leftValueType instanceof CSimpleType simpleType
            && simpleType.getType().isIntegerType()
            && machineModel.getSizeofInBits(simpleType)
                >= machineModel.getSizeofInBits(machineModel.getPointerSizedIntType())) {
          // Char is always byte sized
          objectType = CNumericTypes.CHAR;
        } else {
          returnBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown value due to type error in pointer arithmetics expression in ",
                  cfaEdge));
          continue;
        }

        Value objectTypeBitSize = new NumericValue(machineModel.getSizeofInBits(objectType));

        // Undefined behavior if this assertion does not hold for every pointer/address related type
        // Casts to numbers can have variations, but are not checked
        assert leftValueType.equals(rightValueType);
        Value distance =
            handleBinaryArithmeticOrBitwiseOperation(
                numDistanceInBits,
                leftValueType,
                objectTypeBitSize,
                rightValueType,
                DIVIDE,
                machineModel.getPointerSizedIntType(),
                machineModel.getPointerSizedIntType(),
                originalExpression);

        returnBuilder.add(ValueAndSMGState.of(distance, currentState));
      }
      return returnBuilder.build();
    }
  }

  /**
   * Calculates a pointer arithmetics expression of ONLY the form "pointer +- non-pointer"!
   *
   * @param leftValue some pointer.
   * @param rightValue some non-pointer. Either a {@link NumericValue}, {@link UnknownValue}, or
   *     variable in the form of a {@link SymbolicValue}.
   */
  private ImmutableList<ValueAndSMGState>
      calculatePointerArithmeticsOperationWithPointerAndNonPointer(
          Value leftValue,
          CType leftValueType,
          Value rightValue,
          CType rightValueType,
          BinaryOperator binaryOperator,
          CType calculationType,
          SMGState currentState,
          CBinaryExpression originalExpressionForErrorMessages,
          CType canonicalReturnType)
          throws CPATransferException {

    if (leftValue.isUnknown() || rightValue.isUnknown()) {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unknown input value in pointer"
                  + " arithmetics expression in ",
              cfaEdge));
    }
    checkArgument(currentState.isPointer(leftValue));
    checkArgument(!currentState.isPointer(rightValue)); // 0 would be allowed, add if this happens

    if (!options.trackPredicates() && !(rightValue instanceof NumericValue)) {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to symbolic or unknown offset value in pointer"
                  + " arithmetics expression without predicate tracking in ",
              cfaEdge));
    }

    Value bitSize;
    if (calculationType instanceof CPointerType) {
      // This is the pointer++; case for example.
      // We need the correct types here; the types of the returned value after the pointer
      // expression!
      // TODO: is the type correct?
      bitSize = evaluator.getBitSizeof(currentState, canonicalReturnType, cfaEdge);
    } else {
      // If it's a cast pointer, i.e. ((unsigned int) pointer) + 8;
      // then this is just the numeric value * char size (i.e. char size is always 1 byte, but 1
      // byte is not always 8 bits) and then the operation.
      bitSize = new NumericValue(machineModel.getSizeofCharInBits());
    }
    // Transform the numeric byte value into bits, as we calculate with bits internally
    // (e.g. a "pointer + 3" becomes "pointer + (3*8)" for an int pointer)
    Value additionalOffsetInBits =
        calculateArithmeticOperationWithBitPromotionForAddresses(
            bitSize,
            leftValueType,
            rightValue,
            rightValueType,
            MULTIPLY,
            originalExpressionForErrorMessages);

    // We don't really dereference here, we only want the offset and target, so we don't break
    // abstraction here!
    Optional<SMGStateAndOptionalSMGObjectAndOffset> dereferencedLeftValue =
        currentState.dereferencePointerWithoutMaterilization(leftValue);
    SMGObject pointerTarget = dereferencedLeftValue.orElseThrow().getSMGObject();
    Value pointerOffsetInBits = dereferencedLeftValue.orElseThrow().getOffsetForObject();

    if (!options.trackPredicates() && !(pointerOffsetInBits instanceof NumericValue)) {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to symbolic or unknown offset value in pointer"
                  + " arithmetics expression without predicate tracking in ",
              cfaEdge));
    }

    Value newOffsetInBits =
        calculateArithmeticOperationWithBitPromotionForAddresses(
            pointerOffsetInBits,
            leftValueType,
            additionalOffsetInBits,
            rightValueType,
            binaryOperator,
            originalExpressionForErrorMessages);

    if (newOffsetInBits instanceof SymbolicExpression newSymbolicOffsetInBits) {
      int newOffsetTypeBits =
          evaluator
              .getMachineModel()
              .getSizeofInBits((CType) newSymbolicOffsetInBits.getType())
              .intValueExact();
      int pointerTypeInBits =
          evaluator.getMachineModel().getSizeofInBits(CPointerType.POINTER_TO_CHAR).intValueExact();
      // Make sure that the bit type can truly fit the byte * 8 sizes!
      checkArgument(newOffsetTypeBits >= (pointerTypeInBits + 3));
    }

    return ImmutableList.of(
        evaluator.searchOrCreatePointer(pointerTarget, leftValueType, newOffsetInBits, state));
  }

  /**
   * Used for binary arithmetic operations that need artificial extension of their types width
   * during calculation due to memory addresses being handled.
   */
  private Value calculateArithmeticOperationWithBitPromotionForAddresses(
      Value leftValue,
      CType leftValueType,
      Value rightValue,
      CType rightValueType,
      BinaryOperator binOp,
      CBinaryExpression originalExpressionForErrorMessages)
      throws UnsupportedCodeException {

    CType promotedType = evaluator.getCTypeForBitPreciseMemoryAddresses();
    return handleBinaryArithmeticOrBitwiseOperation(
        leftValue,
        leftValueType,
        rightValue,
        rightValueType,
        binOp,
        promotedType,
        promotedType,
        originalExpressionForErrorMessages);
  }

  /**
   * Join a symbolic expression with something else using a binary expression. One of the values
   * needs to be symbolic. None may be unknown.
   *
   * <p>e.g. joining `a` and `5` with operator `+` will produce `a + 5`.
   */
  public Value calculateSymbolicBinaryExpression(
      Value pLValue, Value pRValue, final CBinaryExpression pExpression) {

    final BinaryOperator operator = pExpression.getOperator();

    final CType leftOperandType = pExpression.getOperand1().getExpressionType();
    final CType rightOperandType = pExpression.getOperand2().getExpressionType();
    final CType expressionType = pExpression.getExpressionType();
    final CType calculationType = pExpression.getCalculationType();

    return createBinarySymbolicExpression(
        pLValue,
        leftOperandType,
        pRValue,
        rightOperandType,
        operator,
        expressionType,
        calculationType);
  }

  /**
   * Transforms both input values into constant symbolic expressions if needed, and builds the
   * binary symbolic expression. None of the inputs is allowed to be unknown. One (left or right)
   * may be numeric, but not both!
   */
  private SymbolicExpression createBinarySymbolicExpression(
      Value pLeftValue,
      CType pLeftType,
      Value pRightValue,
      CType pRightType,
      BinaryOperator pOperator,
      CType pExpressionType,
      CType pCalculationType) {

    // TODO: do we want to try to canonize symbolic expressions to allow things like (1 + a) == (a
    //  + 1) to be recognized? (this is more of a general question)
    // TODO: add automatic NaN behavior to numeric/symbolic values!
    // TODO: move to handling of common simplifications to the expressions directly?

    checkArgument(!(pLeftValue instanceof NumericValue && pRightValue instanceof NumericValue));
    checkArgument(!pLeftValue.isUnknown() || !pRightValue.isUnknown());

    SymbolicExpression leftOperand = ConstantSymbolicExpression.of(pLeftValue, pLeftType);
    SymbolicExpression rightOperand = ConstantSymbolicExpression.of(pRightValue, pRightType);

    return BinarySymbolicExpression.of(
        leftOperand, rightOperand, pExpressionType, pCalculationType, pOperator);
  }

  /**
   * Calculates binary arithmetic or bitwise operations (i.e. +, -, *, /, %, <<, >>, &, |, ^) of two
   * arbitrary {@link Value}s. Types are derived from the given expression.
   */
  private Value handleBinaryArithmeticOrBitwiseOperation(
      final Value leftValue,
      final Value rightValue,
      final BinaryOperator op,
      final CBinaryExpression expression)
      throws UnsupportedCodeException {

    final CType leftType = expression.getOperand1().getExpressionType();
    final CType rightType = expression.getOperand2().getExpressionType();
    final CType returnType = expression.getExpressionType();
    final CType calculationType = expression.getCalculationType();

    return handleBinaryArithmeticOrBitwiseOperation(
        leftValue, leftType, rightValue, rightType, op, returnType, calculationType, expression);
  }

  /**
   * Calculates binary arithmetic or bitwise operations (i.e. +, -, *, /, %, <<, >>, &, |, ^) of two
   * arbitrary {@link Value}s.
   */
  private Value handleBinaryArithmeticOrBitwiseOperation(
      final Value leftValue,
      final CType leftType,
      final Value rightValue,
      final CType rightType,
      final BinaryOperator op,
      final CType returnType,
      final CType calculationType,
      CBinaryExpression originalExpressionForErrorMessages)
      throws UnsupportedCodeException {
    assert !op.isLogicalOperator();

    // At this point we're only handling values of simple types
    final Optional<CSimpleType> maybeSimpleCalculationType = getSimplifiedType(calculationType);
    if (maybeSimpleCalculationType.isEmpty()) {
      throw new UnsupportedCodeException(
          "Unsupported calculation type "
              + calculationType
              + " in binary comparison operation "
              + originalExpressionForErrorMessages,
          cfaEdge);
    }
    CSimpleType simpleCalculationType = maybeSimpleCalculationType.orElseThrow();

    return switch (op) {
      case PLUS ->
          handleAddition(
              leftValue, leftType, rightValue, rightType, simpleCalculationType, returnType);
      case MINUS ->
          handleSubtraction(
              leftValue, leftType, rightValue, rightType, simpleCalculationType, returnType);
      case MULTIPLY ->
          handleMultiplication(
              leftValue, leftType, rightValue, rightType, simpleCalculationType, returnType);
      case DIVIDE ->
          handleDivision(
              leftValue,
              leftType,
              rightValue,
              rightType,
              simpleCalculationType,
              returnType,
              originalExpressionForErrorMessages);
      case REMAINDER ->
          handleRemainder(
              leftValue,
              leftType,
              rightValue,
              rightType,
              simpleCalculationType,
              returnType,
              originalExpressionForErrorMessages);
      case SHIFT_LEFT ->
          handleShiftLeft(
              leftValue,
              leftType,
              rightValue,
              rightType,
              simpleCalculationType,
              returnType,
              originalExpressionForErrorMessages);
      case SHIFT_RIGHT ->
          handleShiftRight(
              leftValue,
              leftType,
              rightValue,
              rightType,
              simpleCalculationType,
              returnType,
              originalExpressionForErrorMessages);
      case BITWISE_AND ->
          handleBitwiseAnd(
              leftValue,
              leftType,
              rightValue,
              rightType,
              simpleCalculationType,
              returnType,
              originalExpressionForErrorMessages);
      case BITWISE_XOR ->
          handleBitwiseXOR(
              leftValue,
              leftType,
              rightValue,
              rightType,
              simpleCalculationType,
              returnType,
              originalExpressionForErrorMessages);
      case BITWISE_OR ->
          handleBitwiseOR(
              leftValue,
              leftType,
              rightValue,
              rightType,
              simpleCalculationType,
              returnType,
              originalExpressionForErrorMessages);
      default ->
          throw new AssertionError(
              "Unknown binary operation "
                  + op
                  + " in arithmetic operation "
                  + originalExpressionForErrorMessages);
    };
  }

  private Value handleAddition(
      Value leftValue,
      CType leftType,
      Value rightValue,
      CType rightType,
      CSimpleType calculationType,
      CType returnType)
      throws UnsupportedCodeException {

    // Handle NaN
    if (leftValue instanceof NumericValue leftNumericValue && leftNumericValue.hasFloatType()) {
      if (leftNumericValue.getFloatValue().isNan()) {
        return leftValue;
      }
    }
    if (rightValue instanceof NumericValue rightNumericValue && rightNumericValue.hasFloatType()) {
      if (rightNumericValue.getFloatValue().isNan()) {
        return rightValue;
      }
    }
    // TODO: check whether the simplifications below also work for floating point numbers!

    if (leftValue instanceof NumericValue leftNumeric
        && rightValue instanceof NumericValue rightNumeric) {
      try {
        return switch (calculationType.getType()) {
          case INT -> {
            // Both l and r must be of the same type, which in this case is INT, so we can cast to
            // long.
            long lVal = leftNumeric.getNumber().longValue();
            long rVal = rightNumeric.getNumber().longValue();
            long result = arithmeticOperation(lVal, rVal, PLUS, calculationType);
            yield new NumericValue(result);
          }
          case INT128 -> {
            // Typeless calculation! This needs to be cast afterward!
            BigInteger lVal = leftNumeric.bigIntegerValue();
            BigInteger rVal = rightNumeric.bigIntegerValue();
            BigInteger result = arithmeticOperation(lVal, rVal, PLUS);
            yield new NumericValue(result);
          }
          case FLOAT, DOUBLE, FLOAT128 ->
              new NumericValue(
                  arithmeticOperation(
                      PLUS,
                      castToFloat(machineModel, calculationType, leftNumeric),
                      castToFloat(machineModel, calculationType, rightNumeric)));
          default ->
              throw new UnsupportedCodeException(
                  String.format(
                      "Unsupported type %s for calculation of: %s (%s) %s %s (%s)",
                      calculationType,
                      leftNumeric,
                      leftType,
                      PLUS.getOperator(),
                      rightNumeric,
                      rightType),
                  cfaEdge);
        };
      } catch (ArithmeticException e) {
        throw new UnsupportedCodeException(
            String.format(
                "Arithmetic exception (%s) for calculation of: %s (%s) %s %s (%s)",
                e.getMessage(), leftNumeric, leftType, PLUS.getOperator(), rightNumeric, rightType),
            cfaEdge);

        // Previous handling:
        // TODO: do we want this? Or do we want this with an option?
        // Log warning and ignore expression
        /*
        logger.logf(
            Level.WARNING,errorMsg
            );
        return UnknownValue.getInstance();*/
      }

    } else if (leftValue.isUnknown() || rightValue.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for addition due to at least one unknown operand ", cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric

      // Simplification is only needed for symbolics here, as all calculations are well-defined for
      // 2 numeric values.
      // Simplify (x + 0) = x
      if (leftValue instanceof NumericValue numLeft
          && numLeft.bigIntegerValue().equals(BigInteger.ZERO)
          && !calculationType.getType().isFloatingPointType()) {
        return rightValue;
      }
      if (rightValue instanceof NumericValue numRight
          && numRight.bigIntegerValue().equals(BigInteger.ZERO)
          && !calculationType.getType().isFloatingPointType()) {
        return leftValue;
      }

      return createBinarySymbolicExpression(
          leftValue, leftType, rightValue, rightType, PLUS, returnType, calculationType);
    }
  }

  private Value handleSubtraction(
      Value leftValue,
      CType leftType,
      Value rightValue,
      CType rightType,
      CSimpleType calculationType,
      CType returnType)
      throws UnsupportedCodeException {

    // Handle NaN
    if (leftValue instanceof NumericValue leftNumericValue && leftNumericValue.hasFloatType()) {
      if (leftNumericValue.getFloatValue().isNan()) {
        return leftValue;
      }
    }
    if (rightValue instanceof NumericValue rightNumericValue && rightNumericValue.hasFloatType()) {
      if (rightNumericValue.getFloatValue().isNan()) {
        return rightValue;
      }
    }
    // TODO: check whether the simplifications below also work for floating point numbers!

    if (leftValue instanceof NumericValue leftNumeric
        && rightValue instanceof NumericValue rightNumeric) {
      try {
        return switch (calculationType.getType()) {
          case INT -> {
            // Both l and r must be of the same type, which in this case is INT, so we can cast to
            // long.
            long lVal = leftNumeric.getNumber().longValue();
            long rVal = rightNumeric.getNumber().longValue();
            long result = arithmeticOperation(lVal, rVal, MINUS, calculationType);
            yield new NumericValue(result);
          }
          case INT128 -> {
            // Typeless calculation! This needs to be cast afterward!
            BigInteger lVal = leftNumeric.bigIntegerValue();
            BigInteger rVal = rightNumeric.bigIntegerValue();
            BigInteger result = arithmeticOperation(lVal, rVal, MINUS);
            yield new NumericValue(result);
          }
          case FLOAT, DOUBLE, FLOAT128 ->
              new NumericValue(
                  arithmeticOperation(
                      MINUS,
                      castToFloat(machineModel, calculationType, leftNumeric),
                      castToFloat(machineModel, calculationType, rightNumeric)));
          default ->
              throw new UnsupportedCodeException(
                  String.format(
                      "Unsupported type %s for calculation of: %s (%s) %s %s (%s)",
                      calculationType,
                      leftNumeric,
                      leftType,
                      MINUS.getOperator(),
                      rightNumeric,
                      rightType),
                  cfaEdge);
        };
      } catch (ArithmeticException e) {
        throw new UnsupportedCodeException(
            String.format(
                "Arithmetic exception (%s) for calculation of: %s (%s) %s %s (%s)",
                e.getMessage(),
                leftNumeric,
                leftType,
                MINUS.getOperator(),
                rightNumeric,
                rightType),
            cfaEdge);
      }

    } else if (leftValue.isUnknown() || rightValue.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for subtraction due to at least one unknown operand ", cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric

      // Simplification is only needed for symbolics here, as all calculations are well defined for
      // 2 numerics.
      // Simplify (0 - x) = -x
      if (leftValue instanceof NumericValue numLeft
          && numLeft.bigIntegerValue().equals(BigInteger.ZERO)
          && !calculationType.getType().isFloatingPointType()) {
        // Automatically strips negation if already present
        return createBinarySymbolicExpression(
            rightValue, calculationType, UnaryOperator.MINUS, returnType);
      }
      // Simplify (x - 0) = x
      if (rightValue instanceof NumericValue numRight
          && numRight.bigIntegerValue().equals(BigInteger.ZERO)
          && !calculationType.getType().isFloatingPointType()) {
        return leftValue;
      }

      return createBinarySymbolicExpression(
          leftValue, leftType, rightValue, rightType, MINUS, returnType, calculationType);
    }
  }

  private Value handleMultiplication(
      Value leftValue,
      CType leftType,
      Value rightValue,
      CType rightType,
      CSimpleType calculationType,
      CType returnType)
      throws UnsupportedCodeException {

    // Handle NaN
    if (leftValue instanceof NumericValue leftNumericValue && leftNumericValue.hasFloatType()) {
      if (leftNumericValue.getFloatValue().isNan()) {
        return leftValue;
      }
    }
    if (rightValue instanceof NumericValue rightNumericValue && rightNumericValue.hasFloatType()) {
      if (rightNumericValue.getFloatValue().isNan()) {
        return rightValue;
      }
    }
    // TODO: check whether the simplifications below also work for floating point numbers!

    if (leftValue instanceof NumericValue leftNumeric
        && rightValue instanceof NumericValue rightNumeric) {
      try {
        return switch (calculationType.getType()) {
          case INT -> {
            // Both left and right must be of the same type, which in this case is INT,
            // so we can cast to Java long.
            long lVal = leftNumeric.getNumber().longValue();
            long rVal = rightNumeric.getNumber().longValue();
            long result = arithmeticOperation(lVal, rVal, MULTIPLY, calculationType);
            yield new NumericValue(result);
          }
          case INT128 -> {
            // Typeless calculation! This needs to be cast afterward!
            BigInteger lVal = leftNumeric.bigIntegerValue();
            BigInteger rVal = rightNumeric.bigIntegerValue();
            BigInteger result = arithmeticOperation(lVal, rVal, MULTIPLY);
            yield new NumericValue(result);
          }
          case FLOAT, DOUBLE, FLOAT128 ->
              new NumericValue(
                  arithmeticOperation(
                      MULTIPLY,
                      castToFloat(machineModel, calculationType, leftNumeric),
                      castToFloat(machineModel, calculationType, rightNumeric)));
          default ->
              throw new UnsupportedCodeException(
                  String.format(
                      "Unsupported type %s for calculation of: %s (%s) %s %s (%s)",
                      calculationType,
                      leftNumeric,
                      leftType,
                      MULTIPLY.getOperator(),
                      rightNumeric,
                      rightType),
                  cfaEdge);
        };
      } catch (ArithmeticException e) {
        throw new UnsupportedCodeException(
            String.format(
                "Arithmetic exception (%s) for calculation of: %s (%s) %s %s (%s)",
                e.getMessage(),
                leftNumeric,
                leftType,
                MULTIPLY.getOperator(),
                rightNumeric,
                rightType),
            cfaEdge);
      }

    } else {
      // Both values may be unknown or symbolic, while one may be numeric

      // Simplification is only needed for symbolics here, as all calculations are well defined for
      // 2 numerics.
      // Simplify (0 * x) = 0 and (1 * x) = x
      if (leftValue instanceof NumericValue numLeft
          && !calculationType.getType().isFloatingPointType()) {
        if (numLeft.bigIntegerValue().equals(BigInteger.ZERO)) {
          return leftValue; // 0
        } else if (numLeft.bigIntegerValue().equals(BigInteger.ONE)) {
          return rightValue;
        }
      }
      // Simplify (x * 0) = 0 and (x * 1) = x
      if (rightValue instanceof NumericValue numRight
          && !calculationType.getType().isFloatingPointType()) {
        if (numRight.bigIntegerValue().equals(BigInteger.ZERO)) {
          return rightValue; // 0
        } else if (numRight.bigIntegerValue().equals(BigInteger.ONE)) {
          return leftValue;
        }
      }

      if (leftValue.isUnknown() || rightValue.isUnknown()) {
        state.logUnknownValue(
            "Returned unknown value for multiplication operation due to at least one unknown"
                + " operand ",
            cfaEdge);
        return UnknownValue.getInstance();
      }

      // At least 1 value is symbolic, the other can be symbolic or numeric from this point onward

      return createBinarySymbolicExpression(
          leftValue, leftType, rightValue, rightType, MULTIPLY, returnType, calculationType);
    }
  }

  private Value handleDivision(
      Value leftValue,
      CType leftType,
      Value rightValue,
      CType rightType,
      CSimpleType calculationType,
      CType returnType,
      CBinaryExpression expression)
      throws UnsupportedCodeException {

    // Handle NaN
    if (leftValue instanceof NumericValue leftNumericValue && leftNumericValue.hasFloatType()) {
      if (leftNumericValue.getFloatValue().isNan()) {
        return leftValue;
      }
    }
    if (rightValue instanceof NumericValue rightNumericValue && rightNumericValue.hasFloatType()) {
      if (rightNumericValue.getFloatValue().isNan()) {
        return rightValue;
      }
    }
    // TODO: check whether the simplifications below also work for floating point numbers!

    // Simplify (x / 0) = 0 and (x / 1) = x
    if (rightValue instanceof NumericValue numRight
        && !calculationType.getType().isFloatingPointType()) {
      if (numRight.bigIntegerValue().equals(BigInteger.ZERO)) {
        return handleDivisionByZero(expression);
      } else if (numRight.bigIntegerValue().equals(BigInteger.ONE)) {
        return leftValue;
      }
    }

    // Simplify (0 / x) = 0
    if (leftValue instanceof NumericValue numLeft
        && !calculationType.getType().isFloatingPointType()) {
      if (numLeft.bigIntegerValue().equals(BigInteger.ZERO)) {
        return leftValue; // 0
      }
    }

    // Simplify (x / x) = 1
    if (leftValue.equals(rightValue) && !calculationType.getType().isFloatingPointType()) {
      return new NumericValue(1);
    }

    if (leftValue instanceof NumericValue leftNumeric
        && rightValue instanceof NumericValue rightNumeric) {
      try {
        return switch (calculationType.getType()) {
          case INT -> {
            // Both left and right must be of the same type, which in this case is INT,
            // so we can cast to Java long.
            long lVal = leftNumeric.getNumber().longValue();
            long rVal = rightNumeric.getNumber().longValue();
            long result = arithmeticOperation(lVal, rVal, DIVIDE, calculationType);
            yield new NumericValue(result);
          }
          case INT128 -> {
            // Typeless calculation! This needs to be cast afterward!
            BigInteger lVal = leftNumeric.bigIntegerValue();
            BigInteger rVal = rightNumeric.bigIntegerValue();
            BigInteger result = arithmeticOperation(lVal, rVal, DIVIDE);
            yield new NumericValue(result);
          }
          case FLOAT, DOUBLE, FLOAT128 ->
              new NumericValue(
                  arithmeticOperation(
                      DIVIDE,
                      castToFloat(machineModel, calculationType, leftNumeric),
                      castToFloat(machineModel, calculationType, rightNumeric)));
          default ->
              throw new UnsupportedCodeException(
                  String.format(
                      "Unsupported type %s for calculation of: %s (%s) %s %s (%s)",
                      calculationType,
                      leftNumeric,
                      leftType,
                      DIVIDE.getOperator(),
                      rightNumeric,
                      rightType),
                  cfaEdge);
        };
      } catch (ArithmeticException e) {
        throw new UnsupportedCodeException(
            String.format(
                "Arithmetic exception (%s) for calculation of: %s (%s) %s %s (%s)",
                e.getMessage(),
                leftNumeric,
                leftType,
                DIVIDE.getOperator(),
                rightNumeric,
                rightType),
            cfaEdge);
      }

    } else if (leftValue.isUnknown() || rightValue.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for division due to at least one unknown operand ", cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric
      return createBinarySymbolicExpression(
          leftValue, leftType, rightValue, rightType, DIVIDE, returnType, calculationType);
    }
  }

  /** % operator in C. (Which is called remainder, NOT REMAINDER!) */
  private Value handleRemainder(
      Value leftValue,
      CType leftType,
      Value rightValue,
      CType rightType,
      CSimpleType calculationType,
      CType returnType,
      CBinaryExpression expression)
      throws UnsupportedCodeException {
    // Given an integer a (leftValue) and a non-zero integer b (rightValue), it can be shown that
    // there exist unique integers q and r, such that a = qb + r and 0 ≤ r < |b|
    // with q = a/b.
    // The number q is called the quotient, while r is called the remainder.
    // -> remainder r = a — (b x q)
    // => r = 0   for a = 0
    // => r = 0   for b = 1
    // => r = 0   for a = b
    // Other simplifications (e.g. a = 1) fail due to rounding.
    // Notes on C: C99+ chooses the remainder with the same sign as the dividend a.
    // C11 §6.5.5.6: When integers are divided, the result of the / operator
    // is the algebraic quotient with any fractional part discarded. If the quotient a/b is
    // representable, the expression (a/b)*b + a%b shall equal a; otherwise, the behavior of
    // both a/b and a%b is undefined.

    // Handle NaN
    if (leftValue instanceof NumericValue leftNumericValue && leftNumericValue.hasFloatType()) {
      if (leftNumericValue.getFloatValue().isNan()) {
        return leftValue;
      }
    }
    if (rightValue instanceof NumericValue rightNumericValue && rightNumericValue.hasFloatType()) {
      if (rightNumericValue.getFloatValue().isNan()) {
        return rightValue;
      }
    }
    // TODO: check whether the simplifications below also work for floating point numbers!

    // Handle (x % 0) = 0 and simplify (a % 1) = 0
    if (rightValue instanceof NumericValue numRight
        && !calculationType.getType().isFloatingPointType()) {
      if (numRight.bigIntegerValue().equals(BigInteger.ZERO)) {
        return handleDivisionByZero(expression);
      } else if (numRight.bigIntegerValue().equals(BigInteger.ONE)) {
        return new NumericValue(0);
      }
    }

    // Simplify (0 % b) = 0
    if (leftValue instanceof NumericValue numLeft
        && !calculationType.getType().isFloatingPointType()) {
      if (numLeft.bigIntegerValue().equals(BigInteger.ZERO)) {
        return leftValue; // 0
      }
    }
    // Simplify (x % x) = 0
    if (leftValue.equals(rightValue) && !calculationType.getType().isFloatingPointType()) {
      return new NumericValue(0);
    }

    if (leftValue instanceof NumericValue leftNumeric
        && rightValue instanceof NumericValue rightNumeric) {
      try {
        return switch (calculationType.getType()) {
          case INT -> {
            // Both left and right must be of the same type, which in this case is INT,
            // so we can cast to Java long.
            long lVal = leftNumeric.getNumber().longValue();
            long rVal = rightNumeric.getNumber().longValue();
            long result = arithmeticOperation(lVal, rVal, REMAINDER, calculationType);
            yield new NumericValue(result);
          }
          case INT128 -> {
            // Typeless calculation! This needs to be cast afterward!
            BigInteger lVal = leftNumeric.bigIntegerValue();
            BigInteger rVal = rightNumeric.bigIntegerValue();
            BigInteger result = arithmeticOperation(lVal, rVal, REMAINDER);
            yield new NumericValue(result);
          }
          case FLOAT, DOUBLE, FLOAT128 ->
              new NumericValue(
                  arithmeticOperation(
                      REMAINDER,
                      castToFloat(machineModel, calculationType, leftNumeric),
                      castToFloat(machineModel, calculationType, rightNumeric)));
          default ->
              throw new UnsupportedCodeException(
                  String.format(
                      "Unsupported type %s for calculation of: %s (%s) %s %s (%s)",
                      calculationType,
                      leftNumeric,
                      leftType,
                      REMAINDER.getOperator(),
                      rightNumeric,
                      rightType),
                  cfaEdge);
        };
      } catch (ArithmeticException e) {
        throw new UnsupportedCodeException(
            String.format(
                "Arithmetic exception (%s) for calculation of: %s (%s) %s %s (%s)",
                e.getMessage(),
                leftNumeric,
                leftType,
                REMAINDER.getOperator(),
                rightNumeric,
                rightType),
            cfaEdge);
      }

    } else if (leftValue.isUnknown() || rightValue.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for remainder operation due to at least one unknown operand ",
          cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric
      return createBinarySymbolicExpression(
          leftValue, leftType, rightValue, rightType, REMAINDER, returnType, calculationType);
    }
  }

  /** << */
  private Value handleShiftLeft(
      Value leftValue,
      CType leftType,
      Value rightValue,
      CType rightType,
      CSimpleType calculationType,
      CType returnType,
      CBinaryExpression expression)
      throws UnsupportedCodeException {
    // C11 §6.5.7 Bitwise shift operators:
    // The integer promotions are performed on each of the operands. The type of the result is
    // that of the promoted left operand. If the value of the right operand is negative or is
    // greater than or equal to the width of the promoted left operand, the behavior is undefined.

    // The result of E1 << E2 is E1 left-shifted E2 bit positions; vacated bits are filled with
    // zeros. If E1 has an unsigned type, the value of the result is E1 × 2E2, reduced REMAINDER
    // one more than the maximum value representable in the result type. If E1 has a signed
    // type and nonnegative value, and E1 × 2E2 is representable in the result type, then that is
    // the resulting value; otherwise, the behavior is undefined.
    // TODO: handle the type thingy

    // Handle negative or larger second arguments
    if (rightValue instanceof NumericValue numRight
        && !calculationType.getType().isFloatingPointType()) {
      // Is this handling correct for integer promotion?
      if (numRight.bigIntegerValue().signum() < 0) {
        return handleUndefinedBitwiseShift(
            expression, "Second argument in left shift operation is negative.");
      } else if (BigInteger.valueOf(machineModel.getSizeofInBits(calculationType))
              .compareTo(numRight.bigIntegerValue())
          >= 0) {
        return handleUndefinedBitwiseShift(
            expression,
            "Second argument in left shift operation is equal or exceeding the width of the first"
                + " arguments type.");
      }
    }

    // Simplify (0 << x) = 0 ?
    if (leftValue instanceof NumericValue numLeft
        && !calculationType.getType().isFloatingPointType()) {
      if (numLeft.bigIntegerValue().equals(BigInteger.ZERO)) {
        // TODO: look into this!
        // return leftValue; // 0
      }
    }

    if (leftValue instanceof NumericValue leftNumeric
        && rightValue instanceof NumericValue rightNumeric) {
      try {
        return switch (calculationType.getType()) {
          case INT -> {
            // Both left and right must be of the same type, which in this case is INT,
            // so we can cast to Java long.
            long lVal = leftNumeric.getNumber().longValue();
            long rVal = rightNumeric.getNumber().longValue();
            long result = arithmeticOperation(lVal, rVal, SHIFT_LEFT, calculationType);
            yield new NumericValue(result);
          }
          case INT128 -> {
            // Typeless calculation! This needs to be cast afterward!
            BigInteger lVal = leftNumeric.bigIntegerValue();
            BigInteger rVal = rightNumeric.bigIntegerValue();
            BigInteger result = arithmeticOperation(lVal, rVal, SHIFT_LEFT);
            yield new NumericValue(result);
          }
          case FLOAT, DOUBLE, FLOAT128 ->
              throw new UnsupportedOperationException(
                  SHIFT_LEFT.getOperator()
                      + " operator in expression "
                      + expression
                      + " on floating point numbers is not supported. "
                      + cfaEdge);

          default ->
              throw new UnsupportedCodeException(
                  String.format(
                      "Unsupported type %s for calculation of: %s (%s) %s %s (%s)",
                      calculationType,
                      leftNumeric,
                      leftType,
                      SHIFT_LEFT.getOperator(),
                      rightNumeric,
                      rightType),
                  cfaEdge);
        };
      } catch (ArithmeticException e) {
        throw new UnsupportedCodeException(
            String.format(
                "Arithmetic exception (%s) for calculation of: %s (%s) %s %s (%s)",
                e.getMessage(),
                leftNumeric,
                leftType,
                SHIFT_LEFT.getOperator(),
                rightNumeric,
                rightType),
            cfaEdge);
      }

    } else if (leftValue.isUnknown() || rightValue.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for left shift operation due to at least one unknown operand ",
          cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric
      return createBinarySymbolicExpression(
          leftValue, leftType, rightValue, rightType, SHIFT_LEFT, returnType, calculationType);
    }
  }

  /** >> */
  private Value handleShiftRight(
      Value leftValue,
      CType leftType,
      Value rightValue,
      CType rightType,
      CSimpleType calculationType,
      CType returnType,
      CBinaryExpression expression)
      throws UnsupportedCodeException {
    // C11 §6.5.7 Bitwise shift operators:
    // The integer promotions are performed on each of the operands. The type of the result is
    // that of the promoted left operand. If the value of the right operand is negative or is
    // greater than or equal to the width of the promoted left operand, the behavior is undefined.

    // The result of E1 >> E2 is E1 right-shifted E2 bit positions. If E1 has an unsigned type
    // or if E1 has a signed type and a nonnegative value, the value of the result is the integral
    // part of the quotient of E1 / 2E2. If E1 has a signed type and a negative value, the
    // resulting value is implementation-defined.
    // TODO: handle the type thingy

    // Handle negative or larger second arguments
    if (rightValue instanceof NumericValue numRight
        && !calculationType.getType().isFloatingPointType()) {
      // Is this handling correct for integer promotion?
      if (numRight.bigIntegerValue().signum() < 0) {
        return handleUndefinedBitwiseShift(
            expression, "Second argument in left shift operation is negative.");
      } else if (BigInteger.valueOf(machineModel.getSizeofInBits(calculationType))
              .compareTo(numRight.bigIntegerValue())
          >= 0) {
        return handleUndefinedBitwiseShift(
            expression,
            "Second argument in left shift operation is equal or exceeding the width of the first"
                + " arguments type.");
      }
    }

    // Simplify (0 << x) = 0 ?
    if (leftValue instanceof NumericValue numLeft
        && !calculationType.getType().isFloatingPointType()) {
      if (numLeft.bigIntegerValue().equals(BigInteger.ZERO)) {
        // TODO: look into this!
        // return leftValue; // 0
      }
    }

    if (leftValue instanceof NumericValue leftNumeric
        && rightValue instanceof NumericValue rightNumeric) {
      try {
        return switch (calculationType.getType()) {
          case INT -> {
            // Both left and right must be of the same type, which in this case is INT,
            // so we can cast to Java long.
            long lVal = leftNumeric.getNumber().longValue();
            long rVal = rightNumeric.getNumber().longValue();
            long result = arithmeticOperation(lVal, rVal, SHIFT_RIGHT, calculationType);
            yield new NumericValue(result);
          }
          case INT128 -> {
            // Typeless calculation! This needs to be cast afterward!
            BigInteger lVal = leftNumeric.bigIntegerValue();
            BigInteger rVal = rightNumeric.bigIntegerValue();
            BigInteger result = arithmeticOperation(lVal, rVal, SHIFT_RIGHT);
            yield new NumericValue(result);
          }
          case FLOAT, DOUBLE, FLOAT128 ->
              throw new UnsupportedOperationException(
                  SHIFT_RIGHT.getOperator()
                      + " operator in expression "
                      + expression
                      + " on floating point numbers is not supported. "
                      + cfaEdge);
          default ->
              throw new UnsupportedCodeException(
                  String.format(
                      "Unsupported type %s for calculation of: %s (%s) %s %s (%s)",
                      calculationType,
                      leftNumeric,
                      leftType,
                      SHIFT_RIGHT.getOperator(),
                      rightNumeric,
                      rightType),
                  cfaEdge);
        };
      } catch (ArithmeticException e) {
        throw new UnsupportedCodeException(
            String.format(
                "Arithmetic exception (%s) for calculation of: %s (%s) %s %s (%s)",
                e.getMessage(),
                leftNumeric,
                leftType,
                SHIFT_RIGHT.getOperator(),
                rightNumeric,
                rightType),
            cfaEdge);
      }

    } else if (leftValue.isUnknown() || rightValue.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for right shift operation due to at least one unknown operand ",
          cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric
      return createBinarySymbolicExpression(
          leftValue, leftType, rightValue, rightType, SHIFT_RIGHT, returnType, calculationType);
    }
  }

  /**
   * & operator. Each bit in the result is set if and only if each of the corresponding bits in the
   * converted operands is set.
   */
  private Value handleBitwiseAnd(
      Value leftValue,
      CType leftType,
      Value rightValue,
      CType rightType,
      CSimpleType calculationType,
      CType returnType,
      CBinaryExpression expression)
      throws UnsupportedCodeException {

    // TODO: // x & x == x
    // TODO: more simplifications possible?
    // If one operand is 0, all bits are 0, hence the other does not matter, the result is 0!
    if (rightValue instanceof NumericValue numRight
        && !calculationType.getType().isFloatingPointType()) {
      // Is this handling correct for integer promotion?
      if (numRight.bigIntegerValue().equals(BigInteger.ZERO)) {
        return rightValue; // 0
      }
    }

    if (leftValue instanceof NumericValue numLeft
        && !calculationType.getType().isFloatingPointType()) {
      if (numLeft.bigIntegerValue().equals(BigInteger.ZERO)) {
        return leftValue; // 0
      }
    }

    if (leftValue instanceof NumericValue leftNumeric
        && rightValue instanceof NumericValue rightNumeric) {
      try {
        return switch (calculationType.getType()) {
          case INT -> {
            // Both left and right must be of the same type, which in this case is INT,
            // so we can cast to Java long.
            long lVal = leftNumeric.getNumber().longValue();
            long rVal = rightNumeric.getNumber().longValue();
            long result = arithmeticOperation(lVal, rVal, BITWISE_AND, calculationType);
            yield new NumericValue(result);
          }
          case INT128 -> {
            // Typeless calculation! This needs to be cast afterward!
            BigInteger lVal = leftNumeric.bigIntegerValue();
            BigInteger rVal = rightNumeric.bigIntegerValue();
            BigInteger result = arithmeticOperation(lVal, rVal, BITWISE_AND);
            yield new NumericValue(result);
          }
          case FLOAT, DOUBLE, FLOAT128 ->
              throw new UnsupportedOperationException(
                  BITWISE_AND.getOperator()
                      + " operator in expression "
                      + expression
                      + " on floating point numbers is not supported. "
                      + cfaEdge);
          default ->
              throw new UnsupportedCodeException(
                  String.format(
                      "Unsupported type %s for calculation of: %s (%s) %s %s (%s)",
                      calculationType,
                      leftNumeric,
                      leftType,
                      BITWISE_AND.getOperator(),
                      rightNumeric,
                      rightType),
                  cfaEdge);
        };
      } catch (ArithmeticException e) {
        throw new UnsupportedCodeException(
            String.format(
                "Arithmetic exception (%s) for calculation of: %s (%s) %s %s (%s)",
                e.getMessage(),
                leftNumeric,
                leftType,
                BITWISE_AND.getOperator(),
                rightNumeric,
                rightType),
            cfaEdge);
      }

    } else if (leftValue.isUnknown() || rightValue.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for bitwise & (AND) operation due to at least one unknown operand"
              + " ",
          cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric
      return createBinarySymbolicExpression(
          leftValue, leftType, rightValue, rightType, BITWISE_AND, returnType, calculationType);
    }
  }

  /**
   * ^ operator. The result of the ^ operator is the bitwise exclusive OR of the operands (that is,
   * each bit in the result is set if and only if exactly one of the corresponding bits in the
   * converted operands is set).
   */
  private Value handleBitwiseXOR(
      Value leftValue,
      CType leftType,
      Value rightValue,
      CType rightType,
      CSimpleType calculationType,
      CType returnType,
      CBinaryExpression expression)
      throws UnsupportedCodeException {

    // TODO: x ^ x == 0 ?
    // TODO: more simplifications possible?
    // If one operand is 0, all bits of the other are taken
    // x ^ 0 = x
    if (rightValue instanceof NumericValue numRight
        && !calculationType.getType().isFloatingPointType()) {
      // Is this handling correct for integer promotion?
      if (numRight.bigIntegerValue().equals(BigInteger.ZERO)) {
        return leftValue;
      }
    }

    // 0 ^ x = x
    if (leftValue instanceof NumericValue numLeft
        && !calculationType.getType().isFloatingPointType()) {
      if (numLeft.bigIntegerValue().equals(BigInteger.ZERO)) {
        return rightValue;
      }
    }

    if (leftValue instanceof NumericValue leftNumeric
        && rightValue instanceof NumericValue rightNumeric) {
      try {
        return switch (calculationType.getType()) {
          case INT -> {
            // Both left and right must be of the same type, which in this case is INT,
            // so we can cast to Java long.
            long lVal = leftNumeric.getNumber().longValue();
            long rVal = rightNumeric.getNumber().longValue();
            long result = arithmeticOperation(lVal, rVal, BITWISE_XOR, calculationType);
            yield new NumericValue(result);
          }
          case INT128 -> {
            // Typeless calculation! This needs to be cast afterward!
            BigInteger lVal = leftNumeric.bigIntegerValue();
            BigInteger rVal = rightNumeric.bigIntegerValue();
            BigInteger result = arithmeticOperation(lVal, rVal, BITWISE_XOR);
            yield new NumericValue(result);
          }
          case FLOAT, DOUBLE, FLOAT128 ->
              throw new UnsupportedOperationException(
                  BITWISE_XOR.getOperator()
                      + " operator in expression "
                      + expression
                      + " on floating point numbers is not supported. "
                      + cfaEdge);
          default ->
              throw new UnsupportedCodeException(
                  String.format(
                      "Unsupported type %s for calculation of: %s (%s) %s %s (%s)",
                      calculationType,
                      leftNumeric,
                      leftType,
                      BITWISE_XOR.getOperator(),
                      rightNumeric,
                      rightType),
                  cfaEdge);
        };
      } catch (ArithmeticException e) {
        throw new UnsupportedCodeException(
            String.format(
                "Arithmetic exception (%s) for calculation of: %s (%s) %s %s (%s)",
                e.getMessage(),
                leftNumeric,
                leftType,
                BITWISE_XOR.getOperator(),
                rightNumeric,
                rightType),
            cfaEdge);
      }

    } else if (leftValue.isUnknown() || rightValue.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for bitwise ^ (XOR) operation due to at least one unknown operand"
              + " ",
          cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric
      return createBinarySymbolicExpression(
          leftValue, leftType, rightValue, rightType, BITWISE_XOR, returnType, calculationType);
    }
  }

  /**
   * | operator. The result of the | operator is the bitwise inclusive OR of the operands (that is,
   * each bit in the result is set if and only if at least one of the corresponding bits in the
   * converted operands is set).
   */
  private Value handleBitwiseOR(
      Value leftValue,
      CType leftType,
      Value rightValue,
      CType rightType,
      CSimpleType calculationType,
      CType returnType,
      CBinaryExpression expression)
      throws UnsupportedCodeException {

    // TODO: x | x == x
    // TODO: max value (in the current type) -> max value returned

    // TODO: more simplifications (e.g. based on types) possible?

    // If one operand is 0, all bits of the other are taken
    // x ^ 0 = x
    if (rightValue instanceof NumericValue numRight
        && !calculationType.getType().isFloatingPointType()) {
      // Is this handling correct for integer promotion?
      if (numRight.bigIntegerValue().equals(BigInteger.ZERO)) {
        return leftValue;
      }
    }

    // 0 ^ x = x
    if (leftValue instanceof NumericValue numLeft
        && !calculationType.getType().isFloatingPointType()) {
      if (numLeft.bigIntegerValue().equals(BigInteger.ZERO)) {
        return rightValue;
      }
    }

    if (leftValue instanceof NumericValue leftNumeric
        && rightValue instanceof NumericValue rightNumeric) {
      try {
        return switch (calculationType.getType()) {
          case INT -> {
            // Both left and right must be of the same type, which in this case is INT,
            // so we can cast to Java long.
            long lVal = leftNumeric.getNumber().longValue();
            long rVal = rightNumeric.getNumber().longValue();
            long result = arithmeticOperation(lVal, rVal, BITWISE_OR, calculationType);
            yield new NumericValue(result);
          }
          case INT128 -> {
            // Typeless calculation! This needs to be cast afterward!
            BigInteger lVal = leftNumeric.bigIntegerValue();
            BigInteger rVal = rightNumeric.bigIntegerValue();
            BigInteger result = arithmeticOperation(lVal, rVal, BITWISE_OR);
            yield new NumericValue(result);
          }
          case FLOAT, DOUBLE, FLOAT128 ->
              throw new UnsupportedOperationException(
                  BITWISE_OR.getOperator()
                      + " operator in expression "
                      + expression
                      + " on floating point numbers is not supported. "
                      + cfaEdge);
          default ->
              throw new UnsupportedCodeException(
                  String.format(
                      "Unsupported type %s for calculation of: %s (%s) %s %s (%s)",
                      calculationType,
                      leftNumeric,
                      leftType,
                      BITWISE_OR.getOperator(),
                      rightNumeric,
                      rightType),
                  cfaEdge);
        };
      } catch (ArithmeticException e) {
        throw new UnsupportedCodeException(
            String.format(
                "Arithmetic exception (%s) for calculation of: %s (%s) %s %s (%s)",
                e.getMessage(),
                leftNumeric,
                leftType,
                BITWISE_OR.getOperator(),
                rightNumeric,
                rightType),
            cfaEdge);
      }

    } else if (leftValue.isUnknown() || rightValue.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for bitwise | (OR) operation due to at least one unknown operand"
              + " ",
          cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric
      return createBinarySymbolicExpression(
          leftValue, leftType, rightValue, rightType, BITWISE_OR, returnType, calculationType);
    }
  }

  /**
   * Calculate an arithmetic operation on two integer types. All divisions by zero (that includes /
   * and %) should be handled BEFORE calling this method!
   *
   * @param left left hand side value
   * @param right right hand side value. Not allowed to be zero for division operations (including
   *     the REMAINDER operator %).
   * @param op the binary operator
   * @param calculationType the type the result of the calculation should have
   * @return the resulting value
   */
  private long arithmeticOperation(
      final long left,
      final long right,
      final BinaryOperator op,
      final CSimpleType calculationType) {

    if (machineModel.getSizeofInBits(calculationType) >= SIZE_OF_JAVA_LONG
        && !machineModel.isSigned(calculationType)) {
      // Special handling for UNSIGNED_LONG (32 and 64bit), UNSIGNED_LONGLONG (64bit)
      // because Java only has one signed long type (64bit)
      switch (op) {
        case DIVIDE -> {
          checkArgument(right != 0);
          return UnsignedLongs.divide(left, right);
        }
        case REMAINDER -> {
          checkArgument(right != 0);
          return UnsignedLongs.remainder(left, right);
        }
        case SHIFT_RIGHT -> {
          /*
           * from http://docs.oracle.com/javase/tutorial/java/nutsandbolts/op3.html
           *
           * The unsigned right shift operator ">>>" shifts a zero
           * into the leftmost position, while the leftmost position
           * after ">>" depends on sign extension.
           */
          return left >>> right;
        }
        default -> {}
      }
    }

    switch (op) {
      case PLUS -> {
        return left + right;
      }
      case MINUS -> {
        return left - right;
      }
      case DIVIDE -> {
        checkArgument(right != 0);
        return left / right;
      }
      case REMAINDER -> {
        checkArgument(right != 0);
        return left % right;
      }
      case MULTIPLY -> {
        return left * right;
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
        return (right >= SIZE_OF_JAVA_LONG) ? 0 : left << right;
      }
      case SHIFT_RIGHT -> {
        return left >> right;
      }
      case BITWISE_AND -> {
        return left & right;
      }
      case BITWISE_OR -> {
        return left | right;
      }
      case BITWISE_XOR -> {
        return left ^ right;
      }
      default -> throw new AssertionError("unknown binary operation: " + op);
    }
  }

  /**
   * To be called for handling of bitwise shift operations (<< or >>) that exhibit undefined
   * behavior, e.g. due to negative second argument.
   */
  private Value handleUndefinedBitwiseShift(
      final CBinaryExpression pExpression, final String additionalMsg)
      throws UnsupportedCodeException {
    final String loggedMsg =
        String.format(
            "Bitwise shift operation with undefined behavior detected in expression %s due to %s."
                + " %s",
            pExpression, additionalMsg, cfaEdge);
    return handleArithmeticUndefinedBehavior(loggedMsg);
  }

  /**
   * To be called for handling of division or REMAINDER with second operand equal to zero. Either
   * throws {@link UnsupportedCodeException}, or returns a {@link UnknownValue}, or a {@link
   * NumericValue}, depending on the set option for how this is handled.
   */
  private Value handleDivisionByZero(final CBinaryExpression pExpression)
      throws UnsupportedCodeException {
    // C11 §6.5.5.5: The result of the / operator is the quotient from the division of the first
    // operand by the second; the result of the % operator is the remainder. In both operations,
    // if the value of the second operand is zero, the behavior is undefined.
    final String loggedMsg =
        String.format(
            "Concrete division by Zero detected in expression %s, in %s", pExpression, cfaEdge);
    return handleArithmeticUndefinedBehavior(loggedMsg);
  }

  private Value handleArithmeticUndefinedBehavior(final String loggedMsg)
      throws UnsupportedCodeException {
    return switch (options.getArithmeticUndefinedBehaviorHandling()) {
      case WARN_AND_RETURN_UNKNOWN ->
          logDivisionByZeroAndReturnValue(UnknownValue.getInstance(), loggedMsg);
      case WARN_AND_RETURN_ZERO -> logDivisionByZeroAndReturnValue(new NumericValue(0), loggedMsg);
      case WARN_AND_RETURN_ONE -> logDivisionByZeroAndReturnValue(new NumericValue(1), loggedMsg);
      case STOP_ANALYSIS ->
          throw new UnsupportedCodeException(
              loggedMsg
                  + ". Stopping analysis as defined in option"
                  + " 'arithmeticUndefinedBehaviorHandling'.",
              cfaEdge);
    };
  }

  @SuppressWarnings("FormatStringAnnotation")
  private Value logDivisionByZeroAndReturnValue(final Value returnedValue, final String loggedMsg) {
    logger.logf(Level.SEVERE, loggedMsg, cfaEdge);
    return returnedValue;
  }

  /**
   * Calculate an arithmetic operation on two int128 types. No operation involving division must
   * divide by zero (i.e. / or %). These cases must be handled before calling this!
   *
   * @param l left hand side value
   * @param r right hand side value. Not allowed to be zero for division operations (including the
   *     REMAINDER operator %).
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
        checkArgument(!r.equals(BigInteger.ZERO));
        return l.divide(r);
      }
      case REMAINDER -> {
        checkArgument(!r.equals(BigInteger.ZERO));
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
      case BITWISE_AND -> {
        return l.and(r);
      }
      case BITWISE_OR -> {
        return l.or(r);
      }
      case BITWISE_XOR -> {
        return l.xor(r);
      }
      default -> throw new AssertionError("Unknown binary operation: " + op);
    }
  }

  /**
   * Calculate an arithmetic operation on two floating point values. Supported arithmetic operations
   * are PLUS, MINUS, DIVIDE, REMAINDER, and MULTIPLY.
   *
   * @param pOperation the binary operator, either +, -, *, /, or %.
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
      case REMAINDER -> pArg1.modulo(pArg2);
      case MULTIPLY -> pArg1.multiply(pArg2);
      case SHIFT_LEFT, SHIFT_RIGHT, BITWISE_AND, BITWISE_OR, BITWISE_XOR ->
          throw new UnsupportedOperationException(
              "Trying to perform " + pOperation + " on floating point operands");
      default -> throw new IllegalArgumentException("Unknown binary operation: " + pOperation);
    };
  }

  /**
   * Handled all (binary) comparisons, i.e. ==, !=, <, <=, >, >=, including common simplifications.
   */
  private Value handleComparisonOperation(
      final Value left,
      final Value right,
      final BinaryOperator op,
      final CBinaryExpression expression)
      throws UnsupportedCodeException {
    assert op.isLogicalOperator();

    final CType leftType = expression.getOperand1().getExpressionType();
    final CType rightType = expression.getOperand2().getExpressionType();
    final CType returnType = expression.getExpressionType();
    final CType calculationType = expression.getCalculationType();
    // TODO: make sure that the calculation type is actually the calculation type AFTER integer
    //  promotion etc.
    // TODO: write unit tests for this! (i.e. all of the common handling in this class)

    // At this point we're only handling values of simple types
    final Optional<CSimpleType> maybeSimpleCalculationType = getSimplifiedType(calculationType);
    if (maybeSimpleCalculationType.isEmpty()) {
      throw new UnsupportedCodeException(
          "Unsupported calculation type "
              + calculationType
              + " in binary comparison operation "
              + expression,
          cfaEdge);
    }

    CSimpleType simpleCalculationType = maybeSimpleCalculationType.orElseThrow();

    return switch (op) {
      case EQUALS ->
          handleEqualityOperator(
              left, leftType, right, rightType, simpleCalculationType, returnType);
      case NOT_EQUALS ->
          handleInequalityOperator(
              left, leftType, right, rightType, simpleCalculationType, returnType);
      case GREATER_THAN ->
          handleGreaterThanOperator(
              left, leftType, right, rightType, simpleCalculationType, returnType);
      case GREATER_EQUAL ->
          handleGreaterEqualsOperator(
              left, leftType, right, rightType, simpleCalculationType, returnType);
      case LESS_THAN ->
          handleLessThanOperator(
              left, leftType, right, rightType, simpleCalculationType, returnType);
      case LESS_EQUAL ->
          handleLessEqualsOperator(
              left, leftType, right, rightType, simpleCalculationType, returnType);
      default ->
          throw new AssertionError(
              "Unknown binary operation " + op + " in arithmetic operation " + expression);
    };
  }

  // ==
  private Value handleEqualityOperator(
      final Value left,
      final CType leftType,
      final Value right,
      final CType rightType,
      final CSimpleType calculationType,
      final CType returnType) {

    if (left instanceof NumericValue leftNumeric && right instanceof NumericValue rightNumeric) {
      boolean equals =
          switch (calculationType.getType()) {
            case INT128, CHAR, INT, BOOL ->
                leftNumeric.bigIntegerValue().equals(rightNumeric.bigIntegerValue());
            case FLOAT, DOUBLE, FLOAT128 ->
                comparisonOperation(
                    EQUALS,
                    castToFloat(machineModel, calculationType, leftNumeric),
                    castToFloat(machineModel, calculationType, rightNumeric));
            default ->
                throw new AssertionError(
                    "Unexpected and unhandled type "
                        + calculationType.getType()
                        + " in comparison operation");
          };
      // return 1 if expression holds, 0 otherwise
      return new NumericValue(equals ? 1 : 0);

    } else if (left.isUnknown() || right.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for equality (==) due to at least one unknown operand ", cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric
      if (left.equals(right) && !calculationType.getType().isFloatingPointType()) {
        return new NumericValue(1);
      }
      // TODO: we can handle some Float combinations here as well! e.g. if one is NaN, this is
      //  always false etc.
      // TODO: we can also handle some additional cases like a == !a
      // TODO: do we want to try to canonize symbolic expressions to allow things like (1 + a) == (a
      //  + 1) to be recognized? (this is more of a general question)
      // TODO: leverage type differences (e.g. one unsigned, one signed)
      return createBinarySymbolicExpression(
          left, leftType, right, rightType, EQUALS, returnType, calculationType);
    }
  }

  // !=
  private Value handleInequalityOperator(
      final Value left,
      final CType leftType,
      final Value right,
      final CType rightType,
      final CSimpleType calculationType,
      final CType returnType) {

    if (left instanceof NumericValue leftNumeric && right instanceof NumericValue rightNumeric) {
      boolean notEquals =
          switch (calculationType.getType()) {
            case INT128, CHAR, INT, BOOL ->
                !leftNumeric.bigIntegerValue().equals(rightNumeric.bigIntegerValue());
            case FLOAT, DOUBLE, FLOAT128 ->
                comparisonOperation(
                    NOT_EQUALS,
                    castToFloat(machineModel, calculationType, leftNumeric),
                    castToFloat(machineModel, calculationType, rightNumeric));
            default ->
                throw new AssertionError(
                    "Unexpected and unhandled type "
                        + calculationType.getType()
                        + " in comparison operation");
          };
      // return 1 if expression holds, 0 otherwise
      return new NumericValue(notEquals ? 1 : 0);

    } else if (left.isUnknown() || right.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for inequality (!=) due to at least one unknown operand ",
          cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric
      if (left.equals(right) && !calculationType.getType().isFloatingPointType()) {
        return new NumericValue(0);
      }
      // TODO: we can handle some Float combinations here as well! e.g. if one is NaN, this is
      //  always true etc.
      // TODO: we can also handle some additional cases like a != !a
      // TODO: leverage type differences (e.g. one unsigned, one signed)
      return createBinarySymbolicExpression(
          left, leftType, right, rightType, NOT_EQUALS, returnType, calculationType);
    }
  }

  // <
  private Value handleLessThanOperator(
      final Value left,
      final CType leftType,
      final Value right,
      final CType rightType,
      final CSimpleType calculationType,
      final CType returnType) {

    if (left instanceof NumericValue leftNumeric && right instanceof NumericValue rightNumeric) {
      boolean lessThan =
          switch (calculationType.getType()) {
            case INT128, CHAR, INT, BOOL ->
                leftNumeric.bigIntegerValue().compareTo(rightNumeric.bigIntegerValue()) < 0;
            case FLOAT, DOUBLE, FLOAT128 ->
                comparisonOperation(
                    LESS_THAN,
                    castToFloat(machineModel, calculationType, leftNumeric),
                    castToFloat(machineModel, calculationType, rightNumeric));
            default ->
                throw new AssertionError(
                    "Unexpected and unhandled type "
                        + calculationType.getType()
                        + " in comparison operation");
          };
      // return 1 if expression holds, 0 otherwise
      return new NumericValue(lessThan ? 1 : 0);

    } else if (left.isUnknown() || right.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for < (less than) operation due to at least one unknown operand ",
          cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric
      // if (!left.equals(right) && !calculationType.getType().isFloatingPointType()) {
      //   return new NumericValue(1);
      // }
      // TODO: look into additional cases (e.g. equality -> can't be less than)
      // TODO: leverage type differences (e.g. one unsigned, one signed)
      return createBinarySymbolicExpression(
          left, leftType, right, rightType, LESS_THAN, returnType, calculationType);
    }
  }

  // <=
  private Value handleLessEqualsOperator(
      final Value left,
      final CType leftType,
      final Value right,
      final CType rightType,
      final CSimpleType calculationType,
      final CType returnType) {

    if (left instanceof NumericValue leftNumeric && right instanceof NumericValue rightNumeric) {
      boolean lessEquals =
          switch (calculationType.getType()) {
            case INT128, CHAR, INT, BOOL ->
                leftNumeric.bigIntegerValue().compareTo(rightNumeric.bigIntegerValue()) <= 0;
            case FLOAT, DOUBLE, FLOAT128 ->
                comparisonOperation(
                    LESS_EQUAL,
                    castToFloat(machineModel, calculationType, leftNumeric),
                    castToFloat(machineModel, calculationType, rightNumeric));
            default ->
                throw new AssertionError(
                    "Unexpected and unhandled type "
                        + calculationType.getType()
                        + " in comparison operation");
          };
      // return 1 if expression holds, 0 otherwise
      return new NumericValue(lessEquals ? 1 : 0);

    } else if (left.isUnknown() || right.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for <= (less equals) operation due to at least one unknown"
              + " operand ",
          cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric
      if (left.equals(right) && !calculationType.getType().isFloatingPointType()) {
        return new NumericValue(1);
      }
      // TODO: look into additional cases (beyond equality == less equals)
      // TODO: leverage type differences (e.g. one unsigned, one signed)
      return createBinarySymbolicExpression(
          left, leftType, right, rightType, LESS_EQUAL, returnType, calculationType);
    }
  }

  // <
  private Value handleGreaterThanOperator(
      final Value left,
      final CType leftType,
      final Value right,
      final CType rightType,
      final CSimpleType calculationType,
      final CType returnType) {

    if (left instanceof NumericValue leftNumeric && right instanceof NumericValue rightNumeric) {
      boolean greaterThan =
          switch (calculationType.getType()) {
            case INT128, CHAR, INT, BOOL ->
                leftNumeric.bigIntegerValue().compareTo(rightNumeric.bigIntegerValue()) > 0;
            case FLOAT, DOUBLE, FLOAT128 ->
                comparisonOperation(
                    GREATER_THAN,
                    castToFloat(machineModel, calculationType, leftNumeric),
                    castToFloat(machineModel, calculationType, rightNumeric));
            default ->
                throw new AssertionError(
                    "Unexpected and unhandled type "
                        + calculationType.getType()
                        + " in comparison operation");
          };
      // return 1 if expression holds, 0 otherwise
      return new NumericValue(greaterThan ? 1 : 0);

    } else if (left.isUnknown() || right.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for > (greater than) operation due to at least one unknown"
              + " operand ",
          cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric
      // if (!left.equals(right) && !calculationType.getType().isFloatingPointType()) {
      //   return new NumericValue(1);
      // }
      // TODO: look into additional cases (e.g. equality -> not greater than)
      // TODO: leverage type differences (e.g. one unsigned, one signed)
      return createBinarySymbolicExpression(
          left, leftType, right, rightType, GREATER_THAN, returnType, calculationType);
    }
  }

  // <=
  private Value handleGreaterEqualsOperator(
      final Value left,
      final CType leftType,
      final Value right,
      final CType rightType,
      final CSimpleType calculationType,
      final CType returnType) {

    if (left instanceof NumericValue leftNumeric && right instanceof NumericValue rightNumeric) {
      boolean greaterEquals =
          switch (calculationType.getType()) {
            case INT128, CHAR, INT, BOOL ->
                leftNumeric.bigIntegerValue().compareTo(rightNumeric.bigIntegerValue()) >= 0;
            case FLOAT, DOUBLE, FLOAT128 ->
                comparisonOperation(
                    GREATER_EQUAL,
                    castToFloat(machineModel, calculationType, leftNumeric),
                    castToFloat(machineModel, calculationType, rightNumeric));
            default ->
                throw new AssertionError(
                    "Unexpected and unhandled type "
                        + calculationType.getType()
                        + " in comparison operation");
          };
      // return 1 if expression holds, 0 otherwise
      return new NumericValue(greaterEquals ? 1 : 0);

    } else if (left.isUnknown() || right.isUnknown()) {
      state.logUnknownValue(
          "Returned unknown value for >= (greater equals) operation due to at least one unknown"
              + " operand ",
          cfaEdge);
      return UnknownValue.getInstance();

    } else {
      // At least 1 value is symbolic, the other can be symbolic or numeric
      if (left.equals(right) && !calculationType.getType().isFloatingPointType()) {
        return new NumericValue(1);
      }
      // TODO: look into additional cases (beyond equality == greater equals)
      // TODO: leverage type differences (e.g. one unsigned, one signed)
      return createBinarySymbolicExpression(
          left, leftType, right, rightType, GREATER_EQUAL, returnType, calculationType);
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
    if (!(value instanceof NumericValue numericValue)) {
      logger.logf(
          Level.FINE, "Can not cast C value %s to %s", value.toString(), targetType.toString());
      return value;
    }

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

    if (pValue instanceof SymbolicValue symbolicValue
        && (pTargetType instanceof JSimpleType || pTargetType instanceof CSimpleType)) {

      return CastExpression.of(symbolicValue, pTargetType);
    }

    // If the value is not symbolic, just return it.
    return pValue;
  }

  /**
   * Returns a numeric type that can be used to perform arithmetics on an instance of the type
   * directly if possible.
   *
   * <p>Most notably, CPointerType will be converted to the unsigned integer type of correct size.
   */
  public Optional<CSimpleType> getSimplifiedType(CType type) {
    type = type.getCanonicalType();
    if (type instanceof CPointerType) {
      return Optional.of(machineModel.getPointerAsUnsignedIntType());
    } else if (type instanceof CSimpleType simpleType) {
      return Optional.of(simpleType);
    } else {
      return Optional.empty();
    }
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

  /**
   * Returns true for the following:
   *
   * <p>- Addition of integer to a pointer
   *
   * <p>- Subtraction of integer from a pointer
   *
   * <p>- Subtracting two pointers of the same type
   *
   * <p>- Comparison of pointers
   */
  private boolean isPointerArithmetics(
      Value pLeftValue, Value pRightValue, CBinaryExpression expr, SMGState currentState) {
    // CType leftType = expr.getOperand1().getExpressionType().getCanonicalType();
    // CType rightType = expr.getOperand2().getExpressionType().getCanonicalType();
    BinaryOperator op = expr.getOperator();

    // Filter out non-pointer arithmetic operators
    if (!op.isLogicalOperator() && op != PLUS && op != MINUS) {
      return false;
    }

    Value leftValue = unwrapPotentialAddressExpression(pLeftValue, currentState);
    boolean leftIsPointer = currentState.isPointer(leftValue);
    // We only care about 0 if it is a pointer. Even an offset stripped in
    // unwrapPotentialAddressExpression() does not matter here!
    boolean leftIsNumeric = leftValue instanceof NumericValue;
    boolean leftIsZero =
        leftIsNumeric && leftValue.asNumericValue().bigIntegerValue().equals(BigInteger.ZERO);
    checkState(!(leftIsPointer && leftIsNumeric) || leftIsZero);

    Value rightValue = unwrapPotentialAddressExpression(pRightValue, currentState);
    boolean rightIsPointer = currentState.isPointer(rightValue);
    boolean rightIsNumeric = rightValue instanceof NumericValue;
    boolean rightIsZero =
        rightIsNumeric && rightValue.asNumericValue().bigIntegerValue().equals(BigInteger.ZERO);
    checkState(!(rightIsPointer && rightIsNumeric) || rightIsZero);

    if (rightIsNumeric && leftIsNumeric) {
      return false; // 0 is a pointer internally, but this can just be calculated normally
    }

    // TODO: also calc type? Can it even happen that calc is pointer but none of the values 2?
    // boolean oneTypeIsPointer =
    //    leftType instanceof CPointerType || rightType instanceof CPointerType;

    boolean leftValueIsPointerButNotNumeric = leftIsPointer && !leftIsNumeric;
    boolean rightValueIsPointerButNotNumeric = rightIsPointer && !rightIsNumeric;

    // Check that at least 1 is a pointer that needs special handling (not numeric)
    return leftValueIsPointerButNotNumeric || rightValueIsPointerButNotNumeric;
    // Below is the previous checking (before the current return was introduced). May be useful?
    /*
    if (op.isLogicalOperator()) {
      // Both need to be pointers with the same type or numeric 0
      if ((leftIsZero && rightType instanceof CPointerType)
          || (rightIsZero && leftType instanceof CPointerType)) {
        return true;
      }
      if (!(leftType instanceof CPointerType leftPtrType
          && rightType instanceof CPointerType rightPtrType)) {
        if (leftType instanceof CArrayType && rightType instanceof CArrayType) {
          // Arrays do not count towards pointer arithmetics, but I want to see whether we encounter
          // this case and we missed something!
          logger.logf(
              Level.WARNING,
              "Binary comparison with operator %s, left-hand type %s, and right-hand type %s not"
                  + " recognized as pointer arithmetics in %s",
              op,
              leftType,
              rightType,
              cfaEdge);
        }
        // "Pseudo" pointer arithmetics; i.e. something like: ((unsigned long) pointer) == pointer
        // We still want to solve this using pointer arithmetics, as it uses memory layout
        // information that is more accurate!
        // TODO: check that there is 2 pointers underneath?
        return true;
      }
      if (!leftPtrType
          .getType()
          .getCanonicalType()
          .equals(rightPtrType.getType().getCanonicalType())) {
        if (rightIsPointer && leftIsPointer) {
          logger.logf(
              Level.WARNING,
              "Binary comparison with 2 pointer values, operator %s, left-hand type %s, and"
                  + " right-hand type %s not recognized as pointer arithmetics in %s",
              op,
              leftType,
              rightType,
              cfaEdge);
        }
        return false;
      }
    }
    // If both are numeric or symbolic (non-pointers), let it be calculated using regular
    // arithmetics
    return leftIsPointer || rightIsPointer;*/
  }

  /**
   * Unwraps {@link AddressExpression}s and {@link ConstantSymbolicExpression}s into their {@link
   * SymbolicIdentifier} used to identify pointers. Returns the input for all other cases. The
   * returned value should never be used on its own, just to check whether it is a pointer or not!
   */
  private Value unwrapPotentialAddressExpression(Value pMaybeAddress, SMGState currentState) {
    Value maybeAddress = pMaybeAddress;
    if (maybeAddress instanceof AddressExpression targetAddress) {
      maybeAddress = targetAddress.getMemoryAddress();
      if (maybeAddress instanceof ConstantSymbolicExpression constRight) {
        maybeAddress = constRight.getValue();
      }
      // AddressExpressions may never be nested
      checkState(!(maybeAddress instanceof AddressExpression));
      // Also, a pointer is always a symbolicIdentifier, never a const expr in SMG2
      checkState(currentState.isPointer(maybeAddress));
    }
    if (maybeAddress instanceof ConstantSymbolicExpression constRight) {
      maybeAddress = constRight.getValue();
    }
    return maybeAddress;
  }
}
