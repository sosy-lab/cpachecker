// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
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
    String result = null;
    // rule for visitation:
    // - every argument already has all necessary brackets, except variables (-> plain string).
    // - the result has surrounding brackets.
    switch (functionDeclaration.getKind()) {
      case NOT:
      case UMINUS:
      case BV_NEG:
      case FP_NEG:
      case BV_NOT:
        result = operatorFromFunctionDeclaration(functionDeclaration) + cache.get(newArgs.get(0));
        break;
      case EQ_ZERO:
      case GTE_ZERO:
        result = cache.get(newArgs.get(0)) + operatorFromFunctionDeclaration(functionDeclaration);
        break;
      case FP_ROUND_EVEN:
      case FP_ROUND_AWAY:
      case FP_ROUND_POSITIVE:
      case FP_ROUND_NEGATIVE:
      case FP_ROUND_ZERO:
      case FP_ROUND_TO_INTEGRAL:
        // Ignore because otherwise rounding mode is treated like an additional operand
        // TODO These cases do not insert anything into the cache and might result in an invalid or
        //      incomplete result. We should better abort here than continue with an invalid result.
        break;
      case FP_ADD:
      case FP_SUB:
      case FP_DIV:
      case FP_MUL:
        { // skip first argument, it represents the rounding-mode.
          List<String> expressions =
              FluentIterable.from(newArgs)
                  .skip(1)
                  .transform(arg -> Preconditions.checkNotNull(cache.get(arg)))
                  .toList();
          result = String.join(operatorFromFunctionDeclaration(functionDeclaration), expressions);
          break;
        }
      case ITE:
        result =
            String.format(
                "%s ? %s : %s",
                cache.get(newArgs.get(0)), cache.get(newArgs.get(1)), cache.get(newArgs.get(2)));
        break;
      default:
        {
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
            // randomly. Let's force some determinism over distinct solvers, by alphabetical order.
            Collections.sort(expressions);
          }
          result = String.join(operatorFromFunctionDeclaration(functionDeclaration), expressions);
        }
    }
    if (result != null) {
      cache.put(f, "(" + result + ")");
    }
    return f;
  }

  private String operatorFromFunctionDeclaration(FunctionDeclaration<?> pDeclaration) {
    switch (pDeclaration.getKind()) {
      case NOT:
        return "!";
      case UMINUS:
      case BV_NEG:
      case FP_NEG:
        return "-";
      case AND:
        return " && ";
      case BV_AND:
        return " & ";
      case OR:
        return "\n|| ";
      case BV_OR:
        return " | ";
      case BV_XOR:
        return " ^ ";
      case SUB:
      case BV_SUB:
      case FP_SUB:
        return " - ";
      case ADD:
      case BV_ADD:
      case FP_ADD:
        return " + ";
      case DIV:
      case BV_SDIV:
      case BV_UDIV:
      case FP_DIV:
        return " / ";
      case MUL:
      case BV_MUL:
      case FP_MUL:
        return " * ";
      case MODULO:
      case BV_SREM:
      case BV_UREM:
        return " % ";
      case LT:
      case BV_SLT:
      case BV_ULT:
      case FP_LT:
        return " < ";
      case LTE:
      case BV_SLE:
      case BV_ULE:
      case FP_LE:
        return " <= ";
      case GT:
      case BV_SGT:
      case BV_UGT:
      case FP_GT:
        return " > ";
      case GTE:
      case BV_SGE:
      case BV_UGE:
      case FP_GE:
        return " >= ";
      case EQ:
      case BV_EQ:
      case FP_EQ:
        return " == ";
      case EQ_ZERO:
        return " == 0";
      case GTE_ZERO:
        return " >= 0";
      case BV_NOT:
        return "~";
      case BV_SHL:
        return " << ";
      case UF:
        // There are several CPAchecker-internal UFs that replace unsupported function in solvers.
        // See FormulaManagerView and ReplaceBitvectorWithNumeralAndFunctionTheory for details.
        switch (pDeclaration.getName()) {
          case "_&_":
            return " & ";
          case "_!!_":
            return " | ";
          case "_^_":
            return " ^ ";
          case "_~_":
            return "~";
          case "_<<_":
            return " << ";
          case "_>>_":
            return " >> "; // TODO arithmetic or logical shift?
          case "_%_":
            return " % ";
          default:
            // $FALL-THROUGH$
        }
        // $FALL-THROUGH$
      default:
        throw new UnsupportedOperationException(
            "Unexpected operand " + pDeclaration.getKind() + "(" + pDeclaration.getName() + ")");
    }
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
