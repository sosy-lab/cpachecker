/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.counterexample.IDExpression;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGAddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.SMGIntersectStates.SMGIntersectionResult;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownAddVal;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMGConsistencyVerifier;
import org.sosy_lab.cpachecker.cpa.smg.graphs.PredRelation;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGIsLessOrEqual;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoin;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGNodeMapping;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.objects.optional.SMGOptionalObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.sll.SMGSingleLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGHeapAbstractionThreshold;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPath;
import org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.SMGStateInterpolant;
import org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.flowdependencebased.SMGFlowDependenceFieldVertice;
import org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.flowdependencebased.SMGHveSources;
import org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.flowdependencebased.SMGHveUnkownSources;
import org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.flowdependencebased.SMGPathDependence.PathPositionMemoryPathDependencys;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGPrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public class SMGState implements AbstractQueryableState, LatticeAbstractState<SMGState> {

  // Properties:
  /**
   * This property is satisfied if free is called on an address that was
   * not allocated through malloc, or was already free.
   */
  public static final String HAS_INVALID_FREES = "has-invalid-frees";

  /**
   * This property is satisfied if a pointer to an unknown or inaccessible part
   * of the memory is dereferenced.
   */
  public static final String HAS_INVALID_READS = "has-invalid-reads";

  /**
   * This property is satisfied if we write to an unknown or inaccessible part
   * of the memory.
   */
  public static final String HAS_INVALID_WRITES = "has-invalid-writes";

  /**
   * This property is satisfied if the smg contains allocated memory that is
   * inaccessible.
   */
  public static final String HAS_LEAKS = "has-leaks";

  private final boolean memoryErrors;
  private final boolean unknownOnUndefined;

  /**
   * Calculate smgState1 is less or equal smgState2 precisely by comparing
   * the join of smgstate1 with smgstate2 to smgstate2. Is needed when
   * enabling heap abstraction. Is less or equal is used for the stop operator.
   */
  private final boolean preciseIsLessOrEqualThroughJoin;

  /**
   * Creates a unique id for every smg.
   */
  private final AtomicInteger id_counter;

  /**
   * Assigns symbolic values of the smg explicit values.
   */
  private final BiMap<SMGKnownSymValue, SMGKnownExpValue> explicitValues = HashBiMap.create();

  private final CLangSMG heap;
  private final LogManager logger;

  /**
   * The unique id of the smg that was used in the transfer relation
   * to create this smg.
   */
  private final int predecessorId;

  /**
   * The unique id of this smg.
   */
  private final int id;

  /**
   * There are several constraints for a smg to be a valid smg.
   * This option enables checking if these constraints are still
   * satisfied for this smg.
   */
  private final SMGRuntimeCheck runtimeCheckLevel;
  private final Pattern externalAllocationRecursivePattern = Pattern.compile("^(r_)(\\d+)(_.*)$");
  private final int externalAllocationSize;
  private final boolean trackPredicates;

  private final SMGHveSources sourcesOfHve;

  /**
   * This flag signals if this smg was created at the end of a block.
   * Blocks can be configured with the {@link BlockOperator} class.
   * It is used to determine, when to merge or to calculate coverage with
   * the stop operator.
   */
  private boolean blockEnded;

  //TODO These flags are not enough, they should contain more about the nature of the error.
  private final boolean invalidWrite;
  private final boolean invalidRead;
  private final boolean invalidFree;

  private void issueMemoryLeakMessage() {
    issueMemoryError("Memory leak found", false);
  }

  private void issueInvalidReadMessage() {
    issueMemoryError("Invalid read found", true);
  }

  private void issueInvalidWriteMessage() {
    issueMemoryError("Invalid write found", true);
  }

  private void issueInvalidFreeMessage() {
    issueMemoryError("Invalid free found", true);
  }

  private void issueMemoryError(String pMessage, boolean pUndefinedBehavior) {
    if (memoryErrors) {
      logger.log(Level.FINE, pMessage);
    } else if (pUndefinedBehavior) {
      logger.log(Level.FINE, pMessage);
      logger.log(Level.FINE,
          "Non-target undefined behavior detected. The verification result is unreliable.");
    }
  }

  /**
   * Constructor.
   *
   * Keeps consistency: yes
   *
   * @param pLogger A logger to log any messages
   * @param pMachineModel A machine model for the underlying SMGs
   * @param pTargetMemoryErrors targets property false valid memtrack
   * @param pUnknownOnUndefined assumes unknown value if undefined
   * @param pSMGRuntimeCheck consistency check threshold
   * @param pPreciseIsLessOrEqualThroughJoin should is less or equal method be performed with a join
   */
  public SMGState(LogManager pLogger, MachineModel pMachineModel, boolean pTargetMemoryErrors,
      boolean pUnknownOnUndefined, SMGRuntimeCheck pSMGRuntimeCheck, int pExternalAllocationSize,
      boolean pTrackPredicates, boolean pPreciseIsLessOrEqualThroughJoin) {
    heap = new CLangSMG(pMachineModel);
    logger = pLogger;
    id_counter = new AtomicInteger(0);
    predecessorId = id_counter.getAndIncrement();
    id = id_counter.getAndIncrement();
    memoryErrors = pTargetMemoryErrors;
    unknownOnUndefined = pUnknownOnUndefined;

    if (pSMGRuntimeCheck == null) {
      runtimeCheckLevel = SMGRuntimeCheck.NONE;
    } else {
      runtimeCheckLevel = pSMGRuntimeCheck;
    }

    invalidFree = false;
    invalidRead = false;
    invalidWrite = false;
    externalAllocationSize = pExternalAllocationSize;
    trackPredicates = pTrackPredicates;
    preciseIsLessOrEqualThroughJoin = pPreciseIsLessOrEqualThroughJoin;
    blockEnded = false;
    sourcesOfHve = SMGHveUnkownSources.getInstance();
  }

  /**
   * Constructor.
   *
   * Keeps consistency: yes
   *
   * @param pLogger A logger to log any messages
   * @param pMachineModel A machine model for the underlying SMGs
   * @param pTargetMemoryErrors targets property false valid memtrack
   * @param pUnknownOnUndefined assumes unknown value if undefined
   * @param pSMGRuntimeCheck consistency check threshold
   * @param pPreciseIsLessOrEqualThroughJoin should is less or equal method be performed with a join
   */
  public SMGState(LogManager pLogger, MachineModel pMachineModel, boolean pTargetMemoryErrors,
      boolean pUnknownOnUndefined, SMGRuntimeCheck pSMGRuntimeCheck, int pExternalAllocationSize,
      boolean pTrackPredicates, boolean pPreciseIsLessOrEqualThroughJoin, boolean pTrackHveSources) {
    heap = new CLangSMG(pMachineModel);
    logger = pLogger;
    id_counter = new AtomicInteger(0);
    predecessorId = id_counter.getAndIncrement();
    id = id_counter.getAndIncrement();
    memoryErrors = pTargetMemoryErrors;
    unknownOnUndefined = pUnknownOnUndefined;

    if (pSMGRuntimeCheck == null) {
      runtimeCheckLevel = SMGRuntimeCheck.NONE;
    } else {
      runtimeCheckLevel = pSMGRuntimeCheck;
    }

    invalidFree = false;
    invalidRead = false;
    invalidWrite = false;
    externalAllocationSize = pExternalAllocationSize;
    trackPredicates = pTrackPredicates;
    preciseIsLessOrEqualThroughJoin = pPreciseIsLessOrEqualThroughJoin;
    blockEnded = false;

    if (pTrackHveSources) {
      sourcesOfHve = new SMGHveSources();
    } else {
      sourcesOfHve = SMGHveUnkownSources.getInstance();
    }
  }

  /**
   * Merge Constructor.
   *
   * Keeps Consistency: Yes.
   *
   * @param pWeakenedState The smg that is weakened through the merge operator.
   * @param pHeap the merged smg.
   * @param pMergedExplicitValues the explicit values of the merges smg.
   */
  public SMGState(SMGState pWeakenedState, CLangSMG pHeap,
      Map<SMGKnownSymValue, SMGKnownExpValue> pMergedExplicitValues) {
    // merge
    heap = pHeap;
    logger = pWeakenedState.logger;
    id_counter = pWeakenedState.id_counter;
    predecessorId = pWeakenedState.predecessorId;
    id = id_counter.getAndIncrement();
    memoryErrors = pWeakenedState.memoryErrors;
    unknownOnUndefined = pWeakenedState.unknownOnUndefined;
    runtimeCheckLevel = pWeakenedState.runtimeCheckLevel;
    invalidFree = pWeakenedState.invalidFree;
    invalidRead = pWeakenedState.invalidRead;
    invalidWrite = pWeakenedState.invalidWrite;
    explicitValues.putAll(pMergedExplicitValues);
    trackPredicates = pWeakenedState.trackPredicates;
    externalAllocationSize = pWeakenedState.externalAllocationSize;
    preciseIsLessOrEqualThroughJoin = pWeakenedState.preciseIsLessOrEqualThroughJoin;
    blockEnded = pWeakenedState.blockEnded;
    sourcesOfHve = SMGHveUnkownSources.getInstance();
  }

  /**
   * Copy constructor for junit Test.
   *
   * Keeps Consistency: Yes.
   *
   * @param pOriginalState copy of this smg.
   * @param pSMGRuntimeCheck level of runtime consistency check
   */
  SMGState(SMGState pOriginalState, SMGRuntimeCheck pSMGRuntimeCheck) {
    heap = new CLangSMG(pOriginalState.heap);
    logger = pOriginalState.logger;
    predecessorId = pOriginalState.getId();
    id_counter = pOriginalState.id_counter;
    id = id_counter.getAndIncrement();
    explicitValues.putAll(pOriginalState.explicitValues);
    memoryErrors = pOriginalState.memoryErrors;
    unknownOnUndefined = pOriginalState.unknownOnUndefined;
    runtimeCheckLevel = pSMGRuntimeCheck;
    invalidFree = pOriginalState.invalidFree;
    invalidRead = pOriginalState.invalidRead;
    invalidWrite = pOriginalState.invalidWrite;
    externalAllocationSize = pOriginalState.externalAllocationSize;
    trackPredicates = pOriginalState.trackPredicates;
    preciseIsLessOrEqualThroughJoin = pOriginalState.preciseIsLessOrEqualThroughJoin;
    blockEnded = pOriginalState.blockEnded;
    sourcesOfHve = SMGHveUnkownSources.getInstance();
  }

  /**
   * Copy constructor.
   *
   * Keeps consistency: yes
   *
   * @param pOriginalState Original state. Will be the predecessor of the
   * new state
   */
  public SMGState(SMGState pOriginalState) {
    heap = new CLangSMG(pOriginalState.heap);
    logger = pOriginalState.logger;
    predecessorId = pOriginalState.getId();
    id_counter = pOriginalState.id_counter;
    id = id_counter.getAndIncrement();
    explicitValues.putAll(pOriginalState.explicitValues);
    memoryErrors = pOriginalState.memoryErrors;
    unknownOnUndefined = pOriginalState.unknownOnUndefined;
    runtimeCheckLevel = pOriginalState.runtimeCheckLevel;
    invalidFree = pOriginalState.invalidFree;
    invalidRead = pOriginalState.invalidRead;
    invalidWrite = pOriginalState.invalidWrite;
    externalAllocationSize = pOriginalState.externalAllocationSize;
    trackPredicates = pOriginalState.trackPredicates;
    preciseIsLessOrEqualThroughJoin = pOriginalState.preciseIsLessOrEqualThroughJoin;
    blockEnded = pOriginalState.blockEnded;
    sourcesOfHve = pOriginalState.sourcesOfHve.copy();
  }

  private SMGState(SMGState pOriginalState, SMGHveSources pSources) {
    heap = new CLangSMG(pOriginalState.heap);
    logger = pOriginalState.logger;
    predecessorId = pOriginalState.getId();
    id_counter = pOriginalState.id_counter;
    id = id_counter.getAndIncrement();
    explicitValues.putAll(pOriginalState.explicitValues);
    memoryErrors = pOriginalState.memoryErrors;
    unknownOnUndefined = pOriginalState.unknownOnUndefined;
    runtimeCheckLevel = pOriginalState.runtimeCheckLevel;
    invalidFree = pOriginalState.invalidFree;
    invalidRead = pOriginalState.invalidRead;
    invalidWrite = pOriginalState.invalidWrite;
    externalAllocationSize = pOriginalState.externalAllocationSize;
    trackPredicates = pOriginalState.trackPredicates;
    preciseIsLessOrEqualThroughJoin = pOriginalState.preciseIsLessOrEqualThroughJoin;
    blockEnded = pOriginalState.blockEnded;
    sourcesOfHve = pSources;
  }

  /**
   * Constructor.
   *
   * Keeps Consistency: Yes
   *
   * Used when identifying a satisfied property.
   *
   * @param pOriginalState state that satisfied a property
   * @param pProperty property that was satisfied.
   */
  protected SMGState(SMGState pOriginalState, Property pProperty) {
    heap = new CLangSMG(pOriginalState.heap);
    logger = pOriginalState.logger;
    predecessorId = pOriginalState.getId();
    id_counter = pOriginalState.id_counter;
    id = id_counter.getAndIncrement();
    explicitValues.putAll(pOriginalState.explicitValues);
    memoryErrors = pOriginalState.memoryErrors;
    unknownOnUndefined = pOriginalState.unknownOnUndefined;
    runtimeCheckLevel = pOriginalState.runtimeCheckLevel;
    preciseIsLessOrEqualThroughJoin = pOriginalState.preciseIsLessOrEqualThroughJoin;
    trackPredicates = pOriginalState.trackPredicates;
    externalAllocationSize = pOriginalState.externalAllocationSize;
    blockEnded = pOriginalState.blockEnded;

    boolean pInvalidFree = pOriginalState.invalidFree;
    boolean pInvalidRead = pOriginalState.invalidRead;
    boolean pInvalidWrite = pOriginalState.invalidWrite;

    switch (pProperty) {
      case INVALID_FREE:
        pInvalidFree = true;
        break;
      case INVALID_READ:
        pInvalidRead = true;
        break;
      case INVALID_WRITE:
        pInvalidWrite = true;
        break;
      case INVALID_HEAP:
        break;
      default:
        throw new AssertionError();
    }

    invalidFree = pInvalidFree;
    invalidRead = pInvalidRead;
    invalidWrite = pInvalidWrite;
    sourcesOfHve = pOriginalState.sourcesOfHve.copy();
  }
/**
 * Constructor.
 *
 * Keeps Consistency: Yes.
 *
 * Used for JUnit Testing.
 *
 * @param pLogger used to log messages
 * @param pSmg smg of smgState.
 */
  SMGState(LogManager pLogger,
      CLangSMG pSmg) {
    heap = pSmg;
    logger = pLogger;
    predecessorId = 0;
    id_counter = new AtomicInteger(1);
    id = id_counter.getAndIncrement();
    memoryErrors = false;
    unknownOnUndefined = false;
    runtimeCheckLevel = SMGRuntimeCheck.FULL;
    invalidFree = false;
    invalidRead = false;
    invalidWrite = false;
    externalAllocationSize = 5;
    trackPredicates = false;
    preciseIsLessOrEqualThroughJoin = false;
    blockEnded = false;
    sourcesOfHve = SMGHveUnkownSources.getInstance();
  }

  /**
   * Makes SMGState create a new object and put it into the global namespace
   *
   * Keeps consistency: yes
   *
   * @param pTypeSize Size of the type of the new global variable
   * @param pVarName Name of the global variable
   * @return Newly created object
   *
   * @throws SMGInconsistentException when resulting SMGState is inconsistent
   * and the checks are enabled
   */
  public SMGObject addGlobalVariable(SMGKnownExpValue pTypeSize, String pVarName)
      throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(pTypeSize.getAsInt(), pVarName);

    heap.addGlobalObject(new_object);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    sourcesOfHve.registerNewObjectAllocation(pTypeSize, new_object, true);
    return new_object;
  }

  /**
   * Change if the current smg represents the end of a block defined by {@link BlockOperator}.
   *
   * @param pBlockEnded true, if block ends, false otherwise.
   */
  public void setBlockEnded(boolean pBlockEnded) {
    blockEnded = pBlockEnded;
  }

  /**
   * Makes SMGState create a new object and put it into the current stack
   * frame.
   *
   * Keeps consistency: yes
   *
   * @param pTypeSize Size of the type the new local variable
   * @param pVarName Name of the local variable
   * @return Newly created object
   *
   * @throws SMGInconsistentException when resulting SMGState is inconsistent
   * and the checks are enabled
   */
  public SMGObject addLocalVariable(SMGKnownExpValue pTypeSize, String pVarName)
      throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(pTypeSize.getAsInt(), pVarName);

    heap.addStackObject(new_object);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    sourcesOfHve.registerNewObjectAllocation(pTypeSize, new_object, true);
    return new_object;
  }

  /**
   * Makes SMGState create a new object, compares it with the given object, and puts the given object into the current stack
   * frame.
   *
   * Keeps consistency: yes
   *
   * @param pTypeSize Size of the type of the new variable
   * @param pVarName Name of the local variable
   * @param smgObject object of local variable
   * @return given object
   *
   * @throws SMGInconsistentException when resulting SMGState is inconsistent
   * and the checks are enabled
   */
  public SMGObject addLocalVariable(SMGKnownExpValue pTypeSize, String pVarName, SMGRegion smgObject)
      throws SMGInconsistentException {
    SMGRegion new_object2 = new SMGRegion(pTypeSize.getAsInt(), pVarName);

    assert smgObject.getLabel().equals(new_object2.getLabel());

    // arrays are converted to pointers
    assert smgObject.getSize() == pTypeSize.getAsInt()
        || smgObject.getSize() == heap.getMachineModel().getSizeofPtr();

    heap.addStackObject(smgObject);
    sourcesOfHve.registerNewObjectAllocation(pTypeSize, smgObject, true);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return smgObject;
  }

  /**
   * Adds a new frame for the function.
   *
   * Keeps consistency: yes
   *
   * @param pFunctionDefinition A function for which to create a new stack frame
   */
  public void addStackFrame(CFunctionDeclaration pFunctionDefinition)
      throws SMGInconsistentException {
    heap.addStackFrame(pFunctionDefinition);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
  }

  /**
   * Constant.
   *
   * @return The ID of this SMGState
   */
  final public int getId() {
    return id;
  }

  /**
   * Constant.
   * .
   * @return The predecessor state, i.e. one from which this one was copied
   */
  final public int getPredecessorId() {
    return predecessorId;
  }

  /**
   * Constant.
   *
   * @return A {@link SMGObject} for current function return value storage.
   */
  final public SMGObject getFunctionReturnObject() {
    return heap.getFunctionReturnObject();
  }

  /**
   * Get memory of variable with the given name.
   *
   * @param pVariableName A name of the desired variable
   * @return An object corresponding to the variable name
   */
  public SMGObject getObjectForVisibleVariable(String pVariableName) {
    return heap.getObjectForVisibleVariable(pVariableName);
  }

  /**
   * Based on the current setting of runtime check level, it either performs
   * a full consistency check or not. If the check is performed and the
   * state is deemed inconsistent, a {@link SMGInconsistentException} is thrown.
   *
   * Constant.
   *
   * @param pLevel A level of the check request. When e.g. HALF is passed, it
   * means "perform the check if the setting is HALF or finer.
   */
  final public void performConsistencyCheck(SMGRuntimeCheck pLevel)
      throws SMGInconsistentException {
    if (pLevel == null || runtimeCheckLevel.isFinerOrEqualThan(pLevel)) {
      if (!CLangSMGConsistencyVerifier.verifyCLangSMG(logger,
          heap)) { throw new SMGInconsistentException(
              "SMG was found inconsistent during a check on state id " + this.getId()); }
    }
  }

  /**
   * Returns a DOT representation of the SMGState.
   *
   * Constant.
   *
   * @param pName A name of the graph.
   * @param pLocation A location in the program.
   * @return String containing a DOT graph corresponding to the SMGState.
   */
  public String toDot(String pName, String pLocation) {
    SMGPlotter plotter = new SMGPlotter();
    return plotter.smgAsDot(heap, pName, pLocation, explicitValues);
  }

  /**
   * @return A string representation of the SMGState.
   */
  @Override
  public String toString() {
    if (getPredecessorId() != 0) {
      return "SMGState [" + getId() + "] <-- parent [" + getPredecessorId() + "]\n"
          + heap.toString();
    } else {
      return "SMGState [" + getId() + "] <-- no parent, initial state\n" + heap.toString();
    }
  }

  /**
   * Returns a address leading from a value. If the target is an abstract heap segment,
   * materialize heap segment.
   *
   * Keeps Consistency.
   *
   * @param pValue A value for which to return the address.
   * @return the address represented by the passed value. The value needs to be
   * a pointer, i.e. it needs to have a points-to edge. If it does not have it, the method raises
   * an exception.
   *
   * @throws SMGInconsistentException When the value passed does not have a Points-To edge.
   */
  public SMGAddressValueAndStateList getPointerFromValue(SMGKnownSymValue pValue)
      throws SMGInconsistentException {

    int value = pValue.getAsInt();

    if (heap.isPointer(value)) {
      SMGEdgePointsTo addressValue = heap.getPointer(value);

      SMGAddressValue address = sourcesOfHve.createPointer(pValue, addressValue);

      SMGObject obj = address.getObject();

      if (obj.isAbstract()) {
        SMGAddressValueAndStateList result =
            handleMaterilisation(addressValue, ((SMGAbstractObject) obj));
        performConsistencyCheck(SMGRuntimeCheck.HALF);
        return result;
      }

      return SMGAddressValueAndStateList.of(SMGAddressValueAndState.of(this, address));
    }

    throw new SMGInconsistentException("Asked for a Points-To edge for a non-pointer value");
  }


  private SMGAddressValueAndStateList handleMaterilisation(SMGEdgePointsTo pointerToAbstractObject,
      SMGAbstractObject pSmgAbstractObject) throws SMGInconsistentException {

    switch (pSmgAbstractObject.getKind()) {
      case DLL:
        SMGDoublyLinkedList dllListSeg = (SMGDoublyLinkedList) pSmgAbstractObject;

        if (dllListSeg.getMinimumLength() == 0) {
          List<SMGAddressValueAndState> result = new ArrayList<>(2);
          SMGState removalState = this.createSuccessor();
          SMGAddressValueAndStateList removalResult =
              removalState.removeDll(dllListSeg, pointerToAbstractObject);
          result.addAll(removalResult.asAddressValueAndStateList());
          SMGAddressValueAndState resultOfMaterilisation =
              materialiseDll(dllListSeg, pointerToAbstractObject);
          result.add(resultOfMaterilisation);
          return SMGAddressValueAndStateList.copyOfAddressValueList(result);
        } else {
          SMGAddressValueAndState result = materialiseDll(dllListSeg, pointerToAbstractObject);
          return SMGAddressValueAndStateList.of(result);
        }
      case SLL:
        SMGSingleLinkedList sllListSeg = (SMGSingleLinkedList) pSmgAbstractObject;

        if (sllListSeg.getMinimumLength() == 0) {
          List<SMGAddressValueAndState> result = new ArrayList<>(2);
          SMGState removalState = this.createSuccessor();
          SMGAddressValueAndStateList resultOfRemoval =
              removalState.removeSll(sllListSeg, pointerToAbstractObject);
          result.addAll(resultOfRemoval.asAddressValueAndStateList());
          SMGAddressValueAndState resultOfMaterilisation =
              materialiseSll(sllListSeg, pointerToAbstractObject);
          result.add(resultOfMaterilisation);
          return SMGAddressValueAndStateList.copyOfAddressValueList(result);
        } else {
          SMGAddressValueAndState result = materialiseSll(sllListSeg, pointerToAbstractObject);
          return SMGAddressValueAndStateList.of(result);
        }
      case OPTIONAL:
        List<SMGAddressValueAndState> result = new ArrayList<>(2);
        SMGOptionalObject optionalObject = (SMGOptionalObject) pSmgAbstractObject;
        SMGState removalState = this.createSuccessor();
        SMGAddressValueAndStateList resultOfRemoval =
            removalState.removeOptionalObject(optionalObject);
        result.addAll(resultOfRemoval.asAddressValueAndStateList());
        SMGAddressValueAndState resultOfMaterilisation =
            materialiseOptionalObject(optionalObject, pointerToAbstractObject);
        result.add(resultOfMaterilisation);
        return SMGAddressValueAndStateList.copyOfAddressValueList(result);
      default:
        throw new UnsupportedOperationException("Materilization of abstraction"
            + pSmgAbstractObject.toString() + " not yet implemented.");
    }
  }

  private SMGAddressValueAndStateList removeOptionalObject(SMGOptionalObject pOptionalObject)
      throws SMGInconsistentException {

    logger.log(Level.ALL, "Remove ", pOptionalObject, " in state id ", this.getId());

    /*Just remove the optional Object and merge all incoming pointer
     * with the one pointer in all fields of the optional edge.
     * If there is no pointer besides zero in the fields of the
     * optional object, use zero.*/

    Set<SMGEdgePointsTo> pointer = heap.getPointerToObject(pOptionalObject);

    Set<SMGEdgeHasValue> fields = getHVEdges(SMGEdgeHasValueFilter.objectFilter(pOptionalObject));

    heap.removeHeapObjectAndEdges(pOptionalObject);

    int pointerValue = 0;

    for (SMGEdgeHasValue field : fields) {
      if (heap.isPointer(field.getValue()) && field.getValue() != 0) {
        pointerValue = field.getValue();
        break;
      }
    }

    for (SMGEdgePointsTo edge : pointer) {
      heap.removePointsToEdge(edge.getValue());
      heap.mergeValues(pointerValue, edge.getValue());
    }

    SMGAddressValueAndStateList result = getPointerFromValue(SMGKnownSymValue.valueOf(pointerValue));

    return result;
  }

  private SMGAddressValueAndState materialiseOptionalObject(SMGOptionalObject pOptionalObject,
      SMGEdgePointsTo pPointerToAbstractObject) {

    /*Just replace the optional object with a region*/
    logger.log(Level.ALL,
        "Materialise ", pOptionalObject, " in state id ", this.getId());

    Set<SMGEdgePointsTo> pointer = heap.getPointerToObject(pOptionalObject);

    Set<SMGEdgeHasValue> fields = getHVEdges(SMGEdgeHasValueFilter.objectFilter(pOptionalObject));

    SMGObject newObject = new SMGRegion(pOptionalObject.getSize(),
        "Concrete object of " + pOptionalObject.toString(), pOptionalObject.getLevel());

    heap.addHeapObject(newObject);
    heap.setValidity(newObject, heap.isObjectValid(pOptionalObject));

    heap.removeHeapObjectAndEdges(pOptionalObject);

    for (SMGEdgeHasValue edge : fields) {
      addHasValueEdge(edge.getObject(), edge.getOffset(),
          new SMGEdgeHasValue(edge.getType(), edge.getOffset(), newObject, edge.getValue()));
    }

    for (SMGEdgePointsTo edge : pointer) {
      heap.removePointsToEdge(edge.getValue());
      heap.addPointsToEdge(new SMGEdgePointsTo(edge.getValue(), newObject, edge.getOffset()));
    }

    SMGAddressValueAndState result =
        SMGAddressValueAndState.of(this, SMGKnownAddVal.valueOf(pPointerToAbstractObject.getValue(),
            newObject, pPointerToAbstractObject.getOffset()));

    return result;
  }

  private SMGAddressValueAndStateList removeSll(SMGSingleLinkedList pListSeg,
      SMGEdgePointsTo pPointerToAbstractObject) throws SMGInconsistentException {

    logger.log(Level.ALL, "Remove ", pListSeg, " in state id ", this.getId());

    /*First, set all sub smgs of sll to be removed to invalid.*/
    Set<Integer> restriction = ImmutableSet.of(pListSeg.getNfo());

    removeRestrictedSubSmg(pListSeg, restriction);

    /*When removing sll, connect target specifier first pointer to next field*/

    int nfo = pListSeg.getNfo();
    int hfo = pListSeg.getHfo();

    SMGEdgeHasValue nextEdge = getHveEdgePointerForMaterialsation(pListSeg, nfo);
    SMGEdgePointsTo nextPointerEdge = heap.getPointer(nextEdge.getValue());

    Integer firstPointer = getAddress(pListSeg, hfo, SMGTargetSpecifier.FIRST);

    heap.removeHeapObjectAndEdges(pListSeg);

    heap.mergeValues(nextEdge.getValue(), firstPointer);

    if (firstPointer == pPointerToAbstractObject.getValue()) {
      return getPointerFromValue(SMGKnownSymValue.valueOf(nextPointerEdge.getValue()));
    } else {
      throw new AssertionError(
          "Unexpected dereference of pointer " + pPointerToAbstractObject.getValue()
              + " pointing to abstraction " + pListSeg.toString());
    }
  }

  private SMGAddressValueAndStateList removeDll(SMGDoublyLinkedList pListSeg,
      SMGEdgePointsTo pPointerToAbstractObject) throws SMGInconsistentException {

    logger.log(Level.ALL, "Remove ", pListSeg, " in state id ", this.getId());

    /*First, set all sub smgs of dll to be removed to invalid.*/
    Set<Integer> restriction = ImmutableSet.of(pListSeg.getNfo(), pListSeg.getPfo());

    removeRestrictedSubSmg(pListSeg, restriction);

    /*When removing dll, connect target specifier first pointer to next field,
     * and target specifier last to prev field*/

    int nfo = pListSeg.getNfo();
    int pfo = pListSeg.getPfo();
    int hfo = pListSeg.getHfo();

    SMGEdgeHasValue nextEdge = getHveEdgePointerForMaterialsation(pListSeg, nfo);
    SMGEdgeHasValue prevEdge = getHveEdgePointerForMaterialsation(pListSeg, pfo);

    SMGEdgePointsTo nextPointerEdge = heap.getPointer(nextEdge.getValue());
    SMGEdgePointsTo prevPointerEdge = heap.getPointer(prevEdge.getValue());

    Integer firstPointer = getAddress(pListSeg, hfo, SMGTargetSpecifier.FIRST);
    Integer lastPointer = getAddress(pListSeg, hfo, SMGTargetSpecifier.LAST);

    heap.removeHeapObjectAndEdges(pListSeg);

    /* We may not have pointers to the beginning/end to this list.
     *  */

    if (firstPointer != null) {
      heap.mergeValues(nextEdge.getValue(), firstPointer);
    }

    if (lastPointer != null) {
      heap.mergeValues(prevEdge.getValue(), lastPointer);
    }

    if (firstPointer != null && firstPointer == pPointerToAbstractObject.getValue()) {
      return getPointerFromValue(SMGKnownSymValue.valueOf(nextPointerEdge.getValue()));
    } else if (lastPointer != null && lastPointer == pPointerToAbstractObject.getValue()) {
      return getPointerFromValue(SMGKnownSymValue.valueOf(prevPointerEdge.getValue()));
    } else {
      throw new AssertionError(
          "Unexpected dereference of pointer " + pPointerToAbstractObject.getValue()
              + " pointing to abstraction " + pListSeg.toString());
    }
  }

  private SMGAddressValueAndState materialiseSll(SMGSingleLinkedList pListSeg,
      SMGEdgePointsTo pPointerToAbstractObject) throws SMGInconsistentException {

    logger.log(Level.ALL, "Materialise ", pListSeg, " in state id ", this.getId());

    if (pPointerToAbstractObject
        .getTargetSpecifier() != SMGTargetSpecifier.FIRST) {
    throw new SMGInconsistentException(
            "Target specifier of pointer " + pPointerToAbstractObject.getValue()
                + "that leads to a sll has unexpected target specifier "
                + pPointerToAbstractObject.getTargetSpecifier().toString());
      }

    SMGRegion newConcreteRegion = new SMGRegion(pListSeg.getSize(),
        "concrete sll segment ID " + SMGValueFactory.getNewValue(), 0);
    heap.addHeapObject(newConcreteRegion);

    Set<Integer> restriction = ImmutableSet.of(pListSeg.getNfo());

    copyRestrictedSubSmgToObject(pListSeg, newConcreteRegion, restriction);

    int hfo = pListSeg.getHfo();
    int nfo = pListSeg.getNfo();

    SMGEdgeHasValue oldSllFieldToOldRegion = getHveEdgePointerForMaterialsation(pListSeg, nfo);

    int oldPointerToSll = pPointerToAbstractObject.getValue();

    Set<SMGEdgeHasValue> oldFieldsEdges =
        heap.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pListSeg));
    Set<SMGEdgePointsTo> oldPtEdges = heap.getPointerToObject(pListSeg);

    heap.removeHasValueEdge(oldSllFieldToOldRegion);
    heap.removePointsToEdge(oldPointerToSll);

    heap.removeHeapObjectAndEdges(pListSeg);

    SMGSingleLinkedList newSll = new SMGSingleLinkedList(pListSeg.getSize(), pListSeg.getHfo(),
        pListSeg.getNfo(), pListSeg.getMinimumLength() > 0 ? pListSeg.getMinimumLength() - 1 : 0,
        0);

    heap.addHeapObject(newSll);
    heap.setValidity(newSll, true);

    /*Check if pointer was already created due to All target Specifier*/
    SMGSymbolicValue newPointerToNewRegion = getAddress(newConcreteRegion, hfo);

    if (!newPointerToNewRegion.isUnknown()) {
      heap.removePointsToEdge(newPointerToNewRegion.getAsInt());
      heap.mergeValues(oldPointerToSll, newPointerToNewRegion.getAsInt());
    }

    SMGEdgePointsTo newPtEdgeToNewRegionFromOutsideSMG =
        new SMGEdgePointsTo(oldPointerToSll, newConcreteRegion, hfo);

    int newPointerToSll = SMGValueFactory.getNewValue();

    /*If you can't find the pointer, use generic pointer type*/
    CType typeOfPointerToSll;

    Set<SMGEdgeHasValue> fieldsContainingOldPointerToSll =
        heap.getHVEdges(SMGEdgeHasValueFilter.valueFilter(oldPointerToSll));

    if (fieldsContainingOldPointerToSll.isEmpty()) {
      typeOfPointerToSll = CPointerType.POINTER_TO_VOID;
    } else {
      typeOfPointerToSll = fieldsContainingOldPointerToSll.iterator().next().getType();
    }

    SMGEdgeHasValue newFieldFromNewRegionToSll = new SMGEdgeHasValue(
        typeOfPointerToSll, nfo, newConcreteRegion, newPointerToSll);
    SMGEdgePointsTo newPtEToSll =
        new SMGEdgePointsTo(newPointerToSll, newSll, hfo, SMGTargetSpecifier.FIRST);

    for (SMGEdgeHasValue hve : oldFieldsEdges) {
      addHasValueEdge(hve.getObject(), hve.getOffset(),
          new SMGEdgeHasValue(hve.getType(), hve.getOffset(), newSll, hve.getValue()));
    }

    for (SMGEdgePointsTo ptE : oldPtEdges) {
      heap.addPointsToEdge(
          new SMGEdgePointsTo(ptE.getValue(), newSll, ptE.getOffset(), ptE.getTargetSpecifier()));
    }

    heap.addPointsToEdge(newPtEdgeToNewRegionFromOutsideSMG);

    heap.addValue(newPointerToSll);
    heap.addHasValueEdge(newFieldFromNewRegionToSll);
    sourcesOfHve.registerHasValueEdge(newFieldFromNewRegionToSll);
    heap.addPointsToEdge(newPtEToSll);

    return SMGAddressValueAndState.of(this,
        SMGKnownAddVal.valueOf(oldPointerToSll, newConcreteRegion, hfo));
  }

  private SMGEdgeHasValue getHveEdgePointerForMaterialsation(SMGObject pObject, int pOffset) throws SMGInconsistentException {

    Set<SMGEdgeHasValue> edges =
        getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject).filterAtOffset(pOffset));

    if (edges.size() == 1) {
      return Iterables.getOnlyElement(edges);
    }

    if (heap.isCoveredByNullifiedBlocks(
        new SMGEdgeHasValue(heap.getMachineModel().getSizeofPtr(), pOffset, pObject, 0))) {
      SMGStateEdgePair result = writeValue(SMGAddress.valueOf(pObject, pOffset),
          CPointerType.POINTER_TO_VOID, SMGKnownSymValue.ZERO);
      return result.getNewEdge();
    } else {
      SMGStateEdgePair result =
          writeValue(SMGAddress.valueOf(pObject, pOffset), CPointerType.POINTER_TO_VOID,
              SMGValueFactory.getNewSymbolicValue());
      return result.getNewEdge();
    }
  }

  private SMGAddressValueAndState materialiseDll(SMGDoublyLinkedList pListSeg,
      SMGEdgePointsTo pPointerToAbstractObject) throws SMGInconsistentException {

    logger.log(Level.ALL, "Materialise ", pListSeg, " in state id ", this.getId());

    SMGRegion newConcreteRegion = new SMGRegion(pListSeg.getSize(),
        "concrete dll segment ID " + SMGValueFactory.getNewValue(), 0);
    heap.addHeapObject(newConcreteRegion);

    Set<Integer> restriction = ImmutableSet.of(pListSeg.getNfo(), pListSeg.getPfo());

    copyRestrictedSubSmgToObject(pListSeg, newConcreteRegion, restriction);

    SMGTargetSpecifier tg = pPointerToAbstractObject.getTargetSpecifier();

    int offsetPointingToDll;
    int offsetPointingToRegion;

    switch (tg) {
      case FIRST:
        offsetPointingToDll = pListSeg.getNfo();
        offsetPointingToRegion = pListSeg.getPfo();
        break;
      case LAST:
        offsetPointingToDll = pListSeg.getPfo();
        offsetPointingToRegion = pListSeg.getNfo();
        break;
      default:
        throw new SMGInconsistentException(
            "Target specifier of pointer " + pPointerToAbstractObject.getValue()
                + "that leads to a dll has unexpected target specifier " + tg.toString());
    }

    int hfo = pListSeg.getHfo();

    SMGEdgeHasValue oldDllFieldToOldRegion = getHveEdgePointerForMaterialsation(pListSeg, offsetPointingToRegion);
    int oldPointerToDll = pPointerToAbstractObject.getValue();

    heap.removeHasValueEdge(oldDllFieldToOldRegion);
    heap.removePointsToEdge(oldPointerToDll);

    Set<SMGEdgeHasValue> oldFieldsEdges =
        heap.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pListSeg));
    Set<SMGEdgePointsTo> oldPtEdges = heap.getPointerToObject(pListSeg);

    heap.removeHeapObjectAndEdges(pListSeg);

    SMGDoublyLinkedList newDll = new SMGDoublyLinkedList(pListSeg.getSize(), pListSeg.getHfo(),
        pListSeg.getNfo(), pListSeg.getPfo(),
        pListSeg.getMinimumLength() > 0 ? pListSeg.getMinimumLength() - 1 : 0, 0);

    heap.addHeapObject(newDll);
    heap.setValidity(newDll, true);

    /*Check if pointer was already created due to All target Specifier*/
    SMGSymbolicValue newPointerToNewRegion = getAddress(newConcreteRegion, hfo);

    if (!newPointerToNewRegion.isUnknown()) {
      heap.removePointsToEdge(newPointerToNewRegion.getAsInt());
      heap.mergeValues(oldPointerToDll, newPointerToNewRegion.getAsInt());
    }

    SMGEdgePointsTo newPtEdgeToNewRegionFromOutsideSMG =
        new SMGEdgePointsTo(oldPointerToDll, newConcreteRegion, hfo);
    SMGEdgeHasValue newFieldFromNewRegionToOutsideSMG =
        new SMGEdgeHasValue(oldDllFieldToOldRegion.getType(), offsetPointingToRegion,
            newConcreteRegion, oldDllFieldToOldRegion.getValue());

    int newPointerToDll = SMGValueFactory.getNewValue();

    CType typeOfPointerToDll;

    Set<SMGEdgeHasValue> fieldsContainingOldPointerToDll =
        heap.getHVEdges(SMGEdgeHasValueFilter.valueFilter(oldPointerToDll));

    if (fieldsContainingOldPointerToDll.isEmpty()) {
      typeOfPointerToDll = CPointerType.POINTER_TO_VOID;
    } else {
      typeOfPointerToDll = fieldsContainingOldPointerToDll.iterator().next().getType();
    }

    SMGEdgeHasValue newFieldFromNewRegionToDll = new SMGEdgeHasValue(typeOfPointerToDll,
        offsetPointingToDll, newConcreteRegion, newPointerToDll);
    SMGEdgePointsTo newPtEToDll = new SMGEdgePointsTo(newPointerToDll, newDll, hfo, tg);

    SMGEdgeHasValue newFieldFromDllToNewRegion = new SMGEdgeHasValue(
        oldDllFieldToOldRegion.getType(), offsetPointingToRegion, newDll, oldPointerToDll);

    for (SMGEdgeHasValue hve : oldFieldsEdges) {
      addHasValueEdge(hve.getObject(), hve.getOffset(),
          new SMGEdgeHasValue(hve.getType(), hve.getOffset(), newDll, hve.getValue()));
    }

    for (SMGEdgePointsTo ptE : oldPtEdges) {
      heap.addPointsToEdge(
          new SMGEdgePointsTo(ptE.getValue(), newDll, ptE.getOffset(), ptE.getTargetSpecifier()));
    }

    heap.addPointsToEdge(newPtEdgeToNewRegionFromOutsideSMG);
    heap.addHasValueEdge(newFieldFromNewRegionToOutsideSMG);
    sourcesOfHve.registerHasValueEdge(newFieldFromNewRegionToOutsideSMG);

    heap.addValue(newPointerToDll);
    heap.addHasValueEdge(newFieldFromNewRegionToDll);
    sourcesOfHve.registerHasValueEdge(newFieldFromNewRegionToDll);
    heap.addPointsToEdge(newPtEToDll);

    heap.addHasValueEdge(newFieldFromDllToNewRegion);


    return SMGAddressValueAndState.of(this,
        SMGKnownAddVal.valueOf(oldPointerToDll, newConcreteRegion, hfo));
  }

  private void copyRestrictedSubSmgToObject(SMGObject pRoot, SMGRegion pNewRegion,
      Set<Integer> pRestriction) {

    Set<SMGObject> toBeChecked = new HashSet<>();
    Map<SMGObject, SMGObject> newObjectMap = new HashMap<>();
    Map<Integer, Integer> newValueMap = new HashMap<>();

    newObjectMap.put(pRoot, pNewRegion);

    Set<SMGEdgeHasValue> hves = heap.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pRoot));

    for (SMGEdgeHasValue hve : hves) {

      if (!pRestriction.contains(hve.getOffset())) {

        int subDlsValue = hve.getValue();
        int newVal = subDlsValue;

        if (heap.isPointer(subDlsValue)) {
          SMGEdgePointsTo reachedObjectSubSmgPTEdge = heap.getPointer(subDlsValue);
          SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();
          int level = reachedObjectSubSmg.getLevel();
          SMGTargetSpecifier tg = reachedObjectSubSmgPTEdge.getTargetSpecifier();

          if ((level != 0 || tg == SMGTargetSpecifier.ALL) && newVal != 0) {

            SMGObject copyOfReachedObject;

            if (!newObjectMap.containsKey(reachedObjectSubSmg)) {
              assert level > 0;
              copyOfReachedObject = reachedObjectSubSmg.copy(reachedObjectSubSmg.getLevel() - 1);
              newObjectMap.put(reachedObjectSubSmg, copyOfReachedObject);
              heap.addHeapObject(copyOfReachedObject);
              heap.setValidity(copyOfReachedObject, heap.isObjectValid(reachedObjectSubSmg));
              toBeChecked.add(reachedObjectSubSmg);
            } else {
              copyOfReachedObject = newObjectMap.get(reachedObjectSubSmg);
            }

            if (newValueMap.containsKey(subDlsValue)) {
              newVal = newValueMap.get(subDlsValue);
            } else {
              newVal = SMGValueFactory.getNewValue();
              heap.addValue(newVal);
              newValueMap.put(subDlsValue, newVal);

              SMGTargetSpecifier newTg;

              if (copyOfReachedObject instanceof SMGRegion) {
                newTg = SMGTargetSpecifier.REGION;
              } else {
                newTg = reachedObjectSubSmgPTEdge.getTargetSpecifier();
              }

              SMGEdgePointsTo newPtEdge = new SMGEdgePointsTo(newVal, copyOfReachedObject,
                  reachedObjectSubSmgPTEdge.getOffset(), newTg);
              heap.addPointsToEdge(newPtEdge);
            }
          }
        }
        addHasValueEdge(hve.getObject(), hve.getOffset(),
            new SMGEdgeHasValue(hve.getType(), hve.getOffset(), pNewRegion, newVal));
      } else {
        MachineModel model = heap.getMachineModel();
        int sizeOfHveInBytes = hve.getSizeInBytes(model);
        /*If a restricted field is 0, and bigger than a pointer, add 0*/
        if (sizeOfHveInBytes > model.getSizeofPtr() && hve.getValue() == 0) {
          int offset = hve.getOffset() + model.getSizeofPtr();
          int sizeInBytes = sizeOfHveInBytes - model.getSizeofPtr();
          SMGEdgeHasValue expandedZeroEdge =
              new SMGEdgeHasValue(sizeInBytes, offset, pNewRegion, 0);
          addHasValueEdge(hve.getObject(), hve.getOffset(), expandedZeroEdge);
        }
      }
    }

    Set<SMGObject> toCheck = new HashSet<>();

    while (!toBeChecked.isEmpty()) {
      toCheck.clear();
      toCheck.addAll(toBeChecked);
      toBeChecked.clear();

      for (SMGObject objToCheck : toCheck) {
        copyObjectAndNodesIntoDestSMG(objToCheck, toBeChecked, newObjectMap, newValueMap);
      }
    }
  }

  private void copyObjectAndNodesIntoDestSMG(SMGObject pObjToCheck,
      Set<SMGObject> pToBeChecked, Map<SMGObject, SMGObject> newObjectMap,
      Map<Integer, Integer> newValueMap) {

    SMGObject newObj = newObjectMap.get(pObjToCheck);

    Set<SMGEdgeHasValue> hves = heap.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObjToCheck));


    for (SMGEdgeHasValue hve : hves) {

      int subDlsValue = hve.getValue();
      int newVal = subDlsValue;

      if (heap.isPointer(subDlsValue)) {
        SMGEdgePointsTo reachedObjectSubSmgPTEdge = heap.getPointer(subDlsValue);
        SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();
        int level = reachedObjectSubSmg.getLevel();
        SMGTargetSpecifier tg = reachedObjectSubSmgPTEdge.getTargetSpecifier();

        if ((level != 0 || tg == SMGTargetSpecifier.ALL) && newVal != 0) {

          SMGObject copyOfReachedObject;

          if (!newObjectMap.containsKey(reachedObjectSubSmg)) {
            assert level > 0;
            copyOfReachedObject = reachedObjectSubSmg.copy(reachedObjectSubSmg.getLevel() - 1);
            newObjectMap.put(reachedObjectSubSmg, copyOfReachedObject);
            heap.addHeapObject(copyOfReachedObject);
            heap.setValidity(copyOfReachedObject, heap.isObjectValid(reachedObjectSubSmg));
            pToBeChecked.add(reachedObjectSubSmg);
          } else {
            copyOfReachedObject = newObjectMap.get(reachedObjectSubSmg);
          }

          if (newValueMap.containsKey(subDlsValue)) {
            newVal = newValueMap.get(subDlsValue);
          } else {
            newVal = SMGValueFactory.getNewValue();
            heap.addValue(newVal);
            newValueMap.put(subDlsValue, newVal);

            SMGTargetSpecifier newTg;

            if (copyOfReachedObject instanceof SMGRegion) {
              newTg = SMGTargetSpecifier.REGION;
            } else {
              newTg = reachedObjectSubSmgPTEdge.getTargetSpecifier();
            }

            SMGEdgePointsTo newPtEdge = new SMGEdgePointsTo(newVal, copyOfReachedObject,
                reachedObjectSubSmgPTEdge.getOffset(),
                newTg);
            heap.addPointsToEdge(newPtEdge);
          }
        }
      }

      addHasValueEdge(hve.getObject(), hve.getOffset(),
          new SMGEdgeHasValue(hve.getType(), hve.getOffset(), newObj, newVal));
    }
  }

  private void removeRestrictedSubSmg(SMGObject pRoot, Set<Integer> pRestriction) {

    Set<SMGObject> toBeChecked = new HashSet<>();
    Set<SMGObject> reached = new HashSet<>();

    reached.add(pRoot);

    Set<SMGEdgeHasValue> hves = heap.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pRoot));

    for (SMGEdgeHasValue hve : hves) {

      if (!pRestriction.contains(hve.getOffset())) {

        int subDlsValue = hve.getValue();

        if (heap.isPointer(subDlsValue)) {
          SMGEdgePointsTo reachedObjectSubSmgPTEdge = heap.getPointer(subDlsValue);
          SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();
          int level = reachedObjectSubSmg.getLevel();
          SMGTargetSpecifier tg = reachedObjectSubSmgPTEdge.getTargetSpecifier();

          if ((!reached.contains(reachedObjectSubSmg))
              && (level != 0 || tg == SMGTargetSpecifier.ALL) && subDlsValue != 0) {
            assert level > 0;
            reached.add(reachedObjectSubSmg);
            heap.setValidity(reachedObjectSubSmg, false);
            toBeChecked.add(reachedObjectSubSmg);
          }
        }
      }
    }

    Set<SMGObject> toCheck = new HashSet<>();

    while (!toBeChecked.isEmpty()) {
      toCheck.clear();
      toCheck.addAll(toBeChecked);
      toBeChecked.clear();

      for (SMGObject objToCheck : toCheck) {
        removeRestrictedSubSmg(objToCheck, toBeChecked, reached);
      }
    }

    for (SMGObject toBeRemoved : reached) {
      if (toBeRemoved != pRoot) {
        heap.removeHeapObjectAndEdges(toBeRemoved);
      }
    }
  }

  private void removeRestrictedSubSmg(SMGObject pObjToCheck,
      Set<SMGObject> pToBeChecked, Set<SMGObject> reached) {

    Set<SMGEdgeHasValue> hves = heap.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObjToCheck));

    for (SMGEdgeHasValue hve : hves) {

      int subDlsValue = hve.getValue();

      if (heap.isPointer(subDlsValue)) {
        SMGEdgePointsTo reachedObjectSubSmgPTEdge = heap.getPointer(subDlsValue);
        SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();
        int level = reachedObjectSubSmg.getLevel();
        SMGTargetSpecifier tg = reachedObjectSubSmgPTEdge.getTargetSpecifier();

        if ((!reached.contains(reachedObjectSubSmg))
            && (level != 0 || tg == SMGTargetSpecifier.ALL) && subDlsValue != 0) {
          assert level > 0;
          reached.add(reachedObjectSubSmg);
          heap.setValidity(reachedObjectSubSmg, false);
          pToBeChecked.add(reachedObjectSubSmg);
        }
      }
    }
  }

  /**
   * Checks, if a symbolic value has a points-to edge.
   * Usually, this means the symbolic value represents an address,
   * but it may also be the end of a heap abstraction after materializing the
   * points-to edge.
   *
   *
   * Constant.
   *
   * @param pValue A value for which to return the Points-To edge
   * @return True, if the smg contains a {@link SMGEdgePointsTo} edge
   * with pValue as source, false otherwise.
   *
   */
  public boolean isPointer(Integer pValue) {

    return heap.isPointer(pValue);
  }

  /**
   * Checks, if a symbolic value has a points-to edge.
   * Usually, this means the symbolic value represents an address,
   * but it may also be the end of a heap abstraction after materializing the
   * points-to edge.
   *
   *
   * Constant.
   *
   * @param pValue A value for which to return the Points-To edge
   * @return True, if the smg contains a {@link SMGEdgePointsTo} edge
   * with pValue as source, false otherwise.
   *
   */
  public boolean isPointer(SMGKnownSymValue pValue) {

    return heap.isPointer(pValue.getAsInt());
  }

  /**
   * Read Value in field (object, type) of an Object. If a Value cannot be determined,
   * but the given object and field is a valid place to read a value, a new value will be
   * generated and returned. (Does not create a new State but modifies this state).
   *
   * @param pAddress read the value from this address
   * @param pType type of field
   * @return the value and the state (may be the given state)
   */
  public SMGValueAndState forceReadValue(SMGAddress pAddress, CType pType)
      throws SMGInconsistentException {
    SMGValueAndState valueAndState = readValue(pAddress, pType);

    // Do not create a value if the read is invalid.
    if (valueAndState.getObject().isUnknown() && valueAndState.getSmgState().invalidRead == false) {
      SMGStateEdgePair stateAndNewEdge;
      if (valueAndState.getSmgState().isObjectExternallyAllocated(pAddress.getObject()) && pType.getCanonicalType()
          instanceof CPointerType) {
        SMGAddressValue new_address = valueAndState.getSmgState().addExternalAllocation(genRecursiveLabel(pAddress.getObject().getLabel()));
        stateAndNewEdge = writeValue(pAddress, pType, new_address);
      } else {
        SMGKnownSymValue newValue = SMGValueFactory.getNewSymbolicValue();
        stateAndNewEdge = writeValue(pAddress, pType, newValue);
      }
      return SMGValueAndState.of(stateAndNewEdge.getState(),
          SMGKnownSymValue.valueOf(stateAndNewEdge.getNewEdge().getValue()));
    } else {
      return valueAndState;
    }
  }

  private String genRecursiveLabel(String pLabel) {
    Matcher result = externalAllocationRecursivePattern.matcher(pLabel);
    if (result.matches()) {
      String in = result.group(2);
      Integer level = Integer.parseInt(in) + 1;
      return result.replaceFirst("$1" + level + "$3");
    } else {
      return "r_1_" + pLabel;
    }
  }

  /**
   * Read Value in field (object, type) of an Object.
   *
   * @param pAddress read the value from this address
   * @param pType type of field
   * @return the value and the state (may be the given state)
   */
  public SMGValueAndState readValue(SMGAddress pAddress, CType pType)
      throws SMGInconsistentException {

    if (pAddress.isUnknown()) {
      return SMGValueAndState.of(setInvalidRead());
    }

    SMGObject object = pAddress.getObject();
    SMGExplicitValue offset = pAddress.getOffset();

    if (!heap.isObjectValid(object) && !heap.isObjectExternallyAllocated(object)) {
      SMGState newState = setInvalidRead();
      return SMGValueAndState.of(newState);
    }

    SMGEdgeHasValue edge = new SMGEdgeHasValue(pType, offset.getAsInt(), object, 0);

    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();
    filter.filterByObject(object);
    filter.filterAtOffset(offset.getAsInt());
    Set<SMGEdgeHasValue> edges = heap.getHVEdges(filter);

    for (SMGEdgeHasValue object_edge : edges) {
      if (edge.isCompatibleFieldOnSameObject(object_edge, heap.getMachineModel())) {
        performConsistencyCheck(SMGRuntimeCheck.HALF);
        SMGSymbolicValue value = SMGKnownSymValue.valueOf(object_edge.getValue());
        value = sourcesOfHve.createReadValueSource(pAddress, value);
        return SMGValueAndState.of(this, value);
      }
    }

    if (heap.isCoveredByNullifiedBlocks(
        edge)) {
      SMGSymbolicValue value = SMGKnownSymValue.ZERO;

      if(sourcesOfHve.getClass().equals(SMGHveSources.class)) {

        Set<SMGKnownAddress> sources = new HashSet<>();

        SMGEdgeHasValueFilter objectFilter = SMGEdgeHasValueFilter.objectFilter(object).filterHavingValue(heap.getNullValue());

        for (SMGEdgeHasValue zeroEdge : getHVEdges(objectFilter)) {
          if (zeroEdge.overlapsWith(edge, getMachineModel())) {
            sources.add(SMGKnownAddress.valueOf(zeroEdge.getObject(), zeroEdge.getOffset()));
          }
        }

        value = sourcesOfHve.createReadValueSource(pAddress, value, sources, true);
      }

      return SMGValueAndState.of(this, value);
    }

    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return SMGValueAndState.of(this);
  }

  /**
   * Indicates to the smg, that the property invalid read is satisfied.
   * @return Returns a new smg, that is a copy of this smg and is a target for the
   * property invalid read.
   */
  public SMGState setInvalidRead() {
    return new SMGState(this, Property.INVALID_READ);
  }

  public SMGState assignStruct(SMGAddress pLStructAddress,
      SMGKnownExpValue pRValueTypeSize, SMGKnownAddVal pRStructAddress)
      throws SMGInconsistentException {

    SMGObject source = pRStructAddress.getObject();
    SMGKnownExpValue structOffset = pRStructAddress.getOffset();

    SMGKnownExpValue structSize = (SMGKnownExpValue) structOffset.add(pRValueTypeSize);
    return copy(source, pLStructAddress.getObject(),
        structOffset, structSize, (SMGKnownExpValue) pLStructAddress.getOffset());
  }

  public SMGAddressValueAndState memcpy(SMGAddressValue targetStr1Address,
      SMGAddressValue sourceStr2Address, SMGExplicitValue sizeValue) throws SMGInconsistentException {

    SMGState resultState = this;

    /*If target is unknown, clear all values, because we don't know where memcpy was used,
     *  and mark invalid write.
     * If source is unknown, just clear all values of target, and mark invalid read.
     * If size is unknown, clear all values of target, and mark invalid write and read.*/
    if (targetStr1Address.isUnknown() || sourceStr2Address.isUnknown()
        || sizeValue.isUnknown()) {

      if (sizeValue.isUnknown()) {
        resultState = resultState.setInvalidWrite();
        resultState = resultState.setInvalidRead();
      } else if (targetStr1Address.isUnknown()) {
        resultState = resultState.setInvalidWrite();
      } else {
        resultState = resultState.setInvalidRead();
      }

      if (targetStr1Address.isUnknown()) {
        resultState.unknownWrite();
        return SMGAddressValueAndState.of(resultState);
      } else {
        //TODO More precise clear of values
        resultState.writeUnknownValueInUnknownField(targetStr1Address.getAddress().getObject());
        return SMGAddressValueAndState.of(resultState);
      }
    }

    sourcesOfHve.registerDereference(targetStr1Address);
    sourcesOfHve.registerDereference(sourceStr2Address);
    sourcesOfHve.registerDereference(sizeValue);
    sourcesOfHve.registerTargetWrite(targetStr1Address);

    SMGObject source = sourceStr2Address.getObject();
    SMGObject target = targetStr1Address.getObject();

    SMGKnownExpValue sourceRangeOffset = (SMGKnownExpValue) sourceStr2Address.getOffset();
    SMGKnownExpValue sourceRangeSize = (SMGKnownExpValue) sizeValue.add(sourceRangeOffset);
    SMGKnownExpValue targetRangeOffset = (SMGKnownExpValue) targetStr1Address.getOffset();

    resultState = copy(source, target, sourceRangeOffset, sourceRangeSize, targetRangeOffset);

    return SMGAddressValueAndState.of(resultState, targetStr1Address);
  }

  public SMGAddressValueAndState memset(SMGAddressValue bufferAddress, SMGExplicitValue countValue,
      SMGSymbolicValue ch,
      SMGExplicitValue expValue)
      throws CPATransferException {

    if (bufferAddress.isUnknown()) {
      unknownWrite();
      return SMGAddressValueAndState.of(setInvalidWrite());
    }

    if (countValue.isUnknown()) {
      writeUnknownValueInUnknownField(bufferAddress.getObject());
      return SMGAddressValueAndState.of(setInvalidWrite());
    }

    sourcesOfHve.registerDereference(countValue);
    sourcesOfHve.registerDereference(expValue);
    sourcesOfHve.registerTargetWrite(bufferAddress);

    long count = countValue.getAsLong();

    if (ch.isUnknown()) {
      // If the symbolic value is not known create a new one.
      ch = SMGValueFactory.getNewSymbolicValue();
    }

    SMGObject bufferMemory = bufferAddress.getObject();

    int offset = bufferAddress.getOffset().getAsInt();
    int signedCharSize = heap.getMachineModel().getSizeof(CNumericTypes.SIGNED_CHAR);

    boolean doesNotFitIntoObject = offset < 0
        || (offset + (count * signedCharSize)) > bufferMemory.getSize();

    if (doesNotFitIntoObject) {
      writeUnknownValueInUnknownField(bufferAddress.getObject());
      return SMGAddressValueAndState.of(setInvalidWrite());
    }

    SMGState resultState = this;

    if (ch.equals(SMGKnownSymValue.ZERO)) {
      // Create one large edge
      SMGStateEdgePair resultStateAndEdge = writeValue(bufferAddress.getAddress(),
          AnonymousTypes.createTypeWithLength(count * signedCharSize), ch);
      resultState = resultStateAndEdge.getState();
      sourcesOfHve.registerMemsetCount(countValue, resultStateAndEdge);

    } else {
      // We need to create many edges, one for each character written
      // memset() copies ch into the first count characters of buffer
      for (int c = 0; c < count; c++) {
        SMGExplicitValue byteShift = SMGKnownExpValue.valueOf(c * signedCharSize);
        SMGAddress bufferAddressTarget =
            SMGAddress.valueOf(bufferMemory, bufferAddress.getOffset().add(byteShift));

        SMGStateEdgePair resultStateAndEdge =
            resultState.writeValue(bufferAddressTarget, CNumericTypes.SIGNED_CHAR, ch);
        resultState = resultStateAndEdge.getState();
        sourcesOfHve.registerMemsetCount(countValue, resultStateAndEdge);
      }

      if (!expValue.isUnknown()) {
        resultState.putExplicit((SMGKnownSymValue) ch, (SMGKnownExpValue) expValue);
      }
    }

    return SMGAddressValueAndState.of(resultState, bufferAddress);
  }

  /**
   * Write a value into a field (offset, type) of an Object.
   * Additionally, this method writes a points-to edge into the
   * SMG, if the given symbolic value points to an address, and
   *
   *
   * @param pAddress writes the value to this address
   * @param pType type of field written into.
   * @param pValue value to be written into field.
   * @return the edge and the new state (may be this state)
   */
  public SMGStateEdgePair writeValue(SMGAddress pAddress,
      CType pType, SMGSymbolicValue pValue) throws SMGInconsistentException {

    SMGKnownSymValue value;

    // If the value is not yet known by the SMG
    // create a unconstrained new symbolic value
    if (pValue.isUnknown()) {
      value = SMGValueFactory.getNewSymbolicValue();
    } else {
      value = (SMGKnownSymValue) pValue;
    }

    // If the value represents an address, and the address is known,
    // add the necessary points-To edge.
    if (pValue instanceof SMGAddressValue) {
      if (!containsValue(value.getAsInt())) {
        SMGAddress address = ((SMGAddressValue) pValue).getAddress();

        if (!address.isUnknown()) {
          addPointsToEdge(
              address.getObject(),
              address.getOffset().getAsInt(),
              value.getAsInt());
        }
      }
    }

    SMGStateEdgePair result = writeValue(pAddress, pType, value);

    sourcesOfHve.registerWriteValueSource(pAddress.getAsKnownAddress(), pValue, result);
    return result;
  }

  /**
   * Writes a unknown value to an unknown field consisting of an unknown offset and
   * the given object. Always leads to invalid read. The result is that every field
   * of the target object should return an unknown value.
   *
   * @param target this object is part of the unknown field that should be written to the smg.
   */
  public void writeUnknownValueInUnknownField(SMGObject target) {
    Set<SMGEdgeHasValue> hves = heap.getHVEdges(SMGEdgeHasValueFilter.objectFilter(target));
    hves.forEach((SMGEdgeHasValue hve) -> {
      heap.removeHasValueEdge(hve);
    });
  }

  /**
   * Write an unknown value to an unknown field.
   * Essentially this overwrites the complete smg and should
   * cause an invalid write.
   */
  public void unknownWrite() {
    heap.unknownWrite();
  }

  /**
   * Adds a points-to edge to the smg.
   *
   * Keeps Consistency: No. The Consistency is violated, if two different
   * points to edges point to the same address.
   *
   * This will override a points-to-edge if the given value
   * already is a pointer.
   *
   * @param pObject the target object of the pointer.
   * @param pOffset the target offset of the pointer.
   * @param pValue the value of the points to edge.
   */
  public void addPointsToEdge(SMGObject pObject, int pOffset, int pValue) {

    // If the value is not known by the SMG, add it.
    if (!containsValue(pValue)) {
      heap.addValue(pValue);
    }

    SMGEdgePointsTo pointsToEdge = new SMGEdgePointsTo(pValue, pObject, pOffset);
    heap.addPointsToEdge(pointsToEdge);

  }

  private SMGStateEdgePair writeValue(SMGAddress pAddress, CType pType, SMGKnownSymValue pValue)
      throws SMGInconsistentException {
    // vgl Algorithm 1 Byte-Precise Verification of Low-Level List Manipulation FIT-TR-2012-04

    if(pAddress.isUnknown()) {

      if(pAddress.isTargetObjectUnknown()) {
        unknownWrite();
      } else {
        writeUnknownValueInUnknownField(pAddress.getObject());
      }

      return new SMGStateEdgePair(setInvalidWrite());
    }

    sourcesOfHve.registerTargetWrite(pAddress);

    SMGObject object = pAddress.getObject();
    SMGExplicitValue offset = pAddress.getOffset();

    if (!heap.isObjectValid(object) && !heap.isObjectExternallyAllocated(object)) {
      //Attempt to write to invalid object
      SMGState newState = setInvalidWrite();
      return new SMGStateEdgePair(newState);
    }

    SMGEdgeHasValue new_edge = new SMGEdgeHasValue(pType, offset.getAsInt(), object, pValue.getAsInt());

    // Check if the edge is  not present already
    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(object);

    Set<SMGEdgeHasValue> edges = heap.getHVEdges(filter);
    if (edges.contains(new_edge)) {
      performConsistencyCheck(SMGRuntimeCheck.HALF);
      return new SMGStateEdgePair(this, new_edge);
    }

    HashSet<SMGEdgeHasValue> overlappingZeroEdges = new HashSet<>();

    /* We need to remove all non-zero overlapping edges
     * and remember all overlapping zero edges to shrink them later
     */
    for (SMGEdgeHasValue hv : edges) {

      boolean hvEdgeOverlaps = new_edge.overlapsWith(hv, heap.getMachineModel());
      boolean hvEdgeIsZero = hv.getValue() == heap.getNullValue();

      if (hvEdgeOverlaps) {
        if (hvEdgeIsZero) {
          overlappingZeroEdges.add(hv);
        } else {
          heap.removeHasValueEdge(hv);
        }
      }
    }

    shrinkOverlappingZeroEdges(new_edge, overlappingZeroEdges);

    if (offset.getAsInt() < 0 || object.getSize() < (offset.getAsInt()
        + new_edge.getSizeInBytes(heap.getMachineModel()))) {

      return new SMGStateEdgePair(this, new_edge);
    }

    // If the value is not in the SMG, we need to add it
    if (!heap.getValues().contains(pValue.getAsInt())) {
      heap.addValue(pValue.getAsInt());
    }

    heap.addHasValueEdge(new_edge);
    performConsistencyCheck(SMGRuntimeCheck.HALF);

    return new SMGStateEdgePair(this, new_edge);
  }

  /**
   * This class represents a pair of a {@link SMGState} state and a {@link SMGEdgeHasValue} edge.
   */
  public static class SMGStateEdgePair {

    private final SMGState smgState;
    private final SMGEdgeHasValue edge;

    private SMGStateEdgePair(SMGState pState, SMGEdgeHasValue pEdge) {
      smgState = pState;
      edge = pEdge;
    }

    private SMGStateEdgePair(SMGState pNewState) {
      smgState = pNewState;
      edge = null;
    }

    public boolean smgStateHasNewEdge() {
      return edge != null;
    }

    public SMGEdgeHasValue getNewEdge() {
      return edge;
    }

    public SMGState getState() {
      return smgState;
    }

    @Override
    public String toString() {
      return "SMGStateEdgePair [smgState=" + smgState + ", edge=" + edge + "]";
    }
  }

  public boolean isObjectExternallyAllocated(SMGObject pObject) {
    return heap.isObjectExternallyAllocated(pObject);
  }

  /**
   * Indicates if the smg represents the end of a block. Configured by {@link BlockOperator}.
   *
   * @return true iff this smg represents the end of a block.
   */
  public boolean isBlockEnded() {
    return blockEnded;
  }

  private void shrinkOverlappingZeroEdges(SMGEdgeHasValue pNew_edge,
      Set<SMGEdgeHasValue> pOverlappingZeroEdges) {

    SMGObject object = pNew_edge.getObject();
    int offset = pNew_edge.getOffset();

    MachineModel maModel = heap.getMachineModel();
    int sizeOfType = pNew_edge.getSizeInBytes(maModel);

    // Shrink overlapping zero edges
    for (SMGEdgeHasValue zeroEdge : pOverlappingZeroEdges) {
      heap.removeHasValueEdge(zeroEdge);

      int zeroEdgeOffset = zeroEdge.getOffset();

      int offset2 = offset + sizeOfType;
      int zeroEdgeOffset2 = zeroEdgeOffset + zeroEdge.getSizeInBytes(maModel);

      if (zeroEdgeOffset < offset) {
        SMGEdgeHasValue newZeroEdge =
            new SMGEdgeHasValue(offset - zeroEdgeOffset, zeroEdgeOffset, object, 0);
        addHasValueEdge(zeroEdge.getObject(), zeroEdge.getOffset(), newZeroEdge);
      }

      if (offset2 < zeroEdgeOffset2) {
        SMGEdgeHasValue newZeroEdge =
            new SMGEdgeHasValue(zeroEdgeOffset2 - offset2, offset2, object, 0);
        addHasValueEdge(zeroEdge.getObject(), zeroEdge.getOffset(), newZeroEdge);
      }
    }
  }

  /**
   * Marks that an invalid write operation was performed on this smgState.
   *
   */
  public SMGState setInvalidWrite() {
    return new SMGState(this, Property.INVALID_WRITE);
  }

  /**
   * Computes the join of this abstract State and the reached abstract State.
   *
   * @param reachedState the abstract state this state will be joined to.
   * @return the join of the two states.
   */
  @Override
  public SMGState join(SMGState reachedState) {

    SMGJoin join;

    try {
      join = new SMGJoin(heap, reachedState.heap, this, reachedState);
    } catch (SMGInconsistentException e) {
      logger.log(Level.WARNING, () -> {
        return "Coarse join due to exception:" + e.toString();
      });

      return reachedState.createEmptyState();
    }

    if (!join.isDefined()) {
      return reachedState.createEmptyState();
    }

    return createStateFromJoin(join, reachedState, true);
  }

  private SMGState createEmptyState() {
    SMGState emptyState = this.createSuccessor();
    emptyState.clearValues();
    emptyState.clearObjects();
    return emptyState;
  }

  /**
   * Computes the merge of this abstract State and the given abstract State based on the
   * join of both states, or returns the reached state, if no join is defined.
   *
   * @param pState the abstract state this state will be joined to.
   * @param pPrecision determines the precision of the merge
   * @return the join of the two states or reached state.
   * @throws SMGInconsistentException inconsistent smgs calculated while merging both smgs
   */
  public SMGState mergeSMG(SMGState pState, SMGPrecision pPrecision)
      throws SMGInconsistentException {
    // Not necessary if merge_SEP and stop_SEP is used.

    SMGJoin join = new SMGJoin(this.heap, pState.heap, this, pState);

    /* If JoinStatus is Equal or right entail, we stop anyway and
       * don't need to merge.*/

    switch (join.getStatus()) {
      case EQUAL:
      case RIGHT_ENTAIL:
        return pState;
      default:
    }

    if (join.isDefined()) {

      SMGState result = createStateFromJoin(join, pState, pPrecision.joinIntegerWhenMerging());

      SMGDebugExporter.dumpPlot("merge_" + id + "_" + pState.getId() + "_" + result.getId(),
          result);

      return result;
    } else {
      return pState;
    }
  }

  private SMGState createStateFromJoin(SMGJoin join, SMGState pState, boolean pJoinExplicitValues) {

    CLangSMG destHeap = join.getJointSMG();

    BiMap<SMGKnownSymValue, SMGKnownExpValue> mergedExplicitValues =
        mergeExplicitValues(explicitValues, pState.explicitValues, pJoinExplicitValues,
            join.getMapping1(), join.getMapping2());

    SMGState result = new SMGState(pState, destHeap, mergedExplicitValues);

    SMGDebugExporter.dumpPlot("Merge-" + id + "-" + pState.getId() + "-" + result.getId(), result);

    return result;
  }

  private BiMap<SMGKnownSymValue, SMGKnownExpValue> mergeExplicitValues(
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues2, boolean pJoinExplicitValues,
      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2) {

    BiMap<SMGKnownSymValue, SMGKnownExpValue> result = HashBiMap.create();

    pExplicitValues2.entrySet().forEach((Entry<SMGKnownSymValue, SMGKnownExpValue> pEntry) -> {
      SMGKnownSymValue originalSymbolicValue = pEntry.getKey();
      SMGKnownExpValue originalExplicitValue = pEntry.getValue();

      if (pMapping2.containsKey(originalSymbolicValue.getAsInt())) {
        result.put(SMGKnownSymValue.valueOf(pMapping2.get(originalSymbolicValue.getAsInt())),
            originalExplicitValue);
      } else {
        result.put(originalSymbolicValue, originalExplicitValue);
      }
    });

    if (pJoinExplicitValues) {

      pMapping1.getValue_mapEntrySet().forEach((Entry<Integer, Integer> pEntry) -> {

        SMGKnownSymValue joinedValue = SMGKnownSymValue.valueOf(pEntry.getValue());

        if (result.containsKey(joinedValue)) {
          SMGKnownExpValue explicitJoinedValue = result.get(joinedValue);
          SMGKnownSymValue originalSymValue = SMGKnownSymValue.valueOf(pEntry.getKey());

          if (!pExplicitValues.containsKey(originalSymValue)
              || !pExplicitValues.get(originalSymValue).equals(explicitJoinedValue)) {
            result.remove(joinedValue);
          }
        }
      });
    }

    return result;
  }

  /**
   * Computes whether this abstract state is covered by the given abstract state.
   * A state is covered by another state, if the set of concrete states
   * a state represents is a subset of the set of concrete states the other
   * state represents.
   *
   *
   * @param reachedState already reached state, that may cover this state already.
   * @return True, if this state is covered by the given state, false otherwise.
   */
  @Override
  public boolean isLessOrEqual(SMGState reachedState) throws SMGInconsistentException {

    if(!getErrorPredicateRelation().isEmpty() || !reachedState.getErrorPredicateRelation().isEmpty()) {
      return false;
    }

    if(preciseIsLessOrEqualThroughJoin) {

      SMGJoin join = new SMGJoin(heap, reachedState.heap, this, reachedState);

      if (join.isDefined()) {
        SMGJoinStatus jss = join.getStatus();
        boolean result = jss == SMGJoinStatus.EQUAL || jss == SMGJoinStatus.RIGHT_ENTAIL;

        /* Only stop if either reached has memleak or this state has no memleak to avoid
         * losing memleak information.
        */
        if (result) {

          SMGState s1 = reachedState.createSuccessor();
          SMGState s2 = this.createSuccessor();

          s1.pruneUnreachable();
          s2.pruneUnreachable();

          logger.log(Level.INFO, this.getId(), " is Less or Equal ", reachedState.getId());

          SMGDebugExporter.dumpPlot("isLessOrEqual_" + id + "_" + reachedState.getId(),
              createStateFromJoin(join, reachedState, true));

          return s1.heap.hasMemoryLeaks() == s2.heap.hasMemoryLeaks();
        } else {
          return false;
        }
      } else {
        return false;
      }
    } else {
      return SMGIsLessOrEqual.isLessOrEqual(reachedState.heap, heap);
    }
  }

  @Override
  public String getCPAName() {
    return "SMGCPA";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    // SMG Properties:
    // has-leaks:boolean

    switch (pProperty) {
      case HAS_LEAKS:
        if (heap.hasMemoryLeaks()) {
          //TODO: Give more information
          issueMemoryLeakMessage();
          return true;
        }
        return false;
      case HAS_INVALID_WRITES:
        if (invalidWrite) {
          //TODO: Give more information
          issueInvalidWriteMessage();
          return true;
        }
        return false;
      case HAS_INVALID_READS:
        if (invalidRead) {
          //TODO: Give more information
          issueInvalidReadMessage();
          return true;
        }
        return false;
      case HAS_INVALID_FREES:
        if (invalidFree) {
          //TODO: Give more information
          issueInvalidFreeMessage();
          return true;
        }
        return false;
      default:
        throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
    }
  }

  /**
   * Add a global object to the SMG
   *
   * Keeps consistency: no
   *
   * With checks: throws {@link IllegalArgumentException} when asked to add
   * an object already present, or an global object with a label identifying
   * different object

   * @param newObject Object to add
   */
  public void addGlobalObject(SMGRegion newObject) {
    heap.addGlobalObject(newObject);
  }

  /**
   * Checks if the variable name is global in the current scope.
   *
   * @param variable the identifier of the variable
   * @return true iff the variable name is assigned an object in this smg in
   *              the current scope and the object is a global object.
   */
  public boolean isGlobal(String variable) {
    return heap.getGlobalObjects().containsValue(heap.getObjectForVisibleVariable(variable));
  }

  /**
   * Checks if the given object is a global object.
   *
   * @param object the object to be checked
   * @return true iff the given object is global.
   */
  public boolean isGlobal(SMGObject object) {
    return heap.getGlobalObjects().containsValue(object);
  }

  /**
   * Checks if the given object is a heap object
   * and not part of the stack.
   *
   * @param object the object to be checked
   * @return true iff the given object is part of the heap
   *                and not the stack.
   */
  public boolean isHeapObject(SMGObject object) {
    return heap.getHeapObjects().contains(object);
  }

  /**
   * Memory allocated in the heap.
   * Has to be freed by the user, otherwise this is a memory-leak.
   *
   * @param pSize the size of the heap allocation in bytes.
   * @param pLabel the label of the heap allocation.
   * @return the address value of the newly allocated heap allocation
   * @throws SMGInconsistentException thrown if the current smg became inconsistent
   *                                  due to adding the current heap allocation. (Only checked if runtimeCheck is enabled and half or finer.)
   */
  public SMGAddressValue addNewHeapAllocation(SMGKnownExpValue pSize, String pLabel)
      throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(pSize.getAsInt(), pLabel);
    int new_value = SMGValueFactory.getNewValue();
    SMGEdgePointsTo points_to = new SMGEdgePointsTo(new_value, new_object, 0);
    heap.addHeapObject(new_object);
    heap.addValue(new_value);
    heap.addPointsToEdge(points_to);

    performConsistencyCheck(SMGRuntimeCheck.HALF);
    sourcesOfHve.registerNewObjectAllocation(pSize, new_object, false);
    return SMGKnownAddVal.valueOf(new_value, new_object, 0);
  }

  /** memory externally allocated could be freed by the user */
  // TODO: refactore
  public SMGAddressValue addExternalAllocation(String pLabel) throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(externalAllocationSize, pLabel);
    int new_value = SMGValueFactory.getNewValue();
    SMGEdgePointsTo points_to = new SMGEdgePointsTo(new_value, new_object, externalAllocationSize/2 );
    heap.addHeapObject(new_object);
    heap.addValue(new_value);
    heap.addPointsToEdge(points_to);

    heap.setExternallyAllocatedFlag(new_object, true);

    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return SMGKnownAddVal.valueOf(new_value, new_object, externalAllocationSize/2);
  }

  /**
   * Memory allocated on the stack.
   * It is automatically freed when leaving the current function scope.
   *
   * @param pSize the size of the stack allocation in bytes.
   * @param pLabel the label of the stack allocation.
   * @return the address value of the newly allocated stack allocation
   * @throws SMGInconsistentException thrown if the current smg became inconsistent
   *                                  due to adding the current stack allocation. (Only checked if runtimeCheck is enabled and half or finer.)
   */
  public SMGAddressValue addNewStackAllocation(SMGKnownExpValue pSize, String pLabel)
      throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(pSize.getAsInt(), pLabel);
    int new_value = SMGValueFactory.getNewValue();
    SMGEdgePointsTo points_to = new SMGEdgePointsTo(new_value, new_object, 0);
    heap.addStackObject(new_object);
    heap.addValue(new_value);
    heap.addPointsToEdge(points_to);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    sourcesOfHve.registerNewObjectAllocation(pSize, new_object, true);
    return SMGKnownAddVal.valueOf(new_value, new_object, 0);
  }

  /**
   * Sets a flag indicating this SMG is a successor over the edge causing a
   * memory leak.
   *
   * Keeps consistency: yes
   */
  public void setMemLeak() {
    heap.setMemoryLeak();
  }

  /**
   * Check if the given symbolic value is part of the smg.
   *
   * @param value check this symbolic value
   * @return true iff the value is in the smg.
   */
  public boolean containsValue(int value) {
    return heap.getValues().contains(value);
  }

  /**
   * Check if the given symbolic value is part of the smg.
   *
   * @param value check this symbolic value
   * @return true iff the value is in the smg.
   */
  public boolean containsValue(SMGSymbolicValue value) {

    if (value.isUnknown()) {
      return containsValue(value.getAsInt());
    } else {
      return false;
    }
  }

  /**
   * Get the symbolic value, that represents the address
   * pointing to the given memory with the given offset, if it exists.
   *
   * @param memory
   *          get address belonging to this memory.
   * @param offset
   *          get address with this offset relative to the beginning of the
   *          memory.
   * @return Address of the given field, or null, if such an address does not
   *         yet exist in the SMG.
   */
  public SMGSymbolicValue getAddress(SMGRegion memory, int offset) {

    SMGEdgePointsToFilter filter =
        SMGEdgePointsToFilter.targetObjectFilter(memory).filterAtTargetOffset(offset);

    Set<SMGEdgePointsTo> edges = heap.getPtEdges(filter);

    if (edges.isEmpty()) {
      return SMGUnknownValue.getInstance();
    } else {
      return SMGKnownSymValue.valueOf(Iterables.getOnlyElement(edges).getValue());
    }
  }

  public SMGSymbolicValue getAddress(SMGRegion memory, SMGKnownExpValue offset) {

    SMGSymbolicValue result = getAddress(memory, offset.getAsInt());

    return sourcesOfHve.createGetAddressSource(result, offset);
  }

  /**
   * Get the symbolic value, that represents the address
   * pointing to the given memory with the given offset, if it exists.
   *
   * @param memory
   *          get address belonging to this memory.
   * @param offset
   *          get address with this offset relative to the beginning of the
   *          memory.
   * @return Address of the given field, or null, if such an address does not
   *         yet exist in the SMG.
   */
  @Nullable
  public Integer getAddress(SMGObject memory, int offset, SMGTargetSpecifier tg) {

    SMGEdgePointsToFilter filter =
        SMGEdgePointsToFilter.targetObjectFilter(memory).filterAtTargetOffset(offset)
            .filterByTargetSpecifier(tg);

    Set<SMGEdgePointsTo> edges = heap.getPtEdges(filter);

    if (edges.isEmpty()) {
      return null;
    } else {
      return Iterables.getOnlyElement(edges).getValue();
    }
  }

  /**
   * This method simulates a free invocation on a pointer. It checks,
   * whether the call is valid, and invalidates the
   * Memory the given pointer points to.
   * The pointer does not need to be part of the SMG,
   * due to for example abstraction.
   *
   *
   * @param pPointer the pointer pointing to the memory that is to be freed.
   * @return returns a possible new State
   */
  public SMGState free(SMGKnownAddVal pPointer)
      throws SMGInconsistentException {

    sourcesOfHve.registerDereference(pPointer);

    if (pPointer.getAsInt() == 0) {
      return this;
    }

    SMGObject smgObject = pPointer.getObject();
    int offset = pPointer.getOffset().getAsInt();

    if (!heap.isHeapObject(smgObject) && !heap.isObjectExternallyAllocated(smgObject)) {
      // You may not free any objects not on the heap.

      return setInvalidFree();
    }

    if (!(offset == 0) && !heap.isObjectExternallyAllocated(smgObject)) {
      // you may not invoke free on any address that you
      // didn't get through a malloc invocation.
      // TODO: externally allocated memory could be freed partially

      return setInvalidFree();
    }

    if (!heap.isObjectValid(smgObject)) {
      // you may not invoke free multiple times on
      // the same object

      return setInvalidFree();
    }

    heap.setValidity(smgObject, false);
    heap.setExternallyAllocatedFlag(smgObject, false);
    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(smgObject);

    List<SMGEdgeHasValue> to_remove = new ArrayList<>();
    for (SMGEdgeHasValue edge : heap.getHVEdges(filter)) {
      to_remove.add(edge);
    }

    for (SMGEdgeHasValue edge : to_remove) {
      heap.removeHasValueEdge(edge);
    }

    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return this;
  }

  /**
   * Drop the stack frame representing the stack of
   * the function with the given name
   */
  public void dropStackFrame() throws SMGInconsistentException {
    heap.dropStackFrame();
    performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  /**
   * Prune the SMG: remove all unreachable objects (heap ones: global and stack
   * are always reachable) and values.
   *
   * Keeps consistency: yes
   */
  public void pruneUnreachable() throws SMGInconsistentException {
    heap.pruneUnreachable();
    //TODO: Explicit values pruning
    performConsistencyCheck(SMGRuntimeCheck.HALF);
  }

  /**
   *
   *  Signals an invalid free call.
   *
   * @return a new smgState that indicates the given property is satisfied.
   */
  public SMGState setInvalidFree() {
    return new SMGState(this, Property.INVALID_FREE);
  }

  /**
   * Getter for obtaining unmodifiable view on Has-Value edges set, filtered by
   * a certain set of criteria.
   * @param pFilter Filtering object
   * @return A set of Has-Value edges for which the criteria in p hold
   */
  public Set<SMGEdgeHasValue> getHVEdges(SMGEdgeHasValueFilter pFilter) {
    return heap.getHVEdges(pFilter);
  }

  /**
   * Getter for obtaining unmodifiable view on Has-Value edges set. Constant.
   * @return Unmodifiable view on Has-Value edges set.
   */
  public Set<SMGEdgeHasValue> getHVEdges() {
    return heap.getHVEdges();
  }

  /**
   * Copys (shallow) the hv-edges of source in the given source range
   * to the target at the given target offset. Note that the source
   * range (pSourceRangeSize - pSourceRangeOffset) has to fit into
   * the target range ( size of pTarget - pTargetRangeOffset).
   * Also, pSourceRangeOffset has to be less or equal to the size
   * of the source Object.
   *
   * This method is mainly used to assign struct variables.
   *
   * @param pSource the SMGObject providing the hv-edges
   * @param pTarget the target of the copy process
   * @param pTargetRangeOffset begin the copy of source at this offset
   * @param pSourceRangeSize the size of the copy of source (not the size of the copy, but the size to the last bit of the source which should be copied).
   * @param pSourceRangeOffset insert the copy of source into target at this offset
   * @throws SMGInconsistentException thrown if the copying leads to an inconsistent SMG.
   */
  private SMGState copy(SMGObject pSource, SMGObject pTarget, SMGKnownExpValue pSourceRangeOffset,
      SMGKnownExpValue pSourceRangeSize, SMGKnownExpValue pTargetRangeOffset) throws SMGInconsistentException {

    SMGState newSMGState = this;

    SMGExplicitValue copyRange = pSourceRangeSize.subtract(pSourceRangeOffset);

    // If copy range is 0, do nothing
    if (copyRange.getAsInt() == 0) {
      return newSMGState;
    }

    SMGExplicitValue targetRangeSize = pTargetRangeOffset.add(copyRange);

    SMGEdgeHasValueFilter filterSource = new SMGEdgeHasValueFilter();
    filterSource.filterByObject(pSource);
    SMGEdgeHasValueFilter filterTarget = new SMGEdgeHasValueFilter();
    filterTarget.filterByObject(pTarget);

    //Remove all Target edges in range
    Set<SMGEdgeHasValue> targetEdges = getHVEdges(filterTarget);

    for (SMGEdgeHasValue edge : targetEdges) {
      if (edge.overlapsWith(pTargetRangeOffset.getAsInt(), targetRangeSize.getAsInt(), heap.getMachineModel())) {
        boolean hvEdgeIsZero = edge.getValue() == heap.getNullValue();
        heap.removeHasValueEdge(edge);
        if (hvEdgeIsZero) {
          SMGObject object = edge.getObject();

          MachineModel maModel = heap.getMachineModel();

          // Shrink overlapping zero edge
          int zeroEdgeOffset = edge.getOffset();

          int zeroEdgeOffset2 = zeroEdgeOffset + edge.getSizeInBytes(maModel);

          if (zeroEdgeOffset < pTargetRangeOffset.getAsInt()) {
            SMGEdgeHasValue newZeroEdge =
                new SMGEdgeHasValue(pTargetRangeOffset.getAsInt() - zeroEdgeOffset, zeroEdgeOffset, object, 0);
            addHasValueEdge(
                edge.getObject(), edge.getOffset(),
                newZeroEdge);
          }

          if (targetRangeSize.getAsInt() < zeroEdgeOffset2) {
            SMGEdgeHasValue newZeroEdge =
                new SMGEdgeHasValue(zeroEdgeOffset2 - targetRangeSize.getAsInt(), targetRangeSize.getAsInt(), object, 0);
            addHasValueEdge(edge.getObject(), edge.getOffset(), newZeroEdge);
          }
        }
      }
    }

    // Copy all Source edges
    Set<SMGEdgeHasValue> sourceEdges = getHVEdges(filterSource);

    // Shift the source edge offset depending on the target range offset
    SMGExplicitValue copyShift = pTargetRangeOffset.subtract(pSourceRangeOffset);

    for (SMGEdgeHasValue edge : sourceEdges) {
      if (edge.overlapsWith(pSourceRangeOffset.getAsInt(), pSourceRangeSize.getAsInt(), heap.getMachineModel())) {
        int offset =
            copyShift.add(SMGKnownExpValue.valueOf(edge.getOffset())).getAsInt();
        SMGKnownSymValue value = SMGKnownSymValue.valueOf(edge.getValue());
        SMGStateEdgePair newSMGStateAndEdge = newSMGState.writeValue(SMGAddress.valueOf(pTarget, offset), edge.getType(), value);
        newSMGState = newSMGStateAndEdge.getState();
        sourcesOfHve.registerHasValueEdgeFromCopy(edge.getObject(), edge.getOffset(),
            newSMGStateAndEdge, copyRange, pTargetRangeOffset);
      }
    }

    performConsistencyCheck(SMGRuntimeCheck.FULL);
    return newSMGState;
  }

  private void addHasValueEdge(SMGObject sourceObject, int sourceOffset, SMGEdgeHasValue pEdge) {
    heap.addHasValueEdge(pEdge);
    sourcesOfHve.registerHasValueEdge(sourceObject, sourceOffset, pEdge);
  }

  /**
   * Signals a dereference of a pointer or array
   *  which could not be resolved.
   */
  public SMGState setUnknownDereference() {
    //TODO: This can actually be an invalid read too
    //      The flagging mechanism should be improved

    return new SMGState(this, Property.INVALID_WRITE);
  }

  /**
   * Getter for obtaining designated NULL object. Constant.
   * @return An object guaranteed to be the only NULL object in the SMG
   */
  public SMGObject getNullObject() {
    return heap.getNullObject();
  }

  /**
   * Merge these two symbolic values, that were identified to be the
   * same value through an assumption. Replaces the second value in the smg with the first.
   *
   * Analysis becomes unsound if the two values are actually different.
   *
   * @param pKnownVal1 first symbolic value to be merged
   * @param pKnownVal2 second symbolic value to be merged
   */
  public void mergeEqualValues(SMGKnownSymValue pKnownVal1, SMGKnownSymValue pKnownVal2) {

    assert !isInNeq(pKnownVal1, pKnownVal2);
    assert !(explicitValues.get(pKnownVal1) != null &&
        explicitValues.get(pKnownVal1).equals(explicitValues.get(pKnownVal2)));

    heap.mergeValues(pKnownVal1.getAsInt(), pKnownVal2.getAsInt());
    SMGKnownExpValue expVal = explicitValues.remove(pKnownVal2);
    if (expVal != null) {
      explicitValues.put(pKnownVal1, expVal);
    }
  }

  /**
   * Identifies two symbolic values as explicitly different.
   *
   * Analysis becomes unsound if the two values are actually different.
   *
   * @param pKnownVal1 first symbolic value
   * @param pKnownVal2 second symbolic value
   */
  public void setNonEqualValues(SMGKnownSymValue pKnownVal1, SMGKnownSymValue pKnownVal2) {
    heap.addNeqRelation(pKnownVal1.getAsInt(), pKnownVal2.getAsInt());
  }

  public boolean isTrackPredicatesEnabled() {
    return trackPredicates;
  }

  public void addPredicateRelation(SMGSymbolicValue pV1, int pCType1,
                                   SMGSymbolicValue pV2, int pCType2,
                                   BinaryOperator pOp, CFAEdge pEdge) {
  if (isTrackPredicatesEnabled() && pEdge instanceof CAssumeEdge) {
    BinaryOperator temp;
    if (((CAssumeEdge) pEdge).getTruthAssumption()) {
      temp = pOp;
    } else {
      temp = pOp.getOppositLogicalOperator();
    }
    logger.log(Level.FINER, "SymValue1 ", pV1 + " ", temp, " SymValue2 ", pV2,
        "; AddPredicate: ", pEdge);
    if (!pV1.isUnknown() && !pV2.isUnknown()) {
      logger.log(Level.FINER,
          "SymValue1 ", pV1.getAsInt(), " ", temp, " SymValue2 ", pV2.getAsInt(),
              "; AddPredicate: ", pEdge);
    }
    heap.addPredicateRelation(pV1, pCType1, pV2, pCType2, pOp, pEdge);
  }
}

  public void addPredicateRelation(SMGSymbolicValue pV1, int pCType1,
                                   SMGExplicitValue pV2, int pCType2,
                                   BinaryOperator pOp, CFAEdge pEdge) {
    if (isTrackPredicatesEnabled() && pEdge instanceof CAssumeEdge) {
      BinaryOperator temp;
      if (((CAssumeEdge) pEdge).getTruthAssumption()) {
        temp = pOp;
      } else {
        temp = pOp.getOppositLogicalOperator();
      }
      logger.log(Level.FINER, "SymValue ", pV1, " ", temp, "; ExplValue ", pV2,
          "; AddPredicate: ", pEdge);
      if (!pV1.isUnknown()) {
        logger.log(Level.FINER, "SymValue ", pV1.getAsInt(), " ", temp, "; ExplValue ", pV2,
            "; AddPredicate: ", pEdge);
      }
      heap.addPredicateRelation(pV1, pCType1, pV2, pCType2, pOp, pEdge);
    }
  }

  public PredRelation getPathPredicateRelation() {
    return heap.getPathPredicateRelation();
  }

  public void addErrorPredicate(SMGSymbolicValue pSymbolicValue, Integer pCType1,
                                SMGExplicitValue pExplicitValue, Integer pCType2,
                                CFAEdge pEdge) {
    if (isTrackPredicatesEnabled()) {
      logger.log(Level.FINER, "Add Error Predicate: SymValue  ",
          pSymbolicValue, " ; ExplValue", " ",
          pExplicitValue, "; on edge: ", pEdge);
      heap.addErrorRelation(pSymbolicValue, pCType1, pExplicitValue, pCType2);
    }
  }

  public PredRelation getErrorPredicateRelation() {
    return heap.getErrorPredicateRelation();
  }

  public void resetErrorRelation() {
    heap.resetErrorRelation();
  }

  /**
   * Assign a symbolic value an explicit value.
   * If the explicit value already exists, merge the symbolic values with
   * the same explicit value. This method will always replace the other symbolic
   * value when merging, so that the given symbolic values is guaranteed to be
   * assigned the given explicit value.
   *
   * @param pKey assign the explicit value this symbolic value.
   * @param pValue assign this explicit value the given symbolic value
   */
  public void putExplicit(SMGKnownSymValue pKey, SMGKnownExpValue pValue) {

    if (explicitValues.inverse().containsKey(pValue)) {
      SMGKnownSymValue symValue = explicitValues.inverse().get(pValue);

      if (pKey.getAsInt() != symValue.getAsInt()) {
        explicitValues.remove(symValue);
        heap.mergeValues(pKey.getAsInt(), symValue.getAsInt());
        explicitValues.put(pKey, pValue);
      }

      sourcesOfHve.registerExplicitValue(pKey, pValue);
      return;
    }

    sourcesOfHve.registerExplicitValue(pKey, pValue);
    explicitValues.put(pKey, pValue);
  }

  /**
   * Remove the explicit value of the given symbolic value.
   *
   * @param pKey remove the explicit value of this symbolic value.
   */
  public void clearExplicit(SMGKnownSymValue pKey) {
    explicitValues.remove(pKey);
  }

  /**
   * Checks, if the given symbolic values has an explicit value assigned.
   *
   * @param value check this symbolic value
   * @return true iff the given symbolic value has an explicit value assigned.
   */
  boolean isExplicit(int value) {
    SMGKnownSymValue key = SMGKnownSymValue.valueOf(value);

    return explicitValues.containsKey(key);
  }

  SMGKnownExpValue getExplicit(int value) {
    SMGKnownSymValue key = SMGKnownSymValue.valueOf(value);

    assert explicitValues.containsKey(key);
    return explicitValues.get(key);
  }

  /**
   * Get the assigned explicit value of the given symbolic value, or an unknown explicit
   * value if no explicit value has been assigned to the given symbolic value.
   *
   * @param pKey get explicit value assigned to this symbolic value.
   * @return an explicit value assigned to the given symbolic value
   */
  public SMGExplicitValue getExplicit(SMGKnownSymValue pKey) {
    if (explicitValues.containsKey(pKey)) {
      return sourcesOfHve.createExpValue(pKey, explicitValues.get(pKey));
    }

    return SMGUnknownValue.getInstance();
  }

  protected static enum Property {
    INVALID_READ,
    INVALID_WRITE,
    INVALID_FREE,
    INVALID_HEAP
  }

  public boolean hasMemoryErrors() {
    return invalidFree || invalidRead || invalidWrite || heap.hasMemoryLeaks();
  }

  /**
   * Check if the given symbolic values are explicitly different.
   * This method only checks if the given values are in the not equal relation.
   *
   * @param pValue1 first symbolic value
   * @param pValue2 second symbolic value
   * @return true iff the given symbolic values are explicitly different.
   */
  public boolean isInNeq(SMGSymbolicValue pValue1, SMGSymbolicValue pValue2) {

    if (pValue1.isUnknown() || pValue2.isUnknown()) {
      return false;
    } else {
      return heap.haveNeqRelation(pValue1.getAsInt(), pValue2.getAsInt());
    }
  }

  IDExpression createIDExpression(SMGObject pObject) {
    return heap.createIDExpression(pObject);
  }

  /**
   * Get an unique object representing a function declaration with an unknown
   * size.
   *
   * @param pDeclaration get the unique object for the function of this declaration.
   * @return returns an object that represents the given function.
   */
  public SMGObject getObjectForFunction(CFunctionDeclaration pDeclaration) {

    /* Treat functions as global objects with unnkown memory size.
     * Only write them into the smg when necessary*/
    String functionQualifiedSMGName = getUniqueFunctionName(pDeclaration);

    return heap.getObjectForVisibleVariable(functionQualifiedSMGName);
  }

  /**
   * Create an unique object representing a function declaration with an unknown
   * size and add it to the smg.
   *
   * @param pDeclaration create the unique object for the function of this declaration.
   * @return returns an object that represents the given function.
   */
  public SMGObject createObjectForFunction(CFunctionDeclaration pDeclaration)
      throws SMGInconsistentException {

    /* Treat functions as global variable with unknown memory size.
     * Only write them into the smg when necessary*/
    String functionQualifiedSMGName = getUniqueFunctionName(pDeclaration);

    assert heap.getObjectForVisibleVariable(functionQualifiedSMGName) == null;

    return addGlobalVariable(SMGKnownExpValue.ZERO, functionQualifiedSMGName);
  }

  private String getUniqueFunctionName(CFunctionDeclaration pDeclaration) {

    StringBuilder functionName = new StringBuilder(pDeclaration.getQualifiedName());

    for (CParameterDeclaration parameterDcl : pDeclaration.getParameters()) {
      functionName.append("_");
      functionName.append(parameterDcl.toASTString().replace("*", "_").replace(" ", "_"));
    }

    return "__" + functionName;
  }

  /**
   * Try to abstract heap segments meaningfully. Execute exactly one step of the heap abstraction.
   *
   *
   * @param pResult The blocks that prevent specified objects to be abstracted.
   * @param pJoinNonSharedNonPointer prevent merging non shared values that are not both pointers when merging
   * @return the abstraction executed in this step represented by the returned abstraction candidate.
   *          If no abstraction was executed, returns a unknown instance of the abstraction candidate.
   * @throws SMGInconsistentException Heap abstraction constructed an inconsistent smg.
   */
  public SMGAbstractionCandidate executeHeapAbstractionOneStep(Set<SMGAbstractionBlock> pResult, boolean pJoinNonSharedNonPointer)
      throws SMGInconsistentException {
    SMGAbstractionManager manager = new SMGAbstractionManager(logger, heap, this, pResult, 2, 2, 2, pJoinNonSharedNonPointer);
    SMGAbstractionCandidate result = manager.executeOneStep();
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return result;
  }

  /**
   * Try to abstract heap segments meaningfully.
   * @throws SMGInconsistentException Heap abstraction constructed an inconsistent smg.
   */
  public void executeHeapAbstraction() throws SMGInconsistentException {
    SMGAbstractionManager manager = new SMGAbstractionManager(logger, heap, this);
    manager.execute();
    performConsistencyCheck(SMGRuntimeCheck.HALF);
  }

  /**
   * Try to abstract heap segments meaningfully.
   *
   * @param blocks blocks heap abstraction on specified objects with specified shape.
   * @param pThreshold configures the minimum threshold of the number of segments in a abstraction
   * @param pJoinNonSharedNonPointer prevent merging non shared values that are not both pointers when merging
   * @return true, iff at least one new heap abstraction was executed.
   * @throws SMGInconsistentException Heap abstraction constructed an inconsistent smg.
   */
  public boolean executeHeapAbstraction(Set<SMGAbstractionBlock> blocks,
      SMGHeapAbstractionThreshold pThreshold, boolean pJoinNonSharedNonPointer)
      throws SMGInconsistentException {

    SMGAbstractionManager manager = new SMGAbstractionManager(logger, heap, this, blocks,
        pThreshold.getEqualThreshold(), pThreshold.getEntailThreshold(),
        pThreshold.getIncombarableThreshold(), pJoinNonSharedNonPointer);
    boolean change = manager.execute();
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return change;
  }

  /**
   * Check if symbolic value1 of this smgState is less or equal to value2
   * of smgsState2.
   *
   * A value is less or equal if every concrete value represented by value1 is also
   * represented by value2.
   *
   * This check may be imprecise, but only insofar that equal symbolic values or
   * symbolic values that entail each other may be identified as incomparable, never the other way around.
   *
   * @param value1 Value of this smgState.
   * @param value2 Value of smgState2.
   * @param smgState2 Another SMG State.
   * @return SMGJoinStatus.RIGHT_ENTAIL iff all values represented by value1 are also represented by value2.
   * SMGJoinStatus.EQUAL iff values represented by value1 and value2 are equal.
   * SMGJoinStatus.INCOMPARABLE otherwise.
   */
  public SMGJoinStatus valueIsLessOrEqual(SMGKnownSymValue value1, SMGKnownSymValue value2,
      SMGState smgState2) {

    if (value1.equals(value2)) {
      return SMGJoinStatus.EQUAL;
    }

    if (smgState2.explicitValues.containsKey(value2)) {
      if (!explicitValues.containsKey(value1)) {
        return SMGJoinStatus.INCOMPARABLE;
      }

      if (!smgState2.explicitValues.get(value2).equals(explicitValues.get(value1))) {
        return SMGJoinStatus.INCOMPARABLE;
      } else {
        // Same explicit values
        return SMGJoinStatus.EQUAL;
      }
    }

    for (Integer neqToVal2 : smgState2.heap.getNeqsForValue(value2.getAsInt())) {
      if (!heap.haveNeqRelation(value1.getAsInt(),
          neqToVal2)) { return SMGJoinStatus.INCOMPARABLE; }
    }

    if (explicitValues.containsKey(value1) || heap.getNeqsForValue(value1.getAsInt())
        .size() > 0) { return SMGJoinStatus.RIGHT_ENTAIL; }

    // Both values represent top
    return SMGJoinStatus.EQUAL;
  }

  SMGEdgePointsTo getPointsToEdge(int pSymbolicValue) {
    return heap.getPointer(pSymbolicValue);
  }

  /**
   * Returns the size of the has-value-edges in the smg.
   *
   * @return Returns the size of the has-value-edges in the smg.
   */
  public int sizeOfHveEdges() {
    return heap.getHVEdges().size();
  }

  /**
   * Calculates the memory paths of this smg.
   *
   * This method goes through the smg and calculates for each reachable
   * field in the smg exactly one memory path.
   * This method makes no guarantees which path to a field is given,
   * except that no path will contain redundant edges due to cycles in the path.
   *
   * A set of memory paths remains stable, meaning the field it points to can be found
   * if it still exists, when adding has-value-edges. It also remains stable when
   * removing has-value-edges iff the value of the edge is not a pointer, or the pointer
   * is not part of the prefix of a memory path in the set.
   *
   *
   * @return A set of memory paths representing all fields of the smg.
   */
  public Set<SMGMemoryPath> getMemoryPaths() {
    return heap.getMemoryPaths();
  }

  /**
   * Tries to remove the has-value-edge from the smg, that is represented
   * by the given memory path. If the given has-value-edge can be found and
   * removed, this method returns the has-value-edge that was just removed.
   *
   * @param pLocation remove the has-value-edge represented by this memory path.
   * @return Returns the removed has-value-edge, or nothing if the removal was not
   * successful.
   */
  public Optional<SMGEdgeHasValue> forget(SMGMemoryPath pLocation) {
    return heap.forget(pLocation);
  }

  /**
   * Creates an interpolant based on this smg and the given abstraction blocks.
   *
   * @param pAbstractionBlocks include these abstraction blocks in the interpolant.
   * @return Interpolant based on this state.
   */
  public SMGStateInterpolant createInterpolant(Set<SMGAbstractionBlock> pAbstractionBlocks) {
    return new SMGStateInterpolant(ImmutableSet.of(this), pAbstractionBlocks);
  }

  /**
   * Creates an interpolant based on this smg.
   *
   * @return Interpolant based on this state.
   */
  public SMGStateInterpolant createInterpolant() {
    return new SMGStateInterpolant(ImmutableSet.of(this));
  }

  /**
   * Remove all values and every edge from the smg, except the 0 edge, so that
   * the smg remains consistent.
   */
  public void clearValues() {
    heap.clearValues();
  }

  /**
   * Remove all objects in this smg except the null object
   * and the return objects of the function frames.
   */
  public void clearObjects() {
    heap.clearObjects();
  }

  /**
   * Calculate the intersection of two smgs. The resulting smg represents the states
   * that both this state and the given state represents.
   *
   * @param pOtherState the state, that this state will be intersected with.
   * @return the intersection of this and the given state, or unknown, if no
   *         intersection can be calculated.
   */
  public SMGIntersectionResult intersectStates(SMGState pOtherState) {
    return SMGIntersectStates.intersect(this, heap, pOtherState, pOtherState.heap, explicitValues,
        pOtherState.explicitValues);
  }

  /**
   * Calculates the memory paths of this smg.
   *
   * This method goes through the smg and calculates for each reachable
   * heap object in the smg exactly one memory path.
   * This method makes no guarantees which path to a heap object is given,
   * except that no path will contain redundant edges due to cycles in the path.
   *
   * A set of memory paths remains stable, meaning the heap object it points to can be found
   * if it still exists, when adding has-value-edges. It also remains stable when
   * removing has-value-edges iff the value of the edge is not a pointer, or the pointer
   * is not part of the prefix of a memory path in the set.
   *
   *
   * @return A map of heap objects as keys and memory paths as values, representing all heap objects of the smg.
   */
  public Map<SMGObject, SMGMemoryPath> getHeapObjectMemoryPaths() {
    return heap.getHeapObjectMemoryPaths();
  }

  /**
   * Remove all has-value-edges not represented by the given set of memory paths.
   *
   * @param pMempaths A set of memory paths representing has-value-edges. All has-value-edges not represented
   *                  by the given path will be erased.
   * @return true iff at least one has-value-edge was removed by this method.
   */
  public boolean forgetNonTrackedHve(Set<SMGMemoryPath> pMempaths) {

    /* First, get all hves that are represented by the given memory paths.*/
    Set<SMGEdgeHasValue> trackkedHves = new HashSet<>(pMempaths.size());
    Set<Integer> trackedValues = new HashSet<>();
    trackedValues.add(0);

    for (SMGMemoryPath path : pMempaths) {
      Optional<SMGEdgeHasValue> hve = heap.getHVEdgeFromMemoryLocation(path);

      if (hve.isPresent()) {
        trackkedHves.add(hve.get());
        trackedValues.add(hve.get().getValue());
      }
    }

    boolean change = false;

    /*Second, remove edges not found. At the moment, we won't remove abstraction edges, due to errors that may occur because of them.*/
    for (SMGEdgeHasValue edge : heap.getHVEdges()) {

      //TODO Robust heap abstraction?
      if (edge.getObject().isAbstract()) {
        trackedValues.add(edge.getValue());
        continue;
      }

      if (!trackkedHves.contains(edge)) {
        heap.removeHasValueEdge(edge);
        change = true;
      }
    }

    /*
     * Lastly, remove all values, that don't occur in the has-value-edges anymore.
     */
    if (change) {
      for (Integer value : ImmutableSet.copyOf(heap.getValues())) {
        if (!trackedValues.contains(value)) {
          heap.removePointsToEdge(value);
          heap.removeValue(value);
          change = true;
        }
      }
    }

    return change;
  }

  /**
   * Remove given has-value-edge.
   *
   * @param pHveEdge remove this edge.
   */
  public void forget(SMGEdgeHasValue pHveEdge) {
    heap.removeHasValueEdge(pHveEdge);
  }

  /**
   * Add given has-value-edge.
   *
   * @param pHveEdge Add this edge.
   */
  public void remember(SMGEdgeHasValue pHveEdge) {
    heap.addHasValueEdge(pHveEdge);
  }

  /**
   * Calculate a map of all stack variable memory locations, and their regions.
   * This method only has a defined behavior if their is no recursion in the program.
   *
   * @return Returns a map with memory locations as keys and region as objects.
   */
  public Map<MemoryLocation, SMGRegion> getStackVariables() {

    Map<MemoryLocation, SMGRegion> result = new HashMap<>();

    for (Entry<String, SMGRegion> variableEntry : heap.getGlobalObjects().entrySet()) {
      String variableName = variableEntry.getKey();
      SMGRegion reg = variableEntry.getValue();
      result.put(MemoryLocation.valueOf(variableName), reg);
    }

    for (CLangStackFrame frame : heap.getStackFrames()) {
      String functionName = frame.getFunctionDeclaration().getName();

      for (Entry<String, SMGRegion> variableEntry : frame.getVariables().entrySet()) {
        String variableName = variableEntry.getKey();
        SMGRegion reg = variableEntry.getValue();
        result.put(MemoryLocation.valueOf(functionName, variableName), reg);
      }
    }

    return result;
  }

  /**
   * Remove all stack variable objects except the variables represented by the given memory locations.
   *
   * This method has only defined behavior, if the program has no recursions.
   *
   * @param pTrackedStackVariables the memory locations represent variables that should not be removed.
   * @return true iff at least one variable was removed.
   */
  public boolean forgetNonTrackedStackVariables(Set<MemoryLocation> pTrackedStackVariables) {

    boolean change = false;

    for (String variable : ImmutableSet.copyOf((heap.getGlobalObjects().keySet()))) {
      MemoryLocation globalVar = MemoryLocation.valueOf(variable);
      if (!pTrackedStackVariables.contains(globalVar)) {
        heap.removeGlobalVariableAndEdges(variable);
        change = true;
      }
    }

    for (CLangStackFrame frame : heap.getStackFrames()) {
      String functionName = frame.getFunctionDeclaration().getName();

      for (String variable : ImmutableSet.copyOf(frame.getVariables().keySet())) {
        MemoryLocation var = MemoryLocation.valueOf(functionName, variable);

        if (!pTrackedStackVariables.contains(var)) {
          heap.removeStackVariableAndEdges(variable, frame);
          change = true;
        }
      }
    }

    return change;
  }

  /**
   * Remove the stack variable represented by the given memory location.
   * @param pMemoryLocation memory location representing a stack variable that should be removed.
   * @return information on the removed variable. If the variable could not be found, this is empty.
   */
  public SMGStateInformation forgetStackVariable(MemoryLocation pMemoryLocation) {
    return heap.forgetStackVariable(pMemoryLocation);
  }

  /**
   * Add a variable to the smg to the location that is represented by the given memory location.
   * The given region, representing the stack variable, is added to the smg.
   * Further edges are added based on the given information.
   *
   * @param pMemoryLocation represents the stack variable to be added to the smg.
   * @param pRegion region to be added to the smg
   * @param pInfo contains edges that contain fields of the variable to be added to the smg
   */
  public void remember(MemoryLocation pMemoryLocation, SMGRegion pRegion,
      SMGStateInformation pInfo) {
    heap.remember(pMemoryLocation, pRegion, pInfo);
  }

  /**
   * Get the current top stack frame representing the current scope.
   *
   * @return the current top stack frame.
   */
  public CLangStackFrame getStackFrame() {
    return heap.getStackFrames().peek();
  }

  /**
   * Filter the given dead variables for refereces that could indirectly cause
   * them to be referenced, even though they are not directly called, or pointer
   * that could lead to memory leaks if they are dropped.
   *
   * @param pDeadVarsMap a map that represent a set of possible dead variables.
   * @return a set of memory locations that contain no live references, causing them to be definitely dead.
   */
  public Set<MemoryLocation> filterLiveReference(Map<MemoryLocation, SMGRegion> pDeadVarsMap) {
    Predicate<? super Entry<MemoryLocation, SMGRegion>> liveReferenceFilter =
        (Entry<MemoryLocation, SMGRegion> pEntry) -> {
          return !hasLiveReference(pEntry.getValue());
        };

    Function<Entry<MemoryLocation, SMGRegion>, MemoryLocation> toKeySet =
        (Entry<MemoryLocation, SMGRegion> pEnntry) -> {
          return pEnntry.getKey();
        };

    return FluentIterable.from(pDeadVarsMap.entrySet()).filter((liveReferenceFilter))
        .transform(toKeySet).toSet();
  }

  private boolean hasLiveReference(SMGRegion pRegion) {

    for (SMGEdgeHasValue hve : getHVEdges(SMGEdgeHasValueFilter.objectFilter(pRegion))) {
      int value = hve.getValue();
      if (isPointer(value)) {
        SMGObject target = getPointsToEdge(value).getObject();
        if (heap.isHeapObject(target)) {
          return true;
        }
      }
    }

    return heap.getPointerToObject(pRegion).size() > 0;
  }

  /**
   * Get all has value edges that are not relevant according to the precision.
   *
   * @param pPrecision relevance is decided according to this precision.
   * @return A set of has value edges that are not relevant according to the precision.
   */
  public Set<SMGEdgeHasValue> getNonRelevantFields(SMGPrecision pPrecision) {
    Set<SMGEdgeHasValue> result = getNonRelevantFieldsOfHeapObjects(pPrecision.getVarClass());
    result.addAll(getNonRelevantStackVariables(pPrecision));
    return result;
  }

  private Set<SMGEdgeHasValue> getNonRelevantStackVariables(
      SMGPrecision pPrecision) {

    Map<MemoryLocation, SMGRegion> stackVariables = getStackVariables();
    Set<SMGRegion> regions = pPrecision.getNonRelevantVariables(stackVariables);

    regions = FluentIterable.from(regions).filter((SMGRegion region) -> {
      return !hasLiveReference(region);
    }).toSet();

    Set<SMGEdgeHasValue> result = new HashSet<>();

    for (SMGEdgeHasValue edge : getHVEdges()) {
      if (regions.contains(edge.getObject())) {
        result.add(edge);
      }
    }

    return result;
  }

  private Set<SMGEdgeHasValue> getNonRelevantFieldsOfHeapObjects(VariableClassification pVarClass) {

    Multimap<CCompositeType, String> relevantFields = pVarClass.getRelevantFields();
    Set<SMGEdgeHasValue> result = new HashSet<>();

    for (SMGObject object : heap.getHeapObjects()) {
      Multimap<Integer, CType> typesOfObject = SMGUtils.getTypesOfHeapObject(object, heap);

      for (Entry<Integer, CType> entry : typesOfObject.entries()) {

        if (entry.getKey() != 0) {
          /*We don't calculate inner fields yet.*/
          continue;
        }

        CType realType = entry.getValue().getCanonicalType();

        if (!(realType instanceof CCompositeType)) {
          continue;
        }

        CCompositeType compositeType = (CCompositeType) realType;

        if (relevantFields.containsKey(compositeType)) {

          Collection<String> relevantMembers = relevantFields.get(compositeType);
          List<CCompositeTypeMemberDeclaration> members =
              new ArrayList<>(compositeType.getMembers());

          if (relevantMembers.size() == members.size()) {
            continue;
          }

          members.removeIf((CCompositeTypeMemberDeclaration memberType) -> {
            return relevantMembers.contains(memberType.getName());
          });

          Map<CCompositeTypeMemberDeclaration, Integer> offsetMap =
              SMGUtils.getOffsetOfFields(compositeType, heap.getMachineModel());

          for (CCompositeTypeMemberDeclaration member : members) {
            if (offsetMap.containsKey(member)) {
              result.addAll(getHVEdges(SMGEdgeHasValueFilter.objectFilter(object)
                  .filterAtOffset(offsetMap.get(member))));
            } else {
              /*Cannot get size of succesor type if offset of this type can't be calculated*/
              break;
            }
          }

        } else {
          result.addAll(getHVEdges(SMGEdgeHasValueFilter.objectFilter(object)));
        }
      }
    }

    return result;
  }

  SMGStateEdgePair writeValue(SMGObject target, int offset, CType type, SMGKnownSymValue value)
      throws SMGInconsistentException {
    return writeValue(SMGAddress.valueOf(target, offset), type, value);
  }

  SMGAddressValueAndStateList getPointerFromValue(int pointer) throws SMGInconsistentException {
    return getPointerFromValue(SMGKnownSymValue.valueOf(pointer));
  }

  SMGAddressValue addNewHeapAllocation(int size, String pLabel) throws SMGInconsistentException {
    return addNewHeapAllocation(SMGKnownExpValue.valueOf(size), pLabel);
  }

  public SMGState createSuccessor() {
    return new SMGState(this);
  }

  public MachineModel getMachineModel() {
    return heap.getMachineModel();
  }

  public SMGState addSourcesToValues() {
    return new SMGState(this, new SMGHveSources());
  }

  public SMGHveSources getSourcesOfHve() {
    return sourcesOfHve;
  }

  public PathPositionMemoryPathDependencys calculatePathDependence(
      Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> pPScope, Set<SMGKnownAddress> pPPathTargets, Set<SMGAbstractionBlock> pBlocks) {

    Map<SMGObject, MemoryLocation> pMemoryLocations = new HashMap<>();
    Map<MemoryLocation, Integer> pLocationOnStack = new HashMap<>();

    SMGDebugExporter.dumpPlot("aa", this);

    for (Entry<String, SMGRegion> variableEntry : heap.getGlobalObjects().entrySet()) {
      String variableName = variableEntry.getKey();
      SMGRegion reg = variableEntry.getValue();
      pMemoryLocations.put(reg, MemoryLocation.valueOf(variableName));
    }

    int locPos = 0;
    for (CLangStackFrame frame : heap.getStackFrames()) {
      String functionName = frame.getFunctionDeclaration().getName();

      for (Entry<String, SMGRegion> variableEntry : frame.getVariables().entrySet()) {
        String variableName = variableEntry.getKey();
        SMGRegion reg = variableEntry.getValue();
        MemoryLocation memloc = MemoryLocation.valueOf(functionName, variableName);
        pMemoryLocations.put(reg, memloc);
        pLocationOnStack.put(memloc, locPos);
      }

      if (frame.getReturnObject() != null) {
        MemoryLocation memloc =
            MemoryLocation.valueOf(functionName, frame.getReturnObject().getLabel());
        pMemoryLocations.put(frame.getReturnObject(), memloc);
        pLocationOnStack.put(memloc, locPos);
      }

      locPos++;
    }

    Multimap<SMGObject, SMGKnownAddress> pHeapObjectToPointerMap = HashMultimap.create();
    Map<SMGKnownAddress, SMGObject> stackPointer = new HashMap<>();

    for (SMGEdgeHasValue field : heap.getHVEdges()) {
      int value = field.getValue();
      if (isPointer(value)) {
        SMGObject obj = heap.getPointer(value).getObject();
        SMGKnownAddress address = SMGKnownAddress.valueOf(field.getObject(), field.getOffset());
        if (heap.isHeapObject(obj)) {
          if (heap.getNullObject() != obj) {
            pHeapObjectToPointerMap.put(obj,
                address);
          }
        } else {
          stackPointer.put(address, obj);
        }
      }
    }

    Map<SMGObject, SMGKnownAddress> pDefaultHeapObjectToPointerConnection = new HashMap<>();

    Map<SMGObject, SMGMemoryPath> heapObjectMemoryPaths = heap.getHeapObjectMemoryPaths();

    for (Entry<SMGObject, SMGMemoryPath> mempath : heapObjectMemoryPaths.entrySet()) {
      Optional<SMGEdgeHasValue> pointer = heap.getHVEdgeFromMemoryLocation(mempath.getValue());

      if(!pointer.isPresent()) {
        throw new AssertionError();
      }

      pDefaultHeapObjectToPointerConnection.put(mempath.getKey(),
          SMGKnownAddress.valueOf(pointer.get().getObject(), pointer.get().getOffset()));
    }

    for (SMGObject obj : heap.getHeapObjects()) {
      if (obj == heap.getNullObject()) {
        continue;
      }

      if (!heapObjectMemoryPaths.containsKey(obj)) {
        SMGDebugExporter.dumpPlot("memeoryReach.dot", this);
      }
    }

    Set<SMGObject> heapObj = new HashSet<>();
    heapObj.addAll(heap.getHeapObjects());
    heapObj.remove(heap.getNullObject());
    Predicate<SMGObject> valid = (SMGObject obj) -> {
      return heap.isObjectValid(obj);
    };
    Set<SMGObject> validObj = FluentIterable.from(heapObj).filter(valid).toSet();
    return new PathPositionMemoryPathDependencys(pPPathTargets, pPScope, heapObj, pHeapObjectToPointerMap, pMemoryLocations, pLocationOnStack, pDefaultHeapObjectToPointerConnection, pBlocks, stackPointer, validObj);
  }
}