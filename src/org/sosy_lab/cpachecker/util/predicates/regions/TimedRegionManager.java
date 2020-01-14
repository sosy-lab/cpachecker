/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.util.predicates.regions;

import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.common.primitives.ImmutableIntArray;
import java.io.PrintStream;
import java.util.function.Function;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.PredicateOrderingStrategy;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class TimedRegionManager implements RegionManager {

  private final RegionManager delegate;
  private final Timer timer = new Timer();
  private final Timer predicateTimer = new Timer();
  private final Timer orderingTimer = new Timer();

  public TimedRegionManager(RegionManager pDelegate) {
    delegate = pDelegate;
  }

  @Override
  public RegionBuilder builder(ShutdownNotifier pShutdownNotifier) {
    timer.start();
    try {
      return delegate.builder(pShutdownNotifier);
    } finally {
      timer.stop();
    }
  }

  @Override
  public Region makeTrue() {
    timer.start();
    try {
      return delegate.makeTrue();
    } finally {
      timer.stop();
    }
  }

  @Override
  public Region makeFalse() {
    timer.start();
    try {
      return delegate.makeFalse();
    } finally {
      timer.stop();
    }
  }

  @Override
  public Region makeNot(Region pF) {
    timer.start();
    try {
      return delegate.makeNot(pF);
    } finally {
      timer.stop();
    }
  }

  @Override
  public Region makeAnd(Region pF1, Region pF2) {
    timer.start();
    try {
      return delegate.makeAnd(pF1, pF2);
    } finally {
      timer.stop();
    }
  }

  @Override
  public Region makeOr(Region pF1, Region pF2) {
    timer.start();
    try {
      return delegate.makeOr(pF1, pF2);
    } finally {
      timer.stop();
    }
  }

  @Override
  public Region makeEqual(Region pF1, Region pF2) {
    timer.start();
    try {
      return delegate.makeEqual(pF1, pF2);
    } finally {
      timer.stop();
    }
  }

  @Override
  public Region makeUnequal(Region pF1, Region pF2) {
    timer.start();
    try {
      return delegate.makeUnequal(pF1, pF2);
    } finally {
      timer.stop();
    }
  }

  @Override
  public Region makeIte(Region pF1, Region pF2, Region pF3) {
    timer.start();
    try {
      return delegate.makeIte(pF1, pF2, pF3);
    } finally {
      timer.stop();
    }
  }

  @Override
  public Region makeExists(Region pF1, Region... pF2) {
    timer.start();
    try {
      return delegate.makeExists(pF1, pF2);
    } finally {
      timer.stop();
    }
  }

  @Override
  public boolean entails(Region pF1, Region pF2) throws SolverException, InterruptedException {
    timer.start();
    try {
      return delegate.entails(pF1, pF2);
    } finally {
      timer.stop();
    }
  }

  @Override
  public Region createPredicate() {
    predicateTimer.start();
    try {
      return delegate.createPredicate();
    } finally {
      predicateTimer.stop();
    }
  }

  @Override
  public Region fromFormula(
      BooleanFormula pF, FormulaManagerView pFmgr, Function<BooleanFormula, Region> pAtomToRegion) {
    timer.start();
    try {
      return delegate.fromFormula(pF, pFmgr, pAtomToRegion);
    } finally {
      timer.stop();
    }
  }

  @Override
  public Triple<Region, Region, Region> getIfThenElse(Region pF) {
    timer.start();
    try {
      return delegate.getIfThenElse(pF);
    } finally {
      timer.stop();
    }
  }

  @Override
  public void printStatistics(PrintStream pOut) {
    writingStatisticsTo(pOut)
        .put("Time for region operations", timer)
        .put("Max time for region operations", timer.getMaxTime())
        .put("Time for predicate creations", predicateTimer)
        .put("Time for reorderings", orderingTimer)
        .put("Number of region operations", timer.getNumberOfIntervals())
        .put("Number of predicate creations", predicateTimer.getNumberOfIntervals())
        .put("Number of reorderings", orderingTimer.getNumberOfIntervals());
  }

  @Override
  public String getVersion() {
    return delegate.getVersion();
  }

  @Override
  public void setVarOrder(ImmutableIntArray pOrder) {
    orderingTimer.start();
    try {
      delegate.setVarOrder(pOrder);
    } finally {
      orderingTimer.stop();
    }
  }

  @Override
  public void reorder(PredicateOrderingStrategy pStrategy) {
    orderingTimer.start();
    try {
      delegate.reorder(pStrategy);
    } finally {
      orderingTimer.stop();
    }
  }

  @Override
  public Region replace(Region pRegion, Region[] pOldPredicates, Region[] pNewPredicates) {
    timer.start();
    try {
      return delegate.replace(pRegion, pOldPredicates, pNewPredicates);
    } finally {
      timer.stop();
    }
  }
}
