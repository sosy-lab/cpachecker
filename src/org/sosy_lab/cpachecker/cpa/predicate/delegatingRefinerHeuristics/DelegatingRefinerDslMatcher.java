// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Applies the first matching DSL pattern rule to a normalized atomic formula. Returns a normalized
 * formula with metadata such as tags or category.
 */
final class DelegatingRefinerDslMatcher {
  private final ImmutableList<DelegatingRefinerPatternRule> patternRules;

  public DelegatingRefinerDslMatcher(ImmutableList<DelegatingRefinerPatternRule> pPatternRules) {
    this.patternRules = pPatternRules;
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
        Map<String, String> bindings = mayBeBindings.get();
        String normalized = substitute(patternRule.normalizedPattern(), bindings);
        String fingerprint = substitute(patternRule.patternFingerprint(), bindings);
        ImmutableMap<String, String> tagged = applyTags(patternRule.tags(), bindings);
        return Optional.of(
            new DelegatingRefinerNormalizedFormula(
                normalized, fingerprint, patternRule.id(), tagged, patternRule.category()));
      }
    }
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

      if (pP.contains("<")) {
        Optional<ImmutableMap<String, String>> mayBeTokenBindings =
            matchTokensWithWildcards(pP, eP);
        if (mayBeTokenBindings.isEmpty()) {
          return Optional.empty();
        }
        bindings.putAll(mayBeTokenBindings.get());
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

  // Matches tokens with underscore-separated wildcards, e.g. BVExtract_31_31.
  private static Optional<ImmutableMap<String, String>> matchTokensWithWildcards(
      String pPatternToken, String pExpressionToken) {
    Map<String, String> bindings = new HashMap<>();

    ImmutableList<String> patternParts =
        ImmutableList.copyOf(Splitter.on('_').split(pPatternToken));
    ImmutableList<String> expressionParts =
        ImmutableList.copyOf(Splitter.on('_').split(pExpressionToken));

    if (patternParts.size() != expressionParts.size()) {
      return Optional.empty();
    }

    for (int i = 0; i < patternParts.size(); i++) {
      String p = patternParts.get(i);
      String e = expressionParts.get(i);
      if (p.startsWith("<") && p.endsWith(">")) {
        bindings.put(p.substring(1, p.length() - 1), e);
      } else if (!p.equals(e)) {
        return Optional.empty();
      }
    }
    return Optional.of(ImmutableMap.copyOf(bindings));
  }

  // Applies tags by substituting placeholders and merging duplicates.
  private ImmutableMap<String, String> applyTags(
      Map<String, String> pTagTemplate, Map<String, String> pBindings) {
    Multimap<String, String> merged = ArrayListMultimap.create();

    for (Map.Entry<String, String> entry : pTagTemplate.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if (key.startsWith("<") && key.endsWith(">")) {
        String bound = pBindings.get(key.substring(1, key.length() - 1));
        if (bound != null) {
          merged.put(bound, value);
        }
      } else {
        merged.put(key, value);
      }
    }

    ImmutableMap.Builder<String, String> tagged = ImmutableMap.builder();
    for (String k : merged.keySet()) {
      String mergedValue = String.join(",", merged.get(k));
      tagged.put(k, mergedValue);
    }

    return tagged.buildKeepingLast();
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
    return head.equals("=") || head.equals("not") || head.startsWith("_T") || head.startsWith("bv");
  }
}
