// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FormulaType.EnumerationFormulaType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;
import org.sosy_lab.java_smt.api.FormulaType.NumeralType;
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
public class FormulaToCExpressionVisitor implements FormulaVisitor<SymbolicExpression> {

  private final Function<String, String> variableNameConverter;
  private final Map<String, SymbolicIdentifier> identifiers;
  private final FormulaManagerView fmgr;

  private boolean bvSigned = false;

  public FormulaToCExpressionVisitor(
      FormulaManagerView pFmgr,
      Map<String, SymbolicIdentifier> pStateVariables,
      Function<String, String> pVariableNameConverter) {
    fmgr = pFmgr;
    variableNameConverter = pVariableNameConverter;
    identifiers = pStateVariables;
  }

  @Override
  public ConstantSymbolicExpression visitFreeVariable(Formula pF, String pName) {
    // reduce variables like 'main::x' to 'x'
    int index = pName.lastIndexOf(":");
    if (index != -1) {
      pName = pName.substring(index + 1);
    }
    String variableName = variableNameConverter.apply(pName);
    return new ConstantSymbolicExpression(identifiers.get(variableName), null);
  }

  @Override
  public ConstantSymbolicExpression visitConstant(Formula pF, Object pValue) {
    FormulaType<?> type = fmgr.getFormulaType(pF);

    if (type.isBitvectorType() && pValue instanceof BigInteger value) {
      final int size = ((BitvectorType) type).getSize();
      return new ConstantSymbolicExpression(
          new NumericValue(interpretBitvectorValue(value, size)), null);
    } else if (pValue instanceof Boolean value) {
      return new ConstantSymbolicExpression(
          value ? BooleanValue.TRUE_VALUE : BooleanValue.FALSE_VALUE, null);
    }

    return new ConstantSymbolicExpression(new NumericValue((Number) pValue), null);
  }

  /**
   * Bitvector values are reported as the unsigned interpretation of their bit pattern, i.e., as a
   * non-negative number. Whenever the surrounding operation reads its operands as signed, that bit
   * pattern denotes the corresponding two's-complement value instead, so it has to be converted.
   *
   * <p>Unlike {@link FormulaToCVisitor}, which renders C source text, this visitor builds symbolic
   * expressions that carry an explicit type. {@link #convertFormuaTypeToCType} derives that type
   * from the very same {@code bvSigned} flag, so the value is reinterpreted exactly when the type
   * it is paired with is signed, and no adjustment for C's implicit literal widening is needed.
   */
  private BigInteger interpretBitvectorValue(BigInteger pValue, int pSize) {
    if (bvSigned && pValue.signum() >= 0 && pValue.testBit(pSize - 1)) {
      return pValue.subtract(BigInteger.ONE.shiftLeft(pSize));
    }
    return pValue;
  }

  private CType convertFormuaTypeToCType(FormulaType<?> type) {
    return switch (type) {
      case FloatingPointType fpt ->
          fpt.getTotalSize() <= 32
              ? CNumericTypes.FLOAT
              : fpt.getTotalSize() <= 64 ? CNumericTypes.DOUBLE : CNumericTypes.LONG_DOUBLE;
      case BitvectorType bvt ->
          new CSimpleType(
              CTypeQualifiers.CONST,
              bySize(bvt.getSize()),
              bvt.getSize() >= 64,
              bvt.getSize() <= 16,
              bvSigned,
              !bvSigned,
              false,
              false,
              bvt.getSize() >= 128);
      case ArrayFormulaType<?, ?> aft ->
          new CArrayType(CTypeQualifiers.CONST, convertFormuaTypeToCType(aft.getElementType()));
      case EnumerationFormulaType ignore -> throw new UnsupportedOperationException();
      case NumeralType<?> ignore -> CNumericTypes.INT;
      default -> throw new IllegalStateException("Unexpected value: " + type);
    };
  }

  private CBasicType bySize(int size) {
    if (size == 8) {
      return CBasicType.CHAR;
    }
    if (size == 128) {
      return CBasicType.INT128;
    }
    return CBasicType.INT;
  }

