// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.FormulaTransformationVisitor;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;

/**
 * This visitor transforms a formula to a plain C expression (in String representation). The visitor
 * returns the input formula unchanged, and provides the corresponding C expression is available via
 * a separate method {@link #getCExpressionForFormula(Formula)}.
 *
 * <p>Warning: The length of the computed string representation can be exponential in formula size.
 *
 * <p>The string-representation depends on the solver-specific representation and internal ordering
 * of the formula. Variables (like "foo::x") that contain a scope (like function-name "foo") are
 * replaced by their unscoped name (like "x"). Constants (numerals) are transformed into their
 * string-representation.
 */
public class FormulaToCExpressionVisitor extends FormulaTransformationVisitor {

  // TODO: do not hardcode the function separator.
  private static final String FUNCTION_NAME_SEPARATOR = "::";

  private static final ImmutableSet<FunctionDeclarationKind> COMMUTATIVE_OPERATIONS =
      ImmutableSet.of(
          FunctionDeclarationKind.EQ, FunctionDeclarationKind.BV_EQ, FunctionDeclarationKind.FP_EQ);

  private final Map<Formula, String> cache = new HashMap<>();

  FormulaToCExpressionVisitor(FormulaManagerView fmgr) {
    super(fmgr);
  }

  @Override
  public Formula visitFreeVariable(Formula f, String name) {
    List<String> parts = Splitter.on(FUNCTION_NAME_SEPARATOR).splitToList(name);
    final String variableName = (parts.size() == 2) ? parts.get(1) : name;
    cache.put(f, variableName);
    return f;
  }

  @Override
  public Formula visitConstant(Formula f, Object value) {
    cache.put(f, value.toString());
    return f;
  }

  @Override
  public Formula visitFunction(
      Formula f, List<Formula> newArgs, FunctionDeclaration<?> functionDeclaration) {
    // rule for visitation:
    // - every argument already has all necessary brackets, except variables (-> plain string).
    // - the result has surrounding brackets.
    final String result =
        switch (functionDeclaration.getKind()) {
          case NOT, UMINUS, BV_NEG, FP_NEG, BV_NOT ->
              operatorFromFunctionDeclaration(functionDeclaration, f)
                  + cache.get(newArgs.getFirst());
          case EQ_ZERO, GTE_ZERO ->
              cache.get(newArgs.getFirst())
                  + operatorFromFunctionDeclaration(functionDeclaration, f);
          case FP_ROUND_EVEN,
              FP_ROUND_AWAY,
              FP_ROUND_POSITIVE,
              FP_ROUND_NEGATIVE,
              FP_ROUND_ZERO,
              FP_ROUND_TO_INTEGRAL ->
              // Ignore because otherwise rounding mode is treated like an additional operand
              // TODO These cases do not insert anything into the cache and might result in an
              // invalid or incomplete result. We should better abort here than continue with an
              // invalid result.
              null;
          case FP_ADD, FP_SUB, FP_DIV, FP_MUL ->
              from(newArgs)
                  .skip(1) // skip first argument, it represents the rounding-mode.
                  .transform(arg -> Preconditions.checkNotNull(cache.get(arg)))
                  .join(Joiner.on(operatorFromFunctionDeclaration(functionDeclaration, f)));
          case ITE ->
              String.format(
                  "%s ? %s : %s",
                  cache.get(newArgs.getFirst()),
                  cache.get(newArgs.get(1)),
                  cache.get(newArgs.get(2)));
          case FP_IS_ZERO
              // +0.0 and -0.0 are equal and are both handled here.
              ->
              cache.get(newArgs.getFirst()) + " == 0.0";
          case FP_IS_NAN -> {
            // NaN is not a number, and it is unequal to itself in C99.
            // see https://sourceware.org/glibc/manual/2.41/html_node/Infinity-and-NaN.html
            String nanArg = cache.get(newArgs.getFirst());
            yield nanArg + " != " + nanArg;
          }
          case FP_IS_INF -> {
            // C99 standard for positive infinity is "1 / 0",
            // see https://sourceware.org/glibc/manual/2.41/html_node/Infinity-and-NaN.html
            String infArg = cache.get(newArgs.getFirst());
            yield infArg + " == (1 / 0) || " + infArg + " == -(1 / 0)";
          }
          default -> {
            if (functionDeclaration.getName().equals("`bvextract_31_31_32`")) {
              // TODO The naming of this SMT function is specific to one solver
              // and the handling in this manner is likely not correct in all cases (unsigned vars)
              // and it is specific to one particular bitwidth, all of which it should not be.
              yield cache.get(newArgs.getFirst()) + " < 0";
            }

            List<String> expressions = new ArrayList<>(newArgs.size());
            for (Formula arg : newArgs) {
              // TODO If the arg is not in the cache, we will get an invalid or incomplete result.
              //      We should better abort here than continue with an invalid result.
              if (cache.containsKey(arg)) {
                expressions.add(Preconditions.checkNotNull(cache.get(arg)));
              }
            }
            if (COMMUTATIVE_OPERATIONS.contains(functionDeclaration.getKind())) {
              // Some solvers (e.g., MathSAT) switch commutative arguments for operations like EQ
              // randomly. Let's force some determinism over distinct solvers, by alphabetical
              // order.
              Collections.sort(expressions);
            }
            yield String.join(operatorFromFunctionDeclaration(functionDeclaration, f), expressions);
          }
        };
    if (result != null) {
      cache.put(f, "(" + result + ")");
    }
    return f;
  }

