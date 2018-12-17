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

import org.sosy_lab.common.time.Timer;

/**
 * This class contains all statistics from PredicateCPA.
 *
 * <p>We aim towards a centralized and threadsafe implementation here.
 */
public class PredicateStatistics {

  // merge operator
  final Timer totalMergeTime = new Timer();

  // precision adjustment
  final Timer totalPrecTime = new Timer();
  final Timer computingAbstractionTime = new Timer();
  int numAbstractions = 0;
  int numTargetAbstractions = 0;
  int numAbstractionsFalse = 0;
  int maxBlockSize = 0;

  // domain
  final Timer coverageCheckTimer = new Timer();
  final Timer bddCoverageCheckTimer = new Timer();
  final Timer symbolicCoverageCheckTimer = new Timer();

  // transfer relation
  final Timer postTimer = new Timer();
  final Timer satCheckTimer = new Timer();
  final Timer pathFormulaTimer = new Timer();
  final Timer strengthenTimer = new Timer();
  final Timer strengthenCheckTimer = new Timer();
  final Timer abstractionCheckTimer = new Timer();
  int numSatChecksFalse = 0;
  int numStrengthenChecksFalse = 0;
}
