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
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolUtil.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.sosy_lab.common.NestedTimer;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaManager;

import com.google.common.base.Preconditions;

import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.Valuation;

public class SmtInterpolTheoremProver implements TheoremProver {

  private final SmtInterpolFormulaManager mgr;
  private SmtInterpolEnvironment env;
  private List<Term> assertedTerms;

  public SmtInterpolTheoremProver(
      SmtInterpolFormulaManager pMgr) {
    this.mgr = pMgr;
    env = null;
  }

  @Override
  public boolean isUnsat() {
    return env.checkSat() == LBool.UNSAT;
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
    final Term t = AbstractFormulaManager.<Term>getTerm(f);
    assertedTerms.add(t);
    env.push(1);
    env.assertTerm(t);
  }

  @Override
  public void init() {
    Preconditions.checkNotNull(mgr);
    assert (env == null);
    assertedTerms = new ArrayList<>();
    env = mgr.createEnvironment();
  }

  @Override
  public void reset() {
    Preconditions.checkNotNull(env);
    while (assertedTerms.size() > 0) { // cleanup stack
      pop();
    }
    env = null;
  }

  @Override
  public AllSatResult allSat(BooleanFormula f, Collection<BooleanFormula> formulas,
                             RegionCreator rmgr, Timer solveTime, NestedTimer enumTime) {
    checkNotNull(rmgr);
    checkNotNull(solveTime);
    checkNotNull(enumTime);

    SmtInterpolEnvironment allsatEnv = mgr.createEnvironment();
    checkNotNull(allsatEnv);

    // create new allSatResult
    SmtInterpolAllSatCallback result = new SmtInterpolAllSatCallback(rmgr, solveTime, enumTime);

    allsatEnv.push(1);

    // unpack formulas to terms
    Term[] importantTerms = new Term[formulas.size()];
    int i = 0;
    for (BooleanFormula impF : formulas) {
      importantTerms[i++] = AbstractFormulaManager.<Term>getTerm(impF);
    }

    solveTime.start();
    int numModels = 0;
    allsatEnv.assertTerm(AbstractFormulaManager.<Term>getTerm(f));
    while (allsatEnv.checkSat() == LBool.SAT) {
      Term[] model = new Term[importantTerms.length];

      if (importantTerms.length == 0) {
        // assert current model to get next model
        result.callback(model);
        throw new IllegalStateException("SMTInterpol could not compute model for satisfiable formula");
      }

      assert importantTerms.length != 0 : "there is no valuation for zero important terms!";

      Valuation val = allsatEnv.getValue(importantTerms);
      for (int j = 0; j < importantTerms.length; j++) {
        Term valueOfT = val.get(importantTerms[j]);
        if (SmtInterpolUtil.isFalse(valueOfT)) {
          model[j] = allsatEnv.term("not", importantTerms[j]);
        } else {
          model[j] = importantTerms[j];
        }
      }
      // add model to BDD
      result.callback(model);

      Term notTerm;
      if (model.length == 1) { // AND needs 2 or more terms
        notTerm = allsatEnv.term("not", model[0]);
      } else {
        notTerm = allsatEnv.term("not", allsatEnv.term("and", model));
      }

      numModels++;
      allsatEnv.push(1);
      allsatEnv.assertTerm(notTerm);
    }

    if (solveTime.isRunning()) {
      solveTime.stop();
    } else {
      enumTime.stopOuter();
    }

    allsatEnv.pop(numModels + 1); // we pushed some levels on assertionStack, remove them
    return result;
  }

  /**
   * callback used to build the predicate abstraction of a formula
   */
  class SmtInterpolAllSatCallback implements TheoremProver.AllSatResult {
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

/*
     public void setInfiniteNumberOfModels() {
      count = Integer.MAX_VALUE;
      cubes.clear();
      formula = rmgr.makeTrue();
    }
*/

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

    public void callback(Term[] model) { // TODO function needed for smtInterpol???
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
          region = rmgr.getPredicate( encapsulate(t));
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
      assert(curCube.size() == 1);
      m = curCube.remove();
      cubes.add(m);

      count++;

      regionTime.stop();
    }

    private BooleanFormula encapsulate(Term pT) {
      return mgr.encapsulate(BooleanFormula.class, pT);
    }
  }
}
