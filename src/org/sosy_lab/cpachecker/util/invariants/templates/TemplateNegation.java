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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

public class TemplateNegation extends TemplateBoolean {

  private final TemplateBoolean arg;

  public TemplateNegation(TemplateBoolean tb) {
    arg = tb;
  }

  /**
   * You should use this method to negate a TemplateBoolean, rather
   * than accessing the constructor directly. This way we can enforce
   * immediate cancellation of double-negatives.
   */
  public static TemplateBoolean negate(TemplateBoolean tb) {
    // We cancel double-negations.
    if (tb instanceof TemplateNegation) {
      TemplateNegation tn = (TemplateNegation) tb;
      tb = tn.arg;
    } else {
      tb = new TemplateNegation(tb);
    }
    return tb;
  }

  //----------------------------------------------------------------
  //Boolean manipulation

  @Override
  public TemplateBoolean makeCNF() {
    TemplateBoolean tb = null;
    if (arg instanceof TemplateConstraint) {
      // In this case the argument is a literal, so this
      // negation is already in CNF.
      tb = this.copy();
    } else {
      tb = arg.logicNegate();
      tb = tb.makeCNF();
    }
    return tb;
  }

  @Override
  public TemplateBoolean makeDNF() {
    TemplateBoolean tb = null;
    if (arg instanceof TemplateConstraint) {
      // In this case the argument is a literal, so this
      // negation is already in DNF.
      tb = this.copy();
    } else {
      tb = arg.logicNegate();
      tb = tb.makeDNF();
    }
    return tb;
  }

  @Override
  public TemplateBoolean logicNegate() {
    return arg;
  }

  @Override
  public TemplateBoolean absorbNegations() {
    TemplateBoolean tb = arg.logicNegate();
    return tb.absorbNegations();
  }

  @Override
  public void flatten() {
    arg.flatten();
  }

  //----------------------------------------------------------------
  // copy

  @Override
  public TemplateNegation copy() {
    return new TemplateNegation(arg.copy());
  }

  //----------------------------------------------------------------
  // Alter and Undo

  @Override
  public void alias(AliasingMap amap) {
    arg.alias(amap);
  }

  @Override
  public void unalias() {
    arg.unalias();
  }

  @Override
  public boolean evaluate(Map<String, Rational> map) {
    boolean ans = arg.evaluate(map);
    return ans;
  }

  @Override
  public void unevaluate() {
    arg.unevaluate();
  }

  @Override
  public void postindex(Map<String, Integer> indices) {
    arg.postindex(indices);
  }

  @Override
  public void preindex(Map<String, Integer> indices) {
    arg.preindex(indices);
  }

  @Override
  public void unindex() {
    arg.unindex();
  }

  @Override
  public Purification purify(Purification pur) {
    pur = arg.purify(pur);
    return pur;
  }

  @Override
  public void unpurify() {
    arg.unpurify();
  }

  //----------------------------------------------------------------
  // Other cascade methods

  @Override
  public Vector<TemplateConstraint> getConstraints() {
    return arg.getConstraints();
  }

  @Override
  public Set<TemplateVariable> getAllVariables() {
    return arg.getAllVariables();
  }

  @Override
  public Set<TemplateVariable> getAllParameters() {
    return arg.getAllParameters();
  }

  @Override
  public HashMap<String, Integer> getMaxIndices(HashMap<String, Integer> map) {
    map = arg.getMaxIndices(map);
    return map;
  }

  @Override
  public TemplateVariableManager getVariableManager() {
    TemplateVariableManager tvm = new TemplateVariableManager();
    tvm.merge(arg.getVariableManager());
    return tvm;
  }

  @Override
  public BooleanFormula translate(FormulaManagerView fmgr) {
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    BooleanFormula form = arg.translate(fmgr);
    form = bfmgr.not(form);
    return form;
  }

  @Override
  public List<TemplateFormula> extractAtoms(boolean sAE, boolean cO) {
    List<TemplateFormula> atoms = new Vector<>();
    if (cO) {
      atoms.add(this);
    } else {
      atoms.addAll(arg.extractAtoms(sAE, cO));
    }
    return atoms;
  }

  @Override
  public Set<TermForm> getTopLevelTermForms() {
    return arg.getTopLevelTermForms();
  }


  //-----------------------------------------------------------------
  // Other

  @Override
  public Set<TemplateTerm> getRHSTerms() {
    return arg.getRHSTerms();
  }

  @Override
  public void prefixVariables(String pPrefix) {
    arg.prefixVariables(pPrefix);
  }

  @Override
  public boolean isTrue() {
    return arg.isFalse();
  }

  @Override
  public boolean isFalse() {
    return arg.isTrue();
  }

  @Override
  public String toString() {
    return toString(VariableWriteMode.PLAIN);
  }

  @Override
  public String toString(VariableWriteMode vwm) {
    String s = "not("+arg.toString(vwm)+")";
    return s;
  }

}
