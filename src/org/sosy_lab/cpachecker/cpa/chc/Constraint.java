// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.chc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jpl.Term;

public class Constraint {

  private List<Term> cns = null;
  private Map<String, Term> vars = null;

  public Constraint() {
    cns = new ArrayList<>();
    vars = new HashMap<>();
  }

  public Constraint(Constraint cn) {
    cns = new ArrayList<>(cn.getConstraint());
    vars = new HashMap<>(cn.getVars());
  }

  public Constraint(List<Term> cns, Map<String, Term> vars) {
    this.cns = cns;
    this.vars = vars;
  }

  public Constraint(List<Term> cns) {
    this.cns = cns;
    vars = new HashMap<>();
  }

  public Constraint(List<Term> cns, List<Term> vars) {
    this.cns = cns;
    createHashMap(vars);
  }

  public Constraint(Term cn, List<Term> vars) {
    cns = new ArrayList<>();
    cns.add(cn);
    createHashMap(vars);
  }

  public void addAtomicConstraint(Term t) {
    cns.add(t);
  }

  public void addVar(String var, Term t) {
    vars.put(var, t);
  }

  public void addVars(HashMap<String, Term> pVars) {
    vars.putAll(pVars);
  }

  public void removeVar(String var) {
    vars.remove(var);
  }

  @Override
  public String toString() {
    if (cns == null) {
      return "false";
    } else if (cns.isEmpty()) {
      return "true";
    } else {
      return cns + " (vars: " + vars + ")";
    }
  }

  public List<Term> getConstraint() {
    return cns;
  }

  public Map<String, Term> getVars() {
    return vars;
  }

  public boolean isTrue() {
    return cns.isEmpty();
  }

  public boolean isFalse() {

    if (cns == null) {
      return true;
    } else {
      if (!cns.isEmpty()) {
        if (cns.get(0).toString().equals("false")) {
          return true;
        }
      }
      return false;
    }
  }

  public static boolean isFalse(String cr) {
    if (cr.equals("'.'(false, [])")) {
      return true;
    } else {
      return false;
    }
  }

  public void setTrue() {
    cns = new ArrayList<>();
  }

  public Constraint setFalse() {
    cns = null;
    return this;
  }

  public void and(Constraint cn) {
    cns.addAll(cn.getConstraint());
  }

  public Constraint setConstraint(List<Term> cns) {
    this.cns = cns;
    return this;
  }

  public void emptyVar() {
    vars = new HashMap<>();
  }

  private void createHashMap(List<Term> varTerms) {
    vars = new HashMap<>();
    for (Term v : varTerms) {
      vars.put(ConstraintManager.var2CVar(v.name()), v);
    }
  }
}
