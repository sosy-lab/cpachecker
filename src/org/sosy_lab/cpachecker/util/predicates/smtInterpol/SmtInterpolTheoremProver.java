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

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.base.Preconditions;

import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.Valuation;

public class SmtInterpolTheoremProver implements TheoremProver {

  private final SmtInterpolFormulaManager mgr;
  private SmtInterpolEnvironment env;
  private List<Term> assertedTerms;

  public SmtInterpolTheoremProver(SmtInterpolFormulaManager pMgr) {
    mgr = pMgr;
    env = null;
  }

  @Override
  public boolean isUnsat() {
    return env.checkSat() == LBool.UNSAT;
  }

  @Override
  public boolean isUnsat(Formula f) {
    push(f);
    try {
      return isUnsat();
    } finally {
      pop();
    }
  }

  @Override
  public Model getModel() {
    Preconditions.checkNotNull(env);
    return SmtInterpolModel.createSmtInterpolModel(env, assertedTerms);
  }

  @Override
  public void pop() {
    Preconditions.checkNotNull(env);
    assertedTerms.remove(assertedTerms.size()-1); // remove last term
    env.pop(1);
  }

  @Override
  public void push(Formula f) {
    Preconditions.checkNotNull(env);
    final Term t = mgr.getTerm(f);
    assertedTerms.add(t);
    env.push(1);
    env.assertTerm(t);
  }

  @Override
  public void init() {
    Preconditions.checkNotNull(mgr);
    assert (env == null);
    assertedTerms = new ArrayList<Term>();
    env = mgr.getEnvironment();
  }

  @Override
  public void reset() {
    Preconditions.checkNotNull(env);
    env = null;
  }

  @Override
  public AllSatResult allSat(Formula f, Collection<Formula> formulas,
                             AbstractionManager amgr, Timer timer) {
    checkNotNull(amgr);
    checkNotNull(timer);

    SmtInterpolEnvironment allsatEnv = mgr.getEnvironment(); //TODO do we need a new environment?
    checkNotNull(allsatEnv);

    // create new allSatResult
    SmtInterpolAllSatCallback result = new SmtInterpolAllSatCallback(amgr, timer);

    allsatEnv.push(1);

    // unpack formulas to terms
    Term[] importantTerms = new Term[formulas.size()];
    int i = 0;
    for (Formula impF : formulas) {
      importantTerms[i++] = mgr.getTerm(impF);
    }

    int numModels = 0;
    allsatEnv.assertTerm(mgr.getTerm(f));
    while (allsatEnv.checkSat() == LBool.SAT) {
      if (numModels > 20) { // TODO remove limit, TODO handle infinity
        System.out.println("i have found some models, making break in allsat()");
        break;
      }

      Term[] model = new Term[importantTerms.length];

      if (importantTerms.length == 0) {
        // assert current model to get next model
        result.callback(model, allsatEnv);

        System.out.println(
            "satCheck is SAT, but there is no model for important terms!");
        break;
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
      result.callback(model, allsatEnv);

      Term notTerm;
      if (model.length == 1) { // AND needs 2 or more terms
        notTerm = allsatEnv.term("not", model[0]);
      } else {
        notTerm = allsatEnv.term("not", allsatEnv.term("and", model));
      }
      System.out.println(numModels + ", term to assert for next model: " + notTerm.toString());
      numModels++;
      allsatEnv.push(1);
      allsatEnv.assertTerm(notTerm);
    }

    allsatEnv.pop(numModels + 1); // we pushed some levels on assertionStack, remove them
    return result;
  }

  /**
   * callback used to build the predicate abstraction of a formula
   */
  class SmtInterpolAllSatCallback implements TheoremProver.AllSatResult {
    private final AbstractionManager amgr;
    private final RegionManager rmgr;

    private final Timer totalTime;

    private int count = 0;

    private Region formula;
    private final Deque<Region> cubes = new ArrayDeque<Region>();

    public SmtInterpolAllSatCallback(AbstractionManager fmgr, Timer timer) {
      this.amgr = fmgr;
      this.rmgr = fmgr.getRegionManager();
      this.formula = rmgr.makeFalse();
      this.totalTime = timer;
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

    public void callback(Term[] model, SmtInterpolEnvironment env) { // TODO function needed for smtInterpol???
      totalTime.start();

      // the abstraction is created simply by taking the disjunction
      // of all the models found by msat_all_sat, and storing them in a BDD
      // first, let's create the BDD corresponding to the model
      Deque<Region> curCube = new ArrayDeque<Region>(model.length + 1);
      Region m = rmgr.makeTrue();
      for (Term t : model) {
        Region region;
        if (isNot(t)) {
          t = getArg(t, 0);
          region = amgr.getPredicate(mgr.encapsulate(t)).getAbstractVariable();
          region = rmgr.makeNot(region);
        } else {
          region = amgr.getPredicate(mgr.encapsulate(t)).getAbstractVariable();
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

      totalTime.stop();
    }
  }
}
