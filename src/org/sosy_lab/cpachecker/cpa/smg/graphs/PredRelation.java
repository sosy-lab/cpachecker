/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.util.Pair;

public final class PredRelation {
  /** The Multimap is used as Bi-Map, i.e. each pair (K,V) is also inserted as pair (V,K). */
  private final SetMultimap<Pair<SMGValue, SMGValue>, SymbolicRelation> smgValuesRelation =
      HashMultimap.create();

  private final SetMultimap<SMGValue, SMGValue> smgValuesDependency = HashMultimap.create();
  private final SetMultimap<SMGValue, ExplicitRelation> smgExplicitValueRelation =
      HashMultimap.create();
  private final Map<SMGValue, Integer> smgValueSizeInBits = new HashMap<>();

  /** Copy PredRelation */
  public void putAll(PredRelation pPred) {
    smgValuesRelation.putAll(pPred.smgValuesRelation);
    smgValuesDependency.putAll(pPred.smgValuesDependency);
    smgExplicitValueRelation.putAll(pPred.smgExplicitValueRelation);
    smgValueSizeInBits.putAll(pPred.smgValueSizeInBits);
  }

  public void addRelation(SMGSymbolicValue pOne, int pCType1,
                          SMGSymbolicValue pTwo, int pCType2,
                          BinaryOperator pOperator) {
    // TODO: track address values
    if (!pOne.isUnknown()
        && !pTwo.isUnknown()
        && !(pOne instanceof SMGKnownAddressValue)
        && !(pTwo instanceof SMGKnownAddressValue)) {
      addRelation(pOne, pTwo, pOperator);
      addValueSize(pOne, pCType1);
      addValueSize(pTwo, pCType2);
    }
  }

  private void addValueSize(SMGValue pValue, Integer pCType2) {
    if (!smgValueSizeInBits.containsKey(pValue)) {
      smgValueSizeInBits.put(pValue, pCType2);
    }
  }

  public void addRelation(SMGValue pOne, SMGValue pTwo, BinaryOperator pOperator) {
    SymbolicRelation relation = new SymbolicRelation(pOne, pTwo, pOperator);
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

  public void addExplicitRelation(SMGSymbolicValue pSymbolicValue, Integer pCType1,
                                  SMGExplicitValue pExplicitValue, Integer pCType2,
                                  BinaryOperator pOp) {
    assert(pCType1.equals(pCType2));
    addExplicitRelation(pSymbolicValue, pExplicitValue, pOp);
    addValueSize(pSymbolicValue, pCType1);
  }

  public void addExplicitRelation(
      SMGValue pSymbolicValue, SMGExplicitValue pExplicitValue, BinaryOperator pOp) {
    ExplicitRelation relation = new ExplicitRelation(pSymbolicValue, pExplicitValue, pOp);
    if (!smgExplicitValueRelation.containsEntry(pSymbolicValue, relation)) {
      smgExplicitValueRelation.put(pSymbolicValue, relation);
    }
  }

  public void removeValue(SMGValue pValue) {
    for (SMGValue pOposit : smgValuesDependency.removeAll(pValue)) {

      smgValuesDependency.remove(pOposit, pValue);

      smgValuesRelation.removeAll(Pair.of(pOposit, pValue));
      smgValuesRelation.removeAll(Pair.of(pValue, pOposit));
    }
    smgExplicitValueRelation.removeAll(pValue);
    smgValueSizeInBits.remove(pValue);
  }

  /** replace the old value with a fresh value. */
  public void replace(SMGValue fresh, SMGValue old) {
    for (SMGValue relatedValue : smgValuesDependency.removeAll(old)) {
      smgValuesDependency.remove(relatedValue, old);
      smgValuesRelation.removeAll(Pair.of(old, relatedValue));
        //TODO: modify predicates on merge values
      smgValuesRelation.removeAll(Pair.of(relatedValue, old));
    }
    for (ExplicitRelation explicitRelation: smgExplicitValueRelation.removeAll(old)) {
      addExplicitRelation(fresh, explicitRelation.explicitValue, explicitRelation.getOperator());
      addValueSize(fresh, getSymbolicSize(old));
    }
    smgValueSizeInBits.remove(old);
  }

  public Integer getSymbolicSize(SMGValue pSymbolic) {
    return smgValueSizeInBits.get(pSymbolic);
  }

  /** Returns closure list of symbolic values which affects pRelation */
  public Set<SMGValue> closureDependencyFor(PredRelation pRelation) {
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
    PredRelation other = (PredRelation) obj;
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

  public boolean isLessOrEqual(PredRelation pPathPredicateRelation) {
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

  static public class SymbolicRelation {
    final SMGValue valueOne;
    SMGValue valueTwo;
    BinaryOperator operator;

    public SymbolicRelation(SMGValue pValueOne, SMGValue pValueTwo, BinaryOperator pOperator) {
      valueOne = pValueOne;
      valueTwo = pValueTwo;
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
      return "SymbolicRelation{" +
          "symbolicValue1=" + valueOne +
          ", symbolicValue2=" + valueTwo +
          ", operator=" + operator +
          '}';
    }
  }


  static public class ExplicitRelation {
    SMGValue symbolicValue;
    SMGExplicitValue explicitValue;
    BinaryOperator operator;

    public ExplicitRelation(
        SMGValue pSymbolicValue, SMGExplicitValue pExplicitValue, BinaryOperator pOperator) {
      symbolicValue = pSymbolicValue;
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
      return "ExplicitRelation{" +
          "symbolicValue=" + symbolicValue +
          ", explicitValue=" + explicitValue +
          ", operator=" + operator +
          '}';
    }
  }

  public void clear() {
    smgExplicitValueRelation.clear();
    smgValuesDependency.clear();
    smgValuesRelation.clear();
  }
}
