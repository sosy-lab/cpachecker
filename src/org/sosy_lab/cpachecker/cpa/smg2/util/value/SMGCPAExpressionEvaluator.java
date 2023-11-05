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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAAddressVisitor;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPABuiltins;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAExportOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAValueVisitor;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.BooleanAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.ConstraintFactory;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.SMGConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
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
import org.sosy_lab.java_smt.api.SolverException;

public class SMGCPAExpressionEvaluator {

  private final SMGCPAExportOptions exportSMGOptions;
  private final SMGOptions options;
  private final LogManagerWithoutDuplicates logger;
  private final MachineModel machineModel;

  private final SMGCPABuiltins builtins;

  private final SMGConstraintsSolver solver;

  public SMGCPAExpressionEvaluator(
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      SMGCPAExportOptions pExportSMGOptions,
      SMGOptions pSMGOptions,
      SMGConstraintsSolver pSolver) {
    logger = pLogger;
    machineModel = pMachineModel;
    exportSMGOptions = pExportSMGOptions;
    options = pSMGOptions;
    builtins = new SMGCPABuiltins(this, options, exportSMGOptions, machineModel, logger);
    solver = pSolver;
  }

  public SMGCPABuiltins getBuiltinFunctionHandler() {
    return builtins;
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
   * @throws SMGException should not happen in this context
   */
  public ValueAndSMGState transformAddressExpressionIntoPointerValue(
      AddressExpression addressExpression, SMGState currentState) throws SMGException {
    Value offset = addressExpression.getOffset();
    if (!offset.isNumericValue()) {
      return ValueAndSMGState.ofUnknownValue(currentState);
    }

    if (offset.asNumericValue().bigIntegerValue().compareTo(BigInteger.ZERO) == 0) {
      // offset == 0 -> known pointer
      return ValueAndSMGState.of(addressExpression.getMemoryAddress(), currentState);
    } else {
      // Offset known but not 0, search for/create the correct address
      List<ValueAndSMGState> pointers =
          findOrcreateNewPointer(
              addressExpression.getMemoryAddress(),
              offset.asNumericValue().bigIntegerValue(),
              currentState);
      Preconditions.checkArgument(pointers.size() == 1);
      // It is impossible for 0+ list abstractions to happen in this context -> only 1 return value
      return pointers.get(0);
    }
  }

  /**
   * Given 2 address Values, left == right, this checks whether they are considered equal in the
   * SPC/SMG of the given state. This returns a Value with the result, which is a boolean (1 or 0).
   * Note: this returns always false (0) if one of the 2 given Values is no valid address.
   *
   * @param leftValue the left hand side address of the equality.
   * @param rightValue the right hand side address of the equality.
   * @param state the current state in which the 2 values are address values.
   * @return a {@link Value} that is either 1 or 0 as true and false result of the equality.
   * @throws SMGException in case of critical errors
   */
  public Value checkEqualityForAddresses(Value leftValue, Value rightValue, SMGState state)
      throws SMGException {
    Value isNotEqual = checkNonEqualityForAddresses(leftValue, rightValue, state);
    if (isNotEqual.isUnknown()) {
      return isNotEqual;
    }
    return isNotEqual.asNumericValue().bigIntegerValue().compareTo(BigInteger.ZERO) == 0
        ? new NumericValue(1)
        : new NumericValue(0);
  }

  /**
   * Given 2 address Values, left != right, this checks whether they are considered NOT equal in the
   * SPC/SMG of the given state. This returns a Value with the result, which is a boolean (1 or 0).
   * Note: this returns always true (1) if one of the 2 given Values is no valid address.
   *
   * @param leftValue the left hand side address of the inequality.
   * @param rightValue the right hand side address of the inequality.
   * @param state the current state in which the 2 values are address values.
   * @return a {@link Value} that is 1 (true) if the 2 addresses are not equal, 0 (false) if they
   *     are equal.
   * @throws SMGException in case of critical errors
   */
  public Value checkNonEqualityForAddresses(Value leftValue, Value rightValue, SMGState state)
      throws SMGException {
    ValueAndSMGState leftValueAndState = unpackAddressExpression(leftValue, state);
    leftValue = leftValueAndState.getValue();
    ValueAndSMGState rightValueAndState =
        unpackAddressExpression(rightValue, leftValueAndState.getState());
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
   * @return {@link ValueAndSMGState}
   * @throws SMGException in case of critical errors.
   */
  public ValueAndSMGState unpackAddressExpression(Value value, SMGState state) throws SMGException {
    if (!(value instanceof AddressExpression)) {
      return ValueAndSMGState.of(value, state);
    }
    AddressExpression address1 = (AddressExpression) value;
    Value offsetValue = address1.getOffset();
    if (offsetValue.isNumericValue()
        && offsetValue.asNumericValue().bigIntegerValue().compareTo(BigInteger.ZERO) == 0) {
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
        throw new SMGException(
            "Comparison of non numeric offset values not possible when comparing addresses.");
      }
      Value additionalOffset = targetAndOffset.getOffsetForObject();
      if (!additionalOffset.isNumericValue()) {
        return ValueAndSMGState.ofUnknownValue(state);
      }
      BigInteger offset =
          offsetValue
              .asNumericValue()
              .bigIntegerValue()
              .add(additionalOffset.asNumericValue().bigIntegerValue());

      return searchOrCreatePointer(target, offset, state);
    }
  }

  /**
   * Returns the offset in bits of the field in a struct/union type expression. Example:
   * struct.field1 with field 1 being the first field and 4 byte size, struct.field2 being the
   * second field with 4 bytes. The offset of field 1 would be 0, while the second one would be 4 *
   * 8.
   *
   * @param ownerExprType the {@link CType} of the owner of the field.
   * @param pFieldName the name of the field.
   * @return the offset in bits of a field as a {@link BigInteger}.
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

    // Should never happen
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
   * Creates memory with size sizeInBits. sizeInBits should not be 0! The memory is then invalidated
   * and added to the malloc zero map.
   *
   * @param pInitialSmgState {@link SMGState} initial state.
   * @param sizeInBits some non-zero size.
   * @return {@link ValueAndSMGState} of the pointer to the memory and its state.
   */
  public ValueAndSMGState createMallocZeroMemoryAndPointer(
      SMGState pInitialSmgState, BigInteger sizeInBits) {
    SMGObjectAndSMGState newObjectAndState = pInitialSmgState.copyAndAddHeapObject(sizeInBits);
    SMGObject newObject = newObjectAndState.getSMGObject();
    SMGState newState = newObjectAndState.getState();

    Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    // New regions always have offset 0
    SMGState finalState = newState.createAndAddPointer(addressValue, newObject, BigInteger.ZERO);
    SymbolicProgramConfiguration newSPC =
        finalState.getMemoryModel().setMemoryAsResultOfMallocZero(newObject);
    newSPC = newSPC.invalidateSMGObject(newObject);
    finalState = newState.copyAndReplaceMemoryModel(newSPC);
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
   * @throws CPATransferException if the & operator is used on an invalid expression.
   */
  public List<ValueAndSMGState> createAddress(CExpression operand, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {
    // SMGCPAAddressVisitor may have side effects! But they should not effect anything as they are
    // only interesting in a failure case in which the analysis stops!
    SMGState currentState = pState;
    SMGCPAAddressVisitor addressVisitor =
        new SMGCPAAddressVisitor(this, currentState, cfaEdge, logger, options);
    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    for (SMGStateAndOptionalSMGObjectAndOffset objectAndOffsetOrState :
        operand.accept(addressVisitor)) {

      currentState = objectAndOffsetOrState.getSMGState();
      if (!objectAndOffsetOrState.hasSMGObjectAndOffset()) {
        // Functions are not declared, but the address might be requested anyway, so we have to
        // create the address
        if (operand instanceof CIdExpression
            && SMGCPAExpressionEvaluator.getCanonicalType(operand.getExpressionType())
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
          // This is not necessarily an error! If we can't get an address because a lookup is based
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
      Value offset = objectAndOffsetOrState.getOffsetForObject();
      if (!offset.isNumericValue()) {
        resultBuilder.add(ValueAndSMGState.ofUnknownValue(objectAndOffsetOrState.getSMGState()));
        continue;
      }
      // search for existing pointer first and return if found; else make a new one
      ValueAndSMGState addressAndState =
          searchOrCreatePointer(target, offset.asNumericValue().bigIntegerValue(), currentState);
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
   * searches for an existing pointer and only creates one if none is found. May return an unknown
   * value with an error state if something goes wrong. Note: the address may have already an
   * offset, this is respected and the new offset is the address offset + the entered offset.
   *
   * @param targetAddress the targets address {@link Value} (Not AddressExpression!) that should be
   *     a valid address leading to a point-to-edge.
   * @param offsetInBits the offset that is added to the address in bits.
   * @param pState current {@link SMGState}.
   * @return either an unknown {@link Value} and an error state, or a valid new {@link Value}
   *     representing an address to the target.
   * @throws SMGException in case of critical abstract memory materilization errors.
   */
  public List<ValueAndSMGState> findOrcreateNewPointer(
      Value targetAddress, BigInteger offsetInBits, SMGState pState) throws SMGException {
    Preconditions.checkArgument(!(targetAddress instanceof AddressExpression));

    ImmutableList.Builder<ValueAndSMGState> returnBuilder = ImmutableList.builder();
    for (SMGStateAndOptionalSMGObjectAndOffset maybeTargetAndOffset :
        pState.dereferencePointer(targetAddress)) {
      if (!maybeTargetAndOffset.hasSMGObjectAndOffset()) {
        // The value is unknown and therefore does not point to a known memory location
        returnBuilder.add(ValueAndSMGState.ofUnknownValue(maybeTargetAndOffset.getSMGState()));
        continue;
      }
      // We don't want to materilize memory here?
      // pState = maybeTargetAndOffset.orElseThrow().getSMGState();
      SMGObject object = maybeTargetAndOffset.getSMGObject();

      // The object may be null, which is fine, the deref is the problem
      // The offset of the pointer used. (the pointer might point to an offset != 0, the other
      // offset needs to the added to that!)
      Value baseOffset = maybeTargetAndOffset.getOffsetForObject();
      if (!baseOffset.isNumericValue()) {
        returnBuilder.add(ValueAndSMGState.ofUnknownValue(maybeTargetAndOffset.getSMGState()));
        continue;
      }
      BigInteger finalOffsetInBits =
          baseOffset.asNumericValue().bigIntegerValue().add(offsetInBits);

      // search for existing pointer first and return if found; else make a new one for the offset
      returnBuilder.add(
          searchOrCreatePointer(object, finalOffsetInBits, maybeTargetAndOffset.getSMGState()));
    }
    return returnBuilder.build();
  }

  private ValueAndSMGState searchOrCreatePointer(
      SMGObject targetObject, BigInteger offsetInBits, SMGState pState) {
    return pState.searchOrCreateAddress(targetObject, offsetInBits);
  }

  /**
   * Creates a pointer from the numeric value entered on the state entered. The pointer is based on
   * the states numeric address assumption. For numeric values less or equal to 0 a 0 pointer is
   * returned. The pointers may also not be valid due to the offsets. This should optimally only be
   * used in this, or a similar way (int *)(int)malloc(..);
   *
   * @param numericPointer {@link NumericValue} to be transformed into a pointer.
   * @param state current {@link SMGState}
   * @return {@link ValueAndSMGState} with a potential new state and a pointer value NOT wrapped in
   *     a {@link AddressExpression}.
   */
  public ValueAndSMGState getPointerFromNumeric(Value numericPointer, SMGState state) {
    Preconditions.checkArgument(numericPointer.isNumericValue());
    if (numericPointer.asNumericValue().bigIntegerValue() != null) {
      return getPointerFromNumeric(numericPointer.asNumericValue().bigIntegerValue(), state);
    } else {
      return ValueAndSMGState.ofUnknownValue(state);
    }
  }

  /**
   * @param numericPointer {@link BigInteger} to be transformed into a pointer.
   * @param state current {@link SMGState}
   * @return {@link ValueAndSMGState} with a potential new state and a pointer value NOT wrapped in
   *     a {@link AddressExpression}.
   */
  private ValueAndSMGState getPointerFromNumeric(BigInteger numericPointer, SMGState state) {
    // TODO: replace currentMemoryAssumptionMax with a better data structure
    SMGObject bestObj = null;
    if (numericPointer.compareTo(BigInteger.ZERO) <= 0) {
      // negative or 0 -> invalid
      return ValueAndSMGState.of(new NumericValue(0), state);
    }
    // bestDifFound = min dif to any boundary of memory!
    @Nullable BigInteger bestDifFound = null;
    // bestDifToMemBeginning is dif to current best mem region 0 offset, this can be negative!
    BigInteger bestDifToMemBeginning = null;
    for (Entry<SMGObject, BigInteger> entry :
        state.getMemoryModel().getNumericAssumptionForMemoryRegionMap().entrySet()) {
      // Search for the closest entry
      BigInteger memoryRegionStart = entry.getValue();
      SMGObject object = entry.getKey();
      BigInteger memoryRegionEnd = memoryRegionStart.add(object.getSize());
      BigInteger difToZero = memoryRegionStart.subtract(numericPointer);
      BigInteger difToEnd = memoryRegionEnd.subtract(numericPointer);
      if (bestObj == null) {
        bestObj = entry.getKey();
        bestDifToMemBeginning = difToZero;
        bestDifFound = difToZero;
        if (bestDifFound.abs().compareTo(difToEnd.abs()) > 0) {
          bestDifFound = difToEnd;
        }
        if (numericPointer.compareTo(memoryRegionStart) >= 0
            && numericPointer.compareTo(memoryRegionEnd) < 0) {
          // Inside mem region
          break;
        }

      } else if (numericPointer.compareTo(memoryRegionStart) < 0) {
        // smaller than obj
        if (bestDifFound == null || difToZero.abs().compareTo(bestDifFound.abs()) < 0) {
          bestDifFound = difToZero;
          bestDifToMemBeginning = difToZero;
          bestObj = object;
        }

      } else if (numericPointer.compareTo(memoryRegionEnd) > 0) {
        // bigger than obj
        if (bestDifFound == null || difToEnd.abs().compareTo(bestDifFound.abs()) < 0) {
          bestDifFound = difToEnd;
          bestDifToMemBeginning = difToZero;
          bestObj = object;
        }

      } else {
        // is inside object
        bestObj = object;
        bestDifToMemBeginning = difToZero;
        break;
      }
    }
    return searchOrCreatePointer(bestObj, bestDifToMemBeginning, state);
  }

  /**
   * This creates or finds and returns the address Value for the given local or global variable.
   * This also creates the pointers in the SMG if not yet created. This is mainly used for the &
   * operator.
   *
   * @param variableName the variable name. The variable should exist, else an exception is thrown.
   * @param pState current {@link SMGState}
   * @return either unknown or a {@link Value} representing the address.
   */
  public ValueAndSMGState createAddressForLocalOrGlobalVariable(
      String variableName, SMGState pState) {
    // Get the variable SMGObject
    Optional<SMGObjectAndOffset> maybeObjectAndOffset =
        getTargetObjectAndOffset(pState, variableName, new NumericValue(BigInteger.ZERO));
    if (maybeObjectAndOffset.isEmpty()) {
      // TODO: improve error handling and add more specific exceptions to the visitor!
      // No address could be found
      return ValueAndSMGState.ofUnknownValue(pState);
      // throw new SMG2Exception("No address could be created for the variable: " + variableName);
    }
    SMGObjectAndOffset targetAndOffset = maybeObjectAndOffset.orElseThrow();
    SMGObject target = targetAndOffset.getSMGObject();
    Value offset = targetAndOffset.getOffsetForObject();
    if (!offset.isNumericValue()) {
      return ValueAndSMGState.ofUnknownValue(pState);
    }
    BigInteger numericOffset = offset.asNumericValue().bigIntegerValue();
    // search for existing pointer first and return if found
    Optional<SMGValue> maybeAddressValue =
        pState.getMemoryModel().getAddressValueForPointsToTarget(target, numericOffset);

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
    SMGState newState = pState.createAndAddPointer(addressValue, target, numericOffset);
    return ValueAndSMGState.of(addressValue, newState);
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
  public List<ValueAndSMGState> readStackOrGlobalVariable(
      SMGState initialState,
      String varName,
      Value offsetInBits,
      BigInteger sizeInBits,
      CType readType)
      throws CPATransferException {

    Optional<SMGObject> maybeObject =
        initialState.getMemoryModel().getObjectForVisibleVariable(varName);

    if (maybeObject.isEmpty()) {
      // If there is no object, the variable is not initialized
      SMGState errorState = initialState.withUninitializedVariableUsage(varName);
      // The Value does not matter here as the error state should always end the analysis
      return ImmutableList.of(ValueAndSMGState.ofUnknownValue(errorState));
    }
    return readValue(
        initialState,
        maybeObject.orElseThrow(),
        offsetInBits,
        sizeInBits,
        readType,
        CNumericTypes.INT);
  }

  /**
   * Read the value at the address of the supplied {@link Value} at the offset with the size (type
   * size) given.
   *
   * @param pState current {@link SMGState}.
   * @param pointerValueToDeref the {@link Value} for the address of the memory to be read. This
   *     should map to a known {@link SMGObject} or a {@link SMGPointsToEdge}.
   * @param pOffset the offset as {@link BigInteger} in bits where to start reading in the object.
   * @param pSizeInBits the size of the type to read in bits as {@link BigInteger}.
   * @param readType the type of the read value before casts etc. Used to determine union float
   *     conversion.
   * @return {@link ValueAndSMGState} tuple for the read {@link Value} and the new {@link SMGState}.
   */
  public List<ValueAndSMGState> readValueWithPointerDereference(
      SMGState pState,
      Value pointerValueToDeref,
      Value pOffset,
      BigInteger pSizeInBits,
      CType readType)
      throws SMGException, SMGSolverException {
    return readValueWithPointerDereference(
        pState, pointerValueToDeref, pOffset, pSizeInBits, readType, CNumericTypes.INT);
  }

  /**
   * Read the value at the address of the supplied {@link Value} at the offset with the size (type
   * size) given.
   *
   * @param pState current {@link SMGState}.
   * @param pointerValueToDeref the {@link Value} for the address of the memory to be read. This
   *     should map to a known {@link SMGObject} or a {@link SMGPointsToEdge}.
   * @param pOffset the offset as {@link BigInteger} in bits where to start reading in the object.
   * @param pSizeInBits the size of the type to read in bits as {@link BigInteger}.
   * @param readType the type of the read value before casts etc. Used to determine union float
   *     conversion.
   * @param pOffsetType type of the offset. Typically, CNumericTypes.INT in C.
   * @return {@link ValueAndSMGState} tuple for the read {@link Value} and the new {@link SMGState}.
   */
  public List<ValueAndSMGState> readValueWithPointerDereference(
      SMGState pState,
      Value pointerValueToDeref,
      Value pOffset,
      BigInteger pSizeInBits,
      CType readType,
      CType pOffsetType)
      throws SMGException, SMGSolverException {

    // Offsets are always interpreted as int in C
    // CType offsetType = CNumericTypes.INT;
    ImmutableList.Builder<ValueAndSMGState> returnBuilder = ImmutableList.builder();
    // Get the SMGObject for the value
    for (SMGStateAndOptionalSMGObjectAndOffset maybeTargetAndOffset :
        pState.dereferencePointer(pointerValueToDeref)) {
      if (!maybeTargetAndOffset.hasSMGObjectAndOffset()) {
        // The value is unknown and therefore does not point to a valid memory location
        SMGState errorState =
            maybeTargetAndOffset
                .getSMGState()
                .withUnknownPointerDereferenceWhenReading(pointerValueToDeref);

        returnBuilder.add(ValueAndSMGState.ofUnknownValue(errorState));
        continue;
      }
      pState = maybeTargetAndOffset.getSMGState();
      SMGObject object = maybeTargetAndOffset.getSMGObject();

      // The object may be null if no such object exists, check and log if 0
      if (object.isZero()) {
        SMGState errorState = pState.withNullPointerDereferenceWhenReading(object);
        returnBuilder.add(ValueAndSMGState.ofUnknownValue(errorState));
        continue;
      }

      // The offset of the pointer used. (the pointer might point to an offset != 0, the other
      // offset  needs to the added to that!)
      Value finalOffset = addOffsetValues(pOffset, maybeTargetAndOffset.getOffsetForObject());

      returnBuilder.addAll(
          readValue(pState, object, finalOffset, pSizeInBits, readType, pOffsetType));
    }
    return returnBuilder.build();
  }

  /**
   * Returns the {@link SMGObjectAndOffset} pair for the entered address {@link Value} and
   * additional offset on the entered state. This does dereference the address {@link Value}.
   *
   * @param pState current {@link SMGState}.
   * @param value {@link Value} pointer to be dereferenced leading to the {@link SMGObject} desired.
   * @param pOffsetInBits used offset when dereferencing.
   * @return the desired {@link SMGObject} and its offset or a State with potentially an error.
   */
  public List<SMGStateAndOptionalSMGObjectAndOffset> getTargetObjectAndOffset(
      SMGState pState, Value value, Value pOffsetInBits) throws SMGException {

    ImmutableList.Builder<SMGStateAndOptionalSMGObjectAndOffset> returnBuilder =
        ImmutableList.builder();
    for (SMGStateAndOptionalSMGObjectAndOffset targetAndOffset : pState.dereferencePointer(value)) {
      if (!targetAndOffset.hasSMGObjectAndOffset()) {
        // The value is unknown and therefore does not point to a valid memory location
        SMGState errorState =
            targetAndOffset.getSMGState().withUnknownPointerDereferenceWhenReading(value);
        returnBuilder.add(SMGStateAndOptionalSMGObjectAndOffset.of(errorState));
        continue;
      }

      Value finalOffset = addOffsetValues(targetAndOffset.getOffsetForObject(), pOffsetInBits);

      returnBuilder.add(
          SMGStateAndOptionalSMGObjectAndOffset.of(
              targetAndOffset.getSMGObject(), finalOffset, targetAndOffset.getSMGState()));
    }
    return returnBuilder.build();
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
    return getTargetObjectAndOffset(state, variableName, new NumericValue(BigInteger.ZERO));
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
      SMGState state, String variableName, Value offsetInBits) {
    // TODO: maybe use this in getStackOrGlobalVar?
    Optional<SMGObject> maybeObject =
        state.getMemoryModel().getObjectForVisibleVariable(variableName);
    if (maybeObject.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(SMGObjectAndOffset.of(maybeObject.orElseThrow(), offsetInBits));
  }

  /**
   * Calculates the distance of 2 addresses in bits. But returns unknown Value if it's not the same
   * object or unknown pointers.
   *
   * @param state the {@link SMGState} the 2 pointers are known in.
   * @param leftPointer {@link Value} left hand side pointer in the minus operation.
   * @param rightPointer {@link Value} right hand side pointer in the minus operation.
   * @return Either distance as {@link NumericValue} or {@link UnknownValue}.
   * @throws SMGException in case of critical list materilization errors
   */
  public List<ValueAndSMGState> calculateAddressDistance(
      SMGState state, Value leftPointer, Value rightPointer) throws SMGException {
    SymbolicProgramConfiguration spc = state.getMemoryModel();
    if (!spc.isPointer(leftPointer) || !spc.isPointer(rightPointer)) {
      // Not known or not known as a pointer, return nothing
      return ImmutableList.of(ValueAndSMGState.ofUnknownValue(state));
    }
    ImmutableList.Builder<ValueAndSMGState> returnBuilder = ImmutableList.builder();
    // We can only compare the underlying SMGObject for equality as the Values are distinct if they
    // point to different parts of the object. We need to compare the object because we can only
    // calculate the distance in the exact same object
    for (SMGStateAndOptionalSMGObjectAndOffset leftTargetAndOffset :
        state.dereferencePointer(leftPointer)) {
      if (!leftTargetAndOffset.hasSMGObjectAndOffset()) {
        returnBuilder.add(ValueAndSMGState.ofUnknownValue(leftTargetAndOffset.getSMGState()));
        continue;
      }

      state = leftTargetAndOffset.getSMGState();
      for (SMGStateAndOptionalSMGObjectAndOffset rightTargetAndOffset :
          state.dereferencePointer(rightPointer)) {
        if (!rightTargetAndOffset.hasSMGObjectAndOffset()) {
          returnBuilder.add(ValueAndSMGState.ofUnknownValue(rightTargetAndOffset.getSMGState()));
          continue;
        }

        state = rightTargetAndOffset.getSMGState();
        SMGObject leftTarget = leftTargetAndOffset.getSMGObject();
        SMGObject rightTarget = rightTargetAndOffset.getSMGObject();
        if (!leftTarget.equals(rightTarget)) {
          returnBuilder.add(ValueAndSMGState.ofUnknownValue(state));
          continue;
        }
        // int because this is always an int

        // TODO: handle this symbolically
        Value rightOffset = rightTargetAndOffset.getOffsetForObject();
        Value leftOffset = leftTargetAndOffset.getOffsetForObject();
        if (!rightOffset.isNumericValue() && !leftOffset.isNumericValue()) {
          returnBuilder.add(ValueAndSMGState.ofUnknownValue(state));
        }
        returnBuilder.add(
            ValueAndSMGState.of(
                new NumericValue(
                    leftOffset
                        .asNumericValue()
                        .bigIntegerValue()
                        .subtract(rightOffset.asNumericValue().bigIntegerValue())
                        .intValue()),
                state));
      }
    }
    return returnBuilder.build();
  }

  /**
   * This is the most general read that should be used in the end by all read smg methods that need
   * checks! This method checks that the offset and size of the read are in range and that the used
   * objects are valid. Might materialize a list! Might use an SMT solver if error predicates are
   * tracked and the offset is not numeric.
   *
   * @param currentState the current {@link SMGState}.
   * @param object the {@link SMGObject} to be read from.
   * @param offsetValueInBits the offset in bits as {@link Value}.
   * @param sizeInBits size of the read value in bits as {@link BigInteger}.
   * @param readType the uncasted type of the read (right hand side innermost type). Null only if
   *     its certain that implicit union casts are not possible.
   * @return {@link ValueAndSMGState} bundeling the most up-to-date state and the read value.
   * @throws SMGException for critical errors when materializing lists.
   */
  private List<ValueAndSMGState> readValue(
      SMGState currentState,
      SMGObject object,
      Value offsetValueInBits,
      BigInteger sizeInBits,
      @Nullable CType readType,
      CType pOffsetType)
      throws SMGException, SMGSolverException {
    // Check that the offset and offset + size actually fit into the SMGObject
    if (offsetValueInBits.isNumericValue()) {
      // Typical read with known offset
      BigInteger offsetInBits = offsetValueInBits.asNumericValue().bigIntegerValue();
      boolean doesNotFitIntoObject =
          offsetInBits.compareTo(BigInteger.ZERO) < 0
              || offsetInBits.add(sizeInBits).compareTo(object.getSize()) > 0;

      if (doesNotFitIntoObject) {
        // Field read does not fit size of declared Memory
        SMGState errorState = currentState.withOutOfRangeRead(object, offsetInBits, sizeInBits);
        // Unknown value that should not be used with an error state that should stop the analysis
        return ImmutableList.of(ValueAndSMGState.ofUnknownValue(errorState));
      }

      // The read in SMGState checks for validity and external allocation
      return currentState.readValue(object, offsetInBits, sizeInBits, readType);

    } else if (options.trackErrorPredicates()) {
      // Use an SMT solver to argue about the offset/size validity
      final ConstraintFactory constraintFactory =
          ConstraintFactory.getInstance(currentState, machineModel, logger, options, this, null);
      final Collection<Constraint> newConstraints =
          constraintFactory.checkValidMemoryAccess(
              offsetValueInBits,
              new NumericValue(sizeInBits),
              new NumericValue(object.getSize()),
              pOffsetType,
              currentState);

      String stackFrameFunctionName = currentState.getStackFrameTopFunctionName();

      // Iff SAT -> memory-safety is violated
      BooleanAndSMGState isUnsatAndState =
          checkMemoryConstraintsAreUnsatIndividually(
              newConstraints, stackFrameFunctionName, currentState);
      boolean isUnsat = isUnsatAndState.getBoolean();
      currentState = isUnsatAndState.getState();

      if (!isUnsat) {
        // Unknown value that should not be used with an error state that should stop the analysis
        return ImmutableList.of(
            ValueAndSMGState.ofUnknownValue(
                currentState.withOutOfRangeRead(object, offsetValueInBits, sizeInBits)));
      }
      // We can't discern the read value, but the read itself was safe
      return ImmutableList.of(ValueAndSMGState.ofUnknownValue(currentState));

    } else {
      // Unknown offset -> invalid read due to over approximation
      SMGState errorState = currentState.withUnknownOffsetMemoryAccess();
      // Unknown value that should not be used with an error state that should stop the analysis
      return ImmutableList.of(ValueAndSMGState.ofUnknownValue(errorState));
    }
  }

  /*
   * Same as readValue() but without materialization. Only to be used for stuff that does 100% never
   * encounter (useful) lists! i.e. string compare or debugging/tests. Don't use this for anything
   * that encounters floats!!!!
   */
  private ValueAndSMGState readValueWithoutMaterialization(
      SMGState currentState, SMGObject object, Value offsetValueInBits, BigInteger sizeInBits)
      throws SMGSolverException {
    // TODO: this can be refacored with readValue, so that the checks are 1 method

    if (offsetValueInBits.isNumericValue()) {
      // Typical read with known offset
      BigInteger offsetInBits = offsetValueInBits.asNumericValue().bigIntegerValue();
      boolean doesNotFitIntoObject =
          offsetInBits.compareTo(BigInteger.ZERO) < 0
              || offsetInBits.add(sizeInBits).compareTo(object.getSize()) > 0;

      if (doesNotFitIntoObject) {
        // Field read does not fit size of declared Memory
        SMGState errorState = currentState.withOutOfRangeRead(object, offsetInBits, sizeInBits);
        // Unknown value that should not be used with an error state that should stop the analysis
        return ValueAndSMGState.ofUnknownValue(errorState);
      }

      // The read in SMGState checks for validity and external allocation
      // null for type is fine as long as we don't encounter floats
      return currentState.readValueWithoutMaterialization(object, offsetInBits, sizeInBits, null);

    } else if (options.trackErrorPredicates()) {
      // Use an SMT solver to argue about the offset/size validity
      final ConstraintFactory constraintFactory =
          ConstraintFactory.getInstance(currentState, machineModel, logger, options, this, null);
      final Collection<Constraint> newConstraints =
          constraintFactory.checkValidMemoryAccess(
              offsetValueInBits,
              new NumericValue(sizeInBits),
              new NumericValue(object.getSize()),
              CNumericTypes.INT,
              currentState);

      String stackFrameFunctionName = currentState.getStackFrameTopFunctionName();

      // Iff SAT -> memory-safety is violated
      BooleanAndSMGState isUnsatAndState =
          checkMemoryConstraintsAreUnsatIndividually(
              newConstraints, stackFrameFunctionName, currentState);
      boolean isUnsat = isUnsatAndState.getBoolean();
      currentState = isUnsatAndState.getState();

      if (!isUnsat) {
        // Unknown value that should not be used with an error state that should stop the analysis
        return ValueAndSMGState.ofUnknownValue(
            currentState.withOutOfRangeRead(object, offsetValueInBits, sizeInBits));
      }

      // We can't discern the read value, but the read itself was safe
      return ValueAndSMGState.ofUnknownValue(currentState);

    } else {
      // Unknown offset -> invalid read due to over approximation
      SMGState errorState = currentState.withUnknownOffsetMemoryAccess();
      // Unknown value that should not be used with an error state that should stop the analysis
      return ValueAndSMGState.ofUnknownValue(errorState);
    }
  }

  /**
   * Returns false for SAT. True for UNSAT. Checks each given constraint individually as a memory
   * access constraint (error constraint). They will not be added to the constraints of the state.
   *
   * @param newConstraints new {@link Constraint}s to be checked/added to the {@link SMGState}.
   * @param stackFrameFunctionName {@link String} name of current Stackframe
   * @param currentState current {@link SMGState}.
   * @return BooleanAndSMGState with the bool as isUnsat and the State possibly with new constraints
   *     added to the error predicates (not regular constraints) if they were not trivial, or
   *     possibly a model added for SAT.
   * @throws SMGSolverException for {@link InterruptedException}, {@link SolverException} or {@link
   *     UnrecognizedCodeException} wrapped.
   */
  public BooleanAndSMGState checkMemoryConstraintsAreUnsatIndividually(
      Collection<Constraint> newConstraints, String stackFrameFunctionName, SMGState currentState)
      throws SMGSolverException {
    // Iff SAT -> memory-safety is violated
    for (Constraint constraint : newConstraints) {
      try {
        // If a constraint is trivial, its satisfiability is not influenced by other constraints.
        // So to evade more expensive SAT checks, we just check the constraint on its own.
        currentState = currentState.updateLastCheckedMemoryBounds(constraint);
        BooleanAndSMGState isUnsatAndState =
            solver.isUnsat(currentState, constraint, stackFrameFunctionName);
        if (!isUnsatAndState.getBoolean()) {
          return isUnsatAndState;
        }

      } catch (InterruptedException | SolverException | UnrecognizedCodeException e) {
        throw new SMGSolverException(e, currentState);
      }
    }
    // trivial fallthrough
    return BooleanAndSMGState.of(true, currentState);
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
      CFAEdge edge, CExpression leftHandSideValue, Value valueToWrite, SMGState currentState)
      throws CPATransferException {
    BigInteger sizeInBits = getBitSizeof(currentState, leftHandSideValue);
    ImmutableList.Builder<SMGState> successorsBuilder = ImmutableList.builder();
    // Get the memory for the left hand side variable
    // Write the return value into the left hand side variable
    for (SMGStateAndOptionalSMGObjectAndOffset variableMemoryAndOffsetOrState :
        leftHandSideValue.accept(
            new SMGCPAAddressVisitor(this, currentState, edge, logger, options))) {
      if (!variableMemoryAndOffsetOrState.hasSMGObjectAndOffset()) {
        // throw new SMG2Exception("No memory found to assign the value to.");
        successorsBuilder.add(variableMemoryAndOffsetOrState.getSMGState());
        continue;
      }
      currentState = variableMemoryAndOffsetOrState.getSMGState();
      SMGObject leftHandSideVariableMemory = variableMemoryAndOffsetOrState.getSMGObject();
      Value offset = variableMemoryAndOffsetOrState.getOffsetForObject();

      ValueAndSMGState castedValueAndState =
          new SMGCPAValueVisitor(this, currentState, edge, logger, options)
              .castCValue(valueToWrite, leftHandSideValue.getExpressionType(), currentState);
      valueToWrite = castedValueAndState.getValue();
      currentState = castedValueAndState.getState();

      successorsBuilder.add(
          currentState.writeValueWithChecks(
              leftHandSideVariableMemory,
              offset,
              sizeInBits,
              valueToWrite,
              leftHandSideValue.getExpressionType(),
              edge));
    }

    return successorsBuilder.build();
  }

  /** TODO: Move all type related stuff into its own class once i rework getBitSizeOf */
  public BigInteger getBitSizeof(SMGState pInitialSmgState, CExpression pExpression)
      throws CPATransferException {
    // TODO check why old implementation did not use machineModel
    // Because in abstracted SMGs we might need the current SMG to get the correct type info.
    // TODO: rework because of that.
    return getBitSizeof(pInitialSmgState, pExpression.getExpressionType());
  }

  public BigInteger getBitSizeof(SMGState pInitialSmgState, CRightHandSide pExpression)
      throws CPATransferException {
    // TODO check why old implementation did not use machineModel
    // Because in abstracted SMGs we might need the current SMG to get the correct type info.
    // TODO: rework because of that.
    return getBitSizeof(pInitialSmgState, pExpression.getExpressionType());
  }

  public BigInteger getBitSizeof(SMGState pInitialSmgState, CType pType)
      throws CPATransferException {
    // TODO check why old implementation did not use machineModel
    // Because in abstracted SMGs we might need the current SMG to get the correct type info.
    // TODO: rework because of that.
    return machineModel.getSizeofInBits(
        pType, new SMG2SizeofVisitor(machineModel, this, pInitialSmgState, logger, options));
  }

  // TODO: revisit this and decide if we want to split structs and unions because of the data
  // reinterpretation because unions will most likely not work with SMG join!
  /*
   * Structs and Union types are treated essentially the same in the SMGs. Note: Both can
   * contain methods. Unions can have data reinterpretations based on type.
   * This should be used to not confuse enums with structs/unions.
   */
  public static boolean isStructOrUnionType(CType rValueType) {
    if (rValueType instanceof CCompositeType type) {
      return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }

    return rValueType instanceof CElaboratedType type
        && type.getKind() != CComplexType.ComplexTypeKind.ENUM;
  }

  /**
   * Copies all existing Values (Value Edges) from the memory behind source to the memory behind
   * target up to the size specified in bits. If a Value starts before the sourcePointer offset, or
   * ends after the size, the value is not copied. This method will check validity of pointers,
   * writes, reads etc. itself. This includes that the 2 memory regions don't overlap and writes and
   * reads are only on valid memory. If an error is found an unchanged errorstate is returned, else
   * a state with the memory copied is returned. Mainly thought to be used by memcpy
   *
   * @param sourcePointer {@link Value} that is a pointer to some memory that is the source of the
   *     copy.
   * @param targetPointer {@link Value} that is a pointer to some memory that is the target of the
   *     copy.
   * @param sizeToCopy the size of the copy in bits.
   * @param pState the {@link SMGState} to start with.
   * @return either a {@link SMGState} with the contents copied or an error state.
   * @throws SMGException in case of abstract memory materialization errors
   */
  public List<SMGState> copyFromMemoryToMemory(
      Value sourcePointer,
      BigInteger sourceOffset,
      Value targetPointer,
      BigInteger targetOffset,
      BigInteger sizeToCopy,
      SMGState pState)
      throws SMGException {
    if (sizeToCopy.compareTo(BigInteger.ZERO) == 0) {
      return ImmutableList.of(pState);
    }
    // TODO: this could end up weird if the types sizes don't match between source and target. If
    // you read the target after the memcpy you don't necessarily get the correct values as read
    // depends on offset + size. Is this something we just accept?

    ImmutableList.Builder<SMGState> returnBuilder = ImmutableList.builder();
    // Dereference the pointers and get the source/target memory and offset
    // Start with source
    for (SMGStateAndOptionalSMGObjectAndOffset maybeSourceAndOffset :
        pState.dereferencePointer(sourcePointer)) {
      if (!maybeSourceAndOffset.hasSMGObjectAndOffset()) {
        // The value is unknown and therefore does not point to a valid memory location
        returnBuilder.add(
            maybeSourceAndOffset
                .getSMGState()
                .withUnknownPointerDereferenceWhenReading(sourcePointer));
        continue;
      }
      pState = maybeSourceAndOffset.getSMGState();
      SMGObject sourceObject = maybeSourceAndOffset.getSMGObject();

      // The object may be null if no such object exists, check and log if 0
      if (sourceObject.isZero()) {
        returnBuilder.add(pState.withNullPointerDereferenceWhenReading(sourceObject));
        continue;
      }

      // The offset of the pointer used. (the pointer might point to an offset != 0, the other
      // offset needs to the added to that!)
      Value finalSourceOffset =
          addOffsetValues(maybeSourceAndOffset.getOffsetForObject(), sourceOffset);

      // The same for the target
      for (SMGStateAndOptionalSMGObjectAndOffset maybeTargetAndOffset :
          pState.dereferencePointer(targetPointer)) {
        if (!maybeTargetAndOffset.hasSMGObjectAndOffset()) {
          // The value is unknown and therefore does not point to a valid memory location
          returnBuilder.add(
              maybeTargetAndOffset
                  .getSMGState()
                  .withUnknownPointerDereferenceWhenReading(targetPointer));
          continue;
        }

        pState = maybeTargetAndOffset.getSMGState();
        SMGObject targetObject = maybeTargetAndOffset.getSMGObject();

        // The object may be null if no such object exists, check and log if 0
        if (targetObject.isZero()) {
          returnBuilder.add(pState.withNullPointerDereferenceWhenWriting(targetObject));
          continue;
        }

        // The offset of the pointer used. (the pointer might point to an offset != 0, the other
        // offset needs to the added to that!)
        Value finalTargetoffset =
            addOffsetValues(maybeTargetAndOffset.getOffsetForObject(), targetOffset);

        // Check that the memory regions don't overlapp as this results in undefined behaviour
        if (checkForUndefinedBehavior(
            pState,
            sourceObject,
            targetObject,
            finalSourceOffset,
            finalTargetoffset,
            sizeToCopy,
            sourcePointer,
            targetPointer,
            returnBuilder)) {
          continue;
        }

        returnBuilder.add(
            pState.copySMGObjectContentToSMGObject(
                sourceObject,
                finalSourceOffset,
                targetObject,
                finalTargetoffset,
                new NumericValue(sizeToCopy)));
      }
    }
    return returnBuilder.build();
  }

  // Adds possible error states to the returnBuilder. Returns true for skipping to the next loop
  // iteration in the loop that called it, false for "nothing found, continue memcpy"
  private boolean checkForUndefinedBehavior(
      SMGState pState,
      SMGObject sourceObject,
      SMGObject targetObject,
      Value finalSourceOffset,
      Value finalTargetOffset,
      BigInteger sizeToCopy,
      Value sourcePointer,
      Value targetPointer,
      ImmutableList.Builder<SMGState> returnBuilder) {
    if (sourceObject.equals(targetObject)) {
      if (!finalTargetOffset.isNumericValue() && !finalSourceOffset.isNumericValue()) {
        // TODO: handle this symbolically
        // Note: this is nice to have, but not critical.
        return false;
      }
      BigInteger finalTargetNumOffset = finalTargetOffset.asNumericValue().bigIntegerValue();
      BigInteger finalSourceNumOffset = finalSourceOffset.asNumericValue().bigIntegerValue();
      int compareOffsets = finalTargetNumOffset.compareTo(finalSourceNumOffset);
      if (compareOffsets == 0) {
        // overlap
        returnBuilder.add(
            pState.withUndefinedbehavior(
                "Undefined behaviour because of overlapping memory regions in a copy function."
                    + " I.e. memcpy().",
                ImmutableList.of(targetPointer, sourcePointer)));
        return true;

      } else if (compareOffsets > 0) {
        // finalTargetoffset > finalSourceOffset -> if the finalTargetoffset < finalSourceOffset
        // + sizeToCopy we have an overlap
        if (finalTargetNumOffset.compareTo(finalSourceNumOffset.add(sizeToCopy)) < 0) {
          returnBuilder.add(
              pState.withUndefinedbehavior(
                  "Undefined behaviour because of overlapping memory regions in a copy"
                      + " function. I.e. memcpy().",
                  ImmutableList.of(targetPointer, sourcePointer)));
          return true;
        }
      } else {
        // finalTargetoffset < finalSourceOffset -> if the finalSourceOffset < finalTargetoffset
        // + sizeToCopy we have an overlap
        if (finalSourceNumOffset.compareTo(finalTargetNumOffset.add(sizeToCopy)) < 0) {
          returnBuilder.add(
              pState.withUndefinedbehavior(
                  "Undefined behaviour because of overlapping memory regions in a copy"
                      + " function. I.e. memcpy().",
                  ImmutableList.of(targetPointer, sourcePointer)));
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Implementation of strcmp(). Compares the characters behind the 2 memory addresses and offsets
   * until a \0 is found or the characters don't equal. If they equal in the entire memory until \0
   * is found a 0 is returned. If not the difference of them numerically is returned.
   *
   * @param firstAddress {@link Value} leading to the first memory/String.
   * @param pFirstOffsetInBits offset to begin reading the first address in bits.
   * @param secondAddress {@link Value} leading to the second memory/String.
   * @param pSecondOffsetInBits offset to begin reading the second address in bits.
   * @param pState initial {@link SMGState}.
   * @return {@link ValueAndSMGState} with a numeric value and the compare result if the values were
   *     concrete or comparable + the non error state. The Value may also be symbolic. If an error
   *     is encountered or the values were not comparable an unknown value is returned + a state
   *     that may have an error state with the error specified if there was any.
   * @throws SMGException for critical errors
   */
  public ValueAndSMGState stringCompare(
      Value firstAddress,
      BigInteger pFirstOffsetInBits,
      Value secondAddress,
      BigInteger pSecondOffsetInBits,
      SMGState pState)
      throws SMGException, SMGSolverException {
    // Dereference the pointers and get the first/second memory and offset
    // Start with first
    SMGState currentState = pState;
    List<SMGStateAndOptionalSMGObjectAndOffset> maybefirstMemorysAndOffsets =
        currentState.dereferencePointer(firstAddress);
    Preconditions.checkArgument(maybefirstMemorysAndOffsets.size() == 1);
    SMGStateAndOptionalSMGObjectAndOffset maybefirstMemoryAndOffset =
        maybefirstMemorysAndOffsets.get(0);
    currentState = maybefirstMemoryAndOffset.getSMGState();
    if (!maybefirstMemoryAndOffset.hasSMGObjectAndOffset()) {
      // The value is unknown and therefore does not point to a valid memory location
      return ValueAndSMGState.ofUnknownValue(
          currentState.withUnknownPointerDereferenceWhenReading(firstAddress));
    }
    SMGObject firstObject = maybefirstMemoryAndOffset.getSMGObject();

    // The object may be null if no such object exists, check and log if 0
    if (firstObject.isZero()) {
      return ValueAndSMGState.ofUnknownValue(
          currentState.withNullPointerDereferenceWhenReading(firstObject));
    }

    // The offset of the pointer used. (the pointer might point to an offset != 0, the other offset
    // needs to the added to that!)
    Value firstOffsetInBits =
        addOffsetValues(maybefirstMemoryAndOffset.getOffsetForObject(), pFirstOffsetInBits);

    // The same for the second address
    List<SMGStateAndOptionalSMGObjectAndOffset> maybeSecondMemorysAndOffsets =
        currentState.dereferencePointer(secondAddress);
    Preconditions.checkArgument(maybeSecondMemorysAndOffsets.size() == 1);
    SMGStateAndOptionalSMGObjectAndOffset maybeSecondMemoryAndOffset =
        maybeSecondMemorysAndOffsets.get(0);
    currentState = maybeSecondMemoryAndOffset.getSMGState();
    if (!maybeSecondMemoryAndOffset.hasSMGObjectAndOffset()) {
      // The value is unknown and therefore does not point to a valid memory location
      return ValueAndSMGState.ofUnknownValue(
          currentState.withUnknownPointerDereferenceWhenReading(secondAddress));
    }
    SMGObject secondObject = maybeSecondMemoryAndOffset.getSMGObject();

    // The object may be null if no such object exists, check and log if 0
    if (secondObject.isZero()) {
      return ValueAndSMGState.ofUnknownValue(
          currentState.withNullPointerDereferenceWhenWriting(secondObject));
    }

    // The offset of the pointer used. (the pointer might point to an offset != 0, the other offset
    // needs to the added to that!)
    Value secondOffsetInBits =
        addOffsetValues(maybeSecondMemoryAndOffset.getOffsetForObject(), pSecondOffsetInBits);

    // Check that they are not ==, if they are the returned value is trivial 0
    if (firstObject.equals(secondObject) && firstOffsetInBits.equals(secondOffsetInBits)) {
      return ValueAndSMGState.of(new NumericValue(0), currentState);
    }

    BigInteger sizeOfCharInBits = BigInteger.valueOf(machineModel.getSizeofCharInBits());
    // Now compare the Strings; stop at first \0
    boolean foundNoStringTerminationChar = true;
    while (foundNoStringTerminationChar) {
      ValueAndSMGState valueAndState1 =
          readValueWithoutMaterialization(
              currentState, firstObject, firstOffsetInBits, sizeOfCharInBits);
      Value value1 = valueAndState1.getValue();
      currentState = valueAndState1.getState();

      if (!value1.isNumericValue()) {
        return ValueAndSMGState.ofUnknownValue(currentState);
      }

      ValueAndSMGState valueAndState2 =
          readValueWithoutMaterialization(
              currentState, secondObject, secondOffsetInBits, sizeOfCharInBits);
      Value value2 = valueAndState2.getValue();
      currentState = valueAndState2.getState();

      if (!value2.isNumericValue()) {
        return ValueAndSMGState.ofUnknownValue(currentState);
      }

      // Now compare the 2 values. Non-equality of non-concrete values has to be checked by the SMG
      // method because of abstraction.
      // easy, just compare the numeric value and return if != 0
      int compare =
          value1
              .asNumericValue()
              .bigIntegerValue()
              .compareTo(value2.asNumericValue().bigIntegerValue());
      if (compare != 0) {
        return ValueAndSMGState.of(new NumericValue(compare), pState);
      }

      if ((value1.isNumericValue() && value1.asNumericValue().longValue() == 0)
          || (value2.isNumericValue() && value2.asNumericValue().longValue() == 0)) {
        foundNoStringTerminationChar = false;
      } else {
        firstOffsetInBits = addOffsetValues(firstOffsetInBits, sizeOfCharInBits);
        secondOffsetInBits = addOffsetValues(secondOffsetInBits, sizeOfCharInBits);
      }
    }
    // Only if we can 100% say they are the same we return 0
    return ValueAndSMGState.of(new NumericValue(0), pState);
  }

  public MachineModel getMachineModel() {
    return machineModel;
  }

  /**
   * Checks if the entered value is a {@link AddressExpression} or a {@link SymbolicIdentifier} with
   * a {@link MemoryLocation}.
   *
   * @param value {@link Value} to be checked.
   */
  public static boolean valueIsAddressExprOrVariableOffset(@Nullable Value value) {
    return value instanceof AddressExpression
        || (value instanceof SymbolicIdentifier identifier
            && identifier.getRepresentedLocation().isPresent());
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

  public static class SMG2SizeofVisitor extends BaseSizeofVisitor<CPATransferException> {

    private final SMGState state;
    private final LogManagerWithoutDuplicates logger;
    private final SMGCPAExpressionEvaluator evaluator;
    private final SMGOptions options;

    protected SMG2SizeofVisitor(
        MachineModel pModel,
        SMGCPAExpressionEvaluator pEvaluator,
        SMGState pState,
        LogManagerWithoutDuplicates pLogger,
        SMGOptions pOptions) {
      super(pModel);
      state = pState;
      logger = pLogger;
      evaluator = pEvaluator;
      options = pOptions;
    }

    @Override
    protected BigInteger evaluateArrayLength(CExpression arrayLength, CArrayType pArrayType)
        throws CPATransferException {
      // Try to get the length variable for arrays with variable length
      for (ValueAndSMGState lengthValueAndState :
          arrayLength.accept(
              new SMGCPAValueVisitor(
                  evaluator,
                  state,
                  new DummyCFAEdge(CFANode.newDummyCFANode(), CFANode.newDummyCFANode()),
                  logger,
                  options))) {
        Value lengthValue = lengthValueAndState.getValue();
        // We simply ignore the State for this as if it's not numeric it does not matter
        if (lengthValue.isNumericValue()) {
          return lengthValue.asNumericValue().bigIntegerValue();
        } else if (options.isGuessSizeOfUnknownMemorySize()) {
          return options.getGuessSize();
        }
      }

      throw new UnsupportedOperationException(
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
   */
  public SMGState writeValueToNewVariableBasedOnTypes(
      Value valueToWrite,
      CType leftHandSideType,
      CType rightHandSideType,
      String qualifiedVarName,
      SMGState pState,
      CFAEdge edge)
      throws CPATransferException {
    SMGState currentState = pState;
    // Parameter type is left hand side type
    CType parameterType = SMGCPAExpressionEvaluator.getCanonicalType(leftHandSideType);
    CType valueType = SMGCPAExpressionEvaluator.getCanonicalType(rightHandSideType);
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
    Value ZeroOffsetInBits = new NumericValue(BigInteger.ZERO);

    if (valueToWrite instanceof AddressExpression paramAddrExpr) {
      // This is either a pointer to be written or this points to a memory region
      // to be copied depending on the type
      Value paramAddrOffsetValue = paramAddrExpr.getOffset();

      if (SMGCPAExpressionEvaluator.isStructOrUnionType(parameterType)
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
            new NumericValue(newVariableMemory.getSize()));
      } else if (parameterType instanceof CPointerType || parameterType instanceof CSimpleType) {
        // Sometimes a pointer is cast to a long or something
        if (!paramAddrOffsetValue.isNumericValue()) {
          // Write unknown for unknown offset
          return currentState.writeToStackOrGlobalVariable(
              qualifiedVarName,
              ZeroOffsetInBits,
              paramSizeInBits,
              UnknownValue.getInstance(),
              parameterType,
              edge);
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
                parameterType,
                edge);
      } else {
        throw new SMGException(
            "Missing type handling when writing a pointer to a " + parameterType + ".");
      }

    } else if (valueToWrite instanceof SymbolicIdentifier
        && ((SymbolicIdentifier) valueToWrite).getRepresentedLocation().isPresent()) {
      return copyStructOrArrayFromValueTo(
          valueToWrite, parameterType, newVariableMemory, ZeroOffsetInBits, currentState);

    } else {
      // Just write the value
      return currentState.writeToStackOrGlobalVariable(
          qualifiedVarName, ZeroOffsetInBits, paramSizeInBits, valueToWrite, parameterType, edge);
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
      Value leftHandSideOffset,
      SMGState pState)
      throws SMGException {
    // A SymbolicIdentifier with location is used to copy entire variable structures (i.e.
    // arrays/structs etc.). We allow arrays here for function parameters.
    Preconditions.checkArgument(
        rightHandSideValue instanceof SymbolicIdentifier
            && ((SymbolicIdentifier) rightHandSideValue).getRepresentedLocation().isPresent());
    Preconditions.checkArgument(
        SMGCPAExpressionEvaluator.isStructOrUnionType(leftHandSideType)
            || leftHandSideType instanceof CArrayType);

    MemoryLocation memLocRight =
        ((SymbolicIdentifier) rightHandSideValue).getRepresentedLocation().orElseThrow();
    String paramIdentifier = memLocRight.getIdentifier();
    BigInteger paramBaseOffset = BigInteger.valueOf(memLocRight.getOffset());

    // Get the SMGObject for the memory region on the right hand side and copy the entire
    // region  into the left hand side
    Optional<SMGObject> maybeRightHandSideMemory =
        pState.getMemoryModel().getObjectForVisibleVariable(paramIdentifier);

    if (maybeRightHandSideMemory.isEmpty()) {
      // This might be called from the function call handler which just created a stack frame
      maybeRightHandSideMemory =
          pState.getMemoryModel().getObjectForVisibleVariable(paramIdentifier);
      if (maybeRightHandSideMemory.isEmpty()) {
        return pState;
      }
    }
    SMGObject paramMemory = maybeRightHandSideMemory.orElseThrow();
    // copySMGObjectContentToSMGObject checks for sizes etc.
    // I currently suspect that we know the copy sizes here concretely as this is the result of
    // struct assignments
    if (!leftHandSideOffset.isNumericValue()) {
      // If this triggers, handle the rest symbolic as well
      throw new SMGException("Symbolic offset in copy of complete memory structure.");
    }
    BigInteger concreteLeftHandSideOffset = leftHandSideOffset.asNumericValue().bigIntegerValue();
    return pState.copySMGObjectContentToSMGObject(
        paramMemory,
        new NumericValue(paramBaseOffset),
        leftHandSideMemory,
        leftHandSideOffset,
        new NumericValue(leftHandSideMemory.getSize().subtract(concreteLeftHandSideOffset)));
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
    // Don't check for existing variables or else an edge that declares a existing variable is not
    // changed!
    String varName = pVarDecl.getQualifiedName();
    CType cType = SMGCPAExpressionEvaluator.getCanonicalType(pVarDecl);

    SMGState currentState = pState;
    // Remove previously invalidated objects and create them anew
    // orElse(true) skips the call for non-local variables
    if (pState.isLocalOrGlobalVariablePresent(varName)
        && !pState.isLocalOrGlobalVariableValid(varName).orElse(true)) {
      currentState = pState.copyAndRemoveStackVariable(varName);
    }

    // There can only be one declaration result state
    return handleInitializerForDeclaration(
        handleVariableDeclarationWithoutInizializer(currentState, pVarDecl).get(0),
        varName,
        pVarDecl,
        cType,
        pEdge);
  }

  public BigInteger getAlignOf(CType pType) {
    return BigInteger.valueOf(machineModel.getAlignof(pType));
  }

  public List<SMGState> handleVariableDeclarationWithoutInizializer(
      SMGState pState, CVariableDeclaration pVarDecl) throws CPATransferException {
    String varName = pVarDecl.getQualifiedName();
    if (pState.isLocalOrGlobalVariablePresent(varName)) {
      return ImmutableList.of(pState);
    }

    CType cType = SMGCPAExpressionEvaluator.getCanonicalType(pVarDecl);
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
      newState = handleInitilizerExpression(varName, cType, newState, isExtern, pVarDecl);
    }
    return ImmutableList.of(newState);
  }

  private SMGState handleInitilizerExpression(
      String varName,
      CType cType,
      SMGState newState,
      boolean isExtern,
      CVariableDeclaration pVarDecl)
      throws CPATransferException {
    BigInteger typeSizeInBits;
    try {
      typeSizeInBits = getBitSizeof(newState, cType);
    } catch (UnsupportedOperationException e) {
      // The visitor forced my hand here!
      // Only to be caught by CEGAR based analyses!!!!
      if (options.isIgnoreUnknownMemoryAllocation()
          && e.getMessage().contains("Could not determine variable array length for length")) {
        return newState;
      }
      throw e;
    }
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
        if (initExpr instanceof CStringLiteralExpression stringLit) {
          typeSizeInBits = BigInteger.valueOf(8).multiply(BigInteger.valueOf(stringLit.getSize()));
        } else {
          throw new SMGException(
              "Could not determine correct type size for an array for initializer expression: "
                  + init);
        }
      } else if (init instanceof CInitializerList initList) {
        CType realCType = cType.getCanonicalType();

        CArrayType arrayType = (CArrayType) realCType;
        CType memberType = SMGCPAExpressionEvaluator.getCanonicalType(arrayType.getType());
        BigInteger memberTypeSize = getBitSizeof(newState, memberType);
        BigInteger numberOfMembers = BigInteger.valueOf(initList.getInitializers().size());
        typeSizeInBits = BigInteger.valueOf(8).multiply(memberTypeSize).multiply(numberOfMembers);

      } else {
        throw new SMGException(
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
      return newState.copyAndAddGlobalVariable(typeSizeInBits, varName, cType);
    } else {
      return newState.copyAndAddLocalVariable(typeSizeInBits, varName, cType);
    }
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
        currentState = currentState.writeToStackOrGlobalVariableToZero(pVarName, cType, pEdge);
      }
    }

    if (newInitializer != null) {
      return handleInitializer(
          currentState,
          pVarDecl,
          pEdge,
          pVarName,
          new NumericValue(BigInteger.ZERO),
          cType,
          newInitializer);
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
      Value pOffset,
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
    } else if (pInitializer instanceof CInitializerList pNewInitializer) {
      CType realCType = pLValueType.getCanonicalType();

      if (realCType instanceof CArrayType arrayType) {
        return handleInitializerList(
            pNewState, pVarDecl, pEdge, variableName, pOffset, arrayType, pNewInitializer);
      } else if (realCType instanceof CCompositeType structType) {
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
      Value pOffset,
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
      Value pOffset,
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
      Value offset =
          addOffsetValues(
              pOffset, new NumericValue(offsetAndPosition.get(memberTypes.get(listCounter))));

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
      Value pOffset,
      CArrayType pLValueType,
      CInitializerList pNewInitializer)
      throws CPATransferException {

    CType memberType = SMGCPAExpressionEvaluator.getCanonicalType(pLValueType.getType());
    BigInteger memberTypeSize = getBitSizeof(pState, memberType);

    // ImmutableList.Builder<SMGState> finalStates = ImmutableList.builder();
    SMGState currentState = pState;
    Value offset = pOffset;
    for (CInitializer initializer : pNewInitializer.getInitializers()) {
      // TODO: this has to be checked with a test!!!!
      if (initializer instanceof CDesignatedInitializer) {
        initializer = ((CDesignatedInitializer) initializer).getRightHandSide();
      }

      List<SMGState> newStates =
          handleInitializer(
              currentState, pVarDecl, pEdge, variableName, offset, memberType, initializer);

      offset = addOffsetValues(offset, memberTypeSize);

      // If this ever fails we have to split the rest of the initializer such that all states are
      // treated the same from this point onwards
      Preconditions.checkArgument(newStates.size() == 1);
      currentState = newStates.get(0);
    }

    return ImmutableList.of(currentState);
  }

  /**
   * Returns the name of the global variable for an entered String literal. We expect all String
   * literals to be global variables after the first usage, so that they can always be found by this
   * variable name.
   *
   * @param pCStringLiteralExpression a {@link CStringLiteralExpression}
   * @return a {@link String} that is the (global) variable name.
   */
  public String getCStringLiteralExpressionVairableName(
      CStringLiteralExpression pCStringLiteralExpression) {
    return "_" + pCStringLiteralExpression.getContentWithoutNullTerminator() + "_STRING_LITERAL";
    /*
    WHY did i do this?!
          // If the var exists we change the name and create a new one
      // (Don't reuse an old variable! They might be different from the new one!)
      int num = 0;
      while (pState.isGlobalVariablePresent(stringVarName + num)) {
        num++;
      }
      stringVarName += num;
     */
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
  public List<SMGState> handleStringInitializer(
      SMGState pState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      Value pOffset,
      CType pCurrentExpressionType,
      FileLocation pFileLocation,
      CStringLiteralExpression pExpression)
      throws CPATransferException {

    // If this is a pointer (i.e. char * name = "iAmAString";) we actually have not yet initialized
    // the memory for the String, just the pointer. So we need to create new memory for the String,
    // write the String into it, make a pointer to the beginning and save that in the char *.
    if (pCurrentExpressionType instanceof CPointerType) {
      // create a new memory region for the string (right hand side)
      CArrayType stringArrayType = pExpression.getExpressionType();
      String stringVarName = getCStringLiteralExpressionVairableName(pExpression);

      BigInteger sizeOfString = getBitSizeof(pState, stringArrayType);
      SMGState currentState =
          pState.copyAndAddGlobalVariable(sizeOfString, stringVarName, stringArrayType);
      List<SMGState> initedStates =
          transformStringToArrayAndInitialize(
              currentState,
              pVarDecl,
              pEdge,
              stringVarName,
              new NumericValue(BigInteger.ZERO),
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
                pCurrentExpressionType,
                pEdge));
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
      Value pOffset,
      FileLocation pFileLocation,
      CStringLiteralExpression pExpression)
      throws CPATransferException {
    // Create a char array from string and call list init
    ImmutableList.Builder<CInitializer> charArrayInitialziersBuilder = ImmutableList.builder();
    CArrayType arrayType = pExpression.getExpressionType();
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
      Value pOffsetInBits,
      CType pWriteType,
      CExpression exprToWrite)
      throws CPATransferException {
    Preconditions.checkArgument(!(exprToWrite instanceof CStringLiteralExpression));
    CType typeOfValueToWrite = SMGCPAExpressionEvaluator.getCanonicalType(exprToWrite);
    CType typeOfWrite = SMGCPAExpressionEvaluator.getCanonicalType(pWriteType);
    BigInteger sizeOfTypeLeft = getBitSizeof(pState, typeOfWrite);
    ImmutableList.Builder<SMGState> resultStatesBuilder = ImmutableList.builder();
    SMGState currentState = pState;

    if (SMGCPAExpressionEvaluator.isStructOrUnionType(typeOfWrite)) {
      // Copy of the entire structure instead of just writing
      // Source == right hand side
      for (SMGStateAndOptionalSMGObjectAndOffset sourceObjectAndOffsetOrState :
          exprToWrite.accept(new SMGCPAAddressVisitor(this, pState, cfaEdge, logger, options))) {
        if (!sourceObjectAndOffsetOrState.hasSMGObjectAndOffset()) {
          resultStatesBuilder.add(sourceObjectAndOffsetOrState.getSMGState());
          continue;
        }
        currentState = sourceObjectAndOffsetOrState.getSMGState();
        Preconditions.checkArgument(
            pOffsetInBits.asNumericValue().bigIntegerValue().intValueExact() == 0);

        Optional<SMGObjectAndOffset> maybeLeftHandSideVariableObject =
            getTargetObjectAndOffset(currentState, variableName);
        if (maybeLeftHandSideVariableObject.isEmpty()) {
          throw new SMGException("Usage of undeclared variable: " + variableName + ".");
        }
        SMGObject addressToWriteTo = maybeLeftHandSideVariableObject.orElseThrow().getSMGObject();
        Value offsetToWriteTo = maybeLeftHandSideVariableObject.orElseThrow().getOffsetForObject();

        // The .asNumericValue().bigIntegerValue() might fail at some point, then we need to handle
        // sizes symbolically as well
        resultStatesBuilder.add(
            currentState.copySMGObjectContentToSMGObject(
                sourceObjectAndOffsetOrState.getSMGObject(),
                sourceObjectAndOffsetOrState.getOffsetForObject(),
                addressToWriteTo,
                offsetToWriteTo,
                subtractOffsetValues(addressToWriteTo.getSize(), offsetToWriteTo)));
      }

    } else if (typeOfWrite instanceof CPointerType && typeOfValueToWrite instanceof CArrayType) {
      // Implicit & on the array expr
      for (ValueAndSMGState addressAndState : createAddress(exprToWrite, currentState, cfaEdge)) {
        Value addressToAssign = addressAndState.getValue();
        currentState = addressAndState.getState();
        resultStatesBuilder.add(
            currentState.writeToStackOrGlobalVariable(
                variableName,
                pOffsetInBits,
                sizeOfTypeLeft,
                addressToAssign,
                typeOfWrite,
                cfaEdge));
      }

    } else {
      // Just a normal write
      SMGCPAValueVisitor vv = new SMGCPAValueVisitor(this, pState, cfaEdge, logger, options);
      for (ValueAndSMGState valueAndState : vv.evaluate(exprToWrite, typeOfWrite)) {

        ValueAndSMGState valueAndStateToAssign =
            unpackAddressExpression(valueAndState.getValue(), valueAndState.getState());
        Value valueToAssign = valueAndStateToAssign.getValue();
        currentState = valueAndStateToAssign.getState();

        if (valueToAssign instanceof SymbolicIdentifier) {
          Preconditions.checkArgument(
              ((SymbolicIdentifier) valueToAssign).getRepresentedLocation().isEmpty());
        }

        resultStatesBuilder.add(
            currentState.writeToStackOrGlobalVariable(
                variableName, pOffsetInBits, sizeOfTypeLeft, valueToAssign, typeOfWrite, cfaEdge));
      }
    }
    return resultStatesBuilder.build();
  }

  /**
   * Adds the two given offsets as good as possible. Might return a concrete {@link NumericValue} or
   * a {@link org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue}.
   *
   * @param leftValue {@link Value} that might be a {@link NumericValue}
   * @param rightValue {@link BigInteger} concrete value.
   * @return either a concrete {@link NumericValue} or any other form of addition of the two {@link
   *     Value}s.
   * @throws SMGException in case of unknowns.
   */
  public static Value addOffsetValues(Value leftValue, BigInteger rightValue) throws SMGException {
    if (rightValue.equals(BigInteger.ZERO)) {
      return leftValue;
    }
    return addOffsetValues(leftValue, new NumericValue(rightValue));
  }

  /**
   * Adds the two given offsets as good as possible. Might return a concrete {@link NumericValue} or
   * a {@link org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue}.
   *
   * @param leftValue {@link Value} that might be a {@link NumericValue}
   * @param rightValue {@link Value} that might be a {@link NumericValue}
   * @return either a concrete {@link NumericValue} or any other form of addition of the two {@link
   *     Value}s.
   * @throws SMGException in case of unknowns.
   */
  public static Value addOffsetValues(Value leftValue, Value rightValue) throws SMGException {
    if (leftValue.isNumericValue() && rightValue.isNumericValue()) {
      BigInteger concreteOffset =
          leftValue
              .asNumericValue()
              .bigIntegerValue()
              .add(rightValue.asNumericValue().bigIntegerValue());
      return new NumericValue(concreteOffset);
    } else if (leftValue.isNumericValue()
        && leftValue.asNumericValue().bigIntegerValue().equals(BigInteger.ZERO)) {
      return rightValue;
    } else if (rightValue.isNumericValue()
        && rightValue.asNumericValue().bigIntegerValue().equals(BigInteger.ZERO)) {
      return leftValue;
    } else if (!leftValue.isUnknown() && !rightValue.isUnknown()) {
      // Not numeric and not unknown -> symbolic
      final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

      SymbolicExpression leftOperand = factory.asConstant(leftValue, CNumericTypes.INT);
      SymbolicExpression rightOperand = factory.asConstant(rightValue, CNumericTypes.INT);

      return factory.add(leftOperand, rightOperand, CNumericTypes.INT, CNumericTypes.INT);
    } else {
      // At some point this triggers with unknowns. And i want to know from where ;D
      throw new SMGException("Error assuming the offset of a memory access operation.");
    }
  }

  /**
   * Subtracts the two given offsets as good as possible with left - right. Might return a concrete
   * {@link NumericValue} or a {@link
   * org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue}.
   *
   * @param leftValue {@link BigInteger}
   * @param rightValue {@link Value} that might be a {@link NumericValue}
   * @return either a concrete {@link NumericValue} or any other form of subtraction of the two
   *     {@link Value}s.
   * @throws SMGException in case of unknowns.
   */
  public static Value subtractOffsetValues(BigInteger leftValue, Value rightValue)
      throws SMGException {
    return subtractOffsetValues(new NumericValue(leftValue), rightValue);
  }

  /**
   * Subtracts the two given offsets as good as possible with left - right. Might return a concrete
   * {@link NumericValue} or a {@link
   * org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue}.
   *
   * @param leftValue {@link Value} that might be a {@link NumericValue}
   * @param rightValue {@link Value} that might be a {@link NumericValue}
   * @return either a concrete {@link NumericValue} or any other form of subtraction of the two
   *     {@link Value}s.
   * @throws SMGException in case of unknowns.
   */
  public static Value subtractOffsetValues(Value leftValue, Value rightValue) throws SMGException {
    if (leftValue.isNumericValue() && rightValue.isNumericValue()) {
      BigInteger concreteOffset =
          leftValue
              .asNumericValue()
              .bigIntegerValue()
              .subtract(rightValue.asNumericValue().bigIntegerValue());
      return new NumericValue(concreteOffset);
    } else if (rightValue.isNumericValue()
        && rightValue.asNumericValue().bigIntegerValue().equals(BigInteger.ZERO)) {
      return leftValue;
    } else if (!leftValue.isUnknown() && !rightValue.isUnknown()) {
      // Not numeric and not unknown -> symbolic
      final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

      SymbolicExpression leftOperand = factory.asConstant(leftValue, CNumericTypes.INT);
      SymbolicExpression rightOperand = factory.asConstant(rightValue, CNumericTypes.INT);

      return factory.minus(leftOperand, rightOperand, CNumericTypes.INT, CNumericTypes.INT);
    } else {
      // At some point this triggers with unknowns. And i want to know from where ;D
      throw new SMGException("Error assuming the offset of a memory access operation.");
    }
  }

  public static Value multiplyOffsetValues(Value leftValue, BigInteger rightValue)
      throws SMGException {
    if (rightValue.equals(BigInteger.ONE)) {
      return leftValue;
    } else if (rightValue.equals(BigInteger.ZERO)) {
      return new NumericValue(rightValue);
    }

    return multiplyOffsetValues(leftValue, new NumericValue(rightValue));
  }

  public static Value multiplyOffsetValues(Value leftValue, Value rightValue) throws SMGException {
    if (leftValue.isNumericValue() && rightValue.isNumericValue()) {
      BigInteger concreteOffset =
          leftValue
              .asNumericValue()
              .bigIntegerValue()
              .multiply(rightValue.asNumericValue().bigIntegerValue());
      return new NumericValue(concreteOffset);
    } else if (rightValue.isNumericValue()
        && rightValue.asNumericValue().bigIntegerValue().equals(BigInteger.ONE)) {
      return leftValue;
    } else if (leftValue.isNumericValue()
        && leftValue.asNumericValue().bigIntegerValue().equals(BigInteger.ONE)) {
      return rightValue;
    } else if (!leftValue.isUnknown() && !rightValue.isUnknown()) {
      // Not numeric and not unknown -> symbolic
      final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

      SymbolicExpression leftOperand = factory.asConstant(leftValue, CNumericTypes.INT);
      SymbolicExpression rightOperand = factory.asConstant(rightValue, CNumericTypes.INT);

      return factory.multiply(leftOperand, rightOperand, CNumericTypes.INT, CNumericTypes.INT);
    } else {
      // At some point this triggers with unknowns. And i want to know from where ;D
      throw new SMGException("Error assuming the offset of a memory access operation.");
    }
  }

  public SMGConstraintsSolver getSolver() {
    return solver;
  }
}
