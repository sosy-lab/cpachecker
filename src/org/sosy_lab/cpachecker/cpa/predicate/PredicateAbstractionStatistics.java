// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.concurrent.atomic.AtomicInteger;
import org.sosy_lab.common.time.NestedTimer;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

public class PredicateAbstractionStatistics {

  AtomicInteger numCallsAbstraction = new AtomicInteger(0); // total calls
  AtomicInteger numAbstractionReuses = new AtomicInteger(0); // total reuses

  // precision completely empty, no computation
  AtomicInteger numSymbolicAbstractions = new AtomicInteger(0);

  // precision was {false}, only sat check
  AtomicInteger numSatCheckAbstractions = new AtomicInteger(0);

  // result was cached, no computation
  AtomicInteger numCallsAbstractionCached = new AtomicInteger(0);

  // loop was cached, no new computation
  AtomicInteger numInductivePathFormulaCacheUsed = new AtomicInteger(0);

  AtomicInteger numTotalPredicates = new AtomicInteger(0);
  AtomicInteger maxPredicates = new AtomicInteger(0);
  AtomicInteger numIrrelevantPredicates = new AtomicInteger(0);
  AtomicInteger numTrivialPredicates = new AtomicInteger(0);
  AtomicInteger numInductivePredicates = new AtomicInteger(0);
  AtomicInteger numCartesianAbsPredicates = new AtomicInteger(0);
  AtomicInteger numCartesianAbsPredicatesCached = new AtomicInteger(0);
  AtomicInteger numBooleanAbsPredicates = new AtomicInteger(0);
  final Timer abstractionReuseTime = new Timer();
  final StatTimer abstractionReuseImplicationTime =
      new StatTimer("Time for checking reusability of abstractions");
  final Timer trivialPredicatesTime = new Timer();
  final Timer inductivePredicatesTime = new Timer();
  final Timer cartesianAbstractionTime = new Timer();
  final Timer quantifierEliminationTime = new Timer();
  final Timer booleanAbstractionTime = new Timer();

  // outer: solver time, inner: bdd time
  final NestedTimer abstractionEnumTime = new NestedTimer();

  // only the time for solving, not for model enumeration
  final Timer abstractionSolveTime = new Timer();

  long allSatCount = 0;
  int maxAllSatCount = 0;

  public PredicateAbstractionStatistics() {}
}