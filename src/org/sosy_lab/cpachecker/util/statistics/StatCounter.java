// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import java.util.concurrent.atomic.LongAdder;

/** Thread-safe implementation of numerical statistics. */
public class StatCounter extends AbstractStatValue {

  private LongAdder counter = new LongAdder();

  public StatCounter(String pTitle) {
    super(StatKind.SUM, pTitle);
  }

  public void inc() {
    counter.increment();
  }

  public long getValue() {
    return counter.sum();
  }

  @Override
  public int getUpdateCount() {
    return counter.intValue();
  }

  @Override
  public String toString() {
    return String.format("%8d", getValue());
  }

  public void mergeWith(StatCounter other) {
    counter.add(other.getValue());
  }
}
