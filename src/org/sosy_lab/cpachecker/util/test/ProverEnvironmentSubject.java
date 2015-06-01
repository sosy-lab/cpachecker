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

import java.util.List;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BasicProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;

import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.TestVerb;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * {@link Subject} subclass for testing assertions about ProverEnvironments with Truth
 * (allows to use <code>assert_().about(...).that(stack).isUnsatisfiable()</code> etc.).
 *
 * Use {@link SolverBasedTest0#ProverEnvironment()}
 * when calling {@link TestVerb#about(com.google.common.truth.SubjectFactory)}..
 */
@SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
public class ProverEnvironmentSubject extends Subject<ProverEnvironmentSubject, BasicProverEnvironment<?>> {

  ProverEnvironmentSubject(FailureStrategy pFailureStrategy, BasicProverEnvironment<?> pStack) {
    super(pFailureStrategy, pStack);
  }

  /**
   * Check that the subject stack is unsatisfiable.
   * Will show a model (satisfying assignment) on failure.
   */
  public void isUnsatisfiable() throws SolverException, InterruptedException {
    if (getSubject().isUnsat()) {
      return; // success
    }

    // get model for failure message
    final Model model = getSubject().getModel();
    if (model.isEmpty()) {
      fail("is", "unsatisfiable");
    } else {
      failWithBadResults("is", "unsatisfiable", "has counterexample", model);
    }
  }

  /**
   * Check that the subject stack is satisfiable.
   * Will show an unsat core on failure.
   */
  public void isSatisfiable() throws SolverException, InterruptedException {
    if (!getSubject().isUnsat()) {
      return; // success
    }

    // get unsat core for failure message if possible
    if (getSubject() instanceof ProverEnvironment) {
      final List<BooleanFormula> unsatCore = ((ProverEnvironment)getSubject()).getUnsatCore();
      if (!unsatCore.isEmpty()) {
        failWithBadResults("is", "satisfiable", "has unsat core", unsatCore);
        return;
      }
    }
    fail("is", "satisfiable");
  }
}