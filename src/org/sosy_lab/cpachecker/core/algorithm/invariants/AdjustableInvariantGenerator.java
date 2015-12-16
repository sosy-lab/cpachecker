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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.LazyFutureTask;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;


public class AdjustableInvariantGenerator<T extends InvariantGenerator> implements InvariantGenerator, StatisticsProvider {

  private final ShutdownNotifier shutdownNotifier;

  private final Function<? super T, ? extends T> adjust;

  private final AtomicBoolean isProgramSafe = new AtomicBoolean();

  private final AtomicReference<T> invariantGenerator;

  private final AtomicReference<Future<InvariantSupplier>> currentInvariantSupplier = new AtomicReference<>();

  public AdjustableInvariantGenerator(ShutdownNotifier pShutdownNotifier, T pInitialGenerator, Function<? super T, ? extends T> pAdjust) {
    shutdownNotifier = pShutdownNotifier;
    invariantGenerator = new AtomicReference<>(pInitialGenerator);
    adjust = pAdjust;
  }

  @Override
  public void start(CFANode pInitialLocation) {
    invariantGenerator.get().start(pInitialLocation);
    setSupplier(invariantGenerator.get());
  }

  public boolean adjustAndContinue(CFANode pInitialLocation) throws CPAException, InterruptedException {
    final T current = invariantGenerator.get();
    try {
      setSupplier(current.get());
    } finally {
      if (current.isProgramSafe()) {
        isProgramSafe.set(true);
      }
    }
    final T next = adjust.apply(current);
    boolean adjustable = next != current && next != null;
    if (adjustable) {
      next.start(pInitialLocation);
      invariantGenerator.set(next);
    }
    return adjustable;
  }

  @Override
  public void cancel() {
    invariantGenerator.get().cancel();
  }

  @Override
  public InvariantSupplier get() throws CPAException, InterruptedException {
    Future<InvariantSupplier> supplier = currentInvariantSupplier.get();
    Preconditions.checkState(supplier != null);
    try {
      return supplier.get();
    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e.getCause(), CPAException.class, InterruptedException.class);
      throw new UnexpectedCheckedException("invariant generation", e.getCause());
    } catch (CancellationException e) {
      shutdownNotifier.shutdownIfNecessary();
      throw e;
    }
  }

  @Override
  public boolean isProgramSafe() {
    return isProgramSafe.get() || invariantGenerator.get().isProgramSafe();
  }

  @Override
  public void injectInvariant(CFANode pLocation, AssumeEdge pAssumption) throws UnrecognizedCodeException {
    invariantGenerator.get().injectInvariant(pLocation, pAssumption);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    InvariantGenerator invariantGenerator = this.invariantGenerator.get();
    if (invariantGenerator instanceof StatisticsProvider) {
      ((StatisticsProvider) invariantGenerator).collectStatistics(pStatsCollection);
    }
  }

  private void setSupplier(final InvariantGenerator pInvariantGenerator) {
    currentInvariantSupplier.set(new LazyFutureTask<>(new Callable<InvariantSupplier>() {

      @Override
      public InvariantSupplier call() throws Exception {
        return pInvariantGenerator.get();
      }

    }));
  }

  private void setSupplier(final InvariantSupplier pSupplier) {
    currentInvariantSupplier.set(new LazyFutureTask<>(new Callable<InvariantSupplier>() {

      @Override
      public InvariantSupplier call() throws Exception {
        return pSupplier;
      }

    }));
  }

}
