// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.usage.UsageCPA;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageProcessor;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

public class ConcurrentUsageExtractor {

  private final LogManager logger;
  private final UsageContainer container;
  private BAMDataManager manager;
  private UsageProcessor usageProcessor;
  private final boolean processCoveredUsages;
  private final boolean useConcurrentExtraction;

  private final StatTimer totalTimer = new StatTimer("Time for extracting usages");
  private final AtomicInteger processingSteps;
  private final AtomicInteger numberOfActiveTasks;
  private final ShutdownNotifier notifier;

  private final ThreadSafeTimerContainer usageExpandingTimer =
      new ThreadSafeTimerContainer("Time for usage expanding");

  private final ThreadSafeTimerContainer usageProcessingTimer =
      new ThreadSafeTimerContainer("Time for usage calculation");

  private final ThreadSafeTimerContainer addingToContainerTimer =
      new ThreadSafeTimerContainer("Time for adding to container");


  public ConcurrentUsageExtractor(
      ConfigurableProgramAnalysis pCpa,
      LogManager pLogger,
      UsageContainer pContainer,
      UsageConfiguration pConfig) {
    logger = pLogger;
    container = pContainer;
    processCoveredUsages = pConfig.getProcessCoveredUsages();
    useConcurrentExtraction = pConfig.useConcurrentExtraction();

    BAMCPA bamCpa = CPAs.retrieveCPA(pCpa, BAMCPA.class);
    if (bamCpa != null) {
      manager = bamCpa.getData();
    }
    UsageCPA usageCpa = CPAs.retrieveCPA(pCpa, UsageCPA.class);
    usageProcessor = usageCpa.getUsageProcessor();
    processingSteps = new AtomicInteger();
    numberOfActiveTasks = new AtomicInteger();
    notifier = usageCpa.getNotifier();
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  public void extractUsages(AbstractState firstState) {
    totalTimer.start();
    logger.log(Level.INFO, "Analysis is finished, start usage extraction");
    Multimap<AbstractState, UsageDelta> processedSets = ArrayListMultimap.create();

    UsageDelta emptyDelta = UsageDelta.constructDeltaBetween(firstState, firstState);
    processedSets.put(firstState, emptyDelta);
    usageProcessor.updateRedundantUnsafes(container.getNotInterestingUnsafes());

    numberOfActiveTasks.incrementAndGet();
    int threads = useConcurrentExtraction ? Runtime.getRuntime().availableProcessors() : 1;
    ExecutorService service = Executors.newFixedThreadPool(threads);
    service.submit(
        new ReachedSetExecutor(firstState, emptyDelta, processedSets, service, ImmutableList.of()));

    try {
      while (numberOfActiveTasks.get() != processingSteps.get()) {
        synchronized (service) {
          service.wait(1000);
        }
        if (notifier.shouldShutdown()) {
          service.shutdown();
          notifier.shutdownIfNecessary();
        }
      }
      logger.log(Level.INFO, "Usage extraction is finished");
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, "Usage extraction is interrupted");
    }
    service.shutdownNow();
    totalTimer.stop();
  }

  public void printStatistics(StatisticsWriter pWriter) {
    StatisticsWriter writer =
        pWriter.spacer().put(totalTimer).beginLevel().put(usageProcessingTimer).beginLevel();
    usageProcessor.printStatistics(writer);
    writer.endLevel()
        .put(addingToContainerTimer)
        .put(usageExpandingTimer)
        .endLevel()
        .put("Number of different reached sets with lock effects", processingSteps);
  }

  public BAMDataManager getManager() {
    return manager;
  }

  private class ReachedSetExecutor implements Runnable {

    private final UsageDelta currentDelta;
    private final AbstractState firstState;
    private final Multimap<AbstractState, UsageDelta> processedSets;
    private final ExecutorService service;
    private final Map<AbstractState, List<UsageInfo>> stateToUsage;
    private final List<AbstractState> expandedStack;

    private final TimerWrapper expandingTimer;
    private final TimerWrapper processingTimer;
    private final TimerWrapper addingTimer;

