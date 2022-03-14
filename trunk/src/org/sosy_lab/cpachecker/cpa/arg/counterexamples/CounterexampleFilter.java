// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

/**
 * This interface defines an abstraction for counterexample filter. When ARGCPA handles multiple
 * counterexamples, there might be many similar counterexamples in the reached set, but the user
 * would probably like to see not all of them, only those that differ significantly. A
 * counterexample filter is used to filter all those unwanted counterexamples.
 *
 * <p>It is expected that a counterexample filter is stateful. It usually keeps track of all
 * previously seen counterexamples (at least of the relevant ones), and compares a new
 * counterexample against this set.
 *
 * <p>IMPORTANT: A counterexample filter should try hard to not have a reference on ARGStates! Doing
 * so would retain a lot of memory, because every ARGState has (transitive) references to the full
 * ARG. Also ARGStates may be deleted later on, which changes their state and thus makes them
 * useless.
 *
 * <p>Instead, prefer keeping references to objects like CFAEdges, or representations of program
 * state in a different form (variable assignments, formulas, etc.).
 *
 * <p>Counterexample filters do not need to be thread-safe.
 *
 * <p>Implementations need to have exactly one public constructor or a static method named "create"
 * which may take a {@link Configuration}, a {@link LogManager}, and a {@link
 * ConfigurableProgramAnalysis}, and throw at most an {@link InvalidConfigurationException}.
 */
public interface CounterexampleFilter {

  boolean isRelevant(CounterexampleInfo counterexample) throws InterruptedException;

  interface Factory {
    CounterexampleFilter create(
        Configuration config, LogManager logger, ConfigurableProgramAnalysis cpa)
        throws InvalidConfigurationException;
  }

  static CounterexampleFilter createCounterexampleFilter(
      Configuration config,
      LogManager logger,
      ConfigurableProgramAnalysis cpa,
      List<CounterexampleFilter.Factory> cexFilterClasses)
      throws InvalidConfigurationException {
    switch (cexFilterClasses.size()) {
      case 0:
        return new NullCounterexampleFilter();
      case 1:
        return cexFilterClasses.get(0).create(config, logger, cpa);
      default:
        List<CounterexampleFilter> filters = new ArrayList<>(cexFilterClasses.size());
        for (CounterexampleFilter.Factory factory : cexFilterClasses) {
          filters.add(factory.create(config, logger, cpa));
        }
        return new ConjunctiveCounterexampleFilter(filters);
    }
  }
}
