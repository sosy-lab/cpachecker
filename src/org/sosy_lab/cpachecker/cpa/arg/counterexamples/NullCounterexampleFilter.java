// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;

/**
 * Dummy implementation of {@link CounterexampleFilter} that does not filter any counterexamples.
 */
public class NullCounterexampleFilter implements CounterexampleFilter {

  public NullCounterexampleFilter() {}

  @Override
  public boolean isRelevant(CounterexampleInfo pCounterexample) {
    return true;
  }
}
