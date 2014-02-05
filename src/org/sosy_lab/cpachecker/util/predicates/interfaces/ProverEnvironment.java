/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.Collection;

import org.sosy_lab.common.time.NestedTimer;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;

/**
 * This class provides an interface to an incremental SMT solver
 * with methods for pushing and popping formulas as well as sat checks.
 * Instances of this class can be used once for a series of related queries.
 * After that, the {@link #close} method should be called
 * (preferably using the try-with-resources syntax).
 * All methods are expected to throw {@link IllegalStateException}s after
 * close was called.
 */
public interface ProverEnvironment extends AutoCloseable {

  /**
   * Add a formula to the environment stack, asserting it.
   */
  void push(BooleanFormula f);

  /**
   * Remove one formula from the environment stack.
   */
  void pop();

  /**
   * Check whether the conjunction of all formulas on the stack is unsatisfiable.
   * @throws InterruptedException
   */
  boolean isUnsat() throws InterruptedException;

  /**
   * Get a satisfying assignment.
   * This should be called only immediately after an {@link #isUnsat()} call that returned <code>false</code>.
   */
  Model getModel() throws SolverException;

  /**
   * Get all satisfying assignments of the current environment with regards
   * to a subset of terms,
   * and create a region representing all those models.
   *
   * @param important A set of variables appearing in f. Only these variables will appear in the region.
   * @param mgr The object used for creating regions.
   * @param solveTime A timer to use for time which the solver needs for finding out whether the formula is satisfiable (without enumerating all the models).
   * @param regionTime A NestedTimer to use for timing model enumeration (outer: solver; inner: region creation).
   * @return A region representing all satisfying models of the formula.
   * @throws InterruptedException
   */
  AllSatResult allSat(Collection<BooleanFormula> important,
                      RegionCreator mgr, Timer solveTime, NestedTimer enumTime) throws InterruptedException;

  @Override
  void close();

  interface AllSatResult {

    /**
     * The result of an allSat call as an abstract formula.
     */
    public Region getResult() throws InterruptedException;

    /**
     * The number of satisfying assignments contained in the result, of
     * {@link Integer#MAX_VALUE} if this number is infinite.
     */
    public int getCount();
  }
}
