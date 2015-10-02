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
package org.sosy_lab.cpachecker.core.algorithm;

import static org.sosy_lab.cpachecker.util.AbstractStates.isTargetState;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonPrecision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonSafetyProperty;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.Sets;

@Options
public class MultiPropertyAlgorithm implements Algorithm {

  private final Algorithm wrapped;
  private final LogManager logger;
  private final ConfigurableProgramAnalysis cpa;

  public MultiPropertyAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa,
    Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException, CPAException {

    pConfig.inject(this);

    this.wrapped = pAlgorithm;
    this.logger = pLogger;
    this.cpa = pCpa;
  }

  private ImmutableMultimap<AbstractState, Property> filterNewTargetStates(ReachedSet pReachedSet) {
    Builder<AbstractState, Property> result = ImmutableMultimap.<AbstractState, Property>builder();

    // ASSUMPTION: no "global refinement" is used! (not yet implemented for this algorithm!)

    final AbstractState e = pReachedSet.getLastState();
    if (isTargetState(e)) {
      Set<Property> violated = AbstractStates.extractViolatedProperties(e, Property.class);
      result.putAll(e, violated);
    }

    return result.build();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      CPAEnabledAnalysisPropertyViolationException {

    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    Set<Property> violatedProperties = Sets.newHashSet();

    do {
      // Run the wrapped algorithm (for example, CEGAR)
      status = status.update(wrapped.run(pReachedSet));

      // ASSUMPTION:
      //    The wrapped algorithm immediately returns
      //    for each FEASIBLE counterexample that has been found!
      //    (no global refinement)

      final ImmutableMultimap<AbstractState, Property> violating = filterNewTargetStates(pReachedSet);
      violatedProperties.addAll(violating.values());

      adjustAutomataPrecision(pReachedSet, violatedProperties);

      // run only until the waitlist is empty
    } while (pReachedSet.hasWaitingState());

    // Compute the overall result:
    //    Violated properties (might have multiple counterexamples)
    //    Safe properties: Properties that were neither violated nor disabled
    //    Not fully checked properties (that were disabled)
    //        (could be derived from the precision of the leaf states)

    return status;
  }

  private void adjustAutomataPrecision(final ReachedSet pReachedSet, final Set<Property> pViolatedProperties) {

    final HashSet<AutomatonSafetyProperty> violated = Sets.newHashSet(
      Collections2.transform(pViolatedProperties, new Function<Property, AutomatonSafetyProperty>() {
        @Override
        public AutomatonSafetyProperty apply(Property pArg0) {
          Preconditions.checkArgument(pArg0 instanceof AutomatonSafetyProperty);
          return (AutomatonSafetyProperty) pArg0;
        }

      }).iterator());

    // update the precision:
    //  (optional) disable some automata transitions (global precision)
    for (AbstractState e: pReachedSet.getWaitlist()) {

      final Precision pi = pReachedSet.getPrecision(e);

      final Precision piPrime = Precisions.replaceByFunction(pi, new Function<Precision, Precision>() {
        @Override
        public Precision apply(Precision pArg0) {
          if (pArg0 instanceof AutomatonPrecision) {
            AutomatonPrecision pi = (AutomatonPrecision) pArg0;
            return pi.cloneAndAddBlacklisted(violated);
          }
          return null;
        }
      });

      if (piPrime != null) {
        pReachedSet.updatePrecision(e, piPrime);
        throw new RuntimeException("Merge of precisions from subgraphs to pivot states not yet implemented!!!");
      }
    }


  }

}
