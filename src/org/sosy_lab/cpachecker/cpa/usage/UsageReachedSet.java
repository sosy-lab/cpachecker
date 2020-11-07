// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ObjectOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;
import org.sosy_lab.cpachecker.cpa.lock.LockState;
import org.sosy_lab.cpachecker.cpa.lock.LockState.LockStateBuilder;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageConfiguration;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageContainer;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@SuppressFBWarnings(justification = "No support for serialization", value = "SE_BAD_FIELD")
public class UsageReachedSet extends PartitionedReachedSet {

  private static final long serialVersionUID = 1L;
  private BAMDataManager manager;
  private UsageProcessor usageProcessor;

  private final StatTimer totalTimer = new StatTimer("Time for extracting usages");
  private final StatTimer addingToContainerTimer = new StatTimer("Time for adding to container");
  private final StatTimer usageExpandingTimer = new StatTimer("Time for usage expanding");
  private final StatCounter processingSteps =
      new StatCounter("Number of different reached sets with lock effects");

  private boolean usagesExtracted = false;

  public static class RaceProperty implements Property {
    @Override
    public String toString() {
      return "Race condition";
    }
  }

  private static final ImmutableSet<Property> RACE_PROPERTY = ImmutableSet.of(new RaceProperty());

  private final LogManager logger;
  private final boolean processCoveredUsages;

  private final UsageContainer container;
  private List<Pair<UsageInfo, UsageInfo>> stableUnsafes = ImmutableList.of();

  public UsageReachedSet(
      WaitlistFactory waitlistFactory, UsageConfiguration pConfig, LogManager pLogger) {
    super(waitlistFactory);
    logger = pLogger;
    container = new UsageContainer(pConfig, logger);
    processCoveredUsages = pConfig.getProcessCoveredUsages();
  }

  @Override
  public void remove(AbstractState pState) {
    super.remove(pState);
    UsageState ustate = UsageState.get(pState);
    container.removeState(ustate);
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) {
    super.add(pState, pPrecision);

    /*UsageState USstate = UsageState.get(pState);
    USstate.saveUnsafesInContainerIfNecessary(pState);*/
  }

  @Override
  public void clear() {
    container.resetUnrefinedUnsafes();
    usagesExtracted = false;
    super.clear();
  }

  @Override
  public boolean hasViolatedProperties() {
    extractUsagesIfNeccessary();
    return container.getTotalUnsafeSize() > 0;
  }

  @Override
  public Set<Property> getViolatedProperties() {
    if (hasViolatedProperties()) {
      return RACE_PROPERTY;
    } else {
      return ImmutableSet.of();
    }
  }

  public UsageContainer getUsageContainer() {
    return container;
  }

  public List<Pair<UsageInfo, UsageInfo>> getUnsafes() {
    return stableUnsafes;
  }

  private void writeObject(@SuppressWarnings("unused") ObjectOutputStream stream) {
    throw new UnsupportedOperationException("cannot serialize Logger");
  }

  @Override
  public void finalize(ConfigurableProgramAnalysis pCpa) {
    BAMCPA bamCpa = CPAs.retrieveCPA(pCpa, BAMCPA.class);
    if (bamCpa != null) {
      manager = bamCpa.getData();
    }
    UsageCPA usageCpa = CPAs.retrieveCPA(pCpa, UsageCPA.class);
    usageProcessor = usageCpa.getUsageProcessor();
  }

  private void extractUsagesIfNeccessary() {
    if (!usagesExtracted) {
      totalTimer.start();
      logger.log(Level.INFO, "Analysis is finished, start usage extraction");
      usagesExtracted = true;
      Deque<Pair<AbstractState, Set<LockEffect>>> waitlist = new ArrayDeque<>();
      Multimap<AbstractState, Set<LockEffect>> processedSets = ArrayListMultimap.create();

      Pair<AbstractState, Set<LockEffect>> currentPair = Pair.of(getFirstState(), new HashSet<>());
      waitlist.add(currentPair);
      processedSets.put(getFirstState(), new HashSet<>());
      usageProcessor.updateRedundantUnsafes(container.getNotInterestingUnsafes());

      while (!waitlist.isEmpty()) {
        currentPair = waitlist.pop();
        Set<LockEffect> currentEffects = currentPair.getSecond();
        processingSteps.inc();
        Map<AbstractState, List<UsageInfo>> stateToUsage = new HashMap<>();
        Deque<AbstractState> stateWaitlist = new ArrayDeque<>();
        stateWaitlist.add(currentPair.getFirst());

        // Waitlist to be sure in order (not start from the middle point)
        while (!stateWaitlist.isEmpty()) {
          ARGState argState = (ARGState) stateWaitlist.poll();
          if (argState.isCovered()) {
            // Covered states has no children, we can not determine usages
            continue;
          }
          List<UsageInfo> expandedUsages =
              expandUsagesAndAdd(argState, stateToUsage, currentEffects);

          if (needToDumpUsages(argState)) {
            addingToContainerTimer.start();
            expandedUsages.forEach(container::add);
            addingToContainerTimer.stop();
          } else {
            stateToUsage.put(argState, expandedUsages);
          }
          stateWaitlist.addAll(argState.getSuccessors());
          if (argState.isCovered()) {
            stateWaitlist.add(argState.getCoveringState());
          }

          // Search state in the BAM cache
          if (manager != null && manager.hasInitialState(argState)) {
            for (ARGState child : argState.getChildren()) {
              AbstractState reducedChild = manager.getReducedStateForExpandedState(child);
              ReachedSet innerReached =
                  manager.getReachedSetForInitialState(argState, reducedChild);

              processReachedSet(argState, innerReached, waitlist, processedSets, currentEffects);
            }
          } else if (manager != null && manager.hasInitialStateWithoutExit(argState)) {
            ReachedSet innerReached = manager.getReachedSetForInitialState(argState);

            processReachedSet(argState, innerReached, waitlist, processedSets, currentEffects);
          }
        }
      }
      logger.log(Level.INFO, "Usage extraction is finished");
      totalTimer.stop();
    }
    stableUnsafes = container.calculateStableUnsafes();
  }

