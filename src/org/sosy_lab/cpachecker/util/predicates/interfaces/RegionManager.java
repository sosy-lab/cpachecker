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

import org.sosy_lab.common.Triple;

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
  public boolean entails(Region f1, Region f2);

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
   * Creates a region representing an existential quantification of the two argument
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return (\exists f2: f1)
   */
  public Region makeExists(Region f1, Region f2);

  /**
   * Creates a new variable and returns the predicate representing it.
   * @return a new predicate
   */
  public Region createPredicate();

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
}