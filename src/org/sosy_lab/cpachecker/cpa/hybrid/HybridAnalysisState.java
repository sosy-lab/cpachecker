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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.hybrid.util.CollectionUtils;
import org.sosy_lab.cpachecker.cpa.hybrid.util.ExpressionUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class HybridAnalysisState
    implements LatticeAbstractState<HybridAnalysisState>, AbstractStateWithAssumptions, Graphable {

  private ImmutableSet<CBinaryExpression> assumptions;

  // variable cache
  private ImmutableSet<CExpression> trackedVariables;

  // the declarations are later used to generate values for variables that are tracked by the
  // value analysis, but with unknown value
  private ImmutableMap<String, CSimpleDeclaration> declarations;

  public HybridAnalysisState() {
    this(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
  }

  public HybridAnalysisState(Set<CExpression> pAssumptions) {
    this(
        Sets.newHashSet(CollectionUtils.ofType(pAssumptions, CBinaryExpression.class)),
        Collections.emptySet(),
        Collections.emptySet());
  }

  protected HybridAnalysisState(
      Set<CBinaryExpression> pAssumptions,
      Set<CExpression> pVariables,
      Set<CSimpleDeclaration> pDeclarations) {
    this.assumptions = ImmutableSet.copyOf(pAssumptions);

    this.trackedVariables = ImmutableSet.copyOf(pVariables);

    this.declarations = ImmutableMap.copyOf(pDeclarations
        .stream()
        .collect(Collectors.toMap(CSimpleDeclaration::getQualifiedName, Function.identity())));
  }

  protected HybridAnalysisState(
      Set<CBinaryExpression> pAssumptions,
      Set<CExpression> pVariables,
      Map<String, CSimpleDeclaration> pDeclarations) {

    this.assumptions = ImmutableSet.copyOf(pAssumptions);
    this.trackedVariables = ImmutableSet.copyOf(pVariables);
    this.declarations = ImmutableMap.copyOf(pDeclarations);

  }

  // creates an exact copy of the given state
  public static HybridAnalysisState copyOf(HybridAnalysisState state) {
    return new HybridAnalysisState(
        state.assumptions,
        state.trackedVariables,
        state.declarations);
  }

  public static HybridAnalysisState copyWithNewAssumptions(HybridAnalysisState pState, CBinaryExpression... pExpressions) {
    Set<CBinaryExpression> currentAssumptions = Sets.newHashSet(pState.assumptions);
    currentAssumptions.addAll(Arrays.asList(pExpressions));
    return new HybridAnalysisState(
        currentAssumptions,
        pState.trackedVariables,
        pState.declarations);
  }

  public static HybridAnalysisState removeOnAssignment(HybridAnalysisState pState, CLeftHandSide pCLeftHandSide) {

    Set<CBinaryExpression> newAssumptions = pState.getExplicitAssumptions();
    Set<CExpression> variables = pState.getVariables();
    Map<String, CSimpleDeclaration> declarationMap = pState.getDeclarations();

    Set<CBinaryExpression> removableAssumptions = Sets.newHashSet();

    for(CBinaryExpression binaryExpression : newAssumptions) {
      if(ExpressionUtils.haveTheSameVariable(
          binaryExpression.getOperand1(),
          pCLeftHandSide)) {
        removableAssumptions.add(binaryExpression);
      }
    }

    newAssumptions.removeAll(removableAssumptions);

    return new HybridAnalysisState(
        newAssumptions,
        variables,
        declarationMap);
  }

  public HybridAnalysisState mergeWithArtificialAssignments(Collection<CBinaryExpression> pArtificialAssumptions) {

    Set<CBinaryExpression> mergedAssumptions = Sets.newHashSet(pArtificialAssumptions);
    Set<CBinaryExpression> seenAssumptions = Sets.newHashSet();
    for(CBinaryExpression currentAssumption : assumptions) {
      if(CollectionUtils.
          appliesToAtLeastOne(
              pArtificialAssumptions,
              artAsssump -> ExpressionUtils.haveTheSameVariable(artAsssump, currentAssumption))) {
        seenAssumptions.add(currentAssumption);
      }
    }

    // filter out the seen assumptions
    Set<CBinaryExpression> filteredAssumptions = assumptions
        .stream()
        .filter(assumption -> !seenAssumptions.contains(assumption))
        .collect(Collectors.toSet());

    mergedAssumptions.addAll(filteredAssumptions);

    Set<CExpression> variables = mergedAssumptions
        .stream()
        .map(assumption -> assumption.getOperand1())
        .collect(Collectors.toSet());

    return new HybridAnalysisState(
        mergedAssumptions,
        variables,
        this.declarations);
  }

  /**
   * The join operator is implemented as an intersection of both states' tracked assumptions
   * Idea for another implementation:
   *   foreach assumption:
   *      1) if variables are different, take the assumption
   *      2) if variables are the same, take the assumption of the other state (reached state)
   *
   *  The declarations get merged
   */
  @Override
  public HybridAnalysisState join(HybridAnalysisState pOther)
      throws CPAException, InterruptedException {

    Set<CBinaryExpression> combinedAssumptions = Sets.newHashSet();
    Set<CExpression> variableIdentifiers = Sets.newHashSet();

    for(CBinaryExpression otherAssumption : pOther.assumptions)
    {
        if(assumptions.contains(otherAssumption)) {
            combinedAssumptions.add(otherAssumption);
            CExpression variableIdentifier = otherAssumption.getOperand1();
            variableIdentifiers.add(variableIdentifier);
        }
    }

    Map<String, CSimpleDeclaration> mergedDeclarations = Maps.newHashMap(this.declarations);

    // simply add new declarations
    pOther.declarations.forEach((key, value) -> mergedDeclarations.putIfAbsent(key, value));

    return new HybridAnalysisState(
        combinedAssumptions,
        variableIdentifiers,
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

    List<CExpression> otherAssumptions = pOther.getAssumptions();

    // if this state contains less elements (assumptions), it cannot be less or equal than the other state, by definition
    if(otherAssumptions.size() > assumptions.size()) {
        return false;
    }

    // check if all assumptions of the other state are contained is this state's assumptions
    return CollectionUtils.appliesToAll(otherAssumptions, a -> assumptions.contains(a));
  }

  @Override
  public List<CExpression> getAssumptions() {
    return ImmutableList.copyOf(assumptions);
  }

  /**
   * Creates a mutable copy of the assumptions held by this state
   * @return the assumptions with explicit expression type (CBinaryExpression)
   */
  protected Set<CBinaryExpression> getExplicitAssumptions() {
    return Sets.newHashSet(assumptions);
  }

  /**
   * Creates a mutable copy of the declarations
   * @return A map containing the declarations
   */
  protected Map<String, CSimpleDeclaration> getDeclarations() {
    return Maps.newHashMap(declarations);
  }

  protected Set<CExpression> getVariables() {
    return Sets.newHashSet(trackedVariables);
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
    return this.assumptions.equals(other.assumptions);
  }

  @Override
  public int hashCode() {
      return assumptions.hashCode();
  }

  @Override
  public String toDOTLabel() {
    StringBuilder builder = new StringBuilder();
    assumptions.forEach(assumption -> builder.append(assumption).append(System.lineSeparator()));
    return builder.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
      return false;
  }

  /**
   *
   * @param pCIdExpression The repsective variable expression
   * @return Whether the state tracks an assumption for this variable
   */
  public boolean tracksVariable(CExpression pCIdExpression) {

    if(trackedVariables.contains(pCIdExpression)) {
      return true;
    }

    boolean match = false;

    for(CBinaryExpression binaryExpression : assumptions) {
        match |= ExpressionUtils.haveTheSameVariable(pCIdExpression, binaryExpression);
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



}