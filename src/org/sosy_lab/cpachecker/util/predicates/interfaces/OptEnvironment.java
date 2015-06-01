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
package org.sosy_lab.cpachecker.util.predicates.interfaces;

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Model;

import com.google.common.base.Optional;

public interface OptEnvironment extends AutoCloseable {

  /**
   * Add constraint to the context.
   */
  void addConstraint(BooleanFormula constraint);

  /**
   * Add the maximization <code>objective</code>.
   *
   * <b>Note: {@code push/pop} may be used for switching objectives</b>
   *
   * @return Objective handle, to be used for retrieving the value.
   */
  int maximize(Formula objective);

  /**
   * Add minimization <code>objective</code>.
   *
   * <b>Note: {@code push/pop} may be used for switching objectives</b>
   *
   * @return Objective handle, to be used for retrieving the value.
   */
  int minimize(Formula objective);

  /**
   * Optimize the objective function subject to the previously
   * imposed constraints.
   *
   * @return Status of the optimization problem.
   */
  OptStatus check() throws InterruptedException, SolverException;

  /**
   * Create backtracking point.
   */
  void push();

  /**
   * Backtrack one level.
   */
  void pop();

  /**
   * @param epsilon Value to substitute for the {@code epsilon}.
   * @return Upper approximation of the optimized value, or
   *  absent optional if the objective is unbounded.
   */
  Optional<Rational> upper(int handle, Rational epsilon);

  /**
   * @param epsilon Value to substitute for the {@code epsilon}.
   * @return Lower approximation of the optimized value, or
   *  absent optional if the objective is unbounded.
   */
  Optional<Rational> lower(int handle, Rational epsilon);

  Model getModel() throws SolverException;

  /**
   * Evaluate the formula with the previously generated model.
   * Assumes that the previous call was {@link #getModel}.
   */
  Formula evaluate(Formula f);

  /**
   * Status of the optimization problem.
   */
  enum OptStatus {
    OPT, // All good, the solution was found (may be unbounded).
    UNSAT,  // SMT problem is unsatisfiable.
    UNDEF // The result is unknown.
  }

  @Override
  void close();
}
