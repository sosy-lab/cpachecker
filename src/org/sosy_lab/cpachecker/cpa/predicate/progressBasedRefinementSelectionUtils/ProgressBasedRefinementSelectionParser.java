// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.List;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionPatternAtom;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionPatternNode;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionPatternOperator;

/**
 * Parses DSL pattern strings (loaded from the DSL in JSON format via {@link
 * ProgressBasedRefinementSelectionDslLoader}) into a recursive, structured {@link
 * ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionPatternNode} tree. The tree
 * is later traversed by the {@link ProgressBasedRefinementSelectionMatchingVisitor} to match
 * against normalized s-expressions. Parsing is done in two steps:
 *
 * <ul>
 *   <li>{@code tokenize()} splits the input into individual tokens (parentheses, operators,
 *       wildcards).
 *   <li>{@code parseNode()} recursively builds {@link
 *       ProgressBasedRefinementSelectionPatternOperator} and {@link
 *       ProgressBasedRefinementSelectionPatternAtom} nodes.
 * </ul>
 *
 * Wildcards such as {@code <var>} are parsed as {@link
 * ProgressBasedRefinementSelectionPatternAtom}, operators and functions applications as {@link
 * ProgressBasedRefinementSelectionPatternOperator}.
 */
public class ProgressBasedRefinementSelectionParser {

  /**
   * Parses a DSL pattern String into a structured pattern tree.
   *
   * @param pPatternMatch DSL pattern String to parse and match
   * @return the root node of the parsed, structured pattern tree
   * @throws IllegalArgumentException if the input contains extra tokens
   */
  public static ProgressBasedRefinementSelectionPatternNode parseExpression(String pPatternMatch) {
    List<String> tokens = tokenize(pPatternMatch);
    ArrayDeque<String> stack = new ArrayDeque<>(tokens);

    ProgressBasedRefinementSelectionPatternNode root = parseNode(stack);

    if (!stack.isEmpty()) {
      throw new IllegalArgumentException("Extra tokens after pattern: " + stack);
    }

    return root;
  }

  /**
   * Splits a DSL pattern String into individual tokens such as parentheses, operators, and
   * wildcards
   *
   * @param pPatternMatch DSL pattern String to parse and match
   * @return a list of tokens, ordered by appearance in DSL pattern String
   */
  private static List<String> tokenize(String pPatternMatch) {
    ImmutableList.Builder<String> tokens = ImmutableList.builder();
    StringBuilder current = new StringBuilder();

    for (int i = 0; i < pPatternMatch.length(); i++) {
      char c = pPatternMatch.charAt(i);
      if (c == '(' || c == ')') {
        if (!current.isEmpty()) {
          tokens.add(current.toString());
          current.setLength(0);
        }
        tokens.add(String.valueOf(c));
      } else if (Character.isWhitespace(c)) {
        if (!current.isEmpty()) {
          tokens.add(current.toString());
          current.setLength(0);
        }
      } else {
        current.append(c);
      }
    }
    if (!current.isEmpty()) {
      tokens.add(current.toString());
    }
    return tokens.build();
  }

  /**
   * Recursively parses tokens into a structured pattern node tree.
   *
   * @param stack the tokens to consume
   * @return a structured pattern node tree
   */
  private static ProgressBasedRefinementSelectionPatternNode parseNode(ArrayDeque<String> stack) {
    checkArgument(!stack.isEmpty(), "Pattern ended unexpectedly");
    String token = stack.removeFirst();

    if ("(".equals(token)) {
      String operator = require(stack);
      ImmutableList.Builder<ProgressBasedRefinementSelectionPatternNode> subNodes =
          ImmutableList.builder();

      while (!")".equals(peek(stack))) {
        subNodes.add(parseNode(stack));
      }
      stack.removeFirst();
      return new ProgressBasedRefinementSelectionPatternOperator(operator, subNodes.build());
    }

    // Unmatched ')'
    checkArgument(!")".equals(token), "Unmatched ')'");

    // Wildcard <var>
    if (token.length() >= 2 && token.startsWith("<") && token.endsWith(">")) {
      return new ProgressBasedRefinementSelectionPatternAtom(
          token.substring(1, token.length() - 1));
    }

    // Zero-arity operator / atom as operator
    return new ProgressBasedRefinementSelectionPatternOperator(token, ImmutableList.of());
  }

  /**
   * Fetches and removes the next token from the stack.
   *
   * @param stack the tokens to consume
   * @return the next token
   */
  private static String require(ArrayDeque<String> stack) {
    checkArgument(!stack.isEmpty(), "Expected operator after '('");
    return stack.removeFirst();
  }

  /**
   * Looks for the next token in the stack, making sure it exists.
   *
   * @param stack the tokens to consume
   * @return the next token
   */
  private static String peek(ArrayDeque<String> stack) {
    String token = stack.peekFirst();
    checkArgument(token != null, "Missing ')' to close '(' for operator");
    return token;
  }
}
