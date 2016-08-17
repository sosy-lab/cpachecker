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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisFactory;
import org.sosy_lab.cpachecker.core.AnalysisFactory.Analysis;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.MainCPAStatistics;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.PartitionBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.InitOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning.PartitioningStatus;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.PartitioningOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.PartitioningOperator.PartitioningException;
import org.sosy_lab.cpachecker.core.algorithm.mpa.partitioning.AllThenSepOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpa.partitioning.Partitions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AnalysisCache;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MultiPropertyAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.PropertySummary;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonPrecision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.InterruptProvider;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceConditionManager;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTimeLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.WalltimeLimit;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime.StatCpuTimer;
import org.sosy_lab.cpachecker.util.statistics.Stats;
import org.sosy_lab.cpachecker.util.statistics.Stats.Contexts;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.JMException;

public class MultiPropertyAnalysisFullReset implements MultiPropertyAlgorithm, StatisticsProvider, Statistics {

  @Options(prefix="analysis.mpa")
  public static class MultiPropertyConfiguration {
    @Option(secure = true, name = "partition.operator",
        description = "Operator for determining the partitions of properties that have to be checked.")
    @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.core.algorithm.mpa.partitioning")
    @Nonnull
    public Class<? extends PartitioningOperator> partitionOperatorClass = AllThenSepOperator.class;

    @Option(secure = true, description = "Operator for initializing the waitlist after the partitioning of properties was performed.")
    @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.core.algorithm.mpa")
    @Nonnull
    public Class<? extends InitOperator> initOperatorClass = InitDefaultOperator.class;

    @Option(secure = true, description = "Clear the caches of the analysis (CPA) when starting with a new partition.")
    public boolean clearAnalysisCachesOnRestart = false;

    @Option(secure = true, description = "Call the garbage collector when starting with a new partition.")
    public boolean callGcOnRestart = false;

    public MultiPropertyConfiguration(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }
  }

  protected final MultiPropertyConfiguration mpaConfig;

  protected final PartitioningOperator partitionOperator;
  protected final InitOperator initOperator;

  private final DecompositionStatistics stats = new DecompositionStatistics();

  private final AnalysisFactory analysisFactory;
  @Nullable protected Analysis partitionAnalysis = null;

  protected final LogManager logger;
  protected final InterruptProvider interruptNotifier;
  protected final CFA cfa;

  private Optional<PropertySummary> lastRunPropertySummary = Optional.absent();
  private ResourceLimitChecker reschecker = null;
  protected ShutdownNotifier globalShutdownManager;
  private ReachedSet initialReached;

  public MultiPropertyAnalysisFullReset(Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pGlobalShutdownNotifier, InterruptProvider pShutdownNotifier, CFA pCfa, String pProgramDenotation, MainCPAStatistics pStats)
      throws InvalidConfigurationException, CPAException {

    mpaConfig = new MultiPropertyConfiguration(pConfig);

    analysisFactory = new AnalysisFactory(pConfig, pLogger, pCfa, pProgramDenotation, pStats);

    logger = pLogger;
    cfa = pCfa;

    initOperator = createInitOperator();
    partitionOperator = createPartitioningOperator(pConfig, pLogger);
    interruptNotifier = pShutdownNotifier;
    globalShutdownManager = pGlobalShutdownNotifier;
  }

  protected InitOperator createInitOperator() throws CPAException, InvalidConfigurationException {
    return Classes.createInstance(InitOperator.class, mpaConfig.initOperatorClass, new Class[] {}, new Object[] { }, CPAException.class);
  }

  protected PartitioningOperator createPartitioningOperator(Configuration pConfig, LogManager pLogger)
      throws CPAException, InvalidConfigurationException {

    return Classes.createInstance(PartitioningOperator.class, mpaConfig.partitionOperatorClass,
        new Class[] { Configuration.class, LogManager.class }, new Object[] { pConfig, pLogger }, CPAException.class);
  }