    ReachedSetExecutor(
        AbstractState pState,
        UsageDelta pDelta,
        Multimap<AbstractState, UsageDelta> pSets,
        ExecutorService pService,
        List<AbstractState> pExpandedStack) {

      currentDelta = pDelta;
      firstState = pState;
      processedSets = pSets;
      service = pService;
      expandingTimer = usageExpandingTimer.getNewTimer();
      processingTimer = usageProcessingTimer.getNewTimer();
      addingTimer = addingToContainerTimer.getNewTimer();
      stateToUsage = new HashMap<>();
      expandedStack = pExpandedStack;
    }

    @Override
    public void run() {
      // processingSteps.inc();
      Deque<AbstractState> stateWaitlist = new ArrayDeque<>();
      stateWaitlist.add(firstState);

      // Waitlist to be sure in order (not start from the middle point)
      try {
        while (!stateWaitlist.isEmpty()) {
          ARGState argState = (ARGState) stateWaitlist.poll();
          if (stateToUsage.containsKey(argState)) {
            continue;
          }

          List<UsageInfo> expandedUsages = expandUsagesAndAdd(argState);

          if (needToDumpUsages(argState)) {
            addingTimer.start();
            for (UsageInfo usage : expandedUsages) {
              container.add(usage);
            }
            addingTimer.stop();
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

              processReachedSet(argState, innerReached);
              notifier.shutdownIfNecessary();
            }
          } else if (manager != null && manager.hasInitialStateWithoutExit(argState)) {
            // Likely thread functions
            ReachedSet innerReached = manager.getReachedSetForInitialState(argState);

            processReachedSet(argState, innerReached);
            notifier.shutdownIfNecessary();
          }
        }
      } catch (InterruptedException e) {
        // Likely, timeout, nothing to do, finish here
      } catch (Throwable e) {
        // catch to be sure, that we increment the counter
        logger.logException(Level.WARNING, e, "Exception: ");
      }

      if (processingSteps.incrementAndGet() == numberOfActiveTasks.get()) {
        service.notify();
      }
    }

    private boolean needToDumpUsages(AbstractState pState) {
      PredicateAbstractState predicateState =
          AbstractStates.extractStateByType(pState, PredicateAbstractState.class);

      return predicateState == null
          || (predicateState.isAbstractionState()
              && !predicateState.getAbstractionFormula().isFalse());
    }

    private List<UsageInfo> expandUsagesAndAdd(ARGState state) {

      List<UsageInfo> expandedUsages = new ArrayList<>();

      for (ARGState covered : state.getCoveredByThis()) {
        expandedUsages.addAll(stateToUsage.getOrDefault(covered, ImmutableList.of()));
      }
      for (ARGState parent : state.getParents()) {
        expandedUsages.addAll(stateToUsage.getOrDefault(parent, ImmutableList.of()));
      }

      processingTimer.start();
      List<UsageInfo> usages = usageProcessor.getUsagesForState(state);
      processingTimer.stop();

      expandingTimer.start();
      for (UsageInfo usage : usages) {
        UsageInfo expanded = usage.expand(currentDelta, expandedStack);
        if (expanded.isRelevant()) {
          expandedUsages.add(expanded);
        }
      }
      expandingTimer.stop();

      return expandedUsages;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private void processReachedSet(
        AbstractState rootState,
        ReachedSet innerReached) {

      AbstractState reducedState = innerReached.getFirstState();

      UsageDelta newDiff = UsageDelta.constructDeltaBetween(reducedState, rootState);
      UsageDelta difference = currentDelta.add(newDiff);

      synchronized (service) {
        if (shouldContinue(processedSets.get(reducedState), difference)) {
          numberOfActiveTasks.incrementAndGet();
          processedSets.put(reducedState, difference);
          List<AbstractState> newStack = new ArrayList<>(expandedStack);
          newStack.add(rootState);
          service.submit(
              new ReachedSetExecutor(reducedState, difference, processedSets, service, newStack));
        }
      }
    }

    private boolean shouldContinue(Collection<UsageDelta> processed, UsageDelta currentDifference) {
      if (processCoveredUsages) {
        return !processed.contains(currentDifference);
      } else {
        for (UsageDelta delta : processed) {
          if (delta.covers(currentDifference)) {
            return false;
          }
        }
        return true;
      }
    }
  }
}
