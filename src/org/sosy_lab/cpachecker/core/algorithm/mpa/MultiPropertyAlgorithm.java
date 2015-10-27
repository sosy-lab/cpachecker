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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.management.JMException;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.InitOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.PartitioningOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.PartitioningOperator.PartitioningException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.PropertySummary;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonPrecision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonSafetyProperty;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.InterruptProvider;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTimeLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.WalltimeLimit;
import org.sosy_lab.cpachecker.util.statistics.Stats;
import org.sosy_lab.cpachecker.util.statistics.Stats.Contexts;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

@Options(prefix="analysis.mpa")
public final class MultiPropertyAlgorithm implements Algorithm {

  private final Algorithm wrapped;
  private final LogManager logger;
  private final InterruptProvider interruptNotifier;
  private final ARGCPA cpa;

  @Option(secure=true, description = "Operator for determining the partitions of properties that have to be checked.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.core.algorithm.mpa")
  @Nonnull private Class<? extends PartitioningOperator> partitionOperatorClass = PartitioningDefaultOperator.class;
  private final PartitioningOperator partitionOperator;

  @Option(secure=true, description = "Operator for initializing the waitlist after the partitioning of properties was performed.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.core.algorithm.mpa")
  @Nonnull private Class<? extends InitOperator> initOperatorClass = InitDefaultOperator.class;
  private final InitOperator initOperator;

  @Option(secure=true, name="partition.time.wall",
      description="Limit for wall time used by CPAchecker (use seconds or specify a unit; -1 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.NANOSECONDS,
      defaultUserUnit=TimeUnit.SECONDS,
      min=-1)
  private TimeSpan walltime = TimeSpan.ofNanos(-1);

  @Option(secure=true, name="partition.time.cpu",
      description="Limit for cpu time used by CPAchecker (use seconds or specify a unit; -1 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.NANOSECONDS,
      defaultUserUnit=TimeUnit.SECONDS,
      min=-1)
  private TimeSpan cpuTime = TimeSpan.ofNanos(-1);

  private Optional<PropertySummary> lastRunPropertySummary = Optional.absent();

