// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Processing records belonging to one block-state occurrence, not to its abstract value.
 *
 * <p>Block ends are kept separate by local coverage until processing records can be transferred
 * across late merges. A ghost state's back-pointer identifies the occurrence whose obligations it
 * discharges. A new exploration always creates fresh records.
 */
final class BlockStateObligations {

  private ImmutableSet<AbstractState> processed = ImmutableSet.of();
  private final Set<AbstractState> hinderedByCallstack = new LinkedHashSet<>();

  boolean isProcessed(AbstractState pCondition) {
    return processed.contains(pCondition);
  }

  void setProcessed(Iterable<? extends AbstractState> pConditions) {
    processed = ImmutableSet.copyOf(pConditions);
  }

  void addHinderedByCallstack(AbstractState pCondition) {
    hinderedByCallstack.add(pCondition);
  }

  ImmutableSet<AbstractState> getHinderedByCallstack() {
    return ImmutableSet.copyOf(hinderedByCallstack);
  }
}
