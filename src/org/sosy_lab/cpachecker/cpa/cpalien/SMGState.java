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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class SMGState implements AbstractQueryableState {
  static private int id_counter = 0;

  private final CLangSMG heap;
  private final LogManager logger;
  private SMGState predecessor;
  private final int id;

  private SMGRuntimeCheck runtimeCheckLevel;

  public SMGState(LogManager pLogger, MachineModel pMachineModel) {
    heap = new CLangSMG(pMachineModel);
    logger = pLogger;
    predecessor = null;
    id = id_counter++;
    runtimeCheckLevel = SMGRuntimeCheck.NONE;
  }

  public SMGState(SMGState originalState) {
    heap = new CLangSMG(originalState.heap);
    logger = originalState.logger;
    predecessor = originalState.predecessor;
    runtimeCheckLevel = originalState.runtimeCheckLevel;
    id = id_counter++;
  }

  public void setRuntimeCheck(SMGRuntimeCheck pLevel) {
    runtimeCheckLevel = pLevel;
    if (pLevel.isFinerOrEqualThan(SMGRuntimeCheck.HALF)) {
      CLangSMG.setPerformChecks(true);
    }
    else {
      CLangSMG.setPerformChecks(false);
    }
  }

  public int getId() {
    return id;
  }

  public SMGState getPredecessor() {
    return predecessor;
  }

  public void setPredecessor(SMGState pSMGState) {
    predecessor = pSMGState;
  }

  void addStackObject(SMGObject obj) {
    heap.addStackObject(obj);
  }

  public void addValue(int pValue) {
    heap.addValue(Integer.valueOf(pValue));
  }

  /**
   * Get memory of variable with the given Name. This method is used for
   * the temporary function return variable.
   *
   * @param variableName
   * @return
   */
  public SMGObject getObjectForVisibleVariable(String variableName) {
    return this.heap.getObjectForVisibleVariable(variableName);
  }

  public void addHVEdge(SMGEdgeHasValue pNewEdge) {
    heap.addHasValueEdge(pNewEdge);
  }

  public void performConsistencyCheck(SMGRuntimeCheck pLevel) throws SMGInconsistentException {
    if (this.runtimeCheckLevel.isFinerOrEqualThan(pLevel)) {
      if ( ! CLangSMGConsistencyVerifier.verifyCLangSMG(logger, heap) ){
        throw new SMGInconsistentException("SMG was found inconsistent during a check");
      }
    }
  }

  public String toDot(String name, String location) {
    SMGPlotter plotter = new SMGPlotter();
    return plotter.smgAsDot(heap, name, location);
  }

  @Override
  public String toString() {
    if ( this.getPredecessor() != null) {
      return "SMGState [" + this.getId() + "] <-- parent [" + this.getPredecessor().getId() + "]\n" + heap.toString();
    } else {
      return "SMGState [" + this.getId() + "] <-- no parent, initial state\n" + heap.toString();
    }
  }

  public void addStackFrame(CFunctionDeclaration pFunctionDefinition) {
    heap.addStackFrame(pFunctionDefinition);
  }

  /**
   * Read Value in field (object, type) of an Object.
   *
   * @param object SMGObject representing the memory the field belongs to.
   * @param offset offset of field being read.
   * @param type type of field written into.
   * @return
   */
  public Integer readValue(SMGObject object, int offset, CType type) {
    // TODO Auto-generated method stub
    return null;
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
   */
  public void writeValue(SMGObject object, int offset, CType type, Integer value, MachineModel machineModel) {
    // vgl Algorithm 1 Byte-Precise Verification of Low-Level List Manipulation FIT-TR-2012-04

  }

  /**
   * Computes the next unused identifier for a symbolic Value.
   *
   * @return the next unused symbolic Value.
   */
  public Integer nextFreeValue() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Computes the join of this abstract State and the reached abstract State.
   *
   * @param reachedState the abstract state this state will be joined to.
   * @return the join of the two states.
   */
  public SMGState join(SMGState reachedState) {
    // Not neccessary if merge_SEP and stop_SEP is used.
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
    // TODO Auto-generated method stub
    return false;
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
        return heap.hasMemoryLeaks();
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

  public void addHeapObject(SMGObject pNewObject) {
    heap.addHeapObject(pNewObject);
  }

  public void addPVEdge(SMGEdgePointsTo pNewPVEdge) {
    heap.addPointsToEdge(pNewPVEdge);
  }

  public void setMemLeak() {
    heap.setMemoryLeak();
  }

 public void insertNewHasValueEdge(SMGEdgeHasValue pNewEdge) {
   heap.addHasValueEdge(pNewEdge);
 }

 public boolean containsValue(int value) {
   return heap.getValues().contains(value);
 }

  /**
   * Get address of the given memory with the given offset.
   *
   * @param memory get address belonging to this memory.
   * @param offset get address with this offset relative to the beginning of the memory.
   * @return Address of the given field, or null, if such an address
   * does not yet exist in the SMG.
   */
  public Integer getAddress(SMGObject memory, Integer offset) {
    // TODO Auto-generated method stub
    return null;
  }


  /**
   * Get the SMGObject representing the Memory the given address points to.
   *
   * @param address the address belonging to the memory to be returned.
   * @return SMGObject representing the Memory this address points to, or null,
   * if the memory this address belongs to is unkown.
   */
  public SMGObject getMemoryOfAddress(Integer address) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Adds a new Address represented by a symbolic value and an offset
   * to the SMG. The address points to the given object. Additionally,
   * adds the necessary points-to edge to the given object representing Memory
   * with the given offset.
   *
   * @param object An existing object in the SMG this address points to.
   * @param offset the offset specifying the byte of memory this address points to.
   * @param address the symbolic Value representing the address.
   * @return
   */
  public Integer addAddress(SMGObject object, Integer offset, Integer address) {
    // TODO Auto-generated method stub
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
   */
  public void free(Integer address, Integer offset, SMGObject smgObject) {
    // TODO Auto-generated method stub
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
   */
  public boolean isUnequal(int value1, int value2) {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * Get the offset of this address.
   *
   * @param address An address pointing to an object in the SMG.
   * @return the offset of the given address or null, if the given value is
   *   not an address or the object its pointing to is unknown.
   *
   */
  public Integer getOffset(Integer address) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Drop the stack frame representing the stack of
   * the function with the given name
   *
   * @param functionName
   */
  public void dropStackFrame(String functionName) {
    // TODO Auto-generated method stub
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
   * Assigns the given symbolic value an explicit value.
   *
   * @param symbolicValue the symbolic value to be assigned.
   * @param explicitValue the explicit value which will be assigned to the given symbolic value.
   */
  public void assignExplicitValue(Integer symbolicValue, Integer explicitValue) {
    // TODO Auto-generated method stub
  }

  /**
   * Return true, if the explicit value of the given symbolic value is known
   *
   * @param symbolicValue Search for the explicit value of the given symbolic value.
   * @return true if the explicit value of the given symbolic one is known, else false.
   */
  public boolean isExplicitValueKnown(Integer symbolicValue) {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * Return the explicit value assigned to the given symbolic value,
   * or null, if the symbolic value was not assigned a explicit value.
   *
   * @param symbolicValue get the explicit value assigned to this symbolic value.
   * @return the explicit value assigned to the given symbolic value, or null.
   */
  public Integer getExplicitValue(Integer symbolicValue) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Returns true, if the given explicit value is assigned to
   * a symbolic value.
   *
   * @param explicitValue the explicit Value to be searched for an assignment,
   *
   * @return true if the given explicit value is assigned to
   * a symbolic value, else false.
   */
  public boolean isSymbolicValueKnown(int explicitValue) {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * Return the symbolic value assigned to a explicit value,
   * or null, if the explicit value was not assigned  one.
   *
   * @param explicitValue get the symbolic value assigned to this explicit value.
   * @return the symbolic value assigned to the given explicit value, or null.
   */
  public Integer getSymbolicValue(int explicitValue) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   *  Signals an invalid free call.
   */
  public void setInvalidFree() {
    // TODO Auto-generated method stub
  }

  /**
   * Signals a dereference of a pointer or array
   *  which could not be resolved.
   */
  public void setUnkownDereference() {
    // TODO Auto-generated method stub
  }
}
