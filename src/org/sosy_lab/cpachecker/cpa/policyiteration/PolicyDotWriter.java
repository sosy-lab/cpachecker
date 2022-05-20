// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.policyiteration;

import org.sosy_lab.common.rationals.LinearExpression;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.templates.Template;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Converts a set of invariants to the pretty text representation.
 */
public class PolicyDotWriter {
  public String toDOTLabel(Map<Template, PolicyBound> data) {
    StringBuilder b = new StringBuilder();
    b.append("\n");

    // Convert to the readable format.
    Map<LinearExpression<?>, PolicyBound> toSort = new HashMap<>();

    // Pretty-printing is tricky.
    Map<LinearExpression<?>, Rational> lessThan = new HashMap<>();
    Map<LinearExpression<?>, Rational> greaterThan = new HashMap<>();
    Map<LinearExpression<?>, Pair<Rational, Rational>> bounded
        = new HashMap<>();
    Map<LinearExpression<?>, Rational> equal = new HashMap<>();

    for (Map.Entry<Template, PolicyBound> e : data.entrySet()) {
      toSort.put(e.getKey().getLinearExpression(), e.getValue());
    }
    while (toSort.size() > 0) {
      LinearExpression<?> template, negTemplate;
      Entry<LinearExpression<?>, PolicyBound> entry = toSort.entrySet().iterator().next();
      template = entry.getKey();
      Rational upperBound = entry.getValue().getBound();

      negTemplate = template.negate();

      toSort.remove(template);

      // Rotate the inequality if necessary.
      boolean negated = isNegated(template);

      if (toSort.containsKey(negTemplate)) {
        Rational lowerBound = toSort.get(negTemplate).getBound().negate();
        toSort.remove(negTemplate);

        if (lowerBound.equals(upperBound)) {
          if (negated) {
            equal.put(template.negate(), lowerBound.negate());
          } else {
            equal.put(template, lowerBound);
          }
        } else {
          if (negated) {
            bounded.put(
                template.negate(), Pair.of(upperBound.negate(), lowerBound.negate()));
          } else {
            bounded.put(
                template, Pair.of(lowerBound, upperBound));
          }
        }
      } else {
        if (negated) {
          greaterThan.put(template.negate(), upperBound.negate());
        } else {
          lessThan.put(template, upperBound);
        }
      }
    }

    // Print equals.
    for (Map.Entry<LinearExpression<?>, Rational> entry : equal.entrySet()) {
      b.append(entry.getKey())
          .append("=")
          .append(entry.getValue())
          .append("\n");
    }

    // Print bounded.
    for (Map.Entry<LinearExpression<?>, Pair<Rational, Rational>> entry : bounded.entrySet()) {
      b
          .append(entry.getValue().getFirst())
          .append("≤")
          .append(entry.getKey())
          .append("≤")
          .append(entry.getValue().getSecond())
          .append("\n");
    }

    // Print less-than.
    for (Map.Entry<LinearExpression<?>, Rational> entry : lessThan.entrySet()) {
      b.append(entry.getKey())
          .append("≤")
          .append(entry.getValue())
          .append("\n");
    }

    // Printing greater-than.
    for (Map.Entry<LinearExpression<?>, Rational> entry : greaterThan.entrySet()) {
      b.append(entry.getKey())
          .append("≥")
          .append(entry.getValue())
          .append("\n");
    }
    return b.toString();
  }

  private boolean isNegated(LinearExpression<?> pTemplate) {
   return pTemplate.toString().trim().startsWith("-");
  }
}
