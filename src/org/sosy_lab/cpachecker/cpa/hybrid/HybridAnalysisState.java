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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.hybrid.util.CollectionUtils;
import org.sosy_lab.cpachecker.cpa.hybrid.util.ExpressionUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class HybridAnalysisState implements LatticeAbstractState<HybridAnalysisState>,
    AbstractStateWithAssumptions, Graphable {

  private ImmutableSet<CBinaryExpression> assumptions;

  // variable cache
  private ImmutableSet<CExpression> trackedVariables;

  public HybridAnalysisState() {
      this(Collections.emptySet());
  }

  public HybridAnalysisState(Collection<CExpression> pAssumptions) {
    this.assumptions = ImmutableSet.copyOf(
      CollectionUtils.ofType(pAssumptions, CBinaryExpression.class));

    trackedVariables = ImmutableSet.copyOf(
      this.assumptions
        .stream()
        .map(expression -> expression.getOperand1())
        .collect(Collectors.toSet()));
  }

  protected HybridAnalysisState(Set<CBinaryExpression> pAssumptions, Set<CExpression> pVariables) {
    this.assumptions = ImmutableSet.copyOf(pAssumptions);

    this.trackedVariables = ImmutableSet.copyOf(pVariables);
  }

  // creates an exact copy of the given state in terms of assumptions
  public static HybridAnalysisState copyOf(HybridAnalysisState state)
  {
    return new HybridAnalysisState(state.getAssumptions());
  }

  public static HybridAnalysisState copyWithNewAssumptions(HybridAnalysisState pState, CExpression... pExpressions) {
    Set<CExpression> currentAssumptions = Sets.newHashSet(pState.getAssumptions());
    currentAssumptions.addAll(Arrays.asList(pExpressions));
    return new HybridAnalysisState(currentAssumptions);
  }

  public HybridAnalysisState mergeWithArtificialAssignments(Collection<CBinaryExpression> pArtificialAssumptions) {

    Set<CBinaryExpression> mergedAssumptions = Sets.newHashSet(pArtificialAssumptions);
    Set<CBinaryExpression> seenAssumptions = Sets.newHashSet();
    for(CBinaryExpression currentAssumption : assumptions) {
      if(CollectionUtils.appliesToAtLeastOne(pArtificialAssumptions, artAsssump -> haveTheSameVariable(artAsssump, currentAssumption))) {
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

    return new HybridAnalysisState(mergedAssumptions, variables);
  }

  /**
   * The join operator is implemented as an intersection of both states' tracked assumptions
   * Idea for another implementation:
   *   foreach assumption:
   *      1) if variables are different, take the assumption
   *      2) if variables are the same, take the assumption of the other state (reached state)
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

    return new HybridAnalysisState(combinedAssumptions, variableIdentifiers);
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
   * Creates a copy of the assumptions held by this state
   * @return the assumptions with explicit expression type (CBinaryExpression)
   */
  public ImmutableSet<CBinaryExpression> getExplicitAssumptions() {
    return ImmutableSet.copyOf(assumptions);
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

    @Nullable final String identifier = ExpressionUtils.extractVariableIdentifier(pCIdExpression);

    for(CBinaryExpression )
  }

  private boolean haveTheSameVariable(CExpression first, CExpression second) {

    CExpression secondLeftHandSide = second.getOperand1();

    @Nullable String nameFirst = ExpressionUtils.extractVariableIdentifier(firstLeftHandSide);
    @Nullable String nameSecond = ExpressionUtils.extractVariableIdentifier(secondLeftHandSide);

    if(first instanceof CBinaryExpression) {
      CExpression firstLeftHandSide = (())
      nameFirst
    }

    return Objects.equals(nameFirst, nameSecond);
  }

}