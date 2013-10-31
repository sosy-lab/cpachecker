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

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;


public class GuessAssumptionVisitor extends DefaultFormulaVisitor<CompoundState, Set<InvariantsFormula<CompoundState>>> {

  @Override
  protected Set<InvariantsFormula<CompoundState>> visitDefault(InvariantsFormula<CompoundState> pFormula) {
    return Collections.singleton(pFormula);
  }

  @Override
  public Set<InvariantsFormula<CompoundState>> visit(LogicalNot<CompoundState> pNot) {
    return pNot.getNegated().accept(this);
  }

  @Override
  public Set<InvariantsFormula<CompoundState>> visit(Equal<CompoundState> pEqual) {
    return guess(pEqual.getOperand1(), pEqual.getOperand2());
  }

  @Override
  public Set<InvariantsFormula<CompoundState>> visit(LessThan<CompoundState> pLessThan) {
    return guess(pLessThan.getOperand1(), pLessThan.getOperand2());
  }

  @Override
  public Set<InvariantsFormula<CompoundState>> visit(LogicalAnd<CompoundState> pAnd) {
    Set<InvariantsFormula<CompoundState>> guessesLeft = pAnd.getOperand1().accept(this);
    Set<InvariantsFormula<CompoundState>> guessesRight = pAnd.getOperand2().accept(this);
    if (guessesLeft.isEmpty()) {
      return guessesRight;
    }
    if (guessesRight.isEmpty() || guessesLeft.equals(guessesRight)) {
      return guessesLeft;
    }
    if (guessesLeft.size() == 1) {
      if (guessesRight.size() == 1 ) {
        Set<InvariantsFormula<CompoundState>> guesses = new HashSet<>();
        guesses.addAll(guessesLeft);
        guesses.addAll(guessesRight);
        return guesses;
      }
      guessesRight.addAll(guessesLeft);
      return guessesRight;
    }
    guessesLeft.addAll(guessesRight);
    return guessesLeft;
  }

  private Set<InvariantsFormula<CompoundState>> guess(InvariantsFormula<CompoundState> pOperand1, InvariantsFormula<CompoundState> pOperand2) {
    CompoundStateFormulaManager ifm = CompoundStateFormulaManager.INSTANCE;
    Set<InvariantsFormula<CompoundState>> guesses = new HashSet<>();
    InvariantsFormula<CompoundState> equation = ifm.equal(pOperand1, pOperand2);
    guesses.add(equation);
    guesses.add(ifm.lessThan(pOperand1, pOperand2));
    guesses.add(ifm.lessThanOrEqual(pOperand1, pOperand2));
    guesses.add(ifm.greaterThan(pOperand1, pOperand2));
    guesses.add(ifm.greaterThanOrEqual(pOperand1, pOperand2));
    guesses.add(ifm.logicalNot(equation));
    return guesses;
  }

}
