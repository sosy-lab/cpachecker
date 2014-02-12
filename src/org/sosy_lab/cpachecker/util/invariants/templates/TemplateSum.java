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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.Coeff;
import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.invariants.balancer.Polynomial;
import org.sosy_lab.cpachecker.util.invariants.balancer.Term;
import org.sosy_lab.cpachecker.util.invariants.balancer.Variable;
import org.sosy_lab.cpachecker.util.invariants.interfaces.GeneralVariable;
import org.sosy_lab.cpachecker.util.invariants.interfaces.VariableManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

public class TemplateSum extends TemplateNumericValue {

  private Vector<TemplateTerm> terms;

//------------------------------------------------------------------
// Constructors

  public TemplateSum(FormulaType<?> type) {
    super(type);
    terms = new Vector<>();
  }

  public TemplateSum(TemplateTerm T) {
    super(T.getFormulaType());
    terms = new Vector<>();
    terms.add(T);
  }

  public TemplateSum(FormulaType<?> type, Vector<TemplateTerm> t) {
    super(type);
    terms = t;
  }

  public TemplateSum(FormulaType<?> type, Collection<TemplateTerm> s) {
    super(type);
    terms = new Vector<>(s);
  }

  public TemplateSum(TemplateSum s1, TemplateSum s2) {
    super(s1.getFormulaType());
    terms = new Vector<>();
    terms.addAll(s1.getTerms());
    terms.addAll(s2.getTerms());
  }

//------------------------------------------------------------------
// Other creation methods.

  /*
   * Create a linear combination over the passed terms, with
   * fresh parameters as coefficients.
   */
  public static TemplateSum freshParamLinComb(FormulaType<?> type,  Collection<TemplateTerm> c) {
    TemplateVariable param;
    Vector<TemplateTerm> v = new Vector<>(c.size());
    for (TemplateTerm t : c) {
      param = TemplateTerm.getNextFreshParameter(type);
      t = t.copy();
      t.setParameter(param);
      v.add(t);
    }
    return new TemplateSum(type, v);
  }

  /*
   * Return this sum minus one.
   */
  public TemplateSum minusOne() {
    return TemplateSum.subtract(this, TemplateSum.makeUnity(getFormulaType()));
  }

  public static TemplateSum makeUnity(FormulaType<?> type) {
    return new TemplateSum(TemplateTerm.makeUnity(type));
  }

//------------------------------------------------------------------
// copy

  @Override
  public TemplateSum copy() {
    return withFormulaType(getFormulaType());
  }

  @Override
  public TemplateSum withFormulaType(FormulaType<?> pNewType) {

    Vector<TemplateTerm> v = new Vector<>();
    for (TemplateTerm t : terms) {
      v.add(t.copy());
    }
    TemplateSum s = new TemplateSum(pNewType, v);
    return s;
  }

  public Polynomial makePolynomial(Map<String, Variable> paramVars) {
    List<Term> tlist = new Vector<>(terms.size());
    for (TemplateTerm t : terms) {
      Term u = t.makeRationalFunctionTerm(paramVars);
      tlist.add(u);
    }
    return new Polynomial(tlist);
  }

//------------------------------------------------------------------
// Alter and Undo

  @Override
  public void alias(AliasingMap amap) {
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      T.alias(amap);
    }
  }

  @Override
  public void unalias() {
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      T.unalias();
    }
  }

  @Override
  public boolean evaluate(Map<String, Rational> map) {
    boolean ans = true;
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      ans &= T.evaluate(map);
    }
    dropZeroTerms();
    return ans;
  }

  @Override
  public void unevaluate() {
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      T.unevaluate();
    }
  }

  @Override
  public void postindex(Map<String, Integer> indices) {
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      T.postindex(indices);
    }
  }

  @Override
  public void preindex(Map<String, Integer> indices) {
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      T.preindex(indices);
    }
  }

  @Override
  public void unindex() {
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      T.unindex();
    }
  }

  @Override
  public Purification purify(Purification pur) {
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      pur = T.purify(pur);
    }
    // Sort and collect terms.
    normalize(new SimpleMonomialOrder());
    return pur;
  }

  @Override
  public void unpurify() {
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      T.unpurify();
    }
  }

  public void generalize() {
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      T.generalize();
    }
  }

