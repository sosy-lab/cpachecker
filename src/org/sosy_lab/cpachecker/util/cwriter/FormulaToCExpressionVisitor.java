// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.FormulaTransformationVisitor;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;

public class FormulaToCExpressionVisitor extends FormulaTransformationVisitor {

  // TODO: do not hardcode the function separator.
  private static final String FUNCTION_NAME_SEPARATOR = "::";

  private final Map<Formula, String> cache = new HashMap<>();

  FormulaToCExpressionVisitor(FormulaManagerView fmgr) {
    super(fmgr);
  }

  @Override
  public Formula visitFreeVariable(Formula f, String name) {
    if (name.contains(FUNCTION_NAME_SEPARATOR)) {
      cache.put(f, Splitter.on(FUNCTION_NAME_SEPARATOR).splitToList(name).get(1));
    } else {
      cache.put(f, name);
    }
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
    String op;
    String result;
    switch (functionDeclaration.getKind()) {
      case NOT:
      case UMINUS:
      case BV_NEG:
      case FP_NEG:
      case BV_NOT:
        op = operatorFromFunctionDeclaration(functionDeclaration);
        result = op + "(" + cache.get(newArgs.get(0)) + ")";
        cache.put(f, result);
        break;
      case EQ_ZERO:
      case GTE_ZERO:
        op = operatorFromFunctionDeclaration(functionDeclaration);
        result = "(" + cache.get(newArgs.get(0)) + ")" + op;
        cache.put(f, result);
        break;
      case FP_ROUND_EVEN:
      case FP_ROUND_AWAY:
      case FP_ROUND_POSITIVE:
      case FP_ROUND_NEGATIVE:
      case FP_ROUND_ZERO:
      case FP_ROUND_TO_INTEGRAL:
        // Ignore because otherwise rounding mode is treated like an additional operand
        break;
      default:
        List<String> expressions = new ArrayList<>(newArgs.size());
        for (Formula arg : newArgs) {
          if (cache.containsKey(arg)) {
            expressions.add(cache.get(arg));
          }
        }
        op = operatorFromFunctionDeclaration(functionDeclaration);
        cache.put(f, "(" + String.join(op, expressions) + ")");
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
      default:
        throw new UnsupportedOperationException(
            "Unexpected operand " + pDeclaration.getKind() + "(" + pDeclaration.getName() + ")");
    }
  }

  public String getCExpressionForFormula(Formula f) {
    return cache.get(f);
  }
}
