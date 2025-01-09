// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.concurrent.atomic.AtomicInteger;
import org.sosy_lab.common.time.Timer;

public final class PredicateAbstractionStatistics {

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

  final Timer abstractionReuseTime = new Timer();
  final Timer abstractionReuseImplicationTime = new Timer();
  final Timer trivialPredicatesTime = new Timer();
  final Timer inductivePredicatesTime = new Timer();
  final Timer cartesianAbstractionTime = new Timer();
  final Timer quantifierEliminationTime = new Timer();
  final Timer booleanAbstractionTime = new Timer();

  final Timer abstractionModelEnumTime = new Timer();
  final Timer abstractionBddConstructionTime = new Timer();

  // only the time for solving, not for model enumeration
  final Timer abstractionSolveTime = new Timer();

  long allSatCount = 0;
  int maxAllSatCount = 0;
}
