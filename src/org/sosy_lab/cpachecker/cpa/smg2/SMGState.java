// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg2.SMGErrorInfo.Property;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSPC;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.join.SMGJoinSPC;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Class holding the SPC (SymbolicProgramConfiguration = memory model) for heap, global
 * variables/constants and the stack. Also provides methods to manipulate the SMG; meaning
 * adding/pruning/reading and memory error/leak handling. This class is meant to represent the
 * CPAState, while the memory state is represented by the SPC. This class therefore hands down
 * write/read and other memory operations. It is expected that in the SPC no CPA specific stuff is
 * handled.
 */
public class SMGState implements LatticeAbstractState<SMGState>, AbstractQueryableState, Graphable {

  // Properties:
  @SuppressWarnings("unused")
  private static final String HAS_INVALID_FREES = "has-invalid-frees";

  @SuppressWarnings("unused")
  private static final String HAS_INVALID_READS = "has-invalid-reads";

  @SuppressWarnings("unused")
  private static final String HAS_INVALID_WRITES = "has-invalid-writes";

  @SuppressWarnings("unused")
  private static final String HAS_LEAKS = "has-leaks";

  @SuppressWarnings("unused")
  private static final String HAS_HEAP_OBJECTS = "has-heap-objects";

  @SuppressWarnings("unused")
  private static final Pattern externalAllocationRecursivePattern =
      Pattern.compile("^(r_)(\\d+)(_.*)$");

  // All memory models (SMGs) (heap/global/stack)
  private final SymbolicProgramConfiguration memoryModel;

  private final MachineModel machineModel;
  private final LogManager logger;
  private SMGErrorInfo errorInfo;
  private final SMGOptions options;

  private SMGState(
      MachineModel pMachineModel,
      SymbolicProgramConfiguration spc,
      LogManager logManager,
      SMGOptions opts) {
    memoryModel = spc;
    machineModel = pMachineModel;
    logger = logManager;
    options = opts;
    errorInfo = SMGErrorInfo.of();
  }

  private SMGState(
      MachineModel pMachineModel,
      SymbolicProgramConfiguration spc,
      LogManager logManager,
      SMGOptions opts,
      SMGErrorInfo errorInf) {
    memoryModel = spc;
    machineModel = pMachineModel;
    logger = logManager;
    options = opts;
    errorInfo = errorInf;
  }

  /**
   * Creates a new, empty {@link SMGState} with the {@link SMGOptions} given. The {@link
   * SymbolicProgramConfiguration} and {@link SMGErrorInfo} inside are new and empty as well.
   *
   * @param pMachineModel the {@link MachineModel} used to determin the size of types.
   * @param logManager {@link LogManager} to log important information.
   * @param opts {@link SMGOptions} to be used.
   * @return a newly created {@link SMGState} with a new and emtpy {@link
   *     SymbolicProgramConfiguration} inside.
   */
  public static SMGState of(MachineModel pMachineModel, LogManager logManager, SMGOptions opts) {
    return new SMGState(
        pMachineModel,
        SymbolicProgramConfiguration.of(BigInteger.valueOf(pMachineModel.getSizeofPtrInBits())),
        logManager,
        opts);
  }

  /**
   * Creates a new {@link SMGState} out of the parameters, but also creates a new, empty {@link
   * SMGErrorInfo} for the new state.
   *
   * @param pMachineModel {@link MachineModel} to be used to determine type sizes.
   * @param heapSPC {@link SymbolicProgramConfiguration} to be used.
   * @param logManager {@link LogManager} to be used to log information.
   * @param opts {@link SMGOptions} to be used.
   * @return new {@link SMGState} out of the parameters with empty {@link SMGErrorInfo}.
   */
  public static SMGState of(
      MachineModel pMachineModel,
      SymbolicProgramConfiguration heapSPC,
      LogManager logManager,
      SMGOptions opts) {
    return new SMGState(pMachineModel, heapSPC, logManager, opts);
  }

  /**
   * Creates a new {@link SMGState} out of the parameters given. No new elements are created by
   * this.
   *
   * @param pMachineModel the {@link MachineModel} used to determin the size of types.
   * @param pSPC the {@link SymbolicProgramConfiguration} to be used in the new state.
   * @param logManager the {@link LogManager} to be used in the new state.
   * @param opts {@link SMGOptions} to be used.
   * @param pErrorInfo the {@link SMGErrorInfo} holding error information.
   * @return a new {@link SMGState} with the arguments given.
   */
  public static SMGState of(
      MachineModel pMachineModel,
      SymbolicProgramConfiguration pSPC,
      LogManager logManager,
      SMGOptions opts,
      SMGErrorInfo pErrorInfo) {
    return new SMGState(pMachineModel, pSPC, logManager, opts, pErrorInfo);
  }

  public SMGState withViolationsOf(SMGState pOther) {
    if (errorInfo.equals(pOther.errorInfo)) {
      return this;
    }
    SMGState result = new SMGState(machineModel, memoryModel, logger, options);
    result.errorInfo = result.errorInfo.mergeWith(pOther.errorInfo);
    return result;
  }

