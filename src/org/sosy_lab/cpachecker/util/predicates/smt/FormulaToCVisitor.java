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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
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

  private final StringBuilder builder = new StringBuilder();

  private final FormulaManagerView fmgr;

  private final int intWidthInBits;

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

  /** Operations that read their operands as signed numbers. */
  private static final ImmutableSet<FunctionDeclarationKind> SIGNED_OPS =
      Sets.immutableEnumSet(
          FunctionDeclarationKind.BV_SDIV,
          FunctionDeclarationKind.BV_SREM,
          FunctionDeclarationKind.BV_SGT,
          FunctionDeclarationKind.BV_SGE,
          FunctionDeclarationKind.BV_SLT,
          FunctionDeclarationKind.BV_SLE,
          FunctionDeclarationKind.BV_ASHR);

  /** Operations that read their operands as unsigned numbers. All of them are binary. */
  private static final ImmutableSet<FunctionDeclarationKind> UNSIGNED_OPS =
      Sets.immutableEnumSet(
          FunctionDeclarationKind.BV_UDIV,
          FunctionDeclarationKind.BV_UREM,
          FunctionDeclarationKind.BV_UGT,
          FunctionDeclarationKind.BV_UGE,
          FunctionDeclarationKind.BV_ULT,
          FunctionDeclarationKind.BV_ULE,
          FunctionDeclarationKind.BV_LSHR);

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
      FormulaManagerView fmgr,
      Function<String, String> pVariableNameConverter,
      MachineModel pMachineModel) {
    this.fmgr = fmgr;
    variableNameConverter = pVariableNameConverter;
    intWidthInBits = pMachineModel.getSizeofInBits(CNumericTypes.INT);
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

    if (type.isBitvectorType() && pValue instanceof BigInteger value) {
      final int size = ((FormulaType.BitvectorType) type).getSize();
      appendLiteral(interpretBitvectorValue(value, size), size);
    } else if (pValue instanceof Boolean value) {
      builder.append(value ? "1" : "0");
    } else {
      builder.append(pValue);
    }

    return true;
  }

  /**
   * Bitvector values are reported as the unsigned interpretation of their bit pattern, i.e., as a
   * non-negative number. Writing such a number verbatim changes its meaning in C, because a literal
   * that does not fit into the signed type of the operands is given a wider type, and the
   * surrounding operation is then evaluated in that wider type. For example, the invariant {@code b
   * >= 0xF0000001} of an {@code int} variable must be written as {@code b >= -268435455}, whereas
   * {@code b >= 4026531841} is unsatisfiable.
   *
   * <p>For bit-widths below the width of {@code int} the operands are promoted to {@code int}
   * anyway, so there the bit pattern is only signed if the enclosing operation reads it as signed.
   */
  private BigInteger interpretBitvectorValue(BigInteger pValue, int pSize) {
    boolean signBitSet = pValue.signum() >= 0 && pValue.testBit(pSize - 1);
    if (signBitSet && (bvSigned || pSize >= intWidthInBits)) {
      return pValue.subtract(BigInteger.ONE.shiftLeft(pSize));
    }
    return pValue;
  }

  /**
   * The magnitude of the smallest value of a signed type exceeds the positive values of that type,
   * so writing it as '-[LITERAL]' would widen the expression, since in C a literal is assigned its
   * corresponding type before the unary '-' is applied. This is only relevant for types that are at
   * least as wide as {@code int}, because narrower operands are promoted to {@code int} anyway.
   */
  private void appendLiteral(BigInteger pValue, int pSize) {
    if (pValue.negate().equals(BigInteger.ONE.shiftLeft(pSize - 1)) && pSize >= intWidthInBits) {
      builder.append("( ( ").append(pValue.add(BigInteger.ONE)).append(" ) - 1 )");
    } else {
      builder.append(pValue);
    }
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
    final boolean signedCarryThrough = bvSigned;

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
    // all other operations do not interpret the sign bit themselves,
    // so their operands keep the signedness of the surrounding context
    bvSigned = SIGNED_OPS.contains(kind) || (signedCarryThrough && !UNSIGNED_OPS.contains(kind));

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
