// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import de.uni_freiburg.informatik.ultimate.util.datastructures.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.transformation.SubCFA;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;
import java.util.Comparator;

public class LocationPrecision implements AdjustablePrecision {

  private ImmutableSet<SubCFA> precisions;

  private Comparator<SubCFA> precisionOrder = (a,b) -> 1;

  public LocationPrecision(Set<SubCFA> pPrecisions) {
    precisions = ImmutableSet.of(pPrecisions);
  }

  public ImmutableSet<SubCFA> getPrecisions() {
    return precisions;
  }

  @Override
  public AdjustablePrecision add(AdjustablePrecision otherPrecision) {
    return null;
  }

  @Override
  public AdjustablePrecision subtract(AdjustablePrecision otherPrecision) {
    return null;
  }

  @Override
  public boolean isEmpty() {
    return precisions.isEmpty();
  }
}
