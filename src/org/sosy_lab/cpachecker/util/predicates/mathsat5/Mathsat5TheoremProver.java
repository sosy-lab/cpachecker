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
package org.sosy_lab.cpachecker.util.predicates.mathsat5;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5FormulaManager.getMsatTerm;
import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.*;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import org.sosy_lab.common.NestedTimer;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.base.Preconditions;

public class Mathsat5TheoremProver extends Mathsat5AbstractProver implements ProverEnvironment {

  private static final boolean USE_SHARED_ENV = true;

  public Mathsat5TheoremProver(Mathsat5FormulaManager pMgr,
      boolean generateModels) {
    super(pMgr, createEnvironment(pMgr, generateModels));
  }

  private static long createEnvironment(Mathsat5FormulaManager mgr, boolean generateModels) {
    long cfg = msat_create_config();
    if (generateModels) {
      msat_set_option_checked(cfg, "model_generation", "true");
    }
    return mgr.createEnvironment(cfg, USE_SHARED_ENV, true);
  }

  @Override
  public void push(BooleanFormula f) {
    Preconditions.checkState(curEnv != 0);
    msat_push_backtrack_point(curEnv);
    msat_assert_formula(curEnv, getMsatTerm(f));
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

    int numModels = msat_all_sat(curEnv, imp, callback);

    if (solveTime.isRunning()) {
      solveTime.stop();
    } else {
      enumTime.stopOuter();
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

    private final Timer solveTime;
    private final NestedTimer enumTime;
    private Timer regionTime = null;

    private int count = 0;

    private Region formula;
    private final Deque<Region> cubes = new ArrayDeque<>();
    private long env;

    private Mathsat5TheoremProver prover;

    public MathsatAllSatCallback(Mathsat5TheoremProver prover, RegionCreator rmgr, Timer pSolveTime, NestedTimer pEnumTime, long env) {
      this.rmgr = rmgr;
      this.prover = prover;
      this.formula = rmgr.makeFalse();
      this.solveTime = pSolveTime;
      this.enumTime = pEnumTime;
      this.env = env;
      this.shutdownNotifier = prover.mgr.getShutdownNotifier();
    }

    public void setInfiniteNumberOfModels() {
      count = Integer.MAX_VALUE;
      cubes.clear();
      formula = rmgr.makeTrue();
    }

    @Override
    public int getCount() {
      return count;
    }

    @Override
    public Region getResult() throws InterruptedException {
      if (cubes.size() > 0) {
        buildBalancedOr();
      }
      return formula;
    }

    private void buildBalancedOr() throws InterruptedException {
      enumTime.startBoth();
      cubes.add(formula);
      while (cubes.size() > 1) {
        shutdownNotifier.shutdownIfNecessary();
        Region b1 = cubes.remove();
        Region b2 = cubes.remove();
        cubes.add(rmgr.makeOr(b1, b2));
      }
      assert (cubes.size() == 1);
      formula = cubes.remove();
      enumTime.stopBoth();
    }

    @Override
    public void callback(long[] model) throws InterruptedException {
      if (count == 0) {
        solveTime.stop();
        enumTime.startOuter();
        regionTime = enumTime.getInnerTimer();
      }

      shutdownNotifier.shutdownIfNecessary();

      regionTime.start();

      // the abstraction is created simply by taking the disjunction
      // of all the models found by msat_all_sat, and storing them
      // in a BDD
      // first, let's create the BDD corresponding to the model
      Deque<Region> curCube = new ArrayDeque<>(model.length + 1);
      Region m = rmgr.makeTrue();
      for (long t : model) {
        Region v;
        if (msat_term_is_not(env, t)) {
          t = msat_term_get_arg(t, 0);

          v = rmgr.getPredicate(prover.mgr.encapsulateBooleanFormula(t));
          v = rmgr.makeNot(v);
        } else {
          v = rmgr.getPredicate(prover.mgr.encapsulateBooleanFormula(t));
        }
        curCube.add(v);
      }
      // now, add the model to the bdd
      curCube.add(m);
      while (curCube.size() > 1) {
        Region v1 = curCube.remove();
        Region v2 = curCube.remove();
        curCube.add(rmgr.makeAnd(v1, v2));
      }
      assert (curCube.size() == 1);
      m = curCube.remove();
      cubes.add(m);

      count++;

      regionTime.stop();
    }
  }
}
