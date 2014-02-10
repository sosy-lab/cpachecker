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

import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;


public abstract class AbstractVariableRelation<ConstantType> implements VariableRelation<ConstantType> {

  private final Object compatibilityKey;

  private final InvariantsFormula<ConstantType> invariantsFormula;

  private final Variable<ConstantType> operand1;

  private final Variable<ConstantType> operand2;

  public AbstractVariableRelation(InvariantsFormula<ConstantType> pInvariantsFormula, Variable<ConstantType> pOperand1, Variable<ConstantType> pOperand2) {
    this.invariantsFormula = pInvariantsFormula;
    this.operand1 = pOperand1;
    this.operand2 = pOperand2;
    this.compatibilityKey = new CompatibilityKey(pOperand1, pOperand2);
  }

  @Override
  public InvariantsFormula<ConstantType> getInvariantsFormula() {
    return this.invariantsFormula;
  }

  @Override
  public Variable<ConstantType> getOperand1() {
    return this.operand1;
  }

  @Override
  public Variable<ConstantType> getOperand2() {
    return this.operand2;
  }

  @Override
  public int hashCode() {
    return (operand1.hashCode() * 317) * operand2.hashCode() + 43;
  }

  @Override
  public abstract boolean equals(Object o);

  @Override
  public boolean isCompatibleWith(VariableRelation<ConstantType> pOther) {
    return getCompatibilityKey().equals(pOther.getCompatibilityKey());
  }

  @Override
  public String toString() {
    return getInvariantsFormula().toString();
  }

  @Override
  public Object getCompatibilityKey() {
    return this.compatibilityKey;
  }

  private static class CompatibilityKey {

    private final String var1;

    private final String var2;

    public CompatibilityKey(Variable<?> pVarA, Variable<?> pVarB) {
      if (pVarA.getName().compareTo(pVarB.getName()) <= 0) {
        this.var1 = pVarA.getName();
        this.var2 = pVarB.getName();
      } else {
        this.var1 = pVarB.getName();
        this.var2 = pVarA.getName();
      }
    }

    @Override
    public String toString() {
      return String.format("(%s, %s)", var1, var2);
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO instanceof CompatibilityKey) {
        CompatibilityKey other = (CompatibilityKey) pO;
        return var1.equals(other.var1) && var2.equals(other.var2);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return var1.hashCode() * 43 + 31 + var2.hashCode();
    }

  }

}
