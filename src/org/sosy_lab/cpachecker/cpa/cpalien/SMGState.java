 /*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien;

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class SMGState implements AbstractQueryableState {
  static private int id_counter = 0;

  private final CLangSMG heap;
  private final LogManager logger;
  private SMGState predecessor;
  private final int id;

  private SMGRuntimeCheck runtimeCheckLevel;

  private boolean invalidWrite = false;
  private boolean invalidRead = false;

  private boolean invalidFree = false;

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
    id = id_counter++;
    runtimeCheckLevel = SMGRuntimeCheck.NONE;
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
    runtimeCheckLevel = pOriginalState.runtimeCheckLevel;
    id = id_counter++;
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
  final public void setRuntimeCheck(SMGRuntimeCheck pLevel) throws SMGInconsistentException {
    runtimeCheckLevel = pLevel;
    if (pLevel.isFinerOrEqualThan(SMGRuntimeCheck.HALF)) {
      CLangSMG.setPerformChecks(true, logger);
    }
    else {
      CLangSMG.setPerformChecks(false, logger);
    }
    this.performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  /**
   * Constant.
   *
   * @param pSMGState A state to set as a predecessor.
   * @throws SMGInconsistentException
   */
  final public void setPredecessor(SMGState pSMGState) throws SMGInconsistentException {
    predecessor = pSMGState;
    this.performConsistencyCheck(SMGRuntimeCheck.FULL);
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
    SMGObject new_object = new SMGObject(size, pVarName);

    heap.addStackObject(new_object);
    this.performConsistencyCheck(SMGRuntimeCheck.HALF);
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
    this.performConsistencyCheck(SMGRuntimeCheck.HALF);
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
    return this.heap.getObjectForVisibleVariable(pVariableName);
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
    if (this.runtimeCheckLevel.isFinerOrEqualThan(pLevel)) {
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
    return plotter.smgAsDot(heap, pName, pLocation);
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
    if ( this.getPredecessor() != null) {
      return "SMGState [" + this.getId() + "] <-- parent [" + this.getPredecessor().getId() + "]\n" + heap.toString();
    } else {
      return "SMGState [" + this.getId() + "] <-- no parent, initial state\n" + heap.toString();
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
   * @return A Points-To edge leading from the passed value. The value needs to be
   * a pointer, i.e. it needs to have that edge. If it does not have it, the method raises
   * an exception.
   *
   * @throws SMGInconsistentException When the value passed does not have a Points-To edge.
   */
  public boolean isPointer(Integer pValue) {

    return heap.isPointer(pValue);
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
    if (! this.heap.isObjectValid(pObject)) {
      this.setInvalidRead();
      return null;
    }

    SMGEdgeHasValue edge = new SMGEdgeHasValue(pType, pOffset, pObject, 0);

    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();
    filter.filterByObject(pObject);
    filter.filterAtOffset(pOffset);
    Set<SMGEdgeHasValue> edges = heap.getHVEdges(filter);

    for (SMGEdgeHasValue object_edge : edges) {
      if (edge.isCompatibleFieldOnSameObject(object_edge, heap.getMachineModel())) {
        this.performConsistencyCheck(SMGRuntimeCheck.HALF);
        return object_edge.getValue();
      }
    }

    // TODO: Nullified blocks coverage interpretation
    this.performConsistencyCheck(SMGRuntimeCheck.HALF);
    return null;
  }

  public void setInvalidRead() {
    this.invalidRead  = true;
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
    // add the neccessary points-To edge.
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
  public SMGEdgeHasValue writeValue(SMGObject pObject, int pOffset, CType pType, Integer pValue) throws SMGInconsistentException {
    // vgl Algorithm 1 Byte-Precise Verification of Low-Level List Manipulation FIT-TR-2012-04
    //TODO Does this method need to be public?

    if (pValue == null) {
      pValue = heap.getNullValue();
    }

    if (! this.heap.isObjectValid(pObject)) {
      //Attempt to write to invalid object
      this.setInvalidWrite();
      return null;
    }

    SMGEdgeHasValue new_edge = new SMGEdgeHasValue(pType, pOffset, pObject, pValue);

    // Check if the edge is  not present already
    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(pObject);

    Set<SMGEdgeHasValue> edges = heap.getHVEdges(filter);
    if (edges.contains(new_edge)) {
      this.performConsistencyCheck(SMGRuntimeCheck.HALF);
      return new_edge;
    }

    // If the value is not in the SMG, we need to add it
    if ( ! heap.getValues().contains(pValue) ) {
      heap.addValue(pValue);
    }

    // We need to remove all non-zero overlapping edges
    for (SMGEdgeHasValue hv : edges) {
      if (/*hv.getValue() != heap.getNullValue() &&*/ new_edge.overlapsWith(hv, heap.getMachineModel())) {
        // as long as we do not shrink zero edges, remove
        heap.removeHasValueEdge(hv);
      }
    }

    //TODO: Shrink overlapping zero edges
    heap.addHasValueEdge(new_edge);
    this.performConsistencyCheck(SMGRuntimeCheck.HALF);

    return new_edge;
  }

  public void setInvalidWrite() {
    this.invalidWrite = true;
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
   *
   * @param reachedState already reached state, that may cover this state already.
   * @return True, if this state is covered by the given state, false otherwise.
   */
  public boolean isLessOrEqual(SMGState reachedState) {

    if(reachedState == this) {
      return true;
    }

    boolean result = heap.isLessOrEqual(reachedState.heap);
    return result;
  }

  @Override
  public String getCPAName() {
    return "CPAlien";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    // SMG Properties:
    // has-leaks:boolean

    switch (pProperty) {
      case "has-leaks":
        if (heap.hasMemoryLeaks()) {
          //TODO: Give more information
          this.logger.log(Level.SEVERE, "Memory leak found");
          return true;
        }
        return false;
      case "has-invalid-writes":
        if (this.invalidWrite) {
          //TODO: Give more information
          this.logger.log(Level.SEVERE, "Invalid write found");
          return true;
        }
        return false;
      case "has-invalid-reads":
        if (this.invalidRead) {
          //TODO: Give more information
          this.logger.log(Level.SEVERE, "Invalid read found");
          return true;
        }
        return false;
      case "has-invalid-frees":
        if (this.invalidFree) {
          //TODO: Give more information
          this.logger.log(Level.SEVERE, "Invalid free found");
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

  public void addGlobalObject(SMGObject newObject) {
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
    SMGObject new_object = new SMGObject(pSize, pLabel);
    int new_value = SMGValueFactory.getNewValue();
    SMGEdgePointsTo points_to = new SMGEdgePointsTo(new_value, new_object, 0);
    heap.addHeapObject(new_object);
    heap.addValue(new_value);
    heap.addPointsToEdge(points_to);

    this.performConsistencyCheck(SMGRuntimeCheck.HALF);
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

    if (! this.heap.isObjectValid(smgObject)) {
      // you may not invoke free multiple times on
      // the same object
      this.setInvalidFree();
      return;
    }

    heap.setValidity(smgObject, false);
    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(smgObject);

    for (SMGEdgeHasValue edge : heap.getHVEdges(filter)) {
      heap.removeHasValueEdge(edge);
    }
    this.performConsistencyCheck(SMGRuntimeCheck.HALF);
  }

  /**
   * Set the two given symbolic values to be not equal.
   *
   * @param value1 the first symbolic value.
   * @param value2 the second symbolic value.
   */
  public void setUnequal(int value1, int value2) {
    // TODO Auto-generated method stub
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
  public boolean isUnequal(int value1, int value2) throws SMGInconsistentException {
    // TODO Neq Relation for more precise comparison

    if (isPointer(value1) && isPointer(value2)) {

      if (value1 != value2) {
        /* This is just a safety check,
        equal pointers should have equal symbolic values.*/
        SMGEdgePointsTo edge1 = getPointerFromValue(value1);
        SMGEdgePointsTo edge2 = getPointerFromValue(value2);

        return edge1.getObject() != edge2.getObject() || edge1.getOffset() != edge2.getOffset();
      } else {
        return false;
      }
    }

    return false;
  }

  /**
   * Drop the stack frame representing the stack of
   * the function with the given name
   *
   * @param functionName
   * @throws SMGInconsistentException
   */
  public void dropStackFrame(String functionName) throws SMGInconsistentException {
    this.heap.dropStackFrame();
    this.performConsistencyCheck(SMGRuntimeCheck.FULL);
    this.heap.pruneUnreachable();
    this.performConsistencyCheck(SMGRuntimeCheck.HALF);
  }

  /**
   * Creates a new SMGObject representing Memory.
   *
   * @param size the size in Bytes of the newly created SMGObject.
   * @param label a label representing this SMGObject as a String.
   * @return A newly created SMGObject representing Memory.
   */
  public SMGObject createObject(int size, String label) {
    return new SMGObject(size, label);
  }

  /**
   *  Signals an invalid free call.
   */
  public void setInvalidFree() {
    this.invalidFree = true;
  }

  public Set<SMGEdgeHasValue> getHVEdges(SMGEdgeHasValueFilter pFilter) {
    return this.heap.getHVEdges(pFilter);
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
   * @param pTargetRangeOffset
   * @param pSourceRangeSize
   * @param pSourceRangeOffset
   * @throws SMGInconsistentException
   */
  public void copy(SMGObject pSource, SMGObject pTarget, int pSourceRangeOffset, int pSourceRangeSize, int pTargetRangeOffset) throws SMGInconsistentException {

    int copyRange = pSourceRangeSize - pSourceRangeOffset;

    assert pSource.getSizeInBytes() >= pSourceRangeSize;
    assert pSourceRangeOffset >= 0;
    assert pTargetRangeOffset >= 0;
    assert copyRange >= 0;
    assert copyRange <= pTarget.getSizeInBytes();

    // If copy range is 0, do nothing
    if(copyRange == 0) {
      return;
    }

    int targetRangeSize = pTargetRangeOffset + copyRange;


    SMGEdgeHasValueFilter filterSource = new SMGEdgeHasValueFilter();
    filterSource.filterByObject(pSource);
    SMGEdgeHasValueFilter filterTarget = new SMGEdgeHasValueFilter();
    filterTarget.filterByObject(pTarget);

    //Remove all Target edges in range
    Set<SMGEdgeHasValue> targetEdges = getHVEdges(filterTarget);

    for (SMGEdgeHasValue edge : targetEdges) {
      if (edge.overlapsWith(pTargetRangeOffset, targetRangeSize, heap.getMachineModel())) {
        heap.removeHasValueEdge(edge);
      }
    }

    // Copy all Source edges
    Set<SMGEdgeHasValue> sourceEdges = getHVEdges(filterSource);

    // Shift the source edge offset depending on the target range offset
    int copyShift = pTargetRangeOffset - pSourceRangeOffset;

    for (SMGEdgeHasValue edge : sourceEdges) {
      if (edge.overlapsWith(pSourceRangeOffset, pSourceRangeSize, heap.getMachineModel())) {
        int offset = edge.getOffset() + copyShift;
        writeValue(pTarget, offset, edge.getType(), edge.getValue());
      }
    }

    performConsistencyCheck(runtimeCheckLevel);
    heap.pruneUnreachable();
    performConsistencyCheck(runtimeCheckLevel);
  }
}
