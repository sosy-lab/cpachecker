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
package org.sosy_lab.cpachecker.cpa.invariants.formula.variablerelations;

import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ParameterizedInvariantsFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;

import com.google.common.base.Preconditions;


public class VariableLT<ConstantType> extends AbstractVariableRelation<ConstantType> implements VariableRelation<ConstantType> {

  public VariableLT(Variable<ConstantType> pOperand1, Variable<ConstantType> pOperand2) {
    super(InvariantsFormulaManager.INSTANCE.<ConstantType>lessThan(pOperand1, pOperand2), pOperand1, pOperand2);
  }

  @Override
  public <ReturnType> ReturnType accept(InvariantsFormulaVisitor<ConstantType, ReturnType> pVisitor) {
    return getInvariantsFormula().accept(pVisitor);
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedInvariantsFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor, ParamType pParameter) {
    return getInvariantsFormula().accept(pVisitor, pParameter);
  }
  @Override
  public <T> T accept(VariableRelationVisitor<ConstantType, T> pVariableRelationVisitor) {
    return pVariableRelationVisitor.visit(this);
  }

  @Override
  public boolean equals(Object pO) {
    if (pO == this) {
      return true;
    }
    if (pO instanceof VariableLT<?>) {
      VariableLT<?> other = (VariableLT<?>) pO;
      return getOperand1().equals(other.getOperand1()) && getOperand2().equals(other.getOperand2());
    }
    return false;
  }

  @Override
  public VariableRelation<ConstantType> union(VariableRelation<ConstantType> pOther) {
    Preconditions.checkArgument(isCompatibleWith(pOther));
    return pOther.accept(new VariableRelationVisitor<ConstantType, VariableRelation<ConstantType>>() {

      @Override
      public VariableRelation<ConstantType> visit(VariableEQ<ConstantType> pVariableEquation) {
        return new VariableLEQ<>(getOperand1(), getOperand2());
      }

      @Override
      public VariableRelation<ConstantType> visit(VariableLT<ConstantType> pVariableEquation) {
        if (getOperand1().equals(pVariableEquation.getOperand1()) && getOperand2().equals(pVariableEquation.getOperand2())) {
          return VariableLT.this;
        }
        return new VariableNEQ<>(getOperand1(), getOperand2());
      }

      @Override
      public VariableRelation<ConstantType> visit(VariableLEQ<ConstantType> pVariableEquation) {
        if (getOperand1().equals(pVariableEquation.getOperand1()) && getOperand2().equals(pVariableEquation.getOperand2())) {
          return pVariableEquation;
        }
        return null;
      }

      @Override
      public VariableRelation<ConstantType> visit(VariableNEQ<ConstantType> pVariableEquation) {
        return pVariableEquation;
      }

    });
  }

  @Override
  public VariableRelation<ConstantType> intersect(VariableRelation<ConstantType> pOther) {
    Preconditions.checkArgument(isCompatibleWith(pOther));
    return pOther.accept(new VariableRelationVisitor<ConstantType, VariableRelation<ConstantType>>() {

      @Override
      public VariableRelation<ConstantType> visit(VariableEQ<ConstantType> pVariableEquation) {
        return null;
      }

      @Override
      public VariableRelation<ConstantType> visit(VariableLT<ConstantType> pVariableEquation) {
        if (getOperand1().equals(pVariableEquation.getOperand1()) && getOperand2().equals(pVariableEquation.getOperand2())) {
          return pVariableEquation;
        }
        return null;
      }

      @Override
      public VariableRelation<ConstantType> visit(VariableLEQ<ConstantType> pVariableEquation) {
        if (getOperand1().equals(pVariableEquation.getOperand1()) && getOperand2().equals(pVariableEquation.getOperand2())) {
          return VariableLT.this;
        }
        return null;
      }

      @Override
      public VariableRelation<ConstantType> visit(VariableNEQ<ConstantType> pVariableEquation) {
        return VariableLT.this;
      }

    });
  }

}
