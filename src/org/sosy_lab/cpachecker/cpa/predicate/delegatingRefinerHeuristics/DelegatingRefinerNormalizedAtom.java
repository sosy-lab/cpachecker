// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

/**
 * Represents a normalized atomic formula created by the DelegatingRefinerAtomNormalizer. Each atom
 * encodes a canonical expression with a left operand, an operator and an optional right operand.
 * Atoms are structurally comparable and are used as input for the DelegatingRefinerDslMatcher.
 */
record DelegatingRefinerNormalizedAtom(String leftAtom, String operator, String rightAtom) {
  String toSExpr() {
    if (operator.equals("and") || operator.equals("or")) {
      if (rightAtom != null && !rightAtom.isEmpty()) {
        return "(" + operator + " " + leftAtom + " " + rightAtom + ")";
      }
      return "(" + operator + " " + leftAtom + ")";
    }

    if (rightAtom != null && !rightAtom.isEmpty()) {
      return "(" + operator + " " + leftAtom + " " + rightAtom + ")";
    }

    return "(" + operator + " " + leftAtom + ")";
  }

  String toExpr() {
    if (rightAtom != null && !rightAtom.isEmpty()) {
      return leftAtom + " " + operator + " " + rightAtom;
    }
    return operator + " " + leftAtom;
  }
}
