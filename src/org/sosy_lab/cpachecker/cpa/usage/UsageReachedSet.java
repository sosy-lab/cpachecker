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
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageConfiguration;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageContainer;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageDelta;
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
      Deque<Pair<AbstractState, UsageDelta>> waitlist = new ArrayDeque<>();
      Multimap<AbstractState, UsageDelta> processedSets = ArrayListMultimap.create();

      UsageDelta emptyDelta = UsageDelta.constructDeltaBetween(getFirstState(), getFirstState());
      Pair<AbstractState, UsageDelta> currentPair = Pair.of(getFirstState(), emptyDelta);
      waitlist.add(currentPair);
      processedSets.put(getFirstState(), emptyDelta);
      usageProcessor.updateRedundantUnsafes(container.getNotInterestingUnsafes());

      while (!waitlist.isEmpty()) {
        currentPair = waitlist.pop();
        UsageDelta currentDelta = currentPair.getSecond();
        processingSteps.inc();
        Map<AbstractState, List<UsageInfo>> stateToUsage = new HashMap<>();
        Deque<AbstractState> stateWaitlist = new ArrayDeque<>();
        stateWaitlist.add(currentPair.getFirst());

        // Waitlist to be sure in order (not start from the middle point)
        while (!stateWaitlist.isEmpty()) {
          ARGState argState = (ARGState) stateWaitlist.poll();
          if (argState.isCovered()) {
            // Covered states has no children, we can not determine usages
            stateWaitlist.add(argState.getCoveringState());
            continue;
          }
          List<UsageInfo> expandedUsages =
              expandUsagesAndAdd(argState, stateToUsage, currentDelta);

          if (needToDumpUsages(argState)) {
            addingToContainerTimer.start();
            expandedUsages.forEach(container::add);
            addingToContainerTimer.stop();
          } else {
            stateToUsage.put(argState, expandedUsages);
          }
          stateWaitlist.addAll(argState.getSuccessors());

          // Search state in the BAM cache
          if (manager != null && manager.hasInitialState(argState)) {
            for (ARGState child : argState.getChildren()) {
              AbstractState reducedChild = manager.getReducedStateForExpandedState(child);
              ReachedSet innerReached =
                  manager.getReachedSetForInitialState(argState, reducedChild);

              processReachedSet(argState, innerReached, waitlist, processedSets, currentDelta);
            }
          } else if (manager != null && manager.hasInitialStateWithoutExit(argState)) {
            ReachedSet innerReached = manager.getReachedSetForInitialState(argState);

            processReachedSet(argState, innerReached, waitlist, processedSets, currentDelta);
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
          UsageDelta currentEffects) {

    List<UsageInfo> expandedUsages = new ArrayList<>();

    for (ARGState covered : state.getCoveredByThis()) {
      expandedUsages.addAll(stateToUsage.getOrDefault(covered, ImmutableList.of()));
    }
    for (ARGState parent : state.getParents()) {
      expandedUsages.addAll(stateToUsage.getOrDefault(parent, ImmutableList.of()));
    }

    List<UsageInfo> usages = usageProcessor.getUsagesForState(state);

    usageExpandingTimer.start();
    usages.stream()
        .map(u -> u.expand(currentEffects))
        .filter(UsageInfo::isRelevant)
        .forEach(expandedUsages::add);
    usageExpandingTimer.stop();

    return expandedUsages;
  }

  private void processReachedSet(
      AbstractState rootState,
      ReachedSet innerReached,
      Deque<Pair<AbstractState, UsageDelta>> waitlist,
      Multimap<AbstractState, UsageDelta> processedSets,
      UsageDelta currentEffects) {

    AbstractState reducedState = innerReached.getFirstState();

    UsageDelta newDiff = UsageDelta.constructDeltaBetween(reducedState, rootState);
    UsageDelta difference = currentEffects.add(newDiff);

    AbstractState firstState = innerReached.getFirstState();
    Pair<AbstractState, UsageDelta> newPair =
        Pair.of(innerReached.getFirstState(), difference);

    if (shouldContinue(processedSets.get(firstState), difference)) {
      waitlist.add(newPair);
      processedSets.put(firstState, difference);
    }
  }

  private boolean
      shouldContinue(
          Collection<UsageDelta> processed,
          UsageDelta currentDifference) {

    if (processCoveredUsages) {
      return !processed.contains(currentDifference);
    } else {
      return !processed.stream().anyMatch(d -> d.covers(currentDifference));
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
