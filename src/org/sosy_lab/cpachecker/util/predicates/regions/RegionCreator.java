/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.regions;

import org.sosy_lab.common.ShutdownNotifier;

/**
 * RegionCreator is an interface that contains all methods for creating
 * {@link Region} instances.
 * It is a super-interface of {@link RegionManager} with a subset of methods
 * intended to be used for code that needs only limited access.
 */
public interface RegionCreator {

  /**
   * Return a new {@link RegionBuilder} instance.
   */
  RegionBuilder builder(ShutdownNotifier pShutdownNotifier);

  /**
   * @return a representation of logical truth
   */
  Region makeTrue();

  /**
   * @return a representation of logical falseness
   */
  Region makeFalse();

  /**
   * Creates a region representing a negation of the argument
   *
   * @param f an AbstractFormula
   * @return (!f1)
   */
  Region makeNot(Region f);

  /**
   * Creates a region representing an AND of the two argument
   *
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return (f1 & f2)
   */
  Region makeAnd(Region f1, Region f2);

  /**
   * Creates a region representing an OR of the two argument
   *
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return (f1 | f2)
   */
  Region makeOr(Region f1, Region f2);

  /**
   * Creates a region representing an equality (bi-implication) of the two argument
   *
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return (f1 <=> f2)
   */
  Region makeEqual(Region f1, Region f2);

  /**
   * Creates a region representing an disequality (XOR) of the two argument
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return (f1 ^ f2)
   */
  Region makeUnequal(Region f1, Region f2);

  /**
   * Creates a region representing an if then else construct of the three arguments
   *
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @param f3 an AbstractFormula
   * @return (if f1 then f2 else f3)
   */
  Region makeIte(Region f1, Region f2, Region f3);

  /**
   * Creates a region representing an existential quantification of the second
   * argument. If there are more arguments, each of them is quantified.
   * @param f1 an AbstractFormula
   * @param f2 one or more AbstractFormulas
   * @return (exists f2... : f1)
   */
  Region makeExists(Region f1, Region... f2);

  /**
   * A stateful region builder for regions that are disjunctions
   * of conjunctive literals.
   * Using this can be more efficient than calling {@link RegionCreator#makeOr(Region, Region)}
   * and {@link RegionCreator#makeAnd(Region, Region)} repeatedly.
   */
  public static interface RegionBuilder extends AutoCloseable {

    /**
     * Start a new conjunctive clause.
     */
    void startNewConjunction();

    /**
     * Add a region to the current conjunctive clause.
     */
    void addPositiveRegion(Region r);

    /**
     * Add the negation of a region to the current conjunctive clause.
     */
    void addNegativeRegion(Region r);

    /**
     * End the current conjunctive clause and add it to the global disjunction.
     */
    void finishConjunction();

    /**
     * Retrieve the disjunction of all the conjunctive clauses created
     * with this builder so far.
     */
    Region getResult() throws InterruptedException;

    @Override
    public void close();
  }
}