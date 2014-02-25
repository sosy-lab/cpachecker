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
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.sosy_lab.cpachecker.util.invariants.Coeff;
import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.invariants.balancer.Monomial;
import org.sosy_lab.cpachecker.util.invariants.balancer.Term;
import org.sosy_lab.cpachecker.util.invariants.balancer.Variable;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

public class TemplateTerm extends TemplateSum {

  private TemplateVariable var = null;
  private TemplateUIF uif = null;
  private TemplateVariable param = null;
  private TemplateNumber coeff = null;

  private static AtomicInteger nextParamIndex = new AtomicInteger(0);
  public static final String paramHead = "p";

  // To write "as form" means that in our toString method we suppress
  // coefficient and parameter. Thus, the "form" of a term simply reflects
  // its variables and UIFs.
  private boolean writingAsForm = false;

  // In case we wish to unevaluate, we store old values.
  private TemplateVariable oldParam = null;
  private TemplateNumber oldCoeff = null;

  public TemplateTerm(FormulaType<?> type) {
    super(type);
  }

  public TemplateTerm(TemplateUIF uif) {
    super(uif.getFormulaType());
    this.uif = uif;
  }

  public static TemplateTerm makeZero(FormulaType<?> type) {
    TemplateTerm T = new TemplateTerm(type);
    T.setCoefficient(new TemplateNumber(type, 0));
    return T;
  }

  public static TemplateTerm makeUnity(FormulaType<?> type) {
    TemplateTerm T = new TemplateTerm(type);
    T.setCoefficient(new TemplateNumber(type, 1));
    return T;
  }

// ------------------------------------------------------------------
// Alter and Undo

  @Override
  public void alias(AliasingMap amap) {
    if (hasVariable()) {
      TemplateVariable v = getVariable();
      v.alias(amap);
    } else if (hasUIF()) {
      TemplateUIF u = getUIF();
      u.alias(amap);
    }
  }

  @Override
  public void unalias() {
    if (hasVariable()) {
      TemplateVariable V = getVariable();
      V.unalias();
    } else if (hasUIF()) {
      TemplateUIF U = getUIF();
      U.unalias();
    }
  }

  @Override
  public boolean evaluate(Map<String, Rational> map) {
    boolean ans = true;
    if (hasParameter()) {
      ans = false;
      String a = getParameter().toString(VariableWriteMode.REDLOG);
      if (map.containsKey(a)) {
        Rational R = map.get(a);
        // Turn off parameter.
        oldParam = param;
        param = null;
        // Update coefficient.
        oldCoeff = coeff;
        TemplateNumber P = new TemplateNumber(getFormulaType(), R);
        if (!hasCoefficient()) {
          setCoefficient(P);
        } else {
          TemplateNumber Q = getCoefficient();
          TemplateNumber M = TemplateNumber.multiply(Q, P);
          setCoefficient(M);
        }
        ans = true;
      }
    }
    if (hasUIF()) {
      TemplateUIF U = getUIF();
      ans &= U.evaluate(map);
    }
    return ans;
  }

  @Override
  public void unevaluate() {
    param = oldParam;
    coeff = oldCoeff;
    if (hasUIF()) {
      TemplateUIF U = getUIF();
      U.unevaluate();
    }
  }

  @Override
  public void postindex(Map<String, Integer> indices) {
    if (hasVariable()) {
      var.postindex(indices);
    } else if (hasUIF()) {
      TemplateUIF U = getUIF();
      U.postindex(indices);
    }
  }

  @Override
  public void preindex(Map<String, Integer> indices) {
    if (hasVariable()) {
      var.preindex(indices);
    } else if (hasUIF()) {
      TemplateUIF U = getUIF();
      U.preindex(indices);
    }
  }

  @Override
  public void unindex() {
    if (hasVariable()) {
      var.unindex();
    } else if (hasUIF()) {
      TemplateUIF U = getUIF();
      U.unindex();
    }
  }

  @Override
  public Purification purify(Purification pur) {
    if (hasUIF()) {
      pur = uif.purify(pur);
    }
    return pur;
  }

  @Override
  public void unpurify() {
    if (hasUIF()) {
      uif.unpurify();
    }
  }

