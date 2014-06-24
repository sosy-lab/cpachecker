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
package org.sosy_lab.cpachecker.cpa.apron;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import apron.Abstract0;
import apron.Dimchange;
import apron.Dimension;
import apron.Interval;
import apron.Lincons0;
import apron.Linexpr0;
import apron.Tcons0;
import apron.Texpr0Intern;
import apron.Texpr0Node;

/**
 * An element of Abstract0 abstract domain. This element contains an {@link Abstract0} which
 * is the concrete representation of the Abstract0 and a map which
 * provides a mapping from variable names to variables.
 *
 */
public class ApronState implements AbstractState {

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

  // the Apron state representation
  private Abstract0 apronState;
  private ApronManager apronManager;

  // mapping from variable name to its identifier
  private List<String> integerToIndexMap;
  private List<String> realToIndexMap;
  private Map<String, Type> variableToTypeMap;
  private final Block block;

  private LogManager logger;

  // also top element
  public ApronState(LogManager log, ApronManager manager) {
    apronManager = manager;
    apronState = new Abstract0(apronManager.getManager(), 0, 0);
    logger = log;
    logger.log(Level.FINEST, "initial apron state");

    integerToIndexMap = new LinkedList<>();
    realToIndexMap = new LinkedList<>();
    variableToTypeMap = new HashMap<>();
    block = new Block();
  }

  public ApronState(Abstract0 apronNativeState, ApronManager manager, List<String> intMap, List<String> realMap, Map<String, Type> typeMap, Block block, LogManager log) {
    apronState = apronNativeState;
    apronManager = manager;
    integerToIndexMap = intMap;
    realToIndexMap = realMap;
    variableToTypeMap = typeMap;
    this.block = block;
    logger = log;
  }

  @Override
  public boolean equals(Object pObj) {
    // TODO loopstack
    if (!(pObj instanceof ApronState)) {
      return false;
    }
    ApronState otherApron = (ApronState) pObj;
logger.log(Level.FINEST, "apron state: isEqual");
    return Objects.equals(integerToIndexMap, otherApron.integerToIndexMap)
           && Objects.equals(realToIndexMap, otherApron.realToIndexMap)
           && this.apronState.isEqual(apronManager.getManager(), otherApron.apronState);
  }

  @Override
  public int hashCode() {
    // TODO loopstack
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(integerToIndexMap);
    result = prime * result + Objects.hash(realToIndexMap);
    result = prime * result + Objects.hashCode(variableToTypeMap);
    return result;
  }

  protected boolean isLessOrEquals(ApronState state) {
    assert !isEmpty() : "Empty states should not occur here!";
    // TODO loopstack

    if (Objects.equals(integerToIndexMap, state.integerToIndexMap)
        && Objects.equals(realToIndexMap, state.realToIndexMap)) {
      logger.log(Level.FINEST, "apron state: isIncluded");
      return apronState.isIncluded(apronManager.getManager(), state.apronState);
    } else {
      logger.log(Level.FINEST, "Removing some temporary (in the transferrelation)"
                 + " introduced variables from the Abstract0 to compute #isLessOrEquals()");

      if (integerToIndexMap.containsAll(state.integerToIndexMap)
          && realToIndexMap.containsAll(state.realToIndexMap)) {
        Pair<ApronState, ApronState> checkStates = shrinkToFittingSize(state);
        logger.log(Level.FINEST, "apron state: isIncluded");
        return checkStates.getFirst().apronState.isIncluded(apronManager.getManager(), checkStates.getSecond().apronState);
      } else {
        return false;
      }
    }
  }

