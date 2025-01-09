// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

/**
 * This class contains all statistics from PredicateCPA.
 */
public final class PredicateStatistics {

  // merge operator
  final StatTimer totalMergeTime = new StatTimer("Time for merge operator");

  // precision adjustment
  final StatTimer totalPrecTime = new StatTimer("Time for prec operator");
  final Timer computingAbstractionTime = new Timer();
  final StatCounter numAbstractions = new StatCounter("Number of abstractions");
  final StatCounter numTargetAbstractions =
      new StatCounter("Times abstraction because of target state");
  final StatCounter numAbstractionsFalse = new StatCounter("Times abstraction was 'false'");
  final StatInt blockSize = new StatInt(StatKind.AVG, "Avg ABE block size");

  // domain
  final StatTimer coverageCheckTimer = new StatTimer("Time for coverage checks");
  final StatTimer bddCoverageCheckTimer = new StatTimer("Time for BDD entailment checks");
  final StatTimer symbolicCoverageCheckTimer = new StatTimer("Time for symbolic coverage check");

  // transfer relation
  final StatTimer postTimer = new StatTimer("Time for post operator");
  final StatTimer satCheckTimer = new StatTimer("Time for satisfiability checks");
  final StatTimer pathFormulaTimer = new StatTimer("Time for path formula creation");
  final StatTimer strengthenTimer = new StatTimer("Time for strengthen operator");
  final StatTimer strengthenCheckTimer = new StatTimer("Time for strengthen sat checks");
  final StatTimer abstractionCheckTimer = new StatTimer("Time for abstraction checks");
  final StatCounter numSatChecksFalse = new StatCounter("Times sat checks was 'false'");
  final StatCounter numStrengthenChecksFalse =
      new StatCounter("Times strengthen sat check was 'false'");
}
