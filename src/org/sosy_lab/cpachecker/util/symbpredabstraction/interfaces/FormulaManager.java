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
package org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces;

import org.sosy_lab.cpachecker.util.symbpredabstraction.PathFormula;
import org.sosy_lab.common.Pair;

public interface FormulaManager {

  /**
   * @return a concrete representation of an abstract formula
   * The formula returned is a "generic" version, not instantiated to any
   * particular "SSA step" (see SymbolicFormulaManager.instantiate()).
   */
  public SymbolicFormula toConcrete(AbstractFormula af);

  /**
   * Creates a new path formula representing an OR of the two arguments. Differently
   * from {@link SymbolicFormulaManager#makeOr(SymbolicFormula, SymbolicFormula)},
   * it also merges the SSA maps and creates the necessary adjustments to the
   * formulas if the two SSA maps contain different values for the same variables.
   *
   * @param pF1 a PathFormula
   * @param pF2 a PathFormula
   * @return (pF1 | pF2)
   */
  public PathFormula makeOr(PathFormula pF1, PathFormula pF2);

  /**
   * creates a Predicate from the Boolean symbolic variable (var) and
   * the atom that defines it
   */
  public Predicate makePredicate(SymbolicFormula var, SymbolicFormula atom);

  /**
   * Get the symbolic formulas for the variable and the atom which belong to a
   * predicate.
   * @param p A predicate which has been return by {@link #makePredicate(SymbolicFormula, SymbolicFormula)}
   * @return The values passed to the makePredicate call (symbolic formula for var and atom)
   */
  public Pair<? extends SymbolicFormula, ? extends SymbolicFormula> getPredicateVarAndAtom(Predicate p);

  /**
   * Get predicate corresponding to a variable.
   * @param var A symbolic formula representing the variable. The same formula has to been passed to makePredicate earlier.
   * @return a Predicate
   */
  public Predicate getPredicate(SymbolicFormula var);

  void dumpFormulasToFile(Iterable<SymbolicFormula> pF, String pFilename);
}
