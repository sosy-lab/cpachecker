// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.util.Pair;

public class ExtendedARGPath extends ARGPath {

  private final UsageInfo usage;
  // All blocks now check pairs
  private final Set<ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>>
      refinedAsTrueBy = new HashSet<>();
  private boolean isUnreachable = false;

  public ExtendedARGPath(ARGPath origin, UsageInfo target) {
    super(origin.asStatesList(), origin.getInnerEdges());
    usage = target;
  }

  public UsageInfo getUsageInfo() {
    return usage;
  }

  public void setAsTrueBy(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> refiner) {
    refinedAsTrueBy.add(refiner);
  }

  public void setAsFalse() {
    isUnreachable = true;
  }

  public boolean isRefinedAsReachableBy(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> refiner) {
    return refinedAsTrueBy.contains(refiner);
  }

  public boolean isUnreachable() {
    return isUnreachable;
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 31 + Objects.hash(isUnreachable, refinedAsTrueBy, usage);
  }

  @Override
  public boolean equals(Object obj) {
    if (super.equals(obj) && obj instanceof ExtendedARGPath) {
      ExtendedARGPath other = (ExtendedARGPath) obj;
      return isUnreachable == other.isUnreachable
          && Objects.equals(refinedAsTrueBy, other.refinedAsTrueBy)
          && Objects.equals(usage, other.usage);
    }
    return false;
  }
}
