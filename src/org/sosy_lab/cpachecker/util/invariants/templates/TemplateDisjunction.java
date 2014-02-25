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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

public class TemplateDisjunction extends TemplateBoolean {

  private Vector<TemplateBoolean> disjuncts = new Vector<>();

  //----------------------------------------------------------------
  // Constructors

  public TemplateDisjunction() {}

  public TemplateDisjunction(Vector<TemplateBoolean> c) {
    disjuncts = c;
  }

  /**
   * Disjoin two booleans.
   */
  public TemplateDisjunction(TemplateBoolean b1, TemplateBoolean b2) {
    disjuncts = new Vector<>();
    disjuncts.add(b1);
    disjuncts.add(b2);
    flatten();
  }

  /**
   * This method should be used instead of using the constructor directly.
   * If either of b1 or b2 is False, the other will simply be returned.
   * If either of b1 or b2 is True, then TemplateTrue will be returned.
   * Otherwise the disjunction of b1 and b2 is actually constructed.
   */
  public static TemplateBoolean disjoin(TemplateBoolean b1, TemplateBoolean b2) {
    TemplateBoolean tb;
    if (b1.isFalse()) {
      tb = b2;
    } else if (b2.isFalse()) {
      tb = b1;
    } else if (b1.isTrue() || b2.isTrue()) {
      tb = new TemplateTrue();
    } else {
      tb = new TemplateDisjunction(b1, b2);
    }
    return tb;
  }

  //----------------------------------------------------------------
  //Boolean manipulation

  /**
   * Supposing the conjunctions in tcs are the disjuncts in a disjunction,
   * we apply the distributive law to expand that disjunction into a
   * conjunction, and we return the result.
   */
  public static TemplateConjunction distribute(Collection<TemplateConjunction> tcs) {
    Vector<TemplateConjunction> tclist = new Vector<>(tcs);
    TemplateConjunction conj = null;
    int N = tclist.size();
    if (N == 1) {
      // There is only one conjunction. It is already expanded.
      conj = tclist.get(0);
    } else if (N == 2) {
      // There are precisely two conjunctions. This is where we actually
      // do the expansion.
      TemplateConjunction a = tclist.get(0);
      TemplateConjunction b = tclist.get(1);
      Vector<TemplateBoolean> conjuncts = new Vector<>();
      Vector<TemplateBoolean> aC = a.getConjuncts();
      Vector<TemplateBoolean> bC = b.getConjuncts();
      TemplateDisjunction d;
      Vector<TemplateBoolean> p;
      for (TemplateBoolean ac : aC) {
        for (TemplateBoolean bc : bC) {
          p = new Vector<>(2);
          p.add(ac);
          p.add(bc);
          d = new TemplateDisjunction(p);
          conjuncts.add(d);
        }
      }
      conj = new TemplateConjunction(conjuncts);
    } else if (N >= 3) {
      // There are more than two. Expand all but the first; then
      // expand the list containing just the result preceded by the first.
      Vector<TemplateConjunction> tail = new Vector<>(N-1);
      for (int i = 1; i < N; i++) {
        tail.add(tclist.get(i));
      }
      TemplateConjunction b = TemplateDisjunction.distribute(tail);
      TemplateConjunction a = tclist.get(0);
      Vector<TemplateConjunction> pair = new Vector<>(2);
      pair.add(a);
      pair.add(b);
      conj = TemplateDisjunction.distribute(pair);
    }
    return conj;
  }

