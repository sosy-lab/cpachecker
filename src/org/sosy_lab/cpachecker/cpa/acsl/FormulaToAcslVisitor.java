// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslArraySubscriptTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAstNode;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPointerType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.FormulaTransformationVisitor;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;

/**
 * This visitor transforms a formula to an ACSL expression. The visitor returns the input formula
 * unchanged, and provides the corresponding ACSL expression is available via a separate method
 * {@link #getAcslExpressionForFormula(Formula)}.
 *
 * <p>Variables (like "foo::x") that contain a scope (like function-name "foo") are replaced by
 * their unscoped name (like "x").
 */
public class FormulaToAcslVisitor extends FormulaTransformationVisitor {

  private static final String FUNCTION_NAME_SEPARATOR = "::";
  private final FormulaManagerView fmgr;
  final FileLocation DUMMY_LOC = FileLocation.DUMMY;

  private final Map<Formula, AcslAstNode> cache = new HashMap<>();

  FormulaToAcslVisitor(FormulaManagerView pFmgr) {
    super(pFmgr);
    this.fmgr = pFmgr;
  }

  private AcslTerm getTerm(Formula f) {
    AcslAstNode node =
        Preconditions.checkNotNull(cache.get(f), "No ACSL expression in cache for formula %s", f);

    Preconditions.checkArgument(
        node instanceof AcslTerm,
        "Expected ACSL term for formula %s, but got %s",
        f,
        node.getClass().getSimpleName());

    return (AcslTerm) node;
  }

  private AcslPredicate getPredicate(Formula f) {
    AcslAstNode node =
        Preconditions.checkNotNull(cache.get(f), "No ACSL expression in cache for formula %s", f);

    Preconditions.checkArgument(
        node instanceof AcslPredicate,
        "Expected ACSL predicate for formula %s, but got %s",
        f,
        node.getClass().getSimpleName());

    return (AcslPredicate) node;
  }

  @Override
  public Formula visitFreeVariable(Formula f, String name) {
    List<String> parts = Splitter.on(FUNCTION_NAME_SEPARATOR).splitToList(name);
    final String variableName = (parts.size() == 2) ? parts.get(1) : name;
    AcslType acslType = getAcslType(f);

    if (fmgr.getFormulaType(f).isBooleanType()) {
      // scope von Program übergeben getScope mit dem CFA
      // TODO AcslPredicateDeclaration declaration = ...
      throw new UnsupportedOperationException(
          "TODO: construct AcslPredicateDeclaration for " + variableName);
    } else {
      AcslSimpleDeclaration declaration =
          // TODO what do I use here? Is this fine?
          new AcslVariableDeclaration(DUMMY_LOC, false, acslType, variableName, variableName, name);

      cache.put(f, new AcslIdTerm(DUMMY_LOC, declaration));
    }

    return f;
  }

  @Override
  public Formula visitConstant(Formula f, Object value) {
    AcslAstNode result;

    if (value instanceof Boolean b) {
      result = new AcslBooleanLiteralPredicate(DUMMY_LOC, b);
    } else if (value instanceof BigInteger i) {
      result = new AcslIntegerLiteralTerm(DUMMY_LOC, AcslBuiltinLogicType.INTEGER, i);
    } else if (value instanceof BigDecimal d) {
      result = new AcslRealLiteralTerm(DUMMY_LOC, AcslBuiltinLogicType.REAL, d);
    } else {
      throw new UnsupportedOperationException(
          "Unsupported constant in SMT formula: " + value + " (" + value.getClass() + ")");
    }
    cache.put(f, result);
    return f;
  }

