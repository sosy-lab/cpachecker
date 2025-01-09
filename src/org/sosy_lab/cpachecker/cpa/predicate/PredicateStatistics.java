// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

/** This class contains all statistics from PredicateCPA. */
public final class PredicateStatistics {

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
