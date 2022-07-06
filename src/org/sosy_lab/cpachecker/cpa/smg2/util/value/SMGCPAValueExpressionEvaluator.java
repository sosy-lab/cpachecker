// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAAddressVisitor;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPABuiltins;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAExportOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGSizeOfVisitor;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

@SuppressWarnings("unused")
public class SMGCPAValueExpressionEvaluator {

  private final SMGCPAExportOptions exportSMGOptions;
  private final SMGOptions options;
  private final LogManagerWithoutDuplicates logger;
  private final MachineModel machineModel;

  private final SMGCPABuiltins builtins;

  public SMGCPAValueExpressionEvaluator(
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      SMGCPAExportOptions pExportSMGOptions,
      SMGOptions pSMGOptions) {
    logger = pLogger;
    machineModel = pMachineModel;
    exportSMGOptions = pExportSMGOptions;
    options = pSMGOptions;
    builtins = new SMGCPABuiltins(this, options, exportSMGOptions, machineModel, logger);
  }

  public SMGCPABuiltins getBuiltinFunctionHandler() {
    return builtins;
  }

  private Value throwUnsupportedOperationException(String methodNameString) {
    throw new AssertionError("The operation " + methodNameString + " is not yet supported.");
  }

  public boolean isPointerValue(Value maybeAddress, SMGState currentState) {
    return currentState.getMemoryModel().isPointer(maybeAddress);
  }

  public List<ValueAndSMGState> handleSafeExternFunction(
      CFunctionCallExpression pFunctionCallExpression, SMGState pSmgState, CFAEdge pCfaEdge)
      throws CPATransferException {
    /* TODO:
     * addExternalAllocation allocates external heap and returns the pointer value to it
    String calledFunctionName = pFunctionCallExpression.getFunctionNameExpression().toString();
    List<CExpression> parameters = pFunctionCallExpression.getParameterExpressions();
    for (int i = 0; i < parameters.size(); i++) {
      CExpression param = parameters.get(i);
      CType paramType = TypeUtils.getRealExpressionType(param);
      if (paramType instanceof CPointerType || paramType instanceof CArrayType) {
        // assign external value to param
        for (SMGAddressValueAndState addressOfFieldAndState :
            evaluateAddress(pSmgState, pCfaEdge, param)) {
          SMGAddress smgAddress = addressOfFieldAndState.getObject().getAddress();

          // Check that write will be correct
          if (!smgAddress.isUnknown()) {
            SMGObject object = smgAddress.getObject();
            SMGExplicitValue offset = smgAddress.getOffset();
            SMGState smgState = addressOfFieldAndState.getSmgState();
            if (!object.equals(SMGNullObject.INSTANCE)
                && object.getSize() - offset.getAsLong() >= machineModel.getSizeofPtrInBits()
                && (smgState.getHeap().isObjectValid(object)
                    || smgState.getHeap().isObjectExternallyAllocated(object))) {

              SMGEdgePointsTo newParamValue =
                  pSmgState.addExternalAllocation(
                      calledFunctionName + "_Param_No_" + i + "_ID" + SMGCPA.getNewValue());
              pSmgState =
                  assignFieldToState(
                      pSmgState,
                      pCfaEdge,
                      object,
                      offset.getAsLong(),
                      newParamValue.getValue(),
                      paramType);
            }
          }
        }
      }
    }

    CType returnValueType =
        TypeUtils.getRealExpressionType(pFunctionCallExpression.getExpressionType());
    if (returnValueType instanceof CPointerType || returnValueType instanceof CArrayType) {
      return Collections.singletonList(
          SMGAddressValueAndState.of(
              pSmgState,
              pSmgState.addExternalAllocation(calledFunctionName + SMGCPA.getNewValue())));
    }
    */
    return Collections.singletonList(ValueAndSMGState.ofUnknownValue(pSmgState));
  }

  /**
   * Given 2 address Values, left == right, this checks whether or not they are considered equal in
   * the SPC/SMG of the given state. This returns a Value with the result, which is a boolean (1 or
   * 0). Note: this returns always false (0) if one of the 2 given Values is no valid address.
   *
   * @param leftValue the left hand side address of the equality.
   * @param rightValue the right hand side address of the equality.
   * @param state the current state in which the 2 values are address values.
   * @return a {@link Value} that is either 1 or 0 as true and false result of the equality.
   */
  public Value checkEqualityForAddresses(Value leftValue, Value rightValue, SMGState state) {
    boolean isNotEqual = state.areNonEqualAddresses(leftValue, rightValue);
    return isNotEqual ? new NumericValue(0) : new NumericValue(1);
  }