  private ImmutableSetMultimap<AbstractState, Property> identifyViolationsInRun(ReachedSet pReachedSet) {
    ImmutableSetMultimap.Builder<AbstractState, Property> result = ImmutableSetMultimap.<AbstractState, Property>builder();

    // ASSUMPTION: no "global refinement" is used! (not yet implemented for this algorithm!)

    final AbstractState e = pReachedSet.getLastState();
    if (AbstractStates.isTargetState(e)) {
      Set<Property> violated = AbstractStates.extractViolatedProperties(e, Property.class);
      result.putAll(e, violated);
    }

    return result.build();
  }

  private ImmutableSortedSet<Property> getActiveProperties(final ReachedSet pReached) {

    Set<Property> active = getActiveProperties(pReached.getFirstState(), pReached);
    Set<Property> inactive = getInactiveProperties(pReached);

    return PropertySets.makeDifference(active, inactive);
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
  static ImmutableSortedSet<Property> getActiveProperties(
      final AbstractState pAbstractState,
      final UnmodifiableReachedSet pReached) {

    Preconditions.checkNotNull(pAbstractState);
    Preconditions.checkNotNull(pReached);
    Preconditions.checkState(!pReached.isEmpty());

    Set<Property> properties = Sets.newLinkedHashSet();

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
        properties.removeAll(ap.getBlacklist().keySet());
      }
    }

