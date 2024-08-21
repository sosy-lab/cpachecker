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
    return "(("
        + pAdd.getSummand1().accept(this)
        + ") +.add ("
        + pAdd.getSummand2().accept(this)
        + "))";
  }

  @Override
  public String visit(BinaryAnd<CompoundInterval> pAnd) {
    return "(("
        + pAnd.getOperand1().accept(this)
        + ") &.binaryAnd ("
        + pAnd.getOperand2().accept(this)
        + "))";
  }

  @Override
  public String visit(BinaryNot<CompoundInterval> pNot) {
    return "(~.binaryNot(" + pNot.getFlipped().accept(this) + "))";
  }

  @Override
  public String visit(BinaryOr<CompoundInterval> pOr) {
    return "(("
        + pOr.getOperand1().accept(this)
        + ") |.binaryOr ("
        + pOr.getOperand2().accept(this)
        + "))";
  }

  @Override
  public String visit(BinaryXor<CompoundInterval> pXor) {
    return "(("
        + pXor.getOperand1().accept(this)
        + ") ^.binaryXor ("
        + pXor.getOperand2().accept(this)
        + "))";
  }

  @Override
  public String visit(Constant<CompoundInterval> pConstant) {
    List<SimpleInterval> intervals = pConstant.getValue().getIntervals();
    StringBuilder output = new StringBuilder();
    for (int i = 0; i < intervals.size(); i++) {
      SimpleInterval interval = intervals.get(i);
      String lowerBoundStr =
          interval.hasLowerBound() ? interval.getLowerBound().toString() : "-inf";
      String upperBoundStr = interval.hasUpperBound() ? interval.getUpperBound().toString() : "inf";

      output.append("[").append(lowerBoundStr).append(",").append(upperBoundStr).append("]");

      if (i < intervals.size() - 1) {
        output.append(",");
      }
    }
    return output.toString() + "-typeInfo>" + pConstant.getTypeInfo();
  }

  @Override
  public String visit(Divide<CompoundInterval> pDivide) {
    return "(("
        + pDivide.getNumerator().accept(this)
        + ") /.divide ("
        + pDivide.getDenominator().accept(this)
        + "))";
  }

  @Override
  public String visit(Exclusion<CompoundInterval> pExclusion) {

    return "(" + " \\.exclusion (" + pExclusion.getExcluded().accept(this) + "))";
  }

  @Override
  public String visit(Modulo<CompoundInterval> pModulo) {
    return "(("
        + pModulo.getNumerator().accept(this)
        + ") %.modulo ("
        + pModulo.getDenominator().accept(this)
        + "))";
  }

  @Override
  public String visit(Multiply<CompoundInterval> pMultiply) {
    return "(("
        + pMultiply.getFactor1().accept(this)
        + ") *.multiply ("
        + pMultiply.getFactor2().accept(this)
        + "))";
  }

  @Override
  public String visit(ShiftLeft<CompoundInterval> pShiftLeft) {
    return "(("
        + pShiftLeft.getShifted().accept(this)
        + ") <<.shiftLeft ("
        + pShiftLeft.getShiftDistance().accept(this)
        + "))";
  }

  @Override
  public String visit(ShiftRight<CompoundInterval> pShiftRight) {
    return "(("
        + pShiftRight.getShifted().accept(this)
        + ") >>.shiftRight ("
        + pShiftRight.getShiftDistance().accept(this)
        + "))";
  }

  @Override
  public String visit(Union<CompoundInterval> pUnion) {
    return "(("
        + pUnion.getOperand1().accept(this)
        + ") U.union ("
        + pUnion.getOperand2().accept(this)
        + "))";
  }

  @Override
  public String visit(Variable<CompoundInterval> pVariable) {
    return "("
        + pVariable.getMemoryLocation().getQualifiedName()
        + "-typeInfo>"
        + pVariable.getTypeInfo()
        + ")";
  }

  @Override
  public String visit(IfThenElse<CompoundInterval> pIfThenElse) {
    return "(("
        + pIfThenElse.getCondition().accept(new SerializeBooleanFormulaVisitor(this))
        + ") ?.if ("
        + pIfThenElse.getPositiveCase().accept(this)
        + ") :.else ("
        + pIfThenElse.getNegativeCase().accept(this)
        + "))";
  }

  @Override
  public String visit(Cast<CompoundInterval> pCast) {
    return "(cast -typeInfo>" + pCast.getTypeInfo() + "(" + pCast.getCasted().accept(this) + "))";
  }
}
