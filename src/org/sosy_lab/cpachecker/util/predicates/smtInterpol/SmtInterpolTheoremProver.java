/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.smtInterpol;
import static com.google.common.base.Preconditions.*;
import static org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolUtil.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.sosy_lab.common.NestedTimer;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.core.Model;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.base.Preconditions;

import de.uni_freiburg.informatik.ultimate.logic.Term;

class SmtInterpolTheoremProver implements ProverEnvironment {

  private final SmtInterpolFormulaManager mgr;
  private SmtInterpolEnvironment env;
  private final List<Term> assertedTerms;

  SmtInterpolTheoremProver(SmtInterpolFormulaManager pMgr) {
    this.mgr = pMgr;

    assertedTerms = new ArrayList<>();
    env = mgr.createEnvironment();
    checkNotNull(env);
  }

  @Override
  public boolean isUnsat() {
    return !env.checkSat();
  }

  @Override
  public Model getModel() {
    Preconditions.checkNotNull(env);
    return SmtInterpolModel.createSmtInterpolModel(mgr, assertedTerms);
  }

  @Override
  public void pop() {
    Preconditions.checkNotNull(env);
    assertedTerms.remove(assertedTerms.size()-1); // remove last term
    env.pop(1);
  }

  @Override
  public void push(BooleanFormula f) {
    Preconditions.checkNotNull(env);
    final Term t = mgr.getTerm(f);
    assertedTerms.add(t);
    env.push(1);
    env.assertTerm(t);
  }

  @Override
  public void close() {
    Preconditions.checkNotNull(env);
    env.pop(assertedTerms.size());
    assertedTerms.clear();
    env = null;
  }

  @Override
  public AllSatResult allSat(Collection<BooleanFormula> formulas,
                             RegionCreator rmgr, Timer solveTime, NestedTimer enumTime) {
    checkNotNull(rmgr);
    checkNotNull(solveTime);
    checkNotNull(enumTime);
    checkArgument(!formulas.isEmpty());

    SmtInterpolEnvironment allsatEnv = env;
    checkNotNull(allsatEnv);

    // create new allSatResult
    SmtInterpolAllSatCallback result = new SmtInterpolAllSatCallback(rmgr, solveTime, enumTime);

    // unpack formulas to terms
    Term[] importantTerms = new Term[formulas.size()];
    int i = 0;
    for (BooleanFormula impF : formulas) {
      importantTerms[i++] = mgr.getTerm(impF);
    }

    solveTime.start();
    allsatEnv.push(1);
    for (Term[] model : allsatEnv.checkAllSat(importantTerms)) {
      result.callback(model);
    }
    allsatEnv.pop(1);

    if (solveTime.isRunning()) {
      solveTime.stop();
    } else {
      enumTime.stopOuter();
    }

    return result;
  }

  /**
   * callback used to build the predicate abstraction of a formula
   */
  class SmtInterpolAllSatCallback implements AllSatResult {
    private final RegionCreator rmgr;

    private final Timer solveTime;
    private final NestedTimer enumTime;
    private Timer regionTime = null;

    private int count = 0;

    private Region formula;
    private final Deque<Region> cubes = new ArrayDeque<>();

    public SmtInterpolAllSatCallback(RegionCreator rmgr, Timer pSolveTime, NestedTimer pEnumTime) {
      this.rmgr = rmgr;
      this.formula = rmgr.makeFalse();
      this.solveTime = pSolveTime;
      this.enumTime = pEnumTime;
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
      assert (cubes.size() == 1);
      formula = cubes.remove();
    }

    public void callback(Term[] model) {
      if (count == 0) {
        solveTime.stop();
        enumTime.startOuter();
        regionTime = enumTime.getInnerTimer();
      }

      regionTime.start();

      // the abstraction is created simply by taking the disjunction
      // of all the models found by msat_all_sat, and storing them in a BDD
      // first, let's create the BDD corresponding to the model
      Deque<Region> curCube = new ArrayDeque<>(model.length + 1);
      Region m = rmgr.makeTrue();
      for (Term t : model) {
        Region region;
        if (isNot(t)) {
          t = getArg(t, 0);
          region = rmgr.getPredicate(encapsulate(t));
          region = rmgr.makeNot(region);
        } else {
          region = rmgr.getPredicate(encapsulate(t));
        }
        curCube.add(region);
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

    private BooleanFormula encapsulate(Term pT) {
      return mgr.encapsulateBooleanFormula(pT);
    }
  }
}