  /**
   * Given 2 address Values, left != right, this checks whether or not they are considered NOT equal
   * in the SPC/SMG of the given state. This returns a Value with the result, which is a boolean (1
   * or 0). Note: this returns always true (1) if one of the 2 given Values is no valid address.
   *
   * @param leftValue the left hand side address of the inequality.
   * @param rightValue the right hand side address of the inequality.
   * @param state the current state in which the 2 values are address values.
   * @return a {@link Value} that is 1 (true) if the 2 addresses are not equal, 0 (false) if they
   *     are equal.
   */
  public Value checkNonEqualityForAddresses(Value leftValue, Value rightValue, SMGState state) {
    if (leftValue instanceof AddressExpression && rightValue instanceof AddressExpression) {
      // AddressExpressions are basically (pointer + offset). We can compare the base pointer, if
      // those are not equal, no offset will make them equal!
      Value leftMemoryBaseAddress = ((AddressExpression) leftValue).getMemoryAddress();
      Value rightMemoryBaseAddress = ((AddressExpression) rightValue).getMemoryAddress();
      boolean baseIsNotEqual =
          state.areNonEqualAddresses(leftMemoryBaseAddress, rightMemoryBaseAddress);
      if (!baseIsNotEqual) {
        // the base is  equal
        Value leftOffset = ((AddressExpression) leftValue).getOffset();
        Value rightOffset = ((AddressExpression) rightValue).getOffset();
        if (leftOffset.isNumericValue() && rightValue.isNumericValue()) {
          NumericValue leftOffsetNum = leftOffset.asNumericValue();
          NumericValue rightOffsetNum = rightOffset.asNumericValue();
          if (leftOffsetNum.getNumber().equals(rightOffsetNum.getNumber())) {
            // they are truly equal (so they are not not-equal)
            return new NumericValue(0);
          }
        } else if (leftOffset.isUnknown()) {
          if (rightOffset.isUnknown()) {
            // Both offsets are unknown, meaning that if the base address is the same, we can check
            // both cases that they are equal and that they are not.
            // If the base address is not equal, they can never be equal.
            // TODO: cover all cases if an option is on
            return UnknownValue.getInstance();
          }
          // left is unknown, right is concrete. We can assume 2 cases here, 1 where the addresses
          // are equal and the unknown value is == the known, and one where they are not.
          // TODO: cover all cases if an option is on
          return UnknownValue.getInstance();

        } else if (rightOffset.isUnknown()) {
          // Only 1 offset is unknown, this could indicate that the 2 addresses may be the same if
          // they point to the same SMGObject. However they might never be equal if they don't.
          // TODO: cover all cases if an option is on
          return UnknownValue.getInstance();
        }
      }
      // They are not equal
      return new NumericValue(1);
    }
    Preconditions.checkArgument(
        !(leftValue instanceof AddressExpression) && !(rightValue instanceof AddressExpression));
    boolean isNotEqual = state.areNonEqualAddresses(leftValue, rightValue);
    return isNotEqual ? new NumericValue(1) : new NumericValue(0);
  }

  /**
   * Checks if the {@link CType} is a {@link CPointerType}, {@link CArrayType}, {@link
   * CFunctionType} or a Struct/Union type and returns true if its one of these. False else.
   *
   * @param cType the {@link CType} to check.
   * @return true if CPointerType, CArrayType, CFunctionType or a Struct/Union type, false else.
   */
  public static boolean isAddressType(CType cType) {
    CType type = getCanonicalType(cType);
    return type instanceof CPointerType
        || type instanceof CArrayType
        || type instanceof CFunctionType
        || isStructOrUnionType(type);
  }

  public Collection<ValueAndSMGState> evaluateArraySubscriptAddress(
      SMGState pInitialSmgState, CExpression pExp) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Returns the offset in bits of the field in a struct/union type expression. Example:
   * struct.field1 with field 1 being the first field and 4 byte size, struct.field2 being the
   * second field with 4 bytes. The offset of field 1 would be 0, while the second one would be 4 *
   * 8.
   *
   * @param ownerExprType the {@link CType} of the owner of the field.
   * @param pFieldName the name of the field.
   * @return the offset in bits of a the field as a {@link BigInteger}.
   */
  public BigInteger getFieldOffsetInBits(CType ownerExprType, String pFieldName) {
    if (ownerExprType instanceof CElaboratedType) {

      // CElaboratedType is either a struct, union or enum. getRealType returns the correct type
      CType realType = ((CElaboratedType) ownerExprType).getRealType();

      if (realType == null) {
        // TODO: This is possible, i don't know when however, handle once i find out.
        throw new AssertionError();
      }

      return getFieldOffsetInBits(realType, pFieldName);
    } else if (ownerExprType instanceof CCompositeType) {

      // Struct or Union type
      return machineModel.getFieldOffsetInBits((CCompositeType) ownerExprType, pFieldName);
    } else if (ownerExprType instanceof CPointerType) {

      // structPointer -> field or (*structPointer).field
      CType type = getCanonicalType(((CPointerType) ownerExprType).getType());

      return getFieldOffsetInBits(type, pFieldName);
    }

    throw new AssertionError();
  }

  /**
   * Create a new heap object with the size in bits and then create a pointer to its beginning and
   * return the state with the pointer and object + the pointer Value (address to the objects
   * beginning).
   *
   * @param pInitialSmgState initial {@link SMGState}.
   * @param sizeInBits size in bits as {@link BigInteger}.
   * @return the {@link Value} that is the address for the new heap memory region created with the
   *     size and the {@link SMGState} with the region (SMGObject) pointer and address Value added.
   */
  public ValueAndSMGState createHeapMemoryAndPointer(
      SMGState pInitialSmgState, BigInteger sizeInBits) {
    SMGObjectAndSMGState newObjectAndState = pInitialSmgState.copyAndAddHeapObject(sizeInBits);
    SMGObject newObject = newObjectAndState.getSMGObject();
    SMGState newState = newObjectAndState.getState();

    Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    // New regions always have offset 0
    SMGState finalState = newState.createAndAddPointer(addressValue, newObject, BigInteger.ZERO);
    return ValueAndSMGState.of(addressValue, finalState);
  }

