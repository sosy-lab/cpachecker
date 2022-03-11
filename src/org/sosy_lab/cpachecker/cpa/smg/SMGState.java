// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import static java.util.Collections.singletonList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMGConsistencyVerifier;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGPredicateRelation;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGType;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter.SMGEdgeHasValueFilterByObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.optional.SMGOptionalObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.SMGSingleLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGIsLessOrEqual;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoin;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPath;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentBiMap;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SMGState implements UnmodifiableSMGState, AbstractQueryableState, Graphable {

  // Properties:
  private static final String HAS_INVALID_FREES = "has-invalid-frees";
  private static final String HAS_INVALID_READS = "has-invalid-reads";
  private static final String HAS_INVALID_WRITES = "has-invalid-writes";
  private static final String HAS_LEAKS = "has-leaks";
  private static final String HAS_HEAP_OBJECTS = "has-heap-objects";

  private static final Pattern externalAllocationRecursivePattern =
      Pattern.compile("^(r_)(\\d+)(_.*)$");

  // use 'id' and 'precessorId' only for debugging or logging, never for important stuff!
  // TODO remove to avoid problems?
  private static final UniqueIdGenerator ID_COUNTER = new UniqueIdGenerator();
  private final int predecessorId;
  private final int id;

  private PersistentBiMap<SMGKnownSymbolicValue, SMGKnownExpValue> explicitValues;

  private final CLangSMG heap;

  private final boolean blockEnded;

  private SMGErrorInfo errorInfo;

  private final LogManager logger;
  private final SMGOptions options;
  private final long sizeOfVoidPointerInBits;

  private void issueMemoryError(String pMessage, boolean pUndefinedBehavior) {
    if (options.isMemoryErrorTarget()) {
      logger.log(Level.FINE, pMessage);
    } else if (pUndefinedBehavior) {
      logger.log(Level.FINE, pMessage);
      logger.log(
          Level.FINE,
          "Non-target undefined behavior detected. The verification result is unreliable.");
    }
  }

  @Override
  public String getErrorDescription() {
    return errorInfo.getErrorDescription();
  }

  @Override
  public SMGState withErrorDescription(String pErrorDescription) {
    return new SMGState(
        logger,
        options,
        heap.copyOf(),
        id,
        explicitValues,
        errorInfo.withErrorMessage(pErrorDescription),
        blockEnded);
  }

  /**
   * Constructor.
   *
   * <p>Keeps consistency: yes
   *
   * @param pLogger A logger to log any messages
   * @param pMachineModel A machine model for the underlying SMGs
   */
  public SMGState(LogManager pLogger, MachineModel pMachineModel, SMGOptions pOptions) {
    this(
        pLogger,
        pOptions,
        new CLangSMG(pMachineModel),
        ID_COUNTER.getFreshId(),
        PersistentBiMap.of());
    explicitValues = explicitValues.putAndCopy(SMGZeroValue.INSTANCE, SMGZeroValue.INSTANCE);
  }

  public SMGState(
      LogManager pLogger,
      SMGOptions pOptions,
      CLangSMG pHeap,
      int pPredId,
      PersistentBiMap<SMGKnownSymbolicValue, SMGKnownExpValue> pMergedExplicitValues) {
    this(pLogger, pOptions, pHeap, pPredId, pMergedExplicitValues, SMGErrorInfo.of(), false);
  }

  /** Copy constructor. */
  private SMGState(
      LogManager pLogger,
      SMGOptions pOptions,
      CLangSMG pHeap,
      int pPredId,
      PersistentBiMap<SMGKnownSymbolicValue, SMGKnownExpValue> pExplicitValues,
      SMGErrorInfo pErrorInfo,
      boolean pBlockEnded) {
    options = pOptions;
    heap = pHeap;
    logger = pLogger;
    predecessorId = pPredId;
    id = ID_COUNTER.getFreshId();
    explicitValues = pExplicitValues;
    errorInfo = pErrorInfo;
    blockEnded = pBlockEnded;
    sizeOfVoidPointerInBits =
        heap.getMachineModel().getSizeofInBits(CPointerType.POINTER_TO_VOID).longValueExact();
  }

  private SMGState(SMGState pOriginalState, Property pProperty) {
    heap = pOriginalState.heap.copyOf();
    logger = pOriginalState.logger;
    options = pOriginalState.options;
    predecessorId = pOriginalState.getId();
    id = ID_COUNTER.getFreshId();
    explicitValues = pOriginalState.explicitValues;
    blockEnded = pOriginalState.blockEnded;
    errorInfo = pOriginalState.errorInfo.withProperty(pProperty);
    sizeOfVoidPointerInBits = pOriginalState.sizeOfVoidPointerInBits;
  }

  @Override
  public SMGState copyOf() {
    return new SMGState(logger, options, heap.copyOf(), id, explicitValues, errorInfo, blockEnded);
  }

  @Override
  public SMGState copyWith(
      CLangSMG pSmg, PersistentBiMap<SMGKnownSymbolicValue, SMGKnownExpValue> pValues) {
    return new SMGState(logger, options, pSmg, id, pValues, errorInfo, blockEnded);
  }

  @Override
  public SMGState copyWithBlockEnd(boolean isBlockEnd) {
    return new SMGState(logger, options, heap.copyOf(), id, explicitValues, errorInfo, isBlockEnd);
  }

  @Override
  public SMGState withViolationsOf(SMGState pOther) {
    if (errorInfo.equals(pOther.errorInfo)) {
      return this;
    }
    SMGState result = new SMGState(logger, options, heap, ID_COUNTER.getFreshId(), explicitValues);
    result.errorInfo = result.errorInfo.mergeWith(pOther.errorInfo);
    return result;
  }

  /**
   * Makes SMGState create a new object and put it into the global namespace
   *
   * <p>Keeps consistency: yes
   *
   * @param pTypeSize Size of the type of the new global variable
   * @param pVarName Name of the global variable
   * @return Newly created object
   * @throws SMGInconsistentException when resulting SMGState is inconsistent and the checks are
   *     enabled
   */
  public SMGRegion addGlobalVariable(long pTypeSize, String pVarName)
      throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(pTypeSize, pVarName);

    heap.addGlobalObject(new_object);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return new_object;
  }

  /**
   * Makes SMGState create a new object and put it into the current stack frame.
   *
   * <p>Keeps consistency: yes
   *
   * @param pTypeSize Size of the type the new local variable
   * @param pVarName Name of the local variable
   * @return Newly created object
   * @throws SMGInconsistentException when resulting SMGState is inconsistent and the checks are
   *     enabled
   */
  public Optional<SMGObject> addLocalVariable(long pTypeSize, String pVarName)
      throws SMGInconsistentException {
    if (heap.getStackFrames().isEmpty()) {
      return Optional.empty();
    }

    SMGRegion new_object = new SMGRegion(pTypeSize, pVarName);

    heap.addStackObject(new_object);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return Optional.of(new_object);
  }

  /**
   * Makes SMGState create a new anonymous object and put it into the current stack frame. Used for
   * string initilizers as function arguments.
   *
   * <p>Keeps consistency: yes
   *
   * @param pTypeSize Size of the type the new local variable
   * @return Newly created object
   * @throws SMGInconsistentException when resulting SMGState is inconsistent and the checks are
   *     enabled
   */
  public Optional<SMGRegion> addAnonymousVariable(long pTypeSize) throws SMGInconsistentException {
    if (heap.getStackFrames().isEmpty()) {
      return Optional.empty();
    }

    SMGRegion new_object = new SMGRegion(pTypeSize);

    heap.addStackObject(new_object);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return Optional.of(new_object);
  }

  /**
   * Makes SMGState create a new object, compares it with the given object, and puts the given
   * object into the current stack frame.
   *
   * <p>Keeps consistency: yes
   *
   * @param pTypeSize Size of the type of the new variable
   * @param pVarName Name of the local variable
   * @param smgObject object of local variable
   * @throws SMGInconsistentException when resulting SMGState is inconsistent and the checks are
   *     enabled
   */
  public void addLocalVariable(long pTypeSize, String pVarName, SMGRegion smgObject)
      throws SMGInconsistentException {
    SMGRegion new_object2 = new SMGRegion(pTypeSize, pVarName);

    assert smgObject.getLabel().equals(new_object2.getLabel());

    // arrays are converted to pointers
    assert smgObject.getSize() == pTypeSize || smgObject.getSize() == heap.getSizeofPtrInBits();

    heap.addStackObject(smgObject);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
  }

  /**
   * Adds a new frame for the function.
   *
   * <p>Keeps consistency: yes
   *
   * @param pFunctionDefinition A function for which to create a new stack frame
   */
  public void addStackFrame(CFunctionDeclaration pFunctionDefinition)
      throws SMGInconsistentException {
    heap.addStackFrame(pFunctionDefinition);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
  }

  /* ********************************************* */
  /* Non-modifying functions: getters and the like */
  /* ********************************************* */

  /**
   * Constant.
   *
   * @return The ID of this SMGState
   */
  @Override
  public final int getId() {
    return id;
  }

  /**
   * Constant. .
   *
   * @return The predecessor state, i.e. one from which this one was copied
   */
  @Override
  public final int getPredecessorId() {
    return predecessorId;
  }

  /**
   * Based on the current setting of runtime check level, it either performs a full consistency
   * check or not. If the check is performed and the state is deemed inconsistent, a {@link
   * SMGInconsistentException} is thrown.
   *
   * <p>Constant.
   *
   * @param pLevel A level of the check request. When e.g. HALF is passed, it means "perform the
   *     check if the setting is HALF or finer.
   */
  public final void performConsistencyCheck(SMGRuntimeCheck pLevel)
      throws SMGInconsistentException {
    if ((pLevel == null || options.getRuntimeCheck().isFinerOrEqualThan(pLevel))) {
      if (!CLangSMGConsistencyVerifier.verifyCLangSMG(logger, heap)) {
        throw new SMGInconsistentException(
            String.format(
                "SMG was found inconsistent during a check on state id %d:%n%s", getId(), this));
      }
    }
  }

  /**
   * Returns a DOT representation of the SMGState.
   *
   * <p>Constant.
   *
   * @param pName A name of the graph.
   * @param pLocation A location in the program.
   * @return String containing a DOT graph corresponding to the SMGState.
   */
  @Override
  public String toDot(String pName, String pLocation) {
    SMGPlotter plotter = new SMGPlotter();
    return plotter.smgAsDot(heap, pName, pLocation, explicitValues);
  }

  /** Return a string representation of the SMGState. */
  @Override
  public String toString() {
    String parent =
        getPredecessorId() == 0
            ? "no parent, initial state"
            : "parent [" + getPredecessorId() + "]";
    return String.format("SMGState [%d] <-- %s: %s", getId(), parent, heap);
  }

  public List<SMGAddressValueAndState> getPointerFromAddress(SMGAddress pAddress)
      throws SMGInconsistentException {
    if (pAddress.isUnknown()) {
      return singletonList(SMGAddressValueAndState.of(this));
    }
    SMGObject target = pAddress.getObject();
    SMGExplicitValue offset = pAddress.getOffset();
    if (target instanceof SMGRegion) {
      SMGValue address = getAddress((SMGRegion) target, offset.getAsLong());
      if (address == null) {
        return singletonList(
            SMGAddressValueAndState.of(
                this, new SMGEdgePointsTo(SMGKnownSymValue.of(), target, offset.getAsLong())));
      }
      return getPointerFromValue(address);
    }
    if (target == SMGNullObject.INSTANCE) {
      // TODO return NULL_POINTER instead of new object?
      return singletonList(
          SMGAddressValueAndState.of(
              this, new SMGEdgePointsTo(SMGZeroValue.INSTANCE, target, offset.getAsLong())));
    }
    if (target.isAbstract()) {
      performConsistencyCheck(SMGRuntimeCheck.HALF);
      SMGKnownSymbolicValue symbolicValue = SMGKnownSymValue.of();
      heap.addValue(symbolicValue);
      return handleMaterilisation(
          new SMGEdgePointsTo(symbolicValue, target, offset.getAsLong(), SMGTargetSpecifier.FIRST),
          ((SMGAbstractObject) target));
    }

    throw new AssertionError("Abstraction " + target + " was not materialised.");
  }

  /**
   * Returns a address leading from a value. If the target is an abstract heap segment, materialize
   * heap segment.
   *
   * <p>Constant.
   *
   * @param pValue A value for which to return the address.
   * @return the address represented by the passed value. The value needs to be a pointer, i.e. it
   *     needs to have a points-to edge. If it does not have it, the method raises an exception.
   * @throws SMGInconsistentException When the value passed does not have a Points-To edge.
   */
  @Override
  public List<SMGAddressValueAndState> getPointerFromValue(SMGValue pValue)
      throws SMGInconsistentException {
    if (heap.isPointer(pValue)) {
      SMGEdgePointsTo addressValue = heap.getPointer(pValue);
      SMGObject obj = addressValue.getObject();

      if (obj.isAbstract()) {
        performConsistencyCheck(SMGRuntimeCheck.HALF);
        return handleMaterilisation(addressValue, ((SMGAbstractObject) obj));
      }

      return Collections.singletonList(SMGAddressValueAndState.of(this, addressValue));
    }

    throw new SMGInconsistentException("Asked for a Points-To edge for a non-pointer value");
  }

  private List<SMGAddressValueAndState> handleMaterilisation(
      SMGEdgePointsTo pointerToAbstractObject, SMGAbstractObject pSmgAbstractObject)
      throws SMGInconsistentException {

    List<SMGAddressValueAndState> result = new ArrayList<>(2);
    switch (pSmgAbstractObject.getKind()) {
      case DLL:
        SMGDoublyLinkedList dllListSeg = (SMGDoublyLinkedList) pSmgAbstractObject;
        if (dllListSeg.getMinimumLength() == 0) {
          result.addAll(copyOf().removeDls(dllListSeg, pointerToAbstractObject));
        }
        result.add(materialiseDls(dllListSeg, pointerToAbstractObject));
        break;
      case SLL:
        SMGSingleLinkedList sllListSeg = (SMGSingleLinkedList) pSmgAbstractObject;
        if (sllListSeg.getMinimumLength() == 0) {
          result.addAll(copyOf().removeSll(sllListSeg, pointerToAbstractObject));
        }
        result.add(materialiseSll(sllListSeg, pointerToAbstractObject));
        break;
      case OPTIONAL:
        SMGOptionalObject optionalObject = (SMGOptionalObject) pSmgAbstractObject;
        result.addAll(copyOf().removeOptionalObject(optionalObject));
        result.add(materialiseOptionalObject(optionalObject, pointerToAbstractObject));
        break;
      default:
        throw new UnsupportedOperationException(
            "Materilization of abstraction" + pSmgAbstractObject + " not yet implemented.");
    }
    return result;
  }

  private List<SMGAddressValueAndState> removeOptionalObject(SMGOptionalObject pOptionalObject)
      throws SMGInconsistentException {

    logger.log(Level.ALL, "Remove ", pOptionalObject, " in state id ", getId());

    /*Just remove the optional Object and merge all incoming pointer
     * with the one pointer in all fields of the optional edge.
     * If there is no pointer besides zero in the fields of the
     * optional object, use zero.*/

    Set<SMGEdgePointsTo> pointer = SMGUtils.getPointerToThisObject(pOptionalObject, heap);

    SMGHasValueEdges fields = getHVEdges(SMGEdgeHasValueFilter.objectFilter(pOptionalObject));

    heap.markHeapObjectDeletedAndRemoveEdges(pOptionalObject);

    SMGValue pointerValue = SMGZeroValue.INSTANCE;

    for (SMGEdgeHasValue field : fields) {
      if (heap.isPointer(field.getValue()) && !field.getValue().isZero()) {
        pointerValue = field.getValue();
        break;
      }
    }

    for (SMGEdgePointsTo edge : pointer) {
      heap.removePointsToEdge(edge.getValue());
      heap.replaceValue(pointerValue, edge.getValue());
    }

    return getPointerFromValue(pointerValue);
  }

  private SMGAddressValueAndState materialiseOptionalObject(
      SMGOptionalObject pOptionalObject, SMGEdgePointsTo pPointerToAbstractObject) {

    /*Just replace the optional object with a region*/
    logger.log(Level.ALL, "Materialise ", pOptionalObject, " in state id ", getId());

    Set<SMGEdgePointsTo> pointer = SMGUtils.getPointerToThisObject(pOptionalObject, heap);

    SMGHasValueEdges fields = getHVEdges(SMGEdgeHasValueFilter.objectFilter(pOptionalObject));

    SMGObject newObject =
        new SMGRegion(
            pOptionalObject.getSize(),
            "Concrete object of " + pOptionalObject,
            pOptionalObject.getLevel());

    heap.addHeapObject(newObject);
    heap.setValidity(newObject, heap.isObjectValid(pOptionalObject));

    heap.markHeapObjectDeletedAndRemoveEdges(pOptionalObject);

    for (SMGEdgeHasValue edge : fields) {
      heap.addHasValueEdge(
          new SMGEdgeHasValue(edge.getSizeInBits(), edge.getOffset(), newObject, edge.getValue()));
    }

    for (SMGEdgePointsTo edge : pointer) {
      heap.removePointsToEdge(edge.getValue());
      heap.addPointsToEdge(new SMGEdgePointsTo(edge.getValue(), newObject, edge.getOffset()));
    }

    return SMGAddressValueAndState.of(
        this,
        new SMGEdgePointsTo(
            pPointerToAbstractObject.getValue(), newObject, pPointerToAbstractObject.getOffset()));
  }

  private List<SMGAddressValueAndState> removeSll(
      SMGSingleLinkedList pListSeg, SMGEdgePointsTo pPointerToAbstractObject)
      throws SMGInconsistentException {

    logger.log(Level.ALL, "Remove ", pListSeg, " in state id ", getId());

    /*First, set all sub smgs of sll to be removed to invalid.*/
    Set<Long> restriction = ImmutableSet.of(pListSeg.getNfo());

    removeRestrictedSubSmg(pListSeg, restriction);

    /*When removing sll, connect target specifier first pointer to next field*/

    long nfo = pListSeg.getNfo();
    long hfo = pListSeg.getHfo();

    SMGValue nextPointer = readValue(pListSeg, nfo, sizeOfVoidPointerInBits).getObject();

    SMGValue firstPointer = getAddress(pListSeg, hfo, SMGTargetSpecifier.FIRST);

    heap.markHeapObjectDeletedAndRemoveEdges(pListSeg);

    heap.replaceValue(nextPointer, firstPointer);

    if (firstPointer.equals(pPointerToAbstractObject.getValue())) {
      return getPointerFromValue(nextPointer);
    } else {
      throw new AssertionError(
          "Unexpected dereference of pointer "
              + pPointerToAbstractObject.getValue()
              + " pointing to abstraction "
              + pListSeg);
    }
  }

  private List<SMGAddressValueAndState> removeDls(
      SMGDoublyLinkedList pListSeg, SMGEdgePointsTo pPointerToAbstractObject)
      throws SMGInconsistentException {

    logger.log(Level.ALL, "Remove ", pListSeg, " in state id ", getId());

    /*First, set all sub smgs of dll to be removed to invalid.*/
    Set<Long> restriction = ImmutableSet.of(pListSeg.getNfo(), pListSeg.getPfo());

    removeRestrictedSubSmg(pListSeg, restriction);

    /*When removing dll, connect target specifier first pointer to next field,
     * and target specifier last to prev field*/

    long nfo = pListSeg.getNfo();
    long pfo = pListSeg.getPfo();
    long hfo = pListSeg.getHfo();

    SMGValue nextPointer = readValue(pListSeg, nfo, sizeOfVoidPointerInBits).getObject();
    SMGValue prevPointer = readValue(pListSeg, pfo, sizeOfVoidPointerInBits).getObject();

    SMGSymbolicValue firstPointer = getAddress(pListSeg, hfo, SMGTargetSpecifier.FIRST);
    SMGSymbolicValue lastPointer = getAddress(pListSeg, hfo, SMGTargetSpecifier.LAST);

    heap.markHeapObjectDeletedAndRemoveEdges(pListSeg);

    /* We may not have pointers to the beginning/end to this list.
     *  */

    if (firstPointer != null) {
      heap.removePointsToEdge(firstPointer);
      heap.replaceValue(nextPointer, firstPointer);
    }

    if (lastPointer != null) {
      heap.removePointsToEdge(lastPointer);
      heap.replaceValue(prevPointer, lastPointer);
    }

    if (firstPointer != null && firstPointer.equals(pPointerToAbstractObject.getValue())) {
      return getPointerFromValue(nextPointer);
    } else if (lastPointer != null && lastPointer.equals(pPointerToAbstractObject.getValue())) {
      return getPointerFromValue(prevPointer);
    } else {
      throw new AssertionError(
          "Unexpected dereference of pointer "
              + pPointerToAbstractObject.getValue()
              + " pointing to abstraction "
              + pListSeg);
    }
  }

  private SMGAddressValueAndState materialiseSll(
      SMGSingleLinkedList pListSeg, SMGEdgePointsTo pPointerToAbstractObject)
      throws SMGInconsistentException {

    logger.log(Level.ALL, "Materialise ", pListSeg, " in state id ", getId());

    if (pPointerToAbstractObject.getTargetSpecifier() != SMGTargetSpecifier.FIRST) {
      throw new SMGInconsistentException(
          "Target specifier of pointer "
              + pPointerToAbstractObject.getValue()
              + "that leads to a sll has unexpected target specifier "
              + pPointerToAbstractObject.getTargetSpecifier());
    }

    SMGRegion newConcreteRegion =
        new SMGRegion(pListSeg.getSize(), "concrete sll segment ID " + SMGCPA.getNewValue(), 0);
    heap.addHeapObject(newConcreteRegion);

    Set<SMGEdgeHasValue> restriction =
        ImmutableSet.of(
            new SMGEdgeHasValue(
                sizeOfVoidPointerInBits, pListSeg.getNfo(), pListSeg, SMGZeroValue.INSTANCE));

    copyRestrictedSubSmgToObject(pListSeg, newConcreteRegion, restriction);

    long hfo = pListSeg.getHfo();
    long nfo = pListSeg.getNfo();

    Iterable<SMGEdgeHasValue> oldSllFieldsToOldRegion =
        heap.getHVEdges(
            SMGEdgeHasValueFilter.objectFilter(pListSeg)
                .filterAtOffset(nfo)
                .filterBySize(sizeOfVoidPointerInBits));
    SMGValue oldPointerToRegion = readValue(pListSeg, nfo, sizeOfVoidPointerInBits).getObject();
    if (oldSllFieldsToOldRegion.iterator().hasNext()) {
      SMGEdgeHasValue oldSllFieldToOldRegion = Iterables.getOnlyElement(oldSllFieldsToOldRegion);
      heap.removeHasValueEdge(oldSllFieldToOldRegion);
    }

    SMGValue oldPointerToSll = pPointerToAbstractObject.getValue();

    SMGHasValueEdges oldFieldsEdges = heap.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pListSeg));
    Set<SMGEdgePointsTo> oldPtEdges = SMGUtils.getPointerToThisObject(pListSeg, heap);

    heap.removePointsToEdge(oldPointerToSll);

    heap.markHeapObjectDeletedAndRemoveEdges(pListSeg);

    SMGSingleLinkedList newSll =
        new SMGSingleLinkedList(
            pListSeg.getSize(),
            pListSeg.getHfo(),
            pListSeg.getNfo(),
            pListSeg.getMinimumLength() > 0 ? pListSeg.getMinimumLength() - 1 : 0,
            0);

    heap.addHeapObject(newSll);
    heap.setValidity(newSll, true);

    /*Check if pointer was already created due to All target Specifier*/
    SMGValue newPointerToNewRegion = getAddress(newConcreteRegion, hfo);

    if (newPointerToNewRegion != null) {
      heap.removePointsToEdge(newPointerToNewRegion);
      heap.replaceValue(oldPointerToSll, newPointerToNewRegion);
    }

    SMGEdgePointsTo newPtEdgeToNewRegionFromOutsideSMG =
        new SMGEdgePointsTo(oldPointerToSll, newConcreteRegion, hfo);

    SMGSymbolicValue newPointerToSll = SMGKnownSymValue.of();

    /*If you can't find the pointer, use generic pointer type*/
    long sizeOfPointerToSll;

    Iterable<SMGEdgeHasValue> fieldsContainingOldPointerToSll =
        heap.getHVEdges(SMGEdgeHasValueFilter.valueFilter(oldPointerToSll));

    if (!fieldsContainingOldPointerToSll.iterator().hasNext()) {
      sizeOfPointerToSll = sizeOfVoidPointerInBits;
    } else {
      sizeOfPointerToSll = fieldsContainingOldPointerToSll.iterator().next().getSizeInBits();
    }

    SMGEdgePointsTo newPtEToSll =
        new SMGEdgePointsTo(newPointerToSll, newSll, hfo, SMGTargetSpecifier.FIRST);

    newPointerToSll = SMGKnownAddressValue.valueOf(newPtEToSll);

    writeValue(newConcreteRegion, nfo, sizeOfPointerToSll, newPointerToSll);

    for (SMGEdgeHasValue hve : oldFieldsEdges) {
      heap.addHasValueEdge(
          new SMGEdgeHasValue(hve.getSizeInBits(), hve.getOffset(), newSll, hve.getValue()));
    }

    for (SMGEdgePointsTo ptE : oldPtEdges) {
      heap.addPointsToEdge(
          new SMGEdgePointsTo(ptE.getValue(), newSll, ptE.getOffset(), ptE.getTargetSpecifier()));
    }

    heap.addPointsToEdge(newPtEdgeToNewRegionFromOutsideSMG);

    heap.addValue(newPointerToSll);

    heap.addPointsToEdge(newPtEToSll);

    writeValue(newSll, nfo, sizeOfVoidPointerInBits, oldPointerToRegion);

    return SMGAddressValueAndState.of(this, newPtEdgeToNewRegionFromOutsideSMG);
  }

  private SMGAddressValueAndState materialiseDls(
      SMGDoublyLinkedList pListSeg, SMGEdgePointsTo pPointerToAbstractObject)
      throws SMGInconsistentException {

    logger.log(Level.ALL, "Materialise ", pListSeg, " in state id ", getId());

    SMGRegion newConcreteRegion =
        new SMGRegion(pListSeg.getSize(), "concrete dll segment ID " + SMGCPA.getNewValue(), 0);
    heap.addHeapObject(newConcreteRegion);

    Set<SMGEdgeHasValue> restriction =
        ImmutableSet.of(
            new SMGEdgeHasValue(
                sizeOfVoidPointerInBits, pListSeg.getNfo(), pListSeg, SMGZeroValue.INSTANCE),
            new SMGEdgeHasValue(
                sizeOfVoidPointerInBits, pListSeg.getPfo(), pListSeg, SMGZeroValue.INSTANCE));

    copyRestrictedSubSmgToObject(pListSeg, newConcreteRegion, restriction);

    SMGTargetSpecifier tg = pPointerToAbstractObject.getTargetSpecifier();

    long offsetPointingToDll;
    long offsetPointingToRegion;

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
            "Target specifier of pointer "
                + pPointerToAbstractObject.getValue()
                + "that leads to a dll has unexpected target specifier "
                + tg);
    }

    long hfo = pListSeg.getHfo();

    Iterable<SMGEdgeHasValue> oldDllFieldsToOldRegion =
        heap.getHVEdges(
            SMGEdgeHasValueFilter.objectFilter(pListSeg)
                .filterAtOffset(offsetPointingToRegion)
                .filterWithoutSize());
    SMGValue oldPointerToRegion =
        readValue(pListSeg, offsetPointingToRegion, sizeOfVoidPointerInBits).getObject();
    if (oldDllFieldsToOldRegion.iterator().hasNext()) {
      SMGEdgeHasValue oldDllFieldToOldRegion = Iterables.getOnlyElement(oldDllFieldsToOldRegion);

      // Work around with nullified memory block
      if (oldDllFieldToOldRegion.getValue().isZero()) {
        oldDllFieldToOldRegion =
            new SMGEdgeHasValue(
                sizeOfVoidPointerInBits,
                oldDllFieldToOldRegion.getOffset(),
                oldDllFieldToOldRegion.getObject(),
                oldDllFieldToOldRegion.getValue());
      }
      heap.removeHasValueEdge(oldDllFieldToOldRegion);
    }

    SMGKnownSymbolicValue oldPointerToDll =
        (SMGKnownSymbolicValue) pPointerToAbstractObject.getValue();

    heap.removePointsToEdge(oldPointerToDll);

    SMGHasValueEdges oldFieldsEdges = heap.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pListSeg));
    Set<SMGEdgePointsTo> oldPtEdges = SMGUtils.getPointerToThisObject(pListSeg, heap);

    heap.markHeapObjectDeletedAndRemoveEdges(pListSeg);

    SMGDoublyLinkedList newDll =
        new SMGDoublyLinkedList(
            pListSeg.getSize(),
            pListSeg.getHfo(),
            pListSeg.getNfo(),
            pListSeg.getPfo(),
            pListSeg.getMinimumLength() > 0 ? pListSeg.getMinimumLength() - 1 : 0,
            0);

    heap.addHeapObject(newDll);
    heap.setValidity(newDll, true);

    /*Check if pointer was already created due to All target Specifier*/
    SMGValue newPointerToNewRegion = getAddress(newConcreteRegion, hfo);

    if (newPointerToNewRegion != null) {
      heap.removePointsToEdge(newPointerToNewRegion);
      heap.replaceValue(oldPointerToDll, newPointerToNewRegion);
    }

    SMGEdgePointsTo newPtEdgeToNewRegionFromOutsideSMG =
        new SMGEdgePointsTo(oldPointerToDll, newConcreteRegion, hfo);
    writeValue(
        newConcreteRegion, offsetPointingToRegion, sizeOfVoidPointerInBits, oldPointerToRegion);

    SMGSymbolicValue newPointerToDll = SMGKnownSymValue.of();

    long sizeOfPointerToDll;

    Iterable<SMGEdgeHasValue> fieldsContainingOldPointerToDll =
        heap.getHVEdges(SMGEdgeHasValueFilter.valueFilter(oldPointerToDll));

    if (!fieldsContainingOldPointerToDll.iterator().hasNext()) {
      sizeOfPointerToDll = sizeOfVoidPointerInBits;
    } else {
      sizeOfPointerToDll = fieldsContainingOldPointerToDll.iterator().next().getSizeInBits();
    }

    writeValue(newConcreteRegion, offsetPointingToDll, sizeOfPointerToDll, newPointerToDll);
    SMGEdgePointsTo newPtEToDll = new SMGEdgePointsTo(newPointerToDll, newDll, hfo, tg);

    writeValue(newDll, offsetPointingToRegion, sizeOfVoidPointerInBits, oldPointerToDll);

    for (SMGEdgeHasValue hve : oldFieldsEdges) {
      heap.addHasValueEdge(
          new SMGEdgeHasValue(hve.getSizeInBits(), hve.getOffset(), newDll, hve.getValue()));
    }

    for (SMGEdgePointsTo ptE : oldPtEdges) {
      heap.addPointsToEdge(
          new SMGEdgePointsTo(ptE.getValue(), newDll, ptE.getOffset(), ptE.getTargetSpecifier()));
    }

    heap.addPointsToEdge(newPtEdgeToNewRegionFromOutsideSMG);

    heap.addValue(newPointerToDll);

    heap.addPointsToEdge(newPtEToDll);

    return SMGAddressValueAndState.of(this, newPtEdgeToNewRegionFromOutsideSMG);
  }

  private void copyRestrictedSubSmgToObject(
      SMGObject pRoot, SMGRegion pNewRegion, Set<SMGEdgeHasValue> pRestrictions) {

    Set<SMGObject> toBeChecked = new HashSet<>();
    Map<SMGObject, SMGObject> newObjectMap = new HashMap<>();
    Map<SMGValue, SMGValue> newValueMap = new HashMap<>();

    newObjectMap.put(pRoot, pNewRegion);

    for (SMGEdgeHasValue hve : heap.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pRoot))) {
      boolean restricted = false;
      for (SMGEdgeHasValue restriction : pRestrictions) {
        if (restriction.overlapsWith(hve)) {
          restricted = true;
        }
      }
      if (!restricted) {

        SMGValue subDlsValue = hve.getValue();
        SMGValue newVal = subDlsValue;

        if (heap.isPointer(subDlsValue)) {
          SMGEdgePointsTo reachedObjectSubSmgPTEdge = heap.getPointer(subDlsValue);
          SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();
          int level = reachedObjectSubSmg.getLevel();
          SMGTargetSpecifier tg = reachedObjectSubSmgPTEdge.getTargetSpecifier();

          if ((level != 0 || tg == SMGTargetSpecifier.ALL) && !newVal.isZero()) {

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
              newVal = SMGKnownSymValue.of();
              heap.addValue(newVal);
              newValueMap.put(subDlsValue, newVal);

              SMGTargetSpecifier newTg;

              if (copyOfReachedObject instanceof SMGRegion) {
                newTg = SMGTargetSpecifier.REGION;
              } else {
                newTg = reachedObjectSubSmgPTEdge.getTargetSpecifier();
              }

              SMGEdgePointsTo newPtEdge =
                  new SMGEdgePointsTo(
                      newVal, copyOfReachedObject, reachedObjectSubSmgPTEdge.getOffset(), newTg);
              heap.addPointsToEdge(newPtEdge);
            }
          }
        }
        heap.addHasValueEdge(
            new SMGEdgeHasValue(hve.getSizeInBits(), hve.getOffset(), pNewRegion, newVal));
      } else {
        /*If a restricted field is 0, and bigger than a pointer, add 0*/
        if (hve.getValue().isZero()) {
          Map<Long, Long> newEdges = new HashMap<>();
          newEdges.put(hve.getOffset(), hve.getSizeInBits());
          for (SMGEdgeHasValue restriction : pRestrictions) {
            for (Entry<Long, Long> newEdge : newEdges.entrySet()) {
              Map<Long, Long> recalcEdges = new HashMap<>();
              Long offset = newEdge.getKey();
              Long sizeInBits = newEdge.getValue();
              if (restriction.overlapsWith(offset, sizeInBits)) {
                if (offset < restriction.getOffset()) {
                  recalcEdges.put(offset, restriction.getOffset() - offset);
                }
                long endOffset = offset + sizeInBits;
                long restrictionEndOffset = restriction.getOffset() + restriction.getSizeInBits();
                if (endOffset > restrictionEndOffset) {
                  recalcEdges.put(restrictionEndOffset, endOffset - restrictionEndOffset);
                }
              } else {
                recalcEdges.put(offset, sizeInBits);
              }
              newEdges = recalcEdges;
            }
          }
          for (Entry<Long, Long> newEdge : newEdges.entrySet()) {
            SMGEdgeHasValue expandedZeroEdge =
                new SMGEdgeHasValue(
                    newEdge.getValue(), newEdge.getKey(), pNewRegion, SMGZeroValue.INSTANCE);
            heap.addHasValueEdge(expandedZeroEdge);
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
        copyObjectAndNodesIntoDestSMG(objToCheck, toBeChecked, newObjectMap, newValueMap);
      }
    }
  }

  private void copyObjectAndNodesIntoDestSMG(
      SMGObject pObjToCheck,
      Set<SMGObject> pToBeChecked,
      Map<SMGObject, SMGObject> newObjectMap,
      Map<SMGValue, SMGValue> newValueMap) {

    SMGObject newObj = newObjectMap.get(pObjToCheck);
    for (SMGEdgeHasValue hve : heap.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObjToCheck))) {
      SMGValue subDlsValue = hve.getValue();
      SMGValue newVal = subDlsValue;

      if (heap.isPointer(subDlsValue)) {
        SMGEdgePointsTo reachedObjectSubSmgPTEdge = heap.getPointer(subDlsValue);
        SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();
        int level = reachedObjectSubSmg.getLevel();
        SMGTargetSpecifier tg = reachedObjectSubSmgPTEdge.getTargetSpecifier();

        if ((level != 0 || tg == SMGTargetSpecifier.ALL) && !newVal.isZero()) {

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
            newVal = SMGKnownSymValue.of();
            heap.addValue(newVal);
            newValueMap.put(subDlsValue, newVal);

            SMGTargetSpecifier newTg;

            if (copyOfReachedObject instanceof SMGRegion) {
              newTg = SMGTargetSpecifier.REGION;
            } else {
              newTg = reachedObjectSubSmgPTEdge.getTargetSpecifier();
            }

            SMGEdgePointsTo newPtEdge =
                new SMGEdgePointsTo(
                    newVal, copyOfReachedObject, reachedObjectSubSmgPTEdge.getOffset(), newTg);
            heap.addPointsToEdge(newPtEdge);
          }
        }
      }
      heap.addHasValueEdge(
          new SMGEdgeHasValue(hve.getSizeInBits(), hve.getOffset(), newObj, newVal));
    }
  }

  private void removeRestrictedSubSmg(SMGObject pRoot, Set<Long> pRestriction) {

    Set<SMGObject> toBeChecked = new HashSet<>();
    Set<SMGObject> reached = new HashSet<>();

    reached.add(pRoot);

    for (SMGEdgeHasValue hve : heap.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pRoot))) {
      if (!pRestriction.contains(hve.getOffset())) {

        SMGValue subDlsValue = hve.getValue();

        if (heap.isPointer(subDlsValue)) {
          SMGEdgePointsTo reachedObjectSubSmgPTEdge = heap.getPointer(subDlsValue);
          SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();
          int level = reachedObjectSubSmg.getLevel();
          SMGTargetSpecifier tg = reachedObjectSubSmgPTEdge.getTargetSpecifier();

          if (!reached.contains(reachedObjectSubSmg)
              && (level != 0 || tg == SMGTargetSpecifier.ALL)
              && !subDlsValue.isZero()) {
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
        heap.markHeapObjectDeletedAndRemoveEdges(toBeRemoved);
      }
    }
  }

  private void removeRestrictedSubSmg(
      SMGObject pObjToCheck, Set<SMGObject> pToBeChecked, Set<SMGObject> reached) {

    for (SMGEdgeHasValue hve : heap.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObjToCheck))) {
      SMGValue subDlsValue = hve.getValue();

      if (heap.isPointer(subDlsValue)) {
        SMGEdgePointsTo reachedObjectSubSmgPTEdge = heap.getPointer(subDlsValue);
        SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();
        int level = reachedObjectSubSmg.getLevel();
        SMGTargetSpecifier tg = reachedObjectSubSmgPTEdge.getTargetSpecifier();

        if (!reached.contains(reachedObjectSubSmg)
            && (level != 0 || tg == SMGTargetSpecifier.ALL)
            && !subDlsValue.isZero()) {
          assert level > 0;
          reached.add(reachedObjectSubSmg);
          heap.setValidity(reachedObjectSubSmg, false);
          pToBeChecked.add(reachedObjectSubSmg);
        }
      }
    }
  }

  /**
   * Read Value in field (object, type) of an Object. If a Value cannot be determined, but the given
   * object and field is a valid place to read a value, a new value will be generated and returned.
   * (Does not create a new State but modifies this state).
   *
   * @param pObject SMGObject representing the memory the field belongs to.
   * @param pOffset offset of field being read.
   * @param pType type of field
   * @return the value and the state (may be the given state)
   */
  public SMGValueAndState forceReadValue(SMGObject pObject, long pOffset, CType pType)
      throws SMGInconsistentException {
    long sizeInBits = heap.getMachineModel().getSizeofInBits(pType).longValueExact();
    SMGValueAndState valueAndState = readValue(pObject, pOffset, sizeInBits);

    // Do not create a value if the read is invalid.
    if (valueAndState.getObject().isUnknown()
        && !valueAndState.getSmgState().errorInfo.isInvalidRead()) {
      SMGStateEdgePair stateAndNewEdge;
      if (valueAndState.getSmgState().getHeap().isObjectExternallyAllocated(pObject)
          && pType.getCanonicalType() instanceof CPointerType) {
        SMGAddressValue new_address =
            SMGKnownAddressValue.valueOf(
                valueAndState
                    .getSmgState()
                    .addExternalAllocation(genRecursiveLabel(pObject.getLabel())));
        stateAndNewEdge = writeValue(pObject, pOffset, sizeInBits, new_address);
      } else {
        SMGValue newValue = SMGKnownSymValue.of();
        stateAndNewEdge = writeValue0(pObject, pOffset, sizeInBits, newValue);
      }
      return SMGValueAndState.of(
          stateAndNewEdge.getState(), stateAndNewEdge.getNewEdge().getValue());
    } else {
      return valueAndState;
    }
  }

  private String genRecursiveLabel(String pLabel) {
    Matcher result = externalAllocationRecursivePattern.matcher(pLabel);
    if (result.matches()) {
      String in = result.group(2);
      int level = Integer.parseInt(in) + 1;
      return result.replaceFirst("$1" + level + "$3");
    } else {
      return "r_1_" + pLabel;
    }
  }

  /**
   * Read Value in field (object, type) of an Object.
   *
   * @param pObject SMGObject representing the memory the field belongs to.
   * @param pOffset offset of field being read.
   * @param pSizeInBits size of the field being read.
   * @return the value and the state (may be the given state)
   */
  public SMGValueAndState readValue(SMGObject pObject, long pOffset, long pSizeInBits)
      throws SMGInconsistentException {
    if (!heap.isObjectValid(pObject) && !heap.isObjectExternallyAllocated(pObject)) {
      SMGState newState =
          withInvalidRead().withErrorDescription("Try to read from deallocated object");
      newState.addInvalidObject(pObject);
      return SMGValueAndState.withUnknownValue(newState);
    }

    SMGEdgeHasValueFilter filter =
        SMGEdgeHasValueFilter.objectFilter(pObject)
            .filterAtOffset(pOffset)
            .filterBySize(pSizeInBits);
    Iterable<SMGEdgeHasValue> matchingEdges = heap.getHVEdges(filter);
    if (matchingEdges.iterator().hasNext()) {
      SMGEdgeHasValue object_edge = Iterables.getOnlyElement(matchingEdges);
      performConsistencyCheck(SMGRuntimeCheck.HALF);
      addElementToCurrentChain(object_edge);
      return SMGValueAndState.of(this, object_edge.getValue());
    }

    // if some edge points to a large enough chunk of memory, then use its middle part.
    // TODO this code is ugly and might better be placed inside heap.getHVEdges,
    // but there it removes the existing value from the heap for the rest of the region
    // and that is not wanted. Thus we add this very special case here.
    SMGEdgeHasValueFilter filterOffsetZero =
        new SMGEdgeHasValueFilter()
            .overlapsWith(
                new SMGEdgeHasValue(pSizeInBits, pOffset, pObject, SMGZeroValue.INSTANCE));
    Iterable<SMGEdgeHasValue> matchingEdgesOffsetZero = heap.getHVEdges(filterOffsetZero);
    for (SMGEdgeHasValue object_edge : matchingEdgesOffsetZero) {
      if (pOffset >= object_edge.getOffset()
          && pOffset + pSizeInBits <= object_edge.getOffset() + object_edge.getSizeInBits()) {
        SMGValue symValue = object_edge.getValue();
        if (isExplicit(symValue)) {
          SMGKnownExpValue expValue = getExplicit(symValue);
          BigInteger value = expValue.getValue();

          // extract the important bits
          // TODO we depend on little or big endian here, query this info from machinemodel?

          // remove the lower part
          value = value.shiftRight((int) (pOffset - object_edge.getOffset()));
          for (int i = (int) pSizeInBits; i < value.bitLength(); i++) {
            value = value.clearBit(i); // remove the upper part
          }

          addElementToCurrentChain(object_edge);
          return SMGValueAndState.of(this, SMGKnownExpValue.valueOf(value));
        }
      }
    }

    SMGEdgeHasValue edge =
        new SMGEdgeHasValue(pSizeInBits, pOffset, pObject, SMGZeroValue.INSTANCE);
    if (heap.isCoveredByNullifiedBlocks(edge)) {
      return SMGValueAndState.of(this, SMGZeroValue.INSTANCE);
    }

    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return SMGValueAndState.withUnknownValue(this);
  }

  @Override
  public SMGState withInvalidRead() {
    SMGState smgState = new SMGState(this, Property.INVALID_READ);
    smgState.moveCurrentChainToInvalidChain();
    return smgState;
  }

  /**
   * Write a value into a field (offset, type) of an Object. Additionally, this method writes a
   * points-to edge into the SMG, if the given symbolic value points to an address, and
   *
   * @param pObject SMGObject representing the memory the field belongs to.
   * @param pOffset offset of field written into.
   * @param pSizeInBits size of field written into.
   * @param pValue value to be written into field.
   * @return the edge and the new state (may be this state)
   */
  public SMGStateEdgePair writeValue(
      SMGObject pObject, long pOffset, long pSizeInBits, SMGValue pValue)
      throws SMGInconsistentException {

    SMGValue value;

    // If the value is not yet known by the SMG
    // create a unconstrained new symbolic value
    if (pValue.isUnknown()) {
      value = SMGKnownSymValue.of();
    } else {
      value = pValue;
    }

    // If the value represents an address, and the address is known,
    // add the necessary points-To edge.
    if (pValue instanceof SMGAddressValue) {
      SMGAddress address = ((SMGAddressValue) pValue).getAddress();

      if (!address.isUnknown()) {
        addPointsToEdge(address.getObject(), address.getOffset().getAsLong(), value);
      }
    }

    return writeValue0(pObject, pOffset, pSizeInBits, value);
  }

  public void addPointsToEdge(SMGObject pObject, long pOffset, SMGValue pValue) {
    heap.addValue(pValue);
    heap.addPointsToEdge(new SMGEdgePointsTo(pValue, pObject, pOffset));
  }

  /**
   * Write a value into a field (offset, type) of an Object.
   *
   * @param pObject SMGObject representing the memory the field belongs to.
   * @param pOffset offset of field written into.
   * @param pSizeInBits size of field written into.
   * @param pValue value to be written into field.
   */
  private SMGStateEdgePair writeValue0(
      SMGObject pObject, long pOffset, long pSizeInBits, SMGValue pValue)
      throws SMGInconsistentException {
    // vgl Algorithm 1 Byte-Precise Verification of Low-Level List Manipulation FIT-TR-2012-04

    if (!heap.isObjectValid(pObject) && !heap.isObjectExternallyAllocated(pObject)) {
      // Attempt to write to invalid object
      SMGState newState = withInvalidWrite();
      newState
          .withErrorDescription("Attempt to write to deallocated object")
          .addInvalidObject(pObject);
      return new SMGStateEdgePair(newState, null);
    }

    SMGEdgeHasValue new_edge = new SMGEdgeHasValue(pSizeInBits, pOffset, pObject, pValue);

    // Check if the edge is  not present already
    SMGEdgeHasValueFilter filter =
        SMGEdgeHasValueFilter.objectFilter(pObject).overlapsWith(new_edge);

    Iterable<SMGEdgeHasValue> overlappingEdges = heap.getHVEdges(filter);
    if (Iterables.contains(overlappingEdges, new_edge)) {
      performConsistencyCheck(SMGRuntimeCheck.HALF);
      return new SMGStateEdgePair(this, new_edge);
    }

    heap.addValue(pValue);

    List<SMGEdgeHasValue> overlappingZeroEdges = new ArrayList<>();

    /* We need to remove all non-zero overlapping edges
     * and remember all overlapping zero edges to shrink them later
     */
    for (SMGEdgeHasValue hv : overlappingEdges) {

      boolean hvEdgeIsZero = hv.getValue() == SMGZeroValue.INSTANCE;

      if (hvEdgeIsZero) {
        overlappingZeroEdges.add(hv);
      } else {
        heap.removeHasValueEdge(hv);
      }
    }

    shrinkOverlappingZeroEdges(new_edge, overlappingZeroEdges);

    heap.addHasValueEdge(new_edge);
    performConsistencyCheck(SMGRuntimeCheck.HALF);

    return new SMGStateEdgePair(this, new_edge);
  }

  @Override
  public boolean isBlockEnded() {
    return blockEnded;
  }

  public static class SMGStateEdgePair {

    private final SMGState smgState;
    @Nullable private final SMGEdgeHasValue edge;

    private SMGStateEdgePair(SMGState pState, @Nullable SMGEdgeHasValue pEdge) {
      smgState = pState;
      edge = pEdge;
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
  }

  private void shrinkOverlappingZeroEdges(
      SMGEdgeHasValue pNew_edge, Iterable<SMGEdgeHasValue> pOverlappingZeroEdges) {

    SMGObject object = pNew_edge.getObject();
    long offset = pNew_edge.getOffset();

    long sizeOfType = pNew_edge.getSizeInBits();

    // Shrink overlapping zero edges
    for (SMGEdgeHasValue zeroEdge : pOverlappingZeroEdges) {
      heap.removeHasValueEdge(zeroEdge);

      long zeroEdgeOffset = zeroEdge.getOffset();

      long offset2 = offset + sizeOfType;
      long zeroEdgeOffset2 = zeroEdgeOffset + zeroEdge.getSizeInBits();

      if (zeroEdgeOffset < offset) {
        SMGEdgeHasValue newZeroEdge =
            new SMGEdgeHasValue(
                Math.toIntExact(offset - zeroEdgeOffset),
                zeroEdgeOffset,
                object,
                SMGZeroValue.INSTANCE);
        heap.addHasValueEdge(newZeroEdge);
      }

      if (offset2 < zeroEdgeOffset2) {
        SMGEdgeHasValue newZeroEdge =
            new SMGEdgeHasValue(
                Math.toIntExact(zeroEdgeOffset2 - offset2), offset2, object, SMGZeroValue.INSTANCE);
        heap.addHasValueEdge(newZeroEdge);
      }
    }
  }

  @Override
  public SMGState withInvalidWrite() {
    SMGState smgState = new SMGState(this, Property.INVALID_WRITE);
    smgState.moveCurrentChainToInvalidChain();
    return smgState;
  }

  /**
   * Computes the join of this abstract State and the reached abstract State, or returns the reached
   * state, if no join is defined.
   *
   * @param reachedState the abstract state this state will be joined to.
   * @return the join of the two states or reached state.
   * @throws SMGInconsistentException inconsistent smgs while
   */
  @Override
  public UnmodifiableSMGState join(UnmodifiableSMGState reachedState)
      throws SMGInconsistentException {
    // Not necessary if merge_SEP and stop_SEP is used.

    if (options.getJoinOnBlockEnd() && !reachedState.isBlockEnded()) {
      return reachedState;
    }

    SMGJoin join = new SMGJoin(heap, reachedState.getHeap(), this, reachedState);

    if (!(join.getStatus() == SMGJoinStatus.INCOMPARABLE && join.isDefined())) {
      return reachedState;
    }

    CLangSMG destHeap = join.getJointSMG();

    return new SMGState(logger, options, destHeap, predecessorId, join.getMergedExplicitValues());
  }

  /**
   * Computes whether this abstract state is covered by the given abstract state. A state is covered
   * by another state, if the set of concrete states a state represents is a subset of the set of
   * concrete states the other state represents.
   *
   * @param reachedState already reached state, that may cover this state already.
   * @return True, if this state is covered by the given state, false otherwise.
   */
  @Override
  public boolean isLessOrEqual(UnmodifiableSMGState reachedState) throws SMGInconsistentException {

    if (!getErrorPredicateRelation().isEmpty()
        || !reachedState.getErrorPredicateRelation().isEmpty()) {
      return false;
    }

    if (options.isHeapAbstractionEnabled()) {
      SMGJoin join = new SMGJoin(heap, reachedState.getHeap(), this, reachedState);

      if (!join.isDefined()) {
        return false;
      }

      SMGJoinStatus jss = join.getStatus();
      if (jss != SMGJoinStatus.EQUAL && jss != SMGJoinStatus.RIGHT_ENTAIL) {
        return false;
      }

      // Only stop if either reached has memleak or this state has no memleak
      // to avoid losing memleak information.
      SMGState s1 = reachedState.copyOf();
      SMGState s2 = copyOf();
      s1.pruneUnreachable();
      s2.pruneUnreachable();
      logger.log(Level.ALL, getId(), " is Less or Equal ", reachedState.getId());
      return s1.errorInfo.hasMemoryLeak() == s2.errorInfo.hasMemoryLeak();

    } else {
      return SMGIsLessOrEqual.isLessOrEqual(reachedState.getHeap(), heap);
    }
  }

  @Override
  public String getCPAName() {
    return "SMGCPA";
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    switch (pProperty) {
      case "toString":
        return toString();
      case "heapObjects":
        return heap.getHeapObjects();
      default:
        // try boolean properties
        return checkProperty(pProperty);
    }
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    switch (pProperty) {
      case HAS_LEAKS:
        if (errorInfo.hasMemoryLeak()) {
          // TODO: Give more information
          issueMemoryError("Memory leak found", false);
          return true;
        }
        return false;
      case HAS_INVALID_WRITES:
        if (errorInfo.isInvalidWrite()) {
          // TODO: Give more information
          issueMemoryError("Invalid write found", true);
          return true;
        }
        return false;
      case HAS_INVALID_READS:
        if (errorInfo.isInvalidRead()) {
          // TODO: Give more information
          issueMemoryError("Invalid read found", true);
          return true;
        }
        return false;
      case HAS_INVALID_FREES:
        if (errorInfo.isInvalidFree()) {
          // TODO: Give more information
          issueMemoryError("Invalid free found", true);
          return true;
        }
        return false;
      case HAS_HEAP_OBJECTS:
        // Having heap objects is not an error on its own.
        // However, when combined with program exit, we can detect property MemCleanup.
        PersistentSet<SMGObject> heapObs = heap.getHeapObjects();
        Preconditions.checkState(
            heapObs.size() >= 1 && heapObs.contains(SMGNullObject.INSTANCE),
            "NULL must always be a heap object");
        for (SMGObject object : heapObs) {
          if (!heap.isObjectValid(object)) {
            heapObs = heapObs.removeAndCopy(object);
          }
        }
        return !heapObs.isEmpty();

      default:
        throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
    }
  }

  public void addGlobalObject(SMGRegion newObject) {
    heap.addGlobalObject(newObject);
  }

  /** memory allocated in the heap has to be freed by the user, otherwise this is a memory-leak. */
  public SMGEdgePointsTo addNewHeapAllocation(int pSize, String pLabel)
      throws SMGInconsistentException {
    return addHeapAllocation(pLabel, pSize, 0, false);
  }

  /** memory externally allocated could be freed by the user */
  public SMGEdgePointsTo addExternalAllocation(String pLabel) throws SMGInconsistentException {
    return addHeapAllocation(
        pLabel, options.getExternalAllocationSize(), options.getExternalAllocationSize() / 2, true);
  }

  private SMGEdgePointsTo addHeapAllocation(String label, int size, int offset, boolean external)
      throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(size, label);
    SMGKnownSymbolicValue new_value = SMGKnownSymValue.of();
    heap.addHeapObject(new_object);
    heap.setValidity(new_object, true);
    heap.addValue(new_value);
    for (SMGObject object : heap.getObjects()) {
      if (!SMGNullObject.INSTANCE.equals(object) && !heap.isObjectValid(object)) {
        heap.addPossibleEqualObjects(new_object, object);
      }
    }
    SMGEdgePointsTo pointsTo = new SMGEdgePointsTo(new_value, new_object, offset);
    heap.addPointsToEdge(pointsTo);
    heap.setExternallyAllocatedFlag(new_object, external);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return pointsTo;
  }

  public void setExternallyAllocatedFlag(SMGObject pObject) {
    heap.setExternallyAllocatedFlag(pObject, true);
  }

  /**
   * memory allocated on the stack is automatically freed when leaving the current function scope
   */
  public SMGEdgePointsTo addNewStackAllocation(int pSize, String pLabel)
      throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(pSize, pLabel);
    SMGKnownSymbolicValue new_value = SMGKnownSymValue.of();
    heap.addStackObject(new_object);
    heap.addValue(new_value);
    for (SMGObject object : heap.getObjects()) {
      if (!SMGNullObject.INSTANCE.equals(object) && !heap.isObjectValid(object)) {
        heap.addPossibleEqualObjects(new_object, object);
      }
    }
    SMGEdgePointsTo pointsTo = new SMGEdgePointsTo(new_value, new_object, 0);
    heap.addPointsToEdge(pointsTo);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return pointsTo;
  }

  /** Sets a flag indicating this SMGState is a successor over an edge causing a memory leak. */
  public void setMemLeak(String errorMsg, Collection<SMGObject> pUnreachableObjects) {
    errorInfo =
        errorInfo
            .withProperty(Property.INVALID_HEAP)
            .withErrorMessage(errorMsg)
            .withInvalidObjects(pUnreachableObjects);
  }

  @Override
  @Nullable
  public SMGSymbolicValue getAddress(SMGObject memory, long offset, SMGTargetSpecifier tg) {

    SMGEdgePointsToFilter filter =
        SMGEdgePointsToFilter.targetObjectFilter(memory)
            .filterAtTargetOffset(offset)
            .filterByTargetSpecifier(tg);

    Set<SMGEdgePointsTo> edges = heap.getPtEdges(filter);

    if (edges.isEmpty()) {
      return null;
    } else {
      return (SMGSymbolicValue) Iterables.getOnlyElement(edges).getValue();
    }
  }

  /**
   * This method simulates a free invocation. It checks, whether the call is valid, and invalidates
   * the Memory the given address points to. The address (address, offset, smgObject) is the
   * argument of the free invocation. It does not need to be part of the SMG.
   *
   * @param offset The offset of the address relative to the beginning of smgObject.
   * @param smgObject The memory the given Address belongs to.
   * @return returns a possible new State
   */
  protected SMGState free(Integer offset, SMGObject smgObject) throws SMGInconsistentException {

    if (!heap.isHeapObject(smgObject) && !heap.isObjectExternallyAllocated(smgObject)) {
      // You may not free any objects not on the heap.
      SMGState newState =
          withInvalidFree().withErrorDescription("Invalid free of unallocated object is found");
      newState.addInvalidObject(smgObject);
      return newState;
    }

    if (!heap.isObjectValid(smgObject)) {
      // you may not invoke free multiple times on the same object
      SMGState newState = withInvalidFree().withErrorDescription("Double free is found");
      newState.addInvalidObject(smgObject);
      return newState;
    }

    if (offset != 0 && !heap.isObjectExternallyAllocated(smgObject)) {
      // you may not invoke free on any address that you
      // didn't get through a malloc invocation.
      // TODO: externally allocated memory could be freed partially

      SMGState newState = withInvalidFree();
      newState.addInvalidObject(smgObject);
      final String description;
      if (offset % 8 != 0) {
        description = "Invalid free at " + offset + " bit offset from allocated is found";
      } else {
        description = "Invalid free at " + offset / 8 + " byte offset from allocated is found";
      }
      return newState.withErrorDescription(description);
    }

    heap.setValidity(smgObject, false);
    heap.setExternallyAllocatedFlag(smgObject, false);
    SMGEdgeHasValueFilterByObject filter = SMGEdgeHasValueFilter.objectFilter(smgObject);

    for (SMGEdgeHasValue edge : heap.getHVEdges(filter)) {
      heap.removeHasValueEdge(edge);
    }

    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return this;
  }

  @Override
  public Collection<Object> getInvalidChain() {
    return Collections.unmodifiableList(errorInfo.getInvalidChain());
  }

  public void addInvalidObject(SMGObject pSmgObject) {
    errorInfo = errorInfo.withInvalidObject(pSmgObject);
  }

  public void addElementToCurrentChain(Object elem) {
    // Avoid to add Null element
    if (elem instanceof SMGValue && ((SMGValue) elem).isZero()) {
      return;
    }
    errorInfo = errorInfo.withObject(elem);
  }

  @Override
  public Collection<Object> getCurrentChain() {
    return Collections.unmodifiableList(errorInfo.getCurrentChain());
  }

  protected void cleanCurrentChain() {
    errorInfo = errorInfo.withClearChain();
  }

  private void moveCurrentChainToInvalidChain() {
    errorInfo = errorInfo.moveCurrentChainToInvalidChain();
  }

  /** Drop the stack frame representing the stack of the function with the given name */
  public void dropStackFrame() throws SMGInconsistentException {
    heap.dropStackFrame();
    performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  public void pruneUnreachable() throws SMGInconsistentException {
    Set<SMGObject> unreachable = heap.pruneUnreachable();
    if (!unreachable.isEmpty()) {
      StringBuilder error = new StringBuilder();
      for (SMGObject obj : unreachable) {
        error.append(obj.getLabel());
      }
      setMemLeak("Memory leak of " + error + " is detected", unreachable);
    }
    // TODO: Explicit values pruning
    performConsistencyCheck(SMGRuntimeCheck.HALF);
  }

  @Override
  public SMGState withInvalidFree() {
    return new SMGState(this, Property.INVALID_FREE);
  }

  @VisibleForTesting
  Iterable<SMGEdgeHasValue> getHVEdges(SMGEdgeHasValueFilter pFilter) {
    return heap.getHVEdges(pFilter);
  }

  SMGHasValueEdges getHVEdges(SMGEdgeHasValueFilterByObject pFilter) {
    return heap.getHVEdges(pFilter);
  }

  /**
   * Copys (shallow) the hv-edges of source in the given source range to the target at the given
   * target offset. Note that the source range (pSourceLastCopyBitOffset - pSourceOffset) has to fit
   * into the target range ( size of pTarget - pTargetOffset). Also, pSourceOffset has to be less or
   * equal to the size of the source Object.
   *
   * <p>This method is mainly used to assign struct variables.
   *
   * @param pSource the SMGObject providing the hv-edges
   * @param pTarget the target of the copy process
   * @param pTargetOffset begin the copy of source at this offset
   * @param pSourceLastCopyBitOffset the size of the copy of source (not the size of the copy, but
   *     the size to the last bit of the source which should be copied).
   * @param pSourceOffset insert the copy of source into target at this offset
   * @throws SMGInconsistentException thrown if the copying leads to an inconsistent SMG.
   */
  public SMGState copy(
      SMGObject pSource,
      SMGObject pTarget,
      long pSourceOffset,
      long pSourceLastCopyBitOffset,
      long pTargetOffset)
      throws SMGInconsistentException {

    SMGState newSMGState = this;

    long copyRange = pSourceLastCopyBitOffset - pSourceOffset;

    assert pSource.getSize() >= pSourceLastCopyBitOffset;
    assert pSourceOffset >= 0;
    assert pTargetOffset >= 0;
    assert copyRange >= 0;
    assert copyRange <= pTarget.getSize();

    // If copy range is 0, do nothing
    if (copyRange == 0) {
      return newSMGState;
    }

    // If self assignment, do nothing
    // TODO this check should not be necessary,
    // there might be a bug in the lines below causing trouble with such cases
    if (pSource.equals(pTarget) && pSourceOffset == pTargetOffset) {
      return newSMGState;
    }

    long targetRangeSize = pTargetOffset + copyRange;

    SMGEdgeHasValueFilterByObject filterSource = SMGEdgeHasValueFilter.objectFilter(pSource);
    SMGEdgeHasValueFilterByObject filterTarget = SMGEdgeHasValueFilter.objectFilter(pTarget);

    // Remove all target edges in range
    for (SMGEdgeHasValue edge : getHVEdges(filterTarget)) {
      if (edge.overlapsWith(pTargetOffset, targetRangeSize)) {
        heap.removeHasValueEdge(edge);

        // Shrink overlapping zero edge
        if (edge.getValue() == SMGZeroValue.INSTANCE) {
          SMGObject object = edge.getObject();

          long zeroEdgeOffset = edge.getOffset();
          if (zeroEdgeOffset < pTargetOffset) {
            heap.addHasValueEdge(
                new SMGEdgeHasValue(
                    pTargetOffset - zeroEdgeOffset, zeroEdgeOffset, object, SMGZeroValue.INSTANCE));
          }

          long zeroEdgeOffset2 = zeroEdgeOffset + edge.getSizeInBits();
          if (targetRangeSize < zeroEdgeOffset2) {
            heap.addHasValueEdge(
                new SMGEdgeHasValue(
                    zeroEdgeOffset2 - targetRangeSize,
                    targetRangeSize,
                    object,
                    SMGZeroValue.INSTANCE));
          }
        }
      }
    }

    // Shift the source edge offset depending on the target range offset
    long copyShift = pTargetOffset - pSourceOffset;
    for (SMGEdgeHasValue edge : getHVEdges(filterSource)) {
      SMGValue newValue = edge.getValue();
      if (edge.overlapsWith(pSourceOffset, pSourceLastCopyBitOffset)) {
        long newSize = edge.getSizeInBits();
        long edgeOffset = edge.getOffset();
        long edgeEndOffset = edgeOffset + newSize;
        boolean writeEdge = true;
        if (edgeEndOffset > pSourceLastCopyBitOffset) {
          newSize = newSize - (edgeEndOffset - pSourceLastCopyBitOffset);
          writeEdge = newValue.isZero();
        }
        long newOffset = edgeOffset + copyShift;
        if (edgeOffset < pSourceOffset) {
          newOffset = pTargetOffset;
          newSize = newSize - (pSourceOffset - edgeOffset);
          writeEdge = newValue.isZero();
        }
        if (writeEdge) {
          newSMGState = writeValue0(pTarget, newOffset, newSize, newValue).getState();
        }
      }
    }

    performConsistencyCheck(SMGRuntimeCheck.FULL);
    // TODO Why do I do this here?
    Set<SMGObject> unreachable = newSMGState.heap.pruneUnreachable();
    if (!unreachable.isEmpty()) {
      newSMGState.setMemLeak("Memory leak is detected", unreachable);
    }
    performConsistencyCheck(SMGRuntimeCheck.FULL);
    return newSMGState;
  }

  @Override
  public SMGState withUnknownDereference() {
    // TODO: accurate define SMG change on unknown dereference with predicate knowledge
    if (options.isHandleUnknownDereferenceAsSafe() && isTrackErrorPredicatesEnabled()) {
      // doesn't stop analysis on unknown dereference
      return this;
    }

    // TODO: This can actually be an invalid read too
    //      The flagging mechanism should be improved
    return new SMGState(this, Property.INVALID_WRITE).withErrorDescription("Unknown dereference");
  }

  public void identifyEqualValues(
      SMGKnownSymbolicValue pKnownVal1, SMGKnownSymbolicValue pKnownVal2) {

    assert !(explicitValues.get(pKnownVal1) != null
        && explicitValues.get(pKnownVal1).equals(explicitValues.get(pKnownVal2)));

    // Avoid remove NULL value on merge
    if (pKnownVal2.isZero()) {
      SMGKnownSymbolicValue tmp = pKnownVal1;
      pKnownVal1 = pKnownVal2;
      pKnownVal2 = tmp;
    }

    if (!pKnownVal1.isZero() && heap.isPointer(pKnownVal1)) {
      SMGObject objectPointedBy1 = heap.getObjectPointedBy(pKnownVal1);
      if (!heap.isObjectValid(objectPointedBy1)) {
        heap.removePointsToEdge(pKnownVal1);
        SMGKnownSymbolicValue tmp = pKnownVal1;
        pKnownVal1 = pKnownVal2;
        pKnownVal2 = tmp;
      } else {
        if (!pKnownVal2.isZero() && heap.isPointer(pKnownVal2)) {
          SMGObject objectPointedBy2 = heap.getObjectPointedBy(pKnownVal2);
          if (!heap.isObjectValid(objectPointedBy2)) {
            heap.removePointsToEdge(pKnownVal2);
          }
        }
      }
    }

    heap.replaceValue(pKnownVal1, pKnownVal2);
    Preconditions.checkArgument(!pKnownVal2.isZero());
    SMGKnownExpValue expVal = explicitValues.get(pKnownVal2);
    explicitValues = explicitValues.removeAndCopy(pKnownVal2);
    if (expVal != null) {
      explicitValues = explicitValues.putAndCopy(pKnownVal1, expVal);
    }
  }

  public void identifyNonEqualValues(
      SMGKnownSymbolicValue pKnownVal1, SMGKnownSymbolicValue pKnownVal2) {
    heap.addNeqRelation(pKnownVal1, pKnownVal2);
  }

  @Override
  public boolean isTrackPredicatesEnabled() {
    return options.trackPredicates();
  }

  public boolean isTrackErrorPredicatesEnabled() {
    return options.trackErrorPredicates();
  }

  public boolean isCrashOnUnknownEnabled() {
    return options.crashOnUnknown();
  }

  public void addPredicateRelation(
      SMGValue pV1,
      SMGType pSMGType1,
      SMGValue pV2,
      SMGType pSMGType2,
      BinaryOperator pOp,
      CFAEdge pEdge) {
    if (isTrackPredicatesEnabled() && pEdge instanceof CAssumeEdge) {
      BinaryOperator temp;
      if (((CAssumeEdge) pEdge).getTruthAssumption()) {
        temp = pOp;
      } else {
        temp = pOp.getOppositLogicalOperator();
      }
      logger.logf(
          Level.FINER, "SymValue1 %s %s SymValue2 %s AddPredicate: %s", pV1, temp, pV2, pEdge);
      getPathPredicateRelation().addRelation(pV1, pSMGType1, pV2, pSMGType2, temp);
    }
  }

  public void addPredicateRelation(
      SMGValue pV1, SMGType pSMGType1, SMGExplicitValue pV2, BinaryOperator pOp, CFAEdge pEdge) {
    if (isTrackPredicatesEnabled() && pEdge instanceof CAssumeEdge) {
      BinaryOperator temp;
      if (((CAssumeEdge) pEdge).getTruthAssumption()) {
        temp = pOp;
      } else {
        temp = pOp.getOppositLogicalOperator();
      }
      logger.logf(
          Level.FINER, "SymValue %s %s; ExplValue %s; AddPredicate: %s", pV1, temp, pV2, pEdge);
      getPathPredicateRelation().addExplicitRelation(pV1, pSMGType1, pV2, temp);
    }
  }

  @Override
  public SMGPredicateRelation getPathPredicateRelation() {
    return heap.getPathPredicateRelation();
  }

  public void addErrorPredicate(
      SMGValue pSymbolicValue,
      SMGType pSymbolicSMGType,
      SMGExplicitValue pExplicitValue,
      CFAEdge pEdge) {
    if (isTrackErrorPredicatesEnabled()) {
      logger.log(
          Level.FINER,
          "Add Error Predicate: SymValue  ",
          pSymbolicValue,
          " ; ExplValue",
          " ",
          pExplicitValue,
          "; on edge: ",
          pEdge);
      getErrorPredicateRelation()
          .addExplicitRelation(
              pSymbolicValue, pSymbolicSMGType, pExplicitValue, BinaryOperator.GREATER_THAN);
    }
  }

  @Override
  public SMGPredicateRelation getErrorPredicateRelation() {
    return heap.getErrorPredicateRelation();
  }

  public SMGState resetErrorRelation() {
    SMGState newState = copyOf();
    newState.heap.resetErrorRelation();
    return newState;
  }

  /**
   * Returns explicit value merged with pKey, or Null if not merged.
   *
   * @param pKey the key.
   * @param pValue the value.
   */
  public SMGKnownSymbolicValue putExplicit(SMGKnownSymbolicValue pKey, SMGKnownExpValue pValue) {
    Preconditions.checkNotNull(pKey);
    Preconditions.checkNotNull(pValue);

    if (explicitValues.inverse().containsKey(pValue)) {
      SMGKnownSymbolicValue symValue = explicitValues.inverse().get(pValue);

      if (!pKey.equals(symValue)) {
        if (symValue.isZero()) { // swap values, we prefer ZERO in the SMG.
          heap.replaceValue(symValue, pKey);
        } else {
          Preconditions.checkArgument(!symValue.isZero());
          explicitValues = explicitValues.removeAndCopy(symValue);
          heap.replaceValue(pKey, symValue);
          explicitValues = explicitValues.putAndCopy(pKey, pValue);
          return symValue;
        }
      }

      return null;
    }

    explicitValues = explicitValues.putAndCopy(pKey, pValue);
    return null;
  }

  @Deprecated // unused
  public void clearExplicit(SMGKnownSymbolicValue pKey) {
    Preconditions.checkArgument(!pKey.isZero());
    explicitValues = explicitValues.removeAndCopy(pKey);
  }

  @Override
  public boolean isExplicit(SMGValue value) {
    return explicitValues.containsKey(value);
  }

  @Override
  @Nullable
  public SMGKnownExpValue getExplicit(SMGValue pKey) {
    return explicitValues.get(pKey);
  }

  @Nullable
  public SMGKnownSymbolicValue getSymbolicOfExplicit(SMGExplicitValue pExplicitValue) {
    if (pExplicitValue.isZero()) {
      return SMGZeroValue.INSTANCE;
    }
    return explicitValues.inverse().get(pExplicitValue);
  }

  enum Property {
    INVALID_READ,
    INVALID_WRITE,
    INVALID_FREE,
    INVALID_HEAP
  }

  @Override
  public boolean hasMemoryErrors() {
    return errorInfo.hasMemoryErrors();
  }

  @Override
  public boolean hasMemoryLeaks() {
    return errorInfo.hasMemoryLeak();
  }

  @Override
  public boolean areNonEqual(SMGValue pValue1, SMGValue pValue2) {

    if (pValue1.isUnknown() || pValue2.isUnknown() || pValue1.equals(pValue2)) {
      return false;
    } else if (pValue1 instanceof SMGExplicitValue && pValue2 instanceof SMGExplicitValue) {
      return !pValue1.equals(pValue2);
    } else if (isExplicit(pValue1) && isExplicit(pValue2)) {
      return !getExplicit(pValue1).equals(getExplicit(pValue2));
    } else if (heap.isPointer(pValue1) && heap.isPointer(pValue2)) {
      SMGObject object1 = heap.getObjectPointedBy(pValue1);
      SMGObject object2 = heap.getObjectPointedBy(pValue2);
      if (!object1.equals(object2)) {
        return !heap.arePossibleEquals(object1, object2);
      }
    }
    return heap.haveNeqRelation(pValue1, pValue2);
  }

  @Override
  public SMGObject getObjectForFunction(CFunctionDeclaration pDeclaration) {

    /* Treat functions as global objects with unknown memory size.
     * Only write them into the smg when necessary*/
    String functionQualifiedSMGName = getUniqueFunctionName(pDeclaration);

    return heap.getObjectForVisibleVariable(functionQualifiedSMGName);
  }

  public SMGObject createObjectForFunction(CFunctionDeclaration pDeclaration)
      throws SMGInconsistentException {

    /* Treat functions as global variable with unknown memory size.
     * Only write them into the smg when necessary*/
    String functionQualifiedSMGName = getUniqueFunctionName(pDeclaration);

    assert heap.getObjectForVisibleVariable(functionQualifiedSMGName) == null;

    return addGlobalVariable(0, functionQualifiedSMGName);
  }

  private static String getUniqueFunctionName(CFunctionDeclaration pDeclaration) {

    StringBuilder functionName = new StringBuilder(pDeclaration.getQualifiedName());

    for (CParameterDeclaration parameterDcl : pDeclaration.getParameters()) {
      functionName.append("_");
      functionName.append(CharMatcher.anyOf("* ").replaceFrom(parameterDcl.toASTString(), "_"));
    }

    return "__" + functionName;
  }

  public boolean executeHeapAbstraction(Set<SMGAbstractionBlock> blocks)
      throws SMGInconsistentException {
    final SMGAbstractionManager manager;
    boolean usesHeapInterpolation = true; // TODO do we need this flag?
    if (usesHeapInterpolation) {
      manager = new SMGAbstractionManager(logger, heap, this, blocks, 2, 2, 2);
    } else {
      manager = new SMGAbstractionManager(logger, heap, this, blocks, 2, 2, 3);
    }
    boolean change = manager.execute();
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return change;
  }

  public Optional<SMGEdgeHasValue> forget(SMGMemoryPath location) {
    return heap.forget(location);
  }

  public void clearValues() {
    heap.clearValues();
  }

  public void writeUnknownValueInUnknownField(SMGObject target) {
    heap.getHVEdges(SMGEdgeHasValueFilter.objectFilter(target)).forEach(heap::removeHasValueEdge);
  }

  public void clearObjects() {
    heap.clearObjects();
  }

  public SMGAbstractionCandidate executeHeapAbstractionOneStep(Set<SMGAbstractionBlock> pResult)
      throws SMGInconsistentException {
    SMGAbstractionManager manager = new SMGAbstractionManager(logger, heap, this, pResult, 2, 2, 2);
    SMGAbstractionCandidate result = manager.executeOneStep();
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return result;
  }

  public boolean forgetNonTrackedHve(Set<SMGMemoryPath> pMempaths) {

    Set<SMGEdgeHasValue> trackedHves = new HashSet<>();
    Set<SMGValue> trackedValues = new HashSet<>();
    trackedValues.add(SMGZeroValue.INSTANCE);

    for (SMGMemoryPath path : pMempaths) {
      Optional<SMGEdgeHasValue> hve = heap.getHVEdgeFromMemoryLocation(path);

      if (hve.isPresent()) {
        trackedHves.add(hve.orElseThrow());
        trackedValues.add(hve.orElseThrow().getValue());
      }
    }

    boolean change = false;

    for (SMGEdgeHasValue edge : heap.getHVEdges()) {

      // TODO Robust heap abstraction?
      if (edge.getObject().isAbstract()) {
        trackedValues.add(edge.getValue());
        continue;
      }

      if (!trackedHves.contains(edge)) {
        heap.removeHasValueEdge(edge);
        change = true;
      }
    }

    if (change) {
      for (SMGValue value : heap.getValues()) {
        if (!trackedValues.contains(value)) {
          heap.removePointsToEdge(value);
          heap.removeValue(value);
          change = true;
        }
      }
    }

    return change;
  }

  public void forget(SMGEdgeHasValue pHveEdge) {
    heap.removeHasValueEdge(pHveEdge);
  }

  public void remember(SMGEdgeHasValue pHveEdge) {
    heap.addHasValueEdge(pHveEdge);
  }

  @Override
  public Map<MemoryLocation, SMGRegion> getStackVariables() {

    Map<MemoryLocation, SMGRegion> result = new LinkedHashMap<>();

    for (Entry<String, SMGRegion> variableEntry : heap.getGlobalObjects().entrySet()) {
      String variableName = variableEntry.getKey();
      SMGRegion reg = variableEntry.getValue();
      result.put(MemoryLocation.parseExtendedQualifiedName(variableName), reg);
    }

    for (CLangStackFrame frame : heap.getStackFrames()) {
      String functionName = frame.getFunctionDeclaration().getName();

      for (Entry<String, SMGRegion> variableEntry : frame.getVariables().entrySet()) {
        String variableName = variableEntry.getKey();
        SMGRegion reg = variableEntry.getValue();
        result.put(MemoryLocation.forLocalVariable(functionName, variableName), reg);
      }
    }

    return result;
  }

  public boolean forgetNonTrackedStackVariables(Set<MemoryLocation> pTrackedStackVariables) {

    boolean change = false;

    for (String variable : heap.getGlobalObjects().keySet()) {
      MemoryLocation globalVar = MemoryLocation.parseExtendedQualifiedName(variable);
      if (!pTrackedStackVariables.contains(globalVar)) {
        heap.removeGlobalVariableAndEdges(variable);
        change = true;
      }
    }

    for (CLangStackFrame frame : heap.getStackFrames()) {
      String functionName = frame.getFunctionDeclaration().getName();
      for (String variable : frame.getVariables().keySet()) {
        MemoryLocation var = MemoryLocation.forLocalVariable(functionName, variable);
        if (!pTrackedStackVariables.contains(var)) {
          heap.forgetFunctionStackVariable(var, false);
          change = true;
        }
      }
    }

    return change;
  }

  /**
   * remove a named variable from the stack (function scope or global). Remove all edges from and to
   * it.
   *
   * <p>Does not prune the SMG for unreachable objects, needs to be done separately.
   */
  public SMGStateInformation forgetStackVariable(MemoryLocation pMemoryLocation) {
    return heap.forgetStackVariable(pMemoryLocation);
  }

  public void remember(
      MemoryLocation pMemoryLocation, SMGRegion pRegion, SMGStateInformation pInfo) {
    heap.remember(pMemoryLocation, pRegion, pInfo);
  }

  public void unknownWrite() {
    if (!isTrackErrorPredicatesEnabled()) {
      heap.clearValues();
    }
  }

  @Override
  public String toDOTLabel() {
    // same as "toString", but without the heap to reduce the amount of text.
    String parent =
        getPredecessorId() == 0
            ? "no parent, initial state"
            : "parent [" + getPredecessorId() + "]";
    return String.format("[%d] with %s", getId(), parent);
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public UnmodifiableCLangSMG getHeap() {
    return heap;
  }

  @Override
  public Set<Entry<SMGKnownSymbolicValue, SMGKnownExpValue>> getExplicitValues() {
    return Collections.unmodifiableSet(explicitValues.entrySet());
  }
}
