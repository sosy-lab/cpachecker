// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;

/**
 * Thread-safe implementation of numerical statistics. This class tracks the number how often each
 * value is added.
 */
public class StatIntHist extends StatInt {

  private Multiset<Integer> hist = ConcurrentHashMultiset.create();

  public StatIntHist(StatKind pMainStatisticKind, String pTitle) {
    super(pMainStatisticKind, pTitle);
  }

  public int getTimesWithValue(Integer value) {
    return hist.count(value);
  }

  @Override
  public void setNextValue(int pNewValue) {
    super.setNextValue(pNewValue);
    hist.add(pNewValue);
  }

  @Override
  public String toString() {
    return super.toString() + " " + hist;
  }
}