    return PropertySets.makeImmutableSortedSet(properties);
  }

  /**
   * Get the INACTIVE properties from the set reached.
   *    (properties that are inactive in any precision)
   *
   * It MUST be EXACT or an OverAPPROXIMATION (of the set of checked properties)
   *    to ensure SOUNDNESS of the analysis result!
   *
   * @param pReachedSet Set of reached states
   * @return  Set of properties
   */
  private ImmutableSortedSet<Property> getInactiveProperties(
      final ReachedSet pReachedSet) {

    Preconditions.checkState(partitionAnalysis.getCpa() instanceof ARGCPA);

    ARGCPA argCpa = (ARGCPA) partitionAnalysis.getCpa();
    Preconditions.checkNotNull(argCpa, "An ARG must be constructed for this type of analysis!");

    // IMPORTANT: Ensure that an reset is performed for this information
    //  as soon the analysis (re-)starts with a new set of properties!!!

    return PropertySets.makeImmutableSortedSet(argCpa.getCexSummary().getDisabledProperties());
  }

  private ImmutableSortedSet<Property> remaining(Set<Property> pAll,
      Set<Property> pViolated,
      Set<Property> pSatisfied,
      Set<Property> pFinallyExhausted) {

    Set<Property> result = pAll;
    result = Sets.difference(result, pViolated);
    result = Sets.difference(result, pSatisfied);
    result = Sets.difference(result, pFinallyExhausted);

    return PropertySets.makeImmutableSortedSet(result);
  }

  public AlgorithmStatus run() throws CPAException,
      CPAEnabledAnalysisPropertyViolationException, InterruptedException {

    final ImmutableSet<Property> all = getFullSetOfPropertiesToCheck(initialReached);
    stats.consideredProperties.addAll(all);

    logger.logf(Level.INFO, "Checking %d properties.", all.size());
    Preconditions.checkState(all.size() > 0, "At least one property must get checked!");

    final Set<Property> relevant = Sets.newLinkedHashSet();
    final Set<Property> satisfied = Sets.newLinkedHashSet();
    final Set<Property> unknown = Sets.newLinkedHashSet();
    TargetSummary violated = TargetSummary.none();

    try(Contexts ctx = Stats.beginRootContext("Multi-Property Verification")) {

      AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

      Preconditions.checkArgument(initialReached.size() == 1, "Please ensure that analysis.stopAfterError=true!");
      Preconditions.checkArgument(initialReached.getWaitlist().size() == 1);

      final Partitioning noPartitioning = Partitions.none();
      Partitioning checkPartitions;
      Partitioning lastPartitioning;

      try {
        checkPartitions = partition(noPartitioning, all, ImmutableSortedSet.<Property>of());
        lastPartitioning = checkPartitions;
      } catch (PartitioningException e1) {
        throw new CPAException("Partitioning failed!", e1);
      }

      // Initialize the waitlist
      Partitioning remainingPartitions = initAnalysisAndReached(checkPartitions, all);

      // Initialize the check for resource limits
      initAndStartLimitChecker(checkPartitions, checkPartitions.getPartitionBudgeting());

      do {
        final Set<Property> runProperties = getActiveProperties(partitionAnalysis.getReached());

        boolean wasInterrutped = false;

        try (Contexts runCtx = Stats.beginRootContextCollection(runProperties)) {
          Stats.incCounter("Multi-Property Verification Iterations", 1);
          try {

            StatCpuTimer timer = stats.pureAnalysisTime.start();
            try (StatCpuTimer t = Stats.startTimer("Pure Analysis Time")) {

              // Update the budgeting strategy of the automaton CPA
              Collection<ControlAutomatonCPA> automatonCPAs = CPAs.retrieveCPAs(partitionAnalysis.getCpa(), ControlAutomatonCPA.class);
              for (ControlAutomatonCPA cpa: automatonCPAs) {
                cpa.setBudgeting(checkPartitions.getPropertyBudgeting());
              }

              // Run the wrapped algorithm (for example, CEGAR)
              status = status.update(partitionAnalysis.getAlgorithm().run(partitionAnalysis.getReached()));

            } finally {
              timer.stop();

              // Track what properties are relevant for the program
              relevant.addAll(PropertyStats.INSTANCE.getRelevantProperties());

              Set<Property> runExhausted = Sets.intersection(getInactiveProperties(partitionAnalysis.getReached()), runProperties);
              if (runProperties.size() == 1) {
                unknown.addAll(runExhausted);
              }
            }

          } catch (InterruptedException ie) {
            // The shutdown notifier might trigger the interrupted exception either because ...
            wasInterrutped = true;

            if (interruptNotifier.hasTemporaryInterruptRequest()) {
              interruptNotifier.reset();

              // ... A) the resource limit for the analysis run has exceeded
              logger.log(Level.WARNING, "Resource limit for properties exceeded!");

              // Stop the checker
              if (reschecker != null) {
                reschecker.cancel();
              }

              Preconditions.checkState(!partitionAnalysis.getReached().isEmpty());
              stats.numberOfPartitionExhaustions++;

              final ImmutableSet<Property> active = Sets.difference(all,
                  Sets.union(violated.getViolatedProperties(), getInactiveProperties(partitionAnalysis.getReached()))).immutableCopy();
              if (runProperties.size() == 1) {
                unknown.addAll(active);
              }

            } else {
              // ... B) the user (or the operating system) requested a stop of the verifier.

              throw ie;
            }

          }
        }

        globalShutdownManager.shutdownIfNecessary();

        // ASSUMPTION:
        //    The wrapped algorithm immediately returns
        //    for each FEASIBLE counterexample that has been found!
        //    (no global refinement)

        // Identify the properties that were violated during the last verification run
        final TargetSummary runViolated = identifyViolationsInRun(partitionAnalysis.getAlgorithm(), partitionAnalysis.getReached());

        if (runViolated.hasTargetStates()) {

          // The waitlist should never be empty in this case!
          //  There might be violations of other properties after the
          //  last abstract state that was added to 'reached'
          //    (which is the target state, in general)
          //  Ensure that a SPLIT of states is performed before
          //    transiting to the ERROR state!
          Preconditions.checkState(!partitionAnalysis.getReached().getWaitlist().isEmpty(),
              "Potential of hidden violations must be considered!");

          // We have to perform another iteration of the algorithm
          //  to check the remaining properties
          //    (or identify more feasible counterexamples)

          // Add the properties that were violated in this run.
          violated = TargetSummary.union(violated, runViolated);

          // The partitioning operator might remove the violated properties
          //  if we have found sufficient counterexamples
          //
          // THIS DOES NOT MAKE SENSE AT THE MOMENT:
          //    checkPartitions = checkPartitions.substract(ImmutableSet.<Property>copyOf(runViolated.values()));

          // Just adjust the precision of the states in the waitlist
          Precisions.updatePropertyBlacklistOnWaitlist((ARGCPA) partitionAnalysis.getCpa(), partitionAnalysis.getReached(), violated.getViolatedProperties());

        } else {

          if (partitionAnalysis.getReached().getWaitlist().isEmpty()
              && !wasInterrutped // The Interrupt might have been performed after a 'pop',
                                 //  and before the successor was added to the waitlist.
             ) {
            // We have reached a fixpoint for the non-blacklisted properties.

            // Properties that are (1) still active
            //  and (2) for that no counterexample was found are considered to be save!
            final ImmutableSet<Property> active = Sets.difference(getActiveProperties(partitionAnalysis.getReached()), violated.getViolatedProperties()).immutableCopy();
            satisfied.addAll(active);

            final ImmutableSortedSet<Property> remain = remaining(all, violated.getViolatedProperties(), satisfied, unknown);

            // On the size of the set 'reached' (assertions and statistics)
            final Integer reachedSetSize = partitionAnalysis.getReached().size();
            logger.logf(Level.INFO, "Fixpoint with %d states reached for: %s. %d properties remain to be checked.", reachedSetSize, active.toString(), remain.size());
            Preconditions.checkState(reachedSetSize >= 10, "The set reached has too few states for a correct analysis run! Bug?");
            stats.reachedStatesWithFixpoint.add(reachedSetSize);
            if (Sets.intersection(relevant, runProperties).size() > 0) {
              stats.reachedStatesForRelPropsWithFixpoint.add(reachedSetSize);
            }

          } else {
            // The analysis terminated because it ran out of resources

            // It is not possible to make any statements about
            //   the satisfaction of more properties here!

            // The partitioning must take care that we verify
            //  smaller (or other) partitions in the next run!

            // Statistics
            final Integer reachedSetSize = partitionAnalysis.getReached().size();
            stats.reachedStates.add(reachedSetSize);
          }

          // A new partitioning must be computed.
          final ImmutableSortedSet<Property> remain = remaining(all,
              violated.getViolatedProperties(), satisfied, unknown);

          if (remain.isEmpty()) {
            break;
          }

          if (remainingPartitions.isEmpty()) {
            Stats.incCounter("Adjustments of property partitions", 1);
            try {
              ImmutableSortedSet<Property> disabledProperties = PropertySets.makeDifference(runProperties,
                  getInactiveProperties(partitionAnalysis.getReached()));

              logger.log(Level.INFO, "All properties: " + all.toString());
              logger.log(Level.INFO, "Disabled properties: " + disabledProperties.toString());
              logger.log(Level.INFO, "Satisfied properties: " + satisfied.toString());
              logger.log(Level.INFO, "Violated properties: " + violated.toString());

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

          if (checkPartitions.getStatus() == PartitioningStatus.BREAK) {
            break;
          }

          // Re-initialize the sets 'waitlist' and 'reached'
          stats.numberOfRestarts++;
          remainingPartitions = initAnalysisAndReached(checkPartitions, all);
          // -- Reset the resource limit checker
          initAndStartLimitChecker(checkPartitions, checkPartitions.getPartitionBudgeting());
        }

        globalShutdownManager.shutdownIfNecessary();

        // Run as long as...
        //  ... (1) the fixpoint has not been reached
        //  ... (2) or not all properties have been checked so far.
      } while (partitionAnalysis.getReached().hasWaitingState()
          || remaining(all, violated.getViolatedProperties(), satisfied, unknown).size() > 0 );

      // Compute the overall result:
      //    Violated properties (might have multiple counterexamples)
      //    Safe properties: Properties that were neither violated nor disabled
      //    Not fully checked properties (that were disabled)
      //        (could be derived from the precision of the leaf states)

      logger.log(Level.WARNING, String.format("Multi-property analysis terminated: %d violated, %d satisfied, %d unknown",
          violated.getViolatedProperties().size(), satisfied.size(), remaining(all, violated.getViolatedProperties(),
              satisfied, unknown).size()));

      return status;

    } finally {
      lastRunPropertySummary = Optional.<PropertySummary>of(createSummary(all, relevant, violated.getViolatedProperties(), satisfied, violated));
    }
  }

  protected TargetSummary identifyViolationsInRun(Algorithm pAlgorithm, ReachedSet pReached) {
    return TargetSummary.identifyViolationsInRun(logger, partitionAnalysis.getReached());
  }

  protected ImmutableSet<Property> getFullSetOfPropertiesToCheck(ReachedSet pReachedSet) {
    return getActiveProperties(pReachedSet.getFirstState(), pReachedSet);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet) throws CPAException,
      CPAEnabledAnalysisPropertyViolationException, InterruptedException {
    initialReached = pReachedSet;
    return run();
  }

  private PropertySummary createSummary(final ImmutableSet<Property> all, final Set<Property> relevant,
      final Set<Property> violated, final Set<Property> satisfied, final TargetSummary pTargetSummary) {

    return new PropertySummary() {

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

      @Override
      public ImmutableMap<Property, PresenceCondition> getConditionalViolatedProperties() {
        PresenceConditionManager pcMgr = GlobalInfo.getInstance().getPresenceConditionManager();
        Builder<Property, PresenceCondition> builder = ImmutableMap.<Property, PresenceCondition>builder();
        for (final Entry<Property, Optional<PresenceCondition>> e: pTargetSummary.getViolationConditions().entrySet()) {
          if (e.getValue().isPresent()) {
            try {
              if (!pcMgr.checkEqualsTrue(e.getValue().get())) {
                builder.put(e.getKey(), new PresenceCondition() {
                  private final PresenceCondition pc = e.getValue().get();

                  @Override
                  public String toString() {
                    return pc.toString();
                  }
                });
              }
            } catch (InterruptedException pE) {
              throw new RuntimeException(pE);
            }
          }
        }
        return builder.build();
      }
    };
  }

  private Partitioning partition(Partitioning pLastPartitioning,
       ImmutableSet<Property> pToCheck,
      ImmutableSortedSet<Property> pDisabledProperties)
          throws PartitioningException {

    Partitioning result = partitionOperator.partition(pLastPartitioning, pToCheck,
        pDisabledProperties, PropertyStats.INSTANCE.getPropertyRefinementComparator());

    logger.log(Level.WARNING, String.format("New partitioning (%s) with %d partitions.",
        result.getStatus(), result.partitionCount()));
    {
      int nth = 0;
      for (ImmutableSet<Property> p: result) {
        nth++;
        logger.logf(Level.WARNING, "Partition %d with %d elements: %s", nth, p.size(), p.toString());
      }
    }

    // Check the partition and the budget for feasibility...
    Preconditions.checkState(pToCheck.size() <= 1
        || result.isEmpty()
        || result.getPartitionBudgeting().getPartitionCpuTimeLimit(result.getFirstPartition().size()).isPresent()
        || result.getPartitionBudgeting().getPartitionWallTimeLimit(result.getFirstPartition().size()).isPresent(),
        "You have to specify a time limit for a multi-property verification runs with more than one partition!");

    return result;
  }

  @Override
  public Optional<PropertySummary> getLastRunPropertySummary() {
    return lastRunPropertySummary;
  }

  private synchronized void initAndStartLimitChecker(Partitioning pPartitions, PartitionBudgeting pBudgeting) {

    try {
      // Configure limits
      List<ResourceLimit> limits = Lists.newArrayList();

      Optional<TimeSpan> partCpuTimeLimit = pBudgeting.getPartitionCpuTimeLimit(pPartitions.getFirstPartition().size());
      if (partCpuTimeLimit.isPresent()) {
        limits.add(ProcessCpuTimeLimit.fromNowOn(partCpuTimeLimit.get()));
      }

      Optional<TimeSpan> partWallTimeLimit = pBudgeting.getPartitionWallTimeLimit(pPartitions.getFirstPartition().size());
      if (partWallTimeLimit.isPresent()) {
        limits.add(WalltimeLimit.fromNowOn(partWallTimeLimit.get()));
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

      PredicateCPA predCpa = CPAs.retrieveCPA(partitionAnalysis.getCpa(), PredicateCPA.class);
      if (predCpa != null) {
        predCpa.setShutdownNotifier(interruptNotifier.getReversibleManager().getNotifier());
      }

    } catch (JMException e) {
      throw new RuntimeException("Initialization of ResourceLimitChecker failed!", e);
    }
  }

  protected Partitioning initAnalysisAndReached(final Partitioning pCheckPartitions,
        Set<Property> pAllProperties)
    throws CPAException, InterruptedException {

    Preconditions.checkState(!pCheckPartitions.isEmpty(), "A non-empty set of properties must be checked in a verification run!");
    Partitioning result = Partitions.none();

    // Clear some of the caches (for experiments)
    if (mpaConfig.clearAnalysisCachesOnRestart) {
      for (ConfigurableProgramAnalysis a: CPAs.asIterable(partitionAnalysis.getCpa())) {
        if (a instanceof AnalysisCache) {
          ((AnalysisCache) a).clearCaches();
        }
      }
    }

    if (mpaConfig.callGcOnRestart) {
      System.gc();
    }

    // Reset the information in counterexamples, inactive properties, ...
    try {
      @Nullable List<Automaton> automata = getAutomataFor(pAllProperties);
      partitionAnalysis = analysisFactory.createFreshAnalysis(interruptNotifier.getReversibleManager(), automata);
    } catch (InvalidConfigurationException e) {
      throw new CPAException("Creating the analysis failed!", e);
    }

    ARGCPA argCpa = CPAs.retrieveCPA(partitionAnalysis.getCpa(), ARGCPA.class);
    Preconditions.checkNotNull(argCpa, "An ARG must be constructed for this type of analysis!");
    argCpa.getCexSummary().resetForNewSetOfProperties();

    try (StatCpuTimer t = Stats.startTimer("Re-initialization of 'reached'")) {
      // Delegate the initialization of the set reached (and the waitlist) to the init operator
      result = initOperator.init(pAllProperties, partitionAnalysis.getCpa(), partitionAnalysis.getReached(), pCheckPartitions, cfa);

      logger.log(Level.WARNING, String.format("%d states in reached.", partitionAnalysis.getReached().size()));
      logger.log(Level.WARNING, String.format("%d states in waitlist.", partitionAnalysis.getReached().getWaitlist().size()));

      // Logging: inactive properties
      ImmutableSortedSet<Property> inactive = getInactiveProperties(partitionAnalysis.getReached());
      ImmutableSortedSet<Property> active = getActiveProperties(partitionAnalysis.getReached());
      logger.log(Level.WARNING, String.format("Waitlist with %d active (%d inactive) properties.", active.size(), inactive.size()));
      logger.logf(Level.WARNING, "Active properties: %s", active.toString());
      logger.logf(Level.WARNING, "Inactive properties: %s", inactive.toString());
    }

    return result;
  }

  @Nullable protected List<Automaton> getAutomataFor(Set<Property> pProperties) {
    return null; // null: the automata that were created during CPAcheckers startup are used.
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    pStatsCollection.add(this);
  }

  public static ImmutableSortedSet<Property> getAllProperties(AbstractState pAbstractState, ReachedSet pReached) {
    Preconditions.checkNotNull(pAbstractState);
    Preconditions.checkNotNull(pReached);
    Preconditions.checkState(!pReached.isEmpty());

    ImmutableSortedSet.Builder<Property> result = PropertySets.builder();

    Collection<AutomatonState> automataStates = AbstractStates.extractStatesByType(
        pAbstractState, AutomatonState.class);
    for (AutomatonState e: automataStates) {
      result.addAll(e.getOwningAutomaton().getEncodedProperties());
    }

    return result.build();
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    if (partitionAnalysis.getAlgorithm() instanceof Statistics) {
      ((Statistics) partitionAnalysis.getAlgorithm()).printStatistics(pOut, pResult, pReached);
    }
  }

  @Override
  public String getName() {
    return "";
  }

}
