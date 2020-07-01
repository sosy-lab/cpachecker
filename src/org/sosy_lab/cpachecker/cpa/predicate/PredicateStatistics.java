/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer;

/**
 * This class contains all statistics from PredicateCPA.
 *
 * <p>We aim towards a centralized and threadsafe implementation here.
 */
public class PredicateStatistics {

  // merge operator
  final ThreadSafeTimerContainer totalMergeTime =
      new ThreadSafeTimerContainer("Time for merge operator");

  // precision adjustment
  final ThreadSafeTimerContainer totalPrecTime =
      new ThreadSafeTimerContainer("Time for prec operator");
  final ThreadSafeTimerContainer computingAbstractionTime =
      new ThreadSafeTimerContainer("Time for abstraction");
  final StatCounter numAbstractions = new StatCounter("Number of abstractions");
  final StatCounter numTargetAbstractions =
      new StatCounter("Times abstraction because of target state");
  final StatCounter numAbstractionsFalse = new StatCounter("Times abstraction was 'false'");
  final StatInt blockSize = new StatInt(StatKind.AVG, "Avg ABE block size");

  // domain
  final ThreadSafeTimerContainer coverageCheckTimer =
      new ThreadSafeTimerContainer("Time for coverage checks");
  final ThreadSafeTimerContainer bddCoverageCheckTimer =
      new ThreadSafeTimerContainer("Time for BDD entailment checks");
  final ThreadSafeTimerContainer symbolicCoverageCheckTimer =
      new ThreadSafeTimerContainer("Time for symbolic coverage check");

  // transfer relation
  final ThreadSafeTimerContainer postTimer = new ThreadSafeTimerContainer("Time for post operator");
  final ThreadSafeTimerContainer satCheckTimer =
      new ThreadSafeTimerContainer("Time for satisfiability checks");
  final ThreadSafeTimerContainer pathFormulaTimer =
      new ThreadSafeTimerContainer("Time for path formula creation");
  final ThreadSafeTimerContainer strengthenTimer =
      new ThreadSafeTimerContainer("Time for strengthen operator");
  final ThreadSafeTimerContainer strengthenCheckTimer =
      new ThreadSafeTimerContainer("Time for strengthen sat checks");
  final ThreadSafeTimerContainer abstractionCheckTimer =
      new ThreadSafeTimerContainer("Time for abstraction checks");
  final StatCounter numSatChecksFalse = new StatCounter("Times sat checks was 'false'");
  final StatCounter numStrengthenChecksFalse =
      new StatCounter("Times strengthen sat check was 'false'");
}
