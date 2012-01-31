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
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.base.Preconditions;

import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.Valuation;

public class SmtInterpolTheoremProver implements TheoremProver {

  private final SmtInterpolFormulaManager mgr;
  private Script script;

  public SmtInterpolTheoremProver(SmtInterpolFormulaManager pMgr) {
    mgr = pMgr;
    script = null;
  }

  @Override
  public boolean isUnsat() {
    LBool res = script.checkSat();
    assert(res != LBool.UNKNOWN);
    return res == LBool.UNSAT;
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
    Preconditions.checkNotNull(script);
    return SmtInterpolModel.createSmtInterpolModel(script, mgr);
  }

  @Override
  public void pop() {
    Preconditions.checkNotNull(script);
    try {
      script.pop(1);
    } catch (SMTLIBException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void push(Formula f) {
    Preconditions.checkNotNull(script);
    script.push(1);
    try {
      script.assertTerm(mgr.getTerm(f));
    } catch (SMTLIBException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void init() {
    Preconditions.checkNotNull(mgr);
    assert (script == null);
    script = mgr.createEnvironment();
    script.push(1); // TODO necessary?
  }

  @Override
  public void reset() {
    Preconditions.checkNotNull(script);

    try { // TODO correct?
      script.pop(1);
    } catch (SMTLIBException e) {
      e.printStackTrace();
    }

    script = null;
  }

  @Override
  public AllSatResult allSat(Formula f, Collection<Formula> important,
                             AbstractionManager amgr, Timer timer) {
    checkNotNull(amgr);
    checkNotNull(timer);
    Term t = mgr.getTerm(f);

    Script allsatEnv = mgr.createEnvironment(); //TODO do we need a new environment?
    checkNotNull(allsatEnv);

    // unpack formulas to terms
    Term[] imp = new Term[important.size()];
    int i = 0;
    for (Formula impF : important) {
      imp[i++] = mgr.getTerm(impF);
    }

    // create new allSatResult
    SmtInterpolAllSatCallback result = new SmtInterpolAllSatCallback(amgr, timer);
    try {
      allsatEnv.assertTerm(t);

      int numModels = 0;
      while(allsatEnv.checkSat() == LBool.SAT) {
        numModels++;
        Valuation val = allsatEnv.getValue(imp);

        System.out.println(numModels);
        System.out.println("Val: " + val);

        Term[] model = new Term[imp.length];
        for (int j=0; j<imp.length; j++) {
          Term valueOfT = val.get(imp[j]);

          Term termForModel;
          if (allsatEnv.term("false") == valueOfT) {
            termForModel = imp[j];
          } else {
            termForModel = allsatEnv.term("not", imp[j]);
          }
          model[j] = termForModel;
          allsatEnv.assertTerm(termForModel);

          Term[] assertions = allsatEnv.getAssertions();
          System.out.println("termForModel: " + termForModel);
          System.out.println("Assertions: " + Arrays.toString(assertions)
              .replace(", ", ",\n            "));

        }

        // add model to BDD
        result.callback(model, allsatEnv);

        if (numModels > 100 || val == null) { // TODO remove limit 100
          System.out.println("i have fount 100 models, break");
          break;
        }
      }

    } catch (UnsupportedOperationException e) {
      e.printStackTrace();
    } catch (SMTLIBException e) {
      e.printStackTrace();
    }

    //allSat(allsatEnv, imp, null);
    // TODO implement something for handling "numModels"
    // ?? allsatEnv.reset();

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

    public void callback(Term[] model, Script script) { // TODO function needed for smtInterpol???
      totalTime.start();

      // the abstraction is created simply by taking the disjunction
      // of all the models found by msat_all_sat, and storing them in a BDD
      // first, let's create the BDD corresponding to the model
      Deque<Region> curCube = new ArrayDeque<Region>(model.length + 1);
      Region m = rmgr.makeTrue();
      int i =0; System.out.println();
      for (Term t : model) {
        System.out.println("model " + (i++) + "  " + t.toString());

        Region region;
        if (isNot(script, t)) {
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
