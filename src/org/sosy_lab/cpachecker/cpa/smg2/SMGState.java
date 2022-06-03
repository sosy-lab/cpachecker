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
import org.sosy_lab.cpachecker.cpa.smg2.util.value.CValue;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.CValueAndSMGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.join.SMGJoinSPC;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

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

  private final SymbolicProgramConfiguration heap;
  private final MachineModel machineModel;
  private final LogManager logger;
  private SMGErrorInfo errorInfo;
  private final SMGOptions options;

  private SMGState(
      MachineModel pMachineModel,
      SymbolicProgramConfiguration heapSPC,
      LogManager logManager,
      SMGOptions opts) {
    heap = heapSPC;
    machineModel = pMachineModel;
    logger = logManager;
    options = opts;
    errorInfo = SMGErrorInfo.of();
  }

  public static SMGState of(MachineModel pMachineModel, LogManager logManager, SMGOptions opts) {
    return new SMGState(
        pMachineModel,
        SymbolicProgramConfiguration.of(BigInteger.valueOf(pMachineModel.getSizeofPtrInBits())),
        logManager,
        opts);
  }

  public static SMGState of(
      MachineModel pMachineModel,
      SymbolicProgramConfiguration heapSPC,
      LogManager logManager,
      SMGOptions opts) {
    return new SMGState(pMachineModel, heapSPC, logManager, opts);
  }

  public SMGState withViolationsOf(SMGState pOther) {
    if (errorInfo.equals(pOther.errorInfo)) {
      return this;
    }
    SMGState result = new SMGState(machineModel, heap, logger, options);
    result.errorInfo = result.errorInfo.mergeWith(pOther.errorInfo);
    return result;
  }

  /**
   * Copy SMGState with a newly created object and put it into the global namespace
   *
   * <p>Keeps consistency: yes
   *
   * @param pTypeSize Size of the type of the new global variable
   * @param pVarName Name of the global variable
   * @return Newly created object
   */
  public SMGState copyAndAddGlobalVariable(int pTypeSize, String pVarName) {
    SMGObject newObject = SMGObject.of(0, BigInteger.valueOf(pTypeSize), BigInteger.ZERO);
    return of(machineModel, heap.copyAndAddGlobalObject(newObject, pVarName), logger, options);
  }

  /**
   * Copy SMGState with a newly created object and put it into the current stack frame.
   *
   * <p>Keeps consistency: yes
   *
   * @param pTypeSize Size of the type the new local variable
   * @param pVarName Name of the local variable
   * @return Newly created object
   */
  public SMGState copyAndAddLocalVariable(int pTypeSize, String pVarName) {
    if (heap.getStackFrames().isEmpty()) {
      return this;
    }
    SMGObject newObject = SMGObject.of(0, BigInteger.valueOf(pTypeSize), BigInteger.ZERO);
    return of(machineModel, heap.copyAndAddStackObject(newObject, pVarName), logger, options);
  }

  /**
   * Copy SMGState with a newly created anonymous object and put it into the current stack frame.
   * Used for string initilizers as function arguments.
   *
   * <p>Keeps consistency: yes
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
  public SMGState opyAndAddStackFrame(CFunctionDeclaration pFunctionDefinition) {
    return of(
        machineModel,
        heap.copyAndAddStackFrame(pFunctionDefinition, machineModel),
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

  @Override
  public SMGState join(SMGState pOther) throws CPAException, InterruptedException {
    SMGJoinSPC joinSPC = new SMGJoinSPC(heap, pOther.heap);
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

  public boolean areNonEqual(SMGValue pValue1, SMGValue pValue2) {
    return heap.proveInequality(pValue1, pValue2);
  }

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
        heap.copyAndRemoveGlobalVariable(pMemoryLocation.getIdentifier()),
        logger,
        options);
  }

  private SMGState copyAndPruneFunctionStackVariable(MemoryLocation pMemoryLocation) {
    return of(
        machineModel,
        heap.copyAndRemoveStackVariable(pMemoryLocation.getIdentifier()),
        logger,
        options);
  }

  public SMGState dropStackFrame() {
    return of(machineModel, heap.copyAndDropStackFrame(), logger, options);
  }

  public SMGState copyAndPruneUnreachable() {
    Collection<SMGObject> unreachableObjects = new HashSet<>();
    SymbolicProgramConfiguration newHeap = heap.copyAndPruneUnreachable(unreachableObjects);
    if (unreachableObjects.isEmpty()) {
      return this;
    }
    return copyWithMemLeak(newHeap, unreachableObjects);
  }

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

  public SMGState copyWithErrorInfo(SymbolicProgramConfiguration newHeap, SMGErrorInfo pErrorInfo) {
    SMGState copy = of(machineModel, newHeap, logger, options);
    copy.errorInfo = pErrorInfo;
    return copy;
  }

  public SymbolicProgramConfiguration getHeap() {
    return heap;
  }

  public Optional<SMGValue> getSMGValueForCValue(CValue cValue) {
    return getHeap().getValue(cValue);
  }

  public SMGState copyAndAddValue(CValue pValue, SMGValue pSmgValueRep) {
    if (getHeap().getValue(pValue).isPresent()) {
      return this;
    } else {
      return of(machineModel, heap.copyAndPutValue(pValue, pSmgValueRep), logger, options);
    }
  }

  @SuppressWarnings("unused")
  public SMGState addElementToCurrentChain(SMGObject pVariableObject) {
    // TODO Auto-generated method stub
    return this;
  }

  @SuppressWarnings("unused")
  public SMGState addElementToCurrentChain(CValueAndSMGState pResult) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Determines the SMGRegion object which is pointed by a given CValue address representation.
   * Return Null Object if there is no such existing address.
   *
   * @param pValue - the given CValue representation of the address.
   * @return the SMGObject which the address points to, or SMGObject.nullInstance() if there is no
   *     such.
   */
  public SMGObject getPointsToTarget(CValue pValue) {
    Optional<SMGValue> addressOptional = getSMGValueForCValue(pValue);
    if (addressOptional.isPresent()) {
      Optional<SMGPointsToEdge> pointerEdgeOptional =
          heap.getSmg().getPTEdge(addressOptional.orElseThrow());
      if (pointerEdgeOptional.isPresent()) {
        return pointerEdgeOptional.orElseThrow().pointsTo();
      }
    }
    return SMGObject.nullInstance();
  }

  public CValueAndSMGState readValue(
      SMGObject pObject, BigInteger pFieldOffset, BigInteger pSizeofInBits) {
    if (!heap.isObjectValid(pObject) && !heap.isObjectExternallyAllocated(pObject)) {
      SMGState newState =
          copyWithErrorInfo(
              heap, errorInfo.withObject(pObject).withErrorMessage(HAS_INVALID_READS));
      return CValueAndSMGState.ofUnknown(newState);
    }
    return CValueAndSMGState.of(getHeap().readValue(pObject, pFieldOffset, pSizeofInBits), this);
  }
}