  /**
   * This method forgets some information about previously tracked variables, this
   * is necessary for isLessOrEquals, or the union operator, be careful using it
   * in other ways (Variables removed from the State cannot be referenced anymore
   * by the Transferrelation)
   * @param oldState the ApronState which has the preferred size, is the parameter,
   *                 so we can check if the variables are matching if not an Exception is thrown
   * @return
   */
  public Pair<ApronState, ApronState> shrinkToFittingSize(ApronState oldState) {
    int maxEqualIntIndex = 0;
    while (maxEqualIntIndex < integerToIndexMap.size()
           && integerToIndexMap.get(maxEqualIntIndex).equals(integerToIndexMap.get(maxEqualIntIndex))) {
      maxEqualIntIndex++;
    }

    int maxEqualRealIndex = 0;
    while (maxEqualRealIndex < realToIndexMap.size()
           && realToIndexMap.get(maxEqualRealIndex).equals(realToIndexMap.get(maxEqualRealIndex))) {
      maxEqualRealIndex++;
    }

    ApronState newState1;
    if (variableToTypeMap.size() != maxEqualIntIndex  + maxEqualRealIndex) {
      List<String> newIntMap1 = integerToIndexMap.subList(0, maxEqualIntIndex);
      List<String> newRealMap1 = realToIndexMap.subList(0, maxEqualRealIndex);
      Map<String, Type> newTypeMap1 = new HashMap<>(variableToTypeMap);
      int amountRemoved = variableToTypeMap.size()-(maxEqualIntIndex + maxEqualRealIndex);
      int[] placesRemoved = new int[amountRemoved];
      int amountInts = integerToIndexMap.size() - maxEqualIntIndex;
      int amountReals = realToIndexMap.size() - maxEqualRealIndex;

      for (int i = 0; i < amountInts; i++) {
        int index = integerToIndexMap.size() - (amountInts - i);
        placesRemoved[i] = index;
        newTypeMap1.remove(integerToIndexMap.get(index));
      }
      for (int i = amountInts; i < placesRemoved.length; i++) {
        int index = placesRemoved.length - (amountReals - i);
        placesRemoved[i] = index;
        newTypeMap1.remove(realToIndexMap.get(index - amountInts));
      }
      logger.log(Level.FINEST, "apron state: removeDimensionCopy: " + new Dimchange(amountInts, amountReals, placesRemoved));
      Abstract0 newApronState1 = apronState.removeDimensionsCopy(apronManager.getManager(),
                                                                 new Dimchange(amountInts, amountReals, placesRemoved));
      newState1 =  new ApronState(newApronState1, apronManager, newIntMap1, newRealMap1, newTypeMap1, block, logger);
    } else {
      newState1 = this;
    }

    ApronState newState2;
    if (oldState.variableToTypeMap.size() != maxEqualIntIndex + maxEqualRealIndex) {
      List<String> newIntMap2 = integerToIndexMap.subList(0, maxEqualIntIndex);
      List<String> newRealMap2 = realToIndexMap.subList(0, maxEqualRealIndex);
      Map<String, Type> newTypeMap2 = new HashMap<>(variableToTypeMap);
      int amountRemoved = oldState.variableToTypeMap.size()-(maxEqualIntIndex + maxEqualRealIndex);
      int[] placesRemoved = new int[amountRemoved];
      int amountInts = oldState.integerToIndexMap.size() - maxEqualIntIndex;
      int amountReals = oldState.realToIndexMap.size() - maxEqualRealIndex;

      for (int i = 0; i < amountInts; i++) {
        int index = oldState.integerToIndexMap.size() - (amountInts - i);
        placesRemoved[i] = index;
        newTypeMap2.remove(oldState.integerToIndexMap.get(index));
      }
      for (int i = amountInts; i < placesRemoved.length; i++) {
        int index = placesRemoved.length - (amountReals - i);
        placesRemoved[i] = index;
        newTypeMap2.remove(oldState.realToIndexMap.get(index - amountInts));
      }
      logger.log(Level.FINEST, "apron state: removeDimensionCopy: " + new Dimchange(amountInts, amountReals, placesRemoved));
      Abstract0 newApronState2 =  oldState.apronState.removeDimensionsCopy(oldState.apronManager.getManager(),
                                                                           new Dimchange(amountInts, amountReals, placesRemoved));
      newState2 = new ApronState(newApronState2, oldState.apronManager, newIntMap2, newRealMap2, newTypeMap2, block, logger);
    } else {
      newState2 = oldState;
    }

    return Pair.of(newState1, newState2);
  }

  public boolean areInSameBlock(ApronState other) {
    return block.equals(other.block);
  }

  public Block getBlock() {
    return block;
  }

