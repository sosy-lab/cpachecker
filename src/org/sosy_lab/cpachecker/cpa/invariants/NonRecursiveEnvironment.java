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
import java.util.Map;
import java.util.SortedSet;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundIntervalFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ContainsVarVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;

import com.google.common.base.Preconditions;


public class NonRecursiveEnvironment implements Map<String, InvariantsFormula<CompoundInterval>> {

  private static final FormulaEvaluationVisitor<CompoundInterval> FORMULA_EVALUATION_VISITOR = new FormulaCompoundStateEvaluationVisitor();

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  private static final InvariantsFormula<CompoundInterval> TOP = CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.top());

  private final ContainsVarVisitor<CompoundInterval> containsVarVisitor = new ContainsVarVisitor<>();

  private final PersistentSortedMap<String, InvariantsFormula<CompoundInterval>> inner;

  private static final NonRecursiveEnvironment EMPTY_ENVIRONMENT = new NonRecursiveEnvironment(PathCopyingPersistentTreeMap.<String, InvariantsFormula<CompoundInterval>>of());

  public static NonRecursiveEnvironment of() {
    return EMPTY_ENVIRONMENT;
  }

  private NonRecursiveEnvironment(Map<String, InvariantsFormula<CompoundInterval>> pInner) {
    this.inner = PathCopyingPersistentTreeMap.copyOf(pInner);
  }

  private NonRecursiveEnvironment(PersistentSortedMap<String, InvariantsFormula<CompoundInterval>> pInner) {
    this.inner = pInner;
  }

  public static NonRecursiveEnvironment copyOf(Map<String, InvariantsFormula<CompoundInterval>> pInner) {
    if (pInner instanceof NonRecursiveEnvironment) {
      return (NonRecursiveEnvironment) pInner;
    }
    if (pInner instanceof PersistentSortedMap) {
      return new NonRecursiveEnvironment((PersistentSortedMap<String, InvariantsFormula<CompoundInterval>>) pInner);
    }
    return new NonRecursiveEnvironment(pInner);
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
  @Deprecated
  public InvariantsFormula<CompoundInterval> put(String pVarName, InvariantsFormula<CompoundInterval> pValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public InvariantsFormula<CompoundInterval> remove(Object pKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void putAll(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pM) {
    throw new UnsupportedOperationException();
  }

  private PersistentSortedMap<String, InvariantsFormula<CompoundInterval>> sanitizedInnerPutAndCopy(PersistentSortedMap<String, InvariantsFormula<CompoundInterval>> pTarget,
      ContainsVarVisitor<CompoundInterval> pContainsVarVisitor,
      String pVarName, InvariantsFormula<CompoundInterval> pValue) {
    if (pValue == null || pValue.equals(TOP)) {
      if (pTarget.containsKey(pVarName)) {
        return pTarget.removeAndCopy(pVarName);
      }
      return pTarget;
    }
    InvariantsFormula<CompoundInterval> previous = pTarget.get(pVarName);
    if (pValue.equals(previous)) {
      return pTarget;
    }
    pTarget = pTarget.removeAndCopy(pVarName);
    ContainsVarVisitor<CompoundInterval> containsVarVisitor = new ContainsVarVisitor<>(pTarget);
    if (pValue.accept(containsVarVisitor, pVarName)) {
      return sanitizedInnerPutAndCopyInternal(pTarget, pVarName, CompoundIntervalFormulaManager.INSTANCE.asConstant(pValue.accept(FORMULA_EVALUATION_VISITOR, this)));
    }
    InvariantsFormula<CompoundInterval> variable = CompoundIntervalFormulaManager.INSTANCE.asVariable(pVarName);
    for (String containedVarName : pValue.accept(COLLECT_VARS_VISITOR)) {
      if (variable.accept(containsVarVisitor, containedVarName)) {
        return sanitizedInnerPutAndCopyInternal(pTarget, pVarName, CompoundIntervalFormulaManager.INSTANCE.asConstant(pValue.accept(FORMULA_EVALUATION_VISITOR, this)));
      }
    }
    return sanitizedInnerPutAndCopyInternal(pTarget, pVarName, pValue);
  }

  private PersistentSortedMap<String, InvariantsFormula<CompoundInterval>> sanitizedInnerPutAndCopyInternal(PersistentSortedMap<String, InvariantsFormula<CompoundInterval>> pTarget,
      String pVarName, InvariantsFormula<CompoundInterval> pValue) {
    Preconditions.checkArgument(pValue != null && !pValue.equals(TOP), "Value must not be TOP");
    Preconditions.checkArgument(!pTarget.containsKey(pVarName), "Variable must be TOP in previous environment");
    InvariantsFormula<CompoundInterval> previous = pTarget.get(pVarName);
    if (pValue.equals(previous)) {
      return pTarget;
    }
    return pTarget.putAndCopy(pVarName, pValue);
  }

  public NonRecursiveEnvironment putAndCopy(String pVarName, InvariantsFormula<CompoundInterval> pValue) {
    InvariantsFormula<CompoundInterval> previous = get(pVarName);
    if (previous == null) {
      previous = TOP;
    }
    if (pValue == null) {
      pValue = TOP;
    }
    if (previous.equals(pValue)) {
      return this;
    }
    PersistentSortedMap<String, InvariantsFormula<CompoundInterval>> resultInner = sanitizedInnerPutAndCopy(this.inner, this.containsVarVisitor, pVarName, pValue);
    if (this.inner == resultInner) {
      return this;
    }
    return new NonRecursiveEnvironment(resultInner);
  }

  public NonRecursiveEnvironment putAndCopyAll(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pM) {
    PersistentSortedMap<String, InvariantsFormula<CompoundInterval>> resultInner = this.inner;
    for (java.util.Map.Entry<? extends String, ? extends InvariantsFormula<CompoundInterval>> entry : pM.entrySet()) {
      resultInner = sanitizedInnerPutAndCopy(resultInner, new ContainsVarVisitor<>(resultInner), entry.getKey(), entry.getValue());
    }
    return new NonRecursiveEnvironment(resultInner);
  }

  public NonRecursiveEnvironment removeAndCopy(Object pKey) {
    if (!containsKey(pKey)) {
      return this;
    }
    return new NonRecursiveEnvironment(this.inner.removeAndCopy(pKey));
  }

  @Override
  @Deprecated
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SortedSet<String> keySet() {
    return this.inner.keySet();
  }

  @Override
  public Collection<InvariantsFormula<CompoundInterval>> values() {
    return Collections.unmodifiableCollection(this.inner.values());
  }

  @Override
  public SortedSet<java.util.Map.Entry<String, InvariantsFormula<CompoundInterval>>> entrySet() {
    return this.inner.entrySet();
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
