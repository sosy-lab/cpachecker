package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

/**
 * Converts a set of invariants to the pretty text representation.
 */
public class PolicyDotWriter {
  public String toDOTLabel(Map<LinearExpression, PolicyTemplateBound> data) {
    StringBuilder b = new StringBuilder();
    b.append("\n");

    // TODO: might want to do it as a pre-processing rather than
    // only for printing.

    // Convert to the readable format.
    Map<LinearExpression, PolicyTemplateBound> toSort = new HashMap<>();

    // Pretty-printing is tricky.
    Map<LinearExpression, ExtendedRational> lessThan = new HashMap<>();
    Map<LinearExpression, Pair<ExtendedRational, ExtendedRational>> bounded
        = new HashMap<>();
    Map<LinearExpression, ExtendedRational> equal = new HashMap<>();

    toSort.putAll(data);
    while (toSort.size() > 0) {
      LinearExpression template, negTemplate;
      template = toSort.keySet().iterator().next();
      ExtendedRational upperBound = toSort.get(template).bound;

      negTemplate = template.negate();

      toSort.remove(template);

      if (toSort.containsKey(negTemplate)) {
        ExtendedRational lowerBound = toSort.get(negTemplate).bound.negate();
        toSort.remove(negTemplate);

        // Rotate the pair if necessary.
        boolean negated = false;
        if (template.toString().startsWith("-")) {
          negated = true;
        }

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
        lessThan.put(template, upperBound);
      }
    }

    // Print equals.
    for (Map.Entry<LinearExpression, ExtendedRational> entry : equal.entrySet()) {
      b.append(entry.getKey())
          .append("=")
          .append(entry.getValue())
          .append("\n");
    }

    // Print bounded.
    for (Map.Entry<LinearExpression, Pair<ExtendedRational, ExtendedRational>> entry
        : bounded.entrySet()) {
      b
          .append(entry.getValue().getFirst())
          .append("≤")
          .append(entry.getKey())
          .append("≤")
          .append(entry.getValue().getSecond())
          .append("\n");
    }

    // Print less-than.
    for (Map.Entry<LinearExpression, ExtendedRational> entry : lessThan.entrySet()) {
      b.append(entry.getKey())
          .append("≤")
          .append(entry.getValue())
          .append("\n");
    }

    return b.toString();

  }
}
