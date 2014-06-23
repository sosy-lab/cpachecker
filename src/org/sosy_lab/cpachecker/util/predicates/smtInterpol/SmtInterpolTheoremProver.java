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
package org.sosy_lab.cpachecker.util.predicates.smtInterpol;
import static com.google.common.base.Preconditions.*;
import static org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.time.NestedTimer;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager.RegionBuilder;

import de.uni_freiburg.informatik.ultimate.logic.Term;

class SmtInterpolTheoremProver extends SmtInterpolAbstractProver implements ProverEnvironment {

  private final List<Term> assertedTerms;

  SmtInterpolTheoremProver(SmtInterpolFormulaManager pMgr, ShutdownNotifier pShutdownNotifier) {
    super(pMgr, pShutdownNotifier);
    assertedTerms = new ArrayList<>();
  }

  @Override
  public OptResult isOpt(Formula f, boolean maximize) throws InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Model getModel() {
    return SmtInterpolModel.createSmtInterpolModel(stack, assertedTerms);
  }

  @Override
  public void pop() {
    assertedTerms.remove(assertedTerms.size()-1); // remove last term
    stack.pop(1);
    mgr.getEnvironment().pop(1);
  }

  @Override
  public void push(BooleanFormula f) {
    final Term t = mgr.getTerm(f);
    assertedTerms.add(t);
    stack.push(1);
    mgr.getEnvironment().push(1);
    stack.assertTerm(t);
  }

  @Override
  public void close() {
    if (!assertedTerms.isEmpty()) {
      stack.pop(assertedTerms.size());
      mgr.getEnvironment().pop(assertedTerms.size());
      assertedTerms.clear();
    }
    super.close();
  }

  @Override
  public List<BooleanFormula> getUnsatCore() {
    Term[] terms = stack.getUnsatCore();
    List<BooleanFormula> result = new ArrayList<>(terms.length);
    for (Term t : terms) {
      result.add(mgr.encapsulateBooleanFormula(t));
    }
    return result;
  }

  @Override
  public AllSatResult allSat(Collection<BooleanFormula> formulas,
                             RegionCreator rmgr, Timer solveTime, NestedTimer enumTime) throws InterruptedException {
    checkNotNull(rmgr);
    checkNotNull(solveTime);
    checkNotNull(enumTime);
    checkArgument(!formulas.isEmpty());

    // create new allSatResult
    SmtInterpolAllSatCallback result = new SmtInterpolAllSatCallback(rmgr, solveTime, enumTime);

    // unpack formulas to terms
    Term[] importantTerms = new Term[formulas.size()];
    int i = 0;
    for (BooleanFormula impF : formulas) {

      importantTerms[i++] = mgr.getTerm(impF);
    }

    solveTime.start();
    try {
      stack.push(1);

      // We actually terminate SmtInterpol during the analysis
      // by using a shutdown listener. However, SmtInterpol resets the
      // mStopEngine flag in DPLLEngine before starting to solve,
      // so we check here, too.
      shutdownNotifier.shutdownIfNecessary();
      for (Term[] model : stack.checkAllsat(importantTerms)) {
        shutdownNotifier.shutdownIfNecessary();
        result.callback(model);
      }
      shutdownNotifier.shutdownIfNecessary();
      stack.pop(1);

    } finally {
      if (solveTime.isRunning()) {
        solveTime.stop();
      } else {
        enumTime.stopOuter();
      }
    }

    return result;
  }

  /**
   * callback used to build the predicate abstraction of a formula
   */
  class SmtInterpolAllSatCallback implements AllSatResult {
    private final RegionCreator rmgr;
    private final RegionBuilder builder;

    private final Timer solveTime;
    private final NestedTimer enumTime;
    private Timer regionTime = null;

    private int count = 0;

    private Region formula = null;

    public SmtInterpolAllSatCallback(RegionCreator rmgr, Timer pSolveTime, NestedTimer pEnumTime) {
      this.rmgr = rmgr;
      this.solveTime = pSolveTime;
      this.enumTime = pEnumTime;
      builder = rmgr.newRegionBuilder(shutdownNotifier);
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

    public void callback(Term[] model) {
      if (count == 0) {
        solveTime.stop();
        enumTime.startOuter();
        regionTime = enumTime.getCurentInnerTimer();
      }

      regionTime.start();

      // the abstraction is created simply by taking the disjunction
      // of all the models found by msat_all_sat, and storing them in a BDD
      // first, let's create the BDD corresponding to the model
      builder.startNewConjunction();
      for (Term t : model) {
        if (isNot(t)) {
          t = getArg(t, 0);
          builder.addNegativeRegion(rmgr.getPredicate(encapsulate(t)));
        } else {
          builder.addPositiveRegion(rmgr.getPredicate(encapsulate(t)));
        }
      }
      builder.finishConjunction();

      count++;

      regionTime.stop();
    }

    private BooleanFormula encapsulate(Term pT) {
      return mgr.encapsulateBooleanFormula(pT);
    }
  }
}