  // The purpose of "generalizing" is to turn the Term into a template.
  // This means that we eliminate any coefficient, and we generate a
  // fresh parameter.
  @Override
  public void generalize() {
    coeff = null;
    param = getNextFreshParameter(getFormulaType());
    if (hasVariable()) {
      var.generalize();
    }
    if (hasUIF()) {
      uif.generalize();
    }
  }

  public static TemplateVariable getNextFreshParameter(FormulaType<?> type) {
    return new TemplateVariable(type, paramHead, nextParamIndex.incrementAndGet());
  }

  public static void resetParameterIndices() {
    nextParamIndex.set(0);
  }

//------------------------------------------------------------------
// Other cascade methods

  @Override
  public Set<TemplateVariable> getAllVariables() {
    HashSet<TemplateVariable> vars = new HashSet<>();
    if (hasVariable()) {
      vars.add(var);
    } else if (hasUIF()) {
      vars.addAll(uif.getAllVariables());
    }
    return vars;
  }

  @Override
  public Set<TemplateVariable> getAllParameters() {
    HashSet<TemplateVariable> params = new HashSet<>();
    if (hasParameter()) {
      params.add(param);
    }
    if (hasUIF()) {
      params.addAll(uif.getAllParameters());
    }
    return params;
  }

  @Override
  public Set<TemplateVariable> getAllPurificationVariables() {
    Set<TemplateVariable> pvs = new HashSet<>();
    if (uif!=null && uif.isPurified()) {
      pvs.add(uif.getPurifiedName().getVariable());
    }
    return pvs;
  }

  @Override
  public HashMap<String, Integer> getMaxIndices(HashMap<String, Integer> map) {
    if (hasVariable()) {
      map = var.getMaxIndices(map);
    }
    if (hasUIF()) {
      TemplateUIF U = getUIF();
      map = U.getMaxIndices(map);
    }
    return map;
  }

  public int getMaxIndex() {
    HashMap<String, Integer> map = getMaxIndices(new HashMap<String, Integer>());
    int n = 0;
    for (Integer I : map.values()) {
      if (I.intValue() > n) {
        n = I.intValue();
      }
    }
    return n;
  }

  @Override
  public TemplateVariableManager getVariableManager() {
    TemplateVariableManager tvm;
    HashSet<TemplateVariable> var = new HashSet<>();
    if (hasAnyVariable()) {
      var.add(getAnyVariable());
    //} else if (hasUIF() && uif.isPurified()) {
    //  var.add( uif.getPurifiedName().getVariable() );
    }
    tvm = new TemplateVariableManager(var);
    return tvm;
  }

  @Override
  public void prefixVariables(String prefix) {
    // Add a prefix to the variable, if there is one.
    if (hasVariable()) {
      var.addPrefix(prefix);
    } else if (hasUIF()) {
      uif.prefixVariables(prefix);
    }
  }

  public Term makeRationalFunctionTerm(Map<String, Variable> paramVars) {
    Term t = new Term();
    Rational c = Rational.makeUnity();
    if (hasCoefficient()) {
      c = coeff.rationalValue();
    }
    t.setCoeff(c);
    if (hasParameter()) {
      Variable v;
      String s = param.toString(VariableWriteMode.REDLOG);
      if (!paramVars.containsKey(s)) {
        System.err.println("Creating parameter not in the TemplateNetwork's parameter map.");
        v = new Variable(s);
      } else {
        v = paramVars.get(s);
      }
      t.setMonomial(new Monomial(v));
    }
    return t;
  }

  @Override
  public Formula translate(FormulaManagerView fmgr) {
    Formula form = null;
    Vector<Formula> factors = new Vector<>(4);

    if (hasCoefficient()) {
      factors.add(getCoefficient().translate(fmgr));
    }
    // We ignore parameters, since other "languages" do not have them.
    if (hasVariable()) {
      factors.add(getVariable().translate(fmgr));
    }
    if (hasUIF()) {
      factors.add(getUIF().translate(fmgr));
    }

    if (factors.size() == 0) {
      // This case probably should not occur.
      form = makeUnity(getFormulaType()).translate(fmgr);
    } else {
      form = factors.get(0);
      Formula f;
      for (int i = 1; i < factors.size(); i++) {
        f = factors.get(i);
        form = fmgr.makeMultiply(form, f);
      }
    }
    return form;
  }

