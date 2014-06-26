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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.octagon.coefficients.IOctagonCoefficients;
import org.sosy_lab.cpachecker.cpa.octagon.coefficients.OctagonUniversalCoefficients;
import org.sosy_lab.cpachecker.cpa.octagon.coefficients.OctagonIntervalCoefficients;
import org.sosy_lab.cpachecker.cpa.octagon.coefficients.OctagonSimpleCoefficients;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonDoubleValue;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonIntValue;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonInterval;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonNumericValue;
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
@SuppressWarnings("rawtypes")
public class OctagonState implements AbstractState {

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

  // mapping from variable name to its identifier
  private BiMap<String, Integer> variableToIndexMap;
  private Map<String, Type> variableToTypeMap;
  private final Block block;

  private LogManager logger;

  // also top element
  public OctagonState(LogManager log, OctagonManager manager) {
    octagon = manager.universe(0);
    octagonManager = manager;
    variableToIndexMap = HashBiMap.create();
    variableToTypeMap = new HashMap<>();
    block = new Block();
    logger = log;

    // cleanup old octagons
    Octagon.removePhantomReferences();
  }

  public OctagonState(Octagon oct, BiMap<String, Integer> map, Map<String, Type> typeMap, Block block, LogManager log) {
    octagon = oct;
    octagonManager = octagon.getManager();
    variableToIndexMap = map;
    variableToTypeMap = typeMap;
    this.block = block;
    logger = log;

    // cleanup old octagons
    Octagon.removePhantomReferences();
  }

  @Override
  public boolean equals(Object pObj) {
    // TODO loopstack
    if (!(pObj instanceof OctagonState)) {
      return false;
    }
    OctagonState otherOct = (OctagonState) pObj;

    return Objects.equals(variableToIndexMap, otherOct.variableToIndexMap)
           && this.octagon.equals(otherOct.octagon);
  }

