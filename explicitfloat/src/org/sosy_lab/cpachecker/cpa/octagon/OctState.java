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
package org.sosy_lab.cpachecker.cpa.octagon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.octagon.NumArray;
import org.sosy_lab.cpachecker.util.octagon.Octagon;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * An element of octagon abstract domain. This element contains an {@link Octagon} which
 * is the concrete representation of the octagon and a map which
 * provides a mapping from variable names to variables.
 * see {@link Variable}.
 *
 */
class OctState implements AbstractState {

  enum BinaryConstraints {
    /**
     * constraint of the form vx <= c
     */
    PX(0),

    /**
     * constraint of the form -vx <= c
     */
    MX(1),

    /**
     * constraint of the form vx  + vy<= c
     */
    PXPY(2),

    /**
     * constraint of the form vx - vy <= c
     */
    PXMY(3),

    /**
     * constraint of the form -vx  + vy <= c
     */
    MXPY(4),

    /**
     * constraint of the form -vx - vy <= c
     */
    MXMY(5);

    private int num;

    BinaryConstraints(int i) {
      num = i;
    }

    int getNumber() {
      return num;
    }
  }

  // the octagon representation
  private Octagon octagon;

  // mapping from variable name to its identifier
  private BiMap<String, Integer> variableToIndexMap;

  private LogManager logger;

  // also top element
  public OctState(LogManager log) {
    octagon = OctagonManager.universe(0);
    variableToIndexMap = HashBiMap.create();
    logger = log;
  }

