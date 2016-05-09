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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.LazyFutureTask;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class AdjustableInvariantGenerator<T extends InvariantGenerator> extends AbstractInvariantGenerator implements StatisticsProvider {

  private final ShutdownNotifier shutdownNotifier;

  private final Function<? super T, ? extends T> adjust;

  private final AtomicBoolean isProgramSafe = new AtomicBoolean();

  private final AtomicReference<T> invariantGenerator;

  private final AtomicReference<T> previousInvariantGenerator = new AtomicReference<>();

  private final AtomicReference<Future<FormulaAndTreeSupplier>> currentInvariantSupplier = new AtomicReference<>();

  private final T initialGenerator;

  private final AtomicBoolean started = new AtomicBoolean();

  public AdjustableInvariantGenerator(ShutdownNotifier pShutdownNotifier, T pInitialGenerator, Function<? super T, ? extends T> pAdjust) {
    shutdownNotifier = pShutdownNotifier;
    invariantGenerator = new AtomicReference<>();
    initialGenerator = pInitialGenerator;
    adjust = pAdjust;
  }

  @Override
  public void start(CFANode pInitialLocation) {
    initialGenerator.start(pInitialLocation);
    started.set(true);
    setSupplier(
        new FormulaAndTreeSupplier(
            InvariantSupplier.TrivialInvariantSupplier.INSTANCE,
            ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE));
  }

  public boolean adjustAndContinue(CFANode pInitialLocation) throws CPAException, InterruptedException {
    final T current = invariantGenerator.get();
    final T next;
    if (current == null) {
      next = initialGenerator;
      setSupplier(
          new FormulaAndTreeSupplier(
              InvariantSupplier.TrivialInvariantSupplier.INSTANCE,
              ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE));
    } else {
      try {
        setSupplier(new FormulaAndTreeSupplier(current.get(), current.getAsExpressionTree()));
      } finally {
        if (current.isProgramSafe()) {
          isProgramSafe.set(true);
        }
      }
      next = adjust.apply(current);
    }
    boolean adjustable = next != current && next != null;
    if (adjustable) {
      if (current != null) {
        next.start(pInitialLocation);
      }
      previousInvariantGenerator.set(invariantGenerator.getAndSet(next));
    }
    return adjustable;
  }

  @Override
  public void cancel() {
    T current = invariantGenerator.get();
    if (current == null) {
      if (started.get()) {
        initialGenerator.cancel();
      }
    } else {
      current.cancel();
    }
  }

  private FormulaAndTreeSupplier getInternal() throws CPAException, InterruptedException {
    Future<FormulaAndTreeSupplier> supplier = currentInvariantSupplier.get();
    Preconditions.checkState(supplier != null);
    try {
      return supplier.get();
    } catch (ExecutionException e) {
      if (e.getCause() instanceof InterruptedException) {
        return new FormulaAndTreeSupplier(
            invariantGenerator.get().get(), invariantGenerator.get().getAsExpressionTree());
      }
      Throwables.propagateIfPossible(e.getCause(), CPAException.class);
      throw new UnexpectedCheckedException("invariant generation", e.getCause());
    } catch (CancellationException e) {
      shutdownNotifier.shutdownIfNecessary();
      throw e;
    }
  }

  @Override
  public InvariantSupplier get() throws CPAException, InterruptedException {
    return getInternal();
  }

  @Override
  public ExpressionTreeSupplier getAsExpressionTree() throws CPAException, InterruptedException {
    return getInternal();
  }

  @Override
  public boolean isProgramSafe() {
    if (isProgramSafe.get()) {
      return true;
    }
    T current = invariantGenerator.get();
    if (current == null) {
      return false;
    }
    return current.isProgramSafe();
  }

  @Override
  public void injectInvariant(CFANode pLocation, AssumeEdge pAssumption) throws UnrecognizedCodeException {
    invariantGenerator.get().injectInvariant(pLocation, pAssumption);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    InvariantGenerator invariantGenerator = this.invariantGenerator.get();
    if (invariantGenerator == null) {
      invariantGenerator = previousInvariantGenerator.get();
    }
    if (invariantGenerator == null) {
      invariantGenerator = initialGenerator;
    }
    if (invariantGenerator instanceof StatisticsProvider) {
      ((StatisticsProvider) invariantGenerator).collectStatistics(pStatsCollection);
    }
  }

  private void setSupplier(final FormulaAndTreeSupplier pSupplier) {
    currentInvariantSupplier.set(new LazyFutureTask<>(new Callable<FormulaAndTreeSupplier>() {

      @Override
      public FormulaAndTreeSupplier call() throws Exception {
        return pSupplier;
      }

    }));
  }

}
