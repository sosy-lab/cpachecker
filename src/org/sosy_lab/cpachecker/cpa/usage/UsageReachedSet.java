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
package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ObjectOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageContainer;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "usage")
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

  @Option(
    name = "processCoveredUsages",
    description = "Should we process the same blocks with covered sets of locks",
    secure = true)
  private boolean processCoveredUsages = true;

  private static final RaceProperty propertyInstance = new RaceProperty();

  private final LogManager logger;

  private UsageContainer container = null;

  public UsageReachedSet(
      WaitlistFactory waitlistFactory, Configuration pConfig, LogManager pLogger) {
    super(waitlistFactory);
    logger = pLogger;
    try {
      pConfig.inject(this);
      container = new UsageContainer(pConfig, logger);
    } catch (InvalidConfigurationException e) {
      logger.log(Level.WARNING, "Can not create container due to wrong config");
      container = null;
    }
  }

  @Override
  public void remove(AbstractState pState) {
    super.remove(pState);
    UsageState ustate = UsageState.get(pState);
    if (container != null) {
      container.removeState(ustate);
    }
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) {
    super.add(pState, pPrecision);

    /*UsageState USstate = UsageState.get(pState);
    USstate.saveUnsafesInContainerIfNecessary(pState);*/
  }

  @Override
  public void clear() {
    if (container != null) {
      container.resetUnrefinedUnsafes();
    }
    usagesExtracted = false;
    super.clear();
  }

  @Override
  public boolean hasViolatedProperties() {
    extractUsagesIfNeccessary();
    return container == null ? false : container.getTotalUnsafeSize() > 0;
  }

  @Override
  public Set<Property> getViolatedProperties() {
    if (hasViolatedProperties()) {
      return Collections.singleton(propertyInstance);
    } else {
      return Collections.emptySet();
    }
  }

  public UsageContainer getUsageContainer() {
    return container;
  }

  private void writeObject(@SuppressWarnings("unused") ObjectOutputStream stream) {
    throw new UnsupportedOperationException("cannot serialize Loger and Configuration.");
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
    if (!usagesExtracted && container != null) {
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
        AbstractState state = currentPair.getFirst();
        Set<LockEffect> currentEffects = currentPair.getSecond();
        processingSteps.inc();
        LockState locks, expandedLocks;
        Map<LockState, LockState> reduceToExpand = new HashMap<>();
        Map<AbstractState, List<UsageInfo>> stateToUsage = new HashMap<>();
        // Not states for optimizations
        Set<Integer> processedIds = new TreeSet<>();
        Deque<AbstractState> stateWaitlist = new ArrayDeque<>();
        stateWaitlist.add(state);
        processedIds.add(getId(state));

        while (!stateWaitlist.isEmpty()) {
          state = stateWaitlist.poll();
          List<UsageInfo> expandedUsages = new ArrayList<>();
          ARGState argState = (ARGState) state;
          if (argState.isCovered()) {
            // Covered states has no children, we can not determine usages
            continue;
          }
          for (ARGState covered : argState.getCoveredByThis()) {
            if (stateToUsage.containsKey(covered)) {
              expandedUsages.addAll(stateToUsage.get(covered));
            }
          }
          for (ARGState parent : argState.getParents()) {
            if (stateToUsage.containsKey(parent)) {
              expandedUsages.addAll(stateToUsage.get(parent));
            }
          }
          // handle state
          boolean alreadyProcessed = processedIds.contains(getId(state));
          if (!alreadyProcessed) {

            List<UsageInfo> usages = usageProcessor.getUsagesForState(state);

            usageExpandingTimer.start();
            for (UsageInfo uinfo : usages) {
              UsageInfo expandedUsage = null;
              if (currentEffects.isEmpty()) {
                expandedUsage = uinfo;
              } else {
                locks = (LockState) uinfo.getLockState();
                if (reduceToExpand.containsKey(locks)) {
                  expandedLocks = reduceToExpand.get(locks);
                } else {
                  LockStateBuilder builder = locks.builder();
                  currentEffects.forEach(e -> e.effect(builder));
                  expandedLocks = builder.build();
                  reduceToExpand.put(locks, expandedLocks);
                }
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
          }

          PredicateAbstractState predicateState =
              AbstractStates.extractStateByType(state, PredicateAbstractState.class);
          if (predicateState == null
              || (predicateState.isAbstractionState()
                  && !predicateState.getAbstractionFormula().isFalse())) {

            addingToContainerTimer.start();
            for (UsageInfo usage : expandedUsages) {
              SingleIdentifier id = usage.getId();
              container.add(id, usage);
            }
            addingToContainerTimer.stop();

            expandedUsages.clear();
            for (ARGState child : argState.getChildren()) {
              if (!processedIds.contains(getId(child))) {
                stateWaitlist.add(child);
              }
            }
          } else {
            stateWaitlist.addAll(argState.getChildren());
            if (argState.isCovered()) {
              stateWaitlist.add(argState.getCoveringState());
            }
            stateToUsage.put(state, expandedUsages);
          }
          if (!alreadyProcessed) {
            processedIds.add(getId(argState));
          }

          // Search state in the BAM cache
          if (manager != null && manager.hasInitialState(state)) {
            for (ARGState child : ((ARGState) state).getChildren()) {
              AbstractState reducedChild = manager.getReducedStateForExpandedState(child);
              ReachedSet innerReached = manager.getReachedSetForInitialState(state, reducedChild);

              process(state, innerReached, waitlist, processedSets, currentEffects);
            }
          } else if (manager != null && manager.hasInitialStateWithoutExit(state)) {
            ReachedSet innerReached = manager.getReachedSetForInitialState(state);

            process(state, innerReached, waitlist, processedSets, currentEffects);
          }
        }
      }
      logger.log(Level.INFO, "Usage extraction is finished");
      totalTimer.stop();
    }
  }

  private void process(
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

  private int getId(AbstractState e) {
    return ((ARGState) e).getStateId();
  }
}
