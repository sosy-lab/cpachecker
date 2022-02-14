// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
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
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAValueExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue.NegativeNaN;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/**
 * This visitor visits values mostly on the right hand side to get values (SMG or not) (but also on
 * the left hand side, for example concrete values used in array access) Important: we need to reuse
 * the state given back by other visitors and use the state in this class just to give it to the
 * next/innermost visitor! Read operations have side effects, hence why using the most up to date
 * state is important.
 */
public class SMGCPAValueVisitor
    extends DefaultCExpressionVisitor<List<ValueAndSMGState>, CPATransferException>
    implements CRightHandSideVisitor<List<ValueAndSMGState>, CPATransferException> {

  // TODO: remove CPAException and use more specific exceptions

  // The evaluator translates C expressions into the SMG counterparts and vice versa.
  @SuppressWarnings("unused")
  private final SMGCPAValueExpressionEvaluator evaluator;

  @SuppressWarnings("unused")
  private final SMGState state;

  /** This edge is only to be used for debugging/logging! */
  @SuppressWarnings("unused")
  private final CFAEdge cfaEdge;

  private final LogManagerWithoutDuplicates logger;

  public SMGCPAValueVisitor(
      SMGCPAValueExpressionEvaluator pEvaluator,
      SMGState currentState,
      CFAEdge edge,
      LogManagerWithoutDuplicates pLogger) {
    evaluator = pEvaluator;
    state = currentState;
    cfaEdge = edge;
    logger = pLogger;
  }

  @Override
  protected List<ValueAndSMGState> visitDefault(CExpression pExp) throws CPATransferException {
    // Just get a default value and log
    logger.logf(
        Level.INFO,
        "%s, Default value: CExpression %s could not be recognized and the default value %s was used for its value. Related CFAEdge: %s",
        cfaEdge.getFileLocation(),
        pExp,
        SMGValue.zeroValue(),
        cfaEdge.getRawStatement());
    return ImmutableList.of(ValueAndSMGState.of(new NumericValue(0), state));
  }

  @Override
  public List<ValueAndSMGState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    // TODO: investigate whats possible here.
    return null;
  }

  @Override
  public List<ValueAndSMGState> visit(CArraySubscriptExpression e) throws CPATransferException {
    // Array subscript is default Java array usage. Example: array[5]
    // In C this can be translated to *(array + 5). Note: this is commutative!
    // TODO: how to handle *(array++) etc.? This case equals *(array + 1). Would the ++ case come
    // from an assignment edge?

    // The expression is split into array and subscript expression
    // Use the array expression in the visitor again to get the array address
    CExpression arrayExpr = e.getArrayExpression();
    List<ValueAndSMGState> arrayValueAndStates = arrayExpr.accept(this);
    // We know that there can only be 1 return value for the array address
    Preconditions.checkArgument(arrayValueAndStates.size() == 1);
    ValueAndSMGState arrayValueAndState = arrayValueAndStates.get(0);

    // Evaluate the subscript as far as possible
    CExpression subscriptExpr = e.getSubscriptExpression();
    List<ValueAndSMGState> subscriptValueAndStates =
        subscriptExpr.accept(
            new SMGCPAValueVisitor(evaluator, arrayValueAndState.getState(), cfaEdge, logger));

    // We know that there can only be 1 return value for the subscript
    Preconditions.checkArgument(subscriptValueAndStates.size() == 1);
    ValueAndSMGState subscriptValueAndState = subscriptValueAndStates.get(0);

    Value subscriptValue = subscriptValueAndState.getValue();
    SMGState newState = subscriptValueAndState.getState();
    // If the subscript is a unknown value, we can't read anything and return unknown
    if (!subscriptValue.isNumericValue()) {
      return ImmutableList.of(ValueAndSMGState.ofUnknownValue(newState));
    }
    // Calculate the offset out of the subscript value and the type
    BigInteger typeSizeInBits =
        evaluator.getBitSizeof(subscriptValueAndState.getState(), subscriptExpr);
    BigInteger subscriptOffset =
        typeSizeInBits.multiply(subscriptValue.asNumericValue().bigInteger());

    // Get the value from the array and return the value + state
    return ImmutableList.of(
        evaluator.readValue(newState, subscriptValue, subscriptOffset, typeSizeInBits));
  }

  @Override
  public List<ValueAndSMGState> visit(CBinaryExpression e) throws CPATransferException {
    // TODO: remove from this class, move to a dedicated
    // From assumption edge
    // binary expression, examples: +, -, *, /, ==, !=, < ....
    // visit left and right, then use the expression and return it. This also means we need to
    // create new SMG values (symbolic value ranges) for them, but don't save them in the SMG right
    // away (save, not write!) as this is only done when write is used.

    return visitDefault(e);
  }

  @Override
  public List<ValueAndSMGState> visit(CCastExpression e) throws CPATransferException {
    // Casts are not trivial within SMGs as there might be type reinterpretation used inside the SMGs,
    // but this should be taken care of by the SMGCPAValueExpressionEvaluator and no longer be a problem here!
    // Get the type and value from the nested expression (might be SMG) and cast the value
    // Also most of this code is taken from the value analysis CPA and modified
    CType targetType = e.getExpressionType();
    // We know that there will be only 1 value as a return here
    List<ValueAndSMGState> valuesAndStates = e.getOperand().accept(this);
    Preconditions.checkArgument(valuesAndStates.size() == 1);
    ValueAndSMGState valueAndState = valuesAndStates.get(0);
    Value value = valueAndState.getValue();
    SMGState currentState = valueAndState.getState();
    MachineModel machineModel = evaluator.getMachineModel();

    // TODO: cast arrays etc.

    if (!value.isExplicitlyKnown()) {
      return ImmutableList.of(
          ValueAndSMGState.of(
              castSymbolicValue(value, targetType, Optional.of(machineModel)), currentState));
    }

    // We only use numeric/symbolic/unknown values anyway and we can't cast unknowns
    if (!value.isNumericValue()) {
      logger.logf(
          Level.FINE, "Can not cast C value %s to %s", value.toString(), targetType.toString());
      return ImmutableList.of(ValueAndSMGState.of(value, state));
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
      return ImmutableList.of(ValueAndSMGState.of(value, state));
    }

    return ImmutableList.of(
        ValueAndSMGState.of(castNumeric(numericValue, type, machineModel, size), currentState));
  }

  @Override
  public List<ValueAndSMGState> visit(CFieldReference e) throws CPATransferException {
    // Get the object holding the field (should be struct/union)
    // I most likely need the CFAEdge for that
    // Read the value of the field from the object

    return visitDefault(e);
  }

  @Override
  public List<ValueAndSMGState> visit(CIdExpression e) throws CPATransferException {
    // essentially variables
    // Either CEnumerator, CVariableDeclaration, CParameterDeclaration
    // Could also be a type/function declaration, decide if we need those.

    // Get the var using the stack SMG, read and return
    return visitDefault(e);
  }

  @SuppressWarnings("unused")
  @Override
  public List<ValueAndSMGState> visit(CCharLiteralExpression e) throws CPATransferException {
    // Simple character expression; We use the numeric value
    int value = e.getCharacter();

    // We simply return the Value, as if a mapping to SMGValue is needed only after Value is written
    // into the memory, but when writing a mapping is created anyway
    return ImmutableList.of(ValueAndSMGState.of(new NumericValue(value), state));
  }

  @SuppressWarnings("unused")
  @Override
  public List<ValueAndSMGState> visit(CFloatLiteralExpression e) throws CPATransferException {
    // Floating point value expression
    BigDecimal value = e.getValue();

    // We simply return the Value, as if a mapping to SMGValue is needed only after Value is written
    // into the memory, but when writing a mapping is created anyway
    return ImmutableList.of(ValueAndSMGState.of(new NumericValue(value), state));
  }

  @SuppressWarnings("unused")
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
    // Either split the String into chars or simply assign in as a single big value
    String string = e.getContentString();
    ImmutableList.Builder<ValueAndSMGState> builder = ImmutableList.builder();
    for (char c : string.toCharArray()) {
      builder.add(ValueAndSMGState.of(new NumericValue((int) c), state));
    }

    return builder.build();
  }

  @Override
  public List<ValueAndSMGState> visit(CTypeIdExpression e) throws CPATransferException {
    // Operators:
    // sizeOf, typeOf and
    // _Alignof or alignof = the number of bytes between successive addresses, essentially a fancy
    // name for size

    // SMGs have type reinterpretation! Get the type of the SMG and translate it back to the C type.
    return visitDefault(e);
  }

  @Override
  public List<ValueAndSMGState> visit(CUnaryExpression e) throws CPATransferException {
    // Unary expression types like & (address of operator), sizeOf(), ++, - (unary minus), --, !
    // (not)
    // Split up into their operators, handle each. Most are not that difficult.
    // & needs the address of an object, so we need to get the mapping or create one to an SMG
    // object

    return visitDefault(e);
  }

  @Override
  public List<ValueAndSMGState> visit(CPointerExpression e) throws CPATransferException {
    // Pointers can be a multitude of things in C
    // Get the operand of the pointer, get the type of that and then split into the different cases
    // to handle them
    // Once we dereference a object with this, we return the objects value at the correct offset.
    // *(array + 2) for example

    return visitDefault(e);
  }

  @Override
  public List<ValueAndSMGState> visit(CAddressOfLabelExpression e) throws CPATransferException {
    // && expression
    // This is not in the C standard, just gcc
    // https://gcc.gnu.org/onlinedocs/gcc/Labels-as-Values.html
    // Returns a address to a function

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

  // ++++++++++++++++++++ Below this point casting helper methods

  /** Taken from the value analysis CPA and modified. Casts symbolic {@link Value}s. */
  private Value castSymbolicValue(
      Value pValue, Type pTargetType, Optional<MachineModel> pMachineModel) {
    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

    if (pValue instanceof SymbolicValue && pTargetType instanceof CSimpleType) {
      return factory.cast((SymbolicValue) pValue, pTargetType, pMachineModel);
    }

    // If the value is not symbolic, just return it.
    return pValue;
  }

  /** Taken from the value analysis CPA. TODO: check that all casts are correct and add missing. */
  private Value castNumeric(
      @NonNull final NumericValue numericValue,
      final CType type,
      final MachineModel machineModel,
      final int size) {

    if (!(type instanceof CSimpleType)) {
      return numericValue;
    }

    final CSimpleType st = (CSimpleType) type;

    switch (st.getType()) {
      case BOOL:
        return convertToBool(numericValue);
      case INT128:
      case INT:
      case CHAR:
        {
          if (isNan(numericValue)) {
            // result of conversion of NaN to integer is undefined
            return UnknownValue.getInstance();

          } else if ((numericValue.getNumber() instanceof Float
                  || numericValue.getNumber() instanceof Double)
              && Math.abs(numericValue.doubleValue() - numericValue.longValue()) >= 1) {
            // if number is a float and float can not be exactly represented as integer, the
            // result of the conversion of float to integer is undefined
            return UnknownValue.getInstance();
        }

        final BigInteger valueToCastAsInt;
        if (numericValue.getNumber() instanceof BigInteger) {
          valueToCastAsInt = numericValue.bigInteger();
        } else if (numericValue.getNumber() instanceof BigDecimal) {
          valueToCastAsInt = numericValue.bigDecimalValue().toBigInteger();
        } else {
          valueToCastAsInt = BigInteger.valueOf(numericValue.longValue());
        }
          final boolean targetIsSigned = machineModel.isSigned(st);

          final BigInteger maxValue = BigInteger.ONE.shiftLeft(size); // 2^size
          BigInteger result = valueToCastAsInt.remainder(maxValue); // shrink to number of bits

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

          if (isGreaterThan(result, signedUpperBound)) {
            // if result overflows, let it 'roll around' and add overflow to lower bound
            result = result.subtract(maxValue);
          } else if (isLessThan(result, signedLowerBound)) {
            result = result.add(maxValue);
          }

          return new NumericValue(result);
        }

      case FLOAT:
      case DOUBLE:
      case FLOAT128:
        {
          // TODO: look more closely at the INT/CHAR cases, especially at the loggedEdges stuff
          // TODO: check for overflow(source larger than the highest number we can store in target
          // etc.)

          // casting to DOUBLE, if value is INT or FLOAT. This is sound, if we would also do this
          // cast in C.
          Value result;

          final int bitPerByte = machineModel.getSizeofCharInBits();

          if (isNan(numericValue) || isInfinity(numericValue)) {
            result = numericValue;
        } else if (size == machineModel.getSizeofFloat128() * 8) {
          result = new NumericValue(numericValue.bigDecimalValue());
        } else if (size == machineModel.getSizeofLongDouble() * bitPerByte) {

            if (numericValue.bigDecimalValue().doubleValue() == numericValue.doubleValue()) {
              result = new NumericValue(numericValue.doubleValue());
            } else if (numericValue.bigDecimalValue().floatValue() == numericValue.floatValue()) {
              result = new NumericValue(numericValue.floatValue());
            } else {
              result = UnknownValue.getInstance();
            }
          } else {
            throw new AssertionError("Unhandled floating point type: " + type);
          }
        return result;
        }

      default:
        throw new AssertionError("Unhandled type: " + type);
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
        || (n instanceof BigDecimal && ((BigDecimal) n).compareTo(BigDecimal.ZERO) == 0)
        || 0 == n.longValue();
  }

  /** Taken from the value analysis CPA. */
  private boolean isNan(NumericValue pValue) {
    Number n = pValue.getNumber();
    return n.equals(Float.NaN) || n.equals(Double.NaN) || NegativeNaN.VALUE.equals(n);
  }

  /** Taken from the value analysis CPA. */
  private boolean isInfinity(NumericValue pValue) {
    Number n = pValue.getNumber();
    return n.equals(Double.POSITIVE_INFINITY)
        || n.equals(Double.NEGATIVE_INFINITY)
        || n.equals(Float.POSITIVE_INFINITY)
        || n.equals(Float.NEGATIVE_INFINITY);
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
}
