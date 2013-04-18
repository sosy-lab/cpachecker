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

public class CollectVarsVisitor<T> implements InvariantsFormulaVisitor<T, Set<String>> {

  private static final Set<String> EMPTY_SET = Collections.emptySet();

  @Override
  public Set<String> visit(Add<T> pAdd) {
    return concat(pAdd.getSummand1().accept(this), pAdd.getSummand2().accept(this));
  }

  @Override
  public Set<String> visit(BinaryAnd<T> pAnd) {
    return concat(pAnd.getOperand1().accept(this), pAnd.getOperand2().accept(this));
  }

  @Override
  public Set<String> visit(BinaryNot<T> pNot) {
    return pNot.getFlipped().accept(this);
  }

  @Override
  public Set<String> visit(BinaryOr<T> pOr) {
    return concat(pOr.getOperand1().accept(this), pOr.getOperand2().accept(this));
  }

  @Override
  public Set<String> visit(BinaryXor<T> pXor) {
    return concat(pXor.getOperand1().accept(this), pXor.getOperand2().accept(this));
  }

  @Override
  public Set<String> visit(Constant<T> pConstant) {
    return EMPTY_SET;
  }

  @Override
  public Set<String> visit(Divide<T> pDivide) {
    return concat(pDivide.getNumerator().accept(this), pDivide.getDenominator().accept(this));
  }

  @Override
  public Set<String> visit(Equal<T> pEqual) {
    return concat(pEqual.getOperand1().accept(this), pEqual.getOperand2().accept(this));
  }

  @Override
  public Set<String> visit(LessThan<T> pLessThan) {
    return concat(pLessThan.getOperand1().accept(this), pLessThan.getOperand2().accept(this));
  }

  @Override
  public Set<String> visit(LogicalAnd<T> pAnd) {
    return concat(pAnd.getOperand1().accept(this), pAnd.getOperand2().accept(this));
  }

  @Override
  public Set<String> visit(LogicalNot<T> pNot) {
    return pNot.getNegated().accept(this);
  }

  @Override
  public Set<String> visit(Modulo<T> pModulo) {
    return concat(pModulo.getNumerator().accept(this), pModulo.getDenominator().accept(this));
  }

  @Override
  public Set<String> visit(Multiply<T> pMultiply) {
    return concat(pMultiply.getFactor1().accept(this), pMultiply.getFactor2().accept(this));
  }

  @Override
  public Set<String> visit(Negate<T> pNegate) {
    return pNegate.getNegated().accept(this);
  }

  @Override
  public Set<String> visit(ShiftLeft<T> pShiftLeft) {
    return concat(pShiftLeft.getShifted().accept(this), pShiftLeft.getShiftDistance().accept(this));
  }

  @Override
  public Set<String> visit(ShiftRight<T> pShiftRight) {
    return concat(pShiftRight.getShifted().accept(this), pShiftRight.getShiftDistance().accept(this));
  }

  @Override
  public Set<String> visit(Union<T> pUnion) {
    return concat(pUnion.getOperand1().accept(this), pUnion.getOperand2().accept(this));
  }

  @Override
  public Set<String> visit(Variable<T> pVariable) {
    return Collections.singleton(pVariable.getName());
  }

  private Set<String> concat(Set<String> a, Set<String> b) {
    if (a.isEmpty()) {
      return b;
    }
    if (b.isEmpty()) {
      return a;
    }
    if (a.equals(b)) {
      return a;
    }
    if (a.size() == 1 && b.size() == 1) {
      Set<String> result = new HashSet<>(a);
      result.addAll(b);
      return result;
    }
    if (a.size() == 1) {
      b.addAll(a);
      return b;
    }
    if (b.size() == 1) {
      a.addAll(b);
      return a;
    }
    a.addAll(b);
    return a;
  }

}