  @Override
  public String toString() {
    logger.log(Level.FINEST, "apron state: toString");
    return apronState.toString(apronManager.getManager());
  }

  public boolean satisfies(Tcons0 cons) {
    logger.log(Level.FINEST, "apron state: satisfy: " + cons);
    return apronState.satisfy(apronManager.getManager(), cons);
  }

  public Abstract0 getApronNativeState() {
    return apronState;
  }

  public ApronManager getManager() {
    return apronManager;
  }

  public int sizeOfVariables() {
    return variableToTypeMap.size();
  }

  public List<String> getIntegerVariableToIndexMap() {
    return integerToIndexMap;
  }

  public List<String> getRealVariableToIndexMap() {
    return realToIndexMap;
  }

  public Map<String, Type> getVariableToTypeMap() {
    return variableToTypeMap;
  }

  public boolean isEmpty() {
    logger.log(Level.FINEST, "apron state: isBottom");
    return apronState.isBottom(apronManager.getManager());
  }

  /**
   * This method sets the coefficients/ the value of a variable to undefined.
   */
  public ApronState forget(String pVariableName) {
    int varIdx = getVariableIndexFor(pVariableName);

    if (varIdx == -1) {
      return this;
    }
    logger.log(Level.FINEST, "apron state: forgetCopy: " + pVariableName);
    return new ApronState(apronState.forgetCopy(apronManager.getManager(), varIdx, false),
                          apronManager,
                          new LinkedList<>(integerToIndexMap),
                          new LinkedList<>(realToIndexMap),
                          new HashMap<>(variableToTypeMap),
                          block,
                          logger);
  }

  /**
   * Returns the index of the variable, if the variable is not in the map -1 is returned.
   */
  protected int getVariableIndexFor(String pVariableName) {

    if (integerToIndexMap.contains(pVariableName)) {
      int counter = 0;
      for (String str : integerToIndexMap) {
        if (str.equals(pVariableName)) {
          return counter;
        }
        counter++;
      }
    }

    if (realToIndexMap.contains(pVariableName)) {
      int counter = 0;
      for (String str : realToIndexMap) {
        if (str.equals(pVariableName)) {
          return counter + integerToIndexMap.size();
        }
        counter++;
      }
    }

    return -1;
  }

  /**
   * True means int, false means real
   */
  protected boolean isInt(int index) {
    return index < integerToIndexMap.size();
  }

  protected boolean existsVariable(String variableName) {
    return integerToIndexMap.contains(variableName)
           || realToIndexMap.contains(variableName);
  }

  public ApronState declareVariable(String varName, Type type){
    assert !existsVariable(varName);

    Dimchange dimch;
    int[] addPlace = new int[1];
    if (type == Type.INT) {
      addPlace[0] = integerToIndexMap.size();
      dimch = new Dimchange(1, 0, addPlace);
    } else {
      addPlace[0] = integerToIndexMap.size() + realToIndexMap.size();
      dimch = new Dimchange(0, 1, addPlace);
    }

    logger.log(Level.FINEST, "apron state: addDimensionCopy: " + varName + " " + dimch);
    ApronState newState = new ApronState(apronState.addDimensionsCopy(apronManager.getManager(), dimch, false),
                                     apronManager,
                                     new LinkedList<>(integerToIndexMap),
                                     new LinkedList<>(realToIndexMap),
                                     new HashMap<>(variableToTypeMap),
                                     block,
                                     logger);
    if (type == Type.INT) {
      newState.integerToIndexMap.add(varName);
    } else {
      newState.realToIndexMap.add(varName);
    }
    newState.variableToTypeMap.put(varName, type);
    return newState;
  }

  public ApronState makeAssignment(String leftVarName, Linexpr0 assignment) {
    int varIndex = getVariableIndexFor(leftVarName);
    if (varIndex == -1) {
      return this;
    }
    if (assignment != null) {
      logger.log(Level.FINEST, "apron state: assignCopy: " + leftVarName + " = " + assignment);
      return new ApronState(apronState.assignCopy(apronManager.getManager(), varIndex, assignment, null),
                            apronManager,
                            integerToIndexMap,
                            realToIndexMap,
                            variableToTypeMap,
                            block,
                            logger);
    } else {
      return forget(leftVarName);
    }
  }

