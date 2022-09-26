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
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.MachineModel.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAAddressVisitor;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPABuiltins;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAExportOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAValueVisitor;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGSizeOfVisitor;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@SuppressWarnings("unused")
public class SMGCPAValueExpressionEvaluator {

  private final SMGCPAExportOptions exportSMGOptions;
  private final SMGOptions options;
  private final LogManagerWithoutDuplicates logger;
  private final MachineModel machineModel;

  private final SMGCPABuiltins builtins;

  // Ignored variables (declarations)
  private final Collection<String> addressedVariables;

  public SMGCPAValueExpressionEvaluator(
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      SMGCPAExportOptions pExportSMGOptions,
      SMGOptions pSMGOptions,
      Collection<String> pAddressedVariables) {
    logger = pLogger;
    machineModel = pMachineModel;
    exportSMGOptions = pExportSMGOptions;
    options = pSMGOptions;
    builtins = new SMGCPABuiltins(this, options, exportSMGOptions, machineModel, logger);
    addressedVariables = pAddressedVariables;
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

  /**
   * Transforms the entered {@link AddressExpression} into a non {@link AddressExpression} that is
   * either a UNKNOWN {@link Value} or a valid pointer with the offset of the {@link
   * AddressExpression}.
   *
   * @param addressExpression {@link AddressExpression} to be transformed.
   * @param currentState current {@link SMGState}.
   * @return a {@link Value} that is not longer a {@link AddressExpression} and either is a pointer
   *     or UNKNOWN.
   * @throws SMG2Exception should not happen in this context
   */
  public ValueAndSMGState transformAddressExpressionIntoPointerValue(
      AddressExpression addressExpression, SMGState currentState) throws SMG2Exception {
    Value offset = addressExpression.getOffset();
    if (!offset.isNumericValue()) {
      return ValueAndSMGState.ofUnknownValue(currentState);
    }

    if (offset.asNumericValue().bigInteger().compareTo(BigInteger.ZERO) == 0) {
      // offset == 0 -> known pointer
      return ValueAndSMGState.of(addressExpression.getMemoryAddress(), currentState);
    } else {
      // Offset known but not 0, search for/create the correct address
      return this.findOrcreateNewPointer(
          addressExpression.getMemoryAddress(), offset.asNumericValue().bigInteger(), currentState);
    }
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
   * @throws SMG2Exception in case of critical errors
   */
  public Value checkEqualityForAddresses(
      Value leftValue, Value rightValue, SMGState state, CFAEdge cfaEdge) throws SMG2Exception {
    Value isNotEqual = checkNonEqualityForAddresses(leftValue, rightValue, state, cfaEdge);
    if (isNotEqual.isUnknown()) {
      return isNotEqual;
    }
    return isNotEqual.asNumericValue().bigInteger().compareTo(BigInteger.ZERO) == 0
        ? new NumericValue(1)
        : new NumericValue(0);
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
   * @throws SMG2Exception in case of critical errors
   */
  public Value checkNonEqualityForAddresses(
      Value leftValue, Value rightValue, SMGState state, CFAEdge cfaEdge) throws SMG2Exception {
    ValueAndSMGState leftValueAndState = unpackAddressExpression(leftValue, state, cfaEdge);
    leftValue = leftValueAndState.getValue();
    ValueAndSMGState rightValueAndState =
        unpackAddressExpression(rightValue, leftValueAndState.getState(), cfaEdge);
    rightValue = rightValueAndState.getValue();
    SMGState currentState = rightValueAndState.getState();
    // Check that both Values are truly addresses
    if (!isPointerValue(rightValue, currentState) || !isPointerValue(leftValue, currentState)) {
      return UnknownValue.getInstance();
    }

    Preconditions.checkArgument(
        !(leftValue instanceof AddressExpression) && !(rightValue instanceof AddressExpression));
    boolean isNotEqual = currentState.areNonEqualAddresses(leftValue, rightValue);
    return isNotEqual ? new NumericValue(1) : new NumericValue(0);
  }

  /**
   * Unpacks the {@link Value} iff it is a {@link AddressExpression}. Else returns the original. If
   * the value is a AddressExpression with offset, a new pointer is created and mapped to a new
   * value that is returned.
   *
   * @param value a {@link Value} that may be a {@link AddressExpression}.
   * @param state current {@link SMGState}.
   * @param cfaEdge current {@link CFAEdge} for debugging.
   * @return {@link ValueAndSMGState}
   * @throws SMG2Exception in case of critical errors.
   */
  public ValueAndSMGState unpackAddressExpression(Value value, SMGState state, CFAEdge cfaEdge)
      throws SMG2Exception {
    if (!(value instanceof AddressExpression)) {
      return ValueAndSMGState.of(value, state);
    }
    AddressExpression address1 = (AddressExpression) value;
    Value offsetValue = address1.getOffset();
    if (offsetValue.isNumericValue()
        && offsetValue.asNumericValue().bigInteger().compareTo(BigInteger.ZERO) == 0) {
      return ValueAndSMGState.of(address1.getMemoryAddress(), state);
    } else {
      // Get the correct address with its offset in the SMGPointsToEdge
      Optional<SMGObjectAndOffset> maybeTargetAndOffset =
          state.getPointsToTarget(address1.getMemoryAddress());
      if (maybeTargetAndOffset.isEmpty()) {
        return ValueAndSMGState.ofUnknownValue(state);
      }
      SMGObjectAndOffset targetAndOffset = maybeTargetAndOffset.orElseThrow();
      SMGObject target = targetAndOffset.getSMGObject();
      if (!offsetValue.isNumericValue()) {
        throw new SMG2Exception(
            "Comparison of non numeric offset values not possible when comparing addresses.");
      }
      BigInteger offset =
          offsetValue.asNumericValue().bigInteger().add(targetAndOffset.getOffsetForObject());

      return searchOrCreatePointer(target, offset, state);
    }
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
   * Create a new stack object with the size in bits and then create a pointer to its beginning and
   * return the state with the pointer and object + the pointer Value (address to the objects
   * beginning).
   *
   * @param pInitialSmgState initial {@link SMGState}.
   * @param sizeInBits size in bits as {@link BigInteger}.
   * @return the {@link Value} that is the address for the new stack memory region created with the
   *     size and the {@link SMGState} with the region (SMGObject) pointer and address Value added.
   */
  public ValueAndSMGState createStackMemoryAndPointer(
      SMGState pInitialSmgState, BigInteger sizeInBits) {
    SMGObjectAndSMGState newObjectAndState = pInitialSmgState.copyAndAddStackObject(sizeInBits);
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
   * @return a list of either unknown or a {@link AddressExpression} representing the address.
   * @throws CPATransferException if the & operator is used on a invalid expression.
   */
  public List<ValueAndSMGState> createAddress(CExpression operand, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {
    // SMGCPAAddressVisitor may have side effects! But they should not effect anything as they are
    // only interesting in a failure case in which the analysis stops!
    SMGCPAAddressVisitor addressVisitor = new SMGCPAAddressVisitor(this, pState, cfaEdge, logger);
    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    SMGState currentState = pState;
    for (SMGStateAndOptionalSMGObjectAndOffset objectAndOffsetOrState :
        operand.accept(addressVisitor)) {

      currentState = objectAndOffsetOrState.getSMGState();
      if (!objectAndOffsetOrState.hasSMGObjectAndOffset()) {
        // Functions are not declared, but the address might be requested anyway, so we have to
        // create the address
        if (operand instanceof CIdExpression
            && SMGCPAValueExpressionEvaluator.getCanonicalType(operand.getExpressionType())
                instanceof CFunctionType) {
          currentState = objectAndOffsetOrState.getSMGState();
          CFunctionDeclaration functionDcl =
              (CFunctionDeclaration) ((CIdExpression) operand).getDeclaration();
          Optional<SMGObject> functionObject = currentState.getObjectForFunction(functionDcl);

          if (functionObject.isEmpty()) {
            currentState = currentState.copyAndAddFunctionVariable(functionDcl);
            functionObject = currentState.getObjectForFunction(functionDcl);
          }
          objectAndOffsetOrState =
              SMGStateAndOptionalSMGObjectAndOffset.withZeroOffset(
                  functionObject.orElseThrow(), currentState);
        } else {
          // This is not necessarily an error! If we can't get a address because a lookup is based
          // on
          // an unknown value for example. We create a dummy pointer in such cases that points
          // nowhere
          // throw new SMG2Exception("No address could be created for the expression: " + operand);
          // Try unknown first, wrapped in AddressExpr and see what happens
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        }
      }
      SMGObject target = objectAndOffsetOrState.getSMGObject();
      BigInteger offset = objectAndOffsetOrState.getOffsetForObject();
      // search for existing pointer first and return if found; else make a new one
      ValueAndSMGState addressAndState = searchOrCreatePointer(target, offset, currentState);
      resultBuilder.add(
          ValueAndSMGState.of(
              AddressExpression.withZeroOffset(
                  addressAndState.getValue(), operand.getExpressionType()),
              addressAndState.getState()));
    }
    return resultBuilder.build();
  }

  public List<ValueAndSMGState> createAddress(
      CRightHandSide operand, SMGState pState, CFAEdge cfaEdge) throws CPATransferException {
    Preconditions.checkArgument(!(operand instanceof CFunctionCallExpression));
    return createAddress((CExpression) operand, pState, cfaEdge);
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
   * @throws SMG2Exception in case of critical abstract memory materilization errors.
   */
  public ValueAndSMGState findOrcreateNewPointer(
      Value targetAddress, BigInteger offsetInBits, SMGState pState) throws SMG2Exception {
    Preconditions.checkArgument(!(targetAddress instanceof AddressExpression));

    SMGStateAndOptionalSMGObjectAndOffset maybeTargetAndOffset =
        pState.dereferencePointer(targetAddress);
    if (!maybeTargetAndOffset.hasSMGObjectAndOffset()) {
      // The value is unknown and therefore does not point to a known memory location
      return ValueAndSMGState.ofUnknownValue(maybeTargetAndOffset.getSMGState());
    }
    // We don't want to materilize memory here?
    // pState = maybeTargetAndOffset.orElseThrow().getSMGState();
    SMGObject object = maybeTargetAndOffset.getSMGObject();

    // The object may be null, which is fine, the deref is the problem
    // The offset of the pointer used. (the pointer might point to a offset != 0, the other offset
    // needs to the added to that!)
    BigInteger baseOffset = maybeTargetAndOffset.getOffsetForObject();
    BigInteger finalOffsetInBits = baseOffset.add(offsetInBits);

    // search for existing pointer first and return if found; else make a new one for the offset
    return searchOrCreatePointer(object, finalOffsetInBits, maybeTargetAndOffset.getSMGState());
  }

  private ValueAndSMGState searchOrCreatePointer(
      SMGObject targetObject, BigInteger offsetInBits, SMGState pState) {
    return pState.searchOrCreateAddress(targetObject, offsetInBits);
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
      String internalName, BigInteger sizeInBits, CType type, SMGState pState)
      throws CPATransferException {
    Preconditions.checkArgument(
        !pState.getMemoryModel().getStackFrames().peek().containsVariable(internalName));
    return createAddressForLocalOrGlobalVariable(
        internalName, pState.copyAndAddLocalVariable(sizeInBits, internalName, type));
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
   * @param readType the type of the read value before casts etc. Used to determine union float
   *     conversion.
   * @return {@link ValueAndSMGState} with the updated {@link SMGState} and the read {@link Value}.
   *     The Value might be unknown either because it was read as unknown or because the variable
   *     was not initialized.
   * @throws CPATransferException if the variable is not known in the memory model (SPC)
   */
  public ValueAndSMGState readStackOrGlobalVariable(
      SMGState initialState,
      String varName,
      BigInteger offsetInBits,
      BigInteger sizeInBits,
      CType readType)
      throws CPATransferException {

    Optional<SMGObject> maybeObject =
        initialState.getMemoryModel().getObjectForVisibleVariable(varName);

    if (maybeObject.isEmpty()) {
      // If there is no object, the variable is not initialized
      SMGState errorState = initialState.withUninitializedVariableUsage(varName);
      // The Value does not matter here as the error state should always end the analysis
      return ValueAndSMGState.ofUnknownValue(errorState);
    }
    return readValue(initialState, maybeObject.orElseThrow(), offsetInBits, sizeInBits, readType);
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
   * @param readType the type of the read value before casts etc. Used to determine union float
   *     conversion.
   * @return {@link ValueAndSMGState} tuple for the read {@link Value} and the new {@link SMGState}.
   */
  public ValueAndSMGState readValueWithPointerDereference(
      SMGState pState, Value value, BigInteger pOffset, BigInteger pSizeInBits, CType readType)
      throws SMG2Exception {
    // Get the SMGObject for the value
    SMGStateAndOptionalSMGObjectAndOffset maybeTargetAndOffset = pState.dereferencePointer(value);
    if (!maybeTargetAndOffset.hasSMGObjectAndOffset()) {
      // The value is unknown and therefore does not point to a valid memory location
      SMGState errorState =
          maybeTargetAndOffset.getSMGState().withUnknownPointerDereferenceWhenReading(value);

      // throw new SMG2Exception(errorState);
      return ValueAndSMGState.ofUnknownValue(errorState);
    }
    pState = maybeTargetAndOffset.getSMGState();
    SMGObject object = maybeTargetAndOffset.getSMGObject();

    // The object may be null if no such object exists, check and log if 0
    if (object.isZero()) {
      SMGState errorState = pState.withNullPointerDereferenceWhenReading(object);
      return ValueAndSMGState.ofUnknownValue(errorState);
    }

    // The offset of the pointer used. (the pointer might point to a offset != 0, the other offset
    // needs to the added to that!)
    BigInteger baseOffset = maybeTargetAndOffset.getOffsetForObject();
    BigInteger offset = baseOffset.add(pOffset);

    return readValue(pState, object, offset, pSizeInBits, readType);
  }

  /**
   * Returns the {@link SMGObjectAndOffset} pair for the entered address {@link Value} and
   * additional offset on the entered state. This does dereference the address {@link Value}.
   *
   * @param pState current {@link SMGState}.
   * @param value {@link Value} pointer to be dereferenced leading to the {@link SMGObject} desired.
   * @param pOffsetInBits used offset when dereferencing.
   * @return the desired {@link SMGObject} and its offset or an State with potentially an error.
   */
  public SMGStateAndOptionalSMGObjectAndOffset getTargetObjectAndOffset(
      SMGState pState, Value value, BigInteger pOffsetInBits) throws SMG2Exception {

    SMGStateAndOptionalSMGObjectAndOffset targetAndOffset = pState.dereferencePointer(value);
    if (!targetAndOffset.hasSMGObjectAndOffset()) {
      // The value is unknown and therefore does not point to a valid memory location
      SMGState errorState =
          targetAndOffset.getSMGState().withUnknownPointerDereferenceWhenReading(value);
      // TODO: the analysis is not precise from this point onwards
      return SMGStateAndOptionalSMGObjectAndOffset.of(errorState);
    }

    BigInteger baseOffset = targetAndOffset.getOffsetForObject();
    BigInteger finalOffset = baseOffset.add(pOffsetInBits);

    return SMGStateAndOptionalSMGObjectAndOffset.of(
        targetAndOffset.getSMGObject(), finalOffset, targetAndOffset.getSMGState());
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
   * @throws SMG2Exception in case of critical list materilization errors
   */
  public ValueAndSMGState calculateAddressDistance(
      SMGState state, Value leftPointer, Value rightPointer) throws SMG2Exception {
    SymbolicProgramConfiguration spc = state.getMemoryModel();
    if (!spc.isPointer(leftPointer) || !spc.isPointer(rightPointer)) {
      // Not known or not known as a pointer, return nothing
      return ValueAndSMGState.ofUnknownValue(state);
    }
    // We can only compare the underlying SMGObject for equality as the Values are distinct if they
    // point to different parts of the object. We need to compare the object because we can only
    // calculate the distance in the exact same object
    SMGStateAndOptionalSMGObjectAndOffset leftTargetAndOffset =
        state.dereferencePointer(leftPointer);
    if (!leftTargetAndOffset.hasSMGObjectAndOffset()) {
      return ValueAndSMGState.ofUnknownValue(leftTargetAndOffset.getSMGState());
    }

    state = leftTargetAndOffset.getSMGState();
    SMGStateAndOptionalSMGObjectAndOffset rightTargetAndOffset =
        state.dereferencePointer(rightPointer);
    if (!rightTargetAndOffset.hasSMGObjectAndOffset()) {
      return ValueAndSMGState.ofUnknownValue(rightTargetAndOffset.getSMGState());
    }

    state = rightTargetAndOffset.getSMGState();
    SMGObject leftTarget = leftTargetAndOffset.getSMGObject();
    SMGObject rightTarget = rightTargetAndOffset.getSMGObject();
    if (!leftTarget.equals(rightTarget)) {
      return ValueAndSMGState.ofUnknownValue(state);
    }
    // int because this is always a int
    return ValueAndSMGState.of(
        new NumericValue(
            leftTargetAndOffset
                .getOffsetForObject()
                .subtract(rightTargetAndOffset.getOffsetForObject())
                .intValue()),
        state);
  }

  /**
   * This is the most general read that should be used in the end by all read smg methods in this
   * class!
   *
   * @param currentState the current {@link SMGState}.
   * @param object the {@link SMGObject} to be read from.
   * @param offsetInBits the offset in bits as {@link BigInteger}.
   * @param sizeInBits size of the read value in bits as {@link BigInteger}.
   * @param readType the uncasted type of the read (right hand side innermost type). Null only if
   *     its certain that implicit union casts are not possible.
   * @return {@link ValueAndSMGState} bundeling the most up to date state and the read value.
   */
  private ValueAndSMGState readValue(
      SMGState currentState,
      SMGObject object,
      BigInteger offsetInBits,
      BigInteger sizeInBits,
      @Nullable CType readType)
      throws SMG2Exception {
    // Check that the offset and offset + size actually fit into the SMGObject
    boolean doesNotFitIntoObject =
        offsetInBits.compareTo(BigInteger.ZERO) < 0
            || offsetInBits.add(sizeInBits).compareTo(object.getSize()) > 0;

    if (doesNotFitIntoObject) {
      // Field read does not fit size of declared Memory
      SMGState errorState = currentState.withOutOfRangeRead(object, offsetInBits, sizeInBits);
      // Unknown value that should not be used with a error state that should stop the analysis
      return ValueAndSMGState.ofUnknownValue(errorState);
    }
    // The read in SMGState checks for validity and external allocation
    return currentState.readValue(object, offsetInBits, sizeInBits, readType);
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
    ImmutableList.Builder<SMGState> successorsBuilder = ImmutableList.builder();
    // Get the memory for the left hand side variable
    // Write the return value into the left hand side variable
    for (SMGStateAndOptionalSMGObjectAndOffset variableMemoryAndOffsetOrState :
        leftHandSideValue.accept(new SMGCPAAddressVisitor(this, currentState, edge, logger))) {
      if (!variableMemoryAndOffsetOrState.hasSMGObjectAndOffset()) {
        // throw new SMG2Exception("No memory found to assign the value to.");
        successorsBuilder.add(variableMemoryAndOffsetOrState.getSMGState());
        continue;
      }
      currentState = variableMemoryAndOffsetOrState.getSMGState();
      SMGObject leftHandSideVariableMemory = variableMemoryAndOffsetOrState.getSMGObject();
      BigInteger offsetInBits = variableMemoryAndOffsetOrState.getOffsetForObject();

      ValueAndSMGState castedValueAndState =
          new SMGCPAValueVisitor(this, currentState, edge, logger)
              .castCValue(valueToWrite, leftHandSideValue.getExpressionType(), currentState);
      valueToWrite = castedValueAndState.getValue();
      currentState = castedValueAndState.getState();

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
    return machineModel.getSizeofInBits(
        pType, new SMG2SizeofVisitor(machineModel, this, pInitialSmgState, logger, options));
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

  public static boolean isEnumType(CType pType) {
    if (pType instanceof CElaboratedType) {
      CElaboratedType type = (CElaboratedType) pType;
      return type.getKind() == CComplexType.ComplexTypeKind.ENUM;
    }

    if (pType instanceof CCompositeType) {
      CCompositeType type = (CCompositeType) pType;
      return type.getKind() == CComplexType.ComplexTypeKind.ENUM;
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
   * @throws SMG2Exception in case of abstract memory materialization errors
   */
  public SMGState copyFromMemoryToMemory(
      Value sourcePointer,
      BigInteger sourceOffset,
      Value targetPointer,
      BigInteger targetOffset,
      BigInteger sizeToCopy,
      SMGState pState)
      throws SMG2Exception {
    if (sizeToCopy.compareTo(BigInteger.ZERO) == 0) {
      return pState;
    }
    // TODO: this could end up weird if the types sizes don't match between source and target. If
    // you read the target after the memcpy you don't necassarly get the correct values as read
    // depends on offset + size. Is this something we just accept?

    // Dereference the pointers and get the source/target memory and offset
    // Start with source
    SMGStateAndOptionalSMGObjectAndOffset maybeSourceAndOffset =
        pState.dereferencePointer(sourcePointer);
    if (!maybeSourceAndOffset.hasSMGObjectAndOffset()) {
      // The value is unknown and therefore does not point to a valid memory location
      return maybeSourceAndOffset
          .getSMGState()
          .withUnknownPointerDereferenceWhenReading(sourcePointer);
    }
    pState = maybeSourceAndOffset.getSMGState();
    SMGObject sourceObject = maybeSourceAndOffset.getSMGObject();

    // The object may be null if no such object exists, check and log if 0
    if (sourceObject.isZero()) {
      return pState.withNullPointerDereferenceWhenReading(sourceObject);
    }

    // The offset of the pointer used. (the pointer might point to a offset != 0, the other offset
    // needs to the added to that!)
    BigInteger finalSourceOffset = maybeSourceAndOffset.getOffsetForObject().add(sourceOffset);

    // The same for the target
    SMGStateAndOptionalSMGObjectAndOffset maybeTargetAndOffset =
        pState.dereferencePointer(targetPointer);
    if (!maybeTargetAndOffset.hasSMGObjectAndOffset()) {
      // The value is unknown and therefore does not point to a valid memory location
      return maybeTargetAndOffset
          .getSMGState()
          .withUnknownPointerDereferenceWhenReading(targetPointer);
    }
    pState = maybeTargetAndOffset.getSMGState();
    SMGObject targetObject = maybeTargetAndOffset.getSMGObject();

    // The object may be null if no such object exists, check and log if 0
    if (targetObject.isZero()) {
      return pState.withNullPointerDereferenceWhenWriting(targetObject);
    }

    // The offset of the pointer used. (the pointer might point to a offset != 0, the other offset
    // needs to the added to that!)
    BigInteger finalTargetoffset = maybeTargetAndOffset.getOffsetForObject().add(targetOffset);

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
    SMGStateAndOptionalSMGObjectAndOffset maybefirstMemoryAndOffset =
        pState.dereferencePointer(firstAddress);
    if (!maybefirstMemoryAndOffset.hasSMGObjectAndOffset()) {
      // The value is unknown and therefore does not point to a valid memory location
      return ValueAndSMGState.ofUnknownValue(
          maybefirstMemoryAndOffset
              .getSMGState()
              .withUnknownPointerDereferenceWhenReading(firstAddress));
    }
    pState = maybefirstMemoryAndOffset.getSMGState();
    SMGObject firstObject = maybefirstMemoryAndOffset.getSMGObject();

    // The object may be null if no such object exists, check and log if 0
    if (firstObject.isZero()) {
      return ValueAndSMGState.ofUnknownValue(
          pState.withNullPointerDereferenceWhenReading(firstObject));
    }

    // The offset of the pointer used. (the pointer might point to a offset != 0, the other offset
    // needs to the added to that!)
    BigInteger firstOffsetInBits =
        maybefirstMemoryAndOffset.getOffsetForObject().add(pFirstOffsetInBits);

    // The same for the second address
    SMGStateAndOptionalSMGObjectAndOffset maybeSecondMemoryAndOffset =
        pState.dereferencePointer(secondAddress);
    if (!maybeSecondMemoryAndOffset.hasSMGObjectAndOffset()) {
      // The value is unknown and therefore does not point to a valid memory location
      return ValueAndSMGState.ofUnknownValue(
          maybeSecondMemoryAndOffset
              .getSMGState()
              .withUnknownPointerDereferenceWhenReading(secondAddress));
    }
    pState = maybeSecondMemoryAndOffset.getSMGState();
    SMGObject secondObject = maybeSecondMemoryAndOffset.getSMGObject();

    // The object may be null if no such object exists, check and log if 0
    if (secondObject.isZero()) {
      return ValueAndSMGState.ofUnknownValue(
          pState.withNullPointerDereferenceWhenWriting(secondObject));
    }

    // The offset of the pointer used. (the pointer might point to a offset != 0, the other offset
    // needs to the added to that!)
    BigInteger secondOffsetInBits =
        maybeSecondMemoryAndOffset.getOffsetForObject().add(pSecondOffsetInBits);

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
          readValue(currentState, firstObject, firstOffsetInBits, sizeOfCharInBits, null);
      Value value1 = valueAndState1.getValue();
      currentState = valueAndState1.getState();

      if (value1.isUnknown()) {
        return ValueAndSMGState.ofUnknownValue(currentState);
      }

      ValueAndSMGState valueAndState2 =
          readValue(currentState, secondObject, secondOffsetInBits, sizeOfCharInBits, null);
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

  /**
   * @param value {@link Value} to be checked.
   * @return true if the entered value is a {@link AddressExpression} or a {@link
   *     SymbolicIdentifier} with a {@link MemoryLocation}.
   */
  public static boolean valueIsAddressExprOrVariableOffset(Value value) {
    if (value == null) {
      return false;
    }
    return value instanceof AddressExpression
        || ((value instanceof SymbolicIdentifier)
            && ((SymbolicIdentifier) value).getRepresentedLocation().isPresent());
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

  public static class SMG2SizeofVisitor extends BaseSizeofVisitor {

    private final MachineModel model;
    private final SMGState state;
    private final LogManagerWithoutDuplicates logger;
    private final SMGCPAValueExpressionEvaluator evaluator;
    private final SMGOptions options;

    protected SMG2SizeofVisitor(
        MachineModel model,
        SMGCPAValueExpressionEvaluator evaluator,
        SMGState state,
        LogManagerWithoutDuplicates logger,
        SMGOptions options) {
      super(model);
      this.model = model;
      this.state = state;
      this.logger = logger;
      this.evaluator = evaluator;
      this.options = options;
    }

    @Override
    public BigInteger visit(CArrayType pArrayType) throws IllegalArgumentException {
      // TODO: Take possible padding into account

      CExpression arrayLength = pArrayType.getLength();
      BigInteger sizeOfType = model.getSizeof(pArrayType.getType());

      if (arrayLength instanceof CIntegerLiteralExpression) {
        BigInteger length = ((CIntegerLiteralExpression) arrayLength).getValue();
        return length.multiply(sizeOfType);
      }

      if (arrayLength == null) {
        return super.visit(pArrayType);
      }

      // Try get the length variable for arrays with variable length
      try {
        for (ValueAndSMGState lengthValueAndState :
            arrayLength.accept(
                new SMGCPAValueVisitor(
                    evaluator,
                    state,
                    new DummyCFAEdge(CFANode.newDummyCFANode(), CFANode.newDummyCFANode()),
                    logger))) {
          Value lengthValue = lengthValueAndState.getValue();
          // We simply ignore the State for this
          // Thats theoretically not sound as the read might fail!
          if (lengthValue.isNumericValue()) {
            return lengthValue.asNumericValue().bigInteger().multiply(sizeOfType);
          } else if (options.isGuessSizeOfUnknownMemorySize()) {
            return options.getGuessSize().multiply(sizeOfType);
          }
        }
      } catch (CPATransferException e) {
        // Just stop the analysis for critical errors
      }
      throw new AssertionError(
          "Could not determine variable array length for length "
              + arrayLength.toASTString()
              + " and array type "
              + pArrayType.getType()
              + ". Try the options GuessSizeOfUnknownMemory.");
    }
  }

  /**
   * Write the valueToWrite Value into the newly created variable (by this method) with the name
   * qualifiedVarName. Might return the given state for blacklisted variables or errors.
   *
   * @param valueToWrite the Value you want to write. Might be AddressExpression for pointers or
   *     SymbolicIdentifier with MemoryLocation for structure copies.
   * @param leftHandSideType the type of the left side memory.
   * @param rightHandSideType the type of the right hand side value.
   * @param qualifiedVarName qualified variable name.
   * @param pState current state.
   * @return a new state with either the value written, or an error state or just a state for writes
   *     that can't be completed.
   * @throws SMG2Exception in case of critical errors.
   */
  public SMGState writeValueToNewVariableBasedOnTypes(
      Value valueToWrite,
      CType leftHandSideType,
      CType rightHandSideType,
      String qualifiedVarName,
      SMGState pState)
      throws SMG2Exception {
    SMGState currentState = pState;
    // Parameter type is left hand side type
    CType parameterType = SMGCPAValueExpressionEvaluator.getCanonicalType(leftHandSideType);
    CType valueType = SMGCPAValueExpressionEvaluator.getCanonicalType(rightHandSideType);
    if (parameterType instanceof CArrayType && ((CArrayType) parameterType).getLength() == null) {
      // TODO: it is a bug actually. The size should be returned correctly. Check if its fixed
      // from time to time.
      // If its declared as array[] we use the size of the old array
      parameterType = valueType;
    }
    BigInteger paramSizeInBits = getBitSizeof(currentState, parameterType);

    // Create the new local variable
    currentState =
        currentState.copyAndAddLocalVariable(paramSizeInBits, qualifiedVarName, parameterType);
    Optional<SMGObject> maybeObject =
        currentState.getMemoryModel().getObjectForVisibleVariable(qualifiedVarName);
    if (maybeObject.isEmpty()) {
      // If this is empty it means that the variable is on the blacklist, skip
      return currentState;
    }
    SMGObject newVariableMemory = maybeObject.orElseThrow();
    BigInteger ZeroOffsetInBits = BigInteger.ZERO;

    if (valueToWrite instanceof AddressExpression) {
      // This is either a pointer to be written or this points to a memory region
      // to be copied depending on the type
      AddressExpression paramAddrExpr = (AddressExpression) valueToWrite;
      Value paramAddrOffsetValue = paramAddrExpr.getOffset();

      if (SMGCPAValueExpressionEvaluator.isStructOrUnionType(parameterType)
          || parameterType instanceof CArrayType) {

        if (!paramAddrOffsetValue.isNumericValue()) {
          // Just continue for now. Reading not inited memory is unknown anyway.
          return currentState;
        }

        // We need a true pointer without AddressExpr
        ValueAndSMGState properPointerAndState =
            transformAddressExpressionIntoPointerValue(paramAddrExpr, currentState);
        currentState = properPointerAndState.getState();

        Optional<SMGObjectAndOffset> maybeParamMemoryAndOffset =
            currentState.getPointsToTarget(properPointerAndState.getValue());

        if (maybeParamMemoryAndOffset.isEmpty()) {
          return currentState;
        }
        SMGObjectAndOffset paramMemoryAndOffset = maybeParamMemoryAndOffset.orElseThrow();

        // copySMGObjectContentToSMGObject checks for sizes etc.
        return currentState.copySMGObjectContentToSMGObject(
            paramMemoryAndOffset.getSMGObject(),
            paramMemoryAndOffset.getOffsetForObject(),
            newVariableMemory,
            ZeroOffsetInBits,
            newVariableMemory.getSize());
      } else if (parameterType instanceof CPointerType || parameterType instanceof CSimpleType) {
        // Sometimes a pointer is casted to a long or smth
        if (!paramAddrOffsetValue.isNumericValue()) {
          // Write unknown for unknown offset
          return currentState.writeToStackOrGlobalVariable(
              qualifiedVarName,
              ZeroOffsetInBits,
              paramSizeInBits,
              UnknownValue.getInstance(),
              parameterType);
        }

        ValueAndSMGState properPointerAndState =
            transformAddressExpressionIntoPointerValue(paramAddrExpr, currentState);

        return properPointerAndState
            .getState()
            .writeToStackOrGlobalVariable(
                qualifiedVarName,
                ZeroOffsetInBits,
                paramSizeInBits,
                properPointerAndState.getValue(),
                parameterType);
      } else {
        throw new SMG2Exception(
            "Missing type handling when writing a pointer to a " + parameterType + ".");
      }

    } else if (valueToWrite instanceof SymbolicIdentifier
        && ((SymbolicIdentifier) valueToWrite).getRepresentedLocation().isPresent()) {
      return copyStructOrArrayFromValueTo(
          valueToWrite, parameterType, newVariableMemory, ZeroOffsetInBits, currentState);

    } else {
      // Just write the value
      return currentState.writeToStackOrGlobalVariable(
          qualifiedVarName, ZeroOffsetInBits, paramSizeInBits, valueToWrite, parameterType);
    }
  }

  /**
   * Copies the memory behind the rightHandSideValue Value into the given left hand side memory.
   *
   * @param rightHandSideValue a Value that is a SymbolicIdentifier with location present. Source of
   *     the copy.
   * @param leftHandSideType left hand side type used to check the operation.
   * @param leftHandSideMemory the memory that the source is to be copied to.
   * @param leftHandSideOffset the offset where to begin to copy to on the left hand side.
   * @param pState current state.
   * @return a new state with the copy performed. Or the current one for errors.
   */
  public SMGState copyStructOrArrayFromValueTo(
      Value rightHandSideValue,
      CType leftHandSideType,
      SMGObject leftHandSideMemory,
      BigInteger leftHandSideOffset,
      SMGState pState) {
    // A SymbolicIdentifier with location is used to copy entire variable structures (i.e.
    // arrays/structs etc.). We allow arrays here for function parameters.
    SMGState currentState = pState;
    Preconditions.checkArgument(
        rightHandSideValue instanceof SymbolicIdentifier
            && ((SymbolicIdentifier) rightHandSideValue).getRepresentedLocation().isPresent());
    Preconditions.checkArgument(
        SMGCPAValueExpressionEvaluator.isStructOrUnionType(leftHandSideType)
            || leftHandSideType instanceof CArrayType);

    MemoryLocation memLocRight =
        ((SymbolicIdentifier) rightHandSideValue).getRepresentedLocation().orElseThrow();
    String paramIdentifier = memLocRight.getIdentifier();
    BigInteger paramBaseOffset = BigInteger.valueOf(memLocRight.getOffset());

    // Get the SMGObject for the memory region on the right hand side and copy the entire
    // region  into the left hand side
    Optional<SMGObject> maybeRightHandSideMemory =
        currentState.getMemoryModel().getObjectForVisibleVariable(paramIdentifier);

    if (maybeRightHandSideMemory.isEmpty()) {
      // This might be called from the the function call handler which just created a stack frame
      maybeRightHandSideMemory =
          currentState.getMemoryModel().getObjectForVisibleVariable(paramIdentifier);
      if (maybeRightHandSideMemory.isEmpty()) {
        return currentState;
      }
    }
    SMGObject paramMemory = maybeRightHandSideMemory.orElseThrow();
    // copySMGObjectContentToSMGObject checks for sizes etc.
    return currentState.copySMGObjectContentToSMGObject(
        paramMemory,
        paramBaseOffset,
        leftHandSideMemory,
        leftHandSideOffset,
        leftHandSideMemory.getSize().subtract(leftHandSideOffset));
  }

  /**
   * Creates (or re-uses) a variable for the name given. The variable is either on the stack, global
   * or externally allocated.
   *
   * @param pState current {@link SMGState}
   * @param pVarDecl declaration of the variable declared.
   * @param pEdge current CFAEdge
   * @return a new state with the variable declared and initialized.
   * @throws CPATransferException in case of critical errors.
   */
  public List<SMGState> handleVariableDeclaration(
      SMGState pState, CVariableDeclaration pVarDecl, CFAEdge pEdge) throws CPATransferException {
    // Don't check for existing variables or else a edge that declares a existing variable is not
    // changed!
    String varName = pVarDecl.getQualifiedName();
    CType cType = SMGCPAValueExpressionEvaluator.getCanonicalType(pVarDecl);

    SMGState currentState = pState;
    // Remove previously invalidated objects and create them anew
    if (pState.isLocalOrGlobalVariablePresent(varName)
        && !pState.isLocalOrGlobalVariableValid(varName)) {
      currentState = pState.copyAndRemoveStackVariable(varName);
    }

    // There can only be one declaration result state
    return handleInitializerForDeclaration(
        handleVariableDeclarationWithoutInizializer(currentState, pVarDecl, pEdge).get(0),
        varName,
        pVarDecl,
        cType,
        pEdge);
  }

  public List<SMGState> handleVariableDeclarationWithoutInizializer(
      SMGState pState, CVariableDeclaration pVarDecl, CFAEdge pEdge) throws CPATransferException {
    String varName = pVarDecl.getQualifiedName();
    if (pState.isLocalOrGlobalVariablePresent(varName)) {
      return ImmutableList.of(pState);
    }

    CType cType = SMGCPAValueExpressionEvaluator.getCanonicalType(pVarDecl);
    boolean isExtern = pVarDecl.getCStorageClass().equals(CStorageClass.EXTERN);

    if (cType.isIncomplete() && cType instanceof CElaboratedType) {
      // for incomplete types, we do not add variables.
      // we are not allowed to read or write them, dereferencing is possible.
      // example: "struct X; extern struct X var; void main() { }"
      // TODO currently we assume that only CElaboratedTypes are unimportant when incomplete.
      return ImmutableList.of(pState);
    }

    /*
     *  If the variable exists it does so because of loops etc.
     *  Invalid declarations should be already caught by the parser.
     */
    SMGState newState = pState;
    if (!newState.checkVariableExists(newState, varName)
        && (!isExtern || options.getAllocateExternalVariables())) {
      BigInteger typeSizeInBits = getBitSizeof(newState, cType);
      if (cType instanceof CArrayType
          && ((CArrayType) cType).getLength() == null
          && pVarDecl.getInitializer() != null) {
        // For some reason the type size is not always correct.
        // in the case: static const char array[] = "blablabla"; for example the cType
        // is just const char[] and returns pointer size. We try to get it from the
        // initializer
        CInitializer init = pVarDecl.getInitializer();
        if (init instanceof CInitializerExpression) {
          CExpression initExpr = ((CInitializerExpression) init).getExpression();
          if (initExpr instanceof CStringLiteralExpression) {
            typeSizeInBits =
                BigInteger.valueOf(8)
                    .multiply(
                        BigInteger.valueOf(
                            (((CStringLiteralExpression) initExpr).getContentString().length()
                                + 1)));
          } else {
            throw new SMG2Exception(
                "Could not determine correct type size for an array for initializer expression: "
                    + init);
          }
        } else if (init instanceof CInitializerList) {
          CInitializerList initList = ((CInitializerList) init);
          CType realCType = cType.getCanonicalType();

          if (realCType instanceof CArrayType) {
            CArrayType arrayType = (CArrayType) realCType;
            CType memberType = SMGCPAValueExpressionEvaluator.getCanonicalType(arrayType.getType());
            BigInteger memberTypeSize = getBitSizeof(pState, memberType);
            BigInteger numberOfMembers = BigInteger.valueOf(initList.getInitializers().size());
            typeSizeInBits =
                BigInteger.valueOf(8).multiply(memberTypeSize).multiply(numberOfMembers);

          } else if (realCType instanceof CCompositeType) {
            CCompositeType structType = (CCompositeType) realCType;
            typeSizeInBits = BigInteger.valueOf(8).multiply(getBitSizeof(pState, structType));
          }
        } else {
          throw new SMG2Exception(
              "Could not determine correct type size for an array for initializer expression: "
                  + init);
        }
      }

      // Handle incomplete type of external variables as externally allocated
      if (options.isHandleIncompleteExternalVariableAsExternalAllocation()
          && cType.isIncomplete()
          && isExtern) {
        typeSizeInBits = BigInteger.valueOf(options.getExternalAllocationSize());
      }
      if (pVarDecl.isGlobal()) {
        newState = pState.copyAndAddGlobalVariable(typeSizeInBits, varName, cType);
      } else {
        newState = pState.copyAndAddLocalVariable(typeSizeInBits, varName, cType);
      }
    }
    return ImmutableList.of(newState);
  }

  /**
   * This method expects that there is a variable (global or otherwise) existing under the name
   * entered with the corect size allocated. This also expects that the type is correct. This method
   * will write globals to 0 and handle futher initialization of variables if necessary.
   *
   * @param pState current {@link SMGState}.
   * @param pVarName name of the variable to be initialized. This var should be present on the
   *     memory model with the correct size.
   * @param pVarDecl {@link CVariableDeclaration} for the variable.
   * @param cType {@link CType} of the variable.
   * @param pEdge {@link CDeclarationEdge} for the declaration.
   * @return a list of states with the variable initialized.
   * @throws CPATransferException if something goes wrong
   */
  private List<SMGState> handleInitializerForDeclaration(
      SMGState pState, String pVarName, CVariableDeclaration pVarDecl, CType cType, CFAEdge pEdge)
      throws CPATransferException {
    CInitializer newInitializer = pVarDecl.getInitializer();
    SMGState currentState = pState;

    if (pVarDecl.isGlobal()) {
      // Global vars are always initialized to 0
      // Don't nullify external variables
      if (pVarDecl.getCStorageClass().equals(CStorageClass.EXTERN)) {
        if (options.isHandleIncompleteExternalVariableAsExternalAllocation()) {
          currentState = currentState.setExternallyAllocatedFlag(pVarName);
        }
      } else {
        // Global variables (but not extern) without initializer are nullified in C
        currentState = currentState.writeToStackOrGlobalVariableToZero(pVarName, cType);
      }
    }

    if (newInitializer != null) {
      return handleInitializer(
          currentState, pVarDecl, pEdge, pVarName, BigInteger.ZERO, cType, newInitializer);
    }

    return ImmutableList.of(currentState);
  }

  /*
   * Handles initializing of just declared variables. I.e. int bla = 5; This expects global vars to be already written to 0.
   */
  private List<SMGState> handleInitializer(
      SMGState pNewState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CType pLValueType,
      CInitializer pInitializer)
      throws CPATransferException {

    if (pInitializer instanceof CInitializerExpression) {
      CExpression expression = ((CInitializerExpression) pInitializer).getExpression();
      // string literal handling
      if (expression instanceof CStringLiteralExpression) {
        return handleStringInitializer(
            pNewState,
            pVarDecl,
            pEdge,
            variableName,
            pOffset,
            pLValueType,
            pInitializer.getFileLocation(),
            (CStringLiteralExpression) expression);
      } else if (expression instanceof CCastExpression) {
        // handle casting on initialization like 'char *str = (char *)"string";'
        return handleCastInitializer(
            pNewState,
            pVarDecl,
            pEdge,
            variableName,
            pOffset,
            pLValueType,
            pInitializer.getFileLocation(),
            (CCastExpression) expression);
      } else {
        return writeCExpressionToLocalOrGlobalVariable(
            pNewState, pEdge, variableName, pOffset, pLValueType, expression);
      }
    } else if (pInitializer instanceof CInitializerList) {
      CInitializerList pNewInitializer = ((CInitializerList) pInitializer);
      CType realCType = pLValueType.getCanonicalType();

      if (realCType instanceof CArrayType) {
        CArrayType arrayType = (CArrayType) realCType;
        return handleInitializerList(
            pNewState, pVarDecl, pEdge, variableName, pOffset, arrayType, pNewInitializer);
      } else if (realCType instanceof CCompositeType) {
        CCompositeType structType = (CCompositeType) realCType;
        return handleInitializerList(
            pNewState, pVarDecl, pEdge, variableName, pOffset, structType, pNewInitializer);
      }

      // Type cannot be resolved
      logger.log(
          Level.INFO,
          () ->
              String.format(
                  "Type %s cannot be resolved sufficiently to handle initializer %s",
                  realCType.toASTString(""), pNewInitializer));
      return ImmutableList.of(pNewState);

    } else if (pInitializer instanceof CDesignatedInitializer) {
      throw new AssertionError(
          "Error in handling initializer, designated Initializer "
              + pInitializer.toASTString()
              + " should not appear at this point.");

    } else {
      throw new UnrecognizedCodeException("Did not recognize Initializer", pInitializer);
    }
  }

  /*
   * Handles castings when initializing variables. I.e. = (char) 55;
   */
  private List<SMGState> handleCastInitializer(
      SMGState pNewState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CType pLValueType,
      FileLocation pFileLocation,
      CCastExpression pExpression)
      throws CPATransferException {
    CExpression expression = pExpression.getOperand();
    if (expression instanceof CStringLiteralExpression) {
      return handleStringInitializer(
          pNewState,
          pVarDecl,
          pEdge,
          variableName,
          pOffset,
          pLValueType,
          pFileLocation,
          (CStringLiteralExpression) expression);
    } else if (expression instanceof CCastExpression) {
      return handleCastInitializer(
          pNewState,
          pVarDecl,
          pEdge,
          variableName,
          pOffset,
          pLValueType,
          pFileLocation,
          (CCastExpression) expression);
    } else {
      return writeCExpressionToLocalOrGlobalVariable(
          pNewState, pEdge, variableName, pOffset, pLValueType, expression);
    }
  }

  /*
   * Handles and inits, to the variable given, the given CInitializerList initializers.
   * In this case composite types like structs and unions.
   */
  private List<SMGState> handleInitializerList(
      SMGState pState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CCompositeType pLValueType,
      CInitializerList pNewInitializer)
      throws CPATransferException {

    int listCounter = 0;

    List<CCompositeType.CCompositeTypeMemberDeclaration> memberTypes = pLValueType.getMembers();
    // Member -> offset map
    Map<CCompositeType.CCompositeTypeMemberDeclaration, BigInteger> offsetAndPosition =
        machineModel.getAllFieldOffsetsInBits(pLValueType);

    SMGState currentState = pState;

    for (CInitializer initializer : pNewInitializer.getInitializers()) {
      // TODO: this has to be checked with a test!!!!
      CType memberType = memberTypes.get(0).getType();
      if (initializer instanceof CDesignatedInitializer) {
        List<CDesignator> designators = ((CDesignatedInitializer) initializer).getDesignators();
        initializer = ((CDesignatedInitializer) initializer).getRightHandSide();
        Preconditions.checkArgument(designators.size() == 1);

        for (CCompositeTypeMemberDeclaration memTypes : memberTypes) {
          if (memTypes.getName().equals(((CFieldDesignator) designators.get(0)).getFieldName())) {
            memberType = memTypes.getType();
            break;
          }
        }
      } else {
        memberType = memberTypes.get(listCounter).getType();
      }

      // The offset is the base offset given + the current offset
      BigInteger offset = pOffset.add(offsetAndPosition.get(memberTypes.get(listCounter)));

      List<SMGState> newStates =
          handleInitializer(
              currentState, pVarDecl, pEdge, variableName, offset, memberType, initializer);

      // If this ever fails: branch into the new states and perform the rest of the loop on both!
      Preconditions.checkArgument(newStates.size() == 1);
      currentState = newStates.get(0);
      // finalStates.addAll(newStates);
      listCounter++;
    }
    return ImmutableList.of(currentState);
  }

  /*
   * Handles and inits, to the variable given, the given CInitializerList initializers. In this case arrays.
   */
  private List<SMGState> handleInitializerList(
      SMGState pState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CArrayType pLValueType,
      CInitializerList pNewInitializer)
      throws CPATransferException {

    CType memberType = SMGCPAValueExpressionEvaluator.getCanonicalType(pLValueType.getType());
    BigInteger memberTypeSize = getBitSizeof(pState, memberType);

    // ImmutableList.Builder<SMGState> finalStates = ImmutableList.builder();
    SMGState currentState = pState;
    BigInteger offset = pOffset;
    for (CInitializer initializer : pNewInitializer.getInitializers()) {
      // TODO: this has to be checked with a test!!!!
      if (initializer instanceof CDesignatedInitializer) {
        initializer = ((CDesignatedInitializer) initializer).getRightHandSide();
      }

      List<SMGState> newStates =
          handleInitializer(
              currentState, pVarDecl, pEdge, variableName, offset, memberType, initializer);

      offset = offset.add(memberTypeSize);

      // If this ever fails we have to split the rest of the initializer such that all states are
      // treated the same from this point onwards
      Preconditions.checkArgument(newStates.size() == 1);
      currentState = newStates.get(0);
    }

    return ImmutableList.of(currentState);
  }

  /*
   * Handle string literal expression initializer:
   * if a string initializer is used with a pointer:
   * - create a new memory for string expression (temporary array)
   * - call #handleInitializer for new region and string expression
   * - create pointer for new region and initialize pointer with it
   * else
   *  - create char array from string and call list init for given memory
   */
  private List<SMGState> handleStringInitializer(
      SMGState pState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CType pCurrentExpressionType,
      FileLocation pFileLocation,
      CStringLiteralExpression pExpression)
      throws CPATransferException {

    // If this is a pointer (i.e. char * name = "iAmAString";) we actually have not yet initialized
    // the memory for the String, just the pointer. So we need to create new memory for the String,
    // write the String into it, make a pointer to the beginning and save that in the char *.
    if (pCurrentExpressionType instanceof CPointerType) {
      // create a new memory region for the string (right hand side)
      CType stringArrayType = pExpression.transformTypeToArrayType();
      String stringVarName = "_" + pExpression.getContentString() + "_STRING_LITERAL";
      // If the var exists we change the name and create a new one
      // (Don't reuse an old variable! They might be different than the new one!)
      int num = 0;
      while (pState.isGlobalVariablePresent(stringVarName + num)) {
        num++;
      }
      stringVarName += num;

      BigInteger sizeOfString = getBitSizeof(pState, stringArrayType);
      SMGState currentState =
          pState.copyAndAddGlobalVariable(sizeOfString, stringVarName, stringArrayType);
      List<SMGState> initedStates =
          transformStringToArrayAndInitialize(
              currentState,
              pVarDecl,
              pEdge,
              stringVarName,
              BigInteger.ZERO,
              pFileLocation,
              pExpression);

      ImmutableList.Builder<SMGState> stateBuilder = ImmutableList.builder();
      for (SMGState initedState : initedStates) {
        // Now create a pointer to the String memory and save that in the original variable
        ValueAndSMGState addressAndState =
            createAddressForLocalOrGlobalVariable(stringVarName, initedState);
        SMGState addressState = addressAndState.getState();
        stateBuilder.add(
            addressState.writeToStackOrGlobalVariable(
                variableName,
                pOffset,
                getBitSizeof(addressState, pCurrentExpressionType),
                addressAndState.getValue(),
                pCurrentExpressionType));
      }
      return stateBuilder.build();
    }

    return transformStringToArrayAndInitialize(
        pState, pVarDecl, pEdge, variableName, pOffset, pFileLocation, pExpression);
  }

  private List<SMGState> transformStringToArrayAndInitialize(
      SMGState pState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      FileLocation pFileLocation,
      CStringLiteralExpression pExpression)
      throws CPATransferException {
    // Create a char array from string and call list init
    ImmutableList.Builder<CInitializer> charArrayInitialziersBuilder = ImmutableList.builder();
    CArrayType arrayType = pExpression.transformTypeToArrayType();
    for (CCharLiteralExpression charLiteralExp : pExpression.expandStringLiteral(arrayType)) {
      charArrayInitialziersBuilder.add(new CInitializerExpression(pFileLocation, charLiteralExp));
    }
    return handleInitializerList(
        pState,
        pVarDecl,
        pEdge,
        variableName,
        pOffset,
        arrayType,
        new CInitializerList(pFileLocation, charArrayInitialziersBuilder.build()));
  }

  /*
   * TODO: important: replace by the write above!
   * Writes valueToWrite (Some CExpression that does not lead to multiple values) into the
   * variable with the name given at the offset given. The type given is used for the size.
   */
  private List<SMGState> writeCExpressionToLocalOrGlobalVariable(
      SMGState pState,
      CFAEdge cfaEdge,
      String variableName,
      BigInteger pOffsetInBits,
      CType pWriteType,
      CExpression exprToWrite)
      throws CPATransferException {
    Preconditions.checkArgument(!(exprToWrite instanceof CStringLiteralExpression));
    CType typeOfValueToWrite = SMGCPAValueExpressionEvaluator.getCanonicalType(exprToWrite);
    CType typeOfWrite = SMGCPAValueExpressionEvaluator.getCanonicalType(pWriteType);
    BigInteger sizeOfTypeLeft = getBitSizeof(pState, typeOfWrite);
    ImmutableList.Builder<SMGState> resultStatesBuilder = ImmutableList.builder();
    SMGState currentState = pState;

    if (SMGCPAValueExpressionEvaluator.isStructOrUnionType(typeOfWrite)) {
      // Copy of the entire structure instead of just a write
      // Source == right hand side
      for (SMGStateAndOptionalSMGObjectAndOffset sourceObjectAndOffsetOrState :
          exprToWrite.accept(new SMGCPAAddressVisitor(this, pState, cfaEdge, logger))) {
        if (!sourceObjectAndOffsetOrState.hasSMGObjectAndOffset()) {
          resultStatesBuilder.add(sourceObjectAndOffsetOrState.getSMGState());
          continue;
        }
        currentState = sourceObjectAndOffsetOrState.getSMGState();
        Preconditions.checkArgument(pOffsetInBits.intValueExact() == 0);

        Optional<SMGObjectAndOffset> maybeLeftHandSideVariableObject =
            getTargetObjectAndOffset(currentState, variableName);
        if (maybeLeftHandSideVariableObject.isEmpty()) {
          throw new SMG2Exception("Usage of undeclared variable: " + variableName + ".");
        }
        SMGObject addressToWriteTo = maybeLeftHandSideVariableObject.orElseThrow().getSMGObject();
        BigInteger offsetToWriteTo =
            maybeLeftHandSideVariableObject.orElseThrow().getOffsetForObject();

        resultStatesBuilder.add(
            currentState.copySMGObjectContentToSMGObject(
                sourceObjectAndOffsetOrState.getSMGObject(),
                sourceObjectAndOffsetOrState.getOffsetForObject(),
                addressToWriteTo,
                offsetToWriteTo,
                addressToWriteTo.getSize().subtract(offsetToWriteTo)));
      }

    } else if (typeOfWrite instanceof CPointerType && typeOfValueToWrite instanceof CArrayType) {
      // Implicit & on the array expr
      for (ValueAndSMGState addressAndState : createAddress(exprToWrite, currentState, cfaEdge)) {
        Value addressToAssign = addressAndState.getValue();
        currentState = addressAndState.getState();
        resultStatesBuilder.add(
            currentState.writeToStackOrGlobalVariable(
                variableName, pOffsetInBits, sizeOfTypeLeft, addressToAssign, typeOfWrite));
      }

    } else {
      // Just a normal write
      SMGCPAValueVisitor vv = new SMGCPAValueVisitor(this, pState, cfaEdge, logger);
      for (ValueAndSMGState valueAndState : vv.evaluate(exprToWrite, typeOfWrite)) {

        ValueAndSMGState valueAndStateToAssign =
            unpackAddressExpression(valueAndState.getValue(), valueAndState.getState(), cfaEdge);
        Value valueToAssign = valueAndStateToAssign.getValue();
        currentState = valueAndStateToAssign.getState();

        if (valueToAssign instanceof SymbolicIdentifier) {
          Preconditions.checkArgument(
              ((SymbolicIdentifier) valueToAssign).getRepresentedLocation().isEmpty());
        }

        resultStatesBuilder.add(
            currentState.writeToStackOrGlobalVariable(
                variableName, pOffsetInBits, sizeOfTypeLeft, valueToAssign, typeOfWrite));
      }
    }
    return resultStatesBuilder.build();
  }
}