  /**
   * This creates or finds and returns the address Value for the underyling expression. This also
   * creates the pointers in the SMG if not yet created. Throws the exception only if either there
   * is no object or if nonsensical addresses are requested; i.e. &3; Used with the & operator for
   * example.
   *
   * @param operand the {@link CExpression} that is the operand of the & expression.
   * @param pState current {@link SMGState}
   * @param cfaEdge debug/logging edge.
   * @return a list of either unknown or a {@link Value} representing the address. This is not a
   *     {@link AddressExpression}!
   * @throws CPATransferException if the & operator is used on a invalid expression.
   */
  public List<ValueAndSMGState> createAddress(CExpression operand, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {
    // SMGCPAAddressVisitor may have side effects! But they should not effect anything as they are
    // only interesing in a failure case in which the analysis stops!
    SMGCPAAddressVisitor addressVisitor = new SMGCPAAddressVisitor(this, pState, cfaEdge, logger);
    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    for (Optional<SMGObjectAndOffset> maybeObjectAndOffset : operand.accept(addressVisitor)) {
      if (maybeObjectAndOffset.isEmpty()) {
        // TODO: improve error handling and add more specific exceptions to the visitor!
        // No address could be found
        throw new SMG2Exception("No address could be created for the expression: " + operand);
      }
      SMGObjectAndOffset targetAndOffset = maybeObjectAndOffset.orElseThrow();
      SMGObject target = targetAndOffset.getSMGObject();
      BigInteger offset = targetAndOffset.getOffsetForObject();
      // search for existing pointer first and return if found; else make a new one
      resultBuilder.add(searchOrCreatePointer(target, offset, pState));
    }
    return resultBuilder.build();
  }

  /**
   * Creates a new pointer (address) pointing to the result of *(targetAddress + offset). First
   * searches for an existing pointer and only creates one if none is found. May return a unknown
   * value with a error state if something goes wrong. Note: the address may have already a offset,
   * this is respected and the new offset is the address offset + the entered offset.
   *
   * @param targetAddress the targets address {@link Value} (Not AddressExpression!) that should be
   *     a valid address leading to a point-to-edge.
   * @param offsetInBits the offset that is added to the address in bits.
   * @param pState current {@link SMGState}.
   * @return either a unknown {@link Value} and a error state, or a valid new {@link Value}
   *     representing a address to the target.
   */
  public ValueAndSMGState findOrcreateNewPointer(
      Value targetAddress, BigInteger offsetInBits, SMGState pState) {
    Preconditions.checkArgument(!(targetAddress instanceof AddressExpression));

    Optional<SMGObjectAndOffset> maybeTargetAndOffset =
        pState.getMemoryModel().dereferencePointer(targetAddress);
    if (maybeTargetAndOffset.isEmpty()) {
      // The value is unknown and therefore does not point to a valid memory location
      SMGState errorState = pState.withUnknownPointerDereferenceWhenReading(targetAddress);
      return ValueAndSMGState.ofUnknownValue(errorState);
    }
    SMGObject object = maybeTargetAndOffset.orElseThrow().getSMGObject();

    // The object may be null if no such object exists, check and log if 0
    if (object.isZero()) {
      SMGState errorState = pState.withNullPointerDereferenceWhenReading(object);
      return ValueAndSMGState.ofUnknownValue(errorState);
    }

    // The offset of the pointer used. (the pointer might point to a offset != 0, the other offset
    // needs to the added to that!)
    BigInteger baseOffset = maybeTargetAndOffset.orElseThrow().getOffsetForObject();
    BigInteger finalOffsetInBits = baseOffset.add(offsetInBits);

    // search for existing pointer first and return if found; else make a new one for the offset
    return searchOrCreatePointer(object, finalOffsetInBits, pState);
  }

  private ValueAndSMGState searchOrCreatePointer(
      SMGObject targetObject, BigInteger offsetInBits, SMGState pState) {
    // search for existing pointer first and return if found
    Optional<SMGValue> maybeAddressValue =
        pState.getMemoryModel().getAddressValueForPointsToTarget(targetObject, offsetInBits);

    if (maybeAddressValue.isPresent()) {
      Optional<Value> valueForSMGValue =
          pState.getMemoryModel().getValueFromSMGValue(maybeAddressValue.orElseThrow());
      // Reuse pointer; there should never be a SMGValue without counterpart!
      // TODO: this might actually be expensive, check once this runs!
      return ValueAndSMGState.of(valueForSMGValue.orElseThrow(), pState);
    }

    Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    SMGState newState = pState.createAndAddPointer(addressValue, targetObject, offsetInBits);
    return ValueAndSMGState.of(addressValue, newState);
  }

  /**
   * This creates or finds and returns the address Value for the given local or global variable.
   * This also creates the pointers in the SMG if not yet created. This is mainly used for the &
   * operator.
   *
   * @param variableName the variable name. The variable should exists, else an exception is thrown.
   * @param pState current {@link SMGState}
   * @return either unknown or a {@link Value} representing the address.
   * @throws CPATransferException if the & operator is used on a invalid expression.
   */
  public ValueAndSMGState createAddressForLocalOrGlobalVariable(
      String variableName, SMGState pState) throws CPATransferException {
    // Get the variable SMGObject
    Optional<SMGObjectAndOffset> maybeObjectAndOffset =
        getTargetObjectAndOffset(pState, variableName, BigInteger.ZERO);
    if (maybeObjectAndOffset.isEmpty()) {
      // TODO: improve error handling and add more specific exceptions to the visitor!
      // No address could be found
      throw new SMG2Exception("No address could be created for the variable: " + variableName);
    }
    SMGObjectAndOffset targetAndOffset = maybeObjectAndOffset.orElseThrow();
    SMGObject target = targetAndOffset.getSMGObject();
    BigInteger offset = targetAndOffset.getOffsetForObject();
    // search for existing pointer first and return if found
    Optional<SMGValue> maybeAddressValue =
        pState.getMemoryModel().getAddressValueForPointsToTarget(target, offset);

    if (maybeAddressValue.isPresent()) {
      Optional<Value> valueForSMGValue =
          pState.getMemoryModel().getValueFromSMGValue(maybeAddressValue.orElseThrow());
      // Reuse pointer; there should never be a SMGValue without counterpart!
      // TODO: this might actually be expensive, check once this runs!
      return ValueAndSMGState.of(valueForSMGValue.orElseThrow(), pState);
    }

    // If none is found, we need a new Value -> SMGValue mapping for the address + a new
    // PointsToEdge with the correct offset
    Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    SMGState newState = pState.createAndAddPointer(addressValue, target, offset);
    return ValueAndSMGState.of(addressValue, newState);
  }

  public SMGState addValueToState(SMGState pState, Value value) {
    return pState.copyAndAddValue(value).getSMGState();
  }

  /**
   * Creates a memory region on the stack with the size entered. This region is not meant to be used
   * by the entered internal name, but by the returned pointer only. This is needed because of the
   * alloca C function. The internal name needs to be unique. It should be chosen such that it is
   * not accidentally used and reference the function/method that it stems from.
   *
   * @param internalName internal name that should be unique.
   * @param sizeInBits size in bits for the memory region created.
   * @return the Value leading to the created region (pointer) and the state with the pointer, value
   *     and the region added.
   * @throws CPATransferException may be thrown if i.e. there is no stackframe or the variable was
   *     not found (should never be possible).
   */
  public ValueAndSMGState createStackAllocation(
      String internalName, BigInteger sizeInBits, SMGState pState) throws CPATransferException {
    Preconditions.checkArgument(
        !pState.getMemoryModel().getStackFrames().peek().containsVariable(internalName));
    return createAddressForLocalOrGlobalVariable(
        internalName, pState.copyAndAddLocalVariable(sizeInBits, internalName));
  }

  /**
   * Read the global or stack variable whoes name is given as a {@link String} from the given {@link
   * SMGState} with the given size in bits as a {@link BigInteger}. The Value might be unknown
   * either because it was read as unknown or because the variable was not initialized. Note: this
   * expects that the given {@link CIdExpression} has a not null declaration.
   *
   * @param initialState the {@link SMGState} holding the {@link SymbolicProgramConfiguration} where
   *     the variable should be read from.
   * @param varName name of the global or stack variable to be read.
   * @param sizeInBits size of the type to be read.
   * @return {@link ValueAndSMGState} with the updated {@link SMGState} and the read {@link Value}.
   *     The Value might be unknown either because it was read as unknown or because the variable
   *     was not initialized.
   * @throws CPATransferException if the variable is not known in the memory model (SPC)
   */
  public ValueAndSMGState readStackOrGlobalVariable(
      SMGState initialState, String varName, BigInteger offsetInBits, BigInteger sizeInBits)
      throws CPATransferException {

    Optional<SMGObject> maybeObject =
        initialState.getMemoryModel().getObjectForVisibleVariable(varName);

    if (maybeObject.isEmpty()) {
      // If there is no object, the variable is not initialized
      SMGState errorState = initialState.withUninitializedVariableUsage(varName);
      throw new SMG2Exception(errorState);
    }
    return readValue(initialState, maybeObject.orElseThrow(), offsetInBits, sizeInBits);
  }

  /**
   * Read the value at the address of the supplied {@link Value} at the offset with the size (type
   * size) given.
   *
   * @param pState current {@link SMGState}.
   * @param value the {@link Value} for the address of the memory to be read. This should map to a
   *     known {@link SMGObject} or a {@link SMGPointsToEdge}.
   * @param pOffset the offset as {@link BigInteger} in bits where to start reading in the object.
   * @param pSizeInBits the size of the type to read in bits as {@link BigInteger}.
   * @return {@link ValueAndSMGState} tuple for the read {@link Value} and the new {@link SMGState}.
   */
  public ValueAndSMGState readValueWithPointerDereference(
      SMGState pState, Value value, BigInteger pOffset, BigInteger pSizeInBits)
      throws SMG2Exception {
    // Get the SMGObject for the value
    Optional<SMGObjectAndOffset> maybeTargetAndOffset =
        pState.getMemoryModel().dereferencePointer(value);
    if (maybeTargetAndOffset.isEmpty()) {
      // The value is unknown and therefore does not point to a valid memory location
      SMGState errorState = pState.withUnknownPointerDereferenceWhenReading(value);
      throw new SMG2Exception(errorState);
    }
    SMGObject object = maybeTargetAndOffset.orElseThrow().getSMGObject();

    // The object may be null if no such object exists, check and log if 0
    if (object.isZero()) {
      SMGState errorState = pState.withNullPointerDereferenceWhenReading(object);
      throw new SMG2Exception(errorState);
    }

    // The offset of the pointer used. (the pointer might point to a offset != 0, the other offset
    // needs to the added to that!)
    BigInteger baseOffset = maybeTargetAndOffset.orElseThrow().getOffsetForObject();
    BigInteger offset = baseOffset.add(pOffset);

    return readValue(pState, object, offset, pSizeInBits);
  }

  /**
   * Returns the {@link SMGObjectAndOffset} pair for the entered address {@link Value} and
   * additional offset on the entered state. This does not really dereference the address {@link
   * Value}, think of it like &*pointer.
   *
   * @param pState current {@link SMGState}.
   * @param value {@link Value} pointer to be dereferenced leading to the {@link SMGObject} desired.
   * @param pOffsetInBits used offset when dereferencing.
   * @return the desired {@link SMGObject} and its offset or an empty optional.
   * @throws SMG2Exception thrown if the dereference encounters a problem.
   */
  public Optional<SMGObjectAndOffset> getTargetObjectAndOffset(
      SMGState pState, Value value, BigInteger pOffsetInBits) throws SMG2Exception {
    // TODO: maybe use this in readValueWithPointerDereference?
    Optional<SMGObjectAndOffset> maybeTargetAndOffset =
        pState.getMemoryModel().dereferencePointer(value);
    if (maybeTargetAndOffset.isEmpty()) {
      // The value is unknown and therefore does not point to a valid memory location
      SMGState errorState = pState.withUnknownPointerDereferenceWhenReading(value);
      throw new SMG2Exception(errorState);
    }
    SMGObjectAndOffset targetAndOffset = maybeTargetAndOffset.orElseThrow();
    BigInteger baseOffset = targetAndOffset.getOffsetForObject();
    BigInteger finalOffset = baseOffset.add(pOffsetInBits);

    return Optional.of(SMGObjectAndOffset.of(targetAndOffset.getSMGObject(), finalOffset));
  }

  /**
   * Returns the target Object of the entered stack/global variable. This assumes the offset to be
   * 0. Mainly used for &variable.
   *
   * @param state current {@link SMGState}.
   * @param variableName the name of the stack/global variable.
   * @return Either an empty {@link Optional} if no object was found, or a filled one with the
   *     {@link SMGObject} and its offset.
   */
  public Optional<SMGObjectAndOffset> getTargetObjectAndOffset(
      SMGState state, String variableName) {
    return getTargetObjectAndOffset(state, variableName, BigInteger.ZERO);
  }

  /**
   * Returns the target Object and offset of the entered stack/global variable. Mainly used for
   * &variable.
   *
   * @param state current {@link SMGState}.
   * @param variableName the name of the stack/global variable.
   * @param offsetInBits the offset of in the {@link SMGObject}.
   * @return Either an empty {@link Optional} if no object was found, or a filled one with the
   *     {@link SMGObject} and its offset.
   */
  public Optional<SMGObjectAndOffset> getTargetObjectAndOffset(
      SMGState state, String variableName, BigInteger offsetInBits) {
    // TODO: maybe use this in getStackOrGlobalVar?
    Optional<SMGObject> maybeObject =
        state.getMemoryModel().getObjectForVisibleVariable(variableName);
    if (maybeObject.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(SMGObjectAndOffset.of(maybeObject.orElseThrow(), offsetInBits));
  }

  /**
   * Calculates the distance of 2 addresses in bits. But returns unknown Value if its not the same
   * object or unknown pointers.
   *
   * @param state the {@link SMGState} the 2 pointers are known in.
   * @param leftPointer {@link Value} left hand side pointer in the minus operation.
   * @param rightPointer {@link Value} right hand side pointer in the minus operation.
   * @return Either distance as {@link NumericValue} or {@link UnknownValue}.
   */
  public Value calculateAddressDistance(SMGState state, Value leftPointer, Value rightPointer) {
    SymbolicProgramConfiguration spc = state.getMemoryModel();
    if (!spc.isPointer(leftPointer) || !spc.isPointer(rightPointer)) {
      // Not known or not known as a pointer, return nothing
      return UnknownValue.getInstance();
    }
    // We can only compare the underlying SMGObject for equality as the Values are distinct if they
    // point to different parts of the object. We need to compare the object because we can only
    // calculate the distance in the exact same object
    Optional<SMGObjectAndOffset> maybeLeftTargetAndOffset =
        state.getMemoryModel().dereferencePointer(leftPointer);
    if (maybeLeftTargetAndOffset.isEmpty()) {
      return UnknownValue.getInstance();
    }
    Optional<SMGObjectAndOffset> maybeRightTargetAndOffset =
        state.getMemoryModel().dereferencePointer(rightPointer);
    if (maybeRightTargetAndOffset.isEmpty()) {
      return UnknownValue.getInstance();
    }
    SMGObjectAndOffset leftTargetAndOffset = maybeLeftTargetAndOffset.orElseThrow();
    SMGObjectAndOffset rightTargetAndOffset = maybeRightTargetAndOffset.orElseThrow();
    SMGObject leftTarget = leftTargetAndOffset.getSMGObject();
    SMGObject rightTarget = rightTargetAndOffset.getSMGObject();
    if (!leftTarget.equals(rightTarget)) {
      return UnknownValue.getInstance();
    }
    // int because this is always a int
    return new NumericValue(
        leftTargetAndOffset
            .getOffsetForObject()
            .subtract(rightTargetAndOffset.getOffsetForObject())
            .intValue());
  }

  /**
   * This is the most general read that should be used in the end by all read smg methods in this
   * class!
   *
   * @param currentState the current {@link SMGState}.
   * @param object the {@link SMGObject} to be read from.
   * @param offsetInBits the offset in bits as {@link BigInteger}.
   * @param sizeInBits size of the read value in bits as {@link BigInteger}.
   * @return {@link ValueAndSMGState} bundeling the most up to date state and the read value.
   */
  private ValueAndSMGState readValue(
      SMGState currentState, SMGObject object, BigInteger offsetInBits, BigInteger sizeInBits)
      throws SMG2Exception {
    // Check that the offset and offset + size actually fit into the SMGObject
    boolean doesNotFitIntoObject =
        offsetInBits.compareTo(BigInteger.ZERO) < 0
            || offsetInBits.add(sizeInBits).compareTo(object.getSize()) > 0;

    if (doesNotFitIntoObject) {
      // Field read does not fit size of declared Memory
      SMGState errorState = currentState.withOutOfRangeRead(object, offsetInBits, sizeInBits);
      throw new SMG2Exception(errorState);
    }
    // The read in SMGState checks for validity and external allocation
    return currentState.readValue(object, offsetInBits, sizeInBits);
  }

  /**
   * Writes the {@link Value} entered into the left hand sides {@link CExpression} memory.
   *
   * @param edge the current {@link CFAEdge} from which this assignment originates.
   * @param leftHandSideValue the {@link CExpression} of the left hand side memory.
   * @param valueToWrite the {@link Value} to be written into the left hand side memory. (So this is
   *     the evaluated right hand side!)
   * @param currentState the current {@link SMGState}.
   * @return a list of {@link SMGState}s with the {@link Value} written into the left hand side.
   *     Might include error states.
   * @throws CPATransferException in case of critical errors.
   */
  public List<SMGState> writeValueToExpression(
      CFAEdge edge,
      CExpression leftHandSideValue,
      Value valueToWrite,
      SMGState currentState,
      CType valueType)
      throws CPATransferException {
    BigInteger sizeInBits = getBitSizeof(currentState, leftHandSideValue);
    // Get the memory for the left hand side variable
    List<Optional<SMGObjectAndOffset>> variableMemorysAndOffsets =
        leftHandSideValue.accept(new SMGCPAAddressVisitor(this, currentState, edge, logger));
    ImmutableList.Builder<SMGState> successorsBuilder = ImmutableList.builder();
    // Write the return value into the left hand side variable
    for (Optional<SMGObjectAndOffset> maybeVariableMemoryAndOffset : variableMemorysAndOffsets) {
      if (maybeVariableMemoryAndOffset.isEmpty()) {
        // TODO: improve error msg
        throw new SMG2Exception("No memory found to assign the value to.");
      }
      SMGObject leftHandSideVariableMemory =
          maybeVariableMemoryAndOffset.orElseThrow().getSMGObject();
      BigInteger offsetInBits = maybeVariableMemoryAndOffset.orElseThrow().getOffsetForObject();
      if (leftHandSideValue.getExpressionType() != valueType) {
        throw new SMG2Exception(
            "TODO: cut/cast values assigning to different types without explicit cast.");
      }
      successorsBuilder.add(
          currentState.writeValueTo(
              leftHandSideVariableMemory,
              offsetInBits,
              sizeInBits,
              valueToWrite,
              leftHandSideValue.getExpressionType()));
    }

    return successorsBuilder.build();
  }

  /*
   * Get the address value of the entered field.
   */
  public Collection<ValueAndSMGState> getAddressOfField(
      SMGState pInitialSmgState, CFieldReference pExpression) {
    // CExpression fieldOwner = pExpression.getFieldOwner();
    // CType ownerType = TypeUtils.getRealExpressionType(fieldOwner);
    // TODO: rework this method because of 2 reasons: 1. its not understandable and documented and
    // 2. because fields are linked by pointsToEdges, meaning we need only the address fo the
    // general field (SMGObject) and the PointsToEdge holds the offsets, meaning we have to check
    // those! Calculating the address + offset as a numeric value is pointless.
    /*
    return evaluateAddress(pInitialSmgState, fieldOwner)
        .stream()
        .map(
            addressAndState -> {
              Value addressValue = addressAndState.getValue();
              SMGState state = addressAndState.getState();
              String fieldName = pExpression.getFieldName();
              CTypeAndValue field = getField(ownerType, fieldName);
              if (field.getValue().isUnknown() || addressValue.isUnknown()) {
                if (pExpression.isPointerDereference()) {
                  state = handleUnknownDereference(state).getState();
                }
                Value fieldOffset = field.getValue().add(addressValue);
                return ValueAndSMGState.of(fieldOffset, state);
              }

              return ValueAndSMGState.ofUnknownValue(state);
            })
        .collect(ImmutableSet.toImmutableSet());
        */
    return null;
  }

  private CTypeAndValue getField(CType pType, String pFieldName) {

    if (pType instanceof CElaboratedType) {

      CType realType = ((CElaboratedType) pType).getRealType();

      if (realType == null) {
        return CTypeAndValue.ofUnknownValue(pType);
      }

      return getField(realType, pFieldName);
    } else if (pType instanceof CCompositeType) {
      return getField((CCompositeType) pType, pFieldName);
    } else if (pType instanceof CPointerType) {

      /*
       * We do not explicitly transform x->b, so when we try to get the field b the ownerType of x
       * is a pointer type.
       */

      CType type = ((CPointerType) pType).getType();

      type = TypeUtils.getRealExpressionType(type);

      return getField(type, pFieldName);
    }

    throw new AssertionError("Unknown CType found: " + pType);
  }

  private CTypeAndValue getField(CCompositeType pOwnerType, String pFieldName) {
    CType resultType = pOwnerType;

    BigInteger offset = machineModel.getFieldOffsetInBits(pOwnerType, pFieldName);

    // TODO: i need to look at this
    for (CCompositeTypeMemberDeclaration typeMember : pOwnerType.getMembers()) {
      if (typeMember.getName().equals(pFieldName)) {
        resultType = typeMember.getType();
      }
    }

    final Value value;
    if (!resultType.equals(pOwnerType)) {
      value = new NumericValue(offset);
      resultType = TypeUtils.getRealExpressionType(resultType);
    } else {
      value = UnknownValue.getInstance();
    }
    return CTypeAndValue.of(resultType, value);
  }

  /*
   * Unknown dereference is different to null dereference in that the object that is dereferenced actually exists, but its unknown.
   * We simply return an unknown value and log it.
   */
  public ValueAndSMGState handleUnknownDereference(SMGState pInitialSmgState) {

    return ValueAndSMGState.ofUnknownValue(pInitialSmgState);
  }

  /** TODO: Move all type related stuff into its own class once i rework getBitSizeOf */
  public BigInteger getBitSizeof(SMGState pInitialSmgState, CExpression pExpression) {
    // TODO check why old implementation did not use machineModel
    // Because in abstracted SMGs we might need the current SMG to get the correct type info.
    // TODO: rework because of that.
    return getBitSizeof(pInitialSmgState, pExpression.getExpressionType());
  }

  public BigInteger getBitSizeof(SMGState pInitialSmgState, CRightHandSide pExpression) {
    // TODO check why old implementation did not use machineModel
    // Because in abstracted SMGs we might need the current SMG to get the correct type info.
    // TODO: rework because of that.
    return getBitSizeof(pInitialSmgState, pExpression.getExpressionType());
  }

  public BigInteger getBitSizeof(SMGState pInitialSmgState, CType pType) {
    // TODO check why old implementation did not use machineModel
    // Because in abstracted SMGs we might need the current SMG to get the correct type info.
    // TODO: rework because of that.
    return machineModel.getSizeofInBits(pType);
  }

  public BigInteger getAlignOf(SMGState pInitialSmgState, CType pType) {
    return BigInteger.valueOf(machineModel.getAlignof(pType));
  }

  // TODO: revisit this and decide if we want to split structs and unions because of the data
  // reinterpretation because unions will most likely not work with SMG join!
  /*
   * Structs and Union types are treated essentially the same in the SMGs. Note: Both can
   * contain methods. Unions can have data reinterpretations based on type.
   * This should be used to not confuse enums with structs/unions.
   */
  public static boolean isStructOrUnionType(CType rValueType) {

    if (rValueType instanceof CElaboratedType) {
      CElaboratedType type = (CElaboratedType) rValueType;
      return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }

    if (rValueType instanceof CCompositeType) {
      CCompositeType type = (CCompositeType) rValueType;
      return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }

    return false;
  }

  private BigInteger getBitSizeofType(
      CFAEdge edge, CType pType, SMGState pState, Optional<CExpression> pExpression)
      throws UnrecognizedCodeException {
    // We don't really care about volatility in C
    // Incomplete types can make problems when calculating the size as they might not have all
    // information to get the size in bits

    // The reason why we need a dedicated visitor and not use MachineModel is inside the visitor
    SMGSizeOfVisitor v = new SMGSizeOfVisitor(this, edge, pState, pExpression);

    // We multiply with char size as it is 8 bit ;D
    try {
      return pType.accept(v).multiply(BigInteger.valueOf(machineModel.getSizeofCharInBits()));
    } catch (IllegalArgumentException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCodeException("Could not resolve type.", edge);
    }
  }

  /**
   * Copies all existing Values (Value Edges) from the memory behind source to the memory behind
   * target up to the size specified in bits. If a Value starts before the sourcePointer offset, or
   * ends after the size, the value is not copied. This method will check validity of pointers,
   * writes, reads etc. itself. This includes that the 2 memory regions don't overlap and writes and
   * reads are only on valid memory. If an error is found a unchanged errorstate is returned, else a
   * state with the memory copied is returned. Mainly thought to be used by memcpy
   *
   * @param sourcePointer {@link Value} that is a pointer to some memory that is the source of the
   *     copy.
   * @param targetPointer {@link Value} that is a pointer to some memory that is the target of the
   *     copy.
   * @param sizeToCopy the size of the copy in bits.
   * @param pState the {@link SMGState} to start with.
   * @return either a {@link SMGState} with the contents copied or a error state.
   */
  public SMGState copyFromMemoryToMemory(
      Value sourcePointer,
      BigInteger sourceOffset,
      Value targetPointer,
      BigInteger targetOffset,
      BigInteger sizeToCopy,
      SMGState pState) {
    if (sizeToCopy.compareTo(BigInteger.ZERO) == 0) {
      return pState;
    }
    // TODO: this could end up weird if the types sizes don't match between source and target. If
    // you read the target after the memcpy you don't necassarly get the correct values as read
    // depends on offset + size. Is this something we just accept?

    // Dereference the pointers and get the source/target memory and offset
    // Start with source
    Optional<SMGObjectAndOffset> maybeSourceAndOffset =
        pState.getMemoryModel().dereferencePointer(sourcePointer);
    if (maybeSourceAndOffset.isEmpty()) {
      // The value is unknown and therefore does not point to a valid memory location
      return pState.withUnknownPointerDereferenceWhenReading(sourcePointer);
    }
    SMGObject sourceObject = maybeSourceAndOffset.orElseThrow().getSMGObject();

    // The object may be null if no such object exists, check and log if 0
    if (sourceObject.isZero()) {
      return pState.withNullPointerDereferenceWhenReading(sourceObject);
    }

    // The offset of the pointer used. (the pointer might point to a offset != 0, the other offset
    // needs to the added to that!)
    BigInteger finalSourceOffset =
        maybeSourceAndOffset.orElseThrow().getOffsetForObject().add(sourceOffset);

    // The same for the target
    Optional<SMGObjectAndOffset> maybeTargetAndOffset =
        pState.getMemoryModel().dereferencePointer(targetPointer);
    if (maybeTargetAndOffset.isEmpty()) {
      // The value is unknown and therefore does not point to a valid memory location
      return pState.withUnknownPointerDereferenceWhenReading(targetPointer);
    }
    SMGObject targetObject = maybeTargetAndOffset.orElseThrow().getSMGObject();

    // The object may be null if no such object exists, check and log if 0
    if (targetObject.isZero()) {
      return pState.withNullPointerDereferenceWhenWriting(targetObject);
    }

    // The offset of the pointer used. (the pointer might point to a offset != 0, the other offset
    // needs to the added to that!)
    BigInteger finalTargetoffset =
        maybeTargetAndOffset.orElseThrow().getOffsetForObject().add(targetOffset);

    // Check that the memory regions don't overlapp as this results in undefined behaviour
    if (sourceObject.equals(targetObject)) {
      int compareOffsets = finalTargetoffset.compareTo(finalSourceOffset);
      if (compareOffsets == 0) {
        // overlap
        return pState.withUndefinedbehavior(
            "Undefined behaviour because of overlapping memory regions in a copy function. I.e."
                + " memcpy().",
            ImmutableList.of(targetPointer, sourcePointer));
      } else if (compareOffsets > 0) {
        // finalTargetoffset > finalSourceOffset -> if the finalTargetoffset < finalSourceOffset +
        // sizeToCopy we have an overlap
        if (finalTargetoffset.compareTo(finalSourceOffset.add(sizeToCopy)) < 0) {
          return pState.withUndefinedbehavior(
              "Undefined behaviour because of overlapping memory regions in a copy function. I.e."
                  + " memcpy().",
              ImmutableList.of(targetPointer, sourcePointer));
        }
      } else {
        // finalTargetoffset < finalSourceOffset -> if the finalSourceOffset < finalTargetoffset +
        // sizeToCopy we have an overlap
        if (finalSourceOffset.compareTo(finalTargetoffset.add(sizeToCopy)) < 0) {
          return pState.withUndefinedbehavior(
              "Undefined behaviour because of overlapping memory regions in a copy function. I.e."
                  + " memcpy().",
              ImmutableList.of(targetPointer, sourcePointer));
        }
      }
    }

    // Check that we don't read beyond the source size and don't write beyonde the target size and
    // that we don't start before the object begins
    if (sourceObject.getSize().subtract(finalSourceOffset).compareTo(sizeToCopy) < 0
        || finalSourceOffset.compareTo(BigInteger.ZERO) < 0) {
      // This would be an invalid read
      SMGState currentState = pState.withInvalidRead(sourceObject);
      if (targetObject.getSize().subtract(finalTargetoffset).compareTo(sizeToCopy) < 0
          || finalTargetoffset.compareTo(BigInteger.ZERO) < 0) {
        // That would be an invalid write
        currentState = currentState.withInvalidWrite(sourceObject);
      }
      return currentState;
    }
    if (targetObject.getSize().subtract(finalTargetoffset).compareTo(sizeToCopy) < 0
        || finalTargetoffset.compareTo(BigInteger.ZERO) < 0) {
      // That would be an invalid write
      return pState.withInvalidWrite(sourceObject);
    }

    return pState.copySMGObjectContentToSMGObject(
        sourceObject, finalSourceOffset, targetObject, finalTargetoffset, sizeToCopy);
  }

  /**
   * Implementation of strcmp(). Compares the characters behind the 2 memory addresses and offsets
   * until a \0 is found or the characters don't equal. If they equal in the entire memory until \0
   * is found a 0 is returned. If not the difference of them numericly is returned.
   *
   * @param firstAddress {@link Value} leading to the first memory/String.
   * @param pFirstOffsetInBits offset to beginn reading the first address in bits.
   * @param secondAddress {@link Value} leading to the second memory/String.
   * @param pSecondOffsetInBits offset to beginn reading the second address in bits.
   * @param pState initial {@link SMGState}.
   * @return {@link ValueAndSMGState} with a numeric value and the compare result if the values were
   *     concrete or comparable + the non error state. The Value may also be symbolic. If an error
   *     is encountered or the values were not comparable a unknown value is returned + a state that
   *     may have a error state with the error specified if there was any.
   * @throws SMG2Exception i want to remove this and use error states.
   */
  public ValueAndSMGState stringCompare(
      Value firstAddress,
      BigInteger pFirstOffsetInBits,
      Value secondAddress,
      BigInteger pSecondOffsetInBits,
      SMGState pState)
      throws SMG2Exception {
    // Dereference the pointers and get the first/second memory and offset
    // Start with first
    Optional<SMGObjectAndOffset> maybefirstMemoryAndOffset =
        pState.getMemoryModel().dereferencePointer(firstAddress);
    if (maybefirstMemoryAndOffset.isEmpty()) {
      // The value is unknown and therefore does not point to a valid memory location
      return ValueAndSMGState.ofUnknownValue(
          pState.withUnknownPointerDereferenceWhenReading(firstAddress));
    }
    SMGObject firstObject = maybefirstMemoryAndOffset.orElseThrow().getSMGObject();

    // The object may be null if no such object exists, check and log if 0
    if (firstObject.isZero()) {
      return ValueAndSMGState.ofUnknownValue(
          pState.withNullPointerDereferenceWhenReading(firstObject));
    }

    // The offset of the pointer used. (the pointer might point to a offset != 0, the other offset
    // needs to the added to that!)
    BigInteger firstOffsetInBits =
        maybefirstMemoryAndOffset.orElseThrow().getOffsetForObject().add(pFirstOffsetInBits);

    // The same for the second address
    Optional<SMGObjectAndOffset> maybeSecondMemoryAndOffset =
        pState.getMemoryModel().dereferencePointer(secondAddress);
    if (maybeSecondMemoryAndOffset.isEmpty()) {
      // The value is unknown and therefore does not point to a valid memory location
      return ValueAndSMGState.ofUnknownValue(
          pState.withUnknownPointerDereferenceWhenReading(secondAddress));
    }
    SMGObject secondObject = maybeSecondMemoryAndOffset.orElseThrow().getSMGObject();

    // The object may be null if no such object exists, check and log if 0
    if (secondObject.isZero()) {
      return ValueAndSMGState.ofUnknownValue(
          pState.withNullPointerDereferenceWhenWriting(secondObject));
    }

    // The offset of the pointer used. (the pointer might point to a offset != 0, the other offset
    // needs to the added to that!)
    BigInteger secondOffsetInBits =
        maybeSecondMemoryAndOffset.orElseThrow().getOffsetForObject().add(pSecondOffsetInBits);

    // Check that they are not ==, if they are the returned value is trivial 0
    if (firstObject.equals(secondObject) && firstOffsetInBits.compareTo(secondOffsetInBits) == 0) {
      return ValueAndSMGState.of(new NumericValue(0), pState);
    }

    BigInteger sizeOfCharInBits = BigInteger.valueOf(machineModel.getSizeofCharInBits());
    // Now compare the Strings; stop at first \0
    SMGState currentState = pState;
    boolean foundNoStringTerminationChar = true;
    while (foundNoStringTerminationChar) {
      ValueAndSMGState valueAndState1 =
          readValue(currentState, firstObject, firstOffsetInBits, sizeOfCharInBits);
      Value value1 = valueAndState1.getValue();
      currentState = valueAndState1.getState();

      if (value1.isUnknown()) {
        return ValueAndSMGState.ofUnknownValue(currentState);
      }

      ValueAndSMGState valueAndState2 =
          readValue(currentState, secondObject, secondOffsetInBits, sizeOfCharInBits);
      Value value2 = valueAndState2.getValue();
      currentState = valueAndState2.getState();

      if (value2.isUnknown()) {
        return ValueAndSMGState.ofUnknownValue(currentState);
      }

      // Now compare the 2 values. Non-equality of non concrete values has to be checked by the SMG
      // method because of abstraction.
      if (value1.isNumericValue() && value2.isNumericValue()) {
        // easy, just compare the numeric value and return if != 0
        int compare =
            value1.asNumericValue().bigInteger().compareTo(value2.asNumericValue().bigInteger());
        if (compare != 0) {
          return ValueAndSMGState.of(new NumericValue(compare), pState);
        }
      } else {
        // Symbolic handling as we know its not unknown and either not or only partially numeric
        // TODO:
      }
      if ((value1.isNumericValue() && value1.asNumericValue().longValue() == 0)
          || (value2.isNumericValue() && value2.asNumericValue().longValue() == 0)) {
        foundNoStringTerminationChar = false;
      } else {
        firstOffsetInBits = firstOffsetInBits.add(sizeOfCharInBits);
        secondOffsetInBits = secondOffsetInBits.add(sizeOfCharInBits);
      }
    }
    // Only if we can 100% say they are the same we return 0
    return ValueAndSMGState.of(new NumericValue(0), pState);
  }

  public MachineModel getMachineModel() {
    return machineModel;
  }

  // Get canonical type information
  public static CType getCanonicalType(CType type) {
    return type.getCanonicalType();
  }

  public static CType getCanonicalType(CSimpleDeclaration decl) {
    return getCanonicalType(decl.getType());
  }

  public static CType getCanonicalType(CRightHandSide exp) {
    return getCanonicalType(exp.getExpressionType());
  }
}