  public ApronState makeAssignment(String leftVarName, Texpr0Node assignment) {
    return makeAssignment(leftVarName, new Texpr0Intern(assignment));
  }

  public ApronState makeAssignment(String leftVarName, Texpr0Intern assignment) {
    int varIndex = getVariableIndexFor(leftVarName);
    if (varIndex == -1) {
      return this;
    }
    if (assignment != null) {
      logger.log(Level.FINEST, "apron state: assignCopy: " + leftVarName + " = " + assignment);
      return new ApronState(apronState.assignCopy(apronManager.getManager(), varIndex, assignment, null),
                            apronManager,
                            integerToIndexMap,
                            realToIndexMap,
                            variableToTypeMap,
                            block,
                            logger);
    } else {
      return forget(leftVarName);
    }
  }


  public ApronState addConstraint(Lincons0 constraint) {
    logger.log(Level.FINEST, "apron state: meetCopy: " + constraint);
    return new ApronState(apronState.meetCopy(apronManager.getManager(), constraint),
                          apronManager,
                          integerToIndexMap,
                          realToIndexMap,
                          variableToTypeMap,
                          block,
                          logger);
  }

  public ApronState addConstraint(Tcons0 constraint) {
    logger.log(Level.FINEST, "apron state: meetCopy: " + constraint);
    return new ApronState(apronState.meetCopy(apronManager.getManager(), constraint),
                          apronManager,
                          integerToIndexMap,
                          realToIndexMap,
                          variableToTypeMap,
                          block,
                          logger);
  }

  public ApronState removeLocalVars(String functionName) {
    return removeVars(functionName, "");
  }

  public Map<String, Interval> getVariablesWithBounds() {
    logger.log(Level.FINEST, "apron state: getBounds");
    Map<String, Interval> vars = new HashMap<>();
    for (String varName : integerToIndexMap) {
      vars.put(varName, apronState.getBound(apronManager.getManager(), getVariableIndexFor(varName)));
    }
    for (String varName : realToIndexMap) {
      vars.put(varName, apronState.getBound(apronManager.getManager(), getVariableIndexFor(varName)));
    }
    return vars;
  }

  private ApronState removeVars(String functionName, String varPrefix) {
    List<String> keysToRemove = new ArrayList<>();
    int intsRemoved = 0;
    for (String var : integerToIndexMap) {
      if (var.startsWith(functionName+"::"+varPrefix)) {
        keysToRemove.add(var);
        intsRemoved++;
      }
    }

    int realsRemoved = 0;
    for (String var : realToIndexMap) {
      if (var.startsWith(functionName+"::"+varPrefix)) {
        keysToRemove.add(var);
        realsRemoved++;
      }
    }

    if (keysToRemove.size() == 0) {
      return this;
    }

    int[] placesToRemove = new int[keysToRemove.size()];
    for (int i = 0;  i < placesToRemove.length; i++) {
      placesToRemove[i] = getVariableIndexFor(keysToRemove.get(i));
    }
    logger.log(Level.FINEST, "apron state: removeDimensionCopy: " + new Dimchange(intsRemoved, realsRemoved, placesToRemove));
    ApronState newState = new ApronState(apronState.removeDimensionsCopy(apronManager.getManager(), new Dimchange(intsRemoved, realsRemoved, placesToRemove)),
                                         apronManager,
                                         new LinkedList<>(integerToIndexMap),
                                         new LinkedList<>(realToIndexMap),
                                         new HashMap<>(variableToTypeMap),
                                         block,
                                         logger);
    newState.integerToIndexMap.removeAll(keysToRemove);
    newState.realToIndexMap.removeAll(keysToRemove);
    newState.variableToTypeMap.keySet().removeAll(keysToRemove);

    logger.log(Level.FINEST, "apron state: getDimension");
    Dimension dim = newState.apronState.getDimension(apronManager.getManager());
    assert dim.intDim + dim.realDim == newState.sizeOfVariables();
    return newState;
  }
}
