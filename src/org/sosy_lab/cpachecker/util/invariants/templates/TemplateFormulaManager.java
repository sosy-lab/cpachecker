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
package org.sosy_lab.cpachecker.util.invariants.templates;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.InfixReln;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

public class TemplateFormulaManager implements FormulaManager {

  private final TemplateParseMode tpMode;

  public TemplateFormulaManager() {
    tpMode = TemplateParseMode.PATHFORMULA;
  }

  public TemplateFormulaManager(TemplateParseMode tpm) {
    tpMode = tpm;
  }

  // ----------------- Boolean formulas -----------------

  @Override
  public boolean isBoolean(Formula pF) {
    // OLD:
    // //For TemplateFormulas, to be boolean is to be a subclass of
    // //TemplateConjunction.
    //return TemplateConjunction.isInstance(pF);
    return (pF instanceof TemplateBoolean);
  }

  /**
   * @return a Formula representing logical truth
   */
  @Override
  public Formula makeTrue() {
    return new TemplateTrue();
  }

  /**
   * @return a Formula representing logical falsity
   */
  @Override
  public Formula makeFalse() {
    return new TemplateFalse();
  }

  /**
   * Creates a formula representing a negation of the argument.
   * @param f a Formula
   * @return (!f1)
   */
  @Override
  public Formula makeNot(Formula f) {
    Formula F = null;
    try {
      TemplateBoolean b = (TemplateBoolean) f;
      F = TemplateNegation.negate(b);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  /**
   * Creates a formula representing an AND of the two arguments.
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (f1 & f2)
   */
  @Override
  public Formula makeAnd(Formula f1, Formula f2) {
    Formula F = null;
    try {
      TemplateBoolean b1 = (TemplateBoolean) f1;
      TemplateBoolean b2 = (TemplateBoolean) f2;
      F = TemplateConjunction.conjoin(b1, b2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    // Below is the old method, in which we make True of anything
    // that won't case to a Boolean. It shouldn't be necessary, but
    // might let us continue to work in some odd case.
    /*
    TemplateBoolean b1, b2;
    try {
      b1 = (TemplateBoolean) f1;
    } catch (ClassCastException e) {
      b1 = new TemplateTrue();
    }
    try {
      b2 = (TemplateBoolean) f2;
    } catch (ClassCastException e) {
      b2 = new TemplateTrue();
    }
    F = TemplateConjunction.conjoin(b1, b2);
    */
    return F;
  }

  /**
   * Creates a formula representing an OR of the two arguments.
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (f1 | f2)
   */
  @Override
  public Formula makeOr(Formula f1, Formula f2) {
    Formula F = null;
    try {
      TemplateBoolean b1 = (TemplateBoolean) f1;
      TemplateBoolean b2 = (TemplateBoolean) f2;
      F = TemplateDisjunction.disjoin(b1, b2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  /**
   * Creates a formula representing an equivalence of the two arguments.
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (f1 <-> f2)
   */
  @Override
  public Formula makeEquivalence(Formula f1, Formula f2) {
    Formula F = null;
    try {
      TemplateBoolean b1 = (TemplateBoolean) f1;
      TemplateBoolean b2 = (TemplateBoolean) f2;
      TemplateBoolean nb1 = TemplateNegation.negate(b1);
      TemplateBoolean nb2 = TemplateNegation.negate(b2);
      TemplateBoolean both = TemplateConjunction.conjoin(b1, b2);
      TemplateBoolean neither = TemplateConjunction.conjoin(nb1, nb2);
      F = TemplateDisjunction.disjoin(both, neither);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  /**
   * Creates a formula representing "IF cond THEN f1 ELSE f2"
   * @param cond a Formula
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (IF atom THEN f1 ELSE f2)
   */

  @Override
  public Formula makeIfThenElse(Formula cond,
                Formula f1, Formula f2) {
    // We do not allow ifthenelse structures in templates.
    return new NonTemplate();
  }


  // ----------------- Numeric formulas -----------------

  @Override
  public Formula makeNumber(int pI) {
    TemplateNumber N = new TemplateNumber(pI);
    TemplateTerm T = new TemplateTerm();
    T.setCoefficient(N);
    return T;
  }

  @Override
  public Formula makeNumber(String pI) {
    TemplateNumber N = new TemplateNumber(pI);
    TemplateTerm T = new TemplateTerm();
    T.setCoefficient(N);
    return T;
  }

  @Override
  public Formula makeNegate(Formula pF) {
    TemplateFormula tf = null;
    try {
      tf = (TemplateFormula) pF;
      tf.negate();
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
    }
    return tf;
  }

  @Override
  public Formula makePlus(Formula pF1, Formula pF2) {
    Formula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = new TemplateSum(s1, s2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  @Override
  public Formula makeMinus(Formula pF1, Formula pF2) {
    Formula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = TemplateSum.subtract(s1, s2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  @Override
  public Formula makeDivide(Formula pF1, Formula pF2) {
    Formula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = TemplateSum.divide(s1, s2);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  @Override
  public Formula makeModulo(Formula pF1, Formula pF2) {
    return new NonTemplate();
  }

  @Override
  public Formula makeMultiply(Formula pF1, Formula pF2) {
    Formula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = TemplateSum.multiply(s1, s2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  // ----------------- Numeric relations -----------------

  @Override
  public Formula makeEqual(Formula pF1, Formula pF2) {
    Formula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = new TemplateConstraint(s1, InfixReln.EQUAL, s2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  @Override
  public Formula makeGt(Formula pF1, Formula pF2) {
    Formula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = new TemplateConstraint(s2, InfixReln.LT, s1);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  @Override
  public Formula makeGeq(Formula pF1, Formula pF2) {
    Formula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = new TemplateConstraint(s2, InfixReln.LEQ, s1);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  @Override
  public Formula makeLt(Formula pF1, Formula pF2) {
    Formula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = new TemplateConstraint(s1, InfixReln.LT, s2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  @Override
  public Formula makeLeq(Formula pF1, Formula pF2) {
    Formula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = new TemplateConstraint(s1, InfixReln.LEQ, s2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  // ----------------- Bit-manipulation functions -----------------

  @Override
  public Formula makeBitwiseNot(Formula pF) {
    return new NonTemplate();
  }

  @Override
  public Formula makeBitwiseAnd(Formula pF1, Formula pF2) {
    return new NonTemplate();
  }

  @Override
  public Formula makeBitwiseOr(Formula pF1, Formula pF2) {
    return new NonTemplate();
  }

  @Override
  public Formula makeBitwiseXor(Formula pF1, Formula pF2) {
    return new NonTemplate();
  }

  @Override
  public Formula makeShiftLeft(Formula pF1, Formula pF2) {
    return new NonTemplate();
  }

  @Override
  public Formula makeShiftRight(Formula pF1, Formula pF2) {
    return new NonTemplate();
  }

  // ----------------- Uninterpreted functions -----------------

  @Override
  public Formula makeUIF(String pName, FormulaList pArgs) {
    Formula F = null;
    try {
      TemplateFormulaList FL = (TemplateFormulaList) pArgs;
      TemplateSumList SL =  new TemplateSumList(FL);
      TemplateUIF U = new TemplateUIF(pName, SL);
      TemplateTerm T = new TemplateTerm();
      T.setUIF(U);
      F = T;
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  @Override
  public Formula makeUIF(String pName, FormulaList pArgs, int pIdx)
  {
    Formula F = null;
    try {
      TemplateFormulaList FL = (TemplateFormulaList) pArgs;
      TemplateSumList SL =  new TemplateSumList(FL);
      TemplateUIF U = new TemplateUIF(pName, SL, pIdx);
      TemplateTerm T = new TemplateTerm();
      T.setUIF(U);
      F = T;
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  // ----------------- Other formulas -----------------

  @Override
  public Formula makeString(int pI) {
    return new NonTemplate();
  }

  /**
   * See declaration of TemplateParseMode enum type.
   */
  @Override
  public Formula makeVariable(String pVar, int pIdx) {
    Integer i = new Integer(pIdx);
    return makeVariable(pVar, i);
  }

  @Override
  public Formula makeVariable(String pVar) {
    return makeVariable(pVar, null);
  }

  /**
   * See declaration of TemplateParseMode enum type.
   */
  private Formula makeVariable(String pVar, Integer pIdx) {
    TemplateTerm T = new TemplateTerm();
    if (tpMode == TemplateParseMode.TEMPLATE) {
      // we are parsing a handwritten template
      if (pVar != null && pVar.length() > 0 && pVar.startsWith("v")) {
        // it is a program variable
        pVar = pVar.substring(1);
        TemplateVariable V = new TemplateVariable(pVar, pIdx);
        T.setVariable(V);
      } else {
        // it is a parameter
        TemplateVariable V = new TemplateVariable(pVar, pIdx);
        T.setParameter(V);
      }
    } else {
      // we are parsing a path formula
      TemplateVariable V = new TemplateVariable(pVar, pIdx);
      T.setVariable(V);
    }
    return T;
  }

  @Override
  public Formula makePredicateVariable(String pVar, int pIdx) {
    return new NonTemplate();
  }

  @Override
  public Formula makeAssignment(Formula pF1, Formula pF2) {
    Formula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = new TemplateConstraint(s1, InfixReln.EQUAL, s2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate();
    }
    return F;
  }

  // ----------------- Convert to list -----------------

  @Override
  public FormulaList makeList(Formula pF) {
    Formula[] fs = {pF};
    return new TemplateFormulaList(fs);
  }

  @Override
  public FormulaList makeList(Formula pF1, Formula pF2) {
    Formula[] fs = {pF1, pF2};
    return new TemplateFormulaList(fs);
  }

  @Override
  public FormulaList makeList(List<Formula> pFs) {
    return new TemplateFormulaList(pFs);
  }

  public FormulaList makeList(Formula... pF) {
    return new TemplateFormulaList(pF);
  }


  // ----------------- Complex formula manipulation -----------------

  // TODO: there remain many stub methods. Write them?

  /**
   * Parse a formula given as a String in the common infix notation.
   * @return The same formula in the internal representation.
   * @throws IllegalArgumentException If the string cannot be parsed.
   */
  @Override
  public Formula parseInfix(String s) throws IllegalArgumentException {
    return null;
  }

  /**
   * Parse a formula given as a String in a solver-specific file format.
   * @return The same formula in the internal representation.
   * @throws IllegalArgumentException If the string cannot be parsed.
   */
  @Override
  public Formula parse(String s) throws IllegalArgumentException {
    return null;
  }

  /**
   * Given a formula that uses "generic" variables, returns the corresponding
   * one that "instantiates" such variables according to the given SSA map.
   *
   * @param f the generic Formula to instantiate
   * @param ssa the SSAMap to use
   * @return a copy of f in which every "generic" variable is replaced by the
   * corresponding "SSA instance"
   */
  @Override
  public Formula instantiate(Formula f, SSAMap ssa) {
    return null;
  }

  /**
   * Given an "instantiated" formula, returns the corresponding formula in
   * which all the variables are "generic" ones. This is the inverse of the
   * instantiate() method above
   */
  @Override
  @Deprecated
    public Formula uninstantiate(Formula pF) { return null; }

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
  @Override
  public Collection<Formula> extractAtoms(Formula f,
    boolean splitArithEqualities, boolean conjunctionsOnly) {

    TemplateFormula tf = null;
    try {
    	tf = (TemplateFormula)f;
    } catch (ClassCastException e) {}
    if (tf == null) {
    	return null;
    }

    List<TemplateFormula> tfAtoms = tf.extractAtoms(splitArithEqualities, conjunctionsOnly);
    Collection<Formula> atoms = new Vector<Formula>(tfAtoms);

  	return atoms;
  }

  /**
   * Create string representation of a formula in a format which may be dumped
   * to a file.
   */
  @Override
  public String dumpFormula(Formula pT) { return null; }

  /**
   * Looks for uninterpreted functions in the formula and adds bitwise
   * axioms for them.
   */
  @Override
  public Formula getBitwiseAxioms(Formula f) { return null; }

  /**
   * Create the variable representing a predicate for the given atom. There won't
   * be any tracking of the correspondence between the atom and the variable,
   * if it is not done by the caller of this method.
   */
  @Override
  public Formula createPredicateVariable(Formula pAtom) {
    return null;
  }

  /**
   * In a handwritten template, parameters should be
   * named 'pk' for various integer indices k, and variables should match actual
   * program variables, but should be prefixed with 'v'.
   * In order to parse such a template, we should be in TEMPLATE parse mode.
   *
   * If instead we are parsing a path formula, then we should be in
   * the PATHFORMULA parse mode. In this case there will be no parameters,
   * and variable names will simply be exactly as they are in the program.
   */
  public static enum TemplateParseMode {
    TEMPLATE, PATHFORMULA;
  }

	@Override
  public Set<String> extractVariables(Formula f) {
	  return null;
  }

  @Override
  public boolean checkSyntacticEntails(Formula pLeftFormula, Formula pRightFormula) {
    return false;
  }

  @Override
  public Formula[] getArguments(Formula pF) {
    return null;
  }

  @Override
  public Formula makeUIP(String pName, FormulaList pArgs) {
    return null;
  }

  @Override
  public void declareUIP(String pName, int pArgCount) {

  }

  @Override
  public String getVersion() {
    throw new UnsupportedOperationException();
  }
}