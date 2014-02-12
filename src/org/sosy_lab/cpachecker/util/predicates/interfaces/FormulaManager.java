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

import org.sosy_lab.common.Appender;


/**
 * Represents a Solver.
 */
public interface FormulaManager {

  /**
   * Returns the Rational-Theory.
   */
  RationalFormulaManager getRationalFormulaManager();

  /**
   * Returns the Boolean-Theory.
   */
  BooleanFormulaManager getBooleanFormulaManager();

  /**
   * Returns the Bitvector-Theory.
   */
  BitvectorFormulaManager getBitvectorFormulaManager();

  /**
   * Returns the Function-Theory.
   */
  FunctionFormulaManager getFunctionFormulaManager();

  /**
   * Returns some unsafe traverse methods.
   */
  UnsafeFormulaManager getUnsafeFormulaManager();

  /**
   * Returns the type of the given Formula.
   * Undefined behavior when an untyped Formula from UnsafeFormulaManager is given.
   */
  public <T extends Formula> FormulaType<T> getFormulaType(T formula);

  /**
   * Parse a formula given as a String in a solver-specific file format.
   * @return The same formula in the internal representation.
   * @throws IllegalArgumentException If the string cannot be parsed.
   */
  // TODO: Implement solver independent file format and remove this method from the solver interface
  // Instead implement the format in the View
  public BooleanFormula parse(String s) throws IllegalArgumentException;

  /**
   * Returns the Interface-Class of the given Formula. For example BitvectorFormula.class.
   */
  public <T extends Formula> Class<T> getInterface(T pInstance);
  /**
   * Create string representation of a formula in a format which may be dumped
   * to a file. To get a String, simply call {@link Object#toString()}
   * on the returned object.
   *
   * This method is lazy and does not create any huge string until the returned
   * object is actually used.
   *
   * @see Appender
   */
  // TODO: Implement solver independent file format and remove this method from the solver interface
  // Instead implement the format in the View
  public Appender dumpFormula(Formula pT);

  /**
   * Get some version information of the solver.
   */
  public String getVersion();
}
