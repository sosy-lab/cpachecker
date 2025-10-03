// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;

/**
 * Applies the first matching DSL pattern rule to a normalized atomic formula. Returns a normalized
 * formula with metadata such as tags or category.
 */
final class DelegatingRefinerDslMatcher {
  private final ImmutableList<DelegatingRefinerPatternRule> patternRules;
  private final LogManager logger;

  public DelegatingRefinerDslMatcher(
      ImmutableList<DelegatingRefinerPatternRule> pPatternRules, LogManager pLogger) {
    this.patternRules = pPatternRules;
    this.logger = pLogger;
  }

  // Splits an s-expression into tokens (e.g. identifiers, parentheses, numbers).
  private static ImmutableList<String> tokenizeSExpr(String pExpr) {
    ImmutableList.Builder<String> tokens = ImmutableList.builder();
    StringBuilder current = new StringBuilder();

    for (int i = 0; i < pExpr.length(); i++) {
      char c = pExpr.charAt(i);
      if (c == '(' || c == ')') {
        flushToken(tokens, current);
        tokens.add(Character.toString(c));
      } else if (CharMatcher.whitespace().matches(c)) {
        flushToken(tokens, current);
      } else {
        current.append(c);
      }
    }
    flushToken(tokens, current);
    return tokens.build();
  }

  private static void flushToken(ImmutableList.Builder<String> pTokens, StringBuilder pCurrent) {
    if (!pCurrent.isEmpty()) {
      pTokens.add(pCurrent.toString());
      pCurrent.setLength(0);
    }
  }

  // Applies the first matching rule or returns null if none match.
  Optional<DelegatingRefinerNormalizedFormula> applyPatternRule(String pSmtExpr) {
    for (DelegatingRefinerPatternRule patternRule : patternRules) {
      Optional<Map<String, String>> mayBeBindings =
          matchAndExtract(patternRule.patternMatch(), pSmtExpr);
      if (mayBeBindings.isPresent()) {
        Map<String, String> bindings = mayBeBindings.orElseThrow();
        String normalized = substitute(patternRule.normalizedPattern(), bindings);
        return Optional.of(
            new DelegatingRefinerNormalizedFormula(
                normalized, patternRule.id(), patternRule.category()));
      }
    }
    logger.logf(Level.INFO, "No rule matched for: %s", pSmtExpr);
    return Optional.empty();
  }

  // Matches an expression against a pattern and extracts wildcard bindings.
  private Optional<Map<String, String>> matchAndExtract(String pPattern, String pExpression) {
    Map<String, String> bindings = new HashMap<>();

    List<String> patternParts = tokenizeSExpr(pPattern);
    List<String> expressionParts = tokenizeSExpr(pExpression);

    if (patternParts.size() != expressionParts.size()) {
      return Optional.empty();
    }

    for (int i = 0; i < patternParts.size(); i++) {
      String pP = patternParts.get(i);
      String eP = expressionParts.get(i);

      if (pP.startsWith("<") || pP.endsWith(">")) {
        String part = pP.substring(1, pP.length() - 1);
        bindings.put(part, eP);
      } else if (!pP.equals(eP)) {
        return Optional.empty();
      }
    }
    return Optional.of(bindings);
  }

  // Substitutes bindings into a pattern template.
  private String substitute(String pTemplate, Map<String, String> pBindings) {
    String result = pTemplate;
    for (Map.Entry<String, String> entry : pBindings.entrySet()) {
      String placeholder = "<" + entry.getKey() + ">";
      result = result.replace(placeholder, entry.getValue());
    }
    return result;
  }

  // Extracts matchable atoms from an s-expression.
  static ImmutableList<String> extractAtoms(String pExpr) {
    ImmutableList<String> tokens = tokenizeSExpr(pExpr);
    Deque<String> stack = new ArrayDeque<>();
    List<String> atoms = new ArrayList<>();

    for (String token : tokens) {
      if ("(".equals(token)) {
        stack.add("(");
      } else if (")".equals(token)) {
        List<String> subExp = new ArrayList<>();
        while (!stack.isEmpty()) {
          String part = stack.removeLast();
          if ("(".equals(part)) {
            break;
          }
          subExp.add(part);
        }
        Collections.reverse(subExp);

        // Subexpressions with only one token (mostly negations) must not be extracted in a singular
        // atom
        if (subExp.size() == 1
            && (subExp.getFirst().equals("!") || subExp.getFirst().equals("not"))) {
          continue;
        }

        String atom = "(" + String.join(" ", subExp) + ")";
        if (isMatchableAtom(subExp)) {
          atoms.add(atom);
        } else {
          stack.add(atom);
        }
      } else {
        stack.add(token);
      }
    }

    return ImmutableList.copyOf(atoms);
  }

  // Checks if a sub-expression is a matchable atom.
  private static boolean isMatchableAtom(List<String> pSubExpr) {
    if (pSubExpr.isEmpty()) {
      return false;
    }
    String head = pSubExpr.getFirst();
    return head.equals("=")
        || head.equals("not")
        || head.equals("!")
        || head.startsWith("_T")
        || head.startsWith("bv")
        || head.startsWith("bvadd")
        || head.startsWith("bvlshl")
        || head.startsWith("bvextract");
  }
}
