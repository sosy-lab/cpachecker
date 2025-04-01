// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import java.math.BigInteger;
import java.util.List;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanConstant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Equal;
import org.sosy_lab.cpachecker.cpa.invariants.formula.LessThan;
import org.sosy_lab.cpachecker.cpa.invariants.formula.LogicalAnd;
import org.sosy_lab.cpachecker.cpa.invariants.formula.LogicalNot;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;

public class SMTToBooleanIntervalFormulaVisitor
    implements FormulaVisitor<BooleanFormula<CompoundInterval>> {

  private final FormulaManagerView fmgr;

  SMTToNumeralIntervalFormulaVisitor smtToNumeralFormulaVisitor;

  public SMTToBooleanIntervalFormulaVisitor(
      FormulaManagerView pFmgr, SMTToNumeralIntervalFormulaVisitor pSmtToNumeralFormulaVisitor) {
    fmgr = pFmgr;
    smtToNumeralFormulaVisitor = pSmtToNumeralFormulaVisitor;
  }

  @Override
  public BooleanFormula<CompoundInterval> visitBoundVariable(Formula pF, int pArg1) {
    return BooleanConstant.getTrue();
  }

  @Override
  public BooleanFormula<CompoundInterval> visitConstant(Formula pF, Object pValue) {
    if ((boolean) pValue) {
      return BooleanConstant.getTrue();
    } else {
      return BooleanConstant.getFalse();
    }
  }

  @Override
  public BooleanFormula<CompoundInterval> visitFreeVariable(Formula pF, String pName) {
    return BooleanConstant.getTrue();
  }

  @Override
  public BooleanFormula<CompoundInterval> visitFunction(
      Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {
    FunctionDeclarationKind kind = pFunctionDeclaration.getKind();

    switch (kind) {
      case NOT:
        return LogicalNot.of(fmgr.visit(pArgs.get(0), this));
      case EQ:
        Formula leftRaw = pArgs.get(0);
        Formula rightRaw = pArgs.get(1);

        if (leftRaw.toString().startsWith("(`bvextract_31_31_32`")
            && rightRaw.toString().equals("1_1")) {
          BooleanFormula<CompoundInterval> test = fmgr.visit(leftRaw, this);
          return test;
        }

        if (leftRaw.toString().startsWith("(`bvextract_31_31_32`")
            && rightRaw.toString().equals("0_1")) {
          BooleanFormula<CompoundInterval> test = fmgr.visit(leftRaw, this);

          return LogicalNot.of(test);
        }

        NumeralFormula<CompoundInterval> left = fmgr.visit(leftRaw, smtToNumeralFormulaVisitor);
        NumeralFormula<CompoundInterval> right = fmgr.visit(rightRaw, smtToNumeralFormulaVisitor);

        if (left == null || right == null) {
          System.err.println("⚠️ EQ: left or right is null: " + pF);
          return BooleanConstant.getFalse();
        }

        return Equal.of(left, right);
      case OR:
        return LogicalAnd.of(
            LogicalNot.of(fmgr.visit(pArgs.get(0), this)),
            LogicalNot.of(fmgr.visit(pArgs.get(1), this)));
      case BV_SGT:
      case BV_UGT:
      case FP_GT:
      case GT:
        return LogicalNot.of(
            LessThan.of(
                fmgr.visit(pArgs.get(1), smtToNumeralFormulaVisitor),
                fmgr.visit(pArgs.get(0), smtToNumeralFormulaVisitor)));
      case BV_SGE:
      case BV_UGE:
      case FP_GE:
      case GTE:
        return LogicalNot.of(
            LessThan.of(
                fmgr.visit(pArgs.get(0), smtToNumeralFormulaVisitor),
                fmgr.visit(pArgs.get(1), smtToNumeralFormulaVisitor)));
      case BV_SLT:
      case BV_ULT:
      case FP_LT:
      case LT:
        return LessThan.of(
            fmgr.visit(pArgs.get(0), smtToNumeralFormulaVisitor),
            fmgr.visit(pArgs.get(1), smtToNumeralFormulaVisitor));
      case BV_SLE:
      case BV_ULE:
      case FP_LE:
      case LTE:
        return LogicalNot.of(
            LessThan.of(
                fmgr.visit(pArgs.get(1), smtToNumeralFormulaVisitor),
                fmgr.visit(pArgs.get(0), smtToNumeralFormulaVisitor)));
      case AND:
        return LogicalAnd.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));
      case GTE_ZERO:
        return LogicalNot.of(
            LessThan.of(
                fmgr.visit(pArgs.get(0), smtToNumeralFormulaVisitor),
                smtToNumeralFormulaVisitor.visitConstant(pF, 0)));
      case EQ_ZERO:
        return Equal.of(
            fmgr.visit(pArgs.get(0), smtToNumeralFormulaVisitor),
            smtToNumeralFormulaVisitor.visitConstant(pF, 0));
      case BV_EXTRACT:
        Formula variable = pArgs.get(0);
        return LessThan.of(
            fmgr.visit(variable, smtToNumeralFormulaVisitor),
            smtToNumeralFormulaVisitor.visitConstant(variable, BigInteger.ZERO));
      default:
        throw new UnsupportedOperationException("Unsupported function: " + kind);
    }
  }

  @Override
  public BooleanFormula<CompoundInterval> visitQuantifier(
      org.sosy_lab.java_smt.api.BooleanFormula pArg0,
      Quantifier pArg1,
      List<Formula> pArg2,
      org.sosy_lab.java_smt.api.BooleanFormula pArg3) {
    return BooleanConstant.getTrue();
  }
}
