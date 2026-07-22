// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Function;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;

/**
 * This visitor is used to translate predicate based invariants from SMT formulae to expressions
 * which are evaluable in C.
 *
 * <p>If visit returns <code>false</code> the computed C code is likely to be invalid, and therefore
 * it is discouraged to use it.
 *
 * <p>Warning: Usage of this class can be exponentially expensive, because formulas are unfolded
 * into C code. For formulas with several shared subtrees this leads to bad performance.
 */
public class FormulaToCVisitor implements FormulaVisitor<Boolean> {

  private static final String LLONG_MIN_LITERAL = "9223372036854775808";

  private static final String INT_MIN_LITERAL = "2147483648";

  private final StringBuilder builder = new StringBuilder();

  private final FormulaManagerView fmgr;

  private boolean bvSigned = false;

  private Function<String, String> variableNameConverter;

  private static final ImmutableSet<FunctionDeclarationKind> UNARY_OPS =
      Sets.immutableEnumSet(
          FunctionDeclarationKind.UMINUS,
          FunctionDeclarationKind.NOT,
          FunctionDeclarationKind.GTE_ZERO,
          FunctionDeclarationKind.EQ_ZERO,
          FunctionDeclarationKind.FP_NEG,
          FunctionDeclarationKind.BV_NOT,
          FunctionDeclarationKind.BV_NEG);

  private static final ImmutableSet<FunctionDeclarationKind> N_ARY_OPS =
      Sets.immutableEnumSet(
          FunctionDeclarationKind.AND,
          FunctionDeclarationKind.OR,
          FunctionDeclarationKind.ADD,
          FunctionDeclarationKind.BV_ADD,
          FunctionDeclarationKind.FP_ADD,
          FunctionDeclarationKind.MUL,
          FunctionDeclarationKind.BV_MUL,
          FunctionDeclarationKind.FP_MUL);

  public FormulaToCVisitor(
      FormulaManagerView fmgr, Function<String, String> pVariableNameConverter) {
    this.fmgr = fmgr;
    variableNameConverter = pVariableNameConverter;
  }

  @Override
  public Boolean visitFreeVariable(Formula pF, String pName) {
    // reduce variables like 'main::x' to 'x'
    int index = pName.lastIndexOf(":");
    if (index != -1) {
      pName = pName.substring(index + 1);
    }
    builder.append(variableNameConverter.apply(pName));
    return true;
  }

  @Override
  public Boolean visitConstant(Formula pF, Object pValue) {
    FormulaType<?> type = fmgr.getFormulaType(pF);
    final String value = pValue.toString();

    if (type.isBitvectorType()) {
      final int size = ((FormulaType.BitvectorType) type).getSize();
      switch (size) {
        case 32 -> {
          if (appendOverflowGuardForNegativeIntegralLiterals(INT_MIN_LITERAL, pValue)) {
            return true;
          }
          builder.append(value);
        }
        case 64 -> {
          if (appendOverflowGuardForNegativeIntegralLiterals(LLONG_MIN_LITERAL, pValue)) {
            return true;
          }
          builder.append(value);
        }
        default -> builder.append(value);
      }
    } else if (pValue instanceof Boolean) {
      builder.append(((boolean) pValue) ? "1" : "0");
    } else {
      builder.append(value);
    }

    return true;
  }