//------------------------------------------------------------------
// Other cascade methods

  @Override
  public Set<TemplateVariable> getAllVariables() {
    HashSet<TemplateVariable> vars = new HashSet<>();
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      vars.addAll(T.getAllVariables());
    }
    return vars;
  }

  @Override
  public Set<TemplateVariable> getAllParameters() {
    HashSet<TemplateVariable> params = new HashSet<>();
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      params.addAll(T.getAllParameters());
    }
    return params;
  }

  public Set<TemplateVariable> getTopLevelParameters() {
    HashSet<TemplateVariable> tlp = new HashSet<>();
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      if (T.hasParameter()) {
        tlp.add(T.getParameter());
      }
    }
    return tlp;
  }

  @Override
  public Set<TemplateUIF> getAllTopLevelUIFs() {
    HashSet<TemplateUIF> tlu = new HashSet<>();
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      if (T.hasUIF()) {
        tlu.add(T.getUIF());
      }
    }
    return tlu;
  }

  @Override
  public Set<TemplateVariable> getAllPurificationVariables() {
    Set<TemplateVariable> pvs = new HashSet<>();
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      pvs.addAll(T.getAllPurificationVariables());
    }
    return pvs;
  }

  @Override
  public HashMap<String, Integer> getMaxIndices(HashMap<String, Integer> map) {
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      map = T.getMaxIndices(map);
    }
    return map;
  }

  @Override
  public TemplateVariableManager getVariableManager() {
    TemplateVariableManager tvm = new TemplateVariableManager();
    TemplateVariableManager Ttvm;
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      Ttvm = T.getVariableManager();
      tvm.merge(Ttvm);
    }
    return tvm;
  }

  public void prefixVariables(String prefix) {
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      T.prefixVariables(prefix);
    }
  }

  @Override
  public Formula translate(FormulaManagerView fmgr) {
    Formula form = null;
    int N = getNumTerms();
    if (N == 0) {
      // Really, this case should not occur.
      TemplateTerm Z = TemplateTerm.makeZero(getFormulaType());
      form = Z.translate(fmgr);
    } else {
      assert N >= 1;
      form = getTerm(0).translate(fmgr);
      Formula augend;
      for (int i = 1; i < N; i++) {
        augend = getTerm(i).translate(fmgr);
        form = fmgr.makePlus(form, augend);
      }
    }
    return form;
  }

