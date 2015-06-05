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

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;

import com.google.common.base.Preconditions;

import de.uni_freiburg.informatik.ultimate.logic.Term;

class SmtInterpolTheoremProver implements ProverEnvironment {

  private final SmtInterpolFormulaManager mgr;
  private SmtInterpolEnvironment env;
  private final List<Term> assertedTerms;

  SmtInterpolTheoremProver(SmtInterpolFormulaManager pMgr) {
    mgr = pMgr;
    assertedTerms = new ArrayList<>();
    env = mgr.createEnvironment();
    checkNotNull(env);
  }

  @Override
  public boolean isUnsat() throws InterruptedException {
    return !env.checkSat();
  }

  @Override
  public Model getModel() {
    Preconditions.checkNotNull(env);
    return SmtInterpolModel.createSmtInterpolModel(env, assertedTerms);
  }

  @Override
  public void pop() {
    Preconditions.checkNotNull(env);
    assertedTerms.remove(assertedTerms.size() - 1); // remove last term
    env.pop(1);
  }

  @Override
  public Void push(BooleanFormula f) {
    Preconditions.checkNotNull(env);
    final Term t = mgr.extractInfo(f);
    assertedTerms.add(t);
    env.push(1);
    env.assertTerm(t);
    return null;
  }

  @Override
  public void close() {
    Preconditions.checkNotNull(env);
    if (!assertedTerms.isEmpty()) {
      env.pop(assertedTerms.size());
      assertedTerms.clear();
    }
    env = null;
  }

  @Override
  public List<BooleanFormula> getUnsatCore() {
    Preconditions.checkNotNull(env);
    Term[] terms = env.getUnsatCore();
    List<BooleanFormula> result = new ArrayList<>(terms.length);
    for (Term t : terms) {
      result.add(mgr.encapsulateBooleanFormula(t));
    }
    return result;
  }


  @Override
  public <T> T allSat(AllSatCallback<T> callback,
      List<BooleanFormula> important)
      throws InterruptedException, SolverException {
    Term[] importantTerms = new Term[important.size()];
    int i = 0;
    for (BooleanFormula impF : important) {
      importantTerms[i++] = mgr.extractInfo(impF);
    }
    for (Term[] model : env.checkAllSat(importantTerms)) {
      // todo: improve efficiency.
      List<BooleanFormula> l = new ArrayList<>();
      for (Term t : model) {
        l.add(mgr.encapsulateBooleanFormula(t));
      }
      callback.apply(l);
    }
    return callback.getResult();
  }
}
