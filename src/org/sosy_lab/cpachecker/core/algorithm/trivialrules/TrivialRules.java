// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;

/** All trivial rules, and the propositions they talk about. */
final class TrivialRules {

  private TrivialRules() {}

  /** All rules, in the order in which they are checked. */
  static ImmutableList<TrivialRule> all() {
    return ImmutableList.<TrivialRule>builder().addAll(ReachabilityRules.rules()).build();
  }

  /** The name that SV-COMP uses for the given proposition. */
  static String nameOf(Property pProperty) {
    if (pProperty instanceof CommonVerificationProperty property) {
      return switch (property) {
        case REACHABILITY, REACHABILITY_ERROR, REACHABILITY_LABEL -> "unreach-call";
        case VALID_FREE -> "valid-free";
        case VALID_DEREF -> "valid-deref";
        case VALID_MEMTRACK -> "valid-memtrack";
        case VALID_MEMCLEANUP -> "valid-memcleanup";
        case OVERFLOW -> "no-overflow";
        case DATA_RACE -> "no-data-race";
        case DEADLOCK -> "no-deadlock";
        case TERMINATION -> "termination";
        case ASSERT -> "assert";
        case CORRECT_ANNOTATIONS -> "correct-annotations";
      };
    }
    return pProperty.toString();
  }

}