  @Override
  public TemplateTerm copy() {
    TemplateTerm t = new TemplateTerm(getFormulaType());
    if (hasCoefficient()) {
      t.coeff = coeff.copy();
    }
    if (hasParameter()) {
      t.param = param.copy();
    }
    if (hasVariable()) {
      t.var = var.copy();
    }
    if (hasUIF()) {
      t.uif = uif.copy();
    }
    return t;
  }


//------------------------------------------------------------------
// "has" methods

  public boolean hasVariable() {
    return (var!=null);
  }

  public boolean hasAnyVariable() {
    return (var!=null || (uif!=null && uif.isPurified()));
  }

  public boolean hasUIF() {
    return (uif!=null);
  }

  public boolean hasParameter() {
    return (param!=null);
  }

  public boolean hasCoefficient() {
    return (coeff!=null);
  }

//------------------------------------------------------------------
// "is" methods

  public boolean isConstant() {
    // Return true iff this term has no variable and no UIF, but
    // does have a parameter and/or a constant.
    return (!hasVariable() && !hasUIF() && (hasCoefficient() || hasParameter()));
  }

  @Override
  public boolean isANumber() {
    // Return true iff this term has no variable, UIF, or parameter, and
    // does have a coefficient.
    return (hasCoefficient() && !hasVariable() && !hasUIF() && !hasParameter());
  }

  public boolean isZero() {
    return (hasCoefficient() && coeff.isZero());
  }

//------------------------------------------------------------------
// "set" methods

  public void setUIF(TemplateUIF u) {
    uif = u;
  }

  public void setVariable(TemplateVariable v) {
    var = v;
  }

  public void setParameter(TemplateVariable p) {
    param = p;
  }

  @Deprecated
  public void setUnknown(TemplateVariable u) {
    // Use this if you don't know whether you have a variable or a
    // parameter, and want to decide based on the first letter.
    String s = u.toString();
    if (s.startsWith("v")) {
      var = u;
    } else if (s.startsWith("p")) {
      param = u;
    }
  }

  public void setCoefficient(TemplateNumber c) {
    coeff = c;
  }

//------------------------------------------------------------------
// "get" methods

  public TemplateVariable getVariable() {
    return var;
  }

  public TemplateVariable getAnyVariable() {
    TemplateVariable V = null;
    if (var!=null) {
      V = var;
    } else if (uif!=null && uif.isPurified()) {
      V = uif.getPurifiedName().getVariable();
    }
    return V;
  }

  public Integer getVariableIndex() {
    return var.getIndex();
  }

  public TemplateUIF getUIF() {
    return uif;
  }

  public TemplateVariable getParameter() {
    return param;
  }

  public TemplateNumber getCoefficient() {
    return coeff;
  }

  public String getMonomialString(VariableWriteMode vwm) {
    String s = "";
    if (hasParameter()) {
      s += param.toString(vwm);
    }
    if (hasVariable()) {
      s += "*"+var.toString(vwm);
    }
    if (hasUIF()) {
      s += "*"+uif.toString(vwm);
    }
    if (s.length() > 0 && !hasParameter()) {
      // In this case there is a * hanging at the beginning.
      s = s.substring(1);
    } else if (s.length() == 0) {
      s = "1";
    }
    return s;
  }

  public Coeff getCoeffWithParam(VariableWriteMode vwm) {
    // Return a Coeff containing both the constant coefficient and
    // the parameter of this term, if they are present.
    String s = "";
    if (hasCoefficient()) {
      s += coeff.toString();
    }
    if (hasParameter()) {
      s += "*"+param.toString(vwm);
    }
    if (s.length() > 0 && !hasCoefficient()) {
      // In this case there is a * hanging at the beginning.
      s = s.substring(1);
    } else if (s.length() == 0) {
      // In this case the coefficient is 1.
      s = "1";
    }
    return new Coeff(getFormulaType(), s);
  }

  @Override
  public Integer getInteger() {
    // Return the value of the coefficient, or null if there isn't
    // any.
    Integer I = null;
    if (hasCoefficient()) {
      I = Integer.valueOf(coeff.toString());
    }
    return I;
  }

