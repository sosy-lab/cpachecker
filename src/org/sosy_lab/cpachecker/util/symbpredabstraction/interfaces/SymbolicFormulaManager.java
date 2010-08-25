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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap;


/**
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 *
 * A SymbolicFormulaManager is an object that can create/manipulate
 * SymbolicFormulas
 */
public interface SymbolicFormulaManager {

  // ----------------- Boolean formulas -----------------

  public boolean isBoolean(SymbolicFormula pF);

  /**
   * @return a SymbolicFormula representing logical truth
   */
  public SymbolicFormula makeTrue();

  /**
   * @return a SymbolicFormula representing logical falsity
   */
  public SymbolicFormula makeFalse();
  
  /**
   * Creates a formula representing a negation of the argument.
   * @param f a SymbolicFormula
   * @return (!f1)
   */
  public SymbolicFormula makeNot(SymbolicFormula f);

  /**
   * Creates a formula representing an AND of the two arguments.
   * @param f1 a SymbolicFormula
   * @param f2 a SymbolicFormula
   * @return (f1 & f2)
   */
  public SymbolicFormula makeAnd(SymbolicFormula f1, SymbolicFormula f2);

  /**
   * Creates a formula representing an OR of the two arguments.
   * @param f1 a SymbolicFormula
   * @param f2 a SymbolicFormula
   * @return (f1 | f2)
   */
  public SymbolicFormula makeOr(SymbolicFormula f1, SymbolicFormula f2);

  /**
   * Creates a formula representing an equivalence of the two arguments.
   * @param f1 a SymbolicFormula
   * @param f2 a SymbolicFormula
   * @return (f1 <-> f2)
   */
  public SymbolicFormula makeEquivalence(SymbolicFormula f1, SymbolicFormula f2);

  /**
   * Creates a formula representing "IF cond THEN f1 ELSE f2"
   * @param cond a SymbolicFormula
   * @param f1 a SymbolicFormula
   * @param f2 a SymbolicFormula
   * @return (IF atom THEN f1 ELSE f2)
   */

  public SymbolicFormula makeIfThenElse(SymbolicFormula cond,
      SymbolicFormula f1, SymbolicFormula f2);

  
  // ----------------- Numeric formulas -----------------

  public SymbolicFormula makeNumber(int pI);
  
  public SymbolicFormula makeNumber(String pI);

  public SymbolicFormula makeNegate(SymbolicFormula pF);

  public SymbolicFormula makePlus(SymbolicFormula pF1, SymbolicFormula pF2);

  public SymbolicFormula makeMinus(SymbolicFormula pF1, SymbolicFormula pF2);

  public SymbolicFormula makeDivide(SymbolicFormula pF1, SymbolicFormula pF2);

  public SymbolicFormula makeModulo(SymbolicFormula pF1, SymbolicFormula pF2);

  public SymbolicFormula makeMultiply(SymbolicFormula pF1, SymbolicFormula pF2);
  
  // ----------------- Numeric relations -----------------
  
  public SymbolicFormula makeEqual(SymbolicFormula pF1, SymbolicFormula pF2);

  public SymbolicFormula makeGt(SymbolicFormula pF1, SymbolicFormula pF2);

  public SymbolicFormula makeGeq(SymbolicFormula pF1, SymbolicFormula pF2);

  public SymbolicFormula makeLt(SymbolicFormula pF1, SymbolicFormula pF2);

  public SymbolicFormula makeLeq(SymbolicFormula pF1, SymbolicFormula pF2);
 
  // ----------------- Bit-manipulation functions -----------------

  public SymbolicFormula makeBitwiseNot(SymbolicFormula pF);

  public SymbolicFormula makeBitwiseAnd(SymbolicFormula pF1, SymbolicFormula pF2);

  public SymbolicFormula makeBitwiseOr(SymbolicFormula pF1, SymbolicFormula pF2);

  public SymbolicFormula makeBitwiseXor(SymbolicFormula pF1, SymbolicFormula pF2);

  public SymbolicFormula makeShiftLeft(SymbolicFormula pF1, SymbolicFormula pF2);

  public SymbolicFormula makeShiftRight(SymbolicFormula pF1, SymbolicFormula pF2);

  // ----------------- Uninterpreted functions -----------------
  
  public SymbolicFormula makeUIF(String pName, SymbolicFormula[] pArgs);

  public SymbolicFormula makeUIF(String pName, SymbolicFormula[] pArgs, int pIdx);

  // ----------------- Other formulas -----------------
  
