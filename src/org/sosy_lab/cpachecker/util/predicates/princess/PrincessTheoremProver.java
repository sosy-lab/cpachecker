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
package org.sosy_lab.cpachecker.util.predicates.princess;

import static com.google.common.base.Preconditions.*;
import static org.sosy_lab.cpachecker.util.predicates.princess.PrincessUtil.castToFormula;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.time.NestedTimer;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.AllSatResult;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager.RegionBuilder;

import scala.Option;
import ap.parser.IBinFormula;
import ap.parser.IBinJunctor;
import ap.parser.IBoolLit;
import ap.parser.IExpression;
import ap.parser.IFormula;
import ap.parser.INot;

class PrincessTheoremProver extends PrincessAbstractProver implements ProverEnvironment {

  private final List<IExpression> assertedTerms = new ArrayList<>();
  private final ShutdownNotifier shutdownNotifier;

  PrincessTheoremProver(PrincessFormulaManager pMgr, ShutdownNotifier pShutdownNotifier) {
    super(pMgr, false);
    this.shutdownNotifier = checkNotNull(pShutdownNotifier);
  }

  @Override
  public Model getModel() {
    return PrincessModel.createModel(stack, assertedTerms);
  }

  @Override
  public void pop() {
    assertedTerms.remove(assertedTerms.size()-1); // remove last term
    stack.pop(1);
  }

  @Override
  public Void push(BooleanFormula f) {
    final IFormula t = castToFormula(mgr.extractInfo(f));
    assertedTerms.add(t);
    stack.push(1);
    stack.assertTerm(t);
    return null;
  }

  @Override
  public List<BooleanFormula> getUnsatCore() {
    throw new UnsupportedOperationException();
  }

  @Override
  public AllSatResult allSat(Collection<BooleanFormula> formulas,
                             RegionCreator rmgr, Timer solveTime, NestedTimer enumTime) throws InterruptedException {
    checkNotNull(rmgr);
    checkNotNull(solveTime);
    checkNotNull(enumTime);
    checkArgument(!formulas.isEmpty());

    // create new allSatResult
    final AllSatCallback result = new AllSatCallback(rmgr, solveTime, enumTime);

    // unpack formulas to terms
    List<IFormula> importantFormulas = new ArrayList<>(formulas.size());
    for (BooleanFormula impF : formulas) {
      importantFormulas.add(castToFormula(mgr.extractInfo(impF)));
    }

    solveTime.start();
    try {
      stack.push(1);
      while (stack.checkSat()) {
        shutdownNotifier.shutdownIfNecessary();

        IFormula newFormula = new IBoolLit(true); // neutral element for AND
        final Map<IFormula, Boolean> partialModel = new HashMap<>();
        for (final IFormula f : importantFormulas) {
          final Option<Object> value = stack.evalPartial(f);
          if (value.isDefined()) {
            final Boolean isTrueValue = (Boolean)value.get();
            final IFormula newElement = isTrueValue ? f : new INot(f);
            newFormula = new IBinFormula(IBinJunctor.And(), newFormula, newElement);
            partialModel.put(f, isTrueValue);
          } else {
            // when does this happen? if formula was not asserted?
          }
        }

        result.callback(partialModel);

        // add negation of current formula to get a new model in next iteration
        stack.assertTerm(new INot(newFormula));
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

  @Override
  public <T> T allSat2(ProverEnvironment.AllSatCallback<T> callback,
      List<BooleanFormula> important)
      throws InterruptedException, SolverException {

    // unpack formulas to terms
    List<IFormula> importantFormulas = new ArrayList<>(important.size());
    for (BooleanFormula impF : important) {
      importantFormulas.add(castToFormula(mgr.extractInfo(impF)));
    }

    stack.push(1);
    while (stack.checkSat()) {
      shutdownNotifier.shutdownIfNecessary();

      IFormula newFormula = new IBoolLit(true); // neutral element for AND
      List<BooleanFormula> wrappedPartialModel = new ArrayList<>(important.size());
      for (final IFormula f : importantFormulas) {
        final Option<Object> value = stack.evalPartial(f);
        if (value.isDefined()) {
          final boolean isTrueValue = (boolean)value.get();
          final IFormula newElement = isTrueValue ? f : new INot(f);

          wrappedPartialModel.add(mgr.encapsulateBooleanFormula(newElement));
          newFormula = new IBinFormula(IBinJunctor.And(), newFormula, newElement);
        } else {
          // when does this happen? if formula was not asserted?
        }
      }
      callback.apply(wrappedPartialModel);

      // add negation of current formula to get a new model in next iteration
      stack.assertTerm(new INot(newFormula));
    }
    shutdownNotifier.shutdownIfNecessary();
    stack.pop(1);

    return callback.getResult();
  }

  /**
   * callback used to build the predicate abstraction of a formula
   */
  class AllSatCallback implements AllSatResult {
    private final RegionCreator rmgr;
    private final RegionBuilder builder;

    private final Timer solveTime;
    private final NestedTimer enumTime;
    private Timer regionTime = null;

    private int count = 0;

    private Region formula = null;

    public AllSatCallback(RegionCreator rmgr, Timer pSolveTime, NestedTimer pEnumTime) {
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

    public void callback(final Map<IFormula, Boolean> model) {
      if (count == 0) {
        solveTime.stop();
        enumTime.startOuter();
        regionTime = enumTime.getCurentInnerTimer();
      }

      regionTime.start();

      // the abstraction is created simply by taking the disjunction
      // of all the models found by all_sat, and storing them in a BDD
      // first, let's create the BDD corresponding to the model
      builder.startNewConjunction();

      for (Map.Entry<IFormula, Boolean> f : model.entrySet()) {
        if (f.getValue()) {
          builder.addPositiveRegion(rmgr.getPredicate(encapsulate(f.getKey())));
        } else {
          builder.addNegativeRegion(rmgr.getPredicate(encapsulate(f.getKey())));
        }
      }
      builder.finishConjunction();

      count++;

      regionTime.stop();
    }

    private BooleanFormula encapsulate(IFormula pT) {
      return mgr.encapsulateBooleanFormula(pT);
    }
  }
}
