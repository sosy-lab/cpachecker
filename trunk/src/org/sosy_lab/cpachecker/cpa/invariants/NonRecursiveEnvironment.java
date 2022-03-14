// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Constant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ContainsVarVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class NonRecursiveEnvironment
    implements Map<MemoryLocation, NumeralFormula<CompoundInterval>> {

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR =
      new CollectVarsVisitor<>();

  private final PersistentSortedMap<MemoryLocation, NumeralFormula<CompoundInterval>> inner;

  private final FormulaEvaluationVisitor<CompoundInterval> formulaEvaluationVisitor;

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  private NonRecursiveEnvironment(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      Map<MemoryLocation, NumeralFormula<CompoundInterval>> pInner) {
    this(pCompoundIntervalManagerFactory, PathCopyingPersistentTreeMap.copyOf(pInner));
  }

  private NonRecursiveEnvironment(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      PersistentSortedMap<MemoryLocation, NumeralFormula<CompoundInterval>> pInner) {
    inner = pInner;
    compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    formulaEvaluationVisitor =
        new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory);
  }

  @Override
  public int size() {
    return inner.size();
  }

  @Override
  public boolean isEmpty() {
    return inner.isEmpty();
  }

  @Override
  public boolean containsKey(Object pVarName) {
    return inner.containsKey(pVarName);
  }

  @Override
  public boolean containsValue(Object pValue) {
    return inner.containsValue(pValue);
  }

  @Override
  public NumeralFormula<CompoundInterval> get(Object pVarName) {
    return inner.get(pVarName);
  }

  @Override
  @Deprecated
  public NumeralFormula<CompoundInterval> put(
      MemoryLocation pVarName, NumeralFormula<CompoundInterval> pValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public NumeralFormula<CompoundInterval> remove(Object pKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void putAll(Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pM) {
    throw new UnsupportedOperationException();
  }

  private boolean isConstantAndContainsAllPossibleValues(
      NumeralFormula<CompoundInterval> pFormula) {
    if (pFormula instanceof Constant) {
      return ((Constant<CompoundInterval>) pFormula).getValue().containsAllPossibleValues();
    }
    return false;
  }

  private PersistentSortedMap<MemoryLocation, NumeralFormula<CompoundInterval>>
      sanitizedInnerPutAndCopy(
          PersistentSortedMap<MemoryLocation, NumeralFormula<CompoundInterval>> pTarget,
          MemoryLocation pMemoryLocation,
          NumeralFormula<CompoundInterval> pValue) {
    if (pValue == null || isConstantAndContainsAllPossibleValues(pValue)) {
      if (pTarget.containsKey(pMemoryLocation)) {
        return pTarget.removeAndCopy(pMemoryLocation);
      }
      return pTarget;
    }
    NumeralFormula<CompoundInterval> previous = pTarget.get(pMemoryLocation);
    if (pValue.equals(previous)) {
      return pTarget;
    }
    pTarget = pTarget.removeAndCopy(pMemoryLocation);
    ContainsVarVisitor<CompoundInterval> containsVarVisitor = new ContainsVarVisitor<>(pTarget);
    TypeInfo typeInfo = pValue.getTypeInfo();
    if (pValue.accept(containsVarVisitor, pMemoryLocation)) {
      return sanitizedInnerPutAndCopyInternal(
          pTarget,
          pMemoryLocation,
          InvariantsFormulaManager.INSTANCE.asConstant(
              typeInfo, pValue.accept(formulaEvaluationVisitor, this)));
    }
    NumeralFormula<CompoundInterval> variable =
        InvariantsFormulaManager.INSTANCE.asVariable(typeInfo, pMemoryLocation);
    for (MemoryLocation containedMemoryLocation : pValue.accept(COLLECT_VARS_VISITOR)) {
      if (variable.accept(containsVarVisitor, containedMemoryLocation)) {
        return sanitizedInnerPutAndCopyInternal(
            pTarget,
            pMemoryLocation,
            InvariantsFormulaManager.INSTANCE.asConstant(
                typeInfo, pValue.accept(formulaEvaluationVisitor, this)));
      }
    }
    return sanitizedInnerPutAndCopyInternal(pTarget, pMemoryLocation, pValue);
  }

  private PersistentSortedMap<MemoryLocation, NumeralFormula<CompoundInterval>>
      sanitizedInnerPutAndCopyInternal(
          PersistentSortedMap<MemoryLocation, NumeralFormula<CompoundInterval>> pTarget,
          MemoryLocation pMemoryLocation,
          NumeralFormula<CompoundInterval> pValue) {
    if (isConstantAndContainsAllPossibleValues(pValue)) {
      return pTarget;
    }
    Preconditions.checkArgument(
        !pTarget.containsKey(pMemoryLocation), "Variable must be TOP in previous environment");
    NumeralFormula<CompoundInterval> previous = pTarget.get(pMemoryLocation);
    if (pValue.equals(previous)) {
      return pTarget;
    }
    return pTarget.putAndCopy(pMemoryLocation, pValue);
  }

  public NonRecursiveEnvironment putAndCopy(
      MemoryLocation pVarName, NumeralFormula<CompoundInterval> pValue) {
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
    PersistentSortedMap<MemoryLocation, NumeralFormula<CompoundInterval>> resultInner =
        sanitizedInnerPutAndCopy(inner, pVarName, pValue);
    if (inner == resultInner) {
      return this;
    }
    return new NonRecursiveEnvironment(compoundIntervalManagerFactory, resultInner);
  }

  public NonRecursiveEnvironment putAndCopyAll(
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pM) {
    PersistentSortedMap<MemoryLocation, NumeralFormula<CompoundInterval>> resultInner = inner;
    for (java.util.Map.Entry<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>>
        entry : pM.entrySet()) {
      resultInner = sanitizedInnerPutAndCopy(resultInner, entry.getKey(), entry.getValue());
    }
    return new NonRecursiveEnvironment(compoundIntervalManagerFactory, resultInner);
  }

  public NonRecursiveEnvironment removeAndCopy(Object pKey) {
    if (!containsKey(pKey)) {
      return this;
    }
    return new NonRecursiveEnvironment(compoundIntervalManagerFactory, inner.removeAndCopy(pKey));
  }

  @Override
  @Deprecated
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NavigableSet<MemoryLocation> keySet() {
    return inner.keySet();
  }

  @Override
  public Collection<NumeralFormula<CompoundInterval>> values() {
    return Collections.unmodifiableCollection(inner.values());
  }

  @Override
  public NavigableSet<Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>>> entrySet() {
    return inner.entrySet();
  }

  @Override
  public String toString() {
    return inner.toString();
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

  private NumeralFormula<CompoundInterval> getAllPossibleValues(Typed pTyped) {
    return InvariantsFormulaManager.INSTANCE.asConstant(
        pTyped.getTypeInfo(),
        compoundIntervalManagerFactory
            .createCompoundIntervalManager(pTyped.getTypeInfo())
            .allPossibleValues());
  }

  public static NonRecursiveEnvironment of(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory) {
    return new NonRecursiveEnvironment(
        pCompoundIntervalManagerFactory,
        PathCopyingPersistentTreeMap.<MemoryLocation, NumeralFormula<CompoundInterval>>of());
  }

  public static NonRecursiveEnvironment copyOf(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      Map<MemoryLocation, NumeralFormula<CompoundInterval>> pInner) {
    if (pInner instanceof NonRecursiveEnvironment
        && ((NonRecursiveEnvironment) pInner)
            .compoundIntervalManagerFactory.equals(pCompoundIntervalManagerFactory)) {
      return (NonRecursiveEnvironment) pInner;
    }
    return new NonRecursiveEnvironment(pCompoundIntervalManagerFactory, pInner);
  }

  public static class Builder implements Map<MemoryLocation, NumeralFormula<CompoundInterval>> {

    private NonRecursiveEnvironment current;

    public Builder(CompoundIntervalManagerFactory pCompoundIntervalManagerFactory) {
      this(NonRecursiveEnvironment.of(pCompoundIntervalManagerFactory));
    }

    public Builder(
        CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
        Map<MemoryLocation, NumeralFormula<CompoundInterval>> pInitialEnvironment) {
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
    public Set<Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>>> entrySet() {
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
    public Set<MemoryLocation> keySet() {
      return current.keySet();
    }

    @Override
    public NumeralFormula<CompoundInterval> put(
        MemoryLocation pKey, NumeralFormula<CompoundInterval> pValue) {
      NumeralFormula<CompoundInterval> result = current.get(pKey);
      current = current.putAndCopy(pKey, pValue);
      return result;
    }

    @Override
    public void putAll(
        Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pM) {
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

    public static Builder of(
        CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
        Map<MemoryLocation, NumeralFormula<CompoundInterval>> pTmpEnvironment) {
      if (pTmpEnvironment instanceof Builder
          && ((Builder) pTmpEnvironment)
              .current.compoundIntervalManagerFactory.equals(pCompoundIntervalManagerFactory)) {
        return (Builder) pTmpEnvironment;
      }
      return new Builder(pCompoundIntervalManagerFactory, pTmpEnvironment);
    }
  }
}
