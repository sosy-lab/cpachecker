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
package org.sosy_lab.cpachecker.cpa.octagon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.octagon.coefficients.IOctCoefficients;
import org.sosy_lab.cpachecker.cpa.octagon.coefficients.OctEmptyCoefficients;
import org.sosy_lab.cpachecker.cpa.octagon.coefficients.OctIntervalCoefficients;
import org.sosy_lab.cpachecker.cpa.octagon.coefficients.OctNumericValue;
import org.sosy_lab.cpachecker.cpa.octagon.coefficients.OctSimpleCoefficients;
import org.sosy_lab.cpachecker.util.octagon.NumArray;
import org.sosy_lab.cpachecker.util.octagon.Octagon;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * An element of octagon abstract domain. This element contains an {@link Octagon} which
 * is the concrete representation of the octagon and a map which
 * provides a mapping from variable names to variables.
 *
 */
public class OctState implements AbstractState {

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

  enum Type {
    INT, FLOAT;
  }

  public static class Block {
    private static int numBlocks = 0;

    private final int id;

    Block() {
      numBlocks++;
      id = numBlocks;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Block)) {
        return false;
      }
      return id == ((Block)obj).id;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 7;
      result = prime * result + id;
      return result;
    }
  }

  // the octagon representation
  private Octagon octagon;
  private OctagonManager octagonManager;
  private final boolean handleFloats;

  // mapping from variable name to its identifier
  private BiMap<String, Integer> variableToIndexMap;
  private Map<String, Type> variableToTypeMap;
  private final Block block;

  private LogManager logger;

  // also top element
  public OctState(LogManager log, boolean pHandleFloats, OctagonManager manager) {
    octagon = manager.universe(0);
    octagonManager = manager;
    variableToIndexMap = HashBiMap.create();
    variableToTypeMap = new HashMap<>();
    block = new Block();
    logger = log;
    handleFloats = pHandleFloats;

    // cleanup old octagons
    Octagon.removePhantomReferences();
  }

  public OctState(Octagon oct, BiMap<String, Integer> map, Map<String, Type> typeMap, Block block, LogManager log, boolean pHandleFloats) {
    octagon = oct;
    octagonManager = octagon.getManager();
    variableToIndexMap = map;
    variableToTypeMap = typeMap;
    this.block = block;
    logger = log;
    handleFloats = pHandleFloats;

    // cleanup old octagons
    Octagon.removePhantomReferences();
  }

  @Override
  public boolean equals(Object pObj) {
    // TODO loopstack
    if (!(pObj instanceof OctState)) {
      return false;
    }
    OctState otherOct = (OctState) pObj;

    return Objects.equals(variableToIndexMap, otherOct.variableToIndexMap)
           && this.octagon.equals(otherOct.octagon);
  }

  protected int isLessOrEquals(OctState state) {
    assert !isEmpty() : "Empty states should not occur here!";
    // TODO loopstack

    if (variableToIndexMap.equals(state.variableToIndexMap)) {
      return octagon.getManager().isIncludedInLazy(octagon, state.octagon);
    } else {
      logger.log(Level.FINEST, "Removing some temporary (in the transferrelation)"
                 + " introduced variables from the octagon to compute #isLessOrEquals()");

      if (variableToIndexMap.entrySet().containsAll(state.variableToIndexMap.entrySet())) {
        Pair<OctState, OctState> checkStates = shrinkToFittingSize(state);
        return octagon.getManager().isIncludedInLazy(checkStates.getFirst().octagon, checkStates.getSecond().octagon);
      } else {
        return 2;
      }
    }
  }

  /**
   * This method forgets some information about previously catched variables, this
   * is necessary for isLessOrEquals, or the union operator, be careful using it
   * in other ways (Variables removed from the OctState cannot be referenced anymore
   * by the OctTransferrelation)
   * @param oct the octagon which has the preferred size, instead of just using
   *             an int, the OctState is the parameter, so we can check if the variables
   *             are matching if not an Exception is thrown
   * @return
   */
  public Pair<OctState, OctState> shrinkToFittingSize(OctState oct) {
    int maxEqualIndex = oct.sizeOfVariables()-1;
    BiMap<Integer, String> inverseThis = variableToIndexMap.inverse();
    BiMap<Integer, String> inverseOther = oct.variableToIndexMap.inverse();
    for (int i = maxEqualIndex; i >= 0; i--) {
      if (!inverseThis.get(i).equals(inverseOther.get(i))) {
        maxEqualIndex = i-1;
      }
    }

    OctState newState1;
    if (variableToIndexMap.size() != maxEqualIndex +1) {
      BiMap<String, Integer> newMap1 = HashBiMap.<String, Integer>create(variableToIndexMap);
      Map<String, Type> newTypeMap1 = new HashMap<>(variableToTypeMap);
      for (int i = variableToIndexMap.size()-1; i > maxEqualIndex; i--) {
        newTypeMap1.remove(newMap1.inverse().remove(i));
      }
      Octagon newOct1 = octagonManager.removeDimension(octagon, variableToIndexMap.size()-(maxEqualIndex+1));
      newState1 =  new OctState(newOct1, newMap1, newTypeMap1, block, logger, handleFloats);
    } else {
      newState1 = this;
    }

    OctState newState2;
    if (oct.variableToIndexMap.size() != maxEqualIndex +1) {
      BiMap<String, Integer> newMap2 = HashBiMap.<String, Integer>create(oct.variableToIndexMap);
      Map<String, Type> newTypeMap2 = new HashMap<>(oct.variableToTypeMap);
      for (int i = oct.variableToIndexMap.size()-1; i > maxEqualIndex; i--) {
        newTypeMap2.remove(newMap2.inverse().remove(i));
      }
      Octagon newOct2 =  octagonManager.removeDimension(oct.octagon, oct.variableToIndexMap.size()-(maxEqualIndex+1));
      newState2 = new OctState(newOct2, newMap2, newTypeMap2, block, logger, handleFloats);
    } else {
      newState2 = oct;
    }

    return Pair.of(newState1, newState2);
  }

  public boolean areInSameBlock(OctState other) {
    return block.equals(other.block);
  }

  public Block getBlock() {
    return block;
  }

  @Override
  public int hashCode() {
    // TODO loopstack
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(variableToIndexMap);
    result = prime * result + Objects.hashCode(variableToTypeMap);
    return result;
  }

  @Override
  public String toString() {
    return octagonManager.print(octagon, variableToIndexMap.inverse());
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

  public Map<String, Type> getVariableToTypeMap() {
    return variableToTypeMap;
  }

  public boolean isEmpty() {
    return octagonManager.isEmpty(octagon);
  }

  /**
   * This method sets the coefficients/ the value of a variable to undefined.
   */
  public OctState forget(String pVariableName) {
    int varIdx = getVariableIndexFor(pVariableName);

    if (varIdx == -1) {
      return this;
    }

    return new OctState(octagonManager.forget(octagon, varIdx),
                        HashBiMap.create(variableToIndexMap),
                        new HashMap<>(variableToTypeMap),
                        block,
                        logger,
                        handleFloats);
  }

  /**
   * Returns the index of the variable, if the variable is not in the map -1 is returned.
   */
  protected int getVariableIndexFor(String pVariableName) {
    Integer result = variableToIndexMap.get(pVariableName);
    if (result == null) {
      return -1;
    }
    return result;
  }

  protected boolean existsVariable(String variableName) {
    return variableToIndexMap.containsKey(variableName);
  }

  public OctState declareVariable(String varName, Type type) {
    assert !variableToIndexMap.containsKey(varName);
    OctState newState = new OctState(octagonManager.addDimensionAndEmbed(octagon, 1),
                                     HashBiMap.create(variableToIndexMap),
                                     new HashMap<>(variableToTypeMap),
                                     block,
                                     logger,
                                     handleFloats);
    newState.variableToIndexMap.put(varName, sizeOfVariables());
    newState.variableToTypeMap.put(varName, type);
    return newState;
  }

  public OctState makeAssignment(String leftVarName, IOctCoefficients oct) {
    if (getVariableIndexFor(leftVarName) == -1) {
      return this;
    }
    if (oct instanceof OctSimpleCoefficients) {
      return makeAssignment(leftVarName, (OctSimpleCoefficients)oct);
    } else if (oct instanceof OctIntervalCoefficients) {
      return makeAssignment(leftVarName, (OctIntervalCoefficients)oct);
    } else if (oct instanceof OctEmptyCoefficients) {
      return forget(leftVarName);
    }
    throw new IllegalArgumentException("Unkown subtype of OctCoefficients.");
  }

  /**
   * This method makes an assignment to a variable
   */
  private OctState makeAssignment(String leftVarName, OctSimpleCoefficients oct) {
    assert sizeOfVariables() == oct.size() : "coefficients do not have the right size";

    if (getVariableIndexFor(leftVarName) == -1) {
      return this;
    }

    NumArray arr = oct.getNumArray(octagonManager);
    int varIdx = getVariableIndexFor(leftVarName);

    if (varIdx == -1) {
      return this;
    }

    OctState newState = new OctState(octagonManager.assingVar(octagon, varIdx, arr),
                                     HashBiMap.create(variableToIndexMap),
                                     new HashMap<>(variableToTypeMap),
                                     block,
                                     logger,
                                     handleFloats);
    octagonManager.num_clear_n(arr, oct.size());
    return newState;
  }

  /**
   * This method makes an interval assignment to a variable. The OctCoefficients
   * need to be twice as long as for normal assignments! (upper and lower bound)
   */
  private OctState makeAssignment(String leftVarName, OctIntervalCoefficients oct) {
    assert sizeOfVariables() == oct.size() : "coefficients do not have the right size";

    if (getVariableIndexFor(leftVarName) == -1) {
      return this;
    }

    NumArray arr = oct.getNumArray(octagonManager);
    int varIdx = getVariableIndexFor(leftVarName);

    if (varIdx == -1) {
      return this;
    }

    OctState newState = new OctState(octagonManager.intervAssingVar(octagon, varIdx, arr),
                                     HashBiMap.create(variableToIndexMap),
                                     new HashMap<>(variableToTypeMap),
                                     block,
                                     logger,
                                     handleFloats);
    octagonManager.num_clear_n(arr, oct.size());
    return newState;
  }

  /**
   * Helper method for all addXXXXConstraint methods
   */
  private OctState addConstraint(BinaryConstraints cons, int leftIndex, int rightIndex, OctNumericValue constantValue) {
    NumArray arr = octagonManager.init_num_t(4);
    octagonManager.num_set_int(arr, 0, cons.getNumber());
    octagonManager.num_set_int(arr, 1, leftIndex);
    octagonManager.num_set_int(arr, 2, rightIndex);
    if (constantValue.isFloat()) {
      octagonManager.num_set_float(arr, 3, constantValue.getFloatVal().doubleValue());
    } else {
      octagonManager.num_set_int(arr, 3, (int) constantValue.getIntVal().longValue());
    }

    OctState newState = new OctState(octagonManager.addBinConstraint(octagon, 1, arr),
                                     HashBiMap.create(variableToIndexMap),
                                     new HashMap<>(variableToTypeMap),
                                     block,
                                     logger,
                                     handleFloats);
    octagonManager.num_clear_n(arr, 4);
    return newState;
  }

  /**
   * This method adds a smaller constraint between two variables (p.e. a <= b).
   * Note that this only works with integers!
   */
  public OctState addSmallerEqConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    if (rVarIdx == -1 || lVarIdx == -1) {
      return this;
    }

    // use 0 as constant value, we don't need it
    return addConstraint(BinaryConstraints.PXMY, lVarIdx, rVarIdx, OctNumericValue.ZERO);
  }

  /**
   * This method adds a smaller equal constraint between a variable and a long (p.e. a <= 3).
   * Note that this only works with integers!
   */
  public OctState addSmallerEqConstraint(String pVariableName, OctNumericValue pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);

    if (varIdx == -1) {
      return this;
    }

    return addConstraint(BinaryConstraints.PX, varIdx, -1, pValueOfLiteral);
  }

  public OctState addSmallerEqConstraint(String pVariableName, IOctCoefficients oct) {
    if (oct instanceof OctEmptyCoefficients) {
      return this;
    } else if (oct instanceof OctSimpleCoefficients) {
        oct = ((OctSimpleCoefficients) oct).convertToInterval();
    }

    oct = oct.add(new OctIntervalCoefficients(oct.size(), OctNumericValue.ZERO, OctNumericValue.ZERO, true, false, this));
    OctState assignedState = makeAssignment(pVariableName, oct);
    return assignedState.intersect(this);
  }

  /**
   * This method adds a smaller constraint between two variables (p.e. a < b).
   * Note that this only works with integers!
   */
  public OctState addSmallerConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    if (rVarIdx == -1 || lVarIdx == -1) {
      return this;
    }

    // the octagon library can only handle <= and >= constraints on floats
    // so we need to check if the a >= b that we can make sure that its really
    // smaller
    if (variableToTypeMap.get(pRightVariableName) == Type.FLOAT
          || variableToTypeMap.get(pLeftVariableName) == Type.FLOAT) {

      if (addGreaterEqConstraint(pRightVariableName, pLeftVariableName).isEmpty()) {
        return addSmallerEqConstraint(pRightVariableName, pLeftVariableName);
      } else {
        return new OctState(octagonManager.empty(variableToIndexMap.size()),
                            HashBiMap.create(variableToIndexMap),
                            new HashMap<>(variableToTypeMap),
                            block, logger, handleFloats);
      }
    }

    // we want the lefthandside to be really smaller than the righthandside
    // so we use -1 as a constant value
    return addConstraint(BinaryConstraints.PXMY, lVarIdx, rVarIdx, new OctNumericValue(-1));
  }

  /**
   * This method adds a smaller constraint between a variable and a long (p.e. a < 3).
   * Note that this only works with integers!
   */
  public OctState addSmallerConstraint(String pVariableName, OctNumericValue pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);

    if (varIdx == -1) {
      return this;
    }

    // the octagon library can only handle <= and >= constraints on floats
    // so we need to check if the a >= b that we can make sure that its really
    // smaller
    if (variableToTypeMap.get(pVariableName) == Type.FLOAT) {

      if (addGreaterEqConstraint(pVariableName, pValueOfLiteral).isEmpty()) {
        return addSmallerEqConstraint(pVariableName, pValueOfLiteral);
      } else {
        return new OctState(octagonManager.empty(variableToIndexMap.size()),
                            HashBiMap.create(variableToIndexMap),
                            new HashMap<>(variableToTypeMap),
                            block, logger, handleFloats);
      }
    }

    // set right index to -1 as it is not used
    return addConstraint(BinaryConstraints.PX, varIdx, -1, pValueOfLiteral.subtract(OctNumericValue.ONE));
  }

  public OctState addSmallerConstraint(String pVariableName, IOctCoefficients oct) {

    // the octagon library can only handle <= and >= constraints on floats
    // so we need to check if the a >= b that we can make sure that its really
    // smaller
    if (variableToTypeMap.get(pVariableName) == Type.FLOAT) {

      if (addGreaterEqConstraint(pVariableName, oct).isEmpty()) {
        return addSmallerEqConstraint(pVariableName, oct);
      } else {
        return new OctState(octagonManager.empty(variableToIndexMap.size()),
                            HashBiMap.create(variableToIndexMap),
                            new HashMap<>(variableToTypeMap),
                            block, logger, handleFloats);
      }
    }

    // TODO review coefficient handling
    if (oct instanceof OctEmptyCoefficients) {
      return this;
    } else if (oct instanceof OctSimpleCoefficients) {
        oct = ((OctSimpleCoefficients) oct).convertToInterval();
    }
    oct = oct.add(new OctIntervalCoefficients(oct.size(), OctNumericValue.ZERO, new OctNumericValue(-1), true, false, this));
    OctState assignedState = makeAssignment(pVariableName, oct);
    return assignedState.intersect(this);
  }

  /**
   * This method adds a greater equal constraint between two variables (p.e. a >= b).
   * Note that this only works with integers!
   */
  public OctState addGreaterEqConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    if (rVarIdx == -1 || lVarIdx == -1) {
      return this;
    }

    // use 0 as constant value, we don't need it
    return addConstraint(BinaryConstraints.MXPY, lVarIdx, rVarIdx, OctNumericValue.ZERO);
  }

  /**
   * This method adds a greater equal constraint between a variable and a literal (p.e. a >= 3).
   * Note that this only works with integers!
   */
  public OctState addGreaterEqConstraint(String pVariableName, OctNumericValue pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);

    if (varIdx == -1) {
      return this;
    }

    // set right index to -1 as it is not used
    return addConstraint(BinaryConstraints.MX, varIdx, -1, pValueOfLiteral.mul(-1));
  }

  public OctState addGreaterEqConstraint(String pVariableName, IOctCoefficients oct) {
    if (oct instanceof OctEmptyCoefficients) {
      return this;
    } else if (oct instanceof OctSimpleCoefficients) {
        oct = ((OctSimpleCoefficients) oct).convertToInterval();
    }
    oct = oct.add(new OctIntervalCoefficients(oct.size(), OctNumericValue.ZERO, OctNumericValue.ZERO, false, true, this));
    OctState assignedState = makeAssignment(pVariableName, oct);
    return assignedState.intersect(this);
  }

  /**
   * This method adds a greater constraint between two variables (p.e. a > b).
   * Note that this only works with integers!
   */
  public OctState addGreaterConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    if (rVarIdx == -1 || lVarIdx == -1) {
      return this;
    }

    // the octagon library can only handle <= and >= constraints on floats
    // so we need to check if the a >= b that we can make sure that its really
    // smaller
    if (variableToTypeMap.get(pRightVariableName) == Type.FLOAT
          || variableToTypeMap.get(pLeftVariableName) == Type.FLOAT) {

      if (addSmallerEqConstraint(pRightVariableName, pLeftVariableName).isEmpty()) {
        return addGreaterEqConstraint(pRightVariableName, pLeftVariableName);
      } else {
        return new OctState(octagonManager.empty(variableToIndexMap.size()),
                            HashBiMap.create(variableToIndexMap),
                            new HashMap<>(variableToTypeMap),
                            block, logger, handleFloats);
      }
    }

    // we want the lefthandside to be really greater than the righthandside
    // so we use -1 as a constant value
    return addConstraint(BinaryConstraints.MXPY, lVarIdx, rVarIdx, new OctNumericValue(-1));
  }

  /**
   * This method adds a greater constraint between a variable and a literal (p.e. a > 3).
   * Note that this only works with integers!
   */
  public OctState addGreaterConstraint(String pVariableName, OctNumericValue pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);

    if (varIdx == -1) {
      return this;
    }

    // the octagon library can only handle <= and >= constraints on floats
    // so we need to check if the a >= b that we can make sure that its really
    // smaller
    if (variableToTypeMap.get(pVariableName) == Type.FLOAT) {

      if (addSmallerEqConstraint(pVariableName, pValueOfLiteral).isEmpty()) {
        return addGreaterEqConstraint(pVariableName, pValueOfLiteral);
      } else {
        return new OctState(octagonManager.empty(variableToIndexMap.size()),
                            HashBiMap.create(variableToIndexMap),
                            new HashMap<>(variableToTypeMap),
                            block, logger, handleFloats);
      }
    }

    // set right index to -1 as it is not used
    return addConstraint(BinaryConstraints.MX, varIdx, -1, pValueOfLiteral.add(OctNumericValue.ONE).mul(-1));
  }

  public OctState addGreaterConstraint(String pVariableName, IOctCoefficients oct) {

    // the octagon library can only handle <= and >= constraints on floats
    // so we need to check if the a >= b that we can make sure that its really
    // smaller
    if (variableToTypeMap.get(pVariableName) == Type.FLOAT) {

      if (addSmallerEqConstraint(pVariableName, oct).isEmpty()) {
        return addGreaterEqConstraint(pVariableName, oct);
      } else {
        return new OctState(octagonManager.empty(variableToIndexMap.size()),
                            HashBiMap.create(variableToIndexMap),
                            new HashMap<>(variableToTypeMap),
                            block, logger, handleFloats);
      }
    }

    // TODO review coefficients
    if (oct instanceof OctEmptyCoefficients) {
      return this;
    } else if (oct instanceof OctSimpleCoefficients) {
        oct = ((OctSimpleCoefficients) oct).convertToInterval();
    }
    oct = oct.add(new OctIntervalCoefficients(oct.size(), OctNumericValue.ONE, OctNumericValue.ZERO, false, true, this));
    OctState assignedState = makeAssignment(pVariableName, oct);

    return assignedState.intersect(this);
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
  public OctState addEqConstraint(String pVariableName, OctNumericValue constantValue) {
    return addSmallerEqConstraint(pVariableName, constantValue)
           .addGreaterEqConstraint(pVariableName, constantValue);
  }

  public OctState addEqConstraint(String pVariableName, IOctCoefficients coeffs) {
    OctState assignedState = makeAssignment(pVariableName, coeffs);
    return assignedState.intersect(this);
  }

  /**
   * This method emulates an inequality constraint for assumptions with two variables.
   * There is no inequality constraint possible in the normal way with the octagon library,
   * as workaraound we added a state for a smaller constraint, and a state for a greater constraint.
   * Note that it only works if both variables are Integers!
   */
  public List<OctState> addIneqConstraint(String rightVarName, String leftVarName) {
    List<OctState> list = new ArrayList<>();
    list.add(addSmallerConstraint(rightVarName, leftVarName));
    list.add(addGreaterConstraint(rightVarName, leftVarName));
    return list;
  }

  /**
   * This method emulates an inequality constraint for assumptions with a variable
   * and a long/int.
   * There is no inequality constraint possible in the normal way with the octagon library,
   * as workaraound we added a state for a smaller constraint, and a state for a greater constraint.
   * Note that it only works if both variables are Integers!
   */
  public List<OctState> addIneqConstraint(String varname, OctNumericValue value) {
    List<OctState> list = new ArrayList<>();
    list.add(addSmallerConstraint(varname, value));
    list.add(addGreaterConstraint(varname, value));
    return list;
  }

  public List<OctState> addIneqConstraint(String varname, IOctCoefficients oct) {
    List<OctState> list = new ArrayList<>();
    list.add(addSmallerConstraint(varname, oct));
    list.add(addGreaterConstraint(varname, oct));
    return list;
  }

  public OctState intersect(OctState other) {
    return new OctState(octagonManager.intersection(octagon, other.octagon),
                        HashBiMap.create(variableToIndexMap),
                        new HashMap<>(variableToTypeMap),
                        block,
                        logger,
                        handleFloats);
  }

  public OctState removeTempVars(String functionName, String varPrefix) {
    return removeVars(functionName, varPrefix);
  }

  public OctState removeLocalVars(String functionName) {
    return removeVars(functionName, "");
  }

  public Map<String, Pair<OctNumericValue, OctNumericValue>> getVariablesWithBounds() {
    Map<String, Pair<OctNumericValue, OctNumericValue>> vars = new HashMap<>();
    for (String varName : variableToIndexMap.keySet()) {
      vars.put(varName, octagonManager.getVariableBounds(octagon, getVariableIndexFor(varName)));
    }
    return vars;
  }

  private OctState removeVars(String functionName, String varPrefix) {
    List<String> keysToRemove = new ArrayList<>();
    for (String var : variableToIndexMap.keySet()) {
      if (var.startsWith(functionName+"::"+varPrefix)) {
        keysToRemove.add(var);
      }
    }

    if (keysToRemove.size() == 0) {
      return this;
    }

    OctState newState = new OctState(octagonManager.removeDimension(octagon, keysToRemove.size()),
                                     HashBiMap.create(variableToIndexMap),
                                     new HashMap<>(variableToTypeMap),
                                     block,
                                     logger,
                                     handleFloats);
    newState.variableToIndexMap.keySet().removeAll(keysToRemove);
    newState.variableToTypeMap.keySet().removeAll(keysToRemove);

    for (int i = 0; i < newState.variableToIndexMap.size(); i++) {
      assert newState.variableToIndexMap.inverse().get(i) != null;
    }
    assert octagonManager.dimension(newState.octagon) == newState.sizeOfVariables();
    return newState;
  }
}