  /**
   * The literals used for INT_MIN or LONG_MIN exceed the positive values of their corresponding
   * data types and therefore an overflow would occur, if just written as '-[LITERAL]', since in C a
   * literal is assigned its corresponding type before the unary '-' is applied.
   *
   * @param pGuardString the representation of a number that would be expected to overflow
   * @param pValue the value of the observed expression
   * @return whether a guard was necessary or not
   */
  private boolean appendOverflowGuardForNegativeIntegralLiterals(
      String pGuardString, Object pValue) {
    if (pValue instanceof BigInteger bigInteger) {
      String valueString = pValue.toString();
      if (valueString.equals("-" + pGuardString)) {
        builder.append("( ( ").append(bigInteger.add(BigInteger.ONE)).append(" ) - 1 )");
        return true;
      }
      if (bvSigned && valueString.equals(pGuardString)) {
        builder.append("( ( -").append(bigInteger.subtract(BigInteger.ONE)).append(" ) - 1 )");
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitFunction(
      Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {
    String op;
    FunctionDeclarationKind kind = pFunctionDeclaration.getKind();

    // despite being ugly, this way I can
    // propagate signedness of bitvectors reliably
    // through the visitor calls
    //
    // Consider a formula like:
    // bv_slt ( bv_ule x b#101010... ) ( bv_slt y b#110010001... )
    boolean signedCarryThrough = false;
    if (bvSigned) {
      signedCarryThrough = true;
      bvSigned = false;
    }

    switch (kind) {
      case BV_ADD, FP_ADD, ADD -> op = "+";
      case BV_SUB, BV_NEG, FP_SUB, FP_NEG, UMINUS, SUB -> op = "-";
      case BV_SDIV, BV_UDIV, FP_DIV, DIV -> op = "/";
      case BV_SREM, BV_UREM, MODULO -> op = "%";
      case BV_MUL, FP_MUL, MUL -> op = "*";
      case BV_EQ, FP_EQ, IFF, EQ -> op = "==";
      case BV_SGT, BV_UGT, FP_GT, GT -> op = ">";
      case BV_SGE, BV_UGE, FP_GE, GTE -> op = ">=";
      case BV_SLT, BV_ULT, FP_LT, LT -> op = "<";
      case BV_SLE, BV_ULE, FP_LE, LTE -> op = "<=";
      case BV_NOT -> op = "~";
      case NOT -> op = "!";
      case BV_XOR, XOR -> op = "^";
      case BV_AND -> op = "&";
      case AND -> op = "&&";
      case BV_OR -> op = "|";
      case OR -> op = "||";
      case GTE_ZERO -> op = "0 <=";
      case EQ_ZERO -> op = "0 ==";
      case ITE ->
          // Special-case that is to be handled separately below
          op = null;
      case BV_SHL -> op = "<<";
      case BV_LSHR, BV_ASHR -> op = ">>";
      default -> {
        return false;
      }
    }
    bvSigned =
        switch (kind) {
          case BV_SDIV, BV_SREM, BV_SGT, BV_SGE, BV_SLT, BV_SLE -> true;
          default -> false;
        };

    builder.append("( ");
    if (pArgs.size() == 3 && pFunctionDeclaration.getKind() == FunctionDeclarationKind.ITE) {
      if (!fmgr.visit(pArgs.getFirst(), this)) {
        return false;
      }
      builder.append(" ? ");
      if (!fmgr.visit(pArgs.get(1), this)) {
        return false;
      }
      builder.append(" : ");
      if (!fmgr.visit(pArgs.get(2), this)) {
        return false;
      }
    } else if (pArgs.size() == 1 && UNARY_OPS.contains(kind)) {
      builder.append(op).append(" ");
      if (!fmgr.visit(pArgs.getFirst(), this)) {
        return false;
      }
    } else if (N_ARY_OPS.contains(kind)) {
      for (int i = 0; i < pArgs.size(); i++) {
        if (!fmgr.visit(pArgs.get(i), this)) {
          return false;
        }
        if (i != pArgs.size() - 1) {
          builder.append(" ").append(op).append(" ");
        }
      }
    } else if (pArgs.size() == 2) {
      if (!fmgr.visit(pArgs.getFirst(), this)) {
        return false;
      }
      builder.append(" ").append(op).append(" ");
      if (!fmgr.visit(pArgs.get(1), this)) {
        return false;
      }
    } else {
      throw new AssertionError(
          String.format(
              "Function call '%s' with unexpected number of arguments: %s",
              pFunctionDeclaration.getName(), pArgs));
    }
    builder.append(" )");

    // (re-)set bvSigned appropriately for the current state
    // of the translation
    bvSigned = signedCarryThrough;

    return true;
  }

  @Override
  public Boolean visitQuantifier(
      BooleanFormula pF,
      Quantifier pQuantifier,
      List<Formula> pBoundVariables,
      BooleanFormula pBody) {
    // No-OP; not relevant for the given use-cases
    return true;
  }

  public String getString() {
    String result = builder.toString();
    builder.setLength(0);
    return result;
  }
}
