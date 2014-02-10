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

import java.io.PrintStream;
import java.util.Set;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Function;

/**
 * An AbstractFormulaManager is an object that knows how to create/manipulate
 * AbstractFormulas
 */
public interface RegionManager {

  /**
   * checks whether the data region represented by f1
   * is a subset of that represented by f2
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return true if (f1 => f2), false otherwise
   */
  public boolean entails(Region f1, Region f2) throws InterruptedException;

  /**
   * @return a representation of logical truth
   */
  public Region makeTrue();

  /**
   * @return a representation of logical falseness
   */
  public Region makeFalse();

  /**
   * Creates a region representing a negation of the argument
   * @param f an AbstractFormula
   * @return (!f1)
   */
  public Region makeNot(Region f);

  /**
   * Creates a region representing an AND of the two argument
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return (f1 & f2)
   */
  public Region makeAnd(Region f1, Region f2);

  /**
   * Creates a region representing an OR of the two argument
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return (f1 | f2)
   */
  public Region makeOr(Region f1, Region f2);

  /**
   * Creates a region representing an equality (bi-implication) of the two argument
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return (f1 <=> f2)
   */
  public Region makeEqual(Region f1, Region f2);

  /**
   * Creates a region representing an disequality (XOR) of the two argument
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return (f1 ^ f2)
   */
  public Region makeUnequal(Region f1, Region f2);

  /**
   * Creates a region representing an if then else construct of the three arguments
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @param f3 an AbstractFormula
   * @return (if f1 then f2 else f3)
   */
  public Region makeIte(Region f1, Region f2, Region f3);

  /**
   * Creates a region representing an existential quantification of the second
   * argument. If there are more arguments, each of them is quantified.
   * @param f1 an AbstractFormula
   * @param f2 one or more AbstractFormulas
   * @return (exists f2... : f1)
   */
  public Region makeExists(Region f1, Region... f2);

  /**
   * Creates a new variable and returns the predicate representing it.
   * @return a new predicate
   */
  public Region createPredicate();

  /**
   * Returns the set of all predicates that occur in the representation of this region.
   * @return a set of regions where each region represents one predicate
   */
  public Set<Region> extractPredicates(Region f);

  /**
   * Convert a formula into a region.
   * @param pF The formula to convert.
   * @param fmgr The formula manager that belongs to pF.
   * @param atomToRegion A function that returns a region for each atom in the formula.
   * @return a region representing pF
   */
  public Region fromFormula(BooleanFormula pF, FormulaManagerView fmgr,
      Function<BooleanFormula, Region> atomToRegion);

  /**
   * A region consists of the form
   * if (predicate) then formula1 else formula2
   * This method decomposes a region into these three parts.
   * @param pF a region
   * @return a triple with the condition predicate and the formulas for the true
   *         branch and the else branch
   */
  public Triple<Region, Region, Region>
      getIfThenElse(Region f);

  /**
   * Prints some information about the RegionManager.
   */
  public void printStatistics(PrintStream out);

  /**
   * Return a new {@link RegionBuilder} instance.
   */
  public RegionBuilder builder(ShutdownNotifier pShutdownNotifier);

  /**
   * A stateful region builder for regions that are disjunctions
   * of conjunctive literals.
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
     * @param r
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