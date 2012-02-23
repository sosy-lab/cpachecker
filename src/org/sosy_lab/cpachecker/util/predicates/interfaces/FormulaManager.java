/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;


/**
 * A FormulaManager is an object that can create/manipulate
 * Formulas
 */
public interface FormulaManager {

  // ----------------- Boolean formulas -----------------

  public boolean isBoolean(Formula pF);

  /**
   * @return a Formula representing logical truth
   */
  public Formula makeTrue();

  /**
   * @return a Formula representing logical falsity
   */
  public Formula makeFalse();

  /**
   * Creates a formula representing a negation of the argument.
   * @param f a Formula
   * @return (!f1)
   */
  public Formula makeNot(Formula f);

  /**
   * Creates a formula representing an AND of the two arguments.
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (f1 & f2)
   */
  public Formula makeAnd(Formula f1, Formula f2);

  /**
   * Creates a formula representing an OR of the two arguments.
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (f1 | f2)
   */
  public Formula makeOr(Formula f1, Formula f2);

  /**
   * Creates a formula representing an equivalence of the two arguments.
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (f1 <-> f2)
   */
  public Formula makeEquivalence(Formula f1, Formula f2);

  /**
   * Creates a formula representing "IF cond THEN f1 ELSE f2"
   * @param cond a Formula
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (IF atom THEN f1 ELSE f2)
   */

  public Formula makeIfThenElse(Formula cond,
      Formula f1, Formula f2);


  // ----------------- Numeric formulas -----------------

  public Formula makeNumber(int pI);

  public Formula makeNumber(String pI);

  public Formula makeNegate(Formula pF);

  public Formula makePlus(Formula pF1, Formula pF2);

  public Formula makeMinus(Formula pF1, Formula pF2);

  public Formula makeDivide(Formula pF1, Formula pF2);

  public Formula makeModulo(Formula pF1, Formula pF2);

  public Formula makeMultiply(Formula pF1, Formula pF2);

  // ----------------- Numeric relations -----------------

  public Formula makeEqual(Formula pF1, Formula pF2);

  public Formula makeGt(Formula pF1, Formula pF2);

  public Formula makeGeq(Formula pF1, Formula pF2);

  public Formula makeLt(Formula pF1, Formula pF2);

  public Formula makeLeq(Formula pF1, Formula pF2);

  // ----------------- Bit-manipulation functions -----------------

  public Formula makeBitwiseNot(Formula pF);

  public Formula makeBitwiseAnd(Formula pF1, Formula pF2);

  public Formula makeBitwiseOr(Formula pF1, Formula pF2);

  public Formula makeBitwiseXor(Formula pF1, Formula pF2);

  public Formula makeShiftLeft(Formula pF1, Formula pF2);

  public Formula makeShiftRight(Formula pF1, Formula pF2);

  // ----------------- Uninterpreted functions -----------------

  public Formula makeUIF(String pName, FormulaList pArgs);

  public Formula makeUIF(String pName, FormulaList pArgs, int pIdx);

  // ----------------- Other formulas -----------------

  public Formula makeString(int pI);

  public Formula makeVariable(String pVar, int pIdx);

  public Formula makeVariable(String pVar);

  public Formula makePredicateVariable(String pVar, int pIdx);

  public Formula makeAssignment(Formula pF1, Formula pF2);

  // ----------------- Convert to list -----------------

  public FormulaList makeList(Formula pF);

  public FormulaList makeList(Formula pF1, Formula pF2);

  public FormulaList makeList(List<Formula> pFs);

  // ----------------- Complex formula manipulation -----------------

    /**
     * Parse a formula given as a String in the common infix notation.
     * @return The same formula in the internal representation.
     * @throws IllegalArgumentException If the string cannot be parsed.
     */
    public Formula parseInfix(String s) throws IllegalArgumentException;

    /**
     * Parse a formula given as a String in a solver-specific file format.
     * @return The same formula in the internal representation.
     * @throws IllegalArgumentException If the string cannot be parsed.
     */
    public Formula parse(String s) throws IllegalArgumentException;

