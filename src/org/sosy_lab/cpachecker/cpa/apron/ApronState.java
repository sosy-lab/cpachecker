// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.apron;

import apron.Abstract0;
import apron.Dimchange;
import apron.Dimension;
import apron.DoubleScalar;
import apron.Interval;
import apron.Lincons0;
import apron.Linexpr0;
import apron.MpfrScalar;
import apron.MpqScalar;
import apron.Scalar;
import apron.Tcons0;
import apron.Texpr0BinNode;
import apron.Texpr0CstNode;
import apron.Texpr0DimNode;
import apron.Texpr0Intern;
import apron.Texpr0Node;
import apron.Texpr0UnNode;
import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;
import gmp.Mpfr;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.util.ApronManager;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.globalinfo.SerializationInfoStorage;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

/**
 * An element of Abstract0 abstract domain. This element contains an {@link Abstract0} which is the
 * concrete representation of the Abstract0 and a map which provides a mapping from variable names
 * to variables.
 */
public class ApronState implements AbstractState, Serializable, FormulaReportingState {

  @Serial private static final long serialVersionUID = -7953805400649927048L;

  enum Type {
    INT,
    FLOAT
  }

  // the Apron state representation
  private transient Abstract0 apronState;
  private transient ApronManager apronManager;

  // mapping from variable name to its identifier
  private List<MemoryLocation> integerToIndexMap;
  private List<MemoryLocation> realToIndexMap;
  private Map<MemoryLocation, Type> variableToTypeMap;
  private final boolean isLoopHead;

  private transient LogManager logger;

