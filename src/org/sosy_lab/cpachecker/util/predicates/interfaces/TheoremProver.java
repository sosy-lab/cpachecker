/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import org.sosy_lab.common.NestedTimer;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.Model;

public interface TheoremProver {

  void init();
  void push(BooleanFormula f);
  void pop();
  boolean isUnsat();
  Model getModel() throws SolverException;
  void reset();

  /**
   * Check a formula for satisfiability,
   * and create a region representing all satisfying models of the formula.
   *
   * @param f The formula to check.
   * @param important A set of variables appearing in f. Only these variables will appear in the region.
   * @param mgr The object used for creating regions.
   * @param solveTime A timer to use for time which the solver needs for finding out whether the formula is satisfiable (without enumerating all the models).
   * @param regionTime A NestedTimer to use for timing model enumeration (outer: solver; inner: region creation).
   * @return A region representing all satisfying models of the formula.
   */
  AllSatResult allSat(BooleanFormula f, Collection<BooleanFormula> important,
                      RegionCreator mgr, Timer solveTime, NestedTimer enumTime);

  interface AllSatResult {

    /**
     * The result of an allSat call as an abstract formula.
     */
    public Region getResult();

    /**
     * The number of satisfying assignments contained in the result, of
     * {@link Integer#MAX_VALUE} if this number is infinite.
     */
    public int getCount();
  }
}
