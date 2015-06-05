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

import java.util.List;

import org.sosy_lab.cpachecker.exceptions.SolverException;

/**
 * This class provides an interface to an incremental SMT solver
 * with methods for pushing and popping formulas as well as sat checks.
 * Instances of this class can be used once for a series of related queries.
 * After that, the {@link #close} method should be called
 * (preferably using the try-with-resources syntax).
 * All methods are expected to throw {@link IllegalStateException}s after
 * close was called.
 */
public interface ProverEnvironment extends BasicProverEnvironment<Void> {

  /**
   * Get an unsat core.
   * This should be called only immediately after an {@link #isUnsat()} call that returned <code>false</code>.
   */
  List<BooleanFormula> getUnsatCore();

  /**
   * Get all satisfying assignments of the current environment with regards
   * to a subset of terms,
   * and create a region representing all those models.
   *
   * @param important A set of variables appearing in f. Only these variables will appear in the region.
   * @return A region representing all satisfying models of the formula.
   * @throws InterruptedException
   */
  <T> T allSat(
      AllSatCallback<T> callback,
      List<BooleanFormula> important)
      throws InterruptedException, SolverException;

  interface AllSatCallback<T> {
    void apply(List<BooleanFormula> model);
    T getResult() throws InterruptedException;
  }
}
