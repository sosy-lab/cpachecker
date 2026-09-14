// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * The answer of a {@link TrivialRule}: whether the rule abstains, whether the propositions of the
 * rule hold, or whether they are violated, together with the concrete reason, such that the answer
 * can be checked by hand.
 *
 * @param outcome what the rule says about its propositions
 * @param reason why the rule gives this answer for this program
 * @param violatingEdge the edge whose execution violates the specification, non-{@code null} if and
 *     only if the outcome is {@link Outcome#REFUTED}
 */
record RuleVerdict(Outcome outcome, String reason, @Nullable CFAEdge violatingEdge) {

  /** What a {@link TrivialRule} says about the propositions it decides. */
  enum Outcome {
    /** The argument of the rule does not hold for this program, so the rule says nothing. */
    ABSTAINED,
    /** The propositions of the rule hold for this program. */
    PROVEN,
    /** The propositions of the rule are violated by this program. */
    REFUTED,
  }

  private static final RuleVerdict ABSTENTION =
      new RuleVerdict(
          Outcome.ABSTAINED, "the argument of the rule does not hold for this program", null);

  RuleVerdict {
    checkNotNull(outcome);
    checkArgument(!reason.isEmpty(), "a verdict needs a reason that can be checked by hand");
    checkArgument(
        (violatingEdge != null) == (outcome == Outcome.REFUTED),
        "a violating edge belongs to a refutation and only to a refutation, but got %s with %s",
        outcome,
        violatingEdge);
  }

  /** The rule says nothing about this program, because its argument does not hold for it. */
  static RuleVerdict abstained() {
    return ABSTENTION;
  }

  /** The propositions of the rule hold, for the given reason. */
  static RuleVerdict proven(String pReason) {
    return new RuleVerdict(Outcome.PROVEN, pReason, null);
  }

  /** The propositions of the rule are violated when the given edge is executed. */
  static RuleVerdict refuted(String pReason, CFAEdge pViolatingEdge) {
    return new RuleVerdict(Outcome.REFUTED, pReason, checkNotNull(pViolatingEdge));
  }

  boolean isAbstention() {
    return outcome == Outcome.ABSTAINED;
  }

  boolean isViolation() {
    return outcome == Outcome.REFUTED;
  }

  /**
   * The edge whose execution violates the specification. Only a refutation has such an edge, cf.
   * {@link #isViolation()}.
   */
  CFAEdge violatingEdgeOrThrow() {
    checkArgument(isViolation(), "only a refutation has a violating edge, but this is %s", outcome);
    return checkNotNull(violatingEdge);
  }
}
