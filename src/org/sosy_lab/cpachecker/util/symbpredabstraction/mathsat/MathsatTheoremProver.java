/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat;

import static mathsat.api.*;
import static org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.*;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import org.sosy_lab.cpachecker.util.symbpredabstraction.Model;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver;

import com.google.common.base.Preconditions;

public class MathsatTheoremProver implements TheoremProver {

  private final MathsatSymbolicFormulaManager mgr;
  private long curEnv;

  public MathsatTheoremProver(MathsatSymbolicFormulaManager pMgr) {
    mgr = pMgr;
    curEnv = 0;
  }

  @Override
  public boolean isUnsat(SymbolicFormula f) {
    push(f);
    int res = msat_solve(curEnv);
    pop();
    assert(res != MSAT_UNKNOWN);
    return res == MSAT_UNSAT;
  }
  
  @Override
  public Model getModel() {
    Preconditions.checkState(curEnv != 0);
    
    return MathsatModel.createMathsatModel(curEnv);
  }

  @Override
  public void pop() {
    Preconditions.checkState(curEnv != 0);
    int ok = msat_pop_backtrack_point(curEnv);
    assert(ok == 0);
  }

  @Override
  public void push(SymbolicFormula f) {
    Preconditions.checkState(curEnv != 0);
    msat_push_backtrack_point(curEnv);
    msat_assert_formula(curEnv, getTerm(f));
  }

  @Override
  public void init() {
    Preconditions.checkState(curEnv == 0);

    curEnv = mgr.createEnvironment(true, true);
  }
  
  @Override
  public void reset() {
    Preconditions.checkState(curEnv != 0);
    msat_destroy_env(curEnv);
    curEnv = 0;
  }
  
  @Override
  public AllSatResult allSat(SymbolicFormula f, Collection<SymbolicFormula> important, 
                             FormulaManager fmgr, AbstractFormulaManager amgr) {
    long formula = getTerm(f);
    
    long allsatEnv = mgr.createEnvironment(true, true);
    
    long[] imp = new long[important.size()];
    int i = 0;
    for (SymbolicFormula impF : important) {
      imp[i++] = getTerm(impF);
    }
    MathsatAllSatCallback callback = new MathsatAllSatCallback(fmgr, amgr);
    msat_assert_formula(allsatEnv, formula);
    int numModels = msat_all_sat(allsatEnv, imp, callback);
    
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
   * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
   */
  static class MathsatAllSatCallback implements mathsat.AllSatModelCallback, TheoremProver.AllSatResult {
    private final FormulaManager fmgr;
    private final AbstractFormulaManager amgr;
    
    private long totalTime = 0;
    private int count = 0;

    private AbstractFormula formula;
    private final Deque<AbstractFormula> cubes = new ArrayDeque<AbstractFormula>();

    MathsatAllSatCallback(FormulaManager fmgr, AbstractFormulaManager amgr) {
      this.fmgr = fmgr;
      this.amgr = amgr;
      this.formula = amgr.makeFalse();
    }

    void setInfiniteNumberOfModels() {
      count = Integer.MAX_VALUE;
      cubes.clear();
      formula = amgr.makeTrue();
    }
    
    @Override
    public long getTotalTime() {
      return totalTime;
    }

    @Override
    public int getCount() {
      return count;
    }

    @Override
    public AbstractFormula getResult() {
      if (cubes.size() > 0) {
        buildBalancedOr();
      }
      return formula;
    }

    private void buildBalancedOr() {
      cubes.add(formula);
      while (cubes.size() > 1) {
        AbstractFormula b1 = cubes.remove();
        AbstractFormula b2 = cubes.remove();
        cubes.add(amgr.makeOr(b1, b2));
      }
      assert(cubes.size() == 1);
      formula = cubes.remove();
    }

    @Override
    public void callback(long[] model) {
      long start = System.currentTimeMillis();

      // the abstraction is created simply by taking the disjunction
      // of all the models found by msat_all_sat, and storing them
      // in a BDD
      // first, let's create the BDD corresponding to the model
      Deque<AbstractFormula> curCube = new ArrayDeque<AbstractFormula>(model.length + 1);
      AbstractFormula m = amgr.makeTrue();
      for (long t : model) {
        AbstractFormula v;
        if (msat_term_is_not(t) != 0) {
          t = msat_term_get_arg(t, 0);
          v = fmgr.getPredicate(encapsulate(t)).getAbstractVariable();
          v = amgr.makeNot(v);
        } else {
          v = fmgr.getPredicate(encapsulate(t)).getAbstractVariable();
        }
        curCube.add(v);
      }
      // now, add the model to the bdd
      curCube.add(m);
      while (curCube.size() > 1) {
        AbstractFormula v1 = curCube.remove();
        AbstractFormula v2 = curCube.remove();
        curCube.add(amgr.makeAnd(v1, v2));
      }
      assert(curCube.size() == 1);
      m = curCube.remove();
      cubes.add(m);

      count++;

      long end = System.currentTimeMillis();
      totalTime += (end - start);
    }
  }
}
