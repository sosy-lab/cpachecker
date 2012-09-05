/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates;

import java.util.Map;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.collect.Maps;

/**
 * Alternative to {@link TheoremProver} which provides not basic calls to the
 * solver but more high-level queries.
 *
 * All methods of this class may only be called when the backing prover is in
 * its default state (i.e., a call to {@link TheoremProver#init()} would not fail.
 * It is guaranteed that after using methods of this class, the solver is again
 * in the same state.
 */
public class Solver {

  private final ExtendedFormulaManager fmgr;
  private final TheoremProver prover;

  private final Map<Formula, Boolean> implicationCache = Maps.newHashMap();

  // stats
  public final Timer solverTime = new Timer();
  public int implicationChecks = 0;
  public int trivialImplicationChecks = 0;
  public int cachedImplicationChecks = 0;

  public Solver(ExtendedFormulaManager pFmgr, TheoremProver pProver) {
    fmgr = pFmgr;
    prover = pProver;
  }

  /**
   * Direct reference to the underlying SMT solver for more complicated queries.
   */
  public TheoremProver getTheoremProver() {
    return prover;
  }

  /**
   * Checks whether a formula is unsat.
   */
  public boolean isUnsat(Formula f) {
    solverTime.start();
    prover.init();
    try {
      prover.push(f);
      return prover.isUnsat();

    } finally {
      prover.reset();
      solverTime.stop();
    }
  }

  /**
   * Checks whether a => b.
   * The result is cached.
   */
  public boolean implies(Formula a, Formula b) {
    implicationChecks++;

    if (a.isFalse() || b.isTrue()) {
      trivialImplicationChecks++;
      return true;
    }
    if (a.equals(b)) {
      trivialImplicationChecks++;
      return true;
    }

    Formula f = fmgr.makeNot(fmgr.makeImplication(a, b));

    Boolean result = implicationCache.get(f);
    if (result != null) {
      cachedImplicationChecks++;
      return result;
    }

    result = isUnsat(f);

    implicationCache.put(f, result);
    return result;
  }
}