  @Override
  public Vector<TemplateTerm> getTerms() {
    Vector<TemplateTerm> V = new Vector<>();
    V.add(this);
    return V;
  }

  @Override
  public int getNumTerms() {
    return 1;
  }

  @Override
  public TemplateTerm getTerm(int i) {
    return this;
  }

//------------------------------------------------------------------
// Other

  /*
   * Create a new TemplateTerm which is obtained from this one by
   * dividing its coefficient by the TemplateNumber n.
   */
  public TemplateTerm divideBy(TemplateNumber n) {
    TemplateTerm t = this.copy();
    if (t.hasCoefficient()) {
      t.coeff = t.coeff.divideBy(n);
    } else {
      t.coeff = n.makeReciprocal();
    }
    return t;
  }

  public static TemplateTerm multiply(TemplateTerm t1, TemplateTerm t2) {
    TemplateTerm T = new TemplateTerm(t1.getFormulaType());

    boolean hv1 = t1.hasVariable();
    boolean hu1 = t1.hasUIF();
    boolean hp1 = t1.hasParameter();
    boolean hc1 = t1.hasCoefficient();
    TemplateVariable v1 = t1.getVariable();
    TemplateUIF u1 = t1.getUIF();
    TemplateVariable p1 = t1.getParameter();
    TemplateNumber c1 = t1.getCoefficient();

    boolean hv2 = t2.hasVariable();
    boolean hu2 = t2.hasUIF();
    boolean hp2 = t2.hasParameter();
    boolean hc2 = t2.hasCoefficient();
    TemplateVariable v2 = t2.getVariable();
    TemplateUIF u2 = t2.getUIF();
    TemplateVariable p2 = t2.getParameter();
    TemplateNumber c2 = t2.getCoefficient();

    if ((hv1 && hu2) || (hu1 && hv2)) {
      System.err.println("Multiplying term with var by term with UIF.");
    }

    if (hv1 && hv2) {
      T.setVariable(v1);
      System.err.println("Multiplying two variables.");
    }

    if (hv1 && !hv2) {
      T.setVariable(v1);
    }

    if (!hv1 && hv2) {
      T.setVariable(v2);
    }


    if (hu1 && hu2) {
      T.setUIF(u1);
      System.err.println("Multiplying two UIFs.");
    }

    if (hu1 && !hu2) {
      T.setUIF(u1);
    }

    if (!hu1 && hu2) {
      T.setUIF(u2);
    }


    if (hp1 && hp2) {
      T.setParameter(p1);
      System.err.println("Multiplying two parameters.");
    }

    if (hp1 && !hp2) {
      T.setParameter(p1);
    }

    if (!hp1 && hp2) {
      T.setParameter(p2);
    }


    if (hc1 && !hc2) {
      T.setCoefficient(c1);
    }

    if (!hc1 && hc2) {
      T.setCoefficient(c2);
    }

    if (hc1 && hc2) {
      TemplateNumber N = TemplateNumber.multiply(c1, c2);
      T.setCoefficient(N);
    }

    return T;
  }

  @Override
  public void negate() {
    if (coeff!=null) {
      coeff.negate();
    } else {
      coeff = new TemplateNumber(getFormulaType(), -1);
    }
  }

  @Override
  void writeAsForm(boolean b) {
    writingAsForm = b;
    if (hasVariable()) {
      var.writeAsForm(b);
    }
    if (hasUIF()) {
      uif.writeAsForm(b);
    }
  }

  @Override
  public String toString() {
    return toString(VariableWriteMode.PLAIN);
  }

  @Override
  public String toString(VariableWriteMode vwm) {
    String s = "";
    if (hasCoefficient() && !writingAsForm) {
      s += coeff.toString();
    }
    if (hasParameter() && !writingAsForm) {
      s += "*"+param.toString(vwm);
    }
    if (hasVariable()) {
      s += "*"+var.toString(vwm);
    }
    if (hasUIF()) {
      s += "*"+uif.toString(vwm);
    }
    if (s.length() > 0 && (!hasCoefficient() || writingAsForm )) {
      // In this case there is a * hanging at the beginning.
      s = s.substring(1);
    } else if (s.startsWith("1*")) {
      s = s.substring(2);
    } else if (s.startsWith("-1*")) {
      s = "-"+s.substring(3);
    }
    return s;
  }

}
