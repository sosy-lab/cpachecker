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

import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class SMGState implements AbstractQueryableState {
  private final CLangSMG heap;
  private final LogManager logger;

  public SMGState(LogManager pLogger){
    heap = new CLangSMG();
    logger = pLogger;
  }

  public SMGState(SMGState originalState){
    heap = new CLangSMG(originalState.heap);
    logger = originalState.logger;
  }

  void addStackObject(SMGObject obj){
    try {
      heap.addStackObject(obj);
    } catch (IllegalAccessException e) {
      logger.log(Level.SEVERE, e.getMessage());
      e.printStackTrace();
    }
  }

  public void addValue(int pValue) {
    heap.addValue(Integer.valueOf(pValue));
  }

  public SMGObject getObjectForVisibleVariable(CIdExpression pVariableName) {
    return heap.getObjectForVisibleVariable(pVariableName);
  }

  public void addHVEdge(SMGEdgeHasValue pNewEdge) {
    heap.addHasValueEdge(pNewEdge);
  }

  public void performConsistencyCheck(){
    CLangSMGConsistencyVerifier.verifyCLangSMG(logger, heap);
  }

  public String toDot(String name){
    SMGPlotter plotter = new SMGPlotter();
    return plotter.smgAsDot(heap, name);
  }

  @Override
  public String toString(){
    return heap.toString();
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

    switch(pProperty){
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

  public void addHeapObject(SMGObject pNewObject) {
    heap.addHeapObject(pNewObject);
  }

  public void addPVEdge(SMGEdgePointsTo pNewPVEdge) {
    heap.addPointsToEdge(pNewPVEdge);
  }

  public void setMemLeak() {
    heap.setMemoryLeak();
  }
}
