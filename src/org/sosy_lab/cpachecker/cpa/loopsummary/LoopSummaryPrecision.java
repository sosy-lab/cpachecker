// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyInterface;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;

class LoopSummaryPrecision implements AdjustablePrecision, WrapperPrecision {

  private final Precision precision;

  private Set<StrategyInterface> usedStrategies = new HashSet<>();

  private boolean loopHead = false;

  public LoopSummaryPrecision(Precision precision, Set<StrategyInterface> usedStrategies) {
    this.usedStrategies = usedStrategies;
    this.precision = precision;
  }

  public LoopSummaryPrecision(Precision precision) {
    this.precision = precision;
  }

  @Override
  public String toString() {
    String name = "Loopsummary precision { precission: "
        + precision.toString()
        + ", used strategies:  ";
    for (StrategyInterface s : usedStrategies) {
      name += s.getName() + " ,";
    }
    return name + " }";
  }

  public void setLoopHead(boolean isLoopHead) {
    this.loopHead = isLoopHead;
  }

  public boolean isLoopHead() {
    return this.loopHead;
  }

  public Precision getPrecision() {
    return precision;
  }

  public Set<StrategyInterface> getUsedStrategies() {
    return usedStrategies;
  }

  public void updateUsedStrategies(StrategyInterface pStrategy) {
    usedStrategies.add(pStrategy);
  }

  @Override
  public AdjustablePrecision add(AdjustablePrecision pOtherPrecision) {
    return new LoopSummaryPrecision(
        ((AdjustablePrecision) this.precision).add(pOtherPrecision), this.usedStrategies);
  }

  @Override
  public AdjustablePrecision subtract(AdjustablePrecision pOtherPrecision) {
    return new LoopSummaryPrecision(
        ((AdjustablePrecision) this.precision).subtract(pOtherPrecision), this.usedStrategies);
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public <T extends Precision> @Nullable T retrieveWrappedPrecision(Class<T> pType) {
    return ((WrapperPrecision) precision).retrieveWrappedPrecision(pType);
  }

  @Override
  public @Nullable Precision replaceWrappedPrecision(
      Precision pNewPrecision, Predicate<? super Precision> pReplaceType) {
    if (precision instanceof WrapperPrecision) {
      return new LoopSummaryPrecision(
          ((WrapperPrecision) precision).replaceWrappedPrecision(pNewPrecision, pReplaceType),
          this.usedStrategies);
    } else {
      assert pNewPrecision.getClass().isAssignableFrom(precision.getClass());

      return new LoopSummaryPrecision(pNewPrecision, this.usedStrategies);
    }
  }

  @Override
  public Iterable<Precision> getWrappedPrecisions() {
    return new ArrayList<>(Arrays.asList(precision));
  }
}