  // also top element
  public ApronState(LogManager log, ApronManager manager) {
    apronManager = manager;
    try {
      apronState = new Abstract0(apronManager.getManager(), 0, 0);
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
    logger = log;
    logger.log(Level.FINEST, "initial apron state");

    integerToIndexMap = new ArrayList<>();
    realToIndexMap = new ArrayList<>();
    variableToTypeMap = new HashMap<>();
    isLoopHead = false;
  }

  public ApronState(
      Abstract0 apronNativeState,
      ApronManager manager,
      List<MemoryLocation> intMap,
      List<MemoryLocation> realMap,
      Map<MemoryLocation, Type> typeMap,
      boolean pIsLoopHead,
      LogManager log) {
    apronState = apronNativeState;
    apronManager = manager;
    integerToIndexMap = intMap;
    realToIndexMap = realMap;
    variableToTypeMap = typeMap;
    isLoopHead = pIsLoopHead;
    logger = log;
  }

  public boolean isLoopHead() {
    return isLoopHead;
  }

  public ApronState asLoopHead() {
    return new ApronState(
        apronState,
        apronManager,
        integerToIndexMap,
        realToIndexMap,
        variableToTypeMap,
        isLoopHead,
        logger);
  }

  @Override
  public boolean equals(Object pObj) {
    // TODO loopstack
    if (!(pObj instanceof ApronState otherApron)) {
      return false;
    }

    logger.log(Level.FINEST, "apron state: isEqual");
    boolean result;
    try {
      result =
          Objects.equals(integerToIndexMap, otherApron.integerToIndexMap)
              && Objects.equals(realToIndexMap, otherApron.realToIndexMap)
              && apronState.isEqual(apronManager.getManager(), otherApron.apronState)
              && isLoopHead == otherApron.isLoopHead;
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
    return result;
  }

  @Override
  public int hashCode() {
    // TODO loopstack
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(integerToIndexMap);
    result = prime * result + Objects.hash(realToIndexMap);
    result = prime * result + Objects.hashCode(variableToTypeMap);
    result = prime * result + Objects.hash(isLoopHead);
    return result;
  }

  protected boolean isLessOrEquals(ApronState state) {
    assert !isEmpty() : "Empty states should not occur here!";
    // TODO loopstack

    if (Objects.equals(integerToIndexMap, state.integerToIndexMap)
        && Objects.equals(realToIndexMap, state.realToIndexMap)) {
      boolean isIncluded;
      logger.log(Level.FINEST, "apron state: isIncluded");
      try {
        isIncluded = apronState.isIncluded(apronManager.getManager(), state.apronState);
      } catch (apron.ApronException e) {
        throw new RuntimeException("An error occured while operating with the apron library", e);
      }
      return isIncluded;
    } else {
      logger.log(
          Level.FINEST,
          "Removing some temporary (in the transferrelation)"
              + " introduced variables from the Abstract0 to compute #isLessOrEquals()");

      if (integerToIndexMap.containsAll(state.integerToIndexMap)
          && realToIndexMap.containsAll(state.realToIndexMap)) {
        logger.log(Level.FINEST, "apron state: isIncluded");
        boolean isIncluded;
        try {
          isIncluded = forgetVars(state).isIncluded(apronManager.getManager(), state.apronState);
        } catch (apron.ApronException e) {
          throw new RuntimeException("An error occured while operating with the apron library", e);
        }
        return isIncluded;
      } else {
        return false;
      }
    }
  }

  private Abstract0 forgetVars(ApronState pConsiderSubsetOfVars) {
    int amountInts = integerToIndexMap.size() - pConsiderSubsetOfVars.integerToIndexMap.size();
    int[] removeDim =
        new int[amountInts + realToIndexMap.size() - pConsiderSubsetOfVars.realToIndexMap.size()];

    int arrayPos = 0;

    for (int indexThis = 0, indexParam = 0; indexThis < integerToIndexMap.size(); ) {
      if (indexParam < pConsiderSubsetOfVars.integerToIndexMap.size()
          && integerToIndexMap
              .get(indexThis)
              .equals(pConsiderSubsetOfVars.integerToIndexMap.get(indexParam))) {
        indexParam++;
      } else {
        removeDim[arrayPos] = indexThis;
        arrayPos++;
      }
      indexThis++;
    }

    for (int indexThis = 0, indexParam = 0; indexThis < realToIndexMap.size(); ) {
      if (indexParam < pConsiderSubsetOfVars.realToIndexMap.size()
          && realToIndexMap
              .get(indexThis)
              .equals(pConsiderSubsetOfVars.realToIndexMap.get(indexParam))) {
        indexParam++;
      } else {
        removeDim[arrayPos] = indexThis;
        arrayPos++;
      }
      indexThis++;
    }
    Abstract0 newApronState;
    try {
      newApronState =
          apronState.removeDimensionsCopy(
              apronManager.getManager(),
              new Dimchange(amountInts, removeDim.length - amountInts, removeDim));
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
    return newApronState;
  }

  /**
   * This method forgets some information about previously tracked variables, this is necessary for
   * isLessOrEquals, or the union operator, be careful using it in other ways (Variables removed
   * from the State cannot be referenced anymore by the Transferrelation)
   *
   * @param oldState the ApronState which has the preferred size, is the parameter, so we can check
   *     if the variables are matching if not an Exception is thrown
   * @return a pair of the shrinked caller and the shrinked stated
   */
  public Pair<ApronState, ApronState> shrinkToFittingSize(ApronState oldState) {
    int maxEqualIntIndex = 0;
    while (maxEqualIntIndex < integerToIndexMap.size()
        && integerToIndexMap
            .get(maxEqualIntIndex)
            .equals(integerToIndexMap.get(maxEqualIntIndex))) {
      maxEqualIntIndex++;
    }

    int maxEqualRealIndex = 0;
    while (maxEqualRealIndex < realToIndexMap.size()
        && realToIndexMap.get(maxEqualRealIndex).equals(realToIndexMap.get(maxEqualRealIndex))) {
      maxEqualRealIndex++;
    }

    ApronState newState1;
    if (variableToTypeMap.size() != maxEqualIntIndex + maxEqualRealIndex) {
      List<MemoryLocation> newIntMap1 = integerToIndexMap.subList(0, maxEqualIntIndex);
      List<MemoryLocation> newRealMap1 = realToIndexMap.subList(0, maxEqualRealIndex);
      Map<MemoryLocation, Type> newTypeMap1 = new HashMap<>(variableToTypeMap);
      int amountRemoved = variableToTypeMap.size() - (maxEqualIntIndex + maxEqualRealIndex);
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
      logger.log(
          Level.FINEST,
          "apron state: removeDimensionCopy: "
              + new Dimchange(amountInts, amountReals, placesRemoved));

      Abstract0 newApronState1;
      try {
        newApronState1 =
            apronState.removeDimensionsCopy(
                apronManager.getManager(), new Dimchange(amountInts, amountReals, placesRemoved));
      } catch (apron.ApronException e) {
        throw new RuntimeException("An error occured while operating with the apron library", e);
      }

      newState1 =
          new ApronState(
              newApronState1,
              apronManager,
              newIntMap1,
              newRealMap1,
              newTypeMap1,
              isLoopHead,
              logger);
    } else {
      newState1 = this;
    }

    ApronState newState2;
    if (oldState.variableToTypeMap.size() != maxEqualIntIndex + maxEqualRealIndex) {
      List<MemoryLocation> newIntMap2 = integerToIndexMap.subList(0, maxEqualIntIndex);
      List<MemoryLocation> newRealMap2 = realToIndexMap.subList(0, maxEqualRealIndex);
      Map<MemoryLocation, Type> newTypeMap2 = new HashMap<>(variableToTypeMap);
      int amountRemoved =
          oldState.variableToTypeMap.size() - (maxEqualIntIndex + maxEqualRealIndex);
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
      logger.log(
          Level.FINEST,
          "apron state: removeDimensionCopy: "
              + new Dimchange(amountInts, amountReals, placesRemoved));

      Abstract0 newApronState2;
      try {
        newApronState2 =
            oldState.apronState.removeDimensionsCopy(
                oldState.apronManager.getManager(),
                new Dimchange(amountInts, amountReals, placesRemoved));
      } catch (apron.ApronException e) {
        throw new RuntimeException("An error occured while operating with the apron library", e);
      }

      newState2 =
          new ApronState(
              newApronState2,
              oldState.apronManager,
              newIntMap2,
              newRealMap2,
              newTypeMap2,
              isLoopHead,
              logger);
    } else {
      newState2 = oldState;
    }

    return Pair.of(newState1, newState2);
  }

  @Override
  public String toString() {
    logger.log(Level.FINEST, "apron state: toString");
    return apronState.toString(apronManager.getManager());
  }

  public boolean satisfies(Tcons0 cons) {
    logger.log(Level.FINEST, "apron state: satisfy: " + cons);
    boolean satisfies;
    try {
      satisfies = apronState.satisfy(apronManager.getManager(), cons);
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
    return satisfies;
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

  public List<MemoryLocation> getIntegerVariableToIndexMap() {
    return integerToIndexMap;
  }

  public List<MemoryLocation> getRealVariableToIndexMap() {
    return realToIndexMap;
  }

  public Map<MemoryLocation, Type> getVariableToTypeMap() {
    return variableToTypeMap;
  }

  public boolean isEmpty() {
    logger.log(Level.FINEST, "apron state: isBottom");
    boolean isEmpty;
    try {
      isEmpty = apronState.isBottom(apronManager.getManager());
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
    return isEmpty;
  }

  /** This method sets the coefficients/ the value of a variable to undefined. */
  public ApronState forget(MemoryLocation pVariableName) {
    int varIdx = getVariableIndexFor(pVariableName);

    if (varIdx == -1) {
      return this;
    }
    logger.log(Level.FINEST, "apron state: forgetCopy: " + pVariableName);
    Abstract0 newApronState;
    try {
      newApronState = apronState.forgetCopy(apronManager.getManager(), varIdx, false);
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
    return new ApronState(
        newApronState,
        apronManager,
        new ArrayList<>(integerToIndexMap),
        new ArrayList<>(realToIndexMap),
        new HashMap<>(variableToTypeMap),
        false,
        logger);
  }

  /** Returns the index of the variable, if the variable is not in the map -1 is returned. */
  protected int getVariableIndexFor(MemoryLocation pVariableName) {

    if (integerToIndexMap.contains(pVariableName)) {
      int counter = 0;
      for (MemoryLocation str : integerToIndexMap) {
        if (str.equals(pVariableName)) {
          return counter;
        }
        counter++;
      }
    }

    if (realToIndexMap.contains(pVariableName)) {
      int counter = 0;
      for (MemoryLocation str : realToIndexMap) {
        if (str.equals(pVariableName)) {
          return counter + integerToIndexMap.size();
        }
        counter++;
      }
    }

    return -1;
  }

  /** True means int, false means real */
  protected boolean isInt(int index) {
    return index < integerToIndexMap.size();
  }

  protected boolean existsVariable(MemoryLocation variableName) {
    return integerToIndexMap.contains(variableName) || realToIndexMap.contains(variableName);
  }

  public ApronState declareVariable(MemoryLocation varName, Type type) {
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
    Abstract0 newApronState;
    try {
      newApronState = apronState.addDimensionsCopy(apronManager.getManager(), dimch, false);
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
    ApronState newState =
        new ApronState(
            newApronState,
            apronManager,
            new ArrayList<>(integerToIndexMap),
            new ArrayList<>(realToIndexMap),
            new HashMap<>(variableToTypeMap),
            false,
            logger);
    if (type == Type.INT) {
      newState.integerToIndexMap.add(varName);
    } else {
      newState.realToIndexMap.add(varName);
    }
    newState.variableToTypeMap.put(varName, type);
    return newState;
  }

  public ApronState makeAssignment(MemoryLocation leftVarName, Linexpr0 assignment) {
    int varIndex = getVariableIndexFor(leftVarName);
    if (varIndex == -1) {
      return this;
    }
    if (assignment != null) {
      logger.log(Level.FINEST, "apron state: assignCopy: " + leftVarName + " = " + assignment);
      Abstract0 newApronState;
      try {
        newApronState =
            apronState.assignCopy(apronManager.getManager(), varIndex, assignment, null);
      } catch (apron.ApronException e) {
        throw new RuntimeException("An error occured while operating with the apron library", e);
      }
      return new ApronState(
          newApronState,
          apronManager,
          integerToIndexMap,
          realToIndexMap,
          variableToTypeMap,
          false,
          logger);
    } else {
      return forget(leftVarName);
    }
  }

  public ApronState makeAssignment(MemoryLocation leftVarName, Texpr0Node assignment) {
    return makeAssignment(leftVarName, new Texpr0Intern(assignment));
  }

  public ApronState makeAssignment(MemoryLocation leftVarName, Texpr0Intern assignment) {
    int varIndex = getVariableIndexFor(leftVarName);
    if (varIndex == -1) {
      return this;
    }
    if (assignment != null) {
      logger.log(Level.FINEST, "apron state: assignCopy: " + leftVarName + " = " + assignment);
      Abstract0 retState;
      try {
        retState = apronState.assignCopy(apronManager.getManager(), varIndex, assignment, null);
      } catch (apron.ApronException e) {
        throw new RuntimeException("An error occured while operating with the apron library", e);
      }

      if (retState == null) {
        logger.log(
            Level.WARNING,
            "Assignment of expression to variable yielded an empty state,"
                + " forgetting the value of the variable as fallback.");
        return forget(leftVarName);
      }

      return new ApronState(
          retState,
          apronManager,
          integerToIndexMap,
          realToIndexMap,
          variableToTypeMap,
          false,
          logger);
    } else {
      return forget(leftVarName);
    }
  }

  public ApronState addConstraint(Lincons0 constraint) {
    logger.log(Level.FINEST, "apron state: meetCopy: " + constraint);
    Abstract0 newApronState;
    try {
      newApronState = apronState.meetCopy(apronManager.getManager(), constraint);
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
    return new ApronState(
        newApronState,
        apronManager,
        integerToIndexMap,
        realToIndexMap,
        variableToTypeMap,
        false,
        logger);
  }

  public ApronState addConstraint(Tcons0 constraint) {
    logger.log(Level.FINEST, "apron state: meetCopy: " + constraint);
    Abstract0 newApronState;
    try {
      newApronState = apronState.meetCopy(apronManager.getManager(), constraint);
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
    return new ApronState(
        newApronState,
        apronManager,
        integerToIndexMap,
        realToIndexMap,
        variableToTypeMap,
        false,
        logger);
  }

  public ApronState removeLocalVars(String functionName) {
    return removeVars(functionName + "::");
  }

  public Map<MemoryLocation, Interval> getVariablesWithBounds() {
    logger.log(Level.FINEST, "apron state: getBounds");
    Map<MemoryLocation, Interval> vars = new HashMap<>();
    for (MemoryLocation varName : integerToIndexMap) {
      try {
        vars.put(
            varName, apronState.getBound(apronManager.getManager(), getVariableIndexFor(varName)));
      } catch (apron.ApronException e) {
        throw new RuntimeException("An error occured while operating with the apron library", e);
      }
    }
    for (MemoryLocation varName : realToIndexMap) {
      try {
        vars.put(
            varName, apronState.getBound(apronManager.getManager(), getVariableIndexFor(varName)));
      } catch (apron.ApronException e) {
        throw new RuntimeException("An error occured while operating with the apron library", e);
      }
    }
    return vars;
  }

  private ApronState removeVars(String varPrefix) {
    List<MemoryLocation> keysToRemove = new ArrayList<>();
    int intsRemoved = 0;
    for (MemoryLocation var : integerToIndexMap) {
      if (var.getExtendedQualifiedName().startsWith(varPrefix)) {
        keysToRemove.add(var);
        intsRemoved++;
      }
    }

    int realsRemoved = 0;
    for (MemoryLocation var : realToIndexMap) {
      if (var.getExtendedQualifiedName().startsWith(varPrefix)) {
        keysToRemove.add(var);
        realsRemoved++;
      }
    }

    if (keysToRemove.isEmpty()) {
      return this;
    }

    int[] placesToRemove = new int[keysToRemove.size()];
    for (int i = 0; i < placesToRemove.length; i++) {
      placesToRemove[i] = getVariableIndexFor(keysToRemove.get(i));
    }
    logger.log(
        Level.FINEST,
        "apron state: removeDimensionCopy: "
            + new Dimchange(intsRemoved, realsRemoved, placesToRemove));

    ApronState newState;
    try {
      newState =
          new ApronState(
              apronState.removeDimensionsCopy(
                  apronManager.getManager(),
                  new Dimchange(intsRemoved, realsRemoved, placesToRemove)),
              apronManager,
              new ArrayList<>(integerToIndexMap),
              new ArrayList<>(realToIndexMap),
              new HashMap<>(variableToTypeMap),
              false,
              logger);
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }

    newState.integerToIndexMap.removeAll(keysToRemove);
    newState.realToIndexMap.removeAll(keysToRemove);
    newState.variableToTypeMap.keySet().removeAll(keysToRemove);

    logger.log(Level.FINEST, "apron state: getDimension");
    Dimension dim;
    try {
      dim = newState.apronState.getDimension(apronManager.getManager());
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
    assert dim.intDim + dim.realDim == newState.sizeOfVariables();
    return newState;
  }

  @Serial
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    byte[] serialized;
    try {
      serialized = apronState.serialize(apronManager.getManager());
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
    out.writeInt(serialized.length);
    out.write(serialized);
  }

  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();

    logger = SerializationInfoStorage.getInstance().getApronLogManager();
    apronManager = SerializationInfoStorage.getInstance().getApronManager();

    byte[] deserialized = new byte[in.readInt()];
    in.readFully(deserialized);
    try {
      apronState = Abstract0.deserialize(apronManager.getManager(), deserialized);
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView pManager) {
    BitvectorFormulaManager bitFmgr = pManager.getBitvectorFormulaManager();
    BooleanFormulaManager bFmgr = pManager.getBooleanFormulaManager();
    Tcons0[] constraints;
    try {
      constraints = apronState.toTcons(apronManager.getManager());
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }

    return bFmgr.and(
        Lists.transform(
            Arrays.asList(constraints), cons -> createFormula(bFmgr, bitFmgr, cons, true)));
  }

  private BooleanFormula createFormula(
      BooleanFormulaManager bFmgr,
      final BitvectorFormulaManager bitFmgr,
      final Tcons0 constraint,
      final boolean useQualifiedVarNames) {
    Texpr0Node tree = constraint.toTexpr0Node();
    BitvectorFormula formula =
        new Texpr0ToFormulaVisitor(bitFmgr, useQualifiedVarNames).visit(tree);

    // TODO fix size, machinemodel needed?
    BitvectorFormula rightHandside = bitFmgr.makeBitvector(32, 0);
    return switch (constraint.kind) {
      case Tcons0.DISEQ -> bFmgr.not(bitFmgr.equal(formula, rightHandside));
      case Tcons0.EQ -> bitFmgr.equal(formula, rightHandside);
      case Tcons0.SUP -> bitFmgr.greaterThan(formula, rightHandside, true);
      case Tcons0.SUPEQ -> bitFmgr.greaterOrEquals(formula, rightHandside, true);
      default -> throw new AssertionError("unhandled constraint kind");
    };
  }

  @Override
  public BooleanFormula getScopedFormulaApproximation(
      final FormulaManagerView pManager, final FunctionEntryNode pFunctionScope) {
    BitvectorFormulaManager bitFmgr = pManager.getBitvectorFormulaManager();
    BooleanFormulaManager bFmgr = pManager.getBooleanFormulaManager();
    Tcons0[] constraints;
    try {
      constraints = apronState.toTcons(apronManager.getManager());
    } catch (apron.ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }

    Texpr0ScopeCheckVisitor scopeChecker =
        new Texpr0ScopeCheckVisitor(pFunctionScope.getFunctionName());
    return bFmgr.and(
        Lists.transform(
            Arrays.asList(constraints).stream()
                .filter(constraint -> scopeChecker.visit(constraint.toTexpr0Node()))
                .toList(),
            cons -> createFormula(bFmgr, bitFmgr, cons, false)));
  }

  abstract static class Texpr0NodeTraversal<T> {

    T visit(Texpr0Node node) {
      if (node instanceof Texpr0BinNode texpr0BinNode) {
        return visit(texpr0BinNode);
      } else if (node instanceof Texpr0CstNode texpr0CstNode) {
        return visit(texpr0CstNode);
      } else if (node instanceof Texpr0DimNode texpr0DimNode) {
        return visit(texpr0DimNode);
      } else if (node instanceof Texpr0UnNode texpr0UnNode) {
        return visit(texpr0UnNode);
      }

      throw new AssertionError("Unhandled Texpr0Node subclass.");
    }

    abstract T visit(Texpr0BinNode node);

    abstract T visit(Texpr0CstNode node);

    abstract T visit(Texpr0DimNode node);

    abstract T visit(Texpr0UnNode node);
  }

  class Texpr0ToFormulaVisitor extends Texpr0NodeTraversal<BitvectorFormula> {

    BitvectorFormulaManager bitFmgr;
    final boolean useQualifiedNames;

    public Texpr0ToFormulaVisitor(BitvectorFormulaManager pBitFmgr, boolean pUseQualifiedVarNames) {
      bitFmgr = pBitFmgr;
      useQualifiedNames = pUseQualifiedVarNames;
    }

    @Override
    BitvectorFormula visit(Texpr0BinNode pNode) {
      BitvectorFormula left = visit(pNode.getLeftArgument());
      BitvectorFormula right = visit(pNode.getRightArgument());
      return switch (pNode.getOperation()) {
        case Texpr0BinNode.OP_ADD -> bitFmgr.add(left, right);
        case Texpr0BinNode.OP_DIV -> bitFmgr.divide(left, right, true);
        case Texpr0BinNode.OP_MOD -> bitFmgr.remainder(left, right, true);
        case Texpr0BinNode.OP_SUB -> bitFmgr.subtract(left, right);
        case Texpr0BinNode.OP_MUL -> bitFmgr.multiply(left, right);
        case Texpr0BinNode.OP_POW ->
            throw new AssertionError("Pow not implemented in this visitor");
        default -> throw new AssertionError("Unhandled operator for binary nodes.");
      };
    }

    @Override
    BitvectorFormula visit(Texpr0CstNode pNode) {
      if (pNode.isScalar()) {
        double value;
        Scalar scalar = pNode.getConstant().inf();
        if (scalar instanceof DoubleScalar doubleScalar) {
          value = doubleScalar.get();
        } else if (scalar instanceof MpqScalar mpqScalar) {
          value = mpqScalar.get().doubleValue();
        } else if (scalar instanceof MpfrScalar mpfrScalar) {
          value = mpfrScalar.get().doubleValue(Mpfr.RNDN);
        } else {
          throw new AssertionError("Unhandled Scalar subclass: " + scalar.getClass());
        }
        if (DoubleMath.isMathematicalInteger(value)) {
          // TODO fix size, machineModel needed?
          return bitFmgr.makeBitvector(32, (int) value);
        } else {
          throw new AssertionError("Floats are currently not handled");
        }

      } else {
        // this is an interval and cannot be handled here because we need
        // the other side of the operator to create > or < constraints
        throw new AssertionError("Intervals are currently not handled");
      }
    }

    @Override
    BitvectorFormula visit(Texpr0DimNode pNode) {

      // TODO fix size, machinemodel needed?
      if (isInt(pNode.dim)) {
        return bitFmgr.makeVariable(
            32,
            useQualifiedNames
                ? integerToIndexMap.get(pNode.dim).getExtendedQualifiedName()
                : integerToIndexMap.get(pNode.dim).getIdentifier());
      } else {
        return bitFmgr.makeVariable(
            32,
            useQualifiedNames
                ? realToIndexMap
                    .get(pNode.dim - integerToIndexMap.size())
                    .getExtendedQualifiedName()
                : realToIndexMap.get(pNode.dim - integerToIndexMap.size()).getIdentifier());
      }
    }

    @Override
    BitvectorFormula visit(Texpr0UnNode pNode) {
      BitvectorFormula operand = visit(pNode.getArgument());
      switch (pNode.getOperation()) {
        case Texpr0UnNode.OP_NEG -> {
          return bitFmgr.negate(operand);
        }
        case Texpr0UnNode.OP_SQRT ->
            throw new AssertionError("sqrt not implemented in this visitor");
        default -> {
          // nothing to do here, we ignore casts
        }
      }
      return operand;
    }
  }

  class Texpr0ScopeCheckVisitor extends Texpr0NodeTraversal<Boolean> {

    private String funScope;

    public Texpr0ScopeCheckVisitor(final String pFunScope) {
      funScope = pFunScope;
    }

    private boolean isInScope(final MemoryLocation memLoc) {
      return !memLoc.isReference()
          && (!memLoc.isOnFunctionStack() || memLoc.isOnFunctionStack(funScope));
    }

    @Override
    Boolean visit(Texpr0BinNode pNode) {
      return visit(pNode.getLeftArgument()) && visit(pNode.getRightArgument());
    }

    @Override
    Boolean visit(Texpr0CstNode pNode) {
      return true;
    }

    @Override
    Boolean visit(Texpr0DimNode pNode) {
      if (isInt(pNode.dim)) {
        return isInScope(integerToIndexMap.get(pNode.dim));
      } else {
        return isInScope(realToIndexMap.get(pNode.dim - integerToIndexMap.size()));
      }
    }

    @Override
    Boolean visit(Texpr0UnNode pNode) {
      return visit(pNode.getArgument());
    }
  }
}
