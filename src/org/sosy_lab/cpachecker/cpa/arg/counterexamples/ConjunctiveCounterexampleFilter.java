/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import java.util.List;

import org.sosy_lab.cpachecker.core.CounterexampleInfo;

import com.google.common.collect.ImmutableList;

/**
 * A {@link CounterexampleFilter} that is a conjunction of other filters,
 * i.e., each filter needs to agree that a counterexample is relevant.
 *
 * Note that because {@link CounterexampleFilter}s are usually stateful,
 * it is important to put the weakest filter first,
 * i.e., the one that defines most counterexamples as relevant.
 * So usually the chain would start with something like a cheap but weak
 * {@link PathEqualityCounterexampleFilter},
 * and contain more expensive (and stronger) filters at the end.
 */
public class ConjunctiveCounterexampleFilter implements CounterexampleFilter {

  private final List<CounterexampleFilter> filters;

  public ConjunctiveCounterexampleFilter(List<CounterexampleFilter> pFilters) {
    filters = ImmutableList.copyOf(pFilters);
  }

  @Override
  public boolean isRelevant(CounterexampleInfo counterexample) throws InterruptedException {
    for (CounterexampleFilter filter : filters) {
      if (!filter.isRelevant(counterexample)) {
        return false;
      }
    }
    return true;
  }
}
