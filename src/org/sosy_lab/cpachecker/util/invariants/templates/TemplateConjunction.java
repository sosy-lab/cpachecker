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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.InfixReln;
import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.invariants.interfaces.Template;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

public class TemplateConjunction extends TemplateBoolean implements Template {

  private Vector<TemplateBoolean> conjuncts = new Vector<>();

  // ----------------------------------------------------------------
  // Constructors

  public TemplateConjunction() {}

  public TemplateConjunction(Vector<TemplateBoolean> c) {
    conjuncts = c;
  }

  /**
   * Conjoin two booleans.
   */
  public TemplateConjunction(TemplateBoolean b1, TemplateBoolean b2) {
    conjuncts = new Vector<>();
    conjuncts.add(b1);
    conjuncts.add(b2);
    flatten();
  }

  /**
   * This method should be used instead of using the constructor directly.
   * If either of b1 or b2 is True, the other will simply be returned.
   * If either of b1 or b2 is False, then a TemplateFalse will be returned.
   * Otherwise the conjunction of b1 and b2 is actually constructed.
   */
  public static TemplateBoolean conjoin(TemplateBoolean b1, TemplateBoolean b2) {
    TemplateBoolean tb;
    if (b1.isTrue()) {
      tb = b2;
    } else if (b2.isTrue()) {
      tb = b1;
    } else if (b1.isFalse() || b2.isFalse()) {
      tb = new TemplateFalse();
    } else {
      tb = new TemplateConjunction(b1, b2);
    }
    return tb;
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
      conjuncts.add(C);
    }
  }

  //----------------------------------------------------------------
  // Boolean manipulation

  /**
   * Supposing the disjunctions in tds are the conjuncts in a conjunction,
   * we apply the distributive law to expand that conjunction into a
   * disjunction, and we return the result.
   */
  public static TemplateDisjunction distribute(Collection<TemplateDisjunction> tds) {
    Vector<TemplateDisjunction> tdlist = new Vector<>(tds);
    TemplateDisjunction disj = null;
    int N = tdlist.size();
    if (N == 1) {
      // There is only one disjunction. It is already expanded.
      disj = tdlist.get(0);
    } else if (N == 2) {
      // There are precisely two disjunctions. This is where we actually
      // do the expansion.
      TemplateDisjunction a = tdlist.get(0);
      TemplateDisjunction b = tdlist.get(1);
      Vector<TemplateBoolean> disjuncts = new Vector<>();
      Vector<TemplateBoolean> aC = a.getDisjuncts();
      Vector<TemplateBoolean> bC = b.getDisjuncts();
      TemplateConjunction d;
      Vector<TemplateBoolean> p;
      for (TemplateBoolean ac : aC) {
        for (TemplateBoolean bc : bC) {
          p = new Vector<>(2);
          p.add(ac);
          p.add(bc);
          d = new TemplateConjunction(p);
          disjuncts.add(d);
        }
      }
      disj = new TemplateDisjunction(disjuncts);
    } else if (N >= 3) {
      // There are more than two. Expand all but the first; then
      // expand the list containing just the result preceded by the first.
      Vector<TemplateDisjunction> tail = new Vector<>(N-1);
      for (int i = 1; i < N; i++) {
        tail.add(tdlist.get(i));
      }
      TemplateDisjunction b = TemplateConjunction.distribute(tail);
      TemplateDisjunction a = tdlist.get(0);
      Vector<TemplateDisjunction> pair = new Vector<>(2);
      pair.add(a);
      pair.add(b);
      disj = TemplateConjunction.distribute(pair);
    }
    return disj;
  }

  @Override
  public TemplateBoolean makeCNF() {
    // This object may get altered, in that it will be flattened.
    // We could avoid this, but we don't really care.
    flatten();
    Vector<TemplateBoolean> newconjuncts = new Vector<>();
    TemplateBoolean tb;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tb = getConjunct(i);
      tb = tb.makeCNF();
      newconjuncts.add(tb);
    }
    tb = new TemplateConjunction(newconjuncts);
    // Must flatten in case some conjuncts were disjunctions, which had to
    // expand and become conjunctions.
    tb.flatten();
    return tb;
  }

  @Override
  public TemplateBoolean makeDNF() {
    flatten();
    TemplateBoolean tb;
    TemplateDisjunction td;

    // Partition the conjuncts into literals and disjunctions.
    Vector<TemplateBoolean> literals = new Vector<>();
    ArrayDeque<TemplateDisjunction> disjunctions = new ArrayDeque<>();
    for (int i = 0; i < getNumConjuncts(); i++) {
      tb = getConjunct(i);
      if (tb instanceof TemplateDisjunction) {
        td = (TemplateDisjunction) tb.makeDNF();
        disjunctions.add(td);
      } else if (tb instanceof TemplateNegation) {
        tb = tb.makeDNF();
        if (tb instanceof TemplateDisjunction) {
          td = (TemplateDisjunction) tb;
          disjunctions.add(td);
        } else {
          literals.add(tb);
        }
      } else {
        literals.add(tb);
      }
    }

    // If there are any literals, then
    // create a conjunction cj of all the literals in this conjunction.
    // Then make k0 a disjunction having cj as its only disjunct.
    // Then add k0 to the beginning of the deque 'disjunctions'.
    if (literals.size() > 0) {
      TemplateConjunction cj = new TemplateConjunction(literals);
      Vector<TemplateBoolean> justCj = new Vector<>();
      justCj.add(cj);
      TemplateDisjunction k0 = new TemplateDisjunction(justCj);
      disjunctions.addFirst(k0);
    }

    // Now apply the contributive law to expand this conjunction of
    // disjunctions into a disjunction of conjunctions.
    td = TemplateConjunction.distribute(disjunctions);
    td.flatten();
    return td;
  }

  @Override
  public TemplateBoolean logicNegate() {
    Vector<TemplateBoolean> disjuncts = new Vector<>();
    TemplateBoolean tb;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tb = getConjunct(i);
      tb = TemplateNegation.negate(tb);
      disjuncts.add(tb);
    }
    return new TemplateDisjunction(disjuncts);
  }

  @Override
  public TemplateBoolean absorbNegations() {
    Vector<TemplateBoolean> conjuncts = new Vector<>();
    TemplateBoolean tb;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tb = getConjunct(i).absorbNegations();
      conjuncts.add(tb);
    }
    return new TemplateConjunction(conjuncts);
  }

  @Override
  public void flatten() {
    Vector<TemplateBoolean> newconjuncts = new Vector<>();
    TemplateBoolean tb;
    Vector<TemplateBoolean> subconjuncts;
    TemplateConjunction tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tb = getConjunct(i);
      tb.flatten();
      if (this.getClass().isInstance(tb)) {
        tc = (TemplateConjunction) tb;
        subconjuncts = tc.getConjuncts();
        newconjuncts.addAll(subconjuncts);
      } else {
        newconjuncts.add(tb);
      }
    }
    conjuncts = newconjuncts;
  }

  //----------------------------------------------------------------
  // copy

  @Override
  public TemplateConjunction copy() {
    Vector<TemplateBoolean> v = new Vector<>();
    for (TemplateBoolean c : conjuncts) {
      v.add(c.copy());
    }
    TemplateConjunction c = new TemplateConjunction(v);
    return c;
  }

  //----------------------------------------------------------------
  // Alter and Undo

  @Override
  public void alias(AliasingMap amap) {
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      tc.alias(amap);
    }
  }

  @Override
  public void unalias() {
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      tc.unalias();
    }
  }

  @Override
  public boolean evaluate(Map<String, Rational> map) {
    boolean ans = true;
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      ans &= tc.evaluate(map);
    }
    return ans;
  }

  @Override
  public void unevaluate() {
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      tc.unevaluate();
    }
  }

  @Override
  public void postindex(Map<String, Integer> indices) {
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      tc.postindex(indices);
    }
  }

  @Override
  public void preindex(Map<String, Integer> indices) {
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      tc.preindex(indices);
    }
  }

  @Override
  public void unindex() {
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      tc.unindex();
    }
  }

  @Override
  public Purification purify(Purification pur) {
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      pur = tc.purify(pur);
    }
    return pur;
  }

  @Override
  public void unpurify() {
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      tc.unpurify();
    }
  }

  //----------------------------------------------------------------
  // Other cascade methods

  @Override
  public Vector<TemplateConstraint> getConstraints() {
    Vector<TemplateConstraint> v = new Vector<>();
    TemplateBoolean tb;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tb = getConjunct(i);
      v.addAll(tb.getConstraints());
    }
    return v;
  }

  @Override
  public Set<TemplateVariable> getAllVariables() {
    HashSet<TemplateVariable> vars = new HashSet<>();
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      vars.addAll(tc.getAllVariables());
    }
    return vars;
  }

  @Override
  public Set<TemplateVariable> getAllParameters() {
    HashSet<TemplateVariable> params = new HashSet<>();
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      params.addAll(tc.getAllParameters());
    }
    return params;
  }

  @Override
  public HashMap<String, Integer> getMaxIndices(HashMap<String, Integer> map) {
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      map = tc.getMaxIndices(map);
    }
    return map;
  }

  @Override
  public TemplateVariableManager getVariableManager() {
    TemplateVariableManager tvm = new TemplateVariableManager();
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      tvm.merge(tc.getVariableManager());
    }
    return tvm;
  }

  @Override
  public BooleanFormula translate(FormulaManagerView fmgr) {
    BooleanFormula form = null;
    BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
    int N = getNumConjuncts();
    if (N == 0) {
      form = bfmgr.makeBoolean(true);
    } else {
      assert N >= 1;
      form = getConjunct(0).translate(fmgr);
      BooleanFormula augend;
      for (int i = 1; i < N; i++) {
        augend = getConjunct(i).translate(fmgr);
        form = bfmgr.and(form, augend);
      }
    }
    return form;
  }

  @Override
  public List<TemplateFormula> extractAtoms(boolean sAE, boolean cO) {
    List<TemplateFormula> atoms = new Vector<>();
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      if (cO) {
        atoms.add(tc);
      } else {
        atoms.addAll(tc.extractAtoms(sAE, cO));
      }
    }
    return atoms;
  }

  /*
   * Delete all conjuncts which are not constraints.
   */
  public void deleteNonConstraints() {
    Vector<TemplateBoolean> newconj = new Vector<>();
    for (TemplateBoolean tb : conjuncts) {
      if (tb instanceof TemplateConstraint) {
        newconj.add(tb);
      }
    }
    conjuncts = newconj;
  }

  @Override
  public Set<TermForm> getTopLevelTermForms() {
    Set<TermForm> forms = new HashSet<>();
    TemplateBoolean tc;
    for (int i = 0; i < getNumConjuncts(); i++) {
      tc = getConjunct(i);
      forms.addAll(tc.getTopLevelTermForms());
    }
    return forms;
  }

  //-----------------------------------------------------------------
  // Other

  public Vector<TemplateBoolean> getConjuncts() {
    return conjuncts;
  }

  public int getNumConjuncts() {
    return conjuncts.size();
  }

  public TemplateBoolean getConjunct(int i) {
    return conjuncts.get(i);
  }

  @Override
  public Set<TemplateTerm> getRHSTerms() {
    Set<TemplateTerm> s = new HashSet<>();
    for (TemplateBoolean c : conjuncts) {
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
    for (int i = 0; i < getNumConjuncts(); i++) {
      C = getConjunct(i);
      C.prefixVariables(prefix);
    }
  }

  @Override
  public boolean isTrue() {
    boolean ans = true;
    for (TemplateBoolean TB : conjuncts) {
      ans &= TB.isTrue();
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
    TemplateBoolean C;
    for (int i = 0; i < getNumConjuncts(); i++) {
      C = getConjunct(i);
      if (TemplateTrue.isInstance(C)) {
        continue;
      } else if (TemplateFalse.isInstance(C)) {
        s = "false";
        return s; // to avoid the chop off of first 5 chars, below, we return early
      } else {
        s += " and "+C.toString(vwm);
      }
    }
    if (s.length() > 0) {
      s = s.substring(5);
    }
    //s = "( "+s+" )";
    return s;
  }

}