  @Override
  public Formula visitFunction(
      Formula f, List<Formula> newArgs, FunctionDeclaration<?> functionDeclaration) {

    AcslAstNode result =
        switch (functionDeclaration.getKind()) {
          case NOT ->
              new AcslUnaryPredicate(
                  DUMMY_LOC,
                  getPredicate(newArgs.getFirst()),
                  AcslUnaryPredicate.AcslUnaryExpressionOperator.NEGATION);
          case AND ->
              new AcslBinaryPredicate(
                  DUMMY_LOC,
                  getPredicate(newArgs.get(0)),
                  getPredicate(newArgs.get(1)),
                  AcslBinaryPredicate.AcslBinaryPredicateOperator.AND);
          case OR ->
              new AcslBinaryPredicate(
                  DUMMY_LOC,
                  getPredicate(newArgs.get(0)),
                  getPredicate(newArgs.get(1)),
                  AcslBinaryPredicate.AcslBinaryPredicateOperator.OR);
          case IMPLIES ->
              new AcslBinaryPredicate(
                  DUMMY_LOC,
                  getPredicate(newArgs.get(0)),
                  getPredicate(newArgs.get(1)),
                  AcslBinaryPredicateOperator.IMPLICATION);

          case UMINUS, BV_NEG, FP_NEG ->
              new AcslUnaryTerm(
                  DUMMY_LOC,
                  getAcslType(f),
                  getTerm(newArgs.getFirst()),
                  AcslUnaryTermOperator.MINUS);

          case BV_NOT ->
              throw new UnsupportedOperationException(
                  "Bitwise complement is not currently an option in ACSL see Issue 1640");

          case BV_AND -> makeBinaryTerm(f, newArgs, AcslBinaryTermOperator.BINARY_AND);
          case BV_OR -> makeBinaryTerm(f, newArgs, AcslBinaryTermOperator.BINARY_OR);
          case BV_XOR -> makeBinaryTerm(f, newArgs, AcslBinaryTermOperator.BINARY_XOR);

          case ADD, BV_ADD, FP_ADD ->
              makeBinaryTerm(
                  f, newArgs, AcslBinaryTermOperator.PLUS, functionDeclaration.getKind());
          case SUB, BV_SUB, FP_SUB ->
              makeBinaryTerm(
                  f, newArgs, AcslBinaryTermOperator.MINUS, functionDeclaration.getKind());
          case MUL, BV_MUL, FP_MUL ->
              makeBinaryTerm(
                  f, newArgs, AcslBinaryTermOperator.MULTIPLY, functionDeclaration.getKind());
          case DIV, BV_SDIV, BV_UDIV, FP_DIV ->
              makeBinaryTerm(
                  f, newArgs, AcslBinaryTermOperator.DIVIDE, functionDeclaration.getKind());
          case MODULO, BV_SREM, BV_UREM ->
              makeBinaryTerm(f, newArgs, AcslBinaryTermOperator.MODULO);
          case BV_SHL -> makeBinaryTerm(f, newArgs, AcslBinaryTermOperator.SHIFT_LEFT);

          case EQ, BV_EQ, FP_EQ ->
              makeBinaryPredicate(
                  newArgs, AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator.EQUALS);
          case LT, BV_SLT, BV_ULT, FP_LT ->
              makeBinaryPredicate(
                  newArgs, AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator.LESS_THAN);
          case LTE, BV_SLE, BV_ULE, FP_LE ->
              makeBinaryPredicate(
                  newArgs, AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator.LESS_EQUAL);
          case GT, BV_SGT, BV_UGT, FP_GT ->
              makeBinaryPredicate(
                  newArgs, AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator.GREATER_THAN);
          case GTE, BV_SGE, BV_UGE, FP_GE ->
              makeBinaryPredicate(
                  newArgs, AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator.GREATER_EQUAL);

          case EQ_ZERO -> makeEqualsZero(newArgs.getFirst());
          case GTE_ZERO ->
              new AcslBinaryTermPredicate(
                  DUMMY_LOC,
                  getTerm(newArgs.getFirst()),
                  AcslIntegerLiteralTerm.ZERO,
                  AcslBinaryTermExpressionOperator.GREATER_EQUAL);

          case ITE ->
              new AcslTernaryTerm(
                  DUMMY_LOC,
                  getPredicate(newArgs.get(0)),
                  getTerm(newArgs.get(1)),
                  getTerm(newArgs.get(2)));

          case FP_IS_ZERO -> makeEqualsZero(newArgs.getFirst());

          case FP_IS_NAN, FP_IS_INF, FP_ROUND_TO_INTEGRAL ->
              throw new UnsupportedOperationException("Not clear how to represent this in ACSL");

          case UF -> makeUninterpretedFunction(f, newArgs, functionDeclaration);

          case SELECT ->
              new AcslArraySubscriptTerm(
                  DUMMY_LOC, getAcslType(f), getTerm(newArgs.get(0)), getTerm(newArgs.get(1)));
          // TODO maybe simplify the adress calculation in here a bit
          case STORE ->
              new AcslBinaryTermPredicate(
                  DUMMY_LOC,
                  getTerm(newArgs.get(1)),
                  getTerm(newArgs.get(2)),
                  AcslBinaryTermExpressionOperator.EQUALS);

          default ->
              throw new UnsupportedOperationException(
                  "Not clear how to represent function in ACSL. Kind: "
                      + functionDeclaration.getKind());
        };

    cache.put(f, result);
    return f;
  }

