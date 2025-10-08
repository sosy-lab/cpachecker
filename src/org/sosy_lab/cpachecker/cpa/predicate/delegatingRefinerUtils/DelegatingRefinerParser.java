// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.List;

/**
 * Parses DSL pattern strings (loaded from the DSL in JSON format via {@link
 * DelegatingRefinerDslLoader}) into a recursive, structured {@link DelegatingRefinerPatternNode}
 * tree. The tree is later traversed by the {@link DelegatingRefinerMatchingVisitor} to match
 * against normalized s-expressions. Parsing is done in two steps:
 *
 * <ul>
 *   <li>{@code tokenize()} splits the input into individual tokens (parentheses, operators,
 *       wildcards).
 *   <li>{@code parseNode()} recursively builds {@link DelegatingRefinerPatternOperator} and {@link
 *       DelegatingRefinerDelegatingRefinerPatternAtom} nodes.
 * </ul>
 *
 * Wildcards such as {@code <var>} are parsed as {@link
 * DelegatingRefinerDelegatingRefinerPatternAtom}, operators and functions applications as {@link
 * DelegatingRefinerPatternOperator}.
 */
public class DelegatingRefinerParser {

  public static DelegatingRefinerPatternNode parseExpression(String pPatternMatch) {
    List<String> tokens = tokenize(pPatternMatch);
    ArrayDeque<String> stack = new ArrayDeque<>(tokens);

    DelegatingRefinerPatternNode root = parseNode(stack);

    if (!stack.isEmpty()) {
      throw new IllegalArgumentException("Extra tokens after pattern: " + stack);
    }

    return root;
  }

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

  private static DelegatingRefinerPatternNode parseNode(ArrayDeque<String> stack) {
    if (stack.isEmpty()) {
      throw new IllegalArgumentException("Pattern ended unexpectedly");
    }
    String token = stack.removeFirst();

    if ("(".equals(token)) {
      String operator = require(stack);
      ImmutableList.Builder<DelegatingRefinerPatternNode> subNodes = ImmutableList.builder();

      while (!")".equals(peek(stack))) {
        subNodes.add(parseNode(stack));
      }
      stack.removeFirst();
      return new DelegatingRefinerPatternOperator(operator, subNodes.build());
    }

    // Unmatched ')'
    if (")".equals(token)) {
      throw new IllegalArgumentException("Unmatched ')'");
    }

    // Wildcard <var>
    if (token.length() >= 2 && token.startsWith("<") && token.endsWith(">")) {
      return new DelegatingRefinerDelegatingRefinerPatternAtom(
          token.substring(1, token.length() - 1));
    }

    // Zero-arity operator / atom as operator
    return new DelegatingRefinerPatternOperator(token, ImmutableList.of());
  }

  private static String require(ArrayDeque<String> stack) {
    if (stack.isEmpty()) {
      throw new IllegalArgumentException("Expected operator after '('");
    }
    return stack.removeFirst();
  }

  private static String peek(ArrayDeque<String> stack) {
    String token = stack.peekFirst();
    if (token == null) {
      throw new IllegalArgumentException("Missing ')' to close '(' for operator");
    }
    return token;
  }
}