  /**
   * Copy SMGState with a newly created object and put it into the global namespace. This replaces
   * an existing old global variable!
   *
   * @param pTypeSizeInBits Size of the type of the new global variable.
   * @param pVarName Name of the global variable.
   * @return Newly created {@link SMGState} with the object added for the name specified.
   */
  public SMGState copyAndAddGlobalVariable(int pTypeSizeInBits, String pVarName) {
    // TODO: do we really need this for ints?
    return copyAndAddGlobalVariable(BigInteger.valueOf(pTypeSizeInBits), pVarName);
  }

  /**
   * Copy SMGState with a newly created object and put it into the global namespace. This replaces
   * an existing old global variable!
   *
   * @param pTypeSizeInBits Size of the type of the new global variable.
   * @param pVarName Name of the global variable.
   * @return Newly created {@link SMGState} with the object added for the name specified.
   */
  public SMGState copyAndAddGlobalVariable(BigInteger pTypeSizeInBits, String pVarName) {
    SMGObject newObject = SMGObject.of(0, pTypeSizeInBits, BigInteger.ZERO);
    return of(
        machineModel, memoryModel.copyAndAddGlobalObject(newObject, pVarName), logger, options);
  }

  /**
   * Copy SMGState with a newly created {@link SMGObject} and returns the new state + the new {@link
   * SMGObject} with the size specified in bits. Make sure that you reuse the {@link SMGObject}
   * right away to create a points-to-edge and not just use SMGObjects in the code.
   *
   * @param pTypeSizeInBits Size of the type of the new global variable.
   * @return Newly created object + state with it.
   */
  public SMGObjectAndSMGState copyAndAddHeapObject(BigInteger pTypeSizeInBits) {
    SMGObject newObject = SMGObject.of(0, pTypeSizeInBits, BigInteger.ZERO);
    return SMGObjectAndSMGState.of(
        newObject, of(machineModel, memoryModel.copyAndAddHeapObject(newObject), logger, options));
  }

  /**
   * Checks if a global variable exists for the name given.
   *
   * @param pVarName Name of the global variable.
   * @return true if the var exists, false else.
   */
  public boolean isGlobalVariablePresent(String pVarName) {
    return memoryModel.getGlobalVariableToSmgObjectMap().containsKey(pVarName);
  }

  /**
   * Copy SMGState with a newly created object with the size given and put it into the current stack
   * frame. If there is no stack frame this throws an exception!
   *
   * <p>Keeps consistency: yes
   *
   * @param pTypeSize Size of the type the new local variable in bits.
   * @param pVarName Name of the local variable
   * @return {@link SMGState} with the new variables searchable by the name given.
   * @throws SMG2Exception thrown if the stack frame is empty.
   */
  public SMGState copyAndAddLocalVariable(int pTypeSize, String pVarName) throws SMG2Exception {
    if (memoryModel.getStackFrames().isEmpty()) {
      throw new SMG2Exception(
          "Can't add a variable named "
              + pVarName
              + " to the memory model because there is no stack frame.");
    }
    SMGObject newObject = SMGObject.of(0, BigInteger.valueOf(pTypeSize), BigInteger.ZERO);
    return of(
        machineModel, memoryModel.copyAndAddStackObject(newObject, pVarName), logger, options);
  }

  /**
   * Copy SMGState with a newly created anonymous object and put it into the current stack frame.
   * Used for string initilizers as function arguments.
   *
   * <p>Keeps consistency: yes
   *
   * @param pTypeSize Size of the type the new local variable
   * @return Newly created object
   * @throws SMG2Exception thrown if there is no stack frame to add the var to.
   */
  public SMGState copyAndAddAnonymousVariable(int pTypeSize) throws SMG2Exception {
    return copyAndAddLocalVariable(pTypeSize, makeAnonymousVariableName());
  }

  /**
   * Returns true if there exists a variable on the stack with the name entered.
   *
   * @param pState state to check the memory model for.
   * @param variableName name of the variable.
   * @return true if the variable exists, false otherwise.
   */
  public boolean checkVariableExists(SMGState pState, String variableName) {
    return pState.getMemoryModel().getObjectForVisibleVariable(variableName).isPresent();
  }

  /**
   * Copy SMGState and adds a new frame for the function.
   *
   * <p>Keeps consistency: yes
   *
   * @param pFunctionDefinition A function for which to create a new stack frame
   */
  public SMGState copyAndAddStackFrame(CFunctionDeclaration pFunctionDefinition) {
    return of(
        machineModel,
        memoryModel.copyAndAddStackFrame(pFunctionDefinition, machineModel),
        logger,
        options);
  }

  @Override
  public String toDOTLabel() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean shouldBeHighlighted() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String getCPAName() {
    return "SMG2CPA";
  }

