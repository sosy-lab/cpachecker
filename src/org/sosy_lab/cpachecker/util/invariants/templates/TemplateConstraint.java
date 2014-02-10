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
package org.sosy_lab.cpachecker.util.invariants.templates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.Coeff;
import org.sosy_lab.cpachecker.util.invariants.InfixReln;
import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.invariants.interfaces.Constraint;
import org.sosy_lab.cpachecker.util.invariants.interfaces.VariableManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

public class TemplateConstraint extends TemplateBoolean implements Constraint {

  private TemplateSum LHS = null;
  private InfixReln reln = null;
  private TemplateSum RHS = null;

  public TemplateSum getLeft() {
    return LHS;
  }
  public TemplateSum getRight() {
    return RHS;
  }

// ------------------------------------------------------------------
// Constructors

  public TemplateConstraint() {
    LHS = new TemplateSum(FormulaType.RationalType);
    RHS = new TemplateSum(FormulaType.RationalType);
  }

  public TemplateConstraint(TemplateSum s1, InfixReln R, TemplateSum s2) {
    construct(s1, R, s2);
  }

  /*
   * Special constructor for comparing a variable to 0.
   */
  public TemplateConstraint(TemplateVariable v, InfixReln R) {
    TemplateTerm t = new TemplateTerm(v.getFormulaType());
    t.setParameter(v);
    TemplateTerm z = TemplateTerm.makeZero(v.getFormulaType());
    LHS = t;
    reln = R;
    RHS = z;
  }

  private void construct(TemplateSum s1, InfixReln R, TemplateSum s2) {
    reln = R;
    // Store in normal form.
    TemplateSum C1 = s1.getConstantPart();
    TemplateSum V1 = s1.getNonConstantPart();
    TemplateSum C2 = s2.getConstantPart();
    TemplateSum V2 = s2.getNonConstantPart();

    LHS = TemplateSum.subtract(V1, V2);
    RHS = TemplateSum.subtract(C2, C1);
  }

//------------------------------------------------------------------
// copy

  @Override
  public TemplateConstraint copy() {
    TemplateConstraint c = new TemplateConstraint(LHS.copy(), reln, RHS.copy());
    return c;
  }

  //----------------------------------------------------------------
  // boolean operations

  @Override
  public void flatten() {}

  @Override
  public TemplateBoolean makeCNF() {
    Vector<TemplateBoolean> v = new Vector<>(1);
    v.add(this);
    return new TemplateConjunction(v);
  }

  @Override
  public TemplateBoolean makeDNF() {
    Vector<TemplateBoolean> v = new Vector<>(1);
    v.add(this);
    return new TemplateDisjunction(v);
  }

  @Override
  public TemplateBoolean logicNegate() {
    TemplateBoolean tb = null;
    switch (reln) {
    case LEQ:
      tb = new TemplateConstraint(RHS.copy(), InfixReln.LT, LHS.copy());
      break;
    case LT:
      tb = new TemplateConstraint(RHS.copy(), InfixReln.LEQ, LHS.copy());
      break;
    case EQUAL:
      Vector<TemplateBoolean> v = new Vector<>(2);
      TemplateConstraint tc1 = new TemplateConstraint(LHS.copy(), InfixReln.LT, RHS.copy());
      TemplateConstraint tc2 = new TemplateConstraint(RHS.copy(), InfixReln.LT, LHS.copy());
      v.add(tc1);
      v.add(tc2);
      tb = new TemplateDisjunction(v);
      break;
    }
    return tb;
  }

  @Override
  public TemplateBoolean absorbNegations() {
    return this;
  }

//------------------------------------------------------------------
// Alter and Undo

  @Override
  public void alias(AliasingMap amap) {
    LHS.alias(amap);
    RHS.alias(amap);
  }

  @Override
  public void unalias() {
    LHS.unalias();
    RHS.unalias();
  }

  @Override
  public boolean evaluate(Map<String, Rational> map) {
    boolean ans = true;
    ans &= LHS.evaluate(map);
    ans &= RHS.evaluate(map);
    return ans;
  }

  @Override
  public void unevaluate() {
    LHS.unevaluate();
    RHS.unevaluate();
  }

  @Override
  public void postindex(Map<String, Integer> indices) {
    LHS.postindex(indices);
    RHS.postindex(indices);
  }

  @Override
  public void preindex(Map<String, Integer> indices) {
    LHS.preindex(indices);
    RHS.preindex(indices);
  }

  @Override
  public void unindex() {
    LHS.unindex();
    RHS.unindex();
  }

  @Override
  public Purification purify(Purification pur) {
    pur = LHS.purify(pur);
    pur = RHS.purify(pur);
    return pur;
  }

  @Override
  public void unpurify() {
    LHS.unpurify();
    RHS.unpurify();
  }

//------------------------------------------------------------------
// Other cascade methods

  @Override
  public Set<TemplateVariable> getAllVariables() {
    HashSet<TemplateVariable> vars = new HashSet<>();
    vars.addAll(LHS.getAllVariables());
    vars.addAll(RHS.getAllVariables());
    return vars;
  }

  @Override
  public Set<TemplateVariable> getAllParameters() {
    HashSet<TemplateVariable> params = new HashSet<>();
    params.addAll(LHS.getAllParameters());
    params.addAll(RHS.getAllParameters());
    return params;
  }

