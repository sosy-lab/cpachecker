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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Instances of this class are invariants formula visitors used to split the
 * visited formulae on their outer conjunctions into lists of the conjunction
 * operands. This is done recursively so that nested conjunctions are split
 * as well.
 *
 * @param <T> the type of the constants used in the formulae.
 */
public class SplitDisjunctionsVisitor<T> implements InvariantsFormulaVisitor<T, List<InvariantsFormula<T>>> {

  @Override
  public List<InvariantsFormula<T>> visit(Add<T> pAdd) {
    return Collections.<InvariantsFormula<T>>singletonList(pAdd);
  }

  @Override
  public List<InvariantsFormula<T>> visit(BinaryAnd<T> pAnd) {
    return Collections.<InvariantsFormula<T>>singletonList(pAnd);
  }

  @Override
  public List<InvariantsFormula<T>> visit(BinaryNot<T> pNot) {
    return Collections.<InvariantsFormula<T>>singletonList(pNot);
  }

  @Override
  public List<InvariantsFormula<T>> visit(BinaryOr<T> pOr) {
    return Collections.<InvariantsFormula<T>>singletonList(pOr);
  }

  @Override
  public List<InvariantsFormula<T>> visit(BinaryXor<T> pXor) {
    return Collections.<InvariantsFormula<T>>singletonList(pXor);
  }

  @Override
  public List<InvariantsFormula<T>> visit(Constant<T> pConstant) {
    return Collections.<InvariantsFormula<T>>singletonList(pConstant);
  }

  @Override
  public List<InvariantsFormula<T>> visit(Divide<T> pDivide) {
    return Collections.<InvariantsFormula<T>>singletonList(pDivide);
  }

  @Override
  public List<InvariantsFormula<T>> visit(Equal<T> pEqual) {
    return Collections.<InvariantsFormula<T>>singletonList(pEqual);
  }

  @Override
  public List<InvariantsFormula<T>> visit(LessThan<T> pLessThan) {
    return Collections.<InvariantsFormula<T>>singletonList(pLessThan);
  }

  @Override
  public List<InvariantsFormula<T>> visit(LogicalAnd<T> pAnd) {
    return Collections.<InvariantsFormula<T>>singletonList(pAnd);
  }

  @Override
  public List<InvariantsFormula<T>> visit(LogicalNot<T> pNot) {
    if (pNot.getNegated() instanceof LogicalAnd<?>) {
      List<InvariantsFormula<T>> result = new ArrayList<>();
      LogicalAnd<T> formula = (LogicalAnd<T>) pNot.getNegated();
      InvariantsFormula<T> term = formula.getOperand1();
      if (!(term instanceof LogicalNot<?>)) {
        term = InvariantsFormulaManager.INSTANCE.logicalNot(term);
      } else {
        term = ((LogicalNot<T>) term).getNegated();
      }
      result.addAll(term.accept(this));
      term = formula.getOperand2();
      if (!(term instanceof LogicalNot<?>)) {
        term = InvariantsFormulaManager.INSTANCE.logicalNot(term);
      } else {
        term = ((LogicalNot<T>) term).getNegated();
      }
      result.addAll(term.accept(this));
      return result;
    }
    return Collections.<InvariantsFormula<T>>singletonList(pNot);
  }

  @Override
  public List<InvariantsFormula<T>> visit(Modulo<T> pModulo) {
    return Collections.<InvariantsFormula<T>>singletonList(pModulo);
  }

  @Override
  public List<InvariantsFormula<T>> visit(Multiply<T> pMultiply) {
    return Collections.<InvariantsFormula<T>>singletonList(pMultiply);
  }

  @Override
  public List<InvariantsFormula<T>> visit(ShiftLeft<T> pShiftLeft) {
    return Collections.<InvariantsFormula<T>>singletonList(pShiftLeft);
  }

  @Override
  public List<InvariantsFormula<T>> visit(ShiftRight<T> pShiftRight) {
    return Collections.<InvariantsFormula<T>>singletonList(pShiftRight);
  }

  @Override
  public List<InvariantsFormula<T>> visit(Union<T> pUnion) {
    return Collections.<InvariantsFormula<T>>singletonList(pUnion);
  }

  @Override
  public List<InvariantsFormula<T>> visit(Variable<T> pVariable) {
    return Collections.<InvariantsFormula<T>>singletonList(pVariable);
  }

}
