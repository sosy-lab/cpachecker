// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;

record GraphMLState(
    String id,
    ImmutableSet<String> invariants,
    Optional<String> explicitInvariantScope,
    ImmutableSet<NodeFlag> flags) {

  GraphMLState {
    Objects.requireNonNull(id);
    Objects.requireNonNull(invariants);
    Objects.requireNonNull(explicitInvariantScope);
    Objects.requireNonNull(flags);
  }

  @Override
  public String toString() {
    return id();
  }

  boolean isEntryState() {
    return flags.contains(NodeFlag.ISENTRY);
  }

  boolean isSinkState() {
    return flags.contains(NodeFlag.ISSINKNODE);
  }

  boolean isViolationState() {
    return flags.contains(NodeFlag.ISVIOLATION);
  }

  boolean isCycleHead() {
    return flags.contains(NodeFlag.ISCYCLEHEAD);
  }
}
