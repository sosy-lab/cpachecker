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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoin;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

import com.google.common.collect.Iterables;

public class SMGState implements AbstractQueryableState, Targetable {
  static boolean targetMemoryErrors = true;
  static boolean unknownOnUndefined = true;

  static private final AtomicInteger id_counter = new AtomicInteger(0);

  private final Map<SMGKnownSymValue, SMGKnownExpValue> explicitValues = new HashMap<>();
  private CLangSMG heap;
  private final LogManager logger;
  private SMGState predecessor;
  private final int id;

  private static SMGRuntimeCheck runtimeCheckLevel = SMGRuntimeCheck.NONE;

  //TODO These flags are not enough, they should contain more about the nature of the error.
  private boolean invalidWrite = false;
  private boolean invalidRead = false;
  private boolean invalidFree = false;

  /*
   * If a property is violated by this state, this member is set
   */
  private ViolatedProperty violatedProperty = null;

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
    if (targetMemoryErrors) {
      logger.log(Level.WARNING, pMessage);
    } else if (pUndefinedBehavior) {
      logger.log(Level.WARNING, pMessage );
      logger.log(Level.WARNING, "Non-target undefined behavior detected. The verification result is unreliable.");
    }
  }

  static public void setTargetMemoryErrors(boolean pV) {
    targetMemoryErrors = pV;
  }

  static public void setUnknownOnUndefined(boolean pV) {
    unknownOnUndefined = pV;
  }

  /**
   * Constructor.
   *
   * Keeps consistency: yes
   *
   * @param pLogger A logger to log any messages
   * @param pMachineModel A machine model for the underlying SMGs
   */
  public SMGState(LogManager pLogger, MachineModel pMachineModel) {
    heap = new CLangSMG(pMachineModel);
    logger = pLogger;
    predecessor = null;
    id = id_counter.getAndIncrement();
  }

  /**
   * Copy constructor.
   *
   * Keeps consistency: yes
   *
   * @param pOriginalState Original state. Will be the predecessor of the
   * new state
   * @throws SMGInconsistentException
   */
  public SMGState(SMGState pOriginalState) {
    heap = new CLangSMG(pOriginalState.heap);
    logger = pOriginalState.logger;
    predecessor = pOriginalState.predecessor;
    id = id_counter.getAndIncrement();
    explicitValues.putAll(pOriginalState.explicitValues);
  }

  /**
   * Sets a level of runtime checks performed.
   *
   * Keeps consistency: yes
   *
   * @param pLevel One of {@link SMGRuntimeCheck.NONE},
   * {@link SMGRuntimeCheck.HALF} or {@link SMGRuntimeCheck.FULL}
   * @throws SMGInconsistentException
   */
  static final public void setRuntimeCheck(SMGRuntimeCheck pLevel) {
    runtimeCheckLevel = pLevel;
  }

  /**
   * Constant.
   *
   * @param pSMGState A state to set as a predecessor.
   * @throws SMGInconsistentException
   */
  final public void setPredecessor(SMGState pSMGState) throws SMGInconsistentException {
    predecessor = pSMGState;
    performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  /**
   * Makes SMGState create a new object and put it into the global namespace
   *
   * Keeps consistency: yes
   *
   * @param pType Type of the new object
   * @param pVarName Name of the global variable
   * @return Newly created object
   *
   * @throws SMGInconsistentException when resulting SMGState is inconsistent
   * and the checks are enabled
   */
  public SMGObject addGlobalVariable(CType pType, String pVarName) throws SMGInconsistentException {
    int size = heap.getMachineModel().getSizeof(pType);
    SMGRegion new_object = new SMGRegion(size, pVarName);

    heap.addGlobalObject(new_object);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return new_object;
  }
  /**
   * Makes SMGState create a new object and put it into the current stack
   * frame.
   *
   * Keeps consistency: yes
   *
   * @param pType Type of the new object
   * @param pVarName Name of the local variable
   * @return Newly created object
   *
   * @throws SMGInconsistentException when resulting SMGState is inconsistent
   * and the checks are enabled
   */
  public SMGObject addLocalVariable(CType pType, String pVarName) throws SMGInconsistentException {
    int size = heap.getMachineModel().getSizeof(pType);
    SMGRegion new_object = new SMGRegion(size, pVarName);

    heap.addStackObject(new_object);
    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return new_object;
  }

  /**
   * Adds a new frame for the function.
   *
   * Keeps consistency: yes
   *
   * @param pFunctionDefinition A function for which to create a new stack frame
   * @throws SMGInconsistentException
   */
  public void addStackFrame(CFunctionDeclaration pFunctionDefinition) throws SMGInconsistentException {
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
  final public int getId() {
    return id;
  }

  /**
   * Constant.
   * .
   * @return The predecessor state, i.e. one from which this one was copied
   */
  final public SMGState getPredecessor() {
    return predecessor;
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
   * @throws SMGInconsistentException
   */
  final public void performConsistencyCheck(SMGRuntimeCheck pLevel) throws SMGInconsistentException {
    if (SMGState.runtimeCheckLevel.isFinerOrEqualThan(pLevel)) {
      if ( ! CLangSMGConsistencyVerifier.verifyCLangSMG(logger, heap) ) {
        throw new SMGInconsistentException("SMG was found inconsistent during a check");
      }
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
   * Returns a DOT representation of the SMGState with explicit Values
   * inserted where possible.
   *
   * @param pName A name of the graph.
   * @param pLocation A location in the program.
   * @param pExplicitState
   * @return String containing a DOT graph corresponding to the SMGState.
   */
  public String toDot(String pName, String pLocation, ExplicitState pExplicitState) {
    SMGExplicitPlotter plotter = new SMGExplicitPlotter(pExplicitState, this);
    return plotter.smgAsDot(heap, "Explicit_"+ pName, pLocation);
  }

  /**
   * @return A string representation of the SMGState.
   */
  @Override
  public String toString() {
    if ( getPredecessor() != null) {
      return "SMGState [" + getId() + "] <-- parent [" + getPredecessor().getId() + "]\n" + heap.toString();
    } else {
      return "SMGState [" + getId() + "] <-- no parent, initial state\n" + heap.toString();
    }
  }

  /**
   * Returns a Points-To edge leading from a value.
   *
   * Constant.
   *
   * @param pValue A value for which to return the Points-To edge
   * @return A Points-To edge leading from the passed value. The value needs to be
   * a pointer, i.e. it needs to have that edge. If it does not have it, the method raises
   * an exception.
   *
   * @throws SMGInconsistentException When the value passed does not have a Points-To edge.
   */
  public SMGEdgePointsTo getPointerFromValue(Integer pValue) throws SMGInconsistentException {
    if (heap.isPointer(pValue)) {
      return heap.getPointer(pValue);
    }

    throw new SMGInconsistentException("Asked for a Points-To edge for a non-pointer value");
  }

  /**
   * Checks, if a symbolic value is an address.
   *
   * Constant.
   *
   * @param pValue A value for which to return the Points-To edge
   * @return True, if the smg contains a {@link SMGEdgePointsTo} edge
   * with {@link pValue} as source, false otherwise.
   *
   */
  public boolean isPointer(Integer pValue) {

    return heap.isPointer(pValue);
  }

  /**
   * Read Value in field (object, type) of an Object.
   *
   * This method does not modify the state being read,
   * and is therefore safe to call outside of a
   * transfer relation context.
   *
   * @param pObject SMGObject representing the memory the field belongs to.
   * @param pOffset offset of field being read.
   * @param pType type of field
   * @return A Symbolic value, if found, otherwise null.
   */
  public Integer readValueNonModifiying(SMGObject pObject, int pOffset, CType pType) throws SMGInconsistentException {
    if (!heap.isObjectValid(pObject)) {
      return null;
    }

    SMGEdgeHasValue edge = new SMGEdgeHasValue(pType, pOffset, pObject, 0);

    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(pObject).filterAtOffset(pOffset);

    for (SMGEdgeHasValue object_edge : heap.getHVEdges(filter)) {
      if (edge.isCompatibleFieldOnSameObject(object_edge, heap.getMachineModel())) {
        performConsistencyCheck(SMGRuntimeCheck.HALF);
        return object_edge.getValue();
      }
    }

    if (heap.isCoveredByNullifiedBlocks(edge)) { return 0; }

    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return null;
  }

  /**
   * Read Value in field (object, type) of an Object.
   *
   * @param pObject SMGObject representing the memory the field belongs to.
   * @param pOffset offset of field being read.
   * @param pType type of field
   * @return
   * @throws SMGInconsistentException
   */
  public Integer readValue(SMGObject pObject, int pOffset, CType pType) throws SMGInconsistentException {
    if (!heap.isObjectValid(pObject)) {
      setInvalidRead();
      return null;
    }

    return readValueNonModifiying(pObject, pOffset, pType);
  }

  public void setInvalidRead() {
    invalidRead  = true;
  }

  /**
   * Write a value into a field (offset, type) of an Object.
   * Additionally, this method writes a points-to edge into the
   * SMG, if the given symbolic value points to an address, and
   *
   *
   * @param object SMGObject representing the memory the field belongs to.
   * @param offset offset of field written into.
   * @param type type of field written into.
   * @param value value to be written into field.
   * @param machineModel Currently used Machine Model
   * @throws SMGInconsistentException
   */
  public SMGEdgeHasValue writeValue(SMGObject pObject, int pOffset,
      CType pType, SMGSymbolicValue pValue) throws SMGInconsistentException {

    int value;

    // If the value is not yet known by the SMG
    // create a unconstrained new symbolic value
    if (pValue.isUnknown()) {
      value = SMGValueFactory.getNewValue();
    } else {
      value = pValue.getAsInt();
    }

    // If the value represents an address, and the address is known,
    // add the necessary points-To edge.
    if (pValue instanceof SMGAddressValue) {
      if (!containsValue(value)) {
        SMGAddress address = ((SMGAddressValue) pValue).getAddress();

        if (!address.isUnknown()) {
          addPointsToEdge(
              address.getObject(),
              address.getOffset().getAsInt(),
              value);
        }
      }
    }

    return writeValue(pObject, pOffset, pType, value);
  }

  private void addPointsToEdge(SMGObject pObject, int pOffset, int pValue) {

    // If the value is not known by the SMG, add it.
    if(!containsValue(pValue)) {
      heap.addValue(pValue);
    }

    SMGEdgePointsTo pointsToEdge = new SMGEdgePointsTo(pValue, pObject, pOffset);
    heap.addPointsToEdge(pointsToEdge);

  }

  /**
   * Write a value into a field (offset, type) of an Object.
   *
   *
   * @param object SMGObject representing the memory the field belongs to.
   * @param offset offset of field written into.
   * @param type type of field written into.
   * @param value value to be written into field.
   * @param machineModel Currently used Machine Model
   * @throws SMGInconsistentException
   */
  private SMGEdgeHasValue writeValue(SMGObject pObject, int pOffset, CType pType, Integer pValue) throws SMGInconsistentException {
    // vgl Algorithm 1 Byte-Precise Verification of Low-Level List Manipulation FIT-TR-2012-04

    if (! heap.isObjectValid(pObject)) {
      //Attempt to write to invalid object
      setInvalidWrite();
      return null;
    }

    SMGEdgeHasValue new_edge = new SMGEdgeHasValue(pType, pOffset, pObject, pValue);

    // Check if the edge is  not present already
    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(pObject);

    Iterable<SMGEdgeHasValue> edges = heap.getHVEdges(filter);
    if (Iterables.contains(edges, new_edge)) {
      performConsistencyCheck(SMGRuntimeCheck.HALF);
      return new_edge;
    }

    // If the value is not in the SMG, we need to add it
    if ( ! heap.getValues().contains(pValue) ) {
      heap.addValue(pValue);
    }

    HashSet<SMGEdgeHasValue> overlappingZeroEdges = new HashSet<>();
    HashSet<SMGEdgeHasValue> toRemove = new HashSet<>();

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
          toRemove.add(hv);
        }


        //TODO This method of shrinking did not work for my benchmarks, investigate
        /*
        if (hv.getValue() == heap.getNullValue()) {
          if (hv.getOffset() < new_edge.getOffset()) {
            int prefixNullSize = new_edge.getOffset() - hv.getOffset();
            SMGEdgeHasValue prefixNull = new SMGEdgeHasValue(prefixNullSize, hv.getOffset(), pObject, heap.getNullValue());
            heap.addHasValueEdge(prefixNull);
          }

          int hvEnd = hv.getOffset() + hv.getSizeInBytes(heap.getMachineModel());
          int neEnd = new_edge.getOffset() + new_edge.getSizeInBytes(heap.getMachineModel());
          if (hvEnd > neEnd) {
            int postfixNullSize = hvEnd - neEnd;
            SMGEdgeHasValue postfixNull = new SMGEdgeHasValue(postfixNullSize, neEnd, pObject, heap.getNullValue());
            heap.addHasValueEdge(postfixNull);
          }
        }
        */
      }
    }

    for (SMGEdgeHasValue hv : toRemove) {
      heap.removeHasValueEdge(hv);
    }
    shrinkOverlappingZeroEdges(new_edge, overlappingZeroEdges);

    heap.addHasValueEdge(new_edge);
    performConsistencyCheck(SMGRuntimeCheck.HALF);

    return new_edge;
  }

  private void shrinkOverlappingZeroEdges(SMGEdgeHasValue pNew_edge,
      Set<SMGEdgeHasValue> pOverlappingZeroEdges) {

    SMGObject object = pNew_edge.getObject();
    int offset = pNew_edge.getOffset();

    boolean newEdgePointsToZero = pNew_edge.getValue() == 0;
    MachineModel maModel = heap.getMachineModel();
    int sizeOfType = pNew_edge.getSizeInBytes(maModel);

    // Shrink overlapping zero edges
    for (SMGEdgeHasValue zeroEdge : pOverlappingZeroEdges) {
      // If the new_edge points to zero, we can just remove them
      heap.removeHasValueEdge(zeroEdge);

      if (!newEdgePointsToZero) {

        int zeroEdgeOffset = zeroEdge.getOffset();

        int offset2 = offset + sizeOfType;
        int zeroEdgeOffset2 = zeroEdgeOffset + zeroEdge.getSizeInBytes(maModel);

        if (zeroEdgeOffset < offset) {
          SMGEdgeHasValue newZeroEdge = new SMGEdgeHasValue(offset - zeroEdgeOffset, zeroEdgeOffset, object, 0);
          heap.addHasValueEdge(newZeroEdge);
        }

        if (offset2 < zeroEdgeOffset2) {
          SMGEdgeHasValue newZeroEdge = new SMGEdgeHasValue(zeroEdgeOffset2 - offset2, offset2, object, 0);
          heap.addHasValueEdge(newZeroEdge);
        }
      }
    }
  }

  /**
   * Marks that an invalid write operation was performed on this smgState.
   *
   */
  public void setInvalidWrite() {
    invalidWrite = true;
  }

  /**
   * Computes the join of this abstract State and the reached abstract State.
   *
   * @param reachedState the abstract state this state will be joined to.
   * @return the join of the two states.
   */
  public SMGState join(SMGState reachedState) {
    // Not necessary if merge_SEP and stop_SEP is used.
    return null;
  }

  /**
   * Computes whether this abstract state is covered by the given abstract state.
   * A state is covered by another state, if the set of concrete states
   * a state represents is a subset of the set of concrete states the other
   * state represents.
   *
   * If this state contains a memory leak and the given does not. The given state
   * does not cover this state even if covering relation (join operation) claim that
   * this state is covered by the other state.
   *
   *
   * @param reachedState already reached state, that may cover this state already.
   * @return True, if this state is covered by the given state, false otherwise.
   * @throws SMGInconsistentException
   */
  public boolean isLessOrEqual(SMGState reachedState) throws SMGInconsistentException {
    SMGJoin join = new SMGJoin(reachedState.heap, heap);
    if (join.isDefined() &&
        (join.getStatus() == SMGJoinStatus.LEFT_ENTAIL || join.getStatus() == SMGJoinStatus.EQUAL)){

      // check memory leaks
      // if reached does NOT contain memory leak and this DOES
      //   this shouldn't be drop
      this.heap.pruneUnreachable();
      if (heap.hasMemoryLeaks()){
        reachedState.heap.pruneUnreachable();
        if (!reachedState.heap.hasMemoryLeaks()){
          return false;
        }
        // else return true
      }


      return true;
    }
    return false;
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
      case "has-leaks":
        if (heap.hasMemoryLeaks()) {
          //TODO: Give more information
          violatedProperty = ViolatedProperty.VALID_MEMTRACK;
          issueMemoryLeakMessage();
          return true;
        }
        return false;
      case "has-invalid-writes":
        if (invalidWrite) {
          //TODO: Give more information
          violatedProperty = ViolatedProperty.VALID_DEREF;
          issueInvalidWriteMessage();
          return true;
        }
        return false;
      case "has-invalid-reads":
        if (invalidRead) {
          //TODO: Give more information
          violatedProperty = ViolatedProperty.VALID_DEREF;
          issueInvalidReadMessage();
          return true;
        }
        return false;
      case "has-invalid-frees":
        if (invalidFree) {
          //TODO: Give more information
          violatedProperty = ViolatedProperty.VALID_FREE;
          issueInvalidFreeMessage();
          return true;
        }
        return false;
      default:
        throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
    }
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    return checkProperty(pProperty);
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    // TODO Auto-generated method stub

  }

  public void addGlobalObject(SMGRegion newObject) {
    heap.addGlobalObject(newObject);
  }

  public boolean isGlobal(String variable) {
    return  heap.getGlobalObjects().containsValue(heap.getObjectForVisibleVariable(variable));
  }

  public boolean isGlobal(SMGObject object) {
    return heap.getGlobalObjects().containsValue(object);
  }

  public boolean isHeapObject(SMGObject object) {
    return heap.getHeapObjects().contains(object);
  }

  public SMGEdgePointsTo addNewHeapAllocation(int pSize, String pLabel) throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(pSize, pLabel);
    int new_value = SMGValueFactory.getNewValue();
    SMGEdgePointsTo points_to = new SMGEdgePointsTo(new_value, new_object, 0);
    heap.addHeapObject(new_object);
    heap.addValue(new_value);
    heap.addPointsToEdge(points_to);

    performConsistencyCheck(SMGRuntimeCheck.HALF);
    return points_to;
  }

  public void setMemLeak() {
    heap.setMemoryLeak();
  }

 public boolean containsValue(int value) {
   return heap.getValues().contains(value);
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
  public Integer getAddress(SMGObject memory, int offset) {

    // TODO A better way of getting those edges, maybe with a filter
    // like the Has-Value-Edges

    Map<Integer, SMGEdgePointsTo> pointsToEdges = heap.getPTEdges();

    for (SMGEdgePointsTo edge : pointsToEdges.values()) {
      if (edge.getObject().equals(memory) && edge.getOffset() == offset) {
        return edge.getValue();
      }
    }

    return null;
  }

  /**
   * This method simulates a free invocation. It checks,
   * whether the call is valid, and invalidates the
   * Memory the given address points to.
   * The address (address, offset, smgObject) is the argument
   * of the free invocation. It does not need to be part of the SMG.
   *
   * @param address The symbolic Value of the address.
   * @param offset The offset of the address relative to the beginning of smgObject.
   * @param smgObject The memory the given Address belongs to.
   * @throws SMGInconsistentException
   */
  public void free(Integer address, Integer offset, SMGObject smgObject) throws SMGInconsistentException {

    if (!heap.isHeapObject(smgObject)) {
      // You may not free any objects not on the heap.
      setInvalidFree();
      return;
    }

    if(!(offset == 0)) {
      // you may not invoke free on any address that you
      // didn't get through a malloc invocation.
      setInvalidFree();
      return;
    }

    if (! heap.isObjectValid(smgObject)) {
      // you may not invoke free multiple times on
      // the same object
      setInvalidFree();
      return;
    }

    heap.setValidity(smgObject, false);
    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(smgObject);

    List<SMGEdgeHasValue> to_remove = new ArrayList<>();
    for (SMGEdgeHasValue edge : heap.getHVEdges(filter)) {
      to_remove.add(edge);
    }

    for (SMGEdgeHasValue edge : to_remove) {
      heap.removeHasValueEdge(edge);
    }

    performConsistencyCheck(SMGRuntimeCheck.HALF);
  }

  /**
   * Determine, whether the two given symbolic values are not equal.
   * If this method does not return true, the relation of these
   * symbolic values is unknown.
   *
   * @param value1 first symbolic value to be checked
   * @param value2 second symbolic value to be checked
   * @return true, if the symbolic values are known to be not equal, false, if it is unknown.
   * @throws SMGInconsistentException
   */
  public boolean isUnequal(int value1, int value2) {
    // TODO Neq Relation for more precise comparison

    if (isPointer(value1) && isPointer(value2)) {

      if (value1 != value2) {
        /* This is just a safety check,
        equal pointers should have equal symbolic values.*/
        SMGEdgePointsTo edge1;
        SMGEdgePointsTo edge2;
        try {
          edge1 = getPointerFromValue(value1);
          edge2 = getPointerFromValue(value2);
        } catch (SMGInconsistentException e) {
          throw new AssertionError(e.getMessage());
        }

        return edge1.getObject() != edge2.getObject() || edge1.getOffset() != edge2.getOffset();
      } else {
        return false;
      }
    } else {
      return heap.haveNeqRelation(Integer.valueOf(value1), Integer.valueOf(value2));
    }
  }

  /**
   * Drop the stack frame representing the stack of
   * the function with the given name
   *
   * @param functionName
   * @throws SMGInconsistentException
   */
  public void dropStackFrame() throws SMGInconsistentException {
    heap.dropStackFrame();
    performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  public void pruneUnreachable() throws SMGInconsistentException {
    heap.pruneUnreachable();
    //TODO: Explicit values pruning
    performConsistencyCheck(SMGRuntimeCheck.HALF);
  }

  /**
   *  Signals an invalid free call.
   */
  public void setInvalidFree() {
    invalidFree = true;
  }

  public Iterable<SMGEdgeHasValue> getHVEdges(SMGEdgeHasValueFilter pFilter) {
    return heap.getHVEdges(pFilter);
  }

  @Nullable
  public MemoryLocation resolveMemLoc(SMGAddress pValue, String pFunctionName) {
    SMGObject object = pValue.getObject();
    long offset = pValue.getOffset().getAsLong();

    if (isGlobal(object) || isHeapObject(object)) {
      return MemoryLocation.valueOf(object.getLabel(), offset);
    } else {
      return MemoryLocation.valueOf(pFunctionName, object.getLabel(), offset);
    }
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
   * @param pSourceRangeSize the size of the copy of source
   * @param pSourceRangeOffset insert the copy of source into target at this offset
   * @throws SMGInconsistentException thrown if the copying leads to an inconsistent SMG.
   */
  public void copy(SMGObject pSource, SMGObject pTarget, int pSourceRangeOffset, int pSourceRangeSize, int pTargetRangeOffset) throws SMGInconsistentException {

    int copyRange = pSourceRangeSize - pSourceRangeOffset;

    assert pSource.getSize() >= pSourceRangeSize;
    assert pSourceRangeOffset >= 0;
    assert pTargetRangeOffset >= 0;
    assert copyRange >= 0;
    assert copyRange <= pTarget.getSize();

    // If copy range is 0, do nothing
    if(copyRange == 0) {
      return;
    }

    int targetRangeSize = pTargetRangeOffset + copyRange;

    SMGEdgeHasValueFilter filterSource = SMGEdgeHasValueFilter.objectFilter(pSource);
    SMGEdgeHasValueFilter filterTarget = SMGEdgeHasValueFilter.objectFilter(pTarget);

    //Remove all Target edges in range
    Iterable<SMGEdgeHasValue> targetEdges = getHVEdges(filterTarget);

    for (SMGEdgeHasValue edge : targetEdges) {
      if (edge.overlapsWith(pTargetRangeOffset, targetRangeSize, heap.getMachineModel())) {
        heap.removeHasValueEdge(edge);
      }
    }

    // Shift the source edge offset depending on the target range offset
    int copyShift = pTargetRangeOffset - pSourceRangeOffset;

    for (SMGEdgeHasValue edge : getHVEdges(filterSource)) {
      if (edge.overlapsWith(pSourceRangeOffset, pSourceRangeSize, heap.getMachineModel())) {
        int offset = edge.getOffset() + copyShift;
        writeValue(pTarget, offset, edge.getType(), edge.getValue());
      }
    }

    performConsistencyCheck(SMGRuntimeCheck.FULL);
    //TODO Why do I do this here?
    heap.pruneUnreachable();
    performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  /**
   * Signals a dereference of a pointer or array
   *  which could not be resolved.
   */
  public void setUnknownDereference() {
    //TODO: This can actually be an invalid read too
    //      The flagging mechanism should be improved

    invalidWrite = true;
  }

  public SMGObject getNullObject() {
    return heap.getNullObject();
  }

  public void identifyEqualValues(SMGKnownSymValue pKnownVal1, SMGKnownSymValue pKnownVal2) {
    heap.mergeValues(pKnownVal1.getAsInt(), pKnownVal2.getAsInt());
  }

  public void identifyNonEqualValues(SMGKnownSymValue pKnownVal1, SMGKnownSymValue pKnownVal2) {
    heap.addNeqRelation(pKnownVal1.getAsInt(), pKnownVal2.getAsInt());
  }

  @Override
  public boolean isTarget() {
    return violatedProperty != null;
  }

  @Override
  public ViolatedProperty getViolatedProperty() throws IllegalStateException {
    return violatedProperty;
  }

  public void putExplicit(SMGKnownSymValue pKey, SMGKnownExpValue pValue) {
    explicitValues.put(pKey, pValue);
  }

  public void clearExplicit(SMGKnownSymValue pKey) {
    explicitValues.remove(pKey);
  }

  public SMGExplicitValue getExplicit(SMGKnownSymValue pKey) {
    if (explicitValues.containsKey(pKey)) {
      return explicitValues.get(pKey);
    }
    return SMGUnknownValue.getInstance();
  }
  public void attemptAbstraction() {
    SMGAbstractionManager manager = new SMGAbstractionManager(heap);
    heap = manager.execute();
  }
}
