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
package org.sosy_lab.cpachecker.util.predicates.smt;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 * <p>If visit returns <code>Boolean.FALSE</code> the computed C code is likely to be invalid and
 * therefore it is discouraged to use it.
 */
public class FormulaToCVisitor implements FormulaVisitor<Boolean> {

  private static final String LLONG_MIN_LITERAL = "9223372036854775808";

  private static final String INT_MIN_LITERAL = "2147483648";

  private final StringBuilder builder = new StringBuilder();

  private final FormulaManagerView fmgr;

  private boolean bvSigned = false;

  private static final Set<FunctionDeclarationKind> UNARY_OPS = new HashSet<>();

  static {
    UNARY_OPS.add(FunctionDeclarationKind.UMINUS);
    UNARY_OPS.add(FunctionDeclarationKind.NOT);
    UNARY_OPS.add(FunctionDeclarationKind.GTE_ZERO);
    UNARY_OPS.add(FunctionDeclarationKind.EQ_ZERO);
    UNARY_OPS.add(FunctionDeclarationKind.FP_NEG);
    UNARY_OPS.add(FunctionDeclarationKind.BV_NOT);
    UNARY_OPS.add(FunctionDeclarationKind.BV_NEG);
  }

  public FormulaToCVisitor(FormulaManagerView fmgr) {
    this.fmgr = fmgr;
  }

  @Override
  public Boolean visitFreeVariable(Formula pF, String pName) {
    // reduce variables like 'main::x' to 'x'
    int index = pName.lastIndexOf(":");
    if (index != -1) {
      pName = pName.substring(index + 1);
    }
    builder.append(pName);
    return Boolean.TRUE;
  }

  @Override
  public Boolean visitBoundVariable(Formula pF, int pDeBruijnIdx) {
    // No-OP; not relevant for the given use-cases
    return Boolean.TRUE;
  }

  @Override
  public Boolean visitConstant(Formula pF, Object pValue) {
    FormulaType<?> type = fmgr.getFormulaType(pF);
    final String value = pValue.toString();

    if (type.isBitvectorType()) {
      final int size = ((FormulaType.BitvectorType) type).getSize();
      switch (size) {
        case 32:
          if (appendOverflowGuardForNegativeIntegralLiterals(INT_MIN_LITERAL, pValue)) {
            return Boolean.TRUE;
          }
          // $FALL-THROUGH$
        case 64:
          if (appendOverflowGuardForNegativeIntegralLiterals(LLONG_MIN_LITERAL, pValue)) {
            return Boolean.TRUE;
          }
          // $FALL-THROUGH$
        default:
          builder.append(value);
      }
    } else {
      builder.append(value);
    }

    return Boolean.TRUE;
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
    if (pValue instanceof BigInteger) {
      String valueString = pValue.toString();
      if (valueString.equals("-" + pGuardString)) {
        builder.append("( ( ").append(((BigInteger) pValue).add(BigInteger.ONE)).append(" ) - 1 )");
        return true;
      }
      if (bvSigned && valueString.equals(pGuardString)) {
        builder
            .append("( ( -")
            .append(((BigInteger) pValue).subtract(BigInteger.ONE))
            .append(" ) - 1 )");
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitFunction(
      Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {
    String op = null;
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

    switch (pFunctionDeclaration.getKind()) {
      case BV_ADD:
      case FP_ADD:
      case ADD:
        op = "+";
        break;
      case BV_SUB:
      case BV_NEG:
      case FP_SUB:
      case FP_NEG:
      case UMINUS:
      case SUB:
        op = "-";
        break;
      case BV_SDIV:
        bvSigned = true;
        // $FALL-THROUGH$
      case BV_UDIV:
      case FP_DIV:
      case DIV:
        op = "/";
        break;
      case BV_SREM:
        bvSigned = true;
        // $FALL-THROUGH$
      case BV_UREM:
      case MODULO:
        op = "%";
        break;
      case BV_MUL:
      case FP_MUL:
      case MUL:
        op = "*";
        break;
      case BV_EQ:
      case FP_EQ:
      case IFF:
      case EQ:
        op = "==";
        break;
      case BV_SGT:
        bvSigned = true;
        // $FALL-THROUGH$
      case BV_UGT:
      case FP_GT:
      case GT:
        op = ">";
        break;
      case BV_SGE:
        bvSigned = true;
        // $FALL-THROUGH$
      case BV_UGE:
      case FP_GE:
      case GTE:
        op = ">=";
        break;
      case BV_SLT:
        bvSigned = true;
        // $FALL-THROUGH$
      case BV_ULT:
      case FP_LT:
      case LT:
        op = "<";
        break;
      case BV_SLE:
        bvSigned = true;
        // $FALL-THROUGH$
      case BV_ULE:
      case FP_LE:
      case LTE:
        op = "<=";
        break;
      case BV_NOT:
        op = "~";
        break;
      case NOT:
        op = "!";
        break;
      case BV_XOR:
      case XOR:
        op = "^";
        break;
      case BV_AND:
        op = "&";
        break;
      case AND:
        op = "&&";
        break;
      case BV_OR:
        op = "|";
        break;
      case OR:
        op = "||";
        break;
      case GTE_ZERO:
        op = "0 <=";
        break;
      case EQ_ZERO:
        op = "0 ==";
        break;
      case ITE:
        // Special-case that is to be handled separately
        // below
        break;
      case BV_SHL:
        op = "<<";
        break;
      case BV_LSHR:
      case BV_ASHR:
        op = ">>";
        break;
      default:
        return Boolean.FALSE;
    }
    builder.append("( ");
    if (pArgs.size() == 3 && pFunctionDeclaration.getKind() == FunctionDeclarationKind.ITE) {
      if (!fmgr.visit(pArgs.get(0), this)) {
        return Boolean.FALSE;
      }
      builder.append(" ? ");
      if (!fmgr.visit(pArgs.get(1), this)) {
        return Boolean.FALSE;
      }
      builder.append(" : ");
      if (!fmgr.visit(pArgs.get(2), this)) {
        return Boolean.FALSE;
      }
    } else if (pArgs.size() == 2) {
      if (!fmgr.visit(pArgs.get(0), this)) {
        return Boolean.FALSE;
      }
      builder.append(" ").append(op).append(" ");
      if (!fmgr.visit(pArgs.get(1), this)) {
        return Boolean.FALSE;
      }
    } else if (pArgs.size() == 1 && UNARY_OPS.contains(kind)) {
      builder.append(op).append(" ");
      if (!fmgr.visit(pArgs.get(0), this)) {
        return Boolean.FALSE;
      }
    } else {
      throw new AssertionError("Function call without arguments " + pFunctionDeclaration.getName());
    }
    builder.append(" )");

    // (re-)set bvSigned appropriately for the current state
    // of the translation
    bvSigned = signedCarryThrough;

    return Boolean.TRUE;
  }

  @Override
  public Boolean visitQuantifier(
      BooleanFormula pF,
      Quantifier pQuantifier,
      List<Formula> pBoundVariables,
      BooleanFormula pBody) {
    // No-OP; not relevant for the given use-cases
    return Boolean.TRUE;
  }

  public String getString() {
    String result = builder.toString();
    builder.setLength(0);
    return result;
  }
}
