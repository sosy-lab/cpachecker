// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.util.Pair;

/** Utility class for representation comparisons of SMGValues */
public final class SMGPredicateRelation {
  /** The Multimap is used as Bi-Map, i.e. each pair (K,V) is also inserted as pair (V,K). */
  private final SetMultimap<Pair<SMGValue, SMGValue>, SymbolicRelation> smgValuesRelation =
      HashMultimap.create();

  private final SetMultimap<SMGValue, SMGValue> smgValuesDependency = HashMultimap.create();
  private final SetMultimap<SMGValue, ExplicitRelation> smgExplicitValueRelation =
      HashMultimap.create();

  /** Copy PredRelation */
  public void putAll(SMGPredicateRelation pPred) {
    smgValuesRelation.putAll(pPred.smgValuesRelation);
    smgValuesDependency.putAll(pPred.smgValuesDependency);
    smgExplicitValueRelation.putAll(pPred.smgExplicitValueRelation);
  }

  public void addRelation(
      SMGValue pOne,
      SMGType pSMGTypeOne,
      SMGValue pTwo,
      SMGType pSMGTypeTwo,
      BinaryOperator pOperator) {
    // TODO: track address values
    if (!pOne.isUnknown()
        && !pTwo.isUnknown()
        && !(pOne instanceof SMGKnownAddressValue)
        && !(pTwo instanceof SMGKnownAddressValue)) {
      if (!pOne.isZero() && !pTwo.isZero()) {
        addSymbolicRelation(pOne, pSMGTypeOne, pTwo, pSMGTypeTwo, pOperator);
      }
      if (pOne.isZero() && !pTwo.isZero()) {
        addExplicitRelation(
            pTwo, pSMGTypeTwo, SMGZeroValue.INSTANCE, pOperator.getOppositLogicalOperator());
      }
      if (pTwo.isZero() && !pOne.isZero()) {
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
      smgValuesRelation.put(Pair.of(pOne, pTwo), relation);
      smgValuesRelation.put(Pair.of(pTwo, pOne), relation);
      smgValuesDependency.put(pOne, pTwo);
      smgValuesDependency.put(pTwo, pOne);
    } else {
      if (!smgValuesRelation.containsEntry(Pair.of(pOne, pTwo), relation)) {
        smgValuesRelation.put(Pair.of(pOne, pTwo), relation);
        smgValuesRelation.put(Pair.of(pTwo, pOne), relation);
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
        smgExplicitValueRelation.put(pSymbolicValue, relation);
      }
    }
  }

  public void removeValue(SMGValue pValue) {
    for (SMGValue pOposit : smgValuesDependency.removeAll(pValue)) {

      smgValuesDependency.remove(pOposit, pValue);

      smgValuesRelation.removeAll(Pair.of(pOposit, pValue));
      smgValuesRelation.removeAll(Pair.of(pValue, pOposit));
    }
    smgExplicitValueRelation.removeAll(pValue);
  }

  /** replace the old value with a fresh value. */
  public void replace(SMGValue fresh, SMGValue old) {
    for (SMGValue relatedValue : smgValuesDependency.removeAll(old)) {
      smgValuesDependency.remove(relatedValue, old);
      smgValuesRelation.removeAll(Pair.of(old, relatedValue));
      // TODO: modify predicates on merge values
      smgValuesRelation.removeAll(Pair.of(relatedValue, old));
    }
    for (ExplicitRelation explicitRelation: smgExplicitValueRelation.removeAll(old)) {
      if (!fresh.isZero()) {
        addExplicitRelation(
            fresh,
            explicitRelation.getSymbolicSMGType(),
            explicitRelation.explicitValue,
            explicitRelation.getOperator());
      }
    }
  }

  /** Returns closure list of symbolic values which affects pRelation */
  public Set<SMGValue> closureDependencyFor(SMGPredicateRelation pRelation) {
    Set<SMGValue> toAdd = new HashSet<>();
    for (Entry<SMGValue, SMGValue> entry : pRelation.smgValuesDependency.entries()) {
      SMGValue key = entry.getKey();
      SMGValue value = entry.getValue();
      if (key.compareTo(value) > 0) {
        toAdd.add(key);
        toAdd.add(value);
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
    return "PredRelation{" +
        "smgValuesRelation=" + smgValuesRelation +
        ", smgValuesDependency=" + smgValuesDependency +
        ", smgExplicitValueRelation=" + smgExplicitValueRelation +
        '}';
  }

  public Collection<ExplicitRelation> getExplicitRelations() {
    return smgExplicitValueRelation.values();
  }

  public Set<Entry<Pair<SMGValue, SMGValue>, SymbolicRelation>> getValuesRelations() {
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
    if (!pPathPredicateRelation.smgValuesDependency.entries().containsAll(smgValuesDependency.entries())) {
      return false;
    }
    if (!pPathPredicateRelation.smgExplicitValueRelation.entries().containsAll(smgExplicitValueRelation.entries())) {
      return false;
    }
    if (!pPathPredicateRelation.smgValuesRelation.entries().containsAll(smgValuesRelation.entries())) {
      return false;
    }
    return true;
  }

  public boolean hasRelation(SMGValue pSymbolicValue) {
    return smgValuesDependency.containsKey(pSymbolicValue);
  }

  static public class SymbolicRelation {
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
      return "SymbolicRelation{"
          + "symbolicValue1="
          + valueOne
          + ", symbolicValue2="
          + valueTwo
          + ", operator="
          + operator
          + '}';
    }

    public SMGType getFirstValSMGType() {
      return firstValSMGType;
    }

    public SMGType getSecondValSMGType() {
      return secondValSMGType;
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
      return "ExplicitRelation{"
          + "symbolicValue="
          + symbolicValue
          + ", explicitValue="
          + explicitValue
          + ", operator="
          + operator
          + '}';
    }

    public SMGType getSymbolicSMGType() {
      return symbolicSMGType;
    }
  }

  public void clear() {
    smgExplicitValueRelation.clear();
    smgValuesDependency.clear();
    smgValuesRelation.clear();
  }
}