  /**
   * This method provides access to the Acsl expression (as String) of the visited formula. The
   * method can be used to query Acsl expressions for all sub-formulas.
   *
   * @return the Acsl expression of a visited formula, or NULL, if the formula (or sub-formula) was
   *     not yet visited.
   */
  public AcslAstNode getAcslExpressionForFormula(Formula f) {
    return cache.get(f);
  }

  // Helpers:
  private AcslTerm makeBinaryTerm(
      Formula f,
      List<Formula> args,
      AcslBinaryTermOperator operator,
      FunctionDeclarationKind kind) {

    // skip first argument for FP operations, it represents the rounding-mode.
    int offset = kind.name().startsWith("FP_") ? 1 : 0;

    return new AcslBinaryTerm(
        DUMMY_LOC,
        getAcslType(f),
        getTerm(args.get(offset)),
        getTerm(args.get(offset + 1)),
        operator);
  }

  private AcslTerm makeBinaryTerm(Formula f, List<Formula> args, AcslBinaryTermOperator operator) {

    return new AcslBinaryTerm(
        DUMMY_LOC, getAcslType(f), getTerm(args.get(0)), getTerm(args.get(1)), operator);
  }

  private AcslPredicate makeBinaryPredicate(
      List<Formula> args, AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator operator) {

    // Special case: for array store we see formulas like (= *int@x (store ...)) where store itself
    // creates the relevant ACSL predicates
    if (operator == AcslBinaryTermExpressionOperator.EQUALS
        && getTerm(args.get(0)) instanceof AcslIdTerm
        && getTerm(args.get(0)).toString().startsWith("*int@")) {
      return getPredicate(args.get(1));
    }

    return new AcslBinaryTermPredicate(
        DUMMY_LOC, getTerm(args.get(0)), getTerm(args.get(1)), operator);
  }

  private AcslPredicate makeEqualsZero(Formula operand) {
    return new AcslBinaryTermPredicate(
        DUMMY_LOC,
        getTerm(operand),
        AcslIntegerLiteralTerm.ZERO,
        AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator.EQUALS);
  }

  private AcslAstNode makeUninterpretedFunction(
      Formula f, List<Formula> args, FunctionDeclaration<?> declaration) {

    // Try to deal with special CPAchecker-internal UFs see FormulaToCExpressionVisitor
    return switch (declaration.getName()) {
      case "_&_" -> makeBinaryTerm(f, args, AcslBinaryTermOperator.BINARY_AND);
      case "_!!_" -> makeBinaryTerm(f, args, AcslBinaryTermOperator.BINARY_OR);
      case "_^_" -> makeBinaryTerm(f, args, AcslBinaryTermOperator.BINARY_XOR);
      case "_<<_" -> makeBinaryTerm(f, args, AcslBinaryTermOperator.SHIFT_LEFT);
      case "_>>_" -> makeBinaryTerm(f, args, AcslBinaryTermOperator.SHIFT_RIGHT);
      case "_%_" -> makeBinaryTerm(f, args, AcslBinaryTermOperator.MODULO);
      case "_~_" ->
          throw new UnsupportedOperationException(
              "Bitwise complement is not currently an option in ACSL see Issue 1640");
      default ->
          throw new UnsupportedOperationException(
              "TODO: translate UF " + declaration.getName() + " to an Acsl Function Call");
    };
  }

  private AcslType getAcslType(Formula f) {
    FormulaType<?> type = fmgr.getFormulaType(f);
    return getAcslType(type);
  }

  private AcslType getAcslType(FormulaType<?> type) {

    if (type.isBooleanType()) {
      return AcslBuiltinLogicType.BOOLEAN;
    }
    if (type.isIntegerType()) {
      return AcslBuiltinLogicType.INTEGER;
    }
    if (type.isRationalType()) {
      return AcslBuiltinLogicType.REAL;
    }
    if (type.isBitvectorType()) {
      return new AcslCType(
          new CSimpleType(
              CTypeQualifiers.NONE,
              CBasicType.INT,
              false,
              false,
              true,
              false,
              false,
              false,
              false));
    }
    if (type.isArrayType()) {
      AcslType elementType = getAcslType(((ArrayFormulaType<?, ?>) type).getElementType());
      return new AcslPointerType(elementType);
    }
    throw new UnsupportedOperationException("Cannot convert formula type to ACSL type: " + type);
  }
}