  protected int isLessOrEquals(OctagonState state) {
    assert !isEmpty() : "Empty states should not occur here!";
    // TODO loopstack

    if (variableToIndexMap.equals(state.variableToIndexMap)) {
      return octagon.getManager().isIncludedInLazy(octagon, state.octagon);
    } else {
      logger.log(Level.FINEST, "Removing some temporary (in the transferrelation)"
                 + " introduced variables from the octagon to compute #isLessOrEquals()");

      if (variableToIndexMap.entrySet().containsAll(state.variableToIndexMap.entrySet())) {
        Pair<OctagonState, OctagonState> checkStates = shrinkToFittingSize(state);
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
  public Pair<OctagonState, OctagonState> shrinkToFittingSize(OctagonState oct) {
    int maxEqualIndex = oct.sizeOfVariables()-1;
    BiMap<Integer, String> inverseThis = variableToIndexMap.inverse();
    BiMap<Integer, String> inverseOther = oct.variableToIndexMap.inverse();
    for (int i = maxEqualIndex; i >= 0; i--) {
      if (!inverseThis.get(i).equals(inverseOther.get(i))) {
        maxEqualIndex = i-1;
      }
    }

    OctagonState newState1;
    if (variableToIndexMap.size() != maxEqualIndex +1) {
      BiMap<String, Integer> newMap1 = HashBiMap.<String, Integer>create(variableToIndexMap);
      Map<String, Type> newTypeMap1 = new HashMap<>(variableToTypeMap);
      for (int i = variableToIndexMap.size()-1; i > maxEqualIndex; i--) {
        newTypeMap1.remove(newMap1.inverse().remove(i));
      }
      Octagon newOct1 = octagonManager.removeDimension(octagon, variableToIndexMap.size()-(maxEqualIndex+1));
      newState1 =  new OctagonState(newOct1, newMap1, newTypeMap1, block, logger);
    } else {
      newState1 = this;
    }

    OctagonState newState2;
    if (oct.variableToIndexMap.size() != maxEqualIndex +1) {
      BiMap<String, Integer> newMap2 = HashBiMap.<String, Integer>create(oct.variableToIndexMap);
      Map<String, Type> newTypeMap2 = new HashMap<>(oct.variableToTypeMap);
      for (int i = oct.variableToIndexMap.size()-1; i > maxEqualIndex; i--) {
        newTypeMap2.remove(newMap2.inverse().remove(i));
      }
      Octagon newOct2 =  octagonManager.removeDimension(oct.octagon, oct.variableToIndexMap.size()-(maxEqualIndex+1));
      newState2 = new OctagonState(newOct2, newMap2, newTypeMap2, block, logger);
    } else {
      newState2 = oct;
    }

    return Pair.of(newState1, newState2);
  }

  public boolean areInSameBlock(OctagonState other) {
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
  public OctagonState forget(String pVariableName) {
    int varIdx = getVariableIndexFor(pVariableName);

    if (varIdx == -1) {
      return this;
    }

    return new OctagonState(octagonManager.forget(octagon, varIdx),
                        HashBiMap.create(variableToIndexMap),
                        new HashMap<>(variableToTypeMap),
                        block,
                        logger);
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

  protected String getVariableNameFor(int index) {
    String result = variableToIndexMap.inverse().get(index);
    if (result == null) {
      throw new IllegalArgumentException();
    }
    return result;
  }

  protected boolean existsVariable(String variableName) {
    return variableToIndexMap.containsKey(variableName);
  }

  public OctagonState declareVariable(String varName, Type type) {
    assert !variableToIndexMap.containsKey(varName);
    OctagonState newState = new OctagonState(octagonManager.addDimensionAndEmbed(octagon, 1),
                                     HashBiMap.create(variableToIndexMap),
                                     new HashMap<>(variableToTypeMap),
                                     block,
                                     logger);
    newState.variableToIndexMap.put(varName, sizeOfVariables());
    newState.variableToTypeMap.put(varName, type);
    return newState;
  }

  public OctagonState makeAssignment(String leftVarName, IOctagonCoefficients oct) {
    if (getVariableIndexFor(leftVarName) == -1) {
      return this;
    }
    if (oct instanceof OctagonSimpleCoefficients) {
      return makeAssignment(leftVarName, (OctagonSimpleCoefficients)oct);
    } else if (oct instanceof OctagonIntervalCoefficients) {
      return makeAssignment(leftVarName, (OctagonIntervalCoefficients)oct);
    } else if (oct instanceof OctagonUniversalCoefficients) {
      return forget(leftVarName);
    }
    throw new IllegalArgumentException("Unkown subtype of OctCoefficients.");
  }

  /**
   * This method makes an assignment to a variable
   */
  private OctagonState makeAssignment(String leftVarName, OctagonSimpleCoefficients oct) {
    assert sizeOfVariables() == oct.size() : "coefficients do not have the right size";

    if (getVariableIndexFor(leftVarName) == -1) {
      return this;
    }

    NumArray arr = oct.getNumArray(octagonManager);
    int varIdx = getVariableIndexFor(leftVarName);

    if (varIdx == -1) {
      return this;
    }

    OctagonState newState = new OctagonState(octagonManager.assingVar(octagon, varIdx, arr),
                                     HashBiMap.create(variableToIndexMap),
                                     new HashMap<>(variableToTypeMap),
                                     block,
                                     logger);
    octagonManager.num_clear_n(arr, oct.size());
    return newState;
  }

  /**
   * This method makes an interval assignment to a variable. The OctCoefficients
   * need to be twice as long as for normal assignments! (upper and lower bound)
   */
  private OctagonState makeAssignment(String leftVarName, OctagonIntervalCoefficients oct) {
    assert sizeOfVariables() == oct.size() : "coefficients do not have the right size";

    if (getVariableIndexFor(leftVarName) == -1) {
      return this;
    }

    NumArray arr = oct.getNumArray(octagonManager);
    int varIdx = getVariableIndexFor(leftVarName);

    if (varIdx == -1) {
      return this;
    }

    OctagonState newState = new OctagonState(octagonManager.intervAssingVar(octagon, varIdx, arr),
                                     HashBiMap.create(variableToIndexMap),
                                     new HashMap<>(variableToTypeMap),
                                     block,
                                     logger);
    octagonManager.num_clear_n(arr, oct.size());
    return newState;
  }

  /**
   * Helper method for all addXXXXConstraint methods
   */
  private OctagonState addConstraint(BinaryConstraints cons, int leftIndex, int rightIndex, OctagonNumericValue constantValue) {
    NumArray arr = octagonManager.init_num_t(4);
    octagonManager.num_set_int(arr, 0, cons.getNumber());
    octagonManager.num_set_int(arr, 1, leftIndex);
    octagonManager.num_set_int(arr, 2, rightIndex);
    if (constantValue instanceof OctagonDoubleValue) {
      octagonManager.num_set_float(arr, 3, constantValue.getValue().doubleValue());
    } else {
      octagonManager.num_set_int(arr, 3, constantValue.getValue().longValue());
    }

    OctagonState newState = new OctagonState(octagonManager.addBinConstraint(octagon, 1, arr),
                                     HashBiMap.create(variableToIndexMap),
                                     new HashMap<>(variableToTypeMap),
                                     block,
                                     logger);
    octagonManager.num_clear_n(arr, 4);
    return newState;
  }

  /**
   * This method adds a smaller constraint between two variables (p.e. a <= b).
   * Note that this only works with integers!
   */
  public OctagonState addSmallerEqConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    if (rVarIdx == -1 || lVarIdx == -1) {
      return this;
    }

    // use 0 as constant value, we don't need it
    return addConstraint(BinaryConstraints.PXMY, lVarIdx, rVarIdx, OctagonIntValue.ZERO);
  }

  /**
   * This method adds a smaller equal constraint between a variable and a long (p.e. a <= 3).
   * Note that this only works with integers!
   */
  public OctagonState addSmallerEqConstraint(String pVariableName, OctagonNumericValue pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);

    if (varIdx == -1) {
      return this;
    }

    return addConstraint(BinaryConstraints.PX, varIdx, -1, pValueOfLiteral);
  }

  public OctagonState addSmallerEqConstraint(String pVariableName, IOctagonCoefficients oct) {
    if (oct instanceof OctagonUniversalCoefficients) {
      return this;
    } else if (oct instanceof OctagonSimpleCoefficients) {
        oct = ((OctagonSimpleCoefficients) oct).convertToInterval();
    }

    oct = oct.add(new OctagonIntervalCoefficients(oct.size(), new OctagonInterval(Double.NEGATIVE_INFINITY, 0), this));
    OctagonState assignedState = makeAssignment(pVariableName, oct);
    return assignedState.intersect(this);
  }

  /**
   * This method adds a smaller constraint between two variables (p.e. a < b).
   * Note that this only works with integers!
   */
  public OctagonState addSmallerConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    if (rVarIdx == -1 || lVarIdx == -1) {
      return this;
    }

    // the octagon library can only handle <= and >= constraints on floats
    if (variableToTypeMap.get(pRightVariableName) == Type.FLOAT
          || variableToTypeMap.get(pLeftVariableName) == Type.FLOAT) {
        return addSmallerEqConstraint(pRightVariableName, pLeftVariableName);
    }

    // we want the lefthandside to be really smaller than the righthandside
    // so we use -1 as a constant value
    return addConstraint(BinaryConstraints.PXMY, lVarIdx, rVarIdx, OctagonIntValue.NEG_ONE);
  }

  /**
   * This method adds a smaller constraint between a variable and a long (p.e. a < 3).
   * Note that this only works with integers!
   */
  public OctagonState addSmallerConstraint(String pVariableName, OctagonNumericValue pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);

    if (varIdx == -1) {
      return this;
    }

    // the octagon library can only handle <= and >= constraints on floats
    if (variableToTypeMap.get(pVariableName) == Type.FLOAT) {
      return addSmallerEqConstraint(pVariableName, pValueOfLiteral);
    }

    // set right index to -1 as it is not used
    return addConstraint(BinaryConstraints.PX, varIdx, -1, pValueOfLiteral.subtract(OctagonIntValue.ONE));
  }

  public OctagonState addSmallerConstraint(String pVariableName, IOctagonCoefficients oct) {

    // the octagon library can only handle <= and >= constraints on floats
    if (variableToTypeMap.get(pVariableName) == Type.FLOAT) {
      return addSmallerEqConstraint(pVariableName, oct);
    }

    // TODO review coefficient handling
    if (oct instanceof OctagonUniversalCoefficients) {
      return this;
    } else if (oct instanceof OctagonSimpleCoefficients) {
        oct = ((OctagonSimpleCoefficients) oct).convertToInterval();
    }
    oct = oct.add(new OctagonIntervalCoefficients(oct.size(), new OctagonInterval(Double.NEGATIVE_INFINITY, -1), this));
    OctagonState assignedState = makeAssignment(pVariableName, oct);
    return assignedState.intersect(this);
  }

  /**
   * This method adds a greater equal constraint between two variables (p.e. a >= b).
   * Note that this only works with integers!
   */
  public OctagonState addGreaterEqConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    if (rVarIdx == -1 || lVarIdx == -1) {
      return this;
    }

    // use 0 as constant value, we don't need it
    return addConstraint(BinaryConstraints.MXPY, lVarIdx, rVarIdx, OctagonIntValue.ZERO);
  }

