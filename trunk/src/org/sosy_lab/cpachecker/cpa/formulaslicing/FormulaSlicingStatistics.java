// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

/** Statistics for formula slicing. */
class FormulaSlicingStatistics implements Statistics {

  private final CachingPathFormulaManager cachingPathFormulaManager;
  private final Solver solver;

  FormulaSlicingStatistics(CachingPathFormulaManager pFmgr, Solver pSolver) {
    cachingPathFormulaManager = pFmgr;
    solver = pSolver;
  }

  /** Time spent constructing formulas. */
  final Timer propagation = new Timer();

  /** Time spent in inductive weakening. */
  final Timer inductiveWeakening = new Timer();

  final Multiset<CFANode> inductiveWeakeningLocations = HashMultiset.create();
  int cachedInductiveWeakenings = 0;

  /** Reachability statistics. */
  final Multiset<CFANode> satChecksLocations = HashMultiset.create();

  final Timer reachabilityTargetTimer = new Timer();
  final Timer reachabilityAbstractionTimer = new Timer();

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    printTimer(
        out, propagation, "propagating formulas", cachingPathFormulaManager.pathFormulaCacheHits);

    printTimer(out, inductiveWeakening, "inductive weakening", cachedInductiveWeakenings);

    printTimer(out, solver.solverTime, "checking reachability", solver.cachedSatChecks);
    printTimer(out, reachabilityTargetTimer, "checking reachability for target states", "?");
    printTimer(
        out, reachabilityAbstractionTimer, "checking reachability for abstraction states", "?");
    out.printf("Locations for checking reachability: %s%n", satChecksLocations);
  }

  @Override
  public String getName() {
    return "Formula Slicing Manager";
  }

  private void printTimer(PrintStream out, Timer t, String name, Object cacheHits) {
    out.printf(
        "Time spent in %s: %s (Max: %s), (Avg: %s), (#calls = %s), " + "(#cached = %s) %n",
        name,
        t.getSumTime().formatAs(TimeUnit.SECONDS),
        t.getMaxTime().formatAs(TimeUnit.SECONDS),
        t.getAvgTime().formatAs(TimeUnit.SECONDS),
        t.getNumberOfIntervals(),
        cacheHits);
  }
}
