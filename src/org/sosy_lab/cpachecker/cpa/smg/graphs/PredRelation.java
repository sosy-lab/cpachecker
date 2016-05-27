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

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownAddVal;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.util.Pair;

public class PredRelation {
  /** The Multimap is used as Bi-Map, i.e. each pair (K,V) is also inserted as pair (V,K). */
  private final SetMultimap<Pair<Integer, Integer>, SymbolicRelation> smgValuesRelation = HashMultimap.create();
  private final SetMultimap<Integer, Integer> smgValuesDependency = HashMultimap.create();
  private final SetMultimap<Integer, ExplicitRelation> smgExplicitValueRelation = HashMultimap.create();

  public void addRelation(SMGSymbolicValue pOne, SMGSymbolicValue pTwo, BinaryOperator pOperator) {
    //TODO: track address values
    if (!pOne.isUnknown() && !pTwo.isUnknown() &&
        !(pOne instanceof SMGKnownAddVal) && !(pTwo instanceof SMGKnownAddVal)) {
      addRelation(pOne.getAsInt(), pTwo.getAsInt(), pOperator);
    }
  }

  public void addRelation(Integer pOne, Integer pTwo, BinaryOperator pOperator) {
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

  public void addExplicitRelation(SMGSymbolicValue pSymbolicValue, SMGExplicitValue pExplicitValue,
                                  BinaryOperator pOp) {
    addExplicitRelation(pSymbolicValue.getAsInt(), pExplicitValue, pOp);
  }

  public void addExplicitRelation(Integer pSymbolicValue, SMGExplicitValue pExplicitValue,
                                  BinaryOperator pOp) {
    ExplicitRelation relation = new ExplicitRelation(pSymbolicValue, pExplicitValue, pOp);
    if (!smgExplicitValueRelation.containsEntry(pSymbolicValue, relation)) {
      smgExplicitValueRelation.put(pSymbolicValue, relation);
    }
  }

  public void removeValue(Integer pValue) {
    for (Integer pOposit: smgValuesDependency.removeAll(pValue)) {

      smgValuesDependency.remove(pOposit, pValue);

      smgValuesRelation.removeAll(Pair.of(pOposit, pValue));
      smgValuesRelation.removeAll(Pair.of(pValue, pOposit));
    }
    smgExplicitValueRelation.removeAll(pValue);
  }

  public void putAll(PredRelation pPred) {
    smgValuesRelation.putAll(pPred.smgValuesRelation);
    smgValuesDependency.putAll(pPred.smgValuesDependency);
    smgExplicitValueRelation.putAll(pPred.smgExplicitValueRelation);
  }

  public void mergeValues(Integer pV1, Integer pV2) {
    for (Integer relatedValue: smgValuesDependency.removeAll(pV2)) {
      smgValuesDependency.remove(relatedValue, pV2);
      for (SymbolicRelation prev: smgValuesRelation.removeAll(Pair.of(pV2, relatedValue))) {
        addRelation(pV1, relatedValue, prev.getOperator());
      }
      smgValuesRelation.removeAll(Pair.of(relatedValue, pV2));
    }
    for (ExplicitRelation explicitRelation: smgExplicitValueRelation.removeAll(pV2)) {
      addExplicitRelation(pV1, explicitRelation.explicitValue, explicitRelation.getOperator());
    }
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

  static class SymbolicRelation {
    Integer valueOne;
    Integer valueTwo;
    BinaryOperator operator;

    public SymbolicRelation(
        Integer pValueOne,
        Integer pValueTwo,
        BinaryOperator pOperator) {
      valueOne = pValueOne;
      valueTwo = pValueTwo;
      operator = pOperator;
    }

    public BinaryOperator getOperator() {
      return operator;
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

      if (!valueOne.equals(relation.valueOne)) {
        return false;
      }
      if (!valueTwo.equals(relation.valueTwo)) {
        return false;
      }
      if (operator != relation.operator) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = valueOne.hashCode();
      result = 31 * result + valueTwo.hashCode();
      result = 31 * result + operator.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "SymbolicRelation{" +
          "symbolicValue=" + valueOne +
          ", explicitValue=" + valueTwo +
          ", operator=" + operator +
          '}';
    }
  }


  static class ExplicitRelation {
    Integer symbolicValue;
    SMGExplicitValue explicitValue;
    BinaryOperator operator;

    public ExplicitRelation(
        Integer pSymbolicValue,
        SMGExplicitValue pExplicitValue,
        BinaryOperator pOperator) {
      symbolicValue = pSymbolicValue;
      explicitValue = pExplicitValue;
      operator = pOperator;
    }

    public BinaryOperator getOperator() {
      return operator;
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

      if (!symbolicValue.equals(relation.symbolicValue)) {
        return false;
      }
      if (!explicitValue.equals(relation.explicitValue)) {
        return false;
      }
      if (operator != relation.operator) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = symbolicValue.hashCode();
      result = 31 * result + explicitValue.hashCode();
      result = 31 * result + operator.hashCode();
      return result;
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