  @Override
  public TemplateBoolean makeCNF() {
    flatten();
    TemplateBoolean tb;
    TemplateConjunction tc;

    // Partition the disjuncts into literals and conjunctions.
    Vector<TemplateBoolean> literals = new Vector<>();
    ArrayDeque<TemplateConjunction> conjunctions = new ArrayDeque<>();
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i);
      if (tb instanceof TemplateConjunction) {
        tc = (TemplateConjunction) tb.makeCNF();
        conjunctions.add(tc);
      } else if (tb instanceof TemplateNegation) {
        tb = tb.makeCNF();
        if (tb instanceof TemplateConjunction) {
          tc = (TemplateConjunction) tb;
          conjunctions.add(tc);
        } else {
          literals.add(tb);
        }
      } else {
        literals.add(tb);
      }
    }

    // If there are any literals,
    // create a disjunction dj of all the literals in this disjunction.
    // Then make k0 a conjunction having dj as its only conjunct.
    // Then add k0 to the beginning of the deque 'conjunctions'.
    if (literals.size() > 0) {
      TemplateDisjunction dj = new TemplateDisjunction(literals);
      Vector<TemplateBoolean> justDj = new Vector<>();
      justDj.add(dj);
      TemplateConjunction k0 = new TemplateConjunction(justDj);
      conjunctions.addFirst(k0);
    }

    // Now apply the distributive law to expand this disjunction of
    // conjunctions into a conjunction of disjunctions.
    tc = TemplateDisjunction.distribute(conjunctions);
    tc.flatten();
    return tc;
  }

  @Override
  public TemplateBoolean makeDNF() {
    // This object may get altered, in that it will be flattened.
    // We could avoid this, but we don't really care.
    flatten();
    Vector<TemplateBoolean> newdisjuncts = new Vector<>();
    TemplateBoolean tb;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i);
      tb = tb.makeDNF();
      newdisjuncts.add(tb);
    }
    tb = new TemplateDisjunction(newdisjuncts);
    // Must flatten in case some disjuncts were conjunctions, which had to
    // expand and become disjunctions.
    tb.flatten();
    return tb;
  }

  @Override
  public TemplateBoolean logicNegate() {
    Vector<TemplateBoolean> conjuncts = new Vector<>();
    TemplateBoolean tb;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i);
      tb = TemplateNegation.negate(tb);
      conjuncts.add(tb);
    }
    return new TemplateConjunction(conjuncts);
  }

  @Override
  public TemplateBoolean absorbNegations() {
    Vector<TemplateBoolean> disjuncts = new Vector<>();
    TemplateBoolean tb;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i).absorbNegations();
      disjuncts.add(tb);
    }
    return new TemplateDisjunction(disjuncts);
  }

  @Override
  public void flatten() {
    Vector<TemplateBoolean> newdisjuncts = new Vector<>();
    TemplateBoolean tb;
    Vector<TemplateBoolean> subdisjuncts;
    TemplateDisjunction td;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i);
      tb.flatten();
      if (this.getClass().isInstance(tb)) {
        td = (TemplateDisjunction) tb;
        subdisjuncts = td.getDisjuncts();
        newdisjuncts.addAll(subdisjuncts);
      } else {
        newdisjuncts.add(tb);
      }
    }
    disjuncts = newdisjuncts;
  }

  //----------------------------------------------------------------
  // copy

  @Override
  public TemplateDisjunction copy() {
    Vector<TemplateBoolean> v = new Vector<>();
    for (TemplateBoolean c : disjuncts) {
      v.add(c.copy());
    }
    TemplateDisjunction c = new TemplateDisjunction(v);
    return c;
  }

  //----------------------------------------------------------------
  // Alter and Undo

  @Override
  public void alias(AliasingMap amap) {
    TemplateBoolean tb;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i);
      tb.alias(amap);
    }
  }

  @Override
  public void unalias() {
    TemplateBoolean tb;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i);
      tb.unalias();
    }
  }

  @Override
  public boolean evaluate(Map<String, Rational> map) {
    boolean ans = true;
    TemplateBoolean tb;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i);
      ans &= tb.evaluate(map);
    }
    return ans;
  }

  @Override
  public void unevaluate() {
    TemplateBoolean tb;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i);
      tb.unevaluate();
    }
  }

  @Override
  public void postindex(Map<String, Integer> indices) {
    TemplateBoolean tb;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i);
      tb.postindex(indices);
    }
  }

  @Override
  public void preindex(Map<String, Integer> indices) {
    TemplateBoolean tb;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i);
      tb.preindex(indices);
    }
  }

  @Override
  public void unindex() {
    TemplateBoolean tb;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i);
      tb.unindex();
    }
  }

  @Override
  public Purification purify(Purification pur) {
    TemplateBoolean tb;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i);
      pur = tb.purify(pur);
    }
    return pur;
  }

  @Override
  public void unpurify() {
    TemplateBoolean tb;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i);
      tb.unpurify();
    }
  }

  //----------------------------------------------------------------
  // Other cascade methods

  @Override
  public Vector<TemplateConstraint> getConstraints() {
    Vector<TemplateConstraint> v = new Vector<>();
    TemplateBoolean tb;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tb = getDisjunct(i);
      v.addAll(tb.getConstraints());
    }
    return v;
  }

  @Override
  public Set<TemplateVariable> getAllVariables() {
    HashSet<TemplateVariable> vars = new HashSet<>();
    TemplateBoolean tc;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tc = getDisjunct(i);
      vars.addAll(tc.getAllVariables());
    }
    return vars;
  }

  @Override
  public Set<TemplateVariable> getAllParameters() {
    HashSet<TemplateVariable> params = new HashSet<>();
    TemplateBoolean tc;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tc = getDisjunct(i);
      params.addAll(tc.getAllParameters());
    }
    return params;
  }

  @Override
  public HashMap<String, Integer> getMaxIndices(HashMap<String, Integer> map) {
    TemplateBoolean tc;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tc = getDisjunct(i);
      map = tc.getMaxIndices(map);
    }
    return map;
  }

  @Override
  public TemplateVariableManager getVariableManager() {
    TemplateVariableManager tvm = new TemplateVariableManager();
    TemplateBoolean tc;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tc = getDisjunct(i);
      tvm.merge(tc.getVariableManager());
    }
    return tvm;
  }

  @Override
  public BooleanFormula translate(FormulaManagerView fmgr) {
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    BooleanFormula form = null;
    int N = getNumDisjuncts();
    if (N == 0) {
      form = bfmgr.makeBoolean(false);
    } else {
      assert N >= 1;
      form = getDisjunct(0).translate(fmgr);
      BooleanFormula augend;
      for (int i = 1; i < N; i++) {
        augend = getDisjunct(i).translate(fmgr);
        form = bfmgr.or(form, augend);
      }
    }
    return form;
  }

  @Override
  public List<TemplateFormula> extractAtoms(boolean sAE, boolean cO) {
    List<TemplateFormula> atoms = new Vector<>();
    if (cO) {
      atoms.add(this);
    } else {
      TemplateBoolean tc;
      for (int i = 0; i < getNumDisjuncts(); i++) {
        tc = getDisjunct(i);
        atoms.addAll(tc.extractAtoms(sAE, cO));
      }
    }
    return atoms;
  }

  @Override
  public Set<TermForm> getTopLevelTermForms() {
    Set<TermForm> forms = new HashSet<>();
    TemplateBoolean tc;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      tc = getDisjunct(i);
      forms.addAll(tc.getTopLevelTermForms());
    }
    return forms;
  }


  //-----------------------------------------------------------------
  // Other

  public Vector<TemplateBoolean> getDisjuncts() {
    return disjuncts;
  }

  public int getNumDisjuncts() {
    return disjuncts.size();
  }

  public TemplateBoolean getDisjunct(int i) {
    return disjuncts.get(i);
  }

  @Override
  public Set<TemplateTerm> getRHSTerms() {
    Set<TemplateTerm> s = new HashSet<>();
    for (TemplateBoolean c : disjuncts) {
      s.addAll(c.getRHSTerms());
    }
    return s;
  }

  @Override
  public void prefixVariables(String prefix) {
    // Add a prefix to all variable names occurring in all
    // constraints. (Note: this means only variables, not
    // parameters!)
    TemplateBoolean C;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      C = getDisjunct(i);
      C.prefixVariables(prefix);
    }
  }

  @Override
  public boolean isTrue() {
    boolean ans = false;
    for (TemplateBoolean TB : disjuncts) {
      ans |= TB.isTrue();
    }
    return ans;
  }

  @Override
  public String toString() {
    return toString(VariableWriteMode.PLAIN);
  }

  @Override
  public String toString(VariableWriteMode vwm) {
    String s = "";
    TemplateBoolean C;
    for (int i = 0; i < getNumDisjuncts(); i++) {
      C = getDisjunct(i);
      if (TemplateFalse.isInstance(C)) {
        continue;
      } else if (TemplateTrue.isInstance(C)) {
        s = "true";
        break;
      } else {
        s += " OR "+C.toString(vwm);
      }
    }
    if (s.length() > 0) {
      s = s.substring(4);
    }
    s = "( "+s+" )";
    return s;
  }

}



















