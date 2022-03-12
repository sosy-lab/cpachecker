// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.defuse;

import com.google.common.collect.ImmutableSet;
import java.util.Iterator;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class DefUseState implements AbstractState, Iterable<DefUseDefinition> {
  private final Set<DefUseDefinition> definitions;

  public DefUseState(Set<DefUseDefinition> definitions) {
    this.definitions = ImmutableSet.copyOf(definitions);
  }

  public DefUseState(DefUseState definitions, DefUseDefinition newDefinition) {
    ImmutableSet.Builder<DefUseDefinition> builder = ImmutableSet.builder();
    builder.add(newDefinition);
    for (DefUseDefinition def : definitions.definitions) {
      if (!def.getVariableName().equals(newDefinition.getVariableName())) {
        builder.add(def);
      }
    }
    this.definitions = builder.build();
  }

  @Override
  public Iterator<DefUseDefinition> iterator() {
    return definitions.iterator();
  }

  public boolean containsAllOf(DefUseState other) {
    return definitions.containsAll(other.definitions);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof DefUseState)) {
      return false;
    }

    DefUseState otherDefUse = (DefUseState) other;
    return otherDefUse.definitions.equals(definitions);
  }

  @Override
  public int hashCode() {
    return definitions.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append('{');

    boolean hasAny = false;
    for (DefUseDefinition def : definitions) {
      CFAEdge assigningEdge = def.getAssigningEdge();
      builder.append('(').append(def.getVariableName()).append(", ");

      if (assigningEdge != null) {
        builder.append(assigningEdge.getPredecessor().getNodeNumber());
      } else {
        builder.append(0);
      }

      builder.append(", ");

      if (assigningEdge != null) {
        builder.append(assigningEdge.getSuccessor().getNodeNumber());
      } else {
        builder.append(0);
      }

      builder.append("), ");
      hasAny = true;
    }

    if (hasAny) {
      builder.replace(builder.length() - 2, builder.length(), "}");
    } else {
      builder.append('}');
    }

    return builder.toString();
  }
}
