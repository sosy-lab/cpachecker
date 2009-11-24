/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package symbpredabstraction.interfaces;

/**
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 *
 * An AbstractFormulaManager is an object that knows how to create/manipulate
 * AbstractFormulas
 */
public interface AbstractFormulaManager {

  /**
   * @return a concrete representation of an abstract formula
   * The formula returned is a "generic" version, not instantiated to any
   * particular "SSA step" (see SymbolicFormulaManager.instantiate()).
   */
  public SymbolicFormula toConcrete(SymbolicFormulaManager mgr,
                                    AbstractFormula af);
  /**
   * Computes the predicate abstraction of the given formula
   * @param mgr the manager for the symbolic formula
   * @param f the formula to abstract
   * @param ssa the SSAMap to use for resolving variable names
   * @param predicates the list of predicates
   * @return the predicate abstraction of f
   */
  /*
   * PW removed in revision 761 because there was only one broken
   * implementation and nobody used it.
   * 
  public AbstractFormula toAbstract(SymbolicFormulaManager mgr,
          SymbolicFormula f, SSAMap ssa, Collection<Predicate> predicates);
  */

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

}
