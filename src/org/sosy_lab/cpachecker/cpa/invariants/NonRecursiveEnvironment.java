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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundStateFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ContainsVarVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;


public class NonRecursiveEnvironment implements Map<String, InvariantsFormula<CompoundInterval>> {

  private static final FormulaEvaluationVisitor<CompoundInterval> FORMULA_EVALUATION_VISITOR = new FormulaCompoundStateEvaluationVisitor();

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  private static final InvariantsFormula<CompoundInterval> TOP = CompoundStateFormulaManager.INSTANCE.asConstant(CompoundInterval.top());

  private final ContainsVarVisitor<CompoundInterval> containsVarVisitor = new ContainsVarVisitor<>(this);

  private final Map<String, InvariantsFormula<CompoundInterval>> inner;

  public NonRecursiveEnvironment() {
    this(Collections.<String, InvariantsFormula<CompoundInterval>>emptyMap());
  }

  public NonRecursiveEnvironment(Map<String, InvariantsFormula<CompoundInterval>> pInner) {
    this.inner = new HashMap<>(pInner);
  }

  @Override
  public int size() {
    return this.inner.size();
  }

  @Override
  public boolean isEmpty() {
    return this.inner.isEmpty();
  }

  @Override
  public boolean containsKey(Object pVarName) {
    return this.inner.containsKey(pVarName);
  }

  @Override
  public boolean containsValue(Object pValue) {
    return this.inner.containsValue(pValue);
  }

  @Override
  public InvariantsFormula<CompoundInterval> get(Object pVarName) {
    return this.inner.get(pVarName);
  }

  @Override
  public InvariantsFormula<CompoundInterval> put(String pVarName, InvariantsFormula<CompoundInterval> pValue) {
    if (pValue == null || pValue.equals(TOP)) {
      return this.inner.remove(pVarName);
    }
    if (pValue.accept(containsVarVisitor, pVarName)) {
      return put(pVarName, CompoundStateFormulaManager.INSTANCE.asConstant(pValue.accept(FORMULA_EVALUATION_VISITOR, this)));
    }
    InvariantsFormula<CompoundInterval> variable = CompoundStateFormulaManager.INSTANCE.asVariable(pVarName);
    for (String containedVarName : pValue.accept(COLLECT_VARS_VISITOR)) {
      if (variable.accept(containsVarVisitor, containedVarName)) {
        return put(pVarName, CompoundStateFormulaManager.INSTANCE.asConstant(pValue.accept(FORMULA_EVALUATION_VISITOR, this)));
      }
    }
    return this.inner.put(pVarName, pValue);
  }

  @Override
  public InvariantsFormula<CompoundInterval> remove(Object pKey) {
    return this.inner.remove(pKey);
  }

  @Override
  public void putAll(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pM) {
    for (Map.Entry<? extends String, ? extends InvariantsFormula<CompoundInterval>> entry : pM.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    this.inner.clear();
  }

  @Override
  public Set<String> keySet() {
    return Collections.unmodifiableSet(this.inner.keySet());
  }

  @Override
  public Collection<InvariantsFormula<CompoundInterval>> values() {
    return Collections.unmodifiableCollection(this.inner.values());
  }

  @Override
  public Set<java.util.Map.Entry<String, InvariantsFormula<CompoundInterval>>> entrySet() {
    return Collections.unmodifiableSet(this.inner.entrySet());
  }

  @Override
  public String toString() {
    return this.inner.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return this.inner.equals(o);
  }

  @Override
  public int hashCode() {
    return this.inner.hashCode();
  }

}
