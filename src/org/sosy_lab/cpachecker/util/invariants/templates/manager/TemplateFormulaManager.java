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
package org.sosy_lab.cpachecker.util.invariants.templates.manager;

import org.sosy_lab.common.Appender;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateTerm;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateVariable;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;

public class TemplateFormulaManager implements FormulaManager {

  private final TemplateParseMode tpMode;
  private  TemplateNumericFormulaManager nfmgr;
  private  TemplateBooleanFormulaManager bfmgr;
  private  TemplateBitvectorFormulaManager efmgr;
  private  TemplateFunctionFormulaManager ffmgr;
  private  TemplateUnsafeFormulaManager ufmgr;

  public TemplateFormulaManager() {
    tpMode = TemplateParseMode.PATHFORMULA;
    init();
  }

  public TemplateFormulaManager(TemplateParseMode tpm) {
    tpMode = tpm;
    init();
  }

  private void init() {
    nfmgr = new TemplateNumericFormulaManager(this);
    bfmgr = new TemplateBooleanFormulaManager(this);
    efmgr = new TemplateBitvectorFormulaManager(this);
    ffmgr = new TemplateFunctionFormulaManager(this);
    ufmgr = new TemplateUnsafeFormulaManager(this);
  }

  @Override
  public String getVersion() {
    throw new UnsupportedOperationException();
  }

  @Override
  public RationalFormulaManager getRationalFormulaManager() {
    return nfmgr;
  }

  @Override
  public BooleanFormulaManager getBooleanFormulaManager() {
    return bfmgr;
  }

  @Override
  public BitvectorFormulaManager getBitvectorFormulaManager() {
    return efmgr;
  }

  @Override
  public FunctionFormulaManager getFunctionFormulaManager() {
    return ffmgr;
  }

  @Override
  public UnsafeFormulaManager getUnsafeFormulaManager() {
    return ufmgr;
  }

  @SuppressWarnings("unchecked")
  static <T extends TemplateFormula> T toTemplate(Formula f) {
    return (T)f;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    return (FormulaType<T>) toTemplate(pFormula).getFormulaType();
  }

  @Override
  public BooleanFormula parse(String pS) throws IllegalArgumentException {
    return null;
  }

  @Override
  public <T extends Formula> Class<T> getInterface(T pInstance) {
    return getFormulaType(pInstance).getInterfaceType();
  }

  @Override
  public Appender dumpFormula(Formula pT) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * See declaration of TemplateParseMode enum type.
   */
  @SuppressWarnings("unchecked")
  <T extends Formula> T makeVariable(FormulaType<?> type, String pVar, Integer pIdx) {
    TemplateTerm T = new TemplateTerm(type);
    if (tpMode == TemplateParseMode.TEMPLATE) {
      // we are parsing a handwritten template
      if (pVar != null && pVar.length() > 0 && pVar.startsWith("v")) {
        // it is a program variable
        pVar = pVar.substring(1);
        TemplateVariable V = new TemplateVariable(type, pVar, pIdx);
        T.setVariable(V);
      } else {
        // it is a parameter
        TemplateVariable V = new TemplateVariable(type, pVar, pIdx);
        T.setParameter(V);
      }
    } else {
      // we are parsing a path formula
      TemplateVariable V = new TemplateVariable(type, pVar, pIdx);
      T.setVariable(V);
    }
    return (T)T;
  }
  // ----------------- Uninterpreted functions -----------------


  // ----------------- Other formulas -----------------
//
//  @Override
//  public BooleanFormula makeString(int pI) {
//    return new NonTemplate();
//  }
//
//  /**
//   * See declaration of TemplateParseMode enum type.
//   */
//  @Override
//  public BooleanFormula makeVariable(String pVar, int pIdx) {
//    Integer i = Integer.valueOf(pIdx);
//    return makeVariable(pVar, i);
//  }
//
//  @Override
//  public BooleanFormula makeVariable(String pVar) {
//    return makeVariable(pVar, null);
//  }
//
//
//  @Override
//  public BooleanFormula makePredicateVariable(String pVar, int pIdx) {
//    return new NonTemplate();
//  }
//
//  @Override
//  public BooleanFormula makeAssignment(BooleanFormula pF1, BooleanFormula pF2) {
//    BooleanFormula F = null;
//    try {
//      TemplateSum s1 = (TemplateSum) pF1;
//      TemplateSum s2 = (TemplateSum) pF2;
//      F = new TemplateConstraint(s1, InfixReln.EQUAL, s2);
//    } catch (ClassCastException e) {
//      System.err.println(e.getMessage());
//      F = new NonTemplate();
//    }
//    return F;
//  }
//
//  // ----------------- Convert to list -----------------
//
//  @Override
//  public FormulaList makeList(BooleanFormula pF) {
//    BooleanFormula[] fs = {pF};
//    return new TemplateFormulaList(fs);
//  }
//
//  @Override
//  public FormulaList makeList(BooleanFormula pF1, BooleanFormula pF2) {
//    BooleanFormula[] fs = {pF1, pF2};
//    return new TemplateFormulaList(fs);
//  }
//
//  @Override
//  public FormulaList makeList(List<BooleanFormula> pFs) {
//    return new TemplateFormulaList(pFs);
//  }
//
//  public FormulaList makeList(BooleanFormula... pF) {
//    return new TemplateFormulaList(pF);
//  }
//
//
//  @Override
//  public Map<String, BooleanFormula> parseFormulas(String pS) throws IllegalArgumentException {
//    throw new UnsupportedOperationException();
//  }
//
//  /**
//   * Given a formula that uses "generic" variables, returns the corresponding
//   * one that "instantiates" such variables according to the given SSA map.
//   *
//   * @param f the generic Formula to instantiate
//   * @param ssa the SSAMap to use
//   * @return a copy of f in which every "generic" variable is replaced by the
//   * corresponding "SSA instance"
//   */
//  @Override
//  public BooleanFormula instantiate(BooleanFormula f, SSAMap ssa) {
//    return null;
//  }
//
//  /**
//   * Given an "instantiated" formula, returns the corresponding formula in
//   * which all the variables are "generic" ones. This is the inverse of the
//   * instantiate() method above
//   */
//  @Override
//  @Deprecated
//    public BooleanFormula uninstantiate(BooleanFormula pF) { return null; }
//
//  /**
//   * Extracts the atoms from the given formula. Any SSA indices are removed
//   * from the symbols in the atoms.
//   * @param f the formula to operate on
//   * @param splitArithEqualities if true, return (x <= y) and (y <= x)
//   *                             instead of (x = y)
//   * @param conjunctionsOnly if true, don't extract atoms, but only top-level
//   *                         conjuncts. For example, if called on:
//   *                         a & (b | c), the result will be [a, (b | c)]
//   *                         instead of [a, b, c]
//   * @return a collection of (atomic) formulas
//   */
//  @Override
//  public Collection<BooleanFormula> extractAtoms(BooleanFormula f,
//    boolean splitArithEqualities, boolean conjunctionsOnly) {
//
//    TemplateFormula tf = null;
//    try {
//      tf = (TemplateFormula)f;
//    } catch (ClassCastException e) {}
//    if (tf == null) {
//      return null;
//    }
//
//    List<TemplateFormula> tfAtoms = tf.extractAtoms(splitArithEqualities, conjunctionsOnly);
//    Collection<BooleanFormula> atoms = new Vector<>(tfAtoms);
//
//    return atoms;
//  }


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
}
