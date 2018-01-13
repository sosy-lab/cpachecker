/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public class NegatedCounterexampleToInductivity extends SingleLocationFormulaInvariant {

  private final CounterexampleToInductivity operand;

  public NegatedCounterexampleToInductivity(CounterexampleToInductivity pOperand) {
    super(pOperand.getLocation());
    operand = Objects.requireNonNull(pOperand);
  }

  @Override
  public BooleanFormula getFormula(FormulaManagerView pFMGR, PathFormulaManager pPFMGR,
      @Nullable PathFormula pContext) throws CPATransferException, InterruptedException {
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    BooleanFormula model = bfmgr.makeFalse();
    for (Map.Entry<String, ModelValue> valueAssignment : operand.getAssignments().entrySet()) {
      String variableName = valueAssignment.getKey();
      ModelValue v = valueAssignment.getValue();
      assert variableName.equals(v.getVariableName());
      model = bfmgr.or(model, bfmgr.not(v.toAssignment(pFMGR)));
    }
    return model;
  }

  public CounterexampleToInductivity getCTI() {
    return operand;
  }

  @Override
  public String toString() {
    return String.format("!(%s)", operand);
  }

  @Override
  public int hashCode() {
    return ~operand.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof NegatedCounterexampleToInductivity) {
      NegatedCounterexampleToInductivity other = (NegatedCounterexampleToInductivity) pOther;
      return operand.equals(other.operand);
    }
    return false;
  }

  public NegatedCounterexampleToInductivity dropLiteral(String pVarName) {
    CounterexampleToInductivity newOperand = operand.dropLiteral(pVarName);
    if (newOperand == operand) {
      return this;
    }
    return new NegatedCounterexampleToInductivity(newOperand);
  }
}