  /*
   * Join 2 SMGStates and as a consequence its SMGs as far as possible.
   */
  @Override
  public SMGState join(SMGState pOther) throws CPAException, InterruptedException {
    SMGJoinSPC joinSPC = new SMGJoinSPC(memoryModel, pOther.memoryModel);
    if (!(joinSPC.getStatus() == SMGJoinStatus.INCOMPARABLE && joinSPC.isDefined())) {
      return pOther;
    }
    return new SMGState(machineModel, joinSPC.getResult(), logger, options);
  }

  @Override
  public boolean isLessOrEqual(SMGState pOther) throws CPAException, InterruptedException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean hasMemoryErrors() {
    return errorInfo.hasMemoryErrors();
  }

  public boolean hasMemoryLeaks() {
    return errorInfo.hasMemoryLeak();
  }

  /*
   * Check non-equality of the 2 entered SMGValues. Never use == or equals on them!
   */
  private boolean areNonEqual(SMGValue pValue1, SMGValue pValue2) {
    return memoryModel.proveInequality(pValue1, pValue2);
  }

  public boolean areNonEqual(Value pValue1, Value pValue2) {
    Optional<SMGValue> smgValue1 = memoryModel.getSMGValueFromValue(pValue1);
    Optional<SMGValue> smgValue2 = memoryModel.getSMGValueFromValue(pValue2);
    if (smgValue1.isEmpty() || smgValue2.isEmpty()) {
      return false;
    }
    return areNonEqual(smgValue1.orElseThrow(), smgValue2.orElseThrow());
  }

  /** Logs the error entered using the states logger. */
  private void logMemoryError(String pMessage, boolean pUndefinedBehavior) {
    if (options.isMemoryErrorTarget()) {
      logger.log(Level.FINE, pMessage);
    } else if (pUndefinedBehavior) {
      logger.log(Level.FINE, pMessage);
      logger.log(
          Level.FINE,
          "Non-target undefined behavior detected. The verification result is unreliable.");
    }
  }

  private static int anonymousVarCount = 0;

  private static String makeAnonymousVariableName() {
    return "anonymous_var_" + anonymousVarCount++;
  }

  public SMGState copyAndPruneOutOfScopeVariables(Set<CSimpleDeclaration> pOutOfScopeVars) {
    SMGState retState = this;
    for (CSimpleDeclaration variable : pOutOfScopeVars) {
      retState = retState.copyAndPruneVariable(MemoryLocation.forDeclaration(variable));
    }

    return retState;
  }

  private SMGState copyAndReplaceMemoryModel(SymbolicProgramConfiguration newSPC) {
    return of(machineModel, newSPC, logger, options, errorInfo);
  }

  private SMGState copyAndPruneVariable(MemoryLocation pMemoryLocation) {
    if (pMemoryLocation.isOnFunctionStack()) {
      return copyAndPruneFunctionStackVariable(pMemoryLocation);
    } else {
      return copyAndPruneGlobalVariable(pMemoryLocation);
    }
  }

  private SMGState copyAndPruneGlobalVariable(MemoryLocation pMemoryLocation) {
    return of(
        machineModel,
        memoryModel.copyAndRemoveGlobalVariable(pMemoryLocation.getIdentifier()),
        logger,
        options);
  }

  private SMGState copyAndPruneFunctionStackVariable(MemoryLocation pMemoryLocation) {
    return of(
        machineModel,
        memoryModel.copyAndRemoveStackVariable(pMemoryLocation.getIdentifier()),
        logger,
        options);
  }

  public SMGState dropStackFrame() {
    return of(machineModel, memoryModel.copyAndDropStackFrame(), logger, options);
  }

  /*
   * Copy the current state and prune all unreachable SMGObjects. Used for example after a function return with the stack. TODO: this might get complicated with merge later.
   */
  public SMGState copyAndPruneUnreachable() {
    Collection<SMGObject> unreachableObjects = new HashSet<>();
    SymbolicProgramConfiguration newHeap = memoryModel.copyAndPruneUnreachable(unreachableObjects);
    if (unreachableObjects.isEmpty()) {
      return this;
    }
    return copyWithMemLeak(newHeap, unreachableObjects);
  }

  /*
   * Copy the state with an error attached. This method is used for memory leaks, meaning its a non fatal error.
   */
  private SMGState copyWithMemLeak(
      SymbolicProgramConfiguration newHeap, Collection<SMGObject> leakedObjects) {
    String leakedObjectsLabels =
        leakedObjects.stream().map(Object::toString).collect(Collectors.joining(","));
    String errorMSG = "Memory leak of " + leakedObjectsLabels + " is detected.";
    SMGErrorInfo newErrorInfo =
        errorInfo
            .withProperty(Property.INVALID_HEAP)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(leakedObjects);
    logMemoryError(errorMSG, true);
    return copyWithErrorInfo(newHeap, newErrorInfo);
  }

  // TODO: invalid read/write because of invalidated object/memory

