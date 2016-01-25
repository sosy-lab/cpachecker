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

import java.io.PrintStream;
import java.util.Collection;
import java.util.Comparator;
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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.InitOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.PartitioningOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.PartitioningOperator.PartitioningException;
import org.sosy_lab.cpachecker.core.algorithm.mpa.partitioning.CheaperFirstDivideOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpa.partitioning.Partitions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.PropertySummary;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonPrecision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonPrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
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
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime.NoTimeMeasurement;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime.StatCpuTimer;
import org.sosy_lab.cpachecker.util.statistics.Stats;
import org.sosy_lab.cpachecker.util.statistics.Stats.Contexts;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

@Options(prefix="analysis.mpa")
public final class MultiPropertyAlgorithm implements Algorithm, StatisticsProvider {

  private final Algorithm wrapped;
  private final LogManager logger;
  private final InterruptProvider interruptNotifier;
  private final ARGCPA cpa;
  private final CFA cfa;

  @Option(secure=true, name="partition.operator",
      description = "Operator for determining the partitions of properties that have to be checked.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.core.algorithm.mpa.partitioning")
  @Nonnull private Class<? extends PartitioningOperator> partitionOperatorClass = CheaperFirstDivideOperator.class;
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

  private class MPAStatistics extends AbstractStatistics {
    int numberOfRestarts = 0;
    int numberOfPartitionExhaustions = 0;
    final StatCpuTime pureAnalysisTime = new StatCpuTime();

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
      super.printStatistics(pOut, pResult, pReached);

      put(pOut, 0, "Number of restarts", numberOfRestarts);
      put(pOut, 0, "Number of exhaustions", numberOfPartitionExhaustions);
      put(pOut, 0, "Number of analysis runs", pureAnalysisTime.getIntervals());