    /**
     * Given a formula that uses "generic" variables, returns the corresponding
     * one that "instantiates" such variables according to the given SSA map.
     *
     * @param f the generic Formula to instantiate
     * @param ssa the SSAMap to use
     * @return a copy of f in which every "generic" variable is replaced by the
     * corresponding "SSA instance"
     */
    public Formula instantiate(Formula f, SSAMap ssa);

    /**
     * Given an "instantiated" formula, returns the corresponding formula in
     * which all the variables are "generic" ones. This is the inverse of the
     * instantiate() method above
     */
    @Deprecated
    public Formula uninstantiate(Formula pF);

    /**
     * Extracts the atoms from the given formula. Any SSA indices are removed
     * from the symbols in the atoms.
     * @param f the formula to operate on
     * @param splitArithEqualities if true, return (x <= y) and (y <= x)
     *                             instead of (x = y)
     * @param conjunctionsOnly if true, don't extract atoms, but only top-level
     *                         conjuncts. For example, if called on:
     *                         a & (b | c), the result will be [a, (b | c)]
     *                         instead of [a, b, c]
     * @return a collection of (atomic) formulas
     */
    public Collection<Formula> extractAtoms(Formula f,
             boolean splitArithEqualities, boolean conjunctionsOnly);

    /**
     * Create string representation of a formula in a format which may be dumped
     * to a file.
     */
    public String dumpFormula(Formula pT);

    /**
     * Looks for uninterpreted functions in the formula and adds bitwise
     * axioms for them.
     */
    public Formula getBitwiseAxioms(Formula f);

    /**
     * Create the variable representing a predicate for the given atom. There won't
     * be any tracking of the correspondence between the atom and the variable,
     * if it is not done by the caller of this method.
     */
    public Formula createPredicateVariable(Formula pAtom);


    // ----------------- Rely-guarantee -----------------

    /**
     * Returns unprimed variable name and the primed number. If the variable is
     * not primed, then the primed no is -1. Works only on uninstantiated variables.
     * @param pFirst
     * @return (unprimed variable name, primed no)
     */
    Pair<String, Integer> getPrimeData(String str);

    /**
     * Primes variable given number of times. Value -1 means no prime value.
     * Works on uninstantiated variables, without prime numbers.
     * @param bareName
     * @param primeNo
     * @return
     */
    String primeVariable(String var, int primeNo);

  /**
   * Makes conjunction of equalities v^i@x <-> v^j@y, where x>0 is the last index of
   * variable v^i in ssa1 and y>0 is the last index of v^j in ssa2.
   * Note that v^-1 is treated as v.
   * @param ssa1
   * @param i
   * @param ssa2
   * @param j
   * @return equalities
   */
  Formula makePrimedEqualities(SSAMap ssa1, int i, SSAMap ssa2, int j);

  /**
   * Changes prime numbers of variables as specified by the map. Value -1 means no prime number.
   * @param pathFormula
   * @param map
   * @return change path formula
   */
  Formula changePrimedNo(Formula formula, Map<Integer, Integer> map);

  /**
   * Instantiates plain variables of f to the low SSA map and hashed variables to the high map.
   * Optionally, a dummy formula will be added for every dummy formula in the high map that was not
   * instantiated -- this enforces that the indexes of the result match the indexes in the high map.
   * @param f
   * @param low
   * @param high
   * @param createDummy
   * @return instantiated formula
   */
  Formula instantiateNextValue(Formula f, SSAMap oldSsa, SSAMap newSsa, boolean createDummy);

    /**
     * Extract atoms from the given formula and uninstantiate them. Variables with SSA index equal to the
     * ssa map are represented as hashed variables, while other variables are plain.
     * @param itp
     * @param Ssa
     * @return atoms
     */
    public Collection<Formula> extractNextValueAtoms(Formula f, SSAMap ssa);
}