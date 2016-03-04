/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.PropertyBudgeting;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA.ControlAutomatonOptions;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

@Options(prefix="cpa.automaton.prec")
public class ControlAutomatonPrecisionAdjustment implements PrecisionAdjustment {

  private final AutomatonState bottomState;
  private final AutomatonState inactiveState;

  /**
   *  We use {@link Supplier} because it should be possible
   *  to have different strategies for one instance of the CPA;
   *  this is used, for example, in context of verifying several properties.
   */
  private final Supplier<PropertyBudgeting> budgeting;

  @Option(secure=true, name="limit.violations",
      description="Handle at most k (> 0) violation of one property.")
  private int violationsLimit = 1;

  enum TargetStateVisitBehaviour {
    SIGNAL, // Signal the target state (default)
    BOTTOM, // Change to the automata state BOTTOM (when splitting states on a violation)
    INACTIVE, // Change to the automata state INACTIVE (when NOT splitting states on a violation)
  }
  @Option(secure=true, description="Behaviour on a property that has already been fully handled.")
  private TargetStateVisitBehaviour onHandledTarget = TargetStateVisitBehaviour.SIGNAL;

  @Option(secure=true, description="Behaviour on a property for which the resources were exhausted.")
  private Action onExhaustedBudget = Action.CONTINUE;

  public ControlAutomatonPrecisionAdjustment(
      Configuration pConfig,
      ControlAutomatonOptions pOptions,
      Supplier<PropertyBudgeting> pBudgeting,
      AutomatonState pBottomState,
      AutomatonState pInactiveState)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    if (pOptions.splitOnTargetStatesToInactive
        && (onHandledTarget != TargetStateVisitBehaviour.SIGNAL
            && onHandledTarget != TargetStateVisitBehaviour.BOTTOM)) {
      throw new InvalidConfigurationException("Splitting to INACTIVE requires an adjustment of handled target states to either SIGNAL or BOTTOM!");
    }

    this.bottomState = pBottomState;
    this.inactiveState = pInactiveState;
    this.budgeting = pBudgeting;
  }

  private void signalDisablingProperties(Set<? extends Property> pProperty) {
    ARGCPA argCpa = CPAs.retrieveCPA(GlobalInfo.getInstance().getCPA().get(), ARGCPA.class);
    for (Property p: pProperty) {
      argCpa.getCexSummary().signalPropertyDisabled(p);
    }
  }

  private int feasibleViolationsOf(Set<? extends Property> pProperties) {

    ARGCPA argCpa = CPAs.retrieveCPA(GlobalInfo.getInstance().getCPA().get(), ARGCPA.class);
    Multiset<Property> reachedViolations = argCpa.getCexSummary().getFeasiblePropertyViolations();

    int result = Integer.MAX_VALUE;
    for (Property p: pProperties) {
      result = Math.min(result, reachedViolations.count(p));
    }

    return result;
  }

  private boolean isPropertyBudgetExhausted(Property pProperty) {
    int timesFeasible = feasibleViolationsOf(ImmutableSet.of(pProperty));

    return (violationsLimit > 0
                   && timesFeasible >= violationsLimit) // the new state is the ith+1
        || budgeting.get().isTargetBudgedExhausted(pProperty);
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
    throws CPAException, InterruptedException {

    // Casts to the AutomataCPA-specific types
    final AutomatonPrecision pi = (AutomatonPrecision) pPrecision;
    final AutomatonInternalState internalState = ((AutomatonState) pState).getInternalState();
    final Automaton automaton = ((AutomatonState) pState).getOwningAutomaton();
    final AutomatonState state = (AutomatonState) pState;

    final AutomatonState stateOnHandledTarget;
    switch (onHandledTarget) {
      case BOTTOM: stateOnHandledTarget = bottomState; break;
      case INACTIVE: stateOnHandledTarget = inactiveState; break;
      default: stateOnHandledTarget = state;
    }

    Set<SafetyProperty> exhaustedProperties = Sets.newHashSet();

    ImmutableSet<? extends SafetyProperty> encoded = automaton.getEncodedProperties();
    ImmutableSet<SafetyProperty> disabled = pi.getBlacklist();

    Set<? extends SafetyProperty> activeProperties = Sets.difference(encoded, disabled);

    if (activeProperties.isEmpty()) {
      return Optional.of(PrecisionAdjustmentResult.create(
          inactiveState,
          pi, Action.CONTINUE));
    }

    for (SafetyProperty p: activeProperties) {
      if (budgeting.get().isTransitionBudgedExhausted(p)) {
        exhaustedProperties.add(p);
      }
    }

    // Specific handling of potential target states!!!
    if (state.isTarget()) {

      // A property might have already been disabled!
      //    Handling of blacklisted (disabled) states:
      if (pi.getBlacklist().containsAll(state.getViolatedProperties())) {
        return Optional.of(PrecisionAdjustmentResult.create(
            stateOnHandledTarget,
            pi, Action.CONTINUE));
      }

      // Handle a target state
      //    We might disable certain target states
      //      (they should not be considered as target states)
      if (onHandledTarget != TargetStateVisitBehaviour.SIGNAL) {
        Set<SafetyProperty> violated = AbstractStates.extractViolatedProperties(state, SafetyProperty.class);

        for (SafetyProperty p: violated) {
          if (isPropertyBudgetExhausted(p)) {
            exhaustedProperties.add(p);
          }
        }
      }
    }

    if (exhaustedProperties.size() > 0) {
      final AutomatonPrecision piPrime = pi.cloneAndAddBlacklisted(exhaustedProperties);
      signalDisablingProperties(exhaustedProperties);

      return Optional.of(PrecisionAdjustmentResult.create(
          state.isTarget() ? stateOnHandledTarget : inactiveState,
          piPrime, onExhaustedBudget));
    }

    // Handle the BREAK state
    if (internalState.getName().equals(AutomatonInternalState.BREAK.getName())) {
      return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, Action.BREAK));
    }

    // No precision adjustment
    return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, Action.CONTINUE));
  }

}
