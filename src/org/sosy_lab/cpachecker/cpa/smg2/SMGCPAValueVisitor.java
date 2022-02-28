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
import java.math.RoundingMode;
import java.util.ArrayList;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAValueExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue.NegativeNaN;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.BuiltinFloatFunctions;
import org.sosy_lab.cpachecker.util.BuiltinFunctions;
import org.sosy_lab.cpachecker.util.BuiltinOverflowFunctions;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

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

    Value functionValue = handleFunctions(pIastFunctionCallExpression);

    return ImmutableList.of(ValueAndSMGState.of(functionValue, state));
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
        evaluator.readValueWithPointerDereference(
            newState, subscriptValue, subscriptOffset, typeSizeInBits));
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

  /**
   * TODO: this currently only casts values. What happens if i try smth like this:
   *
   * <p>int array[] = ...;
   *
   * <p>char * bla = (char *) array;
   */
  @Override
  public List<ValueAndSMGState> visit(CCastExpression e) throws CPATransferException {
    // Casts are not trivial within SMGs as there might be type reinterpretation used inside the SMGs,
    // but this should be taken care of by the SMGCPAValueExpressionEvaluator and no longer be a problem here!
    // Get the type and value from the nested expression (might be SMG) and cast the value
    // Also most of this code is taken from the value analysis CPA and modified
    CType targetType = e.getExpressionType();
    // We know that there will be only 1 value as a return here
    List<ValueAndSMGState> valuesAndStates = e.getOperand().accept(this);
    // it might be that we cast a value with more than 1 return value (Strings)
    // The most up to date state is the last state (even if currently there is no case in which the
    // states would be different)
    SMGState currentState = valuesAndStates.get(valuesAndStates.size() - 1).getState();
    ImmutableList.Builder<ValueAndSMGState> builder = new ImmutableList.Builder<>();
    for (ValueAndSMGState valueAndState : valuesAndStates) {
      // We don't take the state from these!
      Value value = valueAndState.getValue();
      MachineModel machineModel = evaluator.getMachineModel();

      if (!value.isExplicitlyKnown()) {
        builder.add(
            ValueAndSMGState.of(
                castSymbolicValue(value, targetType, Optional.of(machineModel)), currentState));
        continue;
      }

      // We only use numeric/symbolic/unknown values anyway and we can't cast unknowns
      if (!value.isNumericValue()) {
        logger.logf(
            Level.FINE, "Can not cast C value %s to %s", value.toString(), targetType.toString());
        builder.add(ValueAndSMGState.of(value, state));
        continue;
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
        builder.add(ValueAndSMGState.of(value, state));
        continue;
      }

      builder.add(
          ValueAndSMGState.of(castNumeric(numericValue, type, machineModel, size), currentState));
    }
    return builder.build();
  }

  @Override
  public List<ValueAndSMGState> visit(CFieldReference e) throws CPATransferException {
    // This is the field of a struct/union, so smth like struct.field or struct->field.
    // In the later case its a pointer dereference.
    // Read the value of the field from the object.

    // TODO: padding

    // First we transform x->f into (*x).f per default
    CFieldReference explicitReference = e.withExplicitPointerDereference();

    // Owner expression; the struct/union with this field. Use this to get the address of the
    // general object.
    CExpression ownerExpression = explicitReference.getFieldOwner();
    CType fieldType = SMGCPAValueExpressionEvaluator.getCanonicalType(explicitReference.getExpressionType());
    // For (*pointer).field case or struct.field case the visitor returns the Value for the
    // correct SMGObject (if it exists)
    List<ValueAndSMGState> structValuesAndStates = ownerExpression.accept(this);

    // The most up to date state is always the last one
    SMGState currentState = structValuesAndStates.get(structValuesAndStates.size() - 1).getState();

    ImmutableList.Builder<ValueAndSMGState> builder = new ImmutableList.Builder<>();
    // If the field we want to read is a String, we return each char on its own
    for (ValueAndSMGState valueAndState : structValuesAndStates) {
      // This value is either a AddressValue for pointers i.e. (*struct).field or a general
      // SymbolicValue
      Value structValue = valueAndState.getValue();

      // Now get the offset of the current field
      BigInteger fieldOffset =
          evaluator.getFieldOffsetInBits(
              SMGCPAValueExpressionEvaluator.getCanonicalType(ownerExpression),
              explicitReference.getFieldName());

      // Get the size of the current field depending on its type; in the case of Strings however we want to use Chars
      //if (fieldType instanceof CSimpleType) {
          // TODO: Strings
      //}
      BigInteger sizeOfField =
          evaluator.getBitSizeof(currentState, fieldType);

      // This is either a stack/global variable of the form struct.field or a pointer of the form
      // (*structP).field. The later needs a pointer deref
      if (ownerExpression instanceof CPointerExpression) {
          // In the pointer case, the Value needs to be a AddressExpression
          Preconditions.checkArgument(structValue instanceof AddressExpression);
          AddressExpression addressAndOffsetValue = (AddressExpression) structValue;
          // This AddressExpr theoretically can have a offset
          Value structPointerOffsetExpr = addressAndOffsetValue.getOffset();
          if (!structPointerOffsetExpr.isNumericValue()) {
            // The offset is some non numeric Value and therefore not useable!

          }
          BigInteger finalFieldOffset =
              structPointerOffsetExpr.asNumericValue().bigInteger().add(fieldOffset);

          builder.add(
              evaluator.readValueWithPointerDereference(
                  currentState,
                  addressAndOffsetValue.getMemoryAddress(),
                  finalFieldOffset,
                  sizeOfField));

      } else if (ownerExpression instanceof CBinaryExpression
          || ownerExpression instanceof CIdExpression) {
        // In the non pointer case the Value is some SymbolicValue with the correct variable
        // identifier String inside its MemoryLocation
        Preconditions.checkArgument(structValue instanceof SymbolicValue);
        MemoryLocation maybeVariableIdent =
            ((SymbolicValue) structValue).getRepresentedLocation().orElseThrow();

          BigInteger finalFieldOffset = fieldOffset;
          if (maybeVariableIdent.isReference()) {
            finalFieldOffset = fieldOffset.add(BigInteger.valueOf(maybeVariableIdent.getOffset()));
          }

          builder.add(
              evaluator.readStackOrGlobalVariable(
                  currentState, maybeVariableIdent.getIdentifier(), finalFieldOffset, sizeOfField));

      } else {
          // TODO: improve error and check if its even needed
          throw new SMG2Exception("Unknown field type in field expression.");
      }
    }
    return builder.build();
  }

  @Override
  public List<ValueAndSMGState> visit(CIdExpression e) throws CPATransferException {
    // essentially stack or global variables
    // Either CEnumerator, CVariableDeclaration, CParameterDeclaration
    // Could also be a type/function declaration, one if which is malloc.
    // We either read the stack/global variable for non pointer and non struct/unions, or package it
    // in a AddressExpression for pointers
    // or SymbolicValue with a memory location and the name of the variable inside of that.

    CSimpleDeclaration varDecl = e.getDeclaration();
    CType type = SMGCPAValueExpressionEvaluator.getCanonicalType(e.getExpressionType());
    if (varDecl == null) {
      // The var was not declared
      SMGState errorState = state.withUndeclaredVariableUsage(e);
      throw new SMG2Exception(errorState);
    }

    if (SMGCPAValueExpressionEvaluator.isStructOrUnionType(type)) {
      // Struct/Unions on the stack/global; return the identifier in a symbolic value
      return ImmutableList.of(
          ValueAndSMGState.of(
              SymbolicValueFactory.getInstance()
                  .newIdentifier(MemoryLocation.forIdentifier(varDecl.getQualifiedName())),
              state));

    } else if (SMGCPAValueExpressionEvaluator.isAddressType(type)) {
      // Pointer/Array/Function types should return a Value that internally can be translated into a
      // SMGValue that leads to a SMGPointsToEdge that leads to the correct object (with potential
      // offsets inside of the points to edge). These have to be packaged into a AddressExpression
      // with an 0 offset. Modifications of the offset of the address can be done by subsequent
      // methods. (The check is fine because we already filtered out structs/unions)
      BigInteger sizeInBits = evaluator.getBitSizeof(state, e.getExpressionType());
      // Now use the qualified name to get the actual global/stack memory location
      ValueAndSMGState readValueAndState =
          evaluator.readStackOrGlobalVariable(
              state, varDecl.getQualifiedName(), BigInteger.ZERO, sizeInBits);

      Value addressValue = AddressExpression.withZeroOffset(readValueAndState.getValue(), type);

      return ImmutableList.of(ValueAndSMGState.of(addressValue, readValueAndState.getState()));

    } else {
      // Everything else should be readable and returnable directly; just return the Value
      BigInteger sizeInBits = evaluator.getBitSizeof(state, e.getExpressionType());
      // Now use the qualified name to get the actual global/stack memory location
      return ImmutableList.of(
          evaluator.readStackOrGlobalVariable(
              state, varDecl.getQualifiedName(), BigInteger.ZERO, sizeInBits));
    }
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
    for (int i = 0; i < string.length(); i++) {
      builder.add(ValueAndSMGState.of(new NumericValue((int) string.charAt(i)), state));
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
    // This should return a AddressExpression as we always evaluate to the address, but only
    // dereference and read it if its not a struct/union as those will be dereferenced by the field
    // expression

    // Get the type of the target
    CType type = SMGCPAValueExpressionEvaluator.getCanonicalType(e.getExpressionType());
    // Get the expression that is dereferenced
    CExpression expr = e.getOperand();
    // Evaluate the expression to a Value; this should return a Symbolic Value with the address of
    // the target and a offset. If this fails this returns a UnknownValue
    List<ValueAndSMGState> evaluatedExpr = expr.accept(this);
    // Take the last state as thats the most up to date one
    SMGState currentState = evaluatedExpr.get(evaluatedExpr.size() - 1).getState();
    ImmutableList.Builder<ValueAndSMGState> builder = new ImmutableList.Builder<>();
    // This loop only has more than 1 iteration if its a String
    for (ValueAndSMGState valueAndState : evaluatedExpr) {
      // Try to disassemble the values (AddressExpression)
      Value value = valueAndState.getValue();
      Preconditions.checkArgument(value instanceof AddressExpression);
      AddressExpression pointerValue = (AddressExpression) value;

      // The offset part of the pointer; its either numeric or we can't get a concrete value
      Value offset = pointerValue.getOffset();
      if (!offset.isNumericValue()) {
        // If the offset is not numericly known we can't read a value, return unknown
        builder.add(ValueAndSMGState.ofUnknownValue(currentState));
        continue;
      }

      if (SMGCPAValueExpressionEvaluator.isStructOrUnionType(type)) {
        // We don't want to read struct/union! In those cases we return the AddressExpression
        // such that the following visitor methods can dereference the fields correctly
        builder.add(ValueAndSMGState.of(value, currentState));

      } else if (type instanceof CArrayType) {
        // For arrays we want to actually read the values at the addresses

        // TODO: if String type, use char and eval each on its own for the size of the String
        // For Strings the offset changes obviously
        BigInteger sizeInBits = evaluator.getBitSizeof(currentState, type);
        BigInteger offsetInBits = offset.asNumericValue().bigInteger();

        // Dereference the Value and return it. The read checks for validity etc.
        ValueAndSMGState readArray =
            evaluator.readValueWithPointerDereference(
                currentState, pointerValue.getMemoryAddress(), offsetInBits, sizeInBits);
        currentState = readArray.getState();
        builder.add(readArray);

      } else if (type instanceof CPointerType
          || (type instanceof CFunctionType
              && expr instanceof CUnaryExpression
              && ((CUnaryExpression) expr).getOperator() == CUnaryExpression.UnaryOperator.AMPER)) {
        // Special cases
        // TODO:
      }
    }
    return builder.build();
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
            // TODO: Think of floating point types!
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

  // +++++++++++++++++++ Below this point methods for handling functions

  private boolean isUnspecifiedType(CType pType) {
    return pType instanceof CSimpleType
        && ((CSimpleType) pType).getType() == CBasicType.UNSPECIFIED;
  }

  private Value fmin(Number pOp1, Number pOp2) {
    if (Double.isNaN(pOp1.doubleValue())
        || (Double.isInfinite(pOp1.doubleValue()) && pOp1.doubleValue() > 0)
        || (Double.isInfinite(pOp2.doubleValue()) && pOp2.doubleValue() < 0)) {
      return new NumericValue(pOp2);
    }
    if (Double.isNaN(pOp2.doubleValue())
        || (Double.isInfinite(pOp2.doubleValue()) && pOp2.doubleValue() > 0)
        || (Double.isInfinite(pOp1.doubleValue()) && pOp1.doubleValue() < 0)) {
      return new NumericValue(pOp1);
    }

    final BigDecimal op1bd;
    final BigDecimal op2bd;

    if (pOp1 instanceof BigDecimal) {
      op1bd = (BigDecimal) pOp1;
    } else {
      op1bd = BigDecimal.valueOf(pOp1.doubleValue());
    }
    if (pOp2 instanceof BigDecimal) {
      op2bd = (BigDecimal) pOp2;
    } else {
      op2bd = BigDecimal.valueOf(pOp2.doubleValue());
    }

    if (op1bd.compareTo(op2bd) < 0) {
      return new NumericValue(op1bd);
    }
    return new NumericValue(op2bd);
  }

  private Value fdim(Number pOp1, Number pOp2, String pFunctionName) {
    if (Double.isNaN(pOp1.doubleValue()) || Double.isNaN(pOp2.doubleValue())) {
      return new NumericValue(Double.NaN);
    }

    if (Double.isInfinite(pOp1.doubleValue())) {
      if (Double.isInfinite(pOp2.doubleValue())) {
        if (pOp1.doubleValue() > pOp2.doubleValue()) {
          return new NumericValue(pOp1.doubleValue() - pOp2.doubleValue());
        }
        return new NumericValue(0.0);
      }
      if (pOp1.doubleValue() < 0) {
        return new NumericValue(0.0);
      }
      return new NumericValue(pOp1);
    }
    if (Double.isInfinite(pOp2.doubleValue())) {
      if (pOp2.doubleValue() < 0) {
        return new NumericValue(Double.NaN);
      }
      return new NumericValue(0.0);
    }

    final BigDecimal op1bd;
    final BigDecimal op2bd;

    if (pOp1 instanceof BigDecimal) {
      op1bd = (BigDecimal) pOp1;
    } else {
      op1bd = BigDecimal.valueOf(pOp1.doubleValue());
    }
    if (pOp2 instanceof BigDecimal) {
      op2bd = (BigDecimal) pOp2;
    } else {
      op2bd = BigDecimal.valueOf(pOp2.doubleValue());
    }
    if (op1bd.compareTo(op2bd) > 0) {
      BigDecimal difference = op1bd.subtract(op2bd);

      CSimpleType type = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      BigDecimal maxValue;
      switch (type.getType()) {
        case FLOAT:
          maxValue = BigDecimal.valueOf(Float.MAX_VALUE);
          break;
        case DOUBLE:
          maxValue = BigDecimal.valueOf(Double.MAX_VALUE);
          break;
        default:
          return Value.UnknownValue.getInstance();
      }
      if (difference.compareTo(maxValue) > 0) {
        return new NumericValue(Double.POSITIVE_INFINITY);
      }
      return new NumericValue(difference);
    }
    return new NumericValue(0.0);
  }

  private Optional<Boolean> isNegative(Number pNumber) {
    if (pNumber instanceof BigDecimal) {
      return Optional.of(((BigDecimal) pNumber).signum() < 0);
    } else if (pNumber instanceof Float) {
      float number = pNumber.floatValue();
      if (Float.isNaN(number)) {
        return Optional.of(false);
      }
      return Optional.of(number < 0 || 1 / number < 0);
    } else if (pNumber instanceof Double) {
      double number = pNumber.doubleValue();
      if (Double.isNaN(number)) {
        return Optional.of(false);
      }
      return Optional.of(number < 0 || 1 / number < 0);
    } else if (pNumber instanceof NegativeNaN) {
      return Optional.of(true);
    }
    return Optional.empty();
  }

  private Value fmax(Number pOp1, Number pOp2) {
    if (Double.isNaN(pOp1.doubleValue())
        || (Double.isInfinite(pOp1.doubleValue()) && pOp1.doubleValue() < 0)
        || (Double.isInfinite(pOp2.doubleValue()) && pOp2.doubleValue() > 0)) {
      return new NumericValue(pOp2);
    }
    if (Double.isNaN(pOp2.doubleValue())
        || (Double.isInfinite(pOp2.doubleValue()) && pOp2.doubleValue() < 0)
        || (Double.isInfinite(pOp1.doubleValue()) && pOp1.doubleValue() > 0)) {
      return new NumericValue(pOp1);
    }

    final BigDecimal op1bd;
    final BigDecimal op2bd;

    if (pOp1 instanceof BigDecimal) {
      op1bd = (BigDecimal) pOp1;
    } else {
      op1bd = BigDecimal.valueOf(pOp1.doubleValue());
    }
    if (pOp2 instanceof BigDecimal) {
      op2bd = (BigDecimal) pOp2;
    } else {
      op2bd = BigDecimal.valueOf(pOp2.doubleValue());
    }

    if (op1bd.compareTo(op2bd) > 0) {
      return new NumericValue(op1bd);
    }
    return new NumericValue(op2bd);
  }

  private Value handleFunctions(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    CExpression functionNameExp = pIastFunctionCallExpression.getFunctionNameExpression();

    // We only handle builtin functions
    if (functionNameExp instanceof CIdExpression) {
      String calledFunctionName = ((CIdExpression) functionNameExp).getName();

      if (BuiltinFunctions.isBuiltinFunction(calledFunctionName)) {
        CType functionType = BuiltinFunctions.getFunctionType(calledFunctionName);

        if (isUnspecifiedType(functionType)) {
          // unsupported formula
          return Value.UnknownValue.getInstance();
        }

        List<CExpression> parameterExpressions =
            pIastFunctionCallExpression.getParameterExpressions();
        List<Value> parameterValues = new ArrayList<>(parameterExpressions.size());

        for (CExpression currParamExp : parameterExpressions) {
          // Here we expect only 1 result value
          List<ValueAndSMGState> newValuesAndStates = currParamExp.accept(this);
          Preconditions.checkArgument(newValuesAndStates.size() == 1);
          Value newValue = newValuesAndStates.get(0).getValue();

          parameterValues.add(newValue);
        }

        if (BuiltinOverflowFunctions.isBuiltinOverflowFunction(calledFunctionName)) {
          /*
           * Problem: this method needs a AbstractExpressionValueVisitor as input (the this)
           * but this class is not correctly abstracted such that i can inherit it here
           * (because it essentially is the same except for 1 method that would need to be
           * abstract)
           *
           * return BuiltinOverflowFunctions.evaluateFunctionCall(
           *   pIastFunctionCallExpression, this, evaluator.getMachineModel(), logger);
           */
          return UnknownValue.getInstance();
        } else if (BuiltinFloatFunctions.matchesAbsolute(calledFunctionName)) {
          assert parameterValues.size() == 1;

          final CType parameterType = parameterExpressions.get(0).getExpressionType();
          final Value parameter = parameterValues.get(0);

          if (parameterType instanceof CSimpleType && !((CSimpleType) parameterType).isSigned()) {
            return parameter;

          } else if (parameter.isExplicitlyKnown()) {
            assert parameter.isNumericValue();
            final double absoluteValue = Math.abs(((NumericValue) parameter).doubleValue());

            // absolute value for INT_MIN is undefined behaviour, so we do not bother handling it
            // in any specific way
            return new NumericValue(absoluteValue);
          }

        } else if (BuiltinFloatFunctions.matchesHugeVal(calledFunctionName)
            || BuiltinFloatFunctions.matchesInfinity(calledFunctionName)) {

          assert parameterValues.isEmpty();
          if (BuiltinFloatFunctions.matchesHugeValFloat(calledFunctionName)
              || BuiltinFloatFunctions.matchesInfinityFloat(calledFunctionName)) {

            return new NumericValue(Float.POSITIVE_INFINITY);

          } else {
            assert BuiltinFloatFunctions.matchesInfinityDouble(calledFunctionName)
                    || BuiltinFloatFunctions.matchesInfinityLongDouble(calledFunctionName)
                    || BuiltinFloatFunctions.matchesHugeValDouble(calledFunctionName)
                    || BuiltinFloatFunctions.matchesHugeValLongDouble(calledFunctionName)
                : " Unhandled builtin function for infinity: " + calledFunctionName;

            return new NumericValue(Double.POSITIVE_INFINITY);
          }

        } else if (BuiltinFloatFunctions.matchesNaN(calledFunctionName)) {
          assert parameterValues.isEmpty() || parameterValues.size() == 1;

          if (BuiltinFloatFunctions.matchesNaNFloat(calledFunctionName)) {
            return new NumericValue(Float.NaN);
          } else {
            assert BuiltinFloatFunctions.matchesNaNDouble(calledFunctionName)
                    || BuiltinFloatFunctions.matchesNaNLongDouble(calledFunctionName)
                : "Unhandled builtin function for NaN: " + calledFunctionName;

            return new NumericValue(Double.NaN);
          }
        } else if (BuiltinFloatFunctions.matchesIsNaN(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              NumericValue numericValue = value.asNumericValue();
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName);
              switch (paramType.getType()) {
                case FLOAT:
                  return Float.isNaN(numericValue.floatValue())
                      ? new NumericValue(1)
                      : new NumericValue(0);
                case DOUBLE:
                  return Double.isNaN(numericValue.doubleValue())
                      ? new NumericValue(1)
                      : new NumericValue(0);
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesIsInfinity(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              NumericValue numericValue = value.asNumericValue();
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName);
              switch (paramType.getType()) {
                case FLOAT:
                  return Float.isInfinite(numericValue.floatValue())
                      ? new NumericValue(1)
                      : new NumericValue(0);
                case DOUBLE:
                  return Double.isInfinite(numericValue.doubleValue())
                      ? new NumericValue(1)
                      : new NumericValue(0);
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFinite(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              NumericValue numericValue = value.asNumericValue();
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName);
              switch (paramType.getType()) {
                case FLOAT:
                  return Float.isInfinite(numericValue.floatValue())
                      ? new NumericValue(0)
                      : new NumericValue(1);
                case DOUBLE:
                  return Double.isInfinite(numericValue.doubleValue())
                      ? new NumericValue(0)
                      : new NumericValue(1);
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFloor(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);

            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return new NumericValue(((BigDecimal) number).setScale(0, RoundingMode.FLOOR));
              } else if (number instanceof Float) {
                return new NumericValue(Math.floor(number.floatValue()));
              } else if (number instanceof Double) {
                return new NumericValue(Math.floor(number.doubleValue()));
              } else if (number instanceof NumericValue.NegativeNaN) {
                return parameter;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesCeil(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);

            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return new NumericValue(((BigDecimal) number).setScale(0, RoundingMode.CEILING));
              } else if (number instanceof Float) {
                return new NumericValue(Math.ceil(number.floatValue()));
              } else if (number instanceof Double) {
                return new NumericValue(Math.ceil(number.doubleValue()));
              } else if (number instanceof NumericValue.NegativeNaN) {
                return parameter;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesRound(calledFunctionName)
            || BuiltinFloatFunctions.matchesLround(calledFunctionName)
            || BuiltinFloatFunctions.matchesLlround(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);
            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return new NumericValue(((BigDecimal) number).setScale(0, RoundingMode.HALF_UP));
              } else if (number instanceof Float) {
                float f = number.floatValue();
                if (0 == f || Float.isInfinite(f)) {
                  return parameter;
                }
                return new NumericValue(Math.round(f));
              } else if (number instanceof Double) {
                double d = number.doubleValue();
                if (0 == d || Double.isInfinite(d)) {
                  return parameter;
                }
                return new NumericValue(Math.round(d));
              } else if (number instanceof NumericValue.NegativeNaN) {
                return parameter;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesTrunc(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);
            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return new NumericValue(((BigDecimal) number).setScale(0, RoundingMode.DOWN));
              } else if (number instanceof Float) {
                float f = number.floatValue();
                if (0 == f || Float.isInfinite(f) || Float.isNaN(f)) {
                  // +/-0.0 and +/-INF and +/-NaN are returned unchanged
                  return parameter;
                }
                return new NumericValue(
                    BigDecimal.valueOf(number.floatValue())
                        .setScale(0, RoundingMode.DOWN)
                        .floatValue());
              } else if (number instanceof Double) {
                double d = number.doubleValue();
                if (0 == d || Double.isInfinite(d) || Double.isNaN(d)) {
                  // +/-0.0 and +/-INF and +/-NaN are returned unchanged
                  return parameter;
                }
                return new NumericValue(
                    BigDecimal.valueOf(number.doubleValue())
                        .setScale(0, RoundingMode.DOWN)
                        .doubleValue());
              } else if (number instanceof NumericValue.NegativeNaN) {
                return parameter;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFdim(calledFunctionName)) {
          if (parameterValues.size() == 2) {
            Value operand1 = parameterValues.get(0);
            Value operand2 = parameterValues.get(1);
            if (operand1.isExplicitlyKnown() && operand2.isExplicitlyKnown()) {

              assert operand1.isNumericValue();
              assert operand2.isNumericValue();

              Number op1 = operand1.asNumericValue().getNumber();
              Number op2 = operand2.asNumericValue().getNumber();

              Value result = fdim(op1, op2, calledFunctionName);
              if (!Value.UnknownValue.getInstance().equals(result)) {
                return result;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFmax(calledFunctionName)) {
          if (parameterValues.size() == 2) {
            Value operand1 = parameterValues.get(0);
            Value operand2 = parameterValues.get(1);
            if (operand1.isExplicitlyKnown() && operand2.isExplicitlyKnown()) {

              assert operand1.isNumericValue();
              assert operand2.isNumericValue();

              Number op1 = operand1.asNumericValue().getNumber();
              Number op2 = operand2.asNumericValue().getNumber();

              return fmax(op1, op2);
            }
          }
        } else if (BuiltinFloatFunctions.matchesFmin(calledFunctionName)) {
          if (parameterValues.size() == 2) {
            Value operand1 = parameterValues.get(0);
            Value operand2 = parameterValues.get(1);
            if (operand1.isExplicitlyKnown() && operand2.isExplicitlyKnown()) {

              assert operand1.isNumericValue();
              assert operand2.isNumericValue();

              Number op1 = operand1.asNumericValue().getNumber();
              Number op2 = operand2.asNumericValue().getNumber();

              return fmin(op1, op2);
            }
          }
        } else if (BuiltinFloatFunctions.matchesSignbit(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);

            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              Optional<Boolean> isNegative = isNegative(number);
              if (isNegative.isPresent()) {
                return new NumericValue(isNegative.orElseThrow() ? 1 : 0);
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesCopysign(calledFunctionName)) {
          if (parameterValues.size() == 2) {
            Value target = parameterValues.get(0);
            Value source = parameterValues.get(1);
            if (target.isExplicitlyKnown() && source.isExplicitlyKnown()) {
              assert target.isNumericValue();
              assert source.isNumericValue();
              Number targetNumber = target.asNumericValue().getNumber();
              Number sourceNumber = source.asNumericValue().getNumber();
              Optional<Boolean> sourceNegative = isNegative(sourceNumber);
              Optional<Boolean> targetNegative = isNegative(targetNumber);
              if (sourceNegative.isPresent() && targetNegative.isPresent()) {
                if (sourceNegative.orElseThrow().equals(targetNegative.orElseThrow())) {
                  return new NumericValue(targetNumber);
                }
                return target.asNumericValue().negate();
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFloatClassify(calledFunctionName)) {

          if (parameterValues.size() == 1) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              NumericValue numericValue = value.asNumericValue();
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName);
              switch (paramType.getType()) {
                case FLOAT:
                  {
                    float v = numericValue.floatValue();
                    if (Float.isNaN(v)) {
                      return new NumericValue(0);
                    }
                    if (Float.isInfinite(v)) {
                      return new NumericValue(1);
                    }
                    if (v == 0.0) {
                      return new NumericValue(2);
                    }
                    if (Float.toHexString(v).startsWith("0x0.")) {
                      return new NumericValue(3);
                    }
                    return new NumericValue(4);
                  }
                case DOUBLE:
                  {
                    double v = numericValue.doubleValue();
                    if (Double.isNaN(v)) {
                      return new NumericValue(0);
                    }
                    if (Double.isInfinite(v)) {
                      return new NumericValue(1);
                    }
                    if (v == 0.0) {
                      return new NumericValue(2);
                    }
                    if (Double.toHexString(v).startsWith("0x0.")) {
                      return new NumericValue(3);
                    }
                    return new NumericValue(4);
                  }
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesModf(calledFunctionName)) {
          if (parameterValues.size() == 2) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              NumericValue numericValue = value.asNumericValue();
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName);
              switch (paramType.getType()) {
                case FLOAT:
                  {
                    long integralPart = (long) numericValue.floatValue();
                    float fractionalPart = numericValue.floatValue() - integralPart;
                    return new NumericValue(fractionalPart);
                  }
                case DOUBLE:
                  {
                    long integralPart = (long) numericValue.doubleValue();
                    double fractionalPart = numericValue.doubleValue() - integralPart;
                    return new NumericValue(fractionalPart);
                  }
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFremainder(calledFunctionName)) {
          if (parameterValues.size() == 2) {
            Value numer = parameterValues.get(0);
            Value denom = parameterValues.get(1);
            if (numer.isExplicitlyKnown() && denom.isExplicitlyKnown()) {
              NumericValue numerValue = numer.asNumericValue();
              NumericValue denomValue = denom.asNumericValue();
              switch (BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName)
                  .getType()) {
                case FLOAT:
                  {
                    float num = numerValue.floatValue();
                    float den = denomValue.floatValue();
                    if (Float.isNaN(num) || Float.isNaN(den) || Float.isInfinite(num) || den == 0) {
                      return new NumericValue(Float.NaN);
                    }
                    return new NumericValue((float) Math.IEEEremainder(num, den));
                  }
                case DOUBLE:
                  {
                    double num = numerValue.doubleValue();
                    double den = denomValue.doubleValue();
                    if (Double.isNaN(num)
                        || Double.isNaN(den)
                        || Double.isInfinite(num)
                        || den == 0) {
                      return new NumericValue(Double.NaN);
                    }
                    return new NumericValue(Math.IEEEremainder(num, den));
                  }
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFmod(calledFunctionName)) {
          if (parameterValues.size() == 2) {
            Value numer = parameterValues.get(0);
            Value denom = parameterValues.get(1);
            if (numer.isExplicitlyKnown() && denom.isExplicitlyKnown()) {
              NumericValue numerValue = numer.asNumericValue();
              NumericValue denomValue = denom.asNumericValue();
              switch (BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName)
                  .getType()) {
                case FLOAT:
                  {
                    float num = numerValue.floatValue();
                    float den = denomValue.floatValue();
                    if (Float.isNaN(num) || Float.isNaN(den) || Float.isInfinite(num) || den == 0) {
                      return new NumericValue(Float.NaN);
                    }
                    if (num == 0 && den != 0) {
                      // keep the sign on +0 and -0
                      return numer;
                    }
                    // TODO computations on float/double are imprecise! Use epsilon environment?
                    return new NumericValue(num % den);
                  }
                case DOUBLE:
                  {
                    double num = numerValue.doubleValue();
                    double den = denomValue.doubleValue();
                    if (Double.isNaN(num)
                        || Double.isNaN(den)
                        || Double.isInfinite(num)
                        || den == 0) {
                      return new NumericValue(Double.NaN);
                    }
                    if (num == 0 && den != 0) {
                      // keep the sign on +0 and -0
                      return numer;
                    }
                    // TODO computations on float/double are imprecise! Use epsilon environment?
                    return new NumericValue(num % den);
                  }
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesIsgreater(calledFunctionName)) {
          Value op1 = parameterValues.get(0);
          Value op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return new NumericValue(num1 > num2 ? 1 : 0);
          }
        } else if (BuiltinFloatFunctions.matchesIsgreaterequal(calledFunctionName)) {
          Value op1 = parameterValues.get(0);
          Value op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return new NumericValue(num1 >= num2 ? 1 : 0);
          }
        } else if (BuiltinFloatFunctions.matchesIsless(calledFunctionName)) {
          Value op1 = parameterValues.get(0);
          Value op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return new NumericValue(num1 < num2 ? 1 : 0);
          }
        } else if (BuiltinFloatFunctions.matchesIslessequal(calledFunctionName)) {
          Value op1 = parameterValues.get(0);
          Value op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return new NumericValue(num1 <= num2 ? 1 : 0);
          }
        } else if (BuiltinFloatFunctions.matchesIslessgreater(calledFunctionName)) {
          Value op1 = parameterValues.get(0);
          Value op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return new NumericValue(num1 > num2 || num1 < num2 ? 1 : 0);
          }
        } else if (BuiltinFloatFunctions.matchesIsunordered(calledFunctionName)) {
          Value op1 = parameterValues.get(0);
          Value op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return new NumericValue(Double.isNaN(num1) || Double.isNaN(num2) ? 1 : 0);
          }
        }
      }
    }
    return UnknownValue.getInstance();
  }

}
