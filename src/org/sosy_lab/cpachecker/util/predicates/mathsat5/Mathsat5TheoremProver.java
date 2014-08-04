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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5FormulaManager.getMsatTerm;
import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.time.NestedTimer;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager.RegionBuilder;

import com.google.common.base.Preconditions;

public class Mathsat5TheoremProver extends Mathsat5AbstractProver implements ProverEnvironment {

  private static final boolean USE_SHARED_ENV = true;

  public Mathsat5TheoremProver(Mathsat5FormulaManager pMgr,
      boolean generateModels, boolean generateUnsatCore) {
    super(pMgr, createConfig(pMgr, generateModels, generateUnsatCore), USE_SHARED_ENV, true);
  }

  private static long createConfig(Mathsat5FormulaManager mgr,
      boolean generateModels, boolean generateUnsatCore) {
    long cfg = msat_create_config();
    if (generateModels) {
      msat_set_option_checked(cfg, "model_generation", "true");
    }
    if (generateUnsatCore) {
      msat_set_option_checked(cfg, "unsat_core_generation", "1");
    }
    return cfg;
  }

  @Override
  public void push(BooleanFormula f) {
    Preconditions.checkState(curEnv != 0);
    msat_push_backtrack_point(curEnv);
    msat_assert_formula(curEnv, getMsatTerm(f));
  }

  @Override
  public OptResult isOpt(Formula f, boolean maximize) throws InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<BooleanFormula> getUnsatCore() {
    Preconditions.checkState(curEnv != 0);
    long[] terms = msat_get_unsat_core(curEnv);
    List<BooleanFormula> result = new ArrayList<>(terms.length);
    for (long t : terms) {
      result.add(new Mathsat5BooleanFormula(t));
    }
    return result;
  }

  @Override
  public AllSatResult allSat(Collection<BooleanFormula> important,
      RegionCreator rmgr, Timer solveTime, NestedTimer enumTime) throws InterruptedException {
    checkNotNull(rmgr);
    checkNotNull(solveTime);
    checkNotNull(enumTime);
    Preconditions.checkState(curEnv != 0);

    if (important.isEmpty()) {
      throw new RuntimeException("Error occurred during Mathsat allsat: all-sat should not be called with empty 'important'-Collection");
    }

    long[] imp = new long[important.size()];
    int i = 0;
    for (BooleanFormula impF : important) {

      imp[i++] = getMsatTerm(impF);

    }

    MathsatAllSatCallback callback = new MathsatAllSatCallback(this, rmgr, solveTime, enumTime, curEnv);
    solveTime.start();
    int numModels;
    try {
      numModels = msat_all_sat(curEnv, imp, callback);
    } finally {
      if (solveTime.isRunning()) {
        solveTime.stop();
      } else {
        enumTime.stopOuter();
      }
    }

    if (numModels == -1) {
      throw new RuntimeException("Error occurred during Mathsat allsat: " + msat_last_error_message(curEnv));

    } else if (numModels == -2) {
      // infinite models
      callback.setInfiniteNumberOfModels();
    } else {
      assert numModels == callback.count;
    }

    return callback;
  }

  /**
   * callback used to build the predicate abstraction of a formula
   */
  static class MathsatAllSatCallback implements Mathsat5NativeApi.AllSatModelCallback, AllSatResult {

    private final ShutdownNotifier shutdownNotifier;
    private final RegionCreator rmgr;
    private final RegionBuilder builder;

    private final Timer solveTime;
    private final NestedTimer enumTime;
    private Timer regionTime = null;

    private int count = 0;

    private Region formula = null;
    private long env;

    private Mathsat5TheoremProver prover;

    public MathsatAllSatCallback(Mathsat5TheoremProver prover, RegionCreator rmgr, Timer pSolveTime, NestedTimer pEnumTime, long env) {
      this.rmgr = rmgr;
      this.prover = prover;
      this.solveTime = pSolveTime;
      this.enumTime = pEnumTime;
      this.env = env;
      this.shutdownNotifier = prover.mgr.getShutdownNotifier();
      builder = rmgr.newRegionBuilder(shutdownNotifier);
    }

    public void setInfiniteNumberOfModels() {
      count = Integer.MAX_VALUE;
      formula = rmgr.makeTrue();
    }

    @Override
    public int getCount() {
      return count;
    }

    @Override
    public Region getResult() throws InterruptedException {
      if (formula == null) {
        enumTime.startBoth();
        try {
          formula = builder.getResult();
          builder.close();
        } finally {
          enumTime.stopBoth();
        }
      }
      return formula;
    }

    @Override
    public void callback(long[] model) throws InterruptedException {
      if (count == 0) {
        solveTime.stop();
        enumTime.startOuter();
        regionTime = enumTime.getCurentInnerTimer();
      }

      shutdownNotifier.shutdownIfNecessary();

      regionTime.start();

      // the abstraction is created simply by taking the disjunction
      // of all the models found by msat_all_sat, and storing them
      // in a BDD
      // first, let's create the BDD corresponding to the model
      builder.startNewConjunction();
      for (long t : model) {
        if (msat_term_is_not(env, t)) {
          t = msat_term_get_arg(t, 0);

          builder.addNegativeRegion(rmgr.getPredicate(prover.mgr.encapsulateBooleanFormula(t)));
        } else {
          builder.addPositiveRegion(rmgr.getPredicate(prover.mgr.encapsulateBooleanFormula(t)));
        }
      }
      builder.finishConjunction();

      count++;

      regionTime.stop();
    }
  }
}
