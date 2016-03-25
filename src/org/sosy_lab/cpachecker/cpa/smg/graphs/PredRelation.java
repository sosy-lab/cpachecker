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
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownAddVal;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.util.Pair;

public class PredRelation {
  /** The Multimap is used as Bi-Map, i.e. each pair (K,V) is also inserted as pair (V,K). */
  private final SetMultimap<Pair<Integer, Integer>, Relation> smgValuesRelation = HashMultimap.create();
  private final SetMultimap<Integer, Integer> smgValuesDependancy = HashMultimap.create();

  public void addRelation(SMGSymbolicValue pOne, SMGSymbolicValue pTwo, BinaryOperator pOperator) {
    //TODO: track explicit values
    if (!pOne.isUnknown() && !pTwo.isUnknown() &&
        !(pOne instanceof SMGKnownAddVal) && !(pTwo instanceof SMGKnownAddVal)) {
      addRelation(pOne.getAsInt(), pTwo.getAsInt(), pOperator);
    }
  }

  public void addRelation(Integer pOne, Integer pTwo, BinaryOperator pOperator) {
    if (!smgValuesDependancy.containsEntry(pOne, pTwo)) {
      smgValuesRelation.put(Pair.of(pOne, pTwo), new Relation(pOne, pTwo, pOperator));
      smgValuesRelation.put(Pair.of(pTwo, pOne), new Relation(pOne, pTwo, pOperator));
      smgValuesDependancy.put(pOne, pTwo);
      smgValuesDependancy.put(pTwo, pOne);
    } else {
      if (!smgValuesRelation.containsEntry(Pair.of(pOne, pTwo), new Relation(pOne, pTwo, pOperator))) {
        smgValuesRelation.put(Pair.of(pOne, pTwo), new Relation(pOne, pTwo, pOperator));
        smgValuesRelation.put(Pair.of(pTwo, pOne), new Relation(pOne, pTwo, pOperator));
      }
    }
  }

  public void removeValue(Integer pValue) {
    for (Integer pOposit: smgValuesDependancy.removeAll(pValue)) {

      smgValuesDependancy.remove(pOposit, pValue);

      smgValuesRelation.removeAll(Pair.of(pOposit, pValue));
      smgValuesRelation.removeAll(Pair.of(pValue, pOposit));

    }
  }

  public void putAll(PredRelation pPred) {
    smgValuesRelation.putAll(pPred.smgValuesRelation);
    smgValuesDependancy.putAll(pPred.smgValuesDependancy);
  }

  public void mergeValues(Integer pV1, Integer pV2) {
    for (Integer relatedValue: smgValuesDependancy.removeAll(pV2)) {
      smgValuesDependancy.remove(relatedValue, pV2);
      for (Relation prev: smgValuesRelation.removeAll(Pair.of(pV2, relatedValue))) {
        addRelation(pV1, relatedValue, prev.getOperator());
      }
      smgValuesRelation.removeAll(Pair.of(relatedValue, pV2));
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
    return "pred_rel=" + smgValuesRelation.toString();
  }

  static class Relation {
    Integer valueOne;
    Integer valueTwo;
    BinaryOperator operator;

    public Relation(
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
      if (!(pO instanceof Relation)) {
        return false;
      }

      Relation relation = (Relation) pO;

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
      return "Relation{" +
          "valueOne=" + valueOne +
          ", valueTwo=" + valueTwo +
          ", operator=" + operator +
          '}';
    }
  }
}
