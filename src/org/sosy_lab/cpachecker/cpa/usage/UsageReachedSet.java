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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@SuppressFBWarnings(justification = "No support for serialization", value = "SE_BAD_FIELD")
public class UsageReachedSet extends PartitionedReachedSet {

  private static final long serialVersionUID = 1L;
  private BAMDataManager manager;
  private UsageProcessor usageProcessor;

  private final StatTimer usageProcessingTimer = new StatTimer("Time for usage processing");
  private final StatTimer usageExpandingTimer = new StatTimer("Time for usage expanding");

  private boolean usagesExtracted = false;

  public static class RaceProperty implements Property {
    @Override
    public String toString() {
      return "Race condition";
    }
  }

  private static final RaceProperty propertyInstance = new RaceProperty();

  private final Configuration config;
  private final LogManager logger;

  private UsageContainer container = null;

  public UsageReachedSet(
      WaitlistFactory waitlistFactory, Configuration pConfig, LogManager pLogger) {
    super(waitlistFactory);
    config = pConfig;
    logger = pLogger;
    try {
      container = new UsageContainer(config, logger);
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
    manager = bamCpa.getData();
    UsageCPA usageCpa = CPAs.retrieveCPA(bamCpa, UsageCPA.class);
    usageProcessor = usageCpa.getUsageProcessor();
  }

  private void extractUsagesIfNeccessary() {
    if (!usagesExtracted && container != null) {
      logger.log(Level.INFO, "Analysis is finished, start usage extraction");
      usagesExtracted = true;
      Deque<Pair<ReachedSet, Multiset<LockEffect>>> waitlist = new ArrayDeque<>();
      Set<Pair<AbstractState, Multiset<LockEffect>>> processedSets = new HashSet<>();

      waitlist.add(Pair.of(this, HashMultiset.create()));
      processedSets.add(Pair.of(getFirstState(), HashMultiset.create()));

      while (!waitlist.isEmpty()) {
        Pair<ReachedSet, Multiset<LockEffect>> currentPair = waitlist.pop();
        ReachedSet currentReached = currentPair.getFirst();
        Multiset<LockEffect> currentEffects = currentPair.getSecond();
        LockState locks, expandedLocks;
        Map<LockState, LockState> reduceToExpand = new HashMap<>();
        SortedSet<AbstractState> sortedSet = new TreeSet<>(currentReached.asCollection());
        Map<AbstractState, List<UsageInfo>> stateToUsage = new HashMap<>();

        for (AbstractState state : sortedSet) {
          // handle state
          usageProcessingTimer.start();
          List<UsageInfo> usages = usageProcessor.getUsagesForState(state);
          usageProcessingTimer.stop();
          List<UsageInfo> expandedUsages = new ArrayList<>();
          ARGState argState = (ARGState) state;
          Collection<ARGState> parents = argState.getParents();
          for (ARGState parent : parents) {
            if (stateToUsage.containsKey(parent)) {
              expandedUsages.addAll(stateToUsage.get(parent));
            }
          }

          usageExpandingTimer.start();
          for (UsageInfo uinfo : usages) {
            UsageInfo expandedUsage;
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
              expandedUsage = uinfo.expand(expandedLocks);
            }
            expandedUsages.add(expandedUsage);
          }
          usageExpandingTimer.stop();

          PredicateAbstractState predicateState =
              AbstractStates.extractStateByType(state, PredicateAbstractState.class);
          if (predicateState == null
              || (predicateState.isAbstractionState()
                  && !predicateState.getAbstractionFormula().isFalse())) {
            for (UsageInfo usage : expandedUsages) {
              SingleIdentifier id = usage.getId();
              container.add(id, usage);
            }
          } else {
            stateToUsage.put(state, expandedUsages);
          }

          // Search state in the BAM cache
          if (manager.hasInitialState(state)) {
            for (ARGState child : ((ARGState) state).getChildren()) {
              AbstractState reducedChild = manager.getReducedStateForExpandedState(child);
              ReachedSet innerReached = manager.getReachedSetForInitialState(state, reducedChild);

              LockState rootLockState = AbstractStates.extractStateByType(child, LockState.class);
              LockState reducedLockState =
                  AbstractStates.extractStateByType(reducedChild, LockState.class);
              Multiset<LockEffect> difference;
              if (rootLockState == null || reducedLockState == null) {
                // No LockCPA
                difference = HashMultiset.create();
              } else {
                difference = reducedLockState.getDifference(rootLockState);
              }

              difference.addAll(currentEffects);

              Pair<AbstractState, Multiset<LockEffect>> newPair =
                  Pair.of(innerReached.getFirstState(), difference);
              if (!processedSets.contains(newPair)) {
                waitlist.add(Pair.of(innerReached, difference));
                processedSets.add(newPair);
              }
            }
          }
        }
      }
    }
  }

  public void printStatistics(StatisticsWriter pWriter) {
    pWriter.spacer().put(usageProcessingTimer).put(usageExpandingTimer);
  }
}
