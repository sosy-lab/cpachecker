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

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Model;

/**
 * Super interface for {@link ProverEnvironment} and {@link InterpolatingProverEnvironment}
 * that provides only the common operations.
 * In most cases, just use one of the two sub-interfaces
 */
public interface BasicProverEnvironment<T> extends AutoCloseable {

  /**
   * Add a formula to the environment stack, asserting it.
   * The return value may be used to identify this formula later on
   * in a query (this depends on the sub-type of the environment).
   */
  T push(BooleanFormula f);

  /**
   * Remove one formula from the environment stack.
   */
  void pop();

  /**
   * Check whether the conjunction of all formulas on the stack is unsatisfiable.
   */
  boolean isUnsat() throws SolverException, InterruptedException;

  /**
   * Get a satisfying assignment.
   * This should be called only immediately after an {@link #isUnsat()} call that returned <code>false</code>.
   */
  Model getModel() throws SolverException;

  @Override
  void close();
}