  /**
   * This method adds a greater equal constraint between a variable and a literal (p.e. a >= 3).
   * Note that this only works with integers!
   */
  public OctagonState addGreaterEqConstraint(String pVariableName, OctagonNumericValue pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);

    if (varIdx == -1) {
      return this;
    }

    // set right index to -1 as it is not used
    return addConstraint(BinaryConstraints.MX, varIdx, -1, pValueOfLiteral.mul(-1));
  }

  public OctagonState addGreaterEqConstraint(String pVariableName, IOctagonCoefficients oct) {
    if (oct instanceof OctagonUniversalCoefficients) {
      return this;
    } else if (oct instanceof OctagonSimpleCoefficients) {
        oct = ((OctagonSimpleCoefficients) oct).convertToInterval();
    }
    oct = oct.add(new OctagonIntervalCoefficients(oct.size(), new OctagonInterval(0, Double.POSITIVE_INFINITY), this));
    OctagonState assignedState = makeAssignment(pVariableName, oct);
    return assignedState.intersect(this);
  }

  /**
   * This method adds a greater constraint between two variables (p.e. a > b).
   * Note that this only works with integers!
   */
  public OctagonState addGreaterConstraint(String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = getVariableIndexFor(pRightVariableName);
    int lVarIdx = getVariableIndexFor(pLeftVariableName);

    if (rVarIdx == -1 || lVarIdx == -1) {
      return this;
    }

    // the octagon library can only handle <= and >= constraints on floats
    if (variableToTypeMap.get(pRightVariableName) == Type.FLOAT
          || variableToTypeMap.get(pLeftVariableName) == Type.FLOAT) {
      return addGreaterEqConstraint(pRightVariableName, pLeftVariableName);
    }

    // we want the lefthandside to be really greater than the righthandside
    // so we use -1 as a constant value
    return addConstraint(BinaryConstraints.MXPY, lVarIdx, rVarIdx, OctagonIntValue.NEG_ONE);
  }

  /**
   * This method adds a greater constraint between a variable and a literal (p.e. a > 3).
   * Note that this only works with integers!
   */
  public OctagonState addGreaterConstraint(String pVariableName, OctagonNumericValue pValueOfLiteral) {
    int varIdx = getVariableIndexFor(pVariableName);

    if (varIdx == -1) {
      return this;
    }

    // the octagon library can only handle <= and >= constraints on floats
    if (variableToTypeMap.get(pVariableName) == Type.FLOAT) {
      return addGreaterEqConstraint(pVariableName, pValueOfLiteral);
    }

    // set right index to -1 as it is not used
    return addConstraint(BinaryConstraints.MX, varIdx, -1, pValueOfLiteral.add(OctagonIntValue.ONE).mul(-1));
  }

  public OctagonState addGreaterConstraint(String pVariableName, IOctagonCoefficients oct) {

    // the octagon library can only handle <= and >= constraints on floats
    if (variableToTypeMap.get(pVariableName) == Type.FLOAT) {
      return addGreaterEqConstraint(pVariableName, oct);
    }

    // TODO review coefficients
    if (oct instanceof OctagonUniversalCoefficients) {
      return this;
    } else if (oct instanceof OctagonSimpleCoefficients) {
        oct = ((OctagonSimpleCoefficients) oct).convertToInterval();
    }
    oct = oct.add(new OctagonIntervalCoefficients(oct.size(), new OctagonInterval(1, Double.POSITIVE_INFINITY), this));
    OctagonState assignedState = makeAssignment(pVariableName, oct);

    return assignedState.intersect(this);
  }

  /**
   * This method adds an equality constraint between two variables (p.e. a == b).
   * Note that this only works with integers!
   */
  public OctagonState addEqConstraint(String pRightVariableName, String pLeftVariableName) {
    return addSmallerEqConstraint(pRightVariableName, pLeftVariableName)
           .addGreaterEqConstraint(pRightVariableName, pLeftVariableName);
  }

  /**
   * This method adds an equality constraint between a variable and a literal (p.e. a == 3).
   * Note that this only works with integers!
   */
  public OctagonState addEqConstraint(String pVariableName, OctagonNumericValue constantValue) {
    return addSmallerEqConstraint(pVariableName, constantValue)
           .addGreaterEqConstraint(pVariableName, constantValue);
  }

  public OctagonState addEqConstraint(String pVariableName, IOctagonCoefficients coeffs) {
    OctagonState assignedState = makeAssignment(pVariableName, coeffs);
    return assignedState.intersect(this);
  }

  /**
   * This method emulates an inequality constraint for assumptions with two variables.
   * There is no inequality constraint possible in the normal way with the octagon library,
   * as workaraound we added a state for a smaller constraint, and a state for a greater constraint.
   * Note that it only works if both variables are Integers!
   */
  public Set<OctagonState> addIneqConstraint(String rightVarName, String leftVarName) {
    Set<OctagonState> set = new HashSet<>();
    set.add(addSmallerConstraint(rightVarName, leftVarName));
    set.add(addGreaterConstraint(rightVarName, leftVarName));
    return set;
  }

  /**
   * This method emulates an inequality constraint for assumptions with a variable
   * and a long/int.
   * There is no inequality constraint possible in the normal way with the octagon library,
   * as workaraound we added a state for a smaller constraint, and a state for a greater constraint.
   * Note that it only works if both variables are Integers!
   */
  public Set<OctagonState> addIneqConstraint(String varname, OctagonNumericValue value) {
    Set<OctagonState> set = new HashSet<>();
    set.add(addSmallerConstraint(varname, value));
    set.add(addGreaterConstraint(varname, value));
    return set;
  }

  public Set<OctagonState> addIneqConstraint(String varname, IOctagonCoefficients oct) {
    Set<OctagonState> set = new HashSet<>();
    set.add(addSmallerConstraint(varname, oct));
    set.add(addGreaterConstraint(varname, oct));
    return set;
  }

  public OctagonState intersect(OctagonState other) {
    return new OctagonState(octagonManager.intersection(octagon, other.octagon),
                        HashBiMap.create(variableToIndexMap),
                        new HashMap<>(variableToTypeMap),
                        block,
                        logger);
  }

  public OctagonState removeTempVars(String functionName, String varPrefix) {
    return removeVars(functionName, varPrefix);
  }

  public OctagonState removeLocalVars(String functionName) {
    return removeVars(functionName, "");
  }

  public Map<String, OctagonInterval> getVariablesWithBounds() {
    Map<String, OctagonInterval> vars = new HashMap<>();
    for (String varName : variableToIndexMap.keySet()) {
      vars.put(varName, octagonManager.getVariableBounds(octagon, getVariableIndexFor(varName)));
    }
    return vars;
  }

  public OctagonInterval getVariableBounds(int index) {
    assert index < sizeOfVariables();
    return octagonManager.getVariableBounds(octagon, index);
  }

  private OctagonState removeVars(String functionName, String varPrefix) {
    List<String> keysToRemove = new ArrayList<>();
    for (String var : variableToIndexMap.keySet()) {
      if (var.startsWith(functionName+"::"+varPrefix)) {
        keysToRemove.add(var);
      }
    }

    if (keysToRemove.size() == 0) {
      return this;
    }

    OctagonState newState = new OctagonState(octagonManager.removeDimension(octagon, keysToRemove.size()),
                                     HashBiMap.create(variableToIndexMap),
                                     new HashMap<>(variableToTypeMap),
                                     block,
                                     logger);
    newState.variableToIndexMap.keySet().removeAll(keysToRemove);
    newState.variableToTypeMap.keySet().removeAll(keysToRemove);

    for (int i = 0; i < newState.variableToIndexMap.size(); i++) {
      assert newState.variableToIndexMap.inverse().get(i) != null;
    }
    assert octagonManager.dimension(newState.octagon) == newState.sizeOfVariables();
    return newState;
  }
}