  /**
   * Use of an variable that was not initialized. The value will be unknown, but generally
   * undefined.
   *
   * @param uninitializedVariableName the {@link String} that is not initialized.
   * @return A new {@link SMGState} with the error info.
   */
  public SMGState withUninitializedVariableUsage(String uninitializedVariableName) {
    String errorMSG =
        "Usage of uninitialized variable: "
            + uninitializedVariableName
            + ". A unknown value was assumed, but behaviour is of this variable is generally"
            + " undefined.";
    SMGErrorInfo newErrorInfo =
        errorInfo
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(uninitializedVariableName));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithErrorInfo(memoryModel, newErrorInfo);
  }

  /**
   * Error for using a not declared variable.
   *
   * @param undeclaredExpr the {@link CIdExpression} that is undeclared.
   * @return A new {@link SMGState} with the error info.
   */
  public SMGState withUndeclaredVariableUsage(CIdExpression undeclaredExpr) {
    String errorMSG = "Usage of undeclared variable: " + undeclaredExpr.getName() + ".";
    SMGErrorInfo newErrorInfo =
        errorInfo
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(undeclaredExpr));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithErrorInfo(memoryModel, newErrorInfo);
  }

  /**
   * Error for dereferencing unknown pointer {@link Value} when reading. I.e. int bla = *value; with
   * value being unknown.
   *
   * @param unknownAddress the {@link Value} that is unknown to the memory model and was tried to be
   *     dereferenced.
   * @return A new {@link SMGState} with the error info.
   */
  public SMGState withUnknownPointerDereferenceWhenReading(Value unknownAddress) {
    String errorMSG = "Unknown value pointer dereference for value: " + unknownAddress + ".";
    SMGErrorInfo newErrorInfo =
        errorInfo
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(unknownAddress));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithErrorInfo(memoryModel, newErrorInfo);
  }

  /**
   * Error for dereferencing unknown pointer {@link Value} when reading with intent to write. I.e.
   * *value = 3; with value being unknown.
   *
   * @param unknownAddress the {@link Value} that is unknown to the memory model and was tried to be
   *     dereferenced.
   * @return A new {@link SMGState} with the error info.
   */
  public SMGState withUnknownPointerDereferenceWhenWriting(Value unknownAddress) {
    String errorMSG =
        "Unknown value pointer dereference with intent to write to it for value: "
            + unknownAddress
            + ".";
    SMGErrorInfo newErrorInfo =
        errorInfo
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(unknownAddress));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithErrorInfo(memoryModel, newErrorInfo);
  }

  /**
   * Error for trying to write to a local/global variable that is not declared.
   *
   * @param variableName the variable name of the variable that was tried to write to.
   * @return A new {@link SMGState} with the error info.
   */
  public SMGState withWriteToUnknownVariable(String variableName) {
    String errorMSG = "Failed write to variable " + variableName + ".";
    SMGErrorInfo newErrorInfo =
        errorInfo
            .withProperty(Property.INVALID_WRITE)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(variableName));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithErrorInfo(memoryModel, newErrorInfo);
  }

  /**
   * The error sais invalid read as the point that fails is the read of the {@link SMGObject} before
   * writing! I.e. *pointer = ...; With pointer failing to dereference because its pointing to 0.
   *
   * @param nullObject the {@link SMGObject} that is null and was tried to be dereferenced.
   * @return A new SMGState with the error info.
   */
  public SMGState withNullPointerDereferenceWhenWriting(SMGObject nullObject) {
    // Get the SMGValue and Value that lead to this null pointer dereference
    String errorMSG =
        "Null pointer dereference on read of object with the intent to write to: "
            + nullObject
            + ".";
    SMGErrorInfo newErrorInfo =
        errorInfo
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(nullObject));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithErrorInfo(memoryModel, newErrorInfo);
  }

  /**
   * Invalid write to a not initialized, unknown or non-existing memory region.
   *
   * @param invalidAddress the invalid address pointing to nothing.
   * @return A new SMGState with the error info.
   */
  public SMGState withInvalidWrite(Value invalidAddress) {
    String errorMSG =
        "Write to invalid, unknown or non-existing memory region, pointed to by: "
            + invalidAddress
            + ".";
    SMGErrorInfo newErrorInfo =
        errorInfo
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(invalidAddress));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithErrorInfo(memoryModel, newErrorInfo);
  }

  /**
   * Invalid write with custom error msg.
   *
   * @param invalidValue the invalid value. Either address or write value or something like a size
   *     specifier etc.
   * @return A new SMGState with the error info.
   */
  public SMGState withInvalidWrite(String errorMSG, Value invalidValue) {
    SMGErrorInfo newErrorInfo =
        errorInfo
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(invalidValue));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithErrorInfo(memoryModel, newErrorInfo);
  }

  /**
   * I.e. int bla = *pointer; With pointer failing to dereference because its pointing to 0.
   *
   * @param nullObject the {@link SMGObject} that is null and was tried to be dereferenced.
   * @return A new SMGState with the error info.
   */
  public SMGState withNullPointerDereferenceWhenReading(SMGObject nullObject) {
    // getValueForSMGValue(pValue)
    // Get the SMGValue and Value that lead to this null pointer dereference
    String errorMSG =
        "Null pointer dereference on read of object with the intent to read it: "
            + nullObject
            + ".";
    SMGErrorInfo newErrorInfo =
        errorInfo
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(nullObject));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithErrorInfo(memoryModel, newErrorInfo);
  }

  /**
   * I.e. int bla = blub; With blub not existing or having no memory.
   *
   * @param readVariable the variable that was tried to be read.
   * @return A new SMGState with the error info.
   */
  public SMGState withInvalidStackVariableRead(String readVariable) {
    String errorMSG = "Invalid read of variable named: " + readVariable + ".";
    SMGErrorInfo newErrorInfo =
        errorInfo
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(readVariable));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithErrorInfo(memoryModel, newErrorInfo);
  }

  /**
   * Copy and update this {@link SMGState} with an error resulting from trying to write outside of
   * the range of the {@link SMGObject}. Returns an updated state with the error in it.
   *
   * @param objectWrittenTo the {@link SMGObject} that should have been written to.
   * @param writeOffset The offset in bits where you want to write the {@link Value} to.
   * @param writeSize the size of the {@link Value} in bits.
   * @param pValue the {@link Value} you wanted to write.
   * @return A new SMGState with the error info.
   */
  public SMGState withOutOfRangeWrite(
      SMGObject objectWrittenTo, BigInteger writeOffset, BigInteger writeSize, Value pValue) {
    String errorMSG =
        String.format(
            "Try writing value %s with size %d at offset %d bit to object sized %d bit.",
            pValue.toString(), writeSize, writeOffset, objectWrittenTo.getSize());
    SMGErrorInfo newErrorInfo =
        errorInfo
            .withProperty(Property.INVALID_WRITE)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(objectWrittenTo));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithErrorInfo(memoryModel, newErrorInfo);
  }

  /**
   * Copy and update this {@link SMGState} with an error resulting from trying to read outside of
   * the range of the {@link SMGObject}. Returns an updated state with the error in it.
   *
   * @param objectRead the {@link SMGObject} that should have been read.
   * @param readOffset The offset in bits as {@link BigInteger} where you want to read.
   * @param readSize the size of the type in bits to read as {@link BigInteger}.
   * @return A new SMGState with the error info.
   */
  public SMGState withOutOfRangeRead(
      SMGObject objectRead, BigInteger readOffset, BigInteger readSize) {
    String errorMSG =
        String.format(
            "Try reading object %s with size %d bits at offset %d bit with read type size %d bit",
            objectRead, objectRead.getSize(), readOffset, readSize);
    SMGErrorInfo newErrorInfo =
        errorInfo
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(objectRead));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithErrorInfo(memoryModel, newErrorInfo);
  }

  /**
   * Returns a copy of this {@link SMGState} with the entered SPC and {@link SMGErrorInfo} added.
   *
   * @param newMemoryModel the new {@link SymbolicProgramConfiguration} for the state. May be the
   *     same as the old one.
   * @param pErrorInfo The new {@link SMGErrorInfo} tied to the returned state.
   * @return a copy of the {@link SMGState} this is based on with the newly entered SPC and error
   *     info.
   */
  public SMGState copyWithErrorInfo(
      SymbolicProgramConfiguration newMemoryModel, SMGErrorInfo pErrorInfo) {
    SMGState copy = of(machineModel, newMemoryModel, logger, options);
    copy.errorInfo = pErrorInfo;
    return copy;
  }

  /**
   * @return memory model, including Heap, stack and global vars.
   */
  public SymbolicProgramConfiguration getMemoryModel() {
    return memoryModel;
  }

  /**
   * Add the {@link Value} mapping if it was not mapped to a {@link SMGValue}, if it was already
   * present the state is unchanged and the known {@link SMGValue} returned. The {@link SMGValue} is
   * not added to the SPC yet, writeValue() will do that.
   *
   * @param pValue the {@link Value} you want to add to the SPC.
   * @return a copy of the current {@link SMGState} with the mapping of the {@link Value} to its
   *     {@link SMGValue} entered if it was not mapped, if it was already present the state is
   *     unchanged and the known {@link SMGValue} returned.
   */
  public SMGValueAndSMGState copyAndAddValue(Value pValue) {
    Optional<SMGValue> maybeValue = memoryModel.getSMGValueFromValue(pValue);
    if (maybeValue.isPresent()) {
      return SMGValueAndSMGState.of(this, maybeValue.orElseThrow());
    } else {
      SMGValue newSMGValue = SMGValue.of();
      return SMGValueAndSMGState.of(
          of(machineModel, memoryModel.copyAndPutValue(pValue, newSMGValue), logger, options),
          newSMGValue);
    }
  }

  @SuppressWarnings("unused")
  public SMGState addElementToCurrentChain(SMGObject pVariableObject) {
    // TODO Auto-generated method stub
    return this;
  }

  @SuppressWarnings("unused")
  public SMGState addElementToCurrentChain(ValueAndSMGState pResult) {
    // TODO Auto-generated method stub
    return null;
  }

  public SMGErrorInfo getErrorInfo() {
    return errorInfo;
  }

  /**
   * Determines the SMGRegion object which is pointed by a given Value address representation.
   * Return Null SMGObject if there is no such existing address. (will result in null deref later)
   * TODO: do we need unknown derefs here?
   *
   * @param pValue - the given Value representation of the address.
   * @return the SMGObject which the address points to, or SMGObject.nullInstance() if there is no
   *     such.
   */
  public SMGObject getPointsToTarget(Value pValue) {
    Optional<SMGValue> addressOptional = memoryModel.getSMGValueFromValue(pValue);
    if (addressOptional.isPresent()) {
      Optional<SMGPointsToEdge> pointerEdgeOptional =
          memoryModel.getSmg().getPTEdge(addressOptional.orElseThrow());
      if (pointerEdgeOptional.isPresent()) {
        return pointerEdgeOptional.orElseThrow().pointsTo();
      }
    }
    return SMGObject.nullInstance();
  }

  /**
   * Read the value in the {@link SMGObject} at the position specified by the offset and size.
   * Checks for validity of the object and if its externally allocated and may fail because of that.
   * The read {@link SMGValue} will be translated into a {@link Value}. If the Value is known, the
   * known value is used, unknown else.
   *
   * @param pObject {@link SMGObject} where to read. May not be 0.
   * @param pFieldOffset {@link BigInteger} offset.
   * @param pSizeofInBits {@link BigInteger} sizeInBits.
   * @return The {@link Value} read and the {@link SMGState} after the read.
   */
  public ValueAndSMGState readValue(
      SMGObject pObject, BigInteger pFieldOffset, BigInteger pSizeofInBits) {
    if (!memoryModel.isObjectValid(pObject) && !memoryModel.isObjectExternallyAllocated(pObject)) {
      SMGState newState =
          copyWithErrorInfo(
              memoryModel, errorInfo.withObject(pObject).withErrorMessage(HAS_INVALID_READS));
      // TODO: does the analysis need to stop here?
      return ValueAndSMGState.of(UnknownValue.getInstance(), newState);
    }
    SMGValueAndSPC valueAndNewSPC = memoryModel.readValue(pObject, pFieldOffset, pSizeofInBits);
    // Try to translate the SMGValue to a Value or create a new mapping (the same read on the same
    // object/offset/size yields the same SMGValue, so it should return the same Value)
    SMGState newState = copyAndReplaceMemoryModel(valueAndNewSPC.getSPC());
    // Only use the new state for changes!
    SMGValue readSMGValue = valueAndNewSPC.getSMGValue();
    Optional<Value> maybeValue = newState.getMemoryModel().getValueFromSMGValue(readSMGValue);
    if (maybeValue.isPresent()) {
      // The Value to the SMGValue is already known, use it
      return ValueAndSMGState.of(maybeValue.orElseThrow(), newState);
    }
    // If there is no Value for the SMGValue, we need to create it as an unknown, map it and return
    Value unknownValue = UnknownValue.getInstance();
    return ValueAndSMGState.of(
        unknownValue,
        copyAndReplaceMemoryModel(
            newState.getMemoryModel().copyAndPutValue(unknownValue, readSMGValue)));
  }

  /**
   * Don't use this method outside of this class or tests! Writes into the given {@link SMGObject}
   * at the specified offset in bits with the size in bits the value given. This method adds the
   * Value <-> SMGValue mapping if none is known, else it uses a existing mapping. Make sure that
   * all checks are made before using this! I.e. size checks. (The reason why they are not made here
   * is that sometimes you need to write a lot of values but only need 1 check for the size)
   *
   * @param object the memory {@link SMGObject} to write to.
   * @param writeOffsetInBits offset in bits to be written
   * @param sizeInBits size in bits to be written
   * @param valueToWrite the value to write. Is automatically either translated to a known SMGValue
   *     or a new SMGValue is added to the returned state.
   * @return a new SMGState with the value written.
   */
  protected SMGState writeValue(
      SMGObject object, BigInteger writeOffsetInBits, BigInteger sizeInBits, Value valueToWrite) {
    SMGValueAndSMGState valueAndState = copyAndAddValue(valueToWrite);
    SMGValue smgValue = valueAndState.getSMGValue();
    SMGState currentState = valueAndState.getSMGState();
    return currentState.writeValue(object, writeOffsetInBits, sizeInBits, smgValue);
  }

  /*
   * If you are wondering why there are so many writes;
   * this is to optimize the checks and variableName <-> SMGObject and
   * Value <-> SMGValue mappings. I don't want to do unneeded checks multiple times.
   */
  private SMGState writeValue(
      SMGObject object,
      BigInteger writeOffsetInBits,
      BigInteger sizeInBits,
      SMGValue valueToWrite) {
    return copyAndReplaceMemoryModel(
        memoryModel.writeValue(object, writeOffsetInBits, sizeInBits, valueToWrite));
  }

  /**
   * Writes the entered {@link Value} to the {@link SMGObject} at the specified offset with the
   * specified size both in bits. It can be used for heap and stack, it jsut assumes that the {@link
   * SMGObject} exist in the SPC, so make sure beforehand! The Value will either add or find its
   * {@link SMGValue} counterpart automatically. Also this checks that the {@link SMGObject} is
   * large enough for the write. If something fails, this throws an exception with an error info
   * inside the state thrown with.
   *
   * @param object the {@link SMGObject} to write to.
   * @param writeOffsetInBits the offset in bits for the write of the value.
   * @param sizeInBits size of the written value in bits.
   * @param valueToWrite {@link Value} that gets written into the SPC. Will be mapped to a {@link
   *     SMGValue} automatically.
   * @return new {@link SMGState} with the value written to the object.
   * @throws SMG2Exception if something goes wrong. I.e. the sizes of the write don't match with the
   *     size of the object.
   */
  public SMGState writeValueTo(
      SMGObject object, BigInteger writeOffsetInBits, BigInteger sizeInBits, Value valueToWrite)
      throws SMG2Exception {
    // Check that the target can hold the value
    if (object.getOffset().compareTo(writeOffsetInBits) > 0
        || object.getSize().compareTo(sizeInBits) < 0) {
      // Out of range write
      throw new SMG2Exception(
          withOutOfRangeWrite(object, writeOffsetInBits, sizeInBits, valueToWrite));
    }

    return writeValue(object, writeOffsetInBits, sizeInBits, valueToWrite);
  }

  /**
   * Writes the entered {@link Value} to the region that the addressToMemory points to at the
   * specified offset with the specified size both in bits. It can be used for heap and stack, it
   * jsut assumes that the {@link SMGObject} exist in the SPC, so make sure beforehand! The Value
   * will either add or find its {@link SMGValue} counterpart automatically. Also this checks that
   * the {@link SMGObject} is large enough for the write. If something fails, this throws an
   * exception with an error info inside the state thrown with.
   *
   * @param addressToMemory the {@link Value} representing the address of the region to write to.
   * @param writeOffsetInBits the offset in bits for the write of the value.
   * @param sizeInBits size of the written value in bits.
   * @param valueToWrite {@link Value} that gets written into the SPC. Will be mapped to a {@link
   *     SMGValue} automatically.
   * @return new {@link SMGState} with the value written to the object.
   * @throws SMG2Exception if something goes wrong. I.e. the sizes of the write don't match with the
   *     size of the object.
   */
  public SMGState writeValueTo(
      Value addressToMemory,
      BigInteger writeOffsetInBits,
      BigInteger sizeInBits,
      Value valueToWrite)
      throws SMG2Exception {
    Optional<SMGObjectAndOffset> maybeRegion = memoryModel.dereferencePointer(addressToMemory);
    if (maybeRegion.isEmpty()) {
      // Can't write to non existing memory
      throw new SMG2Exception(withInvalidWrite(addressToMemory));
    }
    SMGObjectAndOffset memoryRegionAndOffset = maybeRegion.orElseThrow();
    SMGObject memoryRegion = memoryRegionAndOffset.getSMGObject();

    return writeValueTo(memoryRegion, writeOffsetInBits, sizeInBits, valueToWrite);
  }

  /**
   * Writes the memory, that is accessed by dereferencing the pointer (address) of the {@link Value}
   * given, completely to 0.
   *
   * @param addressToMemory {@link Value} that is a address pointing to a memory region.
   * @return the new {@link SMGState} with the memory region pointed to by the address written 0
   *     completely.
   * @throws SMG2Exception if there is no memory/or pointer for the given Value.
   */
  public SMGState writeToZero(Value addressToMemory) throws SMG2Exception {
    Optional<SMGObjectAndOffset> maybeRegion = memoryModel.dereferencePointer(addressToMemory);
    if (maybeRegion.isEmpty()) {
      // Can't write to non existing memory
      throw new SMG2Exception(withInvalidWrite(addressToMemory));
    }
    SMGObjectAndOffset memoryRegionAndOffset = maybeRegion.orElseThrow();
    SMGObject memoryRegion = memoryRegionAndOffset.getSMGObject();
    return writeValue(
        memoryRegion,
        memoryRegionAndOffset.getOffsetForObject(),
        memoryRegion.getSize(),
        new NumericValue(0));
  }

  /**
   * Copies the content (Values) of the source {@link SMGObject} starting from the sourceOffset into
   * the target {@link SMGObject} starting from the target offset. This expects that both the source
   * and target exist in the SPC. Throws an exception if the sizes don't match.
   *
   * @param sourceObject {@link SMGObject} from which is to be copied.
   * @param sourceStartOffset offset from which the copy is started.
   * @param targetObject target {@link SMGObject}
   * @param targetStartOffset target offset, this is the start of the writes in the target.
   * @return {@link SMGState} with the content of the source copied into the target.
   * @throws SMG2Exception if sizes don't match. I.e. if the source is larger than the target.
   */
  public SMGState copySMGObjectContentToSMGObject(
      SMGObject sourceObject,
      BigInteger sourceStartOffset,
      SMGObject targetObject,
      BigInteger targetStartOffset)
      throws SMG2Exception {
    // Check that the target can hold the source!
    if (sourceObject
            .getSize()
            .subtract(sourceStartOffset)
            .compareTo(targetObject.getSize().subtract(targetStartOffset))
        != 0) {
      throw new SMG2Exception("");
    }
    SMGState currentState = this;
    // Get all source edges
    Set<SMGHasValueEdge> sourceContents = memoryModel.getSmg().getEdges(sourceObject);
    for (SMGHasValueEdge edge : sourceContents) {
      BigInteger offset = edge.getOffset();
      // We only write edges that are >= the beginning offset of the source
      if (sourceStartOffset.compareTo(offset) <= 0) {
        BigInteger sizeInBits = edge.getSizeInBits();
        // We need to take the targetOffset to source offset difference into account
        BigInteger finalOffset = offset.subtract(sourceStartOffset).add(targetStartOffset);
        SMGValue value = edge.hasValue();
        currentState = currentState.writeValue(targetObject, finalOffset, sizeInBits, value);
      }
    }
    return currentState;
  }

  /**
   * Write to a stack (or global) variable with the name given. This method assumes that the
   * variable exists!!!! The offset and size are in bits. The {@link Value} will be added as a
   * {@link SMGValue} mapping if not known.
   *
   * @param variableName name of the variable that should be known already.
   * @param writeOffsetInBits in bits.
   * @param writeSizeInBits in bits.
   * @param valueToWrite {@link Value} to write. If its not yet known as a {@link SMGValue} then the
   *     mapping will be added.
   * @return a {@link SMGState} with the {@link Value} wirrten at the given position in the variable
   *     given.
   * @throws SMG2Exception if the write is out of range or invalid due to the variable being
   *     unknown.
   */
  public SMGState writeToStackOrGlobalVariable(
      String variableName,
      BigInteger writeOffsetInBits,
      BigInteger writeSizeInBits,
      Value valueToWrite)
      throws SMG2Exception {
    Optional<SMGObject> maybeVariableMemory =
        getMemoryModel().getObjectForVisibleVariable(variableName);

    if (maybeVariableMemory.isEmpty()) {
      // Write to unknown variable
      throw new SMG2Exception(withWriteToUnknownVariable(variableName));
    }

    SMGObject variableMemory = maybeVariableMemory.orElseThrow();
    if (variableMemory.getOffset().compareTo(writeOffsetInBits) > 0
        && variableMemory.getSize().compareTo(writeSizeInBits) < 0) {
      // Out of range write
      throw new SMG2Exception(
          withOutOfRangeWrite(variableMemory, writeOffsetInBits, writeSizeInBits, valueToWrite));
    }

    return writeValue(variableMemory, writeOffsetInBits, writeSizeInBits, valueToWrite);
  }

  /**
   * Writes the entire variable given to 0. Same as writeToStackOrGlobalVariable() else.
   *
   * @param variableName name of the variable that should be known already.
   * @return a {@link SMGState} with the {@link Value} wirrten at the given position in the variable
   *     given.
   * @throws SMG2Exception in case of errors like write to not declared variable.
   */
  public SMGState writeToStackOrGlobalVariableToZero(String variableName) throws SMG2Exception {
    Optional<SMGObject> maybeVariableMemory =
        getMemoryModel().getObjectForVisibleVariable(variableName);

    if (maybeVariableMemory.isEmpty()) {
      // Write to unknown variable
      throw new SMG2Exception(withWriteToUnknownVariable(variableName));
    }

    SMGObject variableMemory = maybeVariableMemory.orElseThrow();
    return writeValue(
        variableMemory, variableMemory.getOffset(), variableMemory.getSize(), new NumericValue(0));
  }

  /**
   * Creates a pointer (points-to-edge) from the value to the target at the specified offset. The
   * Value is mapped to a SMGValue if no mapping exists, else the existing will be used. This does
   * not check whether or not a pointer already exists but will override the target if the value
   * already has a mapping!
   *
   * @param addressValue {@link Value} used as address pointing to the target at the offset.
   * @param target {@link SMGObject} where the pointer points to.
   * @param offsetInBits offset in the object.
   * @return the new {@link SMGState} with the pointer and mapping added.
   */
  public SMGState createAndAddPointer(
      Value addressValue, SMGObject target, BigInteger offsetInBits) {
    return copyAndReplaceMemoryModel(
        memoryModel.copyAndAddPointerFromAddressToRegion(addressValue, target, offsetInBits));
  }

  /**
   * Sets the entered variable to extern. The variable has to exist in the current memory model or
   * an exception is thrown. This keeps the former association of the variable intact! So if its
   * declared global before, it is still after this method.
   *
   * @param variableName name of the variable.
   * @return new {@link SMGState} with the variable set to external.
   */
  public SMGState setExternallyAllocatedFlag(String variableName) {
    return copyAndReplaceMemoryModel(
        memoryModel.copyAndAddExternalObject(
            getMemoryModel().getObjectForVisibleVariable(variableName).orElseThrow()));
  }
}
