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
import java.util.Map;

/**
 * This interface represents some formula traverse methods which should not be used on higher levels.
 * These Methods are hidden behind the View, but used in the view for methods like "extractAtoms".
 */
public interface UnsafeFormulaManager {

  /**
   * Type a given Formula (Formulas given by the UnsafeFormulaManager do not have to be typed!)
   * @param type the target type
   * @param f the formula to type
   * @return the typed formula
   */
  <T extends Formula> T typeFormula(FormulaType<T> type, Formula f);

  /**
   * see getArguments.
   * @param f
   * @return
   */
  int getArity(Formula f);

  /**
   * see getArguments.
   * @param f
   * @param n
   * @return
   */
  Formula getArg(Formula f, int n);

  /**
   * Checks if the given Formula is an atom.
   * @param f
   * @return
   */
  boolean isAtom(Formula f);

  /**
   * Checks if the given Formula is a variable.
   * (either free or quantified)
   * @param f
   * @return
   */
  boolean isVariable(Formula f);

  /**
   * Checks if the given Formula is a free (not quantified) variable.
   * @param f
   * @return
   */
  boolean isFreeVariable(Formula f);

  /**
   * Checks if the given Formula is a bound (by a quantifier) variable.
   * @param f
   * @return
   */
  boolean isBoundVariable(Formula f);

  /**
   * Checks if the given Formula is a Number.
   * @param pTt
   * @return
   */
  boolean isNumber(Formula pTt);
  /**
   * Checks if the given Formula is an uninterpreted function call.
   * @param f
   * @return
   */
  boolean isUF(Formula f);

  /**
   * Checks if the given Formula is quantified (either FORALL ..., or EXISTS ...).
   * @param f
   * @return
   */
  boolean isQuantification(Formula f);

  /**
   * Get the body of the given, quantified, formula.
   *
   * Precondition:
   *    isQuantification(f) == true
   *
   * @param f
   * @return
   */
  BooleanFormula getQuantifiedBody(Formula pQuantifiedFormula);

  /**
   * Replace the body of a quantified formula.
   *
   * Precondition:
   *    isQuantification(pF) == true
   *
   * @param pTt
   * @param pNewBody
   * @return
   */
  BooleanFormula replaceQuantifiedBody(BooleanFormula pF, BooleanFormula pNewBody);

  /**
   * Returns the name of the formula (or function)
   * @param f
   * @return
   */
  String getName(Formula f);

  /**
   * Replaces the name and the arguments of the given formula.
   * The old and the new name need to be of the same type.
   * If f is a variable, use an empty list of arguments.
   * @param f
   * @param newName
   * @param args
   * @return
   */
  <T extends Formula> T replaceArgsAndName(T f, String newName, List<Formula> args);
  /**
  * Replaces the arguments of the given formula
  * @param f
  * @param args
  * @return
  */
  <T extends Formula> T replaceArgs(T f, List<Formula> args);

  /**
   * If the given formula is a numeral (i.e., non-boolean) equality "x = y",
   * return a list {@code x<=y, x>=y}.
   *
   * Otherwise, return the unchanged formula.
   * Note:
   *  1) Returned list always has one or two elements.
   *  2) Conjunction over the returned list is equivalent to the input formula.
   */
  <T extends Formula> List<T> splitNumeralEqualityIfPossible(T f);

  /**
   * Substitute every occurrence of any item from {@code changeFrom}
   * in formula {@code f} to the corresponding occurrence from {@code changeTo}.
   *
   * E.g. if {@code changeFrom} contains a variable {@code a} and
   * {@code changeTo} contains a variable {@code b} all occurrences of {@code a}
   * will be changed to {@code b} in the returned formula.
   *
   * @param f Formula to change
   * @param changeFrom List of things to change from.
   * @param changeTo List of things to change to.
   * @return Formula with variables being replaced.
   *
   */
  <T1 extends Formula, T2 extends Formula> T1
      substitute(T1 f, List<T2> changeFrom, List<T2> changeTo);

  <T1 extends Formula, T2 extends Formula> T1
      substitute(T1 f, Map<T2, T2> fromToMapping);

  /**
   * Simplify a given formula (as good as possible).
   *    Equivalence must be ensured!
   *
   * A solver that does not provide a simplify method
   *  might just return the original formula.
   *
   * @param   The input formula
   * @return  Simplified version of the formula
   */
  <T extends Formula> T simplify(T f);

}
