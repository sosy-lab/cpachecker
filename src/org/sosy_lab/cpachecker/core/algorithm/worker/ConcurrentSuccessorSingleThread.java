/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.worker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.sosy_lab.common.Concurrency;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.algorithm.CPAStatistics;
import org.sosy_lab.cpachecker.core.algorithm.Worker;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment.Action;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ConcurrentSuccessorSingleThread implements Worker {

  private class SuccessorThread implements Callable<Object> {

    private final ConfigurableProgramAnalysis cpa;
    private final AbstractElement element;
    private final Precision precision;
    private final MergeOperator mergeOperator;
    private final StopOperator stopOperator;

    private AbstractElement successor;

    public SuccessorThread(AbstractElement successor, AbstractElement element, Precision precision,
        MergeOperator mergeOperator, StopOperator stopOperator, ConfigurableProgramAnalysis cpa) {
      this.successor = successor;
      this.element = element;
      this.precision = precision;
      this.mergeOperator = mergeOperator;
      this.stopOperator = stopOperator;
      this.cpa = cpa;
    }

    @Override
    public Object call() throws  CPAException, InterruptedException {
      final PrecisionAdjustment precisionAdjustment =
          cpa.getPrecisionAdjustment();

      logger.log(Level.FINER, "Considering successor of current element");
      logger.log(Level.ALL, "Successor of", element, "\nis", successor);

      stats.precisionTimer.start();
      Triple<AbstractElement, Precision, Action> precAdjustmentResult =
          precisionAdjustment.prec(successor, precision, reachedSet);
      stats.precisionTimer.stop();

      successor = precAdjustmentResult.getFirst();
      Precision successorPrecision = precAdjustmentResult.getSecond();
      Action action = precAdjustmentResult.getThird();

      if (action == Action.BREAK) {
        stats.countBreak++;
        // re-add the old element to the waitlist, there may be unhandled
        // successors left that otherwise would be forgotten
        reachedSet.reAddToWaitlist(element);
        reachedSet.add(successor, successorPrecision);

        stats.totalTimer.stop();
        //TODO used to return true. how could one restore the behaviour?
        return null;
      }
      assert action == Action.CONTINUE : "Enum Action has unhandled values!";

      Collection<AbstractElement> reached = reachedSet.getReached(successor);

      // An optimization, we don't bother merging if we know that the
      // merge operator won't do anything (i.e., it is merge-sep).
      if (mergeOperator != MergeSepOperator.getInstance() && !reached.isEmpty()) {
        stats.mergeTimer.start();

        List<AbstractElement> toRemove = new ArrayList<AbstractElement>();
        List<Pair<AbstractElement, Precision>> toAdd =
            new ArrayList<Pair<AbstractElement, Precision>>();

        logger.log(Level.FINER, "Considering", reached.size(),
            "elements from reached set for merge");
        for (AbstractElement reachedElement : reached) {
          AbstractElement mergedElement =
              mergeOperator.merge(successor, reachedElement,
                  successorPrecision);

          if (!mergedElement.equals(reachedElement)) {
            logger.log(Level.FINER,
                "Successor was merged with element from reached set");
            logger.log(Level.ALL, "Merged", successor, "\nand",
                reachedElement, "\n-->", mergedElement);
            stats.countMerge++;

            toRemove.add(reachedElement);
            toAdd.add(Pair.of(mergedElement, successorPrecision));
          }
        }
        reachedSet.removeAll(toRemove);
        reachedSet.addAll(toAdd);

        stats.mergeTimer.stop();
      }

      stats.stopTimer.start();
      boolean stop =
          stopOperator.stop(successor, reached, successorPrecision);
      stats.stopTimer.stop();

      if (stop) {
        logger.log(Level.FINER,
            "Successor is covered or unreachable, not adding to waitlist");
        stats.countStop++;

      } else {
        logger.log(Level.FINER,
            "No need to stop, adding successor to waitlist");

        reachedSet.add(successor, successorPrecision);
      }
      return null;
    }

  }

  private final ConfigurableProgramAnalysis cpa;
  private final CPAStatistics stats;
  private final LogManager logger;

  private ReachedSet reachedSet;

  public ConcurrentSuccessorSingleThread(ReachedSet reachedSet, ConfigurableProgramAnalysis 
cpa, LogManager logger,
      CPAStatistics stats) {
    this.cpa = cpa;
    this.stats = stats;
    this.logger = logger;
    this.reachedSet = reachedSet;
  }

  @Override
  public void work() throws CPAException, InterruptedException {
    final TransferRelation transferRelation = cpa.getTransferRelation();
    final MergeOperator mergeOperator = cpa.getMergeOperator();
    final StopOperator stopOperator = cpa.getStopOperator();

    ExecutorService threadPool = Executors.newSingleThreadExecutor();

    while (reachedSet.hasWaitingElement()) {

      CPAchecker.stopIfNecessary();

      stats.countIterations++;

      // Pick next element using strategy
      // BFS, DFS or top sort according to the configuration
      int size = reachedSet.getWaitlistSize();
      if (size >= stats.maxWaitlistSize) {
        stats.maxWaitlistSize = size;
      }
      stats.countWaitlistSize += size;

      stats.chooseTimer.start();
      final AbstractElement element = reachedSet.popFromWaitlist();
      final Precision precision = reachedSet.getPrecision(element);
      stats.chooseTimer.stop();

      logger.log(Level.FINER, "Retrieved element from waitlist");
      logger.log(Level.ALL, "Current element is", element, "with precision",
          precision);

      stats.transferTimer.start();
      Collection<? extends AbstractElement> successors =
          transferRelation.getAbstractSuccessors(element, precision, null);
      stats.transferTimer.stop();
      // TODO When we have a nice way to mark the analysis result as incomplete,
      // we could continue analysis on a CPATransferException with the next element from waitlist.

      int numSuccessors = successors.size();
      logger.log(Level.FINER, "Current element has", numSuccessors,
          "successors");
      stats.countSuccessors += numSuccessors;
      stats.maxSuccessors = Math.max(numSuccessors, stats.maxSuccessors);

//	ArrayList<Future<Object>> jobs = new ArrayList<Future<Object>>();
      for (AbstractElement successor : successors) {
//	jobs.add(
	Future<Object> f = threadPool.submit(new SuccessorThread(successor, element, precision, mergeOperator, stopOperator, cpa));
	                try {
                        f.get();
                }
                catch(ExecutionException e) {
                        Throwable th = e.getCause();
                        if(th == null) {

                        }
                        else if(th instanceof CPAException) {
                                throw (CPAException) th;
                        }
                        else { // (th instanceof InterruptedException)
                                throw (InterruptedException) th;
                        }
                }
                catch(InterruptedException e) {
                        throw e;
                }
      }

//	for(Future<Object> f : jobs) {
//	}
    }
	threadPool.shutdown();
	while(!threadPool.awaitTermination(1,TimeUnit.MILLISECONDS)) {
	}
  }

}