  public boolean isErrorTerm(SymbolicFormula pF);

  public SymbolicFormula makeString(int pI);

  public SymbolicFormula makeVariable(String pVar, int pIdx);
  
  public SymbolicFormula makeAssignment(SymbolicFormula pF1, SymbolicFormula pF2);

  
  // ----------------- Complex formula manipulation -----------------
  
    /**
     * Parse a formula given as a String in the common infix notation.
     * @return The same formula in the internal representation.
     * @throws IllegalArgumentException If the string cannot be parsed.
     */
    public SymbolicFormula parseInfix(String s) throws IllegalArgumentException;

    /**
     * Given a formula that uses "generic" variables, returns the corresponding
     * one that "instantiates" such variables according to the given SSA map.
     * This is used by AbstractFormulaManager.toConcrete()
     *
     * @param f the generic SymbolicFormula to instantiate
     * @param ssa the SSAMap to use
     * @return a copy of f in which every "generic" variable is replaced by the
     * corresponding "SSA instance"
     */
    public SymbolicFormula instantiate(SymbolicFormula f, SSAMap ssa);

    /**
     * TODO: document this method and probably clean up the signature
     */
    public SymbolicFormula[] getInstantiatedAt(SymbolicFormula[] args,
        SSAMap ssa, Map<SymbolicFormula, SymbolicFormula> cache);
    
    public SymbolicFormula uninstantiate(SymbolicFormula pF);

    /**
     * "shifts" forward all the variables in the formula f, of the amount
     * given by the input ssa. That is, variables x with index 1 in f will be
     * replaced by variables with index ssa.getIndex(x), vars with index 2 by
     * vars with index ssa.getIndex(x)+1, and so on.
     * Returns the new formula and the ssa map with the final index for each
     * variable
     * @param f the SymbolicFormula to shift
     * @param ssa the SSAMap to use for shifting
     * @return the shifted formula and the new SSA map
     */
    public PathFormula shift(SymbolicFormula f, SSAMap ssa);

    /**
     * The path formulas have an uninterpreted function :=
     * where an assignment should be. This method replaces all those appearances
     * by equalities (which is a valid representation of an assignment for a SSA
     * formula).
     */
    public SymbolicFormula replaceAssignments(SymbolicFormula f);
      
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
    public Collection<SymbolicFormula> extractAtoms(SymbolicFormula f,
             boolean splitArithEqualities, boolean conjunctionsOnly);

    /**
     * Extracts the SSA indices from a formula. 
     */
    public SSAMap extractSSA(SymbolicFormula f);

    /**
     * Collects all variables names and all lValues in a term.
     * @param term  the symbolic formula to analyze
     * @param vars  the set were all variable names are stored
     * @param lvals the set where all lValue UIFs and their arguments are stored
     */
    public void collectVarNames(SymbolicFormula term, Set<String> vars,
                                Set<Pair<String, SymbolicFormula[]>> lvals);
    
    /**
     * Create string representation of a formula in a format which may be dumped
     * to a file.
     */
    public String dumpFormula(SymbolicFormula pT);

    /**
     * Prepare formula for solving problems. For example, this method can add
     * some extra terms to the formula that help the solver.
     * 
     * This method returns either the argument or a new formula. The caller should
     * use the result when asking the solver.
     */
    public SymbolicFormula prepareFormula(SymbolicFormula f);
    
    /**
     * Prepare formulas for solving problems. For example, this method can add
     * some extra terms to the formulas that help the solver.
     *
     * This method may change the supplied list.
     */
    public void prepareFormulas(List<SymbolicFormula> f);
    
    /**
     * Dump an abstraction problem to a file so that the user can look at this problem later.
     */
    public void dumpAbstraction(SymbolicFormula curState, SymbolicFormula edgeFormula,
        SymbolicFormula predDef, List<SymbolicFormula> importantPreds);
    
    /**
     * Create the variable representing a predicate for the given atom. There won't
     * be any tracking of the correspondence between the atom and the variable,
     * if it is not done by the caller of this method.
     */
    public SymbolicFormula createPredicateVariable(SymbolicFormula pAtom);
    
    /**
     * Returns an instance of a class that knows how parse the AllSat result
     * and create an abstract formula from it.
     */
    public AllSatCallback getAllSatCallback(FormulaManager mgr, AbstractFormulaManager amgr);
    
    public interface AllSatCallback extends TheoremProver.AllSatCallback {
      public AbstractFormula getResult();
      public int getCount();
      public long getTotalTime();
    }
}