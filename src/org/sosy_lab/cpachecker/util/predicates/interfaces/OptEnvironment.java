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

import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.rationals.Rational;

public interface OptEnvironment extends AutoCloseable {

  /**
   * Add constraint to the context.
   */
  void addConstraint(BooleanFormula constraint);

  /**
   * Add maximization {@param objective}.
   *
   * <b>Note: currently only one constraint is supported</b>
   */
  void maximize(Formula objective);

  /**
   * Add minimization {@param objective}.
   *
   * <b>Note: currently only one constraint is supported</b>
   */
  void minimize(Formula objective);

  /**
   * Optimize the objective function subject to the previously
   * imposed constraints.
   *
   * @return Status of the optimization problem.
   */
  OptStatus check() throws InterruptedException, SolverException;

  /**
   * @return Upper approximation of the optimized value.
   */
  Rational upper();

  /**
   * @return Lower approximation of the optimized value.
   */
  Rational lower();

  /**
   * @return Value of the approximation objective:
   * equivalent to {@link #upper()} for the maximization problem
   * and {@link #lower()} for the minimization problem.
   */
  Rational value();

  Model getModel() throws SolverException;

  /**
   * Status of the optimization problem.
   */
  public enum OptStatus {
    OPT, // All good, the solution was found.
    UNSAT,  // SMT problem is unsatisfiable.
    UNDEF, // The result is unknown.
    UNBOUNDED // The optimization problem is unbounded.
  }

  @Override
  void close();
}
