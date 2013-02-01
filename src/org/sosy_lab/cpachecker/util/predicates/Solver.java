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
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.Maps;

/**
 * Abstraction of an SMT solver that also provides some higher-level methods.
 */
public class Solver {

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final FormulaManagerFactory factory;

  private final Map<BooleanFormula, Boolean> implicationCache = Maps.newHashMap();

  // stats
  public final Timer solverTime = new Timer();
  public int implicationChecks = 0;
  public int trivialImplicationChecks = 0;
  public int cachedImplicationChecks = 0;

  public Solver(FormulaManagerView pFmgr, FormulaManagerFactory pFactory) {
    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    factory = pFactory;
  }

  /**
   * Direct reference to the underlying SMT solver for more complicated queries.
   * This creates a fresh, new, environment in the solver.
   * This environment needs to be closed after it is used by calling {@link ProverEnvironment#close()}.
   * It is recommended to use the try-with-resources syntax.
   */
  public ProverEnvironment newProverEnvironment() {
    return factory.newProverEnvironment();
  }

  /**
   * Checks whether a formula is unsat.
   */
  public boolean isUnsat(BooleanFormula f) {
    solverTime.start();
    try (ProverEnvironment prover = newProverEnvironment()) {
      prover.push(f);
      return prover.isUnsat();

    } finally {
      solverTime.stop();
    }
  }

  /**
   * Checks whether a => b.
   * The result is cached.
   */
  public boolean implies(BooleanFormula a, BooleanFormula b) {
    implicationChecks++;

    if (bfmgr.isFalse(a) || bfmgr.isTrue(b)) {
      trivialImplicationChecks++;
      return true;
    }
    if (a.equals(b)) {
      trivialImplicationChecks++;
      return true;
    }

    BooleanFormula f = bfmgr.not(bfmgr.implication(a, b));

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