//------------------------------------------------------------------
// Other

  private void merge(TemplateSum s) {
    terms.addAll(s.getTerms());
  }

  public void sort(MonomialOrder mo) {
    Collections.sort(terms, mo);
  }

  public void dropZeroTerms() {
    Vector<TemplateTerm> v = new Vector<>();
    for (TemplateTerm t : terms) {
      if (!t.isZero()) {
        v.add(t);
      }
    }
    if (v.size() == 0) {
      v.add(TemplateTerm.makeZero(getFormulaType()));
    }
    terms = v;
  }

  /**
   * Sorts terms by the passed monomial order mo, and collects
   * all coefficients, so that no two terms have the same monomial,
   * all terms are sorted according to mo.
   * If the Sum has been purified, and you call this method, then
   * the toString method will produce a unique identifier for this
   * Sum. In other words, two purified Sums are extensionally
   * equivalent as polynomials in their parameters and variables if
   * and only if their toString methods produce the same string after
   * calling normalize.
   */
  public void normalize(MonomialOrder mo) {
    sort(mo);
    Vector<TemplateTerm> newTerms = new Vector<>();
    TemplateTerm T, U = null;
    TemplateNumber C;
    TemplateNumber S = new TemplateNumber(getFormulaType(), 0);
    String lastMonomial = "";
    String monomial;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      monomial = T.getMonomialString(VariableWriteMode.PLAIN);
      C = T.getCoefficient();
      if (monomial.equals(lastMonomial)) {
        S = TemplateNumber.add(S, C);
      } else {
        if (lastMonomial.length() > 0) {
          // record term with collected coefficients
          U.setCoefficient(S);
          newTerms.add(U);
        }
        // start new sum
        S = C;
        lastMonomial = monomial;
      }
      U = T;
    }
    if (lastMonomial.length() > 0) {
      // In this case there was at least one term, and so the final
      // collection has not yet been recorded (since in the for loop
      // they are recorded only when the monomial changes).
      U.setCoefficient(S);
      newTerms.add(U);
    }
    this.terms = newTerms;
  }

  private HashMap<String, TemplateSum> collectWRTVars() {
    // "Collect With Respect to Variables"
    // Return a HashMap that has variable names as keys and
    // TemplateSums as values.
    // Here, the variable names are the string representations of
    // the variables in the PLAIN VaribleWriteMode.
    // It will map every variable occurring in any term in this
    // sum to a new TemplateSum containing all and only those
    // terms in this TemplateSum that have that variable.

    HashMap<String, TemplateSum> map = new HashMap<>();
    TemplateTerm T;
    TemplateVariable V;
    String var;
    TemplateSum R, S;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      if (T.hasAnyVariable()) { // added "Any"
        V = T.getAnyVariable(); // added "Any"
        var = V.toString(VariableWriteMode.PLAIN);
        if (!map.containsKey(var)) {
          // We are encountering this variable for the first
          // time.
          S = new TemplateSum(T);
          map.put(var, S);
        } else {
          // We have already seen this variable at least
          // once.
          S = new TemplateSum(T);
          R = map.get(var);
          R.merge(S);
        }
      }
    }
    return map;
  }

  private Coeff getSumOfAllCoeffsWithParams(VariableWriteMode vwm) {
    // Useful when this TemplateSum happens to be such that every
    // term has one and the same variable in it. Then the returned
    // Coeff is the total coefficient of that variable, including
    // both constants and parameters.
    Collection<TemplateTerm> c = new Vector<>();
    TemplateTerm u;
    for (TemplateTerm t : getTerms()) {
      u = t.copy();
      u.setVariable(null);
      u.setUIF(null);
      if (!u.hasCoefficient()) {
        u.setCoefficient(TemplateNumber.makeUnity(getFormulaType()));
      }
      c.add(u);
    }
    TemplateSum s = new TemplateSum(getFormulaType(), c);
    Coeff co = new Coeff(s, vwm);
    return co;
  }

  /**
   * @param vmgr is the VariableManager containing the list of all
   * variables appearing in any or all of the constraints that are
   * to be put together into one instance of Farkas's lemma.
   * @param vwm is the VariableWriteMode in which any parameters
   * appearing in our return value should be written.
   * @return A vector of Coeff objects <C1, C2, ..., Cn> such that
   * the vector of variables in vmgr is <V1, V2, ..., Vn> and for
   * each i, Ci is the parenthesized sum of the coefficients of all
   * terms in this sum whose variable is Vi.
   */
  public Vector<Coeff> getCoeffsWithParams(VariableWriteMode vwm,
                       VariableManager vmgr) {
    HashMap<String, TemplateSum> vmap = collectWRTVars();
    Vector<Coeff> coeffs = new Vector<>();
    Iterator<GeneralVariable> I = vmgr.iterator();
    GeneralVariable V;
    String var;
    TemplateSum S;
    Coeff C;
    while (I.hasNext()) {
      V = I.next();
      var = V.toString(VariableWriteMode.PLAIN);
      if (!vmap.containsKey(var)) {
        // This sum does not have the variable, so put a zero
        // into the return vector.
        coeffs.add(new Coeff(getFormulaType(), "0") );
      } else {
        // This sum does have the variable.
        S = vmap.get(var);
        C = S.getSumOfAllCoeffsWithParams(vwm);
        coeffs.add(C);
      }
    }
    return coeffs;
  }

  public TemplateSum getConstantPart() {
    Vector<TemplateTerm> V = new Vector<>();
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      if (T.isConstant()) {
        V.add(T);
      }
    }
    return new TemplateSum(getFormulaType(), V);
  }

  public TemplateSum getNonConstantPart() {
    Vector<TemplateTerm> V = new Vector<>();
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      if (!T.isConstant()) {
        V.add(T);
      }
    }
    return new TemplateSum(getFormulaType(), V);
  }

  public Vector<TemplateTerm> getTerms() {
    return terms;
  }

  public int getNumTerms() {
    return terms.size();
  }

  public TemplateTerm getTerm(int i) {
    return terms.get(i);
  }

  @Override
  public void negate() {
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      T.negate();
    }
  }

  public boolean isANumber() {
    // Return true iff this sum is a single term which has no
    // variable or parameter, and does have a coefficient.
    boolean ans = false;
    if (getNumTerms()==1) {
      TemplateTerm T = getTerm(0);
      ans = T.isANumber();
    }
    return ans;
  }

  public Integer getInteger() {
    // If this sum passes the isANumber test, then it is a single
    // term, and we return the integer value of that term's
    // coefficient. Else we return null.
    Integer I = null;
    if (isANumber()) {
      TemplateTerm T = getTerm(0);
      I = T.getInteger();
    }
    return I;
  }

  public static TemplateSum subtract(TemplateSum s1, TemplateSum s2) {
    // Returns s1 minus s2.
    s2.negate();
    return new TemplateSum(s1, s2);
  }

  public static TemplateSum multiply(TemplateSum s1, TemplateSum s2) {
    Vector<TemplateTerm> terms1 = s1.getTerms();
    Vector<TemplateTerm> terms2 = s2.getTerms();
    Vector<TemplateTerm> terms3 = new Vector<>();
    TemplateTerm T1, T2, T3;
    for (int i = 0; i < terms1.size(); i++) {
      for (int j = 0; j < terms2.size(); j++) {
        T1 = terms1.get(i);
        T2 = terms2.get(j);
        T3 = TemplateTerm.multiply(T1, T2);
        terms3.add(T3);
      }
    }
    return new TemplateSum(s1.getFormulaType(), terms3);
  }

  public static TemplateSum divide(TemplateSum s1, TemplateSum s2) {
    // First make sure that s2 is actually just a single number, quitting if it is not.
    TemplateNumber n = null;
    int t = s2.getNumTerms();
    if (t == 1) {
      TemplateTerm term = s2.getTerm(0);
      if (term.isANumber()) {
        n = term.getCoefficient();
      }
    }
    if (n == null) {
      System.err.println("Tried to divide by a TemplateSum that was not a single number.");
      System.exit(1);
    }
    // Now n is the number that we are dividing by.
    TemplateTerm q;
    Collection<TemplateTerm> c = new Vector<>(s1.getNumTerms());
    for (TemplateTerm term : s1.getTerms()) {
      q = term.divideBy(n);
      c.add(q);
    }
    return new TemplateSum(s1.getFormulaType(), c);
  }

  void writeAsForm(boolean b) {
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      T.writeAsForm(b);
    }
  }

  @Override
  public String toString() {
    return toString(VariableWriteMode.PLAIN);
  }

  @Override
  public String toString(VariableWriteMode vwm) {
    String s = "";
    TemplateTerm T;
    for (int i = 0; i < getNumTerms(); i++) {
      T = getTerm(i);
      s += " + "+T.toString(vwm);
    }
    if (s.length() > 0) {
      // We had one or more terms, so we have a superfluous " + " at
      // the beginning.
      s = s.substring(3);
    } else {
      // In this case there were no terms, so we write "0".
      s = "0";
    }
    return s;
  }

}
