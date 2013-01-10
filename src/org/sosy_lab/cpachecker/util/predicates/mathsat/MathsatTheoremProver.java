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
package org.sosy_lab.cpachecker.util.predicates.mathsat;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager.*;
import static org.sosy_lab.cpachecker.util.predicates.mathsat.NativeApi.*;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import org.sosy_lab.common.NestedTimer;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.base.Preconditions;

public class MathsatTheoremProver implements TheoremProver {

  private static final boolean USE_SHARED_ENV = true;

  private final MathsatFormulaManager mgr;
  private long curEnv;

  public MathsatTheoremProver(MathsatFormulaManager pMgr) {
    mgr = pMgr;
    curEnv = 0;
  }

  @Override
  public boolean isUnsat() {
    int res = msat_solve(curEnv);
    assert(res != MSAT_UNKNOWN);
    return res == MSAT_UNSAT;
  }

  @Override
  public Model getModel() {
    Preconditions.checkState(curEnv != 0);

    return MathsatModel.createMathsatModel(curEnv, mgr, USE_SHARED_ENV);
  }

  @Override
  public void pop() {
    Preconditions.checkState(curEnv != 0);
    int ok = msat_pop_backtrack_point(curEnv);
    assert(ok == 0);
  }

  @Override
  public void push(Formula f) {
    Preconditions.checkState(curEnv != 0);
    msat_push_backtrack_point(curEnv);
    msat_assert_formula(curEnv, getTerm(f));
  }

  @Override
  public void init() {
    Preconditions.checkState(curEnv == 0);

    curEnv = mgr.createEnvironment(USE_SHARED_ENV, true);
  }

  @Override
  public void reset() {
    Preconditions.checkState(curEnv != 0);
    msat_destroy_env(curEnv);
    curEnv = 0;
  }

  @Override
  public AllSatResult allSat(Formula f, Collection<Formula> important,
                             RegionCreator rmgr, Timer solveTime, NestedTimer enumTime) {
    checkNotNull(rmgr);
    checkNotNull(solveTime);
    checkNotNull(enumTime);
    long formula = getTerm(f);

    long allsatEnv = mgr.createEnvironment(USE_SHARED_ENV, true);

    long[] imp = new long[important.size()];
    int i = 0;
    for (Formula impF : important) {
      imp[i++] = getTerm(impF);
    }
    MathsatAllSatCallback callback = new MathsatAllSatCallback(rmgr, solveTime, enumTime);
    solveTime.start();
    msat_assert_formula(allsatEnv, formula);
    int numModels = msat_all_sat(allsatEnv, imp, callback);

    if (solveTime.isRunning()) {
      solveTime.stop();
    } else {
      enumTime.stopOuter();
    }

    if (numModels == -1) {
      throw new RuntimeException("Error occurred during Mathsat allsat");

    } else if (numModels == -2) {
      // infinite models
      callback.setInfiniteNumberOfModels();

    } else {
      assert numModels == callback.count;
    }

    msat_destroy_env(allsatEnv);

    return callback;
  }

  /**
   * callback used to build the predicate abstraction of a formula
   */
  static class MathsatAllSatCallback implements NativeApi.AllSatModelCallback, TheoremProver.AllSatResult {
    private final RegionCreator rmgr;

    private final Timer solveTime;
    private final NestedTimer enumTime;
    private Timer regionTime = null;

    private int count = 0;

    private Region formula;
    private final Deque<Region> cubes = new ArrayDeque<Region>();

    public MathsatAllSatCallback(RegionCreator rmgr, Timer pSolveTime, NestedTimer pEnumTime) {
      this.rmgr = rmgr;
      this.formula = rmgr.makeFalse();
      this.solveTime = pSolveTime;
      this.enumTime = pEnumTime;
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
    public Region getResult() {
      if (cubes.size() > 0) {
        buildBalancedOr();
      }
      return formula;
    }

    private void buildBalancedOr() {
      cubes.add(formula);
      while (cubes.size() > 1) {
        Region b1 = cubes.remove();
        Region b2 = cubes.remove();
        cubes.add(rmgr.makeOr(b1, b2));
      }
      assert(cubes.size() == 1);
      formula = cubes.remove();
    }

    @Override
    public void callback(long[] model) {
      if (count == 0) {
        solveTime.stop();
        enumTime.startOuter();
        regionTime = enumTime.getInnerTimer();
      }

      regionTime.start();

      // the abstraction is created simply by taking the disjunction
      // of all the models found by msat_all_sat, and storing them
      // in a BDD
      // first, let's create the BDD corresponding to the model
      Deque<Region> curCube = new ArrayDeque<Region>(model.length + 1);
      Region m = rmgr.makeTrue();
      for (long t : model) {
        Region v;
        if (msat_term_is_not(t) != 0) {
          t = msat_term_get_arg(t, 0);
          v = rmgr.getPredicate(encapsulate(t));
          v = rmgr.makeNot(v);
        } else {
          v = rmgr.getPredicate(encapsulate(t));
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
      assert(curCube.size() == 1);
      m = curCube.remove();
      cubes.add(m);

      count++;

      regionTime.stop();
    }
  }
}
