/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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

  public GraphMLState(String pId,
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
    if (this == pOther) { return true; }
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