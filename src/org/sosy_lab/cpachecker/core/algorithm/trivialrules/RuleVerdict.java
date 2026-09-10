// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * The answer of a {@link TrivialRule} that did not abstain: whether the propositions of the rule
 * hold or are violated, and the concrete reason, such that the answer can be checked by hand.
 *
 * @param isViolation whether the propositions of the rule are violated
 * @param reason why the rule gives this answer for this program
 * @param violatingEdge the edge whose execution violates the specification, if the rule knows it
 */
record RuleVerdict(boolean isViolation, String reason, @Nullable CFAEdge violatingEdge) {

  RuleVerdict {
    checkNotNull(reason);
  }

  /** The propositions of the rule hold, for the given reason. */
  static Optional<RuleVerdict> proven(String pReason) {
    return Optional.of(new RuleVerdict(false, pReason, null));
  }

  /** The propositions of the rule are violated when the given edge is executed. */
  static Optional<RuleVerdict> refuted(String pReason, CFAEdge pViolatingEdge) {
    return Optional.of(new RuleVerdict(true, pReason, checkNotNull(pViolatingEdge)));
  }
}
