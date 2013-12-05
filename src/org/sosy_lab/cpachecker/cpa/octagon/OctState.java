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

import java.util.Map.Entry;

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

  private OctState previousState;

  // also top element
  public OctState() {
    octagon = OctagonManager.universe(0);
    variableToIndexMap = HashBiMap.create();
    previousState = null;
  }

  public OctState(Octagon oct, BiMap<String, Integer> map, OctState previousElement) {
    octagon = oct;
    variableToIndexMap = map;
    this.previousState = previousElement;
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof OctState)) {
      return false;
    }
    OctState otherOct = (OctState) pObj;
    return this.octagon.equals(otherOct.octagon);
  }

  @Override
  public int hashCode() {
    return octagon.hashCode();
  }

  public void printOctagon() {
    OctagonManager.print(octagon);
  }

  @Override
  public String toString() {
    return variableToIndexMap + " [octagon]: " + octagon;
  }

  public Octagon getOctagon() {
    return octagon;
  }

  public int sizeOfVariables() {
    return variableToIndexMap.size();
  }

  public OctState getPreviousState() {
    return previousState;
  }

  public void setPreviousState(OctState pPreviousElement) {
    this.previousState = pPreviousElement;
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

    return new OctState(newOct, newMap, this.previousState);
  }

  /**
   * This method sets the coefficients/ the value of a variable to undefined.
   */
  public void forget(String pVariableName) {
    OctagonManager.forget(octagon, getVariableIndexFor(pVariableName));
  }

  protected Integer getVariableIndexFor(String pVariableName) {
    return variableToIndexMap.get(pVariableName);
  }

  /**
   * This method declares a variable.
   */
  public void declareVariable(String pVariableName) {
    assert (!variableToIndexMap.containsKey(pVariableName));
    variableToIndexMap.put(pVariableName, sizeOfVariables());
    octagon = OctagonManager.addDimensionAndEmbed(octagon, 1);
  }

  /**
   * This method makes an assignment to a variable
   */
  public void makeAssignment(String leftVarName, OctCoefficients oct) {
    NumArray arr = oct.getNumArray();
    octagon = OctagonManager.assingVar(octagon, getVariableIndexFor(leftVarName), arr);
    OctagonManager.num_clear_n(arr, oct.size());
  }

  /**
   * Helper method for all addXXXXConstraint methods
   */
  private void addConstraint(BinaryConstraints cons, int leftIndex, int rightIndex, int constantValue) {
    NumArray arr = OctagonManager.init_num_t(4);
    OctagonManager.num_set_int(arr, 0, cons.getNumber());
    OctagonManager.num_set_int(arr, 1, leftIndex);
    OctagonManager.num_set_int(arr, 2, rightIndex);
    OctagonManager.num_set_int(arr, 3, constantValue);
    octagon = OctagonManager.addBinConstraint(octagon, 1, arr);
    OctagonManager.num_clear_n(arr, 4);
  }

  /**
   * This method adds a smaller constraint between two variables (p.e. a <= b).
   * Note that this only works with integers!
   */
  public void addSmallerEqConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    // use 0 as constant value, we don't need it
    addConstraint(BinaryConstraints.PXMY, lVarIdx, rVarIdx, 0);
  }

  /**
   * This method adds a smaller equal constraint between a variable and a long (p.e. a <= 3).
   * Note that this only works with integers!
   */
  public void addSmallerEqConstraint(String pVariableName, long pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);
    addConstraint(BinaryConstraints.PX, varIdx, 0, (int)pValueOfLiteral);
  }

  /**
   * This method adds a smaller constraint between two variables (p.e. a < b).
   * Note that this only works with integers!
   */
  public void addSmallerConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    // we want the lefthandside to be really smaller than the righthandside
    // so we use -1 as a constant value
    addConstraint(BinaryConstraints.PXMY, lVarIdx, rVarIdx, -1);
  }

  /**
   * This method adds a smaller constraint between a variable and a long (p.e. a < 3).
   * Note that this only works with integers!
   */
  public void addSmallerConstraint(String pVariableName, long pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);

    // set right index to -1 as it is not used
    addConstraint(BinaryConstraints.PX, varIdx, -1, (int)pValueOfLiteral-1);
  }

  /**
   * This method adds a greater equal constraint between two variables (p.e. a >= b).
   * Note that this only works with integers!
   */
  public void addGreaterEqConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    // use 0 as constant value, we don't need it
    addConstraint(BinaryConstraints.MXPY, lVarIdx, rVarIdx, 0);
  }

  /**
   * This method adds a greater equal constraint between a variable and a literal (p.e. a >= 3).
   * Note that this only works with integers!
   */
  public void addGreaterEqConstraint(String pVariableName, long pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);

    // set right index to -1 as it is not used
    addConstraint(BinaryConstraints.MX, varIdx, -1, (int)-pValueOfLiteral);
  }

  /**
   * This method adds a greater constraint between two variables (p.e. a > b).
   * Note that this only works with integers!
   */
  public void addGreaterConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    // we want the lefthandside to be really greater than the righthandside
    // so we use -1 as a constant value
    addConstraint(BinaryConstraints.MXPY, lVarIdx, rVarIdx, -1);
  }

  /**
   * This method adds a greater constraint between a variable and a literal (p.e. a > 3).
   * Note that this only works with integers!
   */
  public void addGreaterConstraint(String pVariableName, long pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);

    // set right index to -1 as it is not used
    addConstraint(BinaryConstraints.MX, varIdx, -1, (-1 - (int)pValueOfLiteral));
  }

  /**
   * This method adds an equality constraint between two variables (p.e. a == b).
   * Note that this only works with integers!
   */
  public void addEqConstraint(String pRightVariableName, String pLeftVariableName) {
    addSmallerEqConstraint(pLeftVariableName, pRightVariableName);
    addGreaterEqConstraint(pLeftVariableName, pRightVariableName);
  }

  /**
   * This method adds an equality constraint between a variable and a literal (p.e. a == 3).
   * Note that this only works with integers!
   */
  public void addEqConstraint(String pVariableName, long constantValue) {
    addSmallerEqConstraint(pVariableName, constantValue);
    addGreaterEqConstraint(pVariableName, constantValue);
  }

  // keep sizeOfpreviousElem dimensions at the beginning and remove the rest
  public void removeLocalVariables(OctState prevState) {
    int noOfLocalVars = (sizeOfVariables()- prevState.sizeOfVariables());

    for (int i = sizeOfVariables(); i>prevState.sizeOfVariables(); i--) {
      String s = variableToIndexMap.inverse().get(i-1);
      variableToIndexMap.remove(s);
    }

    octagon = OctagonManager.removeDimension(octagon, noOfLocalVars);
    assert (OctagonManager.dimension(octagon) == sizeOfVariables());
  }

}
