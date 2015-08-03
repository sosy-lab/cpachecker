/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.mathsat5;

import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.*;

import java.util.List;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

/**
 * Common base class for {@link Mathsat5TheoremProver}
 * and {@link Mathsat5InterpolatingProver}.
 */
abstract class Mathsat5AbstractProver {

  protected final Mathsat5FormulaManager mgr;
  protected long curEnv;
  private final long curConfig;
  private final boolean useSharedEnv;

  private final long terminationTest;

  protected Mathsat5AbstractProver(Mathsat5FormulaManager pMgr, long pConfig,
      boolean pShared, boolean pGhostFilter) {
    mgr = pMgr;
    curConfig = pConfig;
    useSharedEnv = pShared;
    curEnv = mgr.createEnvironment(pConfig, pShared, pGhostFilter);
    terminationTest = mgr.addTerminationTest(curEnv);
  }

  public boolean isUnsat() throws InterruptedException, SolverException {
    Preconditions.checkState(curEnv != 0);
    try {
      return !msat_check_sat(curEnv);
    } catch (IllegalStateException e) {
      handleSolverExceptionInUnsatCheck(e);
      throw e;
    }
  }

  public boolean isUnsatWithAssumptions(List<BooleanFormula> pAssumptions) throws SolverException, InterruptedException {
    Preconditions.checkState(curEnv != 0);
    try {
      long[] assumptions = Longs.toArray(Lists.transform(pAssumptions, new Function<BooleanFormula, Long>() {
        @Override
        public Long apply(BooleanFormula pInput) {
          long t = Mathsat5FormulaManager.getMsatTerm(pInput);
          if (!useSharedEnv) {
            t = msat_make_copy_from(curEnv, t, mgr.getEnvironment());
          }
          return t;
        }
      }));
      return !msat_check_sat_with_assumptions(curEnv, assumptions);
    } catch (IllegalStateException e) {
      handleSolverExceptionInUnsatCheck(e);
      throw e;
    }
  }

  private void handleSolverExceptionInUnsatCheck(IllegalStateException e) throws SolverException {
    String msg = Strings.nullToEmpty(e.getMessage());
    if (msg.contains("too many iterations")
        || msg.contains("impossible to build a suitable congruence graph!")
        || msg.contains("can't produce proofs")) {
      // This is not a bug in CPAchecker, but a problem of MathSAT which happens during interpolation
      throw new SolverException(e.getMessage(), e);
    }
  }

  public Model getModel() throws SolverException {
    Preconditions.checkState(curEnv != 0);
    return Mathsat5Model.createMathsatModel(curEnv);
  }

  public void pop() {
    Preconditions.checkState(curEnv != 0);
    msat_pop_backtrack_point(curEnv);
  }

  public void close() {
    Preconditions.checkState(curEnv != 0);
    msat_destroy_env(curEnv);
    curEnv = 0;
    msat_free_termination_test(terminationTest);
    msat_destroy_config(curConfig);
  }
}