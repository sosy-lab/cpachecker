// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.path;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

public class PathState implements Targetable, AbstractState {

  private final ImmutableList<CFAEdge> path;
  private final int index;

  public static final AbstractState INVALID = new PathState(null, 0);

  public PathState(ImmutableList<CFAEdge> pPath) {
    this(pPath, 0);
  }

  private PathState(ImmutableList<CFAEdge> pPath, int pIndex) {
    path = pPath;
    index = pIndex;
  }

  public CFAEdge getAdvanceEdge() {
    return path.get(index);
  }

  public boolean canAdvance() {
    return path != null && index < path.size();
  }

  public PathState advance() {
    assert canAdvance();
    return new PathState(path, index + 1);
  }

  @Override
  public int hashCode() {
    return Objects.hash(index, path);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof PathState other
        && Objects.equals(path, other.path)
        && index == other.index;
  }

  @Override
  public String toString() {
    return "After step" + index + ", remaining path: " + path.subList(index, path.size());
  }

  @Override
  public boolean isTarget() {
    return path != null && index == path.size();
  }

  @Override
  public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    return isTarget()
        ? ImmutableSet.of(
            new TargetInformation() {
              @Override
              public String toString() {
                // TODO more general message?? if someone else wants to use it / or make it clear
                // that it is for DSS
                return "Path synthesized from violation condition";
              }
            })
        : ImmutableSet.of();
  }
}