  private boolean needToDumpUsages(AbstractState pState) {
    PredicateAbstractState predicateState =
        AbstractStates.extractStateByType(pState, PredicateAbstractState.class);

    return predicateState == null
        || (predicateState.isAbstractionState()
            && !predicateState.getAbstractionFormula().isFalse());
  }

  private List<UsageInfo>
      expandUsagesAndAdd(
          ARGState state,
          Map<AbstractState, List<UsageInfo>> stateToUsage,
          Set<LockEffect> currentEffects) {

    List<UsageInfo> expandedUsages = new ArrayList<>();

    for (ARGState covered : state.getCoveredByThis()) {
      expandedUsages.addAll(stateToUsage.getOrDefault(covered, ImmutableList.of()));
    }
    for (ARGState parent : state.getParents()) {
      expandedUsages.addAll(stateToUsage.getOrDefault(parent, ImmutableList.of()));
    }

    LockState locks, expandedLocks;
    List<UsageInfo> usages = usageProcessor.getUsagesForState(state);

    usageExpandingTimer.start();
    for (UsageInfo uinfo : usages) {
      UsageInfo expandedUsage = null;
      if (currentEffects.isEmpty()) {
        expandedUsage = uinfo;
      } else {
        locks = (LockState) uinfo.getLockState();
        LockStateBuilder builder = locks.builder();
        currentEffects.forEach(e -> e.effect(builder));
        expandedLocks = builder.build();
        if (expandedLocks != null) {
          // means we have impossible state, do not add the usage.
          expandedUsage = uinfo.expand(expandedLocks);
        }
      }
      if (expandedUsage != null) {
        expandedUsages.add(expandedUsage);
      }
    }
    usageExpandingTimer.stop();

    return expandedUsages;
  }

  private void processReachedSet(
      AbstractState rootState,
      ReachedSet innerReached,
      Deque<Pair<AbstractState, Set<LockEffect>>> waitlist,
      Multimap<AbstractState, Set<LockEffect>> processedSets,
      Set<LockEffect> currentEffects) {

    AbstractState reducedState = innerReached.getFirstState();
    LockState rootLockState = AbstractStates.extractStateByType(rootState, LockState.class);
    LockState reducedLockState = AbstractStates.extractStateByType(reducedState, LockState.class);
    Set<LockEffect> difference;
    if (rootLockState == null || reducedLockState == null) {
      // No LockCPA
      difference = new HashSet<>();
    } else {
      // the same element, so do not distinguish lock[1] and lock [2]
      difference = new HashSet<>(reducedLockState.getDifference(rootLockState).elementSet());
    }

    difference.addAll(currentEffects);

    AbstractState firstState = innerReached.getFirstState();
    Pair<AbstractState, Set<LockEffect>> newPair =
        Pair.of(innerReached.getFirstState(), difference);

    if (shouldContinue(processedSets.get(firstState), difference)) {
      waitlist.add(newPair);
      if (difference.isEmpty() && processedSets.containsKey(firstState)) {
        processedSets.removeAll(firstState);
      }
      processedSets.put(firstState, difference);
    }
  }

  private boolean
      shouldContinue(
          Collection<Set<LockEffect>> processed,
          Set<LockEffect> currentDifference) {
    if (processCoveredUsages) {
      return !processed.contains(currentDifference);
    } else {
      for (Set<LockEffect> locks : processed) {
        if (currentDifference.containsAll(locks)) {
          return false;
        }
      }
      return true;
    }
  }

  public void printStatistics(StatisticsWriter pWriter) {
    StatisticsWriter writer =
        pWriter.spacer()
        .put(totalTimer)
            .beginLevel();
    usageProcessor.printStatistics(writer);
    writer.put(addingToContainerTimer)
        .put(usageExpandingTimer)
        .endLevel()
        .put(processingSteps);
  }
}
