// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.transformation.SubCFA;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;

public class LocationPrecision implements AdjustablePrecision {

  private ImmutableSet<SubCFA> allowedProgramTransformations;
  private final ImmutableMultimap<CFANode, SubCFA> nodesToSubCFA;
  private final boolean hasProgramTransformations;
  //private Comparator<SubCFA> precisionOrder = (a,b) -> 1;

  public LocationPrecision(Set<SubCFA> pPrecisions) {
    hasProgramTransformations = !pPrecisions.isEmpty();
    allowedProgramTransformations = ImmutableSet.copyOf(pPrecisions);
    ImmutableMultimap.Builder<CFANode,SubCFA> nodesToStrategiesBuilder = new Builder<>();
    for (SubCFA subCFA : allowedProgramTransformations) {
      nodesToStrategiesBuilder.put(subCFA.originalCFAEntryNode(), subCFA);
    }
    nodesToSubCFA = nodesToStrategiesBuilder.build();
  }

  public ImmutableSet<SubCFA> getStrategiesForNode(CFANode pNode){
    return ImmutableSet.copyOf(nodesToSubCFA.get(pNode));
  }

  /**
   * Function for selecting the most abstract strategy from a set of strategies.
   *
   * @param strategies the given set of allowed strategies
   *
   * @return The most abstract strategy in the precision set or empty for the basic strategy.
   */
  public static Optional<SubCFA> select(ImmutableSet<SubCFA> strategies) {
    if (strategies.isEmpty()) {
      return Optional.empty();
    }
    // TODO For now returns any strategy
    Iterator<SubCFA> strategyIterator = strategies.iterator();
    return Optional.of(strategyIterator.next());
  }

  public ImmutableSet<SubCFA> getAllowedProgramTransformations() {
    return allowedProgramTransformations;
  }

  public boolean hasProgramTransformations() {
    return hasProgramTransformations;
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
    return allowedProgramTransformations.isEmpty();
  }
}
