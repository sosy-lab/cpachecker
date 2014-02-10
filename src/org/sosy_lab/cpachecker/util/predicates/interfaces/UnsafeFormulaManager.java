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
   * @param f
   * @return
   */
  boolean isVariable(Formula f);
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
   * Returns the name of the formula (or function)
   * @param f
   * @return
   */
  String getName(Formula f);

  /**
   * Replaces the name and the arguments of the given formula
   * @param f
   * @param newName
   * @param args
   * @return
   */
  Formula replaceArgsAndName(Formula f, String newName, Formula[] args);
  /**
  * Replaces the arguments of the given formula
  * @param f
  * @param args
  * @return
  */
  Formula replaceArgs(Formula f, Formula[] args);
  /**
   * Replaces the name of the given formula
   * @param f
   * @param newName
   * @return
   */
  Formula replaceName(Formula f, String newName);
}