  private String operatorFromFunctionDeclaration(FunctionDeclaration<?> pDeclaration, Formula f) {
    return switch (pDeclaration.getKind()) {
      case NOT -> "!";
      case UMINUS, BV_NEG, FP_NEG -> "-";
      case AND -> " && ";
      case BV_AND -> " & ";
      case OR -> " || ";
      case BV_OR -> " | ";
      case BV_XOR -> " ^ ";
      case SUB, BV_SUB, FP_SUB -> " - ";
      case ADD, BV_ADD, FP_ADD -> " + ";
      case DIV, BV_SDIV, BV_UDIV, FP_DIV -> " / ";
      case MUL, BV_MUL, FP_MUL -> " * ";
      case MODULO, BV_SREM, BV_UREM -> " % ";
      case LT, BV_SLT, BV_ULT, FP_LT -> " < ";
      case LTE, BV_SLE, BV_ULE, FP_LE -> " <= ";
      case GT, BV_SGT, BV_UGT, FP_GT -> " > ";
      case GTE, BV_SGE, BV_UGE, FP_GE -> " >= ";
      case EQ, BV_EQ, FP_EQ -> " == ";
      case EQ_ZERO -> " == 0";
      case GTE_ZERO -> " >= 0";
      case BV_NOT -> "~";
      case BV_SHL -> " << ";

      case UF -> {
        // There are several CPAchecker-internal UFs that replace unsupported function in solvers.
        // See FormulaManagerView and ReplaceBitvectorWithNumeralAndFunctionTheory for details.
        yield switch (pDeclaration.getName()) {
          case "_&_" -> " & ";
          case "_!!_" -> " | ";
          case "_^_" -> " ^ ";
          case "_~_" -> "~";
          case "_<<_" -> " << ";
          case "_>>_" -> " >> "; // TODO arithmetic or logical shift?
          case "_%_" -> " % ";
          default ->
              throw new UnsupportedOperationException(
                  String.format(
                      "Unexpected operand %s (%s) in formula '%s'",
                      pDeclaration.getKind(), pDeclaration.getName(), f));
        };
      }

      default ->
          throw new UnsupportedOperationException(
              String.format(
                  "Unexpected operand %s (%s) in formula '%s'",
                  pDeclaration.getKind(), pDeclaration.getName(), f));
    };
  }

  /**
   * This method provides access to the C expression (as String) of the visited formula. The method
   * can be used to query C expressions for all sub-formulas.
   *
   * @return the C expression of a visited formula, or NULL, if the formula (or sub-formula) was not
   *     yet visited.
   */
  public String getCExpressionForFormula(Formula f) {
    return cache.get(f);
  }
}
