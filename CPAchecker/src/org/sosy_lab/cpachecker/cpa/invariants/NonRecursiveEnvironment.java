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
import java.util.Set;
import java.util.SortedSet;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Constant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ContainsVarVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;

import com.google.common.base.Preconditions;


public class NonRecursiveEnvironment implements Map<String, NumeralFormula<CompoundInterval>> {

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  private final ContainsVarVisitor<CompoundInterval> containsVarVisitor = new ContainsVarVisitor<>();

  private final PersistentSortedMap<String, NumeralFormula<CompoundInterval>> inner;

  private final FormulaEvaluationVisitor<CompoundInterval> formulaEvaluationVisitor;

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  private NonRecursiveEnvironment(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      Map<String, NumeralFormula<CompoundInterval>> pInner) {
    this(pCompoundIntervalManagerFactory, PathCopyingPersistentTreeMap.copyOf(pInner));
  }

  private NonRecursiveEnvironment(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      PersistentSortedMap<String, NumeralFormula<CompoundInterval>> pInner) {
    this.inner = pInner;
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.formulaEvaluationVisitor = new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory);
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
  public NumeralFormula<CompoundInterval> get(Object pVarName) {
    return this.inner.get(pVarName);
  }

  @Override
  @Deprecated
  public NumeralFormula<CompoundInterval> put(String pVarName, NumeralFormula<CompoundInterval> pValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public NumeralFormula<CompoundInterval> remove(Object pKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void putAll(Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pM) {
    throw new UnsupportedOperationException();
  }

  private boolean isConstantAndContainsAllPossibleValues(NumeralFormula<CompoundInterval> pFormula) {
    if (pFormula instanceof Constant) {
      return ((Constant<CompoundInterval>) pFormula).getValue().containsAllPossibleValues();
    }
    return false;
  }

  private PersistentSortedMap<String, NumeralFormula<CompoundInterval>> sanitizedInnerPutAndCopy(PersistentSortedMap<String, NumeralFormula<CompoundInterval>> pTarget,
      ContainsVarVisitor<CompoundInterval> pContainsVarVisitor,
      String pVarName, NumeralFormula<CompoundInterval> pValue) {
    if (pValue == null || isConstantAndContainsAllPossibleValues(pValue)) {
      if (pTarget.containsKey(pVarName)) {
        return pTarget.removeAndCopy(pVarName);
      }
      return pTarget;
    }
    NumeralFormula<CompoundInterval> previous = pTarget.get(pVarName);
    if (pValue.equals(previous)) {
      return pTarget;
    }
    pTarget = pTarget.removeAndCopy(pVarName);
    ContainsVarVisitor<CompoundInterval> containsVarVisitor = new ContainsVarVisitor<>(pTarget);
    BitVectorInfo bitVectorInfo = pValue.getBitVectorInfo();
    if (pValue.accept(containsVarVisitor, pVarName)) {
      return sanitizedInnerPutAndCopyInternal(
          pTarget,
          pVarName,
          InvariantsFormulaManager.INSTANCE.asConstant(
              bitVectorInfo,
              pValue.accept(formulaEvaluationVisitor, this)));
    }
    NumeralFormula<CompoundInterval> variable =
        InvariantsFormulaManager.INSTANCE.asVariable(bitVectorInfo, pVarName);
    for (String containedVarName : pValue.accept(COLLECT_VARS_VISITOR)) {
      if (variable.accept(containsVarVisitor, containedVarName)) {
        return sanitizedInnerPutAndCopyInternal(
            pTarget,
            pVarName,
            InvariantsFormulaManager.INSTANCE.asConstant(
                bitVectorInfo,
                pValue.accept(formulaEvaluationVisitor, this)));
      }
    }
    return sanitizedInnerPutAndCopyInternal(pTarget, pVarName, pValue);
  }

  private PersistentSortedMap<String, NumeralFormula<CompoundInterval>> sanitizedInnerPutAndCopyInternal(PersistentSortedMap<String, NumeralFormula<CompoundInterval>> pTarget,
      String pVarName, NumeralFormula<CompoundInterval> pValue) {
    if (isConstantAndContainsAllPossibleValues(pValue)) {
      return pTarget;
    }
    Preconditions.checkArgument(!pTarget.containsKey(pVarName), "Variable must be TOP in previous environment");
    NumeralFormula<CompoundInterval> previous = pTarget.get(pVarName);
    if (pValue.equals(previous)) {
      return pTarget;
    }
    return pTarget.putAndCopy(pVarName, pValue);
  }

  public NonRecursiveEnvironment putAndCopy(String pVarName, NumeralFormula<CompoundInterval> pValue) {
    NumeralFormula<CompoundInterval> previous = get(pVarName);
    if (previous == null && pValue == null) {
      return this;
    }
    if (previous == null) {
      previous = getAllPossibleValues(pValue);
    }
    if (pValue == null) {
      pValue = getAllPossibleValues(previous);
    }
    if (previous.equals(pValue)) {
      return this;
    }
    PersistentSortedMap<String, NumeralFormula<CompoundInterval>> resultInner = sanitizedInnerPutAndCopy(this.inner, this.containsVarVisitor, pVarName, pValue);
    if (this.inner == resultInner) {
      return this;
    }
    return new NonRecursiveEnvironment(this.compoundIntervalManagerFactory, resultInner);
  }

  public NonRecursiveEnvironment putAndCopyAll(Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pM) {
    PersistentSortedMap<String, NumeralFormula<CompoundInterval>> resultInner = this.inner;
    for (java.util.Map.Entry<? extends String, ? extends NumeralFormula<CompoundInterval>> entry : pM.entrySet()) {
      resultInner = sanitizedInnerPutAndCopy(resultInner, new ContainsVarVisitor<>(resultInner), entry.getKey(), entry.getValue());
    }
    return new NonRecursiveEnvironment(this.compoundIntervalManagerFactory, resultInner);
  }

  public NonRecursiveEnvironment removeAndCopy(Object pKey) {
    if (!containsKey(pKey)) {
      return this;
    }
    return new NonRecursiveEnvironment(this.compoundIntervalManagerFactory, this.inner.removeAndCopy(pKey));
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
  public Collection<NumeralFormula<CompoundInterval>> values() {
    return Collections.unmodifiableCollection(this.inner.values());
  }

  @Override
  public SortedSet<java.util.Map.Entry<String, NumeralFormula<CompoundInterval>>> entrySet() {
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
    return inner.equals(o);
  }

  @Override
  public int hashCode() {
    return inner.hashCode();
  }

  private NumeralFormula<CompoundInterval> getAllPossibleValues(BitVectorType pBitVectorType) {
    return InvariantsFormulaManager.INSTANCE.asConstant(
        pBitVectorType.getBitVectorInfo(),
        compoundIntervalManagerFactory
          .createCompoundIntervalManager(pBitVectorType.getBitVectorInfo())
          .allPossibleValues());
  }

  public static NonRecursiveEnvironment of(CompoundIntervalManagerFactory pCompoundIntervalManagerFactory) {
    return new NonRecursiveEnvironment(pCompoundIntervalManagerFactory, PathCopyingPersistentTreeMap.<String, NumeralFormula<CompoundInterval>>of());
  }

  @Deprecated
  public static NonRecursiveEnvironment copyOf(NonRecursiveEnvironment pInner) {
    return pInner;
  }

  public static NonRecursiveEnvironment copyOf(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      Map<String, NumeralFormula<CompoundInterval>> pInner) {
    if (pInner instanceof NonRecursiveEnvironment
        && ((NonRecursiveEnvironment) pInner).compoundIntervalManagerFactory.equals(pCompoundIntervalManagerFactory)) {
      return (NonRecursiveEnvironment) pInner;
    }
    if (pInner instanceof PersistentSortedMap) {
      return new NonRecursiveEnvironment(pCompoundIntervalManagerFactory, pInner);
    }
    return new NonRecursiveEnvironment(pCompoundIntervalManagerFactory, pInner);
  }

  public static class Builder implements Map<String, NumeralFormula<CompoundInterval>> {

    private NonRecursiveEnvironment current;

    public Builder(CompoundIntervalManagerFactory pCompoundIntervalManagerFactory) {
      this(NonRecursiveEnvironment.of(pCompoundIntervalManagerFactory));
    }

    public Builder(
        CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
        Map<String, NumeralFormula<CompoundInterval>> pInitialEnvironment) {
      this(NonRecursiveEnvironment.copyOf(pCompoundIntervalManagerFactory, pInitialEnvironment));
    }

    public Builder(NonRecursiveEnvironment pInitialEnvironment) {
      current = pInitialEnvironment;
    }

    @Override
    public void clear() {
      current = NonRecursiveEnvironment.of(current.compoundIntervalManagerFactory);
    }

    @Override
    public boolean containsKey(Object pKey) {
      return current.containsKey(pKey);
    }

    @Override
    public boolean containsValue(Object pValue) {
      return current.containsValue(pValue);
    }

    @Override
    public Set<Map.Entry<String, NumeralFormula<CompoundInterval>>> entrySet() {
      return current.entrySet();
    }

    @Override
    public NumeralFormula<CompoundInterval> get(Object pKey) {
      return current.get(pKey);
    }

    @Override
    public boolean isEmpty() {
      return current.isEmpty();
    }

    @Override
    public Set<String> keySet() {
      return current.keySet();
    }

    @Override
    public NumeralFormula<CompoundInterval> put(String pKey, NumeralFormula<CompoundInterval> pValue) {
      NumeralFormula<CompoundInterval> result = current.get(pKey);
      current = current.putAndCopy(pKey, pValue);
      return result;
    }

    @Override
    public void putAll(Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pM) {
      current = current.putAndCopyAll(pM);
    }

    @Override
    public NumeralFormula<CompoundInterval> remove(Object pKey) {
      NumeralFormula<CompoundInterval> result = current.get(pKey);
      current = current.removeAndCopy(pKey);
      return result;
    }

    @Override
    public int size() {
      return current.size();
    }

    @Override
    public Collection<NumeralFormula<CompoundInterval>> values() {
      return current.values();
    }

    public NonRecursiveEnvironment build() {
      return current;
    }

    @Override
    public int hashCode() {
      return current.hashCode();
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      if (pOther instanceof Map) {
        return current.equals(pOther);
      }
      return false;
    }

    @Override
    public String toString() {
      return current.toString();
    }

    @Deprecated
    public static Builder of(Builder pTmpEnvironment) {
      return pTmpEnvironment;
    }

    public static Builder of(
        CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
        Map<String, NumeralFormula<CompoundInterval>> pTmpEnvironment) {
      if (pTmpEnvironment instanceof Builder
          && ((Builder) pTmpEnvironment).current.compoundIntervalManagerFactory.equals(pCompoundIntervalManagerFactory)) {
        return (Builder) pTmpEnvironment;
      }
      return new Builder(pCompoundIntervalManagerFactory, pTmpEnvironment);
    }

  }

}
