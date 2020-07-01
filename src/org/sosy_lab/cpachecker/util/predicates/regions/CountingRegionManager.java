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
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.PredicateOrderingStrategy;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class CountingRegionManager implements RegionManager {

  private final RegionManager delegate;

  private final StatInt trueCtr = new StatInt(StatKind.COUNT, "Number of accesses to true");
  private final StatInt falseCtr = new StatInt(StatKind.COUNT, "Number of accesses to false");
  private final StatInt operationsCtr = new StatInt(StatKind.COUNT, "Number of operations");
  private final StatInt parserCtr = new StatInt(StatKind.COUNT, "Number of parsings");
  private final StatInt reorderCtr = new StatInt(StatKind.COUNT, "Number of reorderings");

  public CountingRegionManager(RegionManager pDelegate) {
    delegate = pDelegate;
  }

  @Override
  public RegionBuilder builder(ShutdownNotifier pShutdownNotifier) {
    return delegate.builder(pShutdownNotifier);
  }

  @Override
  public Region makeTrue() {
    trueCtr.setNextValue(1);
    return delegate.makeTrue();
  }

  @Override
  public Region makeFalse() {
    falseCtr.setNextValue(1);
    return delegate.makeFalse();
  }

  @Override
  public Region makeNot(Region pF) {
    operationsCtr.setNextValue(1);
    return delegate.makeNot(pF);
  }

  @Override
  public Region makeAnd(Region pF1, Region pF2) {
    operationsCtr.setNextValue(1);
    return delegate.makeAnd(pF1, pF2);
  }

  @Override
  public Region makeOr(Region pF1, Region pF2) {
    operationsCtr.setNextValue(1);
    return delegate.makeOr(pF1, pF2);
  }

  @Override
  public Region makeEqual(Region pF1, Region pF2) {
    operationsCtr.setNextValue(1);
    return delegate.makeEqual(pF1, pF2);
  }

  @Override
  public Region makeUnequal(Region pF1, Region pF2) {
    operationsCtr.setNextValue(1);
    return delegate.makeUnequal(pF1, pF2);
  }

  @Override
  public Region makeIte(Region pF1, Region pF2, Region pF3) {
    operationsCtr.setNextValue(1);
    return delegate.makeIte(pF1, pF2, pF3);
  }

  @Override
  public Region makeExists(Region pF1, Region... pF2) {
    operationsCtr.setNextValue(1);
    return delegate.makeExists(pF1, pF2);
  }

  @Override
  public boolean entails(Region pF1, Region pF2) throws SolverException, InterruptedException {
    operationsCtr.setNextValue(1);
    return delegate.entails(pF1, pF2);
  }

  @Override
  public Region createPredicate() {
    operationsCtr.setNextValue(1);
    return delegate.createPredicate();
  }

  @Override
  public Region fromFormula(
      BooleanFormula pF, FormulaManagerView pFmgr, Function<BooleanFormula, Region> pAtomToRegion) {
    parserCtr.setNextValue(1);
    return delegate.fromFormula(pF, pFmgr, pAtomToRegion);
  }

  @Override
  public Triple<Region, Region, Region> getIfThenElse(Region pF) {
    operationsCtr.setNextValue(1);
    return delegate.getIfThenElse(pF);
  }

  @Override
  public void printStatistics(PrintStream pOut) {
    writingStatisticsTo(pOut)
        .put(trueCtr)
        .put(falseCtr)
        .put(operationsCtr)
        .put(parserCtr)
        .put(reorderCtr);
  }

  @Override
  public String getVersion() {
    return delegate.getVersion();
  }

  @Override
  public void setVarOrder(ImmutableIntArray pOrder) {
    reorderCtr.setNextValue(1);
    delegate.setVarOrder(pOrder);
  }

  @Override
  public void reorder(PredicateOrderingStrategy pStrategy) {
    reorderCtr.setNextValue(1);
    delegate.reorder(pStrategy);
  }

  @Override
  public Region replace(Region pRegion, Region[] pOldPredicates, Region[] pNewPredicates) {
    operationsCtr.setNextValue(1);
    return delegate.replace(pRegion, pOldPredicates, pNewPredicates);
  }
}
