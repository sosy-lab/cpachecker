/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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


public class NonRecursiveEnvironment implements Map<String, InvariantsFormula<CompoundState>> {

  private static final FormulaEvaluationVisitor<CompoundState> FORMULA_EVALUATION_VISITOR = new FormulaCompoundStateEvaluationVisitor();

  private static final CollectVarsVisitor<CompoundState> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  private static final InvariantsFormula<CompoundState> TOP = CompoundStateFormulaManager.INSTANCE.asConstant(CompoundState.top());

  private final ContainsVarVisitor<CompoundState> containsVarVisitor = new ContainsVarVisitor<>(this);

  private final Map<String, InvariantsFormula<CompoundState>> inner;

  public NonRecursiveEnvironment() {
    this(Collections.<String, InvariantsFormula<CompoundState>>emptyMap());
  }

  public NonRecursiveEnvironment(Map<String, InvariantsFormula<CompoundState>> pInner) {
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
  public InvariantsFormula<CompoundState> get(Object pVarName) {
    return this.inner.get(pVarName);
  }

  @Override
  public InvariantsFormula<CompoundState> put(String pVarName, InvariantsFormula<CompoundState> pValue) {
    if (pValue == null || pValue.equals(TOP)) {
      return this.inner.remove(pVarName);
    }
    if (pValue.accept(containsVarVisitor, pVarName)) {
      return put(pVarName, CompoundStateFormulaManager.INSTANCE.asConstant(pValue.accept(FORMULA_EVALUATION_VISITOR, this)));
    }
    InvariantsFormula<CompoundState> variable = CompoundStateFormulaManager.INSTANCE.asVariable(pVarName);
    for (String containedVarName : pValue.accept(COLLECT_VARS_VISITOR)) {
      if (variable.accept(containsVarVisitor, containedVarName)) {
        return put(pVarName, CompoundStateFormulaManager.INSTANCE.asConstant(pValue.accept(FORMULA_EVALUATION_VISITOR, this)));
      }
    }
    return this.inner.put(pVarName, pValue);
  }

  @Override
  public InvariantsFormula<CompoundState> remove(Object pKey) {
    return this.inner.remove(pKey);
  }

  @Override
  public void putAll(Map<? extends String, ? extends InvariantsFormula<CompoundState>> pM) {
    for (Map.Entry<? extends String, ? extends InvariantsFormula<CompoundState>> entry : pM.entrySet()) {
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
  public Collection<InvariantsFormula<CompoundState>> values() {
    return Collections.unmodifiableCollection(this.inner.values());
  }

  @Override
  public Set<java.util.Map.Entry<String, InvariantsFormula<CompoundState>>> entrySet() {
    return Collections.unmodifiableSet(this.inner.entrySet());
  }

  @Override
  public String toString() {
    return this.inner.toString();
  }

  @Override
  public boolean equals(Object o) {
    return this.inner.equals(o);
  }

  @Override
  public int hashCode() {
    return this.inner.hashCode();
  }

}
