// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.PseudoPartitionable;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.EnumConstantValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;
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

public final class ValueAnalysisState
    implements AbstractQueryableState,
        FormulaReportingState,
        ExpressionTreeReportingState,
        ForgetfulState<ValueAnalysisInformation>,
        Serializable,
        Graphable,
        LatticeAbstractState<ValueAnalysisState>,
        PseudoPartitionable {

  @Serial private static final long serialVersionUID = -3152134511524554358L;

  private static final Set<MemoryLocation> blacklist = new HashSet<>();

  static void addToBlacklist(MemoryLocation var) {
    blacklist.add(checkNotNull(var));
  }

  /**
   * the map that keeps the name of variables and their constant values (concrete and symbolic ones)
   */
  private PersistentMap<MemoryLocation, ValueAndType> constantsMap;

  /**
   * hashCode needs to be updated with every change of {@link #constantsMap}.
   *
   * @see java.util.Map#hashCode()
   * @see java.util.Map.Entry#hashCode()
   */
  private int hashCode = 0;

  private final @Nullable MachineModel machineModel;

  public ValueAnalysisState(MachineModel pMachineModel) {
    this(checkNotNull(pMachineModel), PathCopyingPersistentTreeMap.of());
  }

  public ValueAnalysisState(
      Optional<MachineModel> pMachineModel,
      PersistentMap<MemoryLocation, ValueAndType> pConstantsMap) {
    this(pMachineModel.orElse(null), pConstantsMap);
  }

  private ValueAnalysisState(
      @Nullable MachineModel pMachineModel,
      PersistentMap<MemoryLocation, ValueAndType> pConstantsMap) {
    machineModel = pMachineModel;
    constantsMap = checkNotNull(pConstantsMap);
    hashCode = constantsMap.hashCode();
  }

  private ValueAnalysisState(ValueAnalysisState state) {
    machineModel = state.machineModel;
    constantsMap = checkNotNull(state.constantsMap);
    hashCode = state.hashCode;
    assert hashCode == constantsMap.hashCode();
  }

  public static ValueAnalysisState copyOf(ValueAnalysisState state) {
    return new ValueAnalysisState(state);
  }

  /**
   * This method assigns a value to the variable and puts it in the map.
   *
   * @param variableName name of the variable.
   * @param value value to be assigned.
   */
  void assignConstant(String variableName, Value value) {
    addToConstantsMap(MemoryLocation.parseExtendedQualifiedName(variableName), value, null);
  }

  private void addToConstantsMap(
      final MemoryLocation pMemLoc, final Value pValue, final @Nullable Type pType) {

    if (blacklist.contains(pMemLoc)
        || (pMemLoc.isReference() && blacklist.contains(pMemLoc.getReferenceStart()))) {
      return;
    }

    Value valueToAdd = pValue;

    if (valueToAdd instanceof SymbolicValue) {
      valueToAdd = ((SymbolicValue) valueToAdd).copyForLocation(pMemLoc);
    }

    ValueAndType valueAndType = new ValueAndType(checkNotNull(valueToAdd), pType);
    ValueAndType oldValueAndType = constantsMap.get(pMemLoc);
    if (oldValueAndType != null) {
      hashCode -= (pMemLoc.hashCode() ^ oldValueAndType.hashCode());
    }
    constantsMap = constantsMap.putAndCopy(pMemLoc, valueAndType);
    hashCode += (pMemLoc.hashCode() ^ valueAndType.hashCode());
  }

  /**
   * This method assigns a value to the variable and puts it in the map.
   *
   * @param pMemoryLocation the location in the memory.
   * @param value value to be assigned.
   * @param pType the type of <code>value</code>.
   */
  public void assignConstant(MemoryLocation pMemoryLocation, Value value, Type pType) {
    addToConstantsMap(pMemoryLocation, value, pType);
  }

  /**
   * This method assigns a concrete value to the given {@link SymbolicIdentifier}.
   *
   * @param pSymbolicIdentifier the <code>SymbolicIdentifier</code> to assign the concrete value to.
   * @param pValue value to be assigned.
   */
  public void assignConstant(
      SymbolicIdentifier pSymbolicIdentifier,
      Value pValue,
      AbstractExpressionValueVisitor pValueVisitor) {
    for (Entry<MemoryLocation, ValueAndType> entry : constantsMap.entrySet()) {
      CType memLocType = (CType) entry.getValue().getType();
      Value typedValue = pValue;
      if (pValue.isNumericValue()) {
        CIntegerLiteralExpression valueAsExpression =
            new CIntegerLiteralExpression(
                FileLocation.DUMMY, memLocType, pValue.asNumericValue().bigIntegerValue());
        try {
          typedValue = pValueVisitor.evaluate(valueAsExpression, memLocType);
        } catch (UnrecognizedCodeException e) {
          throw new AssertionError(e);
        }
      }
      MemoryLocation currMemloc = entry.getKey();
      Value currVal = entry.getValue().getValue();

      if (currVal instanceof ConstantSymbolicExpression) {
        currVal = ((ConstantSymbolicExpression) currVal).getValue();
      }

      if (currVal instanceof SymbolicIdentifier
          && ((SymbolicIdentifier) currVal).getId() == pSymbolicIdentifier.getId()) {

        assignConstant(currMemloc, typedValue, memLocType);
      }
    }
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

    ValueAndType value = constantsMap.get(pMemoryLocation);
    constantsMap = constantsMap.removeAndCopy(pMemoryLocation);
    hashCode -= (pMemoryLocation.hashCode() ^ value.hashCode());

    PersistentMap<MemoryLocation, ValueAndType> valueAssignment = PathCopyingPersistentTreeMap.of();
    valueAssignment = valueAssignment.putAndCopy(pMemoryLocation, value);

    return new ValueAnalysisInformation(valueAssignment);
  }

  @Override
  public void remember(
      final MemoryLocation pLocation, final ValueAnalysisInformation pValueAndType) {
    final ValueAndType value = pValueAndType.getAssignments().get(pLocation);
    assignConstant(pLocation, value.getValue(), value.getType());
  }

  /**
   * This method retains all variables and their respective values in the underlying map, while
   * removing all others.
   *
   * @param toRetain the names of the variables to retain
   */
  @Deprecated
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
   * This method drops all entries belonging to the stack frame of a function. This method should be
   * called right before leaving a function.
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
   * @param memLoc the name of the variable for which to get the value
   * @throws NullPointerException - if no value is present in this state for the given variable
   * @return the value associated with the given variable
   */
  public Value getValueFor(MemoryLocation memLoc) {
    return checkNotNull(getValueAndTypeFor(memLoc).getValue());
  }

  /**
   * This method returns the type for the given memory location.
   *
   * @param memLoc the memory location for which to get the type
   * @throws NullPointerException - if no type is present in this state for the given memory
   *     location
   * @return the type associated with the given memory location
   */
  public @Nullable Type getTypeForMemoryLocation(MemoryLocation memLoc) {
    return getValueAndTypeFor(memLoc).getType();
  }

  /**
   * This method returns the value and type for the given variable.
   *
   * @param memLoc the name of the variable for which to get the value
   * @throws NullPointerException - if no value is present in this state for the given variable
   * @return the value and type associated with the given variable
   */
  public ValueAndType getValueAndTypeFor(MemoryLocation memLoc) {
    return checkNotNull(constantsMap.get(memLoc));
  }

  /**
   * This method checks whether the given Memory Location is contained in this state.
   *
   * @param pMemoryLocation the location in the Memory to check for
   * @return whether the variable is contained
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
    PersistentMap<MemoryLocation, ValueAndType> newConstantsMap = PathCopyingPersistentTreeMap.of();

    for (Entry<MemoryLocation, ValueAndType> otherEntry : reachedState.constantsMap.entrySet()) {
      MemoryLocation key = otherEntry.getKey();
      ValueAndType value = otherEntry.getValue();

      if (Objects.equals(value, constantsMap.get(key))) {
        newConstantsMap = newConstantsMap.putAndCopy(key, value);
      }
    }

    // return the reached state if both maps are equal
    if (newConstantsMap.size() == reachedState.constantsMap.size()) {
      return reachedState;
    } else {
      return new ValueAnalysisState(machineModel, newConstantsMap);
    }
  }

  /**
   * This method decides if this element is less or equal than the other element, based on the order
   * imposed by the lattice.
   *
   * @param other the other element
   * @return true, if this element is less or equal than the other element, based on the order
   *     imposed by the lattice
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

    // the simple way
    // if (other.constantsMap.entrySet().containsAll(constantsMap.entrySet())) {
    //   return true;
    // }

    // the tolerant way: ignore all type information. TODO really correct?
    for (Entry<MemoryLocation, ValueAndType> otherEntry : other.constantsMap.entrySet()) {
      MemoryLocation key = otherEntry.getKey();
      Value otherValue = otherEntry.getValue().getValue();
      ValueAndType thisValueAndType = constantsMap.get(key);
      if (thisValueAndType == null || !otherValue.equals(thisValueAndType.getValue())) {
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
    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    ValueAnalysisState otherElement = (ValueAnalysisState) other;
    // hashCode is used as optimization: about 20% speedup when using many SingletonSets
    return otherElement.hashCode == hashCode && otherElement.constantsMap.equals(constantsMap);
  }

  @Override
  public int hashCode() {
    assert hashCode == constantsMap.hashCode();
    return hashCode;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (Entry<MemoryLocation, ValueAndType> entry : constantsMap.entrySet()) {
      MemoryLocation key = entry.getKey();
      sb.append(" <");
      sb.append(key.getExtendedQualifiedName());
      sb.append(" = ");
      sb.append(entry.getValue().getValue());
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
      return constantsMap.containsKey(MemoryLocation.parseExtendedQualifiedName(varName));
    } else {
      List<String> parts = Splitter.on("==").trimResults().splitToList(pProperty);
      if (parts.size() != 2) {
        ValueAndType value = constantsMap.get(MemoryLocation.parseExtendedQualifiedName(pProperty));
        if (value != null && value.getValue().isExplicitlyKnown()) {
          return value.getValue();
        } else {
          throw new InvalidQueryException(
              "The Query \""
                  + pProperty
                  + "\" is invalid. Could not find the variable \""
                  + pProperty
                  + "\"");
        }
      } else {
        return checkProperty(pProperty);
      }
    }
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    // e.g. "x==5" where x is a variable. Returns if 5 is the associated constant
    List<String> parts = Splitter.on("==").trimResults().splitToList(pProperty);

    if (parts.size() != 2) {
      throw new InvalidQueryException(
          "The Query \""
              + pProperty
              + "\" is invalid. Could not split the property string correctly.");
    } else {
      // The following is a hack
      ValueAndType val = constantsMap.get(MemoryLocation.parseExtendedQualifiedName(parts.get(0)));
      if (val == null) {
        return false;
      }
      Long value = val.getValue().asLong(CNumericTypes.INT);

      if (value == null) {
        return false;
      } else {
        try {
          return value == Long.parseLong(parts.get(1));
        } catch (NumberFormatException e) {
          // The command might contains something like "main::p==cmd" where the user wants to
          // compare the variable p to the variable cmd (nearest in scope)
          // perhaps we should omit the "main::" and find the variable via static scoping ("main::p"
          // is also not intuitive for a user)
          // TODO: implement Variable finding via static scoping
          throw new InvalidQueryException(
              "The Query \""
                  + pProperty
                  + "\" is invalid. Could not parse the long \""
                  + parts.get(1)
                  + "\"");
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
    for (String statement : Splitter.on(';').trimResults().split(pModification)) {
      if (startsWithIgnoreCase(statement, "deletevalues(")) {
        if (!statement.endsWith(")")) {
          throw new InvalidQueryException(statement + " should end with \")\"");
        }

        MemoryLocation varName =
            MemoryLocation.parseExtendedQualifiedName(
                statement.substring("deletevalues(".length(), statement.length() - 1));

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
        List<String> assignmentParts = Splitter.on(":=").trimResults().splitToList(assignment);

        if (assignmentParts.size() != 2) {
          throw new InvalidQueryException(
              "The Query \""
                  + pModification
                  + "\" is invalid. Could not split the property string correctly.");
        } else {
          String varName = assignmentParts.get(0);
          try {
            Value newValue = new NumericValue(Long.parseLong(assignmentParts.get(1)));
            this.assignConstant(varName, newValue);
          } catch (NumberFormatException e) {
            throw new InvalidQueryException(
                "The Query \""
                    + pModification
                    + "\" is invalid. Could not parse the long \""
                    + assignmentParts.get(1)
                    + "\"");
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
    return getFormulaApproximationWithSpecifiedVars(manager, Predicates.alwaysTrue(), true);
  }

  @Override
  public BooleanFormula getScopedFormulaApproximation(
      final FormulaManagerView pManager, final FunctionEntryNode pFunctionScope) {
    return getFormulaApproximationWithSpecifiedVars(
        pManager,
        memLoc ->
            !memLoc.getIdentifier().startsWith("__CPAchecker_TMP_")
                && (!memLoc.isOnFunctionStack()
                    || memLoc.isOnFunctionStack(pFunctionScope.getFunctionName())),
        false);
  }

  private BooleanFormula getFormulaApproximationWithSpecifiedVars(
      final FormulaManagerView manager,
      final Predicate<MemoryLocation> considerVar,
      final boolean useQualifiedNames) {
    BooleanFormulaManager bfmgr = manager.getBooleanFormulaManager();
    if (machineModel == null) {
      return bfmgr.makeTrue();
    }

    List<BooleanFormula> result = new ArrayList<>();
    BitvectorFormulaManagerView bitvectorFMGR = manager.getBitvectorFormulaManager();
    FloatingPointFormulaManagerView floatFMGR = manager.getFloatingPointFormulaManager();

    for (Entry<MemoryLocation, ValueAndType> entry : constantsMap.entrySet()) {
      MemoryLocation memoryLocation = entry.getKey();
      if (considerVar.test(memoryLocation)) {
        NumericValue num = entry.getValue().getValue().asNumericValue();

        if (num != null) {
          Type type = entry.getValue().getType();
          if (!memoryLocation.isReference() && type instanceof CSimpleType simpleType) {
            if (simpleType.getType().isIntegerType()) {
              int bitSize = machineModel.getSizeof(simpleType) * machineModel.getSizeofCharInBits();
              BitvectorFormula var =
                  bitvectorFMGR.makeVariable(
                      bitSize,
                      useQualifiedNames
                          ? entry.getKey().getExtendedQualifiedName()
                          : entry.getKey().getIdentifier());

              Number value = num.getNumber();
              final BitvectorFormula val;
              if (value instanceof BigInteger bigInteger) {
                val = bitvectorFMGR.makeBitvector(bitSize, bigInteger);
              } else {
                val = bitvectorFMGR.makeBitvector(bitSize, num.longValue());
              }
              result.add(bitvectorFMGR.equal(var, val));
            } else if (simpleType.getType().isFloatingPointType()) {
              final FloatingPointType fpType =
                  switch (simpleType.getType()) {
                    case FLOAT -> FormulaType.getSinglePrecisionFloatingPointType();
                    case DOUBLE -> FormulaType.getDoublePrecisionFloatingPointType();
                    default ->
                        throw new AssertionError("Unsupported floating point type: " + simpleType);
                  };
              FloatingPointFormula var =
                  floatFMGR.makeVariable(
                      useQualifiedNames
                          ? entry.getKey().getExtendedQualifiedName()
                          : entry.getKey().getIdentifier(),
                      fpType);
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
  @Deprecated
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

  @Override
  public Set<MemoryLocation> getTrackedMemoryLocations() {
    // no copy necessary, set is immutable
    return constantsMap.keySet();
  }

  public Set<Entry<MemoryLocation, ValueAndType>> getConstants() {
    return Collections.unmodifiableSet(constantsMap.entrySet());
  }

  /**
   * This method acts as factory to create a value-analysis interpolant from this value-analysis
   * state.
   *
   * @return the value-analysis interpolant reflecting the value assignment of this state
   */
  public ValueAnalysisInterpolant createInterpolant() {
    return new ValueAnalysisInterpolant(constantsMap);
  }

  public ValueAnalysisInformation getInformation() {
    return new ValueAnalysisInformation(constantsMap);
  }

  /**
   * If there was a recursive function, we have wrong values for scoped variables in the
   * returnState. This function rebuilds a new state with the correct values from the previous
   * callState. We delete the wrong values and insert new values, if necessary.
   */
  public ValueAnalysisState rebuildStateAfterFunctionCall(
      final ValueAnalysisState callState, final FunctionExitNode functionExit) {

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
    for (Entry<MemoryLocation, ValueAndType> e : getConstants()) {
      final MemoryLocation trackedVar = e.getKey();

      if (!trackedVar.isOnFunctionStack()) { // global -> override deleted value
        rebuildState.assignConstant(trackedVar, e.getValue().getValue(), e.getValue().getType());

      } else if (functionExit.getEntryNode().getReturnVariable().isPresent()
          && functionExit
              .getEntryNode()
              .getReturnVariable()
              .get()
              .getQualifiedName()
              .equals(trackedVar.getExtendedQualifiedName())) {
        /*assert (!rebuildState.contains(trackedVar)) :
        "calling function should not contain return-variable of called function: " + trackedVar;*/
        if (contains(trackedVar)) {
          rebuildState.assignConstant(trackedVar, e.getValue().getValue(), e.getValue().getType());
        }
      }
    }

    return rebuildState;
  }

  @Override
  public Comparable<?> getPseudoPartitionKey() {
    return getSize();
  }

  @Override
  public Object getPseudoHashCode() {
    return this;
  }

  private Optional<CExpression> buildConstraint(CExpression pVar, CType pCType, NumericValue pNum) {
    // TODO: Refactor the whole code to also handle JExpressions i.e. change CExpression to
    //  AExpression
    // TODO: Get real logger
    CBinaryExpressionBuilder builder =
        new CBinaryExpressionBuilder(machineModel, LogManager.createNullLogManager());
    FileLocation loc = pVar.getFileLocation();
    CExpression val = null;
    if (pCType instanceof CSimpleType simpleType) {
      if (simpleType.getType().isIntegerType()) {
        BigInteger value = getBigIntFromIntegerNumber(pNum.getNumber());
        val = new CIntegerLiteralExpression(loc, simpleType, value);
      } else if (simpleType.getType().isFloatingPointType()) {
        FloatValue.Format precision = FloatValue.Format.fromCType(machineModel, simpleType);
        val =
            new CFloatLiteralExpression(
                loc, machineModel, simpleType, pNum.floatingPointValue(precision));
      } else {
        throw new AssertionError("Unexpected type: " + simpleType);
      }
    } else if (pCType instanceof CEnumType enumType) {
      BigInteger value = getBigIntFromIntegerNumber(pNum.getNumber());
      for (CEnumerator enumerator : enumType.getEnumerators()) {
        if (value.equals(enumerator.getValue())) {
          val = new CIdExpression(loc, enumerator);
          break;
        }
      }
      if (val == null) {
        val = new CIntegerLiteralExpression(loc, enumType, value);
      }
    } else {
      // disabled since this blocks too many programs for which plenty other information
      // would be available, so just skip the current variable

      // throw new AssertionError("Unknown arithmetic type: " + cType);

      return Optional.empty();
    }
    return Optional.of(builder.buildBinaryExpressionUnchecked(pVar, val, BinaryOperator.EQUALS));
  }

  private BigInteger getBigIntFromIntegerNumber(Number pNum) {
    if (pNum instanceof BigInteger bigInteger) {
      return bigInteger;
    } else {
      return BigInteger.valueOf(pNum.longValue());
    }
  }

  private ExpressionTree<Object> getFormulaApproximation(
      FunctionEntryNode pFunctionScope,
      CFANode pLocation,
      Function<String, Boolean> variableNameIScope,
      Function<String, String> variableRenamingFunction)
      throws TranslationToExpressionTreeFailedException {
    if (machineModel == null) {
      throw new TranslationToExpressionTreeFailedException("MachineModel is not available.");
    }

    List<ExpressionTree<Object>> result = new ArrayList<>();

    for (Entry<MemoryLocation, ValueAndType> entry : constantsMap.entrySet()) {
      Value valueOfEntry = entry.getValue().getValue();
      if (valueOfEntry instanceof EnumConstantValue) {
        continue;
      }
      NumericValue num = valueOfEntry.asNumericValue();
      if (num != null) {
        MemoryLocation memoryLocation = entry.getKey();
        Type type = entry.getValue().getType();
        if (!memoryLocation.isReference()
            && memoryLocation.isOnFunctionStack(pFunctionScope.getFunctionName())
            && type instanceof CType cType
            && CTypes.isArithmeticType((CType) type)) {
          if (cType instanceof CBitFieldType) {
            cType = ((CBitFieldType) cType).getType();
          }
          if (cType instanceof CElaboratedType) {
            cType = ((CElaboratedType) cType).getRealType();
          }
          assert cType != null && CTypes.isArithmeticType(cType);
          String id = memoryLocation.getIdentifier();
          if (variableNameIScope.apply(id)) {
            FileLocation loc =
                pLocation.getNumEnteringEdges() > 0
                    ? pLocation.getEnteringEdge(0).getFileLocation()
                    : pFunctionScope.getFileLocation();
            String newVariableName = variableRenamingFunction.apply(id);
            CVariableDeclaration decl =
                new CVariableDeclaration(
                    loc,
                    false,
                    CStorageClass.AUTO,
                    cType,
                    newVariableName,
                    newVariableName,
                    memoryLocation.getExtendedQualifiedName(),
                    null);
            CExpression var = new CIdExpression(loc, decl);
            Optional<CExpression> constraint = buildConstraint(var, cType, num);
            if (constraint.isPresent()) {
              result.add(LeafExpression.of(constraint.orElseThrow()));
            }
          }
        }
      }
    }
    return And.of(result);
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximationAllVariablesInFunctionScope(
      FunctionEntryNode pFunctionScope, CFANode pLocation)
      throws TranslationToExpressionTreeFailedException {
    return getFormulaApproximation(
        pFunctionScope,
        pLocation,
        varName ->
            pFunctionScope.getReturnVariable().isEmpty()
                || !varName.equals(pFunctionScope.getReturnVariable().get().getName()),
        Function.identity());
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximationInputProgramInScopeVariables(
      FunctionEntryNode pFunctionScope,
      CFANode pLocation,
      AstCfaRelation pAstCfaRelation,
      boolean useOldKeywordForVariables)
      throws InterruptedException,
          ReportingMethodNotImplementedException,
          TranslationToExpressionTreeFailedException {

    return getFormulaApproximation(
        pFunctionScope,
        pLocation,
        varName ->
            (pFunctionScope.getReturnVariable().isEmpty()
                    || !varName.equals(pFunctionScope.getReturnVariable().get().getName()))
                && pAstCfaRelation
                    .getVariablesAndParametersInScope(pLocation)
                    .orElseThrow()
                    .anyMatch(v -> v.getName().equals(varName))
                && !varName.contains("__CPAchecker_"),
        varName -> useOldKeywordForVariables ? "\\old(" + varName + ")" : varName);
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximationFunctionReturnVariableOnly(
      FunctionEntryNode pFunctionScope, AIdExpression pFunctionReturnVariable)
      throws TranslationToExpressionTreeFailedException {
    if (machineModel == null) {
      throw new TranslationToExpressionTreeFailedException("MachineModel is not available.");
    }

    ExpressionTree<Object> result = ExpressionTrees.getTrue();

    for (Entry<MemoryLocation, ValueAndType> entry : constantsMap.entrySet()) {
      Value valueOfEntry = entry.getValue().getValue();
      if (valueOfEntry instanceof EnumConstantValue) {
        continue;
      }
      NumericValue num = valueOfEntry.asNumericValue();
      if (num != null) {
        MemoryLocation memoryLocation = entry.getKey();
        Type type = entry.getValue().getType();
        if (!memoryLocation.isReference()
            && memoryLocation.isOnFunctionStack(pFunctionScope.getFunctionName())
            && type instanceof CType cType
            && CTypes.isArithmeticType((CType) type)) {
          if (cType instanceof CBitFieldType) {
            cType = ((CBitFieldType) cType).getType();
          }
          if (cType instanceof CElaboratedType) {
            cType = ((CElaboratedType) cType).getRealType();
          }
          assert cType != null && CTypes.isArithmeticType(cType);
          String id = memoryLocation.getIdentifier();
          if (pFunctionScope.getReturnVariable().isPresent()
              && id.equals(pFunctionScope.getReturnVariable().orElseThrow().getName())
              && pFunctionReturnVariable instanceof CIdExpression var) {
            Optional<CExpression> constraint = buildConstraint(var, cType, num);
            if (constraint.isPresent()) {
              result = LeafExpression.of(constraint.orElseThrow());
              break;
            }
          }
        }
      }
    }

    return result;
  }

  public static class ValueAndType implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final Value value;
    private final Type type;

    public ValueAndType(Value pValue, Type pType) {
      value = checkNotNull(pValue);
      type = pType;
    }

    public Value getValue() {
      return value;
    }

    public Type getType() {
      return type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      return o instanceof ValueAndType other
          && Objects.equals(value, other.value)
          && Objects.equals(type, other.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value, type);
    }

    @Override
    public String toString() {
      return String.format("%s (%s)", value, type);
    }
  }
}
