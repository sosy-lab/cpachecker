// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentMultimap;

/** Utility class for representation comparisons of SMGValues */
public final class SMGPredicateRelation {
  public static class SMGValuesPair implements Comparable<SMGValuesPair> {
    private final SMGValue first;
    private final SMGValue second;

    private SMGValuesPair(SMGValue pFirst, SMGValue pSecond) {
      first = pFirst;
      second = pSecond;
    }

    static SMGValuesPair of(SMGValue pFirst, SMGValue pSecond) {
      checkNotNull(pFirst);
      checkNotNull(pSecond);
      return new SMGValuesPair(pFirst, pSecond);
    }

    public SMGValue getFirst() {
      return first;
    }

    public SMGValue getSecond() {
      return second;
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (!(pO instanceof SMGValuesPair)) {
        return false;
      }
      SMGValuesPair that = (SMGValuesPair) pO;
      return first.equals(that.first) && second.equals(that.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(first, second);
    }

    @Override
    public int compareTo(SMGValuesPair pSMGValuesPair) {
      int cmp = first.compareTo(pSMGValuesPair.first);
      return cmp == 0 ? second.compareTo(pSMGValuesPair.second) : cmp;
    }
  }
  /** The Multimap is used as Bi-Map, i.e. each pair (K,V) is also inserted as pair (V,K). */
  private PersistentMultimap<SMGValuesPair, SymbolicRelation> smgValuesRelation =
      PersistentMultimap.of();

  private PersistentMultimap<SMGValue, SMGValue> smgValuesDependency = PersistentMultimap.of();
  private PersistentMultimap<SMGValue, ExplicitRelation> smgExplicitValueRelation =
      PersistentMultimap.of();

  /** Copy PredRelation */
  public void putAll(SMGPredicateRelation pPred) {
    smgValuesRelation = pPred.smgValuesRelation;
    smgValuesDependency = pPred.smgValuesDependency;
    smgExplicitValueRelation = pPred.smgExplicitValueRelation;
  }

  public void addRelation(
      SMGValue pOne,
      SMGType pSMGTypeOne,
      SMGValue pTwo,
      SMGType pSMGTypeTwo,
      BinaryOperator pOperator) {
    if (!pOne.isUnknown() && !pTwo.isUnknown()) {
      if (pOne instanceof SMGExplicitValue) {
        addExplicitRelation(
            pTwo,
            pSMGTypeTwo,
            (SMGExplicitValue) pOne,
            pOperator.getSwitchOperandsSidesLogicalOperator());
      } else if (pTwo instanceof SMGExplicitValue) {
        addExplicitRelation(pOne, pSMGTypeOne, (SMGExplicitValue) pTwo, pOperator);
      } else if (!pOne.isZero() && !pTwo.isZero()) {
        addSymbolicRelation(pOne, pSMGTypeOne, pTwo, pSMGTypeTwo, pOperator);
      } else if (pOne.isZero() && !pTwo.isZero()) {
        addExplicitRelation(
            pTwo,
            pSMGTypeTwo,
            SMGZeroValue.INSTANCE,
            pOperator.getSwitchOperandsSidesLogicalOperator());
      } else if (pTwo.isZero() && !pOne.isZero()) {
        addExplicitRelation(pOne, pSMGTypeOne, SMGZeroValue.INSTANCE, pOperator);
      }
    }
  }

  public void addSymbolicRelation(
      SMGValue pOne,
      SMGType pSMGTypeOne,
      SMGValue pTwo,
      SMGType pSMGTypeTwo,
      BinaryOperator pOperator) {
    SymbolicRelation relation =
        new SymbolicRelation(pOne, pSMGTypeOne, pTwo, pSMGTypeTwo, pOperator);
    if (!smgValuesDependency.containsEntry(pOne, pTwo)) {
      smgValuesRelation = smgValuesRelation.putAndCopy(SMGValuesPair.of(pOne, pTwo), relation);
      smgValuesRelation = smgValuesRelation.putAndCopy(SMGValuesPair.of(pTwo, pOne), relation);
      smgValuesDependency = smgValuesDependency.putAndCopy(pOne, pTwo);
      smgValuesDependency = smgValuesDependency.putAndCopy(pTwo, pOne);
    } else {
      if (!smgValuesRelation.containsEntry(SMGValuesPair.of(pOne, pTwo), relation)) {
        smgValuesRelation = smgValuesRelation.putAndCopy(SMGValuesPair.of(pOne, pTwo), relation);
        smgValuesRelation = smgValuesRelation.putAndCopy(SMGValuesPair.of(pTwo, pOne), relation);
      }
    }
  }

