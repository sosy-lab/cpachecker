// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;

@Options(prefix="exprconv")
public class ExpressionConverter {

  //static access for debugging purposes
  public static ExpressionConverter converter = new ExpressionConverter();

  // set this option change the formulas from prefix to infix when printed to user
  @Option(secure=true, name="niceexpr",
      description="transform boolean formulas")
  private boolean niceExpressions = false;

  public ExpressionConverter(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  // enable static access
  private ExpressionConverter() {}

  /**
   * Converts every string which matches:
   * expr = ('binary_operator' expr expr) |
   *        ('unary_operator' expr) |
   *        Variable |
   *        Literal
   * operator = and | or | = | <= | < | >= | > ...
   * unary_operator = not
   * Variable = method::varname@digit
   * varname = String
   * Literal = Number | String
   * @param input input-string in pre-order
   * @return infix notation of input
   */
  public String convert(String input) {
    if(!niceExpressions) {
      return input;
    }
    input = input.replaceAll("\\*", "p_");
    int exp = 0;
    Map<String, String> expressions = new HashMap<>();
    String lastExpression = "";
    while (input.contains(")")) {
      // Find the deepest () in input
      StringBuilder formula = new StringBuilder();
      for (int i = 0; i < input.length(); i++) {
        if (input.charAt(i) == ')') {
          break;
        }
        if (input.charAt(i) == '(') {
          formula = new StringBuilder();
        } else {
          formula.append(input.charAt(i));
        }
      }

      // Create Expression
      String expr = "<expr" + exp + ">";
      expressions.put(expr, formula.toString());

      // Prepare new loop
      lastExpression = expr;
      input = input.replaceFirst("\\(" + formula + "\\)", expr);
      exp++;
    }

    if (lastExpression.isEmpty()) {
      return input;
    }

    ArrayDeque<String> process = new ArrayDeque<>();
    process.addFirst(lastExpression);

    while (!process.isEmpty()) {
      String expr = process.removeFirst();
      String current = expressions.get(expr);
      String[] objects = current.split("` ", 2);
      String operator = objects[0];
      operator = operator.replaceFirst("`", "");
      operator = convertOperator(operator);
      String[] operands = objects[1].split(" ", 2);

      if (expressions.containsKey(operands[0])) {
        process.addFirst(operands[0]);
      }

      if (operands.length == 1) {
        if (expressions.getOrDefault(operands[0], "").contains(" ")) {
          input = input.replaceFirst(expr, operator + " (" + operands[0] + ")");
        } else {
          input = input.replaceFirst(expr, operator + " " + operands[0]);
        }
      } else if (operands.length == 2) {
        if (expressions.containsKey(operands[1])) {
          process.addFirst(operands[1]);
        }
        if (operator.equals("∧")) {
          input = input.replaceFirst(expr, operands[0] + " " + operator + " " + operands[1]);
        } else {
          input =
              input.replaceFirst(
                  expr, "(" + operands[0] + " " + operator + " " + operands[1] + ")");
        }
      } else {
        throw new AssertionError("Only unary and binary operations are allowed.");
      }
    }
    return input;
  }

  public String convert(PathFormula formula) {
    return convert(formula.toString());
  }

  public String convert(BooleanFormula formula) {
    return convert(formula.toString());
  }

  /**
   * Convert a formula operator to a readable operator
   * @param operator operator in a BooleanFormula object
   * @return readable operator
   */
  private String convertOperator(String operator) {
    if (operator.startsWith("=")) {
      return "=";
    }
    if (operator.startsWith("bvslt")) {
      return "<";
    }
    if (operator.startsWith("bvadd")) {
      return "+";
    }
    if (operator.startsWith("bvextract")) {
      return "";
    }
    switch (operator) {
      case "and":
        return "∧";
      case "or":
        return "v";
      case "not":
        return "¬";
      case "bvsdiv_32":
        return "/";
      default:
        return operator;
    }
  }
}
