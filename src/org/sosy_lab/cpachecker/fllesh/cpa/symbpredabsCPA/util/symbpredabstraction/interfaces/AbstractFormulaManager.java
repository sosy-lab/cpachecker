/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;

/**
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 *
 * An AbstractFormulaManager is an object that knows how to create/manipulate
 * AbstractFormulas
 */
public interface AbstractFormulaManager {

  /**
   * checks whether the data region represented by f1
   * is a subset of that represented by f2
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return true if (f1 => f2), false otherwise
   */
  public boolean entails(AbstractFormula f1, AbstractFormula f2);

  /**
   * checks whether f represents "false"
   * @return true if f represents logical falsity, false otherwise
   */
  public boolean isFalse(AbstractFormula f);

  /**
   * @return a representation of logical truth
   */
  public AbstractFormula makeTrue();

  /**
   * @return a representation of logical falseness
   */
  public AbstractFormula makeFalse();

  /**
   * Creates a formula representing a negation of the argument
   * @param f an AbstractFormula
   * @return (!f1)
   */
  public AbstractFormula makeNot(AbstractFormula f);

  /**
   * Creates a formula representing an AND of the two argument
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return (f1 & f2)
   */
  public AbstractFormula makeAnd(AbstractFormula f1, AbstractFormula f2);

  /**
   * Creates a formula representing an OR of the two argument
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return (f1 | f2)
   */
  public AbstractFormula makeOr(AbstractFormula f1, AbstractFormula f2);

  /**
   * Creates a new variable and returns the predicate representing it.
   * @return a new predicate
   */
  public Predicate createPredicate();

  /**
   * An abstract formula consists of the form
   * if (predicate) then formula1 else formula2
   * This method decomposes a formula into these three parts.
   * @param pF an abstract formula
   * @return a triple with the condition predicate and the formulas for the true
   *         branch and the else branch
   */
  public Triple<Predicate, AbstractFormula, AbstractFormula>
      getIfThenElse(AbstractFormula f);

}