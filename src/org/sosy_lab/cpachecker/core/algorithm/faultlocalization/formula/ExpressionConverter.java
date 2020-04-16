/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula;

import java.util.ArrayDeque;
import java.util.HashMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class ExpressionConverter {

  /**
   * Converts every string which matches: expr = ('binary_operator' expr expr) | ('unary_operator'
   * expr) | Variable | Literal operator = and | or | = | <= | < | >= | > ... unary_operator =
   * not... Variable = method::varname@digit varname = String Literal = Number | String
   *
   * @param input input-string in pre-order
   * @return infix notation of input
   */
  public static String convert(String input) {
    input = input.replaceAll("\\*", "p_");
    int exp = 0;
    HashMap<String, String> expressions = new HashMap<>();
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

      if (operands.length < 1 || operands.length > 2) {
        throw new IllegalArgumentException("Only unary and binary operations are allowed.");
      }

      if (expressions.containsKey(operands[0])) process.addFirst(operands[0]);
      if (operands.length == 2) {
        if (expressions.containsKey(operands[1])) process.addFirst(operands[1]);
        if (operator.equals("∧")) {
          input = input.replaceFirst(expr, operands[0] + " " + operator + " " + operands[1]);
        } else {
          input =
              input.replaceFirst(
                  expr, "(" + operands[0] + " " + operator + " " + operands[1] + ")");
        }
      } else {
        if (expressions.getOrDefault(operands[0], "").contains(" ")) {
          input = input.replaceFirst(expr, operator + " (" + operands[0] + ")");
        } else {
          input = input.replaceFirst(expr, operator + " " + operands[0]);
        }
      }
    }
    return input;
  }

  public static String convert(PathFormula formula) {
    return convert(formula.toString());
  }

  public static String convert(BooleanFormula formula) {
    return convert(formula.toString());
  }

  private static String convertOperator(String operator) {
    if (operator.startsWith("=")) {
      return "=";
    }
    if (operator.startsWith("bvslt")) {
      return "<";
    }
    if (operator.startsWith("bvadd")) {
      return "+";
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
