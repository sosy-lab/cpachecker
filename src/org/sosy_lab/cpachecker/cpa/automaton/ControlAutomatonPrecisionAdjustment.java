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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.algorithm.mpa.MultiPropertyAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpa.PropertyStats;
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
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime.NoTimeMeasurement;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

@Options(prefix="cpa.automaton.prec")
public class ControlAutomatonPrecisionAdjustment implements PrecisionAdjustment {

  private final LogManager logger;
  private final AutomatonState bottomState;
  private final AutomatonState inactiveState;

  @Option(secure=true, name="limit.violations",
      description="Handle at most k (> 0) violation of one property.")
  private int violationsLimit = 1;

  @Option(secure=true, name="limit.avgRefineTime",
      description="Disable a property after the avg. time for refinements was exhausted.")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
    defaultUserUnit=TimeUnit.MILLISECONDS, min=-1)
  private TimeSpan avgRefineTimeLimit = TimeSpan.ofNanos(-1);

  @Option(secure=true, name="limit.totalRefineTime",
      description="Disable a property after a specific time (total) for refinements was exhausted.")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
    defaultUserUnit=TimeUnit.MILLISECONDS, min=-1)
  private TimeSpan totalRefineTimeLimit = TimeSpan.ofNanos(-1);

  enum TargetStateVisitBehaviour {
    SIGNAL, // Signal the target state (default)
    BOTTOM, // Change to the automata state BOTTOM (when splitting states on a violation)
    INACTIVE, // Change to the automata state INACTIVE (when NOT splitting states on a violation)
  }
  @Option(secure=true, description="Behaviour on a property that has already been fully handled.")
  private TargetStateVisitBehaviour onHandledTarget = TargetStateVisitBehaviour.SIGNAL;

  public static int hackyLimitFactor = 1;

  public ControlAutomatonPrecisionAdjustment(
      LogManager pLogger,
      Configuration pConfig,
      ControlAutomatonOptions pOptions,
      AutomatonState pBottomState,
      AutomatonState pInactiveState)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    if (pOptions.splitOnTargetStatesToInactive
        && (onHandledTarget != TargetStateVisitBehaviour.SIGNAL
            && onHandledTarget != TargetStateVisitBehaviour.BOTTOM)) {
      throw new InvalidConfigurationException("Splitting to INACTIVE requires an adjustment of handled target states to either SIGNAL or BOTTOM!");
    }

    this.logger = pLogger;
    this.bottomState = pBottomState;
    this.inactiveState = pInactiveState;
  }

  private int maxInfeasibleCexFor(Set<? extends Property> pProperties) {
    ARGCPA argCpa = CPAs.retrieveCPA(GlobalInfo.getInstance().getCPA().get(), ARGCPA.class);
    return argCpa.getCexSummary().getMaxInfeasibleCexCountFor(pProperties);
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

  private boolean isBudgedExhausted(Property pProperty) {
    final int targetDisabledAfterRefinements = MultiPropertyAlgorithm.hackyRefinementBound;

    int timesHandled = feasibleViolationsOf(ImmutableSet.of(pProperty));
    int maxInfeasibleCexs = maxInfeasibleCexFor(ImmutableSet.of(pProperty));

    final boolean result =
               (violationsLimit > 0
                   && timesHandled >= violationsLimit) // the new state is the ith+1
             || (targetDisabledAfterRefinements > 0
                 && maxInfeasibleCexs >= targetDisabledAfterRefinements);

    if (avgRefineTimeLimit.asMillis() > 0
     || totalRefineTimeLimit.asMillis() > 0) {
      try {
        Optional<StatCpuTime> t = PropertyStats.INSTANCE.getRefinementTime(pProperty);
        if (t.isPresent()) {
          StatCpuTime s = t.get();
          if (s.getIntervals() > 0) {
            final long avgMsec = s.getCpuTimeSum().asMillis()  / s.getIntervals();
            logger.logf(Level.INFO, "Precision refinement time (msec) for %s: %d avg, %d total",
                pProperty.toString(), avgMsec, s.getCpuTimeSum().asMillis());

            if (avgRefineTimeLimit.asMillis() > 0
                && avgMsec > avgRefineTimeLimit.asMillis() * hackyLimitFactor) {
              logger.log(Level.INFO, "Exhausted avg. refine. time of property " + pProperty.toString());
              return true;
            }

            if (totalRefineTimeLimit.asMillis() > 0
                && s.getCpuTimeSum().asMillis() > totalRefineTimeLimit.asMillis() * hackyLimitFactor) {
              logger.log(Level.INFO, "Exhausted total refine. time of property " + pProperty.toString());
              return true;
            }
          }
        }
      } catch (NoTimeMeasurement e) {
      }
    }

    return result;
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

    ImmutableSet<? extends SafetyProperty> encoded = automaton.getEncodedProperties();
    ImmutableSet<SafetyProperty> disabled = pi.getBlacklist();
    if (disabled.containsAll(encoded)) {
      return Optional.of(PrecisionAdjustmentResult.create(
          inactiveState,
          pi, Action.CONTINUE));
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
        Set<SafetyProperty> exhausted = Sets.newHashSet();

        for (SafetyProperty p: violated) {
          if (isBudgedExhausted(p)) {
            exhausted.add(p);
          }
        }

        if (exhausted.size() > 0) {
          final AutomatonPrecision piPrime = pi.cloneAndAddBlacklisted(exhausted);
          signalDisablingProperties(exhausted);

          return Optional.of(PrecisionAdjustmentResult.create(
              stateOnHandledTarget,
              piPrime, Action.CONTINUE));
        }
      }
    }

    // Handle the BREAK state
    if (internalState.getName().equals(AutomatonInternalState.BREAK.getName())) {
      return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, Action.BREAK));
    }

    // No precision adjustment
    return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, Action.CONTINUE));
  }

}
