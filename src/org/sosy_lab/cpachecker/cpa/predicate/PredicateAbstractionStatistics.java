// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.concurrent.atomic.AtomicInteger;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer;

@SuppressWarnings("deprecation") // remove ThreadSafeTimerContainer
public class PredicateAbstractionStatistics {

  final AtomicInteger numCallsAbstraction = new AtomicInteger(0); // total calls
  final AtomicInteger numAbstractionReuses = new AtomicInteger(0); // total reuses

  // precision completely empty, no computation
  final AtomicInteger numSymbolicAbstractions = new AtomicInteger(0);

  // precision was {false}, only sat check
  final AtomicInteger numSatCheckAbstractions = new AtomicInteger(0);

  // result was cached, no computation
  final AtomicInteger numCallsAbstractionCached = new AtomicInteger(0);

  // loop was cached, no new computation
  final AtomicInteger numInductivePathFormulaCacheUsed = new AtomicInteger(0);

  final AtomicInteger numTotalPredicates = new AtomicInteger(0);
  final AtomicInteger maxPredicates = new AtomicInteger(0);
  final AtomicInteger numIrrelevantPredicates = new AtomicInteger(0);
  final AtomicInteger numTrivialPredicates = new AtomicInteger(0);
  final AtomicInteger numInductivePredicates = new AtomicInteger(0);
  final AtomicInteger numCartesianAbsPredicates = new AtomicInteger(0);
  final AtomicInteger numCartesianAbsPredicatesCached = new AtomicInteger(0);
  final AtomicInteger numBooleanAbsPredicates = new AtomicInteger(0);

  final ThreadSafeTimerContainer abstractionReuseTime =
      new ThreadSafeTimerContainer("Abstraction reuse");
  final ThreadSafeTimerContainer abstractionReuseImplicationTime =
      new ThreadSafeTimerContainer("Time for checking reusability of abstractions");
  final ThreadSafeTimerContainer trivialPredicatesTime =
      new ThreadSafeTimerContainer("Time for relevant predicate analysis");
  final ThreadSafeTimerContainer inductivePredicatesTime =
      new ThreadSafeTimerContainer("Time for inductive predicate analysis");
  final ThreadSafeTimerContainer cartesianAbstractionTime =
      new ThreadSafeTimerContainer("Time for cartesian abstraction");
  final ThreadSafeTimerContainer quantifierEliminationTime =
      new ThreadSafeTimerContainer("Time for eliminating quantifiers");
  final ThreadSafeTimerContainer booleanAbstractionTime =
      new ThreadSafeTimerContainer("Time for boolean abstraction");

  final ThreadSafeTimerContainer abstractionModelEnumTime =
      new ThreadSafeTimerContainer("Time for model enumeration");
  final ThreadSafeTimerContainer abstractionBddConstructionTime =
      new ThreadSafeTimerContainer("Time for BDD construction");

  // only the time for solving, not for model enumeration
  final ThreadSafeTimerContainer abstractionSolveTime =
      new ThreadSafeTimerContainer("Time for abstraction solving");

  long allSatCount = 0;
  int maxAllSatCount = 0;

  public PredicateAbstractionStatistics() {}
}
