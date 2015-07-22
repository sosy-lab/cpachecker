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
import java.util.List;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;

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
    assertedTerms.remove(assertedTerms.size() - 1); // remove last term
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
  public <T> T allSat(ProverEnvironment.AllSatCallback<T> callback,
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
}
