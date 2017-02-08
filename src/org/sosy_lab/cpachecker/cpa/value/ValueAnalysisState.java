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
package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.PseudoPartitionable;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.refinement.ForgetfulState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;

public class ValueAnalysisState
    implements AbstractQueryableState, FormulaReportingState,
        ForgetfulState<ValueAnalysisInformation>, Serializable, Graphable,
        LatticeAbstractState<ValueAnalysisState>, PseudoPartitionable {

  private static final long serialVersionUID = -3152134511524554357L;

  private static final Set<MemoryLocation> blacklist = new HashSet<>();

  static void addToBlacklist(MemoryLocation var) {
    blacklist.add(checkNotNull(var));
  }

  /**
   * the map that keeps the name of variables and their constant values (concrete and symbolic ones)
   */
  private PersistentMap<MemoryLocation, Value> constantsMap;

  private final @Nullable MachineModel machineModel;

  private transient PersistentMap<MemoryLocation, Type> memLocToType = PathCopyingPersistentTreeMap.of();

  public ValueAnalysisState(MachineModel pMachineModel) {
    this(
        checkNotNull(pMachineModel),
        PathCopyingPersistentTreeMap.of(),
        PathCopyingPersistentTreeMap.of());
  }

  public ValueAnalysisState(
      Optional<MachineModel> pMachineModel,
      PersistentMap<MemoryLocation, Value> pConstantsMap,
      PersistentMap<MemoryLocation, Type> pLocToTypeMap) {
    this(pMachineModel.orElse(null), pConstantsMap, pLocToTypeMap);
  }

  private ValueAnalysisState(
      @Nullable MachineModel pMachineModel,
      PersistentMap<MemoryLocation, Value> pConstantsMap,
      PersistentMap<MemoryLocation, Type> pLocToTypeMap) {
    machineModel = pMachineModel;
    constantsMap = checkNotNull(pConstantsMap);
    memLocToType = checkNotNull(pLocToTypeMap);
  }

  public static ValueAnalysisState copyOf(ValueAnalysisState state) {
    return new ValueAnalysisState(state.machineModel, state.constantsMap, state.memLocToType);
  }

  /**
   * This method assigns a value to the variable and puts it in the map.
   *
   * @param variableName name of the variable.
   * @param value value to be assigned.
   */
  void assignConstant(String variableName, Value value) {
    if (blacklist.contains(MemoryLocation.valueOf(variableName))) {
      return;
    }

    addToConstantsMap(MemoryLocation.valueOf(variableName), value);
  }

  private void addToConstantsMap(final MemoryLocation pMemLoc, final Value pValue) {
    Value valueToAdd = pValue;

    if (valueToAdd instanceof SymbolicValue) {
      valueToAdd = ((SymbolicValue) valueToAdd).copyForLocation(pMemLoc);
    }

    constantsMap = constantsMap.putAndCopy(pMemLoc, checkNotNull(valueToAdd));
  }

  /**
   * This method assigns a value to the variable and puts it in the map.
   *
   * @param pMemoryLocation the location in the memory.
   * @param value value to be assigned.
   * @param pType the type of <code>value</code>.
   */
  public void assignConstant(MemoryLocation pMemoryLocation, Value value, Type pType) {
    if (blacklist.contains(pMemoryLocation)) {
      return;
    }

    addToConstantsMap(pMemoryLocation, value);
    memLocToType = memLocToType.putAndCopy(pMemoryLocation, pType);
  }

  /**
   * This method assigns a concrete value to the given {@link SymbolicIdentifier}.
   *
   * @param pSymbolicIdentifier the <code>SymbolicIdentifier</code> to assign the concrete value to.
   * @param pValue value to be assigned.
   */
  public void assignConstant(SymbolicIdentifier pSymbolicIdentifier, Value pValue) {
    for (Map.Entry<MemoryLocation, Value> entry : constantsMap.entrySet()) {
      MemoryLocation currMemloc = entry.getKey();
      Value currVal = entry.getValue();

      if (currVal instanceof ConstantSymbolicExpression) {
        currVal = ((ConstantSymbolicExpression) currVal).getValue();
      }

      if (currVal instanceof SymbolicIdentifier
          && ((SymbolicIdentifier) currVal).getId() == pSymbolicIdentifier.getId()) {

        assignConstant(currMemloc, pValue, getTypeForMemoryLocation(currMemloc));
      }
    }
  }

  /**
   * This method removes a variable from the underlying map and returns the removed value.
   *
   * @param variableName the name of the variable to remove
   * @return the value of the removed variable
   */
  public ValueAnalysisInformation forget(String variableName) {
    return forget(MemoryLocation.valueOf(variableName));
  }

  /**
   * This method removes a memory location from the underlying map and returns the removed value.
   *
   * @param pMemoryLocation the name of the memory location to remove
   * @return the value of the removed memory location
   */
  @Override
  public ValueAnalysisInformation forget(MemoryLocation pMemoryLocation) {

    if (!constantsMap.containsKey(pMemoryLocation)) {
      return ValueAnalysisInformation.EMPTY;
    }

    Value value = constantsMap.get(pMemoryLocation);
    Type type = memLocToType.get(pMemoryLocation);
    constantsMap = constantsMap.removeAndCopy(pMemoryLocation);
    memLocToType = memLocToType.removeAndCopy(pMemoryLocation);

    PersistentMap<MemoryLocation, Type> typeAssignment = PathCopyingPersistentTreeMap.of();
    if (type != null) {
      typeAssignment = typeAssignment.putAndCopy(pMemoryLocation, type);
    }
    PersistentMap<MemoryLocation, Value> valueAssignment = PathCopyingPersistentTreeMap.of();
    valueAssignment = valueAssignment.putAndCopy(pMemoryLocation, value);

    return new ValueAnalysisInformation(valueAssignment, typeAssignment);
  }

  @Override
  public void remember(final MemoryLocation pLocation, final ValueAnalysisInformation pValueAndType) {
    final Value value = pValueAndType.getAssignments().get(pLocation);
    final Type valueType = pValueAndType.getLocationTypes().get(pLocation);

    assignConstant(pLocation, value, valueType);
  }

  /**
   * This method retains all variables and their respective values in the underlying map, while removing all others.
   *
   * @param toRetain the names of the variables to retain
   */
  public void retainAll(Set<MemoryLocation> toRetain) {
    Set<MemoryLocation> toRemove = new HashSet<>();
    for (MemoryLocation memoryLocation : constantsMap.keySet()) {
      if (!toRetain.contains(memoryLocation)) {
        toRemove.add(memoryLocation);
      }
    }

    for (MemoryLocation memoryLocation : toRemove) {
      forget(memoryLocation);
    }
  }

  /**
   * This method drops all entries belonging to the stack frame of a function. This method should be called right before leaving a function.
   *
   * @param functionName the name of the function that is about to be left
   */
  void dropFrame(String functionName) {
    for (MemoryLocation variableName : constantsMap.keySet()) {
      if (variableName.isOnFunctionStack(functionName)) {
        forget(variableName);
      }
    }
  }

  /**
   * This method returns the value for the given variable.
   *
   * @param variableName the name of the variable for which to get the value
   * @throws NullPointerException - if no value is present in this state for the given variable
   * @return the value associated with the given variable
   */
  public Value getValueFor(String variableName) {
    return getValueFor(MemoryLocation.valueOf(variableName));
  }

  /**
   * This method returns the value for the given variable.
   *
   * @param variableName the name of the variable for which to get the value
   * @throws NullPointerException - if no value is present in this state for the given variable
   * @return the value associated with the given variable
   */
  public Value getValueFor(MemoryLocation variableName) {
    Value value = constantsMap.get(variableName);

    return checkNotNull(value);
  }

  /**
   * This method returns the type for the given memory location.
   *
   * @param loc the memory location for which to get the type
   * @throws NullPointerException - if no type is present in this state for the given memory location
   * @return the type associated with the given memory location
   */
  public Type getTypeForMemoryLocation(MemoryLocation loc) {
    return memLocToType.get(loc);
  }

  /**
   * This method checks whether or not the given variable is contained in this state.
   *
   * @param variableName the name of variable to check for
   * @return true, if the variable is contained, else false
   */
  public boolean contains(String variableName) {
    return contains(MemoryLocation.valueOf(variableName));
  }

  /**
   * This method checks whether or not the given Memory Location
   * is contained in this state.
   *
   * @param pMemoryLocation the location in the Memory to check for
   * @return true, if the variable is contained, else false
   */
  public boolean contains(MemoryLocation pMemoryLocation) {
    return constantsMap.containsKey(pMemoryLocation);
  }

  /**
   * This method determines the total number of variables contained in this state.
   *
   * @return the total number of variables contained in this state
   */
  @Override
  public int getSize() {
    return constantsMap.size();
  }

  /**
   * This method determines the number of global variables contained in this state.
   *
   * @return the number of global variables contained in this state
   */
  int getNumberOfGlobalVariables() {
    int numberOfGlobalVariables = 0;

    for (MemoryLocation variableName : constantsMap.keySet()) {
      if (!variableName.isOnFunctionStack()) {
        numberOfGlobalVariables++;
      }
    }

    return numberOfGlobalVariables;
  }

  /**
   * This element joins this element with another element.
   *
   * @param reachedState the other element to join with this element
   * @return a new state representing the join of this element and the other element
   */
  @Override
  public ValueAnalysisState join(ValueAnalysisState reachedState) {
    PersistentMap<MemoryLocation, Value> newConstantsMap = PathCopyingPersistentTreeMap.of();
    PersistentMap<MemoryLocation, Type> newlocToTypeMap = PathCopyingPersistentTreeMap.of();

    for (Map.Entry<MemoryLocation, Value> otherEntry : reachedState.constantsMap.entrySet()) {
      MemoryLocation key = otherEntry.getKey();

      if (Objects.equals(otherEntry.getValue(), constantsMap.get(key))) {
        newConstantsMap = newConstantsMap.putAndCopy(key, otherEntry.getValue());
        newlocToTypeMap = newlocToTypeMap.putAndCopy(key, memLocToType.get(key));
      }
    }

    // return the reached state if both maps are equal
    if (newConstantsMap.size() == reachedState.constantsMap.size()) {
      return reachedState;
    } else {
      return new ValueAnalysisState(machineModel, newConstantsMap, newlocToTypeMap);
    }
  }

  /**
   * This method decides if this element is less or equal than the other element, based on the order imposed by the lattice.
   *
   * @param other the other element
   * @return true, if this element is less or equal than the other element, based on the order imposed by the lattice
   */
  @Override
  public boolean isLessOrEqual(ValueAnalysisState other) {

    // also, this element is not less or equal than the other element, if it contains less elements
    if (constantsMap.size() < other.constantsMap.size()) {
      return false;
    }

    // also, this element is not less or equal than the other element,
    // if any one constant's value of the other element differs from the constant's value in this
    // element
    for (Map.Entry<MemoryLocation, Value> otherEntry : other.constantsMap.entrySet()) {
      MemoryLocation key = otherEntry.getKey();
      Value otherValue = otherEntry.getValue();
      Value thisValue = constantsMap.get(key);

      if (!otherValue.equals(thisValue)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other == null) {
      return false;
    }

    if (!getClass().equals(other.getClass())) {
      return false;
    }

    ValueAnalysisState otherElement = (ValueAnalysisState) other;

    return otherElement.constantsMap.equals(constantsMap) && Objects.equals(memLocToType, otherElement.memLocToType);
  }

  @Override
  public int hashCode() {
    return constantsMap.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (Map.Entry<MemoryLocation, Value> entry : constantsMap.entrySet()) {
      MemoryLocation key = entry.getKey();
      sb.append(" <");
      sb.append(key.getAsSimpleString());
      sb.append(" = ");
      sb.append(entry.getValue());
      sb.append(">\n");
    }

    return sb.append("] size->  ").append(constantsMap.size()).toString();
  }

  /**
   * This method returns a more compact string representation of the state, compared to toString().
   *
   * @return a more compact string representation of the state
   */
  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("[");
    Joiner.on(", ").withKeyValueSeparator("=").appendTo(sb, constantsMap);
    sb.append("]");

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    pProperty = pProperty.trim();

    if (pProperty.startsWith("contains(")) {
      String varName = pProperty.substring("contains(".length(), pProperty.length() - 1);
      return this.constantsMap.containsKey(MemoryLocation.valueOf(varName));
    } else {
      String[] parts = pProperty.split("==");
      if (parts.length != 2) {
        Value value = this.constantsMap.get(MemoryLocation.valueOf(pProperty));
        if (value != null && value.isExplicitlyKnown()) {
          return value;
        } else {
          throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Could not find the variable \""
              + pProperty + "\"");
        }
      } else {
        return checkProperty(pProperty);
      }
    }
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    // e.g. "x==5" where x is a variable. Returns if 5 is the associated constant
    String[] parts = pProperty.split("==");

    if (parts.length != 2) {
      throw new InvalidQueryException("The Query \"" + pProperty
          + "\" is invalid. Could not split the property string correctly.");
    } else {
      // The following is a hack
      Value val = this.constantsMap.get(MemoryLocation.valueOf(parts[0]));
      if (val == null) {
        return false;
      }
      Long value = val.asLong(CNumericTypes.INT);

      if (value == null) {
        return false;
      } else {
        try {
          return value == Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
          // The command might contains something like "main::p==cmd" where the user wants to compare the variable p to the variable cmd (nearest in scope)
          // perhaps we should omit the "main::" and find the variable via static scoping ("main::p" is also not intuitive for a user)
          // TODO: implement Variable finding via static scoping
          throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Could not parse the long \""
              + parts[1] + "\"");
        }
      }
    }
  }

  private static boolean startsWithIgnoreCase(String s, String prefix) {
    s = s.substring(0, prefix.length());
    return s.equalsIgnoreCase(prefix);
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    Preconditions.checkNotNull(pModification);

    // either "deletevalues(methodname::varname)" or "setvalue(methodname::varname:=1929)"
    String[] statements = pModification.split(";");
    for (String statement : statements) {
      statement = statement.trim();
      if (startsWithIgnoreCase(statement, "deletevalues(")) {
        if (!statement.endsWith(")")) {
          throw new InvalidQueryException(statement + " should end with \")\"");
        }

        String varName = statement.substring("deletevalues(".length(), statement.length() - 1);

        if (contains(varName)) {
          forget(varName);
        } else {
          // varname was not present in one of the maps
          // i would like to log an error here, but no logger is available
        }

      } else if (startsWithIgnoreCase(statement, "setvalue(")) {
        if (!statement.endsWith(")")) {
          throw new InvalidQueryException(statement + " should end with \")\"");
        }

        String assignment = statement.substring("setvalue(".length(), statement.length() - 1);
        String[] assignmentParts = assignment.split(":=");

        if (assignmentParts.length != 2) {
          throw new InvalidQueryException("The Query \"" + pModification
              + "\" is invalid. Could not split the property string correctly.");
        } else {
          String varName = assignmentParts[0].trim();
          try {
            Value newValue = new NumericValue(Long.parseLong(assignmentParts[1].trim()));
            this.assignConstant(varName, newValue);
          } catch (NumberFormatException e) {
            throw new InvalidQueryException("The Query \"" + pModification
                + "\" is invalid. Could not parse the long \"" + assignmentParts[1].trim() + "\"");
          }
        }
      }
    }
  }

  @Override
  public String getCPAName() {
    return "ValueAnalysis";
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager) {
    BooleanFormulaManager bfmgr = manager.getBooleanFormulaManager();
    if (machineModel == null) {
      return bfmgr.makeTrue();
    }

    List<BooleanFormula> result = new ArrayList<>();
    BitvectorFormulaManagerView bitvectorFMGR = manager.getBitvectorFormulaManager();
    FloatingPointFormulaManagerView floatFMGR = manager.getFloatingPointFormulaManager();

    for (Map.Entry<MemoryLocation, Value> entry : constantsMap.entrySet()) {
      NumericValue num = entry.getValue().asNumericValue();

      if (num != null) {
        MemoryLocation memoryLocation = entry.getKey();
        Type type = getTypeForMemoryLocation(memoryLocation);
        if (!memoryLocation.isReference() && type instanceof CSimpleType) {
          CSimpleType simpleType = (CSimpleType) type;
          if (simpleType.getType().isIntegerType()) {
            int bitSize = machineModel.getSizeof(simpleType) * machineModel.getSizeofCharInBits();
            BitvectorFormula var =
                bitvectorFMGR.makeVariable(bitSize, entry.getKey().getAsSimpleString());

            Number value = num.getNumber();
            final BitvectorFormula val;
            if (value instanceof BigInteger) {
              val = bitvectorFMGR.makeBitvector(bitSize, (BigInteger) value);
            } else {
              val = bitvectorFMGR.makeBitvector(bitSize, num.longValue());
            }
            result.add(bitvectorFMGR.equal(var, val));
          } else if (simpleType.getType().isFloatingPointType()) {
            final FloatingPointType fpType;
            switch (simpleType.getType()) {
            case FLOAT:
              fpType = FormulaType.getSinglePrecisionFloatingPointType();
              break;
            case DOUBLE:
              fpType = FormulaType.getDoublePrecisionFloatingPointType();
              break;
            default:
              throw new AssertionError("Unsupported floating point type: " + simpleType);
            }
            FloatingPointFormula var = floatFMGR.makeVariable(entry.getKey().getAsSimpleString(), fpType);
            FloatingPointFormula val = floatFMGR.makeNumber(num.doubleValue(), fpType);
            result.add(floatFMGR.equalWithFPSemantics(var, val));
          } else {
            // ignore in formula-approximation
          }
        } else {
          // ignore in formula-approximation
        }
      } else {
        // ignore in formula-approximation
      }
    }

    return bfmgr.and(result);
  }

  /**
   * This method determines the set of variable names that are in the other state but not in this,
   * or that are in both, but differ in their value.
   *
   * @param other the other state for which to get the difference
   * @return the set of variable names that differ
   */
  public Set<MemoryLocation> getDifference(ValueAnalysisState other) {
    Set<MemoryLocation> difference = new HashSet<>();

    for (MemoryLocation variableName : other.constantsMap.keySet()) {
      if (!contains(variableName)) {
        difference.add(variableName);

      } else if (!getValueFor(variableName).equals(other.getValueFor(variableName))) {
        difference.add(variableName);
      }
    }

    return difference;
  }

  /**
   * This method adds the key-value-pairs of this state to the given value mapping and returns the new mapping.
   *
   * @param valueMapping the mapping from variable name to the set of values of this variable
   * @return the new mapping
   */
  public Multimap<String, Value> addToValueMapping(Multimap<String, Value> valueMapping) {
    for (Map.Entry<MemoryLocation, Value> entry : constantsMap.entrySet()) {
      valueMapping.put(entry.getKey().getAsSimpleString(), entry.getValue());
    }

    return valueMapping;
  }

  /**
   * This method returns the set of tracked variables by this state.
   *
   * @return the set of tracked variables by this state
   */
  public Set<String> getTrackedVariableNames() {
    Set<String> result = new HashSet<>();

    for (MemoryLocation loc : constantsMap.keySet()) {
      result.add(loc.getAsSimpleString());
    }

    // no copy necessary, fresh instance of set
    return Collections.unmodifiableSet(result);
  }

  /**
   * This method returns the set of tracked variables by this state.
   *
   * @return the set of tracked variables by this state
   */
  @Override
  public Set<MemoryLocation> getTrackedMemoryLocations() {
    // no copy necessary, set is immutable
    return constantsMap.keySet();
  }

  public Map<MemoryLocation, Value> getConstantsMapView() {
    return Collections.unmodifiableMap(constantsMap);
  }

  /**
   * This method acts as factory to create a value-analysis interpolant from this value-analysis state.
   *
   * @return the value-analysis interpolant reflecting the value assignment of this state
   */
  public ValueAnalysisInterpolant createInterpolant() {
    return new ValueAnalysisInterpolant(new HashMap<>(constantsMap), new HashMap<>(memLocToType));
  }

  public ValueAnalysisInformation getInformation() {
    return new ValueAnalysisInformation(constantsMap, memLocToType);
  }


  public Set<MemoryLocation> getMemoryLocationsOnStack(String pFunctionName) {
    Set<MemoryLocation> result = new HashSet<>();

    Set<MemoryLocation> memoryLocations = constantsMap.keySet();

    for (MemoryLocation memoryLocation : memoryLocations) {
      if (memoryLocation.isOnFunctionStack() && memoryLocation.getFunctionName().equals(pFunctionName)) {
        result.add(memoryLocation);
      }
    }

    // Doesn't need a copy, Memory Location is Immutable
    return Collections.unmodifiableSet(result);
  }

  public Set<MemoryLocation> getGlobalMemoryLocations() {
    Set<MemoryLocation> result = new HashSet<>();

    Set<MemoryLocation> memoryLocations = constantsMap.keySet();

    for (MemoryLocation memoryLocation : memoryLocations) {
      if (!memoryLocation.isOnFunctionStack()) {
        result.add(memoryLocation);
      }
    }

    // Doesn't need a copy, Memory Location is Immutable
    return Collections.unmodifiableSet(result);
  }

  public void forgetValuesWithIdentifier(String pIdentifier) {
    for (MemoryLocation memoryLocation : constantsMap.keySet()) {
      if (memoryLocation.getIdentifier().equals(pIdentifier)) {
        constantsMap = constantsMap.removeAndCopy(memoryLocation);
        memLocToType = memLocToType.removeAndCopy(memoryLocation);
      }
    }
  }

  /** If there was a recursive function, we have wrong values for scoped variables in the returnState.
   * This function rebuilds a new state with the correct values from the previous callState.
   * We delete the wrong values and insert new values, if necessary. */
  public ValueAnalysisState rebuildStateAfterFunctionCall(final ValueAnalysisState callState, final FunctionExitNode functionExit) {

    // we build a new state from:
    // - local variables from callState,
    // - global variables from THIS,
    // - the local return variable from THIS.
    // we copy callState and override all global values and the return variable.

    final ValueAnalysisState rebuildState = ValueAnalysisState.copyOf(callState);

    // first forget all global information
    for (final MemoryLocation trackedVar : callState.getTrackedMemoryLocations()) {
      if (!trackedVar.isOnFunctionStack()) { // global -> delete
        rebuildState.forget(trackedVar);
      }
    }

    // second: learn new information
    for (final MemoryLocation trackedVar : this.getTrackedMemoryLocations()) {

      if (!trackedVar.isOnFunctionStack()) { // global -> override deleted value
        rebuildState.assignConstant(trackedVar, this.getValueFor(trackedVar), this.getTypeForMemoryLocation(trackedVar));

      } else if (functionExit.getEntryNode().getReturnVariable().isPresent() &&
          functionExit.getEntryNode().getReturnVariable().get().getQualifiedName().equals(trackedVar.getAsSimpleString())) {
        /*assert (!rebuildState.contains(trackedVar)) :
                "calling function should not contain return-variable of called function: " + trackedVar;*/
        if (this.contains(trackedVar)) {
          rebuildState.assignConstant(trackedVar, this.getValueFor(trackedVar), this.getTypeForMemoryLocation(trackedVar));
        }
      }
    }

    return rebuildState;
  }

  private void readObject(ObjectInputStream in) throws IOException {
    try {
      in.defaultReadObject();
    } catch (ClassNotFoundException e) {
      throw new IOException("",e);
    }
    memLocToType = PathCopyingPersistentTreeMap.of();
  }

  @Override
  public Comparable<?> getPseudoPartitionKey() {
    return getSize();
  }

  @Override
  public Object getPseudoHashCode() {
    return this;
  }
}
