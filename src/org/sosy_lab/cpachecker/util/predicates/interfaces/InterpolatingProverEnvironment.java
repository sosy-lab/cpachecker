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

import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;

/**
 * This class provides an interface to an incremental SMT solver
 * with methods for pushing and popping formulas as well as sat checks.
 * Furthermore, interpolants can be generated for an unsatisfiable list of formulas.
 *
 * Instances of this class can be used once for a series of related queries.
 * After that, the {@link #close} method should be called
 * (preferably using the try-with-resources syntax).
 * All methods are expected to throw {@link IllegalStateException}s after
 * close was called.

 * @param <T> The type of the objects which can be used to select formulas for interpolant creation.
 */
public interface InterpolatingProverEnvironment<T> extends AutoCloseable {

  /**
   * Add a formula to the environment stack, asserting it.
   * The returned value can be used when selecting the formulas for interpolant generation.
   */
  T push(BooleanFormula f);

  /**
   * Remove one formula from the environment stack.
   */
  void pop();

  /**
   * Check whether the conjunction of all formulas on the stack is unsatisfiable.
   */
  boolean isUnsat() throws InterruptedException;

  /**
   * Get an interpolant for two groups of formulas.
   * This should be called only immediately after an {@link #isUnsat()} call that returned <code>true</code>.
   * @param formulasOfA A list of values returned by {@link #push(BooleanFormula)}. All the corresponding formulas from group A, the remaining formulas form group B.
   * @return An interpolant for A and B
   */
  BooleanFormula getInterpolant(List<T> formulasOfA);

  /**
   * Get a satisfying assignment.
   * This should be called only immediately after an {@link #isUnsat()} call that returned <code>false</code>.
   */
  Model getModel() throws SolverException;

  @Override
  void close();
}