  @Override
  public SymbolicExpression visitFunction(
      Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {
    FunctionDeclarationKind kind = pFunctionDeclaration.getKind();
    SymbolicValueFactory symbolicValueFactory = SymbolicValueFactory.getInstance();

    // despite being ugly, this way I can
    // propagate signedness of bitvectors reliably
    // through the visitor calls
    //
    // Consider a formula like:
    // bv_slt ( bv_ule x b#101010... ) ( bv_slt y b#110010001... )
    boolean signedCarryThrough = false;
    if (bvSigned) {
      signedCarryThrough = true;
    }

    ConstantSymbolicExpression zero =
        new ConstantSymbolicExpression(new NumericValue(0), CNumericTypes.INT);

    FormulaType<?> type = fmgr.getFormulaType(pF);

    bvSigned =
        switch (kind) {
          case BV_SDIV, BV_SREM, BV_SGT, BV_SGE, BV_SLT, BV_SLE -> true;
          default -> false;
        };
    CType formulaType = convertFormuaTypeToCType(type);

    try {
      return switch (kind) {
        case BV_ADD, FP_ADD, ADD -> {
          SymbolicExpression exp =
              symbolicValueFactory.add(
                  fmgr.visit(pArgs.getFirst(), this),
                  fmgr.visit(pArgs.get(1), this),
                  formulaType,
                  formulaType);
          if (pArgs.size() > 2) {
            for (int i = 2; i < pArgs.size(); i++) {
              exp =
                  symbolicValueFactory.add(
                      exp, fmgr.visit(pArgs.get(i), this), formulaType, formulaType);
            }
          }
          yield exp;
        }
        case BV_NEG, FP_NEG, UMINUS ->
            symbolicValueFactory.negate(fmgr.visit(pArgs.getFirst(), this), formulaType);
        case BV_SUB, FP_SUB, SUB ->
            symbolicValueFactory.minus(
                fmgr.visit(pArgs.getFirst(), this),
                fmgr.visit(pArgs.get(1), this),
                formulaType,
                formulaType);
        case BV_SDIV, BV_UDIV, FP_DIV, DIV ->
            symbolicValueFactory.divide(
                fmgr.visit(pArgs.getFirst(), this),
                fmgr.visit(pArgs.get(1), this),
                formulaType,
                formulaType);
        case BV_SREM, BV_UREM, MODULO ->
            symbolicValueFactory.modulo(
                fmgr.visit(pArgs.getFirst(), this),
                fmgr.visit(pArgs.get(1), this),
                formulaType,
                formulaType);
        case BV_MUL, FP_MUL, MUL -> {
          SymbolicExpression exp =
              symbolicValueFactory.multiply(
                  fmgr.visit(pArgs.getFirst(), this),
                  fmgr.visit(pArgs.get(1), this),
                  formulaType,
                  formulaType);
          if (pArgs.size() > 2) {
            for (int i = 2; i < pArgs.size(); i++) {
              exp =
                  symbolicValueFactory.multiply(
                      exp, fmgr.visit(pArgs.get(i), this), formulaType, formulaType);
            }
          }
          yield exp;
        }
        case BV_EQ, FP_EQ, IFF, EQ ->
            symbolicValueFactory.equal(
                fmgr.visit(pArgs.getFirst(), this),
                fmgr.visit(pArgs.get(1), this),
                formulaType,
                formulaType);
        case BV_SGT, BV_UGT, FP_GT, GT ->
            symbolicValueFactory.greaterThan(
                fmgr.visit(pArgs.getFirst(), this),
                fmgr.visit(pArgs.get(1), this),
                formulaType,
                formulaType);
        case BV_SGE, BV_UGE, FP_GE, GTE ->
            symbolicValueFactory.greaterThanOrEqual(
                fmgr.visit(pArgs.getFirst(), this),
                fmgr.visit(pArgs.get(1), this),
                formulaType,
                formulaType);
        case BV_SLT, BV_ULT, FP_LT, LT ->
            symbolicValueFactory.lessThan(
                fmgr.visit(pArgs.getFirst(), this),
                fmgr.visit(pArgs.get(1), this),
                formulaType,
                formulaType);
        case BV_SLE, BV_ULE, FP_LE, LTE ->
            symbolicValueFactory.lessThanOrEqual(
                fmgr.visit(pArgs.getFirst(), this),
                fmgr.visit(pArgs.get(1), this),
                formulaType,
                formulaType);
        case BV_NOT -> symbolicValueFactory.binaryNot(fmgr.visit(pArgs.getFirst(), this), null);
        case NOT -> symbolicValueFactory.negate(fmgr.visit(pArgs.getFirst(), this), null);
        case BV_XOR, XOR ->
            symbolicValueFactory.binaryXor(
                fmgr.visit(pArgs.getFirst(), this),
                fmgr.visit(pArgs.get(1), this),
                formulaType,
                formulaType);
        case BV_AND ->
            symbolicValueFactory.binaryAnd(
                fmgr.visit(pArgs.getFirst(), this),
                fmgr.visit(pArgs.get(1), this),
                formulaType,
                formulaType);
        case AND -> {
          SymbolicExpression exp =
              symbolicValueFactory.logicalAnd(
                  fmgr.visit(pArgs.getFirst(), this),
                  fmgr.visit(pArgs.get(1), this),
                  formulaType,
                  formulaType);
          if (pArgs.size() > 2) {
            for (int i = 2; i < pArgs.size(); i++) {
              exp =
                  symbolicValueFactory.logicalAnd(
                      exp, fmgr.visit(pArgs.get(i), this), formulaType, formulaType);
            }
          }
          yield exp;
        }
        case BV_OR ->
            symbolicValueFactory.binaryOr(
                fmgr.visit(pArgs.getFirst(), this),
                fmgr.visit(pArgs.get(1), this),
                formulaType,
                formulaType);
        case OR -> {
          SymbolicExpression exp =
              symbolicValueFactory.logicalOr(
                  fmgr.visit(pArgs.getFirst(), this),
                  fmgr.visit(pArgs.get(1), this),
                  formulaType,
                  formulaType);
          if (pArgs.size() > 2) {
            for (int i = 2; i < pArgs.size(); i++) {
              exp =
                  symbolicValueFactory.logicalOr(
                      exp, fmgr.visit(pArgs.get(i), this), formulaType, formulaType);
            }
          }
          yield exp;
        }
        case GTE_ZERO ->
            symbolicValueFactory.greaterThanOrEqual(
                zero, fmgr.visit(pArgs.getFirst(), this), formulaType, formulaType);
        case EQ_ZERO ->
            symbolicValueFactory.equal(
                zero, fmgr.visit(pArgs.getFirst(), this), formulaType, formulaType);
        case ITE -> {
          SymbolicExpression first = fmgr.visit(pArgs.getFirst(), this);
          SymbolicExpression second = fmgr.visit(pArgs.get(2), this);
          SymbolicExpression third = fmgr.visit(pArgs.get(3), this);
          // first != 0 && second || first == 0 && third
          yield symbolicValueFactory.binaryOr(
              symbolicValueFactory.binaryAnd(
                  symbolicValueFactory.notEqual(first, zero, null, null), second, null, null),
              symbolicValueFactory.binaryAnd(
                  symbolicValueFactory.equal(first, zero, null, null), third, null, null),
              null,
              null);
        }
        case BV_SHL ->
            symbolicValueFactory.shiftLeft(
                fmgr.visit(pArgs.getFirst(), this),
                fmgr.visit(pArgs.get(1), this),
                formulaType,
                formulaType);
        case BV_LSHR, BV_ASHR -> {
          if (bvSigned) {
            yield symbolicValueFactory.shiftRightSigned(
                fmgr.visit(pArgs.getFirst(), this),
                fmgr.visit(pArgs.get(1), this),
                formulaType,
                formulaType);
          }
          yield symbolicValueFactory.shiftRightUnsigned(
              fmgr.visit(pArgs.getFirst(), this),
              fmgr.visit(pArgs.get(1), this),
              formulaType,
              formulaType);
        }
        default -> throw new UnsupportedOperationException();
      };
    } finally {

      // (re-)set bvSigned appropriately for the current state
      // of the translation
      bvSigned = signedCarryThrough;
    }
  }

  @Override
  public SymbolicExpression visitQuantifier(
      BooleanFormula pF,
      Quantifier pQuantifier,
      List<Formula> pBoundVariables,
      BooleanFormula pBody) {
    // No-OP; not relevant for the given use-cases
    throw new UnsupportedOperationException("Cannot have quantifiers in constraints.");
  }
}
