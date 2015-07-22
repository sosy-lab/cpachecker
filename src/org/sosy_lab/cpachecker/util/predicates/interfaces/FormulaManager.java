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
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;

/**
 * Represents an SMT solver.
 * Instances of this interface provide direct low-level access to an SMT solver.
 * Most code should use this interface directly, but instead use the classes
 * {@link Solver} and {@link FormulaManagerView} which provide additional features.
 */
public interface FormulaManager {

  /**
   * Returns the Integer-Theory.
   * Because most SAT-solvers support automatic casting between Integer- and Rational-Theory,
   * the Integer- and the RationalFormulaManager both return the same Formulas for numeric operations
   * like ADD, SUBSTRACT, TIMES, LESSTHAN, EQUAL, etc.
   */
  NumeralFormulaManager<IntegerFormula, IntegerFormula> getIntegerFormulaManager();

  /**
   * Returns the Rational-Theory.
   * Because most SAT-solvers support automatic casting between Integer- and Rational-Theory,
   * the Integer- and the RationalFormulaManager both return the same Formulas for numeric operations
   * like ADD, SUBSTRACT, TIMES, LESSTHAN, EQUAL, etc.
   */
  NumeralFormulaManager<NumeralFormula, RationalFormula> getRationalFormulaManager();

  /**
   * Returns the Boolean-Theory.
   */
  BooleanFormulaManager getBooleanFormulaManager();

  /**
   * Returns the Array-Theory.
   */
  ArrayFormulaManager getArrayFormulaManager();

  /**
   * Returns the Bitvector-Theory.
   */
  BitvectorFormulaManager getBitvectorFormulaManager();

  /**
   * Returns the Floating-Point-Theory.
   */
  FloatingPointFormulaManager getFloatingPointFormulaManager();

  /**
   * Returns the Function-Theory.
   */
  FunctionFormulaManager getFunctionFormulaManager();

  /**
   * Returns some unsafe traverse methods.
   */
  UnsafeFormulaManager getUnsafeFormulaManager();

  /**
   * Returns the interface for handling quantifiers.
   */
  QuantifiedFormulaManager getQuantifiedFormulaManager();

  /**
   * Create a fresh new {@link ProverEnvironment} which encapsulates an assertion stack
   * and can be used to check formulas for unsatisfiability.
   * @param generateModels Whether the solver should generate models (i.e., satisfying assignments) for satisfiable formulas.
   * @param generateUnsatCore Whether the solver should generate an unsat core for unsatisfiable formulas.
   */
  ProverEnvironment newProverEnvironment(boolean generateModels, boolean generateUnsatCore);

  /**
   * Create a fresh new {@link InterpolatingProverEnvironment} which encapsulates an assertion stack
   * and allows to generate and retrieve interpolants for unsatisfiable formulas.
   * If the SMT solver is able to handle satisfiability tests with assumptions please consider
   * implementing the {@link InterpolatingProverEnvironmentWithAssumptions} interface, and return
   * an Object of this type here.
   * @param shared Whether the solver should share as much as possible with other environments.
   */
  InterpolatingProverEnvironment<?> newProverEnvironmentWithInterpolation(boolean shared);

  /**
   * Create a fresh new {@link OptEnvironment} which encapsulates an assertion stack
   * and allows to solve optimization queries.
   */
  OptEnvironment newOptEnvironment();

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
  public Appender dumpFormula(BooleanFormula pT);

  /**
   * Get some version information of the solver.
   */
  public String getVersion();

  /**
   * @return Returns an instance of SmtAstMatcher if implemented for the specific solver; otherwise 'null'.
   */
  public SmtAstMatcher getSmtAstMatcher();
}
