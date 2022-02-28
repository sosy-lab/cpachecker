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
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg2.SMGErrorInfo.Property;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSPC;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
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
    return new SMGState(
        pMachineModel,
        pSPC,
        logManager,
        opts, pErrorInfo);
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
   * Copy SMGState with a newly created object and put it into the global namespace
   *
   * Keeps consistency: yes
   *
   * @param pTypeSize Size of the type of the new global variable
   * @param pVarName Name of the global variable
   * @return Newly created object
   *
   */
  public SMGState copyAndAddGlobalVariable(int pTypeSize, String pVarName) {
    SMGObject newObject = SMGObject.of(0, BigInteger.valueOf(pTypeSize), BigInteger.ZERO);
    return of(
        machineModel, memoryModel.copyAndAddGlobalObject(newObject, pVarName), logger, options);
  }

  /**
   * Copy SMGState with a newly created object and put it into the current stack frame.
   *
   * Keeps consistency: yes
   *
   * @param pTypeSize Size of the type the new local variable
   * @param pVarName Name of the local variable
   * @return Newly created object
   */
  public SMGState copyAndAddLocalVariable(int pTypeSize, String pVarName) {
    if (memoryModel.getStackFrames().isEmpty()) {
      return this;
    }
    SMGObject newObject = SMGObject.of(0, BigInteger.valueOf(pTypeSize), BigInteger.ZERO);
    return of(
        machineModel, memoryModel.copyAndAddStackObject(newObject, pVarName), logger, options);
  }

  /**
   * Copy SMGState with a newly created anonymous object and put it into the current stack frame.
   * Used for string initilizers as function arguments.
   *
   * Keeps consistency: yes
   *
   * @param pTypeSize Size of the type the new local variable
   * @return Newly created object
   */
  public SMGState copyAndAddAnonymousVariable(int pTypeSize) {
    return copyAndAddLocalVariable(pTypeSize, makeAnonymousVariableName());
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
  public boolean areNonEqual(SMGValue pValue1, SMGValue pValue2) {
    return memoryModel.proveInequality(pValue1, pValue2);
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
      retState =
          retState.copyAndPruneVariable(
              MemoryLocation.forDeclaration(variable));
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
    return this.copyWithMemLeak(newHeap, unreachableObjects);
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
        errorInfo.withProperty(Property.INVALID_HEAP)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(leakedObjects);
    logMemoryError(errorMSG, true);
    return copyWithErrorInfo(newHeap, newErrorInfo);
  }

  // TODO: invalid read/write because of invalidated object/memory

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
   * Copy and update this {@link SMGState} with an error resulting from trying to write outside of
   * the range of the {@link SMGObject}. Returns an updated state with the error in it.
   *
   * @param objectWrittenTo the {@link SMGObject} that should have been written to.
   * @param writeOffset The offset in bits where you want to write the {@link SMGValue} to.
   * @param writeSize the size of the {@link SMGValue} in bits.
   * @param pValue the {@link SMGValue} you wanted to write.
   * @return A new SMGState with the error info.
   */
  public SMGState withOutOfRangeWrite(
      SMGObject objectWrittenTo, BigInteger writeOffset, BigInteger writeSize, SMGValue pValue) {
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
            "Try reading object %s with size %d bits at offset %d bit with read tpe size %d bit.",
            objectRead, objectRead.getSize(), readSize, readOffset);
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

  /** @return memory model, including Heap, stack and global vars. */
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

  public SMGState writeValue(SMGObject object, BigInteger offset, BigInteger sizeInBits, SMGValue value) {
    // TODO: decide if we need more checks here
    return copyAndReplaceMemoryModel(memoryModel.writeValue(object, offset, sizeInBits, value));
  }

}
