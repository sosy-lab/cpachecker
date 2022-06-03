// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import com.google.errorprone.annotations.InlineMe;
import java.util.IntSummaryStatistics;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

/** Thread-safe implementation of numerical statistics. */
public class StatInt extends AbstractStatValue implements IntConsumer {

  private LongAccumulator maxValue = new LongAccumulator(Math::max, Integer.MIN_VALUE);
  private LongAccumulator minValue = new LongAccumulator(Math::min, Integer.MAX_VALUE);
  private LongAdder valueCount = new LongAdder();
  private LongAdder valueSum = new LongAdder();

  public static StatInt forStream(StatKind pMainStatisticKind, String pTitle, IntStream pStream) {
    return new StatInt(pMainStatisticKind, pTitle, pStream.summaryStatistics());
  }

  public StatInt(StatKind pMainStatisticKind, String pTitle) {
    super(pMainStatisticKind, pTitle);
  }

  public StatInt(StatKind pMainStatisticKind, String pTitle, IntSummaryStatistics initialValues) {
    this(pMainStatisticKind, pTitle);
    maxValue.accumulate(initialValues.getMax());
    minValue.accumulate(initialValues.getMin());
    valueCount.add(initialValues.getCount());
    valueSum.add(initialValues.getSum());
  }

  public void add(StatInt pOther) {
    valueSum.add(pOther.getValueSum());
    valueCount.add(pOther.getValueCount());
    maxValue.accumulate(pOther.getMaxValue());
    minValue.accumulate(pOther.getMinValue());
  }

  @InlineMe(replacement = "this.setNextValue(pValue)")
  @Override
  @Deprecated
  public final void accept(int pValue) {
    setNextValue(pValue);
  }

  public void setNextValue(int newValue) {
    valueSum.add(newValue);
    valueCount.increment();
    maxValue.accumulate(newValue);
    minValue.accumulate(newValue);
  }

  public long getMaxValue() {
    return valueCount.sum() == 0 ? 0 : maxValue.intValue();
  }

  public long getMinValue() {
    return valueCount.sum() == 0 ? 0 : minValue.intValue();
  }

  public long getValueCount() {
    return valueCount.sum();
  }

  public long getValueSum() {
    return valueSum.sum();
  }

  public float getAverage() {
    long count = valueCount.sum();
    if (count > 0) {
      return (float) valueSum.sum() / count;
    } else {
      return 0;
    }
  }

  @Deprecated
  public long getMax() {
    return maxValue.get();
  }

  @Deprecated
  public long getMin() {
    return minValue.get();
  }

  @Override
  public int getUpdateCount() {
    return valueCount.intValue();
  }

  @Override
  public String toString() {
    switch (getMainStatisticKind()) {
      case SUM:
        return String.format(
            "%8d (count: %d, min: %d, max: %d, avg: %.2f)",
            getValueSum(), getValueCount(), getMinValue(), getMaxValue(), getAverage());
      case AVG:
        return String.format(
            "%.2f (sum: %d, count: %d, min: %d, max: %d)",
            getAverage(), getValueSum(), getValueCount(), getMinValue(), getMaxValue());
      case COUNT:
        return String.format(
            "%8d (sum: %d, min: %d, max: %d, avg: %.2f)",
            getValueCount(), getValueSum(), getMinValue(), getMaxValue(), getAverage());
      case MIN:
        return String.format(
            "%8d (sum: %d, count: %d, max: %d, avg: %.2f)",
            getMinValue(), getValueSum(), getValueCount(), getMaxValue(), getAverage());
      case MAX:
        return String.format(
            "%8d (sum: %d, count: %d, min: %d, avg: %.2f)",
            getMaxValue(), getValueSum(), getValueCount(), getMinValue(), getAverage());
    }
    throw new AssertionError();
  }
}