  public void addExplicitRelation(
      SMGValue pSymbolicValue,
      SMGType pSymbolicSMGType,
      SMGExplicitValue pExplicitValue,
      BinaryOperator pOp) {
    if (!(pSymbolicValue.isZero() && pExplicitValue.isZero())) {
      ExplicitRelation relation =
          new ExplicitRelation(pSymbolicValue, pSymbolicSMGType, pExplicitValue, pOp);
      if (!smgExplicitValueRelation.containsEntry(pSymbolicValue, relation)) {
        smgExplicitValueRelation = smgExplicitValueRelation.putAndCopy(pSymbolicValue, relation);
      }
    }
  }

  public void removeValue(SMGValue pValue) {
    for (SMGValue pOposit : smgValuesDependency.get(pValue)) {

      smgValuesDependency = smgValuesDependency.removeAndCopy(pOposit, pValue);

      smgValuesRelation = smgValuesRelation.removeAndCopy(SMGValuesPair.of(pOposit, pValue));
      smgValuesRelation = smgValuesRelation.removeAndCopy(SMGValuesPair.of(pValue, pOposit));
    }
    smgValuesDependency = smgValuesDependency.removeAndCopy(pValue);
    smgExplicitValueRelation = smgExplicitValueRelation.removeAndCopy(pValue);
  }

  /** replace the old value with a fresh value. */
  public void replace(SMGValue fresh, SMGValue old) {
    for (SMGValue relatedValue : smgValuesDependency.get(old)) {
      smgValuesDependency = smgValuesDependency.removeAndCopy(relatedValue, old);
      ImmutableSet<SymbolicRelation> symbolicRelations =
          smgValuesRelation.get(SMGValuesPair.of(old, relatedValue));
      for (SymbolicRelation symbolicRelation : symbolicRelations) {
        SymbolicRelation newRelation = symbolicRelation.replace(fresh, old);
        addSymbolicRelation(
            newRelation.getFirstValue(),
            newRelation.getFirstValSMGType(),
            newRelation.getSecondValue(),
            newRelation.getSecondValSMGType(),
            newRelation.getOperator());
      }
      smgValuesRelation = smgValuesRelation.removeAndCopy(SMGValuesPair.of(old, relatedValue));
      smgValuesRelation = smgValuesRelation.removeAndCopy(SMGValuesPair.of(relatedValue, old));
    }
    for (ExplicitRelation explicitRelation : smgExplicitValueRelation.get(old)) {
      if (!fresh.isZero()) {
        addExplicitRelation(
            fresh,
            explicitRelation.getSymbolicSMGType(),
            explicitRelation.explicitValue,
            explicitRelation.getOperator());
      }
    }
    smgValuesDependency = smgValuesDependency.removeAndCopy(old);
    smgExplicitValueRelation = smgExplicitValueRelation.removeAndCopy(old);
  }

  /** Returns closure list of symbolic values which affects pRelation */
  public Set<SMGValue> closureDependencyFor(SMGPredicateRelation pRelation) {
    Set<SMGValue> toAdd = new HashSet<>();
    for (Entry<SMGValue, ImmutableSet<SMGValue>> entry : pRelation.smgValuesDependency.entries()) {
      SMGValue key = entry.getKey();
      ImmutableSet<SMGValue> values = entry.getValue();
      for (SMGValue value : values) {
        if (key.compareTo(value) > 0) {
          toAdd.add(key);
          toAdd.add(value);
        }
      }
    }
    Set<SMGValue> result = new HashSet<>();
    while (!toAdd.isEmpty()) {
      result.addAll(toAdd);
      Set<SMGValue> tempAdd = new HashSet<>();
      for (SMGValue symbolic : toAdd) {
        tempAdd.addAll(smgValuesDependency.get(symbolic));
      }
      tempAdd.removeAll(result);
      toAdd = tempAdd;
    }
    return result;
  }

  public boolean isEmpty() {
    return smgExplicitValueRelation.isEmpty() && smgValuesRelation.isEmpty();
  }

  @Override
  public int hashCode() {
    return smgValuesRelation.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SMGPredicateRelation other = (SMGPredicateRelation) obj;
    return other.smgValuesRelation != null && smgValuesRelation.equals(other.smgValuesRelation);
  }

  @Override
  public String toString() {
    return "PredRelation{"
        + "smgValuesRelation="
        + smgValuesRelation
        + ", smgValuesDependency="
        + smgValuesDependency
        + ", smgExplicitValueRelation="
        + smgExplicitValueRelation
        + '}';
  }

  public Collection<ExplicitRelation> getExplicitRelations() {
    return smgExplicitValueRelation.values();
  }

  public Set<Entry<SMGValuesPair, ImmutableSet<SymbolicRelation>>> getValuesRelations() {
    return smgValuesRelation.entries();
  }

