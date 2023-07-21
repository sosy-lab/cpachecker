// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import com.google.errorprone.annotations.InlineMe;
import java.util.DoubleSummaryStatistics;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.DoubleConsumer;
import java.util.stream.DoubleStream;

/** Thread-safe implementation of numerical statistics. */
public class StatDouble extends AbstractStatValue implements DoubleConsumer {

  private DoubleAccumulator maxValue = new DoubleAccumulator(Math::max, Double.MIN_VALUE);
  private DoubleAccumulator minValue = new DoubleAccumulator(Math::min, Double.MAX_VALUE);
  private LongAdder valueCount = new LongAdder();
  private DoubleAdder valueSum = new DoubleAdder();

  public static StatDouble forStream(
      StatKind pMainStatisticKind, String pTitle, DoubleStream pStream) {
    return new StatDouble(pMainStatisticKind, pTitle, pStream.summaryStatistics());
  }

  public StatDouble(StatKind pMainStatisticKind, String pTitle) {
    super(pMainStatisticKind, pTitle);
  }

  public StatDouble(
      StatKind pMainStatisticKind, String pTitle, DoubleSummaryStatistics initialValues) {
    this(pMainStatisticKind, pTitle);
    maxValue.accumulate(initialValues.getMax());
    minValue.accumulate(initialValues.getMin());
    valueCount.add(initialValues.getCount());
    valueSum.add(initialValues.getSum());
  }

  public void add(StatDouble pOther) {
    valueSum.add(pOther.getValueSum());
    valueCount.add(pOther.getValueCount());
    maxValue.accumulate(pOther.getMaxValue());
    minValue.accumulate(pOther.getMinValue());
  }

  @InlineMe(replacement = "this.setNextValue(pValue)")
  @Override
  @Deprecated
  public final void accept(double pValue) {
    setNextValue(pValue);
  }

  public void setNextValue(double newValue) {
    valueSum.add(newValue);
    valueCount.increment();
    maxValue.accumulate(newValue);
    minValue.accumulate(newValue);
  }

  public double getMaxValue() {
    return valueCount.sum() == 0 ? 0 : maxValue.doubleValue();
  }

  public double getMinValue() {
    return valueCount.sum() == 0 ? 0 : minValue.doubleValue();
  }

  public long getValueCount() {
    return valueCount.sum();
  }

  public double getValueSum() {
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

  @Override
  public int getUpdateCount() {
    return valueCount.intValue();
  }

  @Override
  public String toString() {
    return switch (getMainStatisticKind()) {
      case SUM -> String.format(
          "%8f (count: %d, min: %f, max: %f, avg: %.2f)",
          getValueSum(), getValueCount(), getMinValue(), getMaxValue(), getAverage());
      case AVG -> String.format(
          "%.2f (sum: %f, count: %d, min: %f, max: %f)",
          getAverage(), getValueSum(), getValueCount(), getMinValue(), getMaxValue());
      case COUNT -> String.format(
          "%8d (sum: %f, min: %f, max: %f, avg: %.2f)",
          getValueCount(), getValueSum(), getMinValue(), getMaxValue(), getAverage());
      case MIN -> String.format(
          "%8f (sum: %f, count: %d, max: %f, avg: %.2f)",
          getMinValue(), getValueSum(), getValueCount(), getMaxValue(), getAverage());
      case MAX -> String.format(
          "%8f (sum: %f, count: %d, min: %f, avg: %.2f)",
          getMaxValue(), getValueSum(), getValueCount(), getMinValue(), getAverage());
    };
  }
}
