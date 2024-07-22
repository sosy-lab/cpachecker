// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import java.util.List;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;

public class SerializeNumeralFormulaVisitor
    implements NumeralFormulaVisitor<CompoundInterval, String> {

  @Override
  public String visit(Add<CompoundInterval> pAdd) {
    return "(" + pAdd.getSummand1().accept(this) + " + " + pAdd.getSummand2().accept(this) + ")";
  }

  @Override
  public String visit(BinaryAnd<CompoundInterval> pAnd) {
    return "(" + pAnd.getOperand1().accept(this) + " & " + pAnd.getOperand2().accept(this) + ")";
  }

  @Override
  public String visit(BinaryNot<CompoundInterval> pNot) {
    return "!(" + pNot.getFlipped().accept(this) + ")";
  }

  @Override
  public String visit(BinaryOr<CompoundInterval> pOr) {
    return "(" + pOr.getOperand1().accept(this) + " | " + pOr.getOperand2().accept(this) + ")";
  }

  @Override
  public String visit(BinaryXor<CompoundInterval> pXor) {
    return "(" + pXor.getOperand1().accept(this) + " ^ " + pXor.getOperand2().accept(this) + ")";
  }

  @Override
  public String visit(Constant<CompoundInterval> pConstant) {
    List<SimpleInterval> intervals = pConstant.getValue().getIntervals();
    String output = "";
    for (int i = 0; i < intervals.size(); i++) {
      output +=
          "[" + intervals.get(i).getLowerBound() + "," + intervals.get(i).getUpperBound() + "]";
      if (i < intervals.size() - 1) {
        output += ",";
      }
    }
    return output + "->" + pConstant.getTypeInfo().toString();
  }

  @Override
  public String visit(Divide<CompoundInterval> pDivide) {
    return "("
        + pDivide.getNumerator().accept(this)
        + " / "
        + pDivide.getDenominator().accept(this)
        + ")";
  }

  @Override
  public String visit(Exclusion<CompoundInterval> pExclusion) {

    return "("
        + pExclusion.getExcluded().accept(this)
        + " \\ "
        + pExclusion.getExcluded().accept(this)
        + ")";
  }

  @Override
  public String visit(Modulo<CompoundInterval> pModulo) {
    return "("
        + pModulo.getNumerator().accept(this)
        + " % "
        + pModulo.getDenominator().accept(this)
        + ")";
  }

  @Override
  public String visit(Multiply<CompoundInterval> pMultiply) {
    return "("
        + pMultiply.getFactor1().accept(this)
        + " * "
        + pMultiply.getFactor2().accept(this)
        + ")";
  }

  @Override
  public String visit(ShiftLeft<CompoundInterval> pShiftLeft) {
    return "("
        + pShiftLeft.getShifted().accept(this)
        + " << "
        + pShiftLeft.getShiftDistance().accept(this)
        + ")";
  }

  @Override
  public String visit(ShiftRight<CompoundInterval> pShiftRight) {
    return "("
        + pShiftRight.getShifted().accept(this)
        + " >> "
        + pShiftRight.getShiftDistance().accept(this)
        + ")";
  }

  @Override
  public String visit(Union<CompoundInterval> pUnion) {
    return "("
        + pUnion.getOperand1().accept(this)
        + " U "
        + pUnion.getOperand2().accept(this)
        + ")";
  }

  @Override
  public String visit(Variable<CompoundInterval> pVariable) {
    return pVariable.getMemoryLocation().getQualifiedName() + "->" + pVariable.getTypeInfo().toString();
  }

  @Override
  public String visit(IfThenElse<CompoundInterval> pIfThenElse) {
    return "("
        + pIfThenElse.getCondition()
        + " ? "
        + pIfThenElse.getPositiveCase().accept(this)
        + " : "
        + pIfThenElse.getNegativeCase().accept(this)
        + ")";
  }

  @Override
  public String visit(Cast<CompoundInterval> pCast) {
    return "(" + pCast.getCasted().accept(this) + ")";
  }
}