      try {
        put(pOut, 0, "Min. analysis run CPU time", pureAnalysisTime.getMinCpuTimeSum().formatAs(TimeUnit.SECONDS));
        put(pOut, 0, "Max. analysis run CPU time", pureAnalysisTime.getMaxCpuTimeSum().formatAs(TimeUnit.SECONDS));
        put(pOut, 0, "Avg. analysis run CPU time", pureAnalysisTime.getAvgCpuTimeSum().formatAs(TimeUnit.SECONDS));
        put(pOut, 0, "Total analysis run CPU time", pureAnalysisTime.getCpuTimeSum().formatAs(TimeUnit.SECONDS));
      } catch (NoTimeMeasurement e) {
      }
    }
  }

  private MPAStatistics stats = new MPAStatistics();
  private Optional<PropertySummary> lastRunPropertySummary = Optional.absent();
  private ResourceLimitChecker reschecker = null;

  public static int hackyRefinementBound = 0;

  private final Comparator<Property> propertyExplosionComparator = new Comparator<Property>() {
    @Override
    public int compare(Property p1, Property p2) {
      final double p1ExplosionFactor = PropertyStats.INSTANCE.getExplosionFactor(p1);
      final double p2ExplosionFactor = PropertyStats.INSTANCE.getExplosionFactor(p1);

      // -1 : P1 is cheaper
      // +1 : P1 is more expensive
      if (p1ExplosionFactor < p2ExplosionFactor) {
        return -1;
      } else if (p1ExplosionFactor > p2ExplosionFactor) {
        return 1;
      } else {
        return 0;
      }
    }
  };

  private final Comparator<Property> propertyRefinementComparator = new Comparator<Property>() {

    @Override
    public int compare(Property p1, Property p2) {
      Optional<StatCounter> p1refCount = PropertyStats.INSTANCE.getRefinementCount(p1);
      Optional<StatCounter> p2refCount = PropertyStats.INSTANCE.getRefinementCount(p2);
      Optional<StatCpuTime> p1refTime = PropertyStats.INSTANCE.getRefinementTime(p1);
      Optional<StatCpuTime> p2refTime = PropertyStats.INSTANCE.getRefinementTime(p2);

      // -1 : P1 is cheaper
      // +1 : P1 is more expensive

      if (p1refTime.isPresent()) {
        if (!p2refTime.isPresent()) {
          return 1;
        }

        try {
          if (p1refTime.get().getCpuTimeSum().asMillis() < p2refTime.get().getCpuTimeSum().asMillis()) {
            return -1;
          } else if (p1refTime.get().getCpuTimeSum().asMillis() > p2refTime.get().getCpuTimeSum().asMillis()) {
            return 1;
          }
        } catch (NoTimeMeasurement e) {
          return 0;
        }
      }

      if (p1refCount.isPresent()) {
        if (!p2refCount.isPresent()) {
          return 1;
        }

        if (p1refCount.get().getValue() < p2refCount.get().getValue()) {
          return -1;
        } else if (p1refCount.get().getValue() > p2refCount.get().getValue()) {
          return 1;
        }
      }

      return 0;
    }
  };

  public MultiPropertyAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa,
    Configuration pConfig, LogManager pLogger, InterruptProvider pShutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException, CPAException {

    pConfig.inject(this);

    this.wrapped = pAlgorithm;
    this.logger = pLogger;
    this.cfa = pCfa;

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

  private Set<Property> remaining(Set<Property> pAll,
      Set<Property> pViolated,
      Set<Property> pSatisfied,
      Set<Property> pExhausted) {

    return Sets.difference(pAll, Sets.union(Sets.union(pViolated, pSatisfied), pExhausted));
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet) throws CPAException,
      CPAEnabledAnalysisPropertyViolationException, InterruptedException {

    final ImmutableSet<Property> all = getActiveProperties(pReachedSet.getFirstState(), pReachedSet);

    logger.logf(Level.INFO, "Checking %d properties.", all.size());
    Preconditions.checkState(all.size() > 0, "At least one property must get checked!");

    final Set<Property> relevant = Sets.newHashSet();
    final Set<Property> violated = Sets.newHashSet();
    final Set<Property> satisfied = Sets.newHashSet();
    final Set<Property> exhausted = Sets.newHashSet();

    try(Contexts ctx = Stats.beginRootContext("Multi-Property Verification")) {

      AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

      Preconditions.checkArgument(pReachedSet.size() == 1, "Please ensure that analysis.stopAfterError=true!");
      Preconditions.checkArgument(pReachedSet.getWaitlist().size() == 1);

      final Partitioning noPartitioning = Partitions.none();
      Partitioning remainingPartitions = Partitions.none();
      Partitioning checkPartitions;
      Partitioning lastPartitioning;

      try {
        checkPartitions = partition(noPartitioning, all, ImmutableSet.<Property>of());
        lastPartitioning = checkPartitions;
      } catch (PartitioningException e1) {
        throw new CPAException("Partitioning failed!", e1);
      }

      // Initialize the check for resource limits
      initAndStartLimitChecker();

      // Initialize the waitlist
      initReached(pReachedSet, checkPartitions);

      do {
        Stats.incCounter("Multi-Property Verification Iterations", 1);

        try {

          StatCpuTimer timer = stats.pureAnalysisTime.start();
          try {

            // Run the wrapped algorithm (for example, CEGAR)
            status = status.update(wrapped.run(pReachedSet));

          } finally {
            timer.stop();

            // Track what properties are relevant for the program
            relevant.addAll(PropertyStats.INSTANCE.getRelevantProperties());
          }

        } catch (InterruptedException ie) {
          // The shutdown notifier might trigger the interrupted exception
          // either because ...

          if (interruptNotifier.hasTemporaryInterruptRequest()) {
            interruptNotifier.reset();

            // A) the resource limit for the analysis run has exceeded
            logger.log(Level.WARNING, "Resource limit for properties exceeded!");

            // Stop the checker
            if (reschecker != null) {
              reschecker.cancel();
            }

            Preconditions.checkState(!pReachedSet.isEmpty());
            stats.numberOfPartitionExhaustions++;

            SetView<Property> active = Sets.difference(all,
                Sets.union(violated, getInactiveProperties(pReachedSet)));
            if (active.size() == 1) {
              exhausted.addAll(active);
            }

          } else {
            // B) the user (or the operating system) requested a stop of the verifier.

            throw ie;
          }

        }

        interruptNotifier.canInterrupt();

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
          //
          // THIS DOES NOT MAKE SENSE AT THE MOMENT:
          //    checkPartitions = checkPartitions.substract(ImmutableSet.<Property>copyOf(runViolated.values()));

          // Just adjust the precision of the states in the waitlist
          Precisions.updatePropertyBlacklistOnWaitlist(cpa, pReachedSet, violated);

        } else {

          if (pReachedSet.getWaitlist().isEmpty()) {
            // We have reached a fixpoint for the non-blacklisted properties.

            // Properties that are (1) still active
            //  and (2) for that no counterexample was found are considered to be save!
            SetView<Property> active = Sets.difference(all,
                Sets.union(violated, getInactiveProperties(pReachedSet)));
            satisfied.addAll(active);

            Set<Property> remain = remaining(all, violated, satisfied, exhausted);

            logger.logf(Level.INFO, "Fixpoint with %d states reached for: %s. %d properties remain to be checked.", pReachedSet.size(), active.toString(), remain.size());
            Preconditions.checkState(pReachedSet.size() >= 10, "The set reached has too few states for a correct analysis run! Bug?");

          } else {
            // The analysis terminated because it ran out of resources
            // hackyRefinementBound = hackyRefinementBound * 2;

            // It is not possible to make any statements about
            //   the satisfaction of more properties here!

            // The partitioning must take care that we verify
            //  smaller (or other) partitions in the next run!
          }

          // A new partitioning must be computed.
          Set<Property> remain = remaining(all, violated, satisfied, exhausted);

          if (remain.isEmpty()) {
            break;
          }

          if (remainingPartitions.isEmpty()) {
            Stats.incCounter("Adjustments of property partitions", 1);
            try {
              ImmutableSet<Property> disabledProperties = getInactiveProperties(pReachedSet);

              logger.log(Level.INFO, "All properties: " + all.toString());
              logger.log(Level.INFO, "Disabled properties: " + disabledProperties.toString());
              logger.log(Level.INFO, "Satisfied properties: " + satisfied.toString());
              logger.log(Level.INFO, "Violated properties: " + violated.toString());

              ControlAutomatonPrecisionAdjustment.hackyLimitFactor = ControlAutomatonPrecisionAdjustment.hackyLimitFactor * 2;

              checkPartitions = partition(lastPartitioning, remain, disabledProperties);
              lastPartitioning = checkPartitions;

              // Reset the statistics of the properties
              PropertyStats.INSTANCE.clear();

            } catch (PartitioningException e) {
              logger.log(Level.INFO, e.getMessage());
              break;
            }
          } else {
            checkPartitions = remainingPartitions;
          }

          // Re-initialize the sets 'waitlist' and 'reached'
          stats.numberOfRestarts++;
          remainingPartitions = initReached(pReachedSet, checkPartitions);
          // -- Reset the resource limit checker
          initAndStartLimitChecker();
        }

        interruptNotifier.canInterrupt();

        // Run as long as...
        //  ... (1) the fixpoint has not been reached
        //  ... (2) or not all properties have been checked so far.
      } while (pReachedSet.hasWaitingState()
          || remaining(all, violated, satisfied, exhausted).size() > 0);

      // Compute the overall result:
      //    Violated properties (might have multiple counterexamples)
      //    Safe properties: Properties that were neither violated nor disabled
      //    Not fully checked properties (that were disabled)
      //        (could be derived from the precision of the leaf states)

      logger.log(Level.WARNING, String.format("Multi-property analysis terminated: %d violated, %d satisfied, %d unknown",
          violated.size(), satisfied.size(), remaining(all, violated, satisfied, exhausted).size()));

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
        public Optional<ImmutableSet<Property>> getRelevantProperties() {
          return Optional.of(ImmutableSet.copyOf(relevant));
        }

        @Override
        public Optional<ImmutableSet<Property>> getSatisfiedProperties() {
          return Optional.of(ImmutableSet.copyOf(satisfied));
        }

        @Override
        public ImmutableSet<Property> getConsideredProperties() {
          return ImmutableSet.copyOf(all);
        }
      });
    }
  }

  private Partitioning partition(
      Partitioning lastPartitioning,
      Set<Property> remaining,
      ImmutableSet<Property> disabledProperties) throws PartitioningException {

    Partitioning result = partitionOperator.partition(lastPartitioning, remaining,
        disabledProperties, propertyRefinementComparator);

    logger.log(Level.WARNING, String.format("New partitioning with %d partitions.", result.partitionCount()));
    {
      int nth = 0;
      for (ImmutableSet<Property> p: result) {
        nth++;
        logger.logf(Level.WARNING, "Partition %d with %d elements: %s", nth, p.size(), p.toString());
      }
    }

    return result;
  }

  public Optional<PropertySummary> getLastRunPropertySummary() {
    return lastRunPropertySummary;
  }

  private synchronized void initAndStartLimitChecker() {

    try {
      // Configure limits
      List<ResourceLimit> limits = Lists.newArrayList();

      if (cpuTime.compareTo(TimeSpan.empty()) >= 0) {
        limits.add(ProcessCpuTimeLimit.fromNowOn(cpuTime));
      }

      if (walltime.compareTo(TimeSpan.empty()) >= 0) {
        limits.add(WalltimeLimit.fromNowOn(walltime));
      }

      // Stop the old check
      if (reschecker != null) {
        reschecker.cancel();
      }

      // Start the check
      reschecker = new ResourceLimitChecker(
          interruptNotifier.getReversibleManager(), // The order of notifiers is important!
          limits);

      reschecker.start();

      PredicateCPA predCpa = CPAs.retrieveCPA(cpa, PredicateCPA.class);
      if (predCpa != null) {
        predCpa.setShutdownNotifier(interruptNotifier.getReversibleManager().getNotifier());
      }

    } catch (JMException e) {
      throw new RuntimeException("Initialization of ResourceLimitChecker failed!", e);
    }
  }

  private Partitioning initReached(final ReachedSet pReachedSet,
      final Partitioning pCheckPartitions) throws CPAException, InterruptedException {

    Preconditions.checkState(!pCheckPartitions.isEmpty(), "A non-empty set of properties must be checked in a verification run!");

    Partitioning result = Partitions.none();

    // Reset the information in counterexamples, inactive properties, ...
    ARGCPA argCpa = CPAs.retrieveCPA(cpa, ARGCPA.class);
    Preconditions.checkNotNull(argCpa, "An ARG must be constructed for this type of analysis!");
    argCpa.getCexSummary().resetForNewSetOfProperties();

    try (StatCpuTimer t = Stats.startTimer("Re-initialization of 'reached'")) {
      // Delegate the initialization of the set reached (and the waitlist) to the init operator
      result = initOperator.init(cpa, pReachedSet, pCheckPartitions, cfa);

      logger.log(Level.WARNING, String.format("%d states in reached.", pReachedSet.size()));
      logger.log(Level.WARNING, String.format("%d states in waitlist.", pReachedSet.getWaitlist().size()));

      // Logging: inactive properties
      ImmutableSet<Property> inactive = getInactiveProperties(pReachedSet);
      logger.log(Level.WARNING, String.format("Waitlist with %d inactive properties.", inactive.size()));
      for (Property p: inactive) {
        logger.logf(Level.WARNING, "INACTIVE: %s", p.toString());
      }
    }

    return result;
  }

  public static String toReadable(Iterable<ImmutableSet<Property>> pSetsOfProps) {
    final StringBuilder result = new StringBuilder();
    result.append("[");
    for (Set<Property> s: pSetsOfProps) {
      result.append("[");
      boolean first = true;
      for (Property p: s) {
        if (!first) {
          result.append(",");
        }
        result.append(p.toString());
        first = false;
      }
      result.append("]");
    }
    result.append("]");
    return result.toString();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    if (wrapped instanceof StatisticsProvider) {
      ((StatisticsProvider)wrapped).collectStatistics(pStatsCollection);
    }
  }

  public static ImmutableSet<Property> getAllProperties(AbstractState pAbstractState, ReachedSet pReached) {
    Preconditions.checkNotNull(pAbstractState);
    Preconditions.checkNotNull(pReached);
    Preconditions.checkState(!pReached.isEmpty());

    Builder<Property> result = ImmutableSet.<Property>builder();

    Collection<AutomatonState> automataStates = AbstractStates.extractStatesByType(
        pAbstractState, AutomatonState.class);
    for (AutomatonState e: automataStates) {
      result.addAll(e.getOwningAutomaton().getEncodedProperties());
    }

    return result.build();
  }

}