  public OctState(Octagon oct, BiMap<String, Integer> map, LogManager log) {
    octagon = oct;
    variableToIndexMap = map;
    logger = log;
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof OctState)) {
      return false;
    }
    OctState otherOct = (OctState) pObj;

    return Objects.equals(variableToIndexMap, otherOct.variableToIndexMap)
           && this.octagon.equals(otherOct.octagon);
  }

  protected int isLessOrEquals(OctState state) {
    // TODO is this right???
    if (isEmpty()) {
      return 1;
    }

    if (variableToIndexMap.equals(state.variableToIndexMap)) {
      return OctagonManager.isIncludedInLazy(octagon, state.octagon);
    } else {

      logger.log(Level.FINEST, "----------\n" + variableToIndexMap.keySet() + "\n"+ state.variableToIndexMap.keySet());
      if (variableToIndexMap.keySet().containsAll(state.variableToIndexMap.keySet())) {
        Octagon newOct = OctagonManager.copy(octagon);
        newOct = OctagonManager.removeDimension(newOct, variableToIndexMap.size()-state.variableToIndexMap.size());
        return OctagonManager.isIncludedInLazy(newOct, state.octagon);
      } else {
        return 2;
      }
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(variableToIndexMap);
    return result;
  }

  @Override
  public String toString() {
    return OctagonManager.print(octagon, variableToIndexMap.inverse());
  }

  public Octagon getOctagon() {
    return octagon;
  }

  public int sizeOfVariables() {
    return variableToIndexMap.size();
  }

  public BiMap<String, Integer> getVariableToIndexMap() {
    return variableToIndexMap;
  }

  public boolean isEmpty() {
    return OctagonManager.isEmpty(octagon);
  }

  @Override
  protected OctState clone() {
    Octagon newOct = OctagonManager.full_copy(octagon);
    BiMap<String, Integer> newMap = HashBiMap.create();

    for (Entry<String, Integer> e: variableToIndexMap.entrySet()) {
      newMap.put(e.getKey(), e.getValue());
    }

    return new OctState(newOct, newMap, logger);
  }

  /**
   * This method sets the coefficients/ the value of a variable to undefined.
   */
  public OctState forget(String pVariableName) {
    return new OctState(OctagonManager.forget(octagon, getVariableIndexFor(pVariableName)),
                        HashBiMap.create(variableToIndexMap),
                        logger);
  }

  /**
   * Returns the index of the variable, if the variable is not already in the map
   * a new variable is declarated and this is index gets returned then.
   */
  protected Integer getVariableIndexFor(String pVariableName) {
    return variableToIndexMap.get(pVariableName);
  }

  protected boolean existsVariable(String variableName) {
    return variableToIndexMap.containsKey(variableName);
  }

  /**
   * This method declares a variable.
   */
  public OctState declareVariable(String varName, OctCoefficients coeffs) {
    assert (!variableToIndexMap.containsKey(varName));
    OctState newState = new OctState(OctagonManager.addDimensionAndProject(octagon, 1),
                                     HashBiMap.create(variableToIndexMap),
                                     logger);
    newState.variableToIndexMap.put(varName, sizeOfVariables());

    if (coeffs == null) {
      newState.octagon = OctagonManager.forget(newState.octagon, newState.getVariableIndexFor(varName));
    } else {
      NumArray arr = coeffs.getNumArray();
      newState.octagon = OctagonManager.assingVar(newState.octagon, newState.getVariableIndexFor(varName), arr);
      OctagonManager.num_clear_n(arr, coeffs.size());
    }

    return newState;
  }

  /**
   * This method makes an assignment to a variable
   */
  public OctState makeAssignment(String leftVarName, OctCoefficients oct) {
    NumArray arr = oct.getNumArray();
    OctState newState = new OctState(OctagonManager.assingVar(octagon, getVariableIndexFor(leftVarName), arr),
                                     HashBiMap.create(variableToIndexMap),
                                     logger);
    OctagonManager.num_clear_n(arr, oct.size());
    return newState;
  }

  /**
   * Helper method for all addXXXXConstraint methods
   */
  private OctState addConstraint(BinaryConstraints cons, int leftIndex, int rightIndex, int constantValue) {
    NumArray arr = OctagonManager.init_num_t(4);
    OctagonManager.num_set_int(arr, 0, cons.getNumber());
    OctagonManager.num_set_int(arr, 1, leftIndex);
    OctagonManager.num_set_int(arr, 2, rightIndex);
    OctagonManager.num_set_int(arr, 3, constantValue);
    OctState newState = new OctState(OctagonManager.addBinConstraint(octagon, 1, arr),
                                     HashBiMap.create(variableToIndexMap),
                                     logger);
    OctagonManager.num_clear_n(arr, 4);
    return newState;
  }

  /**
   * This method adds a smaller constraint between two variables (p.e. a <= b).
   * Note that this only works with integers!
   */
  public OctState addSmallerEqConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    // use 0 as constant value, we don't need it
    return addConstraint(BinaryConstraints.PXMY, lVarIdx, rVarIdx, 0);
  }

  /**
   * This method adds a smaller equal constraint between a variable and a long (p.e. a <= 3).
   * Note that this only works with integers!
   */
  public OctState addSmallerEqConstraint(String pVariableName, long pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);
    return addConstraint(BinaryConstraints.PX, varIdx, 0, (int)pValueOfLiteral);
  }

  /**
   * This method adds a smaller constraint between two variables (p.e. a < b).
   * Note that this only works with integers!
   */
  public OctState addSmallerConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    // we want the lefthandside to be really smaller than the righthandside
    // so we use -1 as a constant value
    return addConstraint(BinaryConstraints.PXMY, lVarIdx, rVarIdx, -1);
  }

  /**
   * This method adds a smaller constraint between a variable and a long (p.e. a < 3).
   * Note that this only works with integers!
   */
  public OctState addSmallerConstraint(String pVariableName, long pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);

    // set right index to -1 as it is not used
    return addConstraint(BinaryConstraints.PX, varIdx, -1, (int)pValueOfLiteral-1);
  }

  /**
   * This method adds a greater equal constraint between two variables (p.e. a >= b).
   * Note that this only works with integers!
   */
  public OctState addGreaterEqConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    // use 0 as constant value, we don't need it
    return addConstraint(BinaryConstraints.MXPY, lVarIdx, rVarIdx, 0);
  }

  /**
   * This method adds a greater equal constraint between a variable and a literal (p.e. a >= 3).
   * Note that this only works with integers!
   */
  public OctState addGreaterEqConstraint(String pVariableName, long pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);

    // set right index to -1 as it is not used
    return addConstraint(BinaryConstraints.MX, varIdx, -1, (int)-pValueOfLiteral);
  }

  /**
   * This method adds a greater constraint between two variables (p.e. a > b).
   * Note that this only works with integers!
   */
  public OctState addGreaterConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    // we want the lefthandside to be really greater than the righthandside
    // so we use -1 as a constant value
    return addConstraint(BinaryConstraints.MXPY, lVarIdx, rVarIdx, -1);
  }

  /**
   * This method adds a greater constraint between a variable and a literal (p.e. a > 3).
   * Note that this only works with integers!
   */
  public OctState addGreaterConstraint(String pVariableName, long pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);

    // set right index to -1 as it is not used
    return addConstraint(BinaryConstraints.MX, varIdx, -1, (-1 - (int)pValueOfLiteral));
  }

  /**
   * This method adds an equality constraint between two variables (p.e. a == b).
   * Note that this only works with integers!
   */
  public OctState addEqConstraint(String pRightVariableName, String pLeftVariableName) {
    return addSmallerEqConstraint(pLeftVariableName, pRightVariableName)
           .addGreaterEqConstraint(pLeftVariableName, pRightVariableName);
  }

  /**
   * This method adds an equality constraint between a variable and a literal (p.e. a == 3).
   * Note that this only works with integers!
   */
  public OctState addEqConstraint(String pVariableName, long constantValue) {
    return addSmallerEqConstraint(pVariableName, constantValue)
           .addGreaterEqConstraint(pVariableName, constantValue);
  }

  /**
   * This method emulates an inequality constraint for assumptions with two variables.
   * There is no inequality constraint possible in the normal way with the octagon library,
   * as workaraound we added a state for a smaller constraint, and a state for a greater constraint.
   * Note that it only works if both variables are Integers!
   */
  public List<OctState> addIneqConstraint(String rightVarName, String leftVarName) {
    List<OctState> list = new ArrayList<>();
    list.add(addSmallerConstraint(leftVarName, rightVarName));
    list.add(addGreaterConstraint(leftVarName, rightVarName));
    return list;
  }

  /**
   * This method emulates an inequality constraint for assumptions with a variable
   * and a long/int.
   * There is no inequality constraint possible in the normal way with the octagon library,
   * as workaraound we added a state for a smaller constraint, and a state for a greater constraint.
   * Note that it only works if both variables are Integers!
   */
  public List<OctState> addIneqConstraint(String varname, long value) {
    List<OctState> list = new ArrayList<>();
    list.add(addSmallerConstraint(varname, value));
    list.add(addGreaterConstraint(varname, value));
    return list;
  }

  public OctState removeLocalVars(String functionName) {

    List<String> keysToRemove = new ArrayList<>();
    for (String var : variableToIndexMap.keySet()) {
      if (var.startsWith(functionName+"::")) {
        keysToRemove.add(var);
      }
    }

    OctState newState = new OctState(OctagonManager.removeDimension(octagon, keysToRemove.size()),
                                     HashBiMap.create(variableToIndexMap),
                                     logger);
    newState.variableToIndexMap.keySet().removeAll(keysToRemove);

    assert (OctagonManager.dimension(newState.octagon) == newState.sizeOfVariables());
    return newState;
  }

  @Override
  protected void finalize() throws Throwable {
    OctagonManager.free(octagon);
    super.finalize();
  }
}
