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
package org.sosy_lab.cpachecker.util.test;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;

import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.TestVerb;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * {@link Subject} subclass for testing assertions about BooleanFormulas with Truth
 * (allows to use <code>assert_().about(...).that(formula).isUnsatisfiable()</code> etc.).
 *
 * Use {@link SolverBasedTest0#BooleanFormula()}
 * or {@link SolverBasedTest0#BooleanFormulaOfSolver(FormulaManagerFactory)}
 * when calling {@link TestVerb#about(com.google.common.truth.SubjectFactory)}..
 */
@SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
public class BooleanFormulaSubject extends Subject<BooleanFormulaSubject, BooleanFormula> {

  private final FormulaManager mgr;

  BooleanFormulaSubject(FailureStrategy pFailureStrategy,
      BooleanFormula pFormula, FormulaManager pMgr) {
    super(pFailureStrategy, pFormula);
    mgr = checkNotNull(pMgr);
  }

  private void checkIsUnsat(final BooleanFormula subject, final String verb, final Object expected)
      throws SolverException, InterruptedException {
    try (final ProverEnvironment prover = mgr.newProverEnvironment(true, false)) {
      prover.push(subject);
      if (prover.isUnsat()) {
        return; // success
      }

      // get model for failure message
      final Model model = prover.getModel();
      if (model.isEmpty()) {
        fail(verb, expected);
      } else {
        failWithBadResults(verb, expected, "has counterexample", model);
      }
    }
  }

  /**
   * Check that the subject is unsatisfiable.
   * Will show a model (satisfying assignment) on failure.
   */
  public void isUnsatisfiable() throws SolverException, InterruptedException {
    if (mgr.getBooleanFormulaManager().isTrue(getSubject())) {
      failWithBadResults("is", "unsatisfiable", "is", "trivially satisfiable");
    }

    checkIsUnsat(getSubject(), "is", "unsatisfiable");
  }

  /**
   * Check that the subject is satisfiable.
   * Will show an unsat core on failure.
   */
  public void isSatisfiable() throws SolverException, InterruptedException {
    if (mgr.getBooleanFormulaManager().isFalse(getSubject())) {
      failWithBadResults("is", "satisfiable", "is", "trivially unsatisfiable");
    }

    try (ProverEnvironment prover = mgr.newProverEnvironment(false, true)) {
      prover.push(getSubject());
      if (!prover.isUnsat()) {
        return; // success
      }

      // get unsat core for failure message
      final List<BooleanFormula> unsatCore = prover.getUnsatCore();
      if (unsatCore.isEmpty()
          || (unsatCore.size() == 1 && getSubject().equals(unsatCore.get(0)))) {
        // empty or trivial unsat core
        fail("is", "satisfiable");
      } else {
        failWithBadResults("is", "satisfiable", "has unsat core", unsatCore);
      }
    }
  }

  /**
   * Check that the subject is equivalent to a given formula,
   * i.e. <code>subject <=> expected</code> always holds.
   * Will show a counterexample on failure.
   */
  public void isEquivalentTo(final BooleanFormula expected)
      throws SolverException, InterruptedException {
    if (getSubject().equals(expected)) {
      return;
    }

    final BooleanFormula f = mgr.getBooleanFormulaManager()
        .xor(getSubject(), expected);

    checkIsUnsat(f, "is equivalent to", expected);
  }

  /**
   * Check that the subject implies a given formula,
   * i.e. <code>subject => expected</code> always holds.
   * Will show a counterexample on failure.
   */
  public void implies(final BooleanFormula expected)
      throws SolverException, InterruptedException {
    if (getSubject().equals(expected)) {
      return;
    }

    final BooleanFormulaManager bmgr = mgr.getBooleanFormulaManager();
    final BooleanFormula implication = bmgr.or(bmgr.not(getSubject()), expected);

    checkIsUnsat(bmgr.not(implication), "implies", expected);
  }
}