  public Set<TemplateVariable> getTopLevelParameters() {
    HashSet<TemplateVariable> tlp = new HashSet<>();
    tlp.addAll(LHS.getTopLevelParameters());
    tlp.addAll(RHS.getTopLevelParameters());
    return tlp;
  }

  @Override
  public HashMap<String, Integer> getMaxIndices(HashMap<String, Integer> map) {
    map = LHS.getMaxIndices(map);
    map = RHS.getMaxIndices(map);
    return map;
  }

  @Override
  public TemplateVariableManager getVariableManager() {
    TemplateVariableManager tvm = new TemplateVariableManager();
    tvm.merge(LHS.getVariableManager());
    tvm.merge(RHS.getVariableManager());
    return tvm;
  }

  @Override
  public void prefixVariables(String prefix) {
    // All terms with variables should be in the LHS, so we only
    // work on that side.
    LHS.prefixVariables(prefix);
  }

  @Override
  public BooleanFormula translate(FormulaManagerView fmgr) {
    BooleanFormula form = null;
    Formula lhs = LHS.translate(fmgr);
    Formula rhs = RHS.translate(fmgr);
    switch (reln) {
    case EQUAL: form = fmgr.makeEqual(lhs, rhs); break;
    case LEQ:   form = fmgr.makeLessOrEqual(lhs, rhs, true);   break;
    case LT:    form = fmgr.makeLessThan(lhs, rhs, true); break;
    }
    return form;
  }

  @Override
  public List<TemplateFormula> extractAtoms(boolean sAE, boolean cO) {
    List<TemplateFormula> atoms = new Vector<>();
    if (!sAE) {
      atoms.add(this);
    } else {
      // In this case we want to split equations into pairs of inequalities.
      TemplateConstraint tc1 = new TemplateConstraint(LHS, InfixReln.LEQ, RHS);
      TemplateConstraint tc2 = new TemplateConstraint(RHS, InfixReln.LEQ, LHS);
      atoms.add(tc1);
      atoms.add(tc2);
    }
    return atoms;
  }

  @Override
  public Set<TemplateUIF> getAllTopLevelUIFs() {
    return LHS.getAllTopLevelUIFs();
  }

  @Override
  public Set<TemplateVariable> getTopLevelLHSParameters() {
    return LHS.getTopLevelParameters();
  }

  @Override
  public Set<TemplateTerm> getTopLevelTerms() {
    List<TemplateTerm> terms = LHS.getTerms();
    terms.addAll(RHS.getTerms());
    Set<TemplateTerm> set = new HashSet<>(terms);
    return set;
  }

  @Override
  public Set<TemplateVariable> getAllPurificationVariables() {
    Set<TemplateVariable> pvs = LHS.getAllPurificationVariables();
    pvs.addAll(RHS.getAllPurificationVariables());
    return pvs;
  }

  @Override
  public Set<TermForm> getTopLevelTermForms() {
    // Get a copy of the LHS.
    TemplateSum copy = LHS.copy();

    // By purifying and unpurifying, we can normalize all
    // the sums in the entire syntax tree, without changing
    // anything else.
    // This makes it so that any two occurrences of a given
    // form will be identical except for coefficients.
    copy.purify(new Purification());
    copy.unpurify();

    // Forming the set throws away all but one of each form.
    Vector<TemplateTerm> terms = copy.getTerms();
    Set<TermForm> forms = new HashSet<>();
    for (TemplateTerm t : terms) {
      forms.add(new TermForm(t));
    }
    return forms;
  }

//------------------------------------------------------------------
// Other

  @Override
  public Vector<TemplateConstraint> getConstraints() {
    Vector<TemplateConstraint> v = new Vector<>();
    v.add(this);
    return v;
  }

  @Override
  public Set<TemplateTerm> getRHSTerms() {
    Set<TemplateTerm> s = new HashSet<>(RHS.getTerms());
    return s;
  }

  @Override
  public boolean isTrue() {
    return false;
  }

  @Override
  public boolean isFalse() {
    return false;
  }

  @Override
  public List<Coeff> getNormalFormCoeffs(VariableManager vmgr, VariableWriteMode vwm) {
    return LHS.getCoeffsWithParams(vwm, vmgr);
  }

  @Override
  public Coeff getNormalFormConstant(VariableWriteMode vwm) {
    return new Coeff(RHS, vwm);
  }

  /*
   * This method is for use when we assume that the program variables
   * are integers, and we transform strict inequalities into lax.
   */
  @Override
  public Coeff getNormalFormConstantMinusOne(VariableWriteMode vwm) {
    return new Coeff(RHS.minusOne(), vwm);
  }

  @Override
  public InfixReln getInfixReln() {
    return reln;
  }

  @Override
  public String toString() {
    return toString(VariableWriteMode.PLAIN);
  }

  @Override
  public String toString(VariableWriteMode vwm) {
    String s = "";
    if (LHS!=null && reln!=null && RHS!=null) {
      s = LHS.toString(vwm);
      //System.out.println("LHS returned: "+s);
      s += " "+reln.toString()+" ";
      s += RHS.toString(vwm);
    }
    return s;
  }

}
