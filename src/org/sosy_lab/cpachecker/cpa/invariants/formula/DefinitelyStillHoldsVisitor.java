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

import java.util.Map;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ChangeAnalyzeVisitor.Result;


public class DefinitelyStillHoldsVisitor extends DefaultParameterizedFormulaVisitor<CompoundState, Map<? extends String, ? extends InvariantsFormula<CompoundState>>, Boolean> implements ParameterizedInvariantsFormulaVisitor<CompoundState, Map<? extends String, ? extends InvariantsFormula<CompoundState>>, Boolean> {

  private final InvariantsFormula<CompoundState> newValue;

  private final FormulaEvaluationVisitor<CompoundState> evaluationVisitor;

  private final ContainsVarVisitor<CompoundState> containsVarVisitor;

  private final ChangeAnalyzeVisitor changeAnalyzeVisitor;

  public DefinitelyStillHoldsVisitor(InvariantsFormula<CompoundState> pNewValue,
      FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor,
      ContainsVarVisitor<CompoundState> containsVarVisitor) {
    this.newValue = pNewValue;
    this.evaluationVisitor = pEvaluationVisitor;
    this.containsVarVisitor = containsVarVisitor;
    this.changeAnalyzeVisitor = new ChangeAnalyzeVisitor(newValue, evaluationVisitor);
  }

  @Override
  public Boolean visit(Equal<CompoundState> pEqual, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    if (visitDefault(pEqual, pEnvironment)) {
      return true;
    }
    boolean leftContains = pEqual.getOperand1().accept(containsVarVisitor);
    boolean rightContains = pEqual.getOperand2().accept(containsVarVisitor);
    if (leftContains) {
      if (rightContains) {
        ChangeAnalyzeVisitor cav = new ChangeAnalyzeVisitor(pEqual.getOperand1(), evaluationVisitor);
        return pEqual.getOperand2().accept(cav, pEnvironment) == Result.EQUAL;
      }
      return pEqual.getOperand1().accept(changeAnalyzeVisitor, pEnvironment) == Result.EQUAL;
    }
    if (rightContains) {
      return pEqual.getOperand2().accept(changeAnalyzeVisitor, pEnvironment) == Result.EQUAL;
    }
    return true;
  }

  @Override
  public Boolean visit(LessThan<CompoundState> pLessThan, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    if (visitDefault(pLessThan, pEnvironment)) {
      return true;
    }
    boolean leftContains = pLessThan.getOperand1().accept(containsVarVisitor);
    boolean rightContains = pLessThan.getOperand2().accept(containsVarVisitor);
    if (leftContains) {
      if (rightContains) {
        ChangeAnalyzeVisitor cav = new ChangeAnalyzeVisitor(pLessThan.getOperand1(), evaluationVisitor);
        return pLessThan.getOperand2().accept(cav, pEnvironment) == Result.LESS_THAN;
      }
      Result compLeftToNew = pLessThan.getOperand1().accept(changeAnalyzeVisitor, pEnvironment);
      return compLeftToNew == Result.GREATER_THAN
          || compLeftToNew == Result.GREATER_THAN_OR_EQUAL
          || compLeftToNew == Result.EQUAL;
    }
    if (rightContains) {
      Result compRightToNew = pLessThan.getOperand2().accept(changeAnalyzeVisitor, pEnvironment);
      return compRightToNew == Result.LESS_THAN
          || compRightToNew == Result.LESS_THAN_OR_EQUAL
          || compRightToNew == Result.EQUAL;
    }
    return true;
  }

  @Override
  public Boolean visit(LogicalAnd<CompoundState> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    if (visitDefault(pAnd, pEnvironment)) {
      return true;
    }
    return pAnd.getOperand1().accept(this, pEnvironment) && pAnd.getOperand2().accept(this, pEnvironment);
  }

  @Override
  protected Boolean visitDefault(InvariantsFormula<CompoundState> pFormula, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pFormula.accept(evaluationVisitor, pEnvironment).isDefinitelyTrue();
  }

}
