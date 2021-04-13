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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;

class LoopSummaryPrecision implements AdjustablePrecision, WrapperPrecision {

  private final Precision precision;

  private int strategyCounter = 0;

  public LoopSummaryPrecision(Precision precision) {
    this.precision = precision;
  }

  public LoopSummaryPrecision(Precision precision, int strategyCounter) {
    this.precision = precision;
    this.strategyCounter = strategyCounter;
  }

  @Override
  public String toString() {
    return "Loopsummary precision { precission: "
        + precision.toString()
        + ", strategy:  "
        + strategyCounter
        + " }";
  }


  public Precision getPrecision() {
    return precision;
  }

  public int getStrategyCounter() {
    return strategyCounter;
  }

  public void updateStrategy() {
    strategyCounter += 1;
  }

  @Override
  public AdjustablePrecision add(AdjustablePrecision pOtherPrecision) {
    return new LoopSummaryPrecision(
        ((AdjustablePrecision) this.precision).add(pOtherPrecision), this.strategyCounter);
  }

  @Override
  public AdjustablePrecision subtract(AdjustablePrecision pOtherPrecision) {
    return new LoopSummaryPrecision(
        ((AdjustablePrecision) this.precision).subtract(pOtherPrecision), this.strategyCounter);
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
          this.strategyCounter);
    } else {
      assert pNewPrecision.getClass().isAssignableFrom(precision.getClass());

      return new LoopSummaryPrecision(pNewPrecision, this.strategyCounter);
    }
  }

  @Override
  public Iterable<Precision> getWrappedPrecisions() {
    return new ArrayList<>(Arrays.asList(precision));
  }
}