  public boolean isLessOrEqual(SMGPredicateRelation pPathPredicateRelation) {
    if (smgValuesDependency.size() > pPathPredicateRelation.smgValuesDependency.size()) {
      return false;
    }
    if (smgExplicitValueRelation.size() > pPathPredicateRelation.smgExplicitValueRelation.size()) {
      return false;
    }
    if (smgValuesRelation.size() > pPathPredicateRelation.smgValuesDependency.size()) {
      return false;
    }
    if (!pPathPredicateRelation
        .smgValuesDependency
        .entries()
        .containsAll(smgValuesDependency.entries())) {
      return false;
    }
    if (!pPathPredicateRelation
        .smgExplicitValueRelation
        .entries()
        .containsAll(smgExplicitValueRelation.entries())) {
      return false;
    }
    if (!pPathPredicateRelation
        .smgValuesRelation
        .entries()
        .containsAll(smgValuesRelation.entries())) {
      return false;
    }
    return true;
  }

  public boolean hasRelation(SMGValue pSymbolicValue) {
    return smgValuesDependency.contains(pSymbolicValue);
  }

  public static class SymbolicRelation {
    private SMGValue valueOne;
    private SMGType firstValSMGType;
    private SMGValue valueTwo;
    private SMGType secondValSMGType;
    private BinaryOperator operator;

    public SymbolicRelation(
        SMGValue pValueOne,
        SMGType pFirstValSMGType,
        SMGValue pValueTwo,
        SMGType pSecondValSMGType,
        BinaryOperator pOperator) {
      valueOne = pValueOne;
      firstValSMGType = pFirstValSMGType;
      valueTwo = pValueTwo;
      secondValSMGType = pSecondValSMGType;
      operator = pOperator;
    }

    public BinaryOperator getOperator() {
      return operator;
    }

    public SMGValue getFirstValue() {
      return valueOne;
    }

    public SMGValue getSecondValue() {
      return valueTwo;
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (!(pO instanceof SymbolicRelation)) {
        return false;
      }

      SymbolicRelation relation = (SymbolicRelation) pO;
      return valueOne.equals(relation.valueOne)
          && valueTwo.equals(relation.valueTwo)
          && operator == relation.operator;
    }

    @Override
    public int hashCode() {
      return Objects.hash(valueOne, valueTwo, operator);
    }

    @Override
    public String toString() {
      return "{S_" + valueOne + " " + operator.getOperator() + " S_" + valueTwo + "}";
    }

    public SMGType getFirstValSMGType() {
      return firstValSMGType;
    }

    public SMGType getSecondValSMGType() {
      return secondValSMGType;
    }

    public SymbolicRelation replace(SMGValue pFresh, SMGValue pOld) {
      if (valueOne.equals(pOld)) {
        return new SymbolicRelation(pFresh, firstValSMGType, valueTwo, secondValSMGType, operator);
      } else {
        assert valueTwo.equals(pOld);
        return new SymbolicRelation(valueOne, firstValSMGType, pFresh, secondValSMGType, operator);
      }
    }
  }

  public static class ExplicitRelation {
    private SMGValue symbolicValue;
    private SMGType symbolicSMGType;
    private SMGExplicitValue explicitValue;
    private BinaryOperator operator;

    public ExplicitRelation(
        SMGValue pSymbolicValue,
        SMGType pSymbolicSMGType,
        SMGExplicitValue pExplicitValue,
        BinaryOperator pOperator) {
      symbolicValue = pSymbolicValue;
      symbolicSMGType = pSymbolicSMGType;
      explicitValue = pExplicitValue;
      operator = pOperator;
    }

    public BinaryOperator getOperator() {
      return operator;
    }

    public SMGExplicitValue getExplicitValue() {
      return explicitValue;
    }

    public SMGValue getSymbolicValue() {
      return symbolicValue;
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (!(pO instanceof ExplicitRelation)) {
        return false;
      }

      ExplicitRelation relation = (ExplicitRelation) pO;
      return symbolicValue.equals(relation.symbolicValue)
          && explicitValue.equals(relation.explicitValue)
          && operator == relation.operator;
    }

    @Override
    public int hashCode() {
      return Objects.hash(symbolicValue, explicitValue, operator);
    }

    @Override
    public String toString() {
      return "{S_" + symbolicValue + " " + operator.getOperator() + " " + explicitValue + "}";
    }

    public SMGType getSymbolicSMGType() {
      return symbolicSMGType;
    }
  }

  public void clear() {
    smgExplicitValueRelation = PersistentMultimap.of();
    smgValuesDependency = PersistentMultimap.of();
    smgValuesRelation = PersistentMultimap.of();
  }
}
