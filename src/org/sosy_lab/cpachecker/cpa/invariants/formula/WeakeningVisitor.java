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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Instances of this class are invariants formula visitors used to guess
 * assumptions similar to visited assumptions.
 *
 * @param <T> the type of the constants used in the formulae.
 */
public class WeakeningVisitor<T> extends DefaultFormulaVisitor<T, Set<InvariantsFormula<T>>> {

  @Override
  public Set<InvariantsFormula<T>> visit(LessThan<T> pLessThan) {
    return Collections.singleton(
        InvariantsFormulaManager.INSTANCE.lessThanOrEqual(pLessThan.getOperand1(), pLessThan.getOperand2()));
  }

  @Override
  public Set<InvariantsFormula<T>> visit(Equal<T> pEqual) {
    InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
    Set<InvariantsFormula<T>> result = new HashSet<>();
    result.add(ifm.lessThanOrEqual(pEqual.getOperand1(), pEqual.getOperand2()));
    result.add(ifm.greaterThanOrEqual(pEqual.getOperand1(), pEqual.getOperand2()));
    return result;
  }

  @Override
  public Set<InvariantsFormula<T>> visit(LogicalAnd<T> pAnd) {
    Set<InvariantsFormula<T>> result = new HashSet<>();
    result.addAll(pAnd.getOperand1().accept(this));
    result.addAll(pAnd.getOperand2().accept(this));
    return result;
  }

  @Override
  public Set<InvariantsFormula<T>> visit(LogicalNot<T> pNot) {
    Set<InvariantsFormula<T>> result = new HashSet<>();
    InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
    for (InvariantsFormula<T> similarInner : pNot.getNegated().accept(this)) {
      InvariantsFormula<T> negatedSimilarInner = ifm.logicalNot(similarInner);
      if (!(negatedSimilarInner instanceof LogicalNot<?>)) {
        result.addAll(similarInner.accept(this));
      } else {
        result.add(similarInner);
      }
    }
    result.add(pNot.getNegated());
    return result;
  }

  @Override
  protected Set<InvariantsFormula<T>> visitDefault(InvariantsFormula<T> pFormula) {
    return Collections.singleton(pFormula);
  }

}
