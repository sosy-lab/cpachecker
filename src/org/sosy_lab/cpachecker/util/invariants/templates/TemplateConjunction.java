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
package org.sosy_lab.cpachecker.util.invariants.templates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.InfixReln;
import org.sosy_lab.cpachecker.util.invariants.interfaces.Template;
import org.sosy_lab.cpachecker.util.invariants.redlog.Rational;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

public class TemplateConjunction extends TemplateFormula implements
Template {

  private Vector<TemplateConstraint> constraints = new Vector<TemplateConstraint>();

  // ----------------------------------------------------------------
  // Constructors

  public TemplateConjunction() {}

  public TemplateConjunction(Vector<TemplateConstraint> c) {
    constraints = c;
  }

  /**
   * Build this conjunction by conjoining together two existing conjunctions.
   */
  public TemplateConjunction(TemplateConjunction c1, TemplateConjunction c2) {
    constraints = new Vector<TemplateConstraint>();
    if (c1.isTrue()) {
      constraints.addAll(c2.getConstraints());
    } else if (c2.isTrue()) {
      constraints.addAll(c1.getConstraints());
    } else if (c1.isFalse() || c2.isFalse()) {
      constraints.add(new TemplateFalse());
    } else {
      constraints.addAll(c1.getConstraints());
      constraints.addAll(c2.getConstraints());
    }
  }

  /**
   * Make this the conjunction of componentwise equations of the sums
   * in the passed lists.
   */
  public TemplateConjunction(TemplateSumList L1, TemplateSumList L2) {
    Iterator<TemplateSum> I1 = L1.iterator();
    Iterator<TemplateSum> I2 = L2.iterator();
    TemplateSum S1, S2;
    TemplateConstraint C;
    while (I1.hasNext() && I2.hasNext()) {
      S1 = I1.next();
      S2 = I2.next();
      C = new TemplateConstraint(S1, InfixReln.EQUAL, S2);
      constraints.add(C);
    }
  }

  //----------------------------------------------------------------
  // copy

  @Override
  public TemplateConjunction copy() {
    Vector<TemplateConstraint> v = new Vector<TemplateConstraint>();
    for (TemplateConstraint c : constraints) {
      v.add(c.copy());
    }
    TemplateConjunction c = new TemplateConjunction(v);
    return c;
  }

  //----------------------------------------------------------------
  // Alter and Undo

  @Override
  public void alias(AliasingMap amap) {
    TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      tc.alias(amap);
    }
  }

  @Override
  public void unalias() {
    TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      tc.unalias();
    }
  }

  @Override
  public boolean evaluate(HashMap<String,Rational> map) {
    boolean ans = true;
    TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      ans &= tc.evaluate(map);
    }
    return ans;
  }

  @Override
  public void unevaluate() {
    TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      tc.unevaluate();
    }
  }

  @Override
  public void postindex(Map<String,Integer> indices) {
    TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      tc.postindex(indices);
    }
  }

  @Override
  public void preindex(Map<String,Integer> indices) {
    TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      tc.preindex(indices);
    }
  }

  @Override
  public void unindex() {
    TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      tc.unindex();
    }
  }

  @Override
  public Purification purify(Purification pur) {
    TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      pur = tc.purify(pur);
    }
    return pur;
  }

  @Override
  public void unpurify() {
    TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      tc.unpurify();
    }
  }

  //----------------------------------------------------------------
  // Other cascade methods

  @Override
  public Set<String> getAllVariables(VariableWriteMode vwm) {
    HashSet<String> vars = new HashSet<String>();
    TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      vars.addAll(tc.getAllVariables(vwm));
    }
    return vars;
  }

  @Override
  public Set<TemplateVariable> getAllParameters() {
    HashSet<TemplateVariable> params = new HashSet<TemplateVariable>();
    TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      params.addAll(tc.getAllParameters());
    }
    return params;
  }

  @Override
  public HashMap<String,Integer> getMaxIndices(HashMap<String,Integer> map) {
    TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      map = tc.getMaxIndices(map);
    }
    return map;
  }

  @Override
  public TemplateVariableManager getVariableManager() {
    TemplateVariableManager tvm = new TemplateVariableManager();
    TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      tvm.merge( tc.getVariableManager() );
    }
    return tvm;
  }

  @Override
  public Formula translate(FormulaManager fmgr) {
  	Formula form = null;
  	int N = getNumConstraints();
  	if (N == 0) {
  		form = fmgr.makeTrue();
  	} else {
  		assert N >= 1;
      form = getConstraint(0).translate(fmgr);
      Formula augend;
      for (int i = 1; i < N; i++) {
      	augend = getConstraint(i).translate(fmgr);
      	form = fmgr.makeAnd(form, augend);
      }
  	}
  	return form;
  }

  @Override
  public List<TemplateFormula> extractAtoms(boolean sAE, boolean cO) {
  	// Since we only have conjunctions (for now), the boolean cO really has no effect.
  	List<TemplateFormula> atoms = new Vector<TemplateFormula>();
  	TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      atoms.addAll( tc.extractAtoms(sAE, cO) );
    }
  	return atoms;
  }

  @Override
  Set<TermForm> getTopLevelTermForms() {
    Set<TermForm> forms = new HashSet<TermForm>();
    TemplateConstraint tc;
    for (int i = 0; i < getNumConstraints(); i++) {
      tc = getConstraint(i);
      forms.addAll( tc.getTopLevelTermForms() );
    }
    return forms;
  }

  //-----------------------------------------------------------------
  // Other

  @Override
  public Vector<TemplateConstraint> getConstraints() {
    return constraints;
  }

  public int getNumConstraints() {
    return constraints.size();
  }

  public TemplateConstraint getConstraint(int i) {
    return constraints.get(i);
  }

  @Override
  public Set<TemplateTerm> getRHSTerms() {
    Set<TemplateTerm> s = new HashSet<TemplateTerm>();
    for (TemplateConstraint c : constraints) {
      s.addAll(c.getRHSTerms());
    }
    return s;
  }

  public void prefixVariables(String prefix) {
    // Add a prefix to all variable names occurring in this
    // constraint. (Note: this means only variables, not
    // parameters!)
    TemplateConstraint C;
    for (int i = 0; i < getNumConstraints(); i++) {
      C = getConstraint(i);
      C.prefixVariables(prefix);
    }
  }

  @Override
  public boolean isTrue() {
    boolean ans = true;
    for (TemplateConstraint TC : constraints) {
      ans &= TC.isTrue();
    }
    return ans;
  }

  public static boolean isInstance(Object obj) {
    TemplateConjunction c = new TemplateConjunction();
    return c.getClass().isInstance(obj);
  }

  @Override
  public String toString() {
    return toString(VariableWriteMode.PLAIN);
  }

  @Override
  public String toString(VariableWriteMode vwm) {
    String s = "";
    TemplateConstraint C;
    for (int i = 0; i < getNumConstraints(); i++) {
      C = getConstraint(i);
      if (TemplateTrue.isInstance(C)) {
        continue;
      } else if (TemplateFalse.isInstance(C)) {
        s = "false";
        break;
      } else {
        s += " and "+C.toString(vwm);
      }
    }
    if (s.length() > 0) {
      s = s.substring(5);
    }
    return s;
  }

}