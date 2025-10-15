// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

abstract class GlobalRefinementStrategy extends RefinementStrategy {

  protected GlobalRefinementStrategy(Solver pSolver) {
    super(pSolver);
  }

  abstract void initializeGlobalRefinement();

  abstract void updatePrecisionAndARG() throws InterruptedException;

  abstract void resetGlobalRefinement();
}
