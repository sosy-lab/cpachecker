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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.concurrency.Threads;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import com.google.common.base.Function;


public class AutoAdjustingInvariantGenerator<T extends InvariantGenerator> implements InvariantGenerator, StatisticsProvider {

  private final ShutdownNotifier shutdownNotifier;

  private final AtomicBoolean cancelled = new AtomicBoolean();

  private final AdjustableInvariantGenerator<T> invariantGenerator;

  public AutoAdjustingInvariantGenerator(ShutdownNotifier pShutdownNotifier, AdjustableInvariantGenerator<T> pInitialGenerator) {
    shutdownNotifier = pShutdownNotifier;
    invariantGenerator = pInitialGenerator;
  }

  public AutoAdjustingInvariantGenerator(ShutdownNotifier pShutdownNotifier, T pInitialGenerator, Function<? super T, ? extends T> pAdjust) {
    shutdownNotifier = pShutdownNotifier;
    invariantGenerator = new AdjustableInvariantGenerator<>(pShutdownNotifier, pInitialGenerator, pAdjust);
  }

  @Override
  public void start(final CFANode pInitialLocation) {
    invariantGenerator.start(pInitialLocation);
    ExecutorService executor = Executors.newSingleThreadExecutor(Threads.threadFactory());
    executor.submit(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        while (!cancelled.get() && !invariantGenerator.isProgramSafe() && invariantGenerator.adjustAndContinue(pInitialLocation)) {
          if (shutdownNotifier.shouldShutdown()) {
            cancel();
          }
        }
        return null;
      }

    });
    executor.shutdown();
  }

  @Override
  public void cancel() {
    cancelled.set(true);
    invariantGenerator.cancel();
  }

  @Override
  public InvariantSupplier get() throws CPAException, InterruptedException {
    return invariantGenerator.get();
  }

  @Override
  public boolean isProgramSafe() {
    return invariantGenerator.isProgramSafe();
  }

  @Override
  public void injectInvariant(CFANode pLocation, AssumeEdge pAssumption) throws UnrecognizedCodeException {
    invariantGenerator.injectInvariant(pLocation, pAssumption);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    invariantGenerator.collectStatistics(pStatsCollection);
  }

}
