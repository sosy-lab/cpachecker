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
package org.sosy_lab.cpachecker.cpa.hybrid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.hybrid.util.CollectionUtils;
import org.sosy_lab.cpachecker.cpa.hybrid.util.ExpressionUtils;
import org.sosy_lab.cpachecker.cpa.hybrid.value.HybridValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class HybridAnalysisState
    implements LatticeAbstractState<HybridAnalysisState>, AbstractStateWithAssumptions, Graphable {

  // map of variable expressions with their respective assumption
  private ImmutableMap<CIdExpression, HybridValue> variableMap;

  // the declarations are later used to generate values for variables that are tracked by the
  // value analysis, but with unknown value
  private ImmutableMap<String, CSimpleDeclaration> declarations;

  public HybridAnalysisState() {
    this(Collections.emptySet(), Collections.emptySet());
  }

  public HybridAnalysisState(Set<CExpression> pAssumptions) {
    this(CollectionUtils.ofType(pAssumptions, CBinaryExpression.class)
          .stream()
          .map(expression -> HybridValue.createHybridValueForAssumption(expression))
          .collect(Collectors.toSet()),
        Collections.emptySet());
  }

  protected HybridAnalysisState(
      Set<HybridValue> pAssumptions,
      Set<CSimpleDeclaration> pDeclarations) {

    this.variableMap = ImmutableMap.copyOf(pAssumptions
        .stream()
        .collect(Collectors.toMap(HybridValue::trackedVariable, Function.identity())));

    this.declarations = ImmutableMap.copyOf(pDeclarations
        .stream()
        .collect(Collectors.toMap(CSimpleDeclaration::getQualifiedName, Function.identity())));
  }

  protected HybridAnalysisState(
      Map<CIdExpression, HybridValue> pVariableMap,
      Map<String, CSimpleDeclaration> pDeclarations) {

    this.variableMap = ImmutableMap.copyOf(pVariableMap);
    this.declarations = ImmutableMap.copyOf(pDeclarations);

  }

  // creates an exact copy of the given state
  public static HybridAnalysisState copyOf(HybridAnalysisState state) {
    return new HybridAnalysisState(
        state.variableMap,
        state.declarations);
  }

  public static HybridAnalysisState copyWithNewAssumptions(HybridAnalysisState pState, HybridValue... pNewAssumptions) {

    Collection<HybridValue> hybridValues = Arrays.asList(pNewAssumptions);

    Map<CIdExpression, HybridValue> newAssumptions = Maps.newHashMap(pState.variableMap);
    newAssumptions.putAll(hybridValues
      .stream()
      .collect(Collectors.toMap(HybridValue::trackedVariable, Function.identity())));

    Map<String, CSimpleDeclaration> newDeclarations = Maps.newHashMap(pState.declarations);

    // old values for variable Expression or overwritten
    newDeclarations.putAll(hybridValues
      .stream()
      .map(value -> ExpressionUtils.extractDeclaration(value))
      .collect(Collectors.toMap(CSimpleDeclaration::getQualifiedName, Function.identity())));

    return new HybridAnalysisState(
        newAssumptions,
        pState.declarations);
  }

  public static HybridAnalysisState removeOnAssignment(HybridAnalysisState pState, CLeftHandSide pCLeftHandSide) {

    Set<CExpression> removableAssumptions = Sets.newHashSet();

    for(CExpression expression : pState.variableMap.keySet()) {
      if(ExpressionUtils.haveTheSameVariable(
          expression,
          pCLeftHandSide)) {
        removableAssumptions.add(expression);
      }
    }

    Map<CIdExpression, HybridValue> newVariableMap = Maps.newHashMap(pState.variableMap);
    removableAssumptions.forEach(assumption -> newVariableMap.remove(assumption));
    Map<String, CSimpleDeclaration> declarationMap = pState.getDeclarations();

    return new HybridAnalysisState(
        newVariableMap,
        declarationMap);
  }

  public HybridAnalysisState mergeWithArtificialAssignments(Collection<CBinaryExpression> pArtificialAssumptions) {

    Set<CExpression> seenAssumptions = Sets.newHashSet();
    for(CExpression idExpression : variableMap.keySet()) {
      if(CollectionUtils.
          appliesToAtLeastOne(
              pArtificialAssumptions,
              artAsssump -> ExpressionUtils.haveTheSameVariable(artAsssump, idExpression))) {
        seenAssumptions.add(idExpression);
      }
    }

    Map<CIdExpression, HybridValue> mergedAssumptions = Maps.newHashMap(variableMap);
    seenAssumptions.forEach(assumption -> mergedAssumptions.remove(assumption));

    // build new hybrid value assumptions
    for(CBinaryExpression artificialAssumption : pArtificialAssumptions) {
      @Nullable
      HybridValue newValue = HybridValue.createHybridValueForAssumption(artificialAssumption);
      if(newValue != null) {
        // the assumption is artificially generated by the solver
        newValue.solverGenerated();
        mergedAssumptions.put(newValue.trackedVariable(), newValue);
      }
    }

    return new HybridAnalysisState(
        mergedAssumptions,
        this.declarations);
  }

  /**
   * The join operator is implemented as an intersection of both states' tracked assumptions
   *
   * The declarations are merged
   */
  @Override
  public HybridAnalysisState join(HybridAnalysisState pOther)
      throws CPAException, InterruptedException {

    Map<CIdExpression, HybridValue> combinedAssumptions = Maps.newHashMap();

    for(Entry<CIdExpression, HybridValue> otherEntry : pOther.variableMap.entrySet())
    {
      CIdExpression idExpression = otherEntry.getKey();
      HybridValue value = otherEntry.getValue();

      if(Objects.equals(value, variableMap.get(idExpression))) {
          combinedAssumptions.put(idExpression, value);
      }
    }

    Map<String, CSimpleDeclaration> mergedDeclarations = Maps.newHashMap(this.declarations);

    // simply add new declarations
    pOther.declarations.forEach((key, value) -> mergedDeclarations.putIfAbsent(key, value));

    return new HybridAnalysisState(
        combinedAssumptions,
        mergedDeclarations);
  }

  /**
   * This state is less or equal than the other state if all assumptions of the other state are contained within this state
   * In detail:
   *  1) every variable that is tracked by the other state, is also tracked by this state
   *  2) the assigned value for every variable is the same
   */
  @Override
  public boolean isLessOrEqual(HybridAnalysisState pOther)
      throws CPAException, InterruptedException {

    Collection<HybridValue> otherAssumptions = pOther.variableMap.values();
    Collection<HybridValue> assumptions = this.variableMap.values();

    // if this state contains less elements (assumptions), it cannot be less or equal than the other state, by definition
    if(assumptions.size() < otherAssumptions.size()) {
        return false;
    }

    // check if all assumptions of the other state are contained is this state's assumptions
    return CollectionUtils.appliesToAll(otherAssumptions, a -> assumptions.contains(a));
  }

  @Override
  public List<CExpression> getAssumptions() { 
    return ImmutableList.copyOf(extractAssumptions());
  }

  /**
   * Creates a mutable copy of the assumptions held by this state
   * @return the assumptions with explicit expression type (CBinaryExpression)
   */
  protected Set<HybridValue> getExplicitAssumptions() {
    return Sets.newHashSet(variableMap.values());
  }

  /**
   * Creates a mutable copy of the declarations
   * @return A map containing the declarations
   */
  protected Map<String, CSimpleDeclaration> getDeclarations() {
    return Maps.newHashMap(declarations);
  }

  /**
   * Creates a mutable copy of the tracked variable expressions
   * @return A set containing the variables
   */
  protected Set<CIdExpression> getVariables() {
    return Sets.newHashSet(variableMap.keySet());
  }

  /**
   * Tries to retrieve an assumption for the given variable expression
   * @param pVariableExpression The respective expression representing the variable
   * @return The HybridValue instance held by this state, if it tracks an assumption for the variable
   */
  @Nullable
  protected HybridValue getAssumptionForVariableExpression(CExpression pVariableExpression) {
    return variableMap.get(pVariableExpression);
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null || !(obj instanceof HybridAnalysisState)) {
        return false;
    }

    if(obj == this) {
        return true;
    }

    HybridAnalysisState other = (HybridAnalysisState) obj;
    return this.variableMap.equals(other.variableMap)
        && this.declarations.equals(other.declarations);
  }

  @Override
  public int hashCode() {
      return variableMap.hashCode();
  }

  @Override
  public String toDOTLabel() {
    StringBuilder builder = new StringBuilder();
    variableMap.values()
      .forEach(assumption -> builder.append(assumption).append(System.lineSeparator()));
    return builder.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
      return false;
  }

  /**
   *
   * @param pCIdExpression The respective variable expression
   * @return Whether the state tracks an assumption for this variable
   */
  public boolean tracksVariable(CExpression pCIdExpression) {

    if(variableMap.containsKey(pCIdExpression)) {
      return true;
    }

    boolean match = false;

    for(HybridValue hybridValue : variableMap.values()) {
        match |= hybridValue.tracksVariable(pCIdExpression);
    }

    return match;
  }

  /**
   * Tries to retrieve a declaration for the variable name
   * @param pVariableName The respective variable name
   * @return An Optional containing the declaration, if one is present for the given name
   */
  public Optional<CSimpleDeclaration> getDeclarationForName(final String pVariableName) {
    if(declarations.containsKey(pVariableName)) {
      return Optional.of(declarations.get(pVariableName));
    }

    return Optional.empty();
  }

  // simple method to retrieve the assumption from the tracked hybrid values
  private Set<CBinaryExpression> extractAssumptions() {
    return variableMap.values()
      .stream()
      .map(hybridValue -> hybridValue.getAssumption())
      .collect(Collectors.toSet());
  }

}