  public MultiPropertyAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa,
    Configuration pConfig, LogManager pLogger, InterruptProvider pShutdownNotifier)
      throws InvalidConfigurationException, CPAException {

    pConfig.inject(this);

    this.wrapped = pAlgorithm;
    this.logger = pLogger;

    if (!(pCpa instanceof ARGCPA)) {
      throw new InvalidConfigurationException("ARGCPA needed for MultiPropertyAlgorithm");
    }
    this.cpa = (ARGCPA) pCpa;

    this.initOperator = createInitOperator();
    this.partitionOperator = createPartitioningOperator();
    this.interruptNotifier = pShutdownNotifier;
  }

  private InitOperator createInitOperator() throws CPAException, InvalidConfigurationException {
    return Classes.createInstance(InitOperator.class, initOperatorClass, new Class[] { }, new Object[] { }, CPAException.class);
  }

  private PartitioningOperator createPartitioningOperator() throws CPAException, InvalidConfigurationException {
    return Classes.createInstance(PartitioningOperator.class, partitionOperatorClass, new Class[] { }, new Object[] { }, CPAException.class);
  }

  private ImmutableSetMultimap<AbstractState, Property> identifyViolationsInRun(ReachedSet pReachedSet) {
    ImmutableSetMultimap.Builder<AbstractState, Property> result = ImmutableSetMultimap.<AbstractState, Property>builder();

    // ASSUMPTION: no "global refinement" is used! (not yet implemented for this algorithm!)

    final AbstractState e = pReachedSet.getLastState();
    if (isTargetState(e)) {
      Set<Property> violated = AbstractStates.extractViolatedProperties(e, Property.class);
      result.putAll(e, violated);
    }

    return result.build();
  }

  /**
   * Get the properties that are active in all instances of a precision!
   *    A property is not included if it is INACTIVE in one of
   *    the precisions in the given set reached.
   *
   * It MUST be EXACT or an UnderAPPROXIMATION (of the set of checked properties)
   *    to ensure SOUNDNESS of the analysis result!
   *
   * @return            Set of properties
   */
  static ImmutableSet<Property> getActiveProperties(
      final AbstractState pAbstractState,
      final UnmodifiableReachedSet pReached) {

    Preconditions.checkNotNull(pAbstractState);
    Preconditions.checkNotNull(pReached);
    Preconditions.checkState(!pReached.isEmpty());

    Set<Property> properties = Sets.newHashSet();

    // Retrieve the checked properties from the abstract state
    Collection<AutomatonState> automataStates = AbstractStates.extractStatesByType(
        pAbstractState, AutomatonState.class);
    for (AutomatonState e: automataStates) {
      properties.addAll(e.getOwningAutomaton().getEncodedProperties());
    }

    // Blacklisted properties from the precision
    Precision prec = pReached.getPrecision(pAbstractState);
    for (Precision p: Precisions.asIterable(prec)) {
      if (p instanceof AutomatonPrecision) {
        AutomatonPrecision ap = (AutomatonPrecision) p;
        properties.removeAll(ap.getBlacklist());
      }
    }

    return ImmutableSet.copyOf(properties);
  }

  /**
   * Get the INACTIVE properties from the set reached.
   *    (properties that are inactive in any precision)
   *
   * It MUST be EXACT or an OverAPPROXIMATION (of the set of checked properties)
   *    to ensure SOUNDNESS of the analysis result!
   *
   * @param pReachedSet
   * @return  Set of properties
   */
  private ImmutableSet<Property> getInactiveProperties(
      final ReachedSet pReachedSet) {

    ARGCPA argCpa = CPAs.retrieveCPA(cpa, ARGCPA.class);
    Preconditions.checkNotNull(argCpa, "An ARG must be constructed for this type of analysis!");

    // IMPORTANT: Ensure that an reset is performed for this information
    //  as soon the analysis (re-)starts with a new set of properties!!!

    return argCpa.getCexSummary().getDisabledProperties();
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet) throws CPAException,
      CPAEnabledAnalysisPropertyViolationException, InterruptedException {

    final ImmutableSet<Property> all = getActiveProperties(pReachedSet.getFirstState(), pReachedSet);
    final Set<Property> violated = Sets.newHashSet();
    final Set<Property> satisfied = Sets.newHashSet();

    try(Contexts ctx = Stats.beginRootContext("Multi-Property Verification")) {

      AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

      Preconditions.checkArgument(pReachedSet.size() == 1, "Please ensure that analysis.stopAfterError=true!");
      Preconditions.checkArgument(pReachedSet.getWaitlist().size() == 1);

      final AbstractState e0 = pReachedSet.getFirstState();
      final Precision pi0 = pReachedSet.getPrecision(e0);

      final ImmutableSet<ImmutableSet<Property>> noPartitioning = ImmutableSet.of();

      ImmutableSet<ImmutableSet<Property>> checkPartitions;
      try {
        checkPartitions = partitionOperator.partition(noPartitioning, all);
      } catch (PartitioningException e1) {
        throw new CPAException("Partitioning failed!", e1);
      }

      // Initialize the check for resource limits
      initAndStartLimitChecker();

      // Initialize the waitlist
      initReached(pReachedSet, e0, pi0, checkPartitions);

      do {
        Stats.incCounter("Multi-Property Verification Iterations", 1);

        try {

          // Run the wrapped algorithm (for example, CEGAR)
          status = status.update(wrapped.run(pReachedSet));

        } catch (InterruptedException ie) {
          // The shutdown notifier might trigger the interrupted exception
          // either because ...

          if (interruptNotifier.getNotifier().shouldShutdown()) {

            // A) the resource limit for the analysis run has exceeded
            logger.log(Level.WARNING, "Resource limit for properties exceeded!");

            Preconditions.checkState(!pReachedSet.isEmpty());
            Stats.incCounter("Times reachability interrupted", 1);

          } else {
            // B) the user (or the operating system) requested a stop of the verifier.

            throw ie;
          }

        }

        // ASSUMPTION:
        //    The wrapped algorithm immediately returns
        //    for each FEASIBLE counterexample that has been found!
        //    (no global refinement)

        // Identify the properties that were violated during the last verification run
        final ImmutableSetMultimap<AbstractState, Property> runViolated;
        runViolated = identifyViolationsInRun(pReachedSet);

        if (runViolated.size() > 0) {

          // The waitlist should never be empty in this case!
          //  There might be violations of other properties after the
          //  last abstract state that was added to 'reached'
          //    (which is the target state, in general)
          //  Ensure that a SPLIT of states is performed before
          //    transiting to the ERROR state!
          Preconditions.checkState(!pReachedSet.getWaitlist().isEmpty(),
              "Potential of hidden violations must be considered!");

          // We have to perform another iteration of the algorithm
          //  to check the remaining properties
          //    (or identify more feasible counterexamples)

          // Add the properties that were violated in this run.
          violated.addAll(runViolated.values());

          // The partitioning operator might remove the violated properties
          //  if we have found sufficient counterexamples
          checkPartitions = removePropertiesFrom(checkPartitions, ImmutableSet.<Property>copyOf(runViolated.values()));

          // Just adjust the precision of the states in the waitlist
          disablePropertiesForWaitlist(pReachedSet, violated);

        } else {

          if (pReachedSet.getWaitlist().isEmpty()) {
            // We have reached a fixpoint for the non-blacklisted properties.

            // Properties that are (1) still active
            //  and (2) for that no counterexample was found are considered to be save!
            SetView<Property> active = Sets.difference(all,
                Sets.union(violated, getInactiveProperties(pReachedSet)));
            satisfied.addAll(active);

          } else {
            // The analysis terminated because it ran out of resources

            // It is not possible to make any statements about
            //   the satisfaction of more properties here!

            // The partitioning must take care that we verify
            //  smaller (or other) partitions in the next run!
          }

          // A new partitioning must be computed.
          Set<Property> remaining = Sets.difference(all, Sets.union(violated, satisfied));

          if (remaining.isEmpty()) {
            break;
          }

          Stats.incCounter("Adjustments of property partitions", 1);
          try {
            checkPartitions = partitionOperator.partition(checkPartitions, remaining);
          } catch (PartitioningException e) {
            logger.log(Level.INFO, e.getMessage());
            break;
          }

          // Re-initialize the sets 'waitlist' and 'reached'
          initReached(pReachedSet, e0, pi0, checkPartitions);
          // -- Reset the resource limit checker
          initAndStartLimitChecker();
        }

        // Run as long as...
        //  ... (1) the fixpoint has not been reached
        //  ... (2) or not all properties have been checked so far.
      } while (pReachedSet.hasWaitingState()
          || Sets.difference(all, Sets.union(violated, satisfied)).size() > 0);

      // Compute the overall result:
      //    Violated properties (might have multiple counterexamples)
      //    Safe properties: Properties that were neither violated nor disabled
      //    Not fully checked properties (that were disabled)
      //        (could be derived from the precision of the leaf states)

      logger.log(Level.WARNING, String.format("Multi-property analysis terminated: %d violated, %d satisfied, %d unknown",
          violated.size(), satisfied.size(), Sets.difference(all, Sets.union(violated, satisfied)).size()));

      return status;

    } finally {
      lastRunPropertySummary = Optional.<PropertySummary>of(new PropertySummary() {

        @Override
        public ImmutableSet<Property> getViolatedProperties() {
          return ImmutableSet.copyOf(violated);
        }

        @Override
        public Optional<ImmutableSet<Property>> getUnknownProperties() {
          return Optional.of(ImmutableSet.copyOf(Sets.difference(all, Sets.union(violated, satisfied))));
        }

        @Override
        public Optional<ImmutableSet<Property>> getSatisfiedProperties() {
          return Optional.of(ImmutableSet.copyOf(satisfied));
        }
      });
    }
  }

  public Optional<PropertySummary> getLastRunPropertySummary() {
    return lastRunPropertySummary;
  }

  private void initAndStartLimitChecker() {

    try {
      interruptNotifier.reset();

      // Configure limits
      List<ResourceLimit> limits = Lists.newArrayList();

      if (cpuTime.compareTo(TimeSpan.empty()) >= 0) {
        limits.add(ProcessCpuTimeLimit.fromNowOn(cpuTime));
      }

      if (walltime.compareTo(TimeSpan.empty()) >= 0) {
        limits.add(WalltimeLimit.fromNowOn(walltime));
      }

      // Start the check
      ResourceLimitChecker checker = new ResourceLimitChecker(
          interruptNotifier.getNotifier(), // The order of notifiers is important!
          limits);

      checker.start();

    } catch (JMException e) {
      throw new RuntimeException("Initialization of ResourceLimitChecker failed!", e);
    }
  }

  private void initReached(final ReachedSet pReachedSet,
      final AbstractState pE0, final Precision pPi0,
      final ImmutableSet<ImmutableSet<Property>> pCheckPartitions) {

    // Delegate the initialization of the set reached (and the waitlist) to the init operator
    initOperator.init(pReachedSet, pE0, pPi0, pCheckPartitions);

    logger.log(Level.WARNING, String.format("%d states in reached.", pReachedSet.size()));
    logger.log(Level.WARNING, String.format("%d states in waitlist.", pReachedSet.getWaitlist().size()));
    logger.log(Level.WARNING, String.format("%d partitions.", pCheckPartitions.size()));

    // Reset the information in counterexamples, inactive properties, ...
    ARGCPA argCpa = CPAs.retrieveCPA(cpa, ARGCPA.class);
    Preconditions.checkNotNull(argCpa, "An ARG must be constructed for this type of analysis!");
    argCpa.getCexSummary().resetForNewSetOfProperties();
  }

  static ImmutableSet<ImmutableSet<Property>> removePropertiesFrom(
      ImmutableSet<ImmutableSet<Property>> pOldPartitions,
      Set<Property> pRunViolated) {

    ImmutableSet.Builder<ImmutableSet<Property>> result = ImmutableSet.<ImmutableSet<Property>>builder();

    for (ImmutableSet<Property> p: pOldPartitions) {
      result.add(ImmutableSet.<Property>copyOf(Sets.difference(p, pRunViolated)));
    }

    return result.build();
  }

  static void disablePropertiesForWaitlist(final ReachedSet pReachedSet, final Set<Property> pToBlacklist) {

    final HashSet<AutomatonSafetyProperty> toBlacklist = Sets.newHashSet(
      Collections2.transform(pToBlacklist, new Function<Property, AutomatonSafetyProperty>() {
        @Override
        public AutomatonSafetyProperty apply(Property pProp) {
          Preconditions.checkArgument(pProp instanceof AutomatonSafetyProperty);
          return (AutomatonSafetyProperty) pProp;
        }

      }).iterator());

    // update the precision:
    //  (optional) disable some automata transitions (global precision)
    for (AbstractState e: pReachedSet.getWaitlist()) {

      final Precision pi = pReachedSet.getPrecision(e);
      final Precision piPrime = blacklistProperties(pi, toBlacklist);

      if (piPrime != null) {
        pReachedSet.updatePrecision(e, piPrime);
      }
    }
  }

  public static Precision blacklistProperties(final Precision pi, final HashSet<AutomatonSafetyProperty> toBlacklist) {
    final Precision piPrime = Precisions.replaceByFunction(pi, new Function<Precision, Precision>() {
      @Override
      public Precision apply(Precision pPrecision) {
        if (pPrecision instanceof AutomatonPrecision) {
          AutomatonPrecision pi = (AutomatonPrecision) pPrecision;
          return pi.cloneAndAddBlacklisted(toBlacklist);
        }
        return null;
      }
    });
    return piPrime;
  }


}
