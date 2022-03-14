// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ImmutableSet;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;

class GraphMLState {

  private final String id;

  private final Set<String> invariants;

  private final Optional<String> explicitInvariantScope;

  private final EnumSet<NodeFlag> flags;

  public GraphMLState(
      String pId,
      Set<String> pInvariants,
      Optional<String> pExplicitInvariantScope,
      EnumSet<NodeFlag> pFlags) {
    id = Objects.requireNonNull(pId);
    invariants = ImmutableSet.copyOf(pInvariants);
    explicitInvariantScope = Objects.requireNonNull(pExplicitInvariantScope);
    flags = Objects.requireNonNull(pFlags);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof GraphMLState) {
      GraphMLState other = (GraphMLState) pOther;
      return getId().equals(other.getId())
          && getInvariants().equals(other.getInvariants())
          && getExplicitInvariantScope().equals(other.getExplicitInvariantScope())
          && flags.equals(other.flags);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getInvariants(), getExplicitInvariantScope(), flags);
  }

  @Override
  public String toString() {
    return getId();
  }

  public String getId() {
    return id;
  }

  public Set<String> getInvariants() {
    return invariants;
  }

  public Optional<String> getExplicitInvariantScope() {
    return explicitInvariantScope;
  }

  public boolean isEntryState() {
    return flags.contains(NodeFlag.ISENTRY);
  }

  public boolean isSinkState() {
    return flags.contains(NodeFlag.ISSINKNODE);
  }

  public boolean isViolationState() {
    return flags.contains(NodeFlag.ISVIOLATION);
  }

  public boolean isCycleHead() {
    return flags.contains(NodeFlag.ISCYCLEHEAD);
  }
}
