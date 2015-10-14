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
package org.sosy_lab.cpachecker.core.algorithm.mpa;

import static org.sosy_lab.cpachecker.util.AbstractStates.isTargetState;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.InitOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.PartitioningOperator;
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

@Options(prefix="analysis.mpa")
public final class MultiPropertyAlgorithm implements Algorithm {

  private final Algorithm wrapped;
  private final LogManager logger;
  private final ConfigurableProgramAnalysis cpa;

  @Option(secure=true, description = "Operator for determining the partitions of properties that have to be checked.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.core.algorithm.mpa")
  @Nonnull private Class<? extends PartitioningOperator> partitionOperatorClass = DefaultPartitioningOperator.class;
  private final PartitioningOperator partitionOperator;

  @Option(secure=true, description = "Operator for initializing the waitlist after the partitioning of properties was performed.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.core.algorithm.mpa")
  @Nonnull private Class<? extends InitOperator> initOperatorClass = DefaultInitOperator.class;
  private final InitOperator initOperator;

  public MultiPropertyAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa,
    Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException, CPAException {

    pConfig.inject(this);

    this.wrapped = pAlgorithm;
    this.logger = pLogger;
    this.cpa = pCpa;

    this.initOperator = createInitOperator();
    this.partitionOperator = createPartitioningOperator();
  }

  private InitOperator createInitOperator() throws CPAException, InvalidConfigurationException {
    return Classes.createInstance(InitOperator.class, initOperatorClass, new Class[] { }, new Object[] { }, CPAException.class);
  }

  private PartitioningOperator createPartitioningOperator() throws CPAException, InvalidConfigurationException {
    return Classes.createInstance(PartitioningOperator.class, partitionOperatorClass, new Class[] { }, new Object[] { }, CPAException.class);
  }

  private ImmutableMultimap<AbstractState, Property> identifyViolationsInRun(ReachedSet pReachedSet) {
    Builder<AbstractState, Property> result = ImmutableMultimap.<AbstractState, Property>builder();

    // ASSUMPTION: no "global refinement" is used! (not yet implemented for this algorithm!)

    final AbstractState e = pReachedSet.getLastState();
    if (isTargetState(e)) {
      Set<Property> violated = AbstractStates.extractViolatedProperties(e, Property.class);
      result.putAll(e, violated);
    }

    return result.build();
  }

  private ImmutableSet<Property> getActiveProperties(final ReachedSet pReached) {
    return null;
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet) throws CPAException,
      CPAEnabledAnalysisPropertyViolationException {

    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    final ImmutableSet<Property> all = getActiveProperties(pReachedSet);
    final Set<Property> violated = Sets.newHashSet();
    final Set<Property> satisfied = Sets.newHashSet();

    final ImmutableSet<ImmutableSet<Property>> lastCheckedPartitioning = ImmutableSet.of();

    do {
      final ImmutableSet<ImmutableSet<Property>> checkPartitions =
          partitionOperator.partition(lastCheckedPartitioning, all, violated, satisfied);

      initOperator.init(pReachedSet, checkPartitions);

      try {
        // Run the wrapped algorithm (for example, CEGAR)
        status = status.update(wrapped.run(pReachedSet));

      } catch (InterruptedException ie) {
        // The shutdown notifier might trigger the interrupted exception
        // either because
        //    A) the resource limit for the analysis run has exceeded
        // or
        //    B) the user (or the operating system) requested a stop of the verifier.
        Preconditions.checkState(!pReachedSet.isEmpty());
      }

      // ASSUMPTION:
      //    The wrapped algorithm immediately returns
      //    for each FEASIBLE counterexample that has been found!
      //    (no global refinement)

      // Identify the properties that were violated during the last verification run
      final ImmutableMultimap<AbstractState, Property> runViolated;
      runViolated = identifyViolationsInRun(pReachedSet);
      violated.addAll(runViolated.values());

      // Identify the properties that were deactivated
      final ImmutableSet<Property> inactive;
      inactive = identifyInactiveProperties(pReachedSet);

      if (runViolated.size() > 0) {
        // We have to perform another iteration of the algorithm
        //  to check the remaining properties
        //    (or identify more feasible counterexamples)

      } else if (!pReachedSet.getWaitlist().isEmpty()) {
        // The analysis terminated because it ran out of resources

      } else if (pReachedSet.getWaitlist().isEmpty()) {
        // We have reached a fixpoint for the non-blacklisted properties.

      } else {
        Preconditions.checkState(false, "This state should never be entered!");
      }

      // Run as long as...
      //  ... (1) the fixpoint has not been reached
      //  ... (2) or not all properties have been checked so far.
    } while (pReachedSet.hasWaitingState()
        || Sets.difference(Sets.difference(all, satisfied), violated).size() > 0);

    // Compute the overall result:
    //    Violated properties (might have multiple counterexamples)
    //    Safe properties: Properties that were neither violated nor disabled
    //    Not fully checked properties (that were disabled)
    //        (could be derived from the precision of the leaf states)

    return status;
  }

  private void init(ReachedSet pReachedSet, ImmutableSet<ImmutableSet<Property>> pCheckPartitions) {

    // Build the list of properties that should not be checked in this run
    ImmutableSet.Builder<Property> blacklistBuilder = ImmutableSet.builder();
    ImmutableSet<Property> blacklisted = null;

  }

  private ImmutableSet<ImmutableSet<Property>> partitionProperties(
      ImmutableSet<ImmutableSet<Property>> pLastCheckedPartitioning, ImmutableSet<Property> pAll,
      Set<Property> pViolated, Set<Property> pSatisfied) {
    // TODO Auto-generated method stub
    return null;
  }

  private ImmutableSet<Property> identifyInactiveProperties(ReachedSet pReachedSet) {
    // TODO Auto-generated method stub
    return null;
